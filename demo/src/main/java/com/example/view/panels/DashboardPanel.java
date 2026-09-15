package com.example.view.panels;

import com.example.model.Borrow;
import com.example.service.BookService;
import com.example.service.BorrowService;
import com.example.service.ReaderService;
import com.example.view.MainFrame;
import com.example.view.SidebarPanel;
import com.example.view.UITheme;
import com.example.view.dialogs.BorrowDetailDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Panel Tổng Quan — Executive Library Dashboard.
 * v2.5 — Animated Stat Cards, Compact Welcome Banner, Recent Borrow/Return Activity Table,
 * Quick Action Grid, and Top Borrowed Books with visual progress ranking.
 */
public class DashboardPanel extends JPanel implements MainFrame.Refreshable {

    private final BookService   bookService   = new BookService();
    private final ReaderService readerService = new ReaderService();
    private final BorrowService borrowService = new BorrowService();

    private final MainFrame mainFrame;
    private JPanel statsRow;

    // Recent activity table
    private DefaultTableModel recentModel;
    private JTable recentTable;
    private JPanel recentCardBody;
    private CardLayout recentCardLayout;
    private JLabel lblRecentCount;

    // Top books list
    private JPanel topBooksContainer;

    // Greeting labels
    private JLabel lblGreetingTitle;
    private JLabel lblGreetingSub;

    private static final String[] RECENT_COLUMNS = {
        "#", "Mã Phiếu", "Tên Sách", "Độc Giả", "Ngày Mượn", "Hạn Trả", "Trạng Thái"
    };

    public DashboardPanel() {
        this(null);
    }

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.BG_PRIMARY);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildScrollableContent(), BorderLayout.CENTER);

        loadData();
    }

    private MainFrame getMainFrame() {
        if (mainFrame != null) return mainFrame;
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof MainFrame mf) return mf;
        return null;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_PRIMARY);
        header.setBorder(new EmptyBorder(0, 0, UITheme.PAD_MD, 0));
        header.add(UITheme.createPageHeader("🏠  Tổng Quan",
            "Bảng điều khiển trung tâm & thống kê hoạt động thư viện"), BorderLayout.WEST);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);

        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");
        btnRefresh.addActionListener(e -> loadData());
        rightTools.add(btnRefresh);

        header.add(rightTools, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildScrollableContent() {
        JPanel content = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        content.setBackground(UITheme.BG_PRIMARY);

        // 1. Top: 4 Stat Cards
        statsRow = buildStatCardsRow();
        content.add(statsRow, BorderLayout.NORTH);

        // 2. Main Area: 2 Columns (Left: flexible, Right: fixed 440px)
        JPanel mainArea = new JPanel(new BorderLayout(UITheme.PAD_MD, 0));
        mainArea.setBackground(UITheme.BG_PRIMARY);

        mainArea.add(buildLeftColumn(),  BorderLayout.CENTER);
        mainArea.add(buildRightColumn(), BorderLayout.EAST);

        content.add(mainArea, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    // ================================================================
    //  Stat Cards Row (4 cards)
    // ================================================================

    private JPanel buildStatCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, UITheme.PAD_MD, 0));
        row.setBackground(UITheme.BG_PRIMARY);

        row.add(createClickableStatCard("Tổng Đầu Sách", "...", UITheme.ACCENT_PRIMARY,
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.showPanel(SidebarPanel.MenuItem.BOOKS);
            }));

        row.add(createClickableStatCard("Đang Mượn", "...", UITheme.COLOR_WARNING,
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.showPanel(SidebarPanel.MenuItem.BORROWS);
            }));

        row.add(createClickableStatCard("Độc Giả", "...", UITheme.COLOR_SUCCESS,
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.showPanel(SidebarPanel.MenuItem.READERS);
            }));

        row.add(createClickableStatCard("Quá Hạn", "...", UITheme.COLOR_DANGER,
            () -> {
                MainFrame mf = getMainFrame();
                if (mf != null) mf.showPanel(SidebarPanel.MenuItem.BORROWS);
            }));

        return row;
    }

    private JPanel createClickableStatCard(String title, String initialVal, Color accentColor, Runnable onClick) {
        JPanel card = UITheme.createStatCard(title, initialVal, accentColor);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Nhấp để chuyển đến danh mục " + title);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
        return card;
    }

    // ================================================================
    //  Left Column: Banner + Recent Borrows & Returns Table
    // ================================================================

    private JPanel buildLeftColumn() {
        JPanel left = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        left.setBackground(UITheme.BG_PRIMARY);

        left.add(buildCompactWelcomeBanner(), BorderLayout.NORTH);
        left.add(buildRecentActivityCard(),   BorderLayout.CENTER);

        return left;
    }

    private JPanel buildCompactWelcomeBanner() {
        JPanel card = UITheme.createGlassCard();
        card.setLayout(new BorderLayout(UITheme.PAD_MD, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(12, 16, 12, 16)
        ));

        // Left: Emoji + Texts
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftGroup.setOpaque(false);

        JLabel iconLbl = new JLabel("👋");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        leftGroup.add(iconLbl);

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        lblGreetingTitle = new JLabel(getGreetingTime());
        lblGreetingTitle.setFont(UITheme.FONT_H3);
        lblGreetingTitle.setForeground(UITheme.TEXT_PRIMARY);

        lblGreetingSub = new JLabel("Hôm nay là " + getFormattedToday() + ". Chúc bạn một ngày làm việc hiệu quả!");
        lblGreetingSub.setFont(UITheme.FONT_SMALL);
        lblGreetingSub.setForeground(UITheme.TEXT_SECONDARY);

        textGroup.add(lblGreetingTitle);
        textGroup.add(Box.createVerticalStrut(2));
        textGroup.add(lblGreetingSub);
        leftGroup.add(textGroup);

        card.add(leftGroup, BorderLayout.CENTER);

        // Right: Status Badges
        JPanel rightBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        rightBadges.setOpaque(false);
        rightBadges.add(UITheme.createBadge("🟢 Hệ thống sẵn sàng", "success"));
        rightBadges.add(UITheme.createBadge("v2.5", "info"));
        card.add(rightBadges, BorderLayout.EAST);

        return card;
    }

    private String getGreetingTime() {
        int hour = LocalTime.now().getHour();
        if (hour >= 5 && hour < 12) {
            return "Chào buổi sáng, Quản trị viên!";
        } else if (hour >= 12 && hour < 18) {
            return "Chào buổi chiều, Quản trị viên!";
        } else {
            return "Chào buổi tối, Quản trị viên!";
        }
    }

    private String getFormattedToday() {
        try {
            LocalDate today = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
            return today.format(fmt);
        } catch (Exception e) {
            return "hôm nay";
        }
    }

    private JPanel buildRecentActivityCard() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(14, 16, 14, 16)
        ));

        // Header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JPanel hRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hRow.setOpaque(false);
        JLabel titleLbl = new JLabel("📋  Hoạt Động Mượn / Trả Gần Đây");
        titleLbl.setFont(UITheme.FONT_H3);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        lblRecentCount = UITheme.createBadge("0 giao dịch", "secondary");
        hRow.add(titleLbl);
        hRow.add(lblRecentCount);

        JLabel subLbl = new JLabel("Danh sách các giao dịch mượn và trả sách mới nhất trong hệ thống");
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(hRow);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subLbl);
        cardHeader.add(titlePanel, BorderLayout.WEST);

        JButton btnViewAll = UITheme.createSecondaryButton("Xem tất cả →");
        btnViewAll.setFont(UITheme.FONT_SMALL);
        btnViewAll.setPreferredSize(new Dimension(110, 30));
        btnViewAll.addActionListener(e -> {
            MainFrame mf = getMainFrame();
            if (mf != null) mf.showPanel(SidebarPanel.MenuItem.BORROWS);
        });
        cardHeader.add(btnViewAll, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);

        // Body: CardLayout ("table" or "empty")
        recentCardLayout = new CardLayout();
        recentCardBody   = new JPanel(recentCardLayout);
        recentCardBody.setOpaque(false);

        // 1. Table View
        recentModel = new DefaultTableModel(RECENT_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        recentTable = new JTable(recentModel);
        UITheme.styleTable(recentTable);
        recentTable.setRowHeight(36);

        // Column widths & Alignments
        recentTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        recentTable.getColumnModel().getColumn(0).setMaxWidth(45);
        recentTable.getColumnModel().getColumn(1).setPreferredWidth(65);
        recentTable.getColumnModel().getColumn(1).setMaxWidth(80);
        recentTable.getColumnModel().getColumn(2).setPreferredWidth(190);
        recentTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        recentTable.getColumnModel().getColumn(4).setPreferredWidth(85);
        recentTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        recentTable.getColumnModel().getColumn(6).setPreferredWidth(95);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        recentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        recentTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        recentTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        recentTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        // Status Badge Renderer
        recentTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                wrapper.setOpaque(true);
                wrapper.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                    : (row % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));

                String s = val != null ? val.toString() : "";
                JLabel badge;
                if      (s.equals(Borrow.Status.RETURNED.getLabel())) badge = UITheme.createBadge(s, "success");
                else if (s.equals(Borrow.Status.OVERDUE.getLabel()))  badge = UITheme.createBadge(s, "danger");
                else if (s.equals(Borrow.Status.LOST.getLabel()))     badge = UITheme.createBadge(s, "warning");
                else                                                  badge = UITheme.createBadge(s, "info");

                wrapper.add(badge);
                return wrapper;
            }
        });

        // Double click to open BorrowDetailDialog
        recentTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && recentTable.getSelectedRow() >= 0) {
                    int modelRow = recentTable.convertRowIndexToModel(recentTable.getSelectedRow());
                    Object idVal = recentModel.getValueAt(modelRow, 1);
                    if (idVal != null) {
                        try {
                            int borrowId = Integer.parseInt(idVal.toString());
                            BorrowDetailDialog dlg = new BorrowDetailDialog(
                                SwingUtilities.getWindowAncestor(DashboardPanel.this), borrowId);
                            dlg.setVisible(true);
                            if (dlg.isChanged()) {
                                loadData();
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        });

        JScrollPane tableScroll = UITheme.createTableScrollPane(recentTable);
        tableScroll.setPreferredSize(new Dimension(0, 245));
        recentCardBody.add(tableScroll, "table");

        // 2. Empty View
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(new EmptyBorder(30, 20, 30, 20));

        JLabel emptyIcon = new JLabel("📖");
        emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyMsg = new JLabel("Chưa có giao dịch mượn trả nào gần đây.");
        emptyMsg.setFont(UITheme.FONT_BOLD);
        emptyMsg.setForeground(UITheme.TEXT_SECONDARY);
        emptyMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptySub = new JLabel("Bấm nút 'Mượn Sách' ở bảng bên phải để tạo phiếu mượn đầu tiên.");
        emptySub.setFont(UITheme.FONT_SMALL);
        emptySub.setForeground(UITheme.TEXT_MUTED);
        emptySub.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(emptyIcon);
        emptyPanel.add(Box.createVerticalStrut(8));
        emptyPanel.add(emptyMsg);
        emptyPanel.add(Box.createVerticalStrut(4));
        emptyPanel.add(emptySub);

        recentCardBody.add(emptyPanel, "empty");

        card.add(recentCardBody, BorderLayout.CENTER);

        // Footer hint
        JLabel footerHint = new JLabel("💡 Nhấp đúp chuột vào bất kỳ dòng nào để xem chi tiết và xử lý phiếu mượn");
        footerHint.setFont(UITheme.FONT_SMALL);
        footerHint.setForeground(UITheme.TEXT_MUTED);
        card.add(footerHint, BorderLayout.SOUTH);

        return card;
    }

    // ================================================================
    //  Right Column: Quick Actions + Top Borrowed Books
    // ================================================================

    private JPanel buildRightColumn() {
        JPanel right = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        right.setBackground(UITheme.BG_PRIMARY);
        right.setPreferredSize(new Dimension(440, 0));

        right.add(buildQuickActions(),  BorderLayout.NORTH);
        right.add(buildTopBooksCard(),  BorderLayout.CENTER);

        return right;
    }

    private JPanel buildQuickActions() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(14, 16, 14, 16)
        ));

        // Title row
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

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
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

        JPanel btn = new JPanel(new BorderLayout(10, 0)) {
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
                int arc = 12;

                // Gradient background
                g2.setPaint(new GradientPaint(0, 0, gradFrom, w, h, gradTo));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                // Inner subtle outline
                g2.setColor(new Color(255, 255, 255, 35));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // Hover / Pressed overlay
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
        btn.setBorder(new EmptyBorder(10, 14, 10, 14));
        btn.setPreferredSize(new Dimension(160, 68));

        // Icon badge
        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(36, 36));
        JLabel iconLbl = new JLabel();
        iconLbl.setIcon(new UITheme.VectorIcon(iconType, 18, Color.WHITE));
        iconBadge.add(iconLbl);
        btn.add(iconBadge, BorderLayout.WEST);

        // Text
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(label);
        lblTitle.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 13));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
        lblSub.setForeground(new Color(255, 255, 255, 210));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(lblSub);
        textPanel.add(Box.createVerticalGlue());
        btn.add(textPanel, BorderLayout.CENTER);

        // Small arrow on EAST
        JLabel arrowLbl = new JLabel("→");
        arrowLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        arrowLbl.setForeground(new Color(255, 255, 255, 160));
        btn.add(arrowLbl, BorderLayout.EAST);

        return btn;
    }

    private JPanel buildTopBooksCard() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        card.setBorder(BorderFactory.createCompoundBorder(
            card.getBorder(),
            new EmptyBorder(14, 16, 14, 16)
        ));

        // Header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel titleLbl = new JLabel("🔥  Sách Mượn Nhiều Nhất");
        titleLbl.setFont(UITheme.FONT_H3);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Bảng xếp hạng sách được yêu thích nhất");
        subLbl.setFont(UITheme.FONT_SMALL);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        titlePanel.add(titleLbl);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subLbl);
        cardHeader.add(titlePanel, BorderLayout.WEST);

        JButton btnDetail = UITheme.createSecondaryButton("Báo cáo →");
        btnDetail.setFont(UITheme.FONT_SMALL);
        btnDetail.setPreferredSize(new Dimension(90, 28));
        btnDetail.addActionListener(e -> {
            MainFrame mf = getMainFrame();
            if (mf != null) mf.showPanel(SidebarPanel.MenuItem.REPORT);
        });
        cardHeader.add(btnDetail, BorderLayout.EAST);

        card.add(cardHeader, BorderLayout.NORTH);

        // List container
        topBooksContainer = new JPanel();
        topBooksContainer.setLayout(new BoxLayout(topBooksContainer, BoxLayout.Y_AXIS));
        topBooksContainer.setOpaque(false);

        card.add(topBooksContainer, BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    //  Load Data — SwingWorker
    // ================================================================

    private static class DashboardData {
        int totalBooks;
        int totalBorrowed;
        int totalReaders;
        int overdueBorrows;
        List<Borrow> recentBorrows;
        List<Object[]> topBooks;
    }

    private void loadData() {
        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                DashboardData data = new DashboardData();
                data.totalBooks     = bookService.getTotalBooks();
                data.totalBorrowed  = bookService.getTotalBorrowed();
                data.totalReaders   = readerService.getTotalReaders();
                data.overdueBorrows = borrowService.getOverdueBorrowCount();
                data.recentBorrows  = borrowService.getRecentBorrows(6);
                data.topBooks       = borrowService.getTopBorrowedBooks(4);
                return data;
            }

            @Override
            protected void done() {
                try {
                    DashboardData data = get();

                    // 1. Animate Stat Cards
                    updateStatCards(data);

                    // 2. Populate Recent Activity Table
                    updateRecentTable(data.recentBorrows);

                    // 3. Populate Top Books List
                    updateTopBooks(data.topBooks);

                    // 4. Update Greeting Subtitle with quick count
                    if (lblGreetingSub != null) {
                        lblGreetingSub.setText("Hôm nay là " + getFormattedToday()
                            + " • Thư viện đang lưu hành " + data.totalBorrowed + " cuốn sách"
                            + (data.overdueBorrows > 0 ? " (⚠ " + data.overdueBorrows + " quá hạn)" : "") + ".");
                    }

                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void updateStatCards(DashboardData data) {
        if (statsRow == null) return;
        int[] values = {
            data.totalBooks,
            data.totalBorrowed,
            data.totalReaders,
            data.overdueBorrows
        };

        Component[] cards = statsRow.getComponents();
        for (int i = 0; i < cards.length && i < values.length; i++) {
            if (cards[i] instanceof Container container) {
                JLabel valLbl = findValueLabel(container);
                if (valLbl != null) {
                    UITheme.animateValue(valLbl, values[i], 750);
                }
            }
        }
    }

    private void updateRecentTable(List<Borrow> borrows) {
        recentModel.setRowCount(0);

        if (borrows == null || borrows.isEmpty()) {
            recentCardLayout.show(recentCardBody, "empty");
            lblRecentCount.setText("0 giao dịch");
            return;
        }

        recentCardLayout.show(recentCardBody, "table");
        lblRecentCount.setText(borrows.size() + " mới nhất");

        int idx = 1;
        for (Borrow b : borrows) {
            recentModel.addRow(new Object[]{
                idx++,
                b.getId(),
                b.getBookTitle(),
                b.getReaderName(),
                b.getBorrowDate(),
                b.getDueDate(),
                b.getStatus().getLabel()
            });
        }
    }

    private void updateTopBooks(List<Object[]> topBooks) {
        topBooksContainer.removeAll();

        if (topBooks == null || topBooks.isEmpty()) {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setBorder(new EmptyBorder(20, 10, 20, 10));
            JLabel empty = new JLabel("Chưa có đủ số liệu thống kê mượn sách.");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setForeground(UITheme.TEXT_MUTED);
            p.add(empty);
            topBooksContainer.add(p);
            topBooksContainer.revalidate();
            topBooksContainer.repaint();
            return;
        }

        int maxCount = 1;
        for (Object[] row : topBooks) {
            int cnt = ((Number) row[1]).intValue();
            if (cnt > maxCount) maxCount = cnt;
        }

        String[] medals = {"🥇", "🥈", "🥉", "4.", "5."};
        Color[] medalColors = {
            new Color(0xD97706), // Amber gold
            new Color(0x64748B), // Slate silver
            new Color(0xB45309), // Bronze
            UITheme.TEXT_MUTED,
            UITheme.TEXT_MUTED
        };

        for (int i = 0; i < topBooks.size(); i++) {
            Object[] row = topBooks.get(i);
            String title = (String) row[0];
            int count = ((Number) row[1]).intValue();

            JPanel itemRow = new JPanel(new BorderLayout(8, 0));
            itemRow.setOpaque(false);
            itemRow.setBorder(new EmptyBorder(6, 0, 6, 0));

            // Rank Badge
            JLabel rankLbl = new JLabel(i < medals.length ? medals[i] : (i + 1) + ".");
            rankLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 13));
            rankLbl.setForeground(i < medalColors.length ? medalColors[i] : UITheme.TEXT_MUTED);
            rankLbl.setPreferredSize(new Dimension(28, 20));
            itemRow.add(rankLbl, BorderLayout.WEST);

            // Center: Title + Progress bar
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
            lblTitle.setForeground(UITheme.TEXT_PRIMARY);
            lblTitle.setToolTipText(title);

            // Visual sleek progress bar
            final float ratio = (float) count / maxCount;
            JPanel progressBar = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();

                    // Track
                    g2.setColor(new Color(0xE2E8F0));
                    g2.fillRoundRect(0, 0, w, h, h, h);

                    // Fill
                    int fillW = Math.max(8, (int) (w * ratio));
                    g2.setPaint(new GradientPaint(0, 0, UITheme.ACCENT_PRIMARY, fillW, 0, new Color(0x38BDF8)));
                    g2.fillRoundRect(0, 0, fillW, h, h, h);
                    g2.dispose();
                }
            };
            progressBar.setOpaque(false);
            progressBar.setPreferredSize(new Dimension(0, 6));
            progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));

            center.add(lblTitle);
            center.add(Box.createVerticalStrut(4));
            center.add(progressBar);
            itemRow.add(center, BorderLayout.CENTER);

            // Right: Count badge
            JLabel countLbl = new JLabel(count + " lượt");
            countLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
            countLbl.setForeground(UITheme.ACCENT_PRIMARY);
            countLbl.setBorder(new EmptyBorder(0, 6, 0, 0));
            itemRow.add(countLbl, BorderLayout.EAST);

            topBooksContainer.add(itemRow);

            if (i < topBooks.size() - 1) {
                topBooksContainer.add(Box.createVerticalStrut(2));
            }
        }

        topBooksContainer.revalidate();
        topBooksContainer.repaint();
    }

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

    @Override
    public void refresh() {
        loadData();
    }
}
