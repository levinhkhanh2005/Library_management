package com.example;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class BorrowRenewTest {

    private BorrowService borrowService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        borrowService = new BorrowService();
        try (var conn = com.example.util.DatabaseConnection.getInstance().getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM borrows");
            stmt.execute("UPDATE books SET available_copies = total_copies");
        } catch (Exception ignored) {}
    }

    @Test
    public void testRenewBorrowSuccess() throws Exception {
        // Tạo phiếu mượn mới (sách 1, độc giả 1)
        Borrow borrow = borrowService.borrowBook(1, 1, null, "Test renew borrow");
        assertNotNull(borrow);
        assertEquals(0, borrow.getRenewCount());
        String initialDueDate = borrow.getDueDate();

        // Gia hạn lần 1 (+7 ngày)
        Borrow renewed1 = borrowService.renewBorrow(borrow.getId(), 7);
        assertEquals(1, renewed1.getRenewCount());
        LocalDate expectedDue1 = LocalDate.parse(initialDueDate, DATE_FMT).plusDays(7);
        assertEquals(expectedDue1.format(DATE_FMT), renewed1.getDueDate());
        assertEquals(Borrow.Status.BORROWING, renewed1.getStatus());

        // Gia hạn lần 2 (+14 ngày)
        Borrow renewed2 = borrowService.renewBorrow(borrow.getId(), 14);
        assertEquals(2, renewed2.getRenewCount());
        LocalDate expectedDue2 = expectedDue1.plusDays(14);
        assertEquals(expectedDue2.format(DATE_FMT), renewed2.getDueDate());

        // Gia hạn lần 3 -> Vượt quá giới hạn MAX_RENEW_COUNT (2) -> ném ngoại lệ
        try {
            borrowService.renewBorrow(borrow.getId(), 7);
            fail("Phải ném IllegalStateException khi vượt quá số lần gia hạn");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("giới hạn"));
        }

        // Trả sách
        borrowService.returnBook(borrow.getId());
    }

    @Test(expected = IllegalStateException.class)
    public void testRenewReturnedBookShouldFail() throws Exception {
        Borrow borrow = borrowService.borrowBook(2, 2, null, "Test renew returned");
        borrowService.returnBook(borrow.getId());

        // Cố gia hạn sách đã trả
        borrowService.renewBorrow(borrow.getId(), 7);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRenewWithInvalidDaysShouldFail() throws Exception {
        Borrow borrow = borrowService.borrowBook(3, 3, null, "Test invalid days");
        try {
            borrowService.renewBorrow(borrow.getId(), 0);
        } finally {
            borrowService.returnBook(borrow.getId());
        }
    }

    @Test
    public void testRenewOverdueBorrow() throws Exception {
        Borrow borrow = borrowService.borrowBook(4, 1, null, "Test overdue renew");
        // Giả lập phiếu quá hạn: đổi trạng thái sang OVERDUE
        new com.example.dao.BorrowDAO().updateStatus(borrow.getId(), Borrow.Status.OVERDUE);

        Borrow renewed = borrowService.renewBorrow(borrow.getId(), 7);
        assertEquals(Borrow.Status.BORROWING, renewed.getStatus());
        assertEquals(1, renewed.getRenewCount());
        String expectedDue = LocalDate.now().plusDays(7).format(DATE_FMT);
        assertEquals(expectedDue, renewed.getDueDate());

        borrowService.returnBook(borrow.getId());
    }

    @Test
    public void testDefaultRenewDays() throws Exception {
        Borrow borrow = borrowService.borrowBook(5, 2, null, "Test default renew days");
        String initialDueDate = borrow.getDueDate();

        // Gia hạn không truyền số ngày -> dùng DEFAULT_RENEW_DAYS (7 ngày)
        Borrow renewed = borrowService.renewBorrow(borrow.getId());
        assertEquals(1, renewed.getRenewCount());
        String expectedDue = LocalDate.parse(initialDueDate, DATE_FMT)
                .plusDays(BorrowService.DEFAULT_RENEW_DAYS).format(DATE_FMT);
        assertEquals(expectedDue, renewed.getDueDate());

        borrowService.returnBook(borrow.getId());
    }
}
