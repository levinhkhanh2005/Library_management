package com.example;

/**
 * Điểm vào đa năng của ứng dụng Quản Lý Thư Viện Nguyễn Huệ.
 * - Mặc định: Khởi chạy Ứng dụng Quản Trị (AdminApp).
 * - Nếu truyền tham số "--reader" hoặc "-r": Khởi chạy Ứng dụng Độc Giả (ReaderApp).
 */
public class App {

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if ("--reader".equalsIgnoreCase(arg) || "-r".equalsIgnoreCase(arg)) {
                    ReaderApp.main(args);
                    return;
                }
                if ("--admin".equalsIgnoreCase(arg) || "-a".equalsIgnoreCase(arg)) {
                    AdminApp.main(args);
                    return;
                }
            }
        }

        // Mặc định khởi chạy ứng dụng Quản Trị
        AdminApp.main(args);
    }
}