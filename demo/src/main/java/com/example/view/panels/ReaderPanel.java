package com.example.view.panels;

import com.example.model.Reader;
import com.example.model.User;
import com.example.service.ExportService;
import com.example.service.ReaderService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.view.dialogs.ReaderAccountDialog;
import com.example.view.dialogs.ReaderDialog;
import com.example.view.dialogs.ReaderHistoryDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel Quản Lý Độc Giả — thống kê thành viên, quản lý tài khoản cổng ứng dụng,
 * lọc theo trạng thái tài khoản, xuất Excel, lịch sử mượn trả và đồng bộ khóa/mở thẻ.
 */
public class ReaderPanel extends JPanel implements MainFrame.Refreshable {

    private final ReaderService readerService = new ReaderService();

    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private JComboBox<String> filterCombo;
    private JLabel            statusLabel;
    private JButton           btnEdit, btnDelete, btnLock, btnAccount, btnHistory, btnExport;

    // Stat cards row
    private JPanel statsRow;

    // Cached data
    private final List<Reader> currentLoadedReaders = new ArrayList<>();
    private final Map<Integer, User> userAccountMap = new HashMap<>();

    private static final String[] COLUMNS = {
        "#", "Mã Thẻ", "Họ Tên", "Điện Thoại", "Email", "Tài Khoản App", "Ngày Đăng Ký", "Trạng Thái Thẻ"
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
    //  Header: Tiêu đề + Thẻ thống kê + Toolbar
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        header.setBackground(UITheme.BG_PRIMARY);

        // Tiêu đề trang
        header.add(
            UITheme.createPageHeader("👤  Độc Giả & Tài Khoản Ứng Dụng",
                "Quản lý thông tin thành viên, tài khoản cổng độc giả và lịch sử mượn trả"),
            BorderLayout.NORTH
        );

        // Thống kê nhanh: 4 thẻ stat cards
        statsRow = new JPanel(new GridLayout(1, 4, UITheme.PAD_MD, 0));
        statsRow.setBackground(UITheme.BG_PRIMARY);
        statsRow.add(UITheme.createStatCard("Tổng Độc Giả",    "...", UITheme.ACCENT_PRIMARY));
        statsRow.add(UITheme.createStatCard("Đang Hoạt Động",  "...", UITheme.COLOR_SUCCESS));
        statsRow.add(UITheme.createStatCard("Bị Khóa / Hết Hạn","...", UITheme.COLOR_DANGER));
        statsRow.add(UITheme.createStatCard("Tài Khoản App",   "...", UITheme.COLOR_INFO));

        JPanel middlePanel = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        middlePanel.setOpaque(false);
        middlePanel.add(statsRow, BorderLayout.NORTH);
        middlePanel.add(buildToolbar(), BorderLayout.CENTER);

        header.add(middlePanel, BorderLayout.CENTER);
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

        JButton btnAdd   = UITheme.createPrimaryButton("＋  Thêm Độc Giả");
        btnEdit   = UITheme.createSecondaryButton("✎  Sửa");
        btnDelete = UITheme.createDangerButton("✕  Xóa");
        btnLock   = UITheme.createSecondaryButton("🔒  Khóa / Mở Thẻ");
        btnAccount= UITheme.createSecondaryButton("🔑  Tài Khoản App");
        btnHistory= UITheme.createSecondaryButton("📖  Lịch Sử Mượn");
        btnExport = UITheme.createSecondaryButton("📤  Xuất Excel");
        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");

        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        btnLock.setEnabled(false);
        btnAccount.setEnabled(false);
        btnHistory.setEnabled(false);

        btnGroup.add(btnAdd);
        btnGroup.add(btnEdit);
        btnGroup.add(btnDelete);
        btnGroup.add(btnLock);
        btnGroup.add(btnAccount);
        btnGroup.add(btnHistory);
        btnGroup.add(btnExport);
        btnGroup.add(btnRefresh);
        toolbar.add(btnGroup, BorderLayout.WEST);

        // Bộ lọc + Tìm kiếm bên phải
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);

        JLabel lblFilter = new JLabel("Lọc:");
        lblFilter.setFont(UITheme.FONT_SMALL);
        lblFilter.setForeground(UITheme.TEXT_MUTED);
        rightTools.add(lblFilter);

        filterCombo = new JComboBox<>(new String[]{
            "Tất cả", "Có TK App", "Chưa có TK App", "Hoạt động", "Bị khóa"
        });
        filterCombo.setPreferredSize(new Dimension(130, UITheme.INPUT_HEIGHT));
        filterCombo.addActionListener(e -> applyFilter());
        rightTools.add(filterCombo);

        searchField = UITheme.createSearchField();
        searchField.setPreferredSize(new Dimension(200, UITheme.INPUT_HEIGHT));
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(65, UITheme.INPUT_HEIGHT));
        rightTools.add(searchField);
        rightTools.add(btnSearch);
        toolbar.add(rightTools, BorderLayout.EAST);

        // Sự kiện
        btnAdd    .addActionListener(e -> openAddDialog());
        btnEdit   .addActionListener(e -> openEditDialog());
        btnDelete .addActionListener(e -> deleteSelected());
        btnLock   .addActionListener(e -> toggleLock());
        btnAccount.addActionListener(e -> openAccountDialog());
        btnHistory.addActionListener(e -> openHistoryDialog());
        btnExport .addActionListener(e -> exportToExcel());
        btnRefresh.addActionListener(e -> {
            searchField.setText("");
            filterCombo.setSelectedIndex(0);
            loadData(null);
        });
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

        int[] widths = {40, 85, 170, 110, 160, 130, 100, 110};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Renderer cột "Tài Khoản App" (cột 5)
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                wrapper.setOpaque(true);
                wrapper.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                        : (row % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));

                String text = val != null ? val.toString() : "";
                JLabel badge;
                if (text.startsWith("@")) {
                    if (text.contains("(Khóa)")) {
                        badge = UITheme.createBadge(text, "danger");
                    } else {
                        badge = UITheme.createBadge(text, "info");
                    }
                } else {
                    badge = UITheme.createBadge("Chưa có", "warning");
                }
                wrapper.add(badge);
                return wrapper;
            }
        });

        // Renderer cột "Trạng Thái Thẻ" (cột 7)
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
            btnAccount.setEnabled(sel);
            btnHistory.setEnabled(sel);
        });

        // Double-click → mở thông tin chỉnh sửa
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditDialog();
            }

            @Override public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && !table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                    createContextMenu().show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        return UITheme.createTableScrollPane(table);
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem itemEdit = new JMenuItem("✎  Chỉnh sửa thông tin");
        itemEdit.addActionListener(e -> openEditDialog());
        popup.add(itemEdit);

        JMenuItem itemLock = new JMenuItem("🔒  Khóa / Mở thẻ độc giả");
        itemLock.addActionListener(e -> toggleLock());
        popup.add(itemLock);

        JMenuItem itemAccount = new JMenuItem("🔑  Quản lý tài khoản App");
        itemAccount.addActionListener(e -> openAccountDialog());
        popup.add(itemAccount);

        JMenuItem itemHistory = new JMenuItem("📖  Xem lịch sử mượn trả");
        itemHistory.addActionListener(e -> openHistoryDialog());
        popup.add(itemHistory);

        popup.addSeparator();

        JMenuItem itemDelete = new JMenuItem("✕  Xóa độc giả");
        itemDelete.setForeground(UITheme.COLOR_DANGER);
        itemDelete.addActionListener(e -> deleteSelected());
        popup.add(itemDelete);

        return popup;
    }

    // ================================================================
    //  Footer
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);
        statusLabel = UITheme.createMutedLabel("Đang tải dữ liệu...");
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(UITheme.createMutedLabel("Nhấp chuột phải để xem menu tác vụ • 🔑 Quản lý tài khoản App • 📖 Xem lịch sử mượn"), BorderLayout.EAST);
        return footer;
    }

    // ================================================================
    //  Logic & SwingWorkers
    // ================================================================

    private void loadData(String keyword) {
        statusLabel.setText("Đang tải...");

        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            @Override protected Object[] doInBackground() throws Exception {
                List<Reader> readers = readerService.searchReaders(keyword);
                List<User> accounts  = readerService.getAllReaderAccounts();

                int totalReaders  = readerService.getTotalReaders();
                int activeReaders = readerService.getActiveReaders();
                int lockedReaders = readerService.getLockedReaders();
                int portalAccounts= readerService.getPortalAccountCount();

                return new Object[]{
                    readers, accounts,
                    new int[]{totalReaders, activeReaders, lockedReaders, portalAccounts}
                };
            }

            @Override protected void done() {
                try {
                    Object[] results = get();
                    @SuppressWarnings("unchecked")
                    List<Reader> readers = (List<Reader>) results[0];
                    @SuppressWarnings("unchecked")
                    List<User> accounts = (List<User>) results[1];
                    int[] stats = (int[]) results[2];

                    // Cache accounts
                    userAccountMap.clear();
                    for (User u : accounts) {
                        if (u.getReaderId() > 0) {
                            userAccountMap.put(u.getReaderId(), u);
                        }
                    }

                    currentLoadedReaders.clear();
                    currentLoadedReaders.addAll(readers);

                    // Render table with current filter
                    applyFilter();

                    // Update stat cards
                    updateStatCards(stats);

                } catch (Exception ex) {
                    UITheme.showError(ReaderPanel.this, "Lỗi tải dữ liệu:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyFilter() {
        int filterIndex = filterCombo != null ? filterCombo.getSelectedIndex() : 0;
        tableModel.setRowCount(0);
        int idx = 1;

        for (Reader r : currentLoadedReaders) {
            User user = userAccountMap.get(r.getId());
            boolean hasAccount = (user != null);
            boolean isActive = (r.getStatus() == Reader.Status.ACTIVE);
            boolean isLocked = (r.getStatus() == Reader.Status.LOCKED);

            // Filter match check:
            // 0: Tất cả, 1: Có TK App, 2: Chưa có TK App, 3: Hoạt động, 4: Bị khóa
            boolean match = switch (filterIndex) {
                case 1 -> hasAccount;
                case 2 -> !hasAccount;
                case 3 -> isActive;
                case 4 -> isLocked;
                default -> true;
            };

            if (!match) continue;

            String appAccountText;
            if (user != null) {
                appAccountText = "@" + user.getUsername() + (user.isActive() ? "" : " (Khóa)");
            } else {
                appAccountText = "Chưa có";
            }

            tableModel.addRow(new Object[]{
                idx++,
                r.getReaderCode(),
                r.getFullName(),
                r.getPhone() != null && !r.getPhone().isBlank() ? r.getPhone() : "—",
                r.getEmail() != null && !r.getEmail().isBlank() ? r.getEmail() : "—",
                appAccountText,
                r.getJoinDate(),
                r.getStatus().getLabel()
            });
        }

        String keyword = searchField != null ? searchField.getText().trim() : "";
        statusLabel.setText("Hiển thị: " + (idx - 1) + "/" + currentLoadedReaders.size() + " độc giả"
            + (keyword.isBlank() ? "" : " (từ khóa: \"" + keyword + "\")"));
    }

    private void updateStatCards(int[] stats) {
        String[] titles = {"Tổng Độc Giả", "Đang Hoạt Động", "Bị Khóa / Hết Hạn", "Tài Khoản App"};
        Color[] colors = {
            UITheme.ACCENT_PRIMARY, UITheme.COLOR_SUCCESS,
            UITheme.COLOR_DANGER,   UITheme.COLOR_INFO
        };

        statsRow.removeAll();
        for (int i = 0; i < 4; i++) {
            JPanel card = UITheme.createStatCard(titles[i], "0", colors[i]);
            statsRow.add(card);

            final int val = stats[i];
            SwingUtilities.invokeLater(() -> {
                JLabel valueLbl = findValueLabel(card);
                if (valueLbl != null) {
                    UITheme.animateValue(valueLbl, val, 600);
                }
            });
        }
        statsRow.revalidate();
        statsRow.repaint();
    }

    private JLabel findValueLabel(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel lbl) {
                if (lbl.getFont() != null && lbl.getFont().getSize() >= 28) {
                    return lbl;
                }
            }
            if (c instanceof Container sub) {
                JLabel found = findValueLabel(sub);
                if (found != null) return found;
            }
        }
        return null;
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

    private void openAccountDialog() {
        Reader r = getSelectedReader();
        if (r == null) { UITheme.showWarning(this, "Vui lòng chọn một độc giả."); return; }
        ReaderAccountDialog dlg = new ReaderAccountDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), r);
        dlg.setVisible(true);
        if (dlg.isChanged()) loadData(searchField.getText());
    }

    private void openHistoryDialog() {
        Reader r = getSelectedReader();
        if (r == null) { UITheme.showWarning(this, "Vui lòng chọn một độc giả."); return; }
        ReaderHistoryDialog dlg = new ReaderHistoryDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), r);
        dlg.setVisible(true);
    }

    private void exportToExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất danh sách độc giả ra Excel");
        String defaultName = "Danh_Sach_Doc_Gia_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".xlsx";
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));

        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".xlsx")) {
            target = new File(target.getAbsolutePath() + ".xlsx");
        }

        final File saveFile = target;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                new ExportService().exportReadersToExcel(saveFile);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    UITheme.showSuccess(ReaderPanel.this,
                        "Xuất danh sách độc giả thành công!\nFile: " + saveFile.getAbsolutePath());
                } catch (Exception ex) {
                    UITheme.showError(ReaderPanel.this, "Lỗi khi xuất file: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void deleteSelected() {
        Reader r = getSelectedReader();
        if (r == null) { UITheme.showWarning(this, "Vui lòng chọn một độc giả."); return; }

        boolean confirm = UITheme.showConfirm(this,
            "Xóa độc giả \"" + r.getFullName() + "\" (" + r.getReaderCode() + ")?\n"
            + "Tài khoản ứng dụng độc giả (nếu có) cũng sẽ bị xóa.\n"
            + "Thao tác này không thể hoàn tác.", "Xác nhận xóa độc giả");
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
            "Bạn có muốn " + action + " thẻ độc giả và tài khoản ứng dụng của\n\""
                + r.getFullName() + "\" (" + r.getReaderCode() + ")?",
            "Xác nhận " + action);
        if (!confirm) return;

        try {
            Reader.Status newStatus = isActive ? Reader.Status.LOCKED : Reader.Status.ACTIVE;
            readerService.setReaderStatus(r.getId(), newStatus);
            UITheme.showSuccess(this,
                "Đã " + action + " độc giả và đồng bộ tài khoản đăng nhập thành công!");
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
