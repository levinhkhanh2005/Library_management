package com.example.dao;

import com.example.model.Borrow;
import com.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO xử lý toàn bộ thao tác CRUD cho bảng borrows.
 * Các truy vấn JOIN với books và readers để lấy thông tin hiển thị.
 */
public class BorrowDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // SQL JOIN dùng chung để lấy đủ thông tin hiển thị
    private static final String SELECT_WITH_JOIN = """
            SELECT b.*, bk.title AS book_title, bk.isbn AS book_isbn,
                   r.full_name AS reader_name, r.reader_code
            FROM borrows b
            JOIN books   bk ON b.book_id   = bk.id
            JOIN readers r  ON b.reader_id = r.id
            """;

    // ===================== Thêm phiếu mượn =====================

    /**
     * Tạo phiếu mượn mới. Trả về id tự sinh, hoặc -1 nếu thất bại.
     */
    public int insert(Borrow borrow) throws SQLException {
        String sql = """
                INSERT INTO borrows (book_id, reader_id, borrow_date, due_date, status, notes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, borrow.getBookId());
            ps.setInt   (2, borrow.getReaderId());
            ps.setString(3, borrow.getBorrowDate());
            ps.setString(4, borrow.getDueDate());
            ps.setString(5, borrow.getStatus().name());
            ps.setString(6, borrow.getNotes());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    // ===================== Cập nhật phiếu mượn =====================

    /**
     * Ghi nhận trả sách: cập nhật ngày trả, trạng thái, tiền phạt.
     */
    public boolean returnBook(int borrowId, String returnDate,
                              Borrow.Status status, double fineAmount) throws SQLException {
        String sql = """
                UPDATE borrows SET return_date=?, status=?, fine_amount=?
                WHERE id=?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, returnDate);
            ps.setString(2, status.name());
            ps.setDouble(3, fineAmount);
            ps.setInt   (4, borrowId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Cập nhật trạng thái phiếu mượn (dùng khi cập nhật quá hạn hàng loạt).
     */
    public boolean updateStatus(int borrowId, Borrow.Status status) throws SQLException {
        String sql = "UPDATE borrows SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt   (2, borrowId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Đánh dấu quá hạn tất cả phiếu mượn chưa trả mà đã qua due_date.
     * Gọi khi khởi động ứng dụng để đồng bộ trạng thái.
     * @param today ngày hôm nay định dạng dd/MM/yyyy
     */
    public int markOverdue(String today) throws SQLException {
        // So sánh ngày dạng dd/MM/yyyy: chuyển về yyyy-MM-dd để so sánh chuỗi đúng
        String sql = """
                UPDATE borrows SET status = 'OVERDUE'
                WHERE status = 'BORROWING'
                  AND SUBSTR(due_date,7,4)||SUBSTR(due_date,4,2)||SUBSTR(due_date,1,2)
                    < SUBSTR(?,7,4)||SUBSTR(?,4,2)||SUBSTR(?,1,2)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, today);
            ps.setString(2, today);
            ps.setString(3, today);
            return ps.executeUpdate();
        }
    }

    /**
     * Đếm số phiếu mượn đang hoạt động của một độc giả (BORROWING + OVERDUE).
     * Dùng để kiểm tra hạn mức mượn tối đa.
     */
    public int countActiveByReader(int readerId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM borrows
                WHERE reader_id = ? AND status IN ('BORROWING','OVERDUE')
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, readerId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Gia hạn hạn trả: cập nhật due_date mới và ghi chú gia hạn.
     */
    public boolean renewDueDate(int borrowId, String newDueDate, String newNotes) throws SQLException {
        String sql = "UPDATE borrows SET due_date = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, newDueDate);
            ps.setString(2, newNotes);
            ps.setInt   (3, borrowId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Báo mất sách: cập nhật trạng thái LOST, tiền bồi thường và ghi chú.
     */
    public boolean reportLost(int borrowId, double compensationFee, String newNotes) throws SQLException {
        String sql = """
                UPDATE borrows SET status = 'LOST', fine_amount = ?, notes = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDouble(1, compensationFee);
            ps.setString(2, newNotes);
            ps.setInt   (3, borrowId);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Xóa phiếu mượn =====================

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM borrows WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ===================== Truy vấn =====================

    /** Lấy toàn bộ phiếu mượn, mới nhất trước. */
    public List<Borrow> findAll() throws SQLException {
        String sql = SELECT_WITH_JOIN + " ORDER BY b.id DESC";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Tìm phiếu mượn theo id. */
    public Borrow findById(int id) throws SQLException {
        String sql = SELECT_WITH_JOIN + " WHERE b.id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        }
    }

    /** Lấy phiếu mượn đang hoạt động (BORROWING + OVERDUE). */
    public List<Borrow> findActive() throws SQLException {
        String sql = SELECT_WITH_JOIN +
                     " WHERE b.status IN ('BORROWING','OVERDUE') ORDER BY b.due_date";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Lấy danh sách quá hạn. */
    public List<Borrow> findOverdue() throws SQLException {
        String sql = SELECT_WITH_JOIN + " WHERE b.status = 'OVERDUE' ORDER BY b.due_date";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Lấy danh sách phiếu mượn bị mất sách. */
    public List<Borrow> findLost() throws SQLException {
        String sql = SELECT_WITH_JOIN + " WHERE b.status = 'LOST' ORDER BY b.id DESC";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return mapList(rs);
        }
    }

    /** Lấy lịch sử mượn của một độc giả. */
    public List<Borrow> findByReader(int readerId) throws SQLException {
        String sql = SELECT_WITH_JOIN + " WHERE b.reader_id = ? ORDER BY b.id DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, readerId);
            return mapList(ps.executeQuery());
        }
    }

    /** Lấy lịch sử mượn của một cuốn sách. */
    public List<Borrow> findByBook(int bookId) throws SQLException {
        String sql = SELECT_WITH_JOIN + " WHERE b.book_id = ? ORDER BY b.id DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return mapList(ps.executeQuery());
        }
    }

    /** Tìm kiếm theo từ khóa (tên sách, tên độc giả, mã thẻ). */
    public List<Borrow> search(String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) return findAll();
        String sql = SELECT_WITH_JOIN + """
                     WHERE bk.title      LIKE ? OR r.full_name  LIKE ?
                        OR r.reader_code LIKE ? OR bk.isbn      LIKE ?
                     ORDER BY b.id DESC
                     """;
        String like = "%" + keyword.trim() + "%";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            return mapList(ps.executeQuery());
        }
    }

    /** Kiểm tra độc giả có đang mượn sách cụ thể không. */
    public boolean isBookBorrowedByReader(int bookId, int readerId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM borrows
                WHERE book_id=? AND reader_id=? AND status IN ('BORROWING','OVERDUE')
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, readerId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ===================== Thống kê =====================

    /** Tổng số phiếu mượn đang hoạt động. */
    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrows WHERE status IN ('BORROWING','OVERDUE')";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Tổng số phiếu mượn quá hạn. */
    public int countOverdue() throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrows WHERE status = 'OVERDUE'";
        try (Statement stmt = getConn().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Top N sách được mượn nhiều nhất.
     * @return mảng Object[]: [bookTitle, borrowCount]
     */
    public List<Object[]> getTopBorrowedBooks(int limit) throws SQLException {
        String sql = """
                SELECT bk.title, COUNT(*) AS cnt
                FROM borrows b JOIN books bk ON b.book_id = bk.id
                GROUP BY b.book_id ORDER BY cnt DESC LIMIT ?
                """;
        List<Object[]> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{ rs.getString("title"), rs.getInt("cnt") });
            }
        }
        return list;
    }

    /**
     * Thống kê số lượt mượn theo tháng trong năm chỉ định.
     * @return mảng int[12] — chỉ số 0 = tháng 1, ..., 11 = tháng 12
     */
    public int[] getBorrowCountByMonth(int year) throws SQLException {
        String sql = """
                SELECT CAST(SUBSTR(borrow_date, 4, 2) AS INTEGER) AS month, COUNT(*) AS cnt
                FROM borrows
                WHERE SUBSTR(borrow_date, 7, 4) = ?
                GROUP BY month
                """;
        int[] counts = new int[12];
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, String.valueOf(year));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int month = rs.getInt("month");
                if (month >= 1 && month <= 12) {
                    counts[month - 1] = rs.getInt("cnt");
                }
            }
        }
        return counts;
    }

    // ===================== Mapping =====================

    private List<Borrow> mapList(ResultSet rs) throws SQLException {
        List<Borrow> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Borrow mapRow(ResultSet rs) throws SQLException {
        Borrow b = new Borrow(
            rs.getInt   ("id"),
            rs.getInt   ("book_id"),
            rs.getInt   ("reader_id"),
            rs.getString("borrow_date"),
            rs.getString("due_date"),
            rs.getString("return_date"),
            Borrow.Status.fromString(rs.getString("status")),
            rs.getDouble("fine_amount"),
            rs.getString("notes")
        );
        // Gán các trường join để hiển thị
        try { b.setBookTitle  (rs.getString("book_title"));  } catch (SQLException ignored) {}
        try { b.setBookIsbn   (rs.getString("book_isbn"));   } catch (SQLException ignored) {}
        try { b.setReaderName (rs.getString("reader_name")); } catch (SQLException ignored) {}
        try { b.setReaderCode (rs.getString("reader_code")); } catch (SQLException ignored) {}
        return b;
    }
}
