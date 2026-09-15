package com.example.view;

import com.example.dao.ReaderDAO;
import com.example.model.Reader;
import com.example.model.User;
import com.example.service.AuthService;
import com.example.view.dialogs.RegisterDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hộp thoại đăng nhập dành riêng cho ứng dụng Độc Giả (ReaderApp).
 * Giao diện hiện đại với banner gradient xanh đại dương, bokeh particles,
 * và liên kết đăng ký thẻ độc giả trực tuyến.
 */
public class ReaderLoginDialog extends JDialog {

    private boolean loginSuccess = false;
    private User loggedInUser = null;
    private Reader loggedInReader = null;

    private final AuthService authService = new AuthService();
    private final ReaderDAO readerDAO = new ReaderDAO();

    // Components
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        loginButton;
    private JLabel         errorLabel;

    public ReaderLoginDialog(Frame parent) {
        super(parent, "Đăng Nhập — Cổng Độc Giả Thư Viện Nguyễn Huệ", true);
        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_PRIMARY);
        setContentPane(root);

        // Phần trái: Banner
        root.add(buildBanner(), BorderLayout.WEST);

        // Phần phải: Form đăng nhập
        root.add(buildForm(), BorderLayout.CENTER);
    }

    /** Banner bên trái với tone xanh đại dương / xanh ngọc mát mắt cho độc giả. */
    private JPanel buildBanner() {
        Random rng = new Random(77);
        List<float[]> particles = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            particles.add(new float[]{
                rng.nextFloat(), // x
                rng.nextFloat(), // y
                8 + rng.nextFloat() * 28, // radius
                0.05f + rng.nextFloat() * 0.14f, // alpha
                rng.nextFloat() * 0.35f - 0.17f // vy
            });
        }

        JPanel banner = new JPanel(new GridBagLayout()) {
            private float gradientPhase = 0f;
            private Timer animTimer;
            {
                animTimer = new Timer(50, e -> {
                    gradientPhase += 0.007f;
                    if (gradientPhase > 1f) gradientPhase = 0f;
                    for (float[] p : particles) {
                        p[1] += p[4] * 0.003f;
                        if (p[1] < -0.1f) p[1] = 1.1f;
                        if (p[1] > 1.1f) p[1] = -0.1f;
                    }
                    repaint();
                });
                animTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Gradient xanh đại dương thanh lịch
                float phase = gradientPhase;
                Color c1 = blendColors(new Color(0x0C4A6E), new Color(0x065F46), phase);
                Color c2 = blendColors(new Color(0x0284C7), new Color(0x0D9488), phase);
                Color c3 = blendColors(new Color(0x0369A1), new Color(0x047857), phase);

                GradientPaint gp = new GradientPaint(0, 0, c1, w * 0.6f, h, c2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Lớp overlay thứ 2
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                GradientPaint gp2 = new GradientPaint(w, 0, c3, 0, h, new Color(0x0891B2));
                g2.setPaint(gp2);
                g2.fillRect(0, 0, w, h);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                // Bokeh particles
                for (float[] p : particles) {
                    float px = p[0] * w;
                    float py = p[1] * h;
                    float pr = p[2];
                    float pa = p[3];
                    g2.setColor(new Color(1f, 1f, 1f, pa));
                    g2.fill(new Ellipse2D.Float(px - pr / 2, py - pr / 2, pr, pr));
                }
            }

            private Color blendColors(Color a, Color b, float ratio) {
                float r = a.getRed()   + (b.getRed()   - a.getRed())   * ratio;
                float g = a.getGreen() + (b.getGreen() - a.getGreen()) * ratio;
                float bl = a.getBlue()  + (b.getBlue()  - a.getBlue())  * ratio;
                return new Color(Math.round(r), Math.round(g), Math.round(bl));
            }
        };

        banner.setPreferredSize(new Dimension(280, 480));
        banner.setBorder(new EmptyBorder(30, 24, 30, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 20, 8, 20);

        // Icon sách
        JLabel iconLabel = new JLabel("📖");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        banner.add(iconLabel, gbc);

        // Tên thư viện & cổng độc giả
        JLabel titleLabel = new JLabel("<html><center>CỔNG ĐỘC GIẢ<br><font size='4'>THƯ VIỆN NGUYỄN HUỆ</font></center></html>");
        titleLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(titleLabel, gbc);

        // Đường kẻ gradient
        JPanel sepLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0xFF, 0xFF, 0xFF, 0),
                    getWidth() / 2, 0, new Color(0xFF, 0xFF, 0xFF, 140),
                    true));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sepLine.setPreferredSize(new Dimension(170, 1));
        sepLine.setOpaque(false);
        banner.add(sepLine, gbc);

        // Tagline
        JLabel tagLabel = new JLabel("<html><center>Tra cứu tài liệu<br>Theo dõi sách mượn<br>Gia hạn trực tuyến</center></html>");
        tagLabel.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        tagLabel.setForeground(new Color(0xBAE6FD));
        tagLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(tagLabel, gbc);

        // Badge
        gbc.insets = new Insets(16, 20, 6, 20);
        JLabel versionLbl = new JLabel("Reader Edition v2.0");
        versionLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
        versionLbl.setForeground(new Color(0x7DD3FC));
        banner.add(versionLbl, gbc);

        return banner;
    }

    /** Form đăng nhập bên phải. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(new EmptyBorder(36, 40, 36, 40));
        form.setPreferredSize(new Dimension(370, 480));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);

        // Icon chào mừng
        JLabel waveIcon = new JLabel("👋");
        waveIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        form.add(waveIcon, gbc);

        // Tiêu đề
        gbc.gridy++; gbc.insets = new Insets(4, 0, 4, 0);
        JLabel heading = new JLabel("Chào mừng bạn đọc!");
        heading.setFont(UITheme.FONT_H1);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        form.add(heading, gbc);

        gbc.gridy++; gbc.insets = new Insets(0, 0, 24, 0);
        JLabel sub = new JLabel("Đăng nhập tài khoản để tra cứu & mượn sách");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_MUTED);
        form.add(sub, gbc);

        // Label username
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel userLbl = new JLabel("Tên đăng nhập độc giả");
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(userLbl, gbc);

        // Input username
        gbc.gridy++; gbc.insets = new Insets(0, 0, 16, 0);
        usernameField = UITheme.createTextField("Nhập tên đăng nhập hoặc mã thẻ");
        usernameField.setPreferredSize(new Dimension(280, UITheme.INPUT_HEIGHT + 4));
        form.add(usernameField, gbc);

        // Label password
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel passLbl = new JLabel("Mật khẩu");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(passLbl, gbc);

        // Input password
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        passwordField = UITheme.createPasswordField("Nhập mật khẩu");
        passwordField.setPreferredSize(new Dimension(280, UITheme.INPUT_HEIGHT + 4));
        form.add(passwordField, gbc);

        // Label báo lỗi
        gbc.gridy++; gbc.insets = new Insets(2, 0, 10, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.COLOR_DANGER);
        form.add(errorLabel, gbc);

        // Nút Đăng nhập
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        loginButton = UITheme.createPrimaryButton("Đăng Nhập Cổng Độc Giả");
        loginButton.setPreferredSize(new Dimension(280, UITheme.BUTTON_HEIGHT + 6));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        form.add(loginButton, gbc);

        // Gợi ý tài khoản mẫu
        gbc.gridy++; gbc.insets = new Insets(16, 0, 0, 0);
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        hintPanel.setOpaque(false);
        JLabel hint = new JLabel("Tài khoản mẫu: ");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        JLabel hintCreds = new JLabel("docgia / docgia123");
        hintCreds.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        hintCreds.setForeground(UITheme.ACCENT_PRIMARY);
        hintPanel.add(hint);
        hintPanel.add(hintCreds);
        form.add(hintPanel, gbc);

        // Link đăng ký tài khoản độc giả
        gbc.gridy++; gbc.insets = new Insets(10, 0, 0, 0);
        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerPanel.setOpaque(false);
        JLabel regLabel = new JLabel("Chưa có thẻ thư viện?");
        regLabel.setFont(UITheme.FONT_SMALL);
        regLabel.setForeground(UITheme.TEXT_MUTED);
        registerPanel.add(regLabel);

        JButton btnRegister = new JButton("Đăng ký thẻ mới ngay");
        btnRegister.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        btnRegister.setForeground(new Color(0x0284C7));
        btnRegister.setBorderPainted(false);
        btnRegister.setContentAreaFilled(false);
        btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegister.addActionListener(e -> openRegisterDialog());
        registerPanel.add(btnRegister);
        form.add(registerPanel, gbc);

        // Sự kiện đăng nhập
        loginButton.addActionListener(e -> doLogin());
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);

        return form;
    }

    private void openRegisterDialog() {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        loginButton.setEnabled(false);
        loginButton.setText("⏳  Đang xác thực...");
        errorLabel.setText(" ");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                User user = authService.login(username, password);

                // Chặn tài khoản Quản trị / Thủ thư vào Cổng Độc Giả
                if (!user.isReader()) {
                    authService.logout();
                    throw new IllegalArgumentException(
                        "Đây là ứng dụng Cổng Độc Giả.\nTài khoản Quản trị / Thủ thư vui lòng mở ứng dụng Quản Trị Thư Viện (AdminApp)!"
                    );
                }

                // Tải thông tin độc giả liên kết
                if (user.getReaderId() > 0) {
                    loggedInReader = readerDAO.findById(user.getReaderId());
                }
                if (loggedInReader == null) {
                    // Thử tìm theo email nếu chưa liên kết reader_id trực tiếp
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        List<Reader> found = readerDAO.search(user.getEmail());
                        if (!found.isEmpty()) {
                            loggedInReader = found.get(0);
                        }
                    }
                }

                return user;
            }

            @Override
            protected void done() {
                try {
                    loggedInUser = get();
                    loginSuccess = true;
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    errorLabel.setText("<html>" + msg.replace("\n", "<br>") + "</html>");
                    passwordField.setText("");
                    passwordField.requestFocus();
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("Đăng Nhập Cổng Độc Giả");
                }
            }
        };
        worker.execute();
    }

    public boolean isLoginSuccess() {
        return loginSuccess;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public Reader getLoggedInReader() {
        return loggedInReader;
    }
}
