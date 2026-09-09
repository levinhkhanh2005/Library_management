package com.example.dao;

import com.example.model.Category;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng categories.
 */
public class CategoryDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm thể loại =====================

    /**
     * Thêm thể loại mới. Trả về id được tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật thể loại =====================

    /**
     * Cập nhật thông tin thể loại theo id.
     */
    public boolean update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name=?, description=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt   (3, category.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa thể loại =====================

    /**
     * Xóa thể loại theo id.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    /**
     * Lấy toàn bộ danh sách thể loại, sắp xếp theo tên.
     */
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT * FROM categories ORDER BY name COLLATE NOCASE";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /**
     * Tìm thể loại theo ID.
     */
    public Category findById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Tìm thể loại theo tên (không phân biệt hoa thường).
     */
    public Category findByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        String sql = "SELECT * FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Tìm kiếm thể loại theo từ khóa trong tên hoặc mô tả.
     */
    public List<Category> search(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String sql = "SELECT * FROM categories WHERE name LIKE ? OR description LIKE ? ORDER BY name COLLATE NOCASE";
        String like = "%" + keyword.trim() + "%";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            return mapList(ps.executeQuery());
        }
    }

    /**
     * Đếm số sách đang sử dụng tên thể loại này (không phân biệt hoa thường).
     */
    public int countBooksUsingCategory(String categoryName) throws SQLException {
        if (categoryName == null || categoryName.isBlank()) return 0;
        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(category)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, categoryName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Cập nhật tên thể loại cho các sách khi thể loại được đổi tên.
     */
    public int updateCategoryInBooks(String oldName, String newName) throws SQLException {
        if (oldName == null || oldName.isBlank() || newName == null || newName.isBlank()) return 0;
        String sql = "UPDATE books SET category = ? WHERE LOWER(TRIM(category)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newName.trim());
            ps.setString(2, oldName.trim());
            return ps.executeUpdate();
        }
    }

    /**
     * Tổng số thể loại.
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ===================== Mapping =====================

    private List<Category> mapList(ResultSet rs) throws SQLException {
        List<Category> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt   ("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("created_at")
        );
    }
}
