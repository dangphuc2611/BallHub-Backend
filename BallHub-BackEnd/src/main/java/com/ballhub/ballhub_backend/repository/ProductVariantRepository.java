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

    List<ProductVariant> findByProductProductIdAndStatusTrue(Integer productId);

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