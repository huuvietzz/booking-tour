package com.example.tour.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

/** "Cần xử lý" — số việc ĐANG tồn tại ngay lúc này (không phụ thuộc nút 7 ngày / 30 ngày / 12 tháng) */
@Getter
@Builder
public class DashboardTodoResponse {
    private long awaitingPayment;     // đơn đang giữ chỗ, khách chưa thanh toán (sẽ tự huỷ khi quá hạn)
    private long toComplete;          // đã thanh toán, tour đã về (ngày về ≤ hôm nay) → admin bấm "Hoàn thành" để gửi mail mời đánh giá
    private long unrepliedReviews;    // đánh giá đang hiển thị chưa có phản hồi của VietTrip
    private long lowUpcoming;         // lịch khởi hành 7 ngày tới còn dưới 50% chỗ
}
