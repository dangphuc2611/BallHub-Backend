package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.dto.response.promotion.PromotionResponse;
import com.ballhub.ballhub_backend.dto.request.promotion.CreatePromotionRequest;
import com.ballhub.ballhub_backend.dto.request.promotion.UpdatePromotionRequest;
import com.ballhub.ballhub_backend.entity.Promotion;
import com.ballhub.ballhub_backend.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    // ─── PUBLIC: Lấy tất cả voucher hợp lệ (dùng ở trang Checkout) ───────────
    public List<PromotionResponse> getValidVouchers() {
        return promotionRepository.findValidVouchers().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── ADMIN: Lấy tất cả voucher có phân trang ─────────────────────────────
    public PageResponse<PromotionResponse> getAllVouchers(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Promotion> pageResult = promotionRepository.findAllVouchers(pageRequest);
        Page<PromotionResponse> responsePage = pageResult.map(this::mapToResponse);
        return PageResponse.of(responsePage);
    }

    // ─── ADMIN: Lấy chi tiết một voucher ─────────────────────────────────────
    public PromotionResponse getVoucherById(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + id));
        return mapToResponse(promotion);
    }

    // ─── ADMIN: Tạo mới voucher ───────────────────────────────────────────────
    public PromotionResponse createVoucher(CreatePromotionRequest request) {
        // Kiểm tra promoCode trùng
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            if (promotionRepository.existsByPromoCode(request.getPromoCode().trim().toUpperCase())) {
                throw new RuntimeException("Mã voucher đã tồn tại: " + request.getPromoCode());
            }
        }

        // Đảm bảo giá trị hợp lệ cho DiscountPercent để tránh lỗi CHECK constraint
        Integer discountPercent = request.getDiscountPercent();
        if ("FIXED".equals(request.getDiscountType())) {
            discountPercent = 0; // Nếu giảm tiền mặt thì % = 0
        } else if (discountPercent == null || discountPercent < 0) {
            discountPercent = 0;
        } else if (discountPercent > 100) {
            discountPercent = 100;
        }

        Promotion promotion = Promotion.builder()
                .promotionName(request.getPromotionName())
                .promoCode(request.getPromoCode() != null ? request.getPromoCode().trim().toUpperCase() : null)
                .discountPercent(discountPercent)
                .discountType(request.getDiscountType())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : true)
                .build();

        Promotion saved = promotionRepository.save(promotion);
        return mapToResponse(saved);
    }

    // ─── ADMIN: Cập nhật voucher ──────────────────────────────────────────────
    public PromotionResponse updateVoucher(Integer id, UpdatePromotionRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + id));

        // Kiểm tra promoCode trùng (ngoại trừ bản ghi đang sửa)
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            String newCode = request.getPromoCode().trim().toUpperCase();
            if (promotionRepository.existsByPromoCodeAndPromotionIdNot(newCode, id)) {
                throw new RuntimeException("Mã voucher đã tồn tại: " + newCode);
            }
            promotion.setPromoCode(newCode);
        }

        if (request.getPromotionName() != null)   promotion.setPromotionName(request.getPromotionName());
        
        // Xử lý DiscountPercent khi cập nhật
        if (request.getDiscountType() != null) {
            promotion.setDiscountType(request.getDiscountType());
            if ("FIXED".equals(request.getDiscountType())) {
                promotion.setDiscountPercent(0);
            }
        }
        
        if (request.getDiscountPercent() != null && !"FIXED".equals(promotion.getDiscountType())) {
            int val = request.getDiscountPercent();
            promotion.setDiscountPercent(Math.max(0, Math.min(100, val)));
        }

        if (request.getMinOrderAmount() != null)   promotion.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getMaxDiscountAmount() != null) promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getUsageLimit() != null)       promotion.setUsageLimit(request.getUsageLimit());
        if (request.getStartDate() != null)        promotion.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)          promotion.setEndDate(request.getEndDate());
        if (request.getStatus() != null)           promotion.setStatus(request.getStatus());

        Promotion saved = promotionRepository.save(promotion);
        return mapToResponse(saved);
    }

    // ─── ADMIN: Xóa voucher ───────────────────────────────────────────────────
    public void deleteVoucher(Integer id) {
        if (!promotionRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy voucher với id: " + id);
        }
        promotionRepository.deleteById(id);
    }

    // ─── Dùng cho POS: Lấy danh sách khuyến mãi đang hoạt động ──────────────
    public List<PromotionResponse> getAllActivePromotions() {
        // Tận dụng luôn hàm findValidVouchers đã có trong Repository
        return promotionRepository.findValidVouchers().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── Helper: map entity → DTO ─────────────────────────────────────────────
    private PromotionResponse mapToResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .promotionId(promotion.getPromotionId())
                .promotionName(promotion.getPromotionName())
                .promoCode(promotion.getPromoCode())
                .discountPercent(promotion.getDiscountPercent())
                .discountType(promotion.getDiscountType())
                .minOrderAmount(promotion.getMinOrderAmount())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .usageLimit(promotion.getUsageLimit())
                .usedCount(promotion.getUsedCount())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .status(promotion.getStatus())
                .valid(promotion.isValid())
                .build();
    }
}