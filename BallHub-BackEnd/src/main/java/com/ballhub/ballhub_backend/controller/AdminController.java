package com.ballhub.ballhub_backend.controller;

import java.util.List;

import com.ballhub.ballhub_backend.dto.request.product.PosVariantFilterRequest;
import com.ballhub.ballhub_backend.dto.response.admin.PosVariantResponse;
import com.ballhub.ballhub_backend.dto.response.PageResponse;
import com.ballhub.ballhub_backend.service.ProductVariantService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.dto.response.admin.DashboardStatsResponse;
import com.ballhub.ballhub_backend.dto.response.user.UserResponse;
import com.ballhub.ballhub_backend.service.AdminService;
import com.ballhub.ballhub_backend.service.OrderService;
import com.ballhub.ballhub_backend.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cung cấp API thống kê admin dashboard
 * Endpoint chính: GET /api/admin/stats/dashboard
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminController {

  private final AdminService adminService;
  private final OrderService orderService;
  private final UserService userService;
  private final ProductVariantService productVariantService;

  /**
   * API thống kê tổng hợp
   * GET /api/admin/stats/dashboard?topProductsLimit=10
   *
   * Response:
   * {
   * "code": 200,
   * "message": "Lấy thống kê dashboard thành công",
   * "data": {
   * "totalOrders": 150,
   * "totalRevenue": 50000000,
   * "totalCustomers": 1200,
   * "topProducts": [
   * {
   * "productId": 1,
   * "productName": "Áo bóng đá",
   * "quantitySold": 500,
   * "totalRevenue": 10000000
   * },
   * ...
   * ]
   * }
   * }
   */
  @GetMapping("/dashboard")
  public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
      @RequestParam(value = "topProductsLimit", defaultValue = "10") Integer topProductsLimit) {

    // Validate topProductsLimit
    if (topProductsLimit == null || topProductsLimit < 1)
      topProductsLimit = 10;
    if (topProductsLimit > 100)
      topProductsLimit = 100;

    DashboardStatsResponse stats = adminService.getDashboardStats(topProductsLimit);
    return ResponseEntity.ok(ApiResponse.success("Lấy thống kê dashboard thành công", stats));
  }

  // @GetMapping("/newest-order")
  // public List<OrderResponse> getNewestOrder() {
  // return orderService.getNewestOrders();
  // }

  /**
   * API lấy danh sách tất cả user
   * GET /api/admin/stats/users
   *
   * Response:
   * {
   * "success": true,
   * "message": "Lấy danh sách user thành công",
   * "data": [
   * {
   * "userId": 1,
   * "fullName": "Nguyễn Văn A",
   * "email": "user@example.com",
   * "phone": "0123456789",
   * "avatar": "/uploads/avatars/...",
   * "role": "CUSTOMER"
   * },
   * ...
   * ]
   * }
   */
  @GetMapping("/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    try {
      List<UserResponse> users = userService.getAllUsers();
      return ResponseEntity.ok(ApiResponse.success("Lấy danh sách user thành công", users));
    } catch (Exception e) {
      return ResponseEntity.status(500)
          .body(new ApiResponse<>(false, "Lỗi khi lấy danh sách user: " + e.getMessage(), null));
    }
  }

  // API lấy danh sách biến thể sản phẩm cho màn hình POS (Bán hàng tại quầy)
  @GetMapping("/pos/variants")
  public ResponseEntity<ApiResponse<PageResponse<PosVariantResponse>>> getVariantsForPos(
          @ModelAttribute PosVariantFilterRequest filterRequest,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    PageResponse<PosVariantResponse> result = productVariantService.getVariantsForPos(filterRequest, pageable);

    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm bán quầy thành công", result));
  }
}
