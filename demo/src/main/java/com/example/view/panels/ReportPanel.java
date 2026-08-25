package com.example.view.panels;

import com.example.model.Borrow;
import com.example.service.BookService;
import com.example.service.BorrowService;
import com.example.service.ReaderService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/**
 * Panel Báo Cáo & Thống Kê — tổng quan, biểu đồ mượn theo tháng, top sách, ds quá hạn.
 */
public class ReportPanel extends JPanel implements MainFrame.Refreshable {

    private final BookService   bookService   = new BookService();
    private final ReaderService readerService = new ReaderService();
    private final BorrowService borrowService = new BorrowService();

    // Stat labels
    private JLabel lblTotalBooks, lblAvailBooks, lblTotalReaders, lblActiveReaders;
    private JLabel lblTotalBorrows, lblActiveBorrows, lblOverdueBorrows;

    // Chart
    private int[]         monthlyData = new int[12];
    private int           chartYear   = Year.now().getValue();
    private JPanel        chartPanel;

    // Tables
    private DefaultTableModel topBooksModel;
    private DefaultTableModel overdueModel;

    public ReportPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_MD));
        setBackground(UITheme.BG_PRIMARY);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadData();
    }

    // ================================================================
    //  Header
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_PRIMARY);

        header.add(UITheme.createPageHeader("📊  Báo Cáo & Thống Kê",
            "Tổng hợp hoạt động thư viện, biểu đồ mượn sách và danh sách quá hạn"),
            BorderLayout.WEST);

        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");
        btnRefresh.addActionListener(e -> loadData());
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.setOpaque(false);
        btnWrapper.add(btnRefresh);
        header.add(btnWrapper, BorderLayout.EAST);
        return header;
    }

    // ================================================================
    //  Content: stat cards + chart + tables
    // ================================================================

    private JScrollPane buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG_PRIMARY);

        // ---- Hàng 1: Stat cards ----
        content.add(buildStatRow());
        content.add(Box.createVerticalStrut(UITheme.PAD_MD));

        // ---- Hàng 2: Biểu đồ + Top sách ----
        JPanel row2 = new JPanel(new GridLayout(1, 2, UITheme.PAD_MD, 0));
        row2.setBackground(UITheme.BG_PRIMARY);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        row2.add(buildChartCard());
        row2.add(buildTopBooksCard());
        content.add(row2);
        content.add(Box.createVerticalStrut(UITheme.PAD_MD));

        // ---- Hàng 3: DS quá hạn ----
        content.add(buildOverdueCard());

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.getViewport().setBackground(UITheme.BG_PRIMARY);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ---- 7 Stat Cards ----
    private JPanel buildStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, UITheme.PAD_SM, 0));
        row.setBackground(UITheme.BG_PRIMARY);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Card 1: Sách
        JPanel card1 = createStatCard("📚 Sách");
        lblTotalBooks = statValue("—", UITheme.ACCENT_PRIMARY);
        lblAvailBooks = statSub("—  còn lại");
        card1.add(lblTotalBooks);
        card1.add(lblAvailBooks);
        row.add(wrapCard(card1, UITheme.ACCENT_PRIMARY));

        // Card 2: Độc giả
        JPanel card2 = createStatCard("👤 Độc Giả");
        lblTotalReaders  = statValue("—", UITheme.COLOR_SUCCESS);
        lblActiveReaders = statSub("—  đang hoạt động");
        card2.add(lblTotalReaders);
        card2.add(lblActiveReaders);
        row.add(wrapCard(card2, UITheme.COLOR_SUCCESS));

        // Card 3: Mượn
        JPanel card3 = createStatCard("📋 Phiếu Mượn");
        lblActiveBorrows = statValue("—", UITheme.COLOR_WARNING);
        lblTotalBorrows  = statSub("—  tổng phiếu");
        card3.add(lblActiveBorrows);
        card3.add(lblTotalBorrows);
        row.add(wrapCard(card3, UITheme.COLOR_WARNING));

        // Card 4: Quá hạn
        JPanel card4 = createStatCard("⚠ Quá Hạn");
        lblOverdueBorrows = statValue("—", UITheme.COLOR_DANGER);
        card4.add(lblOverdueBorrows);
        card4.add(statSub("phiếu chưa trả"));
        row.add(wrapCard(card4, UITheme.COLOR_DANGER));

        return row;
    }

    private JPanel createStatCard(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(UITheme.BG_WHITE);
        JLabel t = new JLabel(title);
        t.setFont(UITheme.FONT_SMALL);
        t.setForeground(UITheme.TEXT_SECONDARY);
        p.add(t);
        return p;
    }

    private JPanel wrapCard(JPanel inner, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.BG_WHITE);
        card.setBorder(UITheme.createCardBorder());

        JPanel colorBar = new JPanel();
        colorBar.setBackground(accentColor);
        colorBar.setPreferredSize(new Dimension(5, 0));
        card.add(colorBar, BorderLayout.WEST);

        inner.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JLabel statValue(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 32));
        lbl.setForeground(color);
        return lbl;
    }

    private JLabel statSub(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);
        return lbl;
    }

    // ---- Biểu đồ cột mượn sách theo tháng ----
    private JPanel buildChartCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.BG_WHITE);
        card.setBorder(UITheme.createCardBorder());

        // Header của card
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(UITheme.BG_WHITE);
        cardHeader.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD));

        JLabel title = new JLabel("📈  Lượt Mượn Theo Tháng — " + chartYear);
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        cardHeader.add(title, BorderLayout.WEST);

        // Nút chọn năm
        JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        yearPanel.setOpaque(false);
        JButton btnPrev = new JButton("◀");
        JButton btnNext = new JButton("▶");
        btnPrev.setFont(UITheme.FONT_SMALL);
        btnNext.setFont(UITheme.FONT_SMALL);
        btnPrev.setFocusPainted(false);
        btnNext.setFocusPainted(false);
        yearPanel.add(btnPrev);
        yearPanel.add(btnNext);
        cardHeader.add(yearPanel, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);

        // Vùng vẽ biểu đồ
        chartPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBarChart((Graphics2D) g, monthlyData);
            }
        };
        chartPanel.setBackground(UITheme.BG_WHITE);
        card.add(chartPanel, BorderLayout.CENTER);

        btnPrev.addActionListener(e -> {
            chartYear--;
            title.setText("📈  Lượt Mượn Theo Tháng — " + chartYear);
            loadChartData();
        });
        btnNext.addActionListener(e -> {
            if (chartYear < Year.now().getValue()) {
                chartYear++;
                title.setText("📈  Lượt Mượn Theo Tháng — " + chartYear);
                loadChartData();
            }
        });

        return card;
    }

    /** Vẽ biểu đồ cột với Graphics2D. */
    private void drawBarChart(Graphics2D g2, int[] data) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = chartPanel.getWidth();
        int h = chartPanel.getHeight();
        int padL = 45, padR = 16, padT = 20, padB = 36;

        if (w <= 0 || h <= 0) return;

        int chartW = w - padL - padR;
        int chartH = h - padT - padB;
        int max    = 1;
        for (int v : data) if (v > max) max = v;

        int barW  = chartW / 12;
        int gap   = (int)(barW * 0.2);
        int barBW = barW - gap * 2;

        String[] months = {"T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"};

        // Đường kẻ ngang
        g2.setStroke(new BasicStroke(1f));
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int y = padT + (int)((double)i / gridLines * chartH);
            g2.setColor(UITheme.BORDER_COLOR);
            g2.drawLine(padL, y, padL + chartW, y);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.setFont(UITheme.FONT_SMALL);
            int val = max - (int)((double)i / gridLines * max);
            g2.drawString(String.valueOf(val), 2, y + 4);
        }

        // Vẽ cột
        for (int i = 0; i < 12; i++) {
            int x    = padL + i * barW + gap;
            int barH = data[i] == 0 ? 0 : (int)((double)data[i] / max * chartH);
            int y    = padT + chartH - barH;

            // Cột gradient
            if (barH > 0) {
                GradientPaint gp = new GradientPaint(
                    x, y, UITheme.ACCENT_PRIMARY,
                    x, padT + chartH, UITheme.ACCENT_LIGHT);
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barBW, barH, 4, 4);
            }

            // Giá trị trên đỉnh cột
            if (data[i] > 0) {
                g2.setColor(UITheme.ACCENT_PRIMARY);
                g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
                String val = String.valueOf(data[i]);
                int tx = x + (barBW - g2.getFontMetrics().stringWidth(val)) / 2;
                g2.drawString(val, tx, y - 3);
            }

            // Nhãn tháng
            g2.setColor(UITheme.TEXT_SECONDARY);
            g2.setFont(UITheme.FONT_SMALL);
            int tx = padL + i * barW + (barW - g2.getFontMetrics().stringWidth(months[i])) / 2;
            g2.drawString(months[i], tx, padT + chartH + 20);
        }
    }

    // ---- Top sách được mượn nhiều nhất ----
    private JPanel buildTopBooksCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.BG_WHITE);
        card.setBorder(UITheme.createCardBorder());

        JLabel title = new JLabel("🏆  Top 10 Sách Được Mượn Nhiều Nhất");
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD));
        card.add(title, BorderLayout.NORTH);

        topBooksModel = new DefaultTableModel(new String[]{"#", "Tên Sách", "Lượt Mượn"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable topTable = new JTable(topBooksModel);
        UITheme.styleTable(topTable);
        topTable.getColumnModel().getColumn(0).setMaxWidth(35);
        topTable.getColumnModel().getColumn(2).setMaxWidth(90);

        var rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        topTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);

        // Renderer cột "Lượt Mượn" với thanh mini
        topTable.getColumnModel().getColumn(2).setCellRenderer(
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                    setHorizontalAlignment(CENTER);
                    setFont(UITheme.FONT_BOLD);
                    if (!sel) setForeground(UITheme.ACCENT_PRIMARY);
                    return this;
                }
            });

        card.add(UITheme.createTableScrollPane(topTable), BorderLayout.CENTER);
        return card;
    }

    // ---- Danh sách quá hạn ----
    private JPanel buildOverdueCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.BG_WHITE);
        card.setBorder(UITheme.createCardBorder());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_WHITE);
        header.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD));

        JLabel title = new JLabel("🚨  Danh Sách Phiếu Mượn Quá Hạn");
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.COLOR_DANGER);
        header.add(title, BorderLayout.WEST);

        JLabel today = new JLabel("Hôm nay: " + LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        today.setFont(UITheme.FONT_SMALL);
        today.setForeground(UITheme.TEXT_MUTED);
        header.add(today, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        overdueModel = new DefaultTableModel(
            new String[]{"#", "Tên Sách", "Độc Giả", "Mã Thẻ", "Hạn Trả", "Số Ngày Quá", "Tiền Phạt"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable overdueTable = new JTable(overdueModel);
        UITheme.styleTable(overdueTable);
        int[] ws = {35, 240, 150, 90, 95, 100, 110};
        for (int i = 0; i < ws.length; i++)
            overdueTable.getColumnModel().getColumn(i).setPreferredWidth(ws[i]);

        // Tô đỏ nhạt toàn dòng
        overdueTable.setDefaultRenderer(Object.class,
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                    setBorder(new EmptyBorder(0, UITheme.PAD_MD, 0, UITheme.PAD_SM));
                    setFont(c == 5 || c == 6 ? UITheme.FONT_BOLD : UITheme.FONT_TABLE);
                    if (sel) {
                        setBackground(UITheme.TABLE_ROW_SELECTED);
                        setForeground(UITheme.TEXT_PRIMARY);
                    } else {
                        setBackground(r % 2 == 0 ? UITheme.COLOR_DANGER_LIGHT : new Color(0xFFF5F5));
                        setForeground(c == 5 || c == 6 ? UITheme.COLOR_DANGER : UITheme.TEXT_PRIMARY);
                    }
                    return this;
                }
            });

        card.add(UITheme.createTableScrollPane(overdueTable), BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  Load dữ liệu
    // ================================================================

    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int totalBooks, availBooks, totalReaders, activeReaders;
            int totalBorrows, activeBorrows, overdueBorrows;
            List<Object[]> topBooks;
            List<Borrow> overdue;

            @Override protected Void doInBackground() throws Exception {
                totalBooks    = bookService.getTotalBooks();
                availBooks    = totalBooks - bookService.getTotalBorrowed();
                totalReaders  = readerService.getTotalReaders();
                activeReaders = readerService.getActiveReaders();
                activeBorrows = borrowService.getActiveBorrowCount();
                overdueBorrows= borrowService.getOverdueBorrowCount();
                totalBorrows  = borrowService.getAllBorrows().size();
                topBooks      = borrowService.getTopBorrowedBooks(10);
                overdue       = borrowService.getOverdueBorrows();
                monthlyData   = borrowService.getBorrowCountByMonth(chartYear);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    // Cập nhật stat cards
                    lblTotalBooks   .setText(String.valueOf(totalBooks));
                    lblAvailBooks   .setText(availBooks + "  còn lại");
                    lblTotalReaders .setText(String.valueOf(totalReaders));
                    lblActiveReaders.setText(activeReaders + "  đang hoạt động");
                    lblActiveBorrows.setText(String.valueOf(activeBorrows));
                    lblTotalBorrows .setText(totalBorrows + "  tổng phiếu");
                    lblOverdueBorrows.setText(String.valueOf(overdueBorrows));

                    // Top sách
                    topBooksModel.setRowCount(0);
                    int idx = 1;
                    for (Object[] row : topBooks) {
                        topBooksModel.addRow(new Object[]{idx++, row[0], row[1]});
                    }

                    // Danh sách quá hạn
                    overdueModel.setRowCount(0);
                    idx = 1;
                    for (Borrow b : overdue) {
                        double fine = borrowService.calculateCurrentFine(b.getDueDate());
                        long days   = 0;
                        try {
                            var due  = java.time.LocalDate.parse(b.getDueDate(),
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                            days = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
                        } catch (Exception ignored) {}
                        overdueModel.addRow(new Object[]{
                            idx++, b.getBookTitle(), b.getReaderName(),
                            b.getReaderCode(), b.getDueDate(),
                            days + " ngày",
                            UITheme.formatCurrency(fine)
                        });
                    }

                    // Repaint biểu đồ
                    if (chartPanel != null) chartPanel.repaint();

                } catch (Exception ex) {
                    UITheme.showError(ReportPanel.this, "Lỗi tải báo cáo:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadChartData() {
        SwingWorker<int[], Void> w = new SwingWorker<>() {
            @Override protected int[] doInBackground() throws Exception {
                return borrowService.getBorrowCountByMonth(chartYear);
            }
            @Override protected void done() {
                try { monthlyData = get(); if (chartPanel != null) chartPanel.repaint(); }
                catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    @Override public void refresh() { loadData(); }
}
