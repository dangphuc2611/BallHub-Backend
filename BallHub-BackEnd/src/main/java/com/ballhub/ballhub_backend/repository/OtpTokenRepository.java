package com.ballhub.ballhub_backend.repository;

import com.ballhub.ballhub_backend.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // Tìm mã OTP dựa trên Email và mã Code để xác nhận người dùng nhập đúng hay sai
    Optional<OtpToken> findByEmailAndOtpCode(String email, String otpCode);

    // Xóa OTP cũ của email đó trước khi tạo OTP mới (để dọn dẹp DB)
    void deleteByEmail(String email);
}