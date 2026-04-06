package com.ballhub.ballhub_backend.dto.response.product;

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
public class ProductResponse {

    private Integer productId;
    private String productName;
    private String description;
    private Integer categoryId;
    private String categoryName;
    private Integer brandId;
    private String brandName;
    private String materialName;
    private String styleName;
    private String mainImage;

    // --- BỔ SUNG TRƯỜNG CHO FLASH SALE ---
    private BigDecimal minOriginalPrice; // Giá gốc thấp nhất (để hiển thị gạch ngang)
    private BigDecimal maxOriginalPrice; // Giá gốc cao nhất
    private Integer discountPercent; // Phần trăm giảm giá (để hiện nhãn -10%)
    // -------------------------------------

    private BigDecimal minPrice; // Giá bán thực tế (đã trừ Flash Sale)
    private BigDecimal maxPrice;
    private Boolean status;
    private LocalDateTime createdAt;
}