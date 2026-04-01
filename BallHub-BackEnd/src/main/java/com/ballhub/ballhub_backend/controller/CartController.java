package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.cart.CartResponse;
import com.ballhub.ballhub_backend.dto.request.cart.AddToCartRequest;
import com.ballhub.ballhub_backend.dto.request.cart.UpdateCartItemRequest;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import com.ballhub.ballhub_backend.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
        Integer userId = getUserId(authentication);
        CartResponse cart = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        CartResponse cart = cartService.addToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào giỏ hàng", cart));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        CartResponse cart = cartService.updateCartItem(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật giỏ hàng", cart));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        CartResponse cart = cartService.removeFromCart(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi giỏ hàng", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> clearCart(Authentication authentication) {
        Integer userId = getUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
    }

    private Integer getUserId(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.ballhub.ballhub_backend.exception.UnauthorizedException(
                    "Vui lòng đăng nhập để thực hiện chức năng này");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}
