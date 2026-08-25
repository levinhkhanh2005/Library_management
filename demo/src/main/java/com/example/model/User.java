package com.example.model;

/**
 * Model đại diện cho tài khoản người dùng hệ thống (admin / thủ thư).
 */
public class User {

    /** Vai trò của người dùng trong hệ thống. */
    public enum Role {
        ADMIN("Quản trị viên"),
        LIBRARIAN("Thủ thư");

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
    private Role role;
    private boolean active;

    // ===================== Constructors =====================

    public User() {
        this.role = Role.LIBRARIAN;
        this.active = true;
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
    }

    /** Constructor tạo mới. */
    public User(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = true;
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

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // ===================== Business Logic =====================

    /** Kiểm tra người dùng có quyền admin không. */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getLabel() + ")";
    }
}
