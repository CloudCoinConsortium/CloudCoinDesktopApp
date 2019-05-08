/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 *
 * @author Александр
 */
public class RoundedBorder implements Border {

    private int radius;
    private Color color;
    UICallBack cb;

    RoundedBorder(int radius) {
        this.radius = radius;
        this.color = new Color(0xFFFFFF);
    }

    RoundedBorder(int radius, Color color) {
        this.radius = radius;
        this.color = color;
    }
    
    RoundedBorder(int radius, Color color, UICallBack cb) {
        this.radius = radius;
        this.color = color;
        this.cb = cb;
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }


    public boolean isBorderOpaque() {
        return true;
    }

    public void setColor(Color color) {
        System.out.println("xcolor" + color);
        this.color = color;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g2.setColor(this.color);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x, y, width-1, height-1, radius-1, radius-1);
        g2.setColor(this.color);
        g2.fillRoundRect(x+1, y+1, width -2, height-2, radius, radius);
 
        if (cb != null)
            cb.doWork(g2, (JComponent) c);
        
    }
}
