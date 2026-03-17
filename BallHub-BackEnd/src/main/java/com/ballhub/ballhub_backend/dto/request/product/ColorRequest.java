package com.ballhub.ballhub_backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColorRequest {
    @NotBlank(message = "Tên màu không được để trống")
    @Size(max = 50, message = "Tên màu không được quá 50 ký tự")
    private String colorName;
}
