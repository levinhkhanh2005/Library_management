package com.example.dao;

import com.example.model.InventoryLog;
import com.example.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryLogDAO {
    private Connection conn() throws SQLException { return DatabaseConnection.getInstance().getConnection(); }

    public void insert(int bookId, String action, int quantity, String reason,
                       int userId, String userName) throws SQLException {
        String sql = "INSERT INTO inventory_logs(book_id,action,quantity,reason,user_id,user_name) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, bookId); ps.setString(2, action); ps.setInt(3, quantity);
            ps.setString(4, reason); ps.setInt(5, userId); ps.setString(6, userName);
            ps.executeUpdate();
        }
    }

    public List<InventoryLog> findByBook(int bookId) throws SQLException {
        String sql = "SELECT * FROM inventory_logs WHERE book_id=? ORDER BY id DESC";
        List<InventoryLog> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new InventoryLog(rs.getInt("id"), rs.getInt("book_id"),
                    rs.getString("action"), rs.getInt("quantity"), rs.getString("reason"),
                    rs.getInt("user_id"), rs.getString("user_name"), rs.getString("created_at")));
            }
        }
        return result;
    }
}
