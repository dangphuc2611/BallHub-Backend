package com.ballhub.ballhub_backend.service;

import com.ballhub.ballhub_backend.dto.response.order.OrderDetailResponse;
import com.ballhub.ballhub_backend.dto.response.order.OrderItemResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendNewVoucherEmail(List<String> listEmails, String promoCode, String description) {
        if (listEmails == null || listEmails.isEmpty()) return;

        String safeDescription = (description == null || description.trim().equalsIgnoreCase("null") || description.trim().isEmpty())
                ? "Săn ngay ưu đãi giới hạn dành riêng cho bạn!"
                : description;

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("animefighterssimulator2906@gmail.com", "BallHub - Ưu đãi độc quyền");

            String[] bccArray = listEmails.toArray(new String[0]);
            helper.setBcc(bccArray);
            helper.setTo("animefighterssimulator2906@gmail.com");
            helper.setSubject("🎁 QUÀ TẶNG BẤT NGỜ: Mã " + promoCode + " đã sẵn sàng!");

            String htmlContent =
                    "<div style='background-color: #f0fdf4; padding: 50px 0; font-family: \"Segoe UI\", Roboto, sans-serif;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 30px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.05);'>" +
                            "<div style='background: linear-gradient(135deg, #059669 0%, #10b981 100%); padding: 50px 20px; text-align: center; color: white;'>" +
                            "<div style='font-size: 14px; font-weight: 800; letter-spacing: 3px; margin-bottom: 15px; opacity: 0.9;'>SPECIAL VOUCHER</div>" +
                            "<h1 style='font-size: 32px; margin: 0; font-weight: 900; line-height: 1.2;'>DÀNH RIÊNG CHO BẠN!</h1>" +
                            "</div>" +
                            "<div style='padding: 40px; text-align: center;'>" +
                            "<p style='color: #4b5563; font-size: 16px; line-height: 1.6;'>Chào bạn, BallHub vừa tung ra một ưu đãi cực hời. Đừng để lỡ cơ hội sở hữu những món đồ thể thao chất nhất nhé!</p>" +
                            "<div style='margin: 30px auto; max-width: 400px; padding: 30px; border: 3px dashed #10b981; border-radius: 20px; background-color: #f9fafb;'>" +
                            "<div style='color: #10b981; font-size: 12px; font-weight: 800; margin-bottom: 10px; letter-spacing: 1px;'>MÃ GIẢM GIÁ CỦA BẠN</div>" +
                            "<div style='color: #1f2937; font-size: 38px; font-weight: 900; letter-spacing: 4px;'>" + promoCode + "</div>" +
                            "<div style='color: #6b7280; font-size: 14px; margin-top: 15px; font-style: italic; line-height: 1.4;'>" + safeDescription + "</div>" +
                            "</div>" +
                            "<div style='margin-top: 35px;'>" +
                            "<a href='http://localhost:3000' style='background: #1f2937; color: white; padding: 18px 45px; border-radius: 15px; font-weight: 800; text-decoration: none; display: inline-block; font-size: 16px; box-shadow: 0 10px 20px rgba(0,0,0,0.1);'>SĂN NGAY KẺO HẾT</a>" +
                            "</div>" +
                            "</div>" +
                            "<div style='background-color: #f9fafb; padding: 30px; text-align: center; border-top: 1px solid #f3f4f6;'>" +
                            "<p style='color: #9ca3af; font-size: 12px; margin: 0; line-height: 1.5;'>Bạn nhận được email này vì là thành viên của BallHub.<br>Nếu không muốn nhận tin, hãy bấm <a href='#' style='color: #10b981; text-decoration: none;'>hủy đăng ký</a>.</p>" +
                            "</div>" +
                            "</div>" +
                            "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi Email Voucher: " + e.getMessage());
        }
    }

    // ✅ FIX LUỒNG: Bắt buộc truyền vào OrderDetailResponse (Data chết) chứ không truyền Order (Entity sống)
    @Async
    public void sendOrderSuccessEmail(String toEmail, OrderDetailResponse order) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("animefighterssimulator2906@gmail.com", "BallHub - Store");
            helper.setTo(toEmail);
            helper.setSubject("⚽ Đơn hàng #" + order.getOrderId() + " đã được xác nhận!");

            StringBuilder productRows = new StringBuilder();
            String baseUrl = "http://localhost:8080";

            for (OrderItemResponse item : order.getItems()) {
                String imageUrl = "https://placehold.co/100x100?text=BallHub";

                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    String path = item.getImageUrl();
                    imageUrl = baseUrl + (path.startsWith("/") ? "" : "/") + path;
                }

                String productName = item.getProductName() != null ? item.getProductName() : "Sản phẩm";
                String sizeName = item.getSizeName() != null ? item.getSizeName() : "N/A";

                productRows.append(
                        "<tr>" +
                                "<td valign='middle' style='padding: 15px 0; border-bottom: 1px solid #f0f0f0; width: 80px;'>" +
                                "<img src='" + imageUrl + "' width='70' height='70' style='border-radius: 12px; display: block; object-fit: contain;'>" +
                                "</td>" +
                                "<td valign='middle' style='padding: 15px 15px; border-bottom: 1px solid #f0f0f0;'>" +
                                "<div style='font-size: 15px; font-weight: 700; color: #111; margin: 0;'>" + productName + "</div>" +
                                "<div style='font-size: 12px; color: #888; margin-top: 5px;'>Size: " + sizeName + " | x" + item.getQuantity() + "</div>" +
                                "</td>" +
                                "<td valign='middle' style='padding: 15px 0; border-bottom: 1px solid #f0f0f0; text-align: right;'>" +
                                "<span style='font-size: 15px; font-weight: 800; color: #111;'>" + String.format("%,.0f", item.getFinalPrice().doubleValue()) + "đ</span>" +
                                "</td>" +
                                "</tr>"
                );
            }

            String customerName = order.getUserFullName() != null ? order.getUserFullName() : "Khách hàng";
            double discountAmt = order.getDiscountAmount() != null ? order.getDiscountAmount().doubleValue() : 0;

            String htmlContent =
                    "<div style='background-color: #f9fafb; padding: 40px 0;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 24px; box-shadow: 0 4px 25px rgba(0,0,0,0.05); overflow: hidden;'>" +
                            "<div style='background: linear-gradient(90deg, #10b981 0%, #3b82f6 100%); padding: 40px 20px; text-align: center;'>" +
                            "<img src='https://cdn-icons-png.flaticon.com/512/1162/1162499.png' width='50' style='margin-bottom: 15px;'>" +
                            "<h2 style='color: white; margin: 0; font-size: 24px; font-weight: 900;'>ĐƠN HÀNG ĐÃ SẴN SÀNG!</h2>" +
                            "</div>" +
                            "<div style='padding: 30px;'>" +
                            "<p style='color: #444;'>Chào <b>" + customerName + "</b>,</p>" +
                            "<p style='color: #666; font-size: 14px;'>Đơn hàng của bạn đã được BallHub tiếp nhận và đang được xử lý nhanh nhất có thể. Dưới đây là tóm tắt đơn hàng của bạn:</p>" +
                            "<table style='width: 100%; border-collapse: collapse; margin-top: 20px;'>" +
                            productRows.toString() +
                            "</table>" +
                            "<div style='background: #f8fafc; border-radius: 16px; padding: 20px; margin-top: 25px;'>" +
                            "<div style='display: flex; justify-content: space-between; margin-bottom: 8px;'>" +
                            "<span style='color: #64748b;'>Tạm tính:</span><span style='float: right; color: #1e293b; font-weight: bold;'>" + String.format("%,.0f", order.getSubTotal().doubleValue()) + "đ</span>" +
                            "</div>" +
                            "<div style='display: flex; justify-content: space-between; margin-bottom: 8px;'>" +
                            "<span style='color: #64748b;'>Phí vận chuyển:</span><span style='float: right; color: #1e293b; font-weight: bold;'>+" + String.format("%,.0f", order.getShippingFee().doubleValue()) + "đ</span>" +
                            "</div>" +
                            (discountAmt > 0 ?
                                    "<div style='display: flex; justify-content: space-between; margin-bottom: 8px; color: #ef4444;'>" +
                                            "<span>Giảm giá:</span><span style='float: right; font-weight: bold;'>-" + String.format("%,.0f", discountAmt) + "đ</span>" +
                                            "</div>" : "") +
                            "<div style='border-top: 1px dashed #cbd5e1; margin: 12px 0;'></div>" +
                            "<div style='display: flex; justify-content: space-between;'>" +
                            "<span style='font-size: 18px; font-weight: 800; color: #1e293b;'>TỔNG CỘNG</span>" +
                            "<span style='float: right; font-size: 22px; font-weight: 900; color: #10b981;'>" + String.format("%,.0f", order.getTotalAmount().doubleValue()) + "đ</span>" +
                            "</div>" +
                            "</div>" +
                            "<div style='text-align: center; margin-top: 35px;'>" +
                            "<a href='http://localhost:3000/profile/orders' style='background: #111; color: white; padding: 16px 40px; border-radius: 14px; text-decoration: none; font-weight: bold; font-size: 14px;'>THEO DÕI ĐƠN HÀNG</a>" +
                            "</div>" +
                            "</div>" +
                            "<div style='background: #111; padding: 30px; text-align: center; color: #666; font-size: 11px;'>" +
                            "<p style='color: white; font-weight: bold; margin-bottom: 10px;'>Cảm ơn bạn đã đồng hành cùng BallHub!</p>" +
                            "Hotline: 1900 xxxx | Email: support@ballhub.com<br>© 2026 BallHub Store" +
                            "</div>" +
                            "</div>" +
                            "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String newPassword) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("animefighterssimulator2906@gmail.com", "BallHub - Bảo mật");
            helper.setTo(toEmail);
            helper.setSubject("🔒 BallHub - Yêu cầu cấp lại mật khẩu");

            String htmlContent =
                    "<div style='font-family: Arial, sans-serif; padding: 30px; background-color: #f8fafc; text-align: center;'>" +
                            "<div style='max-width: 500px; margin: auto; background: white; padding: 40px; border-radius: 16px; box-shadow: 0 4px 15px rgba(0,0,0,0.05);'>" +
                            "<h2 style='color: #0f172a; margin-bottom: 20px;'>Cấp Lại Mật Khẩu</h2>" +
                            "<p style='color: #64748b; line-height: 1.6;'>Chào bạn, quản trị viên BallHub vừa thực hiện thao tác reset mật khẩu cho tài khoản của bạn. Dưới đây là mật khẩu đăng nhập mới:</p>" +
                            "<div style='margin: 30px 0; padding: 20px; background: #f1f5f9; border-radius: 12px; font-size: 28px; font-weight: bold; letter-spacing: 4px; color: #10b981;'>" +
                            newPassword +
                            "</div>" +
                            "<p style='color: #ef4444; font-size: 13px; font-weight: bold;'>⚠️ Vui lòng đăng nhập và đổi lại mật khẩu ngay lập tức để bảo vệ tài khoản!</p>" +
                            "</div>" +
                            "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}