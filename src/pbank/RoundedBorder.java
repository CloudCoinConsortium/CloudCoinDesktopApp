/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.border.Border;

/**
 *
 * @author Александр
 */
public class RoundedBorder implements Border {

    private int radius;


    RoundedBorder(int radius) {
        this.radius = radius;
    }


    public Insets getBorderInsets(Component c) {
        return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius);
    }


    public boolean isBorderOpaque() {
        return true;
    }


    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setStroke(new BasicStroke(10));
     //   g2.setColor(Color.BLACK);
       // g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
        System.out.println("x2");
        //g2.setColor(Color.WHITE);
        //g2.fillRoundRect(x, y, width-1, height-1, radius-5, radius-5);
    }
}
