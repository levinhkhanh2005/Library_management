package com.example.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Khởi tạo cơ sở dữ liệu SQLite.
 * Tạo tất cả bảng nếu chưa tồn tại và chèn dữ liệu mặc định.
 */
public class DatabaseInitializer {

    // ============================================================
    //  DDL - Tạo bảng
    // ============================================================

    private static final String CREATE_TABLE_BOOKS = """
            CREATE TABLE IF NOT EXISTS books (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn             TEXT    UNIQUE,
                title            TEXT    NOT NULL,
                author           TEXT    NOT NULL,
                category         TEXT,
                publisher        TEXT,
                publish_year     INTEGER,
                total_copies     INTEGER NOT NULL DEFAULT 1,
                available_copies INTEGER NOT NULL DEFAULT 1,
                description      TEXT,
                created_at       TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String CREATE_TABLE_READERS = """
            CREATE TABLE IF NOT EXISTS readers (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                reader_code TEXT    UNIQUE NOT NULL,
                full_name   TEXT    NOT NULL,
                birth_date  TEXT,
                phone       TEXT,
                email       TEXT,
                address     TEXT,
                join_date   TEXT    NOT NULL,
                status      TEXT    NOT NULL DEFAULT 'ACTIVE',
                created_at  TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String CREATE_TABLE_BORROWS = """
            CREATE TABLE IF NOT EXISTS borrows (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                book_id     INTEGER NOT NULL,
                reader_id   INTEGER NOT NULL,
                borrow_date TEXT    NOT NULL,
                due_date    TEXT    NOT NULL,
                return_date TEXT,
                status      TEXT    NOT NULL DEFAULT 'BORROWING',
                fine_amount REAL             DEFAULT 0.0,
                notes       TEXT,
                created_at  TEXT    DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (book_id)   REFERENCES books(id),
                FOREIGN KEY (reader_id) REFERENCES readers(id)
            )
            """;

    private static final String CREATE_TABLE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                username   TEXT    UNIQUE NOT NULL,
                password   TEXT    NOT NULL,
                full_name  TEXT    NOT NULL,
                role       TEXT    NOT NULL DEFAULT 'LIBRARIAN',
                active     INTEGER NOT NULL DEFAULT 1,
                created_at TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    // ============================================================
    //  Index để tăng tốc truy vấn
    // ============================================================

    private static final String[] CREATE_INDEXES = {
        "CREATE INDEX IF NOT EXISTS idx_books_title    ON books(title)",
        "CREATE INDEX IF NOT EXISTS idx_books_author   ON books(author)",
        "CREATE INDEX IF NOT EXISTS idx_books_category ON books(category)",
        "CREATE INDEX IF NOT EXISTS idx_borrows_book   ON borrows(book_id)",
        "CREATE INDEX IF NOT EXISTS idx_borrows_reader ON borrows(reader_id)",
        "CREATE INDEX IF NOT EXISTS idx_borrows_status ON borrows(status)",
    };

    // ============================================================
    //  Dữ liệu mặc định
    // ============================================================

    /** Chèn tài khoản admin mặc định (username: admin / password: admin123). */
    private static final String INSERT_DEFAULT_ADMIN = """
            INSERT OR IGNORE INTO users (username, password, full_name, role, active)
            VALUES ('admin', 'admin123', 'Quản trị viên', 'ADMIN', 1)
            """;

    /** Chèn tài khoản thủ thư mặc định (username: thuthu / password: thuthu123). */
    private static final String INSERT_DEFAULT_LIBRARIAN = """
            INSERT OR IGNORE INTO users (username, password, full_name, role, active)
            VALUES ('thuthu', 'thuthu123', 'Nguyễn Thị Thu', 'LIBRARIAN', 1)
            """;

    // Dữ liệu mẫu sách
    private static final String[] INSERT_SAMPLE_BOOKS = {
        """
        INSERT OR IGNORE INTO books (isbn, title, author, category, publisher, publish_year, total_copies, available_copies, description)
        VALUES ('978-604-1-00001-0', 'Dế Mèn Phiêu Lưu Ký', 'Tô Hoài', 'Thiếu nhi',
                'NXB Kim Đồng', 1941, 5, 5, 'Cuốn sách nổi tiếng về chú dế mèn phiêu lưu')
        """,
        """
        INSERT OR IGNORE INTO books (isbn, title, author, category, publisher, publish_year, total_copies, available_copies, description)
        VALUES ('978-604-1-00002-0', 'Số Đỏ', 'Vũ Trọng Phụng', 'Văn học',
                'NXB Văn Học', 1936, 3, 3, 'Tiểu thuyết trào phúng nổi tiếng của văn học Việt Nam')
        """,
        """
        INSERT OR IGNORE INTO books (isbn, title, author, category, publisher, publish_year, total_copies, available_copies, description)
        VALUES ('978-604-1-00003-0', 'Lập Trình Java Cơ Bản', 'Nguyễn Văn An', 'Công nghệ',
                'NXB Thông Tin', 2020, 4, 4, 'Sách học lập trình Java từ cơ bản đến nâng cao')
        """,
        """
        INSERT OR IGNORE INTO books (isbn, title, author, category, publisher, publish_year, total_copies, available_copies, description)
        VALUES ('978-604-1-00004-0', 'Nhà Giả Kim', 'Paulo Coelho', 'Triết học',
                'NXB Hội Nhà Văn', 1988, 6, 6, 'Câu chuyện về hành trình tìm kiếm kho báu và ý nghĩa cuộc đời')
        """,
        """
        INSERT OR IGNORE INTO books (isbn, title, author, category, publisher, publish_year, total_copies, available_copies, description)
        VALUES ('978-604-1-00005-0', 'Đắc Nhân Tâm', 'Dale Carnegie', 'Kỹ năng sống',
                'NXB Tổng Hợp', 1936, 8, 8, 'Nghệ thuật thu phục lòng người')
        """
    };

    // Dữ liệu mẫu độc giả
    private static final String[] INSERT_SAMPLE_READERS = {
        """
        INSERT OR IGNORE INTO readers (reader_code, full_name, birth_date, phone, email, address, join_date, status)
        VALUES ('NDG-0001', 'Nguyễn Thị Hoa', '15/03/2000', '0901234567',
                'hoa.nguyen@email.com', '123 Nguyễn Huệ, Quận 1, TP.HCM',
                '01/01/2024', 'ACTIVE')
        """,
        """
        INSERT OR IGNORE INTO readers (reader_code, full_name, birth_date, phone, email, address, join_date, status)
        VALUES ('NDG-0002', 'Trần Văn Nam', '20/07/1998', '0912345678',
                'nam.tran@email.com', '456 Lê Lợi, Quận 1, TP.HCM',
                '15/02/2024', 'ACTIVE')
        """,
        """
        INSERT OR IGNORE INTO readers (reader_code, full_name, birth_date, phone, email, address, join_date, status)
        VALUES ('NDG-0003', 'Lê Thị Mai', '08/11/2001', '0923456789',
                'mai.le@email.com', '789 Đồng Khởi, Quận 1, TP.HCM',
                '10/03/2024', 'ACTIVE')
        """
    };

    // ============================================================
    //  Phương thức khởi tạo
    // ============================================================

    /**
     * Khởi tạo toàn bộ cơ sở dữ liệu:
     * tạo bảng, index, chèn dữ liệu mặc định và dữ liệu mẫu.
     */
    public static void initialize() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            System.out.println("[DB] Bắt đầu khởi tạo cơ sở dữ liệu...");

            createTables(conn);
            createIndexes(conn);
            insertDefaultData(conn);
            insertSampleData(conn);

            System.out.println("[DB] Khởi tạo cơ sở dữ liệu hoàn thành!");
            System.out.println("[DB] File DB: " + DatabaseConnection.getDatabasePath());

        } catch (SQLException e) {
            System.err.println("[DB] Lỗi khởi tạo DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Tạo tất cả bảng. */
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_BOOKS);
            stmt.execute(CREATE_TABLE_READERS);
            stmt.execute(CREATE_TABLE_BORROWS);
            stmt.execute(CREATE_TABLE_USERS);
            System.out.println("[DB] Tạo bảng thành công.");
        }
    }

    /** Tạo index. */
    private static void createIndexes(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (String sql : CREATE_INDEXES) {
                stmt.execute(sql);
            }
            System.out.println("[DB] Tạo index thành công.");
        }
    }

    /** Chèn dữ liệu người dùng mặc định. */
    private static void insertDefaultData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(INSERT_DEFAULT_ADMIN);
            stmt.execute(INSERT_DEFAULT_LIBRARIAN);
            System.out.println("[DB] Dữ liệu người dùng mặc định đã được tạo.");
        }
    }

    /** Chèn dữ liệu mẫu (sách + độc giả). */
    private static void insertSampleData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            for (String sql : INSERT_SAMPLE_BOOKS) {
                stmt.execute(sql);
            }
            for (String sql : INSERT_SAMPLE_READERS) {
                stmt.execute(sql);
            }
            System.out.println("[DB] Dữ liệu mẫu đã được chèn.");
        }
    }
}
