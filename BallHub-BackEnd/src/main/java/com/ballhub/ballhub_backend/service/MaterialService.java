package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.product.MaterialResponse;
import com.ballhub.ballhub_backend.dto.request.product.MaterialRequest;
import com.ballhub.ballhub_backend.entity.Material;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialService {

    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public Page<MaterialResponse> getAllMaterials(Pageable pageable) {
        return materialRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getAllMaterialsList() {
        return materialRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse getMaterialById(Integer id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chất liệu không tồn tại"));
        return mapToResponse(material);
    }

    public MaterialResponse createMaterial(MaterialRequest request) {
        Material material = Material.builder()
                .materialName(request.getMaterialName())
                .build();
        return mapToResponse(materialRepository.save(material));
    }

    public MaterialResponse updateMaterial(Integer id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chất liệu không tồn tại"));
        material.setMaterialName(request.getMaterialName());
        return mapToResponse(materialRepository.save(material));
    }

    public void deleteMaterial(Integer id) {
        if (!materialRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chất liệu không tồn tại");
        }
        materialRepository.deleteById(id);
    }

    private MaterialResponse mapToResponse(Material material) {
        return MaterialResponse.builder()
                .materialId(material.getMaterialId())
                .materialName(material.getMaterialName())
                .build();
    }
}
