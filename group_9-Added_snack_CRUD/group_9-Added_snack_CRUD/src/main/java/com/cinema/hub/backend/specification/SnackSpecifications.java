package com.cinema.hub.backend.specification;

import com.cinema.hub.backend.entity.Snack;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SnackSpecifications {

    private SnackSpecifications() {
    }

    public static Specification<Snack> hasKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String likeValue = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("name")), likeValue),
                builder.like(builder.lower(root.get("slug")), likeValue),
                builder.like(builder.lower(root.get("description")), likeValue)
        );
    }

    public static Specification<Snack> hasCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        return (root, query, builder) -> builder.equal(builder.upper(root.get("category")), normalized);
    }

    public static Specification<Snack> hasAvailability(Boolean available) {
        if (available == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("available"), available);
    }
}
