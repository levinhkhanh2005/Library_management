package com.example.model;

/**
 * DTO đại diện cho số liệu thống kê của một thể loại sách.
 * Dùng để tính toán và hiển thị trên biểu đồ tròn (Pie Chart) và báo cáo.
 */
public class CategoryBookStat {

    private String categoryName;     // Tên thể loại
    private int    titleCount;       // Số lượng đầu sách
    private int    totalCopies;      // Tổng số bản sao (physical copies)
    private int    availableCopies;  // Số bản còn lại trong thư viện
    private int    borrowedCopies;   // Số bản đang được mượn
    private double percentage;       // Tỷ lệ % trong tổng số sách

    public CategoryBookStat() {}

    public CategoryBookStat(String categoryName, int titleCount, int totalCopies, int availableCopies) {
        this.categoryName    = categoryName != null && !categoryName.isBlank() ? categoryName.trim() : "Chưa phân loại";
        this.titleCount      = titleCount;
        this.totalCopies     = totalCopies;
        this.availableCopies = availableCopies;
        this.borrowedCopies  = Math.max(0, totalCopies - availableCopies);
        this.percentage      = 0.0;
    }

    // ===================== Getters & Setters =====================

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    /** Trả về chuỗi định dạng % đẹp, ví dụ "25.4%" */
    public String getFormattedPercentage() {
        return String.format(java.util.Locale.US, "%.1f%%", percentage);
    }

    @Override
    public String toString() {
        return categoryName + " (" + titleCount + " đầu sách, " + totalCopies + " bản - " + getFormattedPercentage() + ")";
    }
}
