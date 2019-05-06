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
import java.awt.geom.RoundRectangle2D;
import javax.imageio.ImageIO;
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
     private static final Color BACKGROUND = Color.WHITE;
  private static final Color FOREGROUND = new Color(0x5FA8FF);
    public JPanel makeUI(AppUI ui) {
      
        
        JRadioButton radio = new JRadioButton();
   
        
         int w = 360;
            int h =50;
     //  radio.setPreferredSize(new Dimension(w, h));
       // radio.setMinimumSize(new Dimension(w, h));
        //radio.setMaximumSize(new Dimension(w, h));
        
        // radio.setUI(new myUI());
        
        
         try {
    Image img = ImageIO.read(getClass().getClassLoader().getResource("resources/radio1.png"));
    radio.setIcon(new ImageIcon(img));
  } catch (Exception ex) {
    System.out.println(ex);
  }
        
         
         
          JRadioButton radio2 = new JRadioButton();
   
       
     //  radio2.setPreferredSize(new Dimension(w, h));
     //   radio2.setMinimumSize(new Dimension(w, h));
    //    radio2.setMaximumSize(new Dimension(w, h));
        
        // radio.setUI(new myUI());
        
        
         try {
    Image img = ImageIO.read(getClass().getClassLoader().getResource("resources/radio.png"));
    radio2.setIcon(new ImageIcon(img));
  } catch (Exception ex) {
    System.out.println(ex);
  }
         
         
         
         
         
         
         
         
         
         
         
         
         
         radio.setOpaque(false);
           radio2.setOpaque(false);
         
         
         
         
         
      
         JPanel p = new JPanel(new FlowLayout());
    ui.setSize(p, w + 120, h + 10);
    p.add(radio);
    p.add(radio2);
   // p.add(combo2);
    p.setOpaque(false);
    p.setBackground(new Color(0x775FA8FF));
    
    return p;
    }
}
class myUI extends BasicRadioButtonUI {
     public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setPaint(Color.RED);
        g2.setStroke(new BasicStroke(2.0f));

        double x = 50;
        double y = 50;
        double w = x + 250;
        double h = y + 100;
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 50, 50));
    }

}