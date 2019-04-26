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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;






/**
 * 
 */
public class AdvancedClient implements ActionListener, ComponentListener {

    AppUI ui;
    JPanel mainPanel;
    
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
        
        headerPanel.add(ui.vr(tw * 0.0082));
        ImageJPanel logoPanel = new ImageJPanel("logo.jpg");
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
    
    
    public void showCore() {
 
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
        
       
        
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setOpaque(false);
        for (int i = 0; i < 10; i++) {
            panel.add(new JButton("Hello-" + i));
            panel.add(ui.hr(4));
        }
       
        
        JLayeredPane lpane = new JLayeredPane();
        lpane.setOpaque(false);
  //       lpane.setBounds(0, 0, 600, 400);
         ui.setSize(lpane, 200, 200);
       //  lpane.setBorder(BorderFactory.createTitledBorder(
         //                           "Move the Mouse to Move Duke"));
        
            JButton addBtn = new JButton("ghhgfhgfhfg+");
  //  addBtn.setBounds(x_pos, y_pos, 30, 25);
    addBtn.setBorder(new RoundedBorder(40)); //10 is the radius
    addBtn.setForeground(Color.BLUE);
     addBtn.setBackground(Color.RED);
     //addBtn.setOpa
      //  addBtn.setOpaque(true);
addBtn.setFocusPainted(false);
//addBtn.setBorderPainted(false);
addBtn.setContentAreaFilled(false);
addBtn.setBounds(0,10,200,100);
//Border(BorderFactory.createEmptyBorder(0,0,0,0));
  

        JPanel cx = new JPanel();
//cx.setBackground(Color.RED);
        cx.setOpaque(false);
   cx.setLayout(new BoxLayout(cx, BoxLayout.PAGE_AXIS));
//ui.setSize(cx, 200, 100);
   cx.add(ui.hr(10));
        JLabel l = new JLabel("xxx1");
             l.setForeground(Color.BLUE);
             l.setBackground(Color.YELLOW);
       // ui.setFont(l);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
       // cx.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        //ui.setSize(l, 200, 100);
        cx.setBounds(0,10,200,100);
        //addBtn.add(l);

        cx.add(l);
        
        lpane.add(addBtn, new Integer(1));
        lpane.add(cx, new Integer(2));
        //lpane.moveToFront(cx);

        panel.add(lpane);
     //   panel.add(addBtn);
        
        JScrollPane scrollPane = new JScrollPane(panel);
         JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL) {

                    @Override
                    public boolean isVisible() {
                        return true;
                    }
                };
         
         //scrollBar.setBackground(Color.white);
          scrollPane.setVerticalScrollBar(scrollBar);
          scrollPane.getViewport().setOpaque(false);
        //scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
       // scrollPane.setBounds(50, 30, 300, 50);
        //JPanel contentPane = new JPanel(null);
        scrollPane.setBackground(Color.YELLOW);
        corePanel.add(scrollPane);
        
        
        
        mainPanel.add(corePanel);
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