package com.ballhub.ballhub_backend.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.dto.response.order.OrderDetailResponse;
import com.ballhub.ballhub_backend.dto.response.order.OrderResponse;
import com.ballhub.ballhub_backend.dto.request.order.CreateOrderRequest;
import com.ballhub.ballhub_backend.security.CustomUserDetails;
import com.ballhub.ballhub_backend.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        Integer userId = getUserId(authentication);

        // 1. Lưu đơn hàng vào Database qua OrderService
        OrderDetailResponse order = orderService.createOrder(userId, request);

        // 2. Kiểm tra phương thức thanh toán
        // Giả sử ID = 2 là "Chuyển khoản ngân hàng"
        if (request.getPaymentMethodId() != null && request.getPaymentMethodId() == 2) {
            try {
                // Cú pháp:
                // https://img.vietqr.io/image/{Mã_NH}-{STK}-compact2.jpg?amount={Tiền}&addInfo={Nội_dung}&accountName={Tên_chủ_TK}
                // TẠO LINK ẢNH MÃ QR BẰNG VIETQR (Dùng STK thật của bạn)
                String paymentUrl = "https://img.vietqr.io/image/MB-0886301661-compact2.jpg?amount="
                        + order.getTotalAmount().intValue()
                        + "&addInfo=THANH TOAN DON HANG " + order.getOrderId()
                        + "&accountName=NGO GIA HIEN"; // Bạn có thể đổi thành BALLHUB STORE hoặc tên thật viết không
                                                       // dấu nhé

                // Trả về dữ liệu gồm cả chi tiết đơn hàng và link ảnh mã QR
                Map<String, Object> result = new HashMap<>();
                result.put("order", order);
                result.put("paymentUrl", paymentUrl);

                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Vui lòng quét mã QR", result));

            } catch (Exception e) {
                throw new RuntimeException("Lỗi tạo link thanh toán: " + e.getMessage());
            }
        }

        // 3. Nếu là COD (Thanh toán khi nhận hàng)
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.getMyOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(orders)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @PathVariable Integer id,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Kiểm tra xem người đang request có phải là ADMIN không
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        OrderDetailResponse order;
        if (isAdmin) {
            // Quyền tối cao: Xem đơn nào cũng được không bị 404
            order = orderService.getOrderDetailAdmin(id);
        } else {
            // User thường: Chỉ xem được đơn của chính mình
            order = orderService.getOrderDetail(userDetails.getUserId(), id);
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelOrder(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer userId = getUserId(authentication);
        orderService.cancelOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", null));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrdersAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(orders)));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetailAdmin(
            @PathVariable Integer id) {
        OrderDetailResponse order = orderService.getOrderDetailAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderStatusAdmin(
            @PathVariable Integer id,
            @RequestParam Integer statusId,
            @RequestParam(required = false) String note) {
        OrderDetailResponse order = orderService.updateOrderStatus(id, statusId, note);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", order));
    }

    // ==========================================
    // 🚀 API: KHÁCH HÀNG XÁC NHẬN ĐÃ NHẬN HÀNG
    // ==========================================
    @PostMapping("/{orderId}/confirm-received")
    public ResponseEntity<?> confirmReceived(
            @PathVariable Integer orderId,
            Authentication authentication) { // Đã sửa: dùng Authentication
        try {
            // Lấy userId chuẩn theo hàm getUserId của bạn
            Integer userId = getUserId(authentication);

            var response = orderService.confirmReceived(userId, orderId);
            return ResponseEntity.ok(ApiResponse.success("Xác nhận nhận hàng thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==========================================
    // 🚀 API: KHÁCH HÀNG KHIẾU NẠI CHƯA NHẬN HÀNG
    // ==========================================
    @PostMapping("/{orderId}/report-not-received")
    public ResponseEntity<?> reportNotReceived(
            @PathVariable Integer orderId,
            Authentication authentication) { // Đã sửa: dùng Authentication
        try {
            // Lấy userId chuẩn theo hàm getUserId của bạn
            Integer userId = getUserId(authentication);

            var response = orderService.reportNotReceived(userId, orderId);
            return ResponseEntity.ok(ApiResponse.success("Đã gửi khiếu nại thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Integer getUserId(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.ballhub.ballhub_backend.exception.UnauthorizedException(
                    "Vui lòng đăng nhập để thực hiện chức năng này");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUserId();
    }
}