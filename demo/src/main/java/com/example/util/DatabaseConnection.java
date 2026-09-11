package com.example.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Quản lý kết nối SQLite - Singleton pattern.
 * File database được lưu tại thư mục chạy ứng dụng: ./data/library.db
 */
public class DatabaseConnection {

    public static File getCanonicalDbFile() {
        // 1. Nếu chạy từ thư mục gốc dự án, file chuẩn là demo/data/library.db
        File fDemo = new File("demo" + File.separator + "data" + File.separator + "library.db");
        if (fDemo.exists()) {
            return fDemo.getAbsoluteFile();
        }
        // 2. Nếu chạy từ thư mục demo/, file chuẩn là data/library.db
        File fData = new File("data" + File.separator + "library.db");
        if (fData.exists()) {
            return fData.getAbsoluteFile();
        }
        // 3. Nếu chạy từ thư mục con
        File fParent = new File(".." + File.separator + "data" + File.separator + "library.db");
        if (fParent.exists()) {
            return fParent.getAbsoluteFile();
        }

        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();
        return fData.getAbsoluteFile();
    }

    private static String getDbUrl() {
        return "jdbc:sqlite:" + getCanonicalDbFile().getAbsolutePath();
    }

    /**
     * Đồng bộ bản sao CSDL giữa data/library.db và demo/data/library.db
     * để dù chạy ở thư mục gốc hay demo/ dữ liệu cũng luôn 100% đồng nhất.
     */
    public static void syncMirrors() {
        try {
            File current = getCanonicalDbFile();
            if (!current.exists()) return;

            // Nếu đang dùng demo/data/library.db, copy sang root data/library.db
            File parentDir = current.getParentFile().getParentFile(); // demo
            if (parentDir != null && parentDir.getName().equalsIgnoreCase("demo")) {
                File rootDir = parentDir.getParentFile();
                if (rootDir != null) {
                    File rootDb = new File(rootDir, "data" + File.separator + "library.db");
                    if (rootDb.getParentFile().exists()) {
                        java.nio.file.Files.copy(current.toPath(), rootDb.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } else if (current.getName().equalsIgnoreCase("library.db") && current.getParentFile().getName().equalsIgnoreCase("data")) {
                // Nếu đang dùng root data/library.db, copy sang demo/data/library.db
                File demoDb = new File(current.getParentFile().getParentFile(), "demo" + File.separator + "data" + File.separator + "library.db");
                if (demoDb.getParentFile().exists()) {
                    java.nio.file.Files.copy(current.toPath(), demoDb.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception ignored) {}
    }

    /** Instance duy nhất (Singleton). */
    private static DatabaseConnection instance;

    /** Connection hiện tại. */
    private Connection connection;

    // ===================== Singleton =====================

    private DatabaseConnection() {}

    /** Lấy instance duy nhất của DatabaseConnection. */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // ===================== Connection =====================

    /**
     * Lấy (hoặc tạo mới) kết nối đến SQLite.
     * Tự động tạo thư mục "data/" nếu chưa tồn tại.
     */
    public Connection getConnection() throws SQLException {
        // Kiểm tra kết nối còn sống không
        if (connection == null || connection.isClosed()) {
            try {
                // Load driver SQLite
                Class.forName("org.sqlite.JDBC");
                String dbUrl = getDbUrl();
                connection = DriverManager.getConnection(dbUrl);

                // Autocommit phải = true trước khi chạy bất kỳ PRAGMA nào
                connection.setAutoCommit(true);

                // Bật foreign key constraints (quan trọng nhất)
                try (var st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                    st.execute("PRAGMA busy_timeout = 3000");
                    st.execute("PRAGMA synchronous = NORMAL");
                }

                System.out.println("[DB] Đã kết nối SQLite: " + dbUrl);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Không tìm thấy SQLite JDBC driver: " + e.getMessage());
            }
        }
        return connection;
    }

    /**
     * Đóng kết nối database.
     * Gọi khi thoát ứng dụng.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    syncMirrors();
                    System.out.println("[DB] Đã đóng kết nối SQLite.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Lỗi khi đóng kết nối: " + e.getMessage());
            } finally {
                connection = null;
                syncMirrors();
            }
        }
    }

    /**
     * Lấy đường dẫn file database.
     */
    public static String getDatabasePath() {
        return getDbUrl().replace("jdbc:sqlite:", "");
    }
}
