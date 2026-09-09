package com.example.service;

import com.example.dao.BorrowDAO;
import com.example.dao.EmailLogDAO;
import com.example.dao.SystemSettingDAO;
import com.example.model.Borrow;
import com.example.model.EmailConfig;
import com.example.model.EmailLog;
import com.example.model.Reader;
import com.example.dao.ReaderDAO;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Service xử lý gửi email nhắc nhở quá hạn.
 * Hỗ trợ gửi đơn, gửi hàng loạt, kiểm tra kết nối SMTP,
 * và tạo nội dung email HTML chuyên nghiệp.
 */
public class EmailService {

    private final SystemSettingDAO settingDAO = new SystemSettingDAO();
    private final EmailLogDAO     emailLogDAO = new EmailLogDAO();
    private final BorrowDAO       borrowDAO   = new BorrowDAO();
    private final ReaderDAO       readerDAO   = new ReaderDAO();

    /** Tiền phạt mỗi ngày quá hạn (đồng). */
    private static final double FINE_PER_DAY = 2000.0;

    /** Số giờ chống spam giữa 2 lần gửi cho cùng phiếu mượn. */
    private static final int ANTI_SPAM_HOURS = 24;

    // ================================================================
    //  Gửi email qua SMTP
    // ================================================================

    /**
     * Gửi email HTML qua SMTP.
     * @return null nếu thành công, message lỗi nếu thất bại.
     */
    public String sendEmail(EmailConfig config, String to, String subject, String htmlBody) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable",  String.valueOf(config.isStartTls()));
            props.put("mail.smtp.host",             config.getHost());
            props.put("mail.smtp.port",             String.valueOf(config.getPort()));
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout",           "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getUsername(), config.getFromName()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            return null; // thành công

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Kiểm tra kết nối SMTP bằng cách gửi thư thử nghiệm.
     * @return null nếu thành công, message lỗi nếu thất bại.
     */
    public String testConnection(EmailConfig config, String testEmail) {
        String subject = "[THỬ NGHIỆM] Kết nối SMTP - Thư Viện Nguyễn Huệ";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:500px;margin:auto;padding:24px;
                            border:1px solid #e0e0e0;border-radius:8px;">
                    <h2 style="color:#1565C0;">✅ Kết nối SMTP thành công!</h2>
                    <p>Email này xác nhận rằng cấu hình SMTP của bạn hoạt động bình thường.</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:16px 0;">
                    <p style="color:#757575;font-size:13px;">
                        Host: %s<br>Port: %d<br>TLS: %s
                    </p>
                    <p style="color:#9e9e9e;font-size:12px;">— Hệ thống Quản Lý Thư Viện Nguyễn Huệ</p>
                </div>
                """.formatted(config.getHost(), config.getPort(),
                              config.isStartTls() ? "Bật" : "Tắt");
        return sendEmail(config, testEmail, subject, html);
    }

    // ================================================================
    //  Gửi nhắc nhở quá hạn
    // ================================================================

    /**
     * Gửi email nhắc nhở cho một phiếu mượn quá hạn.
     * Kiểm tra chống spam (24h) trước khi gửi.
     * @return thông báo kết quả (thành công hoặc lỗi).
     */
    public String sendOverdueReminder(int borrowId) {
        try {
            // Kiểm tra chống spam
            if (emailLogDAO.wasSentWithinHours(borrowId, ANTI_SPAM_HOURS)) {
                EmailLog lastLog = emailLogDAO.findLastSuccessByBorrowId(borrowId);
                String lastTime = lastLog != null ? lastLog.getSentAt() : "gần đây";
                return "⚠ Phiếu #" + borrowId + " đã được nhắc nhở lúc " + lastTime
                     + ". Vui lòng đợi " + ANTI_SPAM_HOURS + " giờ giữa 2 lần gửi.";
            }

            // Load cấu hình
            EmailConfig config = settingDAO.loadEmailConfig();
            if (!config.isValid()) {
                return "❌ Chưa cấu hình SMTP. Vui lòng vào Cài Đặt → Cấu hình Email.";
            }

            // Load dữ liệu phiếu mượn & độc giả
            Borrow borrow = borrowDAO.findById(borrowId);
            if (borrow == null) {
                return "❌ Không tìm thấy phiếu mượn #" + borrowId;
            }

            Reader reader = readerDAO.findById(borrow.getReaderId());
            if (reader == null) {
                return "❌ Không tìm thấy độc giả cho phiếu #" + borrowId;
            }

            String email = reader.getEmail();
            if (email == null || email.isBlank()) {
                return "❌ Độc giả " + reader.getFullName() + " chưa có email.";
            }

            // Tính số ngày quá hạn & tiền phạt
            long overdueDays = calculateOverdueDays(borrow.getDueDate());
            double fineAmount = overdueDays * FINE_PER_DAY;

            // Build email content
            String subject = "⏰ Nhắc nhở: Sách mượn quá hạn - Thư Viện Nguyễn Huệ";
            String html = buildOverdueEmailHtml(borrow, reader, overdueDays, fineAmount);

            // Gửi
            String error = sendEmail(config, email, subject, html);

            // Log
            EmailLog log = new EmailLog(
                borrowId, reader.getId(), email, subject, html,
                error == null ? "SUCCESS" : "FAILED",
                error
            );
            emailLogDAO.insert(log);

            if (error == null) {
                return "✅ Đã gửi email nhắc nhở đến " + email + " (Phiếu #" + borrowId + ")";
            } else {
                return "❌ Gửi thất bại: " + error;
            }

        } catch (SQLException e) {
            return "❌ Lỗi database: " + e.getMessage();
        }
    }

    /**
     * Gửi email nhắc nhở hàng loạt cho danh sách phiếu mượn.
     * Chạy trong background thread (SwingWorker), gọi callback để cập nhật progress.
     * @param borrowIds danh sách borrowId cần gửi
     * @param progressCallback callback nhận thông báo tiến trình
     * @return tổng kết: "Đã gửi X/Y email thành công."
     */
    public String sendBulkOverdueReminders(List<Integer> borrowIds,
                                           Consumer<String> progressCallback) {
        int success = 0;
        int failed  = 0;
        int skipped = 0;

        for (int i = 0; i < borrowIds.size(); i++) {
            int borrowId = borrowIds.get(i);
            String msg = "[" + (i + 1) + "/" + borrowIds.size() + "] Phiếu #" + borrowId + ": ";

            String result = sendOverdueReminder(borrowId);
            if (result.startsWith("✅")) {
                success++;
                msg += "Thành công";
            } else if (result.startsWith("⚠")) {
                skipped++;
                msg += "Bỏ qua (đã gửi gần đây)";
            } else {
                failed++;
                msg += result;
            }

            if (progressCallback != null) {
                progressCallback.accept(msg);
            }

            // Delay giữa mỗi email để tránh rate limit (300ms)
            if (i < borrowIds.size() - 1) {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
        }

        return String.format("Hoàn thành: %d thành công, %d bỏ qua, %d thất bại (tổng %d phiếu)",
                             success, skipped, failed, borrowIds.size());
    }

    // ================================================================
    //  Xem trước nội dung Email (Preview)
    // ================================================================

    /**
     * Tạo nội dung email xem trước cho phiếu mượn (không gửi).
     * @return mảng String[2]: [0] = subject, [1] = htmlBody
     */
    public String[] previewOverdueEmail(int borrowId) {
        try {
            Borrow borrow = borrowDAO.findById(borrowId);
            if (borrow == null) return new String[]{"", "<p>Không tìm thấy phiếu mượn</p>"};

            Reader reader = readerDAO.findById(borrow.getReaderId());
            if (reader == null) return new String[]{"", "<p>Không tìm thấy độc giả</p>"};

            long overdueDays = calculateOverdueDays(borrow.getDueDate());
            double fineAmount = overdueDays * FINE_PER_DAY;

            String subject = "⏰ Nhắc nhở: Sách mượn quá hạn - Thư Viện Nguyễn Huệ";
            String html = buildOverdueEmailHtml(borrow, reader, overdueDays, fineAmount);

            return new String[]{subject, html};
        } catch (SQLException e) {
            return new String[]{"Lỗi", "<p>Lỗi: " + e.getMessage() + "</p>"};
        }
    }

    // ================================================================
    //  HTML Email Template
    // ================================================================

    /**
     * Tạo nội dung email HTML nhắc nhở quá hạn, thiết kế chuyên nghiệp
     * với thương hiệu Thư Viện Nguyễn Huệ.
     */
    public String buildOverdueEmailHtml(Borrow borrow, Reader reader,
                                        long overdueDays, double fineAmount) {
        DecimalFormat df = new DecimalFormat("#,##0");

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Segoe UI',Arial,sans-serif;">
            <div style="max-width:600px;margin:20px auto;background:#ffffff;border-radius:12px;
                        overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                <!-- Header Banner -->
                <div style="background:linear-gradient(135deg,#1565C0 0%%,#0D47A1 100%%);
                            padding:32px 24px;text-align:center;">
                    <h1 style="color:#fff;margin:0;font-size:22px;font-weight:600;">
                        📚 Thư Viện Nguyễn Huệ
                    </h1>
                    <p style="color:#BBDEFB;margin:8px 0 0;font-size:14px;">
                        Thông Báo Nhắc Nhở Sách Quá Hạn
                    </p>
                </div>

                <!-- Body -->
                <div style="padding:28px 24px;">
                    <p style="font-size:15px;color:#333;margin:0 0 16px;">
                        Kính gửi <strong>%s</strong> (Mã thẻ: %s),
                    </p>

                    <p style="font-size:14px;color:#555;line-height:1.6;margin:0 0 20px;">
                        Thư viện xin thông báo rằng sách bạn đang mượn đã <strong style="color:#C62828;">
                        quá hạn trả %d ngày</strong>. Vui lòng đến thư viện để trả sách sớm nhất
                        có thể để tránh phát sinh thêm tiền phạt.
                    </p>

                    <!-- Info Table -->
                    <table style="width:100%%;border-collapse:collapse;margin:0 0 20px;
                                  font-size:14px;border:1px solid #e0e0e0;border-radius:8px;">
                        <tr style="background:#E3F2FD;">
                            <td style="padding:10px 14px;font-weight:600;color:#1565C0;
                                       border-bottom:1px solid #BBDEFB;width:40%%;">📖 Tên sách</td>
                            <td style="padding:10px 14px;border-bottom:1px solid #BBDEFB;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding:10px 14px;font-weight:600;color:#555;
                                       border-bottom:1px solid #f0f0f0;">📅 Ngày mượn</td>
                            <td style="padding:10px 14px;border-bottom:1px solid #f0f0f0;">%s</td>
                        </tr>
                        <tr style="background:#FFF8E1;">
                            <td style="padding:10px 14px;font-weight:600;color:#F57F17;
                                       border-bottom:1px solid #FFF9C4;">⏰ Hạn trả</td>
                            <td style="padding:10px 14px;border-bottom:1px solid #FFF9C4;
                                       color:#E65100;font-weight:600;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding:10px 14px;font-weight:600;color:#555;
                                       border-bottom:1px solid #f0f0f0;">📊 Số ngày quá hạn</td>
                            <td style="padding:10px 14px;border-bottom:1px solid #f0f0f0;
                                       color:#C62828;font-weight:700;">%d ngày</td>
                        </tr>
                        <tr style="background:#FFEBEE;">
                            <td style="padding:10px 14px;font-weight:600;color:#B71C1C;">
                                💰 Tiền phạt tạm tính</td>
                            <td style="padding:10px 14px;color:#C62828;font-weight:700;
                                       font-size:16px;">%s đ</td>
                        </tr>
                    </table>

                    <p style="font-size:13px;color:#777;line-height:1.5;margin:0 0 8px;">
                        <em>* Tiền phạt được tính theo mức %s đ/ngày quá hạn.
                        Số tiền thực tế sẽ được tính tại thời điểm trả sách.</em>
                    </p>

                    <!-- CTA -->
                    <div style="background:#E8F5E9;border-left:4px solid #43A047;
                                padding:14px 16px;border-radius:6px;margin:20px 0;">
                        <p style="margin:0;font-size:14px;color:#2E7D32;font-weight:600;">
                            🏛 Vui lòng đến trả sách tại Thư Viện Nguyễn Huệ
                        </p>
                        <p style="margin:6px 0 0;font-size:13px;color:#555;">
                            Giờ mở cửa: Thứ 2 – Thứ 7, 7:30 – 17:00<br>
                            Liên hệ: (028) 1234 5678
                        </p>
                    </div>
                </div>

                <!-- Footer -->
                <div style="background:#FAFAFA;padding:16px 24px;border-top:1px solid #eee;
                            text-align:center;">
                    <p style="font-size:12px;color:#9e9e9e;margin:0;">
                        Email này được gửi tự động từ hệ thống Quản Lý Thư Viện Nguyễn Huệ.<br>
                        Nếu bạn đã trả sách, vui lòng bỏ qua email này.
                    </p>
                </div>
            </div>
            </body>
            </html>
            """.formatted(
                reader.getFullName(),
                reader.getReaderCode(),
                overdueDays,
                borrow.getBookTitle() != null ? borrow.getBookTitle() : "(Không rõ)",
                borrow.getBorrowDate(),
                borrow.getDueDate(),
                overdueDays,
                df.format(fineAmount),
                df.format(FINE_PER_DAY)
            );
    }

    // ================================================================
    //  Utility
    // ================================================================

    /**
     * Tính số ngày quá hạn từ chuỗi due_date (dd/MM/yyyy) đến hôm nay.
     * @return số ngày quá hạn (>0 nếu đã quá hạn, <=0 nếu chưa)
     */
    public static long calculateOverdueDays(String dueDateStr) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate dueDate = LocalDate.parse(dueDateStr, fmt);
            LocalDate today   = LocalDate.now();
            return ChronoUnit.DAYS.between(dueDate, today);
        } catch (Exception e) {
            return 0;
        }
    }
}
