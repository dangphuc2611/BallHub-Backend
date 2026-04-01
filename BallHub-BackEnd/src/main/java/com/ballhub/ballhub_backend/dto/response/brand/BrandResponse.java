package com.ballhub.ballhub_backend.dto.response.brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandResponse {

    private Integer brandId;
    private String brandName;
    private String description;
    private String logo;
    private Boolean status;
}
