package com.example.tour.common.constant;

/** Tên các vùng cache. Key trong Redis có dạng "viettrip::<tên>::<key>", vd viettrip::tourDetail::5 */
public final class CacheNames {
    private CacheNames() {}

    public static final String TOUR_DETAIL = "tourDetail";   // chi tiết 1 tour phía khách (không gồm lượt xem, lịch, số chỗ)
    public static final String LOCATIONS   = "locations";    // điểm đến đang hiển thị
    public static final String CATEGORIES  = "categories";   // danh mục đang hiển thị
}
