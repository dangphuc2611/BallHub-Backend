package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.product.ColorResponse;
import com.ballhub.ballhub_backend.dto.response.product.SizeResponse;
import com.ballhub.ballhub_backend.repository.ColorRepository;
import com.ballhub.ballhub_backend.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SizeColorController {

  private final SizeRepository sizeRepository;
  private final ColorRepository colorRepository;

  @GetMapping("/sizes")
  public ResponseEntity<ApiResponse<List<SizeResponse>>> getAllSizes() {
    List<SizeResponse> sizes = sizeRepository.findAll().stream()
        .map(s -> SizeResponse.builder()
            .sizeId(s.getSizeId())
            .sizeName(s.getSizeName())
            .build())
        .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(sizes));
  }

  @GetMapping("/colors")
  public ResponseEntity<ApiResponse<List<ColorResponse>>> getAllColors() {
    List<ColorResponse> colors = colorRepository.findAll().stream()
        .map(c -> ColorResponse.builder()
            .colorId(c.getColorId())
            .colorName(c.getColorName())
            .build())
        .collect(Collectors.toList());
    return ResponseEntity.ok(ApiResponse.success(colors));
  }
}
