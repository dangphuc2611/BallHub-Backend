package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.promotion.PromotionResponse;
import com.ballhub.ballhub_backend.dto.request.promotion.PromotionRequest;
import com.ballhub.ballhub_backend.repository.PromotionRepository;
import com.ballhub.ballhub_backend.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private PromotionRepository promotionRepository;

    // ─── PUBLIC: Kiểm tra voucher cụ thể ───
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<PromotionResponse>> checkVoucher(@RequestParam String code) {
        PromotionResponse result = promotionService.checkAndApplyVoucher(code);
        return ResponseEntity.ok(ApiResponse.success("Mã giảm giá hợp lệ", result));
    }

    // ─── ADMIN: Lấy tất cả voucher (có phân trang) ───
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PromotionResponse> result = promotionService.getAllPromotions(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─── ADMIN: Tạo mới ───
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @RequestBody PromotionRequest request) {
        PromotionResponse created = promotionService.createPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chương trình thành công", created));
    }

    // ─── ADMIN: Cập nhật ───
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            @PathVariable Integer id,
            @RequestBody PromotionRequest request) {
        PromotionResponse updated = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", updated));
    }

    // ─── ADMIN: Vô hiệu hóa ───
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Integer id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa mã giảm giá", null));
    }

    // ─── ADMIN: Bật/Tắt nhanh ───
    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromotionResponse>> togglePromotion(@PathVariable Integer id) {
        PromotionResponse result = promotionService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("Đã thay đổi trạng thái", result));
    }

    // Lấy danh sách đang hoạt động
    @GetMapping({"/active", "/vouchers/valid"})
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActivePromotions() {
        List<PromotionResponse> promotions = promotionService.getAllActivePromotions();
        return ResponseEntity.ok(ApiResponse.success(promotions));
    }

    // ─── PHỤC VỤ HIỂN THỊ: Lấy tên sản phẩm áp dụng ───
    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<List<String>>> getAppliedProductNames(@PathVariable Integer id) {
        List<String> productNames = promotionRepository.findProductNamesByPromotionId(id);
        return ResponseEntity.ok(ApiResponse.success(productNames));
    }

    // ─── QUAN TRỌNG: Lấy danh sách ID sản phẩm để Frontend tích Checkbox khi Edit ───
    @GetMapping("/{id}/products/ids")
    public ResponseEntity<ApiResponse<List<Integer>>> getAppliedProductIds(@PathVariable Integer id) {
        // Bạn cần thêm hàm getAppliedProductIds trong PromotionService
        List<Integer> productIds = promotionService.getAppliedProductIds(id);
        return ResponseEntity.ok(ApiResponse.success(productIds));
    }
}