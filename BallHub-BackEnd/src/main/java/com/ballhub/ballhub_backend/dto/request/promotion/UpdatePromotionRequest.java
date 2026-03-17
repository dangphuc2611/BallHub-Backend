package com.ballhub.ballhub_backend.dto.request.promotion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePromotionRequest {

    private String promotionName;

    private String promoCode;

    private Integer discountPercent;

    private String discountType; // "PERCENT" hoặc "FIXED"

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscountAmount;

    private Integer usageLimit;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean status;
}
