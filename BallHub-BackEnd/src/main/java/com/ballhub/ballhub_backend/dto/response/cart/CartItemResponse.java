package com.ballhub.ballhub_backend.dto.response.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Integer cartItemId;
    private Integer variantId;
    private String productName;
    private String sizeName;
    private String colorName;
    private BigDecimal price;
    private BigDecimal finalPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;
    private Integer stockQuantity;
}
