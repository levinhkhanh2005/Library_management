package com.example.model;

/**
 * Model ghi nhận lịch sử gửi email nhắc nhở.
 * Dùng để kiểm tra chống spam và hiển thị lịch sử.
 */
public class EmailLog {

    private int id;
    private int borrowId;
    private int readerId;
    private String recipientEmail;
    private String subject;
    private String content;
    private String status;         // "SUCCESS" hoặc "FAILED"
    private String errorMessage;
    private String sentAt;         // datetime string

    // ===================== Constructors =====================

    public EmailLog() {}

    /** Constructor tạo log mới (chưa có id). */
    public EmailLog(int borrowId, int readerId, String recipientEmail,
                    String subject, String content, String status,
                    String errorMessage) {
        this.borrowId = borrowId;
        this.readerId = readerId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBorrowId() { return borrowId; }
    public void setBorrowId(int borrowId) { this.borrowId = borrowId; }

    public int getReaderId() { return readerId; }
    public void setReaderId(int readerId) { this.readerId = readerId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    // ===================== Helpers =====================

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "EmailLog{borrow=" + borrowId + ", to=" + recipientEmail
             + ", status=" + status + ", at=" + sentAt + "}";
    }
}
