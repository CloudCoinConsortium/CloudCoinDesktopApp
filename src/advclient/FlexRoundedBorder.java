/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author Alexander
 */
public class FlexRoundedBorder extends AbstractBorder {
     @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)      {
    Graphics2D g2 = (Graphics2D)g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,               RenderingHints.VALUE_ANTIALIAS_ON);
    int r = height;
    RoundRectangle2D round = new RoundRectangle2D.Float(x, y, width, height, r+5, r+20);
    Container parent = c.getParent();
    if(parent!=null) {
    g2.setColor(new Color(213, 208, 209, 150));
    Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
    corner.subtract(new Area(round));
    g2.fill(corner);
    }
    g2.setColor(Color.BLACK);
   // g2.draw(round);
    g2.dispose();
    }
    @Override public Insets getBorderInsets(Component c) {
      return new Insets(3, 8, 2, 8);
    }
     @Override public Insets getBorderInsets(Component c, Insets insets) {
     insets.left = insets.right = 8;
      insets.top = insets.bottom = 2;
     return insets;
     }
}
