package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.request.promotion.PromotionRequest;
import com.ballhub.ballhub_backend.dto.response.promotion.PromotionResponse;
import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.entity.Promotion;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.entity.VariantPromotion;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.ProductVariantRepository;
import com.ballhub.ballhub_backend.repository.PromotionRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
import com.ballhub.ballhub_backend.repository.VariantPromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private VariantPromotionRepository variantPromotionRepository;

    // ============================================
    // ADMIN: TẠO KHUYẾN MÃI / VOUCHER
    // ============================================
    public PromotionResponse createPromotion(PromotionRequest request) {
        // 1. Kiểm tra trùng mã code (nếu có nhập code)
        if (request.getPromoCode() != null && !request.getPromoCode().isEmpty()
                && promotionRepository.existsByPromoCode(request.getPromoCode())) {
            throw new BadRequestException("Mã giảm giá '" + request.getPromoCode() + "' đã tồn tại!");
        }

        // 2. Validate ngày tháng
        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc không được trước ngày bắt đầu!");
        }

        // 3. Xây dựng đối tượng Promotion
        Promotion promotion = Promotion.builder()
                .promotionName(request.getPromotionName())
                .promoCode(request.getPromoCode() != null ? request.getPromoCode().toUpperCase() : null)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountPercent(request.getDiscountPercent())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderAmount(request.getMinOrderAmount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(true)
                .build();

        // 4. Lưu Khuyến mãi vào Database trước
        Promotion savedPromotion = promotionRepository.save(promotion);

        // 🚀 5. LOGIC MỚI: ÁP DỤNG CHO SẢN PHẨM ĐƯỢC CHỌN
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            applyPromotionToProducts(savedPromotion, request.getProductIds());
        }

        // 6. GỬI EMAIL MARKETING (Hệ thống chạy ngầm)
        if (savedPromotion.getPromoCode() != null && Boolean.TRUE.equals(savedPromotion.getStatus())) {
            sendMarketingEmail(savedPromotion);
        }

        return mapToResponse(savedPromotion);
    }

    // ============================================
    // ADMIN: CẬP NHẬT KHUYẾN MÃI
    // ============================================
    public PromotionResponse updatePromotion(Integer promoId, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        // Kiểm tra trùng mã code nếu có thay đổi
        if (request.getPromoCode() != null &&
                !request.getPromoCode().equalsIgnoreCase(promotion.getPromoCode()) &&
                promotionRepository.existsByPromoCode(request.getPromoCode())) {
            throw new BadRequestException("Mã giảm giá mới đã tồn tại!");
        }

        promotion.setPromotionName(request.getPromotionName());
        promotion.setPromoCode(request.getPromoCode() != null ? request.getPromoCode().toUpperCase() : null);
        promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());

        if (request.getStatus() != null) {
            promotion.setStatus(request.getStatus());
        }

        // 🚀 7. CẬP NHẬT DANH SÁCH SẢN PHẨM ÁP DỤNG
        if (request.getProductIds() != null) {
            // 1. Xóa các liên kết cũ
            variantPromotionRepository.deleteByPromotion(promotion);

            // 2. QUAN TRỌNG: Ép Hibernate thực hiện lệnh xóa ngay xuống DB
            variantPromotionRepository.flush();

            // 3. Sau đó mới tạo liên kết mới
            if (!request.getProductIds().isEmpty()) {
                applyPromotionToProducts(promotion, request.getProductIds());
            }
        }

        return mapToResponse(promotionRepository.save(promotion));
    }

    // --- Hàm hỗ trợ lưu liên kết Sản phẩm (Biến thể) với Khuyến mãi ---
    private void applyPromotionToProducts(Promotion promotion, List<Integer> productIds) {
        // Lấy tất cả các biến thể của các sản phẩm nằm trong danh sách ID
        List<ProductVariant> variants = variantRepository.findByProduct_ProductIdIn(productIds);

        if (variants.isEmpty()) return;

        List<VariantPromotion> vpList = variants.stream().map(variant -> {
            VariantPromotion vp = new VariantPromotion();
            vp.setPromotion(promotion);
            vp.setVariant(variant);
            return vp;
        }).collect(Collectors.toList());

        variantPromotionRepository.saveAll(vpList);
    }

    // --- Hàm hỗ trợ gửi Email ---
    private void sendMarketingEmail(Promotion promotion) {
        List<User> recipients = userRepository.findByRolesIn(List.of("USER", "ADMIN"));
        List<String> userEmails = recipients.stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .collect(Collectors.toList());

        if (!userEmails.isEmpty()) {
            emailService.sendNewVoucherEmail(userEmails, promotion.getPromoCode(), promotion.getDescription());
        }
    }

    public List<Integer> getAppliedProductIds(Integer promotionId) {
        // Truy vấn bảng VariantPromotion để lấy ProductId của các biến thể thuộc khuyến mãi này
        return variantPromotionRepository.findByPromotion_PromotionId(promotionId)
                .stream()
                .map(vp -> vp.getVariant().getProduct().getProductId())
                .distinct()
                .collect(Collectors.toList());
    }

    // ============================================
    // CÁC HÀM QUẢN LÝ TRẠNG THÁI & TRUY VẤN
    // ============================================
    public void deletePromotion(Integer promoId) {
        Promotion promotion = promotionRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
        promotion.setStatus(false); // Vô hiệu hóa thay vì xóa cứng
        promotionRepository.save(promotion);
    }

    public PromotionResponse toggleActive(Integer promoId) {
        Promotion promotion = promotionRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
        promotion.setStatus(!Boolean.TRUE.equals(promotion.getStatus()));
        return mapToResponse(promotionRepository.save(promotion));
    }

    @Transactional(readOnly = true)
    public Page<PromotionResponse> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PromotionResponse checkAndApplyVoucher(String promoCode) {
        Promotion promotion = promotionRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại!"));

        if (!promotion.isValid()) {
            throw new BadRequestException("Mã giảm giá đã hết hạn hoặc hết lượt sử dụng!");
        }

        return mapToResponse(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> getAllActivePromotions() {
        return promotionRepository.findValidVouchers().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // MAPPING: ENTITY -> RESPONSE DTO
    // ============================================
    private PromotionResponse mapToResponse(Promotion p) {
        // Lấy danh sách tên sản phẩm để trả về (Dùng query có sẵn trong Repository của bạn)
        List<String> productNames = promotionRepository.findProductNamesByPromotionId(p.getPromotionId());

        return PromotionResponse.builder()
                .promotionId(p.getPromotionId())
                .promotionName(p.getPromotionName())
                .promoCode(p.getPromoCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountPercent(p.getDiscountPercent())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderAmount(p.getMinOrderAmount())
                .usageLimit(p.getUsageLimit())
                .usedCount(p.getUsedCount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .valid(p.isValid())
                .appliedProductCount(productNames.size()) // Trả về số lượng sản phẩm áp dụng
                .build();
    }
}