package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.dto.staff.dashboard.StaffBookingSummaryDto;
import com.cinema.hub.backend.dto.staff.dashboard.StaffShowtimeSummaryDto;
import java.util.List;

public interface StaffDashboardService {

    List<StaffShowtimeSummaryDto> getUpcomingShowtimes(int daysAhead);

    List<StaffBookingSummaryDto> getTodayBookings();

    List<StaffBookingSummaryDto> searchBookings(String keyword);
}
