package com.example.view.panels;

import com.example.dao.LoginLogDAO;
import com.example.service.AuthService;
import com.example.view.MainFrame;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Panel Hoạt Động — thống kê lịch sử đăng nhập/đăng xuất.
 * Chỉ hiển thị đầy đủ với Admin. Thủ thư thấy thông báo hạn chế.
 */
public class ActivityPanel extends JPanel implements MainFrame.Refreshable {

    private final LoginLogDAO logDAO = new LoginLogDAO();

    // Stat card labels (cập nhật sau khi load xong)
    private JLabel lblToday, lblWeek, lblDistinct, lblAverage;

    // Biểu đồ 7 ngày
    private WeeklyBarChart barChart;

    // Bảng log
    private DefaultTableModel logModel;

    public ActivityPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_LG));
        setBackground(UITheme.BG_PRIMARY);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_PRIMARY);
        header.add(UITheme.createPageHeader("🕒  Hoạt Động Ra Vào & Đăng Nhập",
            "Kiểm soát lịch sử đăng nhập/đăng xuất hệ thống — thống kê 7 ngày gần nhất"), BorderLayout.WEST);

        if (AuthService.isAdmin()) {
            JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            btnWrapper.setOpaque(false);
            JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");
            btnRefresh.addActionListener(e -> loadData());
            btnWrapper.add(btnRefresh);
            header.add(btnWrapper, BorderLayout.EAST);
        }

        add(header, BorderLayout.NORTH);

        if (!AuthService.isAdmin()) {
            add(buildAccessDenied(), BorderLayout.CENTER);
        } else {
            add(buildContent(), BorderLayout.CENTER);
            loadData();
        }
    }

    // ================================================================
    //  Thông báo từ chối quyền truy cập (thủ thư)
    // ================================================================

    private JPanel buildAccessDenied() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());
        JLabel msg = new JLabel(
            "<html><center><br><br>🔒<br><br>" +
            "<b style='font-size:16px'>Không có quyền truy cập</b><br><br>" +
            "<span style='color:#64748B'>Chức năng này chỉ dành cho Quản trị viên.</span>" +
            "</center></html>",
            SwingConstants.CENTER
        );
        msg.setFont(UITheme.FONT_BODY);
        card.add(msg, BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  Layout chính
    // ================================================================

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        content.setBackground(UITheme.BG_PRIMARY);

        // Hàng trên: Stat cards
        content.add(buildStatCards(), BorderLayout.NORTH);

        // Phần giữa: biểu đồ (trái) + bảng (phải)
        JPanel middle = new JPanel(new GridLayout(1, 2, UITheme.PAD_MD, 0));
        middle.setBackground(UITheme.BG_PRIMARY);
        middle.add(buildChartCard());
        middle.add(buildLogTableCard());
        content.add(middle, BorderLayout.CENTER);

        return content;
    }

    // ================================================================
    //  Stat Cards
    // ================================================================

    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 4, UITheme.PAD_MD, 0));
        row.setBackground(UITheme.BG_PRIMARY);

        lblToday    = addStatCard(row, "Đăng Nhập Hôm Nay",   "...", UITheme.ACCENT_PRIMARY);
        lblWeek     = addStatCard(row, "Đăng Nhập Tuần Này",  "...", UITheme.COLOR_INFO);
        lblDistinct = addStatCard(row, "Người Dùng (7 ngày)", "...", UITheme.COLOR_SUCCESS);
        lblAverage  = addStatCard(row, "Trung Bình / Ngày",   "...", UITheme.COLOR_WARNING);

        return row;
    }

    /** Tạo stat card và trả về JLabel giá trị để cập nhật sau. */
    private JLabel addStatCard(JPanel parent, String title, String value, Color color) {
        // Tạo card tùy chỉnh để lấy label giá trị
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), UITheme.CARD_ARC * 2, UITheme.CARD_ARC * 2);
                // Thanh màu bên trái
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 6, getHeight(), 6, 6);
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD + 8, UITheme.PAD_MD, UITheme.PAD_MD));
        card.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 28));
        valueLbl.setForeground(color);

        JPanel inner = new JPanel(new GridLayout(2, 1, 0, 4));
        inner.setOpaque(false);
        inner.add(titleLbl);
        inner.add(valueLbl);
        card.add(inner, BorderLayout.CENTER);

        parent.add(card);
        return valueLbl;
    }

    // ================================================================
    //  Biểu đồ cột 7 ngày
    // ================================================================

    private JPanel buildChartCard() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        card.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));

        JLabel title = new JLabel("📈  Lượt Đăng Nhập 7 Ngày Gần Nhất");
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        barChart = new WeeklyBarChart();
        card.add(barChart, BorderLayout.CENTER);

        return card;
    }

    // ================================================================
    //  Bảng lịch sử log
    // ================================================================

    private JPanel buildLogTableCard() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        card.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));

        JLabel title = new JLabel("🕐  Lịch Sử Hoạt Động Gần Nhất");
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        logModel = new DefaultTableModel(
            new String[]{"Thời Gian", "Người Dùng", "Họ Tên", "Vai Trò", "Hành Động"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(logModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(145);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);

        // Tô màu cột Hành Động (LOGIN / LOGOUT)
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                p.setOpaque(true);
                p.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                    : (r % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));
                String action = val != null ? val.toString() : "";
                String type   = action.equals("LOGIN") ? "success" : "danger";
                String label  = action.equals("LOGIN") ? "▲ Vào" : "▼ Ra";
                p.add(UITheme.createBadge(label, type));
                return p;
            }
        });

        card.add(UITheme.createTableScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  Load dữ liệu
    // ================================================================

    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            int today, week, distinct;
            double avg;
            Map<String, Integer> weeklyData;
            List<Object[]> recentLogs;

            @Override protected Void doInBackground() {
                today       = logDAO.countLoginsToday();
                week        = logDAO.countLoginsLastWeek();
                distinct    = logDAO.countDistinctUsersLastWeek();
                avg         = logDAO.getAverageLoginsPerDay();
                weeklyData  = logDAO.getWeeklyLoginByDay();
                recentLogs  = logDAO.getRecentLogs(100);
                return null;
            }

            @Override protected void done() {
                try {
                    get(); // re-throw exception nếu có
                    // Cập nhật stat cards
                    lblToday   .setText(String.valueOf(today));
                    lblWeek    .setText(String.valueOf(week));
                    lblDistinct.setText(String.valueOf(distinct));
                    lblAverage .setText(String.format("%.1f", avg));

                    // Cập nhật biểu đồ
                    barChart.setData(weeklyData);

                    // Cập nhật bảng log
                    logModel.setRowCount(0);
                    DateTimeFormatter inFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");
                    for (Object[] row : recentLogs) {
                        String timeStr = row[5] != null ? row[5].toString() : "";
                        // Format lại thời gian cho đẹp
                        try {
                            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(timeStr, inFmt);
                            timeStr = dt.format(outFmt);
                        } catch (Exception ignored) {}
                        logModel.addRow(new Object[]{
                            timeStr, row[1], row[2], formatRole(row[3].toString()), row[4]
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[ActivityPanel] Lỗi load: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String formatRole(String roleCode) {
        return switch (roleCode) {
            case "ADMIN"     -> "Quản trị viên";
            case "LIBRARIAN" -> "Thủ thư";
            default          -> roleCode;
        };
    }

    @Override public void refresh() {
        if (AuthService.isAdmin()) loadData();
    }

    // ================================================================
    //  Inner class: Biểu đồ cột tự vẽ (Java2D)
    // ================================================================

    private static class WeeklyBarChart extends JPanel {

        private Map<String, Integer> data;
        private static final Color BAR_COLOR       = new Color(0x4F46E5);
        private static final Color BAR_HOVER_COLOR = new Color(0x4338CA);
        private static final Color GRID_COLOR      = new Color(0xE2E8F0);
        private static final Color TEXT_COLOR      = new Color(0x64748B);

        WeeklyBarChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 220));
        }

        void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padL = 36, padR = 12, padT = 16, padB = 40;
            int chartW = w - padL - padR;
            int chartH = h - padT - padB;

            int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            if (maxVal == 0) maxVal = 1;

            int n        = data.size();
            int barW     = (chartW / n) - 10;
            int barGap   = (chartW - barW * n) / (n + 1);

            // Vẽ đường grid ngang
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
            int gridLines = 4;
            for (int i = 0; i <= gridLines; i++) {
                int y = padT + (chartH * i / gridLines);
                g2.drawLine(padL, y, padL + chartW, y);
                // Nhãn trục Y
                int gridVal = maxVal - (maxVal * i / gridLines);
                g2.setColor(TEXT_COLOR);
                g2.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
                g2.drawString(String.valueOf(gridVal), 2, y + 4);
                g2.setColor(GRID_COLOR);
            }
            g2.setStroke(new BasicStroke(1f));

            // Vẽ cột
            int idx = 0;
            DateTimeFormatter inFmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE\ndd/MM",
                    new java.util.Locale("vi", "VN"));

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                int val  = entry.getValue();
                int barH = (int) ((double) val / maxVal * chartH);
                int x    = padL + barGap + idx * (barW + barGap);
                int y    = padT + chartH - barH;

                // Cột với bo góc trên
                g2.setColor(BAR_COLOR);
                if (barH > 0) {
                    int arc = Math.min(8, barW / 2);
                    g2.fillRoundRect(x, y, barW, barH, arc, arc);
                    // Phần dưới vuông
                    if (barH > arc) {
                        g2.fillRect(x, y + arc, barW, barH - arc);
                    }
                }

                // Giá trị trên đầu cột
                if (val > 0) {
                    g2.setColor(UITheme.TEXT_PRIMARY);
                    g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
                    FontMetrics fm = g2.getFontMetrics();
                    String valStr = String.valueOf(val);
                    int vx = x + (barW - fm.stringWidth(valStr)) / 2;
                    g2.drawString(valStr, vx, y - 4);
                }

                // Nhãn trục X (ngày trong tuần + ngày/tháng)
                try {
                    LocalDate d = LocalDate.parse(entry.getKey(), inFmt);
                    String[] parts = dayFmt.format(d).split("\n");
                    g2.setColor(TEXT_COLOR);
                    g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    int xCenter = x + barW / 2;
                    g2.drawString(parts[0], xCenter - fm.stringWidth(parts[0]) / 2,
                            padT + chartH + 16);
                    g2.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
                    fm = g2.getFontMetrics();
                    g2.drawString(parts.length > 1 ? parts[1] : "",
                            xCenter - fm.stringWidth(parts.length > 1 ? parts[1] : "") / 2,
                            padT + chartH + 28);
                } catch (Exception ignored) {}

                idx++;
            }

            g2.dispose();
        }
    }
}
