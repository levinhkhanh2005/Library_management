package com.example.view;

import com.example.service.AuthService;
import com.example.util.DatabaseConnection;
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

/**
 * Cửa sổ chính của ứng dụng.
 * Layout: Sidebar (trái) + Header (trên) + ContentArea (giữa)
 */
public class MainFrame extends JFrame {

    private SidebarPanel  sidebarPanel;
    private JPanel        contentArea;    // Vùng hiển thị panel chức năng
    private JLabel        pageTitleLabel; // Tiêu đề trang trên header

    // Các panel chức năng (lazy init)
    private DashboardPanel dashboardPanel;
    private BookPanel      bookPanel;
    private ReaderPanel    readerPanel;
    private BorrowPanel    borrowPanel;
    private ReportPanel    reportPanel;
    private SettingsPanel  settingsPanel;

    // ================================================================
    //  Constructor
    // ================================================================

    public MainFrame() {
        super("Quản Lý Thư Viện — Nguyễn Huệ");
        initUI();
        setupWindowEvents();

        // Mở dashboard mặc định
        showPanel(SidebarPanel.MenuItem.DASHBOARD);
    }

    // ================================================================
    //  Khởi tạo cửa sổ
    // ================================================================

    private void initUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setPreferredSize(new Dimension(1280, 760));

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
    //  Header (thanh trên cùng bên phải)
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(UITheme.BG_WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                // Đường kẻ dưới
                g.setColor(UITheme.BORDER_COLOR);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        header.setPreferredSize(new Dimension(0, UITheme.HEADER_HEIGHT));
        header.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        // Tiêu đề trang
        pageTitleLabel = new JLabel("Tổng Quan");
        pageTitleLabel.setFont(UITheme.FONT_H3);
        pageTitleLabel.setForeground(UITheme.TEXT_PRIMARY);
        header.add(pageTitleLabel, BorderLayout.WEST);

        // Phần bên phải: thông tin user + nút đăng xuất
        JPanel rightSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightSection.setOpaque(false);

        // Tên người dùng
        AuthService auth = new AuthService();
        var currentUser  = AuthService.getCurrentUser();
        String displayName = currentUser != null
            ? currentUser.getFullName() + "  |  " + currentUser.getRole().getLabel()
            : "Người dùng";

        JLabel userLabel = new JLabel(displayName);
        userLabel.setFont(UITheme.FONT_SMALL);
        userLabel.setForeground(UITheme.TEXT_SECONDARY);
        rightSection.add(userLabel);

        // Nút đăng xuất
        JButton logoutBtn = UITheme.createDangerButton("  Đăng xuất  ");
        logoutBtn.setFont(UITheme.FONT_SMALL);
        logoutBtn.setPreferredSize(new Dimension(110, 30));
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
    private void showPanel(SidebarPanel.MenuItem item) {
        sidebarPanel.setActiveItem(item);
        pageTitleLabel.setText(item.label);

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

    /** Tạo panel mới theo loại menu. */
    private JPanel createPanel(SidebarPanel.MenuItem item) {
        return switch (item) {
            case DASHBOARD -> {
                dashboardPanel = new DashboardPanel();
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
