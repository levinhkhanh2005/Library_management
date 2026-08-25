package com.example.view;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Hệ thống thiết kế (Design System) tập trung cho toàn bộ ứng dụng.
 * Định nghĩa bảng màu, font chữ, kích thước và các factory method tạo component.
 *
 * Palette: Deep Navy + Accent Indigo + Clean White/Light Gray
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

    // --- Màu trạng thái ---
    public static final Color COLOR_SUCCESS  = new Color(0x10B981); // Xanh lá — OK / Đã trả
    public static final Color COLOR_WARNING  = new Color(0xF59E0B); // Vàng — Cảnh báo / Gần hạn
    public static final Color COLOR_DANGER   = new Color(0xEF4444); // Đỏ — Nguy hiểm / Quá hạn
    public static final Color COLOR_INFO     = new Color(0x3B82F6); // Xanh — Thông tin

    public static final Color COLOR_SUCCESS_LIGHT = new Color(0xD1FAE5);
    public static final Color COLOR_WARNING_LIGHT = new Color(0xFEF3C7);
    public static final Color COLOR_DANGER_LIGHT  = new Color(0xFEE2E2);

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

    // ================================================================
    //  KÍCH THƯỚC
    // ================================================================

    public static final int SIDEBAR_WIDTH       = 220;
    public static final int HEADER_HEIGHT       = 56;
    public static final int ROW_HEIGHT          = 40;
    public static final int BUTTON_HEIGHT       = 36;
    public static final int INPUT_HEIGHT        = 34;
    public static final int CARD_ARC            = 12;     // Bo góc card
    public static final int BORDER_RADIUS       = 8;      // Bo góc button/input

    // Padding / Gap
    public static final int PAD_SM  = 8;
    public static final int PAD_MD  = 16;
    public static final int PAD_LG  = 24;
    public static final int PAD_XL  = 32;

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
            UIManager.put("Focus.width",                1);
            UIManager.put("ScrollBar.width",            8);
            UIManager.put("Table.rowHeight",            ROW_HEIGHT);
            UIManager.put("Table.showHorizontalLines",  true);
            UIManager.put("Table.showVerticalLines",    false);
            UIManager.put("Table.intercellSpacing",     new Dimension(0, 1));
            UIManager.put("Table.gridColor",            BORDER_COLOR);

        } catch (Exception e) {
            System.err.println("[UI] Không thể áp dụng FlatLaf, dùng mặc định: " + e.getMessage());
        }
    }

    // ================================================================
    //  FACTORY — TẠO BUTTON
    // ================================================================

    /** Nút chính (màu accent indigo). */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(ACCENT_PRIMARY);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, ACCENT_PRIMARY, ACCENT_HOVER, TEXT_WHITE);
        return btn;
    }

    /** Nút nguy hiểm (màu đỏ — xóa). */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(COLOR_DANGER);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, COLOR_DANGER, new Color(0xDC2626), TEXT_WHITE);
        return btn;
    }

    /** Nút thành công (màu xanh lá — thêm / trả sách). */
    public static JButton createSuccessButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(COLOR_SUCCESS);
        btn.setForeground(TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, COLOR_SUCCESS, new Color(0x059669), TEXT_WHITE);
        return btn;
    }

    /** Nút phụ (nền trắng, viền nhạt). */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setBackground(BG_WHITE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, BUTTON_HEIGHT));
        addButtonHoverEffect(btn, BG_WHITE, new Color(0xF1F5F9), TEXT_PRIMARY);
        return btn;
    }

    private static void addButtonHoverEffect(JButton btn, Color normal, Color hover, Color fg) {
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(normal); }
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

    /** Badge màu theo loại: "success", "warning", "danger", "info". */
    public static JLabel createBadge(String text, String type) {
        JLabel lbl = new JLabel(" " + text + " ");
        lbl.setFont(FONT_SMALL);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(2, 8, 2, 8));
        switch (type) {
            case "success" -> { lbl.setBackground(COLOR_SUCCESS_LIGHT); lbl.setForeground(COLOR_SUCCESS); }
            case "warning" -> { lbl.setBackground(COLOR_WARNING_LIGHT); lbl.setForeground(new Color(0xB45309)); }
            case "danger"  -> { lbl.setBackground(COLOR_DANGER_LIGHT);  lbl.setForeground(COLOR_DANGER); }
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
        JTextField tf = createTextField("🔍  Tìm kiếm...");
        tf.setPreferredSize(new Dimension(260, INPUT_HEIGHT));
        return tf;
    }

    // ================================================================
    //  FACTORY — TẠO BẢNG (JTable)
    // ================================================================

    /**
     * Áp dụng style cho JTable: header, row height, alternating rows.
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

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT_WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setReorderingAllowed(false);

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setFont(FONT_TABLE);
                setBorder(new EmptyBorder(0, PAD_MD, 0, PAD_SM));
                if (isSelected) {
                    setBackground(TABLE_ROW_SELECTED);
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
        sp.setBorder(new LineBorder(BORDER_COLOR, 1));
        sp.getViewport().setBackground(TABLE_ROW_ODD);
        return sp;
    }

    // ================================================================
    //  FACTORY — TẠO CARD / PANEL
    // ================================================================

    /**
     * Panel kiểu "card" — nền trắng, bo góc, bóng đổ nhẹ.
     * Dùng làm vùng chứa nội dung chính.
     */
    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(BG_WHITE);
        card.setBorder(createCardBorder());
        return card;
    }

    /** Stat card — hiển thị một con số thống kê. */
    public static JPanel createStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, PAD_SM));
        card.setBackground(BG_WHITE);
        card.setBorder(createCardBorder());
        card.setPreferredSize(new Dimension(180, 90));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_SMALL);
        titleLbl.setForeground(TEXT_SECONDARY);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font(FONT_NAME, Font.BOLD, 28));
        valueLbl.setForeground(accentColor);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(BG_WHITE);
        inner.setBorder(new EmptyBorder(PAD_MD, PAD_MD, PAD_MD, PAD_MD));
        inner.add(titleLbl, BorderLayout.NORTH);
        inner.add(valueLbl, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);

        // Thanh màu bên trái
        JPanel colorBar = new JPanel();
        colorBar.setPreferredSize(new Dimension(5, 0));
        colorBar.setBackground(accentColor);
        card.add(colorBar, BorderLayout.WEST);

        return card;
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

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_H2);
        titleLbl.setForeground(TEXT_PRIMARY);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(FONT_SMALL);
        subLbl.setForeground(TEXT_SECONDARY);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setBackground(BG_PRIMARY);
        textPanel.add(titleLbl);
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
        return new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(0, 0, 0, 0)
        );
    }

    public static Border createPaddedBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    // ================================================================
    //  FORMAT TIỀN
    // ================================================================

    private static final NumberFormat CURRENCY_FMT =
        NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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
