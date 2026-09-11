package com.example.model;

/**
 * Model đại diện cho một tác giả sách trong thư viện.
 */
public class Author {

    private int id;
    private String name;
    private Integer birthYear;
    private Integer deathYear;
    private String nationality;
    private String biography;

    // Các trường thống kê phụ trợ (nạp từ câu JOIN/GROUP BY)
    private int bookCount;
    private int totalCopies;

    // ===================== Constructors =====================

    public Author() {}

    public Author(int id, String name, String nationality) {
        this.id = id;
        this.name = name;
        this.nationality = nationality;
    }

    public Author(String name, Integer birthYear, Integer deathYear, String nationality, String biography) {
        this.name = name;
        this.birthYear = birthYear;
        this.deathYear = deathYear;
        this.nationality = nationality;
        this.biography = biography;
    }

    public Author(int id, String name, Integer birthYear, Integer deathYear,
                  String nationality, String biography) {
        this.id = id;
        this.name = name;
        this.birthYear = birthYear;
        this.deathYear = deathYear;
        this.nationality = nationality;
        this.biography = biography;
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

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getDeathYear() {
        return deathYear;
    }

    public void setDeathYear(Integer deathYear) {
        this.deathYear = deathYear;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
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

    /**
     * Hiển thị chuỗi năm sinh - năm mất.
     * Ví dụ: "1920 - 2014" hoặc "1947 - nay" hoặc "—".
     */
    public String getLifespan() {
        if (birthYear == null && deathYear == null) return "—";
        if (birthYear != null && deathYear != null) return birthYear + " – " + deathYear;
        if (birthYear != null) return birthYear + " – nay";
        return "– " + deathYear;
    }

    @Override
    public String toString() {
        return name;
    }
}
