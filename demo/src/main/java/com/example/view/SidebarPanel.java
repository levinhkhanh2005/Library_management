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
 * v2.0 — Active indicator bar, smooth hover, avatar circle, glow effects.
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
        ACTIVITY  ("🕒", "Hoạt Động"),
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
    // Hover alpha cho smooth transition
    private final java.util.Map<MenuItem, Float> hoverAlphas = new java.util.LinkedHashMap<>();

    // ================================================================
    //  Constructor
    // ================================================================

    public SidebarPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 0));
        setBackground(UITheme.BG_SIDEBAR);
        setBorder(null);

        for (MenuItem item : MenuItem.values()) {
            hoverAlphas.put(item, 0f);
        }

        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildMenuSection(),   BorderLayout.CENTER);
        add(buildBottomSection(), BorderLayout.SOUTH);
    }

    // ================================================================
    //  Phần trên: Logo — gradient header
    // ================================================================

    private JPanel buildTopSection() {
        JPanel top = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F172A),
                    getWidth(), getHeight(), new Color(0x1E293B)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        top.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 64));
        top.setBorder(new EmptyBorder(0, UITheme.PAD_MD, 0, UITheme.PAD_MD));

        // Icon + text
        JLabel iconLbl = new JLabel("📖");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel nameLbl = new JLabel("Nguyễn Huệ");
        nameLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 15));
        nameLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("Thư Viện Sách");
        subLbl.setFont(UITheme.FONT_SIDEBAR_TITLE);
        subLbl.setForeground(new Color(0x818CF8));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        textPanel.setOpaque(false);
        textPanel.add(nameLbl);
        textPanel.add(subLbl);

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        content.setOpaque(false);
        content.add(iconLbl);
        content.add(textPanel);

        top.add(content, BorderLayout.CENTER);

        // Gradient divider
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0x4F46E5, true),
                    getWidth(), 0, new Color(0x7C3AED, true)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 2));
        top.add(divider, BorderLayout.SOUTH);

        return top;
    }

    // ================================================================
    //  Phần giữa: Menu items — với active indicator
    // ================================================================

    private JPanel buildMenuSection() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(UITheme.BG_SIDEBAR);
        menu.setBorder(new EmptyBorder(UITheme.PAD_MD, 0, 0, 0));

        // Label "MENU"
        JLabel sectionLabel = new JLabel("  ĐIỀU HƯỚNG");
        sectionLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
        sectionLabel.setForeground(new Color(0x475569));
        sectionLabel.setBorder(new EmptyBorder(0, UITheme.PAD_MD, UITheme.PAD_SM, 0));
        sectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(sectionLabel);

        for (MenuItem item : MenuItem.values()) {
            JButton btn = createMenuButton(item);
            menuButtons.put(item, btn);
            btn.setAlignmentX(LEFT_ALIGNMENT);
            menu.add(btn);
            menu.add(Box.createVerticalStrut(2));
        }

        return menu;
    }

    /** Tạo nút menu bên sidebar — v2 với active indicator bar + smooth hover. */
    private JButton createMenuButton(MenuItem item) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                boolean isActive = activeItem == item;
                float hoverAlpha = hoverAlphas.getOrDefault(item, 0f);

                int x = 10, y = 2, w = getWidth() - 20, h = getHeight() - 4;

                // Active background — gradient
                if (isActive) {
                    g2.setPaint(new GradientPaint(x, 0, UITheme.ACCENT_PRIMARY,
                        x + w, 0, new Color(0x7C3AED)));
                    g2.fillRoundRect(x, y, w, h, 10, 10);
                }
                // Hover background — subtle
                else if (hoverAlpha > 0) {
                    g2.setColor(new Color(0xFF, 0xFF, 0xFF, (int)(hoverAlpha * 25)));
                    g2.fillRoundRect(x, y, w, h, 10, 10);
                }

                // Active indicator bar (left side)
                if (isActive) {
                    g2.setColor(new Color(0xA5B4FC));
                    g2.fillRoundRect(0, y + 8, 4, h - 16, 4, 4);
                }

                // Vẽ icon + text
                FontMetrics fm = g2.getFontMetrics(UITheme.FONT_SIDEBAR_ITEM);
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                // Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
                g2.setColor(isActive ? Color.WHITE : UITheme.TEXT_SIDEBAR);
                g2.drawString(item.icon, 24, textY);

                // Text
                g2.setFont(isActive
                    ? new Font(UITheme.FONT_NAME, Font.BOLD, 14)
                    : UITheme.FONT_SIDEBAR_ITEM);
                g2.setColor(isActive ? Color.WHITE : UITheme.TEXT_SIDEBAR);
                g2.drawString(item.label, 52, textY);
            }
        };

        btn.setMaximumSize(new Dimension(UITheme.SIDEBAR_WIDTH, 46));
        btn.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 46));
        btn.setMinimumSize(new Dimension(UITheme.SIDEBAR_WIDTH, 46));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Smooth hover animation
        Timer fadeIn = new Timer(16, null);
        Timer fadeOut = new Timer(16, null);

        fadeIn.addActionListener(e -> {
            float current = hoverAlphas.getOrDefault(item, 0f);
            current = Math.min(1f, current + 0.15f);
            hoverAlphas.put(item, current);
            btn.repaint();
            if (current >= 1f) fadeIn.stop();
        });

        fadeOut.addActionListener(e -> {
            float current = hoverAlphas.getOrDefault(item, 0f);
            current = Math.max(0f, current - 0.1f);
            hoverAlphas.put(item, current);
            btn.repaint();
            if (current <= 0f) fadeOut.stop();
        });

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                fadeOut.stop();
                fadeIn.start();
            }
            @Override public void mouseExited(MouseEvent e) {
                fadeIn.stop();
                fadeOut.start();
            }
        });

        btn.addActionListener(e -> {
            setActiveItem(item);
            if (menuListener != null) menuListener.onMenuSelected(item);
        });

        return btn;
    }

    // ================================================================
    //  Phần dưới: Avatar circle + tên + role
    // ================================================================

    private JPanel buildBottomSection() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(UITheme.BG_SIDEBAR);
        bottom.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Gradient divider
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0x334155),
                    getWidth(), 0, new Color(0x1E293B)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(UITheme.SIDEBAR_WIDTH, 1));
        bottom.add(divider, BorderLayout.NORTH);

        // Thông tin user
        User user = AuthService.getCurrentUser();
        String name = user != null ? user.getFullName() : "Người dùng";
        String role = user != null ? user.getRole().getLabel() : "";

        // Avatar circle
        JLabel avatarLbl = UITheme.createAvatarLabel(name, 38);

        // Name + role
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 13));
        nameLbl.setForeground(Color.WHITE);

        // Role badge
        JLabel roleLbl = new JLabel(role) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x10B981, true));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        roleLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
        roleLbl.setForeground(new Color(0x6EE7B7));
        roleLbl.setOpaque(false);
        roleLbl.setBorder(new EmptyBorder(2, 8, 2, 8));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(nameLbl);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(roleLbl);

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F172A),
                    getWidth(), getHeight(), new Color(0x1E293B)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        userInfo.setOpaque(false);
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
