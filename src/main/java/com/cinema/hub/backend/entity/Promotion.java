package com.cinema.hub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import com.cinema.hub.backend.util.TimeProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PromotionId")
    private Long id;

    @Column(name = "Slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "Title", nullable = false, length = 300)
    private String title;

    @Column(name = "ThumbnailUrl", length = 500)
    private String thumbnailUrl;

    @Lob
    @Column(name = "Content", nullable = false)
    private String content;

    @Column(name = "ImgContentUrl", length = 500)
    private String imgContentUrl;

    @Column(name = "PublishedDate")
    private LocalDate publishedDate;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UpdatedAt", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = TimeProvider.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isActive == null) {
            this.isActive = Boolean.TRUE;
        }
        if (this.publishedDate == null) {
            this.publishedDate = LocalDate.now(TimeProvider.VN_ZONE_ID);
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = TimeProvider.now();
        if (this.isActive == null) {
            this.isActive = Boolean.TRUE;
        }
        if (this.publishedDate == null) {
            this.publishedDate = LocalDate.now(TimeProvider.VN_ZONE_ID);
        }
    }
}
