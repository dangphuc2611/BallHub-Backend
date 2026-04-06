package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.reponse.product.StyleResponse;
import com.ballhub.ballhub_backend.dto.request.product.StyleRequest;
import com.ballhub.ballhub_backend.entity.Style;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.StyleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StyleService {

    private final StyleRepository styleRepository;

    @Transactional(readOnly = true)
    public Page<StyleResponse> getAllStyles(Pageable pageable) {
        return styleRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<StyleResponse> getAllStylesList() {
        return styleRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public StyleResponse getStyleById(Integer id) {
        Style style = styleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kiểu dáng không tồn tại"));
        return mapToResponse(style);
    }

    public StyleResponse createStyle(StyleRequest request) {
        Style style = Style.builder()
                .styleName(request.getStyleName())
                .build();
        return mapToResponse(styleRepository.save(style));
    }

    public StyleResponse updateStyle(Integer id, StyleRequest request) {
        Style style = styleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kiểu dáng không tồn tại"));
        style.setStyleName(request.getStyleName());
        return mapToResponse(styleRepository.save(style));
    }

    public void deleteStyle(Integer id) {
        if (!styleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kiểu dáng không tồn tại");
        }
        styleRepository.deleteById(id);
    }

    private StyleResponse mapToResponse(Style style) {
        return StyleResponse.builder()
                .styleId(style.getStyleId())
                .styleName(style.getStyleName())
                .build();
    }
}
