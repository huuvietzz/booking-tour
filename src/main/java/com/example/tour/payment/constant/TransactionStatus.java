package com.example.tour.payment.constant;

public enum TransactionStatus {
    PENDING,   // VNPAY: đã tạo giao dịch, đang chờ kết quả từ cổng
    SUCCESS,   // tiền đã vào, đơn đã xác nhận
    FAILED     // VNPAY: khách huỷ / thanh toán lỗi
}
