package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.reponse.ApiResponse;
import com.ballhub.ballhub_backend.dto.reponse.PageResponse;
import com.ballhub.ballhub_backend.dto.reponse.product.StyleResponse;
import com.ballhub.ballhub_backend.dto.request.product.StyleRequest;
import com.ballhub.ballhub_backend.service.StyleService;
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
public class StyleAdminController {

    private final StyleService styleService;

    @GetMapping("/styles")
    public ResponseEntity<ApiResponse<List<StyleResponse>>> getAllStylesPublic() {
        return ResponseEntity.ok(ApiResponse.success(styleService.getAllStylesList()));
    }

    @GetMapping("/admin/styles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<StyleResponse>>> getAllStyles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("styleId").descending());
        Page<StyleResponse> result = styleService.getAllStyles(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result)));
    }

    @GetMapping("/admin/styles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StyleResponse>> getStyleById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(styleService.getStyleById(id)));
    }

    @PostMapping("/admin/styles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StyleResponse>> createStyle(@Valid @RequestBody StyleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thêm kiểu dáng thành công", styleService.createStyle(request)));
    }

    @PutMapping("/admin/styles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StyleResponse>> updateStyle(
            @PathVariable Integer id,
            @Valid @RequestBody StyleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kiểu dáng thành công", styleService.updateStyle(id, request)));
    }

    @DeleteMapping("/admin/styles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStyle(@PathVariable Integer id) {
        styleService.deleteStyle(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa kiểu dáng thành công", null));
    }
}
