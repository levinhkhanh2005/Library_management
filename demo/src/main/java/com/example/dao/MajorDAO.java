package com.example.dao;

import com.example.model.Major;
import com.example.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO cho bảng majors. */
public class MajorDAO {
    private Connection getConn() throws SQLException { return DatabaseConnection.getInstance().getConnection(); }

    public int insert(Major major) throws SQLException {
        String sql = "INSERT INTO majors (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, major.getName());
            ps.setString(2, major.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public boolean update(Major major) throws SQLException {
        String sql = "UPDATE majors SET name=?, description=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, major.getName());
            ps.setString(2, major.getDescription());
            ps.setInt(3, major.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM majors WHERE id=?")) {
            ps.setInt(1, id); return ps.executeUpdate() > 0;
        }
    }

    public List<Major> findAll() throws SQLException {
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM majors ORDER BY name COLLATE NOCASE")) {
            return mapList(rs);
        }
    }

    public Major findById(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM majors WHERE id=?")) {
            ps.setInt(1, id); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        }
    }

    public Major findByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return null;
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM majors WHERE LOWER(TRIM(name))=LOWER(TRIM(?))")) {
            ps.setString(1, name); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        }
    }

    public int countCategoriesUsingMajor(int majorId) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT COUNT(*) FROM categories WHERE major_id=?")) {
            ps.setInt(1, majorId); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public int countAll() throws SQLException {
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM majors")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private List<Major> mapList(ResultSet rs) throws SQLException {
        List<Major> list = new ArrayList<>(); while (rs.next()) list.add(mapRow(rs)); return list;
    }
    private Major mapRow(ResultSet rs) throws SQLException {
        return new Major(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getString("created_at"));
    }
}
