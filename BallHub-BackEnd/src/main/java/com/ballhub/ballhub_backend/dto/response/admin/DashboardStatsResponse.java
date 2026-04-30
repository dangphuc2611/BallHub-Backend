package com.ballhub.ballhub_backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO tổng hợp tất cả thông tin thống kê dashboard
 * Endpoint: GET /api/admin/stats/dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

  private Long totalOrders; // Tổng số đơn hàng trong hệ thống
  private BigDecimal totalRevenue; // Tổng doanh thu (từ đơn hàng đã giao)
  private Long totalCustomers; // Tổng số khách hàng đã đăng ký
  private List<ProductSalesDTO> topProducts; // Top 10 sản phẩm bán chạy nhất
  private List<DailyRevenueDTO> dailyRevenue; // Doanh thu 7 ngày qua
}
