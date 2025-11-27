package com.cinema.hub.backend.mapper;

import com.cinema.hub.backend.dto.showtime.ShowtimeResponse;
import com.cinema.hub.backend.entity.Showtime;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeMapper {

    public ShowtimeResponse toResponse(Showtime showtime) {
        if (showtime == null) {
            return null;
        }
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovie() != null ? showtime.getMovie().getId() : null)
                .movieTitle(showtime.getMovie() != null ? showtime.getMovie().getTitle() : null)
                .auditoriumId(showtime.getAuditorium() != null ? showtime.getAuditorium().getId() : null)
                .auditoriumName(showtime.getAuditorium() != null ? showtime.getAuditorium().getName() : null)
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .active(showtime.getActive())
                .build();
    }
}
