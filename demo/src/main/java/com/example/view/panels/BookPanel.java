package com.example.view.panels;

import com.example.model.Book;
import com.example.service.BookService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.view.dialogs.BookDetailDialog;
import com.example.view.dialogs.BookDialog;
import com.example.view.dialogs.BookStockAdjustDialog;
import com.example.view.dialogs.CategoryDistributionDialog;
import com.example.view.dialogs.CategoryManageDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel Quản Lý Sách — Bảng danh sách + Toolbar nghiệp vụ đầy đủ + Lọc kho & Biểu đồ thể loại.
 */
public class BookPanel extends JPanel implements MainFrame.Refreshable {

    private final BookService bookService = new BookService();

    // UI Components
    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private JComboBox<String> cbStockFilter;
    private JLabel            statusLabel;

    // Action buttons
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnDetail;
    private JButton btnImportCopies;
    private JButton btnDiscardCopies;
    private JButton btnClone;

    // Dữ liệu sách đang hiển thị
    private List<Book> currentBooks = new ArrayList<>();

    // Columns
    private static final String[] COLUMNS = {
        "#", "ISBN", "Tên Sách", "Tác Giả", "Thể Loại", "NXB", "Năm", "Tổng", "Còn Lại", "Tình Trạng"
    };

    public BookPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_MD));
        setBackground(UITheme.BG_PRIMARY);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);

        loadData(null);
    }

    // ================================================================
    //  Header: Tiêu đề + Toolbar nghiệp vụ
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);

        // Tiêu đề trang
        header.add(UITheme.createPageHeader("📚  Quản Lý Sách",
            "Quản lý danh mục sách, nhập kho, thanh lý bản sao và phân tích phân bố thể loại"), BorderLayout.NORTH);

        // Toolbar
        header.add(buildToolbar(), BorderLayout.CENTER);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel toolbarWrapper = new JPanel(new BorderLayout(0, 6));
        toolbarWrapper.setBackground(UITheme.BG_WHITE);
        toolbarWrapper.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD)
            )
        );

        // Hàng 1: Nút hành động nghiệp vụ
        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);

        JPanel btnGroupLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnGroupLeft.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("＋  Thêm Sách");
        btnDetail = UITheme.createSecondaryButton("📖  Chi Tiết");
        btnImportCopies = UITheme.createSecondaryButton("📥  Nhập Bản");
        btnDiscardCopies = UITheme.createSecondaryButton("📤  Thanh Lý");
        btnEdit = UITheme.createSecondaryButton("✎  Sửa");
        btnClone = UITheme.createSecondaryButton("📋  Nhân Bản");
        btnDelete = UITheme.createDangerButton("✕  Xóa");

        btnDetail.setEnabled(false);
        btnImportCopies.setEnabled(false);
        btnDiscardCopies.setEnabled(false);
        btnEdit.setEnabled(false);
        btnClone.setEnabled(false);
        btnDelete.setEnabled(false);

        btnGroupLeft.add(btnAdd);
        btnGroupLeft.add(btnDetail);
        btnGroupLeft.add(btnImportCopies);
        btnGroupLeft.add(btnDiscardCopies);
        btnGroupLeft.add(btnEdit);
        btnGroupLeft.add(btnClone);
        btnGroupLeft.add(btnDelete);

        JPanel btnGroupRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnGroupRight.setOpaque(false);

        JButton btnChart = UITheme.createPrimaryButton("📊  Biểu Đồ Thể Loại");
        btnChart.setToolTipText("Xem biểu đồ tròn thể loại sách chiếm % tổng số sách");
        JButton btnCategory = UITheme.createSecondaryButton("📂  Thể Loại");
        btnCategory.setToolTipText("Quản lý danh mục thể loại sách");
        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");

        btnGroupRight.add(btnChart);
        btnGroupRight.add(btnCategory);
        btnGroupRight.add(btnRefresh);

        actionRow.add(btnGroupLeft, BorderLayout.WEST);
        actionRow.add(btnGroupRight, BorderLayout.EAST);
        toolbarWrapper.add(actionRow, BorderLayout.NORTH);

        // Hàng 2: Bộ lọc nhanh tình trạng tồn kho & Ô tìm kiếm
        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        filterRow.setOpaque(false);

        JPanel filterLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterLeft.setOpaque(false);
        JLabel lblFilterPrompt = new JLabel("Lọc tình trạng:");
        lblFilterPrompt.setFont(UITheme.FONT_BOLD);
        lblFilterPrompt.setForeground(UITheme.TEXT_SECONDARY);

        cbStockFilter = new JComboBox<>(new String[]{
            "Tất cả trạng thái",
            "Còn sách có thể mượn",
            "Sắp hết (≤ 1 bản)",
            "Đang mượn hết (0 bản)"
        });
        cbStockFilter.setFont(UITheme.FONT_BODY);
        cbStockFilter.setPreferredSize(new Dimension(175, UITheme.INPUT_HEIGHT));
        cbStockFilter.addActionListener(e -> applyStockFilter());

        filterLeft.add(lblFilterPrompt);
        filterLeft.add(cbStockFilter);
        filterRow.add(filterLeft, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);
        searchField = UITheme.createSearchField();
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));
        JButton btnAdvancedFilter = UITheme.createSecondaryButton("🔍 Lọc Nâng Cao");

        searchPanel.add(searchField);
        searchPanel.add(btnSearch);
        searchPanel.add(btnAdvancedFilter);
        filterRow.add(searchPanel, BorderLayout.EAST);

        toolbarWrapper.add(filterRow, BorderLayout.SOUTH);

        // ---- Sự kiện ----
        btnAdd.addActionListener(e -> openAddDialog());
        btnDetail.addActionListener(e -> openDetailDialog());
        btnImportCopies.addActionListener(e -> openImportCopiesDialog());
        btnDiscardCopies.addActionListener(e -> openDiscardCopiesDialog());
        btnEdit.addActionListener(e -> openEditDialog());
        btnClone.addActionListener(e -> openCloneDialog());
        btnDelete.addActionListener(e -> deleteSelected());
        btnChart.addActionListener(e -> openCategoryDistributionDialog());
        btnCategory.addActionListener(e -> openCategoryManageDialog());
        btnRefresh.addActionListener(e -> {
            searchField.setText("");
            cbStockFilter.setSelectedIndex(0);
            loadData(null);
        });
        btnSearch.addActionListener(e -> loadData(searchField.getText()));
        searchField.addActionListener(e -> loadData(searchField.getText()));
        btnAdvancedFilter.addActionListener(e -> openAdvancedFilterDialog());

        return toolbarWrapper;
    }

    // ================================================================
    //  Center: Bảng dữ liệu sách
    // ================================================================

    private JScrollPane buildCenter() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 || c == 6 || c == 7 || c == 8 ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        // Độ rộng cột
        int[] widths = {40, 120, 230, 140, 100, 110, 50, 50, 65, 120};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(45);

        // Căn giữa các cột số
        var centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{0, 6, 7}) {
            table.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
        }

        // Cột "Còn Lại" (col 8)
        table.getColumnModel().getColumn(8).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                    setHorizontalAlignment(CENTER);
                    if (!sel) {
                        int avail = val instanceof Integer ? (Integer) val : 0;
                        setForeground(avail > 0 ? UITheme.COLOR_SUCCESS : UITheme.COLOR_DANGER);
                        setFont(UITheme.FONT_BOLD);
                    }
                    return this;
                }
            }
        );

        // Cột "Tình Trạng" (col 9) với màu sắc trực quan
        table.getColumnModel().getColumn(9).setCellRenderer(
            new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                    setHorizontalAlignment(CENTER);
                    if (!sel) {
                        String status = val != null ? val.toString() : "";
                        if (status.contains("Còn sách")) {
                            setForeground(UITheme.COLOR_SUCCESS);
                        } else if (status.contains("Sắp hết")) {
                            setForeground(UITheme.COLOR_WARNING);
                        } else if (status.contains("Hết bản") || status.contains("Đang mượn hết")) {
                            setForeground(UITheme.COLOR_DANGER);
                        } else {
                            setForeground(UITheme.TEXT_MUTED);
                        }
                        setFont(UITheme.FONT_BOLD);
                    }
                    return this;
                }
            }
        );

        // Khi chọn dòng → bật/tắt các nút hành động
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() >= 0;
            btnDetail.setEnabled(selected);
            btnImportCopies.setEnabled(selected);
            btnDiscardCopies.setEnabled(selected);
            btnEdit.setEnabled(selected);
            btnClone.setEnabled(selected);
            btnDelete.setEnabled(selected);
        });

        // Double-click → mở Chi Tiết Sách & Lịch Sử Mượn
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openDetailDialog();
            }

            @Override public void mousePressed(MouseEvent e) {
                handleContextMenu(e);
            }

            @Override public void mouseReleased(MouseEvent e) {
                handleContextMenu(e);
            }
        });

        return UITheme.createTableScrollPane(table);
    }

    private void handleContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            int row = table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
                JPopupMenu popup = createContextMenu();
                popup.show(table, e.getX(), e.getY());
            }
        }
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem miDetail = new JMenuItem("📖  Chi Tiết Sách & Người Mượn");
        JMenuItem miImport = new JMenuItem("📥  Nhập Thêm Bản Sao");
        JMenuItem miDiscard = new JMenuItem("📤  Thanh Lý Bản Sao");
        JMenuItem miEdit = new JMenuItem("✎  Chỉnh Sửa Sách");
        JMenuItem miClone = new JMenuItem("📋  Nhân Bản Sách Mới");
        JMenuItem miDelete = new JMenuItem("✕  Xóa Sách");

        miDetail.addActionListener(e -> openDetailDialog());
        miImport.addActionListener(e -> openImportCopiesDialog());
        miDiscard.addActionListener(e -> openDiscardCopiesDialog());
        miEdit.addActionListener(e -> openEditDialog());
        miClone.addActionListener(e -> openCloneDialog());
        miDelete.addActionListener(e -> deleteSelected());

        menu.add(miDetail);
        menu.addSeparator();
        menu.add(miImport);
        menu.add(miDiscard);
        menu.addSeparator();
        menu.add(miEdit);
        menu.add(miClone);
        menu.addSeparator();
        menu.add(miDelete);

        return menu;
    }

    // ================================================================
    //  Footer: Thanh trạng thái
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);

        statusLabel = UITheme.createMutedLabel("Đang tải dữ liệu...");
        footer.add(statusLabel, BorderLayout.WEST);

        JLabel hint = UITheme.createMutedLabel("Double-click để xem chi tiết • Chuột phải để mở menu thao tác");
        footer.add(hint, BorderLayout.EAST);
        return footer;
    }

    // ================================================================
    //  Logic tải và lọc dữ liệu
    // ================================================================

    /** Tải danh sách sách (keyword null → tất cả). */
    public void loadData(String keyword) {
        statusLabel.setText("Đang tải...");
        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return bookService.searchBooks(keyword);
            }
            @Override protected void done() {
                try {
                    currentBooks = get();
                    applyStockFilter();
                } catch (Exception ex) {
                    UITheme.showError(BookPanel.this, "Lỗi tải dữ liệu:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void filterByCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank() || "Tất cả".equalsIgnoreCase(categoryName)) {
            loadData(null);
            return;
        }
        statusLabel.setText("Đang lọc theo thể loại: " + categoryName + "...");
        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return bookService.advancedSearchBooks(null, categoryName, null, null);
            }
            @Override protected void done() {
                try {
                    currentBooks = get();
                    applyStockFilter();
                    statusLabel.setText(String.format("Thể loại \"%s\": có %d cuốn sách", categoryName, currentBooks.size()));
                } catch (Exception ex) {
                    UITheme.showError(BookPanel.this, "Lỗi lọc thể loại:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadAdvancedData(String keyword, String category, Integer publishYear, Boolean isAvailable) {
        statusLabel.setText("Đang tải (Lọc nâng cao)...");
        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return bookService.advancedSearchBooks(keyword, category, publishYear, isAvailable);
            }
            @Override protected void done() {
                try {
                    currentBooks = get();
                    applyStockFilter();
                } catch (Exception ex) {
                    UITheme.showError(BookPanel.this, "Lỗi lọc nâng cao:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyStockFilter() {
        int filterIdx = cbStockFilter != null ? cbStockFilter.getSelectedIndex() : 0;
        List<Book> displayList = new ArrayList<>();

        for (Book b : currentBooks) {
            if (filterIdx == 1 && b.getAvailableCopies() <= 0) continue; // Chỉ hiện còn sách
            if (filterIdx == 2 && b.getAvailableCopies() != 1) continue; // Chỉ hiện sắp hết (1 bản)
            if (filterIdx == 3 && b.getAvailableCopies() > 0) continue;  // Chỉ hiện hết bản mượn
            displayList.add(b);
        }

        renderTable(displayList);
    }

    private void renderTable(List<Book> books) {
        tableModel.setRowCount(0);
        int idx = 1;
        for (Book b : books) {
            String statusText;
            if (b.getAvailableCopies() > 1) {
                statusText = "Còn sách (" + b.getAvailableCopies() + ")";
            } else if (b.getAvailableCopies() == 1) {
                statusText = "Sắp hết (1 bản)";
            } else if (b.getTotalCopies() > 0) {
                statusText = "Đang mượn hết";
            } else {
                statusText = "Chưa nhập kho";
            }

            tableModel.addRow(new Object[]{
                idx++,
                b.getIsbn(),
                b.getTitle(),
                b.getAuthor(),
                b.getCategory(),
                b.getPublisher(),
                b.getPublishYear(),
                b.getTotalCopies(),
                b.getAvailableCopies(),
                statusText
            });
        }

        statusLabel.setText(String.format("Tổng hiển thị: %d cuốn sách", books.size()));
    }

    /** Lấy Book được chọn từ bảng dựa vào ID hoặc danh sách hiện tại. */
    private Book getSelectedBook() {
        int row = table.getSelectedRow();
        if (row < 0) return null;

        String title = (String) tableModel.getValueAt(row, 2);
        String isbn  = (String) tableModel.getValueAt(row, 1);

        for (Book b : currentBooks) {
            if (isbn != null && !isbn.isBlank() && isbn.equals(b.getIsbn())) return b;
            if (title != null && title.equals(b.getTitle())) return b;
        }
        return null;
    }

    // ================================================================
    //  Nghiệp vụ thực thi
    // ================================================================

    public void openAddDialog() {
        BookDialog dialog = new BookDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadData(searchField.getText());
    }

    private void openDetailDialog() {
        Book book = getSelectedBook();
        if (book == null) { UITheme.showWarning(this, "Vui lòng chọn một cuốn sách."); return; }
        BookDetailDialog dialog = new BookDetailDialog((Frame) SwingUtilities.getWindowAncestor(this), book);
        dialog.setVisible(true);
        if (dialog.isDataChanged()) loadData(searchField.getText());
    }

    private void openImportCopiesDialog() {
        Book book = getSelectedBook();
        if (book == null) { UITheme.showWarning(this, "Vui lòng chọn một cuốn sách."); return; }
        BookStockAdjustDialog dialog = new BookStockAdjustDialog((Frame) SwingUtilities.getWindowAncestor(this), book, true);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadData(searchField.getText());
    }

    private void openDiscardCopiesDialog() {
        Book book = getSelectedBook();
        if (book == null) { UITheme.showWarning(this, "Vui lòng chọn một cuốn sách."); return; }
        if (book.getAvailableCopies() <= 0) {
            UITheme.showWarning(this, "Sách \"" + book.getTitle() + "\" hiện không còn bản nào có sẵn trong kho để thanh lý.");
            return;
        }
        BookStockAdjustDialog dialog = new BookStockAdjustDialog((Frame) SwingUtilities.getWindowAncestor(this), book, false);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadData(searchField.getText());
    }

    private void openCloneDialog() {
        Book book = getSelectedBook();
        if (book == null) { UITheme.showWarning(this, "Vui lòng chọn một cuốn sách để nhân bản."); return; }
        BookDialog dialog = new BookDialog((Frame) SwingUtilities.getWindowAncestor(this), book, true);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadData(searchField.getText());
    }

    private void openEditDialog() {
        Book book = getSelectedBook();
        if (book == null) { UITheme.showWarning(this, "Vui lòng chọn một cuốn sách."); return; }
        BookDialog dialog = new BookDialog((Frame) SwingUtilities.getWindowAncestor(this), book);
        dialog.setVisible(true);
        if (dialog.isSaved()) loadData(searchField.getText());
    }

    private void deleteSelected() {
        Book book = getSelectedBook();
        if (book == null) { UITheme.showWarning(this, "Vui lòng chọn một cuốn sách."); return; }

        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn xóa sách:\n\"" + book.getTitle() + "\"?",
            "Xác nhận xóa");
        if (!confirm) return;

        try {
            bookService.deleteBook(book);
            UITheme.showSuccess(this, "Đã xóa sách \"" + book.getTitle() + "\" thành công.");
            loadData(searchField.getText());
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void openCategoryDistributionDialog() {
        CategoryDistributionDialog dialog = new CategoryDistributionDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            this::filterByCategory
        );
        dialog.setVisible(true);
    }

    private void openCategoryManageDialog() {
        CategoryManageDialog dialog = new CategoryManageDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        if (dialog.isDataChanged()) {
            loadData(searchField.getText());
        }
    }

    private void openAdvancedFilterDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Lọc Nâng Cao", true);
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

        // Category
        JPanel pnlCategory = new JPanel(new BorderLayout(5, 5));
        pnlCategory.setOpaque(false);
        pnlCategory.add(new JLabel("Thể loại:"), BorderLayout.WEST);
        JComboBox<String> cbCategory = new JComboBox<>();
        cbCategory.addItem("Tất cả");
        try {
            List<String> categories = bookService.getAllCategories();
            for (String cat : categories) {
                cbCategory.addItem(cat);
            }
        } catch (Exception ignored) {}
        pnlCategory.add(cbCategory, BorderLayout.CENTER);

        // Publish Year
        JPanel pnlYear = new JPanel(new BorderLayout(5, 5));
        pnlYear.setOpaque(false);
        pnlYear.add(new JLabel("Năm XB:"), BorderLayout.WEST);
        JTextField txtYear = UITheme.createTextField("VD: 2020");
        pnlYear.add(txtYear, BorderLayout.CENTER);

        // Is Available
        JPanel pnlAvailable = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlAvailable.setOpaque(false);
        JCheckBox chkAvailable = new JCheckBox("Chỉ hiện sách có sẵn");
        chkAvailable.setOpaque(false);
        pnlAvailable.add(chkAvailable);

        content.add(pnlKeyword);
        content.add(Box.createVerticalStrut(10));
        content.add(pnlCategory);
        content.add(Box.createVerticalStrut(10));
        content.add(pnlYear);
        content.add(Box.createVerticalStrut(10));
        content.add(pnlAvailable);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(UITheme.BG_WHITE);
        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnFilter = UITheme.createPrimaryButton("Lọc Kết Quả");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnFilter.addActionListener(e -> {
            String kw = txtKeyword.getText();
            String cat = (String) cbCategory.getSelectedItem();
            Integer year = null;
            if (!txtYear.getText().isBlank()) {
                try {
                    year = Integer.parseInt(txtYear.getText().trim());
                } catch (NumberFormatException ex) {
                    UITheme.showError(dialog, "Năm xuất bản không hợp lệ.");
                    return;
                }
            }
            Boolean isAvail = chkAvailable.isSelected();
            dialog.dispose();
            searchField.setText(kw);
            loadAdvancedData(kw, cat, year, isAvail);
        });

        actionPanel.add(btnCancel);
        actionPanel.add(btnFilter);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(actionPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    @Override
    public void refresh() {
        loadData(searchField != null ? searchField.getText() : null);
    }
}
