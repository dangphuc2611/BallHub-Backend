package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.dto.response.product.ColorResponse;
import com.ballhub.ballhub_backend.dto.request.product.ColorRequest;
import com.ballhub.ballhub_backend.service.ColorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/colors")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ColorAdminController {

    private final ColorService colorService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ColorResponse>>> getAllColors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("colorId").descending());
        Page<ColorResponse> result = colorService.getAllColors(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ColorResponse>> getColorById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(colorService.getColorById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ColorResponse>> createColor(@Valid @RequestBody ColorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thêm màu thành công", colorService.createColor(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ColorResponse>> updateColor(
            @PathVariable Integer id,
            @Valid @RequestBody ColorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật màu thành công", colorService.updateColor(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteColor(@PathVariable Integer id) {
        colorService.deleteColor(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa màu thành công", null));
    }
}
