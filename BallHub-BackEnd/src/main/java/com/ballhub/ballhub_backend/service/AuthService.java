package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.auth.AuthResponse;
import com.ballhub.ballhub_backend.dto.response.auth.UserResponse;
import com.ballhub.ballhub_backend.dto.request.auth.LoginRequest;
import com.ballhub.ballhub_backend.dto.request.auth.RefreshTokenRequest;
import com.ballhub.ballhub_backend.dto.request.auth.RegisterRequest;
import com.ballhub.ballhub_backend.entity.OtpToken;
import com.ballhub.ballhub_backend.entity.RefreshToken;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.exception.UnauthorizedException;
import com.ballhub.ballhub_backend.repository.OtpTokenRepository;
import com.ballhub.ballhub_backend.repository.RefreshTokenRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import com.ballhub.ballhub_backend.security.JwtTokenProvider;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private OtpTokenRepository otpRepository;

    // --- CÁC PHƯƠNG THỨC GIỮ NGUYÊN ---

    public AuthResponse register(RegisterRequest request) throws BadRequestException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role("CUSTOMER")
                .status(true)
                .build();

        User savedUser = userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        saveRefreshToken(savedUser, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public AuthResponse googleLogin(String googleAccessToken) {
        // 1. Lấy thông tin user từ Google API
        Map<String, Object> googleUserInfo = fetchGoogleUserInfo(googleAccessToken);
        String email = (String) googleUserInfo.get("email");
        String name = (String) googleUserInfo.get("name");

        // 2. Tìm hoặc Tạo mới User trong hệ thống
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .fullName(name)
                            .email(email)
                            .passwordHash(null)
                            .role("CUSTOMER")
                            .status(true)
                            .build();
                    return userRepository.save(newUser);
                });

        // 3. Tạo Authentication thủ công (vì không dùng password)
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        // Lưu vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. Sinh Token hệ thống (BallHub JWT)
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // 5. Lưu Refresh Token vào DB
        saveRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    private Map<String, Object> fetchGoogleUserInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return (Map<String, Object>) response.getBody();
        } catch (Exception e) {
            throw new UnauthorizedException("Xác thực với Google thất bại hoặc Token hết hạn");
        }
    }

    // --- CÁC PHƯƠNG THỨC HỖ TRỢ KHÁC GIỮ NGUYÊN ---

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token không hợp lệ");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại hoặc đã bị thu hồi"));

        if (storedToken.isExpired()) {
            throw new UnauthorizedException("Refresh token đã hết hạn");
        }

        User user = storedToken.getUser();
        CustomUserDetails userDetails = CustomUserDetails.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = tokenProvider.generateAccessToken(authentication);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại"));

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiredAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // HÀM 1: TẠO VÀ GỬI OTP
    public void processForgotPassword(String email) {
        // 1. Kiểm tra email có tồn tại trong DB không
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // 2. Tạo mã OTP ngẫu nhiên 6 số
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 3. Lưu vào Database (Thời hạn 5 phút)
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setOtpCode(otp);
        otpToken.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpToken);

        // 4. Gửi Email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã xác nhận đặt lại mật khẩu - BallHub");
        message.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Mã OTP để đặt lại mật khẩu của bạn là: " + otp + "\n"
                + "Mã này sẽ hết hạn sau 5 phút. Vui lòng không chia sẻ cho người khác.\n\n"
                + "Trân trọng,\nĐội ngũ BallHub");
        mailSender.send(message);
    }

    // HÀM 2: KIỂM TRA OTP VÀ ĐỔI MẬT KHẨU
    public void processResetPassword(String email, String otp, String newPassword) {
        // 1. Tìm mã OTP trong DB
        OtpToken otpToken = otpRepository.findByEmailAndOtpCode(email, otp)
                .orElseThrow(() -> new RuntimeException("Mã OTP không chính xác!"));

        // 2. Kiểm tra hết hạn
        if (otpToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn!");
        }

        // 3. Tìm User và Đổi mật khẩu
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 4. Xóa mã OTP sau khi dùng xong
        otpRepository.delete(otpToken);
    }
}