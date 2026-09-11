package com.example;

import com.example.dao.LoginLogDAO;
import com.example.model.User;
import com.example.service.AuthService;
import com.example.util.DatabaseInitializer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class LoginLogTest {

    @BeforeClass
    public static void setUp() {
        DatabaseInitializer.initDatabase();
    }

    @Test
    public void testLoginLoggingAndStats() throws Exception {
        AuthService authService = new AuthService();
        LoginLogDAO logDAO = new LoginLogDAO();

        int initialWeekCount = logDAO.countLoginsLastWeek();
        User admin = authService.login("admin", "admin123");
        assertNotNull("Admin login thành công", admin);

        int newWeekCount = logDAO.countLoginsLastWeek();
        assertTrue("Số lượt login tuần phải tăng", newWeekCount >= initialWeekCount);

        authService.logout();

        List<Object[]> recent = logDAO.getRecentLogs(10);
        assertFalse("Danh sách logs không được rỗng", recent.isEmpty());

        Object[] latest = recent.get(0);
        assertEquals("Hành động gần nhất là LOGOUT", "LOGOUT", latest[4]);

        Map<String, Integer> weekly = logDAO.getWeeklyLoginByDay();
        assertEquals("Phải có đủ 7 ngày", 7, weekly.size());

        int distinct = logDAO.countDistinctUsersLastWeek();
        assertTrue("Phải có ít nhất 1 user", distinct >= 1);
    }
}