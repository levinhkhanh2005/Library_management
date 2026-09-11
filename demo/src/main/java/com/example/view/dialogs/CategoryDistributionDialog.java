package com.example.view.dialogs;

import com.example.model.CategoryBookStat;
import com.example.service.BookService;
import com.example.view.UITheme;
import com.example.view.panels.PieChartPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog hiển thị biểu đồ tròn phân bố thể loại sách chiếm bao nhiêu % trong tổng số sách.
 * Kèm bảng thống kê chi tiết và chức năng chọn thể loại để lọc sách.
 */
public class CategoryDistributionDialog extends JDialog {

    private final BookService bookService = new BookService();
    private PieChartPanel pieChartPanel;
    private DefaultTableModel tableModel;
    private JTable table;
    private JRadioButton rbTitleCount;
    private JRadioButton rbTotalCopies;
    private JLabel lblTotalCategories;
    private JLabel lblTopCategory;
    private JLabel lblTotalBooksCount;
    private String selectedCategoryForFilter = null;
    private Consumer<String> onCategoryFilterCallback;

    public CategoryDistributionDialog(Frame parent, Consumer<String> onCategoryFilterCallback) {
        super(parent, "Biểu Đồ Tròn Phân Bố Thể Loại Sách (%)", true);
        this.onCategoryFilterCallback = onCategoryFilterCallback;

        initUI();
        loadData(true);
        setSize(880, 580);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_PRIMARY);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.ACCENT_PRIMARY);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("📊  Thống Kê & Phân Bố Thể Loại Sách");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Biểu đồ tròn thể hiện tỷ lệ phần trăm (%) của từng thể loại trong tổng số sách thư viện");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));

        // Switch mode inside header
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        switchPanel.setOpaque(false);

        JLabel lblMode = new JLabel("Thống kê theo:");
        lblMode.setFont(UITheme.FONT_BOLD);
        lblMode.setForeground(Color.WHITE);

        rbTitleCount = new JRadioButton("Số Đầu Sách", true);
        rbTotalCopies = new JRadioButton("Tổng Bản Sao", false);
        rbTitleCount.setFont(UITheme.FONT_BOLD);
        rbTotalCopies.setFont(UITheme.FONT_BOLD);
        rbTitleCount.setForeground(Color.WHITE);
        rbTotalCopies.setForeground(Color.WHITE);
        rbTitleCount.setOpaque(false);
        rbTotalCopies.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbTitleCount);
        bg.add(rbTotalCopies);

        rbTitleCount.addActionListener(e -> loadData(true));
        rbTotalCopies.addActionListener(e -> loadData(false));

        switchPanel.add(lblMode);
        switchPanel.add(rbTitleCount);
        switchPanel.add(rbTotalCopies);

        header.add(title, BorderLayout.WEST);
        header.add(switchPanel, BorderLayout.EAST);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Body: 2 Columns (Trái: Biểu đồ tròn, Phải: Thống kê & Bảng)
        JPanel content = new JPanel(new GridLayout(1, 2, UITheme.PAD_MD, 0));
        content.setBackground(UITheme.BG_PRIMARY);
        content.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD));

        // Cột Trái: Card Biểu Đồ Tròn
        JPanel leftCard = UITheme.createCard();
        leftCard.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        leftCard.setBorder(BorderFactory.createCompoundBorder(
            leftCard.getBorder(),
            new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel chartTitle = new JLabel("🍩  Biểu Đồ Tròn Tỷ Lệ % Thể Loại", SwingConstants.CENTER);
        chartTitle.setFont(UITheme.FONT_BOLD);
        chartTitle.setForeground(UITheme.TEXT_PRIMARY);
        leftCard.add(chartTitle, BorderLayout.NORTH);

        pieChartPanel = new PieChartPanel();
        pieChartPanel.setOnCategorySelectListener(categoryName -> {
            selectCategoryInTable(categoryName);
        });
        leftCard.add(pieChartPanel, BorderLayout.CENTER);

        JLabel chartHint = new JLabel("💡 Rê chuột vào từng múi để xem chi tiết • Click múi để chọn", SwingConstants.CENTER);
        chartHint.setFont(UITheme.FONT_SMALL);
        chartHint.setForeground(UITheme.TEXT_MUTED);
        leftCard.add(chartHint, BorderLayout.SOUTH);

        content.add(leftCard);

        // Cột Phải: Thẻ tóm tắt + Bảng dữ liệu
        JPanel rightCard = UITheme.createCard();
        rightCard.setLayout(new BorderLayout(0, UITheme.PAD_SM));
        rightCard.setBorder(BorderFactory.createCompoundBorder(
            rightCard.getBorder(),
            new EmptyBorder(12, 12, 12, 12)
        ));

        // Top mini stat row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 8, 0));
        statsRow.setOpaque(false);
        lblTotalCategories = new JLabel("—", SwingConstants.CENTER);
        lblTopCategory = new JLabel("—", SwingConstants.CENTER);
        lblTotalBooksCount = new JLabel("—", SwingConstants.CENTER);

        statsRow.add(createStatBox("Số Thể Loại", lblTotalCategories, UITheme.ACCENT_PRIMARY));
        statsRow.add(createStatBox("Nhiều Nhất", lblTopCategory, UITheme.COLOR_SUCCESS));
        statsRow.add(createStatBox("Tổng Sách", lblTotalBooksCount, UITheme.COLOR_INFO));
        rightCard.add(statsRow, BorderLayout.NORTH);

        // Table
        String[] cols = {"Màu", "Thể Loại", "Số Đầu Sách", "Tổng Bản", "Tỷ Lệ %"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UITheme.styleTable(table);

        // Column widths & renderers
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(2).setMaxWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        table.getColumnModel().getColumn(4).setMaxWidth(85);

        // Renderer màu sắc cột 0 (Color box)
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                JPanel p = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (val instanceof Color col) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(col);
                            g2.fillRoundRect(8, 6, getWidth() - 16, getHeight() - 12, 6, 6);
                            g2.dispose();
                        }
                    }
                };
                p.setBackground(sel ? UITheme.TABLE_ROW_SELECTED : (r % 2 == 0 ? UITheme.TABLE_ROW_EVEN : UITheme.TABLE_ROW_ODD));
                return p;
            }
        });

        // Renderer căn giữa và đậm cho cột %
        DefaultTableCellRenderer centerBold = new DefaultTableCellRenderer();
        centerBold.setHorizontalAlignment(SwingConstants.CENTER);
        centerBold.setFont(UITheme.FONT_BOLD);
        table.getColumnModel().getColumn(2).setCellRenderer(centerBold);
        table.getColumnModel().getColumn(3).setCellRenderer(centerBold);

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                setFont(UITheme.FONT_BOLD);
                if (!sel) setForeground(UITheme.ACCENT_PRIMARY);
                return this;
            }
        });

        JScrollPane spTable = UITheme.createTableScrollPane(table);
        rightCard.add(spTable, BorderLayout.CENTER);

        // Nút lọc theo thể loại đang chọn
        JPanel tableBottom = new JPanel(new BorderLayout(8, 0));
        tableBottom.setOpaque(false);
        JButton btnFilterThisCategory = UITheme.createPrimaryButton("🔍  Lọc Sách Theo Thể Loại Đang Chọn");
        btnFilterThisCategory.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String cat = (String) tableModel.getValueAt(row, 1);
                applyFilterAndClose(cat);
            } else {
                UITheme.showWarning(this, "Vui lòng chọn một thể loại trong bảng hoặc biểu đồ.");
            }
        });
        tableBottom.add(btnFilterThisCategory, BorderLayout.CENTER);
        rightCard.add(tableBottom, BorderLayout.SOUTH);

        content.add(rightCard);
        root.add(content, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton btnClose = UITheme.createSecondaryButton("Đóng");
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void loadData(boolean byTitle) {
        SwingWorker<List<CategoryBookStat>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<CategoryBookStat> doInBackground() throws Exception {
                return bookService.getCategoryDistribution(byTitle);
            }

            @Override
            protected void done() {
                try {
                    List<CategoryBookStat> stats = get();
                    pieChartPanel.setData(stats, byTitle);

                    tableModel.setRowCount(0);
                    int totalTitles = 0;
                    int totalCopies = 0;
                    String topName = "—";
                    double maxPct = -1;

                    for (int i = 0; i < stats.size(); i++) {
                        CategoryBookStat s = stats.get(i);
                        totalTitles += s.getTitleCount();
                        totalCopies += s.getTotalCopies();
                        Color c = pieChartPanel.getColorForIndex(i);

                        tableModel.addRow(new Object[]{
                            c,
                            s.getCategoryName(),
                            s.getTitleCount(),
                            s.getTotalCopies(),
                            s.getFormattedPercentage()
                        });

                        if (s.getPercentage() > maxPct) {
                            maxPct = s.getPercentage();
                            topName = s.getCategoryName() + " (" + s.getFormattedPercentage() + ")";
                        }
                    }

                    lblTotalCategories.setText(String.valueOf(stats.size()));
                    lblTopCategory.setText(topName);
                    lblTotalBooksCount.setText(byTitle ? (totalTitles + " đầu sách") : (totalCopies + " bản"));

                } catch (Exception ex) {
                    UITheme.showError(CategoryDistributionDialog.this, "Lỗi tải thống kê thể loại:\n" + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void selectCategoryInTable(String categoryName) {
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            if (categoryName.equalsIgnoreCase(String.valueOf(tableModel.getValueAt(r, 1)))) {
                table.setRowSelectionInterval(r, r);
                table.scrollRectToVisible(table.getCellRect(r, 0, true));
                break;
            }
        }
    }

    private void applyFilterAndClose(String categoryName) {
        this.selectedCategoryForFilter = categoryName;
        if (onCategoryFilterCallback != null) {
            onCategoryFilterCallback.accept(categoryName);
        }
        dispose();
    }

    private JPanel createStatBox(String title, JLabel valLabel, Color accentColor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(UITheme.BG_PRIMARY);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            new EmptyBorder(6, 6, 6, 6)
        ));
        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_MUTED);

        valLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        valLabel.setForeground(accentColor);

        p.add(lbl, BorderLayout.NORTH);
        p.add(valLabel, BorderLayout.CENTER);
        return p;
    }

    public String getSelectedCategoryForFilter() {
        return selectedCategoryForFilter;
    }
}
