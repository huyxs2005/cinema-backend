package com.cinema.hub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Snacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Snack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SnackId")
    private Long id;

    @Column(name = "Slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "Name", nullable = false, length = 150)
    private String name;

    @Column(name = "Category", nullable = false, length = 50)
    private String category;

    @Column(name = "Description", length = 1000)
    private String description;

    @Column(name = "Price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "ImageUrl", length = 500)
    private String imageUrl;

    @Column(name = "ServingSize", length = 50)
    private String servingSize;

    @Column(name = "Calories")
    private Integer calories;

    @Column(name = "StockQuantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "DisplayOrder", nullable = false)
    private Integer displayOrder;

    @Column(name = "IsAvailable", nullable = false)
    private Boolean available;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (displayOrder == null) {
            displayOrder = 1;
        }
        if (available == null) {
            available = Boolean.TRUE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (displayOrder == null) {
            displayOrder = 1;
        }
        if (available == null) {
            available = Boolean.TRUE;
        }
    }
}
