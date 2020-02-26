/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

/**
 *
 * @author Alexander
 */
public class MyRadioButton {

  
    Icon imgUnchecked, imgChecked;
    boolean isChecked;
    
    JPanel core;
    JRadioButton rb;
  
    
    public MyRadioButton() {
        core = makeUI();
    }
    
    public JPanel getRadioButton() {
        return core;
    }
    
    public void addListener(ItemListener i) {
        rb.addItemListener(i);
    }
    
    public void attachToGroup(ButtonGroup bg) {
        bg.add(rb);
    }
  
    public boolean isSelected() {
        return rb.isSelected();
    }
    
    public void select() {
        rb.setIcon(imgChecked);
        rb.setSelected(true);
    }
    
    public void deselect() {
        rb.setIcon(imgUnchecked);
        rb.setSelected(false);
    }
    
    public JPanel makeUI() {              
        rb = new JRadioButton();
        AppUI.noOpaque(rb);

        try {
            Image img;
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/radiodark.png"));
            imgUnchecked = new ImageIcon(img);
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/radiodarkChecked.png"));
            imgChecked = new ImageIcon(img);
        } catch (Exception ex) {

        }
        
        deselect();    
        rb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Icon img;

                img = (e.getStateChange() != ItemEvent.SELECTED) ? imgUnchecked : imgChecked;                     
                rb.setIcon(img);
            }
        });
         
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);
        
        p.add(rb);
         
         
        return p;
    }
}
