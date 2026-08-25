package com.example.view.dialogs;

import com.example.model.Book;
import com.example.service.BookService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Year;

/**
 * Dialog thêm mới hoặc chỉnh sửa thông tin sách.
 */
public class BookDialog extends JDialog {

    private final BookService bookService = new BookService();
    private final Book        editBook;     // null = thêm mới, non-null = sửa
    private boolean           saved = false;

    // ---- Form fields ----
    private JTextField  fIsbn, fTitle, fAuthor, fCategory, fPublisher;
    private JSpinner    fYear, fTotalCopies, fAvailCopies;
    private JTextArea   fDescription;

    public BookDialog(Frame parent, Book book) {
        super(parent, book == null ? "Thêm Sách Mới" : "Chỉnh Sửa Sách", true);
        this.editBook = book;
        initUI();
        if (book != null) populateFields(book);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    // ================================================================
    //  Xây dựng UI
    // ================================================================

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildForm(),     BorderLayout.CENTER);
        root.add(buildButtons(),  BorderLayout.SOUTH);
    }

    /** Thanh tiêu đề màu accent. */
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.ACCENT_PRIMARY);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel(editBook == null ? "📚  Thêm Sách Mới" : "✎  Chỉnh Sửa Sách");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel(editBook == null
            ? "Điền thông tin để thêm sách vào thư viện"
            : "Cập nhật thông tin sách đã có");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    /** Form nhập liệu 2 cột. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        // --- Nhóm Thông Tin Chính ---
        JPanel mainGroup = createGroup("Thông Tin Chính");
        mainGroup.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Hàng 1: ISBN + Năm xuất bản
        g.gridy = 0;
        g.gridx = 0; g.weightx = 0.3;
        mainGroup.add(label("ISBN"), g);
        g.gridx = 1; g.weightx = 0.7;
        fIsbn = UITheme.createTextField("Nhập mã ISBN (tuỳ chọn)");
        mainGroup.add(fIsbn, g);
        g.gridx = 2; g.weightx = 0.2;
        mainGroup.add(label("Năm XB *"), g);
        g.gridx = 3; g.weightx = 0.3;
        fYear = new JSpinner(new SpinnerNumberModel(Year.now().getValue(), 1800, Year.now().getValue() + 1, 1));
        fYear.setFont(UITheme.FONT_BODY);
        fYear.setPreferredSize(new Dimension(90, UITheme.INPUT_HEIGHT));
        ((JSpinner.DefaultEditor) fYear.getEditor()).getTextField().setColumns(6);
        mainGroup.add(fYear, g);

        // Hàng 2: Tên sách (full width)
        g.gridy = 1;
        g.gridx = 0; g.weightx = 0.3;
        mainGroup.add(label("Tên Sách *"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 1.0;
        fTitle = UITheme.createTextField("Nhập tên sách");
        mainGroup.add(fTitle, g);
        g.gridwidth = 1;

        // Hàng 3: Tác giả (full width)
        g.gridy = 2;
        g.gridx = 0; g.weightx = 0.3;
        mainGroup.add(label("Tác Giả *"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 1.0;
        fAuthor = UITheme.createTextField("Nhập tên tác giả");
        mainGroup.add(fAuthor, g);
        g.gridwidth = 1;

        // Hàng 4: Thể loại + Nhà xuất bản
        g.gridy = 3;
        g.gridx = 0; g.weightx = 0.3;
        mainGroup.add(label("Thể Loại"), g);
        g.gridx = 1; g.weightx = 0.7;
        fCategory = UITheme.createTextField("VD: Văn học, Khoa học...");
        mainGroup.add(fCategory, g);
        g.gridx = 2; g.weightx = 0.2;
        mainGroup.add(label("Nhà XB"), g);
        g.gridx = 3; g.weightx = 0.3;
        fPublisher = UITheme.createTextField("Tên NXB");
        mainGroup.add(fPublisher, g);

        form.add(mainGroup, BorderLayout.NORTH);

        // --- Nhóm Số Lượng + Mô tả ---
        JPanel bottomGroup = new JPanel(new GridLayout(1, 2, UITheme.PAD_MD, 0));
        bottomGroup.setOpaque(false);

        // Số lượng
        JPanel qtyGroup = createGroup("Số Lượng Bản Sao");
        qtyGroup.setLayout(new GridBagLayout());
        GridBagConstraints q = new GridBagConstraints();
        q.insets = new Insets(5, 8, 5, 8);
        q.fill   = GridBagConstraints.HORIZONTAL;

        q.gridy = 0; q.gridx = 0;
        qtyGroup.add(label("Tổng số bản *"), q);
        q.gridx = 1;
        fTotalCopies = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        fTotalCopies.setFont(UITheme.FONT_BODY);
        fTotalCopies.setPreferredSize(new Dimension(80, UITheme.INPUT_HEIGHT));
        qtyGroup.add(fTotalCopies, q);

        q.gridy = 1; q.gridx = 0;
        qtyGroup.add(label("Số bản còn lại"), q);
        q.gridx = 1;
        fAvailCopies = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        fAvailCopies.setFont(UITheme.FONT_BODY);
        fAvailCopies.setPreferredSize(new Dimension(80, UITheme.INPUT_HEIGHT));
        if (editBook == null) {
            // Thêm mới: tự đồng bộ available = total
            fTotalCopies.addChangeListener(e ->
                fAvailCopies.setValue(fTotalCopies.getValue()));
        }
        qtyGroup.add(fAvailCopies, q);

        // Mô tả
        JPanel descGroup = createGroup("Mô Tả");
        descGroup.setLayout(new BorderLayout());
        fDescription = UITheme.createTextArea(4, 20);
        fDescription.setBackground(UITheme.BG_WHITE);
        JScrollPane descScroll = new JScrollPane(fDescription);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        descScroll.setPreferredSize(new Dimension(0, 100));
        descGroup.add(descScroll, BorderLayout.CENTER);

        bottomGroup.add(qtyGroup);
        bottomGroup.add(descGroup);
        form.add(bottomGroup, BorderLayout.CENTER);

        return form;
    }

    /** Row nút hành động. */
    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.PAD_SM, UITheme.PAD_MD));
        panel.setBackground(UITheme.BG_WHITE);
        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
                new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG)
            )
        );

        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnSave   = UITheme.createPrimaryButton(editBook == null ? "  ＋  Thêm Sách  " : "  ✓  Lưu Thay Đổi  ");

        btnCancel.setPreferredSize(new Dimension(100, UITheme.BUTTON_HEIGHT));
        btnSave  .setPreferredSize(new Dimension(150, UITheme.BUTTON_HEIGHT));

        btnCancel.addActionListener(e -> dispose());
        btnSave  .addActionListener(e -> save());

        panel.add(btnCancel);
        panel.add(btnSave);

        // Nhấn Escape → đóng
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        return panel;
    }

    // ================================================================
    //  Helper UI
    // ================================================================

    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        return lbl;
    }

    private JPanel createGroup(String title) {
        JPanel group = new JPanel();
        group.setBackground(UITheme.BG_WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "  " + title + "  "
        );
        border.setTitleFont(UITheme.FONT_BOLD);
        border.setTitleColor(UITheme.TEXT_SECONDARY);
        group.setBorder(border);
        return group;
    }

    // ================================================================
    //  Populate / Save
    // ================================================================

    private void populateFields(Book book) {
        fIsbn.setText(book.getIsbn());
        fTitle.setText(book.getTitle());
        fAuthor.setText(book.getAuthor());
        fCategory.setText(book.getCategory());
        fPublisher.setText(book.getPublisher());
        fYear.setValue(book.getPublishYear() > 0 ? book.getPublishYear() : Year.now().getValue());
        fTotalCopies.setValue(book.getTotalCopies());
        fAvailCopies.setValue(book.getAvailableCopies());
        fDescription.setText(book.getDescription());
    }

    private void save() {
        try {
            String isbn      = fIsbn.getText().trim();
            String title     = fTitle.getText().trim();
            String author    = fAuthor.getText().trim();
            String category  = fCategory.getText().trim();
            String publisher = fPublisher.getText().trim();
            int    year      = (Integer) fYear.getValue();
            int    total     = (Integer) fTotalCopies.getValue();
            int    avail     = (Integer) fAvailCopies.getValue();
            String desc      = fDescription.getText().trim();

            if (editBook == null) {
                // Thêm mới
                bookService.addBook(isbn, title, author, category, publisher, year, total, desc);
                UITheme.showSuccess(this, "Đã thêm sách \"" + title + "\" thành công!");
            } else {
                // Cập nhật
                editBook.setIsbn(isbn);
                editBook.setTitle(title);
                editBook.setAuthor(author);
                editBook.setCategory(category);
                editBook.setPublisher(publisher);
                editBook.setPublishYear(year);
                editBook.setTotalCopies(total);
                editBook.setAvailableCopies(avail);
                editBook.setDescription(desc);
                bookService.updateBook(editBook);
                UITheme.showSuccess(this, "Đã cập nhật sách \"" + title + "\" thành công!");
            }
            saved = true;
            dispose();

        } catch (IllegalArgumentException | IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi lưu dữ liệu:\n" + ex.getMessage());
        }
    }

    // ================================================================
    //  Getter
    // ================================================================

    public boolean isSaved() { return saved; }
}
