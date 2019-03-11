/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

/**
 *
 * @author Alexander
 */
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class DrawRoundRectangle extends JComponent {
    @Override
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

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rounded Rectangle Demo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(new DrawRoundRectangle(), BorderLayout.CENTER);
        frame.pack();
        frame.setSize(420, 300);
        frame.setVisible(true);
    }
}
