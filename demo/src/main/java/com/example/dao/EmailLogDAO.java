package com.example.dao;

import com.example.model.EmailLog;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO thao tác bảng email_logs.
 * Ghi nhận lịch sử gửi email và hỗ trợ kiểm tra chống spam.
 */
public class EmailLogDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm log =====================

    /** Lưu một bản ghi email log. Trả về id tự sinh. */
    public int insert(EmailLog log) throws SQLException {
        String sql = """
                INSERT INTO email_logs (borrow_id, reader_id, recipient_email,
                                        subject, content, status, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, log.getBorrowId());
            ps.setInt   (2, log.getReaderId());
            ps.setString(3, log.getRecipientEmail());
            ps.setString(4, log.getSubject());
            ps.setString(5, log.getContent());
            ps.setString(6, log.getStatus());
            ps.setString(7, log.getErrorMessage());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Truy vấn =====================

    /**
     * Tìm lần gửi email SUCCESS gần nhất cho phiếu mượn.
     * Trả null nếu chưa từng gửi thành công.
     */
    public EmailLog findLastSuccessByBorrowId(int borrowId) throws SQLException {
        String sql = """
                SELECT * FROM email_logs
                WHERE borrow_id = ? AND status = 'SUCCESS'
                ORDER BY sent_at DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Lấy danh sách lịch sử gửi email gần nhất (cho hiển thị). */
    public List<EmailLog> findRecentLogs(int limit) throws SQLException {
        String sql = "SELECT * FROM email_logs ORDER BY sent_at DESC LIMIT ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            return mapList(ps.executeQuery());
        }
    }

    /**
     * Kiểm tra phiếu mượn đã được gửi email thành công trong N giờ gần nhất.
     * Dùng để chống spam.
     * @param hours số giờ để kiểm tra (vd: 24 = 1 ngày)
     */
    public boolean wasSentWithinHours(int borrowId, int hours) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM email_logs
                WHERE borrow_id = ? AND status = 'SUCCESS'
                  AND sent_at >= datetime('now', 'localtime', '-' || ? || ' hours')
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            ps.setInt(2, hours);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ===================== Mapping =====================

    private List<EmailLog> mapList(ResultSet rs) throws SQLException {
        List<EmailLog> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private EmailLog mapRow(ResultSet rs) throws SQLException {
        EmailLog log = new EmailLog();
        log.setId(rs.getInt("id"));
        log.setBorrowId(rs.getInt("borrow_id"));
        log.setReaderId(rs.getInt("reader_id"));
        log.setRecipientEmail(rs.getString("recipient_email"));
        log.setSubject(rs.getString("subject"));
        log.setContent(rs.getString("content"));
        log.setStatus(rs.getString("status"));
        log.setErrorMessage(rs.getString("error_message"));
        log.setSentAt(rs.getString("sent_at"));
        return log;
    }
}
