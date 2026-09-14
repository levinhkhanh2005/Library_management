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
                INSERT INTO users (username, password, full_name, email, role, active, reader_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString (1, user.getUsername());
            ps.setString (2, user.getPassword());
            ps.setString (3, user.getFullName());
            ps.setString (4, user.getEmail());
            ps.setString (5, user.getRole().name());
            ps.setBoolean(6, user.isActive());
            ps.setInt    (7, user.getReaderId());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật người dùng =====================

    public boolean update(User user) throws SQLException {
        String sql = """
                UPDATE users SET username=?, full_name=?, email=?, role=?, active=?, reader_id=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString (1, user.getUsername());
            ps.setString (2, user.getFullName());
            ps.setString (3, user.getEmail());
            ps.setString (4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.setInt    (6, user.getReaderId());
            ps.setInt    (7, user.getId());
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

    /** Lấy danh sách người dùng theo vai trò. */
    public List<User> findByRole(User.Role role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, role.name());
            return mapList(ps.executeQuery());
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

    /** Tìm người dùng theo email. */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Tìm người dùng theo reader_id (liên kết với bảng readers). */
    public User findByReaderId(int readerId) throws SQLException {
        String sql = "SELECT * FROM users WHERE reader_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, readerId);
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

    /** Kiểm tra email đã tồn tại chưa (dùng khi đăng ký). */
    public boolean isEmailExists(String email) throws SQLException {
        if (email == null || email.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Đếm tổng số người dùng theo vai trò. */
    public int countByRole(User.Role role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ===================== Mapping =====================

    private List<User> mapList(ResultSet rs) throws SQLException {
        List<User> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        // Đọc email và reader_id an toàn (tương thích DB cũ chưa có cột)
        String email = null;
        int readerId = 0;
        try { email = rs.getString("email"); } catch (SQLException ignored) {}
        try { readerId = rs.getInt("reader_id"); } catch (SQLException ignored) {}

        return new User(
            rs.getInt    ("id"),
            rs.getString ("username"),
            rs.getString ("password"),
            rs.getString ("full_name"),
            email,
            User.Role.fromString(rs.getString("role")),
            rs.getBoolean("active"),
            readerId
        );
    }
}

