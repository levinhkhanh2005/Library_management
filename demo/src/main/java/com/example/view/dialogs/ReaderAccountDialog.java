package com.example.view.dialogs;

import com.example.model.Reader;
import com.example.model.User;
import com.example.service.ReaderService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog quản lý tài khoản đăng nhập (cổng độc giả) cho độc giả:
 * - Xem thông tin tài khoản
 * - Cấp tài khoản mới nếu chưa có
 * - Đổi / đặt lại mật khẩu
 * - Khóa / mở khóa tài khoản đăng nhập
 */
public class ReaderAccountDialog extends JDialog {

    private final ReaderService readerService = new ReaderService();
    private final Reader reader;
    private User user;
    private boolean changed = false;

    // Components
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JLabel lblStatusBadge;
    private JButton btnAction;
    private JButton btnToggleLock;

    public ReaderAccountDialog(Frame parent, Reader reader) {
        super(parent, "Quản Lý Tài Khoản Ứng Dụng Độc Giả", true);
        this.reader = reader;

        try {
            this.user = readerService.getUserForReader(reader.getId());
        } catch (Exception e) {
            this.user = null;
        }

        initUI();
        setSize(480, user == null ? 460 : 490);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    public boolean isChanged() {
        return changed;
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(new Color(0x1E40AF));
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("🔑  Tài Khoản Ứng Dụng Độc Giả");
        title.setFont(UITheme.FONT_H2);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Độc giả: " + reader.getFullName() + " (" + reader.getReaderCode() + ")");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xBFDBFE));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_WHITE);
        body.setBorder(new EmptyBorder(20, 24, 16, 24));

        // Reader summary row
        JPanel summaryBox = new JPanel(new GridLayout(2, 2, 8, 4));
        summaryBox.setBackground(UITheme.BG_PRIMARY);
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(10, 14, 10, 14)
        ));

        summaryBox.add(createLabelPair("Mã thẻ:", reader.getReaderCode()));
        summaryBox.add(createLabelPair("Điện thoại:", reader.getPhone() != null ? reader.getPhone() : "—"));
        summaryBox.add(createLabelPair("Email:", reader.getEmail() != null ? reader.getEmail() : "—"));

        String cardStatus = reader.getStatus().getLabel();
        summaryBox.add(createLabelPair("Thẻ thư viện:", cardStatus));

        body.add(summaryBox);
        body.add(Box.createVerticalStrut(16));

        // Account status section
        if (user != null) {
            // Already has account
            JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            statusRow.setOpaque(false);
            JLabel lblStatTitle = new JLabel("Trạng thái tài khoản:");
            lblStatTitle.setFont(UITheme.FONT_BOLD);
            statusRow.add(lblStatTitle);

            lblStatusBadge = UITheme.createBadge(
                user.isActive() ? "Đang hoạt động" : "Đã bị khóa",
                user.isActive() ? "success" : "danger"
            );
            statusRow.add(lblStatusBadge);
            body.add(statusRow);
            body.add(Box.createVerticalStrut(14));

            // Username (read-only)
            JLabel lblUname = new JLabel("Tên đăng nhập");
            lblUname.setFont(UITheme.FONT_SMALL);
            lblUname.setForeground(UITheme.TEXT_MUTED);
            body.add(lblUname);
            txtUsername = UITheme.createTextField("");
            txtUsername.setText(user.getUsername());
            txtUsername.setEditable(false);
            txtUsername.setBackground(new Color(0xF3F4F6));
            body.add(txtUsername);
            body.add(Box.createVerticalStrut(12));

            // New password
            JLabel lblPass = new JLabel("Mật khẩu mới (để trống nếu không đổi)");
            lblPass.setFont(UITheme.FONT_SMALL);
            lblPass.setForeground(UITheme.TEXT_MUTED);
            body.add(lblPass);
            txtPassword = UITheme.createPasswordField("Tối thiểu 6 ký tự");
            body.add(txtPassword);
            body.add(Box.createVerticalStrut(12));

            JLabel lblConfirm = new JLabel("Xác nhận mật khẩu mới");
            lblConfirm.setFont(UITheme.FONT_SMALL);
            lblConfirm.setForeground(UITheme.TEXT_MUTED);
            body.add(lblConfirm);
            txtConfirmPassword = UITheme.createPasswordField("Nhập lại mật khẩu");
            body.add(txtConfirmPassword);

        } else {
            // No account yet
            JPanel noAccRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            noAccRow.setOpaque(false);
            JLabel noAccLbl = new JLabel("⚠️ Độc giả này chưa có tài khoản cổng ứng dụng.");
            noAccLbl.setFont(UITheme.FONT_BODY);
            noAccLbl.setForeground(UITheme.COLOR_WARNING);
            noAccRow.add(noAccLbl);
            body.add(noAccRow);
            body.add(Box.createVerticalStrut(14));

            // Username input (suggested)
            JLabel lblUname = new JLabel("Tên đăng nhập *");
            lblUname.setFont(UITheme.FONT_SMALL);
            lblUname.setForeground(UITheme.TEXT_MUTED);
            body.add(lblUname);
            txtUsername = UITheme.createTextField("Tối thiểu 3 ký tự");
            String suggested = reader.getReaderCode().toLowerCase().replace("-", "");
            txtUsername.setText(suggested);
            body.add(txtUsername);
            body.add(Box.createVerticalStrut(12));

            // Password input
            JLabel lblPass = new JLabel("Mật khẩu ban đầu * (tối thiểu 6 ký tự)");
            lblPass.setFont(UITheme.FONT_SMALL);
            lblPass.setForeground(UITheme.TEXT_MUTED);
            body.add(lblPass);
            txtPassword = UITheme.createPasswordField("Tối thiểu 6 ký tự");
            body.add(txtPassword);
            body.add(Box.createVerticalStrut(12));

            JLabel lblConfirm = new JLabel("Xác nhận mật khẩu *");
            lblConfirm.setFont(UITheme.FONT_SMALL);
            lblConfirm.setForeground(UITheme.TEXT_MUTED);
            body.add(lblConfirm);
            txtConfirmPassword = UITheme.createPasswordField("Nhập lại mật khẩu");
            body.add(txtConfirmPassword);
        }

        root.add(body, BorderLayout.CENTER);

        // Footer buttons
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_PRIMARY);
        footer.setBorder(new EmptyBorder(12, 24, 12, 24));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBtns.setOpaque(false);
        if (user != null) {
            btnToggleLock = UITheme.createSecondaryButton(
                user.isActive() ? "🔒 Khóa Đăng Nhập" : "🔓 Mở Khóa Đăng Nhập"
            );
            btnToggleLock.addActionListener(e -> doToggleUserLock());
            leftBtns.add(btnToggleLock);
        }
        footer.add(leftBtns, BorderLayout.WEST);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);

        JButton btnCancel = UITheme.createSecondaryButton("Đóng");
        btnCancel.addActionListener(e -> dispose());
        rightBtns.add(btnCancel);

        btnAction = UITheme.createPrimaryButton(
            user == null ? "＋  Cấp Tài Khoản" : "💾  Đổi Mật Khẩu"
        );
        btnAction.addActionListener(e -> doSaveAccount());
        rightBtns.add(btnAction);

        footer.add(rightBtns, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
    }

    private JPanel createLabelPair(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
        v.setForeground(UITheme.TEXT_PRIMARY);
        p.add(l);
        p.add(v);
        return p;
    }

    private void doSaveAccount() {
        String pass = new String(txtPassword.getPassword()).trim();
        String confirm = new String(txtConfirmPassword.getPassword()).trim();

        if (user == null) {
            // Create new
            String uname = txtUsername.getText().trim();
            if (uname.length() < 3) {
                UITheme.showWarning(this, "Tên đăng nhập phải có ít nhất 3 ký tự.");
                return;
            }
            if (pass.length() < 6) {
                UITheme.showWarning(this, "Mật khẩu phải có ít nhất 6 ký tự.");
                return;
            }
            if (!pass.equals(confirm)) {
                UITheme.showWarning(this, "Mật khẩu xác nhận không khớp.");
                return;
            }

            try {
                user = readerService.createOrResetReaderAccount(reader.getId(), uname, pass);
                changed = true;
                UITheme.showSuccess(this, "Đã cấp tài khoản thành công cho độc giả " + reader.getFullName() + "!\n" +
                    "Tên đăng nhập: " + uname);
                dispose();
            } catch (Exception ex) {
                UITheme.showError(this, ex.getMessage());
            }

        } else {
            // Update password
            if (pass.isBlank()) {
                UITheme.showWarning(this, "Vui lòng nhập mật khẩu mới cần đổi.");
                return;
            }
            if (pass.length() < 6) {
                UITheme.showWarning(this, "Mật khẩu mới phải có ít nhất 6 ký tự.");
                return;
            }
            if (!pass.equals(confirm)) {
                UITheme.showWarning(this, "Mật khẩu xác nhận không khớp.");
                return;
            }

            try {
                readerService.createOrResetReaderAccount(reader.getId(), user.getUsername(), pass);
                changed = true;
                UITheme.showSuccess(this, "Đã cập nhật mật khẩu mới cho tài khoản " + user.getUsername() + " thành công!");
                dispose();
            } catch (Exception ex) {
                UITheme.showError(this, ex.getMessage());
            }
        }
    }

    private void doToggleUserLock() {
        if (user == null) return;
        boolean nowActive = !user.isActive();
        String action = nowActive ? "mở khóa" : "khóa";
        boolean confirm = UITheme.showConfirm(this,
            "Bạn có chắc muốn " + action + " tài khoản \"" + user.getUsername() + "\" không?",
            "Xác nhận " + action);
        if (!confirm) return;

        try {
            new com.example.dao.UserDAO().setActive(user.getId(), nowActive);
            user.setActive(nowActive);
            changed = true;

            lblStatusBadge.setText(nowActive ? "Đang hoạt động" : "Đã bị khóa");
            lblStatusBadge.setForeground(nowActive ? UITheme.COLOR_SUCCESS : UITheme.COLOR_DANGER);
            btnToggleLock.setText(nowActive ? "🔒 Khóa Đăng Nhập" : "🔓 Mở Khóa Đăng Nhập");

            UITheme.showSuccess(this, "Đã " + action + " tài khoản thành công!");
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }
}
