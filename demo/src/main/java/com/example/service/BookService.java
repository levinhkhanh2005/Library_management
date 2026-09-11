package com.example.service;

import com.example.dao.BookDAO;
import com.example.dao.CategoryDAO;
import com.example.model.Book;
import com.example.model.Category;
import com.example.model.CategoryBookStat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service quản lý nghiệp vụ sách.
 * Chứa validation và logic trước khi gọi DAO.
 */
public class BookService {

    private final BookDAO bookDAO = new BookDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

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

        // Kiểm tra số sách đang được mượn: không được giảm totalCopies xuống dưới số đang mượn
        Book current = bookDAO.findById(book.getId());
        if (current != null) {
            int currentlyBorrowed = current.getBorrowedCopies();
            if (book.getTotalCopies() < currentlyBorrowed) {
                throw new IllegalArgumentException(
                    "Không thể giảm tổng số bản xuống " + book.getTotalCopies() + ".\n" +
                    "Hiện tại đang có " + currentlyBorrowed + " bản đang được bạn đọc mượn!"
                );
            }
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

    // ===================== Nghiệp vụ kho sách (Nhập thêm / Thanh lý) =====================

    /**
     * Nghiệp vụ nhập thêm số lượng bản sao sách vào kho thư viện.
     * @param bookId ID sách
     * @param additionalCopies số bản nhập thêm (phải > 0)
     * @param notes ghi chú / nguồn nhập (tùy chọn)
     */
    public void importCopies(int bookId, int additionalCopies, String notes) throws SQLException {
        if (additionalCopies <= 0) {
            throw new IllegalArgumentException("Số lượng bản sao nhập thêm phải lớn hơn 0.");
        }
        Book book = bookDAO.findById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Sách không tồn tại.");
        }
        if (!bookDAO.addCopies(bookId, additionalCopies)) {
            throw new SQLException("Nhập thêm bản sao thất bại.");
        }
    }

    /**
     * Nghiệp vụ thanh lý / xuất hủy bản sao sách hư hỏng, rách nát, mất mát trong kho.
     * @param bookId ID sách
     * @param discardCopies số lượng bản thanh lý
     * @param reason lý do thanh lý (bắt buộc)
     */
    public void discardCopies(int bookId, int discardCopies, String reason) throws SQLException {
        if (discardCopies <= 0) {
            throw new IllegalArgumentException("Số lượng bản sao thanh lý phải lớn hơn 0.");
        }
        validateRequired(reason, "Lý do thanh lý");
        Book book = bookDAO.findById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Sách không tồn tại.");
        }
        if (discardCopies > book.getAvailableCopies()) {
            throw new IllegalStateException(
                "Không thể thanh lý " + discardCopies + " bản sao.\n" +
                "Hiện kho chỉ còn " + book.getAvailableCopies() + " bản có sẵn.\n" +
                "(Vẫn còn " + book.getBorrowedCopies() + " bản đang được bạn đọc mượn ngoài thư viện)."
            );
        }
        if (!bookDAO.discardCopies(bookId, discardCopies)) {
            throw new SQLException("Thanh lý bản sao thất bại.");
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
        return bookDAO.advancedSearch(keyword, category, null, publishYear, isAvailable);
    }

    public List<Book> advancedSearchBooks(String keyword, String category, String author, Integer publishYear, Boolean isAvailable) throws SQLException {
        return bookDAO.advancedSearch(keyword, category, author, publishYear, isAvailable);
    }

    public List<String> getAllCategories() throws SQLException {
        List<Category> cats = categoryDAO.findAll();
        List<String> list = new ArrayList<>();
        for (Category c : cats) {
            list.add(c.getName());
        }
        return list;
    }

    public int getTotalBooks() throws SQLException {
        return bookDAO.countAll();
    }

    public int getTotalBorrowed() throws SQLException {
        return bookDAO.countBorrowed();
    }

    /**
     * Lấy thống kê phân bố thể loại sách kèm tỷ lệ % chính xác trong tổng số sách.
     * @param byTitleCount true nếu tính % theo số đầu sách (titles), false nếu tính theo tổng số bản sao (copies)
     * @return danh sách CategoryBookStat đã được tính percentage
     */
    public List<CategoryBookStat> getCategoryDistribution(boolean byTitleCount) throws SQLException {
        List<CategoryBookStat> stats = bookDAO.getCategoryStats();
        if (stats.isEmpty()) return stats;

        double total = 0.0;
        for (CategoryBookStat stat : stats) {
            total += byTitleCount ? stat.getTitleCount() : stat.getTotalCopies();
        }

        if (total > 0) {
            for (CategoryBookStat stat : stats) {
                double val = byTitleCount ? stat.getTitleCount() : stat.getTotalCopies();
                double pct = (val * 100.0) / total;
                // Làm tròn 1 chữ số thập phân
                stat.setPercentage(Math.round(pct * 10.0) / 10.0);
            }
        }
        return stats;
    }

    /**
     * Lấy danh sách bạn đọc đang mượn các bản sao của cuốn sách này.
     */
    public List<Object[]> getActiveBorrowers(int bookId) throws SQLException {
        return bookDAO.getActiveBorrowersForBook(bookId);
    }

    // ===================== Helper =====================

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }
}
