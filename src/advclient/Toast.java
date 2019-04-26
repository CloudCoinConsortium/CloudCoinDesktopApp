/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;

/**
 *
 * @author 
 */
public class Toast {
    
    String s; 
  
    // JWindow 
    JWindow w; 
  
   
    public Toast(String s, JFrame f) { 
        final String fs = s;
        w = new JWindow(); 
      
  
        // make the background transparent 
        w.setBackground(new Color(0,0,0,0)); 
  
        // create a panel 
        JPanel p = new JPanel() { 
            public void paintComponent(Graphics g) { 
                int wid = g.getFontMetrics().stringWidth(fs); 
                int hei = g.getFontMetrics().getHeight(); 
  
                //System.out.println("x="+wid+ " h="+hei);
                // draw the boundary of the toast and fill it 
                g.setColor(new Color(0x555555)); 
                g.fillRect(10, 10, wid + 30, hei + 10); 
                //g.setColor(Color.black); 
                //g.drawRect(10, 10, wid + 30, hei + 10); 
  
                // set the color of text 
                g.setColor(new Color(255, 255, 255, 240)); 
                g.drawString(fs, 25, 27); 
                int t = 250; 
                
                // draw the shadow of the toast
                for (int i = 0; i < 4; i++) { 
                    t -= 60; 
                    g.setColor(new Color(0, 0, 0, t)); 
                    g.drawRect(10 - i, 10 - i, wid + 30 + i * 2, 
                               hei + 10 + i * 2); 
                } 
            } 
        }; 
  
        w.add(p); 
        
        w.setSize(180, 50);
        w.setLocationRelativeTo(f);
        
        
        //w.setLocation(0, 0); 
        //f.add(w);
       
    } 
  
    // function to pop up the toast 
    void showtoast() { 
        
        try { 
            w.setOpacity(1); 
            w.setVisible(true); 
  
            // wait for some time 
            Thread.sleep(1000); 
  
            // make the message disappear  slowly 
            for (double d = 1.0; d > 0.2; d -= 0.1) { 
                Thread.sleep(100); 
                w.setOpacity((float)d); 
            } 
  
            // set the visibility to false 
            w.setVisible(false); 
           
        } 
        catch (Exception e) { 
            System.out.println(e.getMessage()); 
        } 
    } 
}
