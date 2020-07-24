/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.Config;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;


import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Александр
 */
public class AppUI {
    
    static int tw, th;
    static double ratio;
    public static Brand brand;
    
    public static void init(int tw, int th, Brand brand) {
        AppUI.tw = tw;
        AppUI.th = th;        
        AppUI.ratio = tw / th;
        
        AppUI.brand = brand;
    }
    
    public static String getAgreementText() {
        ClassLoader cl;            
        cl = AppUI.class.getClassLoader();
            
        
        InputStream is = cl.getResourceAsStream("resources/TermsAndConditions.html");
        String aText = AppCore.loadFileFromInputStream(is);
        
        return aText;
    }
    
    
    public static void setSize(Component c, double w) {
        int h = (int) (w * ratio);
        
        setSize(c, (int) w, h);
    }
    
    public static void setSize(Component c, int w, int h) {
        c.setPreferredSize(new Dimension(w, h));
        c.setMinimumSize(new Dimension(w, h));
        c.setMaximumSize(new Dimension(w, h));
    }

    public static Color blendColors(Color c0, Color c1) {  
        double r0 = (double) c0.getRed() / 255.0;
        double g0 = (double) c0.getGreen()  / 255.0;
        double b0 = (double) c0.getBlue() / 255.0;
        double a0 = (double) c0.getAlpha() / 255.0;
                
        double r1 = (double) c1.getRed() / 255.0;
        double g1 = (double) c1.getGreen()  / 255.0;
        double b1 = (double) c1.getBlue() / 255.0;
        
        
        double r = (r0 * a0) + (r1 * (1 - a0));
        double g = (g0 * a0) + (g1 * (1 - a0));
        double b = (b0 * a0) + (b1 * (1 - a0));
     
        int ir = (int) (r * 255.0);
        int ig = (int) (g * 255.0);
        int ib = (int) (b * 255.0);
         
        Color nc = new Color(ir,ig,ib);
       
        return nc;
    }
    
    
    public static double getBoxWidth() {
        return AppUI.tw / 4;
    }
    
    public static double getBoxHeight() {
        return AppUI.th / 16;
    }
    
    public static void setBoxLayout(Component c, boolean isVertical) {
        int direction = isVertical ? BoxLayout.PAGE_AXIS : BoxLayout.LINE_AXIS;
        
        ((Container) c).setLayout(new BoxLayout((Container) c, direction));
    }
    
    public static void setBackground(JComponent c, Color color) {
        c.setBackground(color);
    }
    
    public static void alignLeft(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    
    public static void alignRight(JComponent c) {
        c.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }
    
    public static void alignCenter(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
    }
    
    
    public static void alignTop(JComponent c) {
        c.setAlignmentY(Component.TOP_ALIGNMENT);
    }
    
    public static void alignBottom(JComponent c) {
        c.setAlignmentY(Component.BOTTOM_ALIGNMENT);
    }
    
    public static void noOpaque(JComponent c) {
        c.setOpaque(false);
    }
    
    public static void opaque(JComponent c) {
        c.setOpaque(true);
    }
    
    public static void vr(JComponent c, double size) {
        Component bc = Box.createRigidArea(new Dimension((int) size, 0));
        c.add(bc);
    }
    
    public static void hr(Component c, double size) {
        Component bc = Box.createRigidArea(new Dimension(0, (int) size));
        ((Container) c).add(bc);
    }
    
    public static void setMargin(JComponent c, int margin) {
        c.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
    }
    
    public static void setMargin(JComponent c, int top, int left, int bottom, int right) {
        c.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }
    
    
    public static void roundCorners(JComponent c, Color color, int radius) {
        c.setBorder(new RoundedBorder(radius, color));
    }
    
    public static void roundCorners(JComponent c, Color color, int radius, UICallBack cb) {
        c.setBorder(new RoundedBorder(radius, color, cb));
    }
    
    //AGIEEE
    public static void underLine(Component label) {
        Font font = label.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        label.setFont(font.deriveFont(attributes));
    }
    
    public static void noUnderLine(Component label) {
        Font font = label.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, null);
        label.setFont(font.deriveFont(attributes));
    }
    
    
    public static void setTitleFont(Component c, int size) {
        c.setFont(brand.getSecondFont().deriveFont(Font.PLAIN, size));
        c.setForeground(Color.WHITE);
    }
    
    public static void setTitleBoldFont(Component c, int size) {
        c.setFont(brand.getMainBoldFont().deriveFont(Font.BOLD, size));
        c.setForeground(Color.WHITE);
    }
    
    public static void setTitleSemiBoldFont(Component c, int size) {
        c.setFont(brand.getMainSemiBoldFont().deriveFont(Font.PLAIN, size));
        c.setForeground(Color.WHITE);
    }
    
    public static void setFont(Component c, int size) {
        c.setFont(brand.getMainFont().deriveFont(Font.PLAIN, size));
    }
    
    public static void setSemiBoldFont(Component c, int size) {
        c.setFont(brand.getMainSemiBoldFont().deriveFont(Font.PLAIN, size));
    }
    
    public static void setBoldFont(Component c, int size) {
        c.setFont(brand.getMainBoldFont().deriveFont(Font.PLAIN, size));
    }
    
    
    
    public static void setCommonFont(Component c) {
        AppUI.setFont(c, 16);
        AppUI.setColor(c, brand.getMainTextColor());
    }
    
    public static void setCommonBoldFont(Component c) {
        AppUI.setBoldFont(c, 25);
    }
    
    public static void setCommonTableFont(Component c) {
        c.setFont(brand.getSecondFont().deriveFont(Font.PLAIN, 14));
    }
    
    public static void setCommonBoldTableFont(Component c) {
        c.setFont(brand.getSecondSemiBoldFont().deriveFont(Font.PLAIN, 14));
    }
    
    public static void setCommonTableFontSize(Component c, int size) {
        c.setFont(brand.getSecondFont().deriveFont(Font.PLAIN, size));
    }
    
    
    public static Component hr(int size) {
        return Box.createRigidArea(new Dimension(0, size));
    }
    
    public static Component vr(double size) {
        return Box.createRigidArea(new Dimension((int) size, 0));
    }
    
    public static void setHandCursor(Component c) {
        c.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    public static void setDefaultCursor(Component c) {
        c.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }
    
    public static void setColor(Component c, Color color) {
        c.setForeground(color);
    }
    

    public static JPanel createRoundedPanel(JPanel parent, Color color, int sideMargin) {
        return createRoundedPanel(parent, color, sideMargin, 20);
    }
   
    public static JPanel createRoundedPanel(JPanel parent, Color color, int sideMargin, int radius) {
        JPanel p = new JPanel();
        
        AppUI.setBoxLayout(p, true);
        AppUI.alignCenter(p);
        //AppUI.roundCorners(p, color, radius);
        AppUI.noOpaque(p);
        AppUI.setBackground(p, color);
        
        JPanel subInnerCore = new JPanel();
        AppUI.setBoxLayout(subInnerCore, true);
        AppUI.alignCenter(subInnerCore);
        AppUI.noOpaque(subInnerCore);
        AppUI.setMargin(subInnerCore, 0, sideMargin, 20, sideMargin);
        p.add(subInnerCore);
           
        parent.add(p);
        
        p.setAlignmentY(Component.CENTER_ALIGNMENT);
        subInnerCore.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        return subInnerCore;
    }
    
    //AGIEEE
    public static JLabel getTitle(String s) {
        JLabel str = new JLabel(s);
        AppUI.alignCenter(str);
        AppUI.setBoldFont(str, 30);
        
        return str;
    }
    
    public static JLabel getCommonLabel(String s) {
        JLabel res = new JLabel(s);
        AppUI.setCommonFont(res);
        AppUI.alignCenter(res);
        
        return res;
    }
    
    public static JLabel getCommonBoldLabel(String s) {
        JLabel res = new JLabel(s);
        AppUI.setBoldFont(res, 25);
        AppUI.alignCenter(res);
        
        return res;
    }
    
    public static JPanel getTextBox(String name, String title) {
        JPanel hct = new JPanel();
        
        AppUI.setBoxLayout(hct, false);
        AppUI.setMargin(hct, 0, 28, 0, 0);
        AppUI.noOpaque(hct);
         
        AppUI.setSize(hct, tw / 2, 50);
        
        // Name Label        
        JLabel x = new JLabel(name);
        AppUI.setCommonFont(x);
        hct.add(x);
        
        AppUI.vr(hct, 50);
        
        MyTextField walletName = new MyTextField(title, false);
        hct.add(walletName.getTextField());
         
        return hct;
    }
    
    public static String getBackupKeysOption() {
        return "- Backup Keys";
    }
    
    public static String getRemoteUserOption() {
        return "New Remote User";
    }
    
    public static String getLocalFolderOption() {
        return "- Local Folder";
    }

    public static JScrollPane setupTable(JTable table, String[] columnNames, String[][] data, 
            DefaultTableCellRenderer r, TableCellRenderer hrr) {
        final String[] fcolumnNames = columnNames;
        DefaultTableModel model = new DefaultTableModel(data.length, data[0].length - 1) {
            @Override
            public String getColumnName(int col) {        
                return fcolumnNames[col].toUpperCase();
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
            
            public boolean isCellEditable(int row, int column){  
               return false;  
            }
        };
        
        table.setModel(model);
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                model.setValueAt(data[row][col], row, col);
            }
        }
 
        table.setRowHeight(table.getRowHeight() + 32);
        table.setDefaultRenderer(String.class, r);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setGridColor(brand.getTableGridColor());
        
        
        JTableHeader header = table.getTableHeader();  
        AppUI.setColor(header, brand.getTableHeaderTextColor());
        AppUI.noOpaque(header);
        AppUI.setCommonTableFont(table);
        AppUI.setSemiBoldFont(header, 14);

        
        final TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
        if (hrr != null)
            header.setDefaultRenderer(hrr);
        else 
            header.setDefaultRenderer(new TableCellRenderer() {
                private JLabel lbl;
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    lbl = (JLabel) hr.getTableCellRendererComponent(table, value, true, true, row, column);
                    //lbl.setHorizontalAlignment(SwingConstants.LEFT);

                    if (column == 0 || column == 1)
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    else
                        lbl.setHorizontalAlignment(SwingConstants.RIGHT);

                    lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, brand.getTableGridColor()));
                    lbl.setBorder(BorderFactory.createCompoundBorder(lbl.getBorder(), BorderFactory.createEmptyBorder(0, 6, 10, 0)));

                    AppUI.setBackground(lbl, brand.getPanelBackgroundColor());

                    return lbl;
                }
            });
        
        
        //This is the wallet table size 
        JScrollPane scrollPane = new JScrollPane(table);
        if (Config.REQUESTED_ADVANCED_VIEW == true)
            AppUI.setSize(scrollPane, 880, 294);
        else
            AppUI.setSize(scrollPane, 1080, 294);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {       
            @Override
            protected JButton createDecreaseButton(int orientation) {
                TriangleButton jbutton = new TriangleButton(false);
                AppUI.setHandCursor(jbutton);
                jbutton.setContentAreaFilled(false);
                jbutton.setFocusPainted(false);
            
                JButton b = new JButton();
                AppUI.setSize(b, 0, 0);
                
                return b;
            }

            @Override    
            protected JButton createIncreaseButton(int orientation) {
                TriangleButton jbutton = new TriangleButton(true);
                AppUI.setHandCursor(jbutton);
                jbutton.setContentAreaFilled(false);
                jbutton.setFocusPainted(false);
            
                JButton b = new JButton();
                AppUI.setSize(b, 0, 0);
                
                return b;
            }
 
            @Override 
            protected void configureScrollBarColors(){
                this.trackColor = brand.getScrollbarTrackColor();
                this.thumbColor = brand.getScrollbarThumbColor();
            }
        });
        
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        return scrollPane;
        
    }
    
    
    public static JLabel getHyperLink(String name, String url, int fontSize) {
        final String furl = url;
        
        JLabel l = new JLabel(name);
        if (fontSize == 0)
            AppUI.setCommonFont(l);
        else
            AppUI.setFont(l, fontSize);
        
        AppUI.setColor(l, brand.getHyperlinkColor());
        AppUI.underLine(l);
        AppUI.setHandCursor(l);
        l.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        URI uri = new URI(furl);
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException ex) { 
  
                    } catch (URISyntaxException ex) {
                        
                    }
                } else { 
                }   
            }
        });
        
        return l;
    }
    
    
    public static void getGBRow(JComponent parent, JComponent left, JComponent right, int y, GridBagLayout gridbag) {
        GridBagConstraints c = new GridBagConstraints();
  
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(20, 0, 0, 0);    
        c.gridy = y;
        c.weightx = 0;
        c.weighty = 0;
                    
                
        if (left != null) {
            if (left instanceof JLabel) {
                AppUI.setCommonFont(left);
            } 
        
            gridbag.setConstraints(left, c);
            parent.add(left);
        } else {
            c.gridwidth = 2;
        }
        
        if (right instanceof JLabel || left instanceof JLabel)
            c.insets = new Insets(8, 0, 0, 0);
        
        c.weightx = 10;       
        if (right != null) {
            if (right instanceof JLabel) {
                if (left == null)
                    c.insets = new Insets(10, 0, 10, 0); 
                else
                    c.insets = new Insets(20, 0, 0, 0);
                AppUI.setCommonFont(right);
            }
   
            gridbag.setConstraints(right, c);
            parent.add(right);       
        }
    }
    
    public static void GBPad(JComponent parent, int y, GridBagLayout gridbag) {
        GridBagConstraints c = new GridBagConstraints();
        JPanel padder = new JPanel();
        AppUI.noOpaque(padder);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 0);    
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.weightx = 1;
        c.weighty = 1;
        gridbag.setConstraints(padder, c);
        parent.add(padder);
    }
    
    public static MyButton getTwoButtonPanel(JComponent parent, String name0, String name1, 
            ActionListener al0, ActionListener al1, int y, GridBagLayout gridbag) {
        GridBagConstraints c = new GridBagConstraints();
        
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(20, 0, 0, 10);    
        c.gridy = y;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 2;
        
        if (!name0.isEmpty()) {
            MyButton cb0 = new MyButton(name0, brand.getSecondaryButtonColor());
            cb0.addListener(al0);
   
            gridbag.setConstraints(cb0.getButton(), c);
            parent.add(cb0.getButton());
        }
        
        MyButton cb1 = new MyButton(name1);
        cb1.addListener(al1);
        c.weightx = 0;
        gridbag.setConstraints(cb1.getButton(), c);
        parent.add(cb1.getButton());       
        
        return cb1;
    }
    
    public static MyButton getThreeButtonPanel(JComponent parent, String name0, String name1, String name2,
            ActionListener al0, ActionListener al1, ActionListener al2, int y, GridBagLayout gridbag) {
        GridBagConstraints c = new GridBagConstraints();
        
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(40, 0, 0, 10);    
        c.gridy = y;
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 2;
        
        JPanel jwr = new JPanel();
        AppUI.setBoxLayout(jwr, false);
        AppUI.noOpaque(jwr);
        
        MyButton cb0 = new MyButton(name0, brand.getSecondaryButtonColor());
        cb0.addListener(al0);  
        //gridbag.setConstraints(cb0.getButton(), c);
        jwr.add(cb0.getButton());
        jwr.add(AppUI.vr(8));
        
        //c.gridwidth = 1;
        MyButton cb1 = new MyButton(name1, brand.getSecondaryButtonColor());
        cb1.addListener(al1);  
        //gridbag.setConstraints(cb1.getButton(), c);
        jwr.add(cb1.getButton());
        jwr.add(AppUI.vr(8));
        
        MyButton cb2 = new MyButton(name2);
        cb2.addListener(al2);
        c.weightx = 0;
        //gridbag.setConstraints(cb2.getButton(), c);
        jwr.add(cb2.getButton());       
        gridbag.setConstraints(jwr, c);
        
        parent.add(jwr);
        
        return cb2;
    }
    
    
    public static JLabel wrapDiv(String text) {
        return new JLabel("<html><div style='width:500px; text-align:left'>" + text + "</div></html>");
    }
    
    
    public static void addInvItem(JComponent parent, String denomination, int count) {
        JPanel invItem = new JPanel();
        AppUI.setMargin(invItem, 0, 0, 0, 0);
        AppUI.noOpaque(invItem);
        AppUI.setBoxLayout(invItem, true);
        
        JLabel l0 = new JLabel(AppCore.formatNumber(count));
        AppUI.alignCenter(l0);
    
        JPanel hyp = new JPanel();
        AppUI.setSize(hyp, 48, 1);
        AppUI.setBackground(hyp, brand.getSecondTextColor());
        
        JLabel l1 = new JLabel(denomination + "'s");
        AppUI.alignCenter(l1);

        AppUI.setFont(l0, 14);
        AppUI.setFont(l1, 14);      
        if (count > 0) {
            AppUI.setColor(l0, brand.getMainTextColor());
            AppUI.setColor(l1, brand.getMainTextColor());
        } else {
            AppUI.setColor(l0, brand.getSecondTextColor());
            AppUI.setColor(l1, brand.getSecondTextColor());
        }
        
        AppUI.setMargin(l0, 0, 0, 0, 0);
        AppUI.setMargin(l1, 4);
        
        
        invItem.add(l0);
        invItem.add(hyp);
        invItem.add(l1);
            
        parent.add(invItem);
    }
    
    public static void invertImage(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = img.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(),
                                255 - col.getGreen(),
                                255 - col.getBlue(), col.getAlpha());
                img.setRGB(x, y, col.getRGB());
            }
        }
    }
    
    public static void whiteImage(BufferedImage img) {
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = img.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255, 255, 255, col.getAlpha());
                img.setRGB(x, y, col.getRGB());
            }
        }
    }
}
