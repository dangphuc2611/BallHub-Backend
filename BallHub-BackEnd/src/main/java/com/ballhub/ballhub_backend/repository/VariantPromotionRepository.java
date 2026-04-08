package com.ballhub.ballhub_backend.repository;

import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.entity.Promotion;
import com.ballhub.ballhub_backend.entity.VariantPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VariantPromotionRepository extends JpaRepository<VariantPromotion, Integer> {

    /**
     * ✅ Dùng để lấy tất cả các liên kết của 1 Khuyến mãi cụ thể.
     * Phục vụ hàm lấy danh sách ID sản phẩm đã áp dụng ở Service.
     */
    List<VariantPromotion> findByPromotion_PromotionId(Integer promotionId);

    /**
     * ✅ QUAN TRỌNG: Dùng để xóa sạch các liên kết giữa 1 Khuyến mãi với các sản phẩm cũ
     * Phục vụ cho việc cập nhật lại danh sách sản phẩm trong PromotionService.updatePromotion
     */
    @Modifying
    @Transactional
    void deleteByPromotion(Promotion promotion);

    /**
     * Xóa các link Flash Sale (PromoCode IS NULL) của 1 list biến thể
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM VariantPromotion vp WHERE vp.variant IN :variants AND vp.promotion.promoCode IS NULL")
    void deleteFlashSalesByVariants(@Param("variants") List<ProductVariant> variants);

    /**
     * Lấy % giảm giá Flash Sale ĐANG ACTIVE của sản phẩm (chất lượng nhất)
     */
    @Query("SELECT MAX(p.discountPercent) FROM VariantPromotion vp " +
            "JOIN vp.promotion p " +
            "WHERE vp.variant.product.productId = :productId " +
            "AND p.promoCode IS NULL " +
            "AND p.status = true " +
            "AND (p.startDate IS NULL OR p.startDate <= CURRENT_TIMESTAMP) " +
            "AND (p.endDate IS NULL OR p.endDate >= CURRENT_TIMESTAMP)")
    Integer findActiveFlashSaleDiscountByProductId(@Param("productId") Integer productId);
}