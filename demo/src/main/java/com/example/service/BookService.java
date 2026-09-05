package com.example.service;

import com.example.dao.BookDAO;
import com.example.model.Book;

import java.sql.SQLException;
import java.util.List;

/**
 * Service quản lý nghiệp vụ sách.
 * Chứa validation và logic trước khi gọi DAO.
 */
public class BookService {

    private final BookDAO bookDAO = new BookDAO();

    // ===================== Thêm sách =====================

    /**
     * Thêm sách mới sau khi validate.
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     * @throws SQLException nếu lỗi DB
     */
    public Book addBook(String isbn, String title, String author, String category,
                        String publisher, int publishYear, int totalCopies,
                        String description) throws SQLException {

        // Validate bắt buộc
        validateRequired(title,  "Tên sách");
        validateRequired(author, "Tác giả");
        if (totalCopies <= 0) {
            throw new IllegalArgumentException("Số lượng bản sao phải lớn hơn 0.");
        }
        if (publishYear < 0 || publishYear > java.time.Year.now().getValue() + 1) {
            throw new IllegalArgumentException("Năm xuất bản không hợp lệ.");
        }

        // Kiểm tra ISBN trùng
        if (isbn != null && !isbn.isBlank()) {
            Book existing = bookDAO.findByIsbn(isbn.trim());
            if (existing != null) {
                throw new IllegalArgumentException("ISBN \"" + isbn + "\" đã tồn tại trong hệ thống.");
            }
        }

        Book book = new Book(
            isbn == null ? "" : isbn.trim(),
            title.trim(), author.trim(),
            category == null ? "" : category.trim(),
            publisher == null ? "" : publisher.trim(),
            publishYear, totalCopies,
            description == null ? "" : description.trim()
        );

        int id = bookDAO.insert(book);
        if (id == -1) throw new SQLException("Thêm sách thất bại.");
        book.setId(id);
        return book;
    }

    // ===================== Cập nhật sách =====================

    public void updateBook(Book book) throws SQLException {
        validateRequired(book.getTitle(),  "Tên sách");
        validateRequired(book.getAuthor(), "Tác giả");
        if (book.getTotalCopies() <= 0) {
            throw new IllegalArgumentException("Số lượng bản sao phải lớn hơn 0.");
        }
        // Số bản còn lại không được âm và không được vượt tổng số
        if (book.getAvailableCopies() < 0) {
            throw new IllegalArgumentException("Số bản còn lại không được âm.");
        }
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw new IllegalArgumentException("Số bản còn lại không được vượt tổng số bản.");
        }

        // Kiểm tra ISBN trùng (nếu đã thay đổi)
        if (book.getIsbn() != null && !book.getIsbn().isBlank()) {
            Book existing = bookDAO.findByIsbn(book.getIsbn());
            if (existing != null && existing.getId() != book.getId()) {
                throw new IllegalArgumentException("ISBN \"" + book.getIsbn() + "\" đã được dùng cho sách khác.");
            }
        }

        if (!bookDAO.update(book)) {
            throw new SQLException("Cập nhật sách thất bại. Sách có thể không tồn tại.");
        }
    }

    // ===================== Xóa sách =====================

    /**
     * Xóa sách. Không cho xóa nếu còn bản đang được mượn.
     */
    public void deleteBook(Book book) throws SQLException {
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new IllegalStateException(
                "Không thể xóa sách \"" + book.getTitle() + "\".\n" +
                "Vẫn còn " + book.getBorrowedCopies() + " bản đang được mượn."
            );
        }
        if (!bookDAO.delete(book.getId())) {
            throw new SQLException("Xóa sách thất bại. Sách có thể không tồn tại.");
        }
    }

    // ===================== Truy vấn =====================

    public List<Book> getAllBooks() throws SQLException {
        return bookDAO.findAll();
    }

    public Book getBookById(int id) throws SQLException {
        return bookDAO.findById(id);
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        return bookDAO.search(keyword);
    }

    public List<Book> advancedSearchBooks(String keyword, String category, Integer publishYear, Boolean isAvailable) throws SQLException {
        return bookDAO.advancedSearch(keyword, category, publishYear, isAvailable);
    }

    public List<String> getAllCategories() throws SQLException {
        return bookDAO.findAllCategories();
    }

    public int getTotalBooks() throws SQLException {
        return bookDAO.countAll();
    }

    public int getTotalBorrowed() throws SQLException {
        return bookDAO.countBorrowed();
    }

    // ===================== Helper =====================

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }
}
