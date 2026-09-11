package com.example.model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Model DTO chứa số liệu thống kê mượn và trả sách.
 */
public class BorrowStats {

    private int totalBorrows;      // Tổng số lượt/cuốn sách đã mượn
    private int borrowingCount;    // Số sách đang mượn (trong hạn)
    private int overdueCount;      // Số sách đang mượn (quá hạn)
    private int returnedCount;     // Số sách đã trả thành công
    private int lostCount;         // Số sách báo mất

    private int todayBorrows;      // Số sách mượn hôm nay
    private int todayReturns;      // Số sách trả hôm nay
    private int monthBorrows;      // Số sách mượn tháng này
    private int monthReturns;      // Số sách trả tháng này

    private double totalFines;     // Tổng tiền phạt đã thu (VNĐ)

    public BorrowStats() {}

    public BorrowStats(int totalBorrows, int borrowingCount, int overdueCount,
                       int returnedCount, int lostCount, int todayBorrows,
                       int todayReturns, int monthBorrows, int monthReturns,
                       double totalFines) {
        this.totalBorrows = totalBorrows;
        this.borrowingCount = borrowingCount;
        this.overdueCount = overdueCount;
        this.returnedCount = returnedCount;
        this.lostCount = lostCount;
        this.todayBorrows = todayBorrows;
        this.todayReturns = todayReturns;
        this.monthBorrows = monthBorrows;
        this.monthReturns = monthReturns;
        this.totalFines = totalFines;
    }

    // ===================== Getters & Setters =====================

    public int getTotalBorrows() {
        return totalBorrows;
    }

    public void setTotalBorrows(int totalBorrows) {
        this.totalBorrows = totalBorrows;
    }

    public int getBorrowingCount() {
        return borrowingCount;
    }

    public void setBorrowingCount(int borrowingCount) {
        this.borrowingCount = borrowingCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    public int getReturnedCount() {
        return returnedCount;
    }

    public void setReturnedCount(int returnedCount) {
        this.returnedCount = returnedCount;
    }

    public int getLostCount() {
        return lostCount;
    }

    public void setLostCount(int lostCount) {
        this.lostCount = lostCount;
    }

    public int getTodayBorrows() {
        return todayBorrows;
    }

    public void setTodayBorrows(int todayBorrows) {
        this.todayBorrows = todayBorrows;
    }

    public int getTodayReturns() {
        return todayReturns;
    }

    public void setTodayReturns(int todayReturns) {
        this.todayReturns = todayReturns;
    }

    public int getMonthBorrows() {
        return monthBorrows;
    }

    public void setMonthBorrows(int monthBorrows) {
        this.monthBorrows = monthBorrows;
    }

    public int getMonthReturns() {
        return monthReturns;
    }

    public void setMonthReturns(int monthReturns) {
        this.monthReturns = monthReturns;
    }

    public double getTotalFines() {
        return totalFines;
    }

    public void setTotalFines(double totalFines) {
        this.totalFines = totalFines;
    }

    // ===================== Tiện ích tính toán =====================

    /**
     * Tổng số sách hiện đang lưu hành bên ngoài (đang mượn + quá hạn).
     */
    public int getActiveCount() {
        return borrowingCount + overdueCount;
    }

    /**
     * Tỷ lệ trả sách thành công (%).
     */
    public double getReturnRate() {
        if (totalBorrows <= 0) return 0.0;
        return (double) returnedCount / totalBorrows * 100.0;
    }

    /**
     * Tỷ lệ trả sách định dạng chuỗi (VD: "78.5%").
     */
    public String getFormattedReturnRate() {
        return String.format(Locale.US, "%.1f%%", getReturnRate());
    }

    /**
     * Tiền phạt định dạng tiền tệ VNĐ (VD: "120.000 đ").
     */
    public String getFormattedTotalFines() {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format((long) totalFines) + " đ";
    }
}
