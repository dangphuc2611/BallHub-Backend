package com.ballhub.ballhub_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OrderID")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AddressID")
    private UserAddress address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PaymentMethodID")
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "StatusID")
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PromotionID")
    private Promotion promotion;

    @Column(name = "SubTotal", precision = 18, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "DiscountAmount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "ShippingFee", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "OrderDate", updatable = false)
    private LocalDateTime orderDate;

    @Column(name = "TotalAmount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    // ✅ THÊM 2 TRƯỜNG MỚI VÀO DATABASE
    @Column(name = "customer_cash", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal customerCash = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (shippingFee == null) shippingFee = BigDecimal.ZERO;
        if (customerCash == null) customerCash = BigDecimal.ZERO; // Khởi tạo mặc định
        if (changeAmount == null) changeAmount = BigDecimal.ZERO; // Khởi tạo mặc định
    }

    public void updateStatus(OrderStatus newStatus, String note) {
        this.status = newStatus;
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(this)
                .status(newStatus)
                .changedAt(LocalDateTime.now())
                .note(note)
                .build();
        statusHistory.add(history);
    }

    public void calculateTotalAmount() {
        if (this.items == null || this.items.isEmpty()) {
            this.subTotal = BigDecimal.ZERO;
        } else {
            this.subTotal = this.items.stream()
                    .map(item -> {
                        BigDecimal price = item.getFinalPrice() != null ? item.getFinalPrice() : BigDecimal.ZERO;
                        return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal discount = (this.discountAmount != null) ? this.discountAmount : BigDecimal.ZERO;
        BigDecimal ship = (this.shippingFee != null) ? this.shippingFee : BigDecimal.ZERO;

        BigDecimal total = this.subTotal.subtract(discount).add(ship);
        this.totalAmount = total.compareTo(BigDecimal.ZERO) > 0 ? total : BigDecimal.ZERO;
    }
}