package com.example;

import com.example.dao.ReaderDAO;
import com.example.dao.UserDAO;
import com.example.model.Reader;
import com.example.model.User;
import com.example.service.ReaderService;
import com.example.util.DatabaseInitializer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReaderAccountManagementTest {

    private ReaderService readerService;
    private UserDAO userDAO;
    private ReaderDAO readerDAO;

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        readerService = new ReaderService();
        userDAO = new UserDAO();
        readerDAO = new ReaderDAO();
    }

    @Test
    public void testCreateAndResetReaderAccount() throws Exception {
        // 1. Tạo một độc giả mới
        Reader reader = readerService.addReader(
            "Nguyễn Văn Test Portal", "01/01/2000", "0912345678",
            "testportal@example.com", "Hà Nội"
        );
        assertNotNull(reader);
        assertTrue(reader.getId() > 0);

        // Độc giả mới chưa có tài khoản
        User initialUser = readerService.getUserForReader(reader.getId());
        assertNull(initialUser);

        // 2. Cấp tài khoản mới cho độc giả
        String username = "testportal_" + reader.getId();
        User createdUser = readerService.createOrResetReaderAccount(reader.getId(), username, "password123");
        assertNotNull(createdUser);
        assertEquals(username, createdUser.getUsername());
        assertEquals(User.Role.READER, createdUser.getRole());
        assertEquals(reader.getId(), createdUser.getReaderId());
        assertTrue(createdUser.isActive());

        // 3. Đổi / Đặt lại mật khẩu
        User updatedUser = readerService.createOrResetReaderAccount(reader.getId(), username, "newpass456");
        assertNotNull(updatedUser);

        // Thử xác thực với mật khẩu mới
        User authenticated = userDAO.authenticate(username, "newpass456");
        assertNotNull(authenticated);
        assertEquals(createdUser.getId(), authenticated.getId());

        // Thử xác thực với mật khẩu cũ -> phải thất bại
        User oldAuth = userDAO.authenticate(username, "password123");
        assertNull(oldAuth);

        // Dọn dẹp
        userDAO.delete(createdUser.getId());
        readerDAO.delete(reader.getId());
    }

    @Test
    public void testLockReaderSyncsWithUserAccount() throws Exception {
        // 1. Tạo độc giả và cấp tài khoản
        Reader reader = readerService.addReader(
            "Lê Khóa Test", "15/05/1999", "0987654321",
            "lelock@example.com", "Đà Nẵng"
        );
        String username = "lelock_" + reader.getId();
        User user = readerService.createOrResetReaderAccount(reader.getId(), username, "password123");
        assertTrue(user.isActive());

        // Đăng nhập được khi ACTIVE
        assertNotNull(userDAO.authenticate(username, "password123"));

        // 2. Khóa độc giả -> tự động đồng bộ khóa user account
        readerService.setReaderStatus(reader.getId(), Reader.Status.LOCKED);

        // Kiểm tra độc giả bị LOCKED
        Reader lockedReader = readerService.getReaderById(reader.getId());
        assertEquals(Reader.Status.LOCKED, lockedReader.getStatus());

        // Kiểm tra User active = false
        User lockedUser = userDAO.findById(user.getId());
        assertFalse(lockedUser.isActive());

        // Không thể đăng nhập khi bị khóa
        assertNull(userDAO.authenticate(username, "password123"));

        // 3. Mở khóa độc giả -> tự động đồng bộ mở user account
        readerService.setReaderStatus(reader.getId(), Reader.Status.ACTIVE);

        Reader activeReader = readerService.getReaderById(reader.getId());
        assertEquals(Reader.Status.ACTIVE, activeReader.getStatus());

        User activeUser = userDAO.findById(user.getId());
        assertTrue(activeUser.isActive());

        // Đăng nhập lại thành công
        assertNotNull(userDAO.authenticate(username, "password123"));

        // Dọn dẹp
        userDAO.delete(user.getId());
        readerDAO.delete(reader.getId());
    }

    @Test
    public void testReaderStatistics() throws Exception {
        int total = readerService.getTotalReaders();
        int active = readerService.getActiveReaders();
        int locked = readerService.getLockedReaders();
        int portalCount = readerService.getPortalAccountCount();

        assertTrue(total >= 0);
        assertTrue(active >= 0);
        assertTrue(locked >= 0);
        assertTrue(portalCount >= 0);
        assertTrue(active + locked <= total);
    }
}
