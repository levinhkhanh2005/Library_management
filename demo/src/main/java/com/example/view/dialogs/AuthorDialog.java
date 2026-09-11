package com.example.view.dialogs;

import com.example.model.Author;
import com.example.service.AuthorService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.Year;

/**
 * Dialog thêm mới hoặc chỉnh sửa thông tin tác giả.
 */
public class AuthorDialog extends JDialog {

    private final AuthorService authorService = new AuthorService();
    private final Author        editAuthor;   // null = thêm mới, non-null = sửa
    private boolean             saved = false;
    private Author              resultAuthor;

    // ---- Form fields ----
    private JTextField fName, fNationality, fBirthYear, fDeathYear;
    private JTextArea  fBiography;

    public AuthorDialog(Dialog parent, Author author) {
        super(parent, author == null ? "Thêm Tác Giả Mới" : "Chỉnh Sửa Tác Giả", true);
        this.editAuthor = author;
        initUI();
        if (author != null) populateFields(author);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    public AuthorDialog(Frame parent, Author author) {
        super(parent, author == null ? "Thêm Tác Giả Mới" : "Chỉnh Sửa Tác Giả", true);
        this.editAuthor = author;
        initUI();
        if (author != null) populateFields(author);
        pack();
        setResizable(false);
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
        root.add(buildForm(),     BorderLayout.CENTER);
        root.add(buildButtons(),  BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.ACCENT_PRIMARY);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel(editAuthor == null ? "✍️  Thêm Tác Giả Mới" : "✎  Chỉnh Sửa Tác Giả");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel(editAuthor == null
            ? "Tạo mới tác giả để quản lý đầu sách chuẩn hóa"
            : "Cập nhật hồ sơ và tiểu sử tác giả");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildForm() {
        JPanel outer = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        outer.setBackground(UITheme.BG_WHITE);
        outer.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JPanel formGroup = createGroup("Thông Tin Tác Giả");
        formGroup.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Hàng 1: Tên tác giả
        g.gridy = 0; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Tên Tác Giả *"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fName = UITheme.createTextField("Nhập tên tác giả (VD: Tô Hoài, Nam Cao...)");
        fName.setPreferredSize(new Dimension(340, UITheme.INPUT_HEIGHT));
        formGroup.add(fName, g);
        g.gridwidth = 1;

        // Hàng 2: Quốc tịch
        g.gridy = 1; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Quốc Tịch"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fNationality = UITheme.createTextField("VD: Việt Nam, Anh, Mỹ, Nhật Bản...");
        formGroup.add(fNationality, g);
        g.gridwidth = 1;

        // Hàng 3: Năm sinh + Năm mất
        g.gridy = 2; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Năm Sinh"), g);
        g.gridx = 1; g.weightx = 0.25;
        fBirthYear = UITheme.createTextField("VD: 1920");
        formGroup.add(fBirthYear, g);

        g.gridx = 2; g.weightx = 0.25;
        formGroup.add(label("Năm Mất"), g);
        g.gridx = 3; g.weightx = 0.25;
        fDeathYear = UITheme.createTextField("Để trống nếu còn sống");
        formGroup.add(fDeathYear, g);

        // Hàng 4: Tiểu sử
        g.gridy = 3; g.gridx = 0; g.weightx = 0.25;
        g.anchor = GridBagConstraints.NORTHWEST;
        formGroup.add(label("Tiểu Sử / Giới Thiệu"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        g.fill = GridBagConstraints.BOTH;
        fBiography = UITheme.createTextArea(4, 25);
        fBiography.setBackground(UITheme.BG_WHITE);
        JScrollPane bioScroll = new JScrollPane(fBiography);
        bioScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        bioScroll.setPreferredSize(new Dimension(340, 90));
        formGroup.add(bioScroll, g);

        outer.add(formGroup, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.PAD_SM, UITheme.PAD_MD));
        panel.setBackground(UITheme.BG_WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG)
        ));

        JButton btnCancel = UITheme.createSecondaryButton("Hủy");
        JButton btnSave   = UITheme.createPrimaryButton(
            editAuthor == null ? "  ＋  Thêm Tác Giả  " : "  ✓  Lưu Thay Đổi  ");

        btnCancel.setPreferredSize(new Dimension(100, UITheme.BUTTON_HEIGHT));
        btnSave  .setPreferredSize(new Dimension(150, UITheme.BUTTON_HEIGHT));

        btnCancel.addActionListener(e -> dispose());
        btnSave  .addActionListener(e -> save());

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        panel.add(btnCancel);
        panel.add(btnSave);
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
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR), "  " + title + "  ");
        border.setTitleFont(UITheme.FONT_BOLD);
        border.setTitleColor(UITheme.TEXT_SECONDARY);
        group.setBorder(border);
        return group;
    }

    private void populateFields(Author a) {
        fName.setText(a.getName());
        fNationality.setText(a.getNationality() != null ? a.getNationality() : "");
        fBirthYear.setText(a.getBirthYear() != null ? String.valueOf(a.getBirthYear()) : "");
        fDeathYear.setText(a.getDeathYear() != null ? String.valueOf(a.getDeathYear()) : "");
        fBiography.setText(a.getBiography() != null ? a.getBiography() : "");
    }

    private void save() {
        try {
            String name = fName.getText().trim();
            String nationality = fNationality.getText().trim();
            String bio = fBiography.getText().trim();

            Integer birth = null;
            if (!fBirthYear.getText().trim().isBlank()) {
                try {
                    birth = Integer.parseInt(fBirthYear.getText().trim());
                } catch (NumberFormatException e) {
                    UITheme.showWarning(this, "Năm sinh phải là số nguyên.");
                    return;
                }
            }

            Integer death = null;
            if (!fDeathYear.getText().trim().isBlank()) {
                try {
                    death = Integer.parseInt(fDeathYear.getText().trim());
                } catch (NumberFormatException e) {
                    UITheme.showWarning(this, "Năm mất phải là số nguyên.");
                    return;
                }
            }

            if (editAuthor == null) {
                // Thêm mới
                resultAuthor = authorService.addAuthor(name, birth, death, nationality, bio);
                UITheme.showSuccess(this, "Đã thêm tác giả \"" + name + "\" thành công!");
            } else {
                // Sửa
                String oldName = editAuthor.getName();
                editAuthor.setName(name);
                editAuthor.setNationality(nationality);
                editAuthor.setBirthYear(birth);
                editAuthor.setDeathYear(death);
                editAuthor.setBiography(bio);
                authorService.updateAuthor(editAuthor, oldName);
                resultAuthor = editAuthor;
                UITheme.showSuccess(this, "Đã cập nhật tác giả \"" + name + "\" thành công!");
            }

            saved = true;
            dispose();

        } catch (IllegalArgumentException | IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi lưu dữ liệu:\n" + ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Author getAuthor() {
        return resultAuthor;
    }
}
