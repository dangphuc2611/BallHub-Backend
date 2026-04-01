package com.ballhub.ballhub_backend.dto.response.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Integer categoryId;
    private String categoryName;
    private Integer parentId;
    private String parentName;
    private Boolean status;
    private List<CategoryResponse> children;
}
