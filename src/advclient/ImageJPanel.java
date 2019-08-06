/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
/**
 *
 * @author Alexander
 */
public class ImageJPanel extends JPanel {
    
    
  private Image backgroundImage;
  private Object point;
  int width, height;
  double ratio;

  // Some code to initialize the background image.
  // Here, we use the constructor to load the image. This
  // can vary depending on the use case of the panel.
  public ImageJPanel(FlowLayout fl, String res)  {
      super(fl);
      init(res);
  }
  
  public ImageJPanel(BorderLayout bl, String res)  {
      super(bl);
      init("resources/" + res);
  }
  
  public ImageJPanel(String res)  {
      super();
      init("resources/" + res);
  }
  
  public void init(String res) {
      URL u = getClass().getClassLoader().getResource(res);

      try {
        backgroundImage = ImageIO.read(u);
        BufferedImage bimg = (BufferedImage) backgroundImage;
      
        height = bimg.getHeight();
        width = bimg.getWidth();
        
        ratio = (double) width / (double) height;
      }catch (IOException e) {
          System.out.println("error");
      }
  }

  public int getImageWidth() {
      return width;
  }
  
  public int getImageHeight() {
      return height;
  }
  
  public void setImageSize(int x) {
      int newHeight = (int) (x / ratio);

      width = x;
      height = newHeight;
      
      setImageSize(x, newHeight);
  }
  
  public void setImageSize(int x, int y) {
      backgroundImage = backgroundImage.getScaledInstance(x, y, Image.SCALE_SMOOTH);
  }
  
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Draw the background image.
    
    
    
    //Graphics2D g2d = (Graphics2D) g;
    //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    //g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
    //g2d.setColor(Color.yellow);
        //g2d.fillOval(point.x, point.y, 120, 60);
    g.drawImage(backgroundImage, 32, 306, this); 
  }
}
