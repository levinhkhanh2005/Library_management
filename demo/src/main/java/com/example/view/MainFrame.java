package com.example.view;

import com.example.service.AuthService;
import com.example.service.BorrowService;
import com.example.util.DatabaseConnection;
import com.example.view.panels.ActivityPanel;
import com.example.view.panels.BookPanel;
import com.example.view.panels.BorrowPanel;
import com.example.view.panels.DashboardPanel;
import com.example.view.panels.ReaderPanel;
import com.example.view.panels.ReportPanel;
import com.example.view.panels.SettingsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cửa sổ chính của ứng dụng.
 * Layout: Sidebar (trái) + Header (trên) + ContentArea (giữa)
 * v2.0 — Modern header with avatar, breadcrumb, clock, notification dot.
 */
public class MainFrame extends JFrame {

    private SidebarPanel  sidebarPanel;
    private JPanel        contentArea;    // Vùng hiển thị panel chức năng
    private JLabel        pageTitleLabel; // Tiêu đề trang trên header
    private JLabel        breadcrumbLabel;
    private JLabel        clockLabel;
    private JPanel        notificationDot;

    // Các panel chức năng (lazy init)
    private DashboardPanel dashboardPanel;
    private BookPanel      bookPanel;
    private ReaderPanel    readerPanel;
    private BorrowPanel    borrowPanel;
    private ReportPanel    reportPanel;
    private ActivityPanel  activityPanel;
    private SettingsPanel  settingsPanel;

    // ================================================================
    //  Constructor
    // ================================================================

    public MainFrame() {
        super("Quản Lý Thư Viện — Nguyễn Huệ");
        initUI();
        setupWindowEvents();
        startClock();

        // Mở dashboard mặc định
        showPanel(SidebarPanel.MenuItem.DASHBOARD);
    }

    // ================================================================
    //  Khởi tạo cửa sổ
    // ================================================================

    private void initUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setPreferredSize(new Dimension(1320, 780));

        // Icon ứng dụng (text emoji làm icon)
        try {
            java.awt.image.BufferedImage icon =
                new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            g.drawString("📖", 2, 26);
            g.dispose();
            setIconImage(icon);
        } catch (Exception ignored) {}

        // Layout tổng thể: sidebar | (header + content)
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_PRIMARY);
        setContentPane(root);

        // Sidebar
        sidebarPanel = new SidebarPanel();
        sidebarPanel.setMenuListener(this::showPanel);
        root.add(sidebarPanel, BorderLayout.WEST);

        // Phần phải: header + content
        JPanel rightPane = new JPanel(new BorderLayout(0, 0));
        rightPane.setBackground(UITheme.BG_PRIMARY);
        rightPane.add(buildHeader(), BorderLayout.NORTH);
        rightPane.add(buildContent(), BorderLayout.CENTER);
        root.add(rightPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    // ================================================================
    //  Header (thanh trên cùng bên phải) — v2 modern
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(UITheme.BG_WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Bottom shadow
                for (int i = 0; i < 3; i++) {
                    g2.setColor(new Color(0, 0, 0, 8 - i * 2));
                    g2.fillRect(0, getHeight() - 1 + i, getWidth(), 1);
                }
            }
        };
        header.setPreferredSize(new Dimension(0, UITheme.HEADER_HEIGHT));
        header.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        // --- Bên trái: Breadcrumb + page title ---
        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSection.setOpaque(false);

        // Breadcrumb
        breadcrumbLabel = new JLabel("Thư Viện  /  ");
        breadcrumbLabel.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        breadcrumbLabel.setForeground(UITheme.TEXT_MUTED);

        pageTitleLabel = new JLabel("Tổng Quan");
        pageTitleLabel.setFont(UITheme.FONT_H3);
        pageTitleLabel.setForeground(UITheme.TEXT_PRIMARY);

        leftSection.add(breadcrumbLabel);
        leftSection.add(pageTitleLabel);
        header.add(leftSection, BorderLayout.WEST);

        // --- Bên phải: clock + notification + user + logout ---
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightSection.setOpaque(false);

        // Clock
        clockLabel = new JLabel();
        clockLabel.setFont(UITheme.FONT_SMALL);
        clockLabel.setForeground(UITheme.TEXT_MUTED);
        updateClock();
        rightSection.add(clockLabel);

        // Separator
        JLabel sep = new JLabel("│");
        sep.setForeground(UITheme.BORDER_COLOR);
        rightSection.add(sep);

        // Notification indicator (overdue dot)
        JPanel notifWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        notifWrapper.setOpaque(false);
        JLabel bellLbl = new JLabel("🔔");
        bellLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        bellLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellLbl.setToolTipText("Thông báo");

        notificationDot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.COLOR_DANGER);
                g2.fillOval(0, 0, 8, 8);
                g2.dispose();
            }
        };
        notificationDot.setPreferredSize(new Dimension(8, 8));
        notificationDot.setOpaque(false);
        notificationDot.setVisible(false);
        checkOverdueNotification();

        notifWrapper.add(bellLbl);
        notifWrapper.add(notificationDot);
        rightSection.add(notifWrapper);

        // Separator
        JLabel sep2 = new JLabel("│");
        sep2.setForeground(UITheme.BORDER_COLOR);
        rightSection.add(sep2);

        // Avatar + tên người dùng
        AuthService auth = new AuthService();
        var currentUser  = AuthService.getCurrentUser();
        String displayName = currentUser != null ? currentUser.getFullName() : "Người dùng";
        String roleName    = currentUser != null ? currentUser.getRole().getLabel() : "";

        JLabel avatarLbl = UITheme.createAvatarLabel(displayName, 32);
        rightSection.add(avatarLbl);

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        JLabel nameLbl = new JLabel(displayName);
        nameLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        nameLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel roleLbl = new JLabel(roleName);
        roleLbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
        roleLbl.setForeground(UITheme.TEXT_MUTED);

        namePanel.add(nameLbl);
        namePanel.add(roleLbl);
        rightSection.add(namePanel);

        // Nút đăng xuất
        JButton logoutBtn = UITheme.createDangerButton("Đăng xuất");
        logoutBtn.setFont(UITheme.FONT_SMALL);
        logoutBtn.setPreferredSize(new Dimension(100, 30));
        logoutBtn.addActionListener(e -> doLogout());
        rightSection.add(logoutBtn);

        header.add(rightSection, BorderLayout.EAST);
        return header;
    }

    // ================================================================
    //  Content area (CardLayout)
    // ================================================================

    private JPanel buildContent() {
        contentArea = new JPanel(new CardLayout());
        contentArea.setBackground(UITheme.BG_PRIMARY);
        contentArea.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG,
                                              UITheme.PAD_LG, UITheme.PAD_LG));
        return contentArea;
    }

    // ================================================================
    //  Điều hướng
    // ================================================================

    /**
     * Hiển thị panel tương ứng với menu item được chọn.
     * Dùng lazy init — chỉ tạo panel khi cần lần đầu.
     */
    public void showPanel(SidebarPanel.MenuItem item) {
        sidebarPanel.setActiveItem(item);
        pageTitleLabel.setText(item.label);
        breadcrumbLabel.setText("Thư Viện  /  ");

        String key = item.name();
        CardLayout cl = (CardLayout) contentArea.getLayout();

        // Kiểm tra panel đã được thêm chưa
        if (contentArea.getComponentCount() == 0 ||
            getCardComponent(key) == null) {

            JPanel panel = createPanel(item);
            contentArea.add(panel, key);
        } else {
            // Refresh dữ liệu khi quay lại panel đã mở
            JPanel panel = getCardComponent(key);
            if (panel instanceof Refreshable) {
                ((Refreshable) panel).refresh();
            }
        }

        cl.show(contentArea, key);
    }

    /** Quick action: chuyển đến Quản lý Sách và mở dialog thêm sách mới. */
    public void quickActionAddBook() {
        showPanel(SidebarPanel.MenuItem.BOOKS);
        if (bookPanel != null) {
            SwingUtilities.invokeLater(() -> bookPanel.openAddDialog());
        }
    }

    /** Quick action: chuyển đến Mượn/Trả và mở dialog tạo phiếu mượn mới. */
    public void quickActionAddBorrow() {
        showPanel(SidebarPanel.MenuItem.BORROWS);
        if (borrowPanel != null) {
            SwingUtilities.invokeLater(() -> borrowPanel.openBorrowDialog());
        }
    }

    /** Tạo panel mới theo loại menu. */
    private JPanel createPanel(SidebarPanel.MenuItem item) {
        return switch (item) {
            case DASHBOARD -> {
                dashboardPanel = new DashboardPanel(this);
                yield dashboardPanel;
            }
            case BOOKS -> {
                bookPanel = new BookPanel();
                yield bookPanel;
            }
            case READERS -> {
                readerPanel = new ReaderPanel();
                yield readerPanel;
            }
            case BORROWS -> {
                borrowPanel = new BorrowPanel();
                yield borrowPanel;
            }
            case REPORT -> {
                reportPanel = new ReportPanel();
                yield reportPanel;
            }
            case ACTIVITY -> {
                activityPanel = new ActivityPanel();
                yield activityPanel;
            }
            case SETTINGS -> {
                settingsPanel = new SettingsPanel();
                yield settingsPanel;
            }
        };
    }

    /** Tìm component trong CardLayout theo key. */
    private JPanel getCardComponent(String key) {
        for (Component c : contentArea.getComponents()) {
            if (key.equals(c.getName())) return (JPanel) c;
        }
        return null;
    }

    // ================================================================
    //  Clock — Đồng hồ header
    // ================================================================

    private void startClock() {
        Timer clockTimer = new Timer(30_000, e -> updateClock());
        clockTimer.start();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm  •  dd/MM/yyyy");
        if (clockLabel != null) {
            clockLabel.setText(now.format(fmt));
        }
    }

    // ================================================================
    //  Notification — Kiểm tra quá hạn
    // ================================================================

    private void checkOverdueNotification() {
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override protected Integer doInBackground() {
                try {
                    return new BorrowService().getOverdueBorrowCount();
                } catch (Exception e) { return 0; }
            }
            @Override protected void done() {
                try {
                    int count = get();
                    if (notificationDot != null) {
                        notificationDot.setVisible(count > 0);
                        notificationDot.setToolTipText(count > 0
                            ? count + " phiếu mượn quá hạn" : null);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    // ================================================================
    //  Đăng xuất
    // ================================================================

    private void doLogout() {
        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn đăng xuất không?", "Xác nhận đăng xuất");
        if (!confirm) return;

        new AuthService().logout();
        dispose();

        // Mở lại màn hình đăng nhập
        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            if (login.isLoginSuccess()) {
                new MainFrame().setVisible(true);
            } else {
                DatabaseConnection.getInstance().closeConnection();
                System.exit(0);
            }
        });
    }

    // ================================================================
    //  Window Events
    // ================================================================

    private void setupWindowEvents() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                boolean confirm = UITheme.showConfirm(MainFrame.this,
                    "Bạn có muốn thoát ứng dụng không?", "Xác nhận thoát");
                if (confirm) {
                    DatabaseConnection.getInstance().closeConnection();
                    System.exit(0);
                }
            }
        });
    }

    // ================================================================
    //  Interface Refreshable (dùng cho các Panel cần reload)
    // ================================================================

    /** Panel nào implement interface này sẽ được gọi refresh() khi người dùng quay lại. */
    public interface Refreshable {
        void refresh();
    }
}
