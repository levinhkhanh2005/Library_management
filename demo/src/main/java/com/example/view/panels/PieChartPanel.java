package com.example.view.panels;

import com.example.model.CategoryBookStat;
import com.example.view.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Component vẽ biểu đồ tròn (Pie/Donut Chart) hiện đại bằng Java2D.
 * Hiển thị tỷ lệ % của các thể loại sách trong tổng số sách của thư viện.
 *
 * Tính năng:
 * - Khử răng cưa mịn màng (Anti-aliasing).
 * - Bảng màu hiện đại 12 màu tương phản cao, phong phú.
 * - Hiệu ứng Donut Chart với tâm hiển thị tổng số sách.
 * - Tương tác di chuột (Hover effect): Múi sách nở ra nhẹ khi rê chuột, hiển thị Tooltip chi tiết.
 * - Hỗ trợ callback khi click vào múi để lọc sách.
 */
public class PieChartPanel extends JPanel {

    // Bảng 12 màu hiện đại hài hòa
    public static final Color[] PALETTE = {
        new Color(0x4F46E5), // Indigo
        new Color(0x06B6D4), // Cyan
        new Color(0x10B981), // Emerald
        new Color(0xF59E0B), // Amber
        new Color(0xEF4444), // Rose
        new Color(0x8B5CF6), // Purple
        new Color(0xEC4899), // Pink
        new Color(0x3B82F6), // Blue
        new Color(0x14B8A6), // Teal
        new Color(0xF97316), // Orange
        new Color(0x6366F1), // Violet
        new Color(0x64748B)  // Slate
    };

    private List<CategoryBookStat> stats = new ArrayList<>();
    private boolean byTitleCount = true; // true = theo số đầu sách, false = theo tổng bản sao
    private int hoveredIndex = -1;
    private Consumer<String> onCategorySelectListener;

    // Lưu hình dạng các múi để hit-test chuột chính xác
    private final List<Shape> sliceShapes = new ArrayList<>();

    public PieChartPanel() {
        setBackground(UITheme.BG_WHITE);
        setOpaque(true);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int oldHover = hoveredIndex;
                hoveredIndex = findSliceAt(e.getPoint());
                if (oldHover != hoveredIndex) {
                    if (hoveredIndex >= 0 && hoveredIndex < stats.size()) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        CategoryBookStat s = stats.get(hoveredIndex);
                        int count = byTitleCount ? s.getTitleCount() : s.getTotalCopies();
                        String unit = byTitleCount ? "đầu sách" : "bản sao";
                        setToolTipText(String.format("<html><b>%s</b><br/>Số lượng: <b>%d</b> %s<br/>Tỷ lệ: <b>%.1f%%</b></html>",
                            s.getCategoryName(), count, unit, s.getPercentage()));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                        setToolTipText(null);
                    }
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredIndex != -1) {
                    hoveredIndex = -1;
                    setCursor(Cursor.getDefaultCursor());
                    setToolTipText(null);
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = findSliceAt(e.getPoint());
                if (idx >= 0 && idx < stats.size() && onCategorySelectListener != null) {
                    onCategorySelectListener.accept(stats.get(idx).getCategoryName());
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setData(List<CategoryBookStat> data, boolean byTitleCount) {
        this.stats = data != null ? new ArrayList<>(data) : new ArrayList<>();
        this.byTitleCount = byTitleCount;
        this.hoveredIndex = -1;
        repaint();
    }

    public void setOnCategorySelectListener(Consumer<String> listener) {
        this.onCategorySelectListener = listener;
    }

    public boolean isByTitleCount() {
        return byTitleCount;
    }

    public List<CategoryBookStat> getStats() {
        return stats;
    }

    public Color getColorForIndex(int i) {
        return PALETTE[i % PALETTE.length];
    }

    private int findSliceAt(Point p) {
        for (int i = 0; i < sliceShapes.size(); i++) {
            if (sliceShapes.get(i).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        sliceShapes.clear();

        if (stats.isEmpty() || w <= 40 || h <= 40) {
            drawEmpty(g2, w, h);
            g2.dispose();
            return;
        }

        // Tính toán kích thước và tâm biểu đồ
        int padding = 20;
        int size = Math.min(w, h) - padding * 2;
        int cx = w / 2;
        int cy = h / 2;
        int radius = size / 2;
        int innerRadius = (int) (radius * 0.55); // Kích thước lỗ Donut

        double startAngle = 90.0; // Bắt đầu từ 12 giờ
        int totalItems = 0;
        for (CategoryBookStat s : stats) {
            totalItems += byTitleCount ? s.getTitleCount() : s.getTotalCopies();
        }

        // Vẽ từng múi sách
        for (int i = 0; i < stats.size(); i++) {
            CategoryBookStat stat = stats.get(i);
            double arcAngle = -(stat.getPercentage() / 100.0) * 360.0;
            if (i == stats.size() - 1 && stats.size() > 1) {
                // Đảm bảo múi cuối cùng lấp đầy trọn vòng 360 độ
                // do sai số làm tròn %
            }

            // Tính toán hiệu ứng nở ra (pop-out) khi hover
            double offset = (i == hoveredIndex) ? 10.0 : 0.0;
            double midAngleRad = Math.toRadians(startAngle + arcAngle / 2.0);
            double offsetX = offset * Math.cos(midAngleRad);
            double offsetY = -offset * Math.sin(midAngleRad);

            // Tạo shape hình vòng khuyên (Donut slice)
            Arc2D.Double outerArc = new Arc2D.Double(
                cx - radius + offsetX, cy - radius + offsetY,
                radius * 2, radius * 2,
                startAngle, arcAngle, Arc2D.PIE
            );

            Ellipse2D.Double innerHole = new Ellipse2D.Double(
                cx - innerRadius + offsetX, cy - innerRadius + offsetY,
                innerRadius * 2, innerRadius * 2
            );

            Area donutSlice = new Area(outerArc);
            donutSlice.subtract(new Area(innerHole));
            sliceShapes.add(donutSlice);

            // Tô màu múi
            Color baseColor = getColorForIndex(i);
            if (i == hoveredIndex) {
                // Đổ bóng nhẹ & màu sáng nổi bật
                g2.setColor(new Color(0, 0, 0, 30));
                Area shadow = new Area(donutSlice);
                shadow.transform(java.awt.geom.AffineTransform.getTranslateInstance(2, 3));
                g2.fill(shadow);

                g2.setColor(baseColor.brighter());
            } else {
                g2.setColor(baseColor);
            }
            g2.fill(donutSlice);

            // Đường viền trắng mỏng ngăn cách giữa các múi
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(donutSlice);

            // Vẽ nhãn phần trăm (%) trên múi nếu góc đủ rộng (> 14 độ)
            if (Math.abs(arcAngle) >= 14.0) {
                double labelRadius = innerRadius + (radius - innerRadius) * 0.52;
                double lx = cx + offsetX + labelRadius * Math.cos(midAngleRad);
                double ly = cy + offsetY - labelRadius * Math.sin(midAngleRad);

                String pctText = String.format(java.util.Locale.US, "%.1f%%", stat.getPercentage());
                g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, Math.max(10, Math.min(13, radius / 12))));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(pctText);
                int textH = fm.getAscent();

                // Đổ bóng chữ để dễ đọc trên mọi màu nền
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(pctText, (float) (lx - textW / 2.0 + 1), (float) (ly + textH / 2.0 - 2 + 1));
                g2.setColor(Color.WHITE);
                g2.drawString(pctText, (float) (lx - textW / 2.0), (float) (ly + textH / 2.0 - 2));
            }

            startAngle += arcAngle;
        }

        // Vẽ vùng tâm Donut
        g2.setColor(UITheme.BG_WHITE);
        g2.fillOval(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2);
        g2.setColor(UITheme.BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2);

        // Văn bản chính giữa tâm Donut
        if (hoveredIndex >= 0 && hoveredIndex < stats.size()) {
            CategoryBookStat s = stats.get(hoveredIndex);
            int count = byTitleCount ? s.getTitleCount() : s.getTotalCopies();

            g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 10));
            g2.setColor(getColorForIndex(hoveredIndex));
            drawCenteredString(g2, truncate(s.getCategoryName(), 14), cx, cy - 18);

            g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 22));
            g2.setColor(UITheme.TEXT_PRIMARY);
            drawCenteredString(g2, String.valueOf(count), cx, cy + 4);

            g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 12));
            g2.setColor(getColorForIndex(hoveredIndex));
            drawCenteredString(g2, s.getFormattedPercentage(), cx, cy + 22);
        } else {
            g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 9));
            g2.setColor(UITheme.TEXT_MUTED);
            drawCenteredString(g2, byTitleCount ? "TỔNG ĐẦU SÁCH" : "TỔNG BẢN SAO", cx, cy - 14);

            g2.setFont(new Font(UITheme.FONT_NAME, Font.BOLD, 24));
            g2.setColor(UITheme.ACCENT_PRIMARY);
            drawCenteredString(g2, String.valueOf(totalItems), cx, cy + 10);

            g2.setFont(new Font(UITheme.FONT_NAME, Font.PLAIN, 10));
            g2.setColor(UITheme.TEXT_SECONDARY);
            drawCenteredString(g2, stats.size() + " thể loại", cx, cy + 26);
        }

        g2.dispose();
    }

    private void drawEmpty(Graphics2D g2, int w, int h) {
        g2.setFont(UITheme.FONT_BODY);
        g2.setColor(UITheme.TEXT_MUTED);
        drawCenteredString(g2, "Chưa có dữ liệu sách để vẽ biểu đồ", w / 2, h / 2);
    }

    private void drawCenteredString(Graphics2D g2, String text, int x, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = x - fm.stringWidth(text) / 2;
        int ty = y + (fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, tx, ty);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }
}
