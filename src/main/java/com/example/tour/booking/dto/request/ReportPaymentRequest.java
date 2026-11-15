//package com.example.tour.booking.dto.request;
//
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import lombok.Data;
//
///**
// * Khách bấm "Tôi đã thanh toán" (thanh toán giả lập).
// * Khách không đăng nhập → phải gửi đúng email đã đặt để không ai đoán mã đơn rồi báo thay.
// */
//@Data
//public class ReportPaymentRequest {
//
//    @NotBlank(message = "Vui lòng nhập email đã đặt tour")
//    @Email
//    private String contactEmail;
//}
