package com.ballhub.ballhub_backend.service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ballhub.ballhub_backend.dto.response.user.UserStatsResponse;
import com.ballhub.ballhub_backend.entity.Order;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ballhub.ballhub_backend.dto.response.user.UserResponse;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ ĐÃ THÊM: Tiêm EmailService để dùng cho hàm gửi mật khẩu
    @Autowired
    private EmailService emailService;

    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndStatusTrue(email)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại hoặc đã bị khóa"));
    }

    // 1. Hàm cập nhật Tên và SĐT
    public void updateProfile(String email, String fullName, String phone) {
        User user = getUserByEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        userRepository.save(user);
    }

    // 2. Hàm lưu Avatar vào thư mục local
    public String updateAvatar(String email, MultipartFile file) {
        User user = getUserByEmail(email);
        try {
            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/uploads/avatars/" + fileName;

            user.setAvatar(avatarUrl);
            userRepository.save(user);

            return avatarUrl;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu ảnh: " + e.getMessage());
        }
    }

    // 3. Lấy danh sách tất cả user
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // 4. API DÀNH CHO ADMIN/POS: Tìm kiếm khách hàng
    public List<UserResponse> searchUsers(String keyword) {
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String searchParam = "%" + safeKeyword + "%";

        List<User> users = userRepository.searchByKeyword(searchParam);

        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ==========================================
    // NHÓM QUẢN LÝ (ADMIN)
    // ==========================================

    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("Không thể khóa tài khoản Quản trị viên!");
        }

        user.setStatus(!Boolean.TRUE.equals(user.getStatus()));
        userRepository.save(user);
    }

    @Transactional
    public void changeUserRole(Integer userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setRole(newRole.toUpperCase());
        userRepository.save(user);
    }

    @Transactional
    public void resetUserPassword(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        String newPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), newPassword);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<Order> orders = orderRepository.findByUserUserId(userId);

        int totalOrders = orders.size();
        int success = 0;
        int canceled = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;

        // Sửa lại đoạn vòng lặp trong getUserStats
        for (Order order : orders) {
            // Thêm dòng if chống null này vào:
            if (order.getStatus() != null && order.getStatus().getStatusName() != null) {
                String status = order.getStatus().getStatusName().toUpperCase();
                if (status.contains("COMPLETED") || status.contains("HOÀN THÀNH") || status.contains("DELIVERED")) {
                    success++;
                    totalSpent = totalSpent.add(order.getTotalAmount());
                } else if (status.contains("CANCELED") || status.contains("HỦY")) {
                    canceled++;
                }
            }
        }

        double cancelRate = totalOrders > 0 ? ((double) canceled / totalOrders) * 100 : 0;

        return UserStatsResponse.builder()
                .userId(user.getUserId()) // ✅ ĐÃ SỬA: getUserID -> getUserId
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .avatar(user.getAvatar())
                .totalOrders(totalOrders)
                .successfulOrders(success)
                .canceledOrders(canceled)
                .totalSpent(totalSpent)
                .cancelRate(Math.round(cancelRate * 100.0) / 100.0)
                .build();
    }

    // 5. XÓA VĨNH VIỄN TÀI KHOẢN (HARD DELETE)
    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("Không thể xóa tài khoản Quản trị viên!");
        }

        try {
            userRepository.delete(user);
            // ✅ THÊM DÒNG FLUSH NÀY: Ép hệ thống phi lệnh SQL xuống DB ngay lập tức
            userRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // ✅ SỬA LẠI TÊN LỖI: Bắt đúng cái lỗi ràng buộc khóa ngoại (Foreign Key)
            throw new BadRequestException("Không thể xóa! Khách hàng này đã có dữ liệu đơn hàng hoặc địa chỉ trên hệ thống. Hãy dùng tính năng Khóa.");
        }
    }
}