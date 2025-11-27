package com.cinema.hub.backend.specification;

import com.cinema.hub.backend.entity.Showtime;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ShowtimeSpecifications {

    private ShowtimeSpecifications() {
    }

    public static Specification<Showtime> filter(Integer movieId,
                                                 Integer auditoriumId,
                                                 Boolean active,
                                                 LocalDate fromDate,
                                                 LocalDate toDate) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (movieId != null) {
                predicates.add(builder.equal(root.get("movie").get("id"), movieId));
            }
            if (auditoriumId != null) {
                predicates.add(builder.equal(root.get("auditorium").get("id"), auditoriumId));
            }
            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }
            if (fromDate != null) {
                LocalDateTime start = fromDate.atStartOfDay();
                predicates.add(builder.greaterThanOrEqualTo(root.get("startTime"), start));
            }
            if (toDate != null) {
                LocalDateTime end = toDate.atTime(LocalTime.MAX);
                predicates.add(builder.lessThanOrEqualTo(root.get("startTime"), end));
            }
            return predicates.isEmpty()
                    ? builder.conjunction()
                    : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
