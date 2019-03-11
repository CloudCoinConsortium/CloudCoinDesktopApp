/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Александр
 */
public class AppUI {
    
    int tw, th;
    
    public AppUI(int tw, int th) {
        this.tw = tw;
        this.th = th;
    }
    
    public Color getOurColor() {
        return new Color(52, 142, 251);
    }
    
    public Color getDisabledColor() {
        return new Color(0x777777);
    }
    
    public void setSize(Component c, int w, int h) {
        c.setPreferredSize(new Dimension(w, h));
        c.setMinimumSize(new Dimension(w, h));
        c.setMaximumSize(new Dimension(w, h));
    }
    
    public void align(JPanel c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
    
    public JButton getMainButton(String res, String name) {
        URL u = getClass().getClassLoader().getResource("resources/" + res);
        
        ImageIcon Flag = new ImageIcon(u);
        TransparentButton button = new TransparentButton(name, Flag);
        button.setMaximumSize(new Dimension(tw/2, th/5));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setActionCommand(name);
        button.setText(name);
        
        return button;
    }
    
    public Component hr(int size) {
        return Box.createRigidArea(new Dimension(0, size));
    }
    
    public JLabel getJLabel(String text) {
        JLabel jlabel = new JLabel(text);
        
        jlabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        jlabel.setFont(new Font("Verdana", 1, 12));
        jlabel.setForeground(Color.WHITE);
        
        return jlabel;
    }
    
    public JLabel getJLabel(String text, int type, int size) {
        JLabel jlabel = new JLabel(text);
        
        jlabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        jlabel.setFont(new Font("Verdana", type, size));
        jlabel.setForeground(Color.BLACK);
        
        return jlabel;
    }
    
    
    public JDialog getDialog(JFrame frame, String name, int height) {
        JDialog dialog = new JDialog(frame, name, true);
        
        dialog.setLayout(new BorderLayout());
        dialog.setSize(new Dimension(300, height));
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(frame);
        dialog.setResizable(false);
        
        return dialog;
    }
    
    public JFrame getMainFrame() {
        JFrame frame = new JFrame();
        
        frame.setTitle("CloudCoin Bank");
        frame.setLayout(new BorderLayout());
        //frame.add(mainPanel, BorderLayout.CENTER);
        frame.setSize(new Dimension(tw, th));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        
        return frame;
    }
    
    public Component getDenomPart(int c, int denom) {
        JPanel s = new JPanel();
        
        s.setLayout(new BoxLayout(s, BoxLayout.PAGE_AXIS));
        s.setMinimumSize(new Dimension(36, 70));
        s.setPreferredSize(new Dimension(36, 70));
        s.setMaximumSize(new Dimension(36, 70));
        
        s.setBackground(Color.WHITE);
               
        JLabel j;
        
        j = new JLabel("" + c);
        
        j.setAlignmentX(Component.CENTER_ALIGNMENT);
        j.setAlignmentY(Component.TOP_ALIGNMENT);
        j.setFont(new Font("Verdana", Font.BOLD, 14));
        j.setForeground(getOurColor());
        s.add(j);
        
        s.add(hr(25));
        
        j = new JLabel(denom + "s");
        j.setMinimumSize(new Dimension(100, 25));
        j.setAlignmentX(Component.CENTER_ALIGNMENT);
        j.setAlignmentY(Component.TOP_ALIGNMENT);
        j.setFont(new Font("Verdana", Font.BOLD, 10));
        j.setForeground(Color.BLACK);
        s.add(j);
        
        return s;
    }
    
    public Component getEDenomPart(int c, int denom) {
        JPanel s;
        
        s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.PAGE_AXIS));
        s.setBackground(Color.WHITE);
        setSize(s, 40, 170);
           
        JLabel j;       
        j = getJLabel(denom + "s", Font.BOLD, 9);    
        s.add(j);

        SpinnerModel model = new SpinnerNumberModel(0, 0, c, 1);
        JSpinner spinner = new JSpinner(model);
        setSize(spinner, 40, 40);
        spinner.setFont(new Font("Verdana", Font.PLAIN, 14));
        spinner.setName("sp" + denom);
        
        Component mySpinnerEditor = spinner.getEditor();
        JFormattedTextField jftf = ((JSpinner.DefaultEditor) mySpinnerEditor).getTextField();
        jftf.setColumns(4);
        
        s.add(spinner);
        
        // of total
        j = getJLabel("of " + c, Font.BOLD, 9);
        j.setForeground(getDisabledColor());
        s.add(j);
        
        s.add(hr(25));
            
        return s;
    }
    
    public Component getByName(Container basic, String name) {
        if (basic.getComponents().length == 0)
            return null;
        
        for (Component c : basic.getComponents()) {
            if(name.equals(c.getName())) 
                return c;
                
            if (c instanceof JRootPane) {
                JRootPane nestedJRootPane = (JRootPane) c;
                Component cnested = getByName(nestedJRootPane.getContentPane(), name);
                if (cnested != null)
                    return cnested;
            }
  
            if (c instanceof Container) {
                Component cnested = getByName((Container) c, name);
                if (cnested != null)
                    return cnested;
            }
        }
        
        return null;
    }
    
    public void updateScreen(JDialog d, Component oldP, Component newP) {
        d.remove(oldP); 
        d.add(newP);                
        d.invalidate();
        d.validate();
        d.repaint();
        d.setVisible(true);
    }
    
    
    public JButton getCommonButton(String text, String actionCommand) {
        JButton jb = new JButton();
        
        jb.setText(text);
        jb.setAlignmentX(Component.CENTER_ALIGNMENT);
        jb.setBackground(getOurColor());
        jb.setOpaque(true);
        jb.setForeground(Color.WHITE);
        jb.setBorderPainted(false);
        setSize(jb, 120, 32);
        jb.setBorder(BorderFactory.createEmptyBorder());
        jb.setActionCommand(actionCommand);
        
        return jb;
    }
    
    public JButton getSwitchButton(String text, String actionCommand, Color color) {
        JButton b = new JButton();
        
        b.setBackground(color);
        JLabel jl = new JLabel(text);
        jl.setForeground(Color.WHITE);
        jl.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.add(jl);
        b.setActionCommand(actionCommand);
        b.setBorder(null);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        return b;
    }
    
    public JTextArea getFolderTA(String path) {
        JTextArea ja = new JTextArea(50, 50);
        
        ja.setText(path);
        ja.setEditable(false);
        //j = new JLabel("<html><p>c:\\documents\\sasdfas\\fsafas\\dsadasdas\\dasdasdas\\file.txt</p><html>");
        ja.setAlignmentX(Component.CENTER_ALIGNMENT);
        ja.setAlignmentY(Component.TOP_ALIGNMENT);
        ja.setFont(new Font("Verdana", Font.BOLD, 10));
        ja.setForeground(Color.BLACK);
        ja.setWrapStyleWord(true);
        ja.setLineWrap(true);
        ja.setBorder(null);
        
        return ja;
    }
    
    public void showMessage(JFrame frame, String text) {
        JOptionPane jo = new JOptionPane();
        
        jo.showMessageDialog(frame, text, "CloudBank", JOptionPane.WARNING_MESSAGE); 
    }
    
    
}
