package com.ballhub.ballhub_backend.repository;


import com.ballhub.ballhub_backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProductProductId(Integer productId);

    List<ProductImage> findByVariantVariantId(Integer variantId);

    Optional<ProductImage> findByProduct_ProductIdAndIsMainTrue(Integer productId);

    void deleteByProductProductId(Integer productId);
}
