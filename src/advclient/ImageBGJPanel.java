/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 *
 * @author Александр
 */
public class ImageBGJPanel extends JPanel {
    private Image backgroundImage;
    private Object point;
    int width, height;
    double ratio;
  
    public ImageBGJPanel(String res) {
        super();
        URL u = getClass().getClassLoader().getResource(res);
        try {
            backgroundImage = ImageIO.read(u);
            BufferedImage bimg = (BufferedImage) backgroundImage;
      
            height = bimg.getHeight();
            width = bimg.getWidth();
        
            ratio = (double) width / (double) height;
        
            System.out.println("w="+width+ " h="+height + " r="+ratio);
        }catch (IOException e) {
            System.out.println("error");
        }
    }
    public void setImageSize(int x, int y) {
        backgroundImage = backgroundImage.getScaledInstance(x, y, Image.SCALE_SMOOTH);
    }
    
    public void setImageSize(int x) {
      int newHeight = (int) (x / ratio);
              
      System.out.println("xxx="+x+ " v="+ratio+" vv="+newHeight+ " h="+height);
      
      width = x;
      height = newHeight;
      
      setImageSize(x, newHeight);
  }
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

    // Draw the background image.
    
    
    
    //Graphics2D g2d = (Graphics2D) g;
    //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
    //g2d.setColor(Color.yellow);
        //g2d.fillOval(point.x, point.y, 120, 60);
        g.drawImage(backgroundImage, 0, 0, this);
    
  }
}
