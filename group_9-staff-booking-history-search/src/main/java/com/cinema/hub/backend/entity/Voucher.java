package com.cinema.hub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VoucherId")
    private Integer id;

    @Column(name = "Code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "DiscountType", nullable = false, length = 1)
    private String discountType;

    @Column(name = "DiscountValue", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "MaxDiscount", precision = 18, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "MinOrderAmount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "ValidFrom", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "ValidTo", nullable = false)
    private OffsetDateTime validTo;

    @Column(name = "MaxUsageCount")
    private Integer maxUsageCount;

    @Column(name = "PerUserLimit")
    private Integer perUserLimit;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "voucher")
    @Builder.Default
    private Set<Booking> bookings = new LinkedHashSet<>();
}
