package com.example.model;

/**
 * Model đại diện cho tài khoản người dùng hệ thống (admin / thủ thư / độc giả).
 */
public class User {

    /** Vai trò của người dùng trong hệ thống. */
    public enum Role {
        ADMIN("Quản trị viên"),
        LIBRARIAN("Thủ thư"),
        READER("Độc giả");

        private final String label;

        Role(String label) { this.label = label; }

        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }

        public static Role fromString(String value) {
            for (Role r : values()) {
                if (r.name().equalsIgnoreCase(value) || r.label.equalsIgnoreCase(value)) {
                    return r;
                }
            }
            return LIBRARIAN;
        }
    }

    private int id;
    private String username;
    private String password;     // Lưu dạng plain hoặc hash tùy AuthService
    private String fullName;
    private String email;        // Email cá nhân (dùng cho OTP và liên hệ)
    private Role role;
    private boolean active;
    private int readerId;        // Liên kết với bảng readers (0 nếu không phải READER)

    // ===================== Constructors =====================

    public User() {
        this.role = Role.LIBRARIAN;
        this.active = true;
        this.readerId = 0;
    }

    /** Constructor đầy đủ (đọc từ DB). */
    public User(int id, String username, String password,
                String fullName, Role role, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.readerId = 0;
    }

    /** Constructor đầy đủ với email và readerId (đọc từ DB). */
    public User(int id, String username, String password,
                String fullName, String email, Role role,
                boolean active, int readerId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
        this.readerId = readerId;
    }

    /** Constructor tạo mới. */
    public User(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = true;
        this.readerId = 0;
    }

    /** Constructor tạo mới với email (dùng cho đăng ký độc giả). */
    public User(String username, String password, String fullName,
                String email, Role role, int readerId) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = true;
        this.readerId = readerId;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getReaderId() { return readerId; }
    public void setReaderId(int readerId) { this.readerId = readerId; }

    // ===================== Business Logic =====================

    /** Kiểm tra người dùng có quyền admin không. */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** Kiểm tra người dùng có phải là độc giả không. */
    public boolean isReader() {
        return role == Role.READER;
    }

    /** Kiểm tra người dùng có quyền quản trị (admin hoặc thủ thư). */
    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.LIBRARIAN;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getLabel() + ")";
    }
}
