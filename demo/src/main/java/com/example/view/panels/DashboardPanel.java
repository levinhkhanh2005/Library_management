package com.example.view.panels;

import com.example.service.BookService;
import com.example.service.BorrowService;
import com.example.service.ReaderService;
import com.example.view.MainFrame;
import com.example.view.SidebarPanel;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel Tổng Quan — hiển thị thống kê tổng hợp, quick actions, hoạt động gần đây.
 * v2.0 — Animated counters, glassmorphism welcome, quick action grid, recent activity.
 */
public class DashboardPanel extends JPanel implements MainFrame.Refreshable {

    private final BookService   bookService   = new BookService();
    private final ReaderService readerService = new ReaderService();
    private final BorrowService borrowService = new BorrowService();

    private final MainFrame mainFrame;
    private JPanel statsRow;

    public DashboardPanel() {
        this(null);
    }

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, UITheme.PAD_LG));
        setBackground(UITheme.BG_PRIMARY);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        add(UITheme.createPageHeader("🏠  Tổng Quan",
            "Thống kê tổng hợp hệ thống thư viện"), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadData();
    }

    private MainFrame getMainFrame() {
        if (mainFrame != null) return mainFrame;
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof MainFrame mf) return mf;
        return null;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, UITheme.PAD_LG));
        content.setBackground(UITheme.BG_PRIMARY);

        // ---- Top: Stat cards ----
        statsRow = new JPanel(new GridLayout(1, 4, UITheme.PAD_MD, 0));
        statsRow.setBackground(UITheme.BG_PRIMARY);
        statsRow.add(UITheme.createStatCard("Tổng Đầu Sách",  "...", UITheme.ACCENT_PRIMARY));
        statsRow.add(UITheme.createStatCard("Đang Mượn",      "...", UITheme.COLOR_WARNING));
        statsRow.add(UITheme.createStatCard("Độc Giả",        "...", UITheme.COLOR_SUCCESS));
        statsRow.add(UITheme.createStatCard("Quá Hạn",        "...", UITheme.COLOR_DANGER));
        content.add(statsRow, BorderLayout.NORTH);

        // ---- Bottom area: Welcome + Quick Actions side by side ----
        JPanel bottomArea = new JPanel(new GridLayout(1, 2, UITheme.PAD_MD, 0));
        bottomArea.setBackground(UITheme.BG_PRIMARY);
        bottomArea.add(buildWelcomeCard());
        bottomArea.add(buildQuickActions());
        content.add(bottomArea, BorderLayout.CENTER);

        return content;
    }

    // ================================================================
    //  Welcome Card — Glassmorphism style
    // ================================================================

    private JPanel buildWelcomeCard() {
        JPanel card = UITheme.createGlassCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_MD));

        // Top content
        JPanel topContent = new JPanel(new BorderLayout(UITheme.PAD_MD, 0));
        topContent.setOpaque(false);

        JLabel waveIcon = new JLabel("📖");
        waveIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel welcomeTitle = new JLabel("Chào mừng đến Thư Viện Nguyễn Huệ!");
        welcomeTitle.setFont(UITheme.FONT_H2);
        welcomeTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel welcomeSub = new JLabel("Chọn chức năng từ menu bên trái để bắt đầu quản lý.");
        welcomeSub.setFont(UITheme.FONT_BODY);
        welcomeSub.setForeground(UITheme.TEXT_SECONDARY);

        textPanel.add(welcomeTitle);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(welcomeSub);

        topContent.add(waveIcon, BorderLayout.WEST);
        topContent.add(textPanel, BorderLayout.CENTER);

        card.add(topContent, BorderLayout.NORTH);

        // Info badges row
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, UITheme.PAD_SM, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(UITheme.createBadge("Hệ thống đang hoạt động", "success"));
        badgeRow.add(UITheme.createBadge("v2.0", "info"));
        card.add(badgeRow, BorderLayout.CENTER);

        return card;
    }

    // ================================================================
    //  Quick Actions — 4 nút lớn có phản hồi click & điều hướng
    // ================================================================

    private JPanel buildQuickActions() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_MD));
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, UITheme.PAD_LG, UITheme.PAD_LG)
        ));

        // Header thao tác nhanh (dùng VectorIcon tránh lỗi hiển thị font)
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        JLabel boltLbl = new JLabel();
        boltLbl.setIcon(new UITheme.VectorIcon(UITheme.VectorIcon.Type.LIGHTNING, 16, UITheme.COLOR_WARNING));

        JLabel title = new JLabel("Thao Tác Nhanh");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(UITheme.TEXT_PRIMARY);

        titleRow.add(boltLbl);
        titleRow.add(title);
        card.add(titleRow, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, UITheme.PAD_MD, UITheme.PAD_MD));
        grid.setOpaque(false);

        // 1. Thêm Sách
        grid.add(createQuickActionBtn(
            UITheme.VectorIcon.Type.PLUS,
            "Thêm Sách",
            "Nhập sách mới vào kho",
            UITheme.ACCENT_PRIMARY,
            new Color(0x7C3AED),
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.quickActionAddBook();
            }
        ));

        // 2. Mượn Sách
        grid.add(createQuickActionBtn(
            UITheme.VectorIcon.Type.DOCUMENT,
            "Mượn Sách",
            "Lập phiếu mượn bạn đọc",
            UITheme.COLOR_INFO,
            UITheme.ACCENT_SECONDARY,
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.quickActionAddBorrow();
            }
        ));

        // 3. Trả Sách
        grid.add(createQuickActionBtn(
            UITheme.VectorIcon.Type.CHECK,
            "Trả Sách",
            "Ghi nhận trả & tính phạt",
            UITheme.COLOR_SUCCESS,
            new Color(0x059669),
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.showPanel(SidebarPanel.MenuItem.BORROWS);
            }
        ));

        // 4. Báo Cáo
        grid.add(createQuickActionBtn(
            UITheme.VectorIcon.Type.STATS,
            "Báo Cáo",
            "Thống kê & xuất dữ liệu",
            UITheme.COLOR_WARNING,
            new Color(0xEA580C),
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.showPanel(SidebarPanel.MenuItem.REPORT);
            }
        ));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createQuickActionBtn(
            UITheme.VectorIcon.Type iconType,
            String label,
            String subtitle,
            Color gradFrom,
            Color gradTo,
            Runnable onClickAction) {

        JPanel btn = new JPanel(new BorderLayout(14, 0)) {
            private boolean hovered = false;
            private boolean pressed = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; pressed = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                    @Override public void mouseReleased(MouseEvent e) {
                        if (pressed && contains(e.getPoint()) && onClickAction != null) {
                            pressed = false;
                            repaint();
                            onClickAction.run();
                        } else {
                            pressed = false;
                            repaint();
                        }
                    }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                int arc = 14;

                // Gradient background
                g2.setPaint(new GradientPaint(0, 0, gradFrom, w, h, gradTo));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Subtle inner border
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // Hover overlay
                if (hovered && !pressed) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                } else if (pressed) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
                    g2.setColor(Color.BLACK);
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(14, 18, 14, 18));
        btn.setPreferredSize(new Dimension(160, 75));

        // Icon badge on WEST
        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(42, 42));
        JLabel iconLbl = new JLabel();
        iconLbl.setIcon(new UITheme.VectorIcon(iconType, 20, Color.WHITE));
        iconBadge.add(iconLbl);
        btn.add(iconBadge, BorderLayout.WEST);

        // Text in CENTER
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(label);
        lblTitle.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 11));
        lblSub.setForeground(new Color(255, 255, 255, 210));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(lblSub);
        textPanel.add(Box.createVerticalGlue());

        btn.add(textPanel, BorderLayout.CENTER);

        // Arrow on EAST
        JLabel arrowLbl = new JLabel("→");
        arrowLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 16));
        arrowLbl.setForeground(new Color(255, 255, 255, 160));
        btn.add(arrowLbl, BorderLayout.EAST);

        return btn;
    }

    // ================================================================
    //  Load Data — với animated counters
    // ================================================================

    private void loadData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override protected int[] doInBackground() throws Exception {
                return new int[]{
                    bookService.getTotalBooks(),
                    bookService.getTotalBorrowed(),
                    readerService.getTotalReaders(),
                    borrowService.getOverdueBorrowCount()
                };
            }
            @Override protected void done() {
                try {
                    int[] data = get();
                    String[] titles = {"Tổng Đầu Sách","Đang Mượn","Độc Giả","Quá Hạn"};
                    Color[] colors = {
                        UITheme.ACCENT_PRIMARY, UITheme.COLOR_WARNING,
                        UITheme.COLOR_SUCCESS,  UITheme.COLOR_DANGER
                    };

                    statsRow.removeAll();
                    for (int i = 0; i < 4; i++) {
                        JPanel card = UITheme.createStatCard(titles[i], "0", colors[i]);
                        statsRow.add(card);

                        // Find the value label and animate it
                        final int targetValue = data[i];
                        SwingUtilities.invokeLater(() -> {
                            JLabel valueLbl = findValueLabel(card);
                            if (valueLbl != null) {
                                UITheme.animateValue(valueLbl, targetValue, 800);
                            }
                        });
                    }
                    statsRow.revalidate();
                    statsRow.repaint();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    /** Tìm JLabel giá trị lớn trong stat card. */
    private JLabel findValueLabel(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel lbl) {
                if (lbl.getFont() != null && lbl.getFont().getSize() >= 28) {
                    return lbl;
                }
            }
            if (c instanceof Container sub) {
                JLabel found = findValueLabel(sub);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Override public void refresh() { loadData(); }
}
