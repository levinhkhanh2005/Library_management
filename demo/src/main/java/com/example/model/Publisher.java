package com.example.model;

/**
 * Model đại diện cho một Nhà Xuất Bản (Publisher) trong thư viện.
 */
public class Publisher {

    private int id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String representative;
    private String description;
    private String createdAt;

    // Các trường thống kê phụ trợ (nạp từ câu JOIN/GROUP BY với bảng books)
    private int bookCount;
    private int totalCopies;

    // ===================== Constructors =====================

    public Publisher() {}

    public Publisher(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Publisher(String name, String address, String phone, String email,
                     String website, String representative, String description) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.representative = representative;
        this.description = description;
    }

    public Publisher(int id, String name, String address, String phone, String email,
                     String website, String representative, String description, String createdAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.representative = representative;
        this.description = description;
        this.createdAt = createdAt;
    }

    // ===================== Getters & Setters =====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
