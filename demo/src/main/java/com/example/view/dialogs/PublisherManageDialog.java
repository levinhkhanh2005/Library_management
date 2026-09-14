package com.example.view.dialogs;

import com.example.model.Book;
import com.example.model.Publisher;
import com.example.service.PublisherService;
import com.example.util.DatabaseConnection;
import com.example.view.UITheme;

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
 * Dialog Quản lý Nhà Xuất Bản (Publisher Management & Master-Detail Cataloging).
 * Bố cục: Bảng Nhà Xuất Bản (Master) + Bảng danh sách sách tương ứng (Detail).
 */
public class PublisherManageDialog extends JDialog {

    private final PublisherService publisherService = new PublisherService();

    // UI Master Table (Nhà Xuất Bản)
    private JTable            publisherTable;
    private DefaultTableModel publisherModel;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JButton           btnEdit, btnDelete;
    private boolean           dataChanged = false;

    // UI Detail Table (Sách của NXB)
    private JTable            booksTable;
    private DefaultTableModel booksModel;
    private JLabel            detailTitleLabel;

    private static final String[] PUBLISHER_COLUMNS = {
        "#", "Tên Nhà Xuất Bản", "Người Đại Diện", "Số Điện Thoại", "Email", "Số Đầu Sách", "Tổng Bản Kho"
    };

    private static final String[] BOOK_COLUMNS = {
        "#", "ISBN", "Tên Sách", "Tác Giả", "Thể Loại", "Năm XB", "Tổng Bản", "Còn Lại"
    };

    public PublisherManageDialog(Frame parent) {
        super(parent, "Quản Lý Nhà Xuất Bản", true);
        initUI();
        setSize(960, 640);
        setMinimumSize(new Dimension(840, 520));
        setLocationRelativeTo(parent);
        loadPublishers(null);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        root.setBackground(UITheme.BG_PRIMARY);
        root.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    // ================================================================
    //  Header & Toolbar
    // ================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);

        header.add(UITheme.createPageHeader("🏢  Quản Lý Nhà Xuất Bản",
            "Quản lý hồ sơ nhà xuất bản và theo dõi toàn bộ các đầu sách phát hành theo từng NXB"), BorderLayout.NORTH);

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

        JButton btnAdd = UITheme.createPrimaryButton("＋  Thêm NXB");
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
        searchField.setPreferredSize(new Dimension(220, UITheme.INPUT_HEIGHT));
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));

        searchPanel.add(searchField);
        searchPanel.add(btnSearch);
        toolbar.add(searchPanel, BorderLayout.EAST);

        // Events
        btnAdd.addActionListener(e -> openAddDialog());
        btnEdit.addActionListener(e -> openEditDialog());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadPublishers(null); });
        btnSearch.addActionListener(e -> loadPublishers(searchField.getText()));
        searchField.addActionListener(e -> loadPublishers(searchField.getText()));

        return toolbar;
    }

    // ================================================================
    //  Center: SplitPane (Master Publisher Table + Detail Books Table)
    // ================================================================

    private JSplitPane buildCenter() {
        // --- 1. Master Table: Publishers ---
        publisherModel = new DefaultTableModel(PUBLISHER_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 5 || c == 6) ? Integer.class : String.class;
            }
        };

        publisherTable = new JTable(publisherModel);
        UITheme.styleTable(publisherTable);

        int[] pWidths = {40, 200, 130, 110, 160, 95, 95};
        for (int i = 0; i < pWidths.length; i++) {
            publisherTable.getColumnModel().getColumn(i).setPreferredWidth(pWidths[i]);
        }
        publisherTable.getColumnModel().getColumn(0).setMaxWidth(50);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        publisherTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        publisherTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        publisherTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        publisherTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        publisherTable.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = publisherTable.getSelectedRow() >= 0;
            btnEdit.setEnabled(sel);
            btnDelete.setEnabled(sel);
            if (sel) {
                loadBooksForSelectedPublisher();
            } else {
                clearBooksTable();
            }
        });

        publisherTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditDialog();
            }
        });

        JPanel masterPanel = new JPanel(new BorderLayout(0, 4));
        masterPanel.setOpaque(false);
        JLabel lblMasterTitle = new JLabel("Danh Sách Nhà Xuất Bản (Nhấp chọn để xem danh mục đầu sách)");
        lblMasterTitle.setFont(UITheme.FONT_BOLD);
        lblMasterTitle.setForeground(UITheme.TEXT_PRIMARY);
        masterPanel.add(lblMasterTitle, BorderLayout.NORTH);
        masterPanel.add(UITheme.createTableScrollPane(publisherTable), BorderLayout.CENTER);

        // --- 2. Detail Table: Books ---
        booksModel = new DefaultTableModel(BOOK_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 5 || c == 6 || c == 7) ? Integer.class : String.class;
            }
        };

        booksTable = new JTable(booksModel);
        UITheme.styleTable(booksTable);

        int[] bWidths = {40, 120, 230, 140, 100, 60, 60, 65};
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
        detailTitleLabel = new JLabel("📚 Danh mục đầu sách của nhà xuất bản");
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

        statusLabel = UITheme.createMutedLabel("Đang tải danh sách nhà xuất bản...");
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
     * Đồng bộ nhà xuất bản từ bảng books vào bảng publishers và cập nhật thông tin chuẩn hóa.
     */
    private void syncPublishersFromBooks() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (Statement stmt = conn.createStatement()) {
                // 1. Chuẩn hóa tên NXB trong bảng books về dạng đầy đủ có tiền tố NXB
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Kim Đồng' WHERE publisher LIKE '%Kim Đồng' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Văn Học' WHERE publisher LIKE '%Văn Học' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Thông Tin' WHERE publisher LIKE '%Thông Tin' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Hội Nhà Văn' WHERE publisher LIKE '%Hội Nhà Văn' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Tổng Hợp' WHERE publisher LIKE '%Tổng Hợp' AND publisher NOT LIKE 'NXB%'");

                // 2. Loại bỏ NXB viết tắt nếu đã có NXB chuẩn hóa
                stmt.executeUpdate("DELETE FROM publishers WHERE (name LIKE '%Kim Đồng' OR name LIKE '%Kim Dong') AND name NOT LIKE 'NXB%'");

                // 3. Đồng bộ các NXB từ bảng books sang bảng publishers nếu chưa có
                stmt.execute(
                    "INSERT OR IGNORE INTO publishers (name) " +
                    "SELECT DISTINCT TRIM(publisher) FROM books " +
                    "WHERE publisher IS NOT NULL AND TRIM(publisher) != ''"
                );

                // 4. Bổ sung thông tin hồ sơ cho các NXB mẫu mặc định nếu còn để trống
                stmt.executeUpdate("UPDATE publishers SET address = '55 Quang Trung, Hai Bà Trưng, Hà Nội', phone = '1900571595', email = 'cskh@nxbkimdong.com.vn', website = 'https://nxbkimdong.com.vn', representative = 'Bùi Tuấn Nghĩa', description = 'Nhà xuất bản chuyên xuất bản sách cho thiếu nhi và thanh thiếu niên' WHERE name = 'NXB Kim Đồng' AND (address IS NULL OR address = '')");
                stmt.executeUpdate("UPDATE publishers SET address = '18 Nguyễn Trường Tộ, Ba Đình, Hà Nội', phone = '02437161518', email = 'nxbvanhoc@gmail.com', website = 'http://nxbvanhoc.com.vn', representative = 'Nguyễn Anh Vũ', description = 'Nhà xuất bản văn học nghệ thuật lâu đời của Việt Nam' WHERE name = 'NXB Văn Học' AND (address IS NULL OR address = '')");
                stmt.executeUpdate("UPDATE publishers SET address = '115 Trần Duy Hưng, Cầu Giấy, Hà Nội', phone = '02435565928', email = 'nxb.tttt@mic.gov.vn', website = 'https://nxbthongtintruyenthong.vn', representative = 'Trần Chí Đạt', description = 'Nhà xuất bản Thông tin và Truyền thông' WHERE name = 'NXB Thông Tin' AND (address IS NULL OR address = '')");
                stmt.executeUpdate("UPDATE publishers SET address = '65 Nguyễn Du, Hai Bà Trưng, Hà Nội', phone = '02438222135', email = 'nxbhoinhavan@gmail.com', website = 'http://nxbhoinhavan.vn', representative = 'Trần Đăng Khoa', description = 'Đơn vị xuất bản trực thuộc Hội Nhà văn Việt Nam' WHERE name = 'NXB Hội Nhà Văn' AND (address IS NULL OR address = '')");
                stmt.executeUpdate("UPDATE publishers SET address = '62 Nguyễn Thị Minh Khai, Đa Kao, Quận 1, TP.HCM', phone = '02838225340', email = 'tonghop@nxbhcm.com.vn', website = 'https://nxbhcm.com.vn', representative = 'Đinh Thị Thanh Thủy', description = 'Nhà xuất bản Tổng hợp Thành phố Hồ Chí Minh' WHERE name = 'NXB Tổng Hợp' AND (address IS NULL OR address = '')");
            }
        } catch (Exception e) {
            System.err.println("[PublisherManageDialog] Lỗi đồng bộ NXB từ bảng books: " + e.getMessage());
        }
    }

    private void loadPublishers(String keyword) {
        statusLabel.setText("Đang tải dữ liệu...");
        SwingWorker<List<Publisher>, Void> worker = new SwingWorker<>() {
            @Override protected List<Publisher> doInBackground() throws Exception {
                // Đồng bộ NXB từ bảng books trước khi nạp
                syncPublishersFromBooks();
                return publisherService.searchPublishers(keyword);
            }
            @Override protected void done() {
                try {
                    List<Publisher> publishers = get();
                    publisherModel.setRowCount(0);
                    int idx = 1;
                    for (Publisher p : publishers) {
                        publisherModel.addRow(new Object[]{
                            idx++,
                            p.getName(),
                            (p.getRepresentative() != null && !p.getRepresentative().isBlank()) ? p.getRepresentative() : "—",
                            (p.getPhone() != null && !p.getPhone().isBlank()) ? p.getPhone() : "—",
                            (p.getEmail() != null && !p.getEmail().isBlank()) ? p.getEmail() : "—",
                            p.getBookCount(),
                            p.getTotalCopies()
                        });
                    }
                    statusLabel.setText("Tổng: " + publishers.size() + " nhà xuất bản"
                        + (keyword != null && !keyword.isBlank() ? " (từ khóa: \"" + keyword + "\")" : ""));

                    btnEdit.setEnabled(false);
                    btnDelete.setEnabled(false);
                    clearBooksTable();

                    // Tự động chọn dòng đầu tiên nếu có
                    if (!publishers.isEmpty()) {
                        publisherTable.setRowSelectionInterval(0, 0);
                    }
                } catch (Exception ex) {
                    UITheme.showError(PublisherManageDialog.this, "Lỗi tải dữ liệu nhà xuất bản:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadBooksForSelectedPublisher() {
        int row = publisherTable.getSelectedRow();
        if (row < 0) {
            clearBooksTable();
            return;
        }

        int modelRow = publisherTable.convertRowIndexToModel(row);
        String publisherName = (String) publisherModel.getValueAt(modelRow, 1);
        detailTitleLabel.setText("📚 Danh mục đầu sách của NXB: \"" + publisherName + "\"");

        SwingWorker<List<Book>, Void> worker = new SwingWorker<>() {
            @Override protected List<Book> doInBackground() throws Exception {
                return publisherService.getBooksByPublisher(publisherName);
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
                            b.getAuthor(),
                            b.getCategory(),
                            b.getPublishYear(),
                            b.getTotalCopies(),
                            b.getAvailableCopies()
                        });
                    }
                    detailTitleLabel.setText("📚 Danh mục đầu sách của NXB: \"" + publisherName + "\" (" + books.size() + " đầu sách)");
                } catch (Exception ex) {
                    detailTitleLabel.setText("Lỗi nạp sách: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void clearBooksTable() {
        booksModel.setRowCount(0);
        detailTitleLabel.setText("📚 Danh mục đầu sách của nhà xuất bản (chưa chọn NXB)");
    }

    private Publisher getSelectedPublisher() {
        int row = publisherTable.getSelectedRow();
        if (row < 0) return null;
        int modelRow = publisherTable.convertRowIndexToModel(row);
        String name = (String) publisherModel.getValueAt(modelRow, 1);
        try {
            return publisherService.getPublisherByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private void openAddDialog() {
        PublisherDialog dlg = new PublisherDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            dataChanged = true;
            loadPublishers(searchField.getText());
        }
    }

    private void openEditDialog() {
        Publisher p = getSelectedPublisher();
        if (p == null) {
            UITheme.showWarning(this, "Vui lòng chọn một nhà xuất bản.");
            return;
        }
        PublisherDialog dlg = new PublisherDialog(this, p);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            dataChanged = true;
            loadPublishers(searchField.getText());
        }
    }

    private void deleteSelected() {
        Publisher p = getSelectedPublisher();
        if (p == null) {
            UITheme.showWarning(this, "Vui lòng chọn một nhà xuất bản.");
            return;
        }

        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn xóa nhà xuất bản:\n\"" + p.getName() + "\"?",
            "Xác nhận xóa nhà xuất bản");
        if (!confirm) return;

        try {
            publisherService.deletePublisher(p.getId());
            dataChanged = true;
            UITheme.showSuccess(this, "Đã xóa nhà xuất bản \"" + p.getName() + "\" thành công.");
            loadPublishers(searchField.getText());
        } catch (IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi xóa nhà xuất bản:\n" + ex.getMessage());
        }
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}
