package com.example.model;

/**
 * DTO đại diện cho số liệu thống kê sách phân bổ theo Ngành (Khối Ngành / Lĩnh Vực).
 */
public class MajorBookStat {

    private String majorName;       // Tên Ngành / Khối ngành
    private int    titleCount;      // Số lượng đầu sách
    private int    totalCopies;     // Tổng số bản sao
    private int    availableCopies; // Số bản còn trong kho
    private int    borrowedCopies;  // Số bản đang được mượn
    private double percentage;      // Tỷ lệ % trong tổng số sách
    private int    categoryCount;   // Số thể loại khác nhau góp mặt trong khối ngành này

    public MajorBookStat() {}

    public MajorBookStat(String majorName, int titleCount, int totalCopies, int availableCopies) {
        this(majorName, titleCount, totalCopies, availableCopies, 0);
    }

    public MajorBookStat(String majorName, int titleCount, int totalCopies, int availableCopies, int categoryCount) {
        this.majorName       = majorName != null && !majorName.isBlank() ? majorName.trim() : "Khác";
        this.titleCount      = titleCount;
        this.totalCopies     = totalCopies;
        this.availableCopies = availableCopies;
        this.borrowedCopies  = Math.max(0, totalCopies - availableCopies);
        this.percentage      = 0.0;
        this.categoryCount   = categoryCount;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public int getTitleCount() {
        return titleCount;
    }

    public void setTitleCount(int titleCount) {
        this.titleCount = titleCount;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public int getBorrowedCopies() {
        return borrowedCopies;
    }

    public void setBorrowedCopies(int borrowedCopies) {
        this.borrowedCopies = borrowedCopies;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }

    public int getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(int categoryCount) {
        this.categoryCount = categoryCount;
    }
}