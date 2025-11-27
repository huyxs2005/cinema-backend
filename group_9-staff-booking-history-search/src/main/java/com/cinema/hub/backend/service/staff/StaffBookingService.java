package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.dto.staff.booking.StaffBookingDetailView;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingRequest;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingResult;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingStatusResponse;
import com.cinema.hub.backend.dto.staff.booking.StaffComboOptionDto;
import com.cinema.hub.backend.dto.staff.booking.StaffVietQrInfo;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatMapView;
import com.cinema.hub.backend.entity.UserAccount;
import java.util.List;

public interface StaffBookingService {

    StaffSeatMapView loadSeatMapForShowtime(Integer showtimeId, UserAccount staffUser);

    List<Integer> holdSeats(Integer showtimeId, List<Integer> seatIds, UserAccount staffUser);

    void releaseSeatHolds(Integer showtimeId, List<Integer> seatIds, UserAccount staffUser);

    List<StaffComboOptionDto> getActiveCombos();

    StaffBookingResult createBookingForStaff(StaffBookingRequest request, UserAccount staffUser);

    StaffBookingDetailView getBookingDetail(String bookingCode);

    StaffVietQrInfo getVietQrInfo(String bookingCode);

    void markBookingPaid(String bookingCode, UserAccount staffUser);

    void cancelBooking(String bookingCode, UserAccount staffUser);

    StaffBookingStatusResponse getBookingStatus(String bookingCode);
}
