package com.example.dao;

import com.example.model.Book;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng books.
 */
public class BookDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm sách =====================

    /**
     * Thêm sách mới. Trả về id được tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Book book) throws SQLException {
        String sql = """
                INSERT INTO books (isbn, title, author, category, publisher,
                                   publish_year, total_copies, available_copies, description)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getCategory());
            ps.setString(5, book.getPublisher());
            ps.setInt   (6, book.getPublishYear());
            ps.setInt   (7, book.getTotalCopies());
            ps.setInt   (8, book.getAvailableCopies());
            ps.setString(9, book.getDescription());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật sách =====================

    /**
     * Cập nhật thông tin sách (trừ available_copies — dùng updateAvailableCopies).
     */
    public boolean update(Book book) throws SQLException {
        String sql = """
                UPDATE books SET isbn=?, title=?, author=?, category=?, publisher=?,
                                 publish_year=?, total_copies=?, available_copies=?, description=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getCategory());
            ps.setString(5, book.getPublisher());
            ps.setInt   (6, book.getPublishYear());
            ps.setInt   (7, book.getTotalCopies());
            ps.setInt   (8, book.getAvailableCopies());
            ps.setString(9, book.getDescription());
            ps.setInt   (10, book.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Tăng / giảm số bản sẵn sàng (delta = +1 khi trả, -1 khi mượn).
     */
    public boolean updateAvailableCopies(int bookId, int delta) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies + ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Giảm total_copies đi 1 khi sách bị báo mất.
     * Lưu ý: available_copies không đổi vì bản này đã bị trừ khi mượn.
     */
    public boolean decreaseTotalCopies(int bookId) throws SQLException {
        String sql = "UPDATE books SET total_copies = total_copies - 1 WHERE id = ? AND total_copies > 0";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa sách =====================

    /**
     * Xóa sách theo id. Chú ý: chỉ xóa được nếu không còn phiếu mượn đang hoạt động.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    /** Lấy toàn bộ danh sách sách, sắp xếp theo tên. */
    public List<Book> findAll() throws SQLException {
        String sql = "SELECT * FROM books ORDER BY title";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Tìm sách theo id. */
    public Book findById(int id) throws SQLException {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Tìm sách theo ISBN. */
    public Book findByIsbn(String isbn) throws SQLException {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, isbn);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Tìm kiếm sách theo từ khóa (tên, tác giả, thể loại, ISBN).
     * keyword rỗng → trả về toàn bộ.
     */
    public List<Book> search(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String sql = """
                SELECT * FROM books
                WHERE title    LIKE ? OR author   LIKE ?
                   OR category LIKE ? OR isbn     LIKE ?
                   OR publisher LIKE ?
                ORDER BY title
                """;
        String like = "%" + keyword.trim() + "%";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, like);
            return mapList(ps.executeQuery());
        }
    }

    /**
     * Tìm kiếm theo thể loại cụ thể.
     */
    public List<Book> findByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM books WHERE category = ? ORDER BY title";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, category);
            return mapList(ps.executeQuery());
        }
    }

    /** Lấy danh sách tất cả thể loại (distinct). */
    public List<String> findAllCategories() throws SQLException {
        String sql = "SELECT DISTINCT category FROM books WHERE category IS NOT NULL ORDER BY category";
        List<String> list = new ArrayList<>();
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("category"));
        }
        return list;
    }

    /** Tổng số đầu sách. */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM books";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Số sách đang được mượn (available_copies < total_copies). */
    public int countBorrowed() throws SQLException {
        String sql = "SELECT SUM(total_copies - available_copies) FROM books";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ===================== Mapping =====================

    private List<Book> mapList(ResultSet rs) throws SQLException {
        List<Book> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt   ("id"),
            rs.getString("isbn"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("category"),
            rs.getString("publisher"),
            rs.getInt   ("publish_year"),
            rs.getInt   ("total_copies"),
            rs.getInt   ("available_copies"),
            rs.getString("description")
        );
    }
}
