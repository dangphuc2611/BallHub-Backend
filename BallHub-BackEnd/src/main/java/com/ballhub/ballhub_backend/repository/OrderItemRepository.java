package com.ballhub.ballhub_backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ballhub.ballhub_backend.dto.response.admin.ProductSalesDTO;
import com.ballhub.ballhub_backend.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findByOrderOrderId(Integer orderId);

    /**
     * Lấy top sản phẩm bán chạy nhất dựa trên số lượng
     * Chỉ tính các đơn hàng đã 'COMPLETED' (Hoàn thành)
     */
    @Query("""
                SELECT new com.ballhub.ballhub_backend.dto.response.admin.ProductSalesDTO(
                    p.productId,
                    p.productName,
                    SUM(oi.quantity),
                    SUM(oi.finalPrice * oi.quantity)
                )
                FROM OrderItem oi
                JOIN oi.variant pv
                JOIN pv.product p
                JOIN oi.order o
                WHERE o.status.statusName = 'COMPLETED' 
                GROUP BY p.productId, p.productName
                ORDER BY SUM(oi.quantity) DESC
            """)
    List<ProductSalesDTO> getTopProductsByQuantity(Pageable pageable);
}