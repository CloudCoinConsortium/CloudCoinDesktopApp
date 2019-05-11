
package advclient;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.accessibility.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.*;

public class MyTextField extends JTextField {
    
    
    JPanel core;
    JButton button;
    ImageIcon imgEye;
    RoundedTextField tf;
    
    boolean isPassword;
    
    public MyTextField(String placeholder) {
        isPassword = true;
        core = makeUI(placeholder);
    }
    
    public MyTextField(String placeholder, boolean isPassword) {
        this.isPassword = isPassword;
        core = makeUI(placeholder);
    }
    
    public JPanel getTextField() {
        return core;
    }
    
    public JPanel makeUI(String placeholder) {
        tf = new RoundedTextField(placeholder);

        AppUI.noOpaque(tf);
        AppUI.setSize(tf, AppUI.tw / 3, AppUI.th / 16);
        AppUI.setBackground(tf, AppUI.getColor4());
        AppUI.setFont(tf, 18);
        AppUI.setMargin(tf, 10);
        tf.setLayout(new BorderLayout());
        
        final RoundedTextField ftf = tf;
        tf.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                ftf.clearPlaceholder();
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (ftf.getText().isEmpty()) {
                    ftf.restorePlaceholder();
                }
            }
        });
        
        if (isPassword) {
            tf.hide();        
            try {
                Image img;
            
                img = ImageIO.read(getClass().getClassLoader().getResource("resources/eye.png"));       
                imgEye = new ImageIcon(img);
            } catch (Exception ex) {

            }        
            
            JLabel label = new JLabel(imgEye);
            tf.add(label, BorderLayout.EAST);
        
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ftf.toggleHide();
                }
            });

            AppUI.setHandCursor(label);
        }
        
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);

        p.add(tf);
        
        return p;
    }
    
    public String getText() {
        return String.valueOf(tf.getPassword());
    }
    
}