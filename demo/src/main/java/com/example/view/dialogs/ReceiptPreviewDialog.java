package com.example.view.dialogs;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.util.PdfExportUtil;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Dialog xem trước và xuất Phiếu Mượn Sách / Biên Lai Thu Tiền Phạt ra file PDF.
 */
public class ReceiptPreviewDialog extends JDialog {

    public enum ReceiptType {
        BORROW_SLIP("Phiếu Mượn Sách", "PhieuMuon_"),
        FINE_RECEIPT("Biên Lai Thu Tiền Phạt", "BienLaiThuPhat_");

        private final String title;
        private final String filePrefix;

        ReceiptType(String title, String filePrefix) {
            this.title = title;
            this.filePrefix = filePrefix;
        }

        public String getTitle() { return title; }
        public String getFilePrefix() { return filePrefix; }
    }

    private final Borrow borrow;
    private final ReceiptType type;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReceiptPreviewDialog(Window parent, Borrow borrow, ReceiptType type) {
        super(parent, "Xem Trước & Xuất PDF — " + type.getTitle(), ModalityType.APPLICATION_MODAL);
        this.borrow = borrow;
        this.type = type;

        initUI();
        pack();
        setSize(540, 680);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_PRIMARY);
        setContentPane(root);

        // Header dialog
        root.add(buildTitleBar(), BorderLayout.NORTH);

        // Content preview (Paper sheet style)
        JScrollPane scrollPane = new JScrollPane(buildPreviewPaper());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        // Footer buttons
        root.add(buildFooterBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(type == ReceiptType.FINE_RECEIPT ? UITheme.COLOR_DANGER : UITheme.ACCENT_PRIMARY);
        bar.setPreferredSize(new Dimension(0, 50));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel("📄  Xem Trước " + type.getTitle());
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("Kiểm tra thông tin trước khi xuất file PDF");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(255, 255, 255, 200));
        bar.add(sub, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildPreviewPaper() {
        JPanel outer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        outer.setOpaque(false);

        JPanel paper = new JPanel();
        paper.setLayout(new BoxLayout(paper, BoxLayout.Y_AXIS));
        paper.setPreferredSize(new Dimension(460, 580));
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
            new EmptyBorder(24, 28, 24, 28)
        ));

        // Header Thư viện
        JLabel lblOrg = new JLabel("THƯ VIỆN TRƯỜNG THPT NGUYỄN HUỆ");
        lblOrg.setFont(UITheme.FONT_BOLD);
        lblOrg.setForeground(UITheme.TEXT_SECONDARY);
        lblOrg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblContact = new JLabel("Địa chỉ: 123 Nguyễn Huệ, Q.1, TP. HCM | Hotline: (028) 3829-1234");
        lblContact.setFont(UITheme.FONT_SMALL);
        lblContact.setForeground(UITheme.TEXT_MUTED);
        lblContact.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(420, 2));

        // Title Receipt
        JLabel lblReceiptTitle = new JLabel(type.getTitle().toUpperCase());
        lblReceiptTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblReceiptTitle.setForeground(type == ReceiptType.FINE_RECEIPT ? UITheme.COLOR_DANGER : UITheme.ACCENT_PRIMARY);
        lblReceiptTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        String codeStr = (type == ReceiptType.FINE_RECEIPT ? "#BL-" : "#") + String.format("%05d", borrow.getId());
        String dateStr = LocalDate.now().format(DATE_FMT);
        JLabel lblMeta = new JLabel("Mã chứng từ: " + codeStr + "  |  Ngày lập: " + dateStr);
        lblMeta.setFont(UITheme.FONT_SMALL);
        lblMeta.setForeground(UITheme.TEXT_MUTED);
        lblMeta.setAlignmentX(Component.CENTER_ALIGNMENT);

        paper.add(lblOrg);
        paper.add(lblContact);
        paper.add(Box.createVerticalStrut(8));
        paper.add(sep);
        paper.add(Box.createVerticalStrut(12));
        paper.add(lblReceiptTitle);
        paper.add(lblMeta);
        paper.add(Box.createVerticalStrut(16));

        // Body Content
        if (type == ReceiptType.BORROW_SLIP) {
            paper.add(buildBorrowSlipHtml());
        } else {
            paper.add(buildFineReceiptHtml());
        }

        paper.add(Box.createVerticalStrut(20));

        // Signatures preview
        JPanel signPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        signPanel.setOpaque(false);
        signPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSignLeft = new JLabel("<html><center><b>" + (type == ReceiptType.FINE_RECEIPT ? "NGƯỜI NỘP TIỀN" : "NGƯỜI MƯỢN SÁCH") + "</b><br><font size='2' color='#64748B'>(Ký và ghi rõ họ tên)</font><br><br><br></center></html>");
        JLabel lblSignRight = new JLabel("<html><center><b>THỦ THƯ XÁC NHẬN</b><br><font size='2' color='#64748B'>(Ký và ghi rõ họ tên)</font><br><br><br></center></html>");

        signPanel.add(lblSignLeft);
        signPanel.add(lblSignRight);
        paper.add(signPanel);

        outer.add(paper);
        return outer;
    }

    private JEditorPane buildBorrowSlipHtml() {
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);

        String html = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 11px; color: #334155;'>"
            + "<table width='100%' cellpadding='4' cellspacing='0'>"
            + "<tr><td width='50%' valign='top'>"
            + "<b style='color: #0F172A;'>👤 THÔNG TIN ĐỘC GIẢ</b><br>"
            + "• Họ tên: <b>" + safe(borrow.getReaderName()) + "</b><br>"
            + "• Mã thẻ: <b>" + safe(borrow.getReaderCode()) + "</b>"
            + "</td>"
            + "<td width='50%' valign='top'>"
            + "<b style='color: #0F172A;'>📚 THÔNG TIN MƯỢN SÁCH</b><br>"
            + "• Sách: <b>" + safe(borrow.getBookTitle()) + "</b><br>"
            + (borrow.getBookIsbn() != null ? "• ISBN: " + borrow.getBookIsbn() + "<br>" : "")
            + "• Ngày mượn: " + safe(borrow.getBorrowDate()) + "<br>"
            + "• Hạn trả: <b style='color: #2563EB;'>" + safe(borrow.getDueDate()) + "</b><br>"
            + (borrow.getRenewCount() > 0 ? "• Gia hạn: " + borrow.getRenewCount() + " lần<br>" : "")
            + "</td></tr>"
            + "</table>"
            + (borrow.getNotes() != null && !borrow.getNotes().isBlank() ? "<br><b>📌 Ghi chú:</b> <i>" + borrow.getNotes() + "</i>" : "")
            + "<hr style='border: 0; border-top: 1px dashed #CBD5E1; margin: 12px 0;'>"
            + "<b style='color: #0F172A;'>LƯU Ý:</b><br>"
            + "• Giữ gìn sách cẩn thận và hoàn trả đúng thời hạn.<br>"
            + "• Phí phạt quá hạn: <b>" + UITheme.formatCurrency(BorrowService.FINE_PER_DAY) + "/ngày</b>."
            + "</body></html>";

        htmlPane.setText(html);
        return htmlPane;
    }

    private JEditorPane buildFineReceiptHtml() {
        JEditorPane htmlPane = new JEditorPane();
        htmlPane.setContentType("text/html");
        htmlPane.setEditable(false);
        htmlPane.setOpaque(false);

        String todayStr = LocalDate.now().format(DATE_FMT);
        String retDate = borrow.getReturnDate() != null ? borrow.getReturnDate() : todayStr;

        long overdueDays = 0;
        try {
            LocalDate due = LocalDate.parse(borrow.getDueDate(), DATE_FMT);
            LocalDate ret = LocalDate.parse(retDate, DATE_FMT);
            overdueDays = ChronoUnit.DAYS.between(due, ret);
            if (overdueDays < 0) overdueDays = 0;
        } catch (Exception ignored) {}

        double fineAmount = borrow.getFineAmount() > 0 ? borrow.getFineAmount() : (overdueDays * BorrowService.FINE_PER_DAY);
        String fineStr = UITheme.formatCurrency(fineAmount);
        String wordsStr = PdfExportUtil.numberToWords((long) fineAmount);

        String html = "<html><body style='font-family: Segoe UI, sans-serif; font-size: 11px; color: #334155;'>"
            + "<table width='100%' cellpadding='3' cellspacing='0'>"
            + "<tr><td width='35%'><b>Người nộp tiền:</b></td><td><b>" + safe(borrow.getReaderName()) + "</b> (" + safe(borrow.getReaderCode()) + ")</td></tr>"
            + "<tr><td><b>Sách mượn:</b></td><td>" + safe(borrow.getBookTitle()) + "</td></tr>"
            + "<tr><td><b>Ngày mượn / Hạn trả:</b></td><td>" + safe(borrow.getBorrowDate()) + " ➔ " + safe(borrow.getDueDate()) + "</td></tr>"
            + "<tr><td><b>Ngày trả thực tế:</b></td><td>" + retDate + "</td></tr>"
            + "<tr><td><b>Lý do thu phạt:</b></td><td><font color='#DC2626'>Trả quá hạn " + overdueDays + " ngày</font> (" + UITheme.formatCurrency(BorrowService.FINE_PER_DAY) + "/ngày)</td></tr>"
            + "</table>"
            + "<br>"
            + "<div style='background-color: #FEF2F2; border: 1px solid #FCA5A5; padding: 10px; text-align: center; border-radius: 6px;'>"
            + "<span style='font-size: 14px; font-weight: bold; color: #DC2626;'>TỔNG TIỀN PHẠT: " + fineStr + "</span><br>"
            + "<span style='font-size: 10px; font-style: italic; color: #475569;'>Bằng chữ: " + wordsStr + "</span><br>"
            + "<span style='font-size: 10px; font-weight: bold; color: #10B981;'>✔ TRẠNG THÁI: ĐÃ THANH TOÁN ĐỦ</span>"
            + "</div>"
            + "</body></html>";

        htmlPane.setText(html);
        return htmlPane;
    }

    private JPanel buildFooterBar() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        JButton btnExport = UITheme.createPrimaryButton("📄  Xuất File PDF");

        btnClose.addActionListener(e -> dispose());
        btnExport.addActionListener(e -> exportPdf());

        footer.add(btnClose);
        footer.add(btnExport);
        return footer;
    }

    private void exportPdf() {
        String defaultFileName = type.getFilePrefix() + String.format("%05d", borrow.getId()) + ".pdf";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu File PDF — " + type.getTitle());
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Tập tin PDF (*.pdf)", "pdf"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
            }

            try {
                if (type == ReceiptType.BORROW_SLIP) {
                    PdfExportUtil.exportBorrowSlip(borrow, selectedFile);
                } else {
                    PdfExportUtil.exportFineReceipt(borrow, selectedFile);
                }

                int choice = JOptionPane.showConfirmDialog(this,
                    "Xuất file PDF thành công!\n"
                    + "Đường dẫn: " + selectedFile.getAbsolutePath() + "\n\n"
                    + "Bạn có muốn mở file PDF vừa tạo không?",
                    "Xuất PDF Thành Công",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(selectedFile);
                    }
                }
                dispose();
            } catch (Exception ex) {
                UITheme.showError(this, "Lỗi khi xuất file PDF:\n" + ex.getMessage());
            }
        }
    }

    private String safe(String input) {
        return input != null ? input : "N/A";
    }
}
