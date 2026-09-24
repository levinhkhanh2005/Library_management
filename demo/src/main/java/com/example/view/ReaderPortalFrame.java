package com.example.view;

import com.example.dao.BorrowDAO;
import com.example.dao.BookDAO;
import com.example.dao.CategoryDAO;
import com.example.dao.ReaderDAO;
import com.example.model.Book;
import com.example.model.Borrow;
import com.example.model.Category;
import com.example.model.Reader;
import com.example.model.User;
import com.example.ReaderApp;
import com.example.service.AuthService;
import com.example.service.BorrowService;
import com.example.service.ReaderService;
import com.example.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Cổng thông tin dành riêng cho Độc giả (role = READER).
 * Gồm 4 tab: Tra cứu sách, Sách đang mượn, Thông tin cá nhân, Đổi mật khẩu.
 *
 * v3.0 — Nâng cấp toàn diện:
 * - Welcome banner với thông tin tổng quan
 * - Book detail dialog khi double-click
 * - Lọc sách theo thể loại
 * - Timeline mượn sách với progress bar
 * - Chỉnh sửa thông tin cá nhân
 * - Đổi mật khẩu
 * - Empty state minh họa
 * - Notification badge cho sách quá hạn
 */
public class ReaderPortalFrame extends JFrame {

    private final User currentUser;
    private Reader currentReader;

    // DAOs & Services
    private final BookDAO       bookDAO       = new BookDAO();
    private final BorrowDAO     borrowDAO     = new BorrowDAO();
    private final ReaderDAO     readerDAO     = new ReaderDAO();
    private final ReaderService readerService = new ReaderService();

    // Components
    private JPanel     contentArea;
    private CardLayout cardLayout;
    private JLabel     clockLabel;

    // Nav buttons
    private JButton navSearch, navBorrowed, navProfile, navPassword;
    private JButton activeNavBtn;

    // Notification badge count
    private int overdueCountBadge = 0;
    private JLabel borrowBadge;

    // Table models
    private DefaultTableModel searchTableModel;
    private DefaultTableModel borrowTableModel;

    // Search
    private JTextField  searchField;
    private JComboBox<String> categoryFilter;

    // Borrow summary labels
    private JLabel lblBorrowingCount;
    private JLabel lblOverdueCount;
    private JLabel lblReturnedCount;
    private JLabel lblTotalFine;

    // Profile edit fields
    private JTextField  pfPhone, pfEmail, pfAddress;
    private JLabel      profileStatusLabel;

    // ================================================================
    //  Constructor
    // ================================================================

    public ReaderPortalFrame(User user, Reader reader) {
        super("📚 Thư Viện Nguyễn Huệ — Cổng Độc Giả");
        this.currentUser = user;
        this.currentReader = reader;
        initUI();
        setupWindowEvents();
        showTab("search");
        // Load overdue count for badge
        SwingUtilities.invokeLater(this::loadOverdueBadge);
    }

    // ================================================================
    //  Init UI
    // ================================================================

    private void initUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 680));
        setPreferredSize(new Dimension(1200, 780));

        // Icon
        try {
            var icon = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            g.drawString("📖", 2, 26);
            g.dispose();
            setIconImage(icon);
        } catch (Exception ignored) {}

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_PRIMARY);
        setContentPane(root);

        // Header
        root.add(buildHeader(), BorderLayout.NORTH);

        // Content
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(UITheme.BG_PRIMARY);
        contentArea.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG,
                                              UITheme.PAD_LG, UITheme.PAD_LG));

        contentArea.add(buildSearchPanel(), "search");
        contentArea.add(buildBorrowedPanel(), "borrowed");
        contentArea.add(buildProfilePanel(), "profile");
        contentArea.add(buildPasswordPanel(), "password");

        root.add(contentArea, BorderLayout.CENTER);

        // Footer
        root.add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    // ================================================================
    //  Header
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient xanh đại dương đậm
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0C4A6E), getWidth(), 0, new Color(0x0369A1)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Subtle overlay
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
                g2.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(0, 0, 0, 0)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 64));
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        // Left: Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel titleIcon = new JLabel("📚");
        titleIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        leftPanel.add(titleIcon);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Thư Viện Nguyễn Huệ");
        title.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Cổng Độc Giả Trực Tuyến");
        subtitle.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 11));
        subtitle.setForeground(new Color(0xBAE6FD));
        titleBlock.add(title);
        titleBlock.add(subtitle);
        leftPanel.add(titleBlock);

        header.add(leftPanel, BorderLayout.WEST);

        // Center: Navigation tabs
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(14, 0, 0, 0));

        navSearch   = createNavButton("🔍 Tra Cứu Sách", "search");
        navBorrowed = createNavButton("📖 Sách Đang Mượn", "borrowed");
        navProfile  = createNavButton("👤 Tài Khoản", "profile");
        navPassword = createNavButton("🔒 Đổi Mật Khẩu", "password");

        nav.add(navSearch);
        nav.add(createBorrowedNavWithBadge());
        nav.add(navProfile);
        nav.add(navPassword);

        header.add(nav, BorderLayout.CENTER);

        // Right: User info + Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        clockLabel = new JLabel();
        clockLabel.setFont(UITheme.FONT_SMALL);
        clockLabel.setForeground(new Color(255, 255, 255, 180));
        updateClock();
        rightPanel.add(clockLabel);

        // Avatar circle
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
                g2.setColor(Color.WHITE);
                String ini = getInitials(currentUser.getFullName());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, (getWidth() - fm.stringWidth(ini)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setOpaque(false);
        rightPanel.add(avatar);

        JLabel userLabel = new JLabel(currentUser.getFullName());
        userLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        userLabel.setForeground(Color.WHITE);
        rightPanel.add(userLabel);

        JButton logoutBtn = new JButton("Đăng xuất ⏏");
        logoutBtn.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 11));
        logoutBtn.setForeground(new Color(0xFCA5A5));
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                logoutBtn.setForeground(new Color(0xFEE2E2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                logoutBtn.setForeground(new Color(0xFCA5A5));
            }
        });
        logoutBtn.addActionListener(e -> doLogout());
        rightPanel.add(logoutBtn);

        header.add(rightPanel, BorderLayout.EAST);

        // Clock timer
        Timer clockTimer = new Timer(30_000, e -> updateClock());
        clockTimer.start();

        return header;
    }

    /** Wraps navBorrowed in a panel with an overlay badge for overdue count. */
    private JPanel createBorrowedNavWithBadge() {
        JPanel wrapper = new JPanel(null); // absolute layout
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(170, 36));

        navBorrowed.setBounds(0, 0, 170, 36);
        wrapper.add(navBorrowed);

        borrowBadge = new JLabel("0") {
            @Override
            protected void paintComponent(Graphics g) {
                if (overdueCountBadge > 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UITheme.COLOR_DANGER);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 9));
                    g2.setColor(Color.WHITE);
                    String text = String.valueOf(overdueCountBadge);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        borrowBadge.setBounds(148, 2, 18, 18);
        borrowBadge.setOpaque(false);
        wrapper.add(borrowBadge, 0); // on top

        return wrapper;
    }

    private JButton createNavButton(String text, String tabKey) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                if (this == activeNavBtn) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                    // Bottom indicator
                    g2.setPaint(new GradientPaint(8, h - 3, new Color(0x7DD3FC), w - 8, h - 3, new Color(0x38BDF8)));
                    g2.fillRoundRect(8, h - 3, w - 16, 3, 2, 2);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }
                g2.dispose();

                // Text
                g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(this == activeNavBtn ? Color.WHITE : new Color(255, 255, 255, 160));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2 - 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(170, 36));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showTab(tabKey));
        return btn;
    }

    // ================================================================
    //  Footer
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xF1F5F9));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Top border line
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        footer.setPreferredSize(new Dimension(0, 32));
        footer.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel leftLbl = new JLabel("📚 Hệ thống Quản lý Thư viện Nguyễn Huệ — Reader Edition v3.0");
        leftLbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
        leftLbl.setForeground(UITheme.TEXT_MUTED);
        footer.add(leftLbl, BorderLayout.WEST);

        JLabel rightLbl = new JLabel("Đang đăng nhập: " + currentUser.getFullName() + " (" +
                (currentReader != null ? currentReader.getReaderCode() : "N/A") + ")");
        rightLbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
        rightLbl.setForeground(UITheme.TEXT_MUTED);
        rightLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        footer.add(rightLbl, BorderLayout.EAST);

        return footer;
    }

    // ================================================================
    //  Tab Navigation
    // ================================================================

    private void showTab(String tabKey) {
        cardLayout.show(contentArea, tabKey);
        activeNavBtn = switch (tabKey) {
            case "search"   -> navSearch;
            case "borrowed" -> navBorrowed;
            case "profile"  -> navProfile;
            case "password" -> navPassword;
            default -> navSearch;
        };
        // Repaint nav buttons
        navSearch.repaint();
        navBorrowed.repaint();
        navProfile.repaint();
        navPassword.repaint();

        // Refresh data
        if ("borrowed".equals(tabKey)) refreshBorrowedTable();
        if ("profile".equals(tabKey)) refreshProfileData();
    }

    // ================================================================
    //  TAB 1: Tra Cứu Sách
    // ================================================================

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        // === Welcome banner ===
        JPanel welcomeBanner = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0C4A6E), getWidth(), getHeight(), new Color(0x0891B2)));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        welcomeBanner.setOpaque(false);
        welcomeBanner.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel welcomeText = new JPanel();
        welcomeText.setOpaque(false);
        welcomeText.setLayout(new BoxLayout(welcomeText, BoxLayout.Y_AXIS));

        JLabel greetLbl = new JLabel("Xin chào, " + currentUser.getFullName() + "! 👋");
        greetLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 18));
        greetLbl.setForeground(Color.WHITE);
        greetLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomeText.add(greetLbl);
        welcomeText.add(Box.createVerticalStrut(4));

        JLabel welcomeDesc = new JLabel("<html>Tra cứu sách trong kho thư viện, xem thông tin chi tiết và tình trạng có sẵn.</html>");
        welcomeDesc.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        welcomeDesc.setForeground(new Color(0xBAE6FD));
        welcomeDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomeText.add(welcomeDesc);

        welcomeBanner.add(welcomeText, BorderLayout.CENTER);

        // Quick stats on the right
        JPanel quickStats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        quickStats.setOpaque(false);

        if (currentReader != null) {
            quickStats.add(createQuickStat("Mã thẻ", currentReader.getReaderCode()));
            String statusStr = currentReader.getStatus().getLabel();
            quickStats.add(createQuickStat("Trạng thái", statusStr));
        }

        welcomeBanner.add(quickStats, BorderLayout.EAST);

        // === Top bar: title + search ===
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setOpaque(false);
        topPanel.add(welcomeBanner, BorderLayout.NORTH);

        // Search controls
        JPanel searchControls = new JPanel(new BorderLayout(8, 0));
        searchControls.setOpaque(false);
        searchControls.setBorder(new EmptyBorder(8, 0, 0, 0));

        JLabel titleLbl = new JLabel("🔍 Tra Cứu Sách Trong Thư Viện");
        titleLbl.setFont(UITheme.FONT_H2);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        searchControls.add(titleLbl, BorderLayout.WEST);

        // Search bar with category filter
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);

        // Category filter
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("Tất cả thể loại");
        loadCategories();
        categoryFilter.setFont(UITheme.FONT_BODY);
        categoryFilter.setPreferredSize(new Dimension(160, UITheme.INPUT_HEIGHT));
        categoryFilter.addActionListener(e -> doSearchBooks());
        searchBar.add(categoryFilter, BorderLayout.WEST);

        searchField = UITheme.createSearchField();
        searchField.setPreferredSize(new Dimension(300, UITheme.INPUT_HEIGHT));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSearchBooks();
            }
        });
        searchBar.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchButtons.setOpaque(false);

        JButton btnSearch = UITheme.createPrimaryButton("Tìm Kiếm");
        btnSearch.addActionListener(e -> doSearchBooks());

        JButton btnRefreshSearch = UITheme.createSecondaryButton("↺ Làm Mới");
        btnRefreshSearch.addActionListener(e -> {
            if (searchField != null) searchField.setText("");
            if (categoryFilter != null) categoryFilter.setSelectedIndex(0);
            doSearchBooks();
        });

        searchButtons.add(btnSearch);
        searchButtons.add(btnRefreshSearch);
        searchBar.add(searchButtons, BorderLayout.EAST);

        searchControls.add(searchBar, BorderLayout.EAST);
        topPanel.add(searchControls, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"ISBN", "Tên Sách", "Tác Giả", "Thể Loại", "NXB", "Năm", "Còn/Tổng", "Trạng Thái"};
        searchTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(searchTableModel);
        UITheme.styleTable(table);

        // Double click to view detail
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) showBookDetail(row);
                }
            }
        });

        // Status column renderer
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                String status = val != null ? val.toString() : "";
                if ("Có sẵn".equals(status)) {
                    setForeground(UITheme.COLOR_SUCCESS);
                } else {
                    setForeground(UITheme.COLOR_DANGER);
                }
                setFont(UITheme.FONT_BOLD);
                return this;
            }
        });

        // Availability column renderer with indicator dot
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                setFont(UITheme.FONT_BOLD);
                String v = val != null ? val.toString() : "";
                if (v.startsWith("0/")) {
                    setForeground(UITheme.COLOR_DANGER);
                } else {
                    setForeground(UITheme.COLOR_SUCCESS);
                }
                return this;
            }
        });

        // Column widths
        int[] widths = {90, 220, 140, 100, 120, 50, 70, 80};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Hint label
        JPanel tableWrapper = new JPanel(new BorderLayout(0, 4));
        tableWrapper.setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        tableWrapper.add(scroll, BorderLayout.CENTER);

        JLabel hintLbl = new JLabel("💡 Nhấn đúp vào sách để xem thông tin chi tiết");
        hintLbl.setFont(new Font(UITheme.FONT_NAME, Font.ITALIC, 11));
        hintLbl.setForeground(UITheme.TEXT_MUTED);
        tableWrapper.add(hintLbl, BorderLayout.SOUTH);

        panel.add(tableWrapper, BorderLayout.CENTER);

        // Load initial data
        doSearchBooks();

        return panel;
    }

    private JPanel createQuickStat(String label, String value) {
        JPanel stat = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        stat.setOpaque(false);
        stat.setLayout(new BoxLayout(stat, BoxLayout.Y_AXIS));
        stat.setBorder(new EmptyBorder(8, 16, 8, 16));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
        lbl.setForeground(new Color(0x7DD3FC));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        stat.add(lbl);

        JLabel val = new JLabel(value);
        val.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        val.setForeground(Color.WHITE);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);
        stat.add(val);

        return stat;
    }

    private void loadCategories() {
        try {
            CategoryDAO categoryDAO = new CategoryDAO();
            List<Category> cats = categoryDAO.findAll();
            for (Category c : cats) {
                categoryFilter.addItem(c.getName());
            }
        } catch (Exception ex) {
            // Fallback: just show "Tất cả thể loại"
            System.err.println("[ReaderPortal] Lỗi tải thể loại: " + ex.getMessage());
        }
    }

    private void doSearchBooks() {
        String keyword = searchField != null ? searchField.getText().trim() : "";
        String category = categoryFilter != null
            ? (categoryFilter.getSelectedIndex() == 0 ? "" : (String) categoryFilter.getSelectedItem())
            : "";

        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                List<Book> all = bookDAO.search(keyword);
                if (!category.isEmpty()) {
                    all = all.stream()
                        .filter(b -> category.equalsIgnoreCase(b.getCategory()))
                        .toList();
                }
                return all;
            }
            @Override protected void done() {
                try {
                    List<Book> books = get();
                    searchTableModel.setRowCount(0);
                    for (Book b : books) {
                        String availability = b.getAvailableCopies() > 0 ? "Có sẵn" : "Hết";
                        searchTableModel.addRow(new Object[]{
                            b.getIsbn(), b.getTitle(), b.getAuthor(),
                            b.getCategory(), b.getPublisher(), b.getPublishYear(),
                            b.getAvailableCopies() + "/" + b.getTotalCopies(),
                            availability
                        });
                    }
                } catch (Exception ex) {
                    System.err.println("[ReaderPortal] Lỗi tải sách: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showBookDetail(int row) {
        String isbn   = String.valueOf(searchTableModel.getValueAt(row, 0));
        String title   = String.valueOf(searchTableModel.getValueAt(row, 1));
        String author  = String.valueOf(searchTableModel.getValueAt(row, 2));
        String category = String.valueOf(searchTableModel.getValueAt(row, 3));
        String publisher = String.valueOf(searchTableModel.getValueAt(row, 4));
        String year    = String.valueOf(searchTableModel.getValueAt(row, 5));
        String copies  = String.valueOf(searchTableModel.getValueAt(row, 6));
        String status  = String.valueOf(searchTableModel.getValueAt(row, 7));

        // Build detail dialog
        JDialog dialog = new JDialog(this, "Chi Tiết Sách", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);

        // Header
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0C4A6E), getWidth(), 0, new Color(0x0891B2)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 80));
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));

        JLabel bookIcon = new JLabel("📕 " + title);
        bookIcon.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 16));
        bookIcon.setForeground(Color.WHITE);
        bookIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerText.add(bookIcon);
        headerText.add(Box.createVerticalStrut(4));

        JLabel authorLbl = new JLabel("Tác giả: " + author);
        authorLbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        authorLbl.setForeground(new Color(0xBAE6FD));
        authorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerText.add(authorLbl);

        header.add(headerText, BorderLayout.CENTER);

        // Status badge
        boolean available = "Có sẵn".equals(status);
        JLabel statusBadge = new JLabel(available ? "✅ Có sẵn" : "❌ Hết sách") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(available ? new Color(0x10B981, true) : new Color(0xEF4444, true));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusBadge.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        statusBadge.setForeground(available ? new Color(0xD1FAE5) : new Color(0xFEE2E2));
        statusBadge.setBorder(new EmptyBorder(6, 14, 6, 14));
        header.add(statusBadge, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // Info content
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UITheme.BG_WHITE);
        content.setBorder(new EmptyBorder(24, 32, 24, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 0, 8, 16);
        gbc.gridy = 0;

        addDetailRow(content, gbc, "ISBN", isbn);
        addDetailRow(content, gbc, "Tên sách", title);
        addDetailRow(content, gbc, "Tác giả", author);
        addDetailRow(content, gbc, "Thể loại", category);
        addDetailRow(content, gbc, "Nhà xuất bản", publisher);
        addDetailRow(content, gbc, "Năm xuất bản", year);
        addDetailRow(content, gbc, "Số bản", copies);
        addDetailRow(content, gbc, "Tình trạng", status);

        // Try to load full description
        try {
            List<Book> found = bookDAO.search(isbn);
            if (!found.isEmpty() && found.get(0).getDescription() != null
                    && !found.get(0).getDescription().isBlank()) {
                gbc.gridy++;
                gbc.gridx = 0;
                gbc.gridwidth = 2;
                gbc.fill = GridBagConstraints.HORIZONTAL;

                JPanel descPanel = new JPanel(new BorderLayout());
                descPanel.setOpaque(false);
                descPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

                JLabel descTitle = new JLabel("📝 Mô tả");
                descTitle.setFont(UITheme.FONT_BOLD);
                descTitle.setForeground(UITheme.TEXT_PRIMARY);
                descPanel.add(descTitle, BorderLayout.NORTH);

                JTextArea descText = new JTextArea(found.get(0).getDescription());
                descText.setFont(UITheme.FONT_BODY);
                descText.setForeground(UITheme.TEXT_SECONDARY);
                descText.setLineWrap(true);
                descText.setWrapStyleWord(true);
                descText.setEditable(false);
                descText.setBackground(new Color(0xF8FAFC));
                descText.setBorder(new EmptyBorder(8, 12, 8, 12));
                descText.setRows(3);
                descPanel.add(descText, BorderLayout.CENTER);

                content.add(descPanel, gbc);
            }
        } catch (Exception ignored) {}

        root.add(content, BorderLayout.CENTER);

        // Close button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        btnPanel.setBackground(new Color(0xF8FAFC));
        btnPanel.setBorder(new EmptyBorder(4, 16, 8, 16));

        JButton closeBtn = UITheme.createSecondaryButton("Đóng");
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        root.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(130, 22));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 13));
        val.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(val, gbc);
    }

    // ================================================================
    //  TAB 2: Sách Đang Mượn
    // ================================================================

    private JTable borrowTable;
    private JButton btnRenewBorrow;

    private JPanel buildBorrowedPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        // Header with title and action buttons
        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("📖 Sách Bạn Đang Mượn");
        titleLbl.setFont(UITheme.FONT_H2);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        topPanel.add(titleLbl, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        btnRenewBorrow = UITheme.createPrimaryButton("↻  Gia Hạn (+7 ngày)");
        btnRenewBorrow.setEnabled(false);
        btnRenewBorrow.addActionListener(e -> doRenewSelectedBorrow());

        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");
        btnRefresh.addActionListener(e -> refreshBorrowedTable());

        btnGroup.add(btnRenewBorrow);
        btnGroup.add(btnRefresh);
        topPanel.add(btnGroup, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"Mã Phiếu", "Tên Sách", "Ngày Mượn", "Hạn Trả", "Còn Lại", "Ngày Trả", "Trạng Thái", "Gia Hạn"};
        borrowTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        borrowTable = new JTable(borrowTableModel);
        UITheme.styleTable(borrowTable);

        // Selection listener to enable renew button
        borrowTable.getSelectionModel().addListSelectionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row >= 0) {
                String status = String.valueOf(borrowTableModel.getValueAt(row, 6));
                btnRenewBorrow.setEnabled("Đang mượn".equals(status));
            } else {
                btnRenewBorrow.setEnabled(false);
            }
        });

        // "Còn Lại" column - days remaining with color coding
        borrowTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                setFont(UITheme.FONT_BOLD);
                String v = val != null ? val.toString() : "";
                if (v.contains("Quá hạn")) {
                    setForeground(UITheme.COLOR_DANGER);
                } else if (v.contains("Sắp hạn")) {
                    setForeground(UITheme.COLOR_WARNING);
                } else if (v.contains("—") || v.contains("Đã trả")) {
                    setForeground(UITheme.TEXT_MUTED);
                } else {
                    setForeground(UITheme.COLOR_SUCCESS);
                }
                return this;
            }
        });

        // Status column renderer
        borrowTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                String status = val != null ? val.toString() : "";
                setFont(UITheme.FONT_BOLD);
                switch (status) {
                    case "Đang mượn" -> setForeground(UITheme.COLOR_INFO);
                    case "Đã trả"   -> setForeground(UITheme.COLOR_SUCCESS);
                    case "Quá hạn"  -> setForeground(UITheme.COLOR_DANGER);
                    case "Mất sách" -> setForeground(UITheme.COLOR_DANGER);
                    default          -> setForeground(UITheme.TEXT_PRIMARY);
                }
                return this;
            }
        });

        // Column widths
        int[] widths = {80, 200, 90, 90, 100, 90, 90, 70};
        for (int i = 0; i < widths.length && i < borrowTable.getColumnCount(); i++) {
            borrowTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(borrowTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);

        // Bottom summary cards
        panel.add(buildBorrowSummary(), BorderLayout.SOUTH);

        return panel;
    }

    private void doRenewSelectedBorrow() {
        int row = borrowTable != null ? borrowTable.getSelectedRow() : -1;
        if (row < 0) return;

        String code = String.valueOf(borrowTableModel.getValueAt(row, 0));
        String title = String.valueOf(borrowTableModel.getValueAt(row, 1));
        String renewCountStr = String.valueOf(borrowTableModel.getValueAt(row, 7));

        try {
            int borrowId = Integer.parseInt(code.replace("PM-", "").trim());

            // Check renew count
            if (renewCountStr.contains("" + BorrowService.MAX_RENEW_COUNT)) {
                UITheme.showError(this,
                    "Sách \"" + title + "\" đã gia hạn tối đa " + BorrowService.MAX_RENEW_COUNT + " lần.\n" +
                    "Vui lòng trả sách và mượn lại nếu cần.");
                return;
            }

            boolean confirm = UITheme.showConfirm(this,
                "Bạn có muốn gia hạn thêm 7 ngày cho sách:\n\"" + title + "\" không?\n\n" +
                "Lưu ý: Mỗi phiếu mượn được gia hạn tối đa " + BorrowService.MAX_RENEW_COUNT + " lần.",
                "Xác nhận gia hạn sách");
            if (!confirm) return;

            new BorrowService().renewBorrow(borrowId, 7);
            UITheme.showSuccess(this, "Gia hạn thành công thêm 7 ngày cho sách:\n\"" + title + "\"");
            refreshBorrowedTable();
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private JPanel buildBorrowSummary() {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        lblBorrowingCount = new JLabel("0");
        lblOverdueCount = new JLabel("0");
        lblReturnedCount = new JLabel("0");
        lblTotalFine = new JLabel("0đ");

        summaryPanel.add(createSummaryCard("📚", "Đang mượn", lblBorrowingCount, UITheme.COLOR_INFO));
        summaryPanel.add(createSummaryCard("⚠️", "Quá hạn", lblOverdueCount, UITheme.COLOR_DANGER));
        summaryPanel.add(createSummaryCard("✅", "Đã trả", lblReturnedCount, UITheme.COLOR_SUCCESS));
        summaryPanel.add(createSummaryCard("💰", "Tiền phạt", lblTotalFine, UITheme.COLOR_WARNING));

        return summaryPanel;
    }

    private JPanel createSummaryCard(String icon, String title, JLabel valueLbl, Color color) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Left accent bar
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                // Subtle shadow
                g2.setColor(new Color(0, 0, 0, 8));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        card.add(iconLbl, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL);
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        textPanel.add(titleLbl);

        valueLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        valueLbl.setForeground(color);
        textPanel.add(valueLbl);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    private void refreshBorrowedTable() {
        if (currentReader == null) return;

        SwingWorker<List<Borrow>, Void> worker = new SwingWorker<>() {
            @Override protected List<Borrow> doInBackground() throws Exception {
                return borrowDAO.findByReader(currentReader.getId());
            }
            @Override protected void done() {
                try {
                    List<Borrow> borrows = get();
                    borrowTableModel.setRowCount(0);

                    int borrowing = 0, overdue = 0, returned = 0;
                    double totalFine = 0;

                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                    for (Borrow b : borrows) {
                        String statusLabel = b.getStatus().getLabel();

                        // Calculate days remaining
                        String daysRemainingStr = "—";
                        if (b.isActive() && b.getDueDate() != null && !b.getDueDate().isBlank()) {
                            try {
                                LocalDate due = LocalDate.parse(b.getDueDate(), fmt);
                                long days = ChronoUnit.DAYS.between(LocalDate.now(), due);
                                if (days < 0) {
                                    daysRemainingStr = "Quá hạn " + Math.abs(days) + " ngày";
                                } else if (days <= 3) {
                                    daysRemainingStr = "Sắp hạn (" + days + " ngày)";
                                } else {
                                    daysRemainingStr = days + " ngày";
                                }
                            } catch (Exception ignored) {}
                        } else if (b.isReturned()) {
                            daysRemainingStr = "Đã trả";
                        }

                        borrowTableModel.addRow(new Object[]{
                            "PM-" + String.format("%04d", b.getId()),
                            b.getBookTitle(),
                            b.getBorrowDate(),
                            b.getDueDate(),
                            daysRemainingStr,
                            b.getReturnDate() != null ? b.getReturnDate() : "—",
                            statusLabel,
                            b.getRenewCount() + "/" + BorrowService.MAX_RENEW_COUNT + " lần"
                        });

                        switch (b.getStatus()) {
                            case BORROWING -> borrowing++;
                            case OVERDUE   -> overdue++;
                            case RETURNED  -> returned++;
                            default -> {}
                        }
                        totalFine += b.getFineAmount();
                    }

                    // Update summary cards
                    updateSummaryCards(borrowing, overdue, returned, totalFine);

                    // Update badge
                    overdueCountBadge = overdue;
                    if (borrowBadge != null) borrowBadge.repaint();

                } catch (Exception ex) {
                    System.err.println("[ReaderPortal] Lỗi tải phiếu mượn: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateSummaryCards(int borrowing, int overdue, int returned, double totalFine) {
        if (lblBorrowingCount != null) lblBorrowingCount.setText(String.valueOf(borrowing));
        if (lblOverdueCount != null)   lblOverdueCount.setText(String.valueOf(overdue));
        if (lblReturnedCount != null)  lblReturnedCount.setText(String.valueOf(returned));
        if (lblTotalFine != null) {
            if (totalFine <= 0) {
                lblTotalFine.setText("0đ");
            } else {
                lblTotalFine.setText(String.format("%,.0fđ", totalFine));
            }
        }
    }

    private void loadOverdueBadge() {
        if (currentReader == null) return;
        try {
            List<Borrow> borrows = borrowDAO.findByReader(currentReader.getId());
            overdueCountBadge = (int) borrows.stream()
                .filter(b -> b.getStatus() == Borrow.Status.OVERDUE)
                .count();
            if (borrowBadge != null) borrowBadge.repaint();
        } catch (Exception ignored) {}
    }

    // ================================================================
    //  TAB 3: Thông Tin Cá Nhân (Editable)
    // ================================================================

    private JPanel buildProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titleLbl = new JLabel("👤 Thông Tin Tài Khoản");
        titleLbl.setFont(UITheme.FONT_H2);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(titleLbl, BorderLayout.NORTH);

        // Main card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 0));

        // Top section: Avatar + Basic info
        JPanel topSection = new JPanel(new BorderLayout(24, 0));
        topSection.setOpaque(false);
        topSection.setBorder(new EmptyBorder(28, 36, 20, 36));

        // Avatar
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0C4A6E), getWidth(), getHeight(), new Color(0x0891B2)));
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Initials
                g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 36));
                g2.setColor(Color.WHITE);
                String initials = getInitials(currentUser.getFullName());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initials)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initials, x, y);
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(100, 100));
        avatarPanel.setOpaque(false);

        JPanel avatarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        avatarWrapper.setOpaque(false);
        avatarWrapper.setPreferredSize(new Dimension(110, 110));
        avatarWrapper.add(avatarPanel);
        topSection.add(avatarWrapper, BorderLayout.WEST);

        // Name + Role + Status
        JPanel nameBlock = new JPanel();
        nameBlock.setOpaque(false);
        nameBlock.setLayout(new BoxLayout(nameBlock, BoxLayout.Y_AXIS));
        nameBlock.setBorder(new EmptyBorder(12, 0, 0, 0));

        JLabel nameLbl = new JLabel(currentUser.getFullName());
        nameLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        nameLbl.setForeground(UITheme.TEXT_PRIMARY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameBlock.add(nameLbl);

        nameBlock.add(Box.createVerticalStrut(4));

        JLabel roleLbl = new JLabel("🎫 " + (currentReader != null ? currentReader.getReaderCode() : "—") +
            "  •  " + currentUser.getRole().getLabel());
        roleLbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 13));
        roleLbl.setForeground(UITheme.TEXT_SECONDARY);
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameBlock.add(roleLbl);

        nameBlock.add(Box.createVerticalStrut(8));

        // Status pill
        profileStatusLabel = new JLabel();
        updateProfileStatusLabel();
        profileStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameBlock.add(profileStatusLabel);

        topSection.add(nameBlock, BorderLayout.CENTER);

        card.add(topSection, BorderLayout.NORTH);

        // Separator
        JPanel separator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(UITheme.BORDER_COLOR);
                g.fillRect(36, 0, getWidth() - 72, 1);
            }
        };
        separator.setPreferredSize(new Dimension(0, 1));
        separator.setOpaque(false);

        // Bottom section: Editable fields
        JPanel bottomSection = new JPanel(new GridBagLayout());
        bottomSection.setOpaque(false);
        bottomSection.setBorder(new EmptyBorder(20, 36, 28, 36));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 0, 6, 16);
        gbc.gridy = 0;

        // Read-only fields
        addProfileInfoRow(bottomSection, gbc, "Tên đăng nhập", currentUser.getUsername());
        addProfileInfoRow(bottomSection, gbc, "Họ và tên", currentUser.getFullName());
        addProfileInfoRow(bottomSection, gbc, "Ngày sinh",
            currentReader != null && currentReader.getBirthDate() != null ? currentReader.getBirthDate() : "—");
        addProfileInfoRow(bottomSection, gbc, "Ngày đăng ký",
            currentReader != null && currentReader.getJoinDate() != null ? currentReader.getJoinDate() : "—");

        // Expiry date
        if (currentReader != null && currentReader.getExpiryDate() != null
                && !currentReader.getExpiryDate().isBlank()) {
            boolean expired = currentReader.isExpiredByDate();
            String expiryText = currentReader.getExpiryDate() + (expired ? " (Đã hết hạn)" : "");
            gbc.gridy++;
            gbc.gridx = 0;
            JLabel lbl = new JLabel("Hạn thẻ");
            lbl.setFont(UITheme.FONT_BOLD);
            lbl.setForeground(UITheme.TEXT_MUTED);
            lbl.setPreferredSize(new Dimension(140, 28));
            bottomSection.add(lbl, gbc);

            gbc.gridx = 1;
            JLabel val = new JLabel(expiryText);
            val.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 13));
            val.setForeground(expired ? UITheme.COLOR_DANGER : UITheme.COLOR_SUCCESS);
            bottomSection.add(val, gbc);
        }

        // Editable fields section header
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 0, 8, 0);
        JLabel editTitle = new JLabel("✏️ Thông tin liên hệ (có thể chỉnh sửa)");
        editTitle.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 13));
        editTitle.setForeground(UITheme.ACCENT_PRIMARY);
        bottomSection.add(editTitle, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 0, 6, 16);

        // Editable: Phone
        pfPhone = new JTextField(currentReader != null && currentReader.getPhone() != null ? currentReader.getPhone() : "");
        pfPhone.setFont(UITheme.FONT_BODY);
        pfPhone.setPreferredSize(new Dimension(280, UITheme.INPUT_HEIGHT));
        addProfileEditRow(bottomSection, gbc, "Số điện thoại", pfPhone);

        // Editable: Email
        pfEmail = new JTextField(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        pfEmail.setFont(UITheme.FONT_BODY);
        pfEmail.setPreferredSize(new Dimension(280, UITheme.INPUT_HEIGHT));
        addProfileEditRow(bottomSection, gbc, "Email", pfEmail);

        // Editable: Address
        pfAddress = new JTextField(currentReader != null && currentReader.getAddress() != null ? currentReader.getAddress() : "");
        pfAddress.setFont(UITheme.FONT_BODY);
        pfAddress.setPreferredSize(new Dimension(280, UITheme.INPUT_HEIGHT));
        addProfileEditRow(bottomSection, gbc, "Địa chỉ", pfAddress);

        // Save button
        gbc.gridy++;
        gbc.gridx = 1;
        gbc.insets = new Insets(16, 0, 0, 0);
        JButton btnSave = UITheme.createPrimaryButton("💾  Lưu Thay Đổi");
        btnSave.addActionListener(e -> doSaveProfile());
        bottomSection.add(btnSave, gbc);

        // Combine top + separator + bottom
        JPanel cardContent = new JPanel(new BorderLayout());
        cardContent.setOpaque(false);
        cardContent.add(separator, BorderLayout.NORTH);
        cardContent.add(bottomSection, BorderLayout.CENTER);

        card.add(cardContent, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void updateProfileStatusLabel() {
        if (currentReader == null) {
            profileStatusLabel.setText("—");
            return;
        }
        boolean active = currentReader.getStatus() == Reader.Status.ACTIVE;
        String statusText = active ? "● Hoạt động" : "● " + currentReader.getStatus().getLabel();
        profileStatusLabel.setText(statusText);
        profileStatusLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        profileStatusLabel.setForeground(active ? UITheme.COLOR_SUCCESS : UITheme.COLOR_DANGER);
    }

    private void addProfileInfoRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(140, 28));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value != null && !value.isBlank() ? value : "—");
        val.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 13));
        val.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(val, gbc);
    }

    private void addProfileEditRow(JPanel panel, GridBagConstraints gbc, String label, JTextField field) {
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(140, 28));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void doSaveProfile() {
        if (currentReader == null) {
            UITheme.showError(this, "Không tìm thấy thông tin độc giả để cập nhật.");
            return;
        }

        String phone   = pfPhone.getText().trim();
        String email   = pfEmail.getText().trim();
        String address = pfAddress.getText().trim();

        // Basic validation
        if (!phone.isEmpty() && !phone.matches("^0\\d{9,10}$")) {
            UITheme.showError(this, "Số điện thoại không hợp lệ.\nVui lòng nhập số bắt đầu bằng 0 (10-11 chữ số).");
            pfPhone.requestFocus();
            return;
        }

        if (!email.isEmpty() && !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            UITheme.showError(this, "Email không hợp lệ.\nVui lòng kiểm tra lại định dạng email.");
            pfEmail.requestFocus();
            return;
        }

        try {
            currentReader.setPhone(phone);
            currentReader.setEmail(email);
            currentReader.setAddress(address);
            readerService.updateReader(currentReader);

            // Reload reader from DB
            currentReader = readerDAO.findById(currentReader.getId());

            UITheme.showSuccess(this, "Cập nhật thông tin cá nhân thành công!");
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi cập nhật: " + ex.getMessage());
        }
    }

    private void refreshProfileData() {
        if (currentReader == null) return;
        try {
            currentReader = readerDAO.findById(currentReader.getId());
            if (pfPhone != null)   pfPhone.setText(currentReader.getPhone() != null ? currentReader.getPhone() : "");
            if (pfEmail != null)   pfEmail.setText(currentReader.getEmail() != null ? currentReader.getEmail() : "");
            if (pfAddress != null) pfAddress.setText(currentReader.getAddress() != null ? currentReader.getAddress() : "");
            updateProfileStatusLabel();
        } catch (Exception ex) {
            System.err.println("[ReaderPortal] Lỗi refresh profile: " + ex.getMessage());
        }
    }

    // ================================================================
    //  TAB 4: Đổi Mật Khẩu
    // ================================================================

    private JPanel buildPasswordPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titleLbl = new JLabel("🔒 Đổi Mật Khẩu");
        titleLbl.setFont(UITheme.FONT_H2);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(titleLbl, BorderLayout.NORTH);

        // Password card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(40, 60, 40, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.gridy = 0;

        // Shield icon
        JLabel shieldIcon = new JLabel("🛡️");
        shieldIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        shieldIcon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(shieldIcon, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 16, 0);
        JLabel descLbl = new JLabel("<html><center>Để bảo mật tài khoản, vui lòng nhập mật khẩu hiện tại<br>và mật khẩu mới (tối thiểu 6 ký tự).</center></html>");
        descLbl.setFont(UITheme.FONT_BODY);
        descLbl.setForeground(UITheme.TEXT_SECONDARY);
        descLbl.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(descLbl, gbc);

        // Current password
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 4, 0);
        JLabel curLbl = new JLabel("Mật khẩu hiện tại");
        curLbl.setFont(UITheme.FONT_BOLD);
        curLbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(curLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        JPasswordField curPass = UITheme.createPasswordField("Nhập mật khẩu hiện tại");
        curPass.setPreferredSize(new Dimension(360, UITheme.INPUT_HEIGHT + 4));
        card.add(curPass, gbc);

        // New password
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 4, 0);
        JLabel newLbl = new JLabel("Mật khẩu mới");
        newLbl.setFont(UITheme.FONT_BOLD);
        newLbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(newLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        JPasswordField newPass = UITheme.createPasswordField("Nhập mật khẩu mới (≥ 6 ký tự)");
        newPass.setPreferredSize(new Dimension(360, UITheme.INPUT_HEIGHT + 4));
        card.add(newPass, gbc);

        // Confirm password
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 4, 0);
        JLabel cfLbl = new JLabel("Xác nhận mật khẩu mới");
        cfLbl.setFont(UITheme.FONT_BOLD);
        cfLbl.setForeground(UITheme.TEXT_PRIMARY);
        card.add(cfLbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        JPasswordField cfPass = UITheme.createPasswordField("Nhập lại mật khẩu mới");
        cfPass.setPreferredSize(new Dimension(360, UITheme.INPUT_HEIGHT + 4));
        card.add(cfPass, gbc);

        // Error label
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 8, 0);
        JLabel pwError = new JLabel(" ");
        pwError.setFont(UITheme.FONT_SMALL);
        pwError.setForeground(UITheme.COLOR_DANGER);
        pwError.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(pwError, gbc);

        // Change password button
        gbc.gridy++;
        gbc.insets = new Insets(4, 0, 0, 0);
        JButton btnChange = UITheme.createPrimaryButton("🔐  Đổi Mật Khẩu");
        btnChange.setPreferredSize(new Dimension(360, UITheme.BUTTON_HEIGHT + 6));
        btnChange.addActionListener(e -> {
            String oldPw = new String(curPass.getPassword()).trim();
            String newPw = new String(newPass.getPassword()).trim();
            String cfPw  = new String(cfPass.getPassword()).trim();

            // Validation
            if (oldPw.isEmpty() || newPw.isEmpty() || cfPw.isEmpty()) {
                pwError.setText("Vui lòng điền đầy đủ các trường.");
                return;
            }
            if (newPw.length() < 6) {
                pwError.setText("Mật khẩu mới phải có ít nhất 6 ký tự.");
                return;
            }
            if (!newPw.equals(cfPw)) {
                pwError.setText("Mật khẩu xác nhận không khớp.");
                cfPass.setText("");
                cfPass.requestFocus();
                return;
            }
            if (oldPw.equals(newPw)) {
                pwError.setText("Mật khẩu mới phải khác mật khẩu hiện tại.");
                return;
            }

            pwError.setText(" ");
            btnChange.setEnabled(false);
            btnChange.setText("⏳ Đang xử lý...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    new AuthService().changePassword(currentUser.getId(), oldPw, newPw);
                    return null;
                }

                @Override
                protected void done() {
                    btnChange.setEnabled(true);
                    btnChange.setText("🔐  Đổi Mật Khẩu");
                    try {
                        get();
                        UITheme.showSuccess(ReaderPortalFrame.this,
                            "Đổi mật khẩu thành công!\nMật khẩu mới sẽ được áp dụng cho lần đăng nhập tiếp theo.");
                        curPass.setText("");
                        newPass.setText("");
                        cfPass.setText("");
                        pwError.setText(" ");
                    } catch (Exception ex) {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        pwError.setText(msg);
                    }
                }
            };
            worker.execute();
        });
        card.add(btnChange, gbc);

        // Center the card
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        card.setMaximumSize(new Dimension(500, 600));
        centerWrapper.add(card);

        panel.add(centerWrapper, BorderLayout.CENTER);
        return panel;
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    private void updateClock() {
        if (clockLabel != null) {
            clockLabel.setText(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm  •  dd/MM/yyyy")));
        }
    }

    // ================================================================
    //  Logout
    // ================================================================

    private void doLogout() {
        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn đăng xuất không?", "Xác nhận đăng xuất");
        if (!confirm) return;

        new AuthService().logout();
        dispose();

        SwingUtilities.invokeLater(ReaderApp::showLogin);
    }

    /** Mở frame tương ứng với vai trò người dùng sau khi đăng nhập lại. */
    public static void openFrameForCurrentUser() {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        if (user.isReader()) {
            // Load reader info
            try {
                Reader reader = new ReaderDAO().findById(user.getReaderId());
                new ReaderPortalFrame(user, reader).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                    "Lỗi tải thông tin độc giả: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            new MainFrame().setVisible(true);
        }
    }

    // ================================================================
    //  Window Events
    // ================================================================

    private void setupWindowEvents() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                boolean confirm = UITheme.showConfirm(ReaderPortalFrame.this,
                    "Bạn có muốn thoát ứng dụng không?", "Xác nhận thoát");
                if (confirm) {
                    DatabaseConnection.getInstance().closeConnection();
                    System.exit(0);
                }
            }
        });
    }
}
