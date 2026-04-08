package com.ballhub.ballhub_backend.dto.response.order;

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
public class OrderResponse {
    private Integer orderId;
    private Integer userId;

    private String userFullName;

    private String fullName;
    private String phone;

    private String statusName;
    private LocalDateTime orderDate;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private String deliveryAddress;
    private BigDecimal totalAmount;

    // ✅ THÊM VÀO LUÔN CHO ĐỒNG BỘ
    private BigDecimal customerCash;
    private BigDecimal changeAmount;

    private Integer totalItems;
    private String paymentMethodName;
}