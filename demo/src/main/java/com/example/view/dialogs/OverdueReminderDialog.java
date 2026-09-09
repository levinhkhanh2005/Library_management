package com.example.view.dialogs;

import com.example.dao.BorrowDAO;
import com.example.dao.EmailLogDAO;
import com.example.dao.ReaderDAO;
import com.example.model.Borrow;
import com.example.model.EmailLog;
import com.example.model.Reader;
import com.example.service.EmailService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog quản lý nhắc nhở quá hạn qua email.
 * Hiển thị bảng phiếu quá hạn, gửi đơn/hàng loạt, xem trước email.
 */
public class OverdueReminderDialog extends JDialog {

    private final EmailService emailService = new EmailService();
    private final BorrowDAO    borrowDAO    = new BorrowDAO();
    private final ReaderDAO    readerDAO    = new ReaderDAO();
    private final EmailLogDAO  emailLogDAO  = new EmailLogDAO();

    private JTable            table;
    private DefaultTableModel tableModel;
    private JLabel            statusLabel;
    private JTextArea         logArea;

    private static final String[] COLUMNS = {
        "#", "Mã Phiếu", "Tên Sách", "Độc Giả", "Email", "Hạn Trả",
        "Ngày Trễ", "Phạt (đ)", "Nhắc Gần Nhất"
    };

    public OverdueReminderDialog(Window owner) {
        super(owner, "🔔  Nhắc Nhở Quá Hạn Qua Email", ModalityType.APPLICATION_MODAL);
        setSize(1000, 650);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 8));
        getContentPane().setBackground(UITheme.BG_PRIMARY);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        loadOverdueData();
    }

    // ================================================================
    //  Header
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_PRIMARY);
        header.setBorder(new EmptyBorder(12, 16, 4, 16));

        JLabel title = new JLabel("📧  Danh Sách Phiếu Quá Hạn & Gửi Email Nhắc Nhở");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        statusLabel = new JLabel("");
        statusLabel.setFont(UITheme.FONT_BODY);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        header.add(statusLabel, BorderLayout.EAST);

        return header;
    }

    // ================================================================
    //  Center: Bảng + Log
    // ================================================================

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(UITheme.BG_PRIMARY);
        center.setBorder(new EmptyBorder(0, 16, 0, 16));

        // Bảng quá hạn
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        int[] widths = {35, 65, 180, 120, 160, 85, 70, 90, 130};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(45);

        // "Ngày Trễ" cột 6 — tô đỏ
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                if (!sel) {
                    setForeground(new Color(0xC62828));
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return this;
            }
        });

        JScrollPane tableScroll = UITheme.createTableScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, 300));

        // Log area
        logArea = new JTextArea(5, 0);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(0xFAFAFA));
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("📋 Nhật ký gửi email"));

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, logScroll);
        split.setResizeWeight(0.65);
        split.setDividerSize(6);
        center.add(split, BorderLayout.CENTER);

        return center;
    }

    // ================================================================
    //  Footer: Các nút hành động
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton btnPreview  = UITheme.createSecondaryButton("👁  Xem Trước Email");
        JButton btnSendOne  = UITheme.createPrimaryButton("📧  Gửi Cho Phiếu Đang Chọn");
        JButton btnSendAll  = UITheme.createSuccessButton("📧  Gửi Tất Cả Quá Hạn");
        JButton btnRefresh  = UITheme.createSecondaryButton("↺  Làm Mới");
        JButton btnClose    = UITheme.createSecondaryButton("Đóng");

        footer.add(btnPreview);
        footer.add(btnSendOne);
        footer.add(btnSendAll);
        footer.add(btnRefresh);
        footer.add(btnClose);

        btnPreview.addActionListener(e -> previewEmail());
        btnSendOne.addActionListener(e -> sendSelectedEmail());
        btnSendAll.addActionListener(e -> sendAllEmails());
        btnRefresh.addActionListener(e -> loadOverdueData());
        btnClose.addActionListener(e -> dispose());

        return footer;
    }

    // ================================================================
    //  Load dữ liệu
    // ================================================================

    private void loadOverdueData() {
        tableModel.setRowCount(0);
        try {
            List<Borrow> overdueList = borrowDAO.findOverdue();
            int idx = 1;
            for (Borrow b : overdueList) {
                Reader reader = readerDAO.findById(b.getReaderId());
                String email = (reader != null && reader.getEmail() != null) ? reader.getEmail() : "—";
                long overdueDays = EmailService.calculateOverdueDays(b.getDueDate());
                double fine = overdueDays * 2000.0;

                // Lần nhắc cuối
                String lastSent = "—";
                try {
                    EmailLog lastLog = emailLogDAO.findLastSuccessByBorrowId(b.getId());
                    if (lastLog != null) {
                        lastSent = lastLog.getSentAt();
                    }
                } catch (SQLException ignored) {}

                tableModel.addRow(new Object[]{
                    idx++, "PM-" + b.getId(), b.getBookTitle(),
                    b.getReaderName(), email, b.getDueDate(),
                    overdueDays + " ngày", String.format("%,.0f", fine),
                    lastSent
                });
            }
            statusLabel.setText("Tổng: " + overdueList.size() + " phiếu quá hạn");
        } catch (SQLException e) {
            UITheme.showError(this, "Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    // ================================================================
    //  Hành động
    // ================================================================

    /** Xem trước nội dung email cho phiếu đang chọn. */
    private void previewEmail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UITheme.showWarning(this, "Vui lòng chọn một phiếu quá hạn để xem trước email.");
            return;
        }

        int borrowId = extractBorrowId(row);
        String[] preview = emailService.previewOverdueEmail(borrowId);

        // Hiển thị HTML trong JEditorPane
        JEditorPane htmlPane = new JEditorPane("text/html", preview[1]);
        htmlPane.setEditable(false);
        htmlPane.setPreferredSize(new Dimension(620, 500));

        JScrollPane scroll = new JScrollPane(htmlPane);
        scroll.setPreferredSize(new Dimension(640, 520));

        JOptionPane.showMessageDialog(this, scroll,
            "👁 Xem Trước Email — " + preview[0],
            JOptionPane.INFORMATION_MESSAGE);
    }

    /** Gửi email cho phiếu đang chọn. */
    private void sendSelectedEmail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UITheme.showWarning(this, "Vui lòng chọn một phiếu quá hạn.");
            return;
        }

        int borrowId = extractBorrowId(row);
        String email = tableModel.getValueAt(row, 4).toString();

        if ("—".equals(email) || email.isBlank()) {
            UITheme.showWarning(this, "Độc giả chưa có email. Không thể gửi.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Gửi email nhắc nhở đến: " + email + "\n(Phiếu #" + borrowId + ")",
            "Xác nhận gửi", JOptionPane.OK_CANCEL_OPTION);

        if (confirm != JOptionPane.OK_OPTION) return;

        // Gửi trong background
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() {
                return emailService.sendOverdueReminder(borrowId);
            }
            @Override protected void done() {
                try {
                    String result = get();
                    appendLog(result);
                    if (result.startsWith("✅")) {
                        UITheme.showSuccess(OverdueReminderDialog.this, result);
                    } else {
                        UITheme.showWarning(OverdueReminderDialog.this, result);
                    }
                    loadOverdueData();
                } catch (Exception ex) {
                    appendLog("❌ Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Gửi email cho tất cả phiếu quá hạn. */
    private void sendAllEmails() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            UITheme.showWarning(this, "Không có phiếu quá hạn nào.");
            return;
        }

        // Lọc ra phiếu có email
        List<Integer> borrowIds = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            String email = tableModel.getValueAt(i, 4).toString();
            if (!"—".equals(email) && !email.isBlank()) {
                borrowIds.add(extractBorrowId(i));
            }
        }

        if (borrowIds.isEmpty()) {
            UITheme.showWarning(this, "Không có phiếu nào có email độc giả.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Gửi email nhắc nhở cho " + borrowIds.size() + " phiếu quá hạn?",
            "Xác nhận gửi hàng loạt", JOptionPane.OK_CANCEL_OPTION);

        if (confirm != JOptionPane.OK_OPTION) return;

        // Progress dialog
        JProgressBar progressBar = new JProgressBar(0, borrowIds.size());
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(400, 25));

        JDialog progressDialog = new JDialog(this, "Đang gửi email...", false);
        progressDialog.setLayout(new BorderLayout(8, 8));
        progressDialog.add(new JLabel("  Đang gửi email nhắc nhở..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(450, 100);
        progressDialog.setLocationRelativeTo(this);

        // Gửi trong background
        final List<Integer> finalIds = borrowIds;
        SwingWorker<String, String> worker = new SwingWorker<>() {
            @Override protected String doInBackground() {
                int count = 0;
                int success = 0, failed = 0, skipped = 0;

                for (int borrowId : finalIds) {
                    String result = emailService.sendOverdueReminder(borrowId);
                    count++;
                    publish("[" + count + "/" + finalIds.size() + "] " + result);

                    if (result.startsWith("✅")) success++;
                    else if (result.startsWith("⚠")) skipped++;
                    else failed++;

                    int c = count;
                    SwingUtilities.invokeLater(() -> progressBar.setValue(c));

                    if (count < finalIds.size()) {
                        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    }
                }
                return String.format("Hoàn thành: %d thành công, %d bỏ qua, %d thất bại",
                                     success, skipped, failed);
            }

            @Override protected void process(List<String> chunks) {
                for (String msg : chunks) appendLog(msg);
            }

            @Override protected void done() {
                progressDialog.dispose();
                try {
                    String result = get();
                    appendLog("═══ " + result + " ═══");
                    UITheme.showSuccess(OverdueReminderDialog.this, result);
                    loadOverdueData();
                } catch (Exception ex) {
                    appendLog("❌ Lỗi: " + ex.getMessage());
                }
            }
        };

        progressDialog.setVisible(true);
        worker.execute();
    }

    // ================================================================
    //  Utility
    // ================================================================

    /** Trích borrowId từ cột "Mã Phiếu" (format "PM-123"). */
    private int extractBorrowId(int row) {
        String code = tableModel.getValueAt(row, 1).toString();
        try {
            return Integer.parseInt(code.replace("PM-", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Ghi thêm dòng vào log area. */
    private void appendLog(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
