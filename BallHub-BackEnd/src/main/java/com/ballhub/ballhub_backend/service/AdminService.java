package com.ballhub.ballhub_backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ballhub.ballhub_backend.dto.response.admin.DailyRevenueDTO;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ballhub.ballhub_backend.dto.response.admin.DashboardStatsResponse;
import com.ballhub.ballhub_backend.dto.response.admin.ProductSalesDTO;
import com.ballhub.ballhub_backend.repository.OrderItemRepository;
import com.ballhub.ballhub_backend.repository.OrderRepository;
import com.ballhub.ballhub_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý logic thống kê admin dashboard
 * Kết hợp dữ liệu từ OrderRepository, UserRepository, OrderItemRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final OrderItemRepository orderItemRepository;

  /**
   * Lấy tổng số đơn hàng
   */
  public Long getTotalOrders() {
    return orderRepository.countTotalOrders();
  }

  /**
   * Lấy tổng doanh thu từ đơn hàng đã giao
   */
  public BigDecimal getTotalRevenue() {
    return orderRepository.sumTotalRevenue();
  }

  /**
   * Lấy tổng số khách hàng
   */
  public Long getTotalCustomers() {
    return userRepository.countTotalCustomers();
  }

  /**
   * Lấy top N sản phẩm bán chạy nhất
   */
  public List<ProductSalesDTO> getTopProducts(int limit) {
    // Validate limit
    if (limit < 1)
      limit = 10;
    if (limit > 100)
      limit = 100;

    Pageable pageable = PageRequest.of(0, limit);
    return orderItemRepository.getTopProductsByQuantity(pageable);
  }

  /**
   * API chính: Lấy tất cả thống kê dashboard cùng lúc
   * 
   * @param topProductsLimit số lượng sản phẩm top (default 10, max 100)
   */
  public DashboardStatsResponse getDashboardStats(int topProductsLimit) {
    // Validate
    if (topProductsLimit < 1)
      topProductsLimit = 10;
    if (topProductsLimit > 100)
      topProductsLimit = 100;

    // Lấy doanh thu 7 ngày qua
    LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6).with(LocalTime.MIN);
    List<DailyRevenueDTO> rawRevenue = orderRepository.getRevenueByDateRange(sevenDaysAgo);

    // Điền các ngày thiếu bằng 0
    Map<LocalDate, BigDecimal> revenueMap = rawRevenue.stream()
            .collect(Collectors.toMap(DailyRevenueDTO::getDate, DailyRevenueDTO::getRevenue));

    List<DailyRevenueDTO> dailyRevenue = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate date = LocalDate.now().minusDays(i);
      dailyRevenue.add(DailyRevenueDTO.builder()
              .date(date)
              .revenue(revenueMap.getOrDefault(date, BigDecimal.ZERO))
              .build());
    }

    return DashboardStatsResponse.builder()
        .totalOrders(getTotalOrders())
        .totalRevenue(getTotalRevenue())
        .totalCustomers(getTotalCustomers())
        .topProducts(getTopProducts(topProductsLimit))
        .dailyRevenue(dailyRevenue)
        .build();
  }

  /**
   * Overload với default topProductsLimit = 10
   */
  public DashboardStatsResponse getDashboardStats() {
    return getDashboardStats(10);
  }
}
