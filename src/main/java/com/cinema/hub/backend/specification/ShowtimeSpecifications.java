package com.cinema.hub.backend.specification;

import com.cinema.hub.backend.entity.Showtime;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ShowtimeSpecifications {

    private ShowtimeSpecifications() {
    }

    public static Specification<Showtime> filter(Integer movieId,
                                                 Integer auditoriumId,
                                                 Boolean active,
                                                 LocalDate fromDate,
                                                 LocalDate toDate,
                                                 String keyword) {
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
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("movie").get("title")), like),
                        builder.like(builder.lower(root.get("movie").get("originalTitle")), like)
                ));
            }
            return predicates.isEmpty()
                    ? builder.conjunction()
                    : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
