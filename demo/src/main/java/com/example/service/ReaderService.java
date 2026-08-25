package com.example.service;

import com.example.dao.ReaderDAO;
import com.example.model.Reader;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service quản lý nghiệp vụ độc giả.
 */
public class ReaderService {

    private final ReaderDAO readerDAO = new ReaderDAO();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ===================== Thêm độc giả =====================

    /**
     * Đăng ký độc giả mới. Mã thẻ được tự sinh theo format NDG-XXXX.
     */
    public Reader addReader(String fullName, String birthDate, String phone,
                            String email, String address) throws SQLException {

        validateRequired(fullName, "Họ tên");
        validatePhone(phone);
        validateBirthDate(birthDate);

        // Tự sinh mã thẻ: NDG-0001, NDG-0002, ...
        int maxNum    = readerDAO.getMaxReaderCodeNumber();
        String code   = String.format("NDG-%04d", maxNum + 1);
        String today  = LocalDate.now().format(DATE_FMT);

        Reader reader = new Reader(
            code,
            fullName.trim(),
            birthDate == null ? "" : birthDate.trim(),
            phone    == null ? "" : phone.trim(),
            email    == null ? "" : email.trim(),
            address  == null ? "" : address.trim(),
            today
        );

        int id = readerDAO.insert(reader);
        if (id == -1) throw new SQLException("Thêm độc giả thất bại.");
        reader.setId(id);
        return reader;
    }

    // ===================== Cập nhật độc giả =====================

    public void updateReader(Reader reader) throws SQLException {
        validateRequired(reader.getFullName(), "Họ tên");
        validatePhone(reader.getPhone());

        if (!readerDAO.update(reader)) {
            throw new SQLException("Cập nhật độc giả thất bại. Độc giả có thể không tồn tại.");
        }
    }

    /** Thay đổi trạng thái tài khoản (khóa / mở khóa). */
    public void setReaderStatus(int readerId, Reader.Status status) throws SQLException {
        if (!readerDAO.updateStatus(readerId, status)) {
            throw new SQLException("Cập nhật trạng thái thất bại.");
        }
    }

    // ===================== Xóa độc giả =====================

    /**
     * Xóa độc giả. Không cho xóa nếu có lịch sử mượn.
     * Trong thực tế nên soft-delete (khóa tài khoản).
     */
    public void deleteReader(Reader reader) throws SQLException {
        if (!readerDAO.delete(reader.getId())) {
            throw new SQLException(
                "Không thể xóa độc giả \"" + reader.getFullName() + "\".\n" +
                "Có thể họ vẫn còn phiếu mượn trong hệ thống."
            );
        }
    }

    // ===================== Truy vấn =====================

    public List<Reader> getAllReaders() throws SQLException {
        return readerDAO.findAll();
    }

    public Reader getReaderById(int id) throws SQLException {
        return readerDAO.findById(id);
    }

    public Reader getReaderByCode(String code) throws SQLException {
        return readerDAO.findByCode(code);
    }

    public List<Reader> searchReaders(String keyword) throws SQLException {
        return readerDAO.search(keyword);
    }

    public int getTotalReaders() throws SQLException {
        return readerDAO.countAll();
    }

    public int getActiveReaders() throws SQLException {
        return readerDAO.countActive();
    }

    // ===================== Validation =====================

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) return; // không bắt buộc
        String digits = phone.replaceAll("[\\s\\-]", "");
        if (!digits.matches("0[0-9]{9,10}")) {
            throw new IllegalArgumentException(
                "Số điện thoại không hợp lệ (phải bắt đầu bằng 0, gồm 10–11 chữ số)."
            );
        }
    }

    private void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) return; // không bắt buộc
        try {
            LocalDate dob  = LocalDate.parse(birthDate.trim(), DATE_FMT);
            LocalDate now  = LocalDate.now();
            if (dob.isAfter(now)) {
                throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
            }
            if (dob.isBefore(now.minusYears(120))) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ (quá 120 năm).");
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không đúng định dạng dd/MM/yyyy.");
        }
    }
}
