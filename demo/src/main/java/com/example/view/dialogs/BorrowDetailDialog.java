package com.example.view.dialogs;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dialog xem chi tiết một phiếu mượn.
 * Hiển thị đầy đủ thông tin: sách, độc giả, timeline, trạng thái, tiền phạt.
 * Có các nút hành động nhanh: Trả sách, Gia hạn, Báo mất, Xuất PDF.
 */
public class BorrowDetailDialog extends JDialog {

    private final BorrowService borrowService = new BorrowService();
    private final int borrowId;
    private Borrow borrow;
    private boolean changed = false;

    public BorrowDetailDialog(Window parent, int borrowId) {
        super(parent, "Chi Tiết Phiếu Mượn", ModalityType.APPLICATION_MODAL);
        this.borrowId = borrowId;
        loadData();
        initUI();
        setSize(560, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void loadData() {
        try {
            borrow = borrowService.getBorrowById(borrowId);
        } catch (Exception e) {
            borrow = null;
        }
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        if (borrow == null) {
            root.add(new JLabel("Không tìm thấy phiếu mượn #" + borrowId,
                SwingConstants.CENTER), BorderLayout.CENTER);
            return;
        }

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildActions(), BorderLayout.SOUTH);

        // ESC to close
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JPanel buildTitleBar() {
        Color barColor = switch (borrow.getStatus()) {
            case RETURNED -> UITheme.COLOR_SUCCESS;
            case OVERDUE -> UITheme.COLOR_DANGER;
            case LOST -> UITheme.COLOR_WARNING;
            default -> UITheme.ACCENT_PRIMARY;
        };

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(barColor);
        bar.setPreferredSize(new Dimension(0, 56));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel("📋  Phiếu Mượn #" + borrow.getId());
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);

        JLabel badge = new JLabel(borrow.getStatus().getLabel());
        badge.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 13));
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(new Color(255, 255, 255, 60));
        badge.setBorder(new EmptyBorder(4, 12, 4, 12));

        bar.add(title, BorderLayout.WEST);
        bar.add(badge, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_WHITE);
        body.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_LG, UITheme.PAD_MD, UITheme.PAD_LG));

        // --- Book info ---
        body.add(createSectionTitle("📚 Thông Tin Sách"));
        body.add(createInfoRow("Tên sách:", borrow.getBookTitle()));
        body.add(createInfoRow("ISBN:", borrow.getBookIsbn() != null ? borrow.getBookIsbn() : "—"));
        body.add(Box.createVerticalStrut(12));

        // --- Reader info ---
        body.add(createSectionTitle("👤 Thông Tin Độc Giả"));
        body.add(createInfoRow("Tên độc giả:", borrow.getReaderName()));
        body.add(createInfoRow("Mã thẻ:", borrow.getReaderCode()));
        body.add(Box.createVerticalStrut(12));

        // --- Timeline ---
        body.add(createSectionTitle("📅 Timeline Mượn Trả"));
        body.add(createInfoRow("Ngày mượn:", borrow.getBorrowDate()));
        body.add(createInfoRow("Hạn trả:", borrow.getDueDate()));

        // Days remaining / overdue
        if (borrow.isActive()) {
            long daysRemaining = borrow.getDaysRemaining();
            String remainText;
            Color remainColor;
            if (daysRemaining < 0) {
                remainText = "Quá hạn " + Math.abs(daysRemaining) + " ngày";
                remainColor = UITheme.COLOR_DANGER;
            } else if (daysRemaining == 0) {
                remainText = "Hết hạn hôm nay!";
                remainColor = UITheme.COLOR_WARNING;
            } else if (daysRemaining <= BorrowService.DUE_SOON_DAYS) {
                remainText = "Còn " + daysRemaining + " ngày (sắp hết hạn)";
                remainColor = UITheme.COLOR_WARNING;
            } else {
                remainText = "Còn " + daysRemaining + " ngày";
                remainColor = UITheme.COLOR_SUCCESS;
            }
            body.add(createInfoRowColored("Còn lại:", remainText, remainColor));
        }

        if (borrow.getReturnDate() != null) {
            body.add(createInfoRow("Ngày trả:", borrow.getReturnDate()));
        }

        body.add(createInfoRow("Số lần gia hạn:", borrow.getRenewCount() + "/" + BorrowService.MAX_RENEW_COUNT));
        body.add(Box.createVerticalStrut(12));

        // --- Fine ---
        body.add(createSectionTitle("💰 Tiền Phạt"));
        if (borrow.isActive()) {
            double currentFine = borrowService.calculateCurrentFine(borrow.getDueDate());
            if (currentFine > 0) {
                body.add(createInfoRowColored("Phạt dự kiến:", UITheme.formatCurrency(currentFine), UITheme.COLOR_DANGER));
            } else {
                body.add(createInfoRowColored("Phạt dự kiến:", "0 đ (đúng hạn)", UITheme.COLOR_SUCCESS));
            }
        } else if (borrow.getFineAmount() > 0) {
            body.add(createInfoRowColored("Tiền phạt:", UITheme.formatCurrency(borrow.getFineAmount()), UITheme.COLOR_DANGER));
        } else {
            body.add(createInfoRowColored("Tiền phạt:", "Không có phạt", UITheme.COLOR_SUCCESS));
        }

        // --- Notes ---
        if (borrow.getNotes() != null && !borrow.getNotes().isBlank()) {
            body.add(Box.createVerticalStrut(12));
            body.add(createSectionTitle("📝 Ghi Chú"));
            JTextArea notesArea = new JTextArea(borrow.getNotes());
            notesArea.setFont(UITheme.FONT_BODY);
            notesArea.setEditable(false);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            notesArea.setBackground(UITheme.BG_PRIMARY);
            notesArea.setBorder(new EmptyBorder(8, 12, 8, 12));
            notesArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            notesArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(notesArea);
        }

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        panel.setBackground(UITheme.BG_WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        if (borrow.isActive()) {
            JButton btnReturn = UITheme.createSuccessButton("✓ Trả Sách");
            btnReturn.addActionListener(e -> doReturn());
            panel.add(btnReturn);

            if (borrow.canRenew(BorrowService.MAX_RENEW_COUNT)) {
                JButton btnRenew = UITheme.createSecondaryButton("⏳ Gia Hạn");
                btnRenew.addActionListener(e -> doRenew());
                panel.add(btnRenew);
            }

            JButton btnLost = UITheme.createDangerButton("⚠ Báo Mất");
            btnLost.addActionListener(e -> doLost());
            panel.add(btnLost);
        }

        JButton btnPdf = UITheme.createSecondaryButton("📄 Xuất PDF");
        btnPdf.addActionListener(e -> doPdf());
        panel.add(btnPdf);

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);

        return panel;
    }

    // ================================================================
    //  Actions
    // ================================================================

    private void doReturn() {
        double fine = borrowService.calculateCurrentFine(borrow.getDueDate());
        String fineMsg = fine > 0
            ? "\n⚠ Tiền phạt quá hạn: " + UITheme.formatCurrency(fine)
            : "\n✓ Trả đúng hạn, không có phạt.";

        boolean confirm = UITheme.showConfirm(this,
            "Xác nhận trả sách:\n📚 " + borrow.getBookTitle() + "\n👤 " + borrow.getReaderName() + fineMsg,
            "Xác nhận Trả Sách");
        if (!confirm) return;

        try {
            borrowService.returnBook(borrowId);
            changed = true;
            UITheme.showSuccess(this, "Đã ghi nhận trả sách thành công!");
            dispose();
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void doRenew() {
        String input = JOptionPane.showInputDialog(this,
            "Nhập số ngày gia hạn thêm:", "Gia Hạn Phiếu #" + borrowId,
            JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.isBlank()) return;

        try {
            int days = Integer.parseInt(input.trim());
            Borrow updated = borrowService.renewBorrow(borrowId, days);
            changed = true;
            UITheme.showSuccess(this,
                "Gia hạn thành công!\n📅 Hạn trả mới: " + updated.getDueDate()
                + "\n🔄 Gia hạn: " + updated.getRenewCount() + "/" + BorrowService.MAX_RENEW_COUNT);
            dispose();
        } catch (NumberFormatException ex) {
            UITheme.showWarning(this, "Số ngày không hợp lệ.");
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void doLost() {
        try {
            borrowService.reportLostBook(borrowId, BorrowService.DEFAULT_LOST_COMPENSATION, "Báo mất từ chi tiết phiếu");
            changed = true;
            UITheme.showSuccess(this, "Đã ghi nhận mất sách.");
            dispose();
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void doPdf() {
        try {
            ReceiptPreviewDialog dlg = new ReceiptPreviewDialog(
                this, borrow, ReceiptPreviewDialog.ReceiptType.BORROW_SLIP);
            dlg.setVisible(true);
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi xuất PDF:\n" + ex.getMessage());
        }
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private JLabel createSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        lbl.setForeground(UITheme.ACCENT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        return lbl;
    }

    private JPanel createInfoRow(String label, String value) {
        return createInfoRowColored(label, value, UITheme.TEXT_PRIMARY);
    }

    private JPanel createInfoRowColored(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(UITheme.FONT_BODY);
        lblKey.setForeground(UITheme.TEXT_SECONDARY);
        lblKey.setPreferredSize(new Dimension(140, 24));

        JLabel lblVal = new JLabel(value != null ? value : "—");
        lblVal.setFont(UITheme.FONT_BOLD);
        lblVal.setForeground(valueColor);

        row.add(lblKey, BorderLayout.WEST);
        row.add(lblVal, BorderLayout.CENTER);
        return row;
    }

    public boolean isChanged() { return changed; }
}
