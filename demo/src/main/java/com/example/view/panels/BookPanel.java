package com.example.view.panels;

import com.example.model.Book;
import com.example.service.BookService;
import com.example.view.MainFrame;
import com.example.view.UITheme;
import com.example.view.dialogs.BookDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel Quản Lý Sách — bảng danh sách + toolbar + tìm kiếm.
 */
public class BookPanel extends JPanel implements MainFrame.Refreshable {

    private final BookService bookService = new BookService();

    // UI Components
    private JTable          table;
    private DefaultTableModel tableModel;
    private JTextField      searchField;
    private JLabel          statusLabel;
    private JButton         btnEdit, btnDelete;

    // Columns
    private static final String[] COLUMNS = {
        "#", "ISBN", "Tên Sách", "Tác Giả", "Thể Loại", "NXB", "Năm", "Tổng", "Còn Lại"
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
    //  Header: tiêu đề + toolbar + search
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);

        // Tiêu đề trang
        header.add(UITheme.createPageHeader("📚  Quản Lý Sách",
            "Thêm, sửa, xóa và tìm kiếm sách trong thư viện"), BorderLayout.NORTH);

        // Toolbar
        header.add(buildToolbar(), BorderLayout.CENTER);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(UITheme.PAD_MD, 0));
        toolbar.setBackground(UITheme.BG_WHITE);
        toolbar.setBorder(new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD,
                                          UITheme.PAD_SM, UITheme.PAD_MD));
        // Viền đẹp
        toolbar.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD)
            )
        );

        // Bên trái: các nút hành động
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("＋  Thêm Sách");
        btnEdit   = UITheme.createSecondaryButton("✎  Sửa");
        btnDelete = UITheme.createDangerButton("✕  Xóa");
        JButton btnRefresh = UITheme.createSecondaryButton("↺  Làm Mới");

        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);

        btnGroup.add(btnAdd);
        btnGroup.add(btnEdit);
        btnGroup.add(btnDelete);
        btnGroup.add(btnRefresh);
        toolbar.add(btnGroup, BorderLayout.WEST);

        // Bên phải: ô tìm kiếm và nút lọc nâng cao
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        searchField = UITheme.createSearchField();
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));
        
        JButton btnAdvancedFilter = UITheme.createSecondaryButton("🔍 Lọc Nâng Cao");

        searchPanel.add(searchField);
        searchPanel.add(btnSearch);
        searchPanel.add(btnAdvancedFilter);
        toolbar.add(searchPanel, BorderLayout.EAST);

        // ---- Sự kiện ----
        btnAdd.addActionListener(e -> openAddDialog());
        btnEdit.addActionListener(e -> openEditDialog());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadData(null); });
        btnSearch.addActionListener(e -> loadData(searchField.getText()));
        searchField.addActionListener(e -> loadData(searchField.getText())); // Enter
        btnAdvancedFilter.addActionListener(e -> openAdvancedFilterDialog());

        return toolbar;
    }

    // ================================================================
    //  Center: bảng dữ liệu
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
        int[] widths = {45, 130, 260, 160, 110, 130, 55, 55, 70};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);

        // Màu cột "Còn Lại": xanh nếu > 0, đỏ nếu = 0
        table.getColumnModel().getColumn(8).setCellRenderer(
            new javax.swing.table.DefaultTableCellRenderer() {
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

        // Căn giữa cột số
        var centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col : new int[]{0, 6, 7}) {
            table.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
        }

        // Khi chọn dòng → bật/tắt nút
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() >= 0;
            btnEdit.setEnabled(selected);
            btnDelete.setEnabled(selected);
        });

        // Double-click → mở dialog sửa
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditDialog();
            }
        });

        return UITheme.createTableScrollPane(table);
    }

    // ================================================================
    //  Footer: thanh trạng thái
    // ================================================================

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);

        statusLabel = UITheme.createMutedLabel("Đang tải dữ liệu...");
        footer.add(statusLabel, BorderLayout.WEST);

        JLabel hint = UITheme.createMutedLabel("Double-click để chỉnh sửa • Enter để tìm kiếm");
        footer.add(hint, BorderLayout.EAST);
        return footer;
    }

    // ================================================================
    //  Logic
    // ================================================================

    /** Tải danh sách sách (keyword null → tất cả). */
    private void loadData(String keyword) {
        statusLabel.setText("Đang tải...");
        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return bookService.searchBooks(keyword);
            }
            @Override protected void done() {
                updateTableData(this, keyword);
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
                updateTableData(this, "Lọc nâng cao");
            }
        };
        worker.execute();
    }

    private void updateTableData(SwingWorker<List<Book>, Void> worker, String labelContext) {
        try {
            List<Book> books = worker.get();
            tableModel.setRowCount(0);
            int idx = 1;
            for (Book b : books) {
                tableModel.addRow(new Object[]{
                    idx++,
                    b.getIsbn(),
                    b.getTitle(),
                    b.getAuthor(),
                    b.getCategory(),
                    b.getPublisher(),
                    b.getPublishYear(),
                    b.getTotalCopies(),
                    b.getAvailableCopies()
                });
            }
            statusLabel.setText("Tổng: " + books.size() + " cuốn sách"
                + (labelContext != null && !labelContext.isBlank() ? "  (từ khóa/lọc: \"" + labelContext + "\")" : ""));
        } catch (Exception ex) {
            UITheme.showError(BookPanel.this, "Lỗi tải dữ liệu:\n" + ex.getMessage());
        }
    }

    /** Lấy Book được chọn từ bảng. */
    private Book getSelectedBook() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        // Tìm theo ISBN
        String isbn = (String) tableModel.getValueAt(row, 1);
        String title = (String) tableModel.getValueAt(row, 2);
        try {
            if (isbn != null && !isbn.isBlank()) {
                return bookService.getAllBooks().stream()
                    .filter(b -> isbn.equals(b.getIsbn())).findFirst().orElse(null);
            }
            return bookService.getAllBooks().stream()
                .filter(b -> title.equals(b.getTitle())).findFirst().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void openAddDialog() {
        BookDialog dialog = new BookDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
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

    private void openAdvancedFilterDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Lọc Nâng Cao", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);
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
            searchField.setText(kw); // Đồng bộ keyword
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
