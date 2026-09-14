package com.example.model;

/**
 * Data model for period-based statistics (Borrows & Returns) for chart rendering.
 */
public class ChartPeriodData {

    public enum TimePeriodMode {
        BY_DAY("Theo Ngày"),
        BY_MONTH("Theo Tháng"),
        BY_QUARTER("Theo Quý"),
        BY_YEAR("Theo Năm");

        private final String label;

        TimePeriodMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String[] labels;
    private int[] borrowCounts;
    private int[] returnCounts;

    public ChartPeriodData() {
        this.labels = new String[0];
        this.borrowCounts = new int[0];
        this.returnCounts = new int[0];
    }

    public ChartPeriodData(String[] labels, int[] borrowCounts, int[] returnCounts) {
        this.labels = labels;
        this.borrowCounts = borrowCounts;
        this.returnCounts = returnCounts;
    }

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public int[] getBorrowCounts() {
        return borrowCounts;
    }

    public void setBorrowCounts(int[] borrowCounts) {
        this.borrowCounts = borrowCounts;
    }

    public int[] getReturnCounts() {
        return returnCounts;
    }

    public void setReturnCounts(int[] returnCounts) {
        this.returnCounts = returnCounts;
    }

    public int size() {
        return labels != null ? labels.length : 0;
    }

    public int getMaxVal() {
        int max = 1;
        if (borrowCounts != null) {
            for (int v : borrowCounts) if (v > max) max = v;
        }
        if (returnCounts != null) {
            for (int v : returnCounts) if (v > max) max = v;
        }
        return max;
    }
}
