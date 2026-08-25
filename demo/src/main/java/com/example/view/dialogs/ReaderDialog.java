package com.example.view.dialogs;

import com.example.model.Reader;
import com.example.service.ReaderService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Dialog thêm mới hoặc chỉnh sửa thông tin độc giả.
 */
public class ReaderDialog extends JDialog {

    private final ReaderService readerService = new ReaderService();
    private final Reader        editReader;   // null = thêm mới
    private boolean             saved = false;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---- Form fields ----
    private JTextField fFullName, fBirthDate, fPhone, fEmail, fAddress;
    private JComboBox<String> fStatus;
    private JLabel     lblReaderCode, lblJoinDate;

    public ReaderDialog(Frame parent, Reader reader) {
        super(parent, reader == null ? "Đăng Ký Độc Giả Mới" : "Chỉnh Sửa Thông Tin", true);
        this.editReader = reader;
        initUI();
        if (reader != null) populateFields(reader);
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

    /** Thanh tiêu đề màu xanh lá (thêm) / amber (sửa). */
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(editReader == null ? UITheme.COLOR_SUCCESS : UITheme.COLOR_WARNING);
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new EmptyBorder(0, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        JLabel title = new JLabel(editReader == null
            ? "👤  Đăng Ký Độc Giả Mới"
            : "✎  Chỉnh Sửa Thông Tin");
        title.setFont(UITheme.FONT_H3);
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.CENTER);

        JLabel sub = new JLabel(editReader == null
            ? "Mã thẻ sẽ được tự động tạo theo thứ tự"
            : "Cập nhật thông tin cá nhân của độc giả");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xFF, 0xFF, 0xFF, 200));
        bar.add(sub, BorderLayout.SOUTH);
        return bar;
    }

    private JPanel buildForm() {
        JPanel outer = new JPanel(new BorderLayout(0, UITheme.PAD_MD));
        outer.setBackground(UITheme.BG_WHITE);
        outer.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG, 0, UITheme.PAD_LG));

        // --- Nhóm: Thông tin tự động (chỉ hiển thị, không nhập) ---
        if (editReader != null) {
            JPanel autoGroup = createGroup("Thông Tin Thẻ");
            autoGroup.setLayout(new FlowLayout(FlowLayout.LEFT, UITheme.PAD_LG, UITheme.PAD_SM));

            lblReaderCode = new JLabel("Mã thẻ: " + editReader.getReaderCode());
            lblReaderCode.setFont(UITheme.FONT_BOLD);
            lblReaderCode.setForeground(UITheme.ACCENT_PRIMARY);

            lblJoinDate = new JLabel("   |   Ngày đăng ký: " + editReader.getJoinDate());
            lblJoinDate.setFont(UITheme.FONT_BODY);
            lblJoinDate.setForeground(UITheme.TEXT_SECONDARY);

            autoGroup.add(lblReaderCode);
            autoGroup.add(lblJoinDate);
            outer.add(autoGroup, BorderLayout.NORTH);
        }

        // --- Nhóm: Thông tin cá nhân ---
        JPanel infoGroup = createGroup("Thông Tin Cá Nhân");
        infoGroup.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Họ tên
        g.gridy = 0; g.gridx = 0; g.weightx = 0.25;
        infoGroup.add(label("Họ và Tên *"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fFullName = UITheme.createTextField("Nhập họ và tên đầy đủ");
        infoGroup.add(fFullName, g);
        g.gridwidth = 1;

        // Ngày sinh + SĐT
        g.gridy = 1;
        g.gridx = 0; g.weightx = 0.25;
        infoGroup.add(label("Ngày Sinh"), g);
        g.gridx = 1; g.weightx = 0.25;
        fBirthDate = UITheme.createTextField("dd/MM/yyyy");
        infoGroup.add(fBirthDate, g);
        g.gridx = 2; g.weightx = 0.25;
        infoGroup.add(label("Điện Thoại"), g);
        g.gridx = 3; g.weightx = 0.25;
        fPhone = UITheme.createTextField("0xxxxxxxxx");
        infoGroup.add(fPhone, g);

        // Email
        g.gridy = 2;
        g.gridx = 0; g.weightx = 0.25;
        infoGroup.add(label("Email"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fEmail = UITheme.createTextField("example@email.com");
        infoGroup.add(fEmail, g);
        g.gridwidth = 1;

        // Địa chỉ
        g.gridy = 3;
        g.gridx = 0; g.weightx = 0.25;
        infoGroup.add(label("Địa Chỉ"), g);
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.75;
        fAddress = UITheme.createTextField("Số nhà, đường, quận, thành phố");
        infoGroup.add(fAddress, g);
        g.gridwidth = 1;

        // Trạng thái (chỉ hiện khi sửa)
        if (editReader != null) {
            g.gridy = 4;
            g.gridx = 0; g.weightx = 0.25;
            infoGroup.add(label("Trạng Thái"), g);
            g.gridx = 1; g.weightx = 0.25;
            fStatus = new JComboBox<>(new String[]{
                Reader.Status.ACTIVE.getLabel(),
                Reader.Status.LOCKED.getLabel(),
                Reader.Status.EXPIRED.getLabel()
            });
            fStatus.setFont(UITheme.FONT_BODY);
            fStatus.setPreferredSize(new Dimension(160, UITheme.INPUT_HEIGHT));
            infoGroup.add(fStatus, g);
        }

        outer.add(infoGroup, editReader != null ? BorderLayout.CENTER : BorderLayout.NORTH);

        // Gợi ý ngày sinh
        if (editReader == null) {
            JLabel hint = UITheme.createMutedLabel(
                "  * Bắt buộc  •  Ngày sinh định dạng dd/MM/yyyy  •  Mã thẻ sẽ tự sinh");
            JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            hintPanel.setOpaque(false);
            hintPanel.add(hint);
            outer.add(hintPanel, BorderLayout.CENTER);
        }

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
        JButton btnSave   = UITheme.createSuccessButton(
            editReader == null ? "  ＋  Đăng Ký  " : "  ✓  Lưu Thay Đổi  ");

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
        JPanel g = new JPanel();
        g.setBackground(UITheme.BG_WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR), "  " + title + "  ");
        border.setTitleFont(UITheme.FONT_BOLD);
        border.setTitleColor(UITheme.TEXT_SECONDARY);
        g.setBorder(border);
        return g;
    }

    // ================================================================
    //  Populate / Save
    // ================================================================

    private void populateFields(Reader r) {
        fFullName.setText(r.getFullName());
        fBirthDate.setText(r.getBirthDate());
        fPhone.setText(r.getPhone());
        fEmail.setText(r.getEmail());
        fAddress.setText(r.getAddress());
        if (fStatus != null) {
            fStatus.setSelectedItem(r.getStatus().getLabel());
        }
    }

    private void save() {
        try {
            String fullName  = fFullName.getText().trim();
            String birthDate = fBirthDate.getText().trim();
            String phone     = fPhone.getText().trim();
            String email     = fEmail.getText().trim();
            String address   = fAddress.getText().trim();

            if (editReader == null) {
                // Thêm mới
                Reader created = readerService.addReader(fullName, birthDate, phone, email, address);
                UITheme.showSuccess(this,
                    "Đăng ký thành công!\n"
                    + "Mã thẻ: " + created.getReaderCode()
                    + "\nNgày đăng ký: " + created.getJoinDate());
            } else {
                // Cập nhật
                editReader.setFullName(fullName);
                editReader.setBirthDate(birthDate);
                editReader.setPhone(phone);
                editReader.setEmail(email);
                editReader.setAddress(address);
                if (fStatus != null) {
                    String selectedLabel = (String) fStatus.getSelectedItem();
                    editReader.setStatus(Reader.Status.fromString(selectedLabel));
                }
                readerService.updateReader(editReader);
                UITheme.showSuccess(this, "Đã cập nhật thông tin \"" + fullName + "\".");
            }
            saved = true;
            dispose();

        } catch (IllegalArgumentException | IllegalStateException ex) {
            UITheme.showWarning(this, ex.getMessage());
        } catch (Exception ex) {
            UITheme.showError(this, "Lỗi lưu dữ liệu:\n" + ex.getMessage());
        }
    }

    public boolean isSaved() { return saved; }
}
