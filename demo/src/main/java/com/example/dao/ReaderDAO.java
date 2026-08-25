package com.example.dao;

import com.example.model.Reader;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng readers.
 */
public class ReaderDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ===================== Thêm độc giả =====================

    /**
     * Thêm độc giả mới. Trả về id tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Reader reader) throws SQLException {
        String sql = """
                INSERT INTO readers (reader_code, full_name, birth_date,
                                     phone, email, address, join_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reader.getReaderCode());
            ps.setString(2, reader.getFullName());
            ps.setString(3, reader.getBirthDate());
            ps.setString(4, reader.getPhone());
            ps.setString(5, reader.getEmail());
            ps.setString(6, reader.getAddress());
            ps.setString(7, reader.getJoinDate());
            ps.setString(8, reader.getStatus().name());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật độc giả =====================

    public boolean update(Reader reader) throws SQLException {
        String sql = """
                UPDATE readers SET reader_code=?, full_name=?, birth_date=?,
                                   phone=?, email=?, address=?, join_date=?, status=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, reader.getReaderCode());
            ps.setString(2, reader.getFullName());
            ps.setString(3, reader.getBirthDate());
            ps.setString(4, reader.getPhone());
            ps.setString(5, reader.getEmail());
            ps.setString(6, reader.getAddress());
            ps.setString(7, reader.getJoinDate());
            ps.setString(8, reader.getStatus().name());
            ps.setInt   (9, reader.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Khóa / mở khóa tài khoản độc giả. */
    public boolean updateStatus(int id, Reader.Status status) throws SQLException {
        String sql = "UPDATE readers SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt   (2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa độc giả =====================

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM readers WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    /** Lấy toàn bộ danh sách độc giả, sắp xếp theo tên. */
    public List<Reader> findAll() throws SQLException {
        String sql = "SELECT * FROM readers ORDER BY full_name";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Tìm độc giả theo id. */
    public Reader findById(int id) throws SQLException {
        String sql = "SELECT * FROM readers WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Tìm độc giả theo mã thẻ. */
    public Reader findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM readers WHERE reader_code = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /**
     * Tìm kiếm độc giả theo từ khóa (tên, mã thẻ, điện thoại, email).
     */
    public List<Reader> search(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String sql = """
                SELECT * FROM readers
                WHERE full_name   LIKE ? OR reader_code LIKE ?
                   OR phone       LIKE ? OR email       LIKE ?
                ORDER BY full_name
                """;
        String like = "%" + keyword.trim() + "%";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            return mapList(ps.executeQuery());
        }
    }

    /** Lấy danh sách độc giả theo trạng thái. */
    public List<Reader> findByStatus(Reader.Status status) throws SQLException {
        String sql = "SELECT * FROM readers WHERE status = ? ORDER BY full_name";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            return mapList(ps.executeQuery());
        }
    }

    /** Tổng số độc giả. */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM readers";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Số độc giả đang hoạt động. */
    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM readers WHERE status = 'ACTIVE'";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Lấy mã thẻ độc giả lớn nhất để tự sinh mã tiếp theo.
     * VD: NDG-0003 → trả về 3.
     */
    public int getMaxReaderCodeNumber() throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTR(reader_code, 5) AS INTEGER)) FROM readers";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ===================== Mapping =====================

    private List<Reader> mapList(ResultSet rs) throws SQLException {
        List<Reader> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Reader mapRow(ResultSet rs) throws SQLException {
        return new Reader(
            rs.getInt   ("id"),
            rs.getString("reader_code"),
            rs.getString("full_name"),
            rs.getString("birth_date"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("address"),
            rs.getString("join_date"),
            Reader.Status.fromString(rs.getString("status"))
        );
    }
}
