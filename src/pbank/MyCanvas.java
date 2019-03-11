/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;
/**
 *
 * @author Александр
 */  

class MyCanvas extends JComponent {
  public void paint(Graphics g) {

    Graphics2D g2d = (Graphics2D) g;

    float[] dash1 = { 2f, 0f, 2f };

    Color original = g.getColor();
    g.setColor(new Color(52, 142, 251));

// your drawings stuff

    
    
   // g2d.drawLine(20, 40, 250, 40);

    BasicStroke bs1 = new BasicStroke(1, 
        BasicStroke.CAP_BUTT, 
        BasicStroke.JOIN_ROUND, 
        1.0f, 
        dash1,
        2f);
    g2d.setStroke(bs1);
    g2d.drawLine(0, 0, 350, 0);
    g.setColor(original);
  }

}
