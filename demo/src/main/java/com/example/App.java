package com.example;

import com.example.service.BorrowService;
import com.example.util.DatabaseConnection;
import com.example.util.DatabaseInitializer;
import com.example.view.LoginDialog;
import com.example.view.MainFrame;
import com.example.view.UITheme;

import javax.swing.*;

/**
 * Điểm vào của ứng dụng Quản Lý Thư Viện Nguyễn Huệ.
 */
public class App {

    public static void main(String[] args) {
        // 1. Áp dụng Look & Feel (phải gọi trước khi tạo bất kỳ component nào)
        UITheme.applyTheme();

        // 2. Chạy toàn bộ UI trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // 3. Khởi tạo CSDL (tạo bảng + dữ liệu mẫu nếu chưa có)
                DatabaseInitializer.initialize();

                // 4. Đồng bộ trạng thái quá hạn
                int overdueUpdated = new BorrowService().syncOverdueStatus();
                if (overdueUpdated > 0) {
                    System.out.println("[APP] Đã cập nhật " + overdueUpdated + " phiếu mượn quá hạn.");
                }

                // 5. Hiển thị màn hình đăng nhập
                LoginDialog login = new LoginDialog(null);
                login.setVisible(true);

                // 6. Nếu đăng nhập thành công → mở cửa sổ chính
                if (login.isLoginSuccess()) {
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                }
                // Nếu đóng login mà không đăng nhập → thoát ứng dụng
                else {
                    DatabaseConnection.getInstance().closeConnection();
                    System.exit(0);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Lỗi khởi động ứng dụng:\n" + e.getMessage(),
                    "Lỗi nghiêm trọng", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        // Đóng kết nối DB khi JVM tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.getInstance().closeConnection();
            System.out.println("[APP] Ứng dụng đã tắt.");
        }));
    }
}
