/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Alexander
 */
public class MyCheckBox {
    
    Icon imgUnchecked, imgChecked;
    boolean isChecked;
    
    JPanel core;
    JCheckBox cb;
    JLabel txt;
    
    public MyCheckBox(String text) {
        core = makeUI(text);
    }
    
    public JPanel getCheckBox() {
        return core;
    }
    
    public void addListener(ItemListener i) {
        cb.addItemListener(i);
    }
    
    public boolean isChecked() {
        return cb.isSelected();
    }
    
    public void setFont(int size, Color color) {
        AppUI.setFont(txt, size);
        AppUI.setColor(txt, color);
    }
    
    public void setBoldFont() {
        AppUI.setBoldFont(txt, 18);
    }
    
    public void setBoldFontSize(int size) {
        AppUI.setBoldFont(txt, size);
    }
    
    public JPanel makeUI(String text) {
        cb = new JCheckBox();
        
        try {
            Image img;
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/checkbox.png"));
            imgUnchecked = new ImageIcon(img);
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/checkboxChecked.png"));
            imgChecked = new ImageIcon(img);
        } catch (Exception ex) {

        }
        
        AppUI.noOpaque(cb);
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Icon img;

                img = (e.getStateChange() != ItemEvent.SELECTED) ? imgUnchecked : imgChecked;                     
                cb.setIcon(img);
            }
        });
        
        cb.setIcon(imgUnchecked);
        
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);
        
        p.add(cb);
        
        AppUI.vr(p, 20);
        
        txt = new JLabel(text);
        AppUI.alignLeft(txt);
        AppUI.setFont(txt, 18);
 
        p.add(txt);
    
        return p;
    }      
}


