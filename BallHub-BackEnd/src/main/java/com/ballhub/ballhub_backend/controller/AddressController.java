package com.ballhub.ballhub_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.user.AddressResponse;
import com.ballhub.ballhub_backend.dto.request.user.CreateAddressRequest;
import com.ballhub.ballhub_backend.dto.request.user.UpdateAddressRequest;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import com.ballhub.ballhub_backend.service.AddressService;

import jakarta.validation.Valid;

@RestController
// Đổi từ /api/users/me/addresses thành /api/addresses để linh hoạt hơn
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    // Các hàm cũ của khách hàng: Cần thêm chữ "/me" vào trước
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(Authentication authentication) {
        Integer userId = getUserId(authentication);
        List<AddressResponse> addresses = addressService.getMyAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Valid @RequestBody CreateAddressRequest request,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        AddressResponse address = addressService.createAddress(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo địa chỉ thành công", address));
    }

    @PutMapping("/me/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAddressRequest request,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        AddressResponse address = addressService.updateAddress(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa chỉ thành công", address));
    }

    @DeleteMapping("/me/{id}")
    public ResponseEntity<ApiResponse<?>> deleteAddress(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        addressService.deleteAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }

    @PutMapping("/me/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        AddressResponse address = addressService.setDefaultAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã đặt làm địa chỉ mặc định", address));
    }

    private Integer getUserId(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.ballhub.ballhub_backend.exception.UnauthorizedException(
                    "Vui lòng đăng nhập để thực hiện chức năng này");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }

    // ==========================================
    // API ADMIN: Lấy danh sách địa chỉ của 1 User bất kỳ
    // ==========================================
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddressesByUserId(@PathVariable Integer userId) {
        // Có thể tái sử dụng luôn hàm getMyAddresses vì bản chất nó chỉ cần truyền ID vào
        List<AddressResponse> addresses = addressService.getMyAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }
}