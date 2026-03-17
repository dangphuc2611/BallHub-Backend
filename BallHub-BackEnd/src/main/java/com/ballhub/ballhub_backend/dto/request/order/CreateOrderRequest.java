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

    // ✅ XÓA @NotNull: Vì đơn hàng tại quầy (POS) sẽ KHÔNG CÓ địa chỉ
    private Integer addressId;

    @NotNull(message = "Payment method ID không được để trống")
    private Integer paymentMethodId;

    private String note;

    private String promoCode;

    private BigDecimal shippingFee;

    // ✅ BỔ SUNG: Cờ đánh dấu đây là đơn hàng bán tại quầy
    private Boolean isPos;
}