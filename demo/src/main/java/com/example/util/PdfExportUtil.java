package com.example.util;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Tiện ích xuất Phiếu Mượn Sách và Biên Lai Thu Tiền Phạt ra định dạng PDF.
 * Đã tối ưu hóa bố cục chuyên nghiệp và hỗ trợ hiển thị tiếng Việt UTF-8.
 */
public class PdfExportUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Dynamic font loading for Vietnamese support
    private static Font fontTitle;
    private static Font fontSubtitle;
    private static Font fontHeader;
    private static Font fontBody;
    private static Font fontBodyBold;
    private static Font fontSmall;
    private static Font fontSmallItalic;

    static {
        initFonts();
    }

    private static void initFonts() {
        BaseFont bf = null;
        // Thử nạp font hệ thống Windows có hỗ trợ tiếng Việt
        String[] fontPaths = {
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\tahoma.ttf",
            "C:\\Windows\\Fonts\\segoeui.ttf",
            "C:\\Windows\\Fonts\\times.ttf"
        };

        for (String path : fontPaths) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    break;
                } catch (Exception ignored) {}
            }
        }

        if (bf == null) {
            try {
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ignored) {}
        }

        if (bf != null) {
            fontTitle       = new Font(bf, 16, Font.BOLD, new Color(30, 41, 59));
            fontSubtitle    = new Font(bf, 12, Font.BOLD, new Color(71, 85, 105));
            fontHeader      = new Font(bf, 11, Font.BOLD, new Color(15, 23, 42));
            fontBody        = new Font(bf, 10, Font.NORMAL, new Color(51, 65, 85));
            fontBodyBold    = new Font(bf, 10, Font.BOLD, new Color(15, 23, 42));
            fontSmall       = new Font(bf, 8.5f, Font.NORMAL, new Color(100, 116, 139));
            fontSmallItalic = new Font(bf, 8.5f, Font.ITALIC, new Color(100, 116, 139));
        } else {
            fontTitle       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(30, 41, 59));
            fontSubtitle    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(71, 85, 105));
            fontHeader      = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(15, 23, 42));
            fontBody        = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
            fontBodyBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 23, 42));
            fontSmall       = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(100, 116, 139));
            fontSmallItalic = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, new Color(100, 116, 139));
        }
    }

    // ================================================================
    //  1. XUẤT PHIẾU MƯỢN SÁCH (BORROW SLIP)
    // ================================================================

    /**
     * Xuất phiếu mượn sách ra file PDF (Khổ giấy A5).
     */
    public static void exportBorrowSlip(Borrow borrow, File destFile) throws DocumentException, IOException {
        Document document = new Document(PageSize.A5, 30, 30, 25, 25);
        PdfWriter.getInstance(document, new FileOutputStream(destFile));
        document.open();

        // 1. Header cơ quan
        Paragraph pLib = new Paragraph("THƯ VIỆN TRƯỜNG THPT NGUYỄN HUỆ", fontSubtitle);
        pLib.setAlignment(Element.ALIGN_CENTER);
        document.add(pLib);

        Paragraph pAddr = new Paragraph("Địa chỉ: 123 Nguyễn Huệ, Q.1, TP. Hồ Chí Minh  |  Hotline: (028) 3829-1234", fontSmall);
        pAddr.setAlignment(Element.ALIGN_CENTER);
        document.add(pAddr);

        document.add(new Paragraph(" ", fontSmall));
        document.add(new LineSeparator(1f, 100f, new Color(203, 213, 225), Element.ALIGN_CENTER, -2));
        document.add(new Paragraph(" ", fontSmall));

        // 2. Tiêu đề phiếu
        Paragraph pTitle = new Paragraph("PHIẾU MƯỢN SÁCH", fontTitle);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitle);

        Paragraph pCode = new Paragraph("Mã phiếu: #" + String.format("%05d", borrow.getId()) +
                                        "  |  Ngày lập: " + (borrow.getBorrowDate() != null ? borrow.getBorrowDate() : LocalDate.now().format(DATE_FMT)), fontSmallItalic);
        pCode.setAlignment(Element.ALIGN_CENTER);
        document.add(pCode);

        document.add(new Paragraph(" ", fontBody));

        // 3. Bảng chi tiết thông tin
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 1f});

        // Độc giả
        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.addElement(new Paragraph("👤 THÔNG TIN ĐỘC GIẢ", fontHeader));
        cellLeft.addElement(createFieldPhrase("Họ và tên: ", borrow.getReaderName() != null ? borrow.getReaderName() : "N/A"));
        cellLeft.addElement(createFieldPhrase("Mã thẻ: ", borrow.getReaderCode() != null ? borrow.getReaderCode() : "N/A"));

        // Sách & Hạn
        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.addElement(new Paragraph("📚 THÔNG TIN MƯỢN SÁCH", fontHeader));
        cellRight.addElement(createFieldPhrase("Tên sách: ", borrow.getBookTitle() != null ? borrow.getBookTitle() : "N/A"));
        if (borrow.getBookIsbn() != null && !borrow.getBookIsbn().isBlank()) {
            cellRight.addElement(createFieldPhrase("Mã ISBN: ", borrow.getBookIsbn()));
        }
        cellRight.addElement(createFieldPhrase("Ngày mượn: ", borrow.getBorrowDate() != null ? borrow.getBorrowDate() : "—"));
        cellRight.addElement(createFieldPhrase("Hạn trả sách: ", borrow.getDueDate() != null ? borrow.getDueDate() : "—"));
        if (borrow.getRenewCount() > 0) {
            cellRight.addElement(createFieldPhrase("Số lần gia hạn: ", borrow.getRenewCount() + "/" + BorrowService.MAX_RENEW_COUNT));
        }

        table.addCell(cellLeft);
        table.addCell(cellRight);
        document.add(table);

        if (borrow.getNotes() != null && !borrow.getNotes().isBlank()) {
            document.add(new Paragraph(" ", fontSmall));
            Paragraph pNotes = new Paragraph("📌 Ghi chú: " + borrow.getNotes(), fontSmallItalic);
            document.add(pNotes);
        }

        document.add(new Paragraph(" ", fontBody));

        // 4. Quy định mượn trả
        Paragraph pRulesHeader = new Paragraph("LƯU Ý ĐỐI VỚI ĐỘC GIẢ:", fontHeader);
        document.add(pRulesHeader);
        Paragraph pRule1 = new Paragraph("• Độc giả có trách nhiệm giữ gìn sách nguyên vẹn, trả sách đúng hạn ghi trên phiếu.", fontSmall);
        Paragraph pRule2 = new Paragraph("• Trả sách quá hạn sẽ bị tính phí phạt " + formatCurrency(BorrowService.FINE_PER_DAY) + "/ngày.", fontSmall);
        document.add(pRule1);
        document.add(pRule2);

        document.add(new Paragraph(" ", fontBody));
        document.add(new Paragraph(" ", fontBody));

        // 5. Phần chữ ký
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{1f, 1f});

        PdfPCell cSignReader = new PdfPCell(new Paragraph("NGƯỜI MƯỢN SÁCH\n(Ký và ghi rõ họ tên)\n\n\n\n", fontBodyBold));
        cSignReader.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSignReader.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSignLibrarian = new PdfPCell(new Paragraph("THỦ THƯ XÁC NHẬN\n(Ký và ghi rõ họ tên)\n\n\n\n", fontBodyBold));
        cSignLibrarian.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSignLibrarian.setBorder(Rectangle.NO_BORDER);

        signTable.addCell(cSignReader);
        signTable.addCell(cSignLibrarian);
        document.add(signTable);

        document.close();
    }

    // ================================================================
    //  2. XUẤT BIÊN LAI THU TIỀN PHẠT (FINE RECEIPT)
    // ================================================================

    /**
     * Xuất biên lai thu tiền phạt ra file PDF.
     */
    public static void exportFineReceipt(Borrow borrow, File destFile) throws DocumentException, IOException {
        Document document = new Document(PageSize.A5, 30, 30, 25, 25);
        PdfWriter.getInstance(document, new FileOutputStream(destFile));
        document.open();

        // 1. Header cơ quan
        Paragraph pLib = new Paragraph("THƯ VIỆN TRƯỜNG THPT NGUYỄN HUỆ", fontSubtitle);
        pLib.setAlignment(Element.ALIGN_CENTER);
        document.add(pLib);

        Paragraph pAddr = new Paragraph("Địa chỉ: 123 Nguyễn Huệ, Q.1, TP. Hồ Chí Minh  |  Hotline: (028) 3829-1234", fontSmall);
        pAddr.setAlignment(Element.ALIGN_CENTER);
        document.add(pAddr);

        document.add(new Paragraph(" ", fontSmall));
        document.add(new LineSeparator(1f, 100f, new Color(239, 68, 68), Element.ALIGN_CENTER, -2));
        document.add(new Paragraph(" ", fontSmall));

        // 2. Tiêu đề biên lai
        Paragraph pTitle = new Paragraph("BIÊN LAI THU TIỀN PHẠT", fontTitle);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(pTitle);

        String todayStr = LocalDate.now().format(DATE_FMT);
        Paragraph pCode = new Paragraph("Mã biên lai: #BL-" + String.format("%05d", borrow.getId()) +
                                        "  |  Ngày thu: " + todayStr, fontSmallItalic);
        pCode.setAlignment(Element.ALIGN_CENTER);
        document.add(pCode);

        document.add(new Paragraph(" ", fontBody));

        // 3. Chi tiết thông tin thu phạt
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.8f});

        addTableRow(table, "Họ tên người nộp:", borrow.getReaderName() != null ? borrow.getReaderName() : "N/A");
        addTableRow(table, "Mã độc giả:", borrow.getReaderCode() != null ? borrow.getReaderCode() : "N/A");
        addTableRow(table, "Sách vi phạm:", borrow.getBookTitle() != null ? borrow.getBookTitle() : "N/A");
        addTableRow(table, "Ngày mượn sách:", borrow.getBorrowDate() != null ? borrow.getBorrowDate() : "—");
        addTableRow(table, "Hạn trả ban đầu:", borrow.getDueDate() != null ? borrow.getDueDate() : "—");

        String retDate = borrow.getReturnDate() != null ? borrow.getReturnDate() : todayStr;
        addTableRow(table, "Ngày trả thực tế:", retDate);

        // Tính số ngày quá hạn
        long overdueDays = 0;
        try {
            LocalDate due = LocalDate.parse(borrow.getDueDate(), DATE_FMT);
            LocalDate ret = LocalDate.parse(retDate, DATE_FMT);
            overdueDays = ChronoUnit.DAYS.between(due, ret);
            if (overdueDays < 0) overdueDays = 0;
        } catch (Exception ignored) {}

        addTableRow(table, "Lý do thu phạt:", "Trả sách quá hạn (" + overdueDays + " ngày)");
        addTableRow(table, "Mức phạt áp dụng:", formatCurrency(BorrowService.FINE_PER_DAY) + " / ngày");

        document.add(table);

        document.add(new Paragraph(" ", fontBody));

        // Highlight tổng tiền
        double fineAmount = borrow.getFineAmount() > 0 ? borrow.getFineAmount() : (overdueDays * BorrowService.FINE_PER_DAY);
        String fineStr = formatCurrency(fineAmount);
        String wordsStr = numberToWords((long) fineAmount);

        PdfPTable totalTable = new PdfPTable(1);
        totalTable.setWidthPercentage(100);

        PdfPCell totalCell = new PdfPCell();
        totalCell.setBackgroundColor(new Color(254, 242, 242));
        totalCell.setBorderColor(new Color(252, 165, 165));
        totalCell.setPadding(10);

        Paragraph pTotal = new Paragraph("TỔNG TIỀN PHẠT ĐÃ THU: " + fineStr, new Font(fontTitle.getBaseFont(), 13, Font.BOLD, new Color(220, 38, 38)));
        pTotal.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(pTotal);

        Paragraph pWords = new Paragraph("Bằng chữ: " + wordsStr, fontSmallItalic);
        pWords.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(pWords);

        Paragraph pStatus = new Paragraph("✔ Trạng thái: ĐÃ THANH TOÁN ĐỦ", new Font(fontHeader.getBaseFont(), 10, Font.BOLD, new Color(16, 185, 129)));
        pStatus.setAlignment(Element.ALIGN_CENTER);
        totalCell.addElement(pStatus);

        totalTable.addCell(totalCell);
        document.add(totalTable);

        document.add(new Paragraph(" ", fontBody));
        document.add(new Paragraph(" ", fontBody));

        // 4. Phần chữ ký
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{1f, 1f});

        PdfPCell cSignPayer = new PdfPCell(new Paragraph("NGƯỜI NỘP TIỀN\n(Ký và ghi rõ họ tên)\n\n\n\n", fontBodyBold));
        cSignPayer.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSignPayer.setBorder(Rectangle.NO_BORDER);

        PdfPCell cSignCollector = new PdfPCell(new Paragraph("NGƯỜI THU TIỀN / THỦ THƯ\n(Ký và ghi rõ họ tên)\n\n\n\n", fontBodyBold));
        cSignCollector.setHorizontalAlignment(Element.ALIGN_CENTER);
        cSignCollector.setBorder(Rectangle.NO_BORDER);

        signTable.addCell(cSignPayer);
        signTable.addCell(cSignCollector);
        document.add(signTable);

        document.close();
    }

    // ================================================================
    //  Helpers
    // ================================================================

    private static Paragraph createFieldPhrase(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label, fontBodyBold));
        p.add(new Chunk(value, fontBody));
        return p;
    }

    private static void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell c1 = new PdfPCell(new Paragraph(label, fontBodyBold));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(4);

        PdfPCell c2 = new PdfPCell(new Paragraph(value, fontBody));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(4);

        table.addCell(c1);
        table.addCell(c2);
    }

    private static String formatCurrency(double amount) {
        return String.format("%,.0f đ", amount).replace(',', '.');
    }

    /**
     * Chuyển đổi số tiền VND sang chuỗi đọc bằng chữ tiếng Việt.
     */
    public static String numberToWords(long number) {
        if (number == 0) return "Không đồng";
        if (number < 0) return "Âm " + numberToWords(-number);

        String[] units = {"", "nghìn", "triệu", "tỷ"};
        StringBuilder result = new StringBuilder();

        int place = 0;
        while (number > 0) {
            long chunk = number % 1000;
            if (chunk > 0) {
                String chunkText = chunkToWords((int) chunk);
                if (!units[place].isEmpty()) {
                    chunkText += " " + units[place];
                }
                if (result.length() > 0) {
                    result.insert(0, chunkText + " ");
                } else {
                    result.append(chunkText);
                }
            }
            number /= 1000;
            place++;
        }

        String str = result.toString().trim();
        str = Character.toUpperCase(str.charAt(0)) + str.substring(1) + " đồng";
        return str;
    }

    private static String chunkToWords(int n) {
        String[] digitNames = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        int hundred = n / 100;
        int ten = (n % 100) / 10;
        int unit = n % 10;

        StringBuilder sb = new StringBuilder();
        if (hundred > 0) {
            sb.append(digitNames[hundred]).append(" trăm");
            if (ten == 0 && unit > 0) sb.append(" lẻ");
        }
        if (ten > 1) {
            sb.append(sb.length() > 0 ? " " : "").append(digitNames[ten]).append(" mươi");
            if (unit == 1) sb.append(" mốt");
            else if (unit == 5) sb.append(" lăm");
            else if (unit > 0) sb.append(" ").append(digitNames[unit]);
        } else if (ten == 1) {
            sb.append(sb.length() > 0 ? " " : "").append("mười");
            if (unit == 5) sb.append(" lăm");
            else if (unit > 0) sb.append(" ").append(digitNames[unit]);
        } else if (unit > 0 && hundred == 0) {
            sb.append(digitNames[unit]);
        } else if (unit > 0 && hundred > 0) {
            sb.append(" ").append(digitNames[unit]);
        }
        return sb.toString();
    }
}
