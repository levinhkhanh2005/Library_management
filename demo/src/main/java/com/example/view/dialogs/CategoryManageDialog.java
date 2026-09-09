package com.example.view.dialogs;

import com.example.model.Category;
import com.example.service.CategoryService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Dialog Quản lý danh mục thể loại sách (Category Management CRUD).
 */
public class CategoryManageDialog extends JDialog {

    private final CategoryService categoryService = new CategoryService();

    private JTable            table;
    private DefaultTableModel tableModel;
    private JTextField        searchField;
    private JLabel            statusLabel;
    private JButton           btnEdit, btnDelete;
    private boolean           dataChanged = false;

    private static final String[] COLUMNS = {
        "#", "Tên Thể Loại", "Mô Tả", "Số Sách", "Ngày Tạo"
    };

    public CategoryManageDialog(Frame parent) {
        super(parent, "Quản Lý Danh Mục Thể Loại", true);
        initUI();
        setSize(780, 520);
        setMinimumSize(new Dimension(650, 420));
        setLocationRelativeTo(parent);
        loadData(null);
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

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, UITheme.PAD_SM));
        header.setBackground(UITheme.BG_PRIMARY);

        header.add(UITheme.createPageHeader("📂  Quản Lý Danh Mục Thể Loại",
            "Quản lý danh mục thể loại chuẩn hóa, tránh nhập tự do không nhất quán"), BorderLayout.NORTH);

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

        JButton btnAdd = UITheme.createPrimaryButton("＋  Thêm Thể Loại");
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
        JButton btnSearch = UITheme.createPrimaryButton("Tìm");
        btnSearch.setPreferredSize(new Dimension(70, UITheme.INPUT_HEIGHT));

        searchPanel.add(searchField);
        searchPanel.add(btnSearch);
        toolbar.add(searchPanel, BorderLayout.EAST);

        // Events
        btnAdd.addActionListener(e -> openAddDialog());
        btnEdit.addActionListener(e -> openEditDialog());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> { searchField.setText(""); loadData(null); });
        btnSearch.addActionListener(e -> loadData(searchField.getText()));
        searchField.addActionListener(e -> loadData(searchField.getText()));

        return toolbar;
    }

    private JScrollPane buildCenter() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 || c == 3 ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        UITheme.styleTable(table);

        int[] widths = {45, 180, 260, 80, 140};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(100);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() >= 0;
            btnEdit.setEnabled(selected);
            btnDelete.setEnabled(selected);
        });

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openEditDialog();
            }
        });

        return UITheme.createTableScrollPane(table);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);

        statusLabel = UITheme.createMutedLabel("Đang tải danh mục...");
        footer.add(statusLabel, BorderLayout.WEST);

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        btnClose.setPreferredSize(new Dimension(90, UITheme.BUTTON_HEIGHT));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose, BorderLayout.EAST);

        return footer;
    }

    private void loadData(String keyword) {
        statusLabel.setText("Đang tải...");
        SwingWorker<List<Category>, Void> worker = new SwingWorker<>() {
            @Override protected List<Category> doInBackground() throws Exception {
                return (keyword == null || keyword.isBlank())
                    ? categoryService.getAllCategories()
                    : categoryService.searchCategories(keyword);
            }
            @Override protected void done() {
                try {
                    List<Category> categories = get();
                    tableModel.setRowCount(0);
                    int idx = 1;
                    for (Category c : categories) {
                        int bookCount = categoryService.countBooksUsingCategory(c.getName());
                        tableModel.addRow(new Object[]{
                            idx++,
                            c.getName(),
                            c.getDescription() != null ? c.getDescription() : "",
                            bookCount,
                            c.getCreatedAt() != null ? c.getCreatedAt() : ""
                        });
                    }
                    statusLabel.setText("Tổng: " + categories.size() + " thể loại");
                } catch (Exception ex) {
                    UITheme.showError(CategoryManageDialog.this, "Lỗi tải dữ liệu thể loại:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private Category getSelectedCategory() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        String name = (String) tableModel.getValueAt(row, 1);
        try {
            return categoryService.getCategoryByName(name);
        } catch (Exception e) {
            return null;
        }
    }

    private void openAddDialog() {
        CategoryDialog dlg = new CategoryDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            dataChanged = true;
            loadData(searchField.getText());
        }
    }

    private void openEditDialog() {
        Category cat = getSelectedCategory();
        if (cat == null) {
            UITheme.showWarning(this, "Vui lòng chọn một thể loại.");
            return;
        }
        CategoryDialog dlg = new CategoryDialog(this, cat);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            dataChanged = true;
            loadData(searchField.getText());
        }
    }

    private void deleteSelected() {
        Category cat = getSelectedCategory();
        if (cat == null) {
            UITheme.showWarning(this, "Vui lòng chọn một thể loại.");
            return;
        }

        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn xóa thể loại:\n\"" + cat.getName() + "\"?",
            "Xác nhận xóa thể loại");
        if (!confirm) return;

        try {
            categoryService.deleteCategory(cat.getId());
            UITheme.showSuccess(this, "Đã xóa thể loại \"" + cat.getName() + "\" thành công.");
            dataChanged = true;
            loadData(searchField.getText());
        } catch (IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi khi xóa thể loại:\n" + ex.getMessage());
        }
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}
