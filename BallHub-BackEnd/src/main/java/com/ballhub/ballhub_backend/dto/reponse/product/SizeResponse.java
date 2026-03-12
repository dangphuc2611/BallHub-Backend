package com.ballhub.ballhub_backend.dto.reponse.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeResponse {
  private Integer sizeId;
  private String sizeName;
}
