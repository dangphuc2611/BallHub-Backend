package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.product.ColorResponse;
import com.ballhub.ballhub_backend.dto.request.product.ColorRequest;
import com.ballhub.ballhub_backend.entity.Color;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ColorService {

    private final ColorRepository colorRepository;

    @Transactional(readOnly = true)
    public Page<ColorResponse> getAllColors(Pageable pageable) {
        return colorRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ColorResponse getColorById(Integer id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Màu không tồn tại"));
        return mapToResponse(color);
    }

    @Transactional
    public ColorResponse createColor(ColorRequest request) {
        if (colorRepository.existsByColorName(request.getColorName())) {
            throw new BadRequestException("Tên màu đã tồn tại");
        }

        Color color = Color.builder()
                .colorName(request.getColorName())
                .build();

        return mapToResponse(colorRepository.save(color));
    }

    @Transactional
    public ColorResponse updateColor(Integer id, ColorRequest request) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Màu không tồn tại"));

        if (colorRepository.existsByColorName(request.getColorName()) && 
            !color.getColorName().equals(request.getColorName())) {
            throw new BadRequestException("Tên màu đã tồn tại");
        }

        color.setColorName(request.getColorName());
        return mapToResponse(colorRepository.save(color));
    }

    @Transactional
    public void deleteColor(Integer id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Màu không tồn tại"));
        
        // Kiểm tra xem có sản phẩm nào đang dùng màu này không
        if (color.getVariants() != null && !color.getVariants().isEmpty()) {
            throw new BadRequestException("Không thể xóa màu này vì đang có sản phẩm sử dụng");
        }

        colorRepository.delete(color);
    }

    private ColorResponse mapToResponse(Color color) {
        return ColorResponse.builder()
                .colorId(color.getColorId())
                .colorName(color.getColorName())
                .build();
    }
}
