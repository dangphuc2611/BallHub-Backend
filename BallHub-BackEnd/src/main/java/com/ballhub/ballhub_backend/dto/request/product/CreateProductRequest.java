package com.ballhub.ballhub_backend.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm không quá 200 ký tự")
    private String productName;

    private String description;

    @NotNull(message = "Category không được để trống")
    private Integer categoryId;

    @NotNull(message = "Brand không được để trống")
    private Integer brandId;

    // Optional — Chất liệu và Kiểu dáng
    private Integer materialId;
    private Integer styleId;

    @NotEmpty(message = "Phải có ít nhất 1 variant")
    @Valid
    private List<CreateVariantRequest> variants;
}
