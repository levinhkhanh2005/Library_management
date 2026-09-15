package com.example;

import com.example.service.AuthService;
import com.example.service.BorrowService;
import com.example.util.DatabaseConnection;
import com.example.util.DatabaseInitializer;
import com.example.view.LoginDialog;
import com.example.view.MainFrame;
import com.example.view.UITheme;

import javax.swing.*;

/**
 * Điểm vào chính của Ứng Dụng Quản Trị Thư Viện — Nguyễn Huệ.
 * Dành riêng cho Quản trị viên (Admin) và Thủ thư (Librarian).
 * Cho phép chạy song song với Ứng Dụng Độc Giả (ReaderApp).
 */
public class AdminApp {

    public static void main(String[] args) {
        // 1. Cấu hình Look & Feel hiện đại
        UITheme.applyTheme();

        // 2. Chạy UI trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // 3. Khởi tạo CSDL nếu chưa có
                DatabaseInitializer.initialize();

                // 4. Đồng bộ trạng thái quá hạn của các phiếu mượn
                int overdueUpdated = new BorrowService().syncOverdueStatus();
                if (overdueUpdated > 0) {
                    System.out.println("[ADMIN-APP] Đã cập nhật " + overdueUpdated + " phiếu mượn quá hạn.");
                }

                // 5. Mở màn hình đăng nhập Quản Trị
                showLogin();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Lỗi khởi động Ứng Dụng Quản Trị:\n" + e.getMessage(),
                        "Lỗi nghiêm trọng", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        // 6. Đóng kết nối DB khi tắt ứng dụng
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.getInstance().closeConnection();
            System.out.println("[ADMIN-APP] Ứng dụng Quản Trị đã tắt.");
        }));
    }

    /**
     * Hiển thị màn hình đăng nhập cho Quản trị viên / Thủ thư.
     * Được gọi khi khởi động ứng dụng hoặc khi quản trị viên đăng xuất.
     */
    public static void showLogin() {
        LoginDialog login = new LoginDialog(null);
        login.setVisible(true);

        if (login.isLoginSuccess()) {
            new MainFrame().setVisible(true);
        } else {
            // Nếu đóng dialog mà không đăng nhập -> đóng kết nối và thoát
            if (!AuthService.isLoggedIn()) {
                DatabaseConnection.getInstance().closeConnection();
                System.exit(0);
            }
        }
    }
}
