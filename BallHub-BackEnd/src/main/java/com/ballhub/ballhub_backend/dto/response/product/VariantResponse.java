package com.ballhub.ballhub_backend.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantResponse {

    private Integer variantId;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer sizeId;
    private String sizeName;
    private Integer colorId;
    private String colorName;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
    private Integer stockQuantity;
    private String sku;
    private Boolean status;
}
