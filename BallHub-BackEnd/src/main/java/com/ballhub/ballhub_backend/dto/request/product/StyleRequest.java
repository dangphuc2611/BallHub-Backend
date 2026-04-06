package com.ballhub.ballhub_backend.dto.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleRequest {
    @NotBlank(message = "Tên kiểu dáng không được để trống")
    private String styleName;
}
