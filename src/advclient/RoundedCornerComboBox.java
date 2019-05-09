
package advclient;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.*;
import java.util.*;
import javax.accessibility.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.*;

public class RoundedCornerComboBox {
    AppUI ui;
  private static final Color BACKGROUND = Color.WHITE;
  private static final Color FOREGROUND = new Color(0x5FA8FF);
 //   private static final Color FOREGROUND = new Color(0x33000000);
  private static final Color SELECTIONFOREGROUND = Color.BLACK;
  public JPanel makeUI(AppUI ui) {
      this.ui = ui;
    UIManager.put("ComboBox.foreground", FOREGROUND);
    UIManager.put("ComboBox.background", BACKGROUND);
    UIManager.put("ComboBox.selectionForeground", SELECTIONFOREGROUND);
    UIManager.put("ComboBox.selectionBackground", BACKGROUND);

    UIManager.put("ComboBox.buttonDarkShadow", BACKGROUND);
    UIManager.put("ComboBox.buttonBackground", FOREGROUND);
    UIManager.put("ComboBox.buttonHighlight",  FOREGROUND);
    UIManager.put("ComboBox.buttonShadow",     FOREGROUND);

    UIManager.put("ComboBox.border", new RoundedCornerBorder());
    JComboBox<String> combo1 = new JComboBox<>(makeModel());
    
    //combo1.setPromptText("xxx");
    //combo1.setUI(new BasicComboBoxUI());
    combo1.setUI(new ColorArrowUI());
    Object o = combo1.getAccessibleContext().getAccessibleChild(0);
    if (o instanceof JComponent) {
      JComponent c = (JComponent) o;
      c.setBorder(new RoundedCornerBorder2());
      c.setForeground(FOREGROUND);
      c.setBackground(BACKGROUND);
    
      /*        
              int w = 360;
            int h = 50;
            
            h*=3;
       c.setPreferredSize(new Dimension(w, h));
        c.setMinimumSize(new Dimension(w, h));
        c.setMaximumSize(new Dimension(w, h));
    */
    }
    
    combo1.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
        System.out.println("xxxxxx = " + e.getActionCommand());
    //    combo1.setBorder(new RoundedCornerBorder1());
    }
    });
    
    combo1.addFocusListener(new FocusAdapter() {

   @Override
   public void focusGained(FocusEvent e) {
      //comboBox.showPopup();
       System.out.println("xxx222xxx");
   }
});
    
    
    
    
    int w = 360;
            int h = 50;
       combo1.setPreferredSize(new Dimension(w, h));
        combo1.setMinimumSize(new Dimension(w, h));
        combo1.setMaximumSize(new Dimension(w, h));
    
    combo1.addPopupMenuListener(new HeavyWeightContainerListener());

    
    
      try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("resources/Montserrat-Regular.otf"));
            combo1.setFont(font.deriveFont(Font.PLAIN, 18f));
             combo1.setForeground(Color.BLACK);
        } catch(Exception e){
            System.out.println("xxxxxx " + e.getMessage());
        }
    
    
    
    
    
    
    
    
    
    
    
    final String NOT_SELECTABLE_OPTION = " - Select an Option - ";
    
    combo1.setModel(new DefaultComboBoxModel<String>() {
      private static final long serialVersionUID = 1L;
      boolean selectionAllowed = true;

      @Override
      public void setSelectedItem(Object anObject) {
        if (!NOT_SELECTABLE_OPTION.equals(anObject)) {
          super.setSelectedItem(anObject);
        } else if (selectionAllowed) {
          // Allow this just once
          selectionAllowed = false;
          super.setSelectedItem(anObject);
        }
      }
    });
    
    combo1.addItem(NOT_SELECTABLE_OPTION);
    combo1.addItem("xxx1");
    combo1.addItem("xxx2");
    Color[] colors = {Color.BLUE, Color.GRAY, Color.RED};
    String[] strings = {"Test1", "Test2", "Test3"};
    
    ComboBoxRenderer renderer = new ComboBoxRenderer(combo1);
         renderer.setColors(colors);
        renderer.setStrings(strings);

        combo1.setRenderer(renderer);
    
    
    
    
    
    
    
    
    UIManager.put("ComboBox.border", new RoundedCornerBorder1());
    JComboBox<String> combo2 = new JComboBox<>(makeModel());
    combo2.setUI(new BasicComboBoxUI());
    o = combo2.getAccessibleContext().getAccessibleChild(0);
    if (o instanceof JComponent) {
      JComponent c = (JComponent) o;
      c.setBorder(new RoundedCornerBorder2());
      c.setForeground(FOREGROUND);
      c.setBackground(BACKGROUND);
    }
    combo2.addPopupMenuListener(new HeavyWeightContainerListener());

    JPanel p = new JPanel();
    ui.setSize(p, w + 20, h + 10);
    p.add(combo1);
   // p.add(combo2);
    p.setOpaque(false);
    p.setBackground(new Color(0x775FA8FF));
    //p.setBackground(Color.RED);
   // p.setAlignmentY(Component.TOP_ALIGNMENT);
    return p;
  }
  private static DefaultComboBoxModel<String> makeModel() {
    DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
    m.addElement("1234");
    m.addElement("5555555555555555555555");
    m.addElement("6789000000000");
    return m;
  }
  
}

class HeavyWeightContainerListener implements PopupMenuListener {
    @Override 
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        JComboBox c = (JComboBox) e.getSource();
        System.out.println("zzzzzzzzzzzzzz");
         c.setBorder(new RoundedCornerBorder1());
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
                        System.out.println("HeavyWeightContainer");
                        ((JWindow) top).setBackground(new Color(0x0, true));
                    }
                }
            }
        });
    }
        
    @Override 
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        System.out.println("inv");
        JComboBox c = (JComboBox) e.getSource();
         c.setBorder(new RoundedCornerBorder());
    }
  
  //@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
  @Override public void popupMenuCanceled(PopupMenuEvent e) {}
}

class RoundedCornerBorder extends AbstractBorder {
  protected static final int ARC = 18;
  @Override public void paintBorder(
      Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    int r = ARC;
    int w = width  - 1;
    int h = height - 1;

    Area round = new Area(new RoundRectangle2D.Float(x, y, w, h, r, r));
    if (c instanceof JPopupMenu) {
      g2.setPaint(c.getBackground());
      g2.fill(round);
    } else {
      Container parent = c.getParent();
    //  if (Objects.nonNull(parent)) {
      if (parent != null) {
        g2.setPaint(parent.getBackground());
        g2.setPaint(new Color(0x5FA8FF));
        Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
        corner.subtract(round);
        g2.fill(corner);
      }
    }
   // g2.setPaint(c.getForeground());
    g2.setPaint(new Color(0x5FA8FF));
    g2.draw(round);
    g2.dispose();
  }
  @Override public Insets getBorderInsets(Component c) {
    return new Insets(4, 8, 4, 8);
  }
  @Override public Insets getBorderInsets(Component c, Insets insets) {
    insets.set(4, 8, 4, 8);
    return insets;
  }
}

class RoundedCornerBorder1 extends RoundedCornerBorder {
  //http://ateraimemo.com/Swing/RoundedComboBox.html
  @Override public void paintBorder(
      Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    int r = ARC;
    int w = width  - 1;
    int h = height - 1;

    Area round = new Area(new RoundRectangle2D.Float(x, y, w, h, r, r));
    Rectangle b = round.getBounds();
    b.setBounds(b.x, b.y + r, b.width, b.height - r);
    round.add(new Area(b));

    Container parent = c.getParent();
    //if (Objects.nonNull(parent)) {
    if (parent != null) {
      g2.setPaint(parent.getBackground());
      Area corner = new Area(new Rectangle2D.Float(x, y, width, height));
      corner.subtract(round);
      g2.fill(corner);
    }

  //  g2.setPaint(c.getForeground());
    g2.draw(round);
    g2.dispose();
  }
}

class RoundedCornerBorder2 extends RoundedCornerBorder {
  @Override public void paintBorder(
      Component c, Graphics g, int x, int y, int width, int height) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    int r = ARC;
    int w = width  - 1;
    int h = height - 1;

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

    g2.setPaint(c.getForeground());
    g2.draw(round);
    g2.setPaint(c.getBackground());
    g2.drawLine(x + 1, y, x + width - 2, y);
    g2.dispose();
  }
  
  
}

 class ColorArrowUI extends BasicComboBoxUI {

    

    @Override protected JButton createArrowButton() {
        //JButton j;
         JButton button = new JButton();
         button.setOpaque(false);
         button.setBackground(Color.WHITE);
         button.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
  try {
    Image img = ImageIO.read(getClass().getClassLoader().getResource("resources/arrow.png"));
    button.setIcon(new ImageIcon(img));
  } catch (Exception ex) {
    System.out.println(ex);
  }
    //    j = new JButton("")
        return button;
        /*
        return new BasicArrowButton(
            BasicArrowButton.SOUTH,
            Color.cyan, Color.magenta,
            Color.yellow, Color.blue);
                */
    }
}

class ComboBoxRenderer extends JPanel implements ListCellRenderer
{

    private static final long serialVersionUID = -1L;
    private Color[] colors;
    private String[] strings;

    JPanel textPanel;
    JLabel text;

    public ComboBoxRenderer(JComboBox combo) {

        textPanel = new JPanel();
        
        textPanel.add(this);
        text = new JLabel();
        text.setOpaque(true);
        text.setFont(combo.getFont());
        textPanel.add(text);
       
              text.setPreferredSize(new Dimension(160, 40)); 
        text.setMinimumSize(new Dimension(160, 40)); 
        text.setMaximumSize(new Dimension(160, 40)); 
    }

    public void setColors(Color[] col)
    {
        colors = col;
    }

    public void setStrings(String[] str)
    {
        strings = str;
    }

    public Color[] getColors()
    {
        return colors;
    }

    public String[] getStrings()
    {
        return strings;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {

        if (isSelected)
        {
            setBackground(list.getSelectionBackground());
        }
        else
        {
            setBackground(Color.WHITE);
        }
        
        
        
        System.out.println("xxxxxx111");
   
/*
        if (colors.length != strings.length)
        {
            System.out.println("colors.length does not equal strings.length");
            return this;
        }
        else if (colors == null)
        {
            System.out.println("use setColors first.");
            return this;
        }
        else if (strings == null)
        {
            System.out.println("use setStrings first.");
            return this;
        }
        */
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("resources/Montserrat-Regular.otf"));
            text.setFont(font.deriveFont(Font.PLAIN, 18f));
        } catch(Exception e){
            System.out.println("xxxxxx " + e.getMessage());
        }
        
        text.setBackground(getBackground());

        text.setText(value.toString());
        if (index>-1) {
            text.setForeground(Color.BLACK);
        }
        
        if (isSelected) {
        System.out.println("xxx="+index + " isSel" + isSelected + " has="+cellHasFocus);
            text.setBackground(Color.RED);
        } else {
            text.setBackground(getBackground());
        }
        
        return text;
    }
}