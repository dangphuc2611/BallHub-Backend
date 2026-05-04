package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.config.VNPayConfig;
import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import com.ballhub.ballhub_backend.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    @Value("${vnpay.url}")
    private String vnp_PayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    @Autowired
    private OrderService orderService;

    @GetMapping("/create-vnpay")
    public ResponseEntity<ApiResponse<String>> createPayment(
            @RequestParam("amount") long amount,
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "isPos", required = false, defaultValue = "false") boolean isPos,
            HttpServletRequest request) throws Exception {

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amountVND = amount * 100;

        String vnp_TxnRef = orderId + "_" + VNPayConfig.getRandomNumber(4);
        String vnp_IpAddr = VNPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountVND));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

        String orderInfoStr = "Thanh toan don hang: " + orderId + (isPos ? "-POS" : "-WEB");
        vnp_Params.put("vnp_OrderInfo", orderInfoStr);

        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Đã sửa múi giờ chuẩn VN
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(timeZone);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(timeZone); // Bắt buộc phải thêm dòng này

        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnp_PayUrl + "?" + queryUrl;

        return ResponseEntity.ok(ApiResponse.success("Tạo link thành công", paymentUrl));
    }

    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String orderInfo = request.getParameter("vnp_OrderInfo");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");

        String realOrderIdStr = "";
        if (vnp_TxnRef != null && vnp_TxnRef.contains("_")) {
            realOrderIdStr = vnp_TxnRef.split("_")[0];
        } else {
            realOrderIdStr = vnp_TxnRef;
        }

        // ✅ CHÍNH LÀ DÒNG NÀY ĐÂY!!! HÔM TRƯỚC MÌNH QUÊN CHO VÀO BẢN CẬP NHẬT
        // Xóa sạch mọi chữ cái, chỉ giữ lại số để Convert không bị lỗi
        realOrderIdStr = realOrderIdStr.replaceAll("[^0-9]", "");

        boolean isPos = orderInfo != null && orderInfo.contains("-POS");

        if ("00".equals(vnp_ResponseCode)) {
            try {
                // Ép kiểu sẽ thành công 100% vì đã xóa sạch chữ "HD" ở trên
                Integer realOrderId = Integer.parseInt(realOrderIdStr);
                orderService.processVnPaySuccess(realOrderId, isPos);
            } catch (Exception e) {
                System.out.println("❌ Lỗi update trạng thái VNPAY: " + e.getMessage());
            }
        }

        if (isPos) {
            response.setContentType("text/html;charset=UTF-8");
            if ("00".equals(vnp_ResponseCode)) {
                response.getWriter().write("<html><body style='background:#1e293b; color:#10b981; font-family:sans-serif; text-align:center; padding-top:100px;'><h1>ĐÃ CHỐT ĐƠN!</h1><h3>Thanh toán VNPAY thành công. Đơn hàng tự động chuyển sang ĐÃ GIAO.</h3><script>alert('Thanh toán VNPAY thành công! Đơn hàng đã tự động chuyển sang ĐÃ GIAO (DELIVERED).'); window.close();</script></body></html>");
            } else {
                response.getWriter().write("<html><body><script>alert('Khách hàng thanh toán VNPAY thất bại!'); window.close();</script></body></html>");
            }
            return;
        }

        if ("00".equals(vnp_ResponseCode)) {
            // Thay localhost bằng link Vercel của sếp
            response.sendRedirect("https://ballhub-front-end.vercel.app/order-success/" + realOrderIdStr);
        } else {
            // Thay localhost bằng link Vercel của sếp
            response.sendRedirect("https://ballhub-front-end.vercel.app/checkout?payment_error=true");
        }
    }
}
