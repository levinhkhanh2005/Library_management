package com.example;

import com.example.model.Borrow;
import com.example.service.BorrowService;
import com.example.util.DatabaseInitializer;
import com.example.util.PdfExportUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

public class PdfExportTest {

    private BorrowService borrowService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Before
    public void setUp() {
        DatabaseInitializer.initialize();
        borrowService = new BorrowService();
    }

    @Test
    public void testNumberToWords() {
        assertEquals("Không đồng", PdfExportUtil.numberToWords(0));
        assertEquals("Mười nghìn đồng", PdfExportUtil.numberToWords(10000));
        assertEquals("Hai mươi nghìn đồng", PdfExportUtil.numberToWords(20000));
        assertEquals("Một trăm năm mươi nghìn đồng", PdfExportUtil.numberToWords(150000));
        assertEquals("Một triệu một nghìn đồng", PdfExportUtil.numberToWords(1001000));
    }

    @Test
    public void testExportBorrowSlipToPdf() throws Exception {
        Borrow borrow = borrowService.borrowBook(1, 1, LocalDate.now().plusDays(14).format(DATE_FMT), "Test PDF Borrow Slip");
        assertNotNull(borrow);

        File tempFile = File.createTempFile("Test_PhieuMuon_", ".pdf");
        tempFile.deleteOnExit();

        PdfExportUtil.exportBorrowSlip(borrow, tempFile);

        assertTrue("File PDF phiếu mượn phải được tạo thành công", tempFile.exists());
        assertTrue("Kích thước file PDF phải > 0", tempFile.length() > 0);

        borrowService.returnBook(borrow.getId());
    }

    @Test
    public void testExportFineReceiptToPdf() throws Exception {
        Borrow borrow = borrowService.borrowBook(2, 2, LocalDate.now().plusDays(14).format(DATE_FMT), "Test PDF Fine Receipt");
        assertNotNull(borrow);

        // Giả lập ngày hạn cũ trong quá khứ
        String pastDue = LocalDate.now().minusDays(5).format(DATE_FMT);
        borrow.setDueDate(pastDue);
        borrow.setFineAmount(10000.0);

        File tempFile = File.createTempFile("Test_BienLaiPhat_", ".pdf");
        tempFile.deleteOnExit();

        PdfExportUtil.exportFineReceipt(borrow, tempFile);

        assertTrue("File PDF biên lai phạt phải được tạo thành công", tempFile.exists());
        assertTrue("Kích thước file PDF phải > 0", tempFile.length() > 0);

        borrowService.returnBook(borrow.getId());
    }
}
