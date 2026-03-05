package com.ballhub.ballhub_backend.dto.reponse.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO để hiển thị sản phẩm bán chạy nhất
 * Dùng trong API thống kê admin dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesDTO {

  private Integer productId; // ID sản phẩm
  private String productName; // Tên sản phẩm
  private Long quantitySold; // Tổng số lượng đã bán
  private BigDecimal totalRevenue; // Tổng doanh thu từ sản phẩm này
}
