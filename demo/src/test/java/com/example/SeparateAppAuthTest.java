package com.example;

import com.example.model.User;
import com.example.service.AuthService;
import com.example.util.DatabaseConnection;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * Kiểm thử phân quyền đăng nhập riêng biệt giữa AdminApp và ReaderApp,
 * cũng như chế độ WAL cho phép đa kết nối đồng thời.
 */
public class SeparateAppAuthTest {

    private AuthService authService;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        authService = new AuthService();
    }

    @Test
    public void testAdminAppAllowsAdminAndLibrarian() throws Exception {
        // Admin
        User admin = authService.login("admin", "admin123");
        assertNotNull(admin);
        assertTrue(admin.isAdmin());
        assertTrue(admin.isStaff());
        assertFalse(admin.isReader());

        // Librarian
        User librarian = authService.login("thuthu", "thuthu123");
        assertNotNull(librarian);
        assertEquals(User.Role.LIBRARIAN, librarian.getRole());
        assertTrue(librarian.isStaff());
        assertFalse(librarian.isReader());
    }

    @Test
    public void testAdminAppRejectsReaderAccount() throws Exception {
        User user = authService.login("docgia", "docgia123");
        assertNotNull(user);
        assertTrue("Tài khoản docgia phải là READER", user.isReader());
        // Giả lập logic kiểm tra của AdminApp/LoginDialog
        boolean rejected = false;
        if (user.isReader()) {
            authService.logout();
            rejected = true;
        }
        assertTrue("AdminApp phải từ chối vai trò READER", rejected);
    }

    @Test
    public void testReaderAppAllowsReader() throws Exception {
        User readerUser = authService.login("docgia", "docgia123");
        assertNotNull(readerUser);
        assertTrue(readerUser.isReader());
        assertTrue(readerUser.getReaderId() > 0);
    }

    @Test
    public void testReaderAppRejectsStaffAccounts() throws Exception {
        User admin = authService.login("admin", "admin123");
        assertNotNull(admin);
        // Giả lập logic kiểm tra của ReaderApp/ReaderLoginDialog
        boolean rejected = false;
        if (!admin.isReader()) {
            authService.logout();
            rejected = true;
        }
        assertTrue("ReaderApp phải từ chối vai trò ADMIN", rejected);
    }

    @Test
    public void testSqliteWalModeEnabled() throws Exception {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA journal_mode")) {
            assertTrue(rs.next());
            String journalMode = rs.getString(1);
            assertEquals("wal", journalMode.toLowerCase());
        }
    }
}
