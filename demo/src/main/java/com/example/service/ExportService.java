package com.example.service;

import com.example.dao.BookDAO;
import com.example.dao.BorrowDAO;
import com.example.dao.ReaderDAO;
import com.example.model.Book;
import com.example.model.Borrow;
import com.example.model.Reader;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xuất dữ liệu thư viện ra file Excel (.xlsx) và CSV.
 * Sử dụng Apache POI cho Excel và java.io thuần cho CSV.
 */
public class ExportService {

    private final BookDAO   bookDAO   = new BookDAO();
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final BorrowDAO borrowDAO = new BorrowDAO();

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");

    // ===================================================================
    //  Excel exports
    // ===================================================================

    /** Xuất danh sách sách ra Excel. */
    public File exportBooksToExcel(File file) throws Exception {
        List<Book> books = bookDAO.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Danh Sach Sach");

            String[] headers = {"#", "ISBN", "Ten Sach", "Tac Gia",
                                 "The Loai", "NXB", "Nam XB",
                                 "Tong Ban", "Con Lai", "Dang Muon"};
            createTitleRow(wb, sheet, "DANH SACH SACH", headers.length);
            createSubtitleRow(wb, sheet, "Xuat ngay: " + LocalDate.now().format(DATE_FMT), headers.length);
            createHeaderRow(wb, sheet, headers, 2);

            CellStyle dataStyle   = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle numStyle    = createNumberStyle(wb);

            int rowIdx = 3;
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20f);

                CellStyle rowStyle = (i % 2 == 0) ? dataStyle : createAltRowStyle(wb);

                setCell(row, 0, i + 1,               centerStyle);
                setCell(row, 1, orEmpty(b.getIsbn()), rowStyle);
                setCell(row, 2, orEmpty(b.getTitle()), rowStyle);
                setCell(row, 3, orEmpty(b.getAuthor()), rowStyle);
                setCell(row, 4, orEmpty(b.getCategory()), rowStyle);
                setCell(row, 5, orEmpty(b.getPublisher()), rowStyle);
                setCell(row, 6, b.getPublishYear() > 0 ? b.getPublishYear() : 0, centerStyle);
                setCell(row, 7, b.getTotalCopies(), numStyle);
                setCell(row, 8, b.getAvailableCopies(), numStyle);
                setCell(row, 9, b.getBorrowedCopies(), numStyle);
            }

            addSummaryRow(wb, sheet, rowIdx, "Tong so dau sach: " + books.size(), headers.length);

            autoSizeColumns(sheet, headers.length);
            sheet.setColumnWidth(2, 12000);

            wb.write(new FileOutputStream(file));
        }
        return file;
    }

    /** Xuất danh sách độc giả ra Excel. */
    public File exportReadersToExcel(File file) throws Exception {
        List<Reader> readers = readerDAO.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Danh Sach Doc Gia");

            String[] headers = {"#", "Ma The", "Ho Ten", "Ngay Sinh",
                                 "Dien Thoai", "Email", "Dia Chi",
                                 "Ngay Dang Ky", "Trang Thai"};
            createTitleRow(wb, sheet, "DANH SACH DOC GIA", headers.length);
            createSubtitleRow(wb, sheet, "Xuat ngay: " + LocalDate.now().format(DATE_FMT), headers.length);
            createHeaderRow(wb, sheet, headers, 2);

            CellStyle dataStyle   = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);

            int rowIdx = 3;
            long active = readers.stream()
                .filter(r -> r.getStatus() == Reader.Status.ACTIVE).count();

            for (int i = 0; i < readers.size(); i++) {
                Reader r = readers.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20f);

                CellStyle rowStyle = (i % 2 == 0) ? dataStyle : createAltRowStyle(wb);

                setCell(row, 0, i + 1, centerStyle);
                setCell(row, 1, orEmpty(r.getReaderCode()), rowStyle);
                setCell(row, 2, orEmpty(r.getFullName()), rowStyle);
                setCell(row, 3, orEmpty(r.getBirthDate()), centerStyle);
                setCell(row, 4, orEmpty(r.getPhone()), rowStyle);
                setCell(row, 5, orEmpty(r.getEmail()), rowStyle);
                setCell(row, 6, orEmpty(r.getAddress()), rowStyle);
                setCell(row, 7, orEmpty(r.getJoinDate()), centerStyle);
                setCell(row, 8, r.getStatus().getLabel(), centerStyle);
            }

            addSummaryRow(wb, sheet, rowIdx,
                "Tong: " + readers.size() + "  |  Hoat dong: " + active
                + "  |  Bi khoa/Het han: " + (readers.size() - active),
                headers.length);

            autoSizeColumns(sheet, headers.length);
            sheet.setColumnWidth(2, 10000);
            sheet.setColumnWidth(5, 10000);
            sheet.setColumnWidth(6, 12000);

            wb.write(new FileOutputStream(file));
        }
        return file;
    }

    /** Xuất danh sách phiếu mượn ra Excel. */
    public File exportBorrowsToExcel(File file) throws Exception {
        List<Borrow> borrows = borrowDAO.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Phieu Muon");

            String[] headers = {"#", "Ma Phieu", "Ten Sach", "ISBN",
                                 "Doc Gia", "Ma The",
                                 "Ngay Muon", "Han Tra", "Ngay Tra",
                                 "Trang Thai", "Tien Phat (VND)", "Ghi Chu"};
            createTitleRow(wb, sheet, "DANH SACH PHIEU MUON", headers.length);
            createSubtitleRow(wb, sheet, "Xuat ngay: " + LocalDate.now().format(DATE_FMT), headers.length);
            createHeaderRow(wb, sheet, headers, 2);

            CellStyle dataStyle   = createDataStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle numStyle    = createNumberStyle(wb);

            int rowIdx = 3;
            for (int i = 0; i < borrows.size(); i++) {
                Borrow b = borrows.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20f);

                CellStyle rowStyle = (i % 2 == 0) ? dataStyle : createAltRowStyle(wb);

                setCell(row,  0, i + 1,                    centerStyle);
                setCell(row,  1, b.getId(),                 centerStyle);
                setCell(row,  2, orEmpty(b.getBookTitle()), rowStyle);
                setCell(row,  3, orEmpty(b.getBookIsbn()),  rowStyle);
                setCell(row,  4, orEmpty(b.getReaderName()), rowStyle);
                setCell(row,  5, orEmpty(b.getReaderCode()), centerStyle);
                setCell(row,  6, orEmpty(b.getBorrowDate()), centerStyle);
                setCell(row,  7, orEmpty(b.getDueDate()),    centerStyle);
                setCell(row,  8, orEmpty(b.getReturnDate()), centerStyle);
                setCell(row,  9, b.getStatus().name(),       centerStyle);
                setCell(row, 10, b.getFineAmount(),          numStyle);
                setCell(row, 11, orEmpty(b.getNotes()),      rowStyle);
            }

            addSummaryRow(wb, sheet, rowIdx, "Tong phieu muon: " + borrows.size(), headers.length);

            autoSizeColumns(sheet, headers.length);
            sheet.setColumnWidth(2, 12000);
            sheet.setColumnWidth(4, 10000);

            wb.write(new FileOutputStream(file));
        }
        return file;
    }

    /** Xuất báo cáo thống kê theo tháng/năm ra Excel (3 sheet). */
    public File exportMonthlyReportToExcel(File file, int year) throws Exception {
        int[]          monthly  = borrowDAO.getBorrowCountByMonth(year);
        List<Borrow>   over     = borrowDAO.findOverdue();
        List<Object[]> topBooks = borrowDAO.getTopBorrowedBooks(10);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ---- Sheet 1: Thong ke theo thang ----
            {
                XSSFSheet sheet = wb.createSheet("Thong Ke " + year);
                String[] headers = {"Thang", "So Luot Muon"};
                createTitleRow(wb, sheet, "THONG KE LUOT MUON SACH NAM " + year, headers.length);
                createSubtitleRow(wb, sheet, "Xuat ngay: " + LocalDate.now().format(DATE_FMT), headers.length);
                createHeaderRow(wb, sheet, headers, 2);

                CellStyle dataStyle = createDataStyle(wb);
                CellStyle numStyle  = createNumberStyle(wb);
                String[] monthNames = {"Thang 1","Thang 2","Thang 3","Thang 4",
                                       "Thang 5","Thang 6","Thang 7","Thang 8",
                                       "Thang 9","Thang 10","Thang 11","Thang 12"};
                int total = 0;
                for (int i = 0; i < 12; i++) {
                    Row row = sheet.createRow(3 + i);
                    row.setHeightInPoints(20f);
                    CellStyle rowStyle = (i % 2 == 0) ? dataStyle : createAltRowStyle(wb);
                    setCell(row, 0, monthNames[i], rowStyle);
                    setCell(row, 1, monthly[i],    numStyle);
                    total += monthly[i];
                }

                Row totalRow = sheet.createRow(15);
                totalRow.setHeightInPoints(22f);
                CellStyle totStyle = createTotalRowStyle(wb);
                setCell(totalRow, 0, "Tong ca nam", totStyle);
                setCell(totalRow, 1, total,         totStyle);

                autoSizeColumns(sheet, headers.length);
            }

            // ---- Sheet 2: Top sach ----
            {
                XSSFSheet sheet = wb.createSheet("Top Sach");
                String[] headers = {"#", "Ten Sach", "So Luot Muon"};
                createTitleRow(wb, sheet, "TOP SACH DUOC MUON NHIEU NHAT", headers.length);
                createSubtitleRow(wb, sheet, "Xuat ngay: " + LocalDate.now().format(DATE_FMT), headers.length);
                createHeaderRow(wb, sheet, headers, 2);

                CellStyle dataStyle   = createDataStyle(wb);
                CellStyle centerStyle = createCenterStyle(wb);
                CellStyle numStyle    = createNumberStyle(wb);

                int rowIdx = 3;
                for (int i = 0; i < topBooks.size(); i++) {
                    Object[] r = topBooks.get(i);
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20f);
                    CellStyle rowStyle = (i % 2 == 0) ? dataStyle : createAltRowStyle(wb);
                    setCell(row, 0, i + 1,                     centerStyle);
                    setCell(row, 1, orEmpty((String) r[0]),    rowStyle);
                    setCell(row, 2, ((Number) r[1]).intValue(), numStyle);
                }

                autoSizeColumns(sheet, headers.length);
                sheet.setColumnWidth(1, 14000);
            }

            // ---- Sheet 3: Qua han ----
            {
                XSSFSheet sheet = wb.createSheet("Qua Han");
                String[] headers = {"#", "Ten Sach", "Doc Gia", "Ma The",
                                     "Ngay Muon", "Han Tra", "So Ngay Qua", "Tien Phat (VND)"};
                createTitleRow(wb, sheet, "DANH SACH PHIEU MUON QUA HAN", headers.length);
                createSubtitleRow(wb, sheet, "Cap nhat ngay: " + LocalDate.now().format(DATE_FMT), headers.length);
                createHeaderRow(wb, sheet, headers, 2);

                CellStyle dangerStyle    = createDangerStyle(wb, false);
                CellStyle dangerAltStyle = createDangerStyle(wb, true);
                CellStyle numStyle       = createNumberStyle(wb);

                int rowIdx = 3;
                for (int i = 0; i < over.size(); i++) {
                    Borrow b = over.get(i);
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20f);

                    CellStyle rowStyle = (i % 2 == 0) ? dangerStyle : dangerAltStyle;

                    long days = 0;
                    try {
                        LocalDate due = LocalDate.parse(b.getDueDate(),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        days = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
                    } catch (Exception ignored) {}

                    setCell(row, 0, i + 1,                     rowStyle);
                    setCell(row, 1, orEmpty(b.getBookTitle()),  rowStyle);
                    setCell(row, 2, orEmpty(b.getReaderName()), rowStyle);
                    setCell(row, 3, orEmpty(b.getReaderCode()), rowStyle);
                    setCell(row, 4, orEmpty(b.getBorrowDate()), rowStyle);
                    setCell(row, 5, orEmpty(b.getDueDate()),    rowStyle);
                    setCell(row, 6, (int) days,                 rowStyle);
                    setCell(row, 7, b.getFineAmount() > 0 ? b.getFineAmount() : days * 5000.0, numStyle);
                }

                addSummaryRow(wb, sheet, rowIdx, "Tong qua han: " + over.size() + " phieu", headers.length);
                autoSizeColumns(sheet, headers.length);
                sheet.setColumnWidth(1, 12000);
                sheet.setColumnWidth(2, 10000);
            }

            wb.write(new FileOutputStream(file));
        }
        return file;
    }

    // ===================================================================
    //  CSV exports
    // ===================================================================

    /** Xuất danh sách sách ra CSV. */
    public File exportBooksToCsv(File file) throws Exception {
        List<Book> books = bookDAO.findAll();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.print('\uFEFF'); // BOM UTF-8
            pw.println("STT,ISBN,Ten Sach,Tac Gia,The Loai,NXB,Nam XB,Tong Ban,Con Lai,Dang Muon");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                pw.println((i + 1) + ","
                    + csvEsc(b.getIsbn()) + ","
                    + csvEsc(b.getTitle()) + ","
                    + csvEsc(b.getAuthor()) + ","
                    + csvEsc(b.getCategory()) + ","
                    + csvEsc(b.getPublisher()) + ","
                    + b.getPublishYear() + ","
                    + b.getTotalCopies() + ","
                    + b.getAvailableCopies() + ","
                    + b.getBorrowedCopies());
            }
        }
        return file;
    }

    /** Xuất danh sách độc giả ra CSV. */
    public File exportReadersToCsv(File file) throws Exception {
        List<Reader> readers = readerDAO.findAll();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.print('\uFEFF');
            pw.println("STT,Ma The,Ho Ten,Ngay Sinh,Dien Thoai,Email,Dia Chi,Ngay Dang Ky,Trang Thai");
            for (int i = 0; i < readers.size(); i++) {
                Reader r = readers.get(i);
                pw.println((i + 1) + ","
                    + csvEsc(r.getReaderCode()) + ","
                    + csvEsc(r.getFullName()) + ","
                    + csvEsc(r.getBirthDate()) + ","
                    + csvEsc(r.getPhone()) + ","
                    + csvEsc(r.getEmail()) + ","
                    + csvEsc(r.getAddress()) + ","
                    + csvEsc(r.getJoinDate()) + ","
                    + csvEsc(r.getStatus().getLabel()));
            }
        }
        return file;
    }

    /** Xuất danh sách phiếu mượn ra CSV. */
    public File exportBorrowsToCsv(File file) throws Exception {
        List<Borrow> borrows = borrowDAO.findAll();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.print('\uFEFF');
            pw.println("STT,Ma Phieu,Ten Sach,ISBN,Doc Gia,Ma The,Ngay Muon,Han Tra,Ngay Tra,Trang Thai,Tien Phat,Ghi Chu");
            for (int i = 0; i < borrows.size(); i++) {
                Borrow b = borrows.get(i);
                pw.println((i + 1) + ","
                    + b.getId() + ","
                    + csvEsc(b.getBookTitle()) + ","
                    + csvEsc(b.getBookIsbn()) + ","
                    + csvEsc(b.getReaderName()) + ","
                    + csvEsc(b.getReaderCode()) + ","
                    + csvEsc(b.getBorrowDate()) + ","
                    + csvEsc(b.getDueDate()) + ","
                    + csvEsc(b.getReturnDate()) + ","
                    + csvEsc(b.getStatus().name()) + ","
                    + b.getFineAmount() + ","
                    + csvEsc(b.getNotes()));
            }
        }
        return file;
    }

    /** Xuất báo cáo thống kê tháng/năm ra CSV. */
    public File exportMonthlyReportToCsv(File file, int year) throws Exception {
        int[] monthly = borrowDAO.getBorrowCountByMonth(year);
        String[] monthNames = {"Thang 1","Thang 2","Thang 3","Thang 4",
                               "Thang 5","Thang 6","Thang 7","Thang 8",
                               "Thang 9","Thang 10","Thang 11","Thang 12"};
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            pw.print('\uFEFF');
            pw.println("THONG KE LUOT MUON SACH NAM " + year);
            pw.println("Xuat ngay: " + LocalDate.now().format(DATE_FMT));
            pw.println();
            pw.println("Thang,So Luot Muon");
            int total = 0;
            for (int i = 0; i < 12; i++) {
                pw.println(monthNames[i] + "," + monthly[i]);
                total += monthly[i];
            }
            pw.println("Tong ca nam," + total);
        }
        return file;
    }

    // ===================================================================
    //  Mở file sau khi xuất
    // ===================================================================

    /** Mở file bằng ứng dụng mặc định của OS (nếu hỗ trợ). */
    public static void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ignored) {}
    }

    // ===================================================================
    //  Helpers — Excel styling
    // ===================================================================

    private void createTitleRow(XSSFWorkbook wb, XSSFSheet sheet, String title, int colSpan) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(32f);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colSpan - 1));
    }

    private void createSubtitleRow(XSSFWorkbook wb, XSSFSheet sheet, String text, int colSpan) {
        Row row = sheet.createRow(1);
        row.setHeightInPoints(18f);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);

        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, colSpan - 1));
    }

    private void createHeaderRow(XSSFWorkbook wb, XSSFSheet sheet, String[] headers, int rowNum) {
        Row row = sheet.createRow(rowNum);
        row.setHeightInPoints(22f);

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void addSummaryRow(XSSFWorkbook wb, XSSFSheet sheet, int rowIdx, String text, int colSpan) {
        Row row = sheet.createRow(rowIdx + 1);
        row.setHeightInPoints(20f);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(style);

        if (colSpan > 1) {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx + 1, rowIdx + 1, 0, colSpan - 1));
        }
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFont(createDefaultFont(wb));
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    private CellStyle createAltRowStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFont(createDefaultFont(wb));
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    private CellStyle createCenterStyle(XSSFWorkbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook wb) {
        CellStyle style = createCenterStyle(wb);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    private CellStyle createTotalRowStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    private CellStyle createDangerStyle(XSSFWorkbook wb, boolean alt) {
        CellStyle style = wb.createCellStyle();
        style.setFont(createDefaultFont(wb));
        style.setFillForegroundColor(alt ? IndexedColors.ROSE.getIndex()
                                         : IndexedColors.CORAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    private Font createDefaultFont(XSSFWorkbook wb) {
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("Arial");
        return font;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void autoSizeColumns(XSSFSheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(w + 512, 20000));
        }
    }

    // ===================================================================
    //  Helpers — cell setters
    // ===================================================================

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Escape CSV: bọc nháy kép nếu chuỗi có dấu phẩy hoặc nháy kép. */
    private String csvEsc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /** Tạo tên file mặc định có timestamp. */
    public String defaultFileName(String prefix, String ext) {
        return prefix + "_" + LocalDateTime.now().format(TIMESTAMP_FMT) + "." + ext;
    }
}
