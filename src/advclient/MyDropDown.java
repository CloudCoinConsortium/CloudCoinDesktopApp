/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.accessibility.Accessible;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

/**
 *
 * @author Alexander
 */
public class MyDropDown {
    
    public Color outerBgColor;
    public Color background;
    public Color foreground;
    
    String[] options;
    String placeholder;
    
    
    JPanel core;
    MyJComboBox combo1;

    
    
    public MyDropDown(Color outerBgColor, JLabel icon, String[] options) {
        this.outerBgColor = outerBgColor;
        this.options = options;
        
        core = makeUI();
    }
    
    public JPanel getComboBox() {
        return core;
    }
    
    public String getSelectedValue() {
        return String.valueOf(combo1.getSelectedItem());
    }
    
    public int getSelectedIndex() {
        return combo1.getSelectedIndex();
    }
    
    public void addActionListener(ActionListener al) {
        combo1.addActionListener(al);
    }
    
    
    
    
    public JPanel makeUI() {
        
     
        background = AppUI.blendColors(AppUI.getColor4(), outerBgColor);
        foreground = background;

        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        UIManager.put("ComboBox.selectionBackground", background);
        
        combo1 = new MyJComboBox();
    //    AppUI.setSize(combo1, 100, 100);  
        AppUI.setFont(combo1, 18);
        AppUI.setBackground(combo1, background);
        AppUI.setColor(combo1, AppUI.getDisabledColor2());
        combo1.setUI(new ColorArrowUI());
        
        Object o = combo1.getAccessibleContext().getAccessibleChild(0);
        if (o instanceof JComponent) {
            JComponent c = (JComponent) o;
            c.setForeground(foreground);
            c.setBackground(background);
       //     AppUI.setSize(combo1, 300, 100);  
        }
    
        
        
        combo1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
    
        combo1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
            }
        });
  
        xComboBoxRenderer renderer = new xComboBoxRenderer(combo1, background);
        combo1.setRenderer(renderer);
        
         for (int i = 0; i < options.length; i++)
            combo1.addItem(options[i]);
     

        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);
        p.add(combo1);

        return p;
    }
}


class xColorArrowUI extends BasicComboBoxUI {
    @Override 
    protected JButton createArrowButton() {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(false);
        AppUI.setHandCursor(button);
        AppUI.setMargin(button, 0);

        try {
            Image img = ImageIO.read(getClass().getClassLoader().getResource("resources/arrow.png"));
            button.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
    
        }

        return button;
    }
}

class xComboBoxRenderer extends JPanel implements ListCellRenderer {
    JPanel textPanel;
    JLabel text;
    Color color;

    public xComboBoxRenderer(JComboBox combo, Color color) {
        this.color = color;

        
        
        text = new JLabel();
        text.setOpaque(true);
 
        AppUI.setMargin(text, 10, 10, 10, 10);
    }


    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        AppUI.setFont(text, 18);
        if (value != null)
            text.setText(value.toString());
        if (index == 0) {
            text.setForeground(AppUI.getDisabledColor2());
        } else if (index > -1) {
            text.setForeground(Color.BLACK);
        }
            
        if (isSelected) {
            text.setBackground(AppUI.getColor9());
        } else {
            text.setBackground(color);
        }
        
        AppUI.setSize(text, 100, 10);
        System.out.println("sdfsdf");
        //    AppUI.setSize(text, (int) AppUI.getBoxWidth(), (int) AppUI.getBoxHeight());
        
        
        
        return text;
    }
}
