
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 *
 * @author Alexander Miroch
 */
public class RoundedTextField extends JPasswordField {
    private Shape shape;
    String placeholder, savedPlaceholder;
    
    public RoundedTextField(String placeholder) {
        
        super();
        this.placeholder = this.savedPlaceholder = placeholder;
        show();    
    }
    
    public void setPlaceholder(String placeholder) {
        this.savedPlaceholder = placeholder;
    

        if (!this.placeholder.equals("")) 
            this.placeholder = placeholder;
            
        repaint();
        revalidate();
    }
    
    public void hide() {
        setEchoChar('*');        
    }
    
    public void show() {      
        setEchoChar((char)0);
    }
    
    public boolean isHidden() {
        return getEchoChar() == '*';
    }
    
    public void toggleHide() {
        if (isHidden())
            show();
        else
            hide();
    }
    
    public void clearPlaceholder() {
        this.placeholder = "";
    }
    
    public void restorePlaceholder() {
        this.placeholder = this.savedPlaceholder;
        repaint();
        revalidate();
    }
    
    protected void paintComponent(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       // g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 42, 42);
        if (!placeholder.equals("")) {
            g.setColor(AppUI.getDisabledColor2());        
            int width = g.getFontMetrics().stringWidth(placeholder);
            int cWidth = getWidth();
            int cHeight = getHeight();                      
            int x = (cWidth - width) / 2;
            x = 8;

            g.drawString(placeholder, x, cHeight/2 + 6);
        }
        
        super.paintComponent(g);     
    }
    /*
    protected void paintBorder(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
    }
    */
    
    public boolean contains(int x, int y) {
         if (shape == null || !shape.getBounds().equals(getBounds())) {
             shape = new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 42, 42);
         }
         return shape.contains(x, y);
    }
    
}
