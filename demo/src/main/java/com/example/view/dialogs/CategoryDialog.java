package com.example.view.dialogs;

import com.example.model.Category;
import com.example.service.CategoryService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Dialog thêm mới hoặc chỉnh sửa thông tin thể loại sách.
 */
public class CategoryDialog extends JDialog {

    private final CategoryService categoryService = new CategoryService();
    private final Category        editCategory; // null = thêm mới, non-null = sửa
    private final String          oldName;
    private boolean               saved = false;
    private Category              createdCategory = null;

    private JTextField fName;
    private JTextArea  fDescription;

    public CategoryDialog(Window parent, Category category) {
        super(parent, category == null ? "Thêm Thể Loại Mới" : "Chỉnh Sửa Thể Loại", ModalityType.APPLICATION_MODAL);
        this.editCategory = category;
        this.oldName = category != null ? category.getName() : null;
        initUI();
        if (category != null) populateFields(category);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildForm(),     BorderLayout.CENTER);
        root.add(buildButtons(),  BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.ACCENT_PRIMARY);
        bar.setPreferredSize(new Dimension(420, 52));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel(editCategory == null ? "📂  Thêm Thể Loại Mới" : "✎  Chỉnh Sửa Thể Loại");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel(editCategory == null
            ? "Tạo danh mục thể loại mới để phân loại sách"
            : "Cập nhật tên hoặc mô tả thể loại");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JPanel mainGroup = new JPanel(new GridBagLayout());
        mainGroup.setBackground(UITheme.BG_WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Tên thể loại
        g.gridy = 0; g.gridx = 0; g.weightx = 0.3;
        JLabel lblName = new JLabel("Tên Thể Loại *");
        lblName.setFont(UITheme.FONT_BOLD);
        lblName.setForeground(UITheme.TEXT_PRIMARY);
        mainGroup.add(lblName, g);

        g.gridx = 1; g.weightx = 0.7;
        fName = UITheme.createTextField("VD: Khoa học viễn tưởng, Kinh tế...");
        mainGroup.add(fName, g);

        // Mô tả
        g.gridy = 1; g.gridx = 0; g.weightx = 0.3;
        g.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblDesc = new JLabel("Mô Tả");
        lblDesc.setFont(UITheme.FONT_BOLD);
        lblDesc.setForeground(UITheme.TEXT_PRIMARY);
        mainGroup.add(lblDesc, g);

        g.gridx = 1; g.weightx = 0.7;
        g.fill = GridBagConstraints.BOTH;
        g.weighty = 1.0;
        fDescription = UITheme.createTextArea(4, 20);
        fDescription.setBackground(UITheme.BG_WHITE);
        JScrollPane descScroll = new JScrollPane(fDescription);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        descScroll.setPreferredSize(new Dimension(0, 90));
        mainGroup.add(descScroll, g);

        form.add(mainGroup, BorderLayout.CENTER);
        return form;
    }

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
        JButton btnSave   = UITheme.createPrimaryButton(editCategory == null ? "  ＋  Thêm Thể Loại  " : "  ✓  Lưu Thay Đổi  ");

        btnCancel.setPreferredSize(new Dimension(100, UITheme.BUTTON_HEIGHT));
        btnSave  .setPreferredSize(new Dimension(160, UITheme.BUTTON_HEIGHT));

        btnCancel.addActionListener(e -> dispose());
        btnSave  .addActionListener(e -> save());

        panel.add(btnCancel);
        panel.add(btnSave);

        // Escape để thoát
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        return panel;
    }

    private void populateFields(Category category) {
        fName.setText(category.getName());
        fDescription.setText(category.getDescription());
    }

    private void save() {
        try {
            String name = fName.getText().trim();
            String desc = fDescription.getText().trim();

            if (name.isEmpty()) {
                UITheme.showWarning(this, "Tên thể loại không được để trống.");
                fName.requestFocus();
                return;
            }

            if (editCategory == null) {
                createdCategory = categoryService.addCategory(name, desc);
                UITheme.showSuccess(this, "Đã thêm thể loại \"" + name + "\" thành công!");
            } else {
                editCategory.setName(name);
                editCategory.setDescription(desc);
                categoryService.updateCategory(editCategory, oldName);
                createdCategory = editCategory;
                UITheme.showSuccess(this, "Đã cập nhật thể loại \"" + name + "\" thành công!");
            }
            saved = true;
            dispose();

        } catch (IllegalArgumentException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi lưu thể loại:\n" + ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Category getCategory() {
        return createdCategory;
    }
}
