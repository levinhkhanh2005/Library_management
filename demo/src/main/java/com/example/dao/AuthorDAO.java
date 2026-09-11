package com.example.dao;

import com.example.model.Author;
import com.example.model.Book;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng authors và thống kê đầu sách theo tác giả.
 */
public class AuthorDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm tác giả =====================

    /**
     * Thêm tác giả mới. Trả về id được tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Author author) throws SQLException {
        String sql = """
                INSERT INTO authors (name, birth_year, death_year, nationality, biography)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, author.getName());
            if (author.getBirthYear() != null) ps.setInt(2, author.getBirthYear());
            else ps.setNull(2, Types.INTEGER);

            if (author.getDeathYear() != null) ps.setInt(3, author.getDeathYear());
            else ps.setNull(3, Types.INTEGER);

            ps.setString(4, author.getNationality());
            ps.setString(5, author.getBiography());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật tác giả =====================

    public boolean update(Author author) throws SQLException {
        String sql = """
                UPDATE authors SET name = ?, birth_year = ?, death_year = ?,
                                   nationality = ?, biography = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, author.getName());
            if (author.getBirthYear() != null) ps.setInt(2, author.getBirthYear());
            else ps.setNull(2, Types.INTEGER);

            if (author.getDeathYear() != null) ps.setInt(3, author.getDeathYear());
            else ps.setNull(3, Types.INTEGER);

            ps.setString(4, author.getNationality());
            ps.setString(5, author.getBiography());
            ps.setInt(6, author.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa tác giả =====================

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM authors WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    public Author findById(int id) throws SQLException {
        String sql = "SELECT * FROM authors WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public Author findByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        String sql = "SELECT * FROM authors WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public List<Author> findAll() throws SQLException {
        String sql = "SELECT * FROM authors ORDER BY name";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /**
     * Lấy toàn bộ danh sách tác giả kèm thống kê số đầu sách và tổng số bản sách.
     */
    public List<Author> findAllWithStats() throws SQLException {
        String sql = """
                SELECT a.id, a.name, a.birth_year, a.death_year, a.nationality, a.biography,
                       COUNT(b.id) AS book_count,
                       COALESCE(SUM(b.total_copies), 0) AS total_copies
                FROM authors a
                LEFT JOIN books b ON LOWER(TRIM(b.author)) = LOWER(TRIM(a.name))
                GROUP BY a.id, a.name, a.birth_year, a.death_year, a.nationality, a.biography
                ORDER BY a.name
                """;
        List<Author> list = new ArrayList<>();
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Author a = mapRow(rs);
                a.setBookCount(rs.getInt("book_count"));
                a.setTotalCopies(rs.getInt("total_copies"));
                list.add(a);
            }
        }
        return list;
    }

    /**
     * Tìm kiếm tác giả theo từ khóa (tên, quốc tịch, tiểu sử) kèm thống kê.
     */
    public List<Author> search(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return findAllWithStats();
        }
        String sql = """
                SELECT a.id, a.name, a.birth_year, a.death_year, a.nationality, a.biography,
                       COUNT(b.id) AS book_count,
                       COALESCE(SUM(b.total_copies), 0) AS total_copies
                FROM authors a
                LEFT JOIN books b ON LOWER(TRIM(b.author)) = LOWER(TRIM(a.name))
                WHERE a.name LIKE ? OR a.nationality LIKE ? OR a.biography LIKE ?
                GROUP BY a.id, a.name, a.birth_year, a.death_year, a.nationality, a.biography
                ORDER BY a.name
                """;
        String like = "%" + keyword.trim() + "%";
        List<Author> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Author a = mapRow(rs);
                a.setBookCount(rs.getInt("book_count"));
                a.setTotalCopies(rs.getInt("total_copies"));
                list.add(a);
            }
        }
        return list;
    }

    /**
     * Lấy danh sách tất cả các cuốn sách do tác giả sáng tác.
     */
    public List<Book> findBooksByAuthor(String authorName) throws SQLException {
        if (authorName == null || authorName.isBlank()) return new ArrayList<>();
        String sql = "SELECT * FROM books WHERE LOWER(TRIM(author)) = LOWER(TRIM(?)) ORDER BY title";
        List<Book> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, authorName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Book(
                    rs.getInt("id"),
                    rs.getString("isbn"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("category"),
                    rs.getString("publisher"),
                    rs.getInt("publish_year"),
                    rs.getInt("total_copies"),
                    rs.getInt("available_copies"),
                    rs.getString("description")
                ));
            }
        }
        return list;
    }

    /**
     * Đếm số sách đang mang tên tác giả này.
     */
    public int countBooksUsingAuthor(String authorName) throws SQLException {
        if (authorName == null || authorName.isBlank()) return 0;
        String sql = "SELECT COUNT(*) FROM books WHERE LOWER(TRIM(author)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, authorName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Cập nhật đồng bộ tên tác giả trong bảng books khi đổi tên tác giả.
     */
    public int updateAuthorInBooks(String oldAuthorName, String newAuthorName) throws SQLException {
        String sql = "UPDATE books SET author = ? WHERE LOWER(TRIM(author)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newAuthorName.trim());
            ps.setString(2, oldAuthorName.trim());
            return ps.executeUpdate();
        }
    }

    /**
     * Tổng số tác giả.
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM authors";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ===================== Mapping =====================

    private List<Author> mapList(ResultSet rs) throws SQLException {
        List<Author> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Author mapRow(ResultSet rs) throws SQLException {
        Integer birth = rs.getObject("birth_year") != null ? rs.getInt("birth_year") : null;
        Integer death = rs.getObject("death_year") != null ? rs.getInt("death_year") : null;
        return new Author(
            rs.getInt("id"),
            rs.getString("name"),
            birth,
            death,
            rs.getString("nationality"),
            rs.getString("biography")
        );
    }
}
