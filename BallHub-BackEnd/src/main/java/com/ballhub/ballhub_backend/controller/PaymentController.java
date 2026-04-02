package com.ballhub.ballhub_backend.controller;

import com.ballhub.ballhub_backend.config.VNPayConfig;
import com.ballhub.ballhub_backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    // 1. API TẠO LINK VNPAY CÓ BỔ SUNG CỜ "isPos"
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

        // Đánh dấu luồng POS hay WEB vào thẳng thông tin đơn hàng gửi sang VNPAY
        String orderInfoStr = "Thanh toan don hang: " + orderId + (isPos ? "-POS" : "-WEB");
        vnp_Params.put("vnp_OrderInfo", orderInfoStr);

        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
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

    // 2. API HỨNG KẾT QUẢ VÀ CHIA LUỒNG REDIRECT
    @GetMapping("/vnpay-return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String orderInfo = request.getParameter("vnp_OrderInfo");

        // Bóc tách xem là POS hay WEB
        String orderIdStr = orderInfo.replace("Thanh toan don hang: ", "").trim();
        boolean isPos = orderIdStr.endsWith("-POS");
        orderIdStr = orderIdStr.replace("-POS", "").replace("-WEB", "");

        // ==========================================
        // NẾU LÀ ĐƠN TỪ POS (TẠI QUẦY)
        // ==========================================
        if (isPos) {
            response.setContentType("text/html;charset=UTF-8");
            if ("00".equals(vnp_ResponseCode)) {
                // Trả về HTML chạy lệnh tự đóng Tab VNPAY
                response.getWriter().write("<html><body><script>alert('Khách hàng thanh toán VNPAY thành công! Có thể đóng cửa sổ này.'); window.close();</script></body></html>");
            } else {
                response.getWriter().write("<html><body><script>alert('Khách hàng thanh toán VNPAY thất bại!'); window.close();</script></body></html>");
            }
            return;
        }

        // ==========================================
        // NẾU LÀ ĐƠN TỪ WEB (KHÁCH TỰ ĐẶT)
        // ==========================================
        if ("00".equals(vnp_ResponseCode)) {
            response.sendRedirect("http://localhost:3000/order-success/" + orderIdStr);
        } else {
            response.sendRedirect("http://localhost:3000/checkout?payment_error=true");
        }
    }
}