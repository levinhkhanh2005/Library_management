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

    /** Số sách tối đa một độc giả được mượn cùng lúc. */
    public static final int MAX_ACTIVE_BORROWS_PER_READER = 3;

    /** Số lần gia hạn tối đa cho mỗi phiếu mượn. */
    public static final int MAX_RENEW_COUNT = 2;

    /** Số ngày gia hạn thêm mỗi lần. */
    public static final int RENEW_DAYS = 7;

    /** Tiền bồi thường mặc định khi mất sách (VND). */
    public static final double DEFAULT_LOST_COMPENSATION = 100_000.0;

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

        // 3. Kiểm tra hạn mức mượn tối đa
        int activeCount = borrowDAO.countActiveByReader(readerId);
        if (activeCount >= MAX_ACTIVE_BORROWS_PER_READER) {
            throw new IllegalStateException(
                "Độc giả \"" + reader.getFullName() +
                "\" đã đạt hạn mức mượn tối đa (" + MAX_ACTIVE_BORROWS_PER_READER + " cuốn).");
        }

        // 4. Kiểm tra chưa mượn cuốn đó
        if (borrowDAO.isBookBorrowedByReader(bookId, readerId)) {
            throw new IllegalStateException(
                "Độc giả \"" + reader.getFullName() +
                "\" đang mượn cuốn \"" + book.getTitle() + "\" rồi.");
        }

        // 5. Tính ngày
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

        // 6. Transaction: tạo phiếu + giảm available_copies
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
        if (borrow.isLost()) throw new IllegalStateException("Sách đã được báo mất.");

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

    // ===================== Gia hạn mượn sách =====================

    /**
     * Gia hạn phiếu mượn: cộng thêm RENEW_DAYS ngày tính từ hạn trả cũ.
     * Điều kiện: phiếu đang BORROWING, chưa vượt MAX_RENEW_COUNT.
     *
     * @param borrowId ID phiếu mượn
     * @return Borrow đã cập nhật
     */
    public Borrow renewBook(int borrowId) throws SQLException {
        Borrow borrow = borrowDAO.findById(borrowId);
        if (borrow == null) throw new IllegalArgumentException("Phiếu mượn không tồn tại.");
        if (borrow.getStatus() != Borrow.Status.BORROWING) {
            throw new IllegalStateException(
                "Chỉ có thể gia hạn phiếu đang mượn (đúng hạn). Phiếu hiện tại: " + borrow.getStatus().getLabel());
        }

        // Kiểm tra số lần gia hạn đã dùng
        int currentRenewCount = parseRenewCount(borrow.getNotes());
        if (currentRenewCount >= MAX_RENEW_COUNT) {
            throw new IllegalStateException(
                "Phiếu đã gia hạn tối đa " + MAX_RENEW_COUNT + " lần.");
        }

        // Tính hạn trả mới: cộng thêm RENEW_DAYS tính từ hạn trả cũ
        LocalDate oldDue = LocalDate.parse(borrow.getDueDate(), DATE_FMT);
        String newDueDate = oldDue.plusDays(RENEW_DAYS).format(DATE_FMT);

        // Cập nhật ghi chú với số lần gia hạn
        int newRenewCount = currentRenewCount + 1;
        String existingNotes = borrow.getNotes() != null ? borrow.getNotes() : "";
        // Loại bỏ ghi chú gia hạn cũ trước khi thêm mới
        existingNotes = existingNotes.replaceAll("\\[Gia hạn \\d+/" + MAX_RENEW_COUNT + "\\]", "").trim();
        String renewNote = "[Gia hạn " + newRenewCount + "/" + MAX_RENEW_COUNT + "]";
        String newNotes = (existingNotes.isEmpty() ? "" : existingNotes + " ") + renewNote;

        borrowDAO.renewDueDate(borrowId, newDueDate, newNotes);

        borrow.setDueDate(newDueDate);
        borrow.setNotes(newNotes);
        borrow.setRenewCount(newRenewCount);
        return borrow;
    }

    /**
     * Phân tích số lần gia hạn từ ghi chú (dạng "[Gia hạn X/Y]").
     */
    private int parseRenewCount(String notes) {
        if (notes == null || notes.isEmpty()) return 0;
        var matcher = java.util.regex.Pattern
            .compile("\\[Gia hạn (\\d+)/" + MAX_RENEW_COUNT + "\\]")
            .matcher(notes);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    // ===================== Báo mất sách =====================

    /**
     * Báo mất sách: chuyển phiếu sang trạng thái LOST,
     * ghi nhận tiền bồi thường và giảm total_copies trong kho.
     *
     * @param borrowId         ID phiếu mượn
     * @param compensationFee  Tiền bồi thường (VND)
     * @param reason           Lý do mất sách
     * @return Borrow đã cập nhật
     */
    public Borrow reportLostBook(int borrowId, double compensationFee,
                                  String reason) throws SQLException {
        Borrow borrow = borrowDAO.findById(borrowId);
        if (borrow == null) throw new IllegalArgumentException("Phiếu mượn không tồn tại.");
        if (!borrow.isActive()) throw new IllegalStateException(
            "Chỉ có thể báo mất phiếu đang mượn hoặc quá hạn.");
        if (compensationFee < 0) throw new IllegalArgumentException(
            "Tiền bồi thường không được âm.");

        String lostNote = "[Mất sách] " + (reason != null ? reason.trim() : "");
        String existingNotes = borrow.getNotes() != null ? borrow.getNotes() : "";
        String newNotes = existingNotes.isEmpty() ? lostNote : existingNotes + " | " + lostNote;

        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            borrowDAO.reportLost(borrowId, compensationFee, newNotes);
            bookDAO.decreaseTotalCopies(borrow.getBookId()); // giảm tổng bản trong kho
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }

        borrow.setStatus(Borrow.Status.LOST);
        borrow.setFineAmount(compensationFee);
        borrow.setNotes(newNotes);
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

    public List<Borrow> getLostBorrows() throws SQLException {
        return borrowDAO.findLost();
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

    /** Đếm số phiếu đang hoạt động của một độc giả (dùng kiểm tra hạn mức mượn). */
    public int getActiveCountByReader(int readerId) throws SQLException {
        return borrowDAO.countActiveByReader(readerId);
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
