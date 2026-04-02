package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.request.promotion.PromotionRequest;
import com.ballhub.ballhub_backend.dto.response.promotion.PromotionResponse;
import com.ballhub.ballhub_backend.entity.Promotion;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.PromotionRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
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

    // ============================================
    // ADMIN: TẠO VOUCHER & BẮN EMAIL MARKETING
    // ============================================
    public PromotionResponse createPromotion(PromotionRequest request) {
        // 1. Kiểm tra trùng mã code trước khi làm bất cứ việc gì
        if (request.getPromoCode() != null && promotionRepository.existsByPromoCode(request.getPromoCode())) {
            throw new BadRequestException("Mã giảm giá '" + request.getPromoCode() + "' đã tồn tại!");
        }

        // 2. Validate ngày tháng
        if (request.getEndDate() != null && request.getStartDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
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

        // 4. Lưu vào Database
        Promotion savedPromotion = promotionRepository.save(promotion);

        // 🚀 5. GỬI EMAIL MARKETING (Hệ thống tự chạy ngầm nhờ @Async trong EmailService)
        // Chỉ gửi nếu là Voucher (có code) và đang ở trạng thái active
        if (savedPromotion.getPromoCode() != null && Boolean.TRUE.equals(savedPromotion.getStatus())) {

            // ✅ KẾT HỢP: Tìm cả khách hàng (USER) và quản trị viên (ADMIN) để cùng nhận mail
            // Dùng List.of("USER", "ADMIN") để lấy cả 2 nhóm
            List<User> recipients = userRepository.findByRolesIn(List.of("USER", "ADMIN"));

            List<String> userEmails = recipients.stream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .collect(Collectors.toList());

            if (!userEmails.isEmpty()) {
                System.out.println("🚀 Đang gửi mail cho " + userEmails.size() + " người (bao gồm cả Admin)...");
                emailService.sendNewVoucherEmail(userEmails, savedPromotion.getPromoCode(), savedPromotion.getDescription());
            } else {
                System.out.println("⚠️ Không tìm thấy email nào để gửi!");
            }
        }

        return mapToResponse(savedPromotion);
    }

    public PromotionResponse updatePromotion(Integer promoId, PromotionRequest request) {
        Promotion promotion = promotionRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        // Nếu đổi mã code, kiểm tra xem mã mới có trùng với ai khác không
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

        return mapToResponse(promotionRepository.save(promotion));
    }

    public void deletePromotion(Integer promoId) {
        Promotion promotion = promotionRepository.findById(promoId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
        promotion.setStatus(false);
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
        // Dùng hàm findValidVouchers từ Repository để lấy danh sách voucher còn hạn
        return promotionRepository.findValidVouchers().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // MAPPING: ENTITY -> RESPONSE DTO
    // ============================================
    private PromotionResponse mapToResponse(Promotion p) {
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
                .build();
    }
}