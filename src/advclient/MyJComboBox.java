/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Graphics;
import javax.swing.JComboBox;

/**
 *
 * @author Alexander
 */
public class MyJComboBox extends JComboBox {
    public void paintComponent(Graphics g) {
        System.out.println("ddd");
    super.paintComponent(g);

    // Draw the background image.
    
    
    
    //Graphics2D g2d = (Graphics2D) g;
    //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
    //g2d.setColor(Color.yellow);
        //g2d.fillOval(point.x, point.y, 120, 60);
    //g.drawImage(backgroundImage, 0, 0, this); 
  }
    
}
