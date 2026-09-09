package com.example;

import com.example.dao.EmailLogDAO;
import com.example.dao.SystemSettingDAO;
import com.example.model.Borrow;
import com.example.model.EmailConfig;
import com.example.model.EmailLog;
import com.example.model.Reader;
import com.example.service.EmailService;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.*;

public class EmailServiceTest {

    private SystemSettingDAO settingDAO;
    private EmailLogDAO emailLogDAO;
    private EmailService emailService;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        settingDAO = new SystemSettingDAO();
        emailLogDAO = new EmailLogDAO();
        emailService = new EmailService();

        // Xóa logs cũ và reset system_settings để môi trường test sạch sẽ
        try (var conn = com.example.util.DatabaseConnection.getInstance().getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM email_logs");
            stmt.execute("DELETE FROM system_settings");
        } catch (Exception ignored) {}
        DatabaseInitializer.initialize();
    }

    @Test
    public void testBuildOverdueEmailHtml() {
        Borrow borrow = new Borrow();
        borrow.setId(99);
        borrow.setBookTitle("Lập Trình Java Cơ Bản");
        borrow.setBorrowDate("01/08/2026");
        borrow.setDueDate("15/08/2026");

        Reader reader = new Reader();
        reader.setId(10);
        reader.setReaderCode("NDG-9999");
        reader.setFullName("Nguyễn Văn Kiểm Thử");
        reader.setEmail("test@example.com");

        long overdueDays = 5;
        double fineAmount = overdueDays * 2000.0;

        String html = emailService.buildOverdueEmailHtml(borrow, reader, overdueDays, fineAmount);

        assertNotNull(html);
        assertTrue(html.contains("Nguyễn Văn Kiểm Thử"));
        assertTrue(html.contains("NDG-9999"));
        assertTrue(html.contains("Lập Trình Java Cơ Bản"));
        assertTrue(html.contains("01/08/2026"));
        assertTrue(html.contains("15/08/2026"));
        assertTrue(html.contains("5 ngày"));
        assertTrue(html.contains("10,000 đ"));
        assertTrue(html.contains("Thư Viện Nguyễn Huệ"));
    }

    @Test
    public void testSystemSettingDAO() throws SQLException {
        // Test load default SMTP config
        EmailConfig config = settingDAO.loadEmailConfig();
        assertNotNull(config);
        assertEquals("smtp.gmail.com", config.getHost());
        assertEquals(587, config.getPort());
        assertTrue(config.isStartTls());

        // Test update and reload config
        config.setHost("smtp.office365.com");
        config.setPort(587);
        config.setUsername("library@school.edu.vn");
        config.setPassword("secret123");
        config.setFromName("Thư Viện Trường");
        config.setStartTls(true);

        settingDAO.saveEmailConfig(config);

        EmailConfig reloaded = settingDAO.loadEmailConfig();
        assertEquals("smtp.office365.com", reloaded.getHost());
        assertEquals(587, reloaded.getPort());
        assertEquals("library@school.edu.vn", reloaded.getUsername());
        assertEquals("secret123", reloaded.getPassword());
        assertEquals("Thư Viện Trường", reloaded.getFromName());
        assertTrue(reloaded.isStartTls());
    }

    @Test
    public void testEmailLogDAO() throws Exception {
        com.example.service.BorrowService borrowService = new com.example.service.BorrowService();
        Borrow borrow = borrowService.borrowBook(1, 1, null, "Test borrow for email log");
        assertNotNull(borrow);

        // Ghi log email
        EmailLog log = new EmailLog(
                borrow.getId(),
                borrow.getReaderId(),
                "reader@example.com",
                "⏰ Nhắc nhở quá hạn",
                "<p>Test content</p>",
                "SUCCESS",
                null
        );

        int logId = emailLogDAO.insert(log);
        assertTrue(logId > 0);

        // Kiểm tra truy vấn lần gửi gần nhất
        EmailLog lastLog = emailLogDAO.findLastSuccessByBorrowId(borrow.getId());
        assertNotNull(lastLog);
        assertEquals("reader@example.com", lastLog.getRecipientEmail());
        assertEquals("SUCCESS", lastLog.getStatus());

        // Kiểm tra chống spam trong 24 giờ
        assertTrue(emailLogDAO.wasSentWithinHours(borrow.getId(), 24));
        // Kiểm tra borrow id khác chưa gửi
        assertFalse(emailLogDAO.wasSentWithinHours(99999, 24));

        // Dọn dẹp trả sách
        borrowService.returnBook(borrow.getId());
    }
}
