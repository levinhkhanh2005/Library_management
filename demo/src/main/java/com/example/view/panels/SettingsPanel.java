package com.example.view.panels;

import com.example.model.User;
import com.example.service.AuthService;
import com.example.util.DatabaseConnection;
import com.example.view.MainFrame;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel Cài Đặt — quản lý tài khoản người dùng, đổi mật khẩu, thông tin hệ thống.
 */
public class SettingsPanel extends JPanel implements MainFrame.Refreshable {

    private final AuthService authService = new AuthService();

    // User management (Admin only)
    private JTable            userTable;
    private DefaultTableModel userModel;
    private JButton           btnEditUser, btnDeleteUser, btnToggleActive;

    public SettingsPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_MD));
        setBackground(UITheme.BG_PRIMARY);

        add(UITheme.createPageHeader("⚙  Cài Đặt",
            "Quản lý tài khoản người dùng và thông tin hệ thống"),
            BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    // ================================================================
    //  Layout chính: 2 cột (trái: tài khoản | phải: đổi PW + sys info)
    // ================================================================

    private JPanel buildContent() {
        JPanel content = new JPanel(new GridLayout(1, 2, UITheme.PAD_LG, 0));
        content.setBackground(UITheme.BG_PRIMARY);

        content.add(buildAccountSection());
        content.add(buildRightColumn());
        return content;
    }

    // ================================================================
    //  Cột trái: Quản lý tài khoản (Admin only)
    // ================================================================

    private JPanel buildAccountSection() {
        JPanel card = createCard("👥  Quản Lý Tài Khoản");
        card.setLayout(new BorderLayout(0, UITheme.PAD_SM));

        boolean isAdmin = AuthService.isAdmin();

        if (!isAdmin) {
            JLabel msg = new JLabel(
                "<html><center>🔒<br><br>Chức năng này chỉ dành cho<br><b>Quản trị viên</b></center></html>",
                SwingConstants.CENTER);
            msg.setFont(UITheme.FONT_BODY);
            msg.setForeground(UITheme.TEXT_MUTED);
            card.add(msg, BorderLayout.CENTER);
            return card;
        }

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("＋  Thêm TK");
        btnEditUser     = UITheme.createSecondaryButton("✎  Sửa");
        btnDeleteUser   = UITheme.createDangerButton("✕  Xóa");
        btnToggleActive = UITheme.createSecondaryButton("🔒  Khóa/Mở");

        btnEditUser.setEnabled(false);
        btnDeleteUser.setEnabled(false);
        btnToggleActive.setEnabled(false);

        toolbar.add(btnAdd);
        toolbar.add(btnEditUser);
        toolbar.add(btnDeleteUser);
        toolbar.add(btnToggleActive);
        card.add(toolbar, BorderLayout.NORTH);

        // Bảng tài khoản
        userModel = new DefaultTableModel(
            new String[]{"#", "Username", "Họ Tên", "Vai Trò", "Trạng Thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userModel);
        UITheme.styleTable(userTable);
        userTable.getColumnModel().getColumn(0).setMaxWidth(40);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Badge vai trò
        userTable.getColumnModel().getColumn(3).setCellRenderer(
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                    p.setOpaque(true);
                    p.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                        : (r % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));
                    String role = val != null ? val.toString() : "";
                    p.add(UITheme.createBadge(role,
                        role.equals(User.Role.ADMIN.getLabel()) ? "danger" : "info"));
                    return p;
                }
            });

        // Badge trạng thái
        userTable.getColumnModel().getColumn(4).setCellRenderer(
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                    p.setOpaque(true);
                    p.setBackground(sel ? UITheme.TABLE_ROW_SELECTED
                        : (r % 2 == 0 ? UITheme.TABLE_ROW_ODD : UITheme.TABLE_ROW_EVEN));
                    String s = val != null ? val.toString() : "";
                    p.add(UITheme.createBadge(s, s.equals("Hoạt động") ? "success" : "danger"));
                    return p;
                }
            });

        userTable.getSelectionModel().addListSelectionListener(e -> {
            boolean sel = userTable.getSelectedRow() >= 0;
            btnEditUser.setEnabled(sel);
            btnDeleteUser.setEnabled(sel);
            btnToggleActive.setEnabled(sel);
        });

        card.add(UITheme.createTableScrollPane(userTable), BorderLayout.CENTER);

        // Sự kiện
        btnAdd.addActionListener(e -> openAddUserDialog());
        btnEditUser.addActionListener(e -> openEditUserDialog());
        btnDeleteUser.addActionListener(e -> deleteUser());
        btnToggleActive.addActionListener(e -> toggleUserActive());

        loadUsers();
        return card;
    }

    // ================================================================
    //  Cột phải: Đổi mật khẩu + Thông tin hệ thống
    // ================================================================

    private JPanel buildRightColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(UITheme.BG_PRIMARY);

        col.add(buildChangePasswordCard());
        col.add(Box.createVerticalStrut(UITheme.PAD_MD));
        col.add(buildSysInfoCard());
        return col;
    }

    private JPanel buildChangePasswordCard() {
        JPanel card = createCard("🔑  Đổi Mật Khẩu");
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets  = new Insets(6, 8, 6, 8);
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        JPasswordField fOld = UITheme.createPasswordField("Mật khẩu hiện tại");
        JPasswordField fNew = UITheme.createPasswordField("Mật khẩu mới (≥ 6 ký tự)");
        JPasswordField fCfm = UITheme.createPasswordField("Nhập lại mật khẩu mới");

        g.gridy = 0; g.gridx = 0; card.add(fieldLabel("Mật khẩu hiện tại"), g);
        g.gridy = 1; card.add(fOld, g);
        g.gridy = 2; card.add(fieldLabel("Mật khẩu mới"), g);
        g.gridy = 3; card.add(fNew, g);
        g.gridy = 4; card.add(fieldLabel("Xác nhận mật khẩu mới"), g);
        g.gridy = 5; card.add(fCfm, g);

        JButton btnSave = UITheme.createPrimaryButton("  ✓  Đổi Mật Khẩu  ");
        btnSave.setAlignmentX(CENTER_ALIGNMENT);
        g.gridy = 6; g.insets = new Insets(12, 8, 6, 8);
        card.add(btnSave, g);

        btnSave.addActionListener(e -> {
            String oldPw = new String(fOld.getPassword());
            String newPw = new String(fNew.getPassword());
            String cfmPw = new String(fCfm.getPassword());

            if (oldPw.isBlank()) {
                UITheme.showWarning(this, "Vui lòng nhập mật khẩu hiện tại.");
                return;
            }
            if (newPw.isBlank()) {
                UITheme.showWarning(this, "Vui lòng nhập mật khẩu mới.");
                return;
            }
            if (!newPw.equals(cfmPw)) {
                UITheme.showWarning(this, "Mật khẩu xác nhận không khớp.");
                return;
            }
            try {
                User cur = AuthService.getCurrentUser();
                if (cur == null) {
                    UITheme.showError(this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
                    return;
                }
                authService.changePassword(cur.getId(), oldPw, newPw);
                UITheme.showSuccess(this, "Đổi mật khẩu thành công!");
                fOld.setText(""); fNew.setText(""); fCfm.setText("");
            } catch (Exception ex) {
                UITheme.showWarning(this, ex.getMessage());
            }
        });

        return card;
    }

    private JPanel buildSysInfoCard() {
        JPanel card = createCard("ℹ  Thông Tin Hệ Thống");
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 10, 6, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;

        User cur = AuthService.getCurrentUser();

        String[][] rows = {
            {"Phần Mềm",       "QL Thư Viện Nguyễn Huệ v1.0"},
            {"Người Dùng",      cur != null ? cur.getFullName() + " (" + cur.getRole().getLabel() + ")" : "—"},
            {"Cơ Sở Dữ Liệu",  DatabaseConnection.getDatabasePath()},
            {"Java Version",   System.getProperty("java.version")},
            {"OS",             System.getProperty("os.name") + " " + System.getProperty("os.arch")},
        };

        for (int i = 0; i < rows.length; i++) {
            g.gridy = i;
            g.gridx = 0; g.weightx = 0.35;
            JLabel key = new JLabel(rows[i][0]);
            key.setFont(UITheme.FONT_BOLD);
            key.setForeground(UITheme.TEXT_SECONDARY);
            card.add(key, g);

            g.gridx = 1; g.weightx = 0.65;
            JLabel val = new JLabel("<html>" + rows[i][1] + "</html>");
            val.setFont(UITheme.FONT_BODY);
            val.setForeground(UITheme.TEXT_PRIMARY);
            card.add(val, g);
        }
        return card;
    }

    // ================================================================
    //  Logic quản lý user
    // ================================================================

    private void loadUsers() {
        if (userModel == null) return;
        try {
            List<User> users = authService.getAllUsers();
            userModel.setRowCount(0);
            int idx = 1;
            for (User u : users) {
                userModel.addRow(new Object[]{
                    idx++, u.getUsername(), u.getFullName(),
                    u.getRole().getLabel(),
                    u.isActive() ? "Hoạt động" : "Bị khóa"
                });
            }
        } catch (Exception ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) return null;
        String username = (String) userModel.getValueAt(row, 1);
        try {
            return authService.getAllUsers().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private void openAddUserDialog() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.setBackground(UITheme.BG_WHITE);
        JTextField tUser = UITheme.createTextField("username");
        JPasswordField tPass = UITheme.createPasswordField("mật khẩu");
        JTextField tName = UITheme.createTextField("Họ tên đầy đủ");
        JComboBox<String> tRole = new JComboBox<>(new String[]{
            User.Role.LIBRARIAN.getLabel(), User.Role.ADMIN.getLabel()});

        form.add(new JLabel("Username:")); form.add(tUser);
        form.add(new JLabel("Mật khẩu:")); form.add(tPass);
        form.add(new JLabel("Họ tên:"));   form.add(tName);
        form.add(new JLabel("Vai trò:"));  form.add(tRole);

        int res = JOptionPane.showConfirmDialog(this, form,
            "Thêm Tài Khoản Mới", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            User.Role role = User.Role.fromString((String) tRole.getSelectedItem());
            authService.addUser(tUser.getText(), new String(tPass.getPassword()),
                tName.getText(), role);
            UITheme.showSuccess(this, "Tạo tài khoản thành công!");
            loadUsers();
        } catch (Exception ex) {
            UITheme.showWarning(this, ex.getMessage());
        }
    }

    private void openEditUserDialog() {
        User u = getSelectedUser();
        if (u == null) return;

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField tName = UITheme.createTextField("");
        tName.setText(u.getFullName());
        JComboBox<String> tRole = new JComboBox<>(new String[]{
            User.Role.LIBRARIAN.getLabel(), User.Role.ADMIN.getLabel()});
        tRole.setSelectedItem(u.getRole().getLabel());
        form.add(new JLabel("Họ tên:")); form.add(tName);
        form.add(new JLabel("Vai trò:")); form.add(tRole);

        int res = JOptionPane.showConfirmDialog(this, form,
            "Sửa: " + u.getUsername(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        try {
            u.setFullName(tName.getText().trim());
            u.setRole(User.Role.fromString((String) tRole.getSelectedItem()));
            authService.updateUser(u);
            UITheme.showSuccess(this, "Đã cập nhật tài khoản.");
            loadUsers();
        } catch (Exception ex) {
            UITheme.showWarning(this, ex.getMessage());
        }
    }

    private void deleteUser() {
        User u = getSelectedUser();
        if (u == null) return;
        boolean confirm = UITheme.showConfirm(this,
            "Xóa tài khoản \"" + u.getUsername() + "\"?", "Xác nhận xóa");
        if (!confirm) return;
        try {
            authService.deleteUser(u.getId());
            UITheme.showSuccess(this, "Đã xóa tài khoản.");
            loadUsers();
        } catch (Exception ex) {
            UITheme.showWarning(this, ex.getMessage());
        }
    }

    private void toggleUserActive() {
        User u = getSelectedUser();
        if (u == null) return;
        try {
            authService.setUserActive(u.getId(), !u.isActive());
            UITheme.showSuccess(this, (u.isActive() ? "Đã khóa" : "Đã mở khóa")
                + " tài khoản \"" + u.getUsername() + "\".");
            loadUsers();
        } catch (Exception ex) {
            UITheme.showWarning(this, ex.getMessage());
        }
    }

    // ================================================================
    //  Helpers UI
    // ================================================================

    private JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(UITheme.BG_WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR), "  " + title + "  ");
        border.setTitleFont(UITheme.FONT_BOLD);
        border.setTitleColor(UITheme.TEXT_SECONDARY);
        card.setBorder(BorderFactory.createCompoundBorder(
            border, new EmptyBorder(UITheme.PAD_SM, UITheme.PAD_SM,
                                    UITheme.PAD_SM, UITheme.PAD_SM)));
        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    @Override public void refresh() { loadUsers(); }
}
