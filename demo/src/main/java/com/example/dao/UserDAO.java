package com.example.dao;

import com.example.model.User;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng users.
 */
public class UserDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm người dùng =====================

    /**
     * Thêm người dùng mới. Trả về id tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, password, full_name, role, active)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString (1, user.getUsername());
            ps.setString (2, user.getPassword());
            ps.setString (3, user.getFullName());
            ps.setString (4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật người dùng =====================

    public boolean update(User user) throws SQLException {
        String sql = """
                UPDATE users SET username=?, full_name=?, role=?, active=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString (1, user.getUsername());
            ps.setString (2, user.getFullName());
            ps.setString (3, user.getRole().name());
            ps.setBoolean(4, user.isActive());
            ps.setInt    (5, user.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Đổi mật khẩu. */
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt   (2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Kích hoạt / vô hiệu hóa tài khoản. */
    public boolean setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt    (2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa người dùng =====================

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    /** Lấy toàn bộ danh sách người dùng. */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY full_name";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Tìm người dùng theo id. */
    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Tìm người dùng theo username. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Xác thực đăng nhập: tìm user khớp username + password + active = true.
     * @return User nếu đăng nhập thành công, null nếu thất bại
     */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND active = 1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Kiểm tra username đã tồn tại chưa (dùng khi tạo mới). */
    public boolean isUsernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ===================== Mapping =====================

    private List<User> mapList(ResultSet rs) throws SQLException {
        List<User> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt    ("id"),
            rs.getString ("username"),
            rs.getString ("password"),
            rs.getString ("full_name"),
            User.Role.fromString(rs.getString("role")),
            rs.getBoolean("active")
        );
    }
}
