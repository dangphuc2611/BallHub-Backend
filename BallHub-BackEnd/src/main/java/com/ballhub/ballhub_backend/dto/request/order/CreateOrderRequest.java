package com.ballhub.ballhub_backend.dto.request.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private Integer customerId;

    private String deliveryAddress;

    private Integer addressId;

    @NotNull(message = "Payment method ID không được để trống")
    private Integer paymentMethodId;

    private String note;

    private String promoCode;

    private BigDecimal shippingFee;

    private Boolean isPos;

    private String fullName;
    private String phone;

    // ✅ BỔ SUNG ĐỂ HỨNG TIỀN TỪ FRONTEND
    private BigDecimal customerCash;
    private BigDecimal changeAmount;
}