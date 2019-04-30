
package advclient;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import javax.accessibility.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.*;

public class RoundedCornerInput extends JTextField {
    private Shape shape;
    public RoundedCornerInput(int size) {
        
        super(size);
        setOpaque(false); // As suggested by @AVD in comment.
        
        
         int w = 360;
            int h = 50;
       setPreferredSize(new Dimension(w, h));
       setMinimumSize(new Dimension(w, h));
       setMaximumSize(new Dimension(w, h));
       
       setForeground(new Color(0x5FA8FF));
         try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("resources/Montserrat-Regular.otf"));
            setFont(font.deriveFont(Font.PLAIN, 18f));
           setForeground(Color.BLACK);
        } catch(Exception e){
            System.out.println("xxxxxx " + e.getMessage());
        }
    setBorder(BorderFactory.createCompoundBorder(
        getBorder(), 
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
    }
    protected void paintComponent(Graphics g) {
         g.setColor(getBackground());
         g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
         super.paintComponent(g);
    }
    protected void paintBorder(Graphics g) {
         g.setColor(new Color(0x5FA8FF));
         g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
    }
    public boolean contains(int x, int y) {
         if (shape == null || !shape.getBounds().equals(getBounds())) {
             shape = new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 15, 15);
         }
         return shape.contains(x, y);
    }
}