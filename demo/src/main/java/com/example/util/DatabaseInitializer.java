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

    private static final String CREATE_TABLE_CATEGORIES = """
            CREATE TABLE IF NOT EXISTS categories (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    UNIQUE NOT NULL COLLATE NOCASE,
                description TEXT,
                created_at  TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

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
                renew_count INTEGER          DEFAULT 0,
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

    private static final String CREATE_TABLE_SYSTEM_SETTINGS = """
            CREATE TABLE IF NOT EXISTS system_settings (
                key   TEXT PRIMARY KEY,
                value TEXT
            )
            """;

    private static final String CREATE_TABLE_EMAIL_LOGS = """
            CREATE TABLE IF NOT EXISTS email_logs (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                borrow_id       INTEGER,
                reader_id       INTEGER,
                recipient_email TEXT,
                subject         TEXT,
                content         TEXT,
                status          TEXT    NOT NULL DEFAULT 'SUCCESS',
                error_message   TEXT,
                sent_at         TEXT    DEFAULT (datetime('now','localtime')),
                FOREIGN KEY (borrow_id) REFERENCES borrows(id),
                FOREIGN KEY (reader_id) REFERENCES readers(id)
            )
            """;

    private static final String CREATE_TABLE_AUTHORS = """
            CREATE TABLE IF NOT EXISTS authors (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    UNIQUE NOT NULL COLLATE NOCASE,
                birth_year  INTEGER,
                death_year  INTEGER,
                nationality TEXT,
                biography   TEXT,
                created_at  TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String CREATE_TABLE_PUBLISHERS = """
            CREATE TABLE IF NOT EXISTS publishers (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                name           TEXT    UNIQUE NOT NULL COLLATE NOCASE,
                address        TEXT,
                phone          TEXT,
                email          TEXT,
                website        TEXT,
                representative TEXT,
                description    TEXT,
                created_at     TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String CREATE_TABLE_LOGIN_LOGS = """
            CREATE TABLE IF NOT EXISTS login_logs (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER,
                username   TEXT    NOT NULL,
                full_name  TEXT,
                role       TEXT,
                action     TEXT    NOT NULL,
                logged_at  TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String CREATE_TABLE_OTP_VERIFICATIONS = """
            CREATE TABLE IF NOT EXISTS otp_verifications (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                email       TEXT    NOT NULL,
                otp_code    TEXT    NOT NULL,
                type        TEXT    NOT NULL DEFAULT 'REGISTER',
                payload     TEXT,
                expires_at  TEXT    NOT NULL,
                attempts    INTEGER NOT NULL DEFAULT 0,
                is_verified INTEGER NOT NULL DEFAULT 0,
                created_at  TEXT    DEFAULT (datetime('now','localtime'))
            )
            """;

    // ============================================================
    //  Index để tăng tốc truy vấn
    // ============================================================

    private static final String[] CREATE_INDEXES = {
        "CREATE INDEX IF NOT EXISTS idx_books_title          ON books(title)",
        "CREATE INDEX IF NOT EXISTS idx_books_author         ON books(author)",
        "CREATE INDEX IF NOT EXISTS idx_books_category       ON books(category)",
        "CREATE INDEX IF NOT EXISTS idx_books_publisher      ON books(publisher)",
        "CREATE INDEX IF NOT EXISTS idx_categories_name      ON categories(name)",
        "CREATE INDEX IF NOT EXISTS idx_authors_name         ON authors(name)",
        "CREATE INDEX IF NOT EXISTS idx_publishers_name      ON publishers(name)",
        "CREATE INDEX IF NOT EXISTS idx_borrows_book         ON borrows(book_id)",
        "CREATE INDEX IF NOT EXISTS idx_borrows_reader       ON borrows(reader_id)",
        "CREATE INDEX IF NOT EXISTS idx_borrows_status       ON borrows(status)",
        "CREATE INDEX IF NOT EXISTS idx_login_logs_logged_at ON login_logs(logged_at)",
        "CREATE INDEX IF NOT EXISTS idx_login_logs_action    ON login_logs(action)",
        "CREATE INDEX IF NOT EXISTS idx_otp_email_type       ON otp_verifications(email, type)",
        "CREATE INDEX IF NOT EXISTS idx_users_email          ON users(email)",
        "CREATE INDEX IF NOT EXISTS idx_users_reader_id      ON users(reader_id)"
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

    /** Chèn tài khoản độc giả mặc định (username: docgia / password: docgia123, liên kết thẻ NDG-0001). */
    private static final String INSERT_DEFAULT_READER = """
            INSERT OR IGNORE INTO users (username, password, full_name, role, active, email, reader_id)
            VALUES ('docgia', 'docgia123', 'Nguyễn Thị Hoa', 'READER', 1, 'hoa.nguyen@email.com', 1)
            """;

    /** Cấu hình SMTP mặc định cho Gmail. */
    private static final String[] INSERT_DEFAULT_SMTP = {
        "INSERT OR IGNORE INTO system_settings (key, value) VALUES ('smtp.host', 'smtp.gmail.com')",
        "INSERT OR IGNORE INTO system_settings (key, value) VALUES ('smtp.port', '587')",
        "INSERT OR IGNORE INTO system_settings (key, value) VALUES ('smtp.username', 'kle45313@gmail.com')",
        "INSERT OR IGNORE INTO system_settings (key, value) VALUES ('smtp.password', 'aozq dvjs yfpu kvrt')",
        "INSERT OR IGNORE INTO system_settings (key, value) VALUES ('smtp.from_name', 'Thư Viện Nguyễn Huệ')",
        "INSERT OR IGNORE INTO system_settings (key, value) VALUES ('smtp.tls', 'true')",
    };

    // Dữ liệu mẫu thể loại
    private static final String[] INSERT_DEFAULT_CATEGORIES = {
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Thiếu nhi', 'Sách dành cho thiếu nhi, truyện tranh, đồng thoại')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Văn học', 'Tiểu thuyết, truyện ngắn, thơ ca, văn học trong và ngoài nước')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Công nghệ', 'Khoa học máy tính, lập trình, công nghệ thông tin')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Triết học', 'Tư tưởng triết học, đạo đức học, nhân sinh quan')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Kỹ năng sống', 'Phát triển bản thân, kỹ năng mềm, tư duy thành công')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Khoa học', 'Khoa học tự nhiên, vũ trụ, sinh học, vật lý')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Lịch sử', 'Lịch sử Việt Nam và thế giới, tư liệu lịch sử')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Kinh tế', 'Kinh doanh, tài chính, quản trị, khởi nghiệp')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Ngoại ngữ', 'Giáo trình, từ điển, sách học ngoại ngữ')",
        "INSERT OR IGNORE INTO categories (name, description) VALUES ('Nghệ thuật', 'Hội họa, âm nhạc, nhiếp ảnh, kiến trúc')"
    };

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

    /** Alias cho initialize(). */
    public static void initDatabase() {
        initialize();
    }

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
            DatabaseConnection.syncMirrors();

        } catch (SQLException e) {
            System.err.println("[DB] Lỗi khởi tạo DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Tạo tất cả bảng. */
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_CATEGORIES);
            stmt.execute(CREATE_TABLE_BOOKS);
            stmt.execute(CREATE_TABLE_READERS);
            stmt.execute(CREATE_TABLE_BORROWS);
            stmt.execute(CREATE_TABLE_USERS);
            stmt.execute(CREATE_TABLE_SYSTEM_SETTINGS);
            stmt.execute(CREATE_TABLE_EMAIL_LOGS);
            stmt.execute(CREATE_TABLE_AUTHORS);
            stmt.execute(CREATE_TABLE_PUBLISHERS);
            stmt.execute(CREATE_TABLE_LOGIN_LOGS);
            stmt.execute(CREATE_TABLE_OTP_VERIFICATIONS);

            // Migration: thêm cột renew_count nếu DB đã tồn tại từ phiên bản trước
            try {
                stmt.execute("ALTER TABLE borrows ADD COLUMN renew_count INTEGER DEFAULT 0");
            } catch (SQLException ignored) {
                // Cột đã tồn tại
            }

            // Migration: thêm cột email vào bảng users (cho OTP và liên hệ)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT");
            } catch (SQLException ignored) {
                // Cột đã tồn tại
            }

            // Migration: thêm cột reader_id vào bảng users (liên kết với bảng readers)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN reader_id INTEGER DEFAULT 0");
            } catch (SQLException ignored) {
                // Cột đã tồn tại
            }

            // Migration: đồng bộ các thể loại hiện có trong bảng books vào bảng categories nếu chưa có
            try {
                stmt.execute("INSERT OR IGNORE INTO categories (name) SELECT DISTINCT TRIM(category) FROM books WHERE category IS NOT NULL AND TRIM(category) != ''");
            } catch (SQLException ignored) {
            }

            // Migration: đồng bộ các tác giả hiện có trong bảng books vào bảng authors nếu chưa có
            try {
                stmt.execute("INSERT OR IGNORE INTO authors (name) SELECT DISTINCT TRIM(author) FROM books WHERE author IS NOT NULL AND TRIM(author) != ''");
            } catch (SQLException ignored) {
            }

            // Migration: Chuẩn hóa tên NXB trong bảng books về dạng đầy đủ có tiền tố NXB
            try {
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Kim Đồng' WHERE publisher LIKE '%Kim Đồng' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Văn Học' WHERE publisher LIKE '%Văn Học' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Thông Tin' WHERE publisher LIKE '%Thông Tin' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Hội Nhà Văn' WHERE publisher LIKE '%Hội Nhà Văn' AND publisher NOT LIKE 'NXB%'");
                stmt.executeUpdate("UPDATE books SET publisher = 'NXB Tổng Hợp' WHERE publisher LIKE '%Tổng Hợp' AND publisher NOT LIKE 'NXB%'");
            } catch (SQLException ignored) {
            }

            // Migration: Loại bỏ NXB viết tắt nếu đã có NXB chuẩn hóa
            try {
                stmt.executeUpdate("DELETE FROM publishers WHERE (name LIKE '%Kim Đồng' OR name LIKE '%Kim Dong') AND name NOT LIKE 'NXB%'");
            } catch (SQLException ignored) {
            }

            // Migration: đồng bộ các nhà xuất bản hiện có trong bảng books vào bảng publishers nếu chưa có
            try {
                stmt.execute("INSERT OR IGNORE INTO publishers (name) SELECT DISTINCT TRIM(publisher) FROM books WHERE publisher IS NOT NULL AND TRIM(publisher) != ''");
            } catch (SQLException ignored) {
            }

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

    /** Dữ liệu mẫu tác giả */
    private static final String[] INSERT_DEFAULT_AUTHORS = {
        "INSERT OR IGNORE INTO authors (name, birth_year, death_year, nationality, biography) VALUES ('Tô Hoài', 1920, 2014, 'Việt Nam', 'Nhà văn lớn của nền văn học hiện đại Việt Nam, tác giả Dế Mèn Phiêu Lưu Ký')",
        "INSERT OR IGNORE INTO authors (name, birth_year, death_year, nationality, biography) VALUES ('Vũ Trọng Phụng', 1912, 1939, 'Việt Nam', 'Nhà văn, nhà báo trào phúng xuất sắc của văn học Việt Nam')",
        "INSERT OR IGNORE INTO authors (name, birth_year, death_year, nationality, biography) VALUES ('Paulo Coelho', 1947, NULL, 'Brazil', 'Tiểu thuyết gia nổi tiếng người Brazil, tác giả cuốn Nhà Giả Kim')",
        "INSERT OR IGNORE INTO authors (name, birth_year, death_year, nationality, biography) VALUES ('Dale Carnegie', 1888, 1955, 'Mỹ', 'Nhà văn và nhà thuyết trình người Mỹ, tác giả cuốn Đắc Nhân Tâm')",
        "INSERT OR IGNORE INTO authors (name, birth_year, death_year, nationality, biography) VALUES ('Nguyễn Văn An', 1980, NULL, 'Việt Nam', 'Chuyên gia công nghệ thông tin và tác giả nhiều đầu sách lập trình')"
    };

    /** Dữ liệu mẫu nhà xuất bản */
    private static final String[] INSERT_DEFAULT_PUBLISHERS = {
        "INSERT OR IGNORE INTO publishers (name, address, phone, email, website, representative, description) VALUES ('NXB Kim Đồng', '55 Quang Trung, Hai Bà Trưng, Hà Nội', '1900571595', 'cskh@nxbkimdong.com.vn', 'https://nxbkimdong.com.vn', 'Bùi Tuấn Nghĩa', 'Nhà xuất bản chuyên xuất bản sách cho thiếu nhi và thanh thiếu niên')",
        "INSERT OR IGNORE INTO publishers (name, address, phone, email, website, representative, description) VALUES ('NXB Văn Học', '18 Nguyễn Trường Tộ, Ba Đình, Hà Nội', '02437161518', 'nxbvanhoc@gmail.com', 'http://nxbvanhoc.com.vn', 'Nguyễn Anh Vũ', 'Nhà xuất bản văn học nghệ thuật lâu đời của Việt Nam')",
        "INSERT OR IGNORE INTO publishers (name, address, phone, email, website, representative, description) VALUES ('NXB Thông Tin', '115 Trần Duy Hưng, Cầu Giấy, Hà Nội', '02435565928', 'nxb.tttt@mic.gov.vn', 'https://nxbthongtintruyenthong.vn', 'Trần Chí Đạt', 'Nhà xuất bản Thông tin và Truyền thông')",
        "INSERT OR IGNORE INTO publishers (name, address, phone, email, website, representative, description) VALUES ('NXB Hội Nhà Văn', '65 Nguyễn Du, Hai Bà Trưng, Hà Nội', '02438222135', 'nxbhoinhavan@gmail.com', 'http://nxbhoinhavan.vn', 'Trần Đăng Khoa', 'Đơn vị xuất bản trực thuộc Hội Nhà văn Việt Nam')",
        "INSERT OR IGNORE INTO publishers (name, address, phone, email, website, representative, description) VALUES ('NXB Tổng Hợp', '62 Nguyễn Thị Minh Khai, Đa Kao, Quận 1, TP.HCM', '02838225340', 'tonghop@nxbhcm.com.vn', 'https://nxbhcm.com.vn', 'Đinh Thị Thanh Thủy', 'Nhà xuất bản Tổng hợp Thành phố Hồ Chí Minh')"
    };

    /** Cập nhật thông tin chi tiết cho các nhà xuất bản mặc định nếu chưa có */
    private static final String[] UPDATE_DEFAULT_PUBLISHERS_INFO = {
        "UPDATE publishers SET address = '55 Quang Trung, Hai Bà Trưng, Hà Nội', phone = '1900571595', email = 'cskh@nxbkimdong.com.vn', website = 'https://nxbkimdong.com.vn', representative = 'Bùi Tuấn Nghĩa', description = 'Nhà xuất bản chuyên xuất bản sách cho thiếu nhi và thanh thiếu niên' WHERE name = 'NXB Kim Đồng' AND (address IS NULL OR address = '')",
        "UPDATE publishers SET address = '18 Nguyễn Trường Tộ, Ba Đình, Hà Nội', phone = '02437161518', email = 'nxbvanhoc@gmail.com', website = 'http://nxbvanhoc.com.vn', representative = 'Nguyễn Anh Vũ', description = 'Nhà xuất bản văn học nghệ thuật lâu đời của Việt Nam' WHERE name = 'NXB Văn Học' AND (address IS NULL OR address = '')",
        "UPDATE publishers SET address = '115 Trần Duy Hưng, Cầu Giấy, Hà Nội', phone = '02435565928', email = 'nxb.tttt@mic.gov.vn', website = 'https://nxbthongtintruyenthong.vn', representative = 'Trần Chí Đạt', description = 'Nhà xuất bản Thông tin và Truyền thông' WHERE name = 'NXB Thông Tin' AND (address IS NULL OR address = '')",
        "UPDATE publishers SET address = '65 Nguyễn Du, Hai Bà Trưng, Hà Nội', phone = '02438222135', email = 'nxbhoinhavan@gmail.com', website = 'http://nxbhoinhavan.vn', representative = 'Trần Đăng Khoa', description = 'Đơn vị xuất bản trực thuộc Hội Nhà văn Việt Nam' WHERE name = 'NXB Hội Nhà Văn' AND (address IS NULL OR address = '')",
        "UPDATE publishers SET address = '62 Nguyễn Thị Minh Khai, Đa Kao, Quận 1, TP.HCM', phone = '02838225340', email = 'tonghop@nxbhcm.com.vn', website = 'https://nxbhcm.com.vn', representative = 'Đinh Thị Thanh Thủy', description = 'Nhà xuất bản Tổng hợp Thành phố Hồ Chí Minh' WHERE name = 'NXB Tổng Hợp' AND (address IS NULL OR address = '')"
    };

    /** Chèn dữ liệu người dùng, thể loại, tác giả, nhà xuất bản và SMTP mặc định. */
    private static void insertDefaultData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(INSERT_DEFAULT_ADMIN);
            stmt.execute(INSERT_DEFAULT_LIBRARIAN);
            stmt.execute(INSERT_DEFAULT_READER);
            for (String sql : INSERT_DEFAULT_CATEGORIES) {
                stmt.execute(sql);
            }
            for (String sql : INSERT_DEFAULT_AUTHORS) {
                stmt.execute(sql);
            }
            for (String sql : INSERT_DEFAULT_PUBLISHERS) {
                stmt.execute(sql);
            }
            for (String sql : UPDATE_DEFAULT_PUBLISHERS_INFO) {
                stmt.execute(sql);
            }
            for (String sql : INSERT_DEFAULT_SMTP) {
                stmt.execute(sql);
            }

            // Chèn dữ liệu mẫu lịch sử đăng nhập nếu bảng còn trống
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM login_logs")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String[] sampleLogs = {
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGIN', datetime('now','localtime','-6 days','+08:30:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGOUT', datetime('now','localtime','-6 days','+17:15:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGIN', datetime('now','localtime','-5 days','+08:00:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGOUT', datetime('now','localtime','-5 days','+17:00:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGIN', datetime('now','localtime','-4 days','+09:10:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGIN', datetime('now','localtime','-4 days','+08:05:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGOUT', datetime('now','localtime','-4 days','+16:45:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGIN', datetime('now','localtime','-3 days','+08:15:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGIN', datetime('now','localtime','-3 days','+08:30:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGOUT', datetime('now','localtime','-3 days','+17:30:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGIN', datetime('now','localtime','-2 days','+08:00:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGIN', datetime('now','localtime','-2 days','+10:00:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGOUT', datetime('now','localtime','-2 days','+17:00:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (2, 'thuthu', 'Nguyễn Thị Thu', 'LIBRARIAN', 'LOGIN', datetime('now','localtime','-1 days','+08:10:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGIN', datetime('now','localtime','-1 days','+13:30:00'))",
                        "INSERT INTO login_logs (user_id, username, full_name, role, action, logged_at) VALUES (1, 'admin', 'Quản trị viên', 'ADMIN', 'LOGIN', datetime('now','localtime','-2 hours'))"
                    };
                    for (String sql : sampleLogs) {
                        stmt.execute(sql);
                    }
                }
            } catch (SQLException ignored) {}

            System.out.println("[DB] Dữ liệu mặc định đã được tạo.");
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
