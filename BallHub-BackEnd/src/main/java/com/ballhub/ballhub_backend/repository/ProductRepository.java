package com.ballhub.ballhub_backend.repository;

import com.ballhub.ballhub_backend.entity.Product;
import com.ballhub.ballhub_backend.entity.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    // =====================================================
    // BASIC (GIỮ NGUYÊN)
    // =====================================================
    Page<Product> findByStatusTrue(Pageable pageable);
    Optional<Product> findByProductIdAndStatusTrue(Integer id);

    // =====================================================
    // SEARCH (GIỮ NGUYÊN)
    // =====================================================
    @Query("""
        SELECT p FROM Product p
        WHERE p.status = true
          AND (:keyword IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (:brandId IS NULL OR p.brand.brandId = :brandId)
    """)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("brandId") Integer brandId,
            Pageable pageable
    );

    // =====================================================
    // SORT ONLY (GIỮ NGUYÊN)
    // =====================================================
    @Query("""
        SELECT p FROM Product p
        JOIN p.variants v
        WHERE p.status = true
          AND v.status = true
          AND v.stockQuantity > 0
        GROUP BY p
        ORDER BY MIN(COALESCE(v.discountPrice, v.price)) ASC
    """)
    Page<Product> findAllOrderByMinPriceAsc(Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        JOIN p.variants v
        WHERE p.status = true
          AND v.status = true
          AND v.stockQuantity > 0
        GROUP BY p
        ORDER BY MIN(COALESCE(v.discountPrice, v.price)) DESC
    """)
    Page<Product> findAllOrderByMinPriceDesc(Pageable pageable);

    // =====================================================
    // PRODUCT DETAIL (GIỮ NGUYÊN)
    // =====================================================
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.variants v " +
            "LEFT JOIN FETCH v.size " +
            "LEFT JOIN FETCH v.color " +
            "WHERE p.productId = :id")
    Optional<Product> findProductWithVariants(@Param("id") Integer id);

    @Query("""
        SELECT i FROM ProductImage i
        WHERE i.product.productId = :productId
    """)
    List<ProductImage> findImagesByProductId(@Param("productId") Integer productId);

    // =====================================================
    // FILTER + SORT + PAGING (SHOP CORE) - ĐÃ CẬP NHẬT LOGIC FLASH SALE
    // =====================================================
    @Query(value = """
    SELECT p.* FROM Products p
    WHERE p.Status = 1
      AND p.ProductID IN (
          SELECT DISTINCT p_sub.ProductID 
          FROM Products p_sub
          JOIN ProductVariants v ON p_sub.ProductID = v.ProductID
          JOIN Categories c ON p_sub.CategoryID = c.CategoryID
          JOIN Brands b ON p_sub.BrandID = b.BrandID
          JOIN Sizes s ON v.SizeID = s.SizeID
          WHERE v.Status = 1
            AND v.StockQuantity > 0
            
            AND (:categories IS NULL OR c.CategoryName IN (:categories))
            AND (:teams IS NULL OR b.BrandName IN (:teams))
            AND (:sizes IS NULL OR s.SizeName IN (:sizes))
            
            AND (:minPrice IS NULL OR COALESCE(v.DiscountPrice, v.Price) >= :minPrice)
            AND (:maxPrice IS NULL OR COALESCE(v.DiscountPrice, v.Price) <= :maxPrice)
            
            AND (
               :search IS NULL
               OR LOWER(p_sub.ProductName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p_sub.Description) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.CategoryName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(b.BrandName) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            
            AND (:isSale = 0 OR p_sub.ProductID IN (
                SELECT v_sale.ProductID FROM ProductVariants v_sale
                JOIN VariantPromotions vp ON v_sale.VariantID = vp.VariantID
                JOIN Promotions pr ON vp.PromotionID = pr.PromotionID
                WHERE pr.Status = 1 
                  -- ✅ SỬA TẠI ĐÂY: Chấp nhận cả NULL và mã FLASH_
                  AND (pr.PromoCode IS NULL OR pr.PromoCode LIKE 'FLASH_%')
                  AND (pr.StartDate IS NULL OR pr.StartDate <= CURRENT_TIMESTAMP)
                  AND (pr.EndDate IS NULL OR pr.EndDate >= CURRENT_TIMESTAMP)
                  AND pr.DiscountPercent > 0
            ))
      )
    ORDER BY
      CASE WHEN :sort = 'new' THEN p.CreatedAt END DESC,
      CASE WHEN :sort = 'price_asc' THEN (SELECT MIN(COALESCE(v2.DiscountPrice, v2.Price)) FROM ProductVariants v2 WHERE v2.ProductID = p.ProductID AND v2.Status = 1 AND v2.StockQuantity > 0) END ASC,
      CASE WHEN :sort = 'price_desc' THEN (SELECT MIN(COALESCE(v2.DiscountPrice, v2.Price)) FROM ProductVariants v2 WHERE v2.ProductID = p.ProductID AND v2.Status = 1 AND v2.StockQuantity > 0) END DESC,
      p.ProductID DESC
    """,
            countQuery = """
    SELECT COUNT(DISTINCT p.ProductID)
    FROM Products p
    JOIN ProductVariants v ON p.ProductID = v.ProductID
    JOIN Categories c ON p.CategoryID = c.CategoryID
    JOIN Brands b ON p.BrandID = b.BrandID
    JOIN Sizes s ON v.SizeID = s.SizeID
    WHERE p.Status = 1
      AND v.Status = 1
      AND v.StockQuantity > 0

      AND (:categories IS NULL OR c.CategoryName IN (:categories))
      AND (:teams IS NULL OR b.BrandName IN (:teams))
      AND (:sizes IS NULL OR s.SizeName IN (:sizes))
      AND (:minPrice IS NULL OR COALESCE(v.DiscountPrice, v.Price) >= :minPrice)
      AND (:maxPrice IS NULL OR COALESCE(v.DiscountPrice, v.Price) <= :maxPrice)

      AND (
         :search IS NULL
         OR LOWER(p.ProductName) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(p.Description) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(c.CategoryName) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(b.BrandName) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      
      AND (:isSale = 0 OR p.ProductID IN (
          SELECT v_sub.ProductID FROM ProductVariants v_sub
          JOIN VariantPromotions vp ON v_sub.VariantID = vp.VariantID
          JOIN Promotions pr ON vp.PromotionID = pr.PromotionID
          WHERE pr.Status = 1 
            -- ✅ SỬA TẠI ĐÂY: Đồng bộ logic cho CountQuery
            AND (pr.PromoCode IS NULL OR pr.PromoCode LIKE 'FLASH_%')
            AND (pr.StartDate IS NULL OR pr.StartDate <= CURRENT_TIMESTAMP)
            AND (pr.EndDate IS NULL OR pr.EndDate >= CURRENT_TIMESTAMP)
            AND pr.DiscountPercent > 0
      ))
    """,
            nativeQuery = true
    )
    Page<Product> filterNativeShop(
            @Param("categories") List<String> categories,
            @Param("teams") List<String> teams,
            @Param("sizes") List<String> sizes,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            @Param("sort") String sort,
            @Param("isSale") Integer isSale,
            Pageable pageable
    );

    // =====================================================
    // FLASH SALE (DYNAMIC CHECK DB)
    // =====================================================
    @Query(value = """
        SELECT MAX(pr.DiscountPercent)
        FROM ProductVariants v
        JOIN VariantPromotions vp ON v.VariantID = vp.VariantID
        JOIN Promotions pr ON vp.PromotionID = pr.PromotionID
        WHERE v.ProductID = :productId
          AND pr.Status = 1
          -- ✅ SỬA TẠI ĐÂY: Đảm bảo tính toán % giảm giá đúng cho cả sản phẩm tạo từ web
          AND (pr.PromoCode IS NULL OR pr.PromoCode LIKE 'FLASH_%')
          AND (pr.StartDate IS NULL OR pr.StartDate <= CURRENT_TIMESTAMP)
          AND (pr.EndDate IS NULL OR pr.EndDate >= CURRENT_TIMESTAMP)
    """, nativeQuery = true)
    Integer findActiveFlashSalePercentByProductId(@Param("productId") Integer productId);
}