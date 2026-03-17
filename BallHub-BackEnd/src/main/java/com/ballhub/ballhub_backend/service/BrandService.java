package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.reponse.brand.BrandResponse;
import com.ballhub.ballhub_backend.dto.request.brand.CreateBrandRequest;
import com.ballhub.ballhub_backend.dto.request.brand.UpdateBrandRequest;
import com.ballhub.ballhub_backend.entity.Brand;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findByStatusTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<BrandResponse> getAllBrandsAdmin(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Integer id) {
        Brand brand = brandRepository.findByBrandIdAndStatusTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại"));
        return mapToResponse(brand);
    }

    public BrandResponse createBrand(CreateBrandRequest request) {
        // Check duplicate name
        if (brandRepository.existsByBrandName(request.getBrandName())) {
            throw new BadRequestException("Tên thương hiệu đã tồn tại");
        }

        Brand brand = Brand.builder()
                .brandName(request.getBrandName())
                .description(request.getDescription())
                .logo(request.getLogo())
                .status(true)
                .build();

        Brand saved = brandRepository.save(brand);
        return mapToResponse(saved);
    }

    public BrandResponse updateBrand(Integer id, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại"));

        // Check duplicate name
        Brand existingBrand = brandRepository.findByBrandName(request.getBrandName()).orElse(null);
        if (existingBrand != null && !existingBrand.getBrandId().equals(id)) {
            throw new BadRequestException("Tên thương hiệu đã tồn tại");
        }

        brand.setBrandName(request.getBrandName());
        brand.setDescription(request.getDescription());
        brand.setLogo(request.getLogo());

        if (request.getStatus() != null) {
            brand.setStatus(request.getStatus());
        }

        Brand updated = brandRepository.save(brand);
        return mapToResponse(updated);
    }

    public void deleteBrand(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu không tồn tại"));

        // Check if brand is used by any products
        if (!brand.getProducts().isEmpty()) {
            throw new BadRequestException("Không thể xóa thương hiệu này vì đang có sản phẩm thuộc thương hiệu này");
        }

        brandRepository.delete(brand);
    }

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .brandId(brand.getBrandId())
                .brandName(brand.getBrandName())
                .description(brand.getDescription())
                .logo(brand.getLogo())
                .status(brand.getStatus())
                .build();
    }
}
