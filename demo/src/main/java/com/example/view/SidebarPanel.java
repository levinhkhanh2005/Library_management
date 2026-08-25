package com.example.view;

import com.example.service.AuthService;
import com.example.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Sidebar (panel bên trái) với các mục menu điều hướng.
 * Phát sự kiện khi người dùng chọn một mục để MainFrame chuyển panel.
 */
public class SidebarPanel extends JPanel {

    // ================================================================
    //  Enum các mục menu
    // ================================================================

    public enum MenuItem {
        DASHBOARD ("🏠", "Tổng Quan"),
        BOOKS     ("📚", "Quản Lý Sách"),
        READERS   ("👤", "Độc Giả"),
        BORROWS   ("📋", "Mượn / Trả"),
        REPORT    ("📊", "Báo Cáo"),
        SETTINGS  ("⚙", "Cài Đặt");

        public final String icon;
        public final String label;
        MenuItem(String icon, String label) { this.icon = icon; this.label = label; }
    }

    // ================================================================
    //  State & Listener
    // ================================================================

    private MenuItem activeItem = MenuItem.DASHBOARD;

    /** Callback khi người dùng click vào menu item. */
    public interface MenuListener {
        void onMenuSelected(MenuItem item);
    }
    private MenuListener menuListener;

    // Lưu nút theo enum để update active state
    private final java.util.Map<MenuItem, JButton> menuButtons = new java.util.LinkedHashMap<>();

    // ================================================================
    //  Constructor
    // ================================================================

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 0));
        setBackground(UITheme.BG_SIDEBAR);
        setBorder(null);

        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildMenuSection(),   BorderLayout.CENTER);
        add(buildBottomSection(), BorderLayout.SOUTH);
    }

    // ================================================================
    //  Phần trên: Logo
    // ================================================================

    private JPanel buildTopSection() {
        JPanel top = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0x0F172A));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        top.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, UITheme.HEADER_HEIGHT));
        top.setBorder(new EmptyBorder(0, UITheme.PAD_MD, 0, UITheme.PAD_MD));

        JLabel iconLbl = new JLabel("📖");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel nameLbl = new JLabel("Nguyễn Huệ");
        nameLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        nameLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("Thư Viện Sách");
        subLbl.setFont(UITheme.FONT_SIDEBAR_TITLE);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        textPanel.setOpaque(false);
        textPanel.add(nameLbl);
        textPanel.add(subLbl);

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        content.setOpaque(false);
        content.add(iconLbl);
        content.add(textPanel);

        top.add(content, BorderLayout.CENTER);

        // Đường kẻ bên dưới
        JPanel divider = new JPanel();
        divider.setBackground(new Color(0xFF, 0xFF, 0xFF, 20));
        divider.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 1));
        top.add(divider, BorderLayout.SOUTH);

        return top;
    }

    // ================================================================
    //  Phần giữa: Menu items
    // ================================================================

    private JPanel buildMenuSection() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(UITheme.BG_SIDEBAR);
        menu.setBorder(new EmptyBorder(UITheme.PAD_SM, 0, 0, 0));

        // Label "MENU"
        JLabel sectionLabel = new JLabel("  MENU");
        sectionLabel.setFont(UITheme.FONT_SIDEBAR_TITLE);
        sectionLabel.setForeground(new Color(0x475569));
        sectionLabel.setBorder(new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD, UITheme.PAD_SM, 0));
        sectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(sectionLabel);

        for (MenuItem item : MenuItem.values()) {
            JButton btn = createMenuButton(item);
            menuButtons.put(item, btn);
            btn.setAlignmentX(LEFT_ALIGNMENT);
            menu.add(btn);
        }

        return menu;
    }

    /** Tạo nút menu bên sidebar. */
    private JButton createMenuButton(MenuItem item) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Nền active
                if (activeItem == item) {
                    g2.setColor(UITheme.ACCENT_PRIMARY);
                    g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 10, 10);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(0xFF, 0xFF, 0xFF, 18));
                    g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 10, 10);
                }

                // Vẽ icon + text
                FontMetrics fm = g2.getFontMetrics(UITheme.FONT_SIDEBAR_ITEM);
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                // Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                g2.setColor(activeItem == item ? Color.WHITE : UITheme.TEXT_SIDEBAR);
                g2.drawString(item.icon, 20, y);

                // Text
                g2.setFont(UITheme.FONT_SIDEBAR_ITEM);
                g2.setColor(activeItem == item ? Color.WHITE : UITheme.TEXT_SIDEBAR);
                g2.drawString(item.label, 48, y);
            }
        };

        btn.setMaximumSize(new Dimension(UITheme.SIDEBAR_WIDTH, 44));
        btn.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 44));
        btn.setMinimumSize(new Dimension(UITheme.SIDEBAR_WIDTH, 44));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.repaint(); }
            @Override public void mouseExited (MouseEvent e) { btn.repaint(); }
        });

        btn.addActionListener(e -> {
            setActiveItem(item);
            if (menuListener != null) menuListener.onMenuSelected(item);
        });

        return btn;
    }

    // ================================================================
    //  Phần dưới: Thông tin người dùng + Đăng xuất
    // ================================================================

    private JPanel buildBottomSection() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UITheme.BG_SIDEBAR);
        bottom.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Đường kẻ trên
        JPanel divider = new JPanel();
        divider.setBackground(new Color(0xFF, 0xFF, 0xFF, 20));
        divider.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 1));
        bottom.add(divider, BorderLayout.NORTH);

        // Thông tin user
        User user = AuthService.getCurrentUser();
        String name = user != null ? user.getFullName() : "Người dùng";
        String role = user != null ? user.getRole().getLabel() : "";

        JLabel avatarLbl = new JLabel("👤");
        avatarLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(UITheme.FONT_BOLD);
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setMaximumSize(new Dimension(130, 20));

        JLabel roleLbl = new JLabel(role);
        roleLbl.setFont(UITheme.FONT_SMALL);
        roleLbl.setForeground(new Color(0x6EE7B7)); // xanh nhạt cho role

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        textPanel.setOpaque(false);
        textPanel.add(nameLbl);
        textPanel.add(roleLbl);

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        userInfo.setBackground(new Color(0x0F172A));
        userInfo.add(avatarLbl);
        userInfo.add(textPanel);
        bottom.add(userInfo, BorderLayout.CENTER);

        return bottom;
    }

    // ================================================================
    //  API công khai
    // ================================================================

    public void setMenuListener(MenuListener listener) {
        this.menuListener = listener;
    }

    public void setActiveItem(MenuItem item) {
        this.activeItem = item;
        menuButtons.forEach((k, btn) -> btn.repaint());
    }

    public MenuItem getActiveItem() {
        return activeItem;
    }
}
