package com.example.model;

/**
 * Model đại diện cho cấu hình gửi email (SMTP).
 * Lưu trữ thông tin kết nối đến mail server.
 */
public class EmailConfig {

    private String host;        // SMTP host (vd: smtp.gmail.com)
    private int port;           // SMTP port (vd: 587)
    private String username;    // Email gửi (vd: thuvien@gmail.com)
    private String password;    // Mật khẩu ứng dụng (App Password)
    private String fromName;    // Tên người gửi (vd: Thư Viện Nguyễn Huệ)
    private boolean startTls;   // Bật mã hóa TLS

    // ===================== Constructors =====================

    public EmailConfig() {
        this.host = "smtp.gmail.com";
        this.port = 587;
        this.fromName = "Thư Viện Nguyễn Huệ";
        this.startTls = true;
    }

    public EmailConfig(String host, int port, String username, String password,
                       String fromName, boolean startTls) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.fromName = fromName;
        this.startTls = startTls;
    }

    // ===================== Getters & Setters =====================

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public boolean isStartTls() { return startTls; }
    public void setStartTls(boolean startTls) { this.startTls = startTls; }

    // ===================== Validation =====================

    /** Kiểm tra cấu hình có đầy đủ thông tin để gửi email. */
    public boolean isValid() {
        return host != null && !host.isBlank()
            && port > 0
            && username != null && !username.isBlank()
            && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "EmailConfig{" + username + "@" + host + ":" + port + "}";
    }
}
