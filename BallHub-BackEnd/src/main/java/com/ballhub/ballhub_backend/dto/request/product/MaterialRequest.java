package com.ballhub.ballhub_backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequest {
    @NotBlank(message = "Tên chất liệu không được để trống")
    private String materialName;
}
