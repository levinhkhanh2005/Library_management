package com.example;

import com.example.model.User;
import com.example.service.AuthService;
import com.example.util.DatabaseInitializer;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppTest {
    @Test
    public void testChangePassword() throws Exception {
        DatabaseInitializer.initDatabase();
        AuthService authService = new AuthService();
        User user = authService.login("admin", "admin123");
        assertNotNull(user);
        
        authService.changePassword(user.getId(), "admin123", "admin1234");
        
        User updatedUser = authService.login("admin", "admin1234");
        assertNotNull(updatedUser);
        
        authService.changePassword(updatedUser.getId(), "admin1234", "admin123");
    }
}
