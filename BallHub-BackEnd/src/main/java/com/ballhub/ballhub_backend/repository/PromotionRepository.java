package com.ballhub.ballhub_backend.repository;

import com.ballhub.ballhub_backend.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    Optional<Promotion> findByPromoCode(String promoCode);

    boolean existsByPromoCode(String promoCode);

    boolean existsByPromoCodeAndPromotionIdNot(String promoCode, Integer promotionId);

    // Tìm tất cả voucher có phân trang
    @Query("SELECT p FROM Promotion p WHERE p.promoCode IS NOT NULL ORDER BY p.promotionId DESC")
    Page<Promotion> findAllVouchers(Pageable pageable);

    // Tìm Voucher cho khách nhập mã (còn hiệu lực)
    @Query("SELECT p FROM Promotion p WHERE p.promoCode IS NOT NULL " +
            "AND p.status = true " +
            "AND (p.startDate IS NULL OR p.startDate <= CURRENT_TIMESTAMP) " +
            "AND (p.endDate IS NULL OR p.endDate >= CURRENT_TIMESTAMP) " +
            "AND (p.usageLimit IS NULL OR p.usageLimit = 0 OR p.usedCount < p.usageLimit)")
    List<Promotion> findValidVouchers();

    // TÌM KHUYẾN MÃI TỰ ĐỘNG (10%, 20%) CHO SẢN PHẨM
    @Query("SELECT p FROM Promotion p " +
            "JOIN p.variantPromotions vp " +
            "WHERE vp.variant.variantId = :variantId " +
            "AND p.status = true " +
            "AND p.promoCode IS NULL " +
            "AND (p.startDate IS NULL OR p.startDate <= CURRENT_TIMESTAMP) " +
            "AND (p.endDate IS NULL OR p.endDate >= CURRENT_TIMESTAMP)")
    Optional<Promotion> findActivePromotionForVariant(@Param("variantId") Integer variantId);
}