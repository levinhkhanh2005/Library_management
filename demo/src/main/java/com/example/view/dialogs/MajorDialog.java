package com.example.view.dialogs;

import com.example.model.Major;
import com.example.service.MajorService;
import com.example.view.UITheme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Dialog thêm/sửa khối ngành. */
public class MajorDialog extends JDialog {
    private final MajorService service = new MajorService();
    private final Major editMajor;
    private boolean saved;
    private Major result;
    private JTextField fName;
    private JTextArea fDescription;

    public MajorDialog(Window parent, Major major) {
        super(parent, major == null ? "Thêm Khối Ngành" : "Chỉnh Sửa Khối Ngành", ModalityType.APPLICATION_MODAL);
        editMajor = major;
        buildUI();
        if (major != null) { fName.setText(major.getName()); fDescription.setText(major.getDescription()); }
        pack(); setResizable(false); setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(UITheme.BG_WHITE); setContentPane(root);
        JLabel title = new JLabel(editMajor == null ? "  🎓  Thêm Khối Ngành" : "  ✎  Chỉnh Sửa Khối Ngành");
        title.setFont(UITheme.FONT_H3); title.setForeground(Color.WHITE);
        JPanel bar = new JPanel(new BorderLayout()); bar.setBackground(UITheme.ACCENT_PRIMARY); bar.setPreferredSize(new Dimension(460, 55)); bar.add(title);
        root.add(bar, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout()); form.setBackground(UITheme.BG_WHITE); form.setBorder(new EmptyBorder(18, 18, 8, 18));
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(6, 6, 6, 6); g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx=0; g.gridy=0; g.weightx=.3; form.add(new JLabel("Tên Khối Ngành *"), g);
        fName=UITheme.createTextField("VD: Công Nghệ & Kỹ Thuật"); g.gridx=1; g.weightx=.7; form.add(fName,g);
        g.gridx=0; g.gridy=1; g.weightx=.3; g.anchor=GridBagConstraints.NORTHWEST; form.add(new JLabel("Mô Tả"),g);
        fDescription=UITheme.createTextArea(4,25); JScrollPane sp=new JScrollPane(fDescription); sp.setPreferredSize(new Dimension(0,90));
        g.gridx=1; g.fill=GridBagConstraints.BOTH; g.weighty=1; form.add(sp,g); root.add(form,BorderLayout.CENTER);

        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,10)); buttons.setBackground(UITheme.BG_WHITE);
        JButton cancel=UITheme.createSecondaryButton("Hủy"), save=UITheme.createPrimaryButton(editMajor==null?"Thêm":"Lưu");
        cancel.addActionListener(e->dispose()); save.addActionListener(e->save()); buttons.add(cancel); buttons.add(save); root.add(buttons,BorderLayout.SOUTH);
    }

    private void save() {
        try {
            String name=fName.getText().trim(); if(name.isEmpty()){UITheme.showWarning(this,"Tên khối ngành không được để trống.");return;}
            if(editMajor==null) result=service.addMajor(name,fDescription.getText().trim());
            else { editMajor.setName(name); editMajor.setDescription(fDescription.getText().trim()); service.updateMajor(editMajor); result=editMajor; }
            saved=true; UITheme.showSuccess(this,"Đã lưu khối ngành thành công!"); dispose();
        } catch(IllegalArgumentException ex){UITheme.showWarning(this,ex.getMessage());}
          catch(Exception ex){UITheme.showError(this,"Lỗi lưu khối ngành:\n"+ex.getMessage());}
    }
    public boolean isSaved(){return saved;}
    public Major getMajor(){return result;}
}
