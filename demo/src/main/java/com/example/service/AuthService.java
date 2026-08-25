package com.example.service;

import com.example.dao.UserDAO;
import com.example.model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Service xác thực đăng nhập và quản lý tài khoản người dùng.
 * Lưu thông tin người dùng đang đăng nhập vào session tĩnh.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /** Người dùng đang đăng nhập (null nếu chưa đăng nhập). */
    private static User currentUser = null;

    // ===================== Đăng nhập / Đăng xuất =====================

    /**
     * Đăng nhập hệ thống.
     * @return User nếu thành công
     * @throws IllegalArgumentException nếu sai thông tin
     * @throws SQLException nếu lỗi DB
     */
    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }

        User user = userDAO.authenticate(username.trim(), password);
        if (user == null) {
            throw new IllegalArgumentException(
                "Tên đăng nhập hoặc mật khẩu không đúng.\nVui lòng thử lại."
            );
        }

        currentUser = user;
        System.out.println("[AUTH] Đăng nhập: " + user.getFullName() + " (" + user.getRole().getLabel() + ")");
        return user;
    }

    /**
     * Đăng xuất — xóa session hiện tại.
     */
    public void logout() {
        System.out.println("[AUTH] Đăng xuất: " + (currentUser != null ? currentUser.getUsername() : "?"));
        currentUser = null;
    }

    // ===================== Session =====================

    /** Trả về người dùng đang đăng nhập. */
    public static User getCurrentUser() {
        return currentUser;
    }

    /** Kiểm tra đã đăng nhập chưa. */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Kiểm tra người dùng hiện tại có quyền Admin không. */
    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    // ===================== Quản lý tài khoản =====================

    /**
     * Thêm tài khoản người dùng mới.
     * Chỉ Admin mới được phép tạo tài khoản mới.
     */
    public User addUser(String username, String password, String fullName,
                        User.Role role) throws SQLException {
        requireAdmin();
        validateRequired(username, "Tên đăng nhập");
        validateRequired(password, "Mật khẩu");
        validateRequired(fullName, "Họ tên");

        if (username.length() < 4) {
            throw new IllegalArgumentException("Tên đăng nhập phải có ít nhất 4 ký tự.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (userDAO.isUsernameExists(username.trim())) {
            throw new IllegalArgumentException("Tên đăng nhập \"" + username + "\" đã tồn tại.");
        }

        User user = new User(username.trim(), password, fullName.trim(), role);
        int id    = userDAO.insert(user);
        if (id == -1) throw new SQLException("Tạo tài khoản thất bại.");
        user.setId(id);
        return user;
    }

    /**
     * Đổi mật khẩu.
     * @param userId      ID người dùng cần đổi
     * @param oldPassword mật khẩu cũ (bỏ qua nếu Admin đổi cho người khác)
     * @param newPassword mật khẩu mới
     */
    public void changePassword(int userId, String oldPassword,
                               String newPassword) throws SQLException {
        validateRequired(newPassword, "Mật khẩu mới");
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }

        User target = userDAO.findById(userId);
        if (target == null) throw new IllegalArgumentException("Người dùng không tồn tại.");

        // Nếu đổi cho chính mình: phải nhập đúng mật khẩu cũ
        boolean isSelf = currentUser != null && currentUser.getId() == userId;
        if (isSelf) {
            if (oldPassword == null || !oldPassword.equals(target.getPassword())) {
                throw new IllegalArgumentException("Mật khẩu cũ không đúng.");
            }
        } else {
            requireAdmin(); // Admin mới được đổi mật khẩu người khác
        }

        if (!userDAO.updatePassword(userId, newPassword)) {
            throw new SQLException("Đổi mật khẩu thất bại.");
        }
    }

    /**
     * Kích hoạt / vô hiệu hóa tài khoản. Chỉ Admin.
     */
    public void setUserActive(int userId, boolean active) throws SQLException {
        requireAdmin();
        // Không cho tự khóa tài khoản của chính mình
        if (currentUser != null && currentUser.getId() == userId && !active) {
            throw new IllegalStateException("Không thể tự vô hiệu hóa tài khoản của chính mình.");
        }
        if (!userDAO.setActive(userId, active)) {
            throw new SQLException("Cập nhật trạng thái tài khoản thất bại.");
        }
    }

    /**
     * Xóa tài khoản. Chỉ Admin. Không được xóa tài khoản đang đăng nhập.
     */
    public void deleteUser(int userId) throws SQLException {
        requireAdmin();
        if (currentUser != null && currentUser.getId() == userId) {
            throw new IllegalStateException("Không thể xóa tài khoản đang đăng nhập.");
        }
        if (!userDAO.delete(userId)) {
            throw new SQLException("Xóa tài khoản thất bại.");
        }
    }

    /** Lấy danh sách tất cả tài khoản. Chỉ Admin. */
    public List<User> getAllUsers() throws SQLException {
        requireAdmin();
        return userDAO.findAll();
    }

    // ===================== Helper =====================

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new SecurityException("Chức năng này chỉ dành cho Quản trị viên.");
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }
}
