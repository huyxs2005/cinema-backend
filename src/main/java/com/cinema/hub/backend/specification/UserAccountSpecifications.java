package com.cinema.hub.backend.specification;

import com.cinema.hub.backend.entity.UserAccount;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserAccountSpecifications {

    private UserAccountSpecifications() {
    }

    public static Specification<UserAccount> filter(String keyword, String roleName, Boolean active) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.toLowerCase().trim() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("email")), like),
                        builder.like(builder.lower(root.get("fullName")), like),
                        builder.like(builder.lower(root.get("phone")), like)
                ));
            }

            if (StringUtils.hasText(roleName)) {
                var roleJoin = root.join("role", JoinType.LEFT);
                predicates.add(builder.equal(builder.lower(roleJoin.get("name")), roleName.toLowerCase().trim()));
            }

            if (active != null) {
                predicates.add(builder.equal(root.get("active"), active));
            }

            return predicates.isEmpty()
                    ? builder.conjunction()
                    : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
