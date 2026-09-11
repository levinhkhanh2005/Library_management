package com.example.view;

import com.example.model.User;
import com.example.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Hộp thoại đăng nhập — hiển thị trước MainFrame.
 * v2.0 — Animated gradient banner, bokeh particles, form icons.
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

    // ================================================================
    //  Banner với animated gradient + floating bokeh
    // ================================================================

    /** Banner bên trái với gradient chuyển màu + hạt bokeh. */
    private JPanel buildBanner() {
        // Bokeh particles
        Random rng = new Random(42);
        List<float[]> particles = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            particles.add(new float[]{
                rng.nextFloat(), // x (0..1)
                rng.nextFloat(), // y (0..1)
                8 + rng.nextFloat() * 30, // radius
                0.04f + rng.nextFloat() * 0.12f, // alpha
                rng.nextFloat() * 0.4f - 0.2f // vy (speed)
            });
        }

        JPanel banner = new JPanel(new GridBagLayout()) {
            private float gradientPhase = 0f;
            private Timer animTimer;
            {
                // Animated gradient phase shift
                animTimer = new Timer(50, e -> {
                    gradientPhase += 0.008f;
                    if (gradientPhase > 1f) gradientPhase = 0f;
                    // Move particles
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

                // Animated gradient — shift colors
                float phase = gradientPhase;
                Color c1 = blendColors(new Color(0x0F172A), new Color(0x1E1B4B), phase);
                Color c2 = blendColors(new Color(0x312E81), new Color(0x4F46E5), phase);
                Color c3 = blendColors(new Color(0x1E1B4B), new Color(0x0F172A), phase);

                GradientPaint gp = new GradientPaint(0, 0, c1, w * 0.5f, h, c2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Second gradient overlay
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                GradientPaint gp2 = new GradientPaint(w, 0, c3, 0, h, new Color(0x7C3AED));
                g2.setPaint(gp2);
                g2.fillRect(0, 0, w, h);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                // Bokeh particles
                for (float[] p : particles) {
                    float px = p[0] * w;
                    float py = p[1] * h;
                    float r = p[2];
                    float alpha = p[3];
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.setColor(new Color(0xA5B4FC));
                    g2.fill(new Ellipse2D.Float(px - r, py - r, r * 2, r * 2));
                }
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

            private Color blendColors(Color a, Color b, float t) {
                float it = 1 - t;
                return new Color(
                    Math.min(255, (int)(a.getRed() * it + b.getRed() * t)),
                    Math.min(255, (int)(a.getGreen() * it + b.getGreen() * t)),
                    Math.min(255, (int)(a.getBlue() * it + b.getBlue() * t))
                );
            }

            @Override public void removeNotify() {
                super.removeNotify();
                if (animTimer != null) animTimer.stop();
            }
        };
        banner.setPreferredSize(new Dimension(300, 460));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 30, 8, 30);

        // Icon sách — lớn hơn
        JLabel iconLabel = new JLabel("📚");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        banner.add(iconLabel, gbc);

        // Tên thư viện
        JLabel titleLabel = new JLabel("<html><center>THƯ VIỆN<br>NGUYỄN HUỆ</center></html>");
        titleLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(titleLabel, gbc);

        // Đường kẻ gradient
        JPanel sepLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0xFF, 0xFF, 0xFF, 0),
                    getWidth() / 2, 0, new Color(0xFF, 0xFF, 0xFF, 120),
                    true));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sepLine.setPreferredSize(new Dimension(180, 1));
        sepLine.setOpaque(false);
        banner.add(sepLine, gbc);

        // Tagline
        JLabel tagLabel = new JLabel("<html><center>Hệ thống quản lý<br>thư viện thông minh</center></html>");
        tagLabel.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 12));
        tagLabel.setForeground(new Color(0xA5B4FC));
        tagLabel.setHorizontalAlignment(SwingConstants.CENTER);
        banner.add(tagLabel, gbc);

        // Version badge
        gbc.insets = new Insets(20, 30, 8, 30);
        JLabel versionLbl = new JLabel("v2.0");
        versionLbl.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
        versionLbl.setForeground(new Color(0x818CF8));
        banner.add(versionLbl, gbc);

        return banner;
    }

    /** Form đăng nhập bên phải. */
    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.BG_WHITE);
        form.setBorder(new EmptyBorder(40, 44, 40, 44));
        form.setPreferredSize(new Dimension(360, 460));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);

        // Icon chào mừng
        JLabel waveIcon = new JLabel("👋");
        waveIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        form.add(waveIcon, gbc);

        // Tiêu đề
        gbc.gridy++; gbc.insets = new Insets(4, 0, 4, 0);
        JLabel heading = new JLabel("Chào mừng trở lại!");
        heading.setFont(UITheme.FONT_H1);
        heading.setForeground(UITheme.TEXT_PRIMARY);
        form.add(heading, gbc);

        gbc.gridy++; gbc.insets = new Insets(0, 0, 28, 0);
        JLabel sub = new JLabel("Vui lòng đăng nhập để tiếp tục");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_MUTED);
        form.add(sub, gbc);

        // Label username
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        JLabel userLbl = new JLabel("Tên đăng nhập");
        userLbl.setFont(UITheme.FONT_BOLD);
        userLbl.setForeground(UITheme.TEXT_PRIMARY);
        form.add(userLbl, gbc);

        // Input username
        gbc.gridy++; gbc.insets = new Insets(0, 0, 18, 0);
        usernameField = UITheme.createTextField("Nhập tên đăng nhập");
        usernameField.setPreferredSize(new Dimension(270, UITheme.INPUT_HEIGHT + 4));
        usernameField.putClientProperty("JTextField.leadingIcon",
            UIManager.getIcon("Tree.closedIcon")); // fallback icon
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
        passwordField.setPreferredSize(new Dimension(270, UITheme.INPUT_HEIGHT + 4));
        passwordField.putClientProperty("JTextField.showRevealButton", true);
        form.add(passwordField, gbc);

        // Error label (ẩn ban đầu)
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.COLOR_DANGER);
        form.add(errorLabel, gbc);

        // Nút đăng nhập — gradient style
        gbc.gridy++; gbc.insets = new Insets(0, 0, 0, 0);
        loginButton = new JButton("Đăng Nhập") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();
                if (isEnabled()) {
                    g2.setPaint(new GradientPaint(0, 0, UITheme.ACCENT_PRIMARY,
                        w, 0, new Color(0x7C3AED)));
                } else {
                    g2.setColor(UITheme.TEXT_MUTED);
                }
                g2.fillRoundRect(0, 0, w, h, UITheme.BORDER_RADIUS * 2, UITheme.BORDER_RADIUS * 2);

                // Hover glow
                if (getModel().isRollover() && isEnabled()) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, UITheme.BORDER_RADIUS * 2, UITheme.BORDER_RADIUS * 2);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
                g2.dispose();

                // Draw text
                g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        loginButton.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(270, UITheme.BUTTON_HEIGHT + 6));
        loginButton.setFocusPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        form.add(loginButton, gbc);

        // Gợi ý tài khoản mặc định
        gbc.gridy++; gbc.insets = new Insets(24, 0, 0, 0);
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        hintPanel.setOpaque(false);
        JLabel hint = new JLabel("Mặc định: ");
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        JLabel hintCreds = new JLabel("admin / admin123");
        hintCreds.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        hintCreds.setForeground(UITheme.ACCENT_PRIMARY);
        hintPanel.add(hint);
        hintPanel.add(hintCreds);
        form.add(hintPanel, gbc);

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
        loginButton.setText("⏳  Đang đăng nhập...");
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
                    loginButton.setText("Đăng Nhập");
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
