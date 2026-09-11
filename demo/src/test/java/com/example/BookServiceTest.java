package com.example;

import com.example.model.Book;
import com.example.model.CategoryBookStat;
import com.example.service.BookService;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests cho nghiệp vụ quản lý sách và phân bố thể loại sách (Pie Chart stats).
 */
public class BookServiceTest {

    private BookService bookService;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        bookService = new BookService();
    }

    @Test
    public void testImportCopiesSuccess() throws Exception {
        String isbn = "TEST-IMP-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Test Nhập Kho", "Tác giả Test", "Văn học", "NXB Test", 2023, 5, "Mô tả");
        assertNotNull(book);
        int bookId = book.getId();
        assertEquals(5, book.getTotalCopies());
        assertEquals(5, book.getAvailableCopies());

        // Nhập thêm 3 bản
        bookService.importCopies(bookId, 3, "Nhập bổ sung từ nhà sách");

        Book reloaded = bookService.getBookById(bookId);
        assertNotNull(reloaded);
        assertEquals(8, reloaded.getTotalCopies());
        assertEquals(8, reloaded.getAvailableCopies());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImportCopiesInvalidCount() throws Exception {
        bookService.importCopies(1, 0, "Test 0 bản");
    }

    @Test
    public void testDiscardCopiesSuccess() throws Exception {
        String isbn = "TEST-DIS-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Test Thanh Lý", "Tác giả Test", "Khoa học", "NXB Test", 2022, 10, "Mô tả");
        assertNotNull(book);
        int bookId = book.getId();

        // Thanh lý 4 bản hư hỏng
        bookService.discardCopies(bookId, 4, "Sách bị rách nát");

        Book reloaded = bookService.getBookById(bookId);
        assertNotNull(reloaded);
        assertEquals(6, reloaded.getTotalCopies());
        assertEquals(6, reloaded.getAvailableCopies());
    }

    @Test(expected = IllegalStateException.class)
    public void testDiscardCopiesExceedingAvailable() throws Exception {
        String isbn = "TEST-EXC-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Test Vượt Kho", "Tác giả Test", "Công nghệ", "NXB Test", 2021, 2, "Mô tả");
        assertNotNull(book);

        // Thử thanh lý 5 bản trong khi kho chỉ có 2 bản -> Phải chặn
        bookService.discardCopies(book.getId(), 5, "Cố tình thanh lý vượt quá");
    }

    @Test
    public void testCategoryDistributionCalculation() throws Exception {
        List<CategoryBookStat> statsByTitles = bookService.getCategoryDistribution(true);
        assertNotNull(statsByTitles);
        assertFalse(statsByTitles.isEmpty());

        double totalPct = 0.0;
        int totalTitles = 0;
        for (CategoryBookStat s : statsByTitles) {
            assertTrue(s.getPercentage() >= 0.0);
            assertTrue(s.getPercentage() <= 100.0);
            assertTrue(s.getTitleCount() > 0);
            totalPct += s.getPercentage();
            totalTitles += s.getTitleCount();
        }
        assertTrue(totalTitles > 0);
        // Tổng phần trăm phải xấp xỉ 100% (cho phép sai số làm tròn 1% giữa 98.0% và 102.0%)
        assertTrue("Tổng phần trăm phải gần 100%: " + totalPct, totalPct >= 98.0 && totalPct <= 102.0);

        // Kiểm tra thống kê theo tổng bản sao
        List<CategoryBookStat> statsByCopies = bookService.getCategoryDistribution(false);
        assertNotNull(statsByCopies);
        assertFalse(statsByCopies.isEmpty());
        double totalCopiesPct = 0.0;
        for (CategoryBookStat s : statsByCopies) {
            totalCopiesPct += s.getPercentage();
        }
        assertTrue("Tổng phần trăm theo bản sao phải gần 100%: " + totalCopiesPct, totalCopiesPct >= 98.0 && totalCopiesPct <= 102.0);
    }
}
