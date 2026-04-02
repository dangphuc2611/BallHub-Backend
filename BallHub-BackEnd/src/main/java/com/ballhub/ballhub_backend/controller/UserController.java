package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.request.user.ChangePasswordRequest;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.user.UserResponse;
import com.ballhub.ballhub_backend.dto.request.user.UpdateProfileRequest;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private String getAuthEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.ballhub.ballhub_backend.exception.UnauthorizedException(
                    "Vui lòng đăng nhập để thực hiện chức năng này");
        }
        return authentication.getName();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        String email = getAuthEmail();
        try {
            User user = userService.getUserByEmail(email);
            UserResponse userResponse = UserResponse.builder()
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .role(user.getRole())
                    .build();

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy thông tin thành công", userResponse));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ==========================================
    // 1. API CẬP NHẬT TÊN VÀ SĐT
    // ==========================================
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        String email = getAuthEmail();
        try {
            userService.updateProfile(email, request.getFullName(), request.getPhone());
            return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật thông tin thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ==========================================
    // 2. API UPLOAD AVATAR
    // ==========================================
    @PostMapping("/me/avatar")
    public ResponseEntity<?> updateAvatar(@RequestParam("file") MultipartFile file) {
        String email = getAuthEmail();
        try {
            String avatarUrl = userService.updateAvatar(email, file);
            return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật ảnh thành công", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    // ==========================================
    // 3. API ADMIN: Tìm kiếm khách hàng cho POS
    // ==========================================
    @GetMapping("/admin/search")
    public ResponseEntity<?> searchUsersForPos(
            // ✅ ĐÃ SỬA: Thêm required = false và defaultValue = ""
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        try {
            // Lấy danh sách user từ Service
            java.util.List<UserResponse> users = userService.searchUsers(keyword);
            return ResponseEntity.ok(new ApiResponse<>(true, "Tìm kiếm khách hàng thành công", users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<?>> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        // Lấy ID user đang đăng nhập (Giống hệt cách bạn làm ở AddressController)
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Integer userId = userDetails.getUserId();

        // Gọi Service xử lý
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }


}