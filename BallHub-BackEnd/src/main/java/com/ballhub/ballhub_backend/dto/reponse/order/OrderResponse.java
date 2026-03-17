package com.ballhub.ballhub_backend.dto.reponse.order;

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
<<<<<<< HEAD
    private String userFullName;
=======
    private String fullName;
    private String phone;
>>>>>>> c064b82 (feat: Implement admin review and color management, enhance promotion features with new DTOs and repository methods, and expand order response details.)
    private String statusName;
    private LocalDateTime orderDate;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private String paymentMethodName;
}