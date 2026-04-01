package com.ballhub.ballhub_backend.repository.spec;

import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.entity.Product;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductVariantSpecification {

    public static Specification<ProductVariant> filterForPos(
            String keyword, Integer categoryId, Integer brandId, Integer colorId, Integer sizeId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<ProductVariant, Product> productJoin = root.join("product");

            // Chỉ lấy các sản phẩm và biến thể đang active (Status = 1)
            predicates.add(cb.equal(root.get("status"), true));
            predicates.add(cb.equal(productJoin.get("status"), true));

            // Tìm kiếm theo Tên sản phẩm hoặc SKU
            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(productJoin.get("productName")), likeKeyword);
                Predicate skuPredicate = cb.like(cb.lower(root.get("sku")), likeKeyword);
                predicates.add(cb.or(namePredicate, skuPredicate));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(productJoin.get("category").get("categoryID"), categoryId));
            }
            if (brandId != null) {
                predicates.add(cb.equal(productJoin.get("brand").get("brandID"), brandId));
            }
            if (colorId != null) {
                predicates.add(cb.equal(root.get("color").get("colorID"), colorId));
            }
            if (sizeId != null) {
                predicates.add(cb.equal(root.get("size").get("sizeID"), sizeId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}