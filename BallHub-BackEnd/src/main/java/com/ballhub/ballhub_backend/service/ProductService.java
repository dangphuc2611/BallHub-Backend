package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.request.product.*;
import com.ballhub.ballhub_backend.dto.response.product.*;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.entity.*;
import com.ballhub.ballhub_backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // ✅ Bổ sung import này
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
        private MaterialRepository materialRepository;
        @Autowired
        private StyleRepository styleRepository;

        // ✅ Inject thêm 2 Repository xử lý khuyến mãi
        @Autowired
        private PromotionRepository promotionRepository;
        @Autowired
        private VariantPromotionRepository variantPromotionRepository;

        @Transactional(readOnly = true)
        public Page<ProductResponse> getAllProducts(Pageable pageable) {
                return productRepository.findAll(pageable) // ✅ Lấy tất cả (cả bật và tắt)
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

                Material material = null;
                if (request.getMaterialId() != null) {
                        material = materialRepository.findById(request.getMaterialId()).orElse(null);
                }
                Style style = null;
                if (request.getStyleId() != null) {
                        style = styleRepository.findById(request.getStyleId()).orElse(null);
                }

                Product product = Product.builder()
                        .productName(request.getProductName())
                        .description(request.getDescription())
                        .category(category)
                        .brand(brand)
                        .material(material)
                        .style(style)
                        .status(true)
                        .build();

                Product savedProduct = productRepository.save(product);

                for (CreateVariantRequest variantReq : request.getVariants()) {
                        createVariant(savedProduct, variantReq);
                }

                // ✅ XỬ LÝ KHUYẾN MÃI
                handleProductDiscount(savedProduct, request.getDiscountPercent());

                return mapToDetailResponse(savedProduct);
        }

        public ProductDetailResponse updateProduct(Integer id, UpdateProductRequest request) {
                // 1. Sử dụng findProductWithVariants để lấy SP kèm Variants ngay từ đầu
                Product product = productRepository.findProductWithVariants(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
                Brand brand = brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại"));

                product.setProductName(request.getProductName());
                product.setDescription(request.getDescription());
                product.setCategory(category);
                product.setBrand(brand);

                if (request.getMaterialId() != null) {
                        Integer matId = request.getMaterialId();
                        Material material = materialRepository.findById(matId).orElse(null);
                        product.setMaterial(material);
                }
                if (request.getStyleId() != null) {
                        Integer styId = request.getStyleId();
                        Style style = styleRepository.findById(styId).orElse(null);
                        product.setStyle(style);
                }

                if (request.getStatus() != null) {
                        product.setStatus(request.getStatus());
                }

                // 2. Lưu thông tin cơ bản
                productRepository.save(product);

                // 3. Xử lý khuyến mãi (Đảm bảo trong hàm này bạn đã thêm .flush() như tôi hướng dẫn)
                handleProductDiscount(product, request.getDiscountPercent());

                // 🚀 BƯỚC QUAN TRỌNG NHẤT: TRUY VẤN LẠI DỮ LIỆU ĐÃ NẠP ĐỦ SIZE/COLOR
                // Việc này giúp tránh lỗi "LazyInitializationException: could not initialize proxy Size#3"
                Product freshProduct = productRepository.findProductWithVariants(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Lỗi đồng bộ dữ liệu sau khi cập nhật"));

                // 4. Trả về kết quả từ đối tượng đã nạp đầy đủ (Eager Loading)
                return mapToDetailResponse(freshProduct);
        }

        // ✅ HÀM DÙNG CHUNG ĐỂ XỬ LÝ KHUYẾN MÃI (INSERT VÀO DB ĐÚNG CHUẨN)
        private void handleProductDiscount(Product product, Integer newDiscount) {
                List<ProductVariant> variants = variantRepository.findByProduct_ProductId(product.getProductId());
                if (variants.isEmpty()) return;

                variantPromotionRepository.deleteFlashSalesByVariants(variants);

                if (newDiscount != null && newDiscount > 0) {
                        Promotion promo = promotionRepository.findByDiscountPercentAndPromoCodeIsNull(newDiscount)
                                .orElseGet(() -> {
                                        Promotion newPromo = new Promotion();
                                        newPromo.setPromotionName("Flash Sale " + newDiscount + "%");
                                        newPromo.setDiscountPercent(newDiscount);
                                        newPromo.setStartDate(java.time.LocalDateTime.now());
                                        newPromo.setEndDate(java.time.LocalDateTime.now().plusYears(10));
                                        newPromo.setStatus(true);
                                        return promotionRepository.save(newPromo);
                                });

                        List<VariantPromotion> newLinks = new ArrayList<>();
                        for (ProductVariant variant : variants) {
                                VariantPromotion vp = new VariantPromotion();
                                vp.setVariant(variant);
                                vp.setPromotion(promo);
                                newLinks.add(vp);
                        }
                        variantPromotionRepository.saveAll(newLinks);
                }
        }

        public void deleteProduct(Integer id) {
                Product product = productRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
                product.setStatus(false);
                productRepository.save(product);
        }

        public void addImagesToProduct(Integer productId, List<String> imageUrls, Boolean setFirstAsMain) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

                if (Boolean.TRUE.equals(setFirstAsMain)) {
                        List<ProductImage> existing = imageRepository.findByProductProductId(productId);
                        existing.forEach(img -> img.setIsMain(false));
                        imageRepository.saveAll(existing);
                }

                boolean firstImage = Boolean.TRUE.equals(setFirstAsMain);
                for (int i = 0; i < imageUrls.size(); i++) {
                        String url = imageUrls.get(i);
                        boolean isMain = firstImage && (i == 0);
                        ProductImage image = ProductImage.builder()
                                .product(product)
                                .imageUrl(url)
                                .isMain(isMain)
                                .build();
                        imageRepository.save(image);
                }
        }

        public VariantResponse addVariant(Integer productId, CreateVariantRequest request) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

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

                ProductVariant saved = variantRepository.save(variant);
                return mapToVariantResponse(saved);
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

        public void hardDeleteVariant(Integer variantId) {
                ProductVariant variant = variantRepository.findById(variantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Variant không tồn tại"));
                variantRepository.delete(variant);
        }

        @Transactional(readOnly = true)
        public Page<VariantResponse> getAllVariants(Pageable pageable) {
                return variantRepository.findAll(pageable)
                        .map(this::mapToVariantResponse);
        }

        // ✅ BỔ SUNG HÀM NÀY: Để lấy list đơn giản phục vụ Modal chọn sản phẩm
        @Transactional(readOnly = true)
        public List<ProductSimpleResponse> getAllActiveSimpleList() {
                return productRepository.findAll().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getStatus()))
                        .map(p -> {
                                java.math.BigDecimal minPrice = p.getVariants().stream()
                                        .filter(v -> Boolean.TRUE.equals(v.getStatus()))
                                        .map(ProductVariant::getPrice)
                                        .min(java.math.BigDecimal::compareTo)
                                        .orElse(java.math.BigDecimal.ZERO);

                                // Dùng constructor của DTO, không bao giờ lo lỗi kiểu dữ liệu
                                return new ProductSimpleResponse(p.getProductId(), p.getProductName(), minPrice);
                        })
                        .collect(Collectors.toList());
        }

        // ==========================================================
        // MAPPING DATA CHO FRONTEND KÈM THEO LOGIC TÍNH FLASH SALE
        // ==========================================================
        public ProductResponse mapToListResponse(Product product) {

                List<ProductVariant> variants = product.getVariants().stream()
                        .filter(v -> Boolean.TRUE.equals(v.getStatus()))
                        .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                        .toList();

                BigDecimal minOriginalPrice = variants.stream()
                        .map(ProductVariant::getPrice)
                        .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

                BigDecimal maxOriginalPrice = variants.stream()
                        .map(ProductVariant::getPrice)
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

                Integer activePercent = variantPromotionRepository.findActiveFlashSaleDiscountByProductId(product.getProductId());
                int discountPct = activePercent != null ? activePercent : 0;

                BigDecimal minPrice = minOriginalPrice;
                BigDecimal maxPrice = maxOriginalPrice;

                if (discountPct > 0) {
                        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPct).divide(BigDecimal.valueOf(100), 2,
                                RoundingMode.HALF_UP);
                        minPrice = minOriginalPrice.multiply(multiplier);
                        maxPrice = maxOriginalPrice.multiply(multiplier);
                }

                String mainImage = product.getImages().stream()
                        .filter(ProductImage::getIsMain).findFirst()
                        .map(ProductImage::getImageUrl).orElse(null);

                return ProductResponse.builder()
                        .productId(product.getProductId())
                        .productName(product.getProductName())
                        .description(product.getDescription())
                        .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                        .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                        .brandId(product.getBrand() != null ? product.getBrand().getBrandId() : null)
                        .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                        .materialName(product.getMaterial() != null ? product.getMaterial().getMaterialName() : null)
                        .styleName(product.getStyle() != null ? product.getStyle().getStyleName() : null)
                        .mainImage(mainImage)
                        .minOriginalPrice(minOriginalPrice)
                        .maxOriginalPrice(maxOriginalPrice)
                        .discountPercent(discountPct)
                        .minPrice(minPrice)
                        .maxPrice(maxPrice)
                        .status(product.getStatus())
                        .createdAt(product.getCreatedAt())
                        .build();
        }

        private ProductDetailResponse mapToDetailResponse(Product product) {

                if (product.getVariants() != null) {
                        product.getVariants().size();
                }

                Integer activePercent = variantPromotionRepository.findActiveFlashSaleDiscountByProductId(product.getProductId());
                int discountPct = activePercent != null ? activePercent : 0;

                List<VariantResponse> variants = product.getVariants().stream()
                        .filter(v -> Boolean.TRUE.equals(v.getStatus()))
                        .map(v -> {
                                BigDecimal basePrice = v.getPrice();
                                BigDecimal dynamicFinalPrice = v.getFinalPrice();

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
                        .materialId(product.getMaterial() != null ? product.getMaterial().getMaterialId() : null)
                        .materialName(product.getMaterial() != null ? product.getMaterial().getMaterialName() : null)
                        .styleId(product.getStyle() != null ? product.getStyle().getStyleId() : null)
                        .styleName(product.getStyle() != null ? product.getStyle().getStyleName() : null)
                        .variants(variants)
                        .images(images)
                        .sizeOptions(sizeOptions)
                        .colorOptions(colorOptions)
                        .minPrice(minPrice)
                        .maxPrice(maxPrice)
                        .discountPercent(discountPct)
                        .status(product.getStatus())
                        .createdAt(product.getCreatedAt())
                        .build();
        }

        private VariantResponse mapToVariantResponse(ProductVariant variant) {
                // 1. TÌM % SALE TỪ BẢNG TRUNG GIAN ĐANG ACTIVE
                Integer activePercent = variantPromotionRepository.findActiveFlashSaleDiscountByProductId(variant.getProduct().getProductId());
                int discountPct = activePercent != null ? activePercent : 0;

                BigDecimal basePrice = variant.getPrice();
                BigDecimal dynamicFinalPrice = variant.getFinalPrice() != null ? variant.getFinalPrice() : basePrice;

                // 2. TÍNH TOÁN LẠI GIÁ BÁN NẾU CÓ FLASH SALE
                if (discountPct > 0) {
                        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPct).divide(
                                BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        dynamicFinalPrice = basePrice.multiply(multiplier);
                }

                return VariantResponse.builder()
                        .variantId(variant.getVariantId())
                        .productId(variant.getProduct().getProductId())
                        .productName(variant.getProduct().getProductName())
                        .productImage(variant.getProduct().getImages().stream()
                                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                                .findFirst()
                                .map(ProductImage::getImageUrl)
                                .orElse(variant.getProduct().getImages().isEmpty() ? null
                                        : variant.getProduct().getImages().get(0)
                                        .getImageUrl()))
                        .sizeId(variant.getSize().getSizeId())
                        .sizeName(variant.getSize().getSizeName())
                        .colorId(variant.getColor().getColorId())
                        .colorName(variant.getColor().getColorName())
                        .price(basePrice)                 // Giá gốc
                        .discountPrice(dynamicFinalPrice) // Giá hiển thị (đã giảm)
                        .finalPrice(dynamicFinalPrice)    // Giá chốt thanh toán
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