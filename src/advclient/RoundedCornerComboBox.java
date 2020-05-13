
package advclient;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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

public class RoundedCornerComboBox {
    
    public Color outerBgColor;
    public Color background;
    public Color foreground;
    
    String[] options;
    String placeholder;
    
    
    JPanel core;
    JComboBox<String> combo1;
    MyComboBoxModel cbm;
    ComboBoxRenderer renderer;

    
    public RoundedCornerComboBox(Color outerBgColor, String placeholder, String[] options) {
        this.outerBgColor = outerBgColor;
        this.placeholder = placeholder;
        this.options = options;
        this.foreground = Color.WHITE;
        
        core = makeUI();
    }
    
    public void setDefault(String value) {
        if (options.length == 1) {
            combo1.setSelectedIndex(1);
            return;
        }
        
        if (value != null)
            combo1.setSelectedItem(value);
        
    }

    public void setDefaultIdx(int idx) {
        combo1.setSelectedIndex(idx);
        return;
    }
    
    public JPanel getComboBox() {
        return core;
    }
    
    public String getSelectedValue() {
        return String.valueOf(combo1.getSelectedItem());
    }
    
    public int getSelectedIndex() {
        return combo1.getSelectedIndex();
    }
    
    public void addActionListener(ActionListener al) {
        combo1.addActionListener(al);
        
        setDefault(null);
    }
    
    public void setOptions(String[] options) {
        this.options = options;
        
        combo1.removeAllItems();
        combo1.addItem(placeholder);
        for (int i = 0; i < options.length; i++)
            combo1.addItem(options[i]);
        
        cbm.drop();
        combo1.revalidate();
        combo1.repaint();
    }
    
    public void addOption(String option) {
        int i;
        
        String[] nopts = new String[options.length + 1];
        for (i = 0; i < options.length; i++)
            nopts[i] = options[i];
        
        nopts[i] = option;
        this.options = nopts;
        
        combo1.addItem(option);
    }
    
    public void setEditable() {
        combo1.setEditable(true);

    }
    
    public String getCustomValue() {
        return (String) combo1.getEditor().getItem();
    }
    
    public void setData(String data) {
        JTextField text = ((JTextField) combo1.getEditor().getEditorComponent());
        text.setText(data);
    }
    
    
    public JPanel makeUI() { 
        background = AppUI.blendColors(AppUI.brand.getInputBackgroundColor(), outerBgColor);
        foreground = background;

        background = AppUI.brand.getInputBackgroundColor();
        UIManager.put("ComboBox.selectionForeground", AppUI.brand.getMainTextColor());
        UIManager.put("ComboBox.selectionBackground", background);
        
        combo1 = new JComboBox<>();
        AppUI.setSize(combo1, (int) AppUI.getBoxWidth(), (int) AppUI.getBoxHeight());  
        AppUI.setFont(combo1, 18);
        AppUI.setBackground(combo1, background);

        
        combo1.setBorder(new RoundedCornerBorder(this.outerBgColor));
        combo1.setUI(new ColorArrowUI(background));
        
        Object o = combo1.getAccessibleContext().getAccessibleChild(0);
        if (o instanceof JComponent) {
            JComponent c = (JComponent) o;
            c.setBorder(new RoundedCornerBorder2(this.outerBgColor));
            c.setForeground(foreground);
            c.setBackground(background);
        }
        
        JTextField text = ((JTextField) combo1.getEditor().getEditorComponent());
       
       // text.setBackground(background);
       // text.setForeground(AppUI.brand.getMainTextColor());
        AppUI.setBackground(text, AppUI.brand.getInputBackgroundColor());
        //AppUI.setColor(text, AppUI.brand.getMainTextColor());
        text.setCaretColor(AppUI.brand.getMainTextColor());
        AppUI.setMargin(text, 10);

        AppUI.setColor(text, AppUI.brand.getSecondTextColor());
        final JTextField ftf = text;       
        text.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (ftf.getText().equals(placeholder)) {
                    ftf.setForeground(AppUI.brand.getMainTextColor());
                    ftf.setText("");
                    ftf.repaint();
                } else {
                    ftf.setForeground(AppUI.brand.getMainTextColor());
                    ftf.repaint();
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (ftf.getText().isEmpty()) {
                    ftf.setForeground(AppUI.brand.getSecondTextColor());
                    ftf.setText(placeholder);
                    ftf.repaint();
                }
            }
        });
        

        combo1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
    
        combo1.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
            }
        });
   
        cbm = new MyComboBoxModel(this.placeholder, combo1);
        
        combo1.addPopupMenuListener(new HeavyWeightContainerListener(this.outerBgColor, cbm));   
        renderer = new ComboBoxRenderer(combo1, background);
        combo1.setRenderer(renderer);
        combo1.setModel(cbm);
        setOptions(this.options);

        
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, false);
        AppUI.noOpaque(p);
        p.add(combo1);

        return p;
    }
}

class MyComboBoxModel extends DefaultComboBoxModel {
    private static final long serialVersionUID = 1L;
    boolean selectionAllowed = true;
    String placeholder;
    JComboBox<String> combo1;

    public MyComboBoxModel(String placeholder, JComboBox<String> combo1) {
        this.placeholder = placeholder;
        this.combo1 = combo1;
    }
    
    public void drop() {
        selectionAllowed = true;
    }
            
    @Override
    public void setSelectedItem(Object anObject) {
        if (!placeholder.equals(anObject)) {
            AppUI.setColor(combo1, AppUI.brand.getMainTextColor());
            super.setSelectedItem(anObject);
        } else if (selectionAllowed) {
            AppUI.setColor(combo1, AppUI.brand.getSecondTextColor());
            selectionAllowed = false;
            super.setSelectedItem(anObject);
        }
    }


}

class HeavyWeightContainerListener implements PopupMenuListener {
    Color outerBgColor;
    MyComboBoxModel cbm;
    
    public HeavyWeightContainerListener(Color outerBgColor, MyComboBoxModel cbm) {
        this.outerBgColor = outerBgColor;
        this.cbm = cbm;
    }
    
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JComboBox c = (JComboBox) e.getSource();
        c.setBorder(new RoundedCornerBorder1(this.outerBgColor));
        final PopupMenuEvent fe = e;
        EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                JComboBox combo = (JComboBox) fe.getSource();
                Accessible a = combo.getUI().getAccessibleChild(combo, 0);
                if (a instanceof BasicComboPopup) {
                    BasicComboPopup pop = (BasicComboPopup) a;
                    Container top = pop.getTopLevelAncestor();
                    if (top instanceof JWindow) {
                        //http://ateraimemo.com/Swing/DropShadowPopup.html
                        //((JWindow) top).setBackground(new Color(0x0, true));
                    }
                }
            }
        });
    }
        
    @Override 
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        JComboBox c = (JComboBox) e.getSource();
        c.setBorder(new RoundedCornerBorder(this.outerBgColor));
        cbm.drop();
    }
    
    @Override 
    public void popupMenuCanceled(PopupMenuEvent e) {
        JComboBox combo1 = (JComboBox) e.getSource();
    }
}

class RoundedCornerBorder extends AbstractBorder {
    protected Color outerBgColor;
    protected static final int ARC = 32;
    
    public RoundedCornerBorder(Color outerBgColor) {
        this.outerBgColor = outerBgColor;
    }

    @Override 
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int r = ARC;
        int w = width  - 1;
        int h = height - 1;
        
        Area round = new Area(new RoundRectangle2D.Float(x, y, w, h, r, r));
        if (c instanceof JPopupMenu) {
            g2.setPaint(c.getBackground());
            g2.fill(round);
        } else {
            Container parent = c.getParent();
            if (parent != null) {
                // Must match background Color
                g2.setPaint(outerBgColor);
                Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
                corner.subtract(round);
                g2.fill(corner);
            }
        }
    
        g2.setPaint(c.getBackground());
        g2.draw(round);
        g2.dispose();
    }

    
}

class RoundedCornerBorder1 extends RoundedCornerBorder {
    
    public RoundedCornerBorder1(Color outerBgColor) {
        super(outerBgColor);
    }
    
    @Override 
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int r = ARC;
        int w = width  - 1;
        int h = height - 1;

        Area round = new Area(new RoundRectangle2D.Float(x, y, w, h, r, r));
        Rectangle b = round.getBounds();
        b.setBounds(b.x, b.y + r, b.width, b.height - r);
        round.add(new Area(b));

        
        Container parent = c.getParent();
        if (parent != null) {
            g2.setPaint(outerBgColor);
            Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
            corner.subtract(round);
            g2.fill(corner);
        }

        g2.setPaint(c.getBackground());
        g2.draw(round);
        g2.dispose();
    }
    
   
    
}

class RoundedCornerBorder2 extends RoundedCornerBorder {
    public RoundedCornerBorder2(Color outerBgColor) {
        super(outerBgColor);
    }
    
    @Override 
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
       
        int r = ARC;
        int w = width  - 1 + 1;
        int h = height - 1;

        
        g2.setPaint(outerBgColor);
        //g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f));
        Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
        g2.fill(corner);
   //     g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
       

        Path2D.Float p = new Path2D.Float();
        p.moveTo(x, y );
        p.lineTo(x, y + h - r);
        p.quadTo(x, y + h, x + r, y + h);
        p.lineTo(x + w - r, y + h);
        p.quadTo(x + w, y + h, x + w, y + h - r);
        p.lineTo(x + w, y);
        p.closePath();
        Area round = new Area(p);

        g2.setPaint(c.getBackground());
        g2.fill(round);
        g2.setPaint(c.getBackground());

        g2.drawLine(x + 1, y, x + width - 2, y);
        g2.dispose();
    }
    
    
    @Override 
    public Insets getBorderInsets(Component c) {
      //  return new Insets(4, 8, 4, 8);
        return new Insets(0, 0, 12, 0);

    }
    /*
    @Override 
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(4, 8, 4, 8);

        return insets;
    }*/
}

class ColorArrowUI extends BasicComboBoxUI {
    Color background;
    
    ColorArrowUI(Color background) {
        this.background = background;
    }
    
    @Override 
    protected JButton createArrowButton() {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setRolloverEnabled(false);
        AppUI.setHandCursor(button);
        AppUI.setMargin(button, 0);

        //button.setBounds(100,100,100,100);
        try {
            BufferedImage img = ImageIO.read(AppUI.brand.getImgDropdownArrow());
            AppUI.invertImage(img);
            button.setIcon(new ImageIcon(img));
            button.setForeground(background);
            button.setText(" ");
        } catch (Exception ex) {
    
        }

        return button;
    }
}

class ComboBoxRenderer extends JPanel implements ListCellRenderer {
    JPanel textPanel;
    JLabel text;
    Color color;

    public ComboBoxRenderer(JComboBox combo, Color color) {
        this.color = color;
        textPanel = new JPanel();      
        textPanel.add(this);

        text = new JLabel();
        text.setOpaque(true);
        textPanel.add(text);
        AppUI.setMargin(text, 10, 10, 10, 10);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        AppUI.setFont(text, 18);
        if (value != null)
            text.setText(value.toString());
        if (index == 0) {
            text.setForeground(AppUI.brand.getSecondTextColor());
        } else if (index > -1) {
            text.setForeground(AppUI.brand.getMainTextColor());
        }
            
        if (isSelected) {
            text.setBackground(AppUI.brand.getDropdownHoverColor());
        } else {
            text.setBackground(color);
        }
        
        if (index == 0) {
            AppUI.setSize(text, 0, 0);
        } else {
            AppUI.setSize(text, (int) AppUI.getBoxWidth(), (int) AppUI.getBoxHeight());
        }

        
        return text;
    }
}