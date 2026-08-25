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
    public void setCategory(String category) { this.category = category; }

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
