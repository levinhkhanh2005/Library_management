package com.example.view.dialogs;

import com.example.model.Book;
import com.example.service.BookService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog hiển thị chi tiết sách & danh sách độc giả đang mượn sách.
 */
public class BookDetailDialog extends JDialog {

    private final BookService bookService = new BookService();
    private Book              book;
    private boolean           dataChanged = false;

    private JLabel lblStatusBadge;
    private JLabel lblTotalCopies;
    private JLabel lblAvailCopies;
    private JLabel lblBorrowedCopies;
    private DefaultTableModel borrowerModel;

    public BookDetailDialog(Frame parent, Book book) {
        super(parent, "Chi Tiết Sách & Lịch Sử Mượn", true);
        this.book = book;

        initUI();
        loadBorrowers();
        setSize(680, 560);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.ACCENT_PRIMARY);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("📖  " + book.getTitle());
        title.setFont(UITheme.FONT_H2);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Tác giả: " + book.getAuthor() + " • Thể loại: " + (book.getCategory() == null || book.getCategory().isBlank() ? "Chưa phân loại" : book.getCategory()));
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Body with Scroll
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_WHITE);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Block 1: Chi tiết metadata
        JPanel pnlMeta = new JPanel(new GridLayout(3, 2, 10, 8));
        pnlMeta.setOpaque(false);
        pnlMeta.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "  Thông Tin Thư Mục  ",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            UITheme.FONT_BOLD, UITheme.TEXT_SECONDARY
        ));

        pnlMeta.add(createFieldLabel("Mã ISBN", book.getIsbn() == null || book.getIsbn().isBlank() ? "Chưa có ISBN" : book.getIsbn()));
        pnlMeta.add(createFieldLabel("Nhà Xuất Bản", book.getPublisher() == null || book.getPublisher().isBlank() ? "—" : book.getPublisher()));
        pnlMeta.add(createFieldLabel("Năm Xuất Bản", book.getPublishYear() > 0 ? String.valueOf(book.getPublishYear()) : "—"));
        pnlMeta.add(createFieldLabel("Thể Loại", book.getCategory() == null || book.getCategory().isBlank() ? "Chưa phân loại" : book.getCategory()));
        pnlMeta.add(createFieldLabel("Mô Tả", book.getDescription() == null || book.getDescription().isBlank() ? "Không có mô tả" : book.getDescription()));

        String statusStr = "Còn sách";
        Color statusColor = UITheme.COLOR_SUCCESS;
        if (book.getAvailableCopies() == 0) {
            statusStr = "Đã mượn hết (0 bản)";
            statusColor = UITheme.COLOR_DANGER;
        } else if (book.getAvailableCopies() == 1) {
            statusStr = "Sắp hết (còn 1 bản)";
            statusColor = UITheme.COLOR_WARNING;
        }
        lblStatusBadge = new JLabel(statusStr);
        lblStatusBadge.setFont(UITheme.FONT_BOLD);
        lblStatusBadge.setForeground(statusColor);
        pnlMeta.add(createCustomField("Tình Trạng", lblStatusBadge));

        body.add(pnlMeta);
        body.add(Box.createVerticalStrut(12));

        // Block 2: Thẻ kho sách
        JPanel pnlStock = new JPanel(new GridLayout(1, 3, 12, 0));
        pnlStock.setOpaque(false);
        pnlStock.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "  Tình Trạng Kho Bản Sao  ",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            UITheme.FONT_BOLD, UITheme.TEXT_SECONDARY
        ));

        lblTotalCopies = new JLabel(String.valueOf(book.getTotalCopies()), SwingConstants.CENTER);
        lblTotalCopies.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        lblTotalCopies.setForeground(UITheme.TEXT_PRIMARY);

        lblAvailCopies = new JLabel(String.valueOf(book.getAvailableCopies()), SwingConstants.CENTER);
        lblAvailCopies.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        lblAvailCopies.setForeground(UITheme.COLOR_SUCCESS);

        lblBorrowedCopies = new JLabel(String.valueOf(book.getBorrowedCopies()), SwingConstants.CENTER);
        lblBorrowedCopies.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        lblBorrowedCopies.setForeground(UITheme.COLOR_WARNING);

        pnlStock.add(createMiniCard("Tổng Số Bản", lblTotalCopies));
        pnlStock.add(createMiniCard("Bản Có Sẵn", lblAvailCopies));
        pnlStock.add(createMiniCard("Đang Mượn", lblBorrowedCopies));

        body.add(pnlStock);
        body.add(Box.createVerticalStrut(12));

        // Block 3: Bảng độc giả đang mượn cuốn sách này
        JPanel pnlBorrowers = new JPanel(new BorderLayout(0, 6));
        pnlBorrowers.setOpaque(false);
        pnlBorrowers.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "  Độc Giả Đang Mượn Cuốn Sách Này  ",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            UITheme.FONT_BOLD, UITheme.TEXT_SECONDARY
        ));

        String[] cols = {"#", "Mã Thẻ", "Tên Độc Giả", "Số ĐT", "Ngày Mượn", "Hạn Trả", "Trạng Thái"};
        borrowerModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(borrowerModel);
        UITheme.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(35);

        JScrollPane spTable = UITheme.createTableScrollPane(table);
        spTable.setPreferredSize(new Dimension(0, 140));
        pnlBorrowers.add(spTable, BorderLayout.CENTER);

        body.add(pnlBorrowers);
        root.add(body, BorderLayout.CENTER);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        footer.setBackground(UITheme.BG_WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));

        JButton btnImport = UITheme.createSecondaryButton("📥  Nhập Thêm Bản");
        JButton btnDiscard = UITheme.createSecondaryButton("📤  Thanh Lý Bản");
        JButton btnClose = UITheme.createPrimaryButton("Đóng");

        btnImport.addActionListener(e -> {
            BookStockAdjustDialog dlg = new BookStockAdjustDialog((Frame) getParent(), book, true);
            dlg.setVisible(true);
            if (dlg.isSaved()) refreshData();
        });

        btnDiscard.addActionListener(e -> {
            BookStockAdjustDialog dlg = new BookStockAdjustDialog((Frame) getParent(), book, false);
            dlg.setVisible(true);
            if (dlg.isSaved()) refreshData();
        });

        btnClose.addActionListener(e -> dispose());

        footer.add(btnImport);
        footer.add(btnDiscard);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void loadBorrowers() {
        borrowerModel.setRowCount(0);
        try {
            List<Object[]> list = bookService.getActiveBorrowers(book.getId());
            int idx = 1;
            for (Object[] r : list) {
                borrowerModel.addRow(new Object[]{
                    idx++,
                    r[1], // reader_code
                    r[2], // full_name
                    r[3], // phone
                    r[4], // borrow_date
                    r[5], // due_date
                    "OVERDUE".equalsIgnoreCase(String.valueOf(r[6])) ? "⚠ Quá hạn" : "Đang mượn"
                });
            }
        } catch (Exception ex) {
            borrowerModel.addRow(new Object[]{"-", "-", "Lỗi: " + ex.getMessage(), "-", "-", "-", "-"});
        }
    }

    private void refreshData() {
        try {
            Book updated = bookService.getBookById(book.getId());
            if (updated != null) {
                this.book = updated;
                lblTotalCopies.setText(String.valueOf(book.getTotalCopies()));
                lblAvailCopies.setText(String.valueOf(book.getAvailableCopies()));
                lblBorrowedCopies.setText(String.valueOf(book.getBorrowedCopies()));

                String statusStr = "Còn sách";
                Color statusColor = UITheme.COLOR_SUCCESS;
                if (book.getAvailableCopies() == 0) {
                    statusStr = "Đã mượn hết (0 bản)";
                    statusColor = UITheme.COLOR_DANGER;
                } else if (book.getAvailableCopies() == 1) {
                    statusStr = "Sắp hết (còn 1 bản)";
                    statusColor = UITheme.COLOR_WARNING;
                }
                lblStatusBadge.setText(statusStr);
                lblStatusBadge.setForeground(statusColor);

                loadBorrowers();
                dataChanged = true;
            }
        } catch (Exception ignored) {}
    }

    private JPanel createFieldLabel(String fieldName, String value) {
        JPanel p = new JPanel(new BorderLayout(4, 2));
        p.setOpaque(false);
        JLabel lblName = new JLabel(fieldName + ":");
        lblName.setFont(UITheme.FONT_SMALL);
        lblName.setForeground(UITheme.TEXT_MUTED);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(UITheme.FONT_BODY);
        lblVal.setForeground(UITheme.TEXT_PRIMARY);

        p.add(lblName, BorderLayout.NORTH);
        p.add(lblVal, BorderLayout.CENTER);
        return p;
    }

    private JPanel createCustomField(String fieldName, Component comp) {
        JPanel p = new JPanel(new BorderLayout(4, 2));
        p.setOpaque(false);
        JLabel lblName = new JLabel(fieldName + ":");
        lblName.setFont(UITheme.FONT_SMALL);
        lblName.setForeground(UITheme.TEXT_MUTED);

        p.add(lblName, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel createMiniCard(String label, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UITheme.BG_PRIMARY);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(8, 8, 8, 8)
        ));
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        p.add(lbl, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    public boolean isDataChanged() {
        return dataChanged;
    }
}
