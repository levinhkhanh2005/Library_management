package com.example.view.dialogs;

import com.example.model.OtpVerification;
import com.example.service.OtpService;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Hộp thoại nhập mã OTP 6 chữ số với đồng hồ đếm ngược.
 * Hỗ trợ gửi lại mã OTP (cooldown 60s) và giới hạn số lần nhập sai.
 */
public class OtpDialog extends JDialog {

    private final OtpService otpService = new OtpService();
    private final String email;
    private final OtpVerification.OtpType otpType;
    private final String payload;

    private boolean verified = false;
    private OtpVerification verifiedOtp = null;

    // UI Components
    private final JTextField[] digitFields = new JTextField[6];
    private JLabel timerLabel;
    private JLabel errorLabel;
    private JButton btnVerify;
    private JButton btnResend;
    private Timer countdownTimer;
    private int remainingSeconds;

    /**
     * @param parent  Frame cha
     * @param email   Email đã gửi OTP
     * @param otpType Loại OTP
     * @param payload Payload JSON (thông tin đăng ký tạm)
     */
    public OtpDialog(Dialog parent, String email, OtpVerification.OtpType otpType,
                     String payload) {
        super(parent, "Xác Thực Mã OTP", true);
        this.email = email;
        this.otpType = otpType;
        this.payload = payload;
        this.remainingSeconds = OtpVerification.EXPIRY_MINUTES * 60;

        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Bắt đầu đếm ngược
        startCountdown();
    }

    // ================================================================
    //  UI
    // ================================================================

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);
        root.setBorder(new EmptyBorder(32, 40, 32, 40));
        setContentPane(root);

        // === Panel chính ===
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        // Icon + Tiêu đề
        JLabel iconLabel = new JLabel("📧");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(iconLabel);
        mainPanel.add(Box.createVerticalStrut(12));

        JLabel titleLabel = new JLabel("Nhập Mã Xác Thực");
        titleLabel.setFont(UITheme.FONT_H2);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(8));

        // Mô tả
        String maskedEmail = maskEmail(email);
        JLabel descLabel = new JLabel("<html><center>Mã OTP 6 chữ số đã được gửi đến<br>"
            + "<b>" + maskedEmail + "</b></center></html>");
        descLabel.setFont(UITheme.FONT_BODY);
        descLabel.setForeground(UITheme.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(descLabel);
        mainPanel.add(Box.createVerticalStrut(24));

        // === 6 ô nhập OTP ===
        JPanel otpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        otpPanel.setOpaque(false);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            JTextField field = new JTextField(1);
            field.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 24));
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setPreferredSize(new Dimension(48, 56));
            field.setForeground(UITheme.ACCENT_PRIMARY);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2),
                new EmptyBorder(4, 4, 4, 4)
            ));

            // Chỉ cho nhập 1 ký tự số
            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!Character.isDigit(c)) {
                        e.consume();
                        return;
                    }
                    // Nếu đã có ký tự, xóa trước
                    if (!field.getText().isEmpty()) {
                        field.setText("");
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    // Auto-focus sang ô tiếp theo
                    if (!field.getText().isEmpty() && idx < 5) {
                        digitFields[idx + 1].requestFocus();
                    }
                    // Backspace -> quay lại ô trước
                    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && field.getText().isEmpty() && idx > 0) {
                        digitFields[idx - 1].requestFocus();
                    }
                    // Enter = xác nhận
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        doVerify();
                    }
                    updateVerifyButton();
                }
            });

            // Focus highlight
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.ACCENT_PRIMARY, 2),
                        new EmptyBorder(4, 4, 4, 4)
                    ));
                    field.selectAll();
                }
                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2),
                        new EmptyBorder(4, 4, 4, 4)
                    ));
                }
            });

            digitFields[i] = field;
            otpPanel.add(field);
        }
        mainPanel.add(otpPanel);
        mainPanel.add(Box.createVerticalStrut(12));

        // Timer label
        timerLabel = new JLabel("05:00");
        timerLabel.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 16));
        timerLabel.setForeground(UITheme.COLOR_WARNING);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(timerLabel);
        mainPanel.add(Box.createVerticalStrut(8));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.COLOR_DANGER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createVerticalStrut(16));

        // Nút Xác Nhận
        btnVerify = new JButton("Xác Nhận") {
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
        btnVerify.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 14));
        btnVerify.setPreferredSize(new Dimension(320, UITheme.BUTTON_HEIGHT + 6));
        btnVerify.setMaximumSize(new Dimension(320, UITheme.BUTTON_HEIGHT + 6));
        btnVerify.setFocusPainted(false);
        btnVerify.setContentAreaFilled(false);
        btnVerify.setBorderPainted(false);
        btnVerify.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnVerify.setEnabled(false);
        btnVerify.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnVerify.addActionListener(e -> doVerify());
        mainPanel.add(btnVerify);
        mainPanel.add(Box.createVerticalStrut(16));

        // Gửi lại mã
        JPanel resendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        resendPanel.setOpaque(false);
        JLabel resendLabel = new JLabel("Chưa nhận được mã?");
        resendLabel.setFont(UITheme.FONT_SMALL);
        resendLabel.setForeground(UITheme.TEXT_MUTED);
        resendPanel.add(resendLabel);

        btnResend = new JButton("Gửi lại");
        btnResend.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 11));
        btnResend.setForeground(UITheme.ACCENT_PRIMARY);
        btnResend.setBorderPainted(false);
        btnResend.setContentAreaFilled(false);
        btnResend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResend.setEnabled(false); // Bắt đầu disabled (cooldown 60s)
        btnResend.addActionListener(e -> doResend());
        resendPanel.add(btnResend);
        mainPanel.add(resendPanel);

        root.add(mainPanel, BorderLayout.CENTER);

        // Kích hoạt nút "Gửi lại" sau cooldown
        Timer resendTimer = new Timer(OtpVerification.RESEND_COOLDOWN_SECONDS * 1000, e -> {
            btnResend.setEnabled(true);
            btnResend.setText("Gửi lại");
        });
        resendTimer.setRepeats(false);
        resendTimer.start();
    }

    // ================================================================
    //  Countdown Timer
    // ================================================================

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            int min = remainingSeconds / 60;
            int sec = remainingSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", min, sec));

            if (remainingSeconds <= 60) {
                timerLabel.setForeground(UITheme.COLOR_DANGER);
            }
            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                timerLabel.setText("Mã đã hết hạn!");
                for (JTextField f : digitFields) f.setEnabled(false);
                btnVerify.setEnabled(false);
                errorLabel.setText("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
            }
        });
        countdownTimer.start();
    }

    // ================================================================
    //  Actions
    // ================================================================

    private void updateVerifyButton() {
        boolean allFilled = true;
        for (JTextField f : digitFields) {
            if (f.getText().isEmpty()) { allFilled = false; break; }
        }
        btnVerify.setEnabled(allFilled && remainingSeconds > 0);
    }

    private String getEnteredCode() {
        StringBuilder sb = new StringBuilder();
        for (JTextField f : digitFields) sb.append(f.getText());
        return sb.toString();
    }

    private void doVerify() {
        String code = getEnteredCode();
        if (code.length() != 6) return;

        btnVerify.setEnabled(false);
        btnVerify.setText("Đang xác thực...");
        errorLabel.setText(" ");

        SwingWorker<OtpVerification, Void> worker = new SwingWorker<>() {
            @Override
            protected OtpVerification doInBackground() throws Exception {
                return otpService.verifyOtp(email, code, otpType);
            }

            @Override
            protected void done() {
                try {
                    verifiedOtp = get();
                    verified = true;
                    if (countdownTimer != null) countdownTimer.stop();
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    errorLabel.setText("<html>" + msg.replace("\n", "<br>") + "</html>");
                    // Clear nhập liệu
                    for (JTextField f : digitFields) f.setText("");
                    digitFields[0].requestFocus();
                    updateVerifyButton();
                } finally {
                    btnVerify.setText("Xác Nhận");
                }
            }
        };
        worker.execute();
    }

    private void doResend() {
        btnResend.setEnabled(false);
        btnResend.setText("Đang gửi...");
        errorLabel.setText(" ");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                otpService.sendOtp(email, otpType, payload);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // Reset countdown
                    remainingSeconds = OtpVerification.EXPIRY_MINUTES * 60;
                    timerLabel.setForeground(UITheme.COLOR_WARNING);
                    for (JTextField f : digitFields) {
                        f.setEnabled(true);
                        f.setText("");
                    }
                    digitFields[0].requestFocus();
                    errorLabel.setText("<html><span style='color:#10B981'>Đã gửi lại mã OTP!</span></html>");

                    // Restart cooldown cho nút Gửi lại
                    Timer cd = new Timer(OtpVerification.RESEND_COOLDOWN_SECONDS * 1000, e2 -> {
                        btnResend.setEnabled(true);
                        btnResend.setText("Gửi lại");
                    });
                    cd.setRepeats(false);
                    cd.start();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    errorLabel.setText("<html>" + msg + "</html>");
                    btnResend.setEnabled(true);
                }
                btnResend.setText("Gửi lại");
            }
        };
        worker.execute();
    }

    // ================================================================
    //  Helpers
    // ================================================================

    /** Ẩn bớt email: abc***@gmail.com */
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 3) return email;
        return email.substring(0, 3) + "***" + email.substring(at);
    }

    @Override
    public void dispose() {
        if (countdownTimer != null) countdownTimer.stop();
        super.dispose();
    }

    // ================================================================
    //  Getters
    // ================================================================

    public boolean isVerified() { return verified; }
    public OtpVerification getVerifiedOtp() { return verifiedOtp; }
}
