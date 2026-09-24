package com.example.dao;

import com.example.model.FineTransaction;
import com.example.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FineTransactionDAO {
    private Connection conn() throws SQLException { return DatabaseConnection.getInstance().getConnection(); }

    public int insert(int borrowId, int readerId, double amount, String type, String reason,
                      int userId, String userName, String receiptNo) throws SQLException {
        String sql = "INSERT INTO fine_transactions(borrow_id,reader_id,amount,type,reason,user_id,user_name,receipt_no) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, borrowId); ps.setInt(2, readerId); ps.setDouble(3, amount); ps.setString(4, type);
            ps.setString(5, reason); ps.setInt(6, userId); ps.setString(7, userName); ps.setString(8, receiptNo);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public List<FineTransaction> findByBorrow(int borrowId) throws SQLException {
        String sql = "SELECT * FROM fine_transactions WHERE borrow_id=? ORDER BY id DESC";
        List<FineTransaction> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, borrowId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new FineTransaction(rs.getInt("id"), rs.getInt("borrow_id"),
                    rs.getInt("reader_id"), rs.getDouble("amount"), rs.getString("type"), rs.getString("reason"),
                    rs.getInt("user_id"), rs.getString("user_name"), rs.getString("receipt_no"), rs.getString("created_at")));
            }
        }
        return result;
    }
}
