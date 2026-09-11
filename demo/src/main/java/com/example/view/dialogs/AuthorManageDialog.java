package com.example.view.dialogs;

import com.example.model.Author;
import com.example.model.Book;
import com.example.service.AuthorService;
import com.example.view.UITheme;

import com.example.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * Dialog Quản lý đầu sách theo tác giả (Author Management & Master-Detail Cataloging).
 * Bố cục: Bảng tác giả (Master) + Bảng danh sách sách tương ứng (Detail).
 */
public class AuthorManageDialog extends JDialog {

    private final AuthorService authorService = new AuthorService();

    // UI Master Table (Tác giả)
    private JTable            authorTable;
    private DefaultTableModel authorModel;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JButton           btnEdit, btnDelete;
    private boolean           dataChanged = false;

    // UI Detail Table (Sách của tác giả)
    private JTable            booksTable;
    private DefaultTableModel booksModel;
    private JLabel            detailTitleLabel;

    private static final String[] AUTHOR_COLUMNS = {
        "#", "Tên Tác Giả", "Quốc Tịch", "Năm Sinh – Mất", "Số Đầu Sách", "Tổng Bản Kho"
    };

    private static final String[] BOOK_COLUMNS = {
        "#", "ISBN", "Tên Sách", "Thể Loại", "Nhà Xuất Bản", "Năm XB", "Tổng Bản", "Còn Lại"
    };

    public AuthorManageDialog(Frame parent) {
        super(parent, "Quản Lý Đầu Sách Theo Tác Giả", true);
        initUI();
        setSize(920, 640);
        setMinimumSize(new Dimension(800, 520));
        setLocationRelativeTo(parent);
        loadAuthors(null);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        root.setBackground(UITheme.BG_PRIMARY);
        root.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));
        setContentPane(root);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildCenter(),  BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ================================================================
    //  Header & Toolbar
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);

        header.add(UITheme.createPageHeader("✍️  Quản Lý Đầu Sách Theo Tác Giả",
            "Quản lý hồ sơ tác giả và theo dõi toàn bộ các đầu sách do từng tác giả sáng tác"), BorderLayout.NORTH);

        header.add(buildToolbar(), BorderLayout.CENTER);
        return header;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(UITheme.PAD_MD, 0));
        toolbar.setBackground(UITheme.BG_WHITE);
        toolbar.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_MD, UITheme.PAD_SM, UITheme.PAD_MD)
            )
        );

        // Nút hành động
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("＋  Thêm Tác Giả");
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

        // Tìm kiếm
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        searchField = UITheme.createSearchField();
        searchField.setPreferredSize(new Dimension(200, UITheme.INPUT_HEIGHT));
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));

        searchPanel.add(searchField);
        searchPanel.add(btnSearch);
        toolbar.add(searchPanel, BorderLayout.EAST);

        // Events
        btnAdd.addActionListener(e -> openAddDialog());
        btnEdit.addActionListener(e -> openEditDialog());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadAuthors(null); });
        btnSearch.addActionListener(e -> loadAuthors(searchField.getText()));
        searchField.addActionListener(e -> loadAuthors(searchField.getText()));

        return toolbar;
    }

    // ================================================================
    //  Center: SplitPane (Master Author Table + Detail Books Table)
    // ================================================================

    private JSplitPane buildCenter() {
        // --- 1. Master Table: Authors ---
        authorModel = new DefaultTableModel(AUTHOR_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 4 || c == 5) ? Integer.class : String.class;
            }
        };

        authorTable = new JTable(authorModel);
        UITheme.styleTable(authorTable);

        int[] aWidths = {40, 200, 120, 130, 95, 95};
        for (int i = 0; i < aWidths.length; i++) {
            authorTable.getColumnModel().getColumn(i).setPreferredWidth(aWidths[i]);
        }
        authorTable.getColumnModel().getColumn(0).setMaxWidth(50);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        authorTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        authorTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        authorTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        authorTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        authorTable.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = authorTable.getSelectedRow() >= 0;
            btnEdit.setEnabled(sel);
            btnDelete.setEnabled(sel);
            if (sel) {
                loadBooksForSelectedAuthor();
            } else {
                clearBooksTable();
            }
        });

        authorTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditDialog();
            }
        });

        JPanel masterPanel = new JPanel(new BorderLayout(0, 4));
        masterPanel.setOpaque(false);
        JLabel lblMasterTitle = new JLabel("Danh Sách Tác Giả (Nhấp chọn để xem danh mục đầu sách)");
        lblMasterTitle.setFont(UITheme.FONT_BOLD);
        lblMasterTitle.setForeground(UITheme.TEXT_PRIMARY);
        masterPanel.add(lblMasterTitle, BorderLayout.NORTH);
        masterPanel.add(UITheme.createTableScrollPane(authorTable), BorderLayout.CENTER);

        // --- 2. Detail Table: Books ---
        booksModel = new DefaultTableModel(BOOK_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 5 || c == 6 || c == 7) ? Integer.class : String.class;
            }
        };

        booksTable = new JTable(booksModel);
        UITheme.styleTable(booksTable);

        int[] bWidths = {40, 120, 230, 100, 120, 60, 60, 65};
        for (int i = 0; i < bWidths.length; i++) {
            booksTable.getColumnModel().getColumn(i).setPreferredWidth(bWidths[i]);
        }
        booksTable.getColumnModel().getColumn(0).setMaxWidth(45);
        booksTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        booksTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        booksTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        // Cột Còn Lại: xanh nếu > 0, đỏ nếu = 0
        booksTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
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
        });

        JPanel detailPanel = new JPanel(new BorderLayout(0, 4));
        detailPanel.setOpaque(false);
        detailTitleLabel = new JLabel("📚 Danh mục đầu sách của tác giả");
        detailTitleLabel.setFont(UITheme.FONT_BOLD);
        detailTitleLabel.setForeground(UITheme.ACCENT_PRIMARY);
        detailPanel.add(detailTitleLabel, BorderLayout.NORTH);
        detailPanel.add(UITheme.createTableScrollPane(booksTable), BorderLayout.CENTER);

        // SplitPane dọc
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, masterPanel, detailPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        return splitPane;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);

        statusLabel = UITheme.createMutedLabel("Đang tải danh sách tác giả...");
        footer.add(statusLabel, BorderLayout.WEST);

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        btnClose.setPreferredSize(new Dimension(80, 30));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose, BorderLayout.EAST);
        return footer;
    }

    // ================================================================
    //  Data Loading & Actions
    // ================================================================

    /**
     * Đồng bộ tác giả từ bảng books vào bảng authors.
     * Tự động:
     * 1. Loại bỏ các dữ liệu rác/test (nếu có từ quá trình kiểm thử trước đó).
     * 2. INSERT các tác giả mới từ bảng books mà chưa có trong authors.
     * 3. Bổ sung thông tin tác giả mẫu chuẩn hóa nếu chưa có.
     * Đảm bảo dữ liệu luôn nhất quán giữa chức năng Quản lý sách và Quản lý đầu sách theo tác giả.
     */
    private void syncAuthorsFromBooks() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (Statement stmt = conn.createStatement()) {
                // 1. Loại bỏ dữ liệu rác từ các lần test trước (nếu có)
                stmt.executeUpdate("DELETE FROM authors WHERE name LIKE 'Duplicate Author%' OR name LIKE 'Test Author%' OR name LIKE 'Invalid Lifespan%'");

                // 2. Đồng bộ các tác giả từ bảng books sang bảng authors nếu chưa có
                stmt.execute(
                    "INSERT OR IGNORE INTO authors (name) " +
                    "SELECT DISTINCT TRIM(author) FROM books " +
                    "WHERE author IS NOT NULL AND TRIM(author) != ''"
                );

                // 3. Bổ sung thông tin hồ sơ cho các tác giả mẫu mặc định nếu chưa có
                stmt.executeUpdate("UPDATE authors SET nationality = 'Việt Nam', birth_year = 1920, death_year = 2014, biography = 'Nhà văn lớn của nền văn học hiện đại Việt Nam, tác giả Dế Mèn Phiêu Lưu Ký' WHERE name = 'Tô Hoài' AND (nationality IS NULL OR nationality = '')");
                stmt.executeUpdate("UPDATE authors SET nationality = 'Việt Nam', birth_year = 1912, death_year = 1939, biography = 'Nhà văn, nhà báo trào phúng xuất sắc của văn học Việt Nam' WHERE name = 'Vũ Trọng Phụng' AND (nationality IS NULL OR nationality = '')");
                stmt.executeUpdate("UPDATE authors SET nationality = 'Brazil', birth_year = 1947, biography = 'Tiểu thuyết gia nổi tiếng người Brazil, tác giả cuốn Nhà Giả Kim' WHERE name = 'Paulo Coelho' AND (nationality IS NULL OR nationality = '')");
                stmt.executeUpdate("UPDATE authors SET nationality = 'Mỹ', birth_year = 1888, death_year = 1955, biography = 'Nhà văn và nhà thuyết trình người Mỹ, tác giả cuốn Đắc Nhân Tâm' WHERE name = 'Dale Carnegie' AND (nationality IS NULL OR nationality = '')");
                stmt.executeUpdate("UPDATE authors SET nationality = 'Việt Nam', birth_year = 1980, biography = 'Chuyên gia công nghệ thông tin và tác giả nhiều đầu sách lập trình' WHERE name = 'Nguyễn Văn An' AND (nationality IS NULL OR nationality = '')");
            }
        } catch (Exception e) {
            System.err.println("[AuthorManageDialog] Lỗi đồng bộ tác giả từ bảng books: " + e.getMessage());
        }
    }

    private void loadAuthors(String keyword) {
        statusLabel.setText("Đang tải dữ liệu...");
        SwingWorker<List<Author>, Void> worker = new SwingWorker<>() {
            @Override protected List<Author> doInBackground() throws Exception {
                // Đồng bộ tác giả từ bảng books trước khi load
                syncAuthorsFromBooks();
                return authorService.searchAuthors(keyword);
            }
            @Override protected void done() {
                try {
                    List<Author> authors = get();
                    authorModel.setRowCount(0);
                    int idx = 1;
                    for (Author a : authors) {
                        authorModel.addRow(new Object[]{
                            idx++,
                            a.getName(),
                            (a.getNationality() != null && !a.getNationality().isBlank()) ? a.getNationality() : "—",
                            a.getLifespan(),
                            a.getBookCount(),
                            a.getTotalCopies()
                        });
                    }
                    statusLabel.setText("Tổng: " + authors.size() + " tác giả"
                        + (keyword != null && !keyword.isBlank() ? " (từ khóa: \"" + keyword + "\")" : ""));

                    btnEdit.setEnabled(false);
                    btnDelete.setEnabled(false);
                    clearBooksTable();

                    // Tự động chọn dòng đầu tiên nếu có
                    if (!authors.isEmpty()) {
                        authorTable.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception ex) {
                    UITheme.showError(AuthorManageDialog.this, "Lỗi tải dữ liệu tác giả:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadBooksForSelectedAuthor() {
        int row = authorTable.getSelectedRow();
        if (row < 0) {
            clearBooksTable();
            return;
        }

        int modelRow = authorTable.convertRowIndexToModel(row);
        String authorName = (String) authorModel.getValueAt(modelRow, 1);
        detailTitleLabel.setText("📚 Danh mục đầu sách của tác giả: \"" + authorName + "\"");

        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return authorService.getBooksByAuthor(authorName);
            }
            @Override protected void done() {
                try {
                    List<Book> books = get();
                    booksModel.setRowCount(0);
                    int idx = 1;
                    for (Book b : books) {
                        booksModel.addRow(new Object[]{
                            idx++,
                            b.getIsbn(),
                            b.getTitle(),
                            b.getCategory(),
                            b.getPublisher(),
                            b.getPublishYear(),
                            b.getTotalCopies(),
                            b.getAvailableCopies()
                        });
                    }
                    detailTitleLabel.setText("📚 Danh mục đầu sách của tác giả: \"" + authorName + "\" (" + books.size() + " đầu sách)");
                } catch (Exception ex) {
                    detailTitleLabel.setText("Lỗi nạp sách: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void clearBooksTable() {
        booksModel.setRowCount(0);
        detailTitleLabel.setText("📚 Danh mục đầu sách của tác giả (chưa chọn tác giả)");
    }

    private Author getSelectedAuthor() {
        int row = authorTable.getSelectedRow();
        if (row < 0) return null;
        int modelRow = authorTable.convertRowIndexToModel(row);
        String name = (String) authorModel.getValueAt(modelRow, 1);
        try {
            return authorService.getAuthorByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private void openAddDialog() {
        AuthorDialog dlg = new AuthorDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            dataChanged = true;
            loadAuthors(searchField.getText());
        }
    }

    private void openEditDialog() {
        Author a = getSelectedAuthor();
        if (a == null) {
            UITheme.showWarning(this, "Vui lòng chọn một tác giả.");
            return;
        }
        AuthorDialog dlg = new AuthorDialog(this, a);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            dataChanged = true;
            loadAuthors(searchField.getText());
        }
    }

    private void deleteSelected() {
        Author a = getSelectedAuthor();
        if (a == null) {
            UITheme.showWarning(this, "Vui lòng chọn một tác giả.");
            return;
        }

        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn xóa tác giả:\n\"" + a.getName() + "\"?",
            "Xác nhận xóa tác giả");
        if (!confirm) return;

        try {
            authorService.deleteAuthor(a.getId());
            dataChanged = true;
            UITheme.showSuccess(this, "Đã xóa tác giả \"" + a.getName() + "\" thành công.");
            loadAuthors(searchField.getText());
        } catch (IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi xóa tác giả:\n" + ex.getMessage());
        }
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}
