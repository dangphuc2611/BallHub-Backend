package com.ballhub.ballhub_backend.dto.request.promotion;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data // Tự động tạo Getter/Setter
public class PromotionRequest {
    private String promotionName;
    private String promoCode;
    private String description;

    private String discountType; // "PERCENT" hoặc "FIXED"
    private Integer discountPercent; // Dùng nếu type là PERCENT
    private BigDecimal maxDiscountAmount; // Số tiền giảm tối đa
    private BigDecimal minOrderAmount; // Đơn hàng tối thiểu để áp dụng

    private Integer usageLimit; // Tổng lượt sử dụng (VD: 100 lượt)

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Boolean status;

    private List<Integer> productIds;
}