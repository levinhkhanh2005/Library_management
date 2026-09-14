package com.example.model;

/**
 * Model đại diện cho một bản ghi xác thực OTP.
 * Dùng cho đăng ký tài khoản hoặc đặt lại mật khẩu.
 */
public class OtpVerification {

    /** Loại xác thực OTP. */
    public enum OtpType {
        REGISTER("Đăng ký tài khoản"),
        RESET_PASSWORD("Đặt lại mật khẩu");

        private final String label;

        OtpType(String label) { this.label = label; }

        public String getLabel() { return label; }

        public static OtpType fromString(String value) {
            for (OtpType t : values()) {
                if (t.name().equalsIgnoreCase(value)) return t;
            }
            return REGISTER;
        }
    }

    private int id;
    private String email;        // Email nhận mã OTP
    private String otpCode;      // Mã OTP 6 chữ số
    private OtpType type;        // REGISTER hoặc RESET_PASSWORD
    private String payload;      // JSON lưu tạm thông tin đăng ký (họ tên, sdt, ...)
    private String expiresAt;    // Thời điểm hết hạn (datetime)
    private int attempts;        // Số lần nhập sai
    private boolean verified;    // Đã xác thực thành công chưa
    private String createdAt;    // Thời điểm tạo

    /** Số lần nhập sai tối đa trước khi hủy OTP. */
    public static final int MAX_ATTEMPTS = 5;

    /** Thời hạn OTP tính bằng phút. */
    public static final int EXPIRY_MINUTES = 5;

    /** Khoảng cách tối thiểu (giây) giữa 2 lần gửi lại OTP. */
    public static final int RESEND_COOLDOWN_SECONDS = 60;

    // ===================== Constructors =====================

    public OtpVerification() {
        this.type = OtpType.REGISTER;
        this.attempts = 0;
        this.verified = false;
    }

    /** Constructor đầy đủ (đọc từ DB). */
    public OtpVerification(int id, String email, String otpCode, OtpType type,
                           String payload, String expiresAt, int attempts,
                           boolean verified, String createdAt) {
        this.id = id;
        this.email = email;
        this.otpCode = otpCode;
        this.type = type;
        this.payload = payload;
        this.expiresAt = expiresAt;
        this.attempts = attempts;
        this.verified = verified;
        this.createdAt = createdAt;
    }

    /** Constructor tạo mới (chưa có id). */
    public OtpVerification(String email, String otpCode, OtpType type,
                           String payload, String expiresAt) {
        this.email = email;
        this.otpCode = otpCode;
        this.type = type;
        this.payload = payload;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.verified = false;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public OtpType getType() { return type; }
    public void setType(OtpType type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // ===================== Business Logic =====================

    /** Kiểm tra đã vượt quá số lần thử tối đa chưa. */
    public boolean isMaxAttemptsReached() {
        return attempts >= MAX_ATTEMPTS;
    }

    @Override
    public String toString() {
        return "OTP[" + email + ", type=" + type + ", verified=" + verified + "]";
    }
}
