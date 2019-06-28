/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
    
    //AGIEEE
    static Font montLight, montMedium, montReg;
   // static Font regFont, semiBoldFont, boldFont;
    //static Font osRegFont, osSemiBoldFont;
    
    public static void init(int tw, int th) {
        AppUI.tw = tw;
        AppUI.th = th;
        
        AppUI.ratio = tw / th;
        
        try {
            ClassLoader cl;
            
            cl = AppUI.class.getClassLoader();
            
            //AGIEEE
            montLight = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Light.ttf"));
            montMedium = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Medium.ttf"));
            montReg = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Regular.ttf"));
            
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(montReg);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(montMedium);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(montLight);
            
            
            //osRegFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Medium.ttf"));
            //osSemiBoldFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Semibold.ttf"));
            
        } catch(Exception e){
            System.out.println("Failed to load font: " + e.getMessage());
        }
        
        
    }
    
    public static String getAgreementText() {
        String agreement = "<br><p>Please read these terms and conditions carefully before using this application "
                + "distributed by the CloudCoin Consortium.</p><br>"
                + "<p>Conditions of Use</p><br>"
                + "<p>The CloudCoin Consortium provides this software free of cost, and as is. "
                + "Every time you utilize this application or its services, you accept the following conditions. "
                + "Therefore, the CloudCoin Consortium urges you to read them carefully.</p>"
                + "<p>Privacy Policy</p><br>"
                + "<p>The CloudCoin consortium does not collect or store any user data. "
                + "All transactions are anonymous and no user information is ever collected "
                + "by the RAIDA or the CloudCoin Consortium.</p><br>"
                + "<p>Copyright</p><br>"
                + "Content provided to you by the CloudCoin Consortium is developed by our partner RAIDAtech. "
                + "All graphics, logos, images, and text herein are considered the property of the "
                + "CloudCoin Consortium and protected by international copyright laws. "
                + "This software is considered open source and not owned by either "
                + "the CloudCoin Consortium, RAIDAtech, or any of its affiliates.</p><br>"
                + "<p>Communications</p><br>"
                + "<p>All communication with the CloudCoin Consortium, RAIDA, or RAIDAtech "
                + "affiliates is electronic. Any time you use the RAIDA to pown, send, or transfer coins"
                + " you are going to be communicating with us. If you subscribe to the newsletter "
                + "through the Consortium, you are going to receive regular emails from us. "
                + "We will continue to communicate with you by posting newsletters and notices "
                + "by sending you emails. You also agree that all notices, disclosures, "
                + "agreements, and other communications we provide to you electronically meet "
                + "the legal requirements that such communications be in writing.</p><br>"
                + "<p>Emails</p><br>"
                + "<p>Users may send emails to the support staff for this software as long "
                + "as it is not obscene, illegal, defamatory, threatening, infringing of "
                + "intellectual property rights, invasive of privacy or injurious in "
                + "any other way to third parties.</p><br>"
                + "<p>We reserve all rights to refuse service to individuals involved in such transmission.</p><br>"
                + "<p>Ability to Accept Terms of Service</p><br>"
                + "<p>You affirm that you are either more than 18 years of age, or an emancipated minor,"
                + " or possess legal parental or guardian consent, and are fully able and competent "
                + "to enter into the terms, conditions, obligations, affirmations, and representations "
                + "set forth in these Terms of Service, and to abide by and comply with "
                + "these Terms of Service. In any case, you affirm that you are over "
                + "the age of 18 years of age. If you are under 18 years of age, "
                + "then please do not use this service.</p><br>"
                + "<p>NO WARRANTY</p><br>"
                + "<p>You expressly acknowledge and agree that the use of this application "
                + "is at your sole risk. To the maximum extent permitted by applicable law, "
                + "the CloudCoin Advanced Client Software and any services performed "
                + "or provided by the CloudCoin Consortium are provided “as is” and “as available,” "
                + "with all faults and without warranty of any kind, and the CloudCoin Consortium "
                + "hereby disclaims all warranties and conditions with respect "
                + "to the CloudCoin Advanced Client Software and any services, "
                + "either express, implied, or statutory, including, but not limited to, "
                + "the implied warranties and/or conditions of satisfactory quality, "
                + "of fitness for a particular purpose, of accuracy, of quiet enjoyment, "
                + "and of non infringement of third-party rights. </p><br><p>No oral or written "
                + "information or advice given by the CloudCoin Consortium or any authorized "
                + "representative of the CloudCoin Consortium shall create a warranty of any kind. "
                + "Should the application or services prove defective, you as the user assume the "
                + "entire cost of any loss, broken coins, or correction of. Some jurisdictions do not allow "
                + "the exclusion of implied warranties or limitations on applicable statutory rights of a consumer, "
                + "so the above exclusion and limitations may not apply to you.</p><br> "
                + "<p>Limitation of Liability</p><br>"
                + "<p>In no event shall the CloudCoin Consortium, nor any of its officers,"
                + " directors, or members be liable to you for anything arising out of or in any "
                + "way connected with your use of this service, whether such liability is under contract, "
                + "tort or otherwise, and the CloudCoin Consortium, including its officers, directors, "
                + "and members shall not be liable for any indirect, consequential or special "
                + "liability arising out of or in any way related to your use of this service.</p><br>"
                + "<p>Indemnification</p><br>"
                + "<p>You hereby indemnify to the fullest extent the CloudCoin Consortium "
                + "from and against any and all liabilities, costs, demands, causes of action, "
                + "damages, and expenses (including reasonable attorney’s fees) arising out of or in any "
                + "way related to your breach of any of the provisions of these Terms.</p><br>"
                + "<p>Variation of Terms</p><br>"
                + "<p>The CloudCoin Consortium is permitted to revise these Terms at any time "
                + "as it sees fit, and by using this software you are expected to review such "
                + "Terms on a regular basis to ensure you understand all terms and conditions "
                + "governing the use of this software.</p><br>"
                + "<p>Assignment</p><br>"
                + "<p>The CloudCoin Consortium shall be permitted to assign, transfer, "
                + "and subcontract its rights and/or obligations under these Terms without "
                + "any notification or consent required. However, you shall not be permitted "
                + "to assign, transfer, or subcontract any of your rights "
                + "and/or obligations under these Terms.</p><br>"
                + "<p>Entire Agreement</p><br>"
                + "<p>These Terms, including any legal notices and disclaimers contained "
                + "in this software, constitute the entire agreement between "
                + "the CloudCoin Consortium, you and in relation to your use of this "
                + "software, and supersede all prior agreements and understandings "
                + "with respect to the same.<br><br>";
        
        return agreement;
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
    
       public static Color getColor0() {
        return new Color(0x1c2340);
    }
    
    public static Color getColor1() {
        return new Color(0x151935);
    }
    
    public static Color getColor2() {
        return new Color(0x1c2340);
    }
    
    public static Color getColor3() {
        //return new Color(0xBFFFFFFF, true);
        return new Color(0x1c2340);
    }
    
    public static Color getColor4() {
        return new Color(0x1f222b, true);
    }
    
    public static Color getColor5() {
        return new Color(0x338fff);
    }
    public static Color getColor6() {
        return new Color(0x2c303d);
    }
    public static Color getColor7() {
        return new Color(0x2c303d);
    }
    
    public static Color getColor8() {
        return new Color(0x2c303d);
    }
    
    public static Color getColor9() {
        return new Color(0x338fff);
    }
    
    public static Color getColor10() {
        return new Color(0x151935);
    }
    
    public static Color getColor11() {
        return new Color(0x1c2340);
    }
    
    public static Color getColor12() {
        return new Color(0x1c2340);
    }
       
    public static Color getColor13() {
        return new Color(0x151935);
    }
    
    
    public static Color getDisabledColor() {
        return new Color(0xCCCCCC);
    }
    
    public static Color getDisabledColor2() {
        return new Color(0x999999);
    }
    
    
    
    public static Color getErrorColor() {
        return Color.RED;
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
        return AppUI.tw / 3;
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
        c.setFont(montReg.deriveFont(Font.PLAIN, size));
        c.setForeground(getColor5());
    }
    
    public static void setTitleBoldFont(Component c, int size) {
        c.setFont(montReg.deriveFont(Font.BOLD, size));
        c.setForeground(getColor5());
    }
    
    public static void setTitleSemiBoldFont(Component c, int size) {
        c.setFont(montMedium.deriveFont(Font.PLAIN, size));
        c.setForeground(getColor5());
    }
    
    public static void setFont(Component c, int size) {
        c.setFont(montReg.deriveFont(Font.PLAIN, size));
        c.setForeground(Color.lightGray);
    }
    
    public static void setSemiBoldFont(Component c, int size) {
        c.setFont(montMedium.deriveFont(Font.PLAIN, size));
        c.setForeground(Color.lightGray);
    }
    
    public static void setBoldFont(Component c, int size) {
        c.setFont(montReg.deriveFont(Font.PLAIN, size));
        c.setForeground(getColor5());
    }
    
    
    
    public static void setCommonFont(Component c) {
        AppUI.setFont(c, 25);
    }
    
    public static void setCommonBoldFont(Component c) {
        AppUI.setBoldFont(c, 25);
    }
    
    public static void setCommonTableFont(Component c) {
        c.setFont(montLight.deriveFont(Font.PLAIN, 14));
    }
    
    public static void setCommonBoldTableFont(Component c) {
        c.setFont(montMedium.deriveFont(Font.PLAIN, 14));
    }
    
    public static void setCommonTableFontSize(Component c, int size) {
        c.setFont(montLight.deriveFont(Font.PLAIN, size));
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
    
    public static JFrame getMainFrame() {
        JFrame frame = new JFrame();
        
        frame.setTitle("CloudCoin Bank");
        frame.setLayout(new BorderLayout());
        frame.setSize(new Dimension(tw, th));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);
        
        return frame;
    }
   
   public static JPanel createRoundedPanel(JPanel parent) {
        return createRoundedPanel(parent, AppUI.getColor2(), 0, 0);
    }
    
    public static JPanel createRoundedPanel(JPanel parent, Color color, int sideMargin) {
        return createRoundedPanel(parent, color, sideMargin, 0);
    }
   
    public static JPanel createRoundedPanel(JPanel parent, Color color, int sideMargin, int radius) {
        JPanel p = new JPanel();
        
        AppUI.setBoxLayout(p, true);
        AppUI.alignCenter(p);
        AppUI.roundCorners(p, color, radius);
        AppUI.noOpaque(p);
        
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
    
    public static String getRemoteUserOption() {
        return "- To Remote User";
    }
    
    public static String getLocalFolderOption() {
        return "- To Local Folder";
    }

    public static JScrollPane setupTable(JTable table, String[] columnNames, String[][] data, DefaultTableCellRenderer r) {
        final String[] fcolumnNames = columnNames;
        DefaultTableModel model = new DefaultTableModel(data.length, data[0].length - 1) {
            @Override
            public String getColumnName(int col) {        
                return fcolumnNames[col];
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
 
        table.setRowHeight(table.getRowHeight() + 15);
        table.setDefaultRenderer(String.class, r);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setGridColor(AppUI.getColor7());
        
        
        JTableHeader header = table.getTableHeader();    
        AppUI.setColor(header, Color.WHITE);
        AppUI.noOpaque(header);
        AppUI.setCommonTableFont(table);
        AppUI.setCommonTableFont(header);
        
        final TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
        header.setDefaultRenderer(new TableCellRenderer() {
            private JLabel lbl;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                lbl = (JLabel) hr.getTableCellRendererComponent(table, value, true, true, row, column);
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    //internal transaction table margins
                AppUI.setMargin(lbl, 2, 10, 2, 2);
                AppUI.setBackground(lbl, AppUI.getColor0());
                return lbl;
            }
        });
        
        
        //This is the wallet table size 
        JScrollPane scrollPane = new JScrollPane(table);
        AppUI.setSize(scrollPane, 660, 275);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {       
            @Override
            protected JButton createDecreaseButton(int orientation) {
                TriangleButton jbutton = new TriangleButton(false);
                AppUI.setHandCursor(jbutton);
                jbutton.setContentAreaFilled(false);
                jbutton.setFocusPainted(false);
            
                return jbutton;
            }

            @Override    
            protected JButton createIncreaseButton(int orientation) {
                TriangleButton jbutton = new TriangleButton(true);
                AppUI.setHandCursor(jbutton);
                jbutton.setContentAreaFilled(false);
                jbutton.setFocusPainted(false);
            
                return jbutton;
            }
            
         @Override 
            protected void configureScrollBarColors(){
                this.trackColor = AppUI.getColor1();
                this.thumbColor = AppUI.getColor0();
            }
        });
        
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        return scrollPane;
        
    }
    
    
    public static JLabel getHyperLink(String name, String url, int fontSize) {
        final String fname = name;
        final String furl = url;
        
        JLabel l = new JLabel(name);
        if (fontSize == 0)
            AppUI.setCommonFont(l);
        else
            AppUI.setFont(l, fontSize);
        
        AppUI.setColor(l, AppUI.getColor0());
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
    
}
