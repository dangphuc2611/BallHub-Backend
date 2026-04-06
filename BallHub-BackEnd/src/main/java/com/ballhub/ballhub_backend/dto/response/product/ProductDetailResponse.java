package com.ballhub.ballhub_backend.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Integer productId;
    private String productName;
    private String description;

    private Integer categoryId;
    private String categoryName;

    private Integer brandId;
    private String brandName;

    private Integer materialId;
    private String materialName;

    private Integer styleId;
    private String styleName;

    private List<VariantResponse> variants;
    private List<ProductImageResponse> images;

    private List<SizeOptionResponse> sizeOptions;
    private List<ColorOptionResponse> colorOptions;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private ProductContentBlock contentBlock;

    private Boolean status;
    private LocalDateTime createdAt;
}

