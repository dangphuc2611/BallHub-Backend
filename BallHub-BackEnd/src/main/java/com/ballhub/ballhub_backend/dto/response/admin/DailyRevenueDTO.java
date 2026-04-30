package com.ballhub.ballhub_backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRevenueDTO {
    private LocalDate date;
    private BigDecimal revenue;

    // Thêm constructor để xử lý kiểu dữ liệu từ JPQL (java.sql.Date)
    public DailyRevenueDTO(java.sql.Date date, BigDecimal revenue) {
        this.date = date.toLocalDate();
        this.revenue = revenue;
    }

    // Constructor phụ cho java.util.Date nếu cần
    public DailyRevenueDTO(java.util.Date date, BigDecimal revenue) {
        this.date = new java.sql.Date(date.getTime()).toLocalDate();
        this.revenue = revenue;
    }
}
