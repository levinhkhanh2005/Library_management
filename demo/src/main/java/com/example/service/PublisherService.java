package com.example.service;

import com.example.dao.PublisherDAO;
import com.example.model.Book;
import com.example.model.Publisher;

import java.sql.SQLException;
import java.util.List;

/**
 * Service quản lý nghiệp vụ Nhà Xuất Bản (Publisher).
 * Kiểm tra validation, chống trùng lặp dữ liệu không phân biệt hoa thường và xử lý ràng buộc liên quan đến sách.
 */
public class PublisherService {

    private final PublisherDAO publisherDAO = new PublisherDAO();

    // ===================== Thêm Nhà Xuất Bản =====================

    /**
     * Thêm nhà xuất bản mới sau khi validate.
     * @throws IllegalArgumentException nếu tên rỗng hoặc đã tồn tại
     * @throws SQLException nếu lỗi CSDL
     */
    public Publisher addPublisher(String name, String address, String phone, String email,
                                  String website, String representative, String description) throws SQLException {
        validateName(name);
        validateEmail(email);

        Publisher existing = publisherDAO.findByName(name.trim());
        if (existing != null) {
            throw new IllegalArgumentException("Nhà xuất bản \"" + name.trim() + "\" đã tồn tại trong hệ thống.");
        }

        Publisher publisher = new Publisher(
            name.trim(),
            address == null ? "" : address.trim(),
            phone == null ? "" : phone.trim(),
            email == null ? "" : email.trim(),
            website == null ? "" : website.trim(),
            representative == null ? "" : representative.trim(),
            description == null ? "" : description.trim()
        );

        int id = publisherDAO.insert(publisher);
        if (id == -1) throw new SQLException("Thêm nhà xuất bản thất bại.");
        publisher.setId(id);
        return publisher;
    }

    // ===================== Cập nhật Nhà Xuất Bản =====================

    /**
     * Cập nhật thông tin NXB. Tự động cập nhật tên NXB trong bảng books nếu tên thay đổi.
     * @throws IllegalArgumentException nếu tên rỗng hoặc trùng với NXB khác
     * @throws SQLException nếu lỗi CSDL
     */
    public void updatePublisher(Publisher publisher, String oldName) throws SQLException {
        validateName(publisher.getName());
        validateEmail(publisher.getEmail());

        // Kiểm tra xem tên mới có trùng với NXB nào khác không
        Publisher existing = publisherDAO.findByName(publisher.getName().trim());
        if (existing != null && existing.getId() != publisher.getId()) {
            throw new IllegalArgumentException("Nhà xuất bản \"" + publisher.getName().trim() + "\" đã tồn tại trong hệ thống.");
        }

        publisher.setName(publisher.getName().trim());
        publisher.setAddress(publisher.getAddress() == null ? "" : publisher.getAddress().trim());
        publisher.setPhone(publisher.getPhone() == null ? "" : publisher.getPhone().trim());
        publisher.setEmail(publisher.getEmail() == null ? "" : publisher.getEmail().trim());
        publisher.setWebsite(publisher.getWebsite() == null ? "" : publisher.getWebsite().trim());
        publisher.setRepresentative(publisher.getRepresentative() == null ? "" : publisher.getRepresentative().trim());
        publisher.setDescription(publisher.getDescription() == null ? "" : publisher.getDescription().trim());

        if (!publisherDAO.update(publisher)) {
            throw new SQLException("Cập nhật nhà xuất bản thất bại. NXB có thể không tồn tại.");
        }

        // Tự động đồng bộ tên mới sang bảng books nếu tên thay đổi
        if (oldName != null && !oldName.trim().equalsIgnoreCase(publisher.getName())) {
            publisherDAO.updatePublisherInBooks(oldName.trim(), publisher.getName());
        }
    }

    // ===================== Xóa Nhà Xuất Bản =====================

    /**
     * Xóa NXB. Chặn xóa nếu còn sách thuộc NXB này.
     * @throws IllegalStateException nếu còn sách đang thuộc NXB
     * @throws SQLException nếu lỗi CSDL
     */
    public void deletePublisher(int id) throws SQLException {
        Publisher publisher = publisherDAO.findById(id);
        if (publisher == null) {
            throw new SQLException("Nhà xuất bản không tồn tại.");
        }

        int bookCount = publisherDAO.countBooksUsingPublisher(publisher.getName());
        if (bookCount > 0) {
            throw new IllegalStateException(
                "Không thể xóa nhà xuất bản \"" + publisher.getName() + "\".\n" +
                "Hiện có " + bookCount + " cuốn sách đang thuộc nhà xuất bản này.\n" +
                "Vui lòng đổi nhà xuất bản của các cuốn sách trước khi xóa."
            );
        }

        if (!publisherDAO.delete(id)) {
            throw new SQLException("Xóa nhà xuất bản thất bại.");
        }
    }

    // ===================== Truy vấn =====================

    public List<Publisher> getAllPublishers() throws SQLException {
        return publisherDAO.findAll();
    }

    public List<Publisher> getAllPublishersWithStats() throws SQLException {
        return publisherDAO.findAllWithStats();
    }

    public List<Publisher> searchPublishers(String keyword) throws SQLException {
        return publisherDAO.search(keyword);
    }

    public Publisher getPublisherById(int id) throws SQLException {
        return publisherDAO.findById(id);
    }

    public Publisher getPublisherByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        Publisher p = publisherDAO.findByName(name.trim());
        if (p == null && !name.trim().toUpperCase().startsWith("NXB ")) {
            p = publisherDAO.findByName("NXB " + name.trim());
        }
        if (p == null && name.trim().toUpperCase().startsWith("NXB ")) {
            p = publisherDAO.findByName(name.trim().substring(4).trim());
        }
        return p;
    }

    public List<Book> getBooksByPublisher(String publisherName) throws SQLException {
        return publisherDAO.findBooksByPublisher(publisherName);
    }

    public int countBooksUsingPublisher(String publisherName) throws SQLException {
        return publisherDAO.countBooksUsingPublisher(publisherName);
    }

    public int getTotalPublishers() throws SQLException {
        return publisherDAO.countAll();
    }

    // ===================== Validation =====================

    private void validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Tên nhà xuất bản không được để trống.");
        }
    }

    private void validateEmail(String email) {
        if (email != null && !email.trim().isBlank()) {
            String trimmed = email.trim();
            if (!trimmed.contains("@") || !trimmed.contains(".")) {
                throw new IllegalArgumentException("Email không đúng định dạng (VD: contact@nxb.vn).");
            }
        }
    }
}
