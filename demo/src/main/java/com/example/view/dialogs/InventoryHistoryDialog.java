package com.example.view.dialogs;

import com.example.model.Book;
import com.example.model.InventoryLog;
import com.example.service.BookService;
import com.example.view.UITheme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Hiển thị lịch sử nhập, thanh lý và người thực hiện điều chỉnh kho. */
public class InventoryHistoryDialog extends JDialog {
    public InventoryHistoryDialog(Window parent, Book book) {
        super(parent, "Lịch sử kho: " + book.getTitle(), ModalityType.APPLICATION_MODAL);
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Thời gian", "Thao tác", "Số lượng", "Lý do", "Người thực hiện"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        UITheme.styleTable(table);
        try {
            for (InventoryLog log : new BookService().getInventoryHistory(book.getId())) {
                model.addRow(new Object[]{log.createdAt(), log.action(), log.quantity(),
                    log.reason() == null ? "" : log.reason(), log.userName()});
            }
        } catch (Exception e) {
            model.addRow(new Object[]{"", "Lỗi", "", e.getMessage(), ""});
        }
        JButton close = UITheme.createPrimaryButton("Đóng");
        close.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(close);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        setSize(760, 360);
        setLocationRelativeTo(parent);
    }
}
