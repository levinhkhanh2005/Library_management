package com.example.view;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Hệ thống thiết kế (Design System) tập trung cho toàn bộ ứng dụng.
 * Định nghĩa bảng màu, font chữ, kích thước và các factory method tạo component.
 *
 * Palette: Deep Navy + Accent Indigo + Clean White/Light Gray
 * v2.0 — Nâng cấp: shadow, gradient, animation, glassmorphism
 */
public class UITheme {

    // ================================================================
    //  BẢNG MÀU (Color Palette)
    // ================================================================

    // --- Màu nền chính ---
    public static final Color BG_PRIMARY    = new Color(0xF0F4F8);  // Nền trang nhạt
    public static final Color BG_WHITE      = new Color(0xFFFFFF);  // Card trắng
    public static final Color BG_SIDEBAR    = new Color(0x1E293B);  // Sidebar tối
    public static final Color BG_HEADER     = new Color(0x0F172A);  // Header tối hơn

    // --- Màu accent ---
    public static final Color ACCENT_PRIMARY   = new Color(0x4F46E5); // Indigo chính
    public static final Color ACCENT_HOVER     = new Color(0x4338CA); // Indigo đậm hơn (hover)
    public static final Color ACCENT_LIGHT     = new Color(0xEEF2FF); // Indigo nhạt (badge bg)
    public static final Color ACCENT_SECONDARY = new Color(0x06B6D4); // Cyan phụ

    // --- Gradient pairs ---
    public static final Color GRADIENT_START   = new Color(0x4F46E5); // Indigo
    public static final Color GRADIENT_END     = new Color(0x7C3AED); // Violet
    public static final Color GRADIENT_CYAN_START = new Color(0x06B6D4);
    public static final Color GRADIENT_CYAN_END   = new Color(0x3B82F6);

    // --- Màu trạng thái ---
    public static final Color COLOR_SUCCESS  = new Color(0x10B981); // Xanh lá — OK / Đã trả
    public static final Color COLOR_WARNING  = new Color(0xF59E0B); // Vàng — Cảnh báo / Gần hạn
    public static final Color COLOR_DANGER   = new Color(0xEF4444); // Đỏ — Nguy hiểm / Quá hạn
    public static final Color COLOR_INFO     = new Color(0x3B82F6); // Xanh — Thông tin

    public static final Color COLOR_SUCCESS_LIGHT = new Color(0xD1FAE5);
    public static final Color COLOR_WARNING_LIGHT = new Color(0xFEF3C7);
    public static final Color COLOR_DANGER_LIGHT  = new Color(0xFEE2E2);
    public static final Color COLOR_INFO_LIGHT    = new Color(0xDBEAFE);

    // --- Màu văn bản ---
    public static final Color TEXT_PRIMARY   = new Color(0x1E293B); // Chữ chính tối
    public static final Color TEXT_SECONDARY = new Color(0x64748B); // Chữ phụ xám
    public static final Color TEXT_MUTED     = new Color(0x94A3B8); // Chữ mờ
    public static final Color TEXT_WHITE     = new Color(0xFFFFFF); // Chữ trắng
    public static final Color TEXT_SIDEBAR   = new Color(0xCBD5E1); // Chữ sidebar

    // --- Màu viền ---
    public static final Color BORDER_COLOR   = new Color(0xE2E8F0); // Viền nhạt
    public static final Color BORDER_FOCUS   = ACCENT_PRIMARY;      // Viền khi focus

    // --- Màu bảng ---
    public static final Color TABLE_HEADER_BG    = new Color(0x1E293B);
    public static final Color TABLE_ROW_ODD      = new Color(0xFFFFFF);
    public static final Color TABLE_ROW_EVEN     = new Color(0xF8FAFC);
    public static final Color TABLE_ROW_HOVER    = new Color(0xEEF2FF);
    public static final Color TABLE_ROW_SELECTED = new Color(0xC7D2FE);

    // --- Sidebar item ---
    public static final Color SIDEBAR_ITEM_NORMAL  = new Color(0, 0, 0, 0);   // Trong suốt
    public static final Color SIDEBAR_ITEM_HOVER   = new Color(0xFF, 0xFF, 0xFF, 20);
    public static final Color SIDEBAR_ITEM_ACTIVE  = new Color(0x4F46E5);

    // ================================================================
    //  FONT CHỮ
    // ================================================================

    public static final String FONT_NAME = "Segoe UI";   // Windows; FlatLaf sẽ dùng Inter

    public static final Font FONT_H1     = new Font(FONT_NAME, Font.BOLD,  24);
    public static final Font FONT_H2     = new Font(FONT_NAME, Font.BOLD,  18);
    public static final Font FONT_H3     = new Font(FONT_NAME, Font.BOLD,  15);
    public static final Font FONT_BODY   = new Font(FONT_NAME, Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font(FONT_NAME, Font.PLAIN, 11);
    public static final Font FONT_BOLD   = new Font(FONT_NAME, Font.BOLD,  13);
    public static final Font FONT_BUTTON = new Font(FONT_NAME, Font.BOLD,  13);
    public static final Font FONT_TABLE  = new Font(FONT_NAME, Font.PLAIN, 13);
    public static final Font FONT_TABLE_HEADER = new Font(FONT_NAME, Font.BOLD, 13);
    public static final Font FONT_SIDEBAR_ITEM = new Font(FONT_NAME, Font.PLAIN, 14);
    public static final Font FONT_SIDEBAR_TITLE= new Font(FONT_NAME, Font.BOLD, 11);
    public static final Font FONT_STAT_VALUE   = new Font(FONT_NAME, Font.BOLD, 32);
    public static final Font FONT_STAT_TITLE   = new Font(FONT_NAME, Font.PLAIN, 12);

    // ================================================================
    //  KÍCH THƯỚC
    // ================================================================

    public static final int SIDEBAR_WIDTH       = 240;
    public static final int HEADER_HEIGHT       = 56;
    public static final int ROW_HEIGHT          = 42;
    public static final int BUTTON_HEIGHT       = 36;
    public static final int INPUT_HEIGHT        = 36;
    public static final int CARD_ARC            = 14;     // Bo góc card
    public static final int BORDER_RADIUS       = 8;      // Bo góc button/input

    // Padding / Gap
    public static final int PAD_SM  = 8;
    public static final int PAD_MD  = 16;
    public static final int PAD_LG  = 24;
    public static final int PAD_XL  = 32;

    // ================================================================
    //  SHADOW BORDER — Bóng đổ cho card
    // ================================================================

    /**
     * Custom border tạo bóng đổ nhẹ xung quanh component.
     * Dùng cho card, panel chính để tạo chiều sâu.
     */
    public static class ShadowBorder extends AbstractBorder {
        private final int shadowSize;
        private final Color shadowColor;
        private final int arc;

        public ShadowBorder(int shadowSize, Color shadowColor, int arc) {
            this.shadowSize = shadowSize;
            this.shadowColor = shadowColor;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Vẽ nhiều lớp shadow mờ dần
            for (int i = 0; i < shadowSize; i++) {
                float ratio = (float) i / shadowSize;
                int alpha = (int) (30 * (1 - ratio * ratio));
                g2.setColor(new Color(
                    shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), alpha));
                g2.drawRoundRect(x + i, y + i, width - 1 - i * 2, height - 1 - i * 2, arc, arc);
            }

            // Viền chính nhạt
            g2.setColor(new Color(0xE2E8F0));
            g2.drawRoundRect(x + shadowSize, y + shadowSize,
                width - 1 - shadowSize * 2, height - 1 - shadowSize * 2, arc, arc);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(shadowSize + 1, shadowSize + 1, shadowSize + 2, shadowSize + 2);
        }
    }

    // ================================================================
    //  KHỞI TẠO LOOK & FEEL  (gọi từ App.main trước khi tạo UI)
    // ================================================================

    /**
     * Áp dụng FlatLaf IntelliJ Light theme + các tùy chỉnh toàn cục.
     */
    public static void applyTheme() {
        try {
            // Áp dụng FlatLaf — giao diện hiện đại
            com.formdev.flatlaf.FlatLightLaf.setup();

            // Tùy chỉnh UIManager toàn cục
            UIManager.put("defaultFont",                FONT_BODY);
            UIManager.put("Button.font",                FONT_BUTTON);
            UIManager.put("Label.font",                 FONT_BODY);
            UIManager.put("TextField.font",             FONT_BODY);
            UIManager.put("TextArea.font",              FONT_BODY);
            UIManager.put("ComboBox.font",              FONT_BODY);
            UIManager.put("Table.font",                 FONT_TABLE);
            UIManager.put("TableHeader.font",           FONT_TABLE_HEADER);
            UIManager.put("TabbedPane.font",            FONT_BOLD);
            UIManager.put("TitledBorder.font",          FONT_BOLD);

            // FlatLaf specific
            UIManager.put("Button.arc",                 BORDER_RADIUS * 2);
            UIManager.put("Component.arc",              BORDER_RADIUS * 2);
            UIManager.put("TextComponent.arc",          BORDER_RADIUS * 2);
            UIManager.put("Button.background",          ACCENT_PRIMARY);
            UIManager.put("Button.foreground",          TEXT_WHITE);
            UIManager.put("Button.hoverBackground",     ACCENT_HOVER);
            UIManager.put("Button.pressedBackground",   new Color(0x3730A3));
            UIManager.put("Button.disabledBackground",  new Color(0xF1F5F9));
            UIManager.put("Button.disabledText",        new Color(0x94A3B8));
            UIManager.put("Button.disabledBorderColor", new Color(0xE2E8F0));
            UIManager.put("Focus.width",                1);
            UIManager.put("ScrollBar.width",            8);
            UIManager.put("ScrollBar.thumbArc",         999);
            UIManager.put("ScrollBar.trackArc",         999);
            UIManager.put("Table.rowHeight",            ROW_HEIGHT);
            UIManager.put("Table.showHorizontalLines",  true);
            UIManager.put("Table.showVerticalLines",    false);
            UIManager.put("Table.intercellSpacing",     new Dimension(0, 1));
            UIManager.put("Table.gridColor",            BORDER_COLOR);
            UIManager.put("TabbedPane.selectedBackground", BG_WHITE);
            UIManager.put("TabbedPane.hoverColor",      ACCENT_LIGHT);

        } catch (Exception e) {
            System.err.println("[UI] Không thể áp dụng FlatLaf, dùng mặc định: " + e.getMessage());
        }
    }

    // ================================================================
    //  GRADIENT PAINT HELPERS
    // ================================================================

    /** Tạo gradient dọc. */
    public static GradientPaint createVerticalGradient(int height, Color from, Color to) {
        return new GradientPaint(0, 0, from, 0, height, to);
    }

    /** Tạo gradient ngang. */
    public static GradientPaint createHorizontalGradient(int width, Color from, Color to) {
        return new GradientPaint(0, 0, from, width, 0, to);
    }

    // ================================================================
    //  VECTOR ICONS (Java2D Antialiased Icons — Khắc phục triệt để lỗi tofu [?])
    // ================================================================

    public static class VectorIcon implements Icon {
        public enum Type {
            PLUS, CHECK, RENEW, WARNING, DOCUMENT, EMAIL, DELETE, BELL,
            REFRESH, EDIT, FOLDER, LOCK, EXPORT, SEARCH, BOOK, USER, STATS, LIGHTNING
        }

        private final Type type;
        private final int size;
        private final Color fixedColor;

        public VectorIcon(Type type) {
            this(type, 14, null);
        }

        public VectorIcon(Type type, int size) {
            this(type, size, null);
        }

        public VectorIcon(Type type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.fixedColor = color;
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color col;
            if (fixedColor != null) {
                col = fixedColor;
            } else if (c != null && !c.isEnabled()) {
                col = new Color(0x94A3B8);
            } else if (c != null) {
                col = c.getForeground();
            } else {
                col = Color.WHITE;
            }
            g2.setColor(col);
            g2.translate(x, y);

            float s = size / 14.0f;
            g2.scale(s, s);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case PLUS -> {
                    g2.drawLine(7, 2, 7, 12);
                    g2.drawLine(2, 7, 12, 7);
                }
                case CHECK -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(2, 7.5f);
                    p.lineTo(5.5f, 11);
                    p.lineTo(12, 3);
                    g2.draw(p);
                }
                case DELETE -> {
                    g2.drawLine(3, 3, 11, 11);
                    g2.drawLine(11, 3, 3, 11);
                }
                case REFRESH, RENEW -> {
                    g2.drawArc(2, 2, 10, 10, 45, 270);
                    Path2D arrow = new Path2D.Float();
                    arrow.moveTo(8, 1);
                    arrow.lineTo(12, 3);
                    arrow.lineTo(10, 6);
                    g2.draw(arrow);
                }
                case WARNING -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(7, 1.5f);
                    p.lineTo(12.5f, 12);
                    p.lineTo(1.5f, 12);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(7, 5, 7, 8);
                    g2.fillRect(6, 10, 2, 2);
                }
                case DOCUMENT -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(3, 1);
                    p.lineTo(8.5f, 1);
                    p.lineTo(11.5f, 4);
                    p.lineTo(11.5f, 13);
                    p.lineTo(3, 13);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(5, 6, 9, 6);
                    g2.drawLine(5, 9, 9, 9);
                }
                case EMAIL -> {
                    g2.drawRoundRect(1, 2, 12, 9, 2, 2);
                    Path2D flap = new Path2D.Float();
                    flap.moveTo(1, 3);
                    flap.lineTo(7, 7.5f);
                    flap.lineTo(13, 3);
                    g2.draw(flap);
                }
                case BELL -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(7, 1.5f);
                    p.curveTo(4.5f, 1.5f, 3, 4.5f, 3, 7.5f);
                    p.lineTo(2, 9.5f);
                    p.lineTo(12, 9.5f);
                    p.lineTo(11, 7.5f);
                    p.curveTo(11, 4.5f, 9.5f, 1.5f, 7, 1.5f);
                    p.closePath();
                    g2.draw(p);
                    g2.drawArc(5, 10, 4, 3, 0, -180);
                }
                case EDIT -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(2, 12);
                    p.lineTo(5, 12);
                    p.lineTo(12, 5);
                    p.lineTo(9, 2);
                    p.lineTo(2, 9);
                    p.closePath();
                    g2.draw(p);
                    g2.drawLine(8, 3, 11, 6);
                }
                case FOLDER -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(1, 4);
                    p.lineTo(5, 4);
                    p.lineTo(6.5f, 5.5f);
                    p.lineTo(13, 5.5f);
                    p.lineTo(13, 12);
                    p.lineTo(1, 12);
                    p.closePath();
                    g2.draw(p);
                }
                case LOCK -> {
                    g2.drawRoundRect(2, 6, 10, 6, 2, 2);
                    g2.drawArc(4, 2, 6, 6, 0, 180);
                }
                case EXPORT -> {
                    g2.drawLine(7, 9, 7, 1);
                    Path2D head = new Path2D.Float();
                    head.moveTo(4, 4.5f);
                    head.lineTo(7, 1.5f);
                    head.lineTo(10, 4.5f);
                    g2.draw(head);
                    g2.drawLine(2, 12, 12, 12);
                }
                case SEARCH -> {
                    g2.drawOval(2, 2, 7, 7);
                    g2.drawLine(8, 8, 12, 12);
                }
                case BOOK -> {
                    g2.drawRoundRect(2, 2, 10, 10, 2, 2);
                    g2.drawLine(7, 2, 7, 12);
                }
                case USER -> {
                    g2.drawOval(4, 1, 6, 6);
                    g2.drawArc(1, 8, 12, 8, 0, 180);
                }
                case STATS -> {
                    g2.drawLine(3, 12, 3, 8);
                    g2.drawLine(7, 12, 7, 4);
                    g2.drawLine(11, 12, 11, 2);
                    g2.drawLine(1, 12, 13, 12);
                }
                case LIGHTNING -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(8, 1);
                    p.lineTo(3, 8);
                    p.lineTo(7, 8);
                    p.lineTo(6, 13);
                    p.lineTo(11, 6);
                    p.lineTo(7, 6);
                    p.closePath();
                    g2.fill(p);
                }
            }
            g2.dispose();
        }
    }

    /**
     * Tự động nhận diện biểu tượng Unicode ở đầu chuỗi (VD: "＋  Tạo Phiếu Mượn", "✓  Trả Sách")
     * để gán VectorIcon tương ứng và trả về text sạch (không còn ký tự lỗi).
     */
    private static void setupButtonIconAndText(JButton btn, String rawText) {
        if (rawText == null || rawText.isEmpty()) return;

        VectorIcon.Type iconType = null;
        String text = rawText.trim();

        if (text.startsWith("＋") || text.matches("^\\+\\s+.*")) {
            iconType = VectorIcon.Type.PLUS;
            text = text.replaceFirst("^[＋+]\\s*", "");
        } else if (text.startsWith("✓") || text.startsWith("✔")) {
            iconType = VectorIcon.Type.CHECK;
            text = text.replaceFirst("^[✓✔]\\s*", "");
        } else if (text.startsWith("✕") || text.startsWith("✖") || text.startsWith("✗")) {
            iconType = VectorIcon.Type.DELETE;
            text = text.replaceFirst("^[✕✖✗]\\s*", "");
        } else if (text.startsWith("⏳")) {
            iconType = VectorIcon.Type.RENEW;
            text = text.replaceFirst("^⏳\\s*", "");
        } else if (text.startsWith("⚠")) {
            iconType = VectorIcon.Type.WARNING;
            text = text.replaceFirst("^⚠\\s*", "");
        } else if (text.startsWith("📄") || text.startsWith("💾")) {
            iconType = VectorIcon.Type.DOCUMENT;
            text = text.replaceFirst("^[📄💾]\\s*", "");
        } else if (text.startsWith("📧")) {
            iconType = VectorIcon.Type.EMAIL;
            text = text.replaceFirst("^📧\\s*", "");
        } else if (text.startsWith("🔔")) {
            iconType = VectorIcon.Type.BELL;
            text = text.replaceFirst("^🔔\\s*", "");
        } else if (text.startsWith("↺") || text.startsWith("↻")) {
            iconType = VectorIcon.Type.REFRESH;
            text = text.replaceFirst("^[↺↻]\\s*", "");
        } else if (text.startsWith("✎") || text.startsWith("✏")) {
            iconType = VectorIcon.Type.EDIT;
            text = text.replaceFirst("^[✎✏]\\s*", "");
        } else if (text.startsWith("📂")) {
            iconType = VectorIcon.Type.FOLDER;
            text = text.replaceFirst("^📂\\s*", "");
        } else if (text.startsWith("🔒")) {
            iconType = VectorIcon.Type.LOCK;
            text = text.replaceFirst("^🔒\\s*", "");
        } else if (text.startsWith("📤") || text.startsWith("⬇")) {
            iconType = VectorIcon.Type.EXPORT;
            text = text.replaceFirst("^[📤⬇]\\s*", "");
        } else if (text.startsWith("🔍") || text.startsWith("👁")) {
            iconType = VectorIcon.Type.SEARCH;
            text = text.replaceFirst("^[🔍👁]\\s*", "");
        } else if (text.startsWith("⚡")) {
            iconType = VectorIcon.Type.LIGHTNING;
            text = text.replaceFirst("^⚡\\s*", "");
        }

        btn.setText(text);
        if (iconType != null) {
            btn.setIcon(new VectorIcon(iconType, 14));
            btn.setIconTextGap(7);
        }
    }

    // ================================================================
    //  FACTORY — TẠO BUTTON
    // ================================================================

    /** Nút chính (màu accent indigo). */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton();
        btn.setFont(FONT_BUTTON);
        btn.setBackground(ACCENT_PRIMARY);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setupButtonIconAndText(btn, text);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, ACCENT_PRIMARY, ACCENT_HOVER, TEXT_WHITE);
        return btn;
    }

    /** Nút nguy hiểm (màu đỏ — xóa). */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton();
        btn.setFont(FONT_BUTTON);
        btn.setBackground(COLOR_DANGER);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setupButtonIconAndText(btn, text);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, COLOR_DANGER, new Color(0xDC2626), TEXT_WHITE);
        return btn;
    }

    /** Nút thành công (màu xanh lá — thêm / trả sách). */
    public static JButton createSuccessButton(String text) {
        JButton btn = new JButton();
        btn.setFont(FONT_BUTTON);
        btn.setBackground(COLOR_SUCCESS);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setupButtonIconAndText(btn, text);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, COLOR_SUCCESS, new Color(0x059669), TEXT_WHITE);
        return btn;
    }

    /** Nút phụ (nền trắng, viền nhạt). */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton();
        btn.setFont(FONT_BUTTON);
        btn.setBackground(BG_WHITE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setupButtonIconAndText(btn, text);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, BG_WHITE, new Color(0xF1F5F9), TEXT_PRIMARY);
        return btn;
    }

    /** Nút icon nhỏ — chỉ hiện icon, có tooltip mô tả. */
    public static JButton createIconButton(String icon, String tooltip, Color bgColor, Color hoverColor) {
        JButton btn = new JButton(icon);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        btn.setBackground(bgColor);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setToolTipText(tooltip);
        addButtonHoverEffect(btn, bgColor, hoverColor, TEXT_WHITE);
        return btn;
    }

    private static void addButtonHoverEffect(JButton btn, Color normal, Color hover, Color fg) {
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hover);
            }
            @Override public void mouseExited (MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(normal);
            }
        });
        btn.addPropertyChangeListener("enabled", evt -> {
            boolean enabled = (Boolean) evt.getNewValue();
            if (enabled) {
                btn.setBackground(normal);
            }
            btn.repaint();
        });
    }

    // ================================================================
    //  FACTORY — TẠO LABEL
    // ================================================================

    public static JLabel createTitleLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_H2);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel createBodyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    public static JLabel createMutedLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    /** Badge màu theo loại: "success", "warning", "danger", "info" — kiểu pill bo tròn. */
    public static JLabel createBadge(String text, String type) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font(FONT_NAME, Font.BOLD, 11));
        lbl.setOpaque(false); // We paint ourselves
        lbl.setBorder(new EmptyBorder(3, 12, 3, 12));
        switch (type) {
            case "success" -> { lbl.setBackground(COLOR_SUCCESS_LIGHT); lbl.setForeground(new Color(0x065F46)); }
            case "warning" -> { lbl.setBackground(COLOR_WARNING_LIGHT); lbl.setForeground(new Color(0x92400E)); }
            case "danger"  -> { lbl.setBackground(COLOR_DANGER_LIGHT);  lbl.setForeground(new Color(0x991B1B)); }
            case "info"    -> { lbl.setBackground(COLOR_INFO_LIGHT);    lbl.setForeground(new Color(0x1E40AF)); }
            default        -> { lbl.setBackground(ACCENT_LIGHT);        lbl.setForeground(ACCENT_PRIMARY); }
        }
        return lbl;
    }

    // ================================================================
    //  FACTORY — TẠO INPUT
    // ================================================================

    public static JTextField createTextField(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FONT_BODY);
        tf.setForeground(TEXT_PRIMARY);
        tf.setPreferredSize(new Dimension(200, INPUT_HEIGHT));
        // FlatLaf placeholder
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        return tf;
    }

    public static JPasswordField createPasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField();
        pf.setFont(FONT_BODY);
        pf.setForeground(TEXT_PRIMARY);
        pf.setPreferredSize(new Dimension(200, INPUT_HEIGHT));
        pf.putClientProperty("JTextField.placeholderText", placeholder);
        return pf;
    }

    public static JTextArea createTextArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setFont(FONT_BODY);
        ta.setForeground(TEXT_PRIMARY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(PAD_SM, PAD_SM, PAD_SM, PAD_SM));
        return ta;
    }

    public static JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FONT_BODY);
        cb.setForeground(TEXT_PRIMARY);
        cb.setPreferredSize(new Dimension(200, INPUT_HEIGHT));
        return cb;
    }

    public static JTextField createSearchField() {
        JTextField tf = createTextField("Tìm kiếm...");
        tf.setPreferredSize(new Dimension(280, INPUT_HEIGHT));
        tf.putClientProperty("JTextField.leadingIcon", new VectorIcon(VectorIcon.Type.SEARCH, 14, TEXT_MUTED));
        tf.putClientProperty("JTextField.showClearButton", true);
        return tf;
    }

    // ================================================================
    //  FACTORY — TẠO BẢNG (JTable) — v2 với row hover
    // ================================================================

    /**
     * Áp dụng style cho JTable: header, row height, alternating rows, row hover.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_TABLE);
        table.setRowHeight(ROW_HEIGHT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(TABLE_ROW_SELECTED);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setBackground(TABLE_ROW_ODD);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Header — gradient style
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT_WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 44));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setFont(FONT_TABLE_HEADER);
                setBackground(TABLE_HEADER_BG);
                setForeground(TEXT_WHITE);
                setBorder(new EmptyBorder(0, PAD_MD, 0, PAD_SM));
                setHorizontalAlignment(col == 0 ? CENTER : LEFT);
                return this;
            }
        });

        // Row hover tracking per table
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Integer prev = (Integer) table.getClientProperty("hoveredRow");
                if (prev == null || prev != row) {
                    table.putClientProperty("hoveredRow", row);
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                table.putClientProperty("hoveredRow", -1);
                table.repaint();
            }
        });

        // Alternating row renderer + hover highlight
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setFont(FONT_TABLE);
                setBorder(new EmptyBorder(0, PAD_MD, 0, PAD_SM));
                Integer hovered = (Integer) tbl.getClientProperty("hoveredRow");
                if (isSelected) {
                    setBackground(TABLE_ROW_SELECTED);
                    setForeground(TEXT_PRIMARY);
                } else if (hovered != null && row == hovered) {
                    setBackground(TABLE_ROW_HOVER);
                    setForeground(TEXT_PRIMARY);
                } else {
                    setBackground(row % 2 == 0 ? TABLE_ROW_ODD : TABLE_ROW_EVEN);
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        });
    }

    /** JScrollPane bọc bảng (bỏ viền mặc định, thêm viền nhạt tùy chỉnh). */
    public static JScrollPane createTableScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new CompoundBorder(
            new ShadowBorder(3, new Color(0x94A3B8), CARD_ARC),
            new EmptyBorder(0, 0, 0, 0)
        ));
        sp.getViewport().setBackground(TABLE_ROW_ODD);
        return sp;
    }

    // ================================================================
    //  FACTORY — TẠO CARD / PANEL — v2 với shadow
    // ================================================================

    /**
     * Panel kiểu "card" — nền trắng, bo góc, bóng đổ nhẹ.
     * Dùng làm vùng chứa nội dung chính.
     */
    public static JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Insets insets = getInsets();
                int x = insets.left, y = insets.top;
                int w = getWidth() - insets.left - insets.right;
                int h = getHeight() - insets.top - insets.bottom;
                g2.setColor(getBackground());
                g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(BG_WHITE);
        card.setBorder(new ShadowBorder(4, new Color(0x94A3B8), CARD_ARC));
        return card;
    }

    /**
     * Glass card — semi-transparent, blur-feel. Dùng cho welcome card, overlay.
     */
    public static JPanel createGlassCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Insets insets = getInsets();
                int x = insets.left, y = insets.top;
                int w = getWidth() - insets.left - insets.right;
                int h = getHeight() - insets.top - insets.bottom;

                // Semi-transparent background
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);

                // Subtle gradient overlay
                g2.setPaint(new GradientPaint(x, y, new Color(0xEEF2FF, true),
                    x + w, y + h, new Color(0xE0E7FF, true)));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);

                // Border glow
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.setColor(new Color(0xC7D2FE));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, w - 1, h - 1, CARD_ARC, CARD_ARC);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(PAD_MD, PAD_LG, PAD_MD, PAD_LG));
        return card;
    }

    /** Stat card v2 — icon lớn, gradient accent bar, animated counter. */
    public static JPanel createStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Insets insets = getInsets();
                int x = insets.left, y = insets.top;
                int w = getWidth() - insets.left - insets.right;
                int h = getHeight() - insets.top - insets.bottom;

                // Card background
                g2.setColor(BG_WHITE);
                g2.fillRoundRect(x, y, w, h, CARD_ARC, CARD_ARC);

                // Gradient accent bar (bottom)
                int barH = 4;
                Color barEnd = accentColor.brighter();
                g2.setPaint(new GradientPaint(x, y + h - barH, accentColor, x + w, y + h - barH, barEnd));
                g2.fillRoundRect(x, y + h - barH, w, barH, barH, barH);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new ShadowBorder(4, new Color(0x94A3B8), CARD_ARC));
        card.setPreferredSize(new Dimension(200, 110));

        // Inner content
        JPanel inner = new JPanel(new BorderLayout(PAD_SM, 2));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(PAD_MD, PAD_LG, PAD_MD + 4, PAD_LG));

        // Icon circle (trái)
        Color iconBg = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25);
        JLabel iconLbl = new JLabel(getStatIcon(title), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(iconBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        iconLbl.setPreferredSize(new Dimension(48, 48));
        iconLbl.setOpaque(false);

        // Text (phải)
        JPanel textPanel = new JPanel(new BorderLayout(0, 2));
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_STAT_TITLE);
        titleLbl.setForeground(TEXT_SECONDARY);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(FONT_STAT_VALUE);
        valueLbl.setForeground(accentColor);

        textPanel.add(titleLbl, BorderLayout.NORTH);
        textPanel.add(valueLbl, BorderLayout.CENTER);

        inner.add(iconLbl, BorderLayout.WEST);
        inner.add(textPanel, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);

        return card;
    }

    /** Map title to emoji icon for stat cards. */
    private static String getStatIcon(String title) {
        if (title.contains("Sách") || title.contains("sách")) return "📚";
        if (title.contains("Mượn") || title.contains("mượn")) return "📋";
        if (title.contains("Độc Giả") || title.contains("độc giả")) return "👥";
        if (title.contains("Quá Hạn") || title.contains("quá hạn") || title.contains("Quá hạn")) return "⚠";
        return "📊";
    }

    // ================================================================
    //  ANIMATED COUNTER — Hiệu ứng đếm số
    // ================================================================

    /**
     * Animate giá trị từ 0 đến target trên JLabel.
     * @param label      Label hiển thị giá trị
     * @param target     Giá trị đích
     * @param durationMs Thời gian animation (ms)
     */
    public static void animateValue(JLabel label, int target, int durationMs) {
        if (target <= 0) {
            label.setText(String.valueOf(target));
            return;
        }
        final int fps = 30;
        final int interval = 1000 / fps;
        final int totalFrames = durationMs / interval;
        Timer timer = new Timer(interval, null);
        final int[] frame = {0};
        timer.addActionListener(e -> {
            frame[0]++;
            // Ease-out quadratic
            float t = (float) frame[0] / totalFrames;
            t = Math.min(t, 1f);
            float eased = 1 - (1 - t) * (1 - t);
            int current = Math.round(target * eased);
            label.setText(String.valueOf(current));
            if (frame[0] >= totalFrames) {
                label.setText(String.valueOf(target));
                timer.stop();
            }
        });
        timer.start();
    }

    // ================================================================
    //  FACTORY — TẠO PANEL TIÊU ĐỀ (Page Header)
    // ================================================================

    /**
     * Tạo thanh tiêu đề trang (chứa tên trang + mô tả).
     */
    public static JPanel createPageHeader(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(new EmptyBorder(0, 0, PAD_MD, 0));

        // Tách biểu tượng và tiêu đề để hiển thị icon bằng font Segoe UI Emoji tránh lỗi tofu
        String iconStr = null;
        String cleanTitle = title;
        if (title.contains("  ")) {
            int idx = title.indexOf("  ");
            iconStr = title.substring(0, idx).trim();
            cleanTitle = title.substring(idx + 2).trim();
        }

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        if (iconStr != null && !iconStr.isEmpty()) {
            JLabel iconLbl = new JLabel(iconStr);
            iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            titleRow.add(iconLbl);
        }

        JLabel titleLbl = new JLabel(cleanTitle);
        titleLbl.setFont(FONT_H2);
        titleLbl.setForeground(TEXT_PRIMARY);
        titleRow.add(titleLbl);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(FONT_SMALL);
        subLbl.setForeground(TEXT_SECONDARY);
        if (iconStr != null && !iconStr.isEmpty()) {
            subLbl.setBorder(new EmptyBorder(2, 28, 0, 0));
        }

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleRow);
        textPanel.add(subLbl);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    // ================================================================
    //  FACTORY — SEPARATOR
    // ================================================================

    public static JSeparator createSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        return sep;
    }

    // ================================================================
    //  BORDER HELPERS
    // ================================================================

    public static Border createCardBorder() {
        return new ShadowBorder(4, new Color(0x94A3B8), CARD_ARC);
    }

    public static Border createPaddedBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    // ================================================================
    //  AVATAR CIRCLE — Vẽ avatar chữ cái đầu
    // ================================================================

    /**
     * Tạo avatar hình tròn với chữ cái đầu tên, gradient background.
     */
    public static JLabel createAvatarLabel(String fullName, int size) {
        String initial = fullName != null && !fullName.isEmpty()
            ? fullName.substring(0, 1).toUpperCase() : "?";

        JLabel lbl = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Gradient circle
                g2.setPaint(new GradientPaint(0, 0, GRADIENT_START, getWidth(), getHeight(), GRADIENT_END));
                g2.fillOval(0, 0, getWidth(), getHeight());

                // Letter
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(FONT_NAME, Font.BOLD, size / 2));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        lbl.setPreferredSize(new Dimension(size, size));
        lbl.setOpaque(false);
        return lbl;
    }

    // ================================================================
    //  EMPTY STATE — Khi bảng trống
    // ================================================================

    /**
     * Panel hiển thị khi không có dữ liệu (bảng trống).
     */
    public static JPanel createEmptyState(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_WHITE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel iconLbl = new JLabel("📭", SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLbl = new JLabel(message, SwingConstants.CENTER);
        msgLbl.setFont(FONT_BODY);
        msgLbl.setForeground(TEXT_MUTED);
        msgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(iconLbl);
        content.add(Box.createVerticalStrut(PAD_SM));
        content.add(msgLbl);

        panel.add(content);
        return panel;
    }

    // ================================================================
    //  FORMAT TIỀN
    // ================================================================

    private static final NumberFormat CURRENCY_FMT =
        NumberFormat.getNumberInstance(new Locale.Builder().setLanguage("vi").setRegion("VN").build());

    /** Định dạng số tiền: 2000 → "2.000 đ". */
    public static String formatCurrency(double amount) {
        return CURRENCY_FMT.format((long) amount) + " đ";
    }

    // ================================================================
    //  DIALOG HELPER
    // ================================================================

    /** Hiển thị dialog thông báo lỗi. */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /** Hiển thị dialog thông báo thành công. */
    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Hiển thị dialog xác nhận (Yes/No). Trả về true nếu người dùng chọn Yes. */
    public static boolean showConfirm(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(
            parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /** Hiển thị dialog cảnh báo. */
    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }
}
