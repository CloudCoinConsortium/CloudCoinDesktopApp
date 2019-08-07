/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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
    Color color;
    
    public MyButton(String text) {
        this.color = AppUI.getColor2();
        core = makeUI(text);
    }
    
    public MyButton(String text, Color color) {
        this.color = color;
        core = makeUI(text);
        
    }
    
    public JPanel getButton() {
        return core;
    }
    
    public void addListener(ActionListener a) {
        button.addActionListener(a);
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
        rb.setColor(this.color);
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
        
        AppUI.roundCorners(button, this.color, 48, cb);
        AppUI.noOpaque(button);
        button.setContentAreaFilled(false);
        AppUI.setSize(button, 190, 48);
        AppUI.setFont(button, 18);
        AppUI.setHandCursor(button);
 
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);

        p.add(button);
            
        return p;       
    }
}