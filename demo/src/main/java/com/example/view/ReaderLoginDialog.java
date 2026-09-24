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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hộp thoại đăng nhập dành riêng cho ứng dụng Độc Giả (ReaderApp).
 * Giao diện hiện đại với banner gradient xanh đại dương, bokeh particles,
 * show/hide password, hiệu ứng hover trên nút, và liên kết đăng ký thẻ độc giả trực tuyến.
 *
 * v3.0 — Cải thiện:
 * - Show/hide mật khẩu
 * - Hiệu ứng hover nâng cao
 * - Hiển thị ngày giờ hiện tại
 * - Loading spinner đẹp hơn
 * - Gợi ý tài khoản nổi bật hơn
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
    private boolean        passwordVisible = false;

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
        for (int i = 0; i < 18; i++) {
            particles.add(new float[]{
                rng.nextFloat(), // x
                rng.nextFloat(), // y
                6 + rng.nextFloat() * 32, // radius
                0.04f + rng.nextFloat() * 0.12f, // alpha
                rng.nextFloat() * 0.35f - 0.17f // vy
            });
        }

        JPanel banner = new JPanel(new GridBagLayout()) {
            private float gradientPhase = 0f;
            private Timer animTimer;
            {
                animTimer = new Timer(45, e -> {
                    gradientPhase += 0.005f;
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

        banner.setPreferredSize(new Dimension(300, 520));
        banner.setBorder(new EmptyBorder(30, 24, 30, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 20, 8, 20);

        // Icon sách
        JLabel iconLabel = new JLabel("📖");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
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
        sepLine.setPreferredSize(new Dimension(180, 1));
        sepLine.setOpaque(false);
        banner.add(sepLine, gbc);

        // Tagline
        JLabel tagLabel = new JLabel("<html><center>Tra cứu tài liệu<br>Theo dõi sách mượn<br>Gia hạn trực tuyến<br>Đổi mật khẩu an toàn</center></html>");
        tagLabel.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        tagLabel.setForeground(new Color(0xBAE6FD));
        tagLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(tagLabel, gbc);

        // Badge version
        gbc.insets = new Insets(16, 20, 4, 20);
        JLabel versionLbl = new JLabel("Reader Edition v3.0");
        versionLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
        versionLbl.setForeground(new Color(0x7DD3FC));
        banner.add(versionLbl, gbc);

        // Date/Time
        gbc.insets = new Insets(0, 20, 6, 20);
        JLabel dateLbl = new JLabel(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy — HH:mm")));
        dateLbl.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
        dateLbl.setForeground(new Color(0x67E8F9));
        banner.add(dateLbl, gbc);

        return banner;
    }

    /** Form đăng nhập bên phải. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(new EmptyBorder(36, 44, 36, 44));
        form.setPreferredSize(new Dimension(390, 520));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);

        // Icon chào mừng
        JLabel waveIcon = new JLabel("👋");
        waveIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
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
        usernameField.setPreferredSize(new Dimension(300, UITheme.INPUT_HEIGHT + 4));
        form.add(usernameField, gbc);

        // Label password
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel passLbl = new JLabel("Mật khẩu");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(passLbl, gbc);

        // Input password with show/hide toggle
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JPanel passwordPanel = new JPanel(new BorderLayout(4, 0));
        passwordPanel.setOpaque(false);

        passwordField = UITheme.createPasswordField("Nhập mật khẩu");
        passwordField.setPreferredSize(new Dimension(260, UITheme.INPUT_HEIGHT + 4));
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        JButton togglePw = new JButton("👁");
        togglePw.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        togglePw.setPreferredSize(new Dimension(40, UITheme.INPUT_HEIGHT + 4));
        togglePw.setFocusPainted(false);
        togglePw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        togglePw.setToolTipText("Hiện/ẩn mật khẩu");
        togglePw.setContentAreaFilled(false);
        togglePw.setBorderPainted(false);
        togglePw.addActionListener(e -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                passwordField.setEchoChar((char) 0);
                togglePw.setText("🔒");
            } else {
                passwordField.setEchoChar('●');
                togglePw.setText("👁");
            }
        });
        passwordPanel.add(togglePw, BorderLayout.EAST);
        form.add(passwordPanel, gbc);

        // Label báo lỗi
        gbc.gridy++; gbc.insets = new Insets(2, 0, 10, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.COLOR_DANGER);
        form.add(errorLabel, gbc);

        // Nút Đăng nhập — xanh đại dương phù hợp tone Reader
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        loginButton = new JButton("Đăng Nhập Cổng Độc Giả") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                if (!isEnabled()) {
                    g2.setColor(new Color(0xE2E8F0));
                } else if (getModel().isPressed()) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x075985), w, 0, new Color(0x065F46)));
                } else if (getModel().isRollover()) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x0284C7), w, 0, new Color(0x0D9488)));
                } else {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x0369A1), w, 0, new Color(0x0891B2)));
                }
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.dispose();

                // Text
                g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? Color.WHITE : new Color(0x94A3B8));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        loginButton.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(300, UITheme.BUTTON_HEIGHT + 8));
        loginButton.setFocusPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        form.add(loginButton, gbc);

        // Gợi ý tài khoản mẫu — enhanced card style
        gbc.gridy++; gbc.insets = new Insets(20, 0, 0, 0);
        JPanel hintCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF0F9FF));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xBAE6FD));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        hintCard.setOpaque(false);
        hintCard.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel hintIcon = new JLabel("💡 ");
        hintIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        hintCard.add(hintIcon);

        JLabel hint = new JLabel("Tài khoản mẫu: ");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        hintCard.add(hint);

        JLabel hintCreds = new JLabel("docgia / docgia123");
        hintCreds.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        hintCreds.setForeground(new Color(0x0369A1));
        hintCreds.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hintCreds.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                usernameField.setText("docgia");
                passwordField.setText("docgia123");
                usernameField.requestFocus();
            }
        });
        hintCard.add(hintCreds);
        form.add(hintCard, gbc);

        // Link đăng ký tài khoản độc giả
        gbc.gridy++; gbc.insets = new Insets(12, 0, 0, 0);
        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerPanel.setOpaque(false);
        JLabel regLabel = new JLabel("Chưa có thẻ thư viện?");
        regLabel.setFont(UITheme.FONT_SMALL);
        regLabel.setForeground(UITheme.TEXT_MUTED);
        registerPanel.add(regLabel);

        JButton btnRegister = new JButton("Đăng ký thẻ mới ngay →");
        btnRegister.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        btnRegister.setForeground(new Color(0x0369A1));
        btnRegister.setBorderPainted(false);
        btnRegister.setContentAreaFilled(false);
        btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegister.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnRegister.setForeground(new Color(0x0C4A6E));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnRegister.setForeground(new Color(0x0369A1));
            }
        });
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
