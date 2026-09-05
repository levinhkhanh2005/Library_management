package com.example.view.panels;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.view.dialogs.BorrowDialog;
import com.example.view.dialogs.ReceiptPreviewDialog;

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
    private JLabel            statusLabel;
    private JButton           btnReturn, btnRenew, btnLost, btnExportPdf, btnDelete;

    private static final String[] COLUMNS = {
        "#", "Mã Phiếu", "Tên Sách", "Độc Giả", "Mã Thẻ",
        "Ngày Mượn", "Hạn Trả", "Gia Hạn", "Ngày Trả", "Trạng Thái", "Phạt (đ)"
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
                "Quản lý phiếu mượn, gia hạn, ghi nhận trả sách và tính tiền phạt"),
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
        btnRenew          = UITheme.createSecondaryButton("⏳  Gia Hạn");
        btnLost           = UITheme.createDangerButton("⚠  Báo Mất");
        btnExportPdf      = UITheme.createSecondaryButton("📄  Xuất PDF");
        btnDelete         = UITheme.createDangerButton("✕  Xóa Phiếu");
        JButton btnRefresh= UITheme.createSecondaryButton("↺  Làm Mới");

        btnReturn.setEnabled(false);
        btnRenew.setEnabled(false);
        btnLost.setEnabled(false);
        btnExportPdf.setEnabled(false);
        btnDelete.setEnabled(false);

        leftGroup.add(btnNew);
        leftGroup.add(btnReturn);
        leftGroup.add(btnRenew);
        leftGroup.add(btnLost);
        leftGroup.add(btnExportPdf);
        leftGroup.add(btnDelete);
        leftGroup.add(btnRefresh);
        toolbar.add(leftGroup, BorderLayout.WEST);

        // Bên phải: lọc trạng thái + tìm kiếm
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightGroup.setOpaque(false);

        searchField = UITheme.createSearchField();
        searchField.setPreferredSize(new Dimension(220, UITheme.INPUT_HEIGHT));

        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));

        JButton btnAdvancedFilter = UITheme.createSecondaryButton("🔍 Lọc Nâng Cao");

        rightGroup.add(searchField);
        rightGroup.add(btnSearch);
        rightGroup.add(btnAdvancedFilter);
        toolbar.add(rightGroup, BorderLayout.EAST);

        // Sự kiện
        btnNew.addActionListener(e -> openBorrowDialog());
        btnReturn.addActionListener(e -> returnSelected());
        btnRenew.addActionListener(e -> renewSelected());
        btnLost.addActionListener(e -> reportLostSelected());
        btnExportPdf.addActionListener(e -> exportPdfSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> {
            searchField.setText("");
            loadData("Tất cả", null);
        });
        btnSearch.addActionListener(e ->
            loadData("Tất cả", searchField.getText()));
        searchField.addActionListener(e ->
            loadData("Tất cả", searchField.getText()));
        btnAdvancedFilter.addActionListener(e -> openAdvancedFilterDialog());

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

        int[] widths = {40, 70, 210, 140, 75, 90, 90, 65, 90, 95, 85};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Renderer cột "Trạng Thái" (cột 9)
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
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

        // Renderer cột "Phạt" (cột 10) — đỏ nếu > 0
        table.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
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

        // Căn giữa cột số thứ tự, mã phiếu, mã thẻ, hạn trả, gia hạn
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{0, 1, 4, 7}) {
            table.getColumnModel().getColumn(col).setCellRenderer(center);
        }

        // Chọn dòng → bật/tắt nút
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                btnReturn.setEnabled(false);
                btnRenew.setEnabled(false);
                btnLost.setEnabled(false);
                btnExportPdf.setEnabled(false);
                btnDelete.setEnabled(false);
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            String statusText = (String) tableModel.getValueAt(modelRow, 9);
            boolean isBorrowing = statusText.equals(Borrow.Status.BORROWING.getLabel());
            boolean isOverdue   = statusText.equals(Borrow.Status.OVERDUE.getLabel());
            boolean isReturned  = statusText.equals(Borrow.Status.RETURNED.getLabel());
            boolean isLost      = statusText.equals(Borrow.Status.LOST.getLabel());

            btnReturn.setEnabled(isBorrowing || isOverdue);
            btnRenew.setEnabled(isBorrowing || isOverdue);
            btnLost.setEnabled(isBorrowing || isOverdue);
            btnExportPdf.setEnabled(true);
            btnDelete.setEnabled(true);
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
            "💡 Chọn phiếu để: \"Gia Hạn\" | \"Trả Sách\" | \"Báo Mất\" | \"Xuất PDF\""),
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
                updateTableData(this);
            }
        };
        worker.execute();
    }

    private void loadAdvancedData(String keyword, String fromDate, String toDate, Borrow.Status status) {
        statusLabel.setText("Đang tải (Lọc nâng cao)...");
        SwingWorker<List<Borrow>, Void> worker = new SwingWorker<>() {
            @Override protected List<Borrow> doInBackground() throws Exception {
                return borrowService.advancedSearchBorrows(keyword, fromDate, toDate, status);
            }
            @Override protected void done() {
                updateTableData(this);
            }
        };
        worker.execute();
    }

    private void updateTableData(SwingWorker<List<Borrow>, Void> worker) {
        try {
            List<Borrow> list = worker.get();
            tableModel.setRowCount(0);
            int idx = 1;
            for (Borrow b : list) {
                String fine = b.getFineAmount() > 0
                    ? UITheme.formatCurrency(b.getFineAmount()) : "—";
                String renewDisplay = b.getRenewCount() + "/" + BorrowService.MAX_RENEW_COUNT;
                tableModel.addRow(new Object[]{
                    idx++,
                    b.getId(),
                    b.getBookTitle(),
                    b.getReaderName(),
                    b.getReaderCode(),
                    b.getBorrowDate(),
                    b.getDueDate(),
                    renewDisplay,
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
            btnReturn.setEnabled(false);
            btnRenew.setEnabled(false);
            btnLost.setEnabled(false);
            btnExportPdf.setEnabled(false);
            btnDelete.setEnabled(false);
        } catch (Exception ex) {
            UITheme.showError(BorrowPanel.this, "Lỗi tải dữ liệu:\n" + ex.getMessage());
        }
    }

    /** Lấy borrowId từ dòng đang chọn. */
    private int getSelectedBorrowId() {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        int modelRow = table.convertRowIndexToModel(row);
        Object val = tableModel.getValueAt(modelRow, 1);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString().trim()); }
            catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private void openBorrowDialog() {
        BorrowDialog dlg = new BorrowDialog(
            (Frame) SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadData("Tất cả", searchField.getText());
        }
    }

    private void renewSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) {
            UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn.");
            return;
        }

        int row = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(row);
        String bookTitle  = (String) tableModel.getValueAt(modelRow, 2);
        String readerName = (String) tableModel.getValueAt(modelRow, 3);
        String dueDate    = (String) tableModel.getValueAt(modelRow, 6);
        String renewStr   = (String) tableModel.getValueAt(modelRow, 7);

        // Hộp thoại tùy chỉnh cho phép chọn số ngày gia hạn
        JDialog renewDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Gia Hạn Phiếu Mượn", true);
        renewDialog.setLayout(new BorderLayout());
        renewDialog.setSize(440, 280);
        renewDialog.setLocationRelativeTo(this);
        renewDialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        content.setBackground(UITheme.BG_WHITE);

        JLabel lblTitle = new JLabel("⏳ Gia hạn phiếu mượn #" + borrowId);
        lblTitle.setFont(UITheme.FONT_H3);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblInfo = new JLabel("<html>"
            + "<b>Sách:</b> " + bookTitle + "<br>"
            + "<b>Độc giả:</b> " + readerName + "<br>"
            + "<b>Hạn trả hiện tại:</b> <font color='#2563EB'>" + dueDate + "</font><br>"
            + "<b>Số lần đã gia hạn:</b> " + renewStr + "<br>"
            + "</html>");
        lblInfo.setFont(UITheme.FONT_BODY);
        lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblInfo.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel daysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        daysPanel.setOpaque(false);
        daysPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDays = new JLabel("Số ngày gia hạn:");
        lblDays.setFont(UITheme.FONT_BOLD);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(BorrowService.DEFAULT_RENEW_DAYS, 1, 60, 1));
        spinner.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));
        spinner.setFont(UITheme.FONT_BODY);

        JButton btnPlus7 = UITheme.createSecondaryButton("+7");
        btnPlus7.setPreferredSize(new Dimension(50, UITheme.INPUT_HEIGHT));
        btnPlus7.addActionListener(e -> spinner.setValue(7));

        JButton btnPlus14 = UITheme.createSecondaryButton("+14");
        btnPlus14.setPreferredSize(new Dimension(50, UITheme.INPUT_HEIGHT));
        btnPlus14.addActionListener(e -> spinner.setValue(14));

        daysPanel.add(lblDays);
        daysPanel.add(spinner);
        daysPanel.add(btnPlus7);
        daysPanel.add(btnPlus14);

        content.add(lblTitle);
        content.add(lblInfo);
        content.add(daysPanel);

        // Nút hành động
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        actionPanel.setBackground(UITheme.BG_WHITE);
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnConfirm = UITheme.createPrimaryButton("Xác Nhận Gia Hạn");

        btnCancel.addActionListener(e -> renewDialog.dispose());
        btnConfirm.addActionListener(e -> {
            int extraDays = (Integer) spinner.getValue();
            try {
                Borrow updated = borrowService.renewBorrow(borrowId, extraDays);
                renewDialog.dispose();
                UITheme.showSuccess(this,
                    "Gia hạn phiếu mượn #" + borrowId + " thành công!\n"
                    + "📅 Hạn trả mới: " + updated.getDueDate() + "\n"
                    + "🔄 Số lần gia hạn: " + updated.getRenewCount() + "/" + BorrowService.MAX_RENEW_COUNT);
                loadData("Tất cả", searchField.getText());
            } catch (Exception ex) {
                UITheme.showError(renewDialog, ex.getMessage());
            }
        });

        actionPanel.add(btnCancel);
        actionPanel.add(btnConfirm);

        renewDialog.add(content, BorderLayout.CENTER);
        renewDialog.add(actionPanel, BorderLayout.SOUTH);
        renewDialog.setVisible(true);
    }

    private void returnSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn."); return; }

        int row = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(row);
        String bookTitle  = (String) tableModel.getValueAt(modelRow, 2);
        String readerName = (String) tableModel.getValueAt(modelRow, 3);
        String dueDate    = (String) tableModel.getValueAt(modelRow, 6);

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
            loadData("Tất cả", searchField.getText());

            if (updated.getFineAmount() > 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Đã ghi nhận trả sách \"" + bookTitle + "\" thành công!\n"
                    + "💰 Tiền phạt quá hạn: " + UITheme.formatCurrency(updated.getFineAmount()) + "\n\n"
                    + "Bạn có muốn xem trước và xuất file PDF Biên lai thu tiền phạt ngay không?",
                    "Xuất Biên Lai Thu Tiền Phạt",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    ReceiptPreviewDialog dlg = new ReceiptPreviewDialog(
                        SwingUtilities.getWindowAncestor(this),
                        updated,
                        ReceiptPreviewDialog.ReceiptType.FINE_RECEIPT);
                    dlg.setVisible(true);
                }
            } else {
                UITheme.showSuccess(this, "Đã ghi nhận trả sách \"" + bookTitle + "\" thành công!");
            }
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void exportPdfSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) {
            UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn.");
            return;
        }

        try {
            Borrow borrow = borrowService.getBorrowById(borrowId);
            if (borrow == null) {
                UITheme.showError(this, "Không tìm thấy thông tin phiếu mượn #" + borrowId);
                return;
            }

            Window parent = SwingUtilities.getWindowAncestor(this);
            if (borrow.getFineAmount() > 0) {
                Object[] options = {"📄 Phiếu Mượn Sách", "💰 Biên Lai Thu Tiền Phạt", "Hủy"};
                int choice = JOptionPane.showOptionDialog(this,
                    "Phiếu mượn #" + borrowId + " có tiền phạt (" + UITheme.formatCurrency(borrow.getFineAmount()) + ").\n"
                    + "Vui lòng chọn loại chứng từ cần xuất PDF:",
                    "Chọn Loại Chứng Từ Xuất PDF",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

                if (choice == 0) {
                    new ReceiptPreviewDialog(parent, borrow, ReceiptPreviewDialog.ReceiptType.BORROW_SLIP).setVisible(true);
                } else if (choice == 1) {
                    new ReceiptPreviewDialog(parent, borrow, ReceiptPreviewDialog.ReceiptType.FINE_RECEIPT).setVisible(true);
                }
            } else {
                new ReceiptPreviewDialog(parent, borrow, ReceiptPreviewDialog.ReceiptType.BORROW_SLIP).setVisible(true);
            }
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi khi nạp dữ liệu phiếu mượn:\n" + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu."); return; }

        int row = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(row);
        String statusTxt = (String) tableModel.getValueAt(modelRow, 9);

        String msg = "Xóa phiếu mượn #" + borrowId + "?";
        if (!statusTxt.equals(Borrow.Status.RETURNED.getLabel())) {
            msg += "\n⚠ Phiếu này chưa trả sách. Xóa phiếu sẽ tự động hoàn trả +1 bản sách vào kho.";
        }

        boolean confirm = UITheme.showConfirm(this, msg, "Xác nhận xóa phiếu");
        if (!confirm) return;
        try {
            borrowService.deleteBorrow(borrowId);
            UITheme.showSuccess(this, "Đã xóa phiếu mượn #" + borrowId + ".");
            loadData("Tất cả", searchField.getText());
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void reportLostSelected() {
        int borrowId = getSelectedBorrowId();
        if (borrowId < 0) { UITheme.showWarning(this, "Vui lòng chọn một phiếu mượn."); return; }

        int row = table.getSelectedRow();
        int modelRow = table.convertRowIndexToModel(row);
        String bookTitle  = (String) tableModel.getValueAt(modelRow, 2);
        String readerName = (String) tableModel.getValueAt(modelRow, 3);

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
            loadData("Tất cả", searchField.getText());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi báo mất sách:\n" + ex.getMessage());
        }
    }

    private void openAdvancedFilterDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Lọc Phiếu Mượn", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(16, 20, 16, 20));
        content.setBackground(UITheme.BG_WHITE);

        // Keyword
        JPanel pnlKeyword = new JPanel(new BorderLayout(5, 5));
        pnlKeyword.setOpaque(false);
        pnlKeyword.add(new JLabel("Từ khóa:"), BorderLayout.WEST);
        JTextField txtKeyword = UITheme.createTextField("");
        txtKeyword.setText(searchField.getText());
        pnlKeyword.add(txtKeyword, BorderLayout.CENTER);

        // Status
        JPanel pnlStatus = new JPanel(new BorderLayout(5, 5));
        pnlStatus.setOpaque(false);
        pnlStatus.add(new JLabel("Trạng thái:"), BorderLayout.WEST);
        JComboBox<String> cbStatus = new JComboBox<>(FILTER_OPTIONS);
        pnlStatus.add(cbStatus, BorderLayout.CENTER);

        // Date Range
        JPanel pnlFromDate = new JPanel(new BorderLayout(5, 5));
        pnlFromDate.setOpaque(false);
        pnlFromDate.add(new JLabel("Từ ngày (dd/MM/yyyy):"), BorderLayout.WEST);
        JTextField txtFromDate = UITheme.createTextField("dd/MM/yyyy");
        pnlFromDate.add(txtFromDate, BorderLayout.CENTER);

        JPanel pnlToDate = new JPanel(new BorderLayout(5, 5));
        pnlToDate.setOpaque(false);
        pnlToDate.add(new JLabel("Đến ngày (dd/MM/yyyy):"), BorderLayout.WEST);
        JTextField txtToDate = UITheme.createTextField("dd/MM/yyyy");
        pnlToDate.add(txtToDate, BorderLayout.CENTER);

        content.add(pnlKeyword);
        content.add(Box.createVerticalStrut(10));
        content.add(pnlStatus);
        content.add(Box.createVerticalStrut(10));
        content.add(pnlFromDate);
        content.add(Box.createVerticalStrut(10));
        content.add(pnlToDate);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(UITheme.BG_WHITE);
        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnFilter = UITheme.createPrimaryButton("Lọc Kết Quả");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnFilter.addActionListener(e -> {
            String kw = txtKeyword.getText();
            String selStatus = (String) cbStatus.getSelectedItem();
            Borrow.Status status = null;
            if ("Đang mượn".equals(selStatus)) status = Borrow.Status.BORROWING;
            else if ("Quá hạn".equals(selStatus)) status = Borrow.Status.OVERDUE;
            else if ("Đã trả".equals(selStatus)) status = Borrow.Status.RETURNED;

            String fromD = txtFromDate.getText().trim();
            String toD = txtToDate.getText().trim();

            dialog.dispose();
            searchField.setText(kw);
            loadAdvancedData(kw, fromD, toD, status);
        });

        actionPanel.add(btnCancel);
        actionPanel.add(btnFilter);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(actionPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    @Override public void refresh() {
        loadData("Tất cả", searchField != null ? searchField.getText() : null);
    }
}
