package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.category.CategoryResponse;
import com.ballhub.ballhub_backend.dto.request.category.CreateCategoryRequest;
import com.ballhub.ballhub_backend.dto.request.category.UpdateCategoryRequest;
import com.ballhub.ballhub_backend.entity.Category;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByStatusTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findByCategoryIdAndStatusTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
        return mapToResponse(category);
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Check duplicate name
        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setStatus(true);

        // Set parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục cha không tồn tại"));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    public CategoryResponse updateCategory(Integer id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        // Check duplicate name (exclude current category)
        Category existingCategory = categoryRepository.findByCategoryName(request.getCategoryName()).orElse(null);
        if (existingCategory != null && !existingCategory.getCategoryId().equals(id)) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        category.setCategoryName(request.getCategoryName());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Danh mục không thể là cha của chính nó");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục cha không tồn tại"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        // Soft delete
        category.setStatus(false);
        categoryRepository.save(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .parentName(category.getParent() != null ? category.getParent().getCategoryName() : null)
                .status(category.getStatus())
                .build();
    }
}
