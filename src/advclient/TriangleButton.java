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
    private Shape triangle = createTriangle();

    public void paintBorder( Graphics g ) {
        ((Graphics2D)g).draw(triangle);
    }
    public void paintComponent( Graphics g ) {
        g.setColor(Color.WHITE);
        ((Graphics2D)g).fill(triangle);
    }
    public Dimension getPreferredSize() {
        return new Dimension(20,10);
    }
    public boolean contains(int x, int y) {
        return triangle.contains(x, y);
    }

    private Shape createTriangle() {
        Polygon p = new Polygon();
        p.addPoint( 0   , 10 );
        p.addPoint( 10 , 0   );
        p.addPoint( 20 ,10  );
        return p;
    }
}
