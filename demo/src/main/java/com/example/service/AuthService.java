package com.example.service;

import com.example.dao.LoginLogDAO;
import com.example.dao.ReaderDAO;
import com.example.dao.UserDAO;
import com.example.model.Reader;
import com.example.model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xác thực đăng nhập và quản lý tài khoản người dùng.
 * Lưu thông tin người dùng đang đăng nhập vào session tĩnh.
 */
public class AuthService {

    private final UserDAO     userDAO     = new UserDAO();
    private final ReaderDAO   readerDAO   = new ReaderDAO();
    private final LoginLogDAO loginLogDAO = new LoginLogDAO();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

        // Ghi log hoạt động đăng nhập
        try {
            loginLogDAO.insert(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                "LOGIN"
            );
        } catch (Exception e) {
            System.err.println("[AuthService] Lỗi ghi login log: " + e.getMessage());
        }

        return user;
    }

    /**
     * Đăng xuất — xóa session hiện tại.
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println("[AUTH] Đăng xuất: " + currentUser.getUsername());
            // Ghi log hoạt động đăng xuất
            try {
                loginLogDAO.insert(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getFullName(),
                    currentUser.getRole().name(),
                    "LOGOUT"
                );
            } catch (Exception e) {
                System.err.println("[AuthService] Lỗi ghi logout log: " + e.getMessage());
            }
        }
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

    /** Kiểm tra người dùng hiện tại có phải là Độc giả không. */
    public static boolean isReader() {
        return currentUser != null && currentUser.isReader();
    }

    /** Kiểm tra người dùng hiện tại có quyền quản trị (Admin hoặc Thủ thư) không. */
    public static boolean isStaff() {
        return currentUser != null && currentUser.isStaff();
    }

    // ===================== Đăng ký tài khoản Độc giả =====================

    /**
     * Đăng ký tài khoản độc giả mới (sau khi đã xác thực OTP thành công).
     * Quy trình:
     *   1. Validate dữ liệu đầu vào.
     *   2. Kiểm tra username và email chưa tồn tại.
     *   3. Tạo bản ghi Reader trong bảng readers (sinh mã thẻ tự động).
     *   4. Tạo bản ghi User trong bảng users với role READER, liên kết reader_id.
     *
     * @return User vừa tạo
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ
     * @throws SQLException nếu lỗi DB
     */
    public User registerReader(String username, String password, String fullName,
                               String email, String phone, String birthDate,
                               String address) throws SQLException {
        // 1. Validate
        validateRequired(username, "Tên đăng nhập");
        validateRequired(password, "Mật khẩu");
        validateRequired(fullName, "Họ tên");
        validateRequired(email, "Email");

        if (username.trim().length() < 4) {
            throw new IllegalArgumentException("Tên đăng nhập phải có ít nhất 4 ký tự.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (!email.trim().matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }

        // 2. Kiểm tra trùng lặp
        if (userDAO.isUsernameExists(username.trim())) {
            throw new IllegalArgumentException("Tên đăng nhập \"" + username.trim() + "\" đã tồn tại.");
        }
        if (userDAO.isEmailExists(email.trim())) {
            throw new IllegalArgumentException("Email \"" + email.trim() + "\" đã được sử dụng.");
        }

        // 3. Tạo Reader (thẻ thư viện)
        int maxNum = readerDAO.getMaxReaderCodeNumber();
        String readerCode = String.format("NDG-%04d", maxNum + 1);
        String today = LocalDate.now().format(DATE_FMT);

        Reader reader = new Reader(
            readerCode,
            fullName.trim(),
            birthDate == null ? "" : birthDate.trim(),
            phone == null ? "" : phone.trim(),
            email.trim(),
            address == null ? "" : address.trim(),
            today
        );
        // Đặt hạn thẻ 1 năm
        reader.setExpiryDate(LocalDate.now().plusYears(1).format(DATE_FMT));

        int readerId = readerDAO.insert(reader);
        if (readerId == -1) throw new SQLException("Tạo thẻ độc giả thất bại.");
        reader.setId(readerId);

        // 4. Tạo User liên kết
        User user = new User(
            username.trim(), password, fullName.trim(),
            email.trim(), User.Role.READER, readerId
        );
        int userId = userDAO.insert(user);
        if (userId == -1) throw new SQLException("Tạo tài khoản thất bại.");
        user.setId(userId);

        System.out.println("[AUTH] Đăng ký thành công: " + user.getFullName()
            + " (Mã thẻ: " + readerCode + ")");
        return user;
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
        validateRequired(oldPassword, "Mật khẩu hiện tại");
        validateRequired(newPassword, "Mật khẩu mới");
        String trimmedOld = oldPassword.trim();
        String trimmedNew = newPassword.trim();

        if (trimmedNew.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }

        User target = userDAO.findById(userId);
        if (target == null) throw new IllegalArgumentException("Người dùng không tồn tại.");

        // Nếu đổi cho chính mình: phải nhập đúng mật khẩu cũ
        boolean isSelf = currentUser != null && currentUser.getId() == userId;
        if (isSelf) {
            String dbPass   = target.getPassword() != null ? target.getPassword().trim() : "";
            String sessPass = currentUser.getPassword() != null ? currentUser.getPassword().trim() : "";
            boolean match = trimmedOld.equals(dbPass) || trimmedOld.equals(sessPass);

            if (!match) {
                throw new IllegalArgumentException("Mật khẩu cũ không đúng.");
            }
        } else {
            requireAdmin(); // Admin mới được đổi mật khẩu người khác
        }

        if (!userDAO.updatePassword(userId, trimmedNew)) {
            throw new SQLException("Đổi mật khẩu thất bại.");
        }

        // Cập nhật session tĩnh nếu tự đổi mật khẩu của chính mình
        if (isSelf && currentUser != null) {
            currentUser.setPassword(trimmedNew);
        }
    }

    /**
     * Cập nhật thông tin người dùng. Chỉ Admin.
     */
    public void updateUser(User user) throws SQLException {
        requireAdmin();
        if (!userDAO.update(user)) {
            throw new SQLException("Cập nhật thông tin tài khoản thất bại.");
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
