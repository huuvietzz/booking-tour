package com.example.tour.booking.service;

public interface BookingPdfService {

    /** Phiếu xác nhận đặt tour của 1 đơn, dạng PDF (byte[] để controller trả thẳng về FE) */
    byte[] exportBookingPdf(Integer bookingId);
}
