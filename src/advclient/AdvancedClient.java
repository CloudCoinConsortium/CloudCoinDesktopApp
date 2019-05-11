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
import global.cloudcoin.ccbank.ServantManager.ServantManager;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

    String version = "2.0.0";

    JPanel headerPanel;
    JPanel mainPanel;
    JPanel corePanel;
    JPanel wpanel;
    
    int tw = 1208;
    int th = 726;    
    
 //   int tw = 870;
 //   int th = 524;
    
    
    int headerHeight;
        
    ProgramState ps;
    ServantManager sm;
    WLogger wl;
    
    
    MyButton continueButton;
    
    public AdvancedClient() {
        initSystem();
                
        AppUI.init(tw, th);
        
        headerHeight = th / 10;
        
        
        
        initMainScreen();
        
        if (!ps.errText.equals("")) {
            mainPanel.add(new JLabel("Failed to init app: " + ps.errText));
            return;
        }
        
        initHeaderPanel();
        initCorePanel();
        
        
        mainPanel.add(headerPanel);
        mainPanel.add(corePanel);

        
        
        showScreen();
    }

    public void initSystem() {
        wl = new WLogger();
        ps = new ProgramState();

        String home = System.getProperty("user.home");
        home += "\\DebugX";
        
        sm = new ServantManager(wl, home);
        if (!sm.init()) 
            ps.errText = "Failed to init ServantManager";
        
    }
    
    public void clear() {
        corePanel.removeAll();
        corePanel.repaint();
        corePanel.revalidate();
    }
    
    public void initMainScreen() {
        mainPanel = new JPanel();
       
        AppUI.setBoxLayout(mainPanel, true);
        AppUI.setSize(mainPanel, tw, th);
        AppUI.setBackground(mainPanel, AppUI.getColor1());
    
        JFrame mainFrame = AppUI.getMainFrame();
        mainFrame.setContentPane(mainPanel);
    }
    
    public void initHeaderPanel() {
        
        // Init header
        headerPanel = new JPanel();
        AppUI.setBoxLayout(headerPanel, false);
        AppUI.setSize(headerPanel, tw, headerHeight);
        AppUI.setBackground(headerPanel, AppUI.getColor0());
        AppUI.alignLeft(headerPanel);
        AppUI.alignTop(headerPanel);
        
        // Space
        AppUI.vr(headerPanel, tw * 0.0082 * 2);
        
        // Add Logo
        ImageJPanel logoPanel = new ImageJPanel("CloudCoinLogo2.png");
        AppUI.noOpaque(logoPanel);
        AppUI.setSize(logoPanel, tw / 22.37);        
        headerPanel.add(logoPanel);
        
        // Space
        AppUI.vr(headerPanel, tw * 0.0082 * 2);
        
        // Init Label
        JLabel titleText = new JLabel("CloudCoin Wallet " + version);
        AppUI.setTitleSemiBoldFont(titleText, 32);
        headerPanel.add(titleText);
        
        
        if (ps.currentScreen == ProgramState.SCREEN_AGREEMENT) {
            // Pad 
            JPanel jpad = new JPanel();
            AppUI.noOpaque(jpad);
            headerPanel.add(jpad);
        } else {
            // Space
            AppUI.vr(headerPanel, tw * 0.0082 * 12);
     
            // Deposit Button
            JLabel dp = new JLabel("Deposit");      
            AppUI.setTitleBoldFont(dp, 28);
            //dp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            headerPanel.add(dp);

            // Space
            AppUI.vr(headerPanel, tw * 0.0082 * 10);
            
            // Transfer Button
            dp = new JLabel("Transfer");
            AppUI.setTitleBoldFont(dp, 28);
            //dp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            headerPanel.add(dp);
        
            // Space
            AppUI.vr(headerPanel, tw * 0.0082 * 20);
        }

        // Icon Support
        ImageJPanel icon = new ImageJPanel("Help_Support Icon.png");
        AppUI.setSize(icon, tw / 30.51);
        AppUI.noOpaque(icon);
        headerPanel.add(icon);
        
        // Space
        AppUI.vr(headerPanel, tw * 0.0082 * 2);
        
        // Icon Gear
        icon = new ImageJPanel("Gear icon.png");
        AppUI.setSize(icon, tw / 34.51);
        AppUI.noOpaque(icon);
        headerPanel.add(icon);
        
         // Space
        AppUI.vr(headerPanel, tw * 0.0082 * 2);
            
        // Icon Flag Wrapper
        JPanel flagWrapper = new JPanel();
        AppUI.noOpaque(flagWrapper);
        AppUI.setBoxLayout(flagWrapper, true);
        AppUI.hr(flagWrapper, 14);       
        icon = new ImageJPanel( "Brithish flag.png");
        AppUI.setSize(icon, tw / 28.23);
        AppUI.noOpaque(icon);
        flagWrapper.add(icon);
        headerPanel.add(flagWrapper);
                      
        // Space
        AppUI.vr(headerPanel, tw * 0.0082 * 2);       
    }
    
    
    public void initCorePanel() {
        corePanel = new JPanel();
        
        AppUI.setBoxLayout(corePanel, false);
        AppUI.noOpaque(corePanel);
        AppUI.alignLeft(corePanel);
        AppUI.setMargin(corePanel, 20);
    }
    
    public void showScreen() {
        
        clear();
        showWalletCreatedScreen();
     //   showSetEmailScreen();
     //   showUnderstandPasswordScreen();
        //ps.isDefaultWalletBeingCreated = true;
        //showCreateWalletScreen();
        //showSetPasswordScreen();
        if (1==1)             return;
        switch (ps.currentScreen) {
            case ProgramState.SCREEN_AGREEMENT:
                showAgreementScreen();
                break;
            case ProgramState.SCREEN_CREATE_FIRST_WALLET:
                showCreateWalletScreen();
                break;
            case ProgramState.SCREEN_DEFAULT:
                showDefaultScreen();
                break;
            case ProgramState.SCREEN_SET_PASSWORD:
                showSetPasswordScreen();
                break;
            case ProgramState.SCREEN_UNDERSTAND_PASSWORD:
                showUnderstandPasswordScreen();
                break;
            case ProgramState.SCREEN_SET_EMAIL:
                showSetEmailScreen();
                break;
            case ProgramState.SCREEN_WALLET_CREATED:
                showWalletCreatedScreen();
                break;
        }
    }
  
    public void maybeShowError(JPanel p) {
        if (!ps.errText.isEmpty()) {
            AppUI.hr(p, 10);
            
            JLabel err = new JLabel(ps.errText);
            AppUI.setFont(err, 16);
            AppUI.setColor(err, AppUI.getErrorColor());
            AppUI.alignCenter(err);
            
            AppUI.hr(p, 2);
            p.add(err);
            
            ps.errText = "";
        }
    }
    
    public void showSetEmailScreen() {
        JPanel subInnerCore = getModalJPanel("Type Coin Recovery Email");
        maybeShowError(subInnerCore);
        
           // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        // Password Label
        JLabel x = new JLabel("Email");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);

        MyTextField tf0 = new MyTextField("Email", false);
        c.insets = new Insets(0, 0, 16, 0);
        c.gridx = 0;
        c.gridy = 1;
        gridbag.setConstraints(tf0.getTextField(), c);
        ct.add(tf0.getTextField());
        
        
        // Confirm Password Label
        x = new JLabel("Confirm Email");
        AppUI.setCommonFont(x);
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        MyTextField tf1 = new MyTextField("Confirm Email", false);
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = 3;
        gridbag.setConstraints(tf1.getTextField(), c);
        ct.add(tf1.getTextField());
                        
        // Buttons
        final MyTextField ftf0 = tf0;
        final MyTextField ftf1 = tf1;
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String p0 = ftf0.getText();
                String p1 = ftf1.getText();
   
                if (p0.isEmpty() || p1.isEmpty()) {
                    ps.errText = "Please fill out both fields";
                    showScreen();
                    return;
                }
                
                if (!p0.equals(p1)) { 
                    ps.errText = "Emails do not match";
                    showScreen();
                    return;
                }    
                
                ps.typedEmail = p0;
                ps.currentScreen = ProgramState.SCREEN_WALLET_CREATED;
                
                showScreen();                
            }
        });
  
        
        subInnerCore.add(bp);    
    }
    
    public void showWalletCreatedScreen() {
        JPanel subInnerCore = getModalJPanel("Wallet created");
        maybeShowError(subInnerCore);
        
        // if not ok return
        //if (ps.typedPassword)
        
        JLabel res = new JLabel("Wallet was set with password encryption and email for coin recovery was set as Recovery@email.com");
        
    }
    
    
    public void showUnderstandPasswordScreen() {
        JPanel subInnerCore = getModalJPanel("Confirmation");

        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        // Checkbox
        MyCheckBox cb0 = new MyCheckBox("<html>I understand that I will lose access to my<br>CloudCoins if I lose my password</html>");
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 12, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(cb0.getCheckBox(), c);       
        ct.add(cb0.getCheckBox());
        
        MyCheckBox cb1 = new MyCheckBox("<html>I understand that no one can recover my password if<br>I lose or forget it</html>");
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(cb1.getCheckBox(), c);       
        ct.add(cb1.getCheckBox());
        
        MyCheckBox cb2 = new MyCheckBox("<html>I have written down or otherwise stored<br>my password</html>");
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        gridbag.setConstraints(cb2.getCheckBox(), c);       
        ct.add(cb2.getCheckBox());
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ps.cwalletRecoveryRequested) {
                    ps.currentScreen = ProgramState.SCREEN_SET_EMAIL;
                } else {
                    ps.currentScreen = ProgramState.SCREEN_WALLET_CREATED;
                }
                    
                showScreen();
            }
        });
        
        continueButton.disable();
        
        final MyCheckBox fcb0 = cb0;
        final MyCheckBox fcb1 = cb1;
        final MyCheckBox fcb2 = cb2;
               
        ItemListener il = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (fcb0.isChecked() && fcb1.isChecked() && fcb2.isChecked()) {
                    continueButton.enable();
                } else {
                    continueButton.disable();
                }
            }
        };
        
        cb0.addListener(il);
        cb1.addListener(il);
        cb2.addListener(il);
                
        subInnerCore.add(bp);
      //  final MyButton fbutton = button;
       
        
        
        
        
        
        
        
        System.out.println("tp="+ps.typedPassword);
        
    }
    
    public void showSetPasswordScreen() {
        JPanel subInnerCore = getModalJPanel("Create Wallet Password");
        maybeShowError(subInnerCore);
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        // Password Label
        JLabel x = new JLabel("Password");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);

        MyTextField tf0 = new MyTextField("Password");
        c.insets = new Insets(0, 0, 16, 0);
        c.gridx = 0;
        c.gridy = 1;
        gridbag.setConstraints(tf0.getTextField(), c);
        ct.add(tf0.getTextField());
        
        
        // Confirm Password Label
        x = new JLabel("Confirm Password");
        AppUI.setCommonFont(x);
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        MyTextField tf1 = new MyTextField("Confirm Password");
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = 3;
        gridbag.setConstraints(tf1.getTextField(), c);
        ct.add(tf1.getTextField());
                        
        // Buttons
        final MyTextField ftf0 = tf0;
        final MyTextField ftf1 = tf1;
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String p0 = ftf0.getText();
                String p1 = ftf1.getText();
   
                if (p0.isEmpty() || p1.isEmpty()) {
                    ps.errText = "Please fill out both fields";
                    showScreen();
                    return;
                }
                
                if (!p0.equals(p1)) { 
                    ps.errText = "Passwords do not match";
                    showScreen();
                    return;
                }    
                
                ps.typedPassword = p0;
                ps.currentScreen = ProgramState.SCREEN_UNDERSTAND_PASSWORD;
                
                showScreen();                
            }
        });
  
        
        subInnerCore.add(bp); 
        
    }
    
    public void showDefaultScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();
    }
    
    
    
    public void showCreateWalletScreen() {
        JLabel x;
        String str;
        MyTextField walletName = null;
              
        if (!ps.isDefaultWalletBeingCreated) {
            str = "Create Wallet";
        } else {
            str = "Create Default Wallet";
        }
        
        JPanel subInnerCore = getModalJPanel(str);
        maybeShowError(subInnerCore);
        
        // Outer Container
        JPanel oct = new JPanel();
        AppUI.setBoxLayout(oct, true);
        AppUI.noOpaque(oct);
        subInnerCore.add(oct);
        
        // Space
        AppUI.hr(oct, 22);
        
        if (!ps.isDefaultWalletBeingCreated) {
            // Horizontal Container for Wallet name
            JPanel hct = new JPanel();
            AppUI.setBoxLayout(hct, false);
            AppUI.setMargin(hct, 0, 28, 0, 0);
            AppUI.noOpaque(hct);
            oct.add(hct);
            AppUI.setSize(hct, tw / 2, 50);
        
            // Name Label        
            x = new JLabel("Name");
            AppUI.setCommonFont(x);
            hct.add(x);
        
            AppUI.vr(hct, 50);
        
            walletName = new MyTextField("Wallet Name", false);
            hct.add(walletName.getTextField());
        }

        // GridHolder Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        oct.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
 
        
        int y = 0;
        
        
        // Empty Label before "Yes/No"
        x = new JLabel();
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(18, 18, 0, 0);    
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y + 1;
        gridbag.setConstraints(x, c);
        ct.add(x);
          
        x = new JLabel("Yes");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        ct.add(x);
        
        x = new JLabel(" No");
        AppUI.setCommonFont(x);
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        // Y
        x = new JLabel("Password Protected Wallet");
        AppUI.setCommonFont(x);
        c.gridx = 0;
        c.gridy = y + 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        ButtonGroup passwordGroup = new ButtonGroup();
        MyRadioButton rb0 = new MyRadioButton();
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y + 2;
        gridbag.setConstraints(rb0.getRadioButton(), c);
        ct.add(rb0.getRadioButton());
        rb0.attachToGroup(passwordGroup);
        rb0.select();
        
        MyRadioButton rb1 = new MyRadioButton();
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y + 2;
        gridbag.setConstraints(rb1.getRadioButton(), c);
        ct.add(rb1.getRadioButton());
        rb1.attachToGroup(passwordGroup);
      
        // Next Y
        x = new JLabel("Enable Coin Recovery by Email");
        AppUI.setCommonFont(x);
        c.gridx = 0;
        c.gridy = y + 3;
        gridbag.setConstraints(x, c);
        ct.add(x);
           
        ButtonGroup recoveryGroup = new ButtonGroup();
        MyRadioButton rb2 = new MyRadioButton();
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y + 3;
        gridbag.setConstraints(rb2.getRadioButton(), c);
        ct.add(rb2.getRadioButton());
        rb2.attachToGroup(recoveryGroup);
        rb2.select();
        
        MyRadioButton rb3 = new MyRadioButton();
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y + 3;
        gridbag.setConstraints(rb3.getRadioButton(), c);
        ct.add(rb3.getRadioButton());
        rb3.attachToGroup(recoveryGroup);
              
        // Buttons
        final MyRadioButton frb0 = rb0;
        final MyRadioButton frb2 = rb2;
        final MyTextField fwalletName = walletName;
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.cwalletPasswordRequested = frb0.isSelected();
                ps.cwalletRecoveryRequested = frb2.isSelected();
                if (!ps.isDefaultWalletBeingCreated && fwalletName != null) {
                    if (fwalletName.getText().isEmpty()) {
                        ps.errText = "Wallet name is empty";
                        showScreen();
                        return;
                    }
                    
                    ps.typedWalletName = fwalletName.getText();
                }
                
                if (ps.cwalletPasswordRequested) {
                    ps.currentScreen = ProgramState.SCREEN_SET_PASSWORD;
                    showScreen();
                } 
                
                System.out.println("Continue = " + frb0.isSelected() + " x=" +frb2.isSelected());
            }
        });
        
        AppUI.hr(subInnerCore, 20);
        subInnerCore.add(bp); 
    }
    
    public JPanel getTwoButtonPanel(ActionListener al) {
        JPanel bp = new JPanel();
        AppUI.setBoxLayout(bp, false);
        AppUI.noOpaque(bp);
        
        MyButton cb = new MyButton("Cancel");
        cb.addListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
        
        bp.add(cb.getButton());           
        AppUI.vr(bp, 26);

        cb = new MyButton("Continue");
        cb.addListener(al);
        bp.add(cb.getButton());
        
        continueButton = cb;
        
        return bp;
    }
    
    public JPanel getRightPanel() {
        JPanel mwrapperPanel = new JPanel();
        
        AppUI.setBoxLayout(mwrapperPanel, true);
        AppUI.noOpaque(mwrapperPanel);
        AppUI.alignLeft(mwrapperPanel);
        AppUI.alignTop(mwrapperPanel);
        AppUI.setSize(mwrapperPanel, tw - 260, th);

        JPanel subInnerCore = AppUI.createRoundedPanel(mwrapperPanel);
        AppUI.setSize(subInnerCore, tw - 260, th - headerHeight - 120);
        
        corePanel.add(mwrapperPanel);
        
        return subInnerCore;
    }
    
    public void showLeftScreen() {
        JPanel lwrapperPanel = new JPanel();
        
        AppUI.setBoxLayout(lwrapperPanel, true);
        AppUI.noOpaque(lwrapperPanel);
        AppUI.alignLeft(lwrapperPanel);
        AppUI.alignTop(lwrapperPanel);
        
        // Panel with wallets
        wpanel = new JPanel();
        AppUI.alignTop(wpanel);
        AppUI.noOpaque(wpanel);
        AppUI.setBoxLayout(wpanel, true);
 
    
        JLayeredPane wallet = getWallet();
      
        wpanel.add(wallet);
        wallet = getWallet();
      
        wpanel.add(wallet);
        
            wallet = getWallet();
      
        wpanel.add(wallet);
        
        wallet = getWallet();
      
        wpanel.add(wallet);
        
        wallet = getWallet();
      
        wpanel.add(wallet);

        // Padding from the bottom
        AppUI.hr(wpanel, 120);

        JScrollPane scrollPane = new JScrollPane(wpanel);
        JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL) {
            @Override
            public boolean isVisible() {
                return true;
            }
        };

        scrollPane.setVerticalScrollBar(scrollBar);
        scrollPane.getVerticalScrollBar().setUnitIncrement(42);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        lwrapperPanel.add(scrollPane);  
        
        corePanel.add(lwrapperPanel);
    }
    
    public JLayeredPane getWallet() {
        // Pane
        JLayeredPane lpane = new JLayeredPane();
        AppUI.noOpaque(lpane);
        AppUI.setSize(lpane, 200, 140);
        AppUI.alignLeft(lpane);

        // Rounded Background
        JButton addBtn = new JButton("");
        addBtn.setBorder(new RoundedBorder(40));
        addBtn.setFocusPainted(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setBounds(0, 0, 200, 120);

        // Wrapper for label
        JPanel cx = new JPanel();
        AppUI.noOpaque(cx);
        AppUI.setBoxLayout(cx, true);
        AppUI.alignTop(cx);
        cx.setBounds(0,10,200,100);

        // Space
        AppUI.hr(cx, 10);
        
        // Label
        JLabel l = new JLabel("Default Wallet");
        AppUI.setFont(l, 22);
        AppUI.alignCenter(l);
        cx.add(l);
          
        // Space
        AppUI.hr(cx, 12);
        
        // Line wrapper (2 icons + string of coins)
        JPanel inner = new JPanel();
        AppUI.setBoxLayout(inner, false);
        AppUI.noOpaque(inner);
       
        // Icon wrapper
        JPanel jc = new JPanel();
        AppUI.setBoxLayout(jc, true);
        AppUI.noOpaque(jc);

        // Space
        AppUI.hr(jc, 18);
        
        // Icon
        ImageJPanel icon = new ImageJPanel("Cloud Icon.png");
        AppUI.setSize(icon, tw /34.51f);
        AppUI.noOpaque(icon);        
        jc.add(icon);
        
        inner.add(jc);
        
        // Space 
        AppUI.vr(inner, 18);
        
        // Amout of coins
        JLabel jxl = new JLabel("100,02");
        AppUI.setFont(jxl, 20);
        inner.add(jxl);

        // Space
        AppUI.vr(inner, 18);

        // Icon wrapper
        jc = new JPanel();
        AppUI.setBoxLayout(jc, true);
        AppUI.noOpaque(jc);
        AppUI.hr(jc, 2);
        
        // Icon
        icon = new ImageJPanel("Envelope.png");
        AppUI.setSize(icon, tw /38.51f);
        AppUI.noOpaque(icon);
        jc.add(icon);
        
        inner.add(jc);

        cx.add(inner);        
        
        lpane.add(addBtn, new Integer(1));
        lpane.add(cx, new Integer(2));

        return lpane;
    }
    
    public void showAgreementScreen() {
        
        JPanel subInnerCore = AppUI.createRoundedPanel(corePanel);
        
        // Title
        JLabel text = new JLabel("CloudCoin Wallet");
        AppUI.alignCenter(text);
        AppUI.setBoldFont(text, 24);
        subInnerCore.add(text);
 
        // Agreement Panel        
        JPanel agreementPanel = AppUI.createRoundedPanel(subInnerCore);
        AppUI.roundCorners(agreementPanel, AppUI.getColor3(), 20);
        AppUI.alignCenter(agreementPanel);
             
        // Title 
        text = new JLabel("Terms and Conditions");
        AppUI.alignCenter(text);
        AppUI.setBoldFont(text, 24);
        agreementPanel.add(text);
        
        // Space
        AppUI.hr(agreementPanel,  tw * 0.0082 * 2);
                
        // Text
        text = new JLabel("<html><div style='padding-right: 20px; width:" + (tw / 1.6) + "px'>" + AppUI.getAgreementText() + "</div></html>");
        AppUI.alignCenter(text);
        AppUI.setFont(text, 18);
              
        JPanel wrapperAgreement = new JPanel();
        AppUI.setBoxLayout(wrapperAgreement, true);
        AppUI.alignCenter(wrapperAgreement);
        AppUI.noOpaque(wrapperAgreement);
        wrapperAgreement.add(text);
        
        // Checkbox
        MyCheckBox cb = new MyCheckBox("I have read and agree with the Terms and Conditions");
        cb.setBoldFont();
        wrapperAgreement.add(cb.getCheckBox());
        
        // Space
        AppUI.hr(wrapperAgreement, 20);
        
        // JButton
        MyButton button = new MyButton("Continue");
        button.disable();
        wrapperAgreement.add(button.getButton());
        
        final MyButton fbutton = button;
        cb.addListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object o = e.getSource();
                
                if (o instanceof JCheckBox) {
                    JCheckBox cb = (JCheckBox) o;
                    if (cb.isSelected()) {
                        fbutton.enable();
                    } else {
                        fbutton.disable();
                    }  
                }
            }
        });
        
        button.addListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.currentScreen = ProgramState.SCREEN_CREATE_FIRST_WALLET;
                showScreen();
            }
        });
                
        // ScrollBlock
        JScrollPane scrollPane = new JScrollPane(wrapperAgreement);
        JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL) {
            @Override
            public boolean isVisible() {
                return true;
            }
        };

        scrollPane.setVerticalScrollBar(scrollBar);
        scrollPane.getVerticalScrollBar().setUnitIncrement(42);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);     
        agreementPanel.add(scrollPane);
      
        subInnerCore.add(agreementPanel);
    }
    
    public JPanel getModalJPanel(String title) {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();

        JPanel xpanel = new JPanel(new GridBagLayout());
        AppUI.noOpaque(xpanel);
        rightPanel.add(xpanel);
        
        
        JPanel subInnerCore = AppUI.createRoundedPanel(xpanel, Color.WHITE, 20);
        AppUI.setSize(subInnerCore, tw/2, (int) (th/1.8));
      //  AppUI.alignCenter(subInnerCore);
        
        // Title
        JLabel ltitle = AppUI.getTitle(title);
        subInnerCore.add(ltitle);
        
        return subInnerCore;
    }
    /*
   
    
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
       // for (int i = 0; i < 10; i++) {
         //   panel.add(new JButton("Hello-" + i));
           // panel.add(ui.hr(4));
        //}
       
        
      
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
       
       // JComboBox comboBox = new JComboBox();
      //  ui.setSize(comboBox, 300, 50);
  
  //      comboBox.addItem("None");
    //    comboBox.addItem("None1");
      //  comboBox.addItem("None2");
//        comboBox.setBounds(0,10,1200,120);
        
  //      comboBox.setOpaque(false);
    //    comboBox.setBackground(new Color(0x775FA8FF));
        
        
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
        
    
        */
     
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
    //}
    
    
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