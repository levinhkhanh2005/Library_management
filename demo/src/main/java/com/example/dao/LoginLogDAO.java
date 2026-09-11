package com.example.dao;

import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO xử lý toàn bộ thao tác đọc/ghi cho bảng login_logs.
 * Dùng để theo dõi lịch sử đăng nhập / đăng xuất của người dùng.
 */
public class LoginLogDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Ghi log =====================

    /**
     * Ghi một bản ghi đăng nhập hoặc đăng xuất.
     *
     * @param userId   ID người dùng
     * @param username Tên đăng nhập
     * @param fullName Họ tên đầy đủ
     * @param role     Vai trò (ADMIN / LIBRARIAN)
     * @param action   "LOGIN" hoặc "LOGOUT"
     */
    public void insert(int userId, String username, String fullName,
                       String role, String action) {
        String sql = """
                INSERT INTO login_logs (user_id, username, full_name, role, action)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt   (1, userId);
            ps.setString(2, username);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.setString(5, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[LoginLogDAO] Lỗi ghi log: " + e.getMessage());
        }
    }

    // ===================== Truy vấn tổng hợp =====================

    /**
     * Đếm số lần đăng nhập hôm nay.
     */
    public int countLoginsToday() {
        String sql = """
                SELECT COUNT(*) FROM login_logs
                WHERE action = 'LOGIN'
                  AND date(logged_at) = date('now','localtime')
                """;
        return queryCount(sql);
    }

    /**
     * Đếm số lần đăng nhập trong 7 ngày gần nhất.
     */
    public int countLoginsLastWeek() {
        String sql = """
                SELECT COUNT(*) FROM login_logs
                WHERE action = 'LOGIN'
                  AND logged_at >= datetime('now','localtime','-6 days','start of day')
                """;
        return queryCount(sql);
    }

    /**
     * Đếm số người dùng khác nhau đã đăng nhập trong 7 ngày gần nhất.
     */
    public int countDistinctUsersLastWeek() {
        String sql = """
                SELECT COUNT(DISTINCT user_id) FROM login_logs
                WHERE action = 'LOGIN'
                  AND logged_at >= datetime('now','localtime','-6 days','start of day')
                """;
        return queryCount(sql);
    }

    /**
     * Lấy số lần đăng nhập theo từng ngày trong 7 ngày gần nhất.
     * Trả về Map: ngày (yyyy-MM-dd) → số lần đăng nhập.
     * Các ngày không có dữ liệu vẫn xuất hiện với giá trị 0.
     */
    public Map<String, Integer> getWeeklyLoginByDay() {
        // Tạo map khởi tạo 7 ngày với giá trị 0
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            String sql = "SELECT date('now','localtime','-" + i + " days')";
            try (Statement st = getConn().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) result.put(rs.getString(1), 0);
            } catch (SQLException e) {
                System.err.println("[LoginLogDAO] Lỗi tính ngày: " + e.getMessage());
            }
        }

        // Lấy dữ liệu thực tế từ DB
        String sql = """
                SELECT date(logged_at) AS day, COUNT(*) AS cnt
                FROM login_logs
                WHERE action = 'LOGIN'
                  AND logged_at >= datetime('now','localtime','-6 days','start of day')
                GROUP BY day
                """;
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String day = rs.getString("day");
                if (result.containsKey(day)) {
                    result.put(day, rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[LoginLogDAO] Lỗi lấy weekly login: " + e.getMessage());
        }

        return result;
    }

    /**
     * Tính số lần đăng nhập trung bình mỗi ngày trong 7 ngày qua.
     * Chỉ tính các ngày có ít nhất 1 lần đăng nhập.
     */
    public double getAverageLoginsPerDay() {
        int total = countLoginsLastWeek();
        return total / 7.0;
    }

    // ===================== Truy vấn danh sách log =====================

    /**
     * Lấy N bản ghi log gần nhất (mới nhất trước).
     *
     * @param limit số bản ghi tối đa
     * @return danh sách mảng: [id, username, full_name, role, action, logged_at]
     */
    public List<Object[]> getRecentLogs(int limit) {
        String sql = """
                SELECT id, username, full_name, role, action, logged_at
                FROM login_logs
                ORDER BY id DESC
                LIMIT ?
                """;
        List<Object[]> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt   ("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    rs.getString("action"),
                    rs.getString("logged_at")
                });
            }
        } catch (SQLException e) {
            System.err.println("[LoginLogDAO] Lỗi lấy recent logs: " + e.getMessage());
        }
        return list;
    }

    // ===================== Helper =====================

    private int queryCount(String sql) {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("[LoginLogDAO] Lỗi query count: " + e.getMessage());
            return 0;
        }
    }
}
