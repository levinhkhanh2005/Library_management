package com.example.view.dialogs;

import com.example.model.Publisher;
import com.example.service.PublisherService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Dialog thêm mới hoặc chỉnh sửa thông tin Nhà Xuất Bản (Publisher).
 */
public class PublisherDialog extends JDialog {

    private final PublisherService publisherService = new PublisherService();
    private final Publisher        editPublisher;   // null = thêm mới, non-null = sửa
    private boolean                saved = false;
    private Publisher              resultPublisher;

    // ---- Form fields ----
    private JTextField fName, fRepresentative, fPhone, fEmail, fWebsite, fAddress;
    private JTextArea  fDescription;

    public PublisherDialog(Dialog parent, Publisher publisher) {
        super(parent, publisher == null ? "Thêm Nhà Xuất Bản Mới" : "Chỉnh Sửa Nhà Xuất Bản", true);
        this.editPublisher = publisher;
        initUI();
        if (publisher != null) populateFields(publisher);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    public PublisherDialog(Frame parent, Publisher publisher) {
        super(parent, publisher == null ? "Thêm Nhà Xuất Bản Mới" : "Chỉnh Sửa Nhà Xuất Bản", true);
        this.editPublisher = publisher;
        initUI();
        if (publisher != null) populateFields(publisher);
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

        JLabel title = new JLabel(editPublisher == null ? "🏢  Thêm Nhà Xuất Bản Mới" : "✎  Chỉnh Sửa Nhà Xuất Bản");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel(editPublisher == null
            ? "Tạo mới nhà xuất bản để quản lý đầu sách chuẩn hóa"
            : "Cập nhật hồ sơ pháp lý và thông tin liên hệ nhà xuất bản");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xC7D2FE));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildForm() {
        JPanel outer = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        outer.setBackground(UITheme.BG_WHITE);
        outer.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JPanel formGroup = createGroup("Thông Tin Nhà Xuất Bản");
        formGroup.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Hàng 0: Tên NXB
        g.gridy = 0; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Tên NXB *"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fName = UITheme.createTextField("Nhập tên nhà xuất bản (VD: NXB Kim Đồng, NXB Trẻ...)");
        fName.setPreferredSize(new Dimension(360, UITheme.INPUT_HEIGHT));
        formGroup.add(fName, g);
        g.gridwidth = 1;

        // Hàng 1: Người đại diện + Số điện thoại
        g.gridy = 1; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Người Đại Diện"), g);
        g.gridx = 1; g.weightx = 0.35;
        fRepresentative = UITheme.createTextField("Họ tên người đại diện");
        formGroup.add(fRepresentative, g);

        g.gridx = 2; g.weightx = 0.15;
        formGroup.add(label("Số Điện Thoại"), g);
        g.gridx = 3; g.weightx = 0.25;
        fPhone = UITheme.createTextField("VD: 0243.8222135");
        formGroup.add(fPhone, g);

        // Hàng 2: Email + Website
        g.gridy = 2; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Email"), g);
        g.gridx = 1; g.weightx = 0.35;
        fEmail = UITheme.createTextField("VD: contact@nxb.vn");
        formGroup.add(fEmail, g);

        g.gridx = 2; g.weightx = 0.15;
        formGroup.add(label("Website"), g);
        g.gridx = 3; g.weightx = 0.25;
        fWebsite = UITheme.createTextField("VD: https://nxb.vn");
        formGroup.add(fWebsite, g);

        // Hàng 3: Địa chỉ trụ sở
        g.gridy = 3; g.gridx = 0; g.weightx = 0.25;
        formGroup.add(label("Địa Chỉ Trụ Sở"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fAddress = UITheme.createTextField("Địa chỉ văn phòng / trụ sở NXB");
        formGroup.add(fAddress, g);
        g.gridwidth = 1;

        // Hàng 4: Mô tả / Giới thiệu
        g.gridy = 4; g.gridx = 0; g.weightx = 0.25;
        g.anchor = GridBagConstraints.NORTHWEST;
        formGroup.add(label("Mô Tả / Giới Thiệu"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        g.fill = GridBagConstraints.BOTH;
        fDescription = UITheme.createTextArea(3, 25);
        fDescription.setBackground(UITheme.BG_WHITE);
        JScrollPane descScroll = new JScrollPane(fDescription);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR));
        descScroll.setPreferredSize(new Dimension(360, 80));
        formGroup.add(descScroll, g);

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
            editPublisher == null ? "  ＋  Thêm NXB  " : "  ✓  Lưu Thay Đổi  ");

        btnCancel.setPreferredSize(new Dimension(100, UITheme.BUTTON_HEIGHT));
        btnSave  .setPreferredSize(new Dimension(160, UITheme.BUTTON_HEIGHT));

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

    private void populateFields(Publisher p) {
        fName.setText(p.getName());
        fRepresentative.setText(p.getRepresentative() != null ? p.getRepresentative() : "");
        fPhone.setText(p.getPhone() != null ? p.getPhone() : "");
        fEmail.setText(p.getEmail() != null ? p.getEmail() : "");
        fWebsite.setText(p.getWebsite() != null ? p.getWebsite() : "");
        fAddress.setText(p.getAddress() != null ? p.getAddress() : "");
        fDescription.setText(p.getDescription() != null ? p.getDescription() : "");
    }

    private void save() {
        try {
            String name = fName.getText().trim();
            String representative = fRepresentative.getText().trim();
            String phone = fPhone.getText().trim();
            String email = fEmail.getText().trim();
            String website = fWebsite.getText().trim();
            String address = fAddress.getText().trim();
            String desc = fDescription.getText().trim();

            if (editPublisher == null) {
                // Thêm mới
                resultPublisher = publisherService.addPublisher(name, address, phone, email, website, representative, desc);
                UITheme.showSuccess(this, "Đã thêm nhà xuất bản \"" + name + "\" thành công!");
            } else {
                // Sửa
                String oldName = editPublisher.getName();
                editPublisher.setName(name);
                editPublisher.setRepresentative(representative);
                editPublisher.setPhone(phone);
                editPublisher.setEmail(email);
                editPublisher.setWebsite(website);
                editPublisher.setAddress(address);
                editPublisher.setDescription(desc);
                publisherService.updatePublisher(editPublisher, oldName);
                resultPublisher = editPublisher;
                UITheme.showSuccess(this, "Đã cập nhật nhà xuất bản \"" + name + "\" thành công!");
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

    public Publisher getPublisher() {
        return resultPublisher;
    }
}
