package com.example.view.dialogs;

import com.example.model.Major;
import com.example.service.MajorService;
import com.example.view.UITheme;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Quản lý Khối Ngành độc lập với Thể Loại. */
public class MajorManageDialog extends JDialog {
    private final MajorService service=new MajorService();
    private JTable table; private DefaultTableModel model; private boolean changed;
    public MajorManageDialog(Frame parent){super(parent,"Quản Lý Khối Ngành",true); buildUI(); setSize(720,480); setLocationRelativeTo(parent); load();}
    private void buildUI(){
        JPanel root=new JPanel(new BorderLayout(0,8)); root.setBackground(UITheme.BG_PRIMARY); setContentPane(root);
        root.setBorder(BorderFactory.createEmptyBorder(14,14,14,14));
        root.add(UITheme.createPageHeader("🎓  Quản Lý Khối Ngành","Một khối ngành có thể chứa nhiều thể loại sách"),BorderLayout.NORTH);
        JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4)); bar.setBackground(UITheme.BG_WHITE);
        JButton add=UITheme.createPrimaryButton("＋ Thêm Khối Ngành"), edit=UITheme.createSecondaryButton("✎ Sửa"), del=UITheme.createDangerButton("✕ Xóa"), refresh=UITheme.createSecondaryButton("↺ Làm Mới");
        bar.add(add);bar.add(edit);bar.add(del);bar.add(refresh);root.add(bar,BorderLayout.CENTER);
        model=new DefaultTableModel(new String[]{"#","Khối Ngành","Mô Tả","Số Thể Loại"},0){public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(model); UITheme.styleTable(table); root.add(UITheme.createTableScrollPane(table),BorderLayout.SOUTH);
        // JTable cần nằm ở CENTER, toolbar ở NORTH; sửa layout bằng cách tái cấu trúc
        root.remove(root.getComponentCount()-1);
        JPanel center=new JPanel(new BorderLayout(0,8)); center.setOpaque(false); center.add(bar,BorderLayout.NORTH); center.add(UITheme.createTableScrollPane(table),BorderLayout.CENTER); root.add(center,BorderLayout.CENTER);
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT)); footer.setBackground(UITheme.BG_PRIMARY); JButton close=UITheme.createSecondaryButton("Đóng"); close.addActionListener(e->dispose()); footer.add(close); root.add(footer,BorderLayout.SOUTH);
        add.addActionListener(e->openAdd()); edit.addActionListener(e->openEdit()); del.addActionListener(e->delete()); refresh.addActionListener(e->load());
    }
    private void load(){try{List<Major> list=service.getAllMajors();model.setRowCount(0);int i=1;for(Major m:list){int count=new com.example.dao.MajorDAO().countCategoriesUsingMajor(m.getId());model.addRow(new Object[]{i++,m.getName(),m.getDescription()==null?"":m.getDescription(),count});}}catch(Exception ex){UITheme.showError(this,"Lỗi tải khối ngành:\n"+ex.getMessage());}}
    private Major selected(){int r=table.getSelectedRow();if(r<0)return null;try{return service.getMajorById((Integer)model.getValueAt(r,0));}catch(Exception e){return null;}}
    // Cột # là số thứ tự, nên lấy theo tên thay vì id
    private Major selectedSafe(){int r=table.getSelectedRow();if(r<0)return null;try{String n=String.valueOf(model.getValueAt(r,1));for(Major m:service.getAllMajors())if(m.getName().equalsIgnoreCase(n))return m;}catch(Exception ignored){}return null;}
    private void openAdd(){MajorDialog d=new MajorDialog(this,null);d.setVisible(true);if(d.isSaved()){changed=true;load();}}
    private void openEdit(){Major m=selectedSafe();if(m==null){UITheme.showWarning(this,"Vui lòng chọn khối ngành.");return;}MajorDialog d=new MajorDialog(this,m);d.setVisible(true);if(d.isSaved()){changed=true;load();}}
    private void delete(){Major m=selectedSafe();if(m==null){UITheme.showWarning(this,"Vui lòng chọn khối ngành.");return;}if(!UITheme.showConfirm(this,"Xóa khối ngành \""+m.getName()+"\"?","Xác nhận"))return;try{service.deleteMajor(m.getId());changed=true;load();UITheme.showSuccess(this,"Đã xóa khối ngành.");}catch(IllegalStateException ex){UITheme.showWarning(this,ex.getMessage());}catch(Exception ex){UITheme.showError(this,"Lỗi xóa:\n"+ex.getMessage());}}
    public boolean isDataChanged(){return changed;}
}
