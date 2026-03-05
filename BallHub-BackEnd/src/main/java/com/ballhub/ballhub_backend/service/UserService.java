package com.ballhub.ballhub_backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ballhub.ballhub_backend.dto.reponse.user.UserResponse;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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
            // Khai báo thư mục lưu ảnh (nằm ngay ngoài thư mục gốc của project Spring Boot)
            String uploadDir = "uploads/avatars/";
            Path uploadPath = Paths.get(uploadDir);

            // Nếu thư mục chưa tồn tại thì tự động tạo mới
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file ngẫu nhiên (UUID) để không bao giờ bị trùng tên
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;

            // Copy file từ request vào thư mục
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Tạo đường dẫn tương đối để lưu vào Database
            String avatarUrl = "/uploads/avatars/" + fileName;

            // Lưu vào DB
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
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .avatar(user.getAvatar())
                        .role(user.getRole())
                        .build())
                .collect(Collectors.toList());
    }

}
