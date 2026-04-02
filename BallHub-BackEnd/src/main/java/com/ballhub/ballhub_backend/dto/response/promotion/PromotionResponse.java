package com.ballhub.ballhub_backend.dto.response.promotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponse {
    private Integer promotionId;
    private String promotionName;
    private String promoCode;
    private Integer discountPercent;
    private String discountType;         // PERCENT hoặc FIXED
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean status;
    private boolean valid;               // Computed: còn hiệu lực hay không
    private String description;
}