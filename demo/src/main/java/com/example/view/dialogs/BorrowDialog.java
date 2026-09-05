package com.example.view.dialogs;

import com.example.model.Book;
import com.example.model.Reader;
import com.example.service.BookService;
import com.example.service.BorrowService;
import com.example.service.ReaderService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialog tạo phiếu mượn sách mới.
 * Hỗ trợ tìm kiếm sách và độc giả theo tên hoặc mã.
 */
public class BorrowDialog extends JDialog {

    private final BorrowService  borrowService  = new BorrowService();
    private final BookService    bookService    = new BookService();
    private final ReaderService  readerService  = new ReaderService();
    private boolean              saved = false;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Book search
    private JTextField      bookSearch;
    private JList<Book>     bookList;
    private DefaultListModel<Book> bookListModel;
    private JLabel          lblBookInfo;

    // Reader search
    private JTextField      readerSearch;
    private JList<Reader>   readerList;
    private DefaultListModel<Reader> readerListModel;
    private JLabel          lblReaderInfo;

    // Borrow info
    private JTextField      fDueDate;
    private JTextArea       fNotes;
    private JLabel          lblFineInfo;

    // Selected
    private Book   selectedBook;
    private Reader selectedReader;

    public BorrowDialog(Frame parent) {
        super(parent, "Tạo Phiếu Mượn Sách", true);
        initUI();
        pack();
        setMinimumSize(new Dimension(780, 560));
        setResizable(true);
        setLocationRelativeTo(parent);
    }

    // ================================================================
    //  Xây dựng UI
    // ================================================================

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildBookSection(), buildReaderSection());
        split.setDividerLocation(390);
        split.setBackground(UITheme.BG_WHITE);
        split.setBorder(new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, 0, UITheme.PAD_MD));
        root.add(split, BorderLayout.CENTER);

        root.add(buildBorrowInfoSection(), BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.ACCENT_SECONDARY);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel("📋  Tạo Phiếu Mượn Sách Mới");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel("Chọn sách và độc giả, sau đó thiết lập thời hạn mượn");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xFF, 0xFF, 0xFF, 200));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    // ---- Cột trái: chọn sách ----
    private JPanel buildBookSection() {
        JPanel panel = createGroup("📚  Chọn Sách");
        panel.setLayout(new BorderLayout(0, UITheme.PAD_SM));

        // Tìm kiếm sách
        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        bookSearch = UITheme.createTextField("Tìm theo tên sách, tác giả, ISBN...");
        JButton btn = UITheme.createSecondaryButton("Tìm");
        btn.setPreferredSize(new Dimension(60, UITheme.INPUT_HEIGHT));
        searchRow.add(bookSearch, BorderLayout.CENTER);
        searchRow.add(btn, BorderLayout.EAST);
        panel.add(searchRow, BorderLayout.NORTH);

        // Danh sách sách
        bookListModel = new DefaultListModel<>();
        bookList = new JList<>(bookListModel);
        bookList.setFont(UITheme.FONT_BODY);
        bookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookList.setCellRenderer(new BookListRenderer());
        JScrollPane scroll = new JScrollPane(bookList);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        scroll.setPreferredSize(new Dimension(0, 200));
        panel.add(scroll, BorderLayout.CENTER);

        // Thông tin sách đã chọn
        lblBookInfo = new JLabel("Chưa chọn sách");
        lblBookInfo.setFont(UITheme.FONT_SMALL);
        lblBookInfo.setForeground(UITheme.TEXT_MUTED);
        lblBookInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            new EmptyBorder(6, 10, 6, 10)));
        panel.add(lblBookInfo, BorderLayout.SOUTH);

        // Sự kiện
        Runnable searchBooks = () -> {
            try {
                List<Book> books = bookService.searchBooks(bookSearch.getText());
                bookListModel.clear();
                books.stream().filter(Book::isAvailable).forEach(bookListModel::addElement);
                if (bookListModel.isEmpty()) {
                    lblBookInfo.setText("Không tìm thấy sách còn bản có thể mượn.");
                }
            } catch (Exception ex) {
                UITheme.showError(this, ex.getMessage());
            }
        };

        btn.addActionListener(e -> searchBooks.run());
        bookSearch.addActionListener(e -> searchBooks.run());

        bookList.addListSelectionListener(e -> {
            selectedBook = bookList.getSelectedValue();
            if (selectedBook != null) {
                lblBookInfo.setText("<html><b>" + selectedBook.getTitle() + "</b>"
                    + "  |  " + selectedBook.getAuthor()
                    + "  |  Còn: <font color='#10B981'>"
                    + selectedBook.getAvailableCopies() + " bản</font></html>");
            }
        });

        // Load sách ban đầu
        SwingUtilities.invokeLater(searchBooks::run);
        return panel;
    }

    // ---- Cột phải: chọn độc giả ----
    private JPanel buildReaderSection() {
        JPanel panel = createGroup("👤  Chọn Độc Giả");
        panel.setLayout(new BorderLayout(0, UITheme.PAD_SM));

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        readerSearch = UITheme.createTextField("Tìm theo tên, mã thẻ, điện thoại...");
        JButton btn = UITheme.createSecondaryButton("Tìm");
        btn.setPreferredSize(new Dimension(60, UITheme.INPUT_HEIGHT));
        searchRow.add(readerSearch, BorderLayout.CENTER);
        searchRow.add(btn, BorderLayout.EAST);
        panel.add(searchRow, BorderLayout.NORTH);

        readerListModel = new DefaultListModel<>();
        readerList = new JList<>(readerListModel);
        readerList.setFont(UITheme.FONT_BODY);
        readerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        readerList.setCellRenderer(new ReaderListRenderer());
        JScrollPane scroll = new JScrollPane(readerList);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);

        lblReaderInfo = new JLabel("Chưa chọn độc giả");
        lblReaderInfo.setFont(UITheme.FONT_SMALL);
        lblReaderInfo.setForeground(UITheme.TEXT_MUTED);
        lblReaderInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            new EmptyBorder(6, 10, 6, 10)));
        panel.add(lblReaderInfo, BorderLayout.SOUTH);

        Runnable searchReaders = () -> {
            try {
                List<Reader> readers = readerService.searchReaders(readerSearch.getText());
                readerListModel.clear();
                readers.stream().filter(Reader::canBorrow).forEach(readerListModel::addElement);
            } catch (Exception ex) {
                UITheme.showError(this, ex.getMessage());
            }
        };

        btn.addActionListener(e -> searchReaders.run());
        readerSearch.addActionListener(e -> searchReaders.run());

        readerList.addListSelectionListener(e -> {
            selectedReader = readerList.getSelectedValue();
            if (selectedReader != null) {
                // Truy vấn số sách đang mượn để hiển thị hạn mức
                int activeCount = 0;
                try {
                    activeCount = new BorrowService().getActiveCountByReader(selectedReader.getId());
                } catch (Exception ignored) {}
                int max = BorrowService.MAX_ACTIVE_BORROWS_PER_READER;
                String countColor = activeCount >= max ? "#EF4444" : "#10B981";
                String countText  = activeCount >= max
                    ? "Đang giữ: <font color='" + countColor + "'><b>" + activeCount + "/" + max + " cuốn (Đã đạt hạn mức!)</b></font>"
                    : "Đang giữ: <font color='" + countColor + "'>" + activeCount + "/" + max + " cuốn</font>";

                lblReaderInfo.setText("<html><b>" + selectedReader.getFullName() + "</b>"
                    + "  |  Mã thẻ: " + selectedReader.getReaderCode()
                    + "  |  " + selectedReader.getPhone()
                    + "  |  " + countText + "</html>");
            }
        });

        SwingUtilities.invokeLater(searchReaders::run);
        return panel;
    }

    // ---- Phần dưới: thiết lập phiếu mượn + nút ----
    private JPanel buildBorrowInfoSection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD, UITheme.PAD_MD)
        ));

        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, UITheme.PAD_LG, 0));
        infoRow.setOpaque(false);

        // Hạn trả
        JLabel dueLbl = new JLabel("Hạn Trả:");
        dueLbl.setFont(UITheme.FONT_BOLD);
        fDueDate = UITheme.createTextField("");
        fDueDate.setPreferredSize(new Dimension(120, UITheme.INPUT_HEIGHT));
        fDueDate.setText(LocalDate.now()
            .plusDays(BorrowService.DEFAULT_BORROW_DAYS).format(DATE_FMT));
        JButton btnToday = UITheme.createSecondaryButton("+7 ngày");
        btnToday.setPreferredSize(new Dimension(80, UITheme.INPUT_HEIGHT));
        btnToday.setFont(UITheme.FONT_SMALL);
        btnToday.addActionListener(e -> fDueDate.setText(
            LocalDate.now().plusDays(7).format(DATE_FMT)));
        JButton btn14 = UITheme.createSecondaryButton("+14 ngày");
        btn14.setPreferredSize(new Dimension(88, UITheme.INPUT_HEIGHT));
        btn14.setFont(UITheme.FONT_SMALL);
        btn14.addActionListener(e -> fDueDate.setText(
            LocalDate.now().plusDays(14).format(DATE_FMT)));
        JButton btn30 = UITheme.createSecondaryButton("+30 ngày");
        btn30.setPreferredSize(new Dimension(88, UITheme.INPUT_HEIGHT));
        btn30.setFont(UITheme.FONT_SMALL);
        btn30.addActionListener(e -> fDueDate.setText(
            LocalDate.now().plusDays(30).format(DATE_FMT)));

        // Ghi chú
        JLabel notesLbl = new JLabel("Ghi Chú:");
        notesLbl.setFont(UITheme.FONT_BOLD);
        fNotes = UITheme.createTextArea(1, 20);
        fNotes.setPreferredSize(new Dimension(220, UITheme.INPUT_HEIGHT));

        // Thông tin phạt
        lblFineInfo = new JLabel("Phạt: " + UITheme.formatCurrency(BorrowService.FINE_PER_DAY) + "/ngày quá hạn");
        lblFineInfo.setFont(UITheme.FONT_SMALL);
        lblFineInfo.setForeground(UITheme.COLOR_WARNING);

        infoRow.add(dueLbl);
        infoRow.add(fDueDate);
        infoRow.add(btnToday);
        infoRow.add(btn14);
        infoRow.add(btn30);
        infoRow.add(Box.createHorizontalStrut(UITheme.PAD_MD));
        infoRow.add(notesLbl);
        infoRow.add(fNotes);
        infoRow.add(Box.createHorizontalStrut(UITheme.PAD_MD));
        infoRow.add(lblFineInfo);

        wrapper.add(infoRow, BorderLayout.CENTER);

        // Nút
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnSave   = UITheme.createPrimaryButton("  ✓  Xác Nhận Mượn  ");
        btnCancel.setPreferredSize(new Dimension(100, UITheme.BUTTON_HEIGHT));
        btnSave  .setPreferredSize(new Dimension(160, UITheme.BUTTON_HEIGHT));
        btnCancel.addActionListener(e -> dispose());
        btnSave  .addActionListener(e -> save());
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        btnRow.add(btnCancel);
        btnRow.add(btnSave);
        wrapper.add(btnRow, BorderLayout.EAST);

        return wrapper;
    }

    // ================================================================
    //  Lưu phiếu mượn
    // ================================================================

    private void save() {
        if (selectedBook == null) {
            UITheme.showWarning(this, "Vui lòng chọn một cuốn sách."); return;
        }
        if (selectedReader == null) {
            UITheme.showWarning(this, "Vui lòng chọn một độc giả."); return;
        }
        String dueDate = fDueDate.getText().trim();
        if (dueDate.isBlank()) {
            UITheme.showWarning(this, "Vui lòng nhập hạn trả."); return;
        }

        try {
            borrowService.borrowBook(
                selectedBook.getId(),
                selectedReader.getId(),
                dueDate,
                fNotes.getText().trim()
            );
            UITheme.showSuccess(this,
                "Tạo phiếu mượn thành công!\n"
                + "📚 Sách: " + selectedBook.getTitle() + "\n"
                + "👤 Độc giả: " + selectedReader.getFullName() + "\n"
                + "📅 Hạn trả: " + dueDate);
            saved = true;
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi tạo phiếu mượn:\n" + ex.getMessage());
        }
    }

    public boolean isSaved() { return saved; }

    // ================================================================
    //  Helper
    // ================================================================

    private JPanel createGroup(String title) {
        JPanel g = new JPanel();
        g.setBackground(UITheme.BG_WHITE);
        g.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                "  " + title + "  ",
                TitledBorder.LEFT, TitledBorder.TOP,
                UITheme.FONT_BOLD, UITheme.TEXT_SECONDARY),
            new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_SM, UITheme.PAD_SM, UITheme.PAD_SM)
        ));
        return g;
    }

    // ================================================================
    //  Custom List Cell Renderers
    // ================================================================

    static class BookListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object val,
                int idx, boolean sel, boolean focus) {
            super.getListCellRendererComponent(list, val, idx, sel, focus);
            Book b = (Book) val;
            setText("<html><b>" + b.getTitle() + "</b>"
                + " <font color='#64748B'>— " + b.getAuthor() + "</font>"
                + " &nbsp;<font color='" + (b.getAvailableCopies() > 0 ? "#10B981" : "#EF4444") + "'>"
                + "[Còn: " + b.getAvailableCopies() + "]</font></html>");
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setFont(UITheme.FONT_BODY);
            if (!sel) setBackground(idx % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN);
            return this;
        }
    }

    static class ReaderListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object val,
                int idx, boolean sel, boolean focus) {
            super.getListCellRendererComponent(list, val, idx, sel, focus);
            Reader r = (Reader) val;
            setText("<html><b>" + r.getFullName() + "</b>"
                + " <font color='#64748B'>— " + r.getReaderCode() + "</font>"
                + " <font color='#94A3B8'>" + r.getPhone() + "</font></html>");
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setFont(UITheme.FONT_BODY);
            if (!sel) setBackground(idx % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN);
            return this;
        }
    }
}
