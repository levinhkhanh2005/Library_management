package com.example.dao;

import com.example.model.EmailConfig;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * DAO thao tác bảng system_settings.
 * Đọc/ghi cấu hình SMTP dưới dạng cặp key-value.
 */
public class SystemSettingDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Đọc/ghi đơn lẻ =====================

    /** Đọc giá trị setting theo key. Trả null nếu không tồn tại. */
    public String get(String key) throws SQLException {
        String sql = "SELECT value FROM system_settings WHERE key = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("value") : null;
        }
    }

    /** Ghi (INSERT hoặc UPDATE) giá trị setting. */
    public void set(String key, String value) throws SQLException {
        String sql = """
                INSERT INTO system_settings (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    // ===================== Đọc/ghi EmailConfig =====================

    /** Đọc toàn bộ cấu hình SMTP thành đối tượng EmailConfig. */
    public EmailConfig loadEmailConfig() throws SQLException {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT key, value FROM system_settings WHERE key LIKE 'smtp.%'";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("key"), rs.getString("value"));
            }
        }

        EmailConfig config = new EmailConfig();
        config.setHost(map.getOrDefault("smtp.host", "smtp.gmail.com"));
        try {
            config.setPort(Integer.parseInt(map.getOrDefault("smtp.port", "587")));
        } catch (NumberFormatException e) {
            config.setPort(587);
        }
        config.setUsername(map.getOrDefault("smtp.username", ""));
        config.setPassword(map.getOrDefault("smtp.password", ""));
        config.setFromName(map.getOrDefault("smtp.from_name", "Thư Viện Nguyễn Huệ"));
        config.setStartTls("true".equalsIgnoreCase(map.getOrDefault("smtp.tls", "true")));
        return config;
    }

    /** Lưu cấu hình EmailConfig vào DB. */
    public void saveEmailConfig(EmailConfig config) throws SQLException {
        set("smtp.host",      config.getHost());
        set("smtp.port",      String.valueOf(config.getPort()));
        set("smtp.username",  config.getUsername());
        set("smtp.password",  config.getPassword());
        set("smtp.from_name", config.getFromName());
        set("smtp.tls",       String.valueOf(config.isStartTls()));
    }
}
