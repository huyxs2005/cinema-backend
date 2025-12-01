package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeGroupedResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeRequest;
import com.cinema.hub.backend.dto.showtime.ShowtimeResponse;
import com.cinema.hub.backend.web.view.SeatSelectionShowtimeView;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ShowtimeService {

    List<ShowtimeResponse> create(ShowtimeRequest request);

    ShowtimeResponse update(int id, ShowtimeRequest request);

    ShowtimeResponse get(int id);

    void delete(int id);

    ShowtimeResponse updateActiveStatus(int id, boolean active);

    PageResponse<ShowtimeResponse> search(Integer movieId,
                                          Integer auditoriumId,
                                          Boolean active,
                                          LocalDate fromDate,
                                          LocalDate toDate,
                                          String keyword,
                                          Pageable pageable);

    List<ShowtimeResponse> getUpcomingShowtimesForMovie(int movieId, LocalDate fromDate, LocalDate toDate);

    List<ShowtimeResponse> getShowtimesByDate(LocalDate date);

    List<ShowtimeGroupedResponse> groupByMovie(Integer movieId,
                                               Integer auditoriumId,
                                               Boolean active,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               String keyword);

    SeatSelectionShowtimeView getSeatSelectionDetails(int showtimeId);
}
