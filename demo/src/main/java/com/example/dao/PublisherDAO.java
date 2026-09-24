package com.example.dao;

import com.example.model.Book;
import com.example.model.Publisher;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng publishers và thống kê đầu sách theo nhà xuất bản.
 */
public class PublisherDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm Nhà Xuất Bản =====================

    /**
     * Thêm nhà xuất bản mới. Trả về id được tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Publisher publisher) throws SQLException {
        String sql = """
                INSERT INTO publishers (name, address, phone, email, website, representative, description)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, publisher.getName());
            ps.setString(2, publisher.getAddress());
            ps.setString(3, publisher.getPhone());
            ps.setString(4, publisher.getEmail());
            ps.setString(5, publisher.getWebsite());
            ps.setString(6, publisher.getRepresentative());
            ps.setString(7, publisher.getDescription());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật Nhà Xuất Bản =====================

    public boolean update(Publisher publisher) throws SQLException {
        String sql = """
                UPDATE publishers SET name = ?, address = ?, phone = ?, email = ?,
                                      website = ?, representative = ?, description = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, publisher.getName());
            ps.setString(2, publisher.getAddress());
            ps.setString(3, publisher.getPhone());
            ps.setString(4, publisher.getEmail());
            ps.setString(5, publisher.getWebsite());
            ps.setString(6, publisher.getRepresentative());
            ps.setString(7, publisher.getDescription());
            ps.setInt(8, publisher.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa Nhà Xuất Bản =====================

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM publishers WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    public Publisher findById(int id) throws SQLException {
        String sql = "SELECT * FROM publishers WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public Publisher findByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        String sql = "SELECT * FROM publishers WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    public List<Publisher> findAll() throws SQLException {
        String sql = "SELECT * FROM publishers ORDER BY name";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /**
     * Lấy toàn bộ danh sách NXB kèm thống kê số đầu sách và tổng số bản sách trong kho.
     */
    public List<Publisher> findAllWithStats() throws SQLException {
        String sql = """
                SELECT p.id, p.name, p.address, p.phone, p.email, p.website,
                       p.representative, p.description, p.created_at,
                       COUNT(b.id) AS book_count,
                       COALESCE(SUM(b.total_copies), 0) AS total_copies
                FROM publishers p
                LEFT JOIN books b ON (
                    LOWER(TRIM(b.publisher)) = LOWER(TRIM(p.name))
                    OR LOWER(TRIM('NXB ' || b.publisher)) = LOWER(TRIM(p.name))
                    OR LOWER(TRIM(b.publisher)) = LOWER(TRIM(REPLACE(p.name, 'NXB ', '')))
                )
                GROUP BY p.id, p.name, p.address, p.phone, p.email, p.website,
                         p.representative, p.description, p.created_at
                ORDER BY p.name
                """;
        List<Publisher> list = new ArrayList<>();
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Publisher p = mapRow(rs);
                p.setBookCount(rs.getInt("book_count"));
                p.setTotalCopies(rs.getInt("total_copies"));
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Tìm kiếm NXB theo từ khóa (tên, địa chỉ, SĐT, email, người đại diện) kèm thống kê.
     */
    public List<Publisher> search(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return findAllWithStats();
        }
        String sql = """
                SELECT p.id, p.name, p.address, p.phone, p.email, p.website,
                       p.representative, p.description, p.created_at,
                       COUNT(b.id) AS book_count,
                       COALESCE(SUM(b.total_copies), 0) AS total_copies
                FROM publishers p
                LEFT JOIN books b ON (
                    LOWER(TRIM(b.publisher)) = LOWER(TRIM(p.name))
                    OR LOWER(TRIM('NXB ' || b.publisher)) = LOWER(TRIM(p.name))
                    OR LOWER(TRIM(b.publisher)) = LOWER(TRIM(REPLACE(p.name, 'NXB ', '')))
                )
                WHERE p.name LIKE ? OR p.address LIKE ? OR p.phone LIKE ?
                   OR p.email LIKE ? OR p.representative LIKE ?
                GROUP BY p.id, p.name, p.address, p.phone, p.email, p.website,
                         p.representative, p.description, p.created_at
                ORDER BY p.name
                """;
        String like = "%" + keyword.trim() + "%";
        List<Publisher> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Publisher p = mapRow(rs);
                p.setBookCount(rs.getInt("book_count"));
                p.setTotalCopies(rs.getInt("total_copies"));
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Lấy danh sách tất cả các cuốn sách do nhà xuất bản này phát hành.
     */
    public List<Book> findBooksByPublisher(String publisherName) throws SQLException {
        if (publisherName == null || publisherName.isBlank()) return new ArrayList<>();
        String sql = """
                SELECT * FROM books
                AND (LOWER(TRIM(publisher)) = LOWER(TRIM(?))
                   OR LOWER(TRIM('NXB ' || publisher)) = LOWER(TRIM(?))
                   OR LOWER(TRIM(publisher)) = LOWER(TRIM(REPLACE(?, 'NXB ', ''))))
                ORDER BY title
                """;
        List<Book> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, publisherName);
            ps.setString(2, publisherName);
            ps.setString(3, publisherName);
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
     * Đếm số sách đang mang tên nhà xuất bản này.
     */
    public int countBooksUsingPublisher(String publisherName) throws SQLException {
        if (publisherName == null || publisherName.isBlank()) return 0;
        String sql = """
                SELECT COUNT(*) FROM books
                WHERE deleted_at IS NULL
                WHERE LOWER(TRIM(publisher)) = LOWER(TRIM(?))
                   OR LOWER(TRIM('NXB ' || publisher)) = LOWER(TRIM(?))
                   OR LOWER(TRIM(publisher)) = LOWER(TRIM(REPLACE(?, 'NXB ', '')))
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, publisherName);
            ps.setString(2, publisherName);
            ps.setString(3, publisherName);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Cập nhật đồng bộ tên nhà xuất bản trong bảng books khi đổi tên NXB.
     */
    public int updatePublisherInBooks(String oldPublisherName, String newPublisherName) throws SQLException {
        String sql = """
                UPDATE books SET publisher = ?
                WHERE LOWER(TRIM(publisher)) = LOWER(TRIM(?))
                   OR LOWER(TRIM('NXB ' || publisher)) = LOWER(TRIM(?))
                   OR LOWER(TRIM(publisher)) = LOWER(TRIM(REPLACE(?, 'NXB ', '')))
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newPublisherName.trim());
            ps.setString(2, oldPublisherName.trim());
            ps.setString(3, oldPublisherName.trim());
            ps.setString(4, oldPublisherName.trim());
            return ps.executeUpdate();
        }
    }

    /**
     * Tổng số nhà xuất bản.
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM publishers";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ===================== Mapping =====================

    private List<Publisher> mapList(ResultSet rs) throws SQLException {
        List<Publisher> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Publisher mapRow(ResultSet rs) throws SQLException {
        return new Publisher(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("address"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("website"),
            rs.getString("representative"),
            rs.getString("description"),
            rs.getString("created_at")
        );
    }
}
