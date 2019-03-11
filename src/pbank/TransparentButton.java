/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 *
 * @author Александр
 */
public class TransparentButton extends JButton {
    
    String text;
    ImageIcon u;
    
    int iwidth, iheight;
    
    int width, height;
    
    int fwidth;
    
    public TransparentButton(String text) { 
	    super(text);
	    setOpaque(false); 
    } 
        
    public TransparentButton(String name, ImageIcon u) {
        super(name);
        this.u = u;
        
        iwidth = u.getImage().getWidth(this);
        iheight = u.getImage().getHeight(this);
                
     
	setOpaque(false); 
    }
	    
    public void setText(String text) {
        this.text = text;
    }
    
    public void paint(Graphics g) { 
        Graphics2D g2 = (Graphics2D) g.create(); 
        
        RenderingHints qualityHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON );
        
        qualityHints.put(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY );
        
        qualityHints.put(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        
        g2.setRenderingHints(qualityHints);
        
        width = getWidth();
        height = getHeight();
        
       // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); 
        
        g2.setColor(Color.WHITE);
        int radius = 80;
           
        g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);
        g2.setColor(Color.RED);
        
        Font font = new Font("default", Font.BOLD, 14);
        
        g2.setFont(font);
        
        FontMetrics metrics = g2.getFontMetrics(font);
        int fheight = metrics.getHeight();
        int fwidth = metrics.stringWidth(text);
        
        
        g2.drawString(text, width/2 - fwidth /2, height/2 + (int)(iheight/1.3));
        g2.drawImage(u.getImage(), width/2 - iwidth/2, height/2 - iheight/2 - (int)(iheight/4.5), this);
        super.paint(g2); 
        g2.dispose(); 
    } 

}
