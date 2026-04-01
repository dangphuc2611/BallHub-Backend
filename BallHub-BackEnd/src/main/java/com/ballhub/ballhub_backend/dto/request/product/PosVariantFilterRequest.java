package com.ballhub.ballhub_backend.dto.request.product;

import lombok.Data;

@Data
public class PosVariantFilterRequest {
    private String keyword; // Tìm theo Tên sản phẩm hoặc Mã SKU
    private Integer categoryId;
    private Integer brandId;
    private Integer colorId;
    private Integer sizeId;
}