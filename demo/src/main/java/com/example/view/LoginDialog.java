package com.example.view;

import com.example.model.User;
import com.example.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Hộp thoại đăng nhập — hiển thị trước MainFrame.
 */
public class LoginDialog extends JDialog {

    private boolean loginSuccess = false;
    private final AuthService authService = new AuthService();

    // Components
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        loginButton;
    private JLabel         errorLabel;

    public LoginDialog(Frame parent) {
        super(parent, "Đăng Nhập — Thư Viện Nguyễn Huệ", true);
        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // ================================================================
    //  Xây dựng UI
    // ================================================================

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_PRIMARY);
        setContentPane(root);

        // --- Phần trái: Banner ---
        root.add(buildBanner(), BorderLayout.WEST);

        // --- Phần phải: Form đăng nhập ---
        root.add(buildForm(), BorderLayout.CENTER);
    }

    /** Banner bên trái với gradient + tên thư viện. */
    private JPanel buildBanner() {
        JPanel banner = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient từ trên xuống
                GradientPaint gp = new GradientPaint(
                    0, 0, UITheme.BG_HEADER,
                    0, getHeight(), new Color(0x312E81)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        banner.setPreferredSize(new Dimension(260, 420));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 24, 8, 24);

        // Icon sách
        JLabel iconLabel = new JLabel("📚");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        banner.add(iconLabel, gbc);

        // Tên thư viện
        JLabel titleLabel = new JLabel("<html><center>THƯ VIỆN<br>NGUYỄN HUỆ</center></html>");
        titleLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(titleLabel, gbc);

        // Đường kẻ
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xFF, 0xFF, 0xFF, 60));
        sep.setPreferredSize(new Dimension(160, 1));
        banner.add(sep, gbc);

        // Tagline
        JLabel tagLabel = new JLabel("<html><center>Hệ thống quản lý<br>thư viện thông minh</center></html>");
        tagLabel.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        tagLabel.setForeground(new Color(0xA5B4FC));
        tagLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(tagLabel, gbc);

        return banner;
    }

    /** Form đăng nhập bên phải. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(new EmptyBorder(40, 40, 40, 40));
        form.setPreferredSize(new Dimension(340, 420));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 6, 0);

        // Tiêu đề
        JLabel heading = new JLabel("Chào mừng trở lại!");
        heading.setFont(UITheme.FONT_H2);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        form.add(heading, gbc);

        gbc.gridy++; gbc.insets = new Insets(0, 0, 28, 0);
        JLabel sub = new JLabel("Vui lòng đăng nhập để tiếp tục");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_MUTED);
        form.add(sub, gbc);

        // Label username
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel userLbl = new JLabel("Tên đăng nhập");
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(userLbl, gbc);

        // Input username
        gbc.gridy++; gbc.insets = new Insets(0, 0, 16, 0);
        usernameField = UITheme.createTextField("Nhập tên đăng nhập");
        usernameField.setPreferredSize(new Dimension(260, UITheme.INPUT_HEIGHT));
        form.add(usernameField, gbc);

        // Label password
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel passLbl = new JLabel("Mật khẩu");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(passLbl, gbc);

        // Input password
        gbc.gridy++; gbc.insets = new Insets(0, 0, 8, 0);
        passwordField = UITheme.createPasswordField("Nhập mật khẩu");
        passwordField.setPreferredSize(new Dimension(260, UITheme.INPUT_HEIGHT));
        form.add(passwordField, gbc);

        // Error label (ẩn ban đầu)
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.COLOR_DANGER);
        form.add(errorLabel, gbc);

        // Nút đăng nhập
        gbc.gridy++; gbc.insets = new Insets(0, 0, 0, 0);
        loginButton = UITheme.createPrimaryButton("  Đăng Nhập  ");
        loginButton.setPreferredSize(new Dimension(260, UITheme.BUTTON_HEIGHT + 4));
        loginButton.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        form.add(loginButton, gbc);

        // Gợi ý tài khoản mặc định
        gbc.gridy++; gbc.insets = new Insets(20, 0, 0, 0);
        JLabel hint = new JLabel("<html><center><font color='#94A3B8'>Mặc định: admin / admin123</font></center></html>");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(hint, gbc);

        // --- Sự kiện ---
        loginButton.addActionListener(e -> doLogin());
        // Enter để đăng nhập
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);

        return form;
    }

    // ================================================================
    //  Logic đăng nhập
    // ================================================================

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        loginButton.setEnabled(false);
        loginButton.setText("Đang đăng nhập...");
        errorLabel.setText(" ");

        // Chạy trên background thread để không block UI
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override protected User doInBackground() throws Exception {
                return authService.login(username, password);
            }

            @Override protected void done() {
                try {
                    get(); // throw nếu có exception
                    loginSuccess = true;
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    errorLabel.setText("<html>" + msg.replace("\n", "<br>") + "</html>");
                    passwordField.setText("");
                    passwordField.requestFocus();
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("  Đăng Nhập  ");
                }
            }
        };
        worker.execute();
    }

    // ================================================================
    //  Getter
    // ================================================================

    /** Trả về true nếu đăng nhập thành công. */
    public boolean isLoginSuccess() {
        return loginSuccess;
    }
}
