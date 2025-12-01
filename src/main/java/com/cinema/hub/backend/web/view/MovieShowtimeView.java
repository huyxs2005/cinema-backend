package com.cinema.hub.backend.web.view;

import lombok.Builder;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

@Builder
public class MovieShowtimeView {
    private final Integer movieId;
    private final String title;
    private final String synopsis;
    private final String posterUrl;
    private final String rating;
    private final String genre;
    @Singular
    private final List<String> tags;
    @Singular("addSlot")
    private final List<ShowtimeSlotView> showtimes;

    public Integer getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getRating() {
        return rating;
    }

    public String getGenre() {
        return genre;
    }

    public List<String> getTags() {
        return tags != null ? tags : List.of();
    }

    public List<ShowtimeSlotView> getShowtimes() {
        return showtimes != null ? showtimes : new ArrayList<>();
    }
}
