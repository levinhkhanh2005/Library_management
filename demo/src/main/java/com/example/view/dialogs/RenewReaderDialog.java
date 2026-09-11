package com.example.view.dialogs;

import com.example.model.Reader;
import com.example.service.ReaderService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dialog gia hạn thẻ độc giả.
 * Cho phép chọn thời hạn gia hạn (3 / 6 / 12 tháng) và xem trước ngày hết hạn mới.
 */
public class RenewReaderDialog extends JDialog {

    private final ReaderService readerService = new ReaderService();
    private final Reader        reader;
    private boolean             renewed = false;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Tùy chọn gia hạn
    private static final int[]    MONTHS_VALUES = {3, 6, 12};
    private static final String[] MONTHS_LABELS = {"3 tháng", "6 tháng", "12 tháng"};

    private JComboBox<String> cbDuration;
    private JLabel            lblNewExpiry;
    private JLabel            lblOldExpiry;
    private JLabel            lblStatus;

    public RenewReaderDialog(Frame parent, Reader reader) {
        super(parent, "Gia Hạn Thẻ Độc Giả", true);
        this.reader = reader;
        initUI();
        updatePreview();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    // ================================================================
    //  Xây dựng UI
    // ================================================================

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildForm(),     BorderLayout.CENTER);
        root.add(buildButtons(),  BorderLayout.SOUTH);
    }

    /** Thanh tiêu đề màu teal / cyan nổi bật, khác Thêm và Sửa. */
    private JPanel buildTitleBar() {
        Color teal = new Color(0x00, 0x89, 0x8A);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(teal);
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel("🔄  Gia Hạn Thẻ Độc Giả");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel("Gia hạn sẽ tự động kích hoạt lại thẻ nếu đã hết hạn");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xFF, 0xFF, 0xFF, 200));
        bar.add(sub, BorderLayout.SOUTH);

        return bar;
    }

    private JPanel buildForm() {
        JPanel outer = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        outer.setBackground(UITheme.BG_WHITE);
        outer.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        // --- Nhóm: Thông tin độc giả ---
        JPanel infoGroup = createGroup("Thông Tin Độc Giả");
        infoGroup.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Mã thẻ
        g.gridy = 0; g.gridx = 0; g.weightx = 0.3;
        infoGroup.add(label("Mã thẻ:"), g);
        g.gridx = 1; g.weightx = 0.7;
        JLabel lblCode = new JLabel(reader.getReaderCode());
        lblCode.setFont(UITheme.FONT_BOLD);
        lblCode.setForeground(UITheme.ACCENT_PRIMARY);
        infoGroup.add(lblCode, g);

        // Họ tên
        g.gridy = 1; g.gridx = 0; g.weightx = 0.3;
        infoGroup.add(label("Họ tên:"), g);
        g.gridx = 1; g.weightx = 0.7;
        infoGroup.add(new JLabel(reader.getFullName()), g);

        // Trạng thái hiện tại
        g.gridy = 2; g.gridx = 0; g.weightx = 0.3;
        infoGroup.add(label("Trạng thái:"), g);
        g.gridx = 1; g.weightx = 0.7;
        lblStatus = new JLabel(reader.getStatus().getLabel());
        lblStatus.setFont(UITheme.FONT_BOLD);
        lblStatus.setForeground(statusColor(reader.getStatus()));
        infoGroup.add(lblStatus, g);

        // Hạn thẻ hiện tại
        g.gridy = 3; g.gridx = 0; g.weightx = 0.3;
        infoGroup.add(label("Hạn thẻ hiện tại:"), g);
        g.gridx = 1; g.weightx = 0.7;
        String oldExp = reader.getExpiryDate();
        lblOldExpiry = new JLabel(oldExp != null && !oldExp.isBlank() ? oldExp : "Chưa có");
        lblOldExpiry.setFont(UITheme.FONT_BOLD);
        lblOldExpiry.setForeground(reader.isExpiredByDate()
            ? UITheme.COLOR_DANGER : UITheme.TEXT_PRIMARY);
        infoGroup.add(lblOldExpiry, g);

        outer.add(infoGroup, BorderLayout.NORTH);

        // --- Nhóm: Chọn thời hạn gia hạn ---
        JPanel renewGroup = createGroup("Thời Hạn Gia Hạn");
        renewGroup.setLayout(new GridBagLayout());
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(8, 10, 8, 10);
        g2.fill   = GridBagConstraints.HORIZONTAL;

        // ComboBox chọn mốc gia hạn
        g2.gridy = 0; g2.gridx = 0; g2.weightx = 0.35;
        renewGroup.add(label("Gia hạn thêm:"), g2);
        g2.gridx = 1; g2.weightx = 0.65;
        cbDuration = new JComboBox<>(MONTHS_LABELS);
        cbDuration.setFont(UITheme.FONT_BODY);
        cbDuration.setPreferredSize(new Dimension(180, UITheme.INPUT_HEIGHT));
        cbDuration.setSelectedIndex(2); // mặc định 12 tháng
        cbDuration.addActionListener(e -> updatePreview());
        renewGroup.add(cbDuration, g2);

        // Preview ngày hết hạn mới
        g2.gridy = 1; g2.gridx = 0; g2.weightx = 0.35;
        renewGroup.add(label("Hết hạn mới:"), g2);
        g2.gridx = 1; g2.weightx = 0.65;
        lblNewExpiry = new JLabel("--/--/----");
        lblNewExpiry.setFont(UITheme.FONT_BOLD.deriveFont(16f));
        lblNewExpiry.setForeground(new Color(0x00, 0x89, 0x8A));
        renewGroup.add(lblNewExpiry, g2);

        outer.add(renewGroup, BorderLayout.CENTER);

        return outer;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.PAD_SM, UITheme.PAD_MD));
        panel.setBackground(UITheme.BG_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG)
        ));

        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnRenew  = UITheme.createSuccessButton("  🔄  Gia Hạn  ");

        btnCancel.setPreferredSize(new Dimension(100, UITheme.BUTTON_HEIGHT));
        btnRenew .setPreferredSize(new Dimension(150, UITheme.BUTTON_HEIGHT));
        btnRenew.setBackground(new Color(0x00, 0x89, 0x8A));

        btnCancel.addActionListener(e -> dispose());
        btnRenew .addActionListener(e -> doRenew());

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        panel.add(btnCancel);
        panel.add(btnRenew);
        return panel;
    }

    // ================================================================
    //  Logic
    // ================================================================

    /** Cập nhật label preview ngày hết hạn mới khi đổi lựa chọn. */
    private void updatePreview() {
        int months = MONTHS_VALUES[cbDuration.getSelectedIndex()];

        LocalDate base = LocalDate.now();
        String oldExp  = reader.getExpiryDate();
        if (oldExp != null && !oldExp.isBlank()) {
            try {
                LocalDate oldDate = LocalDate.parse(oldExp.trim(), DATE_FMT);
                if (oldDate.isAfter(base)) base = oldDate;
            } catch (Exception ignored) { }
        }

        lblNewExpiry.setText(base.plusMonths(months).format(DATE_FMT));
    }

    private void doRenew() {
        int months = MONTHS_VALUES[cbDuration.getSelectedIndex()];
        String durationLabel = MONTHS_LABELS[cbDuration.getSelectedIndex()];

        boolean confirm = UITheme.showConfirm(this,
            "Gia hạn thẻ \"" + reader.getFullName() + "\" thêm " + durationLabel + "?\n"
            + "Hạn mới: " + lblNewExpiry.getText(),
            "Xác nhận gia hạn");
        if (!confirm) return;

        try {
            String newExpiry = readerService.renewCard(reader, months);
            UITheme.showSuccess(this,
                "Gia hạn thành công!\n"
                + "Độc giả: " + reader.getFullName() + "\n"
                + "Hạn thẻ mới: " + newExpiry);
            renewed = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi gia hạn:\n" + ex.getMessage());
        }
    }

    // ================================================================
    //  Helper UI
    // ================================================================

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        return lbl;
    }

    private JPanel createGroup(String title) {
        JPanel grp = new JPanel();
        grp.setBackground(UITheme.BG_WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR), "  " + title + "  ");
        border.setTitleFont(UITheme.FONT_BOLD);
        border.setTitleColor(UITheme.TEXT_SECONDARY);
        grp.setBorder(border);
        return grp;
    }

    private Color statusColor(Reader.Status status) {
        return switch (status) {
            case ACTIVE  -> UITheme.COLOR_SUCCESS;
            case LOCKED  -> UITheme.COLOR_DANGER;
            case EXPIRED -> UITheme.COLOR_WARNING;
        };
    }

    /** Trả về true nếu đã gia hạn thành công. */
    public boolean isRenewed() { return renewed; }
}
