package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.request.user.ChangePasswordRequest;
import com.ballhub.ballhub_backend.dto.response.user.UserStatsResponse;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.user.UserResponse;
import com.ballhub.ballhub_backend.dto.request.user.UpdateProfileRequest;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users") // ✅ Đã thêm dấu / cho chuẩn chuẩn mực
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

    @GetMapping("/admin/search")
    public ResponseEntity<?> searchUsersForPos(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        try {
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

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Integer userId = userDetails.getUserId();

        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    // ==========================================
    // CÁC HÀM ADMIN (Đã fix đường dẫn tránh bị lặp chữ "users")
    // ==========================================

    @PutMapping("/admin/{id}/toggle-status")
    public ResponseEntity<ApiResponse<String>> toggleStatus(@PathVariable Integer id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Đã thay đổi trạng thái tài khoản!", null));
    }

    @PutMapping("/admin/{id}/change-role")
    public ResponseEntity<ApiResponse<String>> changeRole(@PathVariable Integer id, @RequestParam String role) {
        userService.changeUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật quyền thành công!", null));
    }

    @PostMapping("/admin/{id}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@PathVariable Integer id) {
        userService.resetUserPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi mật khẩu mới vào email của khách hàng!", null));
    }

    @GetMapping("/admin/{id}/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(@PathVariable Integer id) {
        UserStatsResponse stats = userService.getUserStats(id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa vĩnh viễn tài khoản thành công!", null));
    }
}