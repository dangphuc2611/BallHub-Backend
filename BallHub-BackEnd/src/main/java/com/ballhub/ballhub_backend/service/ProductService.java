package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.request.product.*;
import com.ballhub.ballhub_backend.dto.reponse.product.*;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.entity.*;
import com.ballhub.ballhub_backend.repository.*;
import com.ballhub.ballhub_backend.repository.spec.ProductSpecification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

        @Autowired
        private ProductRepository productRepository;
        @Autowired
        private CategoryRepository categoryRepository;
        @Autowired
        private BrandRepository brandRepository;
        @Autowired
        private SizeRepository sizeRepository;
        @Autowired
        private ColorRepository colorRepository;
        @Autowired
        private ProductVariantRepository variantRepository;
        @Autowired
        private ProductContentRepository productContentRepository;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private ProductImageRepository imageRepository;
        @Autowired
        private FavoriteRepository favoriteRepository;
        @Autowired
        private UserRepository userRepository;

        @Transactional(readOnly = true)
        public Page<ProductResponse> getAllProducts(Pageable pageable) {
                return productRepository.findByStatusTrue(pageable)
                                .map(this::mapToListResponse);
        }

        @Transactional(readOnly = true)
        public Page<ProductResponse> searchProducts(ProductFilterRequest filter, Pageable pageable) {
                return productRepository.searchProducts(
                                filter.getKeyword(),
                                filter.getCategoryId(),
                                filter.getBrandId(),
                                pageable).map(this::mapToListResponse);
        }

        @Transactional(readOnly = true)
        public ProductDetailResponse getProductById(Integer id) {
                Product product = productRepository.findProductWithVariants(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

                List<ProductImage> images = productRepository.findImagesByProductId(id);
                product.setImages(images);

                List<ProductContent> contents = productContentRepository
                                .findByProduct_ProductIdAndStatusTrueOrderBySortOrderAsc(id);
                ProductContentBlock contentBlock = mapToContentBlock(contents);

                ProductDetailResponse response = mapToDetailResponse(product);
                response.setContentBlock(contentBlock);

                return response;
        }

        @Transactional(readOnly = true)
        public Page<ProductResponse> filterProducts(
                        List<String> categories, List<String> teams, List<String> sizes,
                        BigDecimal minPrice, BigDecimal maxPrice, String search, String sort,
                        boolean isSale, Pageable pageable) {
                categories = (categories == null || categories.isEmpty()) ? null : categories;
                teams = (teams == null || teams.isEmpty()) ? null : teams;
                sizes = (sizes == null || sizes.isEmpty()) ? null : sizes;
                if (search != null && search.trim().isEmpty())
                        search = null;

                Integer isSaleParam = isSale ? 1 : 0;

                Page<Product> pageData = productRepository.filterNativeShop(
                                categories, teams, sizes, minPrice, maxPrice, search, sort, isSaleParam, pageable);

                List<ProductResponse> list = pageData.getContent().stream()
                                .map(this::mapToListResponse)
                                .toList();

                return new org.springframework.data.domain.PageImpl<>(
                                list, pageable, pageData.getTotalElements());
        }

        public ProductDetailResponse createProduct(CreateProductRequest request) {
                Category category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
                Brand brand = brandRepository.findById(request.getBrandId())
                                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại"));

                Product product = Product.builder()
                                .productName(request.getProductName())
                                .description(request.getDescription())
                                .category(category)
                                .brand(brand)
                                .status(true)
                                .build();

                Product savedProduct = productRepository.save(product);

                for (CreateVariantRequest variantReq : request.getVariants()) {
                        createVariant(savedProduct, variantReq);
                }

                return mapToDetailResponse(savedProduct);
        }

        public ProductDetailResponse updateProduct(Integer id, UpdateProductRequest request) {
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
                Category category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
                Brand brand = brandRepository.findById(request.getBrandId())
                                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại"));

                product.setProductName(request.getProductName());
                product.setDescription(request.getDescription());
                product.setCategory(category);
                product.setBrand(brand);

                if (request.getStatus() != null) {
                        product.setStatus(request.getStatus());
                }

                Product updated = productRepository.save(product);
                return mapToDetailResponse(updated);
        }

        public void deleteProduct(Integer id) {
                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
                product.setStatus(false);
                productRepository.save(product);
        }

        private void createVariant(Product product, CreateVariantRequest request) {
                Size size = sizeRepository.findById(request.getSizeId())
                                .orElseThrow(() -> new ResourceNotFoundException("Size không tồn tại"));
                Color color = colorRepository.findById(request.getColorId())
                                .orElseThrow(() -> new ResourceNotFoundException("Color không tồn tại"));

                variantRepository.findByProductAndSizeAndColor(
                                product.getProductId(), size.getSizeId(), color.getColorId()).ifPresent(v -> {
                                        throw new BadRequestException("Variant này đã tồn tại");
                                });

                String sku = request.getSku();
                if (sku == null || sku.trim().isEmpty()) {
                        sku = generateSKU(product, size, color);
                }

                if (variantRepository.existsBySku(sku)) {
                        throw new BadRequestException("SKU đã tồn tại: " + sku);
                }

                ProductVariant variant = ProductVariant.builder()
                                .product(product).size(size).color(color)
                                .price(request.getPrice())
                                .stockQuantity(request.getStockQuantity())
                                .sku(sku).status(true).build();

                variantRepository.save(variant);
        }

        private String generateSKU(Product product, Size size, Color color) {
                String brandCode = product.getBrand() != null
                                ? product.getBrand().getBrandName().toUpperCase().replaceAll("\\s+", "")
                                : "BRAND";
                String productId = product.getProductId().toString();
                String sizeCode = size.getSizeName().toUpperCase();
                String colorCode = color.getColorName().toUpperCase().replaceAll("\\s+", "");
                return String.format("%s-%s-%s-%s", brandCode, productId, sizeCode, colorCode);
        }

        public VariantResponse updateVariant(Integer variantId, UpdateVariantRequest request) {
                ProductVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> new ResourceNotFoundException("Variant không tồn tại"));

                variant.setPrice(request.getPrice());
                variant.setDiscountPrice(request.getDiscountPrice());
                variant.setStockQuantity(request.getStockQuantity());
                if (request.getStatus() != null) {
                        variant.setStatus(request.getStatus());
                }

                ProductVariant updated = variantRepository.save(variant);
                return mapToVariantResponse(updated);
        }

        public void deleteVariant(Integer variantId) {
                ProductVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> new ResourceNotFoundException("Variant không tồn tại"));
                variant.setStatus(false);
                variantRepository.save(variant);
        }

        // ==========================================================
        // MAPPING DATA CHO FRONTEND KÈM THEO LOGIC TÍNH FLASH SALE
        // ==========================================================
        public ProductResponse mapToListResponse(Product product) {

                List<ProductVariant> variants = product.getVariants().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getStatus()))
                                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                                .toList();

                // 1. Lấy giá gốc (để hiển thị gạch ngang)
                BigDecimal minOriginalPrice = variants.stream()
                                .map(ProductVariant::getPrice)
                                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

                BigDecimal maxOriginalPrice = variants.stream()
                                .map(ProductVariant::getPrice)
                                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

                // 2. Tìm xem sản phẩm có đang trong Flash Sale không
                Integer activePercent = productRepository.findActiveFlashSalePercentByProductId(product.getProductId());
                int discountPct = activePercent != null ? activePercent : 0;

                // 3. Tính toán Giá mới sau khi Sale
                BigDecimal minPrice = minOriginalPrice;
                BigDecimal maxPrice = maxOriginalPrice;

                if (discountPct > 0) {
                        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPct).divide(BigDecimal.valueOf(100), 2,
                                        RoundingMode.HALF_UP);
                        minPrice = minOriginalPrice.multiply(multiplier);
                        maxPrice = maxOriginalPrice.multiply(multiplier);
                } else {
                        // Logic cũ (Phòng trường hợp Admin tự điền vào cột DiscountPrice bằng tay)
                        minPrice = variants.stream()
                                        .map(ProductVariant::getFinalPrice).min(BigDecimal::compareTo)
                                        .orElse(BigDecimal.ZERO);
                        maxPrice = variants.stream()
                                        .map(ProductVariant::getFinalPrice).max(BigDecimal::compareTo)
                                        .orElse(BigDecimal.ZERO);

                        if (minOriginalPrice.compareTo(BigDecimal.ZERO) > 0
                                        && minOriginalPrice.compareTo(minPrice) > 0) {
                                discountPct = minOriginalPrice.subtract(minPrice)
                                                .divide(minOriginalPrice, 2, RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal(100)).intValue();
                        }
                }

                String mainImage = product.getImages().stream()
                                .filter(ProductImage::getIsMain).findFirst()
                                .map(ProductImage::getImageUrl).orElse(null);

                return ProductResponse.builder()
                                .productId(product.getProductId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId()
                                                : null)
                                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName()
                                                : null)
                                .brandId(product.getBrand() != null ? product.getBrand().getBrandId() : null)
                                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                                .mainImage(mainImage)
                                .minOriginalPrice(minOriginalPrice) // Trả về giá gốc
                                .maxOriginalPrice(maxOriginalPrice) // Trả về giá gốc
                                .discountPercent(discountPct) // Trả về % Flash Sale
                                .minPrice(minPrice) // Trả về giá Đã Sale
                                .maxPrice(maxPrice) // Trả về giá Đã Sale
                                .status(product.getStatus())
                                .createdAt(product.getCreatedAt())
                                .build();
        }

        private ProductDetailResponse mapToDetailResponse(Product product) {

                Integer activePercent = productRepository.findActiveFlashSalePercentByProductId(product.getProductId());
                int discountPct = activePercent != null ? activePercent : 0;

                List<VariantResponse> variants = product.getVariants().stream()
                                .filter(v -> Boolean.TRUE.equals(v.getStatus()))
                                .map(v -> {
                                        BigDecimal basePrice = v.getPrice();
                                        BigDecimal dynamicFinalPrice = v.getFinalPrice();

                                        // Nếu có Flash Sale, ép giá finalPrice xuống mức sale
                                        if (discountPct > 0) {
                                                BigDecimal multiplier = BigDecimal.valueOf(100 - discountPct).divide(
                                                                BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                                                dynamicFinalPrice = basePrice.multiply(multiplier);
                                        }

                                        return VariantResponse.builder()
                                                        .variantId(v.getVariantId())
                                                        .productId(product.getProductId())
                                                        .sizeId(v.getSize().getSizeId())
                                                        .sizeName(v.getSize().getSizeName())
                                                        .colorId(v.getColor().getColorId())
                                                        .colorName(v.getColor().getColorName())
                                                        .price(basePrice)
                                                        .discountPrice(dynamicFinalPrice)
                                                        .finalPrice(dynamicFinalPrice)
                                                        .stockQuantity(v.getStockQuantity())
                                                        .status(v.getStatus())
                                                        .sku(v.getSku())
                                                        .build();
                                }).toList();

                BigDecimal minPrice = variants.stream().map(VariantResponse::getFinalPrice).min(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                BigDecimal maxPrice = variants.stream().map(VariantResponse::getFinalPrice).max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);

                List<ProductImageResponse> images = product.getImages().stream()
                                .map(this::mapToImageResponse).toList();

                List<SizeOptionResponse> sizeOptions = variants.stream()
                                .collect(Collectors.groupingBy(VariantResponse::getSizeId))
                                .entrySet().stream()
                                .map(e -> SizeOptionResponse.builder()
                                                .sizeId(e.getKey())
                                                .sizeName(e.getValue().get(0).getSizeName())
                                                .available(e.getValue().stream()
                                                                .anyMatch(v -> v.getStockQuantity() != null
                                                                                && v.getStockQuantity() > 0))
                                                .build())
                                .toList();

                List<ColorOptionResponse> colorOptions = variants.stream()
                                .collect(Collectors.groupingBy(VariantResponse::getColorId))
                                .entrySet().stream()
                                .map(e -> ColorOptionResponse.builder()
                                                .colorId(e.getKey())
                                                .colorName(e.getValue().get(0).getColorName())
                                                .available(e.getValue().stream()
                                                                .anyMatch(v -> v.getStockQuantity() != null
                                                                                && v.getStockQuantity() > 0))
                                                .build())
                                .toList();

                return ProductDetailResponse.builder()
                                .productId(product.getProductId())
                                .productName(product.getProductName())
                                .description(product.getDescription())
                                .categoryId(product.getCategory().getCategoryId())
                                .categoryName(product.getCategory().getCategoryName())
                                .brandId(product.getBrand().getBrandId())
                                .brandName(product.getBrand().getBrandName())
                                .variants(variants)
                                .images(images)
                                .sizeOptions(sizeOptions)
                                .colorOptions(colorOptions)
                                .minPrice(minPrice)
                                .maxPrice(maxPrice)
                                .status(product.getStatus())
                                .createdAt(product.getCreatedAt())
                                .build();
        }

        private VariantResponse mapToVariantResponse(ProductVariant variant) {
                return VariantResponse.builder()
                                .variantId(variant.getVariantId())
                                .productId(variant.getProduct().getProductId())
                                .sizeId(variant.getSize().getSizeId())
                                .sizeName(variant.getSize().getSizeName())
                                .colorId(variant.getColor().getColorId())
                                .colorName(variant.getColor().getColorName())
                                .price(variant.getPrice())
                                .discountPrice(variant.getDiscountPrice())
                                .finalPrice(variant.getFinalPrice())
                                .stockQuantity(variant.getStockQuantity())
                                .sku(variant.getSku())
                                .status(variant.getStatus())
                                .build();
        }

        private ProductContentBlock mapToContentBlock(List<ProductContent> contents) {
                DescriptionBlock description = null;
                List<String> highlights = List.of();
                List<SpecItem> specs = List.of();

                for (ProductContent c : contents) {
                        if (!Boolean.TRUE.equals(c.getStatus()))
                                continue;
                        switch (c.getType()) {
                                case DESCRIPTION ->
                                        description = DescriptionBlock.builder().html(c.getContent()).build();
                                case HIGHLIGHT -> {
                                        try {
                                                highlights = objectMapper.readValue(c.getContent(),
                                                                new TypeReference<>() {
                                                                });
                                        } catch (Exception e) {
                                                throw new RuntimeException("Parse HIGHLIGHT failed", e);
                                        }
                                }
                                case SPEC -> {
                                        try {
                                                specs = objectMapper.readValue(c.getContent(), new TypeReference<>() {
                                                });
                                        } catch (Exception e) {
                                                throw new RuntimeException("Parse SPEC failed", e);
                                        }
                                }
                        }
                }
                return ProductContentBlock.builder().description(description).highlights(highlights).specs(specs)
                                .build();
        }

        private ProductImageResponse mapToImageResponse(ProductImage image) {
                return ProductImageResponse.builder()
                                .imageId(image.getImageId())
                                .productId(image.getProduct().getProductId())
                                .variantId(image.getVariant() != null ? image.getVariant().getVariantId() : null)
                                .imageUrl(image.getImageUrl())
                                .isMain(image.getIsMain())
                                .build();
        }
}