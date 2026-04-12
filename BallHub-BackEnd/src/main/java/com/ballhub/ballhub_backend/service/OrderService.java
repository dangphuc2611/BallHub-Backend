package com.ballhub.ballhub_backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ballhub.ballhub_backend.dto.request.order.CreateOrderRequest;
import com.ballhub.ballhub_backend.dto.response.order.OrderDetailResponse;
import com.ballhub.ballhub_backend.dto.response.order.OrderItemResponse;
import com.ballhub.ballhub_backend.dto.response.order.OrderResponse;
import com.ballhub.ballhub_backend.dto.response.order.OrderStatusHistoryResponse;
import com.ballhub.ballhub_backend.entity.Cart;
import com.ballhub.ballhub_backend.entity.CartItem;
import com.ballhub.ballhub_backend.entity.Order;
import com.ballhub.ballhub_backend.entity.OrderItem;
import com.ballhub.ballhub_backend.entity.OrderStatus;
import com.ballhub.ballhub_backend.entity.OrderStatusHistory;
import com.ballhub.ballhub_backend.entity.PaymentMethod;
import com.ballhub.ballhub_backend.entity.ProductVariant;
import com.ballhub.ballhub_backend.entity.Promotion;
import com.ballhub.ballhub_backend.entity.User;
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
import com.ballhub.ballhub_backend.repository.UserRepository;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public OrderDetailResponse createOrder(Integer userId, CreateOrderRequest request) {
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        UserAddress address = null;
        if (request.getAddressId() != null) {
            address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại"));
        } else if (request.getIsPos() == null || !request.getIsPos()) {
            throw new BadRequestException("Địa chỉ không được để trống với đơn hàng giao đi");
        }

        PaymentMethod paymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndIsActiveTrue(request.getPaymentMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("Phương thức thanh toán không hợp lệ"));

        Promotion appliedVoucher = null;
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            appliedVoucher = promotionRepository.findByPromoCode(request.getPromoCode())
                    .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));
            if (!appliedVoucher.isValid()) {
                throw new BadRequestException("Mã giảm giá không còn hiệu lực");
            }
        }

        User orderOwner = cart.getUser();
        if (Boolean.TRUE.equals(request.getIsPos()) && request.getCustomerId() != null) {
            orderOwner = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new BadRequestException("Khách hàng không tồn tại"));
        }

        boolean isDelivery = request.getAddressId() != null ||
                (request.getDeliveryAddress() != null && !request.getDeliveryAddress().trim().isEmpty());

        boolean isCash = request.getPaymentMethodId() == null || request.getPaymentMethodId() == 1;

        OrderStatus finalStatus;

        if (!isCash) {
            finalStatus = statusRepository.findByStatusName("PENDING")
                    .orElseThrow(() -> new RuntimeException("Lỗi trạng thái PENDING"));
        } else if (Boolean.TRUE.equals(request.getIsPos())) {
            if (isDelivery) {
                finalStatus = statusRepository.findByStatusName("CONFIRMED")
                        .orElseThrow(() -> new RuntimeException("Lỗi trạng thái CONFIRMED"));
            } else {
                finalStatus = statusRepository.findByStatusName("COMPLETED")
                        .orElseThrow(() -> new RuntimeException("Lỗi trạng thái COMPLETED"));
            }
        } else {
            finalStatus = statusRepository.findByStatusName("PENDING")
                    .orElseThrow(() -> new RuntimeException("Lỗi trạng thái PENDING"));
        }

        BigDecimal shipFee = (request.getShippingFee() != null) ? request.getShippingFee() : BigDecimal.ZERO;
        BigDecimal cash = (request.getCustomerCash() != null) ? request.getCustomerCash() : BigDecimal.ZERO;
        BigDecimal change = (request.getChangeAmount() != null) ? request.getChangeAmount() : BigDecimal.ZERO;

        Order order = Order.builder()
                .user(orderOwner)
                .address(address)
                .paymentMethod(paymentMethod)
                .status(finalStatus)
                .promotion(appliedVoucher)
                .shippingFee(shipFee)
                .customerCash(cash)
                .changeAmount(change)
                .isPos(request.getIsPos() != null ? request.getIsPos() : false)
                .deliveryAddress(request.getDeliveryAddress())
                .phone(request.getPhone())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getVariant();
            BigDecimal originalPrice = variant.getPrice();

            Promotion itemPromo = promotionRepository.findActivePromotionForVariant(variant.getVariantId()).orElse(null);
            int discountPct = (itemPromo != null && "PERCENT".equals(itemPromo.getDiscountType())) ? itemPromo.getDiscountPercent() : 0;
            BigDecimal finalPrice = originalPrice.multiply(BigDecimal.valueOf(100 - discountPct)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder).variant(variant).quantity(cartItem.getQuantity())
                    .originalPrice(originalPrice).discountPercent(discountPct).finalPrice(finalPrice).appliedPromotion(itemPromo)
                    .build();
            savedOrder.getItems().add(orderItem);

            variant.decreaseStock(cartItem.getQuantity());
            variantRepository.save(variant);
        }

        savedOrder.calculateTotalAmount();

        if (appliedVoucher != null) {
            BigDecimal subTotal = savedOrder.getSubTotal();
            if (subTotal.compareTo(appliedVoucher.getMinOrderAmount()) < 0) throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu để dùng Voucher này");
            BigDecimal discountAmt;
            if ("PERCENT".equals(appliedVoucher.getDiscountType())) {
                discountAmt = subTotal.multiply(BigDecimal.valueOf(appliedVoucher.getDiscountPercent())).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (appliedVoucher.getMaxDiscountAmount() != null && discountAmt.compareTo(appliedVoucher.getMaxDiscountAmount()) > 0) discountAmt = appliedVoucher.getMaxDiscountAmount();
            } else {
                discountAmt = appliedVoucher.getMaxDiscountAmount();
            }
            savedOrder.setDiscountAmount(discountAmt);
            savedOrder.calculateTotalAmount();
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            promotionRepository.save(appliedVoucher);
        }

        String historyNote = "Khách hàng đặt đơn thành công";
        if (Boolean.TRUE.equals(request.getIsPos())) {
            String cusName = (request.getFullName() != null && !request.getFullName().trim().isEmpty()) ? request.getFullName() : "Khách lẻ";
            String cusPhone = (request.getPhone() != null && !request.getPhone().trim().isEmpty()) ? request.getPhone() : "Trống";
            String cusAddress = (request.getDeliveryAddress() != null && !request.getDeliveryAddress().trim().isEmpty()) ? request.getDeliveryAddress() : "Nhận tại cửa hàng (POS)";

            historyNote = (request.getNote() != null && !request.getNote().trim().isEmpty())
                    ? request.getNote()
                    : "POS|" + cusName + "|" + cusPhone + "|" + cusAddress;
        } else {
            if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
                historyNote = request.getNote();
            }
        }

        savedOrder.updateStatus(finalStatus, historyNote);
        cart.clearCart();

        Order finalSavedOrder = orderRepository.save(savedOrder);
        cartRepository.save(cart);

        OrderDetailResponse responseDto = mapToDetailResponse(finalSavedOrder);
        emailService.sendOrderSuccessEmail(orderOwner.getEmail(), responseDto);

        return responseDto;
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

    // ==========================================
    // 🚀 NEW: HÀM KHÁCH XÁC NHẬN ĐÃ NHẬN HÀNG
    // ==========================================
    public OrderDetailResponse confirmReceived(Integer userId, Integer orderId) {
        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        if (!"DELIVERED".equals(order.getStatus().getStatusName())) {
            throw new BadRequestException("Chỉ có thể xác nhận khi đơn hàng ở trạng thái Đã giao");
        }

        OrderStatus completedStatus = statusRepository.findByStatusName("COMPLETED")
                .orElseThrow(() -> new RuntimeException("OrderStatus COMPLETED không tồn tại"));

        order.updateStatus(completedStatus, "Khách hàng xác nhận đã nhận hàng thành công");
        Order updated = orderRepository.save(order);
        return mapToDetailResponse(updated);
    }

    // ==========================================
    // 🚀 NEW: HÀM KHÁCH KHIẾU NẠI CHƯA NHẬN HÀNG
    // ==========================================
    public OrderDetailResponse reportNotReceived(Integer userId, Integer orderId) {
        Order order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại"));

        if (!"DELIVERED".equals(order.getStatus().getStatusName())) {
            throw new BadRequestException("Chỉ có thể khiếu nại khi đơn hàng ở trạng thái Đã giao");
        }

        OrderStatus failedStatus = statusRepository.findByStatusName("FAILED")
                .orElseThrow(() -> new RuntimeException("OrderStatus FAILED không tồn tại"));

        order.updateStatus(failedStatus, "KHIẾU NẠI: Khách báo chưa nhận được hàng (Shipper báo sai)");

        // Cộng lại số lượng vì giao không thành công
        for (OrderItem item : order.getItems()) {
            item.getVariant().increaseStock(item.getQuantity());
            variantRepository.save(item.getVariant());
        }

        Order updated = orderRepository.save(order);
        return mapToDetailResponse(updated);
    }

    private OrderResponse mapToResponse(Order order) {
        int totalItems = 0;
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                totalItems += item.getQuantity();
            }
        }

        boolean isPos = Boolean.TRUE.equals(order.getIsPos());

        String deliveryAddress = order.getDeliveryAddress();
        if (deliveryAddress == null && order.getAddress() != null) {
            deliveryAddress = order.getAddress().getFullAddress();
        }

        String phone = order.getPhone();
        if (phone == null && order.getUser() != null) {
            phone = order.getUser().getPhone();
        }

        boolean isAdmin = order.getUser() != null && "ADMIN".equalsIgnoreCase(order.getUser().getRole());
        String displayFullName = (order.getUser() != null && !isAdmin) ? order.getUser().getFullName() : "Khách lẻ";

        if (isPos && order.getStatusHistory() != null) {
            for (OrderStatusHistory h : order.getStatusHistory()) {
                if (h.getNote() != null && h.getNote().contains("POS")) {
                    String[] parts = h.getNote().split("\\|");
                    if (parts.length >= 2 && !parts[1].trim().isEmpty() && !parts[1].contains("POS")) {
                        displayFullName = parts[1];
                    }
                    break;
                }
            }
        }

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId((order.getUser() != null) ? order.getUser().getUserId() : null)
                .userFullName(displayFullName)
                .fullName(displayFullName)
                .phone(phone)
                .statusName(order.getStatus().getStatusName())
                .orderDate(order.getOrderDate())
                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .deliveryAddress(deliveryAddress)
                .totalAmount(order.getTotalAmount())
                .customerCash(order.getCustomerCash())
                .changeAmount(order.getChangeAmount())
                .totalItems(totalItems)
                .isPos(isPos)
                .paymentMethodName(order.getPaymentMethod().getMethodName())
                .build();
    }

    private OrderDetailResponse mapToDetailResponse(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : new ArrayList<>(order.getItems())) {
                itemResponses.add(mapToItemResponse(item));
            }
        }

        List<OrderStatusHistoryResponse> historyResponses = new ArrayList<>();
        if (order.getStatusHistory() != null) {
            for (OrderStatusHistory history : new ArrayList<>(order.getStatusHistory())) {
                historyResponses.add(mapToHistoryResponse(history));
            }
        }

        String promoCodeUsed = (order.getPromotion() != null) ? order.getPromotion().getPromoCode() : null;
        boolean isPos = Boolean.TRUE.equals(order.getIsPos());
        boolean isAdmin = order.getUser() != null && "ADMIN".equalsIgnoreCase(order.getUser().getRole());

        String displayFullName = (order.getUser() != null && !isAdmin) ? order.getUser().getFullName() : "Khách lẻ";
        String displayPhone = order.getPhone();
        if (displayPhone == null && order.getUser() != null) {
            displayPhone = order.getUser().getPhone();
        }

        String displayAddress = order.getDeliveryAddress();
        if (displayAddress == null && order.getAddress() != null) {
            displayAddress = order.getAddress().getFullAddress();
        }

        String displayEmail = (order.getUser() != null && !isAdmin) ? order.getUser().getEmail() : "";

        if (isPos && (displayPhone == null || displayAddress == null)) {
            for (OrderStatusHistory h : order.getStatusHistory()) {
                if (h.getNote() != null && h.getNote().contains("POS")) {
                    String[] parts = h.getNote().split("\\|");
                    if (parts.length >= 2 && displayFullName.equals("Khách lẻ")) displayFullName = parts[1];
                    if (parts.length >= 3 && (displayPhone == null || displayPhone.equals("---"))) displayPhone = parts[2];
                    if (parts.length >= 4 && (displayAddress == null)) displayAddress = parts[3];
                    break;
                }
            }
        }

        if (displayAddress == null || displayAddress.trim().isEmpty()) {
            displayAddress = isPos ? "Nhận tại cửa hàng (POS)" : "---";
        }
        if (displayPhone == null || displayPhone.trim().isEmpty() || displayPhone.equals("Trống")) {
            displayPhone = isPos ? "Mua tại quầy" : "---";
        }

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .userId((order.getUser() != null) ? order.getUser().getUserId() : null)
                .userFullName(displayFullName)
                .userEmail(displayEmail)
                .userPhone(displayPhone)
                .deliveryAddress(displayAddress)
                .paymentMethodName(order.getPaymentMethod().getMethodName())
                .statusName(order.getStatus().getStatusName())
                .orderDate(order.getOrderDate())
                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .promoCode(promoCodeUsed)
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .customerCash(order.getCustomerCash())
                .changeAmount(order.getChangeAmount())
                .isPos(isPos)
                .items(itemResponses)
                .statusHistory(historyResponses)
                .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem item) {
        ProductVariant variant = item.getVariant();
        String imageUrl = null;

        try {
            if (variant != null && variant.getProduct() != null && variant.getProduct().getImages() != null) {
                List<com.ballhub.ballhub_backend.entity.ProductImage> images = new ArrayList<>(
                        variant.getProduct().getImages());
                for (var img : images) {
                    if (Boolean.TRUE.equals(img.getIsMain())) {
                        imageUrl = img.getImageUrl();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Cảnh báo: Bỏ qua lỗi Hibernate Proxy tải ảnh: " + e.getMessage());
        }

        String promotionName = (item.getAppliedPromotion() != null) ? item.getAppliedPromotion().getPromotionName() : null;

        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .variantId((variant != null) ? variant.getVariantId() : null)
                .productName((variant != null && variant.getProduct() != null) ? variant.getProduct().getProductName() : "Sản phẩm không rõ")
                .sizeName((variant != null && variant.getSize() != null) ? variant.getSize().getSizeName() : "N/A")
                .colorName((variant != null && variant.getColor() != null) ? variant.getColor().getColorName() : "N/A")
                .quantity(item.getQuantity())
                .originalPrice(item.getOriginalPrice())
                .discountPercent(item.getDiscountPercent())
                .appliedPromotionName(promotionName)
                .finalPrice(item.getFinalPrice())
                .subtotal(item.getSubtotal())
                .imageUrl(imageUrl)
                .sku((variant != null) ? variant.getSku() : "N/A")
                .build();
    }

    private OrderStatusHistoryResponse mapToHistoryResponse(OrderStatusHistory history) {
        String displayNote = history.getNote();

        if (displayNote != null && displayNote.startsWith("POS|")) {
            displayNote = "Thanh toán thành công tại quầy (POS)";
        }

        return OrderStatusHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .statusName(history.getStatus().getStatusName())
                .changedAt(history.getChangedAt())
                .note(displayNote)
                .build();
    }

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

        // 🚀 NẾU HỦY HOẶC GIAO THẤT BẠI THÌ CỘNG LẠI KHO
        if ("CANCELLED".equals(targetStatus) || "FAILED".equals(targetStatus)) {
            for (OrderItem item : order.getItems()) {
                item.getVariant().increaseStock(item.getQuantity());
                variantRepository.save(item.getVariant());
            }
        }

        order.updateStatus(newStatus, (note != null) ? note : "Admin cập nhật trạng thái");
        Order updated = orderRepository.save(order);

        return mapToDetailResponse(updated);
    }

    // ==========================================
    // 🚀 LÀM CHẶT LUẬT CHUYỂN TRẠNG THÁI
    // ==========================================
    private void validateStatusTransition(String currentStatus, String targetStatus) {
        switch (currentStatus) {
            case "PENDING":
                if (!"CONFIRMED".equals(targetStatus) && !"CANCELLED".equals(targetStatus)) {
                    throw new BadRequestException("Không thể chuyển từ PENDING sang " + targetStatus);
                }
                break;
            case "CONFIRMED":
                if (!"SHIPPING".equals(targetStatus) && !"CANCELLED".equals(targetStatus)) {
                    throw new BadRequestException("Không thể chuyển từ CONFIRMED sang " + targetStatus);
                }
                break;
            case "SHIPPING":
                if (!"DELIVERED".equals(targetStatus) && !"FAILED".equals(targetStatus)) {
                    throw new BadRequestException("Không thể chuyển từ SHIPPING sang " + targetStatus + ". Chỉ có thể chuyển sang DELIVERED (Đã giao) hoặc FAILED (Giao thất bại)");
                }
                break;
            case "DELIVERED":
                if (!"COMPLETED".equals(targetStatus) && !"FAILED".equals(targetStatus)) {
                    throw new BadRequestException("Từ DELIVERED chỉ có thể chuyển sang COMPLETED (Hoàn thành) hoặc FAILED (Giao thất bại/Khiếu nại)");
                }
                break;
            case "COMPLETED":
                throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã hoàn thành");
            case "FAILED":
                throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã thất bại");
            case "CANCELLED":
                throw new BadRequestException("Không thể thay đổi trạng thái đơn hàng đã hủy");
            default:
                throw new BadRequestException("Trạng thái không hợp lệ: " + currentStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getNewestOrders() {
        List<OrderResponse> responses = new ArrayList<>();
        for (Order o : orderRepository.findAll(org.springframework.data.domain.Sort.by("orderDate").descending())) {
            responses.add(mapToResponse(o));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String statusName) {
        List<OrderResponse> responses = new ArrayList<>();
        for (Order o : orderRepository.findByStatusStatusName(statusName)) {
            responses.add(mapToResponse(o));
        }
        return responses;
    }

    @Transactional
    public void processVnPaySuccess(Integer orderId, boolean isPos) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            boolean isDelivery = order.getAddress() != null;
            if (isPos && order.getStatusHistory() != null) {
                for (OrderStatusHistory h : order.getStatusHistory()) {
                    if (h.getNote() != null && h.getNote().contains("POS")) {
                        String[] parts = h.getNote().split("\\|");
                        if (parts.length >= 4 && !parts[3].trim().isEmpty() && !parts[3].equals("Nhận tại cửa hàng (POS)")) {
                            isDelivery = true;
                        }
                        break;
                    }
                }
            }

            String targetStatusName = (isPos && !isDelivery) ? "COMPLETED" : "CONFIRMED";
            OrderStatus targetStatus = statusRepository.findByStatusName(targetStatusName).orElse(null);

            if (targetStatus != null) {
                order.updateStatus(targetStatus, "Đã thanh toán VNPAY (Tự động cập nhật)");
                orderRepository.save(order);
            }
        }
    }
}