package com.ballhub.ballhub_backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ballhub.ballhub_backend.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // .authorizeHttpRequests(auth -> auth

                // .requestMatchers(
                // "/img/**")
                // .permitAll()

                // // Auth endpoints
                // .requestMatchers("/api/auth/**").permitAll()
                // .requestMatchers("/api/auth/google-login").permitAll()
                // .requestMatchers("/uploads/**").permitAll()

                // // Public endpoints
                // .requestMatchers("/api/products/**").permitAll()
                // .requestMatchers("/api/categories/**").permitAll()
                // .requestMatchers("/api/brands/**").permitAll()
                // .requestMatchers("/api/test/**").permitAll()
                // .requestMatchers("/api/promotions/**").permitAll()

                // .requestMatchers("/api/users/me").authenticated()
                // .requestMatchers("/api/users/update").authenticated()

                // // Admin endpoints
                // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // .requestMatchers("/api/stats/**").permitAll()

                // // All other endpoints require authentication
                // .anyRequest().authenticated())

                // security disabled: permit all
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// ## 🎯 4. LUỒNG NGHIỆP VỤ ĐẶT HÀNG & THANH TOÁN
//
// ### 4.1 Sequence Diagram - Create Order Flow
// ```
// User → Controller: POST /api/orders + CreateOrderRequest
// Controller → AuthFilter: Verify JWT
// AuthFilter → Controller: userId = 123
//
// Controller → OrderService: createOrder(userId, request)
// OrderService → CartRepository: findByUserId(123)
// CartRepository → OrderService: Cart + CartItems
//
// OrderService → Validation:
// - Cart không rỗng?
// - AddressID hợp lệ?
// - PaymentMethodID hợp lệ?
//
// OrderService → Loop [For each CartItem]:
// - Check variant.stockQuantity >= item.quantity
// - Nếu không đủ → throw InsufficientStockException
//
// OrderService → Transaction BEGIN:
// 1. Tạo Order mới
// 2. Loop [For each CartItem]:
// - Snapshot giá hiện tại (originalPrice, discountPrice)
// - Tính finalPrice
// - Tạo OrderItem
// - Trừ stock: variant.stockQuantity -= quantity
// 3. Tính totalAmount
// 4. Lưu Order
// 5. Tạo OrderStatusHistory (status = PENDING)
// 6. Xóa CartItems
// 7. Commit
//
// OrderService → Controller: OrderResponse
// Controller → User: 201 Created + OrderResponse
