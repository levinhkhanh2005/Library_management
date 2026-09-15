package com.example.dao;

import com.example.model.Category;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng categories.
 *
 * Quan hệ:
 * majors (1) ---- (N) categories
 *
 * categories.major_id -> majors.id
 */
public class CategoryDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // =========================================================
    // THÊM THỂ LOẠI
    // =========================================================

    /**
     * Thêm thể loại mới.
     * Trả về ID được tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Category category) throws SQLException {

        String sql = """
                INSERT INTO categories
                (name, description, major_id)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());

            if (category.getMajorId() > 0) {
                ps.setInt(3, category.getMajorId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    // =========================================================
    // CẬP NHẬT THỂ LOẠI
    // =========================================================

    /**
     * Cập nhật thông tin thể loại theo ID.
     */
    public boolean update(Category category) throws SQLException {

        String sql = """
                UPDATE categories
                SET name = ?,
                    description = ?,
                    major_id = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());

            if (category.getMajorId() > 0) {
                ps.setInt(3, category.getMajorId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            // ID của category phải là parameter thứ 4
            ps.setInt(4, category.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // XÓA THỂ LOẠI
    // =========================================================

    /**
     * Xóa thể loại theo ID.
     */
    public boolean delete(int id) throws SQLException {

        String sql = "DELETE FROM categories WHERE id = ?";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;
        }
    }

    // =========================================================
    // LẤY TẤT CẢ THỂ LOẠI
    // =========================================================

    /**
     * Lấy toàn bộ danh sách thể loại.
     *
     * JOIN với bảng majors để lấy tên khối ngành.
     */
    public List<Category> findAll() throws SQLException {

        String sql = """
                SELECT
                    c.*,
                    m.name AS major_name
                FROM categories c
                LEFT JOIN majors m
                    ON c.major_id = m.id
                ORDER BY c.name COLLATE NOCASE
                """;

        try (Statement stmt = getConn().createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            return mapList(rs);
        }
    }

    // =========================================================
    // TÌM THEO ID
    // =========================================================

    /**
     * Tìm thể loại theo ID.
     */
    public Category findById(int id) throws SQLException {

        String sql = """
                SELECT
                    c.*,
                    m.name AS major_name
                FROM categories c
                LEFT JOIN majors m
                    ON c.major_id = m.id
                WHERE c.id = ?
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // =========================================================
    // TÌM THEO TÊN
    // =========================================================

    /**
     * Tìm thể loại theo tên.
     *
     * Không phân biệt chữ hoa/chữ thường.
     */
    public Category findByName(String name) throws SQLException {

        if (name == null || name.isBlank()) {
            return null;
        }

        String sql = """
                SELECT
                    c.*,
                    m.name AS major_name
                FROM categories c
                LEFT JOIN majors m
                    ON c.major_id = m.id
                WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(?))
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // =========================================================
    // TÌM KIẾM
    // =========================================================

    /**
     * Tìm kiếm thể loại theo:
     * - Tên thể loại
     * - Mô tả
     * - Tên khối ngành
     */
    public List<Category> search(String keyword) throws SQLException {

        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        String sql = """
                SELECT
                    c.*,
                    m.name AS major_name
                FROM categories c
                LEFT JOIN majors m
                    ON c.major_id = m.id
                WHERE c.name LIKE ?
                   OR c.description LIKE ?
                   OR COALESCE(m.name, '') LIKE ?
                ORDER BY c.name COLLATE NOCASE
                """;

        String like = "%" + keyword.trim() + "%";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    // =========================================================
    // LẤY THỂ LOẠI THEO KHỐI NGÀNH
    // =========================================================

    /**
     * Lấy tất cả thể loại thuộc một khối ngành.
     *
     * Đây chính là phần tạo quan hệ:
     *
     * Một Major -> nhiều Category
     */
    public List<Category> findByMajorId(int majorId) throws SQLException {

        String sql = """
                SELECT
                    c.*,
                    m.name AS major_name
                FROM categories c
                LEFT JOIN majors m
                    ON c.major_id = m.id
                WHERE c.major_id = ?
                ORDER BY c.name COLLATE NOCASE
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setInt(1, majorId);

            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    // =========================================================
    // ĐẾM SÁCH ĐANG SỬ DỤNG THỂ LOẠI
    // =========================================================

    /**
     * Đếm số sách đang sử dụng tên thể loại này.
     *
     * Bảng books hiện tại đang lưu category bằng tên.
     */
    public int countBooksUsingCategory(String categoryName)
            throws SQLException {

        if (categoryName == null || categoryName.isBlank()) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*)
                FROM books
                WHERE LOWER(TRIM(category))
                    = LOWER(TRIM(?))
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setString(1, categoryName);

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // =========================================================
    // ĐỔI TÊN THỂ LOẠI TRONG BẢNG BOOKS
    // =========================================================

    /**
     * Khi đổi tên thể loại thì cập nhật tên thể loại
     * tương ứng trong bảng books.
     */
    public int updateCategoryInBooks(
            String oldName,
            String newName) throws SQLException {

        if (oldName == null
                || oldName.isBlank()
                || newName == null
                || newName.isBlank()) {

            return 0;
        }

        String sql = """
                UPDATE books
                SET category = ?
                WHERE LOWER(TRIM(category))
                    = LOWER(TRIM(?))
                """;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {

            ps.setString(1, newName.trim());
            ps.setString(2, oldName.trim());

            return ps.executeUpdate();
        }
    }

    // =========================================================
    // ĐẾM TỔNG SỐ THỂ LOẠI
    // =========================================================

    /**
     * Tổng số thể loại.
     */
    public int countAll() throws SQLException {

        String sql = "SELECT COUNT(*) FROM categories";

        try (Statement stmt = getConn().createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // =========================================================
    // MAPPING RESULTSET -> CATEGORY
    // =========================================================

    private List<Category> mapList(ResultSet rs)
            throws SQLException {

        List<Category> list = new ArrayList<>();

        while (rs.next()) {
            list.add(mapRow(rs));
        }

        return list;
    }

    /**
     * Chuyển một dòng ResultSet thành Category.
     */
    private Category mapRow(ResultSet rs)
            throws SQLException {

        return new Category(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("major_id"),
                rs.getString("major_name"),
                rs.getString("created_at"));
    }
}