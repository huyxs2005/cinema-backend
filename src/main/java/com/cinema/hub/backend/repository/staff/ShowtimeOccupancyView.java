package com.cinema.hub.backend.repository.staff;

public record ShowtimeOccupancyView(
        Integer showtimeId,
        long totalSeats,
        long sellableSeats,
        long soldSeats,
        long heldSeats) {
}
