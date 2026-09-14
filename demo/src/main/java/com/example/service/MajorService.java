package com.example.service;

import com.example.dao.MajorDAO;
import com.example.model.Major;
import java.sql.SQLException;
import java.util.List;

public class MajorService {
    private final MajorDAO dao = new MajorDAO();

    public Major addMajor(String name, String description) throws SQLException {
        validate(name);
        name = name.trim();
        if (dao.findByName(name) != null) throw new IllegalArgumentException("Khối ngành \"" + name + "\" đã tồn tại.");
        Major m = new Major(0, name, description == null ? "" : description.trim(), null);
        int id = dao.insert(m); if (id < 0) throw new SQLException("Không thể thêm khối ngành.");
        m.setId(id); return m;
    }

    public void updateMajor(Major m) throws SQLException {
        validate(m.getName());
        m.setName(m.getName().trim());
        m.setDescription(m.getDescription() == null ? "" : m.getDescription().trim());
        Major other = dao.findByName(m.getName());
        if (other != null && other.getId() != m.getId()) throw new IllegalArgumentException("Khối ngành \"" + m.getName() + "\" đã tồn tại.");
        if (!dao.update(m)) throw new SQLException("Không thể cập nhật khối ngành.");
    }

    public void deleteMajor(int id) throws SQLException {
        int count = dao.countCategoriesUsingMajor(id);
        if (count > 0) throw new IllegalStateException("Không thể xóa khối ngành vì đang có " + count + " thể loại sử dụng khối ngành này.");
        if (!dao.delete(id)) throw new SQLException("Không thể xóa khối ngành.");
    }

    public List<Major> getAllMajors() throws SQLException { return dao.findAll(); }
    public Major getMajorById(int id) throws SQLException { return dao.findById(id); }

    private void validate(String name) {
        if (name == null || name.trim().isBlank()) throw new IllegalArgumentException("Tên khối ngành không được để trống.");
    }
}
