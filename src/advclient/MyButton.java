/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Alexander
 */
public class MyButton {
    
    JPanel core;
    JButton button;
    boolean lastActionAdded;
    
    public MyButton(String text) {
        core = makeUI(text);
        this.lastActionAdded = false;
        if (!text.toLowerCase().equals("continue"))
            this.lastActionAdded = true;
    }
    
    public JPanel getButton() {
        return core;
    }
    
    public void addListener(ActionListener a) {
        button.addActionListener(a);     
        if (!this.lastActionAdded) {  
            this.lastActionAdded = true;
            addListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    disable();
                } 
            });
        }    
    }
    
    public void disable() {
        button.setEnabled(false);
        RoundedBorder rb = (RoundedBorder) button.getBorder();
        rb.setColor(AppUI.getDisabledColor());
        button.repaint();
        button.revalidate();
    }
    
    public void enable() {
        button.setEnabled(true);
        RoundedBorder rb = (RoundedBorder) button.getBorder();
        rb.setColor(AppUI.getColor0());
        button.repaint();
        button.revalidate();
    }
    
    
    private JPanel makeUI(String text) {
        button = new JButton(text);
    
        final String ftext = text;
        UICallBack cb = new UICallBack() {
            public boolean doWork(Graphics g, JComponent c) {
                int width = g.getFontMetrics().stringWidth(ftext);
                int cWidth = c.getWidth();
                int cHeight = c.getHeight();
                              
                int x = (cWidth - width) / 2;
                g.setColor(Color.WHITE);     
                g.drawChars(ftext.toCharArray(), 0, ftext.length(), x, cHeight/2 + 6);
                
                return true;
            }
        };
        
        AppUI.roundCorners(button, AppUI.getColor0(), 18, cb);
        AppUI.noOpaque(button);
        button.setContentAreaFilled(false);
        AppUI.setSize(button, 224, 50);
        AppUI.setBoldFont(button, 20);
        AppUI.setHandCursor(button);
 
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);

        p.add(button);

        return p;       
    }
}