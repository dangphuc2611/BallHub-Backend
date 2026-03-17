package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.reponse.ApiResponse;
import com.ballhub.ballhub_backend.dto.reponse.PageResponse;
import com.ballhub.ballhub_backend.dto.reponse.brand.BrandResponse;
import com.ballhub.ballhub_backend.dto.request.brand.CreateBrandRequest;
import com.ballhub.ballhub_backend.dto.request.brand.UpdateBrandRequest;
import com.ballhub.ballhub_backend.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BrandController {

    @Autowired
    private BrandService brandService;

    // PUBLIC - Get all brands
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        List<BrandResponse> brands = brandService.getAllBrands();
        return ResponseEntity.ok(ApiResponse.success(brands));
    }

    // PUBLIC - Get brand by ID
    @GetMapping("/brands/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Integer id) {
        BrandResponse brand = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success(brand));
    }

    // ADMIN - Get all brands paginated
    @GetMapping("/admin/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<BrandResponse>>> getBrandsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("brandId").descending());
        Page<BrandResponse> brands = brandService.getAllBrandsAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(brands)));
    }

    // ADMIN - Create brand
    @PostMapping("/admin/brands")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody CreateBrandRequest request) {
        BrandResponse brand = brandService.createBrand(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thương hiệu thành công", brand));
    }

    // ADMIN - Update brand
    @PutMapping("/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateBrandRequest request) {
        BrandResponse brand = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công", brand));
    }

    // ADMIN - Delete brand
    @DeleteMapping("/admin/brands/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteBrand(@PathVariable Integer id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thương hiệu thành công", null));
    }
}
