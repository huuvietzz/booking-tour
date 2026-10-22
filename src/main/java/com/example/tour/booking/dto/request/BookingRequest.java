//package com.example.tour.booking.dto.request;
//
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotEmpty;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.util.List;
//
//@Getter
//@Setter
//public class BookingRequest {
//
//    @NotNull(message = "Vui lòng chọn lịch khởi hành")
//    private Integer scheduleId;
//
//    @Min(value = 1, message = "Đoàn phải có ít nhất 1 người lớn")
//    private int totalAdults = 1;
//
//    @Min(value = 0, message = "Số trẻ em không được âm")
//    private int totalChildren = 0;
//
//    @Min(value = 0, message = "Số em bé không được âm")
//    private int totalInfants = 0;
//
//    private String specialRequests;
//
//    @NotEmpty(message = "Vui lòng khai báo thông tin hành khách")
//    @Valid
//    private List<BookingParticipantRequest> participants;
//}