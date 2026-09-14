package com.example.model;

/**
 * Model đại diện cho một cuốn sách trong thư viện.
 */
public class Book {

    private int id;
    private String isbn;
    private String title;
    private String author;
    private String category;
    private String publisher;
    private int publishYear;
    private int totalCopies;
    private int availableCopies;
    private String description;
    private String major;

    // ===================== Constructors =====================

    public Book() {}

    /** Constructor đầy đủ (dùng khi đọc từ DB). */
    public Book(int id, String isbn, String title, String author,
                String category, String publisher, int publishYear,
                int totalCopies, int availableCopies, String description) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.description = description;
        this.major = null;
    }

    public Book(int id, String isbn, String title, String author,
                String category, String publisher, int publishYear,
                int totalCopies, int availableCopies, String description, String major) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.description = description;
        this.major = major != null && !major.isBlank() ? major.trim() : null;
    }

    /** Constructor tạo mới (không có id). */
    public Book(String isbn, String title, String author,
                String category, String publisher, int publishYear,
                int totalCopies, String description) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies; // ban đầu toàn bộ đều có sẵn
        this.description = description;
        this.major = null;
    }

    // ===================== Getters & Setters =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) {
        this.category = category;
        if (this.major == null || this.major.isBlank() || "Khác".equals(this.major)) {
            this.major = null;
        }
    }

    public String getMajor() {
        return major != null && !major.isBlank() ? major : "Chưa phân khối ngành";
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getPublishYear() { return publishYear; }
    public void setPublishYear(int publishYear) { this.publishYear = publishYear; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ===================== Business Logic =====================

    /** Kiểm tra sách có thể được mượn không. */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    /** Số bản đang được mượn. */
    public int getBorrowedCopies() {
        return totalCopies - availableCopies;
    }

    @Override
    public String toString() {
        return title + " - " + author;
    }
}
