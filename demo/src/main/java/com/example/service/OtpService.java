package com.example.service;

import com.example.dao.OtpVerificationDAO;
import com.example.dao.SystemSettingDAO;
import com.example.model.EmailConfig;
import com.example.model.OtpVerification;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service quản lý nghiệp vụ xác thực OTP:
 * - Sinh mã OTP 6 chữ số ngẫu nhiên
 * - Gửi mã OTP qua email
 * - Xác thực mã OTP
 * - Hỗ trợ gửi lại mã OTP (có cooldown)
 */
public class OtpService {

    private final OtpVerificationDAO otpDAO     = new OtpVerificationDAO();
    private final SystemSettingDAO   settingDAO = new SystemSettingDAO();
    private final EmailService       emailService = new EmailService();

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ================================================================
    //  Sinh & Gửi OTP
    // ================================================================

    /**
     * Sinh mã OTP 6 chữ số ngẫu nhiên.
     */
    public String generateOtpCode() {
        int code = 100000 + RANDOM.nextInt(900000); // 100000 - 999999
        return String.valueOf(code);
    }

    /**
     * Tạo và gửi mã OTP đến email.
     * Quy trình:
     *   1. Kiểm tra cooldown (tránh spam).
     *   2. Vô hiệu hóa tất cả OTP cũ cho email + type.
     *   3. Sinh mã OTP mới, lưu vào DB.
     *   4. Gửi email chứa mã OTP.
     *
     * @param email   Email nhận mã
     * @param type    Loại OTP (REGISTER hoặc RESET_PASSWORD)
     * @param payload JSON lưu tạm thông tin đăng ký (có thể null)
     * @return OtpVerification đã tạo (chứa id)
     * @throws IllegalStateException nếu đang trong cooldown
     * @throws RuntimeException nếu gửi email thất bại
     */
    public OtpVerification sendOtp(String email, OtpVerification.OtpType type,
                                   String payload) throws SQLException {
        // 1. Kiểm tra cooldown
        if (otpDAO.hasRecentOtp(email, type)) {
            throw new IllegalStateException(
                "Vui lòng đợi " + OtpVerification.RESEND_COOLDOWN_SECONDS
                + " giây trước khi gửi lại mã OTP."
            );
        }

        // 2. Vô hiệu hóa OTP cũ
        otpDAO.invalidateOldOtps(email, type);

        // 3. Sinh mã mới
        String otpCode = generateOtpCode();
        String expiresAt = LocalDateTime.now()
            .plusMinutes(OtpVerification.EXPIRY_MINUTES)
            .format(DT_FMT);

        OtpVerification otp = new OtpVerification(email, otpCode, type, payload, expiresAt);
        int id = otpDAO.insert(otp);
        if (id == -1) {
            throw new SQLException("Không thể tạo bản ghi OTP.");
        }
        otp.setId(id);

        // 4. Gửi email OTP
        EmailConfig config = settingDAO.loadEmailConfig();
        if (!config.isValid()) {
            throw new RuntimeException("Chưa cấu hình SMTP. Liên hệ quản trị viên.");
        }

        String subject = buildOtpSubject(type);
        String html    = buildOtpEmailHtml(otpCode, type, email);
        String error   = emailService.sendEmail(config, email, subject, html);

        if (error != null) {
            throw new RuntimeException("Gửi email OTP thất bại: " + error);
        }

        System.out.println("[OTP] Đã gửi mã OTP đến " + email + " (type=" + type + ")");
        return otp;
    }

    // ================================================================
    //  Xác thực OTP
    // ================================================================

    /**
     * Xác thực mã OTP do người dùng nhập.
     *
     * @param email   Email đã nhận OTP
     * @param code    Mã OTP người dùng nhập
     * @param type    Loại OTP
     * @return OtpVerification đã xác thực (chứa payload để lấy thông tin đăng ký)
     * @throws IllegalArgumentException nếu mã OTP sai hoặc hết hạn
     */
    public OtpVerification verifyOtp(String email, String code,
                                     OtpVerification.OtpType type) throws SQLException {
        // Tìm OTP hợp lệ (chưa xác thực, chưa hết hạn, chưa vượt max attempts)
        OtpVerification otp = otpDAO.findValidOtp(email, type);

        if (otp == null) {
            throw new IllegalArgumentException(
                "Mã OTP đã hết hạn hoặc không tồn tại.\nVui lòng yêu cầu gửi lại mã mới."
            );
        }

        // Kiểm tra mã
        if (!otp.getOtpCode().equals(code.trim())) {
            // Tăng số lần thử
            otpDAO.incrementAttempts(otp.getId());
            int remaining = OtpVerification.MAX_ATTEMPTS - otp.getAttempts() - 1;

            if (remaining <= 0) {
                throw new IllegalArgumentException(
                    "Mã OTP không đúng. Bạn đã nhập sai quá "
                    + OtpVerification.MAX_ATTEMPTS + " lần.\n"
                    + "Vui lòng yêu cầu gửi lại mã mới."
                );
            } else {
                throw new IllegalArgumentException(
                    "Mã OTP không đúng.\nCòn " + remaining + " lần thử."
                );
            }
        }

        // Đánh dấu đã xác thực
        otpDAO.markVerified(otp.getId());
        otp.setVerified(true);

        System.out.println("[OTP] Xác thực thành công cho " + email + " (type=" + type + ")");
        return otp;
    }

    /**
     * Kiểm tra xem có thể gửi lại OTP không (đã qua cooldown chưa).
     */
    public boolean canResendOtp(String email, OtpVerification.OtpType type) throws SQLException {
        return !otpDAO.hasRecentOtp(email, type);
    }

    /**
     * Dọn dẹp các bản ghi OTP đã hết hạn quá 24 giờ.
     */
    public int cleanupExpiredOtps() throws SQLException {
        return otpDAO.cleanupExpired();
    }

    // ================================================================
    //  Email Template cho OTP
    // ================================================================

    private String buildOtpSubject(OtpVerification.OtpType type) {
        return switch (type) {
            case REGISTER -> "[Xác Thực OTP] Đăng ký tài khoản — Thư Viện Nguyễn Huệ";
            case RESET_PASSWORD -> "[Xác Thực OTP] Đặt lại mật khẩu — Thư Viện Nguyễn Huệ";
        };
    }

    /**
     * Tạo email HTML chứa mã OTP với thiết kế chuyên nghiệp.
     */
    private String buildOtpEmailHtml(String otpCode, OtpVerification.OtpType type,
                                     String email) {
        String purposeText = switch (type) {
            case REGISTER -> "đăng ký tài khoản mới";
            case RESET_PASSWORD -> "đặt lại mật khẩu";
        };

        // Tách mã OTP thành các ký tự riêng lẻ để hiển thị đẹp
        StringBuilder otpBoxes = new StringBuilder();
        for (char c : otpCode.toCharArray()) {
            otpBoxes.append("""
                <td style="width:48px;height:56px;text-align:center;font-size:28px;
                           font-weight:700;color:#1565C0;background:#E3F2FD;
                           border:2px solid #90CAF9;border-radius:10px;
                           font-family:'Segoe UI',monospace;">%c</td>
                """.formatted(c));
        }

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Segoe UI',Arial,sans-serif;">
            <div style="max-width:520px;margin:20px auto;background:#ffffff;border-radius:12px;
                        overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                <!-- Header -->
                <div style="background:linear-gradient(135deg,#1565C0 0%%,#0D47A1 100%%);
                            padding:28px 24px;text-align:center;">
                    <h1 style="color:#fff;margin:0;font-size:20px;font-weight:600;">
                        📚 Thư Viện Nguyễn Huệ
                    </h1>
                    <p style="color:#BBDEFB;margin:8px 0 0;font-size:13px;">
                        Xác Thực Mã OTP
                    </p>
                </div>

                <!-- Body -->
                <div style="padding:28px 24px;text-align:center;">
                    <p style="font-size:15px;color:#333;margin:0 0 8px;">
                        Bạn đang yêu cầu <strong>%s</strong>
                    </p>
                    <p style="font-size:13px;color:#777;margin:0 0 24px;">
                        Vui lòng nhập mã xác thực bên dưới vào ứng dụng:
                    </p>

                    <!-- OTP Code -->
                    <table style="margin:0 auto 24px;border-collapse:separate;border-spacing:8px;">
                        <tr>%s</tr>
                    </table>

                    <p style="font-size:13px;color:#C62828;font-weight:600;margin:0 0 16px;">
                        ⏰ Mã có hiệu lực trong %d phút
                    </p>

                    <!-- Cảnh báo bảo mật -->
                    <div style="background:#FFF3E0;border-left:4px solid #FF9800;
                                padding:12px 16px;border-radius:6px;text-align:left;margin:16px 0;">
                        <p style="margin:0;font-size:13px;color:#E65100;font-weight:600;">
                            🔒 Lưu ý bảo mật
                        </p>
                        <p style="margin:6px 0 0;font-size:12px;color:#555;">
                            Không chia sẻ mã này với bất kỳ ai. Thư viện không bao giờ
                            yêu cầu bạn cung cấp mã OTP qua điện thoại hoặc tin nhắn.
                        </p>
                    </div>
                </div>

                <!-- Footer -->
                <div style="background:#FAFAFA;padding:16px 24px;border-top:1px solid #eee;
                            text-align:center;">
                    <p style="font-size:12px;color:#9e9e9e;margin:0;">
                        Email được gửi đến: <strong>%s</strong><br>
                        Nếu bạn không yêu cầu, vui lòng bỏ qua email này.
                    </p>
                </div>
            </div>
            </body>
            </html>
            """.formatted(
                purposeText,
                otpBoxes.toString(),
                OtpVerification.EXPIRY_MINUTES,
                email
            );
    }
}
