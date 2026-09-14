package com.example.view;

import com.example.dao.BorrowDAO;
import com.example.dao.BookDAO;
import com.example.dao.ReaderDAO;
import com.example.model.Book;
import com.example.model.Borrow;
import com.example.model.Reader;
import com.example.model.User;
import com.example.service.AuthService;
import com.example.service.BorrowService;
import com.example.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Cổng thông tin dành riêng cho Độc giả (role = READER).
 * Gồm 3 tab: Tra cứu sách, Sách đang mượn, Thông tin cá nhân.
 */
public class ReaderPortalFrame extends JFrame {

    private final User currentUser;
    private final Reader currentReader;

    // DAOs
    private final BookDAO   bookDAO   = new BookDAO();
    private final BorrowDAO borrowDAO = new BorrowDAO();

    // Components
    private JPanel     contentArea;
    private CardLayout cardLayout;
    private JLabel     clockLabel;

    // Nav buttons
    private JButton navSearch, navBorrowed, navProfile;
    private JButton activeNavBtn;

    // Table models
    private DefaultTableModel searchTableModel;
    private DefaultTableModel borrowTableModel;

    // Search
    private JTextField searchField;

    // Borrow summary labels
    private JLabel lblBorrowingCount;
    private JLabel lblOverdueCount;
    private JLabel lblReturnedCount;

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
    }

    // ================================================================
    //  Init UI
    // ================================================================

    private void initUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setPreferredSize(new Dimension(1100, 720));

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

        root.add(contentArea, BorderLayout.CENTER);

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
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1565C0), getWidth(), 0, new Color(0x0D47A1)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 60));
        header.setBorder(new EmptyBorder(0, 20, 0, 20));

        // Left: Title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JLabel titleIcon = new JLabel("📚");
        titleIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        leftPanel.add(titleIcon);

        JLabel title = new JLabel("Thư Viện Nguyễn Huệ");
        title.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        leftPanel.add(title);

        header.add(leftPanel, BorderLayout.WEST);

        // Center: Navigation tabs
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        nav.setOpaque(false);
        nav.setBorder(new EmptyBorder(12, 0, 0, 0));

        navSearch   = createNavButton("🔍 Tra Cứu Sách", "search");
        navBorrowed = createNavButton("📖 Sách Đang Mượn", "borrowed");
        navProfile  = createNavButton("👤 Tài Khoản", "profile");

        nav.add(navSearch);
        nav.add(navBorrowed);
        nav.add(navProfile);

        header.add(nav, BorderLayout.CENTER);

        // Right: User info + Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        clockLabel = new JLabel();
        clockLabel.setFont(UITheme.FONT_SMALL);
        clockLabel.setForeground(new Color(255, 255, 255, 180));
        updateClock();
        rightPanel.add(clockLabel);

        JLabel userLabel = new JLabel(currentUser.getFullName());
        userLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        userLabel.setForeground(Color.WHITE);
        rightPanel.add(userLabel);

        JButton logoutBtn = new JButton("Đăng xuất");
        logoutBtn.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 11));
        logoutBtn.setForeground(new Color(255, 200, 200));
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> doLogout());
        rightPanel.add(logoutBtn);

        header.add(rightPanel, BorderLayout.EAST);

        // Clock timer
        Timer clockTimer = new Timer(30_000, e -> updateClock());
        clockTimer.start();

        return header;
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
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
                    // Bottom indicator
                    g2.setColor(Color.WHITE);
                    g2.fillRect(8, h - 3, w - 16, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRoundRect(0, 0, w, h, 8, 8);
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
        btn.setPreferredSize(new Dimension(160, 36));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showTab(tabKey));
        return btn;
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
            default -> navSearch;
        };
        // Repaint nav buttons
        navSearch.repaint();
        navBorrowed.repaint();
        navProfile.repaint();

        // Refresh data
        if ("borrowed".equals(tabKey)) refreshBorrowedTable();
    }

    // ================================================================
    //  TAB 1: Tra Cứu Sách
    // ================================================================

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        // Title
        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("🔍 Tra Cứu Sách Trong Thư Viện");
        titleLbl.setFont(UITheme.FONT_H2);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        topPanel.add(titleLbl, BorderLayout.WEST);

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        searchField = UITheme.createSearchField();
        searchField.setPreferredSize(new Dimension(350, UITheme.INPUT_HEIGHT));
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSearchBooks();
            }
        });
        searchBar.add(searchField, BorderLayout.CENTER);

        JButton btnSearch = UITheme.createPrimaryButton("Tìm Kiếm");
        btnSearch.addActionListener(e -> doSearchBooks());
        searchBar.add(btnSearch, BorderLayout.EAST);

        topPanel.add(searchBar, BorderLayout.EAST);
        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {"ISBN", "Tên Sách", "Tác Giả", "Thể Loại", "NXB", "Năm", "Còn/Tổng", "Trạng Thái"};
        searchTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(searchTableModel);
        UITheme.styleTable(table);

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

        // Column widths
        int[] widths = {100, 200, 130, 100, 120, 50, 70, 80};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);

        // Load initial data
        doSearchBooks();

        return panel;
    }

    private void doSearchBooks() {
        String keyword = searchField != null ? searchField.getText().trim() : "";
        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return bookDAO.search(keyword);
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
        String[] cols = {"Mã Phiếu", "Tên Sách", "Ngày Mượn", "Hạn Trả", "Ngày Trả", "Trạng Thái", "Gia Hạn"};
        borrowTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        borrowTable = new JTable(borrowTableModel);
        UITheme.styleTable(borrowTable);

        // Selection listener to enable renew button
        borrowTable.getSelectionModel().addListSelectionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row >= 0) {
                String status = String.valueOf(borrowTableModel.getValueAt(row, 5));
                btnRenewBorrow.setEnabled("Đang mượn".equals(status));
            } else {
                btnRenewBorrow.setEnabled(false);
            }
        });

        // Status column renderer
        borrowTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
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
        try {
            int borrowId = Integer.parseInt(code.replace("PM-", "").trim());
            boolean confirm = UITheme.showConfirm(this,
                "Bạn có muốn gia hạn thêm 7 ngày cho sách:\n\"" + title + "\" không?",
                "Xác nhận gia hạn sách");
            if (!confirm) return;

            new BorrowService().renewBorrow(borrowId, 7);
            UITheme.showSuccess(this, "Gia hạn thành công thêm 7 ngày!");
            refreshBorrowedTable();
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private JPanel buildBorrowSummary() {
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(new EmptyBorder(12, 0, 0, 0));

        lblBorrowingCount = new JLabel("0");
        lblOverdueCount = new JLabel("0");
        lblReturnedCount = new JLabel("0");

        summaryPanel.add(createSummaryCard("📚", "Đang mượn", lblBorrowingCount, UITheme.COLOR_INFO));
        summaryPanel.add(createSummaryCard("⚠️", "Quá hạn", lblOverdueCount, UITheme.COLOR_DANGER));
        summaryPanel.add(createSummaryCard("✅", "Đã trả", lblReturnedCount, UITheme.COLOR_SUCCESS));

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
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

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

                    for (Borrow b : borrows) {
                        String statusLabel = b.getStatus().getLabel();
                        borrowTableModel.addRow(new Object[]{
                            "PM-" + String.format("%04d", b.getId()),
                            b.getBookTitle(),
                            b.getBorrowDate(),
                            b.getDueDate(),
                            b.getReturnDate() != null ? b.getReturnDate() : "—",
                            statusLabel,
                            b.getRenewCount() + " lần"
                        });

                        switch (b.getStatus()) {
                            case BORROWING -> borrowing++;
                            case OVERDUE   -> overdue++;
                            case RETURNED  -> returned++;
                            default -> {}
                        }
                    }

                    // Update summary cards
                    updateSummaryCards(borrowing, overdue, returned);

                } catch (Exception ex) {
                    System.err.println("[ReaderPortal] Lỗi tải phiếu mượn: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateSummaryCards(int borrowing, int overdue, int returned) {
        if (lblBorrowingCount != null) lblBorrowingCount.setText(String.valueOf(borrowing));
        if (lblOverdueCount != null)   lblOverdueCount.setText(String.valueOf(overdue));
        if (lblReturnedCount != null)  lblReturnedCount.setText(String.valueOf(returned));
    }

    // ================================================================
    //  TAB 3: Thông Tin Cá Nhân
    // ================================================================

    private JPanel buildProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setOpaque(false);

        JLabel titleLbl = new JLabel("👤 Thông Tin Tài Khoản");
        titleLbl.setFont(UITheme.FONT_H2);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(titleLbl, BorderLayout.NORTH);

        // Card chứa thông tin
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Shadow
                g2.setColor(new Color(0, 0, 0, 10));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(32, 40, 32, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 20);
        gbc.anchor = GridBagConstraints.WEST;

        // Avatar section
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.insets = new Insets(0, 0, 0, 40);
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1565C0), getWidth(), getHeight(), new Color(0x0D47A1)));
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Initials
                g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 32));
                g2.setColor(Color.WHITE);
                String initials = getInitials(currentUser.getFullName());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initials)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initials, x, y);
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(90, 90));
        avatarPanel.setOpaque(false);
        card.add(avatarPanel, gbc);

        // Info rows
        gbc.gridheight = 1;
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.insets = new Insets(6, 0, 6, 20);

        addProfileRow(card, gbc, "Họ và tên", currentUser.getFullName());
        addProfileRow(card, gbc, "Tên đăng nhập", currentUser.getUsername());
        addProfileRow(card, gbc, "Email", currentUser.getEmail() != null ? currentUser.getEmail() : "—");
        addProfileRow(card, gbc, "Mã thẻ thư viện", currentReader != null ? currentReader.getReaderCode() : "—");
        addProfileRow(card, gbc, "Số điện thoại", currentReader != null && currentReader.getPhone() != null ? currentReader.getPhone() : "—");
        addProfileRow(card, gbc, "Ngày sinh", currentReader != null && currentReader.getBirthDate() != null ? currentReader.getBirthDate() : "—");
        addProfileRow(card, gbc, "Địa chỉ", currentReader != null && currentReader.getAddress() != null ? currentReader.getAddress() : "—");
        addProfileRow(card, gbc, "Ngày đăng ký", currentReader != null && currentReader.getJoinDate() != null ? currentReader.getJoinDate() : "—");

        String statusText = currentReader != null ? currentReader.getStatus().getLabel() : "—";
        Color statusColor = currentReader != null && currentReader.getStatus() == Reader.Status.ACTIVE
            ? UITheme.COLOR_SUCCESS : UITheme.COLOR_DANGER;

        gbc.gridy++;
        gbc.gridx = 1;
        JLabel sLabel = new JLabel("Trạng thái thẻ");
        sLabel.setFont(UITheme.FONT_BOLD);
        sLabel.setForeground(UITheme.TEXT_MUTED);
        card.add(sLabel, gbc);

        gbc.gridx = 2;
        JLabel sValue = new JLabel("● " + statusText);
        sValue.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        sValue.setForeground(statusColor);
        card.add(sValue, gbc);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void addProfileRow(JPanel card, GridBagConstraints gbc, String label, String value) {
        gbc.gridy++;
        gbc.gridx = 1;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(150, 24));
        card.add(lbl, gbc);

        gbc.gridx = 2;
        JLabel val = new JLabel(value != null && !value.isBlank() ? value : "—");
        val.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 14));
        val.setForeground(UITheme.TEXT_PRIMARY);
        card.add(val, gbc);
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

        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            if (login.isLoginSuccess()) {
                openFrameForCurrentUser();
            } else {
                DatabaseConnection.getInstance().closeConnection();
                System.exit(0);
            }
        });
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
