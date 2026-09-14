package com.example.view.dialogs;

import com.example.model.OtpVerification;
import com.example.service.AuthService;
import com.example.service.OtpService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog đăng ký tài khoản độc giả.
 * Quy trình: Nhập thông tin → Gửi OTP → Xác thực OTP → Tạo tài khoản.
 */
public class RegisterDialog extends JDialog {

    private final OtpService  otpService  = new OtpService();
    private final AuthService authService = new AuthService();

    private boolean registered = false;

    // === Form fields ===
    private JTextField     fFullName;
    private JTextField     fEmail;
    private JTextField     fPhone;
    private JTextField     fBirthDate;
    private JTextField     fAddress;
    private JTextField     fUsername;
    private JPasswordField fPassword;
    private JPasswordField fConfirmPassword;
    private JLabel         lblError;
    private JButton        btnRegister;

    /**
     * @param parent Frame cha (LoginDialog)
     */
    public RegisterDialog(Dialog parent) {
        super(parent, "Đăng Ký Tài Khoản Độc Giả", true);
        initUI();
        setSize(500, 680);
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // ================================================================
    //  UI
    // ================================================================

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        setContentPane(root);

        // === HEADER ===
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UITheme.ACCENT_PRIMARY, getWidth(), 0, new Color(0x7C3AED)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 70));
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel titleLabel = new JLabel("📚  Đăng Ký Tài Khoản Độc Giả");
        titleLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel, BorderLayout.WEST);

        JLabel subtitleLabel = new JLabel("Thư Viện Nguyễn Huệ");
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(new Color(255, 255, 255, 180));
        subtitleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(subtitleLabel, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // === FORM PANEL (scrollable) ===
        JPanel formWrapper = new JPanel();
        formWrapper.setLayout(new BoxLayout(formWrapper, BoxLayout.Y_AXIS));
        formWrapper.setBackground(UITheme.BG_WHITE);
        formWrapper.setBorder(new EmptyBorder(24, 32, 24, 32));

        // Thông tin cá nhân
        addSectionTitle(formWrapper, "Thông tin cá nhân");

        fFullName  = addField(formWrapper, "Họ và tên *", "Nguyễn Văn A");
        fEmail     = addField(formWrapper, "Email *", "example@email.com");
        fPhone     = addField(formWrapper, "Số điện thoại", "0901234567");
        fBirthDate = addField(formWrapper, "Ngày sinh", "dd/MM/yyyy");
        fAddress   = addField(formWrapper, "Địa chỉ", "Số nhà, đường, quận, thành phố");

        formWrapper.add(Box.createVerticalStrut(16));

        // Thông tin tài khoản
        addSectionTitle(formWrapper, "Thông tin tài khoản");

        fUsername = addField(formWrapper, "Tên đăng nhập *", "Tối thiểu 4 ký tự");

        // Password fields
        JPanel passRow = createFieldRow("Mật khẩu *");
        fPassword = UITheme.createPasswordField("Tối thiểu 6 ký tự");
        fPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.INPUT_HEIGHT));
        passRow.add(fPassword);
        formWrapper.add(passRow);

        JPanel confirmRow = createFieldRow("Xác nhận mật khẩu *");
        fConfirmPassword = UITheme.createPasswordField("Nhập lại mật khẩu");
        fConfirmPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.INPUT_HEIGHT));
        confirmRow.add(fConfirmPassword);
        formWrapper.add(confirmRow);

        formWrapper.add(Box.createVerticalStrut(8));

        // Error label
        lblError = new JLabel(" ");
        lblError.setFont(UITheme.FONT_SMALL);
        lblError.setForeground(UITheme.COLOR_DANGER);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrapper.add(lblError);
        formWrapper.add(Box.createVerticalStrut(12));

        // Nút Đăng ký
        btnRegister = new JButton("Đăng Ký") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (isEnabled()) {
                    g2.setPaint(new GradientPaint(0, 0, UITheme.ACCENT_PRIMARY, w, 0, new Color(0x7C3AED)));
                } else {
                    g2.setColor(UITheme.TEXT_MUTED);
                }
                g2.fillRoundRect(0, 0, w, h, UITheme.BORDER_RADIUS * 2, UITheme.BORDER_RADIUS * 2);
                if (getModel().isRollover() && isEnabled()) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, UITheme.BORDER_RADIUS * 2, UITheme.BORDER_RADIUS * 2);
                }
                g2.dispose();
                // Text
                g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btnRegister.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        btnRegister.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.BUTTON_HEIGHT + 8));
        btnRegister.setPreferredSize(new Dimension(0, UITheme.BUTTON_HEIGHT + 8));
        btnRegister.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRegister.setFocusPainted(false);
        btnRegister.setContentAreaFilled(false);
        btnRegister.setBorderPainted(false);
        btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegister.addActionListener(e -> doRegister());

        // Enter key to submit
        fConfirmPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doRegister();
            }
        });

        formWrapper.add(btnRegister);
        formWrapper.add(Box.createVerticalStrut(16));

        // Link quay lại đăng nhập
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkPanel.setOpaque(false);
        linkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel linkLabel = new JLabel("Đã có tài khoản?");
        linkLabel.setFont(UITheme.FONT_SMALL);
        linkLabel.setForeground(UITheme.TEXT_MUTED);
        linkPanel.add(linkLabel);

        JButton btnBack = new JButton("Đăng nhập");
        btnBack.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        btnBack.setForeground(UITheme.ACCENT_PRIMARY);
        btnBack.setBorderPainted(false);
        btnBack.setContentAreaFilled(false);
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> dispose());
        linkPanel.add(btnBack);
        formWrapper.add(linkPanel);

        JScrollPane scroll = new JScrollPane(formWrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        root.add(scroll, BorderLayout.CENTER);
    }

    // ================================================================
    //  Form Helpers
    // ================================================================

    private void addSectionTitle(JPanel parent, String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        lbl.setForeground(UITheme.ACCENT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        parent.add(lbl);

        // Separator line
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(UITheme.BORDER_COLOR);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(sep);
        parent.add(Box.createVerticalStrut(12));
    }

    private JPanel createFieldRow(String labelText) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        row.add(Box.createVerticalStrut(4));
        return row;
    }

    private JTextField addField(JPanel parent, String label, String placeholder) {
        JPanel row = createFieldRow(label);
        JTextField tf = UITheme.createTextField(placeholder);
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, UITheme.INPUT_HEIGHT));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(tf);
        parent.add(row);
        return tf;
    }

    // ================================================================
    //  Register Logic
    // ================================================================

    private void doRegister() {
        lblError.setText(" ");

        // 1. Validate cơ bản
        String fullName   = fFullName.getText().trim();
        String email      = fEmail.getText().trim();
        String phone      = fPhone.getText().trim();
        String birthDate  = fBirthDate.getText().trim();
        String address    = fAddress.getText().trim();
        String username   = fUsername.getText().trim();
        String password   = new String(fPassword.getPassword());
        String confirmPwd = new String(fConfirmPassword.getPassword());

        if (fullName.isEmpty()) { showError("Vui lòng nhập họ tên."); fFullName.requestFocus(); return; }
        if (email.isEmpty())    { showError("Vui lòng nhập email."); fEmail.requestFocus(); return; }
        if (!email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Email không hợp lệ."); fEmail.requestFocus(); return;
        }
        if (username.isEmpty()) { showError("Vui lòng nhập tên đăng nhập."); fUsername.requestFocus(); return; }
        if (username.length() < 4) { showError("Tên đăng nhập phải có ít nhất 4 ký tự."); fUsername.requestFocus(); return; }
        if (password.isEmpty()) { showError("Vui lòng nhập mật khẩu."); fPassword.requestFocus(); return; }
        if (password.length() < 6) { showError("Mật khẩu phải có ít nhất 6 ký tự."); fPassword.requestFocus(); return; }
        if (!password.equals(confirmPwd)) { showError("Mật khẩu xác nhận không khớp."); fConfirmPassword.requestFocus(); return; }

        // 2. Gửi OTP (background)
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang gửi mã OTP...");

        // Build payload JSON đơn giản (lưu tạm thông tin)
        String payload = buildPayload(fullName, email, phone, birthDate, address, username, password);

        SwingWorker<OtpVerification, Void> otpWorker = new SwingWorker<>() {
            @Override
            protected OtpVerification doInBackground() throws Exception {
                return otpService.sendOtp(email, OtpVerification.OtpType.REGISTER, payload);
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                btnRegister.setText("Đăng Ký");
                try {
                    get(); // Check for errors

                    // 3. Mở OTP Dialog
                    OtpDialog otpDialog = new OtpDialog(
                        RegisterDialog.this, email,
                        OtpVerification.OtpType.REGISTER, payload
                    );
                    otpDialog.setVisible(true);

                    if (otpDialog.isVerified()) {
                        // 4. OTP xác thực thành công → Tạo tài khoản
                        doCreateAccount(username, password, fullName, email, phone, birthDate, address);
                    }

                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    showError(msg);
                }
            }
        };
        otpWorker.execute();
    }

    private void doCreateAccount(String username, String password, String fullName,
                                 String email, String phone, String birthDate, String address) {
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang tạo tài khoản...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                authService.registerReader(username, password, fullName, email, phone, birthDate, address);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    registered = true;
                    JOptionPane.showMessageDialog(
                        RegisterDialog.this,
                        "<html><div style='text-align:center;'>"
                            + "<span style='font-size:24pt;'>🎉</span><br><br>"
                            + "<b style='font-size:12pt;'>Đăng ký thành công!</b><br><br>"
                            + "Tài khoản <b>" + username + "</b> đã được tạo.<br>"
                            + "Bạn có thể đăng nhập ngay bây giờ.</div></html>",
                        "Thành Công",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    showError(msg);
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Đăng Ký");
                }
            }
        };
        worker.execute();
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private void showError(String msg) {
        lblError.setText("<html><span style='color:#EF4444'>" + msg + "</span></html>");
    }

    /**
     * Build JSON payload đơn giản để lưu thông tin đăng ký tạm thời.
     * Dùng String.format thay vì thư viện JSON.
     */
    private String buildPayload(String fullName, String email, String phone,
                                String birthDate, String address,
                                String username, String password) {
        return String.format(
            "{\"fullName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\","
            + "\"birthDate\":\"%s\",\"address\":\"%s\",\"username\":\"%s\"}",
            escapeJson(fullName), escapeJson(email), escapeJson(phone),
            escapeJson(birthDate), escapeJson(address), escapeJson(username)
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ================================================================
    //  Getters
    // ================================================================

    public boolean isRegistered() { return registered; }
}
