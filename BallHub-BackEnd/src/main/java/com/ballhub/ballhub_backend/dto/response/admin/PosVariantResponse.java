package com.ballhub.ballhub_backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosVariantResponse {
    private Integer variantId;
    private Integer productId;
    private String productName;
    private String sku;
    private String sizeName;
    private String colorName;
    private String brandName;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private String imageUrl;
}