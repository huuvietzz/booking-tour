package com.example.tour.tour.dto.request;

import com.example.tour.tour.constant.TourClass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bộ lọc trang tour phía khách (GET /api/public/tours). Trường nào null / rỗng → bỏ qua.
 *
 * Tách khỏi TourFilterRequest (admin) để khách CHỈ gửi được những gì có trong class này:
 * không có isActive, nên không cần "filter.setIsActive(null)" ở controller nữa.
 *
 * Ví dụ: ?departureLocation=Hà Nội&locationIds=3,5&departureDate=2026-11-15
 *         &tourClasses=STANDARD,PREMIUM&minPrice=1000000&maxPrice=5000000&sort=price_asc
 */
@Getter
@Setter
@NoArgsConstructor
public class PublicTourFilterRequest {

    public static final String SORT_DEPARTURE  = "departure";    // ngày khởi hành gần nhất (mặc định)
    public static final String SORT_PRICE_ASC  = "price_asc";
    public static final String SORT_PRICE_DESC = "price_desc";

    private Integer categoryId;

    private String departureLocation;      // điểm khởi hành, khớp chính xác Tour.departureLocation

    private List<Integer> locationIds;     // điểm đến, chọn nhiều

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)   // "2026-11-15"; thiếu dòng này @ModelAttribute không đọc được LocalDate
    private LocalDate departureDate;       // ngày khởi hành (= "Ngày đi" trên thanh tìm kiếm)

    private List<TourClass> tourClasses;   // dòng tour, chọn nhiều (checkbox)

    private BigDecimal minPrice;           // thanh trượt hay ô nhập thì backend vẫn chỉ nhận 2 số này
    private BigDecimal maxPrice;

    private Boolean isFeatured;            // trang chủ: "Tour nổi bật"

    private String sort = SORT_DEPARTURE;
}