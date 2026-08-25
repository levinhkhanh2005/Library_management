package com.example.view.panels;

import com.example.service.BookService;
import com.example.service.BorrowService;
import com.example.service.ReaderService;
import com.example.view.MainFrame;
import com.example.view.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel Tổng Quan — hiển thị thống kê tổng hợp.
 */
public class DashboardPanel extends JPanel implements MainFrame.Refreshable {

    private final BookService   bookService   = new BookService();
    private final ReaderService readerService = new ReaderService();
    private final BorrowService borrowService = new BorrowService();

    private JLabel lblTotalBooks, lblBorrowed, lblReaders, lblOverdue;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, UITheme.PAD_LG));
        setBackground(UITheme.BG_PRIMARY);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        add(UITheme.createPageHeader("🏠  Tổng Quan",
            "Thống kê tổng hợp hệ thống thư viện"), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        loadData();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, UITheme.PAD_LG));
        content.setBackground(UITheme.BG_PRIMARY);

        // Stat cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, UITheme.PAD_MD, 0));
        statsRow.setBackground(UITheme.BG_PRIMARY);

        lblTotalBooks = new JLabel("...");
        lblBorrowed   = new JLabel("...");
        lblReaders    = new JLabel("...");
        lblOverdue    = new JLabel("...");

        statsRow.add(UITheme.createStatCard("Tổng Đầu Sách",  "...", UITheme.ACCENT_PRIMARY));
        statsRow.add(UITheme.createStatCard("Đang Mượn",      "...", UITheme.COLOR_WARNING));
        statsRow.add(UITheme.createStatCard("Độc Giả",        "...", UITheme.COLOR_SUCCESS));
        statsRow.add(UITheme.createStatCard("Quá Hạn",        "...", UITheme.COLOR_DANGER));

        content.add(statsRow, BorderLayout.NORTH);

        // Welcome card
        JPanel welcomeCard = UITheme.createCard();
        welcomeCard.setLayout(new BorderLayout());
        welcomeCard.setBorder(new EmptyBorder(UITheme.PAD_LG, UITheme.PAD_LG,
                                              UITheme.PAD_LG, UITheme.PAD_LG));

        JLabel welcomeLbl = new JLabel(
            "<html><h2 style='color:#1E293B'>Chào mừng đến Thư Viện Nguyễn Huệ! 📖</h2>" +
            "<p style='color:#64748B'>Chọn chức năng từ menu bên trái để bắt đầu quản lý.</p></html>"
        );
        welcomeCard.add(welcomeLbl, BorderLayout.CENTER);
        content.add(welcomeCard, BorderLayout.CENTER);

        return content;
    }

    private void loadData() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override protected int[] doInBackground() throws Exception {
                return new int[]{
                    bookService.getTotalBooks(),
                    bookService.getTotalBorrowed(),
                    readerService.getTotalReaders(),
                    borrowService.getOverdueBorrowCount()
                };
            }
            @Override protected void done() {
                try {
                    int[] data = get();
                    // Tìm lại các stat card và cập nhật giá trị
                    JPanel statsRow = findStatsRow();
                    if (statsRow != null) {
                        String[] values = {
                            String.valueOf(data[0]),
                            String.valueOf(data[1]),
                            String.valueOf(data[2]),
                            String.valueOf(data[3])
                        };
                        Color[] colors = {
                            UITheme.ACCENT_PRIMARY, UITheme.COLOR_WARNING,
                            UITheme.COLOR_SUCCESS,  UITheme.COLOR_DANGER
                        };
                        String[] titles = {"Tổng Đầu Sách","Đang Mượn","Độc Giả","Quá Hạn"};
                        statsRow.removeAll();
                        for (int i = 0; i < 4; i++) {
                            statsRow.add(UITheme.createStatCard(titles[i], values[i], colors[i]));
                        }
                        statsRow.revalidate();
                        statsRow.repaint();
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private JPanel findStatsRow() {
        for (Component c : ((JPanel)((BorderLayout)getLayout())
                .getLayoutComponent(BorderLayout.CENTER)).getComponents()) {
            if (c instanceof JPanel p && p.getLayout() instanceof GridLayout) return p;
        }
        return null;
    }

    @Override public void refresh() { loadData(); }
}
