package com.example.dao;

import com.example.model.OtpVerification;
import com.example.util.DatabaseConnection;

import java.sql.*;

/**
 * DAO xử lý thao tác CRUD cho bảng otp_verifications.
 * Quản lý mã OTP dùng cho đăng ký tài khoản và đặt lại mật khẩu.
 */
public class OtpVerificationDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm OTP =====================

    /**
     * Thêm bản ghi OTP mới. Trả về id tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(OtpVerification otp) throws SQLException {
        String sql = """
                INSERT INTO otp_verifications (email, otp_code, type, payload, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, otp.getEmail());
            ps.setString(2, otp.getOtpCode());
            ps.setString(3, otp.getType().name());
            ps.setString(4, otp.getPayload());
            ps.setString(5, otp.getExpiresAt());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Truy vấn =====================

    /**
     * Tìm OTP mới nhất theo email và loại, chưa xác thực, chưa hết hạn, chưa vượt MAX_ATTEMPTS.
     * @return OTP hợp lệ hoặc null nếu không tìm thấy.
     */
    public OtpVerification findValidOtp(String email, OtpVerification.OtpType type) throws SQLException {
        String sql = """
                SELECT * FROM otp_verifications
                WHERE email = ? AND type = ? AND is_verified = 0
                  AND attempts < ? AND expires_at > datetime('now','localtime')
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, type.name());
            ps.setInt(3, OtpVerification.MAX_ATTEMPTS);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Tìm OTP theo id.
     */
    public OtpVerification findById(int id) throws SQLException {
        String sql = "SELECT * FROM otp_verifications WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Kiểm tra xem có OTP đang hiệu lực nào cho email này không
     * (dùng để tránh gửi spam OTP liên tục).
     * @return true nếu tồn tại OTP tạo trong vòng RESEND_COOLDOWN_SECONDS giây gần đây.
     */
    public boolean hasRecentOtp(String email, OtpVerification.OtpType type) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM otp_verifications
                WHERE email = ? AND type = ?
                  AND created_at > datetime('now','localtime','-%d seconds')
                """.formatted(OtpVerification.RESEND_COOLDOWN_SECONDS);
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, type.name());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ===================== Cập nhật =====================

    /**
     * Tăng số lần thử nhập OTP (attempts + 1).
     */
    public boolean incrementAttempts(int id) throws SQLException {
        String sql = "UPDATE otp_verifications SET attempts = attempts + 1 WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Đánh dấu OTP đã xác thực thành công.
     */
    public boolean markVerified(int id) throws SQLException {
        String sql = "UPDATE otp_verifications SET is_verified = 1 WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Vô hiệu hóa tất cả OTP cũ cho email và loại (khi tạo OTP mới).
     */
    public void invalidateOldOtps(String email, OtpVerification.OtpType type) throws SQLException {
        String sql = """
                UPDATE otp_verifications
                SET expires_at = datetime('now','localtime','-1 second')
                WHERE email = ? AND type = ? AND is_verified = 0
                  AND expires_at > datetime('now','localtime')
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, type.name());
            ps.executeUpdate();
        }
    }

    // ===================== Dọn dẹp =====================

    /**
     * Xóa các bản ghi OTP đã hết hạn quá 24 giờ (dọn rác).
     * @return số bản ghi đã xóa.
     */
    public int cleanupExpired() throws SQLException {
        String sql = """
                DELETE FROM otp_verifications
                WHERE expires_at < datetime('now','localtime','-24 hours')
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // ===================== Mapping =====================

    private OtpVerification mapRow(ResultSet rs) throws SQLException {
        return new OtpVerification(
            rs.getInt("id"),
            rs.getString("email"),
            rs.getString("otp_code"),
            OtpVerification.OtpType.fromString(rs.getString("type")),
            rs.getString("payload"),
            rs.getString("expires_at"),
            rs.getInt("attempts"),
            rs.getBoolean("is_verified"),
            rs.getString("created_at")
        );
    }
}
