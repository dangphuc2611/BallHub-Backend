package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.reponse.cart.CartItemResponse;
import com.ballhub.ballhub_backend.dto.reponse.cart.CartResponse;
import com.ballhub.ballhub_backend.dto.request.cart.AddToCartRequest;
import com.ballhub.ballhub_backend.dto.request.cart.UpdateCartItemRequest;
import com.ballhub.ballhub_backend.entity.Cart;
import com.ballhub.ballhub_backend.entity.CartItem;
import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.CartItemRepository;
import com.ballhub.ballhub_backend.repository.CartRepository;
import com.ballhub.ballhub_backend.repository.ProductRepository;
import com.ballhub.ballhub_backend.repository.ProductVariantRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private UserRepository userRepository;

    // --- IMPORT THÊM PRODUCT REPOSITORY ĐỂ CHECK FLASH SALE ---
    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    public CartResponse addToCart(Integer userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = variantRepository.findByVariantIdAndStatusTrue(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

        int currentQuantityInCart = 0;
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                if (item.getVariant().getVariantId().equals(variant.getVariantId())) {
                    currentQuantityInCart = item.getQuantity();
                    break;
                }
            }
        }

        int totalRequestedQuantity = currentQuantityInCart + request.getQuantity();

        if (totalRequestedQuantity > variant.getStockQuantity()) {
            if (currentQuantityInCart > 0) {
                throw new BadRequestException("Bạn đã có " + currentQuantityInCart + " sản phẩm này trong giỏ. Chỉ có thể thêm tối đa " + (variant.getStockQuantity() - currentQuantityInCart) + " sản phẩm nữa!");
            } else {
                throw new BadRequestException("Không đủ tồn kho. Kho chỉ còn: " + variant.getStockQuantity());
            }
        }

        cart.addItem(variant, request.getQuantity());
        Cart savedCart = cartRepository.save(cart);

        return mapToResponse(savedCart);
    }

    public CartResponse updateCartItem(Integer userId, Integer cartItemId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item không tồn tại"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new BadRequestException("Cart item không thuộc về giỏ hàng của bạn");
        }

        if (!cartItem.getVariant().hasStock(request.getQuantity())) {
            throw new BadRequestException("Không đủ tồn kho. Còn lại: " + cartItem.getVariant().getStockQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return mapToResponse(cart);
    }

    public CartResponse removeFromCart(Integer userId, Integer cartItemId) {
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item không tồn tại"));

        if (!cartItem.getCart().getCartId().equals(cart.getCartId())) {
            throw new BadRequestException("Cart item không thuộc về giỏ hàng của bạn");
        }

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        return mapToResponse(cart);
    }

    public void clearCart(Integer userId) {
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        cart.clearCart();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUserUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();

                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItem> items = cart.getItems();
        if (items == null) {
            items = new java.util.ArrayList<>();
        }

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());

        // --- TÍNH TỔNG TIỀN DỰA TRÊN THÀNH TIỀN ĐÃ SALE (Thay vì lấy cart.getTotalAmount() cũ) ---
        BigDecimal dynamicTotalAmount = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUser().getUserId())
                .items(itemResponses)
                .totalItems(cart.getTotalItems())
                .totalAmount(dynamicTotalAmount)
                .build();
    }

    private CartItemResponse mapToItemResponse(CartItem item) {
        ProductVariant variant = item.getVariant();
        String imageUrl = null;

        if (variant.getProduct() != null && variant.getProduct().getImages() != null) {
            imageUrl = variant.getProduct().getImages().stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(null);
        }

        // --- ÁP DỤNG LOGIC FLASH SALE ---
        BigDecimal basePrice = variant.getPrice();
        BigDecimal dynamicFinalPrice = variant.getFinalPrice();

        Integer activePercent = null;
        if (variant.getProduct() != null) {
            activePercent = productRepository.findActiveFlashSalePercentByProductId(variant.getProduct().getProductId());
        }
        int discountPct = activePercent != null ? activePercent : 0;

        if (discountPct > 0) {
            BigDecimal multiplier = BigDecimal.valueOf(100 - discountPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            dynamicFinalPrice = basePrice.multiply(multiplier);
        }

        BigDecimal dynamicSubtotal = dynamicFinalPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        // ---------------------------------

        return CartItemResponse.builder()
                .cartItemId(item.getCartItemId())
                .variantId(variant.getVariantId())
                .productName(variant.getProduct() != null ? variant.getProduct().getProductName() : null)
                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                .colorName(variant.getColor() != null ? variant.getColor().getColorName() : null)
                .price(basePrice)
                .finalPrice(dynamicFinalPrice) // Giá 1 sản phẩm đã trừ Sale
                .quantity(item.getQuantity())
                .subtotal(dynamicSubtotal)     // Thành tiền đã trừ Sale
                .imageUrl(imageUrl)
                .stockQuantity(variant.getStockQuantity())
                .build();
    }
}