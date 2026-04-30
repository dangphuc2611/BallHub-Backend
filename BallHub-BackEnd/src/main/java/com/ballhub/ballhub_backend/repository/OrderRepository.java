package com.ballhub.ballhub_backend.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ballhub.ballhub_backend.dto.response.admin.DailyRevenueDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ballhub.ballhub_backend.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    Page<Order> findByUserUserId(Integer userId, Pageable pageable);

    // ✅ ĐÃ THÊM: Hàm này dùng để lấy danh sách tính thống kê
    List<Order> findByUserUserId(Integer userId);

    Optional<Order> findByOrderIdAndUserUserId(Integer orderId, Integer userId);

    List<Order> findByStatusStatusName(String statusName);

    /**
     * Đếm tổng số lượng đơn hàng trong hệ thống
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status.statusName = 'COMPLETED'")
    Long countTotalOrders();

    /**
     * Tính tổng doanh thu từ các đơn hàng đã DELIVERED
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status.statusName = 'COMPLETED'")
    BigDecimal sumTotalRevenue();

    /**
     * Thống kê doanh thu theo từng ngày trong khoảng thời gian
     */
    @Query("SELECT new com.ballhub.ballhub_backend.dto.response.admin.DailyRevenueDTO(CAST(o.orderDate AS date), SUM(o.totalAmount)) " +
            "FROM Order o " +
            "WHERE o.status.statusName = 'COMPLETED' AND o.orderDate >= :startDate " +
            "GROUP BY CAST(o.orderDate AS date) " +
            "ORDER BY CAST(o.orderDate AS date) ASC")
    List<DailyRevenueDTO> getRevenueByDateRange(@Param("startDate") LocalDateTime startDate);
}