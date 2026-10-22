// package com.example.tour.booking.dto.request;

//import com.example.tour.common.constant.Gender;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.time.LocalDate;
//
//@Getter
//@Setter
//public class BookingParticipantRequest {
//
//    @NotBlank(message = "Họ tên hành khách không được để trống")
//    private String fullName;
//
//    private LocalDate dateOfBirth;
//    private Gender gender;
//    private String nationality;
//
//    private IdentityType identityType = IdentityType.CCCD;
//    private String identityNumber;
//    private LocalDate identityExpiryDate; //Khach tour quoc te
//    private String identityIssuePlace;  // Khach tour quoc te
//
//    @NotNull(message = "Vui lòng chọn loại hành khách")
//    private ParticipantType participantType;
//
//    /** Người đại diện đoàn — đúng một người trong danh sách */
//    private boolean primary = false;
//
//    /** Chỉ áp dụng cho người lớn — trẻ em và em bé không được chọn */
//    private boolean singleRoom = false;
//}

