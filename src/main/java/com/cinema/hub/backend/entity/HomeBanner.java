package com.cinema.hub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "HomeBanners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BannerId")
    private Integer id;

    @Column(name = "ImagePath", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "LinkType", nullable = false, length = 20)
    private String linkType;

    @Column(name = "MovieId")
    private Integer movieId;

    @Column(name = "PromotionId")
    private Long promotionId;

    @Column(name = "TargetUrl", length = 500)
    private String targetUrl;

    @Column(name = "SortOrder", nullable = false)
    private Integer sortOrder;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;
}
