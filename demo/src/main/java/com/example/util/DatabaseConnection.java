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

    private static String getDbUrl() {
        File f1 = new File("data" + File.separator + "library.db");
        File f2 = new File("demo" + File.separator + "data" + File.separator + "library.db");
        File f3 = new File(".." + File.separator + "data" + File.separator + "library.db");

        if (f1.exists()) {
            return "jdbc:sqlite:" + f1.getAbsolutePath();
        } else if (f2.exists()) {
            return "jdbc:sqlite:" + f2.getAbsolutePath();
        } else if (f3.exists()) {
            return "jdbc:sqlite:" + f3.getAbsolutePath();
        } else {
            File dataDir = new File("data");
            if (!dataDir.exists()) dataDir.mkdirs();
            return "jdbc:sqlite:" + f1.getAbsolutePath();
        }
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
                    System.out.println("[DB] Đã đóng kết nối SQLite.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Lỗi khi đóng kết nối: " + e.getMessage());
            } finally {
                connection = null;
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
