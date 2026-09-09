package com.example.service;

import com.example.dao.CategoryDAO;
import com.example.model.Category;

import java.sql.SQLException;
import java.util.List;

/**
 * Service quản lý nghiệp vụ thể loại sách.
 * Kiểm tra validation, chống trùng lặp dữ liệu không phân biệt hoa thường và xử lý ràng buộc liên quan đến sách.
 */
public class CategoryService {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    // ===================== Thêm thể loại =====================

    /**
     * Thêm thể loại mới sau khi validate.
     * @throws IllegalArgumentException nếu tên rỗng hoặc đã tồn tại
     * @throws SQLException nếu lỗi CSDL
     */
    public Category addCategory(String name, String description) throws SQLException {
        validateName(name);

        Category existing = categoryDAO.findByName(name.trim());
        if (existing != null) {
            throw new IllegalArgumentException("Thể loại \"" + name.trim() + "\" đã tồn tại trong hệ thống.");
        }

        Category category = new Category(
            name.trim(),
            description == null ? "" : description.trim()
        );

        int id = categoryDAO.insert(category);
        if (id == -1) throw new SQLException("Thêm thể loại thất bại.");
        category.setId(id);
        return category;
    }

    // ===================== Cập nhật thể loại =====================

    /**
     * Cập nhật thông tin thể loại. Đồng thời cập nhật tên thể loại cho các sách liên quan nếu tên thay đổi.
     * @throws IllegalArgumentException nếu tên rỗng hoặc trùng với thể loại khác
     * @throws SQLException nếu lỗi CSDL
     */
    public void updateCategory(Category category, String oldName) throws SQLException {
        validateName(category.getName());

        // Kiểm tra xem tên mới có trùng với thể loại nào khác không
        Category existing = categoryDAO.findByName(category.getName().trim());
        if (existing != null && existing.getId() != category.getId()) {
            throw new IllegalArgumentException("Thể loại \"" + category.getName().trim() + "\" đã tồn tại trong hệ thống.");
        }

        category.setName(category.getName().trim());
        category.setDescription(category.getDescription() == null ? "" : category.getDescription().trim());

        if (!categoryDAO.update(category)) {
            throw new SQLException("Cập nhật thể loại thất bại. Thể loại có thể không tồn tại.");
        }

        // Tự động đồng bộ tên mới sang bảng books nếu tên thay đổi
        if (oldName != null && !oldName.trim().equalsIgnoreCase(category.getName())) {
            categoryDAO.updateCategoryInBooks(oldName.trim(), category.getName());
        }
    }

    // ===================== Xóa thể loại =====================

    /**
     * Xóa thể loại. Chặn xóa nếu còn sách thuộc thể loại này.
     * @throws IllegalStateException nếu còn sách đang gán thể loại
     * @throws SQLException nếu lỗi CSDL
     */
    public void deleteCategory(int id) throws SQLException {
        Category category = categoryDAO.findById(id);
        if (category == null) {
            throw new SQLException("Thể loại không tồn tại.");
        }

        int bookCount = categoryDAO.countBooksUsingCategory(category.getName());
        if (bookCount > 0) {
            throw new IllegalStateException(
                "Không thể xóa thể loại \"" + category.getName() + "\".\n" +
                "Hiện có " + bookCount + " cuốn sách đang thuộc thể loại này.\n" +
                "Vui lòng đổi thể loại của các sách trước khi xóa."
            );
        }

        if (!categoryDAO.delete(id)) {
            throw new SQLException("Xóa thể loại thất bại.");
        }
    }

    // ===================== Truy vấn =====================

    public List<Category> getAllCategories() throws SQLException {
        return categoryDAO.findAll();
    }

    public List<Category> searchCategories(String keyword) throws SQLException {
        return categoryDAO.search(keyword);
    }

    public Category getCategoryById(int id) throws SQLException {
        return categoryDAO.findById(id);
    }

    public Category getCategoryByName(String name) throws SQLException {
        return categoryDAO.findByName(name);
    }

    public int countBooksUsingCategory(String categoryName) throws SQLException {
        return categoryDAO.countBooksUsingCategory(categoryName);
    }

    public int getTotalCategories() throws SQLException {
        return categoryDAO.countAll();
    }

    // ===================== Helper =====================

    private void validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống.");
        }
    }
}
