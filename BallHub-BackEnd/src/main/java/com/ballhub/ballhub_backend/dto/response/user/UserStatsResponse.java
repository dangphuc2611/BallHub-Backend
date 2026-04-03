package com.ballhub.ballhub_backend.dto.response.user;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class UserStatsResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private Boolean status;
    private String avatar;

    // Thống kê thấu hiểu khách hàng
    private int totalOrders;           // Tổng số đơn đã đặt
    private int successfulOrders;      // Đơn thành công (Đã nhận)
    private int canceledOrders;        // Đơn hủy / Bom hàng
    private BigDecimal totalSpent;     // Tổng tiền đã cúng cho shop
    private double cancelRate;         // Tỷ lệ hủy (%)
}