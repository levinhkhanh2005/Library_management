package com.example;

import com.example.model.Book;
import com.example.model.Category;
import com.example.service.BookService;
import com.example.service.CategoryService;
import com.example.util.DatabaseConnection;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.Assert.*;

public class CategoryServiceTest {

    private CategoryService categoryService;
    private BookService bookService;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        categoryService = new CategoryService();
        bookService = new BookService();
    }

    @Test
    public void testAddCategorySuccess() throws Exception {
        String catName = "Test Category Unique " + System.currentTimeMillis();
        Category c = categoryService.addCategory(catName, "Mô tả test");
        assertNotNull(c);
        assertTrue(c.getId() > 0);
        assertEquals(catName, c.getName());
        assertEquals("Mô tả test", c.getDescription());

        Category found = categoryService.getCategoryByName(catName);
        assertNotNull(found);
        assertEquals(c.getId(), found.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDuplicateCategoryCaseInsensitive() throws Exception {
        String baseName = "Test Duplicate " + System.currentTimeMillis();
        categoryService.addCategory(baseName, "Mô tả 1");

        // Cố thêm thể loại với tên viết thường/viết hoa khác nhau
        categoryService.addCategory(baseName.toLowerCase(), "Mô tả 2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddBlankCategoryName() throws Exception {
        categoryService.addCategory("   ", "Mô tả");
    }

    @Test
    public void testUpdateCategoryAndCascadeToBooks() throws Exception {
        String oldName = "Old Category " + System.currentTimeMillis();
        String newName = "New Category " + System.currentTimeMillis();

        Category c = categoryService.addCategory(oldName, "Mô tả cũ");

        // Thêm một cuốn sách thuộc thể loại oldName
        String isbn = "TEST-ISBN-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Test Cascade", "Tác Giả Test", oldName, "NXB Test", 2024, 5, "Mô tả");
        assertNotNull(book);
        assertEquals(oldName, book.getCategory());

        // Đổi tên thể loại
        c.setName(newName);
        c.setDescription("Mô tả mới");
        categoryService.updateCategory(c, oldName);

        // Kiểm tra thể loại đã cập nhật
        Category updatedCat = categoryService.getCategoryById(c.getId());
        assertEquals(newName, updatedCat.getName());
        assertEquals("Mô tả mới", updatedCat.getDescription());

        // Kiểm tra sách cũng được tự động đồng bộ tên thể loại mới
        Book updatedBook = bookService.getBookById(book.getId());
        assertEquals(newName, updatedBook.getCategory());

        // Dọn dẹp
        bookService.deleteBook(updatedBook);
        categoryService.deleteCategory(c.getId());
    }

    @Test
    public void testDeleteCategoryWithBooksBlocked() throws Exception {
        String catName = "Blocked Delete " + System.currentTimeMillis();
        Category c = categoryService.addCategory(catName, "Mô tả");

        String isbn = "TEST-BLOCKED-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Thuộc Thể Loại", "Tác Giả", catName, "NXB", 2024, 2, "");

        try {
            categoryService.deleteCategory(c.getId());
            fail("Phải ném IllegalStateException khi xóa thể loại đang có sách sử dụng");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("cuốn sách đang thuộc thể loại này"));
        } finally {
            bookService.deleteBook(book);
            categoryService.deleteCategory(c.getId());
        }
    }

    @Test
    public void testDeleteCategoryWithoutBooksSuccess() throws Exception {
        String catName = "Empty Category " + System.currentTimeMillis();
        Category c = categoryService.addCategory(catName, "Mô tả");

        categoryService.deleteCategory(c.getId());
        Category found = categoryService.getCategoryByName(catName);
        assertNull(found);
    }

    @Test
    public void testSearchCategories() throws Exception {
        String uniquePrefix = "KWSearch" + System.currentTimeMillis();
        Category c1 = categoryService.addCategory(uniquePrefix + " Science", "Mô tả 1");
        Category c2 = categoryService.addCategory("Other " + uniquePrefix, "Mô tả 2");

        List<Category> results = categoryService.searchCategories(uniquePrefix);
        assertTrue(results.size() >= 2);

        // Dọn dẹp
        categoryService.deleteCategory(c1.getId());
        categoryService.deleteCategory(c2.getId());
    }
}
