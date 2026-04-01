package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.product.ProductResponse;
import com.ballhub.ballhub_backend.entity.Favorite;
import com.ballhub.ballhub_backend.entity.Product;
import com.ballhub.ballhub_backend.entity.User;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.FavoriteRepository;
import com.ballhub.ballhub_backend.repository.ProductRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class FavoriteService {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService; // Gọi nhờ hàm map để lấy thiết kế thẻ sản phẩm

    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyFavorites(Integer userId, Pageable pageable) {
        return favoriteRepository.findByUserUserId(userId, pageable)
                .map(favorite -> productService.mapToListResponse(favorite.getProduct()));
    }

    public boolean toggleFavorite(Integer userId, Integer productId) {
        Optional<Favorite> existingFav = favoriteRepository.findByUserUserIdAndProductProductId(userId, productId);

        if (existingFav.isPresent()) {
            // Đã tim -> Hủy tim
            favoriteRepository.delete(existingFav.get());
            return false;
        } else {
            // Chưa tim -> Thêm tim
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));

            Favorite newFav = Favorite.builder()
                    .user(user)
                    .product(product)
                    .build();
            favoriteRepository.save(newFav);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean checkFavorite(Integer userId, Integer productId) {
        return favoriteRepository.existsByUserUserIdAndProductProductId(userId, productId);
    }
}