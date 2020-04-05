/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

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
 * @author Alexander
 */
public class DashedBorder implements Border {
    private int radius;
    private Color color;


    DashedBorder(int radius, Color color) {
        this.radius = radius;
        this.color = color;
    }


    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }


    public boolean isBorderOpaque() {
        return true;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float dash1[] = { 10.0f };
        final BasicStroke dashed = new BasicStroke(5.0f, BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_MITER, 10.0f, dash1, 0.0f);
        
        // final BasicStroke dashed = new BasicStroke(3.0f, BasicStroke.CAP_SQUARE, 
        //        BasicStroke.JOIN_ROUND, 1.0f, dash1, 0.0f);
        
        g2.setColor(AppUI.brand.getDropfilesBackgroundColor());
        g2.fillRoundRect(x, y, width, height, radius, radius);
        
        g2.setColor(this.color);
        g2.setStroke(dashed);
        g2.drawRoundRect(x, y, width-1, height-1, radius, radius);  
        
        
       
        
    }
}
