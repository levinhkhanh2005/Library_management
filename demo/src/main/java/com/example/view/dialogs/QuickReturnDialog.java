package com.example.view.dialogs;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog "Trả Nhanh" — nhập mã thẻ độc giả để tìm và trả sách nhanh chóng.
 * Hiển thị tất cả sách đang mượn, cho phép chọn và trả hàng loạt.
 */
public class QuickReturnDialog extends JDialog {

    private final BorrowService borrowService = new BorrowService();
    private boolean returned = false;

    private JTextField txtReaderCode;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblReaderInfo;
    private JLabel lblSummary;
    private JButton btnReturnSelected, btnReturnAll;

    private List<Borrow> activeBorrows = new ArrayList<>();

    private static final String[] COLUMNS = {
        "✓", "#", "Mã Phiếu", "Tên Sách", "Ngày Mượn", "Hạn Trả", "Còn Lại", "Phạt Dự Kiến"
    };

    public QuickReturnDialog(Window parent) {
        super(parent, "⚡ Trả Sách Nhanh", ModalityType.APPLICATION_MODAL);
        initUI();
        setSize(750, 520);
        setLocationRelativeTo(parent);
        setResizable(true);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.COLOR_SUCCESS);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel("⚡  Trả Sách Nhanh");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel("Nhập mã thẻ hoặc tên độc giả để tìm sách đang mượn");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xFF, 0xFF, 0xFF, 200));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        content.setBackground(UITheme.BG_WHITE);
        content.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, 0, UITheme.PAD_MD));

        // --- Search bar ---
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);

        JLabel lblCode = new JLabel("🔍 Mã thẻ / Tên:");
        lblCode.setFont(UITheme.FONT_BOLD);
        txtReaderCode = UITheme.createTextField("VD: NDG-0001 hoặc tên độc giả...");
        txtReaderCode.setPreferredSize(new Dimension(250, UITheme.INPUT_HEIGHT));
        JButton btnSearch = UITheme.createPrimaryButton("Tìm Kiếm");
        btnSearch.setPreferredSize(new Dimension(110, UITheme.INPUT_HEIGHT));

        searchPanel.add(lblCode, BorderLayout.WEST);
        searchPanel.add(txtReaderCode, BorderLayout.CENTER);
        searchPanel.add(btnSearch, BorderLayout.EAST);

        // --- Reader info ---
        lblReaderInfo = new JLabel("Chưa tìm kiếm độc giả");
        lblReaderInfo.setFont(UITheme.FONT_BODY);
        lblReaderInfo.setForeground(UITheme.TEXT_MUTED);
        lblReaderInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            new EmptyBorder(6, 12, 6, 12)));

        JPanel topPanel = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        topPanel.setOpaque(false);
        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(lblReaderInfo, BorderLayout.SOUTH);
        content.add(topPanel, BorderLayout.NORTH);

        // --- Table ---
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : Object.class; }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setMaxWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(7).setPreferredWidth(100);

        // Renderer for "Còn Lại" column
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setFont(UITheme.FONT_BOLD);
                if (!sel && val != null) {
                    String text = val.toString();
                    if (text.contains("quá")) {
                        setForeground(UITheme.COLOR_DANGER);
                    } else if (text.contains("hôm") || text.startsWith("1 ") || text.startsWith("2 ") || text.startsWith("3 ")) {
                        setForeground(UITheme.COLOR_WARNING);
                    } else {
                        setForeground(UITheme.COLOR_SUCCESS);
                    }
                }
                return this;
            }
        });

        // Renderer for "Phạt Dự Kiến" column
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(RIGHT);
                setFont(UITheme.FONT_BOLD);
                String text = val != null ? val.toString() : "";
                if (!sel) {
                    setForeground(text.equals("0 đ") || text.equals("—")
                        ? UITheme.TEXT_MUTED : UITheme.COLOR_DANGER);
                }
                return this;
            }
        });

        // Center align columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{1, 2, 4, 5}) {
            table.getColumnModel().getColumn(col).setCellRenderer(center);
        }

        JScrollPane scroll = UITheme.createTableScrollPane(table);
        content.add(scroll, BorderLayout.CENTER);

        // Events
        btnSearch.addActionListener(e -> searchReader());
        txtReaderCode.addActionListener(e -> searchReader());

        // Listen to checkbox changes to update summary
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) updateSummary();
        });

        return content;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD)));

        lblSummary = new JLabel("Chưa có dữ liệu");
        lblSummary.setFont(UITheme.FONT_BODY);
        lblSummary.setForeground(UITheme.TEXT_SECONDARY);
        footer.add(lblSummary, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnSelectAll = UITheme.createSecondaryButton("☑ Chọn Tất Cả");
        JButton btnDeselectAll = UITheme.createSecondaryButton("☐ Bỏ Chọn");
        btnReturnSelected = UITheme.createSuccessButton("✓ Trả Sách Đã Chọn");
        btnReturnAll = UITheme.createPrimaryButton("✓ Trả Tất Cả");
        JButton btnCancel = UITheme.createSecondaryButton("Đóng");

        btnReturnSelected.setEnabled(false);
        btnReturnAll.setEnabled(false);

        btnSelectAll.addActionListener(e -> toggleAll(true));
        btnDeselectAll.addActionListener(e -> toggleAll(false));
        btnReturnSelected.addActionListener(e -> returnSelectedBooks());
        btnReturnAll.addActionListener(e -> returnAllBooks());
        btnCancel.addActionListener(e -> dispose());

        btnPanel.add(btnSelectAll);
        btnPanel.add(btnDeselectAll);
        btnPanel.add(btnReturnSelected);
        btnPanel.add(btnReturnAll);
        btnPanel.add(btnCancel);
        footer.add(btnPanel, BorderLayout.EAST);

        return footer;
    }

    // ================================================================
    //  Logic
    // ================================================================

    private void searchReader() {
        String input = txtReaderCode.getText().trim();
        if (input.isEmpty()) {
            UITheme.showWarning(this, "Vui lòng nhập mã thẻ hoặc tên độc giả.");
            return;
        }

        try {
            // Try by reader code first
            activeBorrows = borrowService.getActiveBorrowsByReaderCode(input);

            // If no results, try searching by keyword
            if (activeBorrows.isEmpty()) {
                List<Borrow> allBorrows = borrowService.searchBorrows(input);
                activeBorrows = allBorrows.stream()
                    .filter(Borrow::isActive)
                    .toList();
            }

            tableModel.setRowCount(0);
            if (activeBorrows.isEmpty()) {
                lblReaderInfo.setText("<html><font color='#EF4444'>Không tìm thấy sách đang mượn cho \"" + input + "\"</font></html>");
                btnReturnSelected.setEnabled(false);
                btnReturnAll.setEnabled(false);
                lblSummary.setText("Không có dữ liệu");
                return;
            }

            // Show reader info
            Borrow first = activeBorrows.get(0);
            lblReaderInfo.setText("<html><b>👤 " + first.getReaderName() + "</b>"
                + "  |  Mã thẻ: <font color='#4F46E5'>" + first.getReaderCode() + "</font>"
                + "  |  Đang mượn: <font color='#2563EB'><b>" + activeBorrows.size() + " cuốn</b></font></html>");

            int idx = 1;
            for (Borrow b : activeBorrows) {
                long daysRemaining = b.getDaysRemaining();
                String remainStr;
                if (daysRemaining < 0) {
                    remainStr = Math.abs(daysRemaining) + " ngày quá";
                } else if (daysRemaining == 0) {
                    remainStr = "hôm nay";
                } else {
                    remainStr = daysRemaining + " ngày";
                }

                double fine = borrowService.calculateCurrentFine(b.getDueDate());
                String fineStr = fine > 0 ? UITheme.formatCurrency(fine) : "—";

                tableModel.addRow(new Object[]{
                    true,  // default checked
                    idx++,
                    b.getId(),
                    b.getBookTitle(),
                    b.getBorrowDate(),
                    b.getDueDate(),
                    remainStr,
                    fineStr
                });
            }

            btnReturnSelected.setEnabled(true);
            btnReturnAll.setEnabled(true);
            updateSummary();

        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi tìm kiếm:\n" + ex.getMessage());
        }
    }

    private void toggleAll(boolean selected) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(selected, i, 0);
        }
    }

    private void updateSummary() {
        int selected = 0;
        double totalFine = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean checked = (Boolean) tableModel.getValueAt(i, 0);
            if (checked != null && checked) {
                selected++;
                if (i < activeBorrows.size()) {
                    totalFine += borrowService.calculateCurrentFine(activeBorrows.get(i).getDueDate());
                }
            }
        }
        String fineText = totalFine > 0
            ? "  |  💰 Tổng phạt: " + UITheme.formatCurrency(totalFine)
            : "  |  ✅ Không có phạt";
        lblSummary.setText("<html>Đã chọn: <b>" + selected + "/" + tableModel.getRowCount() + "</b> cuốn"
            + fineText + "</html>");
        btnReturnSelected.setEnabled(selected > 0);
    }

    private void returnSelectedBooks() {
        List<Integer> selectedIds = getSelectedBorrowIds();
        if (selectedIds.isEmpty()) {
            UITheme.showWarning(this, "Vui lòng chọn ít nhất một cuốn sách.");
            return;
        }
        doReturn(selectedIds);
    }

    private void returnAllBooks() {
        List<Integer> allIds = new ArrayList<>();
        for (Borrow b : activeBorrows) {
            allIds.add(b.getId());
        }
        doReturn(allIds);
    }

    private void doReturn(List<Integer> borrowIds) {
        // Calculate total fine for confirmation
        double totalFine = 0;
        StringBuilder bookList = new StringBuilder();
        for (int id : borrowIds) {
            for (Borrow b : activeBorrows) {
                if (b.getId() == id) {
                    double fine = borrowService.calculateCurrentFine(b.getDueDate());
                    totalFine += fine;
                    bookList.append("  • ").append(b.getBookTitle());
                    if (fine > 0) bookList.append(" (phạt: ").append(UITheme.formatCurrency(fine)).append(")");
                    bookList.append("\n");
                    break;
                }
            }
        }

        String fineMsg = totalFine > 0
            ? "\n⚠ Tổng tiền phạt: " + UITheme.formatCurrency(totalFine)
            : "\n✅ Không có tiền phạt.";

        boolean confirm = UITheme.showConfirm(this,
            "Xác nhận trả " + borrowIds.size() + " cuốn sách:\n" + bookList + fineMsg,
            "Xác Nhận Trả Sách");
        if (!confirm) return;

        try {
            List<Borrow> results = borrowService.returnMultipleBooks(borrowIds);
            returned = true;

            double actualFine = results.stream().mapToDouble(Borrow::getFineAmount).sum();
            String msg = "Đã trả thành công " + results.size() + "/" + borrowIds.size() + " cuốn sách!";
            if (actualFine > 0) {
                msg += "\n💰 Tổng tiền phạt: " + UITheme.formatCurrency(actualFine);
            }
            UITheme.showSuccess(this, msg);

            // Refresh the search
            searchReader();
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi trả sách:\n" + ex.getMessage());
        }
    }

    private List<Integer> getSelectedBorrowIds() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean checked = (Boolean) tableModel.getValueAt(i, 0);
            if (checked != null && checked && i < activeBorrows.size()) {
                ids.add(activeBorrows.get(i).getId());
            }
        }
        return ids;
    }

    public boolean isReturned() { return returned; }
}
