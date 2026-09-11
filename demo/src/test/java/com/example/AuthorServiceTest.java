package com.example;

import com.example.model.Author;
import com.example.model.Book;
import com.example.service.AuthorService;
import com.example.service.BookService;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AuthorServiceTest {

    private AuthorService authorService;
    private BookService   bookService;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        authorService = new AuthorService();
        bookService = new BookService();
    }

    @Test
    public void testAddAuthorSuccess() throws Exception {
        String authorName = "Test Author " + System.currentTimeMillis();
        Author a = authorService.addAuthor(authorName, 1950, 2020, "Việt Nam", "Tiểu sử tác giả test");
        assertNotNull(a);
        assertTrue(a.getId() > 0);
        assertEquals(authorName, a.getName());
        assertEquals("Việt Nam", a.getNationality());
        assertEquals(Integer.valueOf(1950), a.getBirthYear());
        assertEquals(Integer.valueOf(2020), a.getDeathYear());

        Author found = authorService.getAuthorByName(authorName);
        assertNotNull(found);
        assertEquals(a.getId(), found.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDuplicateAuthorCaseInsensitive() throws Exception {
        String baseName = "Duplicate Author " + System.currentTimeMillis();
        authorService.addAuthor(baseName, 1960, null, "Anh", "Mô tả 1");

        // Cố thêm tác giả trùng tên viết thường
        authorService.addAuthor(baseName.toLowerCase(), 1960, null, "Anh", "Mô tả 2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddBlankAuthorName() throws Exception {
        authorService.addAuthor("   ", null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLifespanDeathBeforeBirth() throws Exception {
        authorService.addAuthor("Invalid Lifespan Author " + System.currentTimeMillis(), 2000, 1990, null, null);
    }

    @Test
    public void testUpdateAuthorAndCascadeToBooks() throws Exception {
        String oldName = "Old Author " + System.currentTimeMillis();
        String newName = "New Author " + System.currentTimeMillis();

        Author a = authorService.addAuthor(oldName, 1970, null, "Mỹ", "Tiểu sử cũ");

        // Thêm một cuốn sách mang tên tác giả oldName
        String isbn = "TEST-AUTHOR-ISBN-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Của Tác Giả Cũ", "Tác Giả Khác", "Công nghệ", "NXB Test", 2023, 4, "Mô tả");
        book.setAuthor(oldName);
        bookService.updateBook(book);

        assertEquals(oldName, bookService.getBookById(book.getId()).getAuthor());

        // Đổi tên tác giả
        a.setName(newName);
        a.setNationality("Pháp");
        authorService.updateAuthor(a, oldName);

        // Kiểm tra tác giả đã cập nhật tên mới
        Author updatedAuthor = authorService.getAuthorById(a.getId());
        assertEquals(newName, updatedAuthor.getName());
        assertEquals("Pháp", updatedAuthor.getNationality());

        // Kiểm tra cuốn sách đã được tự động cascade cập nhật sang tên tác giả mới
        Book updatedBook = bookService.getBookById(book.getId());
        assertEquals(newName, updatedBook.getAuthor());

        // Dọn dẹp
        bookService.deleteBook(updatedBook);
        authorService.deleteAuthor(a.getId());
    }

    @Test
    public void testDeleteAuthorWithBooksBlocked() throws Exception {
        String authorName = "Blocked Author " + System.currentTimeMillis();
        Author a = authorService.addAuthor(authorName, 1985, null, "Nhật Bản", "Mô tả");

        String isbn = "TEST-BLOCKED-AUTHOR-" + System.currentTimeMillis();
        Book book = bookService.addBook(isbn, "Sách Thuộc Tác Giả Bị Chặn Xóa", authorName, "Văn học", "NXB", 2024, 3, "");

        try {
            authorService.deleteAuthor(a.getId());
            fail("Phải ném IllegalStateException khi xóa tác giả đang có sách trong thư viện");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("cuốn sách đang thuộc tác giả này"));
        } finally {
            bookService.deleteBook(book);
            authorService.deleteAuthor(a.getId());
        }
    }

    @Test
    public void testDeleteAuthorWithoutBooksSuccess() throws Exception {
        String authorName = "Empty Author " + System.currentTimeMillis();
        Author a = authorService.addAuthor(authorName, null, null, null, null);

        authorService.deleteAuthor(a.getId());
        Author found = authorService.getAuthorByName(authorName);
        assertNull(found);
    }

    @Test
    public void testSearchAuthorsAndStats() throws Exception {
        String unique = "UniSearch" + System.currentTimeMillis();
        Author a1 = authorService.addAuthor(unique + " Nam Cao", 1915, 1951, "Việt Nam", "Nhà văn hiện thực");
        Author a2 = authorService.addAuthor("Other " + unique, 1980, null, "Hàn Quốc", "Tác giả hiện đại");

        List<Author> results = authorService.searchAuthors(unique);
        assertTrue(results.size() >= 2);

        // Dọn dẹp
        authorService.deleteAuthor(a1.getId());
        authorService.deleteAuthor(a2.getId());
    }
}
