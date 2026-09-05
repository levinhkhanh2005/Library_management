package com.example.view.panels;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.view.dialogs.BorrowDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel Quản Lý Mượn/Trả Sách — bảng phiếu mượn, lọc trạng thái, trả sách.
 */
public class BorrowPanel extends JPanel implements MainFrame.Refreshable {

    private final BorrowService borrowService = new BorrowService();

    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private JComboBox<String> filterStatus;
    private JLabel            statusLabel;
    private JButton           btnReturn, btnDelete, btnRenew, btnLost;

    private static final String[] COLUMNS = {
        "#", "Mã Phiếu", "Tên Sách", "Độc Giả", "Mã Thẻ",
        "Ngày Mượn", "Hạn Trả", "Ngày Trả", "Trạng Thái", "Phạt (đ)"
    };

    private static final String[] FILTER_OPTIONS = {
        "Tất cả", "Đang mượn", "Quá hạn", "Đã trả", "Mất sách"
    };

    public BorrowPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_MD));
        setBackground(UITheme.BG_PRIMARY);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        loadData("Tất cả", null);
    }

    // ================================================================
    //  Header
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);
        header.add(
            UITheme.createPageHeader("📋  Mượn / Trả Sách",
                "Quản lý phiếu mượn, ghi nhận trả sách và tính tiền phạt"),
            BorderLayout.NORTH
        );
        header.add(buildToolbar(), BorderLayout.CENTER);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(UITheme.PAD_MD, 0));
        toolbar.setBackground(UITheme.BG_WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD)
        ));

        // Bên trái: nút hành động
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);

        JButton btnNew    = UITheme.createPrimaryButton("＋  Tạo Phiếu Mượn");
        btnReturn         = UITheme.createSuccessButton("✓  Trả Sách");
        btnRenew          = UITheme.createSecondaryButton("↻  Gia Hạn");
        btnLost           = UITheme.createDangerButton("⚠  Báo Mất");
        btnDelete         = UITheme.createDangerButton("✕  Xóa Phiếu");
        JButton btnRefresh= UITheme.createSecondaryButton("↺  Làm Mới");

        btnReturn.setEnabled(false);
        btnRenew.setEnabled(false);
        btnLost.setEnabled(false);
        btnDelete.setEnabled(false);

        leftGroup.add(btnNew);
        leftGroup.add(btnReturn);
        leftGroup.add(btnRenew);
        leftGroup.add(btnLost);
        leftGroup.add(btnDelete);
        leftGroup.add(btnRefresh);
        toolbar.add(leftGroup, BorderLayout.WEST);

        // Bên phải: lọc trạng thái + tìm kiếm
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightGroup.setOpaque(false);

        JLabel filterLbl = new JLabel("Lọc:");
        filterLbl.setFont(UITheme.FONT_BOLD);
        filterLbl.setForeground(UITheme.TEXT_SECONDARY);

        filterStatus = new JComboBox<>(FILTER_OPTIONS);
        filterStatus.setFont(UITheme.FONT_BODY);
        filterStatus.setPreferredSize(new Dimension(130, UITheme.INPUT_HEIGHT));

        searchField = UITheme.createSearchField();
        searchField.setPreferredSize(new Dimension(220, UITheme.INPUT_HEIGHT));

        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));

        rightGroup.add(filterLbl);
        rightGroup.add(filterStatus);
        rightGroup.add(searchField);
        rightGroup.add(btnSearch);
        toolbar.add(rightGroup, BorderLayout.EAST);

        // Sự kiện
        btnNew.addActionListener(e -> openBorrowDialog());
        btnReturn.addActionListener(e -> returnSelected());
        btnRenew.addActionListener(e -> renewSelected());
        btnLost.addActionListener(e -> reportLostSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> {
            searchField.setText("");
            filterStatus.setSelectedIndex(0);
            loadData("Tất cả", null);
        });
        filterStatus.addActionListener(e ->
            loadData((String) filterStatus.getSelectedItem(), searchField.getText()));
        btnSearch.addActionListener(e ->
            loadData((String) filterStatus.getSelectedItem(), searchField.getText()));
        searchField.addActionListener(e ->
            loadData((String) filterStatus.getSelectedItem(), searchField.getText()));

        return toolbar;
    }

    // ================================================================
    //  Bảng
    // ================================================================

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        int[] widths = {40, 75, 220, 150, 80, 95, 95, 95, 100, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Renderer cột "Trạng Thái"
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                wrapper.setOpaque(true);
                wrapper.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                        : (row % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));
                String s = val != null ? val.toString() : "";
                JLabel badge;
                if      (s.equals(Borrow.Status.RETURNED.getLabel())) badge = UITheme.createBadge(s, "success");
                else if (s.equals(Borrow.Status.OVERDUE.getLabel()))  badge = UITheme.createBadge(s, "danger");
                else if (s.equals(Borrow.Status.LOST.getLabel()))     badge = UITheme.createBadge(s, "warning");
                else                                                    badge = UITheme.createBadge(s, "info");
                wrapper.add(badge);
                return wrapper;
            }
        });

        // Renderer cột "Phạt" — đỏ nếu > 0
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(RIGHT);
                setFont(UITheme.FONT_BOLD);
                String text = val != null ? val.toString() : "0 đ";
                if (!sel) {
                    setForeground(text.equals("0 đ") || text.equals("—")
                        ? UITheme.TEXT_MUTED : UITheme.COLOR_DANGER);
                }
                return this;
            }
        });

        // Căn giữa cột số thứ tự và mã phiếu
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{0, 1, 4}) {
            table.getColumnModel().getColumn(col).setCellRenderer(center);
        }

        // Chọn dòng → bật/tắt nút
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                btnReturn.setEnabled(false);
                btnRenew.setEnabled(false);
                btnLost.setEnabled(false);
                btnDelete.setEnabled(false);
                return;
            }
            String statusText = (String) tableModel.getValueAt(row, 8);
            boolean isBorrowing = statusText.equals(Borrow.Status.BORROWING.getLabel());
            boolean isOverdue   = statusText.equals(Borrow.Status.OVERDUE.getLabel());
            boolean isReturned  = statusText.equals(Borrow.Status.RETURNED.getLabel());
            boolean isLost      = statusText.equals(Borrow.Status.LOST.getLabel());

            btnReturn.setEnabled(isBorrowing || isOverdue);
            btnRenew.setEnabled(isBorrowing);                   // chỉ gia hạn khi đúng hạn
            btnLost.setEnabled(isBorrowing || isOverdue);       // báo mất khi đang mượn/quá hạn
            btnDelete.setEnabled(isReturned || isLost);         // chỉ xóa phiếu đã hoàn tất
        });

        return UITheme.createTableScrollPane(table);
    }

    // ================================================================
    //  Footer
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);
        statusLabel = UITheme.createMutedLabel("Đang tải dữ liệu...");
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(UITheme.createMutedLabel(
            "💡 Trả Sách | ↻ Gia Hạn (+7 ngày, tối đa 2 lần) | ⚠ Báo Mất Sách"),
            BorderLayout.EAST);
        return footer;
    }

    // ================================================================
    //  Logic
    // ================================================================

    private void loadData(String filter, String keyword) {
        statusLabel.setText("Đang tải...");
        SwingWorker<List<Borrow>, Void> worker = new SwingWorker<>() {
            @Override protected List<Borrow> doInBackground() throws Exception {
                if (keyword != null && !keyword.isBlank()) {
                    return borrowService.searchBorrows(keyword);
                }
                return switch (filter) {
                    case "Đang mượn" -> borrowService.getActiveBorrows().stream()
                        .filter(b -> b.getStatus() == Borrow.Status.BORROWING)
                        .toList();
                    case "Quá hạn"   -> borrowService.getOverdueBorrows();
                    case "Đã trả"    -> borrowService.getAllBorrows().stream()
                        .filter(b -> b.getStatus() == Borrow.Status.RETURNED)
                        .toList();
                    case "Mất sách"  -> borrowService.getLostBorrows();
                    default          -> borrowService.getAllBorrows();
                };
            }
            @Override protected void done() {
                try {
                    List<Borrow> list = get();
                    tableModel.setRowCount(0);
                    int idx = 1;
                    for (Borrow b : list) {
                        String fine = b.getFineAmount() > 0
                            ? UITheme.formatCurrency(b.getFineAmount()) : "—";
                        tableModel.addRow(new Object[]{
                            idx++,
                            b.getId(),
                            b.getBookTitle(),
                            b.getReaderName(),
                            b.getReaderCode(),
                            b.getBorrowDate(),
                            b.getDueDate(),
                            b.getReturnDate() != null ? b.getReturnDate() : "—",
                            b.getStatus().getLabel(),
                            fine
                        });
                    }
                    long active  = list.stream().filter(Borrow::isActive).count();
                    long overdue = list.stream().filter(b -> b.getStatus() == Borrow.Status.OVERDUE).count();
                    long lost    = list.stream().filter(Borrow::isLost).count();
                    statusLabel.setText(String.format(
                        "Tổng: %d phiếu  |  Đang mượn: %d  |  Quá hạn: %d  |  Mất: %d",
                        list.size(), active, overdue, lost));
                } catch (Exception ex) {
                    UITheme.showError(BorrowPanel.this, "Lỗi tải dữ liệu:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Lấy borrowId từ dòng đang chọn. */
    private int getSelectedBorrowId() {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        return (Integer) tableModel.getValueAt(row, 1);
    }

    private void openBorrowDialog() {
        BorrowDialog dlg = new BorrowDialog(
            (Frame) SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData(
            (String) filterStatus.getSelectedItem(), searchField.getText());
    }

    private void returnSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn."); return; }

        int row = table.getSelectedRow();
        String bookTitle  = (String) tableModel.getValueAt(row, 2);
        String readerName = (String) tableModel.getValueAt(row, 3);
        String dueDate    = (String) tableModel.getValueAt(row, 6);

        // Tính tiền phạt trước
        double fine = borrowService.calculateCurrentFine(dueDate);
        String fineMsg = fine > 0
            ? "\n⚠ Tiền phạt quá hạn: " + UITheme.formatCurrency(fine)
            : "\n✓ Trả đúng hạn, không có phạt.";

        boolean confirm = UITheme.showConfirm(this,
            "Xác nhận trả sách:\n"
            + "📚 Sách: " + bookTitle + "\n"
            + "👤 Độc giả: " + readerName
            + fineMsg,
            "Xác nhận Trả Sách");
        if (!confirm) return;

        try {
            Borrow updated = borrowService.returnBook(borrowId);
            String msg = "Đã ghi nhận trả sách \"" + bookTitle + "\" thành công!";
            if (updated.getFineAmount() > 0) {
                msg += "\n💰 Tiền phạt: " + UITheme.formatCurrency(updated.getFineAmount());
            }
            UITheme.showSuccess(this, msg);
            loadData((String) filterStatus.getSelectedItem(), searchField.getText());
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void deleteSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu."); return; }

        int row = table.getSelectedRow();
        String statusTxt = (String) tableModel.getValueAt(row, 8);
        if (!statusTxt.equals(Borrow.Status.RETURNED.getLabel())
                && !statusTxt.equals(Borrow.Status.LOST.getLabel())) {
            UITheme.showWarning(this, "Chỉ có thể xóa phiếu đã trả sách hoặc đã báo mất.\nVui lòng ghi nhận trả hoặc báo mất trước.");
            return;
        }

        boolean confirm = UITheme.showConfirm(this,
            "Xóa phiếu mượn #" + borrowId + "?", "Xác nhận xóa");
        if (!confirm) return;
        try {
            UITheme.showSuccess(this, "Đã xóa phiếu mượn #" + borrowId + ".");
            loadData((String) filterStatus.getSelectedItem(), searchField.getText());
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void renewSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn."); return; }

        int row = table.getSelectedRow();
        String bookTitle  = (String) tableModel.getValueAt(row, 2);
        String readerName = (String) tableModel.getValueAt(row, 3);
        String dueDate    = (String) tableModel.getValueAt(row, 6);

        // Tính hạn trả mới
        try {
            java.time.LocalDate oldDue = java.time.LocalDate.parse(dueDate,
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String newDueDate = oldDue.plusDays(BorrowService.RENEW_DAYS)
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            boolean confirm = UITheme.showConfirm(this,
                "Xác nhận gia hạn phiếu mượn:\n"
                + "📚 Sách: " + bookTitle + "\n"
                + "👤 Độc giả: " + readerName + "\n"
                + "📅 Hạn trả cũ: " + dueDate + "\n"
                + "📅 Hạn trả mới: " + newDueDate + "\n"
                + "(+" + BorrowService.RENEW_DAYS + " ngày, tối đa " + BorrowService.MAX_RENEW_COUNT + " lần)",
                "Xác nhận Gia Hạn");
            if (!confirm) return;

            Borrow updated = borrowService.renewBook(borrowId);
            UITheme.showSuccess(this,
                "Đã gia hạn thành công!\n📅 Hạn trả mới: " + updated.getDueDate());
            loadData((String) filterStatus.getSelectedItem(), searchField.getText());
        } catch (IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi gia hạn:\n" + ex.getMessage());
        }
    }

    private void reportLostSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn."); return; }

        int row = table.getSelectedRow();
        String bookTitle  = (String) tableModel.getValueAt(row, 2);
        String readerName = (String) tableModel.getValueAt(row, 3);

        // Dialog nhập thông tin bồi thường
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        formPanel.setBackground(UITheme.BG_WHITE);

        JLabel lblBook = new JLabel("📚 Sách:");
        lblBook.setFont(UITheme.FONT_BOLD);
        formPanel.add(lblBook);
        formPanel.add(new JLabel(bookTitle));

        JLabel lblReader = new JLabel("👤 Độc giả:");
        lblReader.setFont(UITheme.FONT_BOLD);
        formPanel.add(lblReader);
        formPanel.add(new JLabel(readerName));

        JLabel lblFee = new JLabel("💰 Tiền bồi thường (đ):");
        lblFee.setFont(UITheme.FONT_BOLD);
        JTextField fFee = UITheme.createTextField("");
        fFee.setText(UITheme.formatCurrency(BorrowService.DEFAULT_LOST_COMPENSATION).replace(" đ", "").replace(".", "").trim());
        formPanel.add(lblFee);
        formPanel.add(fFee);

        JLabel lblReason = new JLabel("📝 Lý do:");
        lblReason.setFont(UITheme.FONT_BOLD);
        JTextField fReason = UITheme.createTextField("VD: Làm mất, hư hỏng nặng...");
        formPanel.add(lblReason);
        formPanel.add(fReason);

        int result = JOptionPane.showConfirmDialog(this, formPanel,
            "⚠ Báo Mất Sách", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            double fee;
            try {
                fee = Double.parseDouble(fFee.getText().trim().replace(",", "").replace(".", ""));
            } catch (NumberFormatException ex) {
                UITheme.showWarning(this, "Số tiền bồi thường không hợp lệ.");
                return;
            }
            String reason = fReason.getText().trim();

            Borrow updated = borrowService.reportLostBook(borrowId, fee, reason);
            UITheme.showSuccess(this,
                "Đã ghi nhận mất sách \"" + bookTitle + "\"!\n"
                + "💰 Tiền bồi thường: " + UITheme.formatCurrency(updated.getFineAmount()));
            loadData((String) filterStatus.getSelectedItem(), searchField.getText());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi báo mất sách:\n" + ex.getMessage());
        }
    }

    @Override public void refresh() {
        loadData(filterStatus != null
            ? (String) filterStatus.getSelectedItem() : "Tất cả",
            searchField != null ? searchField.getText() : null);
    }
}
