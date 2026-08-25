package com.example.view.panels;

import com.example.model.Reader;
import com.example.service.ReaderService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.view.dialogs.ReaderDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel Quản Lý Độc Giả — bảng danh sách + toolbar + tìm kiếm.
 */
public class ReaderPanel extends JPanel implements MainFrame.Refreshable {

    private final ReaderService readerService = new ReaderService();

    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JButton           btnEdit, btnDelete, btnLock;

    private static final String[] COLUMNS = {
        "#", "Mã Thẻ", "Họ Tên", "Ngày Sinh", "Điện Thoại", "Email", "Ngày Đăng Ký", "Trạng Thái"
    };

    public ReaderPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_MD));
        setBackground(UITheme.BG_PRIMARY);

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        loadData(null);
    }

    // ================================================================
    //  Header: tiêu đề + toolbar
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);
        header.add(
            UITheme.createPageHeader("👤  Độc Giả",
                "Quản lý thông tin thành viên thư viện"),
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

        // Nút hành động
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnAdd  = UITheme.createPrimaryButton("＋  Thêm Độc Giả");
        btnEdit  = UITheme.createSecondaryButton("✎  Sửa");
        btnDelete= UITheme.createDangerButton("✕  Xóa");
        btnLock  = UITheme.createSecondaryButton("🔒  Khóa / Mở");
        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");

        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        btnLock.setEnabled(false);

        btnGroup.add(btnAdd);
        btnGroup.add(btnEdit);
        btnGroup.add(btnDelete);
        btnGroup.add(btnLock);
        btnGroup.add(btnRefresh);
        toolbar.add(btnGroup, BorderLayout.WEST);

        // Tìm kiếm
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        searchField = UITheme.createSearchField();
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));
        searchPanel.add(searchField);
        searchPanel.add(btnSearch);
        toolbar.add(searchPanel, BorderLayout.EAST);

        // Sự kiện
        btnAdd    .addActionListener(e -> openAddDialog());
        btnEdit   .addActionListener(e -> openEditDialog());
        btnDelete .addActionListener(e -> deleteSelected());
        btnLock   .addActionListener(e -> toggleLock());
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadData(null); });
        btnSearch .addActionListener(e -> loadData(searchField.getText()));
        searchField.addActionListener(e -> loadData(searchField.getText()));

        return toolbar;
    }

    // ================================================================
    //  Bảng dữ liệu
    // ================================================================

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        int[] widths = {40, 90, 180, 100, 120, 170, 110, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Renderer cột "Trạng Thái" → badge màu
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                wrapper.setOpaque(true);
                wrapper.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                        : (row % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));

                String status = val != null ? val.toString() : "";
                JLabel badge;
                if      (status.equals(Reader.Status.ACTIVE.getLabel()))  badge = UITheme.createBadge(status, "success");
                else if (status.equals(Reader.Status.LOCKED.getLabel()))  badge = UITheme.createBadge(status, "danger");
                else                                                        badge = UITheme.createBadge(status, "warning");
                wrapper.add(badge);
                return wrapper;
            }
        });

        // Căn giữa cột số thứ tự
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);

        // Khi chọn dòng
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = table.getSelectedRow() >= 0;
            btnEdit.setEnabled(sel);
            btnDelete.setEnabled(sel);
            btnLock.setEnabled(sel);
        });

        // Double-click → sửa
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditDialog();
            }
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
        footer.add(UITheme.createMutedLabel("Double-click để chỉnh sửa"), BorderLayout.EAST);
        return footer;
    }

    // ================================================================
    //  Logic
    // ================================================================

    private void loadData(String keyword) {
        statusLabel.setText("Đang tải...");
        SwingWorker<List<Reader>, Void> worker = new SwingWorker<>() {
            @Override protected List<Reader> doInBackground() throws Exception {
                return readerService.searchReaders(keyword);
            }
            @Override protected void done() {
                try {
                    List<Reader> readers = get();
                    tableModel.setRowCount(0);
                    int idx = 1;
                    for (Reader r : readers) {
                        tableModel.addRow(new Object[]{
                            idx++,
                            r.getReaderCode(),
                            r.getFullName(),
                            r.getBirthDate(),
                            r.getPhone(),
                            r.getEmail(),
                            r.getJoinDate(),
                            r.getStatus().getLabel()
                        });
                    }
                    statusLabel.setText("Tổng: " + readers.size() + " độc giả"
                        + (keyword != null && !keyword.isBlank()
                           ? "  (từ khóa: \"" + keyword + "\")" : ""));
                } catch (Exception ex) {
                    UITheme.showError(ReaderPanel.this, "Lỗi tải dữ liệu:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Lấy Reader từ dòng đang chọn. */
    private Reader getSelectedReader() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        String code = (String) tableModel.getValueAt(row, 1);
        try {
            return readerService.getReaderByCode(code);
        } catch (Exception e) {
            return null;
        }
    }

    private void openAddDialog() {
        ReaderDialog dlg = new ReaderDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData(searchField.getText());
    }

    private void openEditDialog() {
        Reader r = getSelectedReader();
        if (r == null) { UITheme.showWarning(this, "Vui lòng chọn một độc giả."); return; }
        ReaderDialog dlg = new ReaderDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), r);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadData(searchField.getText());
    }

    private void deleteSelected() {
        Reader r = getSelectedReader();
        if (r == null) { UITheme.showWarning(this, "Vui lòng chọn một độc giả."); return; }

        boolean confirm = UITheme.showConfirm(this,
            "Xóa độc giả \"" + r.getFullName() + "\"?\n"
            + "Thao tác này không thể hoàn tác.", "Xác nhận xóa");
        if (!confirm) return;
        try {
            readerService.deleteReader(r);
            UITheme.showSuccess(this, "Đã xóa độc giả \"" + r.getFullName() + "\".");
            loadData(searchField.getText());
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void toggleLock() {
        Reader r = getSelectedReader();
        if (r == null) return;

        boolean isActive = r.getStatus() == Reader.Status.ACTIVE;
        String action = isActive ? "khóa" : "mở khóa";
        boolean confirm = UITheme.showConfirm(this,
            "Bạn có muốn " + action + " tài khoản của\n\"" + r.getFullName() + "\"?",
            "Xác nhận " + action);
        if (!confirm) return;

        try {
            Reader.Status newStatus = isActive ? Reader.Status.LOCKED : Reader.Status.ACTIVE;
            readerService.setReaderStatus(r.getId(), newStatus);
            UITheme.showSuccess(this,
                "Đã " + action + " tài khoản \"" + r.getFullName() + "\".");
            loadData(searchField.getText());
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        loadData(searchField != null ? searchField.getText() : null);
    }
}
