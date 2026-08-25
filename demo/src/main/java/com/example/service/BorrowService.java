package com.example.service;

import com.example.dao.BookDAO;
import com.example.dao.BorrowDAO;
import com.example.dao.ReaderDAO;
import com.example.model.Book;
import com.example.model.Borrow;
import com.example.model.Reader;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.util.DatabaseConnection;

/**
 * Service quản lý nghiệp vụ mượn/trả sách.
 * Tiền phạt: 2.000đ/ngày quá hạn.
 */
public class BorrowService {

    /** Tiền phạt mỗi ngày quá hạn (VND). */
    public static final double FINE_PER_DAY = 2_000.0;

    /** Số ngày mượn mặc định. */
    public static final int DEFAULT_BORROW_DAYS = 14;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowDAO  borrowDAO  = new BorrowDAO();
    private final BookDAO    bookDAO    = new BookDAO();
    private final ReaderDAO  readerDAO  = new ReaderDAO();

    // ===================== Mượn sách =====================

    /**
     * Tạo phiếu mượn sách.
     * Kiểm tra: sách còn bản, độc giả hoạt động, chưa mượn cuốn đó.
     *
     * @param bookId    ID sách cần mượn
     * @param readerId  ID độc giả
     * @param dueDateStr ngày hạn trả (dd/MM/yyyy), null → tự tính DEFAULT_BORROW_DAYS ngày
     * @param notes     ghi chú
     * @return Borrow vừa tạo
     */
    public Borrow borrowBook(int bookId, int readerId,
                             String dueDateStr, String notes) throws SQLException {

        // 1. Kiểm tra sách tồn tại và còn bản
        Book book = bookDAO.findById(bookId);
        if (book == null)        throw new IllegalArgumentException("Sách không tồn tại.");
        if (!book.isAvailable()) throw new IllegalStateException(
            "Sách \"" + book.getTitle() + "\" hiện đã hết bản để mượn.");

        // 2. Kiểm tra độc giả tồn tại và được phép mượn
        Reader reader = readerDAO.findById(readerId);
        if (reader == null)        throw new IllegalArgumentException("Độc giả không tồn tại.");
        if (!reader.canBorrow())   throw new IllegalStateException(
            "Tài khoản độc giả \"" + reader.getFullName() + "\" đang bị khóa hoặc hết hạn.");

        // 3. Kiểm tra chưa mượn cuốn đó
        if (borrowDAO.isBookBorrowedByReader(bookId, readerId)) {
            throw new IllegalStateException(
                "Độc giả \"" + reader.getFullName() +
                "\" đang mượn cuốn \"" + book.getTitle() + "\" rồi.");
        }

        // 4. Tính ngày
        String today   = LocalDate.now().format(DATE_FMT);
        String dueDate = (dueDateStr != null && !dueDateStr.isBlank())
            ? dueDateStr.trim()
            : LocalDate.now().plusDays(DEFAULT_BORROW_DAYS).format(DATE_FMT);

        // Validate due_date phải sau today
        LocalDate due = LocalDate.parse(dueDate, DATE_FMT);
        if (!due.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Hạn trả phải sau ngày hôm nay.");
        }

        Borrow borrow = new Borrow(bookId, readerId, today, dueDate,
                                   notes == null ? "" : notes.trim());

        // 5. Transaction: tạo phiếu + giảm available_copies
        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            int id = borrowDAO.insert(borrow);
            if (id == -1) throw new SQLException("Tạo phiếu mượn thất bại.");
            borrow.setId(id);
            bookDAO.updateAvailableCopies(bookId, -1);   // giảm 1 bản
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }

        // Gán thông tin join để trả về UI
        borrow.setBookTitle(book.getTitle());
        borrow.setBookIsbn(book.getIsbn());
        borrow.setReaderName(reader.getFullName());
        borrow.setReaderCode(reader.getReaderCode());
        return borrow;
    }

    // ===================== Trả sách =====================

    /**
     * Ghi nhận trả sách và tính tiền phạt nếu quá hạn.
     *
     * @param borrowId ID phiếu mượn
     * @return Borrow đã cập nhật (kèm fineAmount)
     */
    public Borrow returnBook(int borrowId) throws SQLException {
        Borrow borrow = borrowDAO.findById(borrowId);
        if (borrow == null) throw new IllegalArgumentException("Phiếu mượn không tồn tại.");
        if (borrow.isReturned()) throw new IllegalStateException("Sách đã được trả rồi.");

        String today       = LocalDate.now().format(DATE_FMT);
        double fine        = calculateFine(borrow.getDueDate(), today);
        Borrow.Status status = fine > 0 ? Borrow.Status.RETURNED : Borrow.Status.RETURNED;

        Connection conn    = DatabaseConnection.getInstance().getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            borrowDAO.returnBook(borrowId, today, status, fine);
            bookDAO.updateAvailableCopies(borrow.getBookId(), +1); // tăng 1 bản
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }

        borrow.setReturnDate(today);
        borrow.setStatus(status);
        borrow.setFineAmount(fine);
        return borrow;
    }

    // ===================== Đồng bộ trạng thái quá hạn =====================

    /**
     * Cập nhật trạng thái OVERDUE cho các phiếu chưa trả mà đã qua hạn.
     * Gọi khi khởi động ứng dụng.
     */
    public int syncOverdueStatus() throws SQLException {
        String today = LocalDate.now().format(DATE_FMT);
        return borrowDAO.markOverdue(today);
    }

    // ===================== Truy vấn =====================

    public List<Borrow> getAllBorrows() throws SQLException {
        return borrowDAO.findAll();
    }

    public List<Borrow> getActiveBorrows() throws SQLException {
        return borrowDAO.findActive();
    }

    public List<Borrow> getOverdueBorrows() throws SQLException {
        return borrowDAO.findOverdue();
    }

    public List<Borrow> getBorrowsByReader(int readerId) throws SQLException {
        return borrowDAO.findByReader(readerId);
    }

    public List<Borrow> searchBorrows(String keyword) throws SQLException {
        return borrowDAO.search(keyword);
    }

    public int getActiveBorrowCount() throws SQLException {
        return borrowDAO.countActive();
    }

    public int getOverdueBorrowCount() throws SQLException {
        return borrowDAO.countOverdue();
    }

    public List<Object[]> getTopBorrowedBooks(int limit) throws SQLException {
        return borrowDAO.getTopBorrowedBooks(limit);
    }

    public int[] getBorrowCountByMonth(int year) throws SQLException {
        return borrowDAO.getBorrowCountByMonth(year);
    }

    // ===================== Tính tiền phạt =====================

    /**
     * Tính tiền phạt dựa trên số ngày quá hạn.
     * @param dueDateStr  hạn trả (dd/MM/yyyy)
     * @param returnDateStr ngày trả thực tế (dd/MM/yyyy)
     * @return tiền phạt (0 nếu trả đúng hạn hoặc trước hạn)
     */
    public double calculateFine(String dueDateStr, String returnDateStr) {
        try {
            LocalDate due    = LocalDate.parse(dueDateStr,    DATE_FMT);
            LocalDate ret    = LocalDate.parse(returnDateStr, DATE_FMT);
            long overdueDays = ChronoUnit.DAYS.between(due, ret);
            return overdueDays > 0 ? overdueDays * FINE_PER_DAY : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Tính tiền phạt dự tính đến ngày hôm nay (dùng để hiển thị cảnh báo).
     */
    public double calculateCurrentFine(String dueDateStr) {
        return calculateFine(dueDateStr, LocalDate.now().format(DATE_FMT));
    }
}
