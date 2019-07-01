/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 *
 * @author Alexander
 */
public class FancyProgressBar extends BasicProgressBarUI {
    int w, h;
    
  /*  
    @Override
    protected Dimension getPreferredInnerVertical() {
        return new Dimension(h, w);
    }

    @Override
    protected Dimension getPreferredInnerHorizontal() {
        return new Dimension(w, h);
    }
*/
    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int iStrokWidth = 1;
        g2d.setColor(AppUI.getColor4());

        int width = progressBar.getWidth();
        int height = progressBar.getHeight();

        RoundRectangle2D outline = new RoundRectangle2D.Double(0, 0, width - 1, height - 1, 0, 0);
        g2d.fill(outline);

        double dProgress = progressBar.getPercentComplete();
        if (dProgress < 0) {
            dProgress = 0;
        } else if (dProgress > 1) {
            dProgress = 1;
        }

        width = (int) Math.round(width * dProgress);

        g2d.setColor(AppUI.getColor5());
        RoundRectangle2D fill = new RoundRectangle2D.Double(0, 0, width, height - 1, 0, 0);

        g2d.fill(fill);

        g2d.dispose();
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        super.paintIndeterminate(g, c); 
    }

}
