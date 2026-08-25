package com.example.model;

/**
 * Model đại diện cho một độc giả (thành viên) của thư viện.
 */
public class Reader {

    /** Trạng thái tài khoản độc giả. */
    public enum Status {
        ACTIVE("Hoạt động"),
        LOCKED("Bị khóa"),
        EXPIRED("Hết hạn");

        private final String label;

        Status(String label) { this.label = label; }

        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }

        public static Status fromString(String value) {
            for (Status s : values()) {
                if (s.name().equalsIgnoreCase(value) || s.label.equalsIgnoreCase(value)) {
                    return s;
                }
            }
            return ACTIVE;
        }
    }

    private int id;
    private String readerCode;   // Mã thẻ độc giả (VD: NDG-0001)
    private String fullName;
    private String birthDate;    // Định dạng: dd/MM/yyyy
    private String phone;
    private String email;
    private String address;
    private String joinDate;     // Ngày đăng ký thẻ: dd/MM/yyyy
    private Status status;

    // ===================== Constructors =====================

    public Reader() {
        this.status = Status.ACTIVE;
    }

    /** Constructor đầy đủ (dùng khi đọc từ DB). */
    public Reader(int id, String readerCode, String fullName, String birthDate,
                  String phone, String email, String address,
                  String joinDate, Status status) {
        this.id = id;
        this.readerCode = readerCode;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.joinDate = joinDate;
        this.status = status;
    }

    /** Constructor tạo mới (không có id). */
    public Reader(String readerCode, String fullName, String birthDate,
                  String phone, String email, String address, String joinDate) {
        this.readerCode = readerCode;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.joinDate = joinDate;
        this.status = Status.ACTIVE;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReaderCode() { return readerCode; }
    public void setReaderCode(String readerCode) { this.readerCode = readerCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    // ===================== Business Logic =====================

    /** Kiểm tra độc giả có thể mượn sách không. */
    public boolean canBorrow() {
        return status == Status.ACTIVE;
    }

    @Override
    public String toString() {
        return readerCode + " - " + fullName;
    }
}
