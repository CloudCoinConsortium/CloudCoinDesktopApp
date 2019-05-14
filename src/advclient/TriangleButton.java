/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.swing.JButton;

/**
 *
 * @author Alexander
 */
class TriangleButton extends JButton {
    private boolean isReversed;
    private Shape triangle;
    
    public TriangleButton(boolean isReversed) {
        super();
        this.isReversed = isReversed;
        this.triangle = createTriangle();
    }
  

    public void paintBorder( Graphics g ) {
        ((Graphics2D)g).draw(triangle);
    }
    public void paintComponent( Graphics g ) {
        g.setColor(Color.WHITE);
        ((Graphics2D)g).fill(triangle);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(20, 18);
    }
   
    public boolean contains(int x, int y) {
        return triangle.contains(x, y);
    }

    private Shape createTriangle() {
        Polygon p = new Polygon();
        
        if (isReversed) {
            System.out.println("rev");
            p.addPoint( 4 ,4  );
            p.addPoint( 12 , 4   );
            p.addPoint( 8   , 12 );
            
        } else {
            p.addPoint( 4   , 12 );
            p.addPoint( 8 , 4   );
            p.addPoint( 12 ,12  );
        }
        
        return p;
    }
}
