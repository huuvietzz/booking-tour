package com.example.tour.tour.service.impl;

import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.tour.dto.request.ItineraryRequest;
import com.example.tour.tour.dto.request.TourFilterRequest;
import com.example.tour.tour.dto.request.TourRequest;
import com.example.tour.tour.dto.response.*;
import com.example.tour.tour.entity.*;
import com.example.tour.tour.repository.CategoryRepository;
import com.example.tour.tour.repository.LocationRepository;
import com.example.tour.tour.repository.ScheduleRepository;
import com.example.tour.tour.repository.TourRepository;
import com.example.tour.tour.service.TourService;
import com.example.tour.tour.specification.TourSpecification;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private static final String THUMBNAIL_FOLDER = "tours/thumbnails";
    private static final String GALLERY_FOLDER   = "tours/gallery";
    private static final int MAX_GALLERY_SIZE = 20;
    private static final Safelist DESCRIPTION_SAFELIST = Safelist.basic()   // p, b, i, u, ul, ol, li, a, blockquote...
            .addTags("h2", "h3")
            .addAttributes(":all", "class");

    private final TourRepository tourRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final FileStorageService fileStorageService;

    // =====================================================================
    // THÊM
    // =====================================================================
    @Override
    @Transactional
    public TourResponse createTour(TourRequest request, MultipartFile thumbnail, List<MultipartFile> galleryImages) {
        Category category = findCategory(request.getCategoryId());
        Location location = findLocation(request.getLocationId());

        List<MultipartFile> newFiles = nonEmpty(galleryImages);
        if (newFiles.size() > MAX_GALLERY_SIZE) {
            throw new AppException(ErrorCode.GALLERY_LIMIT_EXCEEDED);
        }

        // Đăng ký dọn file TRƯỚC khi lưu file: lỗi ở bất kỳ bước nào sau đây
        //    (ảnh thứ 3 sai định dạng, lỗi DB...) → rollback → xoá mọi file đã lưu
        List<String> newUrls = new ArrayList<>();
        fileStorageService.cleanupAfterTransaction(List.of(), newUrls);

        Tour tour = new Tour();
        // Mã tạm để INSERT được (cột NOT NULL + UNIQUE); đổi sang mã thật ngay sau khi có tourId
        tour.setTourCode("TMP-" + UUID.randomUUID());
        applyFields(tour, request, category, location);

        // Ảnh đại diện
        if (hasFile(thumbnail)) {
            String url = fileStorageService.storeImage(thumbnail, THUMBNAIL_FOLDER);
            newUrls.add(url);
            tour.setThumbnailUrl(url);
        }

        // Thư viện ảnh
        appendGallery(tour, newFiles, 0, newUrls);

        // Lịch trình
        addItineraries(tour, request.getItineraries());

        Tour saved = tourRepository.save(tour);

        // tourId không bao giờ trùng → mã sinh từ tourId cũng không bao giờ trùng
        saved.setTourCode(String.format("TOUR%05d", saved.getTourId()));   // 7 → TOUR00007
        // Không cần save lại: entity đang được Hibernate theo dõi, commit sẽ tự UPDATE

        return toTourResponse(saved);
    }

    // =====================================================================
    // SỬA
    // =====================================================================
    @Override
    @Transactional
    public TourResponse updateTour(int tourId, TourRequest request, MultipartFile thumbnail,
                                   boolean removeThumbnail, List<MultipartFile> galleryImages) {
        Tour tour = findTour(tourId);
        Category category = findCategory(request.getCategoryId());
        Location location = findLocation(request.getLocationId());

        // Ảnh thư viện giữ lại: null → giữ hết; có danh sách → chỉ giữ các id đó, theo đúng thứ tự
        List<Image> kept = resolveKeptImages(tour, request.getKeepImageIds());
        List<MultipartFile> newFiles = nonEmpty(galleryImages);
        if (kept.size() + newFiles.size() > MAX_GALLERY_SIZE) {
            throw new AppException(ErrorCode.GALLERY_LIMIT_EXCEEDED);
        }

        // 2. commit → xoá file cũ bị thay / bị bỏ; rollback → xoá file mới vừa lưu
        List<String> oldUrlsToDelete = new ArrayList<>();
        List<String> newUrls = new ArrayList<>();
        fileStorageService.cleanupAfterTransaction(oldUrlsToDelete, newUrls);

        applyFields(tour, request, category, location);

        // 3. Ảnh đại diện: ảnh mới > bỏ ảnh > giữ nguyên
        if (hasFile(thumbnail)) {
            String url = fileStorageService.storeImage(thumbnail, THUMBNAIL_FOLDER);
            newUrls.add(url);
            addIfPresent(oldUrlsToDelete, tour.getThumbnailUrl());
            tour.setThumbnailUrl(url);
        } else if (removeThumbnail) {
            addIfPresent(oldUrlsToDelete, tour.getThumbnailUrl());
            tour.setThumbnailUrl(null);
        }

        // 4. Thư viện ảnh: bỏ ảnh không giữ (orphanRemoval xoá dòng trong DB), sắp lại thứ tự, thêm ảnh mới
        Set<Image> keptSet = Collections.newSetFromMap(new IdentityHashMap<>());
        keptSet.addAll(kept);
        Iterator<Image> it = tour.getImages().iterator();
        while (it.hasNext()) {
            Image img = it.next();
            if (!keptSet.contains(img)) {
                oldUrlsToDelete.add(img.getImageUrl());
                it.remove();
            }
        }
        for (int i = 0; i < kept.size(); i++) {
            kept.get(i).setDisplayOrder(i);
        }
        appendGallery(tour, newFiles, kept.size(), newUrls);

        // 5. Lịch trình: null → giữ nguyên; có danh sách → xoá hết rồi tạo lại.
        //    flush() để lệnh DELETE chạy TRƯỚC INSERT: Hibernate mặc định INSERT trước,
        //    sẽ vi phạm UNIQUE (tour_id, day_number) khi ngày 1 cũ chưa bị xoá.
        if (request.getItineraries() != null) {
            tour.getItineraries().clear();
            tourRepository.flush();
            addItineraries(tour, request.getItineraries());
        }

        Tour saved = tourRepository.save(tour);
        return toTourResponse(saved);
    }

    // =====================================================================
    // XOÁ CỨNG (chỉ khi chưa có booking)
    // =====================================================================
    @Override
    @Transactional
    public void deleteTour(int tourId) {
        Tour tour = findTour(tourId);

        // Có booking (kể cả đã huỷ) → không xoá: đơn hàng, thanh toán, đánh giá cần dữ liệu tour
        if (bookingRepository.existsBySchedule_Tour_TourId(tourId)) {
            throw new AppException(ErrorCode.TOUR_HAS_BOOKINGS);
        }

        // Gom file ảnh để xoá SAU KHI commit
        List<String> urls = new ArrayList<>();
        addIfPresent(urls, tour.getThumbnailUrl());
        tour.getImages().forEach(img -> urls.add(img.getImageUrl()));

        // Lịch khởi hành không cascade từ Tour (cố ý) → xoá tay. Chưa có booking nên xoá được.
        scheduleRepository.deleteAll(tour.getSchedules());

        // Ảnh thư viện + lịch trình: cascade ALL → tự xoá theo tour
        tourRepository.delete(tour);

        fileStorageService.cleanupAfterTransaction(urls, List.of());
    }

    // =====================================================================
    // ẨN / HIỆN
    // =====================================================================
    @Override
    @Transactional
    public TourResponse changeStatus(int tourId, boolean active) {
        Tour tour = findTour(tourId);
        tour.setIsActive(active);
        return toTourResponse(tourRepository.save(tour));
    }

    // =====================================================================
    // ĐỌC
    // =====================================================================
    @Override
    @Transactional(readOnly = true)
    public Page<TourAdminRowResponse> getAllToursForAdmin(TourFilterRequest filter, Pageable pageable) {
        return tourRepository.findAll(TourSpecification.adminFilter(filter), pageable)
                .map(this::toAdminRowResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TourResponse getTourByIdForAdmin(int tourId) {
        return toTourResponse(findTour(tourId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TourCardResponse> getAllTours(TourFilterRequest filter, Pageable pageable) {
        Page<Tour> page = tourRepository.findAll(TourSpecification.publicFilter(filter), pageable);

        // 1 câu truy vấn cho ngày khởi hành của CẢ TRANG, rồi chia theo tourId
        List<Integer> tourIds = page.getContent().stream().map(Tour::getTourId).toList();
        Map<Integer, List<DepartureDateResponse>> datesByTour = tourIds.isEmpty()
                ? Map.of()
                : scheduleRepository.findUpcomingByTourIds(tourIds, LocalDate.now()).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getTour().getTourId(),
                        Collectors.mapping(s -> DepartureDateResponse.builder()
                                .scheduleId(s.getScheduleId())
                                .departureDate(s.getDepartureDate())
                                .status(s.getStatus())
                                .build(), Collectors.toList())));

        return page.map(t -> toTourCardResponse(t,
                datesByTour.getOrDefault(t.getTourId(), List.of())));
    }

    @Override
    @Transactional   // KHÔNG readOnly: có câu UPDATE tăng lượt xem
    public TourResponse getTourById(int tourId) {
        Tour tour = tourRepository.findPublicById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
        tourRepository.incrementViewCount(tourId);

        TourResponse response = toTourResponse(tour);
        response.setViewCount(tour.getViewCount() + 1);   // entity đã nạp trước khi UPDATE
        return response;
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================
    private Tour findTour(int tourId) {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_NOT_FOUND));
    }

    private Category findCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Location findLocation(Integer locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
    }

    /** Các field thường, dùng chung cho thêm và sửa. viewCount / rating / totalReviews do hệ thống quản lý */
    private void applyFields(Tour tour, TourRequest r, Category category, Location location) {
        tour.setTourName(r.getTourName().trim());
        tour.setCategory(category);
        tour.setLocation(location);
        tour.setTourClass(r.getTourClass());
        tour.setDepartureLocation(r.getDepartureLocation().trim());
        tour.setDuration(r.getDuration().trim());
        tour.setBasePrice(r.getBasePrice());
        tour.setDescription(sanitize(r.getDescription()));
        tour.setIsFeatured(r.getIsFeatured());
        tour.setIsActive(r.getIsActive());
    }

    private void addItineraries(Tour tour, List<ItineraryRequest> items) {
        if (items == null) return;
        for (int i = 0; i < items.size(); i++) {
            ItineraryRequest item = items.get(i);
            tour.getItineraries().add(Itinerary.builder()
                    .tour(tour)
                    .dayNumber(i + 1)                       // ngày do server đánh số → không thể trùng
                    .title(item.getTitle().trim())
                    .description(sanitize(item.getDescription()))
                    .build());
        }
    }

    private void appendGallery(Tour tour, List<MultipartFile> files, int startOrder, List<String> newUrls) {
        for (int i = 0; i < files.size(); i++) {
            String url = fileStorageService.storeImage(files.get(i), GALLERY_FOLDER);
            newUrls.add(url);
            tour.getImages().add(Image.builder()
                    .tour(tour)
                    .imageUrl(url)
                    .displayOrder(startOrder + i)
                    .build());
        }
    }

    /** Ảnh cũ được giữ, theo thứ tự trong keepImageIds. Id không thuộc tour này bị bỏ qua. */
    private List<Image> resolveKeptImages(Tour tour, List<Integer> keepImageIds) {
        List<Image> current = new ArrayList<>(tour.getImages());
        if (keepImageIds == null) {
            current.sort(Comparator.comparing(Image::getDisplayOrder, Comparator.nullsLast(Integer::compare)));
            return current;
        }
        Map<Integer, Image> byId = new HashMap<>();
        current.forEach(img -> byId.put(img.getImageId(), img));

        List<Image> kept = new ArrayList<>();
        for (Integer id : new LinkedHashSet<>(keepImageIds)) {   // bỏ id gửi trùng
            Image img = byId.get(id);
            if (img != null) kept.add(img);
        }
        return kept;
    }

    private static boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
        if (files == null) return List.of();
        return files.stream().filter(TourServiceImpl::hasFile).toList();
    }

    private static void addIfPresent(List<String> list, String url) {
        if (url != null) list.add(url);
    }

    private static String sanitize(String html) {
        return html == null ? null : Jsoup.clean(html, DESCRIPTION_SAFELIST);
    }

    private TourAdminRowResponse toAdminRowResponse(Tour t) {
        return TourAdminRowResponse.builder()
                .tourId(t.getTourId())
                .thumbnailUrl(t.getThumbnailUrl())
                .tourCode(t.getTourCode())
                .tourName(t.getTourName())
                .categoryName(t.getCategory().getCategoryName())
                .locationName(t.getLocation().getLocationName())
                .basePrice(t.getBasePrice())
                .isActive(t.getIsActive())
                .build();
    }

    private TourCardResponse toTourCardResponse(Tour t, List<DepartureDateResponse> dates) {
        return TourCardResponse.builder()
                .tourId(t.getTourId())
                .thumbnailUrl(t.getThumbnailUrl())
                .tourClass(t.getTourClass())
                .tourName(t.getTourName())
                .departureLocation(t.getDepartureLocation())
                .duration(t.getDuration())
                .basePrice(t.getBasePrice())
                .departureDates(dates)
                .build();
    }

    private TourResponse toTourResponse(Tour t) {
        List<ImageResponse> images = t.getImages().stream()
                .sorted(Comparator.comparing(Image::getDisplayOrder, Comparator.nullsLast(Integer::compare)))
                .map(img -> ImageResponse.builder()
                        .imageId(img.getImageId())
                        .imageUrl(img.getImageUrl())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .toList();

        List<ItineraryResponse> itineraries = t.getItineraries().stream()
                .sorted(Comparator.comparing(Itinerary::getDayNumber))
                .map(iti -> ItineraryResponse.builder()
                        .itineraryId(iti.getItineraryId())
                        .dayNumber(iti.getDayNumber())
                        .title(iti.getTitle())
                        .description(iti.getDescription())
                        .build())
                .toList();

        return TourResponse.builder()
                .tourId(t.getTourId())
                .tourCode(t.getTourCode())
                .tourName(t.getTourName())
                .categoryId(t.getCategory().getCategoryId())
                .categoryName(t.getCategory().getCategoryName())
                .locationId(t.getLocation().getLocationId())
                .locationName(t.getLocation().getLocationName())
                .tourClass(t.getTourClass())
                .departureLocation(t.getDepartureLocation())
                .duration(t.getDuration())
                .basePrice(t.getBasePrice())
                .description(t.getDescription())
                .thumbnailUrl(t.getThumbnailUrl())
                .images(images)
                .itineraries(itineraries)
                .isFeatured(t.getIsFeatured())
                .isActive(t.getIsActive())
                .viewCount(t.getViewCount())
                .averageRating(t.getAverageRating())
                .totalReviews(t.getTotalReviews())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}