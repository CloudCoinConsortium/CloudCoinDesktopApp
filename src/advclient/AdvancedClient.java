package advclient;

import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.ServantRegistry;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.spi.ServiceRegistry;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;






/**
 * 
 */
public class AdvancedClient implements ActionListener, ComponentListener {

    AppUI ui;
    JPanel mainPanel;
    JPanel wpanel;
    
    int tw = 1208;
    int th = 726;
    
    public AdvancedClient() {
        ui = new AppUI(tw, th);
        System.out.println("HELLO");
        
        showScreen();
        addHeader();
        //addHeader();
        //addHeader();
     //     addHeader();
        mainPanel.add(ui.hr(10));
        showCore();
    }

    
    
    
    public void showScreen() {
        mainPanel = new JPanel();
       
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        ui.setSize(mainPanel, tw, th);
        //ui.align(mainPanel);
        mainPanel.setBackground(ui.getBgColor());
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
      //   mainPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        
        JFrame mainFrame = ui.getMainFrame();
        //mainFrame.add(mainPanel, BorderLayout.EAST);
        mainFrame.setContentPane(mainPanel);
    }
    
    public void addHeader() {
        JPanel headerPanel = new JPanel();
        
        
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.LINE_AXIS));
        ui.setSize(headerPanel, tw, th / 10);
        
        
        //ui.align(headerPanel);
        headerPanel.setBackground(ui.getHeaderBgColor());
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        headerPanel.add(ui.vr(tw * 0.0082 * 2));
       // ImageJPanel logoPanel = new ImageJPanel("logo.jpg");
        ImageJPanel logoPanel = new ImageJPanel("CloudCoinLogo2.png");
        logoPanel.setOpaque(false);
        ui.setSize(logoPanel, tw / 22.37);
        headerPanel.add(logoPanel);
        
        
        headerPanel.add(ui.vr(tw * 0.0082 * 2));
        JLabel jl = new JLabel("Total coins: ");
        jl.setForeground(Color.WHITE);
        ui.setFont(jl);
        headerPanel.add(jl);
        
        jl = new JLabel("1,4242");
        jl.setForeground(Color.WHITE);
        ui.setRegFont(jl);
        headerPanel.add(jl);
        
        headerPanel.add(ui.vr(tw * 0.0082));
        jl = new JLabel("<html><sup>CC</sup></html>");
        ui.setSize(jl, 100);
        jl.setForeground(Color.WHITE);
        //ui.setRegFont(jl);
        headerPanel.add(jl);
        
        
        
        headerPanel.add(ui.vr(tw * 0.0082 * 8));
        JLabel dp = new JLabel("Deposit");
        dp.setForeground(Color.WHITE);
        ui.setFont(dp);
        dp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerPanel.add(dp);
        
        
        
        headerPanel.add(ui.vr(tw * 0.0082 * 10));
        JLabel tr = new JLabel("Transfer");
        tr.setForeground(Color.WHITE);
        ui.setFont(tr);
        tr.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerPanel.add(tr);
        
        
        
        headerPanel.add(ui.vr(tw * 0.0082 * 20));
        
        
        
        
        ImageJPanel icon = new ImageJPanel("Gear icon.png");
        ui.setSize(icon, tw / 34.51);
        icon.setOpaque(false);
        headerPanel.add(icon);
        headerPanel.add(ui.vr(tw * 0.0082 * 2));
        
        
        
        JPanel jp = new JPanel();
        jp.setOpaque(false);
        jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
        jp.add(ui.hr(14));
       
        
        
        
        icon = new ImageJPanel("Help_Support Icon.png");
        ui.setSize(icon, tw / 30.51);
        icon.setOpaque(false);
        headerPanel.add(icon);
        headerPanel.add(ui.vr(tw * 0.0082 * 2));
        
        
        
        jp = new JPanel();
        jp.setOpaque(false);
        jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
        jp.add(ui.hr(14));
        
        
        
        
        
        
        
        
        ImageJPanel icon2 = new ImageJPanel("Brithish flag.png");
        
        icon2.add(ui.hr(120));
        ui.setSize(icon2, tw / 28.23);
        icon2.setOpaque(false);
        //ui.setMargins(icon2, 20,20,20,20);
       
          jp.add(icon2);
        headerPanel.add(jp);
        
        headerPanel.add(ui.vr(tw * 0.0082 * 2));
        //JPanel hcontainerPanel = new JPanel();
        //headerPanel.add(hcontanerPanel);
        
         //headerPanel.add(ui.kr(tw * 0.0082 * 2));
        mainPanel.add(headerPanel);
        
    }
    
    
    public void showWallet() {
          JLayeredPane lpane = new JLayeredPane();
        lpane.setOpaque(false);
  //       lpane.setBounds(0, 0, 600, 400);
        lpane.setAlignmentY(Component.TOP_ALIGNMENT);
         ui.setSize(lpane, 200, 140);
       //  lpane.setBorder(BorderFactory.createTitledBorder(
         //                           "Move the Mouse to Move Duke"));
        
            JButton addBtn = new JButton("");
  //  addBtn.setBounds(x_pos, y_pos, 30, 25);
    addBtn.setBorder(new RoundedBorder(40)); //10 is the radius
   // addBtn.setForeground(Color.BLUE);
  //   addBtn.setBackground(Color.RED);
     //addBtn.setOpa
      //  addBtn.setOpaque(true);
addBtn.setFocusPainted(false);
//addBtn.setBorderPainted(false);
addBtn.setContentAreaFilled(false);
addBtn.setBounds(0,10,200,120);
//Border(BorderFactory.createEmptyBorder(0,0,0,0));
  

        JPanel cx = new JPanel();
//cx.setBackground(Color.RED);
        cx.setOpaque(false);
   cx.setLayout(new BoxLayout(cx, BoxLayout.PAGE_AXIS));
//ui.setSize(cx, 200, 100);
   cx.add(ui.hr(10));
        JLabel l = new JLabel("Default Wallet");
        ui.setRegFont(l, 22);
           //  l.setForeground(new Color(0x000000CC));
          //   l.setBackground(Color.YELLOW);
       // ui.setFont(l);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
       // cx.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        //ui.setSize(l, 200, 100);
        cx.setBounds(0,10,200,100);
        //addBtn.add(l);

        cx.add(l);
          cx.add(ui.hr(12));
        
        
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.LINE_AXIS));
        inner.setOpaque(false);
        
        
        
        JPanel jc = new JPanel();
        jc.setLayout(new BoxLayout(jc, BoxLayout.PAGE_AXIS));
        jc.setOpaque(false);
        jc.add(ui.hr(18));
        
        ImageJPanel icon = new ImageJPanel("Cloud Icon.png");
        icon.setImageSize((int)(tw/34.51));
        ui.setSize(icon, tw /34.51f);
        icon.setOpaque(false);
        
        jc.add(icon);
        //inner.add(icon);
        inner.add(jc);
        inner.add(ui.vr(12));
        
        
        
        
        
         JLabel jxl = new JLabel("100,02");
        ui.setRegFont(jxl, 20);
        inner.add(jxl);
        inner.add(ui.vr(18));
        
        
        
        
        
        
        
        
       
        jc = new JPanel();
        jc.setLayout(new BoxLayout(jc, BoxLayout.PAGE_AXIS));
        jc.setOpaque(false);
        jc.add(ui.hr(2));
        
        icon = new ImageJPanel("Envelope.png");
        icon.setImageSize((int)(tw/38.51));
        ui.setSize(icon, tw /38.51f);
        icon.setOpaque(false);
        
        
        jc.add(icon);
        //inner.add(icon);
        inner.add(jc);
        
        
        
        //inner.add(ui.vr(tw * 0.0082 * 2));
        
       
        /*
        icon = new ImageJPanel("Envelope Icon.png");
        ui.setSize(icon, tw / 34.51);
        icon.setOpaque(false);
        inner.add(icon);
        */
      //  inner.setBackground(Color.RED);   
        cx.add(inner);
        
        
        
        
        
        
        
        
        
        
        lpane.add(addBtn, new Integer(1));
        lpane.add(cx, new Integer(2));
        //lpane.moveToFront(cx);
        wpanel.add(lpane);
      //  wpanel.setAlignmentY(Component.TOP_ALIGNMENT);
    }
    
    
    public void showCore() {
 
        JPanel cPanel = new JPanel();
        cPanel.setLayout(new BoxLayout(cPanel, BoxLayout.LINE_AXIS));
       // cPanel.setBackground(Color.RED);
        cPanel.setOpaque(false);
         cPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      //  ui.setSize(cPanel, (int) (tw/5.5f), (int) (th/1.2f));
        
        JPanel corePanel = new JPanel();
              
        corePanel.setLayout(new BoxLayout(corePanel, BoxLayout.PAGE_AXIS));
        ui.setSize(corePanel, (int) (tw/5.5f), (int) (th/1.2f));
     //   corePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
    //    ui.align(corePanel);
        //headePanel.setBackground(ui.getHeaderBgColor());
        
    corePanel.setOpaque(false);
        
        //corePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        //c.setAlignmentX(Component.LEFT_ALIGNMENT);
        corePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      //  corePanel.setBackground(Color.BLACK);
          //    c.setBackground(Color.RED);
        //c.add(corePanel);
          //    ImageJPanel icon2 = new ImageJPanel("Brithish flag.png");
        
       
        
        
        wpanel = new JPanel();
        wpanel.setLayout(new BoxLayout(wpanel, BoxLayout.PAGE_AXIS));
        wpanel.setOpaque(false);
       /* for (int i = 0; i < 10; i++) {
            panel.add(new JButton("Hello-" + i));
            panel.add(ui.hr(4));
        }
       
        */
      
       showWallet();
    //    showWallet();
      //   showWallet();
          showWallet();
     //   panel.add(addBtn);
        
        JScrollPane scrollPane = new JScrollPane(wpanel);
         JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL) {

                    @Override
                    public boolean isVisible() {
                        return true;
                    }
                };
         
         //scrollBar.setBackground(Color.white);
        // scrollPane.setVerticalAlignment(Component.TOP_ALIGNMENT);
          scrollPane.setVerticalScrollBar(scrollBar);
          scrollPane.getViewport().setOpaque(false);
          scrollPane.getViewport().setBackground(Color.RED);
           scrollPane.setOpaque(false);
           scrollPane.setBorder(BorderFactory.createEmptyBorder());
        //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
       // scrollPane.setBounds(50, 30, 300, 50);
        //JPanel contentPane = new JPanel(null);
        //scrollPane.setBackground(Color.YELLOW);
        corePanel.add(scrollPane);
        
        corePanel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        cPanel.add(corePanel);
        
        // cPanel.add(scrollPane);
      //  cPanel.add(ui.vr(10));
        
        JPanel gc = new JPanel();
        gc.setLayout(new BoxLayout(gc, BoxLayout.PAGE_AXIS));
       // gc.add(ui.hr(20));
        gc.setOpaque(false);
        gc.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.PAGE_AXIS));
        
        
          g.setBorder(new RoundedBorder(40, 0x5FA8FF)); //10 is the radius
    //g.setForeground(Color.BLUE);
     //g.setBackground(new Color(0xFFFFFFFF) );
     g.setOpaque(false);
     //addBtn.setOpa
      //  addBtn.setOpaque(true);
//g.setFocusPainted(false);
//addBtn.setBorderPainted(false);
//g.setContentAreaFilled(false);
     
  //   g.setFocusPainted(false);
//addBtn.setBorderPainted(false);
//g.setContentAreaFilled(false);
     
     
//g.setBounds(0,10,1200,120);
ui.setSize(g, tw - (int) (tw/5.5f) - 20, (int) (th/1.22f));
       // g.setAlignmentX(Component.RIGHT_ALIGNMENT);
    
        
        gc.add(g);
        
        
        
        
        
        
        JPanel comboBox = new RoundedCornerComboBox().makeUI(ui);
        //ui.setSize(comboBox, 300, 50);
        /*
        JComboBox comboBox = new JComboBox();
        ui.setSize(comboBox, 300, 50);
  //      comboBox.setRenderer(new IconListRenderer(icons));
        comboBox.addItem("None");
        comboBox.addItem("None1");
        comboBox.addItem("None2");
        comboBox.setBounds(0,10,1200,120);
        
        comboBox.setOpaque(false);
        comboBox.setBackground(new Color(0x775FA8FF));
        //comboBox.setBorder(new RoundedBorder(20));
        */
        g.setAlignmentX(Component.CENTER_ALIGNMENT);
        // g.setAlignmentY(Component.TOP_ALIGNMENT);
        JLabel l = new JLabel("Wallet 2 - 100 CC");
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        ui.setFont(l, 24);
        g.add(l);
        
        
         l = new JLabel("Recovery email cloudcoin@cloudcoin.global");
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
          l.setForeground(Color.BLACK);
        ui.setRegFont(l, 14);
        g.add(l);
        
        
        g.add(ui.hr(20));
        
        
         l = new JLabel("This is some text that the user should be made aware of");
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setForeground(Color.BLACK);
        ui.setRegFont(l, 20);
        g.add(l);
        
         g.add(ui.hr(10));
        
        g.add(comboBox);
        
       g.add(ui.hr(10));
        
        l = new JLabel("This is another text that the user should be made aware of");
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setForeground(Color.BLACK);
        ui.setRegFont(l, 20);
        g.add(l);
        
        
        
        comboBox = new RoundedCornerComboBox().makeUI(ui);
        JPanel ct = new JPanel();
        ui.setSize(ct, 800, 380);
        ct.setAlignmentY(Component.TOP_ALIGNMENT);
        ct.setOpaque(false);
        //ui.setSize(ct, 600, 100);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; 
      
        
        ct.setLayout(gridbag);
        //ct.setOpaque(false);
        
        JLabel x= new JLabel("Please choose from this dropdown: ");
        //x.setAlignmentX(Component.LEFT_ALIGNMENT);
        //x.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        ui.setRegFont(x, 20);
        x.setForeground(Color.BLACK);
        //comboBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
        comboBox.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
        //ui.setSize(x,100);
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(16, 16, 0, 0);
    //    c.weightx = 0.5;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        
        ct.add(x);
        
           //  c.weightx = 0.5;
       // c.gridx = 1;
        //c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(comboBox, c);
        ct.add(comboBox);
        
        
        
        
        
        x= new JLabel("own: ");
        //x.setAlignmentX(Component.LEFT_ALIGNMENT);
        //x.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        ui.setRegFont(x, 20);
        x.setForeground(Color.BLACK);
        
             c.gridx = 0;
        c.gridy = 1;
         gridbag.setConstraints(x, c);
        
        ct.add(x);
        
        
        
        comboBox = new RoundedCornerComboBox().makeUI(ui);
              c.gridx = 1;
        c.gridy = 1;
         gridbag.setConstraints(comboBox, c);
        
        ct.add(comboBox);
        
        //g.setAlignmentY(Component.TOP_ALIGNMENT);
        
        
        
        
        
          x= new JLabel("Inssssssssssput: ");
      
        ui.setRegFont(x, 20);
        x.setForeground(Color.BLACK);
        
             c.gridx = 0;
        c.gridy = 2;
         gridbag.setConstraints(x, c);
        
        ct.add(x);
        
        
        JPanel xx = new JPanel();
        xx.setLayout(new BoxLayout(xx,BoxLayout.PAGE_AXIS ));
        xx.setOpaque(false);
           JTextField i = new RoundedCornerInput(4);
           //ui.setSize(i, 150, 50);
              c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        xx.add(i);
         gridbag.setConstraints(xx, c);
        
        ct.add(xx);
        //ct.setOpaque(true);
        
        
        
        
        
        
        
        
        
                x= new JLabel("Radio: ");
      
        ui.setRegFont(x, 20);
        x.setForeground(Color.BLACK);
        
             c.gridx = 0;
        c.gridy = 3;
         gridbag.setConstraints(x, c);
        
        ct.add(x);
        
        
        
        
        
          
           JPanel r = new MyRadioButton().makeUI(ui);
           //ui.setSize(r, 350, 50);
              c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        
         gridbag.setConstraints(r, c);
        
        ct.add(r);
        
        
        
        
         x= new JLabel("Checkbox: ");
      
        ui.setRegFont(x, 20);
        x.setForeground(Color.BLACK);
        
             c.gridx = 0;
        c.gridy = 4;
         gridbag.setConstraints(x, c);
        
        ct.add(x);
        
        
        
        
        JPanel cr = new MyCheckbox().makeUI(ui);
           //ui.setSize(r, 350, 50);
              c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        
        
         gridbag.setConstraints(cr, c);
        
        ct.add(cr);
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        g.add(ct);
        
        
     
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        cPanel.add(gc);
        
        
        
        
        
        
        
        mainPanel.add(cPanel);
       // mainPanel.add(scrollPane);
        
        if (1==1)
            return;
        
    
        
     
      /*  
       JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(50, 30, 300, 50);
        JPanel contentPane = new JPanel(null);
        contentPane.setPreferredSize(new Dimension(500, 400));
        contentPane.add(scrollPane);
    */
        /*
        DrawRoundRectangle bx1 = new DrawRoundRectangle();
        bx1.setLayout(new BoxLayout(bx1, BoxLayout.PAGE_AXIS ));
        
        
        JLabel l = new JLabel("xxx1");
             l.setForeground(Color.WHITE);
        ui.setFont(l);
        bx1.add(l);
        */
   //     l = new JLabel("xxx2");
     //   bx1.add(l);
               
        
      //  corePanel.add(bx1);
        
        
        
       // frame.pack();
       // frame.setSize(420, 300);
        
        
        
        
        
        
      //  mainPanel.add(corePanel, Component.LEFT_ALIGNMENT);
    }
    
    
    public void actionPerformed(ActionEvent e) {
        
    }
    
    public void componentHidden(ComponentEvent e) {
        
    }
    
    public void componentShown(ComponentEvent e) {
        
    }
    
    public void componentMoved(ComponentEvent e) {
        
    }
    
    public void componentResized(ComponentEvent e) {
        
    }
    
    
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
           for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                   // javax.swing.UIManager.setLookAndFeel(info.getClassName());
                   break;
                }
           }   
           UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
          
        } catch (InstantiationException ex) {
 
           
        } catch (IllegalAccessException ex) {
       
        } catch (javax.swing.UnsupportedLookAndFeelException ex) { 
      
        }
          
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new AdvancedClient();
            }
        });
    }
   
    
   
}