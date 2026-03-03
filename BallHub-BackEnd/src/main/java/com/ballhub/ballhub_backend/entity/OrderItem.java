package com.ballhub.ballhub_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "OrderItems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderItemID")
    private Integer orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OrderID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "VariantID", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AppliedPromotionID")
    private Promotion appliedPromotion;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "OriginalPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "DiscountPercent")
    @Builder.Default
    private Integer discountPercent = 0;

    @Column(name = "FinalPrice", precision = 18, scale = 2, nullable = false)
    private BigDecimal finalPrice;

    public BigDecimal getSubtotal() {
        if (finalPrice == null) return BigDecimal.ZERO;
        return finalPrice.multiply(BigDecimal.valueOf(quantity));
    }
}