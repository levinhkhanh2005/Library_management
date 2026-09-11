package com.example.view.dialogs;

import com.example.model.Book;
import com.example.service.BookService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Dialog xử lý nghiệp vụ kho sách:
 * 1. Nhập thêm bản sao vào thư viện (Stock In)
 * 2. Thanh lý / xuất hủy bản sao sách hư hỏng, rách nát, mất mát (Discard / Stock Out)
 */
public class BookStockAdjustDialog extends JDialog {

    private final BookService bookService = new BookService();
    private final Book        book;
    private boolean           saved = false;

    private JRadioButton rbImport;
    private JRadioButton rbDiscard;
    private JSpinner     spCount;
    private JTextArea    txtNotes;
    private JLabel       lblCurrentTotal;
    private JLabel       lblCurrentAvail;
    private JLabel       lblCurrentBorrowed;
    private JLabel       lblHint;

    public BookStockAdjustDialog(Frame parent, Book book, boolean defaultToImport) {
        super(parent, "Điều Chỉnh Số Lượng Bản Sao Sách", true);
        this.book = book;

        initUI(defaultToImport);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initUI(boolean defaultToImport) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.ACCENT_PRIMARY);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel title = new JLabel("📦  Nghiệp Vụ Kho Bản Sao");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Nhập thêm sách mới vào kho hoặc thanh lý bản sao hư hỏng, mất mát");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_WHITE);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Group 1: Thông tin sách hiện tại
        JPanel pnlInfo = new JPanel(new GridLayout(2, 2, 8, 6));
        pnlInfo.setOpaque(false);
        pnlInfo.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "  Thông Tin Sách Hiện Tại  ",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            UITheme.FONT_BOLD, UITheme.TEXT_SECONDARY
        ));

        JLabel lblTitle = new JLabel("<html><b>Sách:</b> " + escapeHtml(book.getTitle()) + "</html>");
        JLabel lblAuthor = new JLabel("<html><b>Tác giả:</b> " + escapeHtml(book.getAuthor()) + "</html>");
        lblCurrentTotal = new JLabel("Tổng số bản hiện có: " + book.getTotalCopies());
        lblCurrentAvail = new JLabel("Bản có sẵn: " + book.getAvailableCopies());
        lblCurrentBorrowed = new JLabel("Đang mượn: " + book.getBorrowedCopies());

        lblTitle.setFont(UITheme.FONT_BODY);
        lblAuthor.setFont(UITheme.FONT_BODY);
        lblCurrentTotal.setFont(UITheme.FONT_BOLD);
        lblCurrentAvail.setFont(UITheme.FONT_BOLD);
        lblCurrentAvail.setForeground(UITheme.COLOR_SUCCESS);

        pnlInfo.add(lblTitle);
        pnlInfo.add(lblAuthor);
        pnlInfo.add(lblCurrentTotal);
        pnlInfo.add(lblCurrentAvail);
        body.add(pnlInfo);
        body.add(Box.createVerticalStrut(12));

        // Group 2: Chọn nghiệp vụ
        JPanel pnlAction = new JPanel(new GridLayout(1, 2, 10, 0));
        pnlAction.setOpaque(false);
        pnlAction.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "  Chọn Nghiệp Vụ  ",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            UITheme.FONT_BOLD, UITheme.TEXT_SECONDARY
        ));

        rbImport = new JRadioButton("📥  Nhập thêm bản sao", defaultToImport);
        rbDiscard = new JRadioButton("📤  Thanh lý / Xuất hủy", !defaultToImport);
        rbImport.setFont(UITheme.FONT_BOLD);
        rbDiscard.setFont(UITheme.FONT_BOLD);
        rbImport.setOpaque(false);
        rbDiscard.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbImport);
        bg.add(rbDiscard);
        pnlAction.add(rbImport);
        pnlAction.add(rbDiscard);
        body.add(pnlAction);
        body.add(Box.createVerticalStrut(12));

        // Group 3: Nhập số lượng & Lý do
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0.3;
        JLabel lblCountPrompt = new JLabel("Số lượng bản:");
        lblCountPrompt.setFont(UITheme.FONT_BOLD);
        pnlForm.add(lblCountPrompt, g);

        g.gridx = 1; g.weightx = 0.7;
        spCount = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spCount.setFont(UITheme.FONT_BODY);
        spCount.setPreferredSize(new Dimension(100, UITheme.INPUT_HEIGHT));
        pnlForm.add(spCount, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0.3;
        JLabel lblNotesPrompt = new JLabel("Ghi chú / Lý do *:");
        lblNotesPrompt.setFont(UITheme.FONT_BOLD);
        pnlForm.add(lblNotesPrompt, g);

        g.gridx = 1; g.weightx = 0.7;
        txtNotes = UITheme.createTextArea(3, 20);
        JScrollPane spNotes = new JScrollPane(txtNotes);
        spNotes.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        pnlForm.add(spNotes, g);

        body.add(pnlForm);
        body.add(Box.createVerticalStrut(8));

        lblHint = new JLabel();
        lblHint.setFont(UITheme.FONT_SMALL);
        lblHint.setForeground(UITheme.TEXT_MUTED);
        body.add(lblHint);

        root.add(body, BorderLayout.CENTER);

        // Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnSubmit = UITheme.createPrimaryButton("  ✓  Xác Nhận Thực Hiện  ");

        btnCancel.addActionListener(e -> dispose());
        btnSubmit.addActionListener(e -> executeAdjustment());

        footer.add(btnCancel);
        footer.add(btnSubmit);
        root.add(footer, BorderLayout.SOUTH);

        // Sự kiện đổi radio button
        rbImport.addActionListener(e -> updateHint());
        rbDiscard.addActionListener(e -> updateHint());
        updateHint();
    }

    private void updateHint() {
        if (rbImport.isSelected()) {
            lblHint.setText("ℹ Nhập thêm sẽ tăng đồng thời Tổng số bản và Số bản có sẵn.");
            lblHint.setForeground(UITheme.ACCENT_PRIMARY);
            if (txtNotes.getText().isBlank()) txtNotes.setText("Nhập bổ sung từ nhà sách / quyên góp");
        } else {
            lblHint.setText("⚠ Thanh lý tối đa " + book.getAvailableCopies() + " bản (bản có sẵn). Bản đang mượn không thể thanh lý.");
            lblHint.setForeground(UITheme.COLOR_DANGER);
            if (txtNotes.getText().isBlank()) txtNotes.setText("Thanh lý sách rách nát, hư hỏng không thể sử dụng");
        }
    }

    private void executeAdjustment() {
        try {
            int count = (Integer) spCount.getValue();
            String reason = txtNotes.getText().trim();

            if (rbImport.isSelected()) {
                // Nghiệp vụ Nhập thêm
                bookService.importCopies(book.getId(), count, reason);
                UITheme.showSuccess(this, String.format(
                    "Đã nhập thêm %d bản cho sách \"%s\" thành công!\nTổng số bản hiện tại: %d bản.",
                    count, book.getTitle(), book.getTotalCopies() + count
                ));
            } else {
                // Nghiệp vụ Thanh lý
                if (count > book.getAvailableCopies()) {
                    UITheme.showWarning(this, String.format(
                        "Không thể thanh lý %d bản!\nSố bản có sẵn trong thư viện hiện chỉ còn: %d bản.",
                        count, book.getAvailableCopies()
                    ));
                    return;
                }
                bookService.discardCopies(book.getId(), count, reason);
                UITheme.showSuccess(this, String.format(
                    "Đã thanh lý %d bản sách \"%s\" thành công!\nSố bản còn lại: %d bản.",
                    count, book.getTitle(), book.getAvailableCopies() - count
                ));
            }

            saved = true;
            dispose();
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi thực hiện nghiệp vụ:\n" + ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
