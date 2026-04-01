package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.payment.PaymentMethodResponse;
import com.ballhub.ballhub_backend.dto.request.payment.CreatePaymentMethodRequest;
import com.ballhub.ballhub_backend.dto.request.payment.UpdatePaymentMethodRequest;
import com.ballhub.ballhub_backend.service.PaymentMethodService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PaymentMethodController {

    @Autowired
    private PaymentMethodService paymentMethodService;

    // PUBLIC - Get all active payment methods
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getAllPaymentMethods() {
        List<PaymentMethodResponse> paymentMethods = paymentMethodService.getAllPaymentMethods();
        return ResponseEntity.ok(ApiResponse.success(paymentMethods));
    }

    // PUBLIC - Get payment method by ID
    @GetMapping("/payment-methods/{id}")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> getPaymentMethodById(@PathVariable Integer id) {
        PaymentMethodResponse paymentMethod = paymentMethodService.getPaymentMethodById(id);
        return ResponseEntity.ok(ApiResponse.success(paymentMethod));
    }

    // ADMIN - Create payment method
    @PostMapping("/admin/payment-methods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> createPaymentMethod(
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        PaymentMethodResponse paymentMethod = paymentMethodService.createPaymentMethod(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phương thức thanh toán thành công", paymentMethod));
    }

    // ADMIN - Update payment method
    @PutMapping("/admin/payment-methods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentMethodResponse>> updatePaymentMethod(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePaymentMethodRequest request) {
        PaymentMethodResponse paymentMethod = paymentMethodService.updatePaymentMethod(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phương thức thanh toán thành công", paymentMethod));
    }

    // ADMIN - Delete payment method (soft delete)
    @DeleteMapping("/admin/payment-methods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deletePaymentMethod(@PathVariable Integer id) {
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phương thức thanh toán thành công", null));
    }
}
