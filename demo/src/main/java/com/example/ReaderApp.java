package com.example;

import com.example.model.Reader;
import com.example.model.User;
import com.example.service.AuthService;
import com.example.util.DatabaseConnection;
import com.example.util.DatabaseInitializer;
import com.example.view.ReaderLoginDialog;
import com.example.view.ReaderPortalFrame;
import com.example.view.UITheme;

import javax.swing.*;

/**
 * Điểm vào chính của Ứng Dụng Độc Giả — Thư Viện Nguyễn Huệ.
 * Chạy độc lập với Ứng Dụng Quản Trị (AdminApp),
 * hỗ trợ bạn đọc tra cứu tài liệu, theo dõi sách đang mượn, gia hạn sách và đăng ký thẻ mới.
 */
public class ReaderApp {

    public static void main(String[] args) {
        // 1. Cấu hình Look & Feel hiện đại
        UITheme.applyTheme();

        // 2. Chạy UI trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // 3. Khởi tạo CSDL nếu chưa có
                DatabaseInitializer.initialize();

                // 4. Mở hộp thoại đăng nhập Cổng Độc Giả
                showLogin();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Lỗi khởi động Cổng Độc Giả:\n" + e.getMessage(),
                        "Lỗi nghiêm trọng", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        // 5. Đóng kết nối khi tắt ứng dụng
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.getInstance().closeConnection();
            System.out.println("[READER-APP] Cổng Độc Giả đã tắt.");
        }));
    }

    /**
     * Hiển thị màn hình đăng nhập cho độc giả.
     * Được gọi khi khởi động ứng dụng hoặc khi độc giả đăng xuất.
     */
    public static void showLogin() {
        ReaderLoginDialog login = new ReaderLoginDialog(null);
        login.setVisible(true);

        if (login.isLoginSuccess()) {
            User user = login.getLoggedInUser();
            Reader reader = login.getLoggedInReader();
            ReaderPortalFrame portal = new ReaderPortalFrame(user, reader);
            portal.setVisible(true);
        } else {
            // Nếu đóng dialog mà không đăng nhập -> thoát ứng dụng
            if (!AuthService.isLoggedIn()) {
                DatabaseConnection.getInstance().closeConnection();
                System.exit(0);
            }
        }
    }
}
