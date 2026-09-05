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

    /** Số lần gia hạn tối đa cho mỗi phiếu mượn. */
    public static final int MAX_RENEW_COUNT = 2;

    /** Số ngày gia hạn mặc định. */
    public static final int DEFAULT_RENEW_DAYS = 7;

    /** Số lượng sách mượn tối đa cùng lúc. */
    public static final int MAX_CONCURRENT_BORROWS = 5;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowDAO  borrowDAO  = new BorrowDAO();
    private final BookDAO    bookDAO    = new BookDAO();
    private final ReaderDAO  readerDAO  = new ReaderDAO();

    // ===================== Mượn sách =====================

    /**
     * Tạo phiếu mượn sách.
     * Kiểm tra: sách còn bản, độc giả hoạt động, chưa vượt giới hạn mượn, chưa mượn cuốn đó.
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

        // 3. Kiểm tra số sách đang mượn chưa vượt quá giới hạn
        long activeBorrowsCount = borrowDAO.findByReader(readerId).stream()
                .filter(Borrow::isActive)
                .count();
        if (activeBorrowsCount >= MAX_CONCURRENT_BORROWS) {
            throw new IllegalStateException(
                "Độc giả \"" + reader.getFullName() + "\" đã đạt giới hạn mượn " +
                MAX_CONCURRENT_BORROWS + " cuốn sách cùng lúc.");
        }

        // 4. Kiểm tra chưa mượn cuốn đó
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

    // ===================== Gia hạn mượn sách =====================

    /**
     * Gia hạn thời gian mượn sách thêm số ngày chỉ định.
     * Kiểm tra: phiếu mượn tồn tại, chưa trả sách, chưa vượt quá số lần gia hạn tối đa.
     *
     * @param borrowId  ID phiếu mượn cần gia hạn
     * @param extraDays Số ngày gia hạn thêm (phải > 0)
     * @return Borrow đã được cập nhật hạn trả và số lần gia hạn
     */
    public Borrow renewBorrow(int borrowId, int extraDays) throws SQLException {
        if (extraDays <= 0) {
            throw new IllegalArgumentException("Số ngày gia hạn phải lớn hơn 0.");
        }

        Borrow borrow = borrowDAO.findById(borrowId);
        if (borrow == null) {
            throw new IllegalArgumentException("Phiếu mượn không tồn tại.");
        }
        if (borrow.isReturned()) {
            throw new IllegalStateException("Không thể gia hạn phiếu đã trả sách.");
        }
        if (borrow.getRenewCount() >= MAX_RENEW_COUNT) {
            throw new IllegalStateException(
                "Phiếu mượn đã đạt giới hạn số lần gia hạn tối đa (" + MAX_RENEW_COUNT + " lần).");
        }

        // Tính hạn trả mới
        LocalDate currentDue;
        try {
            currentDue = LocalDate.parse(borrow.getDueDate(), DATE_FMT);
        } catch (Exception e) {
            currentDue = LocalDate.now();
        }

        // Nếu phiếu đã quá hạn, hạn mới tính từ ngày hôm nay; nếu còn hạn thì cộng dồn từ hạn cũ
        LocalDate baseDate = (borrow.getStatus() == Borrow.Status.OVERDUE || currentDue.isBefore(LocalDate.now()))
                ? LocalDate.now()
                : currentDue;
        LocalDate newDue = baseDate.plusDays(extraDays);
        String newDueDateStr = newDue.format(DATE_FMT);

        int newRenewCount = borrow.getRenewCount() + 1;
        Borrow.Status newStatus = Borrow.Status.BORROWING; // Chuyển về đang mượn sau khi gia hạn

        boolean ok = borrowDAO.renewBorrow(borrowId, newDueDateStr, newRenewCount, newStatus);
        if (!ok) {
            throw new SQLException("Gia hạn phiếu mượn thất bại trong CSDL.");
        }

        borrow.setDueDate(newDueDateStr);
        borrow.setRenewCount(newRenewCount);
        borrow.setStatus(newStatus);
        borrow.setFineAmount(0.0);
        return borrow;
    }

    /**
     * Gia hạn thời gian mượn sách với số ngày mặc định (DEFAULT_RENEW_DAYS).
     */
    public Borrow renewBorrow(int borrowId) throws SQLException {
        return renewBorrow(borrowId, DEFAULT_RENEW_DAYS);
    }

    // ===================== Xóa phiếu mượn =====================
    /**
     * Xóa phiếu mượn khỏi cơ sở dữ liệu.
     * Nếu phiếu mượn chưa được trả, tự động cập nhật lại số lượng sách sẵn có (+1).
     *
     * @param borrowId ID phiếu mượn cần xóa
     */
    public void deleteBorrow(int borrowId) throws SQLException {
        Borrow borrow = borrowDAO.findById(borrowId);
        if (borrow == null) {
            throw new IllegalArgumentException("Phiếu mượn không tồn tại.");
        }
        Connection conn = DatabaseConnection.getInstance().getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            if (!borrowDAO.delete(borrowId)) {
                throw new SQLException("Xóa phiếu mượn thất bại.");
            }
            if (!borrow.isReturned()) {
                bookDAO.updateAvailableCopies(borrow.getBookId(), +1);
            }
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
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

    public Borrow getBorrowById(int id) throws SQLException {
        return borrowDAO.findById(id);
    }

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

    public List<Borrow> advancedSearchBorrows(String keyword, String fromDate, String toDate, Borrow.Status status) throws SQLException {
        return borrowDAO.advancedSearch(keyword, fromDate, toDate, status);
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
