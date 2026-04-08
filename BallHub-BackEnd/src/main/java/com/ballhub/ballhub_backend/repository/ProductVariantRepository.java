package com.ballhub.ballhub_backend.repository;

import com.ballhub.ballhub_backend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer>, JpaSpecificationExecutor<ProductVariant> {

    // ✅ BỔ SUNG HÀM NÀY: Để lấy toàn bộ biến thể từ danh sách ID sản phẩm
    // Dùng cho logic PromotionService.applyPromotionToProducts
    List<ProductVariant> findByProduct_ProductIdIn(List<Integer> productIds);

    List<ProductVariant> findByProductProductIdAndStatusTrue(Integer productId);

    List<ProductVariant> findByProduct_ProductId(Integer productId);

    Optional<ProductVariant> findByVariantIdAndStatusTrue(Integer variantId);

    @Query("SELECT v FROM ProductVariant v " +
            "WHERE v.product.productId = :productId " +
            "AND v.size.sizeId = :sizeId " +
            "AND v.color.colorId = :colorId")
    Optional<ProductVariant> findByProductAndSizeAndColor(
            @Param("productId") Integer productId,
            @Param("sizeId") Integer sizeId,
            @Param("colorId") Integer colorId
    );

    boolean existsBySku(String sku);
}