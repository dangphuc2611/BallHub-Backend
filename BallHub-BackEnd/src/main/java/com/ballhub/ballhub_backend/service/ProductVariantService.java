package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.request.product.PosVariantFilterRequest;
import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.dto.response.admin.PosVariantResponse;
import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.repository.ProductImageRepository;
import com.ballhub.ballhub_backend.repository.ProductVariantRepository;
import com.ballhub.ballhub_backend.repository.VariantPromotionRepository;
import com.ballhub.ballhub_backend.repository.spec.ProductVariantSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    // ✅ THÊM DÒNG NÀY ĐỂ GỌI ĐƯỢC % KHUYẾN MÃI
    private final VariantPromotionRepository variantPromotionRepository;

    @Transactional(readOnly = true)
    public PageResponse<PosVariantResponse> getVariantsForPos(PosVariantFilterRequest request, Pageable pageable) {
        Specification<ProductVariant> spec = ProductVariantSpecification.filterForPos(
                request.getKeyword(),
                request.getCategoryId(),
                request.getBrandId(),
                request.getColorId(),
                request.getSizeId()
        );

        Page<ProductVariant> variantPage = productVariantRepository.findAll(spec, pageable);

        List<PosVariantResponse> responses = variantPage.getContent().stream().map(variant -> {
            // Lấy ảnh chính của sản phẩm
            String imgUrl = productImageRepository.findByProduct_ProductIdAndIsMainTrue(variant.getProduct().getProductId())
                    .map(img -> img.getImageUrl())
                    .orElse(null);

            // 🚀 BẮT ĐẦU: LOGIC TÍNH TOÁN LẠI GIÁ BÁN CHO MÀN HÌNH POS
            Integer activePercent = variantPromotionRepository.findActiveFlashSaleDiscountByProductId(variant.getProduct().getProductId());
            int discountPct = activePercent != null ? activePercent : 0;

            BigDecimal basePrice = variant.getPrice();
            BigDecimal dynamicFinalPrice = variant.getFinalPrice() != null ? variant.getFinalPrice() : basePrice;

            if (discountPct > 0) {
                BigDecimal multiplier = BigDecimal.valueOf(100 - discountPct).divide(
                        BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                dynamicFinalPrice = basePrice.multiply(multiplier);
            }
            // 🚀 KẾT THÚC LOGIC TÍNH GIÁ

            return PosVariantResponse.builder()
                    .variantId(variant.getVariantId())
                    .productId(variant.getProduct().getProductId())
                    .productName(variant.getProduct().getProductName())
                    .sku(variant.getSku())
                    .sizeName(variant.getSize().getSizeName())
                    .colorName(variant.getColor().getColorName())
                    .brandName(variant.getProduct().getBrand() != null ? variant.getProduct().getBrand().getBrandName() : "")
                    .categoryName(variant.getProduct().getCategory() != null ? variant.getProduct().getCategory().getCategoryName() : "")
                    .price(basePrice)                 // ✅ Giá gốc (hiển thị gạch ngang)
                    .discountPrice(dynamicFinalPrice) // ✅ Giá đã giảm 10% (hiển thị màu cam)
                    .stockQuantity(variant.getStockQuantity())
                    .imageUrl(imgUrl)
                    .build();
        }).collect(Collectors.toList());

        return new PageResponse<>(
                responses,
                variantPage.getNumber(),
                variantPage.getSize(),
                variantPage.getTotalElements(),
                variantPage.getTotalPages(),
                variantPage.isLast(),
                variantPage.isFirst(),
                variantPage.isEmpty()
        );
    }
}