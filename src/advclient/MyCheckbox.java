/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author Alexander
 */
public class MyCheckbox {
    
      public JPanel makeUI(AppUI ui) {
           int w = 50;
        int h =50;
        
          JCheckBox cb = new JCheckBox();
          
           try {
    Image img = ImageIO.read(getClass().getClassLoader().getResource("resources/radio.png"));
    cb.setIcon(new ImageIcon(img));
  } catch (Exception ex) {
    System.out.println(ex);
  }
           cb.setOpaque(false);
        cb.addItemListener(new ItemListener() {

    public void itemStateChanged(ItemEvent e) {
        
         try {
    Image img = ImageIO.read(getClass().getClassLoader().getResource("resources/radio1.png"));
    cb.setIcon(new ImageIcon(img));
  } catch (Exception ex) {
    System.out.println(ex);
  }
        
        System.err.println(e.getStateChange());
    }
});
      
  // cb.setBorder(new RoundedBorder(5));
            JPanel p = new JPanel(new FlowLayout());
    ui.setSize(p, w + 120, h + 10);
    //p.add(radio);
    p.add(cb);
   // p.add(combo2);
    p.setOpaque(false);
   // p.setBackground(new Color(0x775FA8FF));
    
    return p;
      }
      
          
}


