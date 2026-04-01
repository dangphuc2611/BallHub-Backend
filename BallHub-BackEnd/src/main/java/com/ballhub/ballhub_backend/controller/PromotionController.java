package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.dto.response.promotion.PromotionResponse;
import com.ballhub.ballhub_backend.dto.request.promotion.CreatePromotionRequest;
import com.ballhub.ballhub_backend.dto.request.promotion.UpdatePromotionRequest;
import com.ballhub.ballhub_backend.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    // ─── PUBLIC: Lấy danh sách voucher hợp lệ (dùng trang Checkout) ──────────
    @GetMapping("/vouchers/valid")
    public ResponseEntity<List<PromotionResponse>> getValidVouchers() {
        return ResponseEntity.ok(promotionService.getValidVouchers());
    }

    // ─── ADMIN: Lấy tất cả voucher (có phân trang) ───────────────────────────
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PromotionResponse> result = promotionService.getAllVouchers(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─── ADMIN: Lấy chi tiết voucher theo ID ─────────────────────────────────
    @GetMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getVoucherById(@PathVariable Integer id) {
        try {
            PromotionResponse voucher = promotionService.getVoucherById(id);
            return ResponseEntity.ok(ApiResponse.success(voucher));
        } catch (RuntimeException e) {
            ApiResponse<PromotionResponse> err = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
    }

    // ─── ADMIN: Tạo mới voucher ───────────────────────────────────────────────
    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<PromotionResponse>> createVoucher(
            @RequestBody CreatePromotionRequest request) {
        try {
            PromotionResponse created = promotionService.createVoucher(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Tạo voucher thành công", created));
        } catch (RuntimeException e) {
            ApiResponse<PromotionResponse> err = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    // ─── ADMIN: Cập nhật voucher ──────────────────────────────────────────────
    @PutMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> updateVoucher(
            @PathVariable Integer id,
            @RequestBody UpdatePromotionRequest request) {
        try {
            PromotionResponse updated = promotionService.updateVoucher(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật voucher thành công", updated));
        } catch (RuntimeException e) {
            ApiResponse<PromotionResponse> err = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    // ─── ADMIN: Xóa voucher ───────────────────────────────────────────────────
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVoucher(@PathVariable Integer id) {
        try {
            promotionService.deleteVoucher(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa voucher thành công", null));
        } catch (RuntimeException e) {
            ApiResponse<Void> err = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActivePromotions() {
        // Lấy các promotion còn hạn và Status = true
        List<PromotionResponse> promotions = promotionService.getAllActivePromotions();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khuyến mãi thành công", promotions));
    }
}