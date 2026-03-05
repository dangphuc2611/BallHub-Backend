package com.ballhub.ballhub_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ballhub.ballhub_backend.dto.reponse.order.OrderDetailResponse;
import com.ballhub.ballhub_backend.dto.reponse.order.OrderItemResponse;
import com.ballhub.ballhub_backend.dto.reponse.order.OrderResponse;
import com.ballhub.ballhub_backend.dto.reponse.order.OrderStatusHistoryResponse;
import com.ballhub.ballhub_backend.dto.request.order.CreateOrderRequest;
import com.ballhub.ballhub_backend.entity.Cart;
import com.ballhub.ballhub_backend.entity.CartItem;
import com.ballhub.ballhub_backend.entity.Order;
import com.ballhub.ballhub_backend.entity.OrderItem;
import com.ballhub.ballhub_backend.entity.OrderStatus;
import com.ballhub.ballhub_backend.entity.OrderStatusHistory;
import com.ballhub.ballhub_backend.entity.PaymentMethod;
import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.entity.Promotion;
import com.ballhub.ballhub_backend.entity.UserAddress;
import com.ballhub.ballhub_backend.exception.BadRequestException;
import com.ballhub.ballhub_backend.exception.ResourceNotFoundException;
import com.ballhub.ballhub_backend.repository.CartRepository;
import com.ballhub.ballhub_backend.repository.OrderRepository;
import com.ballhub.ballhub_backend.repository.OrderStatusRepository;
import com.ballhub.ballhub_backend.repository.PaymentMethodRepository;
import com.ballhub.ballhub_backend.repository.ProductVariantRepository;
import com.ballhub.ballhub_backend.repository.PromotionRepository;
import com.ballhub.ballhub_backend.repository.UserAddressRepository;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserAddressRepository addressRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private OrderStatusRepository statusRepository;
    @Autowired
    private ProductVariantRepository variantRepository;
    @Autowired
    private PromotionRepository promotionRepository;

    public OrderDetailResponse createOrder(Integer userId, CreateOrderRequest request) {
        // 1. Lấy giỏ hàng
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        // 2. Kiểm tra địa chỉ
        UserAddress address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));

        // 3. Kiểm tra phương thức thanh toán
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndIsActiveTrue(request.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không hợp lệ"));

        // 4. Kiểm tra Voucher
        Promotion appliedVoucher = null;
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            appliedVoucher = promotionRepository.findByPromoCode(request.getPromoCode())
                    .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));
            if (!appliedVoucher.isValid()) {
                throw new BadRequestException("Mã giảm giá không còn hiệu lực");
            }
        }

        // 5. Khởi tạo đơn hàng trạng thái PENDING
        OrderStatus pendingStatus = statusRepository.findByStatusName("PENDING")
                .orElseThrow(() -> new RuntimeException("Trạng thái PENDING không tồn tại"));

        // ✅ LẤY PHÍ SHIP TỪ FRONTEND (Nếu null thì mặc định = 0)
        BigDecimal shipFee = (request.getShippingFee() != null) ? request.getShippingFee() : BigDecimal.ZERO;

        Order order = Order.builder()
                .user(cart.getUser())
                .address(address)
                .paymentMethod(paymentMethod)
                .status(pendingStatus)
                .promotion(appliedVoucher)
                .shippingFee(shipFee) // ✅ LƯU PHÍ SHIP VÀO ĐƠN HÀNG
                .build();

        Order savedOrder = orderRepository.save(order);

        // 6. XỬ LÝ TỪNG SẢN PHẨM: Tự động áp dụng giảm giá lẻ
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();
            BigDecimal originalPrice = variant.getPrice();

            Promotion itemPromo = promotionRepository.findActivePromotionForVariant(variant.getVariantId())
                    .orElse(null);

            int discountPct = 0;
            if (itemPromo != null && "PERCENT".equals(itemPromo.getDiscountType())) {
                discountPct = itemPromo.getDiscountPercent();
            }

            BigDecimal finalPrice = originalPrice.multiply(BigDecimal.valueOf(100 - discountPct))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .variant(variant)
                    .quantity(cartItem.getQuantity())
                    .originalPrice(originalPrice)
                    .discountPercent(discountPct)
                    .finalPrice(finalPrice)
                    .appliedPromotion(itemPromo)
                    .build();

            savedOrder.getItems().add(orderItem);

            variant.decreaseStock(cartItem.getQuantity());
            variantRepository.save(variant);
        }

        // 7. TÍNH TỔNG TIỀN ĐƠN HÀNG (Bao gồm tiền hàng + phí ship)
        savedOrder.calculateTotalAmount();

        // 8. Áp dụng giảm giá từ Voucher (Nếu có)
        if (appliedVoucher != null) {
            BigDecimal subTotal = savedOrder.getSubTotal();
            if (subTotal.compareTo(appliedVoucher.getMinOrderAmount()) < 0) {
                throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu để dùng Voucher này");
            }

            BigDecimal discountAmt = BigDecimal.ZERO;
            if ("PERCENT".equals(appliedVoucher.getDiscountType())) {
                discountAmt = subTotal.multiply(BigDecimal.valueOf(appliedVoucher.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (appliedVoucher.getMaxDiscountAmount() != null
                        && discountAmt.compareTo(appliedVoucher.getMaxDiscountAmount()) > 0) {
                    discountAmt = appliedVoucher.getMaxDiscountAmount();
                }
            } else {
                discountAmt = appliedVoucher.getMaxDiscountAmount();
            }

            savedOrder.setDiscountAmount(discountAmt);
            savedOrder.calculateTotalAmount(); // Tính lại tổng cuối cùng (đã có ship)

            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            promotionRepository.save(appliedVoucher);
        }

        // 9. Lưu lịch sử và xóa giỏ hàng
        savedOrder.updateStatus(pendingStatus, "Khách hàng đặt đơn thành công");
        cart.clearCart();
        cartRepository.save(cart);

        return mapToDetailResponse(orderRepository.save(savedOrder));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Integer userId, Integer orderId) {
        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
        return mapToDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(Integer userId, Pageable pageable) {
        return orderRepository.findByUserUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    public void cancelOrder(Integer userId, Integer orderId) {
        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        String currentStatus = order.getStatus().getStatusName();
        if (!"PENDING".equals(currentStatus) && !"CONFIRMED".equals(currentStatus)) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái: " + currentStatus);
        }

        OrderStatus cancelledStatus = statusRepository.findByStatusName("CANCELLED")
                .orElseThrow(() -> new RuntimeException("OrderStatus CANCELLED không tồn tại"));

        for (OrderItem item : order.getItems()) {
            item.getVariant().increaseStock(item.getQuantity());
            variantRepository.save(item.getVariant());
        }

        order.updateStatus(cancelledStatus, "Đơn hàng bị hủy bởi khách hàng");
        orderRepository.save(order);
    }

    // ============================================
    // MAPPING METHODS
    // ============================================
    private OrderResponse mapToResponse(Order order) {
        int totalItems = order.getItems() != null
                ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum()
                : 0;

        // ✅ Lấy địa chỉ giao hàng để nhét ra ngoài List Order
        String deliveryAddress = null;
        if (order.getAddress() != null) {
            deliveryAddress = order.getAddress().getFullAddress();
        }

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUser().getUserId())
                .statusName(order.getStatus().getStatusName())
                .orderDate(order.getOrderDate())
                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee()) // ✅ TRẢ VỀ PHÍ SHIP
                .deliveryAddress(deliveryAddress) // ✅ TRẢ VỀ ĐỊA CHỈ GIAO HÀNG
                .totalAmount(order.getTotalAmount())
                .totalItems(totalItems)
                .paymentMethodName(order.getPaymentMethod().getMethodName())
                .build();
    }

    private OrderDetailResponse mapToDetailResponse(Order order) {
        List<OrderItem> items = order.getItems();
        if (items == null) {
            items = new java.util.ArrayList<>();
        }

        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::mapToItemResponse)
                .collect(Collectors.toList());

        List<OrderStatusHistory> history = order.getStatusHistory();
        if (history == null) {
            history = new java.util.ArrayList<>();
        }

        List<OrderStatusHistoryResponse> historyResponses = history.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());

        String promoCodeUsed = null;
        if (order.getPromotion() != null) {
            promoCodeUsed = order.getPromotion().getPromoCode();
        }

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUser().getUserId())
                .userFullName(order.getUser().getFullName())
                .userEmail(order.getUser().getEmail())
                .userPhone(order.getUser().getPhone())
                .deliveryAddress(order.getAddress() != null ? order.getAddress().getFullAddress() : "")
                .paymentMethodName(order.getPaymentMethod().getMethodName())
                .statusName(order.getStatus().getStatusName())
                .orderDate(order.getOrderDate())
                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .promoCode(promoCodeUsed)
                .shippingFee(order.getShippingFee()) // ✅ TRẢ VỀ PHÍ SHIP
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .statusHistory(historyResponses)
                .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem item) {
        ProductVariant variant = item.getVariant();
        String imageUrl = null;

        if (variant.getProduct() != null && variant.getProduct().getImages() != null) {
            imageUrl = variant.getProduct().getImages().stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(null);
        }

        String promotionName = null;
        if (item.getAppliedPromotion() != null) {
            promotionName = item.getAppliedPromotion().getPromotionName();
        }

        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .variantId(variant.getVariantId())
                .productName(variant.getProduct() != null ? variant.getProduct().getProductName() : null)
                .sizeName(variant.getSize() != null ? variant.getSize().getSizeName() : null)
                .colorName(variant.getColor() != null ? variant.getColor().getColorName() : null)
                .quantity(item.getQuantity())
                .originalPrice(item.getOriginalPrice())
                .discountPercent(item.getDiscountPercent())
                .appliedPromotionName(promotionName)
                .finalPrice(item.getFinalPrice())
                .subtotal(item.getSubtotal())
                .imageUrl(imageUrl)
                .build();
    }

    private OrderStatusHistoryResponse mapToHistoryResponse(OrderStatusHistory history) {
        return OrderStatusHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .statusName(history.getStatus().getStatusName())
                .changedAt(history.getChangedAt())
                .note(history.getNote())
                .build();
    }

    // ============================================
    // ADMIN METHODS
    // ============================================

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailAdmin(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));
        return mapToDetailResponse(order);
    }

    public OrderDetailResponse updateOrderStatus(Integer orderId, Integer statusId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        OrderStatus newStatus = statusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("Trạng thái không tồn tại"));

        String currentStatus = order.getStatus().getStatusName();
        String targetStatus = newStatus.getStatusName();

        validateStatusTransition(currentStatus, targetStatus);

        order.updateStatus(newStatus, note != null ? note : "Admin cập nhật trạng thái");
        Order updated = orderRepository.save(order);

        return mapToDetailResponse(updated);
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        switch (currentStatus) {
            case "PENDING":
                if (!"CONFIRMED".equals(targetStatus) && !"CANCELLED".equals(targetStatus)) {
                    throw new BadRequestException(
                            "Không thể chuyển từ PENDING sang " + targetStatus);
                }
                break;
            case "CONFIRMED":
                if (!"SHIPPING".equals(targetStatus) && !"CANCELLED".equals(targetStatus)) {
                    throw new BadRequestException(
                            "Không thể chuyển từ CONFIRMED sang " + targetStatus);
                }
                break;
            case "SHIPPING":
                if (!"DELIVERED".equals(targetStatus)) {
                    throw new BadRequestException(
                            "Không thể chuyển từ SHIPPING sang " + targetStatus + ". Chỉ có thể chuyển sang DELIVERED");
                }
                break;
            case "DELIVERED":
                throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã giao");
            case "CANCELLED":
                throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã hủy");
            default:
                throw new BadRequestException("Trạng thái không hợp lệ: " + currentStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String statusName) {
        return orderRepository.findByStatusStatusName(statusName).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}