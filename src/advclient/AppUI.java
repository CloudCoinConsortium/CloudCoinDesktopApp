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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.io.InputStream;
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
    
    static int tw, th;
    static double ratio;
    
    
    static Font regFont, semiBoldFont, boldFont;
    static Font osRegFont, osSemiBoldFont;
    
    public static void init(int tw, int th) {
        AppUI.tw = tw;
        AppUI.th = th;
        
        AppUI.ratio = tw / th;
        
        try {
            ClassLoader cl;
            
            cl = AppUI.class.getClassLoader();
            
            semiBoldFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-SemiBold.otf"));
            boldFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Bold.otf"));
            regFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/Montserrat-Regular.otf"));
            
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(regFont);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(boldFont);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(semiBoldFont);
            
            
            osRegFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/OpenSans-Regular.ttf"));
            osSemiBoldFont = Font.createFont(Font.TRUETYPE_FONT, cl.getResourceAsStream("resources/OpenSans-Semibold.ttf"));
            
        } catch(Exception e){
            System.out.println("Failed to load font: " + e.getMessage());
        }
        
        
    }
    
    public static String getAgreementText() {
        String agreement = "<br><p>Please read these terms and conditions carefully before using this application "
                + "distributed by the CloudCoin Consortium.</p><br>"
                + "<p>Conditions of Use</p><br>"
                + "<p>The CloudCoin Consortium provides this software free of cost, and as is. "
                + "Every time you utilize this application or its services you accept the following conditions. "
                + "This is why the CloudCoin Consortium urges you to read them carefully.</p>"
                + "<p>Privacy Policy</p><br>"
                + "<p>The CloudCoin consortium does not collect or store any user data. "
                + "All transactions are anonymous and no user information is ever collected "
                + "by the RAIDA or the CloudCoin Consortium.</p><br>"
                + "<p>Copyright</p><br>"
                + "Content provided to you by the CloudCoin Consortium is developed by our partner RAIDAtech. "
                + "All graphics, logos, images, and text herein is considered property of the "
                + "CloudCoin Consortium and protected by international copyright laws. "
                + "This software is considered open source and not owned by either "
                + "the CloudCoin Consortium, RAIDAtech, or any of its affiliates.</p><br>"
                + "<p>Communications</p><br>"
                + "<p>All communication with the CloudCoin Consortium, RAIDA, or RAIDAtech "
                + "affiliates is electronic. Any time you use the RAIDA to pown, send, or transfer coins"
                + " you are going to be communicating with us. If you subscribe to the newsletter "
                + "through the consortium you are going to receive regular emails from us. "
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
                + "governing use of this software.</p><br>"
                + "<p>Assignment</p><br>"
                + "<p>The CloudCoin Consortium shall be permitted to assign, transfer, "
                + "and subcontract its rights and/or obligations under these Terms without "
                + "any notification or consent required. However, you shall not be permitted "
                + "to assign, transfer, or subcontract any of your rights "
                + "and/or obligations under these Terms.</p><br>"
                + "<p>Entire Agreement</p><br>"
                + "<p>These Terms, including any legal notices and disclaimers contained "
                + "in this software, constitute the entire agreement between "
                + "the CloudCoin Consortium and you in relation to your use of this "
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
        return new Color(0x0061E1);
    }
    
    public static Color getColor1() {
        return new Color(0xC9DBEE);
    }
    
    public static Color getColor2() {
        return new Color(0xDFEAF5);
    }
    
    public static Color getColor3() {
        return new Color(0xBFFFFFFF, true);
    }
    
    public static Color getColor4() {
        return new Color(0x665FA8FF, true);
    }
    
    public static Color getColor5() {
        return Color.WHITE;
    }
    public static Color getColor6() {
        return new Color(0x5FA8FF);
    }
    public static Color getColor7() {
        return new Color(0x90C1FE);
    }
    
    public static Color getColor8() {
        return new Color(0xE5E5E5);
    }
    
    public static Color getColor9() {
        return new Color(0x5FA8FF);
    }
    
    public static Color getColor10() {
        return new Color(0x777777);
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
        c.setFont(regFont.deriveFont(Font.PLAIN, size));
        c.setForeground(Color.WHITE);
    }
    
    public static void setTitleBoldFont(Component c, int size) {
        c.setFont(boldFont.deriveFont(Font.BOLD, size));
        c.setForeground(Color.WHITE);
    }
    
    public static void setTitleSemiBoldFont(Component c, int size) {
        c.setFont(semiBoldFont.deriveFont(Font.PLAIN, size));
        c.setForeground(Color.WHITE);
    }
    
    public static void setFont(Component c, int size) {
        c.setFont(regFont.deriveFont(Font.PLAIN, size));
    }
    
    public static void setSemiBoldFont(Component c, int size) {
        c.setFont(semiBoldFont.deriveFont(Font.PLAIN, size));
    }
    
    public static void setBoldFont(Component c, int size) {
        c.setFont(boldFont.deriveFont(Font.PLAIN, size));
    }
    
    
    
    public static void setCommonFont(Component c) {
        AppUI.setFont(c, 25);
    }
    
    public static void setCommonBoldFont(Component c) {
        AppUI.setBoldFont(c, 25);
    }
    
    public static void setCommonTableFont(Component c) {
        c.setFont(osRegFont.deriveFont(Font.PLAIN, 14));
    }
    
    public static void setCommonBoldTableFont(Component c) {
        c.setFont(osSemiBoldFont.deriveFont(Font.PLAIN, 14));
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
        frame.setResizable(false);
        frame.setVisible(true);
        
        return frame;
    }
   
    public static JPanel createRoundedPanel(JPanel parent) {
        return createRoundedPanel(parent, AppUI.getColor2(), 60, 20);
    }
    
    public static JPanel createRoundedPanel(JPanel parent, Color color, int sideMargin) {
        return createRoundedPanel(parent, color, sideMargin, 20);
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
    
    
    /*
    
    
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
    
    */
}
