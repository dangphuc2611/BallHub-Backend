package com.ballhub.ballhub_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    @Column(name = "PromotionID")
    private Integer promotionId;

    @Column(name = "PromotionName", length = 150)
    private String promotionName;

    @Column(name = "PromoCode", length = 50, unique = true)
    private String promoCode;

    // ✅ BỔ SUNG TRƯỜNG NÀY ĐỂ HẾT LỖI ĐỎ TRONG SERVICE
    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "DiscountPercent")
    private Integer discountPercent;

    @Column(name = "DiscountType", length = 20)
    private String discountType;

    @Column(name = "MinOrderAmount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "MaxDiscountAmount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "UsageLimit")
    private Integer usageLimit;

    @Column(name = "UsedCount")
    private Integer usedCount;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Column(name = "Status")
    private Boolean status; // Lưu ý: Entity dùng status

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VariantPromotion> variantPromotions;

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(this.status) &&
                (this.startDate == null || !now.isBefore(this.startDate)) &&
                (this.endDate == null || !now.isAfter(this.endDate)) &&
                (this.usageLimit == null || this.usageLimit == 0 || (this.usedCount != null && this.usedCount < this.usageLimit));
    }
}