package com.example.view.dialogs;

import com.example.model.BorrowStats;
import com.example.service.BorrowService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Hộp thoại thống kê chuyên sâu số lượng sách đã mượn và trả.
 * Hỗ trợ lọc theo chu kỳ thời gian (Hôm nay, 7 ngày, Tháng này, Năm nay, Tùy chọn),
 * hiển thị thẻ KPI, thanh phân bổ trạng thái mượn/trả và bảng phân rã theo thể loại/độc giả.
 */
public class BorrowStatsDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BorrowService borrowService = new BorrowService();

    // UI Filter components
    private JComboBox<String> cbPeriod;
    private JTextField fFromDate, fToDate;
    private JButton btnFilter;

    // Stat card labels
    private JLabel lblTotalBorrowsVal, lblTotalBorrowsSub;
    private JLabel lblReturnedVal, lblReturnedSub;
    private JLabel lblActiveVal, lblActiveSub;
    private JLabel lblOverdueVal, lblOverdueSub;
    private JLabel lblFinesVal, lblFinesSub;

    // Visual Distribution Bar
    private DistributionBar distributionBar;

    // Tables
    private DefaultTableModel categoryModel;
    private JTable categoryTable;
    private DefaultTableModel readerModel;
    private JTable readerTable;

    private JLabel statusLabel;

    public BorrowStatsDialog(Window parent) {
        super(parent, "Thống Kê Số Lượng Sách Đã Mượn Và Trả", ModalityType.APPLICATION_MODAL);
        initUI();
        setSize(860, 680);
        setMinimumSize(new Dimension(780, 580));
        setLocationRelativeTo(parent);
        loadStatistics();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        root.setBackground(UITheme.BG_PRIMARY);
        root.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildContent(),  BorderLayout.CENTER);
        root.add(buildFooter(),   BorderLayout.SOUTH);
    }

    // ================================================================
    //  Header & Filter Bar
    // ================================================================

    private JPanel buildTitleBar() {
        JPanel container = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        container.setOpaque(false);

        // Header text banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UITheme.ACCENT_PRIMARY);
        banner.setBorder(new EmptyBorder(12, UITheme.PAD_LG, 12, UITheme.PAD_LG));

        JLabel title = new JLabel("📊  Thống Kê Số Lượng Sách Đã Mượn & Đã Trả");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Phân tích chi tiết số liệu sách lưu thông, tình trạng hoàn trả và tiến độ mượn trả");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(new Color(0xC7D2FE));

        banner.add(title, BorderLayout.NORTH);
        banner.add(subtitle, BorderLayout.SOUTH);
        container.add(banner, BorderLayout.NORTH);

        // Filter toolbar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setBackground(UITheme.BG_WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(4, 10, 4, 10)
        ));

        JLabel lblPeriod = new JLabel("Thời gian:");
        lblPeriod.setFont(UITheme.FONT_BOLD);
        lblPeriod.setForeground(UITheme.TEXT_PRIMARY);
        filterBar.add(lblPeriod);

        String[] periods = {
            "Toàn bộ thời gian",
            "Hôm nay",
            "7 ngày gần nhất",
            "Tháng này",
            "Năm nay",
            "Tùy chọn khoảng ngày..."
        };
        cbPeriod = new JComboBox<>(periods);
        cbPeriod.setFont(UITheme.FONT_BODY);
        cbPeriod.setPreferredSize(new Dimension(170, UITheme.INPUT_HEIGHT));
        filterBar.add(cbPeriod);

        JLabel lblFrom = new JLabel("Từ:");
        lblFrom.setFont(UITheme.FONT_SMALL);
        filterBar.add(lblFrom);

        fFromDate = UITheme.createTextField("dd/MM/yyyy");
        fFromDate.setPreferredSize(new Dimension(95, UITheme.INPUT_HEIGHT));
        fFromDate.setEnabled(false);
        filterBar.add(fFromDate);

        JLabel lblTo = new JLabel("Đến:");
        lblTo.setFont(UITheme.FONT_SMALL);
        filterBar.add(lblTo);

        fToDate = UITheme.createTextField("dd/MM/yyyy");
        fToDate.setPreferredSize(new Dimension(95, UITheme.INPUT_HEIGHT));
        fToDate.setEnabled(false);
        filterBar.add(fToDate);

        btnFilter = UITheme.createPrimaryButton("Lọc");
        btnFilter.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));
        filterBar.add(btnFilter);

        JButton btnReset = UITheme.createSecondaryButton("↺ Mặc Định");
        btnReset.setPreferredSize(new Dimension(100, UITheme.INPUT_HEIGHT));
        filterBar.add(btnReset);

        cbPeriod.addActionListener(e -> {
            boolean isCustom = cbPeriod.getSelectedIndex() == 5;
            fFromDate.setEnabled(isCustom);
            fToDate.setEnabled(isCustom);
            if (!isCustom) {
                applyPresetPeriod(cbPeriod.getSelectedIndex());
                loadStatistics();
            }
        });

        btnFilter.addActionListener(e -> loadStatistics());
        btnReset.addActionListener(e -> {
            cbPeriod.setSelectedIndex(0);
            fFromDate.setText("");
            fToDate.setText("");
            loadStatistics();
        });

        container.add(filterBar, BorderLayout.SOUTH);
        return container;
    }

    private void applyPresetPeriod(int index) {
        LocalDate now = LocalDate.now();
        switch (index) {
            case 1 -> { // Hôm nay
                fFromDate.setText(now.format(DATE_FMT));
                fToDate.setText(now.format(DATE_FMT));
            }
            case 2 -> { // 7 ngày qua
                fFromDate.setText(now.minusDays(6).format(DATE_FMT));
                fToDate.setText(now.format(DATE_FMT));
            }
            case 3 -> { // Tháng này
                LocalDate firstDay = now.withDayOfMonth(1);
                fFromDate.setText(firstDay.format(DATE_FMT));
                fToDate.setText(now.format(DATE_FMT));
            }
            case 4 -> { // Năm nay
                LocalDate firstDay = now.withDayOfYear(1);
                fFromDate.setText(firstDay.format(DATE_FMT));
                fToDate.setText(now.format(DATE_FMT));
            }
            default -> { // Toàn bộ
                fFromDate.setText("");
                fToDate.setText("");
            }
        }
    }

    // ================================================================
    //  Center: Cards + Visual Bar + Detail Tables
    // ================================================================

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG_PRIMARY);

        // 1. KPI Stat Cards (4 Cards)
        content.add(buildStatCardsRow());
        content.add(Box.createVerticalStrut(UITheme.PAD_SM));

        // 2. Visual Distribution Bar
        content.add(buildDistributionCard());
        content.add(Box.createVerticalStrut(UITheme.PAD_SM));

        // 3. Tabbed Tables (Theo Thể Loại & Top Độc Giả)
        content.add(buildTabsSection());

        return content;
    }

    private JPanel buildStatCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 8, 0));
        row.setBackground(UITheme.BG_PRIMARY);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        // Card 1: Tổng đã mượn
        lblTotalBorrowsVal = new JLabel("0 cuốn");
        lblTotalBorrowsSub = new JLabel("Tổng số đã mượn");
        row.add(createModernStatCard("TỔNG SÁCH ĐÃ MƯỢN", lblTotalBorrowsVal, lblTotalBorrowsSub, UITheme.ACCENT_PRIMARY));

        // Card 2: Đã trả sách
        lblReturnedVal = new JLabel("0 cuốn");
        lblReturnedSub = new JLabel("Tỷ lệ: —%");
        row.add(createModernStatCard("SÁCH ĐÃ TRẢ", lblReturnedVal, lblReturnedSub, UITheme.COLOR_SUCCESS));

        // Card 3: Đang mượn
        lblActiveVal = new JLabel("0 cuốn");
        lblActiveSub = new JLabel("Đang lưu hành");
        row.add(createModernStatCard("SÁCH ĐANG MƯỢN", lblActiveVal, lblActiveSub, new Color(0x2563EB)));

        // Card 4: Quá hạn
        lblOverdueVal = new JLabel("0 cuốn");
        lblOverdueSub = new JLabel("Cần thu hồi");
        row.add(createModernStatCard("SÁCH QUÁ HẠN / MẤT", lblOverdueVal, lblOverdueSub, UITheme.COLOR_DANGER));

        // Card 5: Tiền phạt
        lblFinesVal = new JLabel("0 đ");
        lblFinesSub = new JLabel("Đã thu");
        row.add(createModernStatCard("TIỀN PHẠT THU ĐƯỢC", lblFinesVal, lblFinesSub, UITheme.COLOR_WARNING));

        return row;
    }

    private JPanel createModernStatCard(String title, JLabel valLbl, JLabel subLbl, Color color) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(UITheme.BG_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);

        valLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        valLbl.setForeground(color);

        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 2));
        center.setOpaque(false);
        center.add(valLbl);
        center.add(subLbl);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);

        // Thanh màu trên đầu card
        JPanel colorTop = new JPanel();
        colorTop.setPreferredSize(new Dimension(0, 3));
        colorTop.setBackground(color);
        card.add(colorTop, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildDistributionCard() {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(UITheme.BG_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel title = new JLabel("Tỷ lệ phân bổ trạng thái sách mượn:");
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);

        distributionBar = new DistributionBar();
        distributionBar.setPreferredSize(new Dimension(0, 18));

        card.add(title, BorderLayout.NORTH);
        card.add(distributionBar, BorderLayout.CENTER);
        return card;
    }

    private JTabbedPane buildTabsSection() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BOLD);

        // Tab 1: Theo Thể Loại
        tabs.addTab("📂 Thống Kê Theo Thể Loại Sách", buildCategoryTab());

        // Tab 2: Top Độc Giả
        tabs.addTab("👤 Top Độc Giả Mượn / Trả Nhiều Nhất", buildReaderTab());

        return tabs;
    }

    private JPanel buildCategoryTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_WHITE);

        String[] cols = {"#", "Thể Loại Sách", "Tổng Lượt Mượn", "Đã Trả", "Đang Mượn", "Tỷ Lệ Đã Trả"};
        categoryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 2 || c == 3 || c == 4) ? Integer.class : String.class;
            }
        };

        categoryTable = new JTable(categoryModel);
        UITheme.styleTable(categoryTable);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        categoryTable.getColumnModel().getColumn(0).setMaxWidth(45);
        categoryTable.getColumnModel().getColumn(0).setCellRenderer(center);
        categoryTable.getColumnModel().getColumn(2).setCellRenderer(center);
        categoryTable.getColumnModel().getColumn(3).setCellRenderer(center);
        categoryTable.getColumnModel().getColumn(4).setCellRenderer(center);
        categoryTable.getColumnModel().getColumn(5).setCellRenderer(center);

        p.add(UITheme.createTableScrollPane(categoryTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildReaderTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG_WHITE);

        String[] cols = {"#", "Mã Độc Giả", "Họ Tên Độc Giả", "Tổng Lượt Mượn", "Đã Trả", "Đang Giữ Sách"};
        readerModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 3 || c == 4 || c == 5) ? Integer.class : String.class;
            }
        };

        readerTable = new JTable(readerModel);
        UITheme.styleTable(readerTable);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        readerTable.getColumnModel().getColumn(0).setMaxWidth(45);
        readerTable.getColumnModel().getColumn(0).setCellRenderer(center);
        readerTable.getColumnModel().getColumn(1).setCellRenderer(center);
        readerTable.getColumnModel().getColumn(3).setCellRenderer(center);
        readerTable.getColumnModel().getColumn(4).setCellRenderer(center);
        readerTable.getColumnModel().getColumn(5).setCellRenderer(center);

        p.add(UITheme.createTableScrollPane(readerTable), BorderLayout.CENTER);
        return p;
    }

    // ================================================================
    //  Footer
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(UITheme.PAD_SM, 0, 0, 0));

        statusLabel = UITheme.createMutedLabel("Đang tải dữ liệu thống kê...");
        footer.add(statusLabel, BorderLayout.WEST);

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        btnClose.setPreferredSize(new Dimension(85, 32));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose, BorderLayout.EAST);

        return footer;
    }

    // ================================================================
    //  Data Loading
    // ================================================================

    private void loadStatistics() {
        statusLabel.setText("Đang tính toán số liệu thống kê...");
        String fromDate = fFromDate.getText().trim();
        String toDate   = fToDate.getText().trim();
        boolean hasFilter = (!fromDate.isEmpty() && !fromDate.equals("dd/MM/yyyy"))
                         || (!toDate.isEmpty() && !toDate.equals("dd/MM/yyyy"));

        SwingWorker<BorrowStats, Void> worker = new SwingWorker<>() {
            private List<Object[]> categories;
            private List<Object[]> readers;

            @Override protected BorrowStats doInBackground() throws Exception {
                categories = borrowService.getBorrowStatsByCategory();
                readers    = borrowService.getTopReadersByBorrows(20);

                if (hasFilter) {
                    return borrowService.getBorrowStatsByPeriod(
                        fromDate.equals("dd/MM/yyyy") ? "" : fromDate,
                        toDate.equals("dd/MM/yyyy") ? "" : toDate
                    );
                } else {
                    return borrowService.getBorrowStats();
                }
            }

            @Override protected void done() {
                try {
                    BorrowStats stats = get();

                    // 1. Cập nhật thẻ KPI
                    lblTotalBorrowsVal.setText(stats.getTotalBorrows() + " cuốn");
                    lblTotalBorrowsSub.setText(hasFilter ? "Trong kỳ lọc"
                            : "Hôm nay: +" + stats.getTodayBorrows() + " cuốn");

                    lblReturnedVal.setText(stats.getReturnedCount() + " cuốn");
                    lblReturnedSub.setText(String.format("Tỷ lệ trả: %.1f%%", stats.getReturnRate()));

                    lblActiveVal.setText(stats.getBorrowingCount() + " cuốn");
                    lblActiveSub.setText(stats.getBorrowingCount() > 0 ? "Đang mượn lưu hành" : "Không có");

                    int overdueAndLost = stats.getOverdueCount() + stats.getLostCount();
                    lblOverdueVal.setText(overdueAndLost + " cuốn");
                    lblOverdueSub.setText(stats.getOverdueCount() + " quá hạn, " + stats.getLostCount() + " mất");

                    lblFinesVal.setText(stats.getFormattedTotalFines());
                    lblFinesSub.setText("Tiền phạt đã thu");

                    // 2. Cập nhật thanh tỷ lệ phân bổ
                    distributionBar.setValues(
                        stats.getReturnedCount(),
                        stats.getBorrowingCount(),
                        stats.getOverdueCount(),
                        stats.getLostCount()
                    );

                    // 3. Nạp bảng thể loại
                    categoryModel.setRowCount(0);
                    int catIdx = 1;
                    for (Object[] row : categories) {
                        int tot = (Integer) row[1];
                        int ret = (Integer) row[2];
                        double rate = tot > 0 ? ((double) ret / tot * 100.0) : 0.0;
                        categoryModel.addRow(new Object[]{
                            catIdx++,
                            row[0],
                            row[1],
                            row[2],
                            row[3],
                            String.format("%.1f%%", rate)
                        });
                    }

                    // 4. Nạp bảng độc giả
                    readerModel.setRowCount(0);
                    int rIdx = 1;
                    for (Object[] row : readers) {
                        readerModel.addRow(new Object[]{
                            rIdx++,
                            row[0],
                            row[1],
                            row[2],
                            row[3],
                            row[4]
                        });
                    }

                    statusLabel.setText("✔ Thống kê tổng: " + stats.getTotalBorrows() + " lượt mượn ("
                        + stats.getReturnedCount() + " đã trả, "
                        + stats.getBorrowingCount() + " đang mượn). Cập nhật lúc: "
                        + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

                } catch (Exception ex) {
                    statusLabel.setText("Lỗi nạp số liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ================================================================
    //  Custom Component: Distribution Bar (Thanh phân bổ tỷ lệ)
    // ================================================================

    private static class DistributionBar extends JComponent {
        private int returned = 0;
        private int borrowing = 0;
        private int overdue = 0;
        private int lost = 0;

        public void setValues(int returned, int borrowing, int overdue, int lost) {
            this.returned = returned;
            this.borrowing = borrowing;
            this.overdue = overdue;
            this.lost = lost;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int total = returned + borrowing + overdue + lost;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 8;

            if (total == 0) {
                g2.setColor(new Color(0xE2E8F0));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(UITheme.TEXT_MUTED);
                g2.setFont(UITheme.FONT_SMALL);
                String msg = "Chưa có dữ liệu mượn trả";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
                return;
            }

            int wRet = (int) Math.round((double) returned / total * w);
            int wBor = (int) Math.round((double) borrowing / total * w);
            int wOve = (int) Math.round((double) overdue / total * w);
            int wLos = w - (wRet + wBor + wOve);

            int x = 0;
            // Đã trả (Xanh lá)
            if (wRet > 0) {
                g2.setColor(UITheme.COLOR_SUCCESS);
                g2.fillRect(x, 0, wRet, h);
                x += wRet;
            }
            // Đang mượn (Xanh dương)
            if (wBor > 0) {
                g2.setColor(new Color(0x2563EB));
                g2.fillRect(x, 0, wBor, h);
                x += wBor;
            }
            // Quá hạn (Đỏ)
            if (wOve > 0) {
                g2.setColor(UITheme.COLOR_DANGER);
                g2.fillRect(x, 0, wOve, h);
                x += wOve;
            }
            // Mất sách (Cam)
            if (wLos > 0) {
                g2.setColor(UITheme.COLOR_WARNING);
                g2.fillRect(x, 0, wLos, h);
            }

            // Bo viền ngoài
            g2.setColor(UITheme.BORDER_COLOR);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        }
    }
}
