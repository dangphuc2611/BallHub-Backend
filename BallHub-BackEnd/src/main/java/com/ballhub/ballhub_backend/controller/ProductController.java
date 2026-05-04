package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.dto.response.product.*;
import com.ballhub.ballhub_backend.dto.request.product.*;
import com.ballhub.ballhub_backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    // PUBLIC - Get all products (with pagination)
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(products)));
    }

    @GetMapping("/products/filter")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> filterProducts(
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> teams,
            @RequestParam(required = false) List<String> sizes,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean isSale) {
        Pageable pageable = PageRequest.of(page, size);

        // ✅ FIX: nếu user bấm tìm mà không nhập gì
        if (search != null && search.trim().isEmpty()) {
            search = null;
        }

        Page<ProductResponse> products = productService.filterProducts(
                categories, teams, sizes, minPrice, maxPrice, search, sort, isSale, pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(products)));
    }

    // PUBLIC - Search products
    @PostMapping("/products/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestBody ProductFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.searchProducts(filter, pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(products)));
    }

    // Đảm bảo đặt cái này TRÊN hàm getProductById(@PathVariable Integer id)
    @GetMapping("/products/all-active")
    public ResponseEntity<ApiResponse<List<ProductSimpleResponse>>> getAllActiveForPromotion() {
        List<ProductSimpleResponse> productList = productService.getAllActiveSimpleList();
        return ResponseEntity.ok(ApiResponse.success(productList));
    }

    // PUBLIC - Get product detail
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable Integer id) {
        ProductDetailResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    // ADMIN - Create product
    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductDetailResponse product = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", product));
    }

    // ADMIN - Update product
    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductDetailResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", product));
    }

    // ADMIN - Delete product
    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công", null));
    }

    // ADMIN - Add images to product from static resources
    @PostMapping("/admin/products/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<?>> addImagesToProduct(
                @PathVariable Integer id,
                @RequestBody Map<String, Object> body) {
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) body.get("imageUrls");
            Boolean isMain = body.get("isMain") != null && (Boolean) body.get("isMain");
            Integer variantId = body.get("variantId") != null ? Integer.valueOf(body.get("variantId").toString()) : null;
            List<ProductImageResponse> images = productService.addImagesToProduct(id, imageUrls, isMain, variantId);
            return ResponseEntity.ok(ApiResponse.success("Thêm ảnh thành công", images));
        }

        // ADMIN - Delete image from product
        @DeleteMapping("/admin/products/images/{imageId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<?>> deleteProductImage(@PathVariable Integer imageId) {
            productService.deleteProductImage(imageId);
            return ResponseEntity.ok(ApiResponse.success("Xóa ảnh thành công", null));
        }



    // ADMIN - List all static images from resources/static/img
    @GetMapping("/admin/images/static")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> listStaticImages() {
        List<String> urls = new ArrayList<>();
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:static/img/**/*.{png,jpg,jpeg,webp,gif}");
            for (Resource resource : resources) {
                String path = resource.getURL().getPath();
                int index = path.lastIndexOf("/static/img/");
                if (index != -1) {
                    urls.add(path.substring(index + 7)); // +7 to keep "/img/..."
                } else if (path.contains("/img/")) {
                    urls.add(path.substring(path.lastIndexOf("/img/")));
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading static images: " + e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.success(urls));
    }

    // ADMIN - Add variant to product
    @PostMapping("/admin/products/{id}/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VariantResponse>> addVariant(
            @PathVariable Integer id,
            @Valid @RequestBody CreateVariantRequest request) {
        VariantResponse variant = productService.addVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Thêm variant thành công", variant));
    }

    // ADMIN - Update variant
    @PutMapping("/admin/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VariantResponse>> updateVariant(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateVariantRequest request) {
        VariantResponse variant = productService.updateVariant(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật variant thành công", variant));
    }

    // ADMIN - Delete variant
    @DeleteMapping("/admin/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteVariant(@PathVariable Integer id) {
        productService.deleteVariant(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa variant thành công", null));
    }

    // ADMIN - List all variants with pagination
    @GetMapping("/admin/variants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<VariantResponse>>> listAllVariants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "variantId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<VariantResponse> variants = productService.getAllVariants(pageable);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(variants)));
    }


}
