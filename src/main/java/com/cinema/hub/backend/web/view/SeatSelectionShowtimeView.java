package com.cinema.hub.backend.web.view;

import com.cinema.hub.backend.entity.Showtime;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeatSelectionShowtimeView {
    private Integer movieId;
    private Integer id;
    private String movieTitle;
    private String posterUrl;
    private String theaterName;
    private String auditoriumName;
    private String format;
    private LocalDateTime startTime;
    private int maxSeatsPerOrder;
    private int holdDurationSeconds;

    public static SeatSelectionShowtimeView fromEntity(Showtime showtime) {
        return SeatSelectionShowtimeView.builder()
                .movieId(showtime.getMovie().getId())
                .id(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .posterUrl(showtime.getMovie().getPosterUrl())
                .theaterName(showtime.getAuditorium().getName())
                .auditoriumName(showtime.getAuditorium().getName())
                .format("Tiêu chuẩn")
                .startTime(showtime.getStartTime())
                .maxSeatsPerOrder(0)
                .holdDurationSeconds(600)
                .build();
    }
}
