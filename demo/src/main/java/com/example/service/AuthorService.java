package com.example.service;

import com.example.dao.AuthorDAO;
import com.example.model.Author;
import com.example.model.Book;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;

/**
 * Service quản lý nghiệp vụ tác giả sách.
 * Kiểm tra validation, chống trùng lặp dữ liệu không phân biệt hoa thường và xử lý ràng buộc liên quan đến sách.
 */
public class AuthorService {

    private final AuthorDAO authorDAO = new AuthorDAO();

    // ===================== Thêm tác giả =====================

    /**
     * Thêm tác giả mới sau khi validate.
     * @throws IllegalArgumentException nếu tên rỗng hoặc đã tồn tại
     * @throws SQLException nếu lỗi CSDL
     */
    public Author addAuthor(String name, Integer birthYear, Integer deathYear,
                            String nationality, String biography) throws SQLException {
        validateName(name);
        validateLifespan(birthYear, deathYear);

        Author existing = authorDAO.findByName(name.trim());
        if (existing != null) {
            throw new IllegalArgumentException("Tác giả \"" + name.trim() + "\" đã tồn tại trong hệ thống.");
        }

        Author author = new Author(
            name.trim(),
            birthYear,
            deathYear,
            nationality == null ? "" : nationality.trim(),
            biography == null ? "" : biography.trim()
        );

        int id = authorDAO.insert(author);
        if (id == -1) throw new SQLException("Thêm tác giả thất bại.");
        author.setId(id);
        return author;
    }

    // ===================== Cập nhật tác giả =====================

    /**
     * Cập nhật thông tin tác giả. Đồng thời tự động cập nhật tên tác giả cho các sách liên quan nếu tên thay đổi.
     * @throws IllegalArgumentException nếu tên rỗng hoặc trùng với tác giả khác
     * @throws SQLException nếu lỗi CSDL
     */
    public void updateAuthor(Author author, String oldName) throws SQLException {
        validateName(author.getName());
        validateLifespan(author.getBirthYear(), author.getDeathYear());

        // Kiểm tra xem tên mới có trùng với tác giả nào khác không
        Author existing = authorDAO.findByName(author.getName().trim());
        if (existing != null && existing.getId() != author.getId()) {
            throw new IllegalArgumentException("Tác giả \"" + author.getName().trim() + "\" đã tồn tại trong hệ thống.");
        }

        author.setName(author.getName().trim());
        author.setNationality(author.getNationality() == null ? "" : author.getNationality().trim());
        author.setBiography(author.getBiography() == null ? "" : author.getBiography().trim());

        if (!authorDAO.update(author)) {
            throw new SQLException("Cập nhật tác giả thất bại. Tác giả có thể không tồn tại.");
        }

        // Tự động đồng bộ tên mới sang bảng books nếu tên thay đổi
        if (oldName != null && !oldName.trim().equalsIgnoreCase(author.getName())) {
            authorDAO.updateAuthorInBooks(oldName.trim(), author.getName());
        }
    }

    // ===================== Xóa tác giả =====================

    /**
     * Xóa tác giả. Chặn xóa nếu còn sách thuộc tác giả này.
     * @throws IllegalStateException nếu còn sách đang gán tác giả
     * @throws SQLException nếu lỗi CSDL
     */
    public void deleteAuthor(int id) throws SQLException {
        Author author = authorDAO.findById(id);
        if (author == null) {
            throw new SQLException("Tác giả không tồn tại.");
        }

        int bookCount = authorDAO.countBooksUsingAuthor(author.getName());
        if (bookCount > 0) {
            throw new IllegalStateException(
                "Không thể xóa tác giả \"" + author.getName() + "\".\n" +
                "Hiện có " + bookCount + " cuốn sách đang thuộc tác giả này.\n" +
                "Vui lòng đổi tác giả của các cuốn sách trước khi xóa."
            );
        }

        if (!authorDAO.delete(id)) {
            throw new SQLException("Xóa tác giả thất bại.");
        }
    }

    // ===================== Truy vấn =====================

    public List<Author> getAllAuthors() throws SQLException {
        return authorDAO.findAll();
    }

    public List<Author> getAllAuthorsWithStats() throws SQLException {
        return authorDAO.findAllWithStats();
    }

    public List<Author> searchAuthors(String keyword) throws SQLException {
        return authorDAO.search(keyword);
    }

    public Author getAuthorById(int id) throws SQLException {
        return authorDAO.findById(id);
    }

    public Author getAuthorByName(String name) throws SQLException {
        return authorDAO.findByName(name);
    }

    public List<Book> getBooksByAuthor(String authorName) throws SQLException {
        return authorDAO.findBooksByAuthor(authorName);
    }

    public int countBooksUsingAuthor(String authorName) throws SQLException {
        return authorDAO.countBooksUsingAuthor(authorName);
    }

    public int getTotalAuthors() throws SQLException {
        return authorDAO.countAll();
    }

    // ===================== Validation =====================

    private void validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Tên tác giả không được để trống.");
        }
    }

    private void validateLifespan(Integer birthYear, Integer deathYear) {
        int currentYear = Year.now().getValue();
        if (birthYear != null) {
            if (birthYear < 0 || birthYear > currentYear) {
                throw new IllegalArgumentException("Năm sinh không hợp lệ (phải từ 0 đến " + currentYear + ").");
            }
        }
        if (deathYear != null) {
            if (deathYear < 0 || deathYear > currentYear) {
                throw new IllegalArgumentException("Năm mất không hợp lệ (phải từ 0 đến " + currentYear + ").");
            }
        }
        if (birthYear != null && deathYear != null && deathYear < birthYear) {
            throw new IllegalArgumentException("Năm mất không thể trước năm sinh.");
        }
    }
}
