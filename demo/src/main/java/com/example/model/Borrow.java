package com.example.model;

/**
 * Model đại diện cho một phiếu mượn/trả sách.
 */
public class Borrow {

    /** Trạng thái của phiếu mượn. */
    public enum Status {
        BORROWING("Đang mượn"),
        RETURNED("Đã trả"),
        OVERDUE("Quá hạn"),
        LOST("Mất sách");

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
            return BORROWING;
        }
    }

    private int id;
    private int bookId;
    private int readerId;
    private String borrowDate;   // dd/MM/yyyy
    private String dueDate;      // dd/MM/yyyy — hạn trả
    private String returnDate;   // dd/MM/yyyy — ngày trả thực tế (null nếu chưa trả)
    private Status status;
    private double fineAmount;   // Tiền phạt (đồng)
    private String notes;
    private int renewCount;      // Số lần đã gia hạn

    // Dữ liệu join (không lưu trong DB, dùng để hiển thị)
    private String bookTitle;
    private String bookIsbn;
    private String readerName;
    private String readerCode;

    // ===================== Constructors =====================

    public Borrow() {
        this.status = Status.BORROWING;
        this.fineAmount = 0.0;
        this.renewCount = 0;
    }

    /** Constructor tạo phiếu mượn mới. */
    public Borrow(int bookId, int readerId, String borrowDate,
                  String dueDate, String notes) {
        this.bookId = bookId;
        this.readerId = readerId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.notes = notes;
        this.status = Status.BORROWING;
        this.fineAmount = 0.0;
        this.renewCount = 0;
    }

    /** Constructor đầy đủ (đọc từ DB). */
    public Borrow(int id, int bookId, int readerId, String borrowDate,
                  String dueDate, String returnDate, Status status,
                  double fineAmount, String notes) {
        this.id = id;
        this.bookId = bookId;
        this.readerId = readerId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
        this.fineAmount = fineAmount;
        this.notes = notes;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getReaderId() { return readerId; }
    public void setReaderId(int readerId) { this.readerId = readerId; }

    public String getBorrowDate() { return borrowDate; }
    public void setBorrowDate(String borrowDate) { this.borrowDate = borrowDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getRenewCount() { return renewCount; }
    public void setRenewCount(int renewCount) { this.renewCount = renewCount; }

    // Dữ liệu join (display only)
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }

    public String getReaderName() { return readerName; }
    public void setReaderName(String readerName) { this.readerName = readerName; }

    public String getReaderCode() { return readerCode; }
    public void setReaderCode(String readerCode) { this.readerCode = readerCode; }

    // ===================== Business Logic =====================

    /** Kiểm tra phiếu mượn đang hoạt động (chưa trả). */
    public boolean isActive() {
        return status == Status.BORROWING || status == Status.OVERDUE;
    }

    /** Kiểm tra đã trả sách chưa. */
    public boolean isReturned() {
        return status == Status.RETURNED;
    }

    /** Kiểm tra sách bị mất. */
    public boolean isLost() {
        return status == Status.LOST;
    }

    /** Kiểm tra phiếu có thể gia hạn không (chỉ BORROWING, chưa vượt hạn mức). */
    public boolean canRenew(int maxRenewCount) {
        return status == Status.BORROWING && renewCount < maxRenewCount;
    }

    @Override
    public String toString() {
        return "Phiếu #" + id + " | " + bookTitle + " | " + readerName;
    }
}
