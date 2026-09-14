package com.example.view.dialogs;

import com.example.dao.BorrowDAO;
import com.example.model.Borrow;
import com.example.model.Reader;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog hiển thị lịch sử mượn trả sách chi tiết của một độc giả.
 */
public class ReaderHistoryDialog extends JDialog {

    private final Reader reader;
    private final BorrowDAO borrowDAO = new BorrowDAO();
    private DefaultTableModel tableModel;
    private JLabel lblTotal, lblBorrowing, lblOverdue, lblReturned;

    public ReaderHistoryDialog(Frame parent, Reader reader) {
        super(parent, "Lịch Sử Mượn Trả — " + reader.getFullName(), true);
        this.reader = reader;

        initUI();
        loadHistory();
        setSize(800, 520);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        root.setBackground(UITheme.BG_PRIMARY);
        root.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(UITheme.BG_WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(14, 18, 14, 18)
        ));

        JLabel title = new JLabel("📖  Lịch Sử Mượn Trả Sách");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Độc giả: " + reader.getFullName() +
            "  •  Mã thẻ: " + reader.getReaderCode() +
            "  •  Điện thoại: " + (reader.getPhone() != null ? reader.getPhone() : "—"));
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Table
        String[] cols = {"Mã Phiếu", "Tên Sách", "Ngày Mượn", "Hạn Trả", "Ngày Trả", "Trạng Thái", "Gia Hạn"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        UITheme.styleTable(table);

        // Renderer cho cột trạng thái
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                setHorizontalAlignment(CENTER);
                String status = val != null ? val.toString() : "";
                setFont(UITheme.FONT_BOLD);
                switch (status) {
                    case "Đang mượn" -> setForeground(UITheme.COLOR_INFO);
                    case "Đã trả"   -> setForeground(UITheme.COLOR_SUCCESS);
                    case "Quá hạn"  -> setForeground(UITheme.COLOR_DANGER);
                    case "Mất sách" -> setForeground(UITheme.COLOR_DANGER);
                    default          -> setForeground(UITheme.TEXT_PRIMARY);
                }
                return this;
            }
        });

        int[] widths = {80, 240, 95, 95, 95, 100, 70};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        root.add(scroll, BorderLayout.CENTER);

        // Footer: summary stats + close button
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(10, 16, 10, 16)
        ));

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        statsPanel.setOpaque(false);

        lblTotal = new JLabel("Tổng: 0");
        lblTotal.setFont(UITheme.FONT_BOLD);

        lblBorrowing = new JLabel("Đang mượn: 0");
        lblBorrowing.setFont(UITheme.FONT_BOLD);
        lblBorrowing.setForeground(UITheme.COLOR_INFO);

        lblOverdue = new JLabel("Quá hạn: 0");
        lblOverdue.setFont(UITheme.FONT_BOLD);
        lblOverdue.setForeground(UITheme.COLOR_DANGER);

        lblReturned = new JLabel("Đã trả: 0");
        lblReturned.setFont(UITheme.FONT_BOLD);
        lblReturned.setForeground(UITheme.COLOR_SUCCESS);

        statsPanel.add(lblTotal);
        statsPanel.add(lblBorrowing);
        statsPanel.add(lblOverdue);
        statsPanel.add(lblReturned);
        footer.add(statsPanel, BorderLayout.CENTER);

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);
    }

    private void loadHistory() {
        SwingWorker<List<Borrow>, Void> worker = new SwingWorker<>() {
            @Override protected List<Borrow> doInBackground() throws Exception {
                return borrowDAO.findByReader(reader.getId());
            }
            @Override protected void done() {
                try {
                    List<Borrow> borrows = get();
                    tableModel.setRowCount(0);
                    int borrowing = 0, overdue = 0, returned = 0;

                    for (Borrow b : borrows) {
                        tableModel.addRow(new Object[]{
                            "PM-" + String.format("%04d", b.getId()),
                            b.getBookTitle(),
                            b.getBorrowDate(),
                            b.getDueDate(),
                            b.getReturnDate() != null ? b.getReturnDate() : "—",
                            b.getStatus().getLabel(),
                            b.getRenewCount() + " lần"
                        });

                        switch (b.getStatus()) {
                            case BORROWING -> borrowing++;
                            case OVERDUE   -> overdue++;
                            case RETURNED  -> returned++;
                            default -> {}
                        }
                    }

                    lblTotal.setText("Tổng: " + borrows.size() + " lượt mượn");
                    lblBorrowing.setText("Đang mượn: " + borrowing);
                    lblOverdue.setText("Quá hạn: " + overdue);
                    lblReturned.setText("Đã trả: " + returned);

                } catch (Exception ex) {
                    System.err.println("[ReaderHistoryDialog] Lỗi tải lịch sử: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
