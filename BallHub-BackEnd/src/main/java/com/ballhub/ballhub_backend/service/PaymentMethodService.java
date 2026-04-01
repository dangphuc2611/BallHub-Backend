package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.payment.PaymentMethodResponse;
import com.ballhub.ballhub_backend.dto.request.payment.CreatePaymentMethodRequest;
import com.ballhub.ballhub_backend.dto.request.payment.UpdatePaymentMethodRequest;
import com.ballhub.ballhub_backend.entity.PaymentMethod;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.PaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentMethodService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> getAllPaymentMethods() {
        return paymentMethodRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentMethodResponse getPaymentMethodById(Integer id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByPaymentMethodIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không tồn tại"));
        return mapToResponse(paymentMethod);
    }

    public PaymentMethodResponse createPaymentMethod(CreatePaymentMethodRequest request) {
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .methodName(request.getMethodName())
                .isActive(request.getIsActive())
                .build();

        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        return mapToResponse(saved);
    }

    public PaymentMethodResponse updatePaymentMethod(Integer id, UpdatePaymentMethodRequest request) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không tồn tại"));

        paymentMethod.setMethodName(request.getMethodName());
        if (request.getIsActive() != null) {
            paymentMethod.setIsActive(request.getIsActive());
        }

        PaymentMethod updated = paymentMethodRepository.save(paymentMethod);
        return mapToResponse(updated);
    }

    public void deletePaymentMethod(Integer id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không tồn tại"));

        // Soft delete
        paymentMethod.setIsActive(false);
        paymentMethodRepository.save(paymentMethod);
    }

    private PaymentMethodResponse mapToResponse(PaymentMethod paymentMethod) {
        return PaymentMethodResponse.builder()
                .paymentMethodId(paymentMethod.getPaymentMethodId())
                .methodName(paymentMethod.getMethodName())
                .isActive(paymentMethod.getIsActive())
                .build();
    }
}
