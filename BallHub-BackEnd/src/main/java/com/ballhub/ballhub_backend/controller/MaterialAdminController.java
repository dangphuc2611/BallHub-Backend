package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.reponse.ApiResponse;
import com.ballhub.ballhub_backend.dto.reponse.PageResponse;
import com.ballhub.ballhub_backend.dto.reponse.product.MaterialResponse;
import com.ballhub.ballhub_backend.dto.request.product.MaterialRequest;
import com.ballhub.ballhub_backend.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MaterialAdminController {

    private final MaterialService materialService;

    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getAllMaterialsPublic() {
        return ResponseEntity.ok(ApiResponse.success(materialService.getAllMaterialsList()));
    }

    @GetMapping("/admin/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<MaterialResponse>>> getAllMaterials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("materialId").descending());
        Page<MaterialResponse> result = materialService.getAllMaterials(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/admin/materials/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaterialResponse>> getMaterialById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.getMaterialById(id)));
    }

    @PostMapping("/admin/materials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaterialResponse>> createMaterial(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thêm chất liệu thành công", materialService.createMaterial(request)));
    }

    @PutMapping("/admin/materials/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaterialResponse>> updateMaterial(
            @PathVariable Integer id,
            @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chất liệu thành công", materialService.updateMaterial(id, request)));
    }

    @DeleteMapping("/admin/materials/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(@PathVariable Integer id) {
        materialService.deleteMaterial(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa chất liệu thành công", null));
    }
}
