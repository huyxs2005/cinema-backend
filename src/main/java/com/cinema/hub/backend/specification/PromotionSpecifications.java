package com.cinema.hub.backend.specification;

import com.cinema.hub.backend.entity.Promotion;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PromotionSpecifications {

    private PromotionSpecifications() {
    }

    public static Specification<Promotion> hasKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        final String normalized = keyword.trim().toLowerCase();
        return (root, query, cb) -> {
            String pattern = "%" + normalized + "%";
            // SQL Server cannot apply LOWER() directly on NVARCHAR(MAX)/CLOB columns, so we
            // restrict keyword searching to the slug and title which are standard string types.
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("slug")), pattern));
        };
    }

    public static Specification<Promotion> hasStatus(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("isActive"), active);
    }

    public static Specification<Promotion> publishedFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("publishedDate"), from);
    }

    public static Specification<Promotion> publishedTo(LocalDate to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("publishedDate"), to);
    }

    public static Specification<Promotion> publishedUpTo(LocalDate date) {
        if (date == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("publishedDate"), date);
    }
}
