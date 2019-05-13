package advclient;

import advclient.common.core.Validator;
import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Backupper.BackupperResult;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Eraser.EraserResult;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.LossFixer.LossFixerResult;
import global.cloudcoin.ccbank.Receiver.ReceiverResult;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.ServantManager.ServantManager;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.Vaulter.VaulterResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.DNSSn;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.ServantRegistry;
import global.cloudcoin.ccbank.core.Wallet;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;






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
    
    Wallet[] wallets;
    
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
        
    public void showCoinsDone(int[][] counters) {
        System.out.println("DONE");
        
        Wallet w = wallets[ps.currentWalletIdx];
        JLabel cntLabel = (JLabel) w.getuiRef();
     
        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);
        
        w.setTotal(totalCnt);
        ps.counters = counters;
        String strCnt = AppCore.formatNumber(totalCnt);
        cntLabel.setText(strCnt);
        
        Thread t = new Thread(new Runnable() {
            public void run(){
                showCoinsGoNext();
            }
        });
        
        t.start();
    }
    
    
    public void showCoinsGoNext() {
        if (wallets.length > ps.currentWalletIdx + 1) {
            ps.currentWalletIdx++;
            sm.setActiveWallet(wallets[ps.currentWalletIdx].getName());
            if (!sm.getActiveWallet().isSkyWallet()) {
                sm.startShowCoinsService(new ShowCoinsCb());
            } else {
              System.out.println("new service sky");
            }       
        }      
    }
    
    public void setCounters(int[][] counters) {
        ps.counters = counters;
    }
    
    public void cbDone() {
        ps.cbState = ProgramState.CB_STATE_DONE;
    }
    
    public void initSystemUser() {
        if (ps.isDefaultWalletBeingCreated)
            ps.typedWalletName = Config.DIR_DEFAULT_USER;
        
        if (!sm.initUser(ps.typedWalletName, ps.typedEmail, ps.typedPassword)) {
            ps.errText = "Failed to init Wallet"; 
            return;
        }
            
        AppCore.copyTemplatesFromJar(ps.typedWalletName);
    }
    
    public boolean isActiveWallet(Wallet wallet) {
        return ps.currentWalletName.equals(wallet.getName());
    }
    
    public void setActiveWallet(String walletName) {    
        ps.currentWalletName = walletName;
        
        sm.setActiveWallet(walletName);
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
    
    public void resetState() {
        ps = new ProgramState();
    }
    
    public void showScreen() {
        
        clear();
        showTransactionsScreen();
    //    showCreateSkyWalletScreen();
     //   showPrepareToAddWalletScreen();
    //    showDefaultScreen();
      //  showWalletCreatedScreen();
      //  showSetEmailScreen();
     //   showUnderstandPasswordScreen();
        //ps.isDefaultWalletBeingCreated = true;
        //showCreateWalletScreen();
     //   showSetPasswordScreen();
        if (1==1)             return;
        switch (ps.currentScreen) {
            case ProgramState.SCREEN_AGREEMENT:
                resetState();
                showAgreementScreen();
                break;
            case ProgramState.SCREEN_CREATE_WALLET:
                showCreateWalletScreen();
                break;
            case ProgramState.SCREEN_DEFAULT:
                resetState();
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
            case ProgramState.SCREEN_PREPARE_TO_ADD_WALLET:
                showPrepareToAddWalletScreen();
                break;
            case ProgramState.SCREEN_CREATE_SKY_WALLET:
                showCreateSkyWalletScreen();
                break;
            case ProgramState.SCREEN_SHOW_TRANSACTIONS:
                showTransactionsScreen();
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
        
        // Space
        AppUI.hr(subInnerCore, 40);
        
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
               
        // Confirm Email Label
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
                
                initSystemUser();
                
                showScreen();                
            }
        });
  
        
        subInnerCore.add(bp);    
    }
    
    public void showWalletCreatedScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            return;
        }

        subInnerCore = getModalJPanel("Wallet created");
        AppUI.hr(subInnerCore, 82);
        
        JLabel res;
        if (!ps.typedPassword.equals("")) {
            res = AppUI.getCommonLabel("Wallet was set with password encryption");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);            
            if (!ps.typedEmail.equals("")) {
                res = AppUI.getCommonLabel("and email for coin recovery was set as");
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12);
                
                res = AppUI.getCommonBoldLabel(ps.typedEmail);
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12);               
            } else {
                res = AppUI.getCommonLabel("and no recovery email");
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12); 
            }
        } else if (!ps.typedEmail.equals("")) {
            res = AppUI.getCommonLabel("Coin recovery email was set as");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);
            
            res = AppUI.getCommonBoldLabel(ps.typedEmail);
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);       
        } else {
            res = AppUI.getCommonLabel("Wallet has been created successfully");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);
        }
            
        JPanel bp = getOneButtonPanel();     
        subInnerCore.add(bp);      
    }
    
    
    public void showUnderstandPasswordScreen() {
        JPanel subInnerCore = getModalJPanel("Confirmation");

        // Space
        AppUI.hr(subInnerCore, 40);
        
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
                    initSystemUser();  
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
    }
    
    public void showSetPasswordScreen() {
        JPanel subInnerCore = getModalJPanel("Create Wallet Password");
        maybeShowError(subInnerCore);
        
        // Space
        AppUI.hr(subInnerCore, 40);
        
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
    
    public void updateWalletAmount() {
        if (wallets == null)
            wallets = sm.getWallets();
        if (wallets.length > 0) {
            ps.currentWalletIdx = 0;
            sm.setActiveWallet(wallets[ps.currentWalletIdx].getName());
            if (!sm.getActiveWallet().isSkyWallet()) {
                sm.startShowCoinsService(new ShowCoinsCb());
            } else {
                //
            }      
        }
    }
    
    public void showDefaultScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();     
    }
    
    public void showTransactionsScreen() {
        showLeftScreen();
        
        setActiveWallet("Default Wallet");
        
        Wallet w = sm.getActiveWallet();
        
        JPanel rightPanel = getRightPanel();    
        
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        
        
        JLabel ltitle = AppUI.getTitle(w.getName() + " - " + w.getTotal() + " CC");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        JLabel trLabel = new JLabel("Transaction History");
        AppUI.setFont(trLabel, 18);
        AppUI.alignCenter(trLabel);
        ct.add(trLabel);
                
        AppUI.hr(ct, 20);
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
       /*
        
         UIManager.put("ScrollBar.thumb", new ColorUIResource(Color.black));
        
        
        UIManager.put("ScrollBar.track", new ColorUIResource(Color.black));
        
        
        UIManager.put("ScrollBar.trackHighlight", new ColorUIResource(Color.black));
        UIManager.put("ScrollPane.background", new ColorUIResource(Color.black));
        UIManager.put("ScrollPane.foreground", new ColorUIResource(Color.black));
        */
        
        UIManager.put("ScrollBar.background", new ColorUIResource(AppUI.getColor0()));
        UIManager.put("ScrollBar.foreground", new ColorUIResource(Color.YELLOW));
        /*
        ScrollBar.background
ScrollBar.foreground
ScrollBar.thumb
ScrollBar.thumbDarkShadow
ScrollBar.thumbHighlight
ScrollBar.thumbShadow
ScrollBar.track
ScrollBar.trackHighlight
ScrollPane.background
ScrollPane.foreground
        */
        
        
        
        
        
        
        
        
        
         Object[][] data = {
	    {"Kathy", "Smith",
	     "Snowboarding", new Integer(5), new Boolean(false)},
	    {"John", "Doe",
	     "Rowing", new Integer(3), new Boolean(true)},
	    {"Sue", "Black",
	     "Knitting", new Integer(2), new Boolean(false)},
	    {"Jane", "White",
	     "Speed reading", new Integer(20), new Boolean(true)},
	    {"Joe", "Brown",
	     "Pool", new Integer(10), new Boolean(false)}
        };
        
        String[] columnNames = {"Memo (note)",
                                "Date",
                                "Deposit",
                                "Withdraw",
                                "Total"};
        
        
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object
                value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                setForeground(Color.blue);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(getFont().deriveFont(50f));
                setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
                return this;
            }
        };
        
        final JTable table = new JTable(data, columnNames);
                table.setRowHeight(table.getRowHeight() + 150);
      //  table.getColumnModel().getColumn(1).setCellRenderer(r);
        //table.setIntercellSpacing(new Dimension (101,201));
        JTableHeader header = table.getTableHeader();
        AppUI.setBackground(header, AppUI.getColor0());
        AppUI.setColor(header, Color.WHITE);
        AppUI.setMargin(header, 12);
        AppUI.noOpaque(header);
        AppUI.setFont(table, 14);
        AppUI.setBoldFont(header, 14);
        //table.setPreferredScrollableViewportSize(new Dimension(500, 10));
        //table.setFillsViewportHeight(true);
    
        
        JPanel p = new JPanel();
        AppUI.setBoxLayout(p, true);
        AppUI.setMargin(p, 0);
        //p.add(table);
        
                 
        
        JScrollPane scrollPane = new JScrollPane(table);

        Image img;
          try {
               
            
                img = ImageIO.read(getClass().getClassLoader().getResource("resources/Envelope.png"));
                
            } catch (Exception ex) {
               return;
            }
 
        
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {   
        
            @Override
            protected JButton createDecreaseButton(int orientation) {
                TriangleButton jbutton = new TriangleButton();
               // AppUI.noOpaque(jbutton);
                //AppUI.setBackground(jbutton, AppUI.getColor0());
               // AppUI.setSize(jbutton, 20);
               // AppUI.setMargin(jbutton, 0);
               // jbutton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

               // return jbutton;
                //JButton jbutton = new JButton(new ImageIcon(img));
               //  JButton jbutton = new JButton("^");
                AppUI.setSize(jbutton, 30);
                //AppUI.setBackground(jbutton, AppUI.getColor1());
                //AppUI.noOpaque(jbutton);
                
                jbutton.setContentAreaFilled(false);
             
                jbutton.setFocusPainted(false);
                
                
                return jbutton;
            }

            @Override    
            protected JButton createIncreaseButton(int orientation) {
                JButton jbutton = new JButton();

                return jbutton;
            }
            
            @Override 
            protected void configureScrollBarColors(){
                this.trackColor = AppUI.getColor6();
                this.thumbColor = AppUI.getColor7();
            }
        });
        
        
        
        
        
        
        
        
        
      //  scrollPane.getVerticalScrollBar().setOpaque(false);
       // scrollPane.getVerticalScrollBar().setUnitIncrement(42);
       
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
       // scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
       // scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); 
     //   scrollPane.setVerticalScrollBar(scrollBar);
        
        //Add the scroll pane to this panel.
        ct.add(scrollPane);
        
         
          JButton jbutton = new JButton(new ImageIcon(img));
              //  AppUI.setSize(jbutton, 140);
               // AppUI.setBackground(jbutton, Color.BLACK);
               // AppUI.noOpaque(jbutton);
             //   jbutton.setOpaque(true);
                //jbutton.setBackground(Color.RED);
                
         
        //addBtn.setContentAreaFilled(false);
       
        rightPanel.add(ct);
      //  rightPanel.add(new TriangleButton());
      //  rightPanel.add(new JButton());
    }
    
    
    public void showPrepareToAddWalletScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();
        
        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);

        
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);
        
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        
        gct.add(ct);
        
        // Add local wallet button
        JPanel subInnerCore = AppUI.createRoundedPanel(ct, AppUI.getColor3(), 20, 40);
        AppUI.setSize(subInnerCore, (int) (tw/3), (int) (th/5));
        AppUI.setHandCursor(subInnerCore);
        
        // Title
        AppUI.hr(subInnerCore, 10);
        JLabel jt = new JLabel("Add Local Wallet");
        AppUI.setBoldFont(jt, 32);
        AppUI.alignCenter(jt);
        subInnerCore.add(jt);
        
        AppUI.hr(subInnerCore, 40);
        
        jt = new JLabel("Wallet folder will be created on");
        AppUI.setFont(jt, 18);
        AppUI.alignCenter(jt);
        subInnerCore.add(jt);
        
        jt = new JLabel("your computer or connected device");
        AppUI.setFont(jt, 18);
        AppUI.alignCenter(jt);
        subInnerCore.add(jt);
        
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                ps.isDefaultWalletBeingCreated = false;
                ps.currentScreen = ProgramState.SCREEN_CREATE_WALLET;
                showScreen();
            }   
            
            public void mouseEntered(MouseEvent e) {
                JPanel p = (JPanel) e.getSource();
                p = (JPanel) p.getParent();
                AppUI.roundCorners(p, AppUI.getColor5(), 40);
            }
            
            public void mouseExited(MouseEvent e) {
                JPanel p = (JPanel) e.getSource();
                p = (JPanel) p.getParent();
                AppUI.roundCorners(p, AppUI.getColor3(), 40);
            }
        };
            
        subInnerCore.addMouseListener(ma);
              
        // Space between buttons
        AppUI.hr(ct, 30);

        // Sky wallet button
        subInnerCore = AppUI.createRoundedPanel(ct, AppUI.getColor3(), 20, 40);
        AppUI.setSize(subInnerCore, (int) (tw/3), (int) (th/5));
        AppUI.setHandCursor(subInnerCore);
        
        // Title
        AppUI.hr(subInnerCore, 10);
        jt = new JLabel("Add Sky Wallet");
        AppUI.setBoldFont(jt, 32);
        AppUI.alignCenter(jt);
        subInnerCore.add(jt);
        
        AppUI.hr(subInnerCore, 40);
        
        jt = new JLabel("Sky Wallet will be created");
        AppUI.setFont(jt, 18);
        AppUI.alignCenter(jt);
        subInnerCore.add(jt);
        
        jt = new JLabel("on the Trusted Server");
        AppUI.setFont(jt, 18);
        AppUI.alignCenter(jt);
        subInnerCore.add(jt);
            
        ma = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                ps.isDefaultWalletBeingCreated = false;
                ps.currentScreen = ProgramState.SCREEN_CREATE_SKY_WALLET;
                showScreen();
            }   
            
            public void mouseEntered(MouseEvent e) {
                JPanel p = (JPanel) e.getSource();
                p = (JPanel) p.getParent();
                AppUI.roundCorners(p, AppUI.getColor5(), 40);
            }
            
            public void mouseExited(MouseEvent e) {
                JPanel p = (JPanel) e.getSource();
                p = (JPanel) p.getParent();
                AppUI.roundCorners(p, AppUI.getColor3(), 40);
            }
        };
            
        subInnerCore.addMouseListener(ma);
 
        rightPanel.add(gct);
    }
    
    public void showCreateSkyWalletScreen() {
        final JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CloudCoins", "jpg", "jpeg", "stack", "json", "txt");
        chooser.setFileFilter(filter);
        
        
        JPanel subInnerCore = getModalJPanel("Create Sky Wallet");
        maybeShowError(subInnerCore);
      
        AppUI.hr(subInnerCore, 30);
        
        // Outer Container
        JPanel oct = new JPanel();
        AppUI.setBoxLayout(oct, true);
        AppUI.noOpaque(oct);
        subInnerCore.add(oct);
        
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        oct.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        
        // Password Label
        JLabel x = new JLabel("Wallet Name");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);

        final MyTextField tf0 = new MyTextField("Wallet Name", false);
        c.insets = new Insets(0, 0, 36, 0);
        c.gridx = 0;
        c.gridy = 1;
        gridbag.setConstraints(tf0.getTextField(), c);
        ct.add(tf0.getTextField());
        
        // Text
        JLabel txt = new JLabel("Select CloudCoin for your Sky Wallet");
        AppUI.setCommonFont(txt);
        AppUI.alignCenter(txt);
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = 0;
        c.gridy = 2;
        gridbag.setConstraints(txt, c);
        ct.add(txt);
        
        // Space
        //AppUI.hr(oct, 22);
        
        final MyTextField tf1 = new MyTextField("", false, true);
        tf1.disable();
        tf1.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                 //   System.out.println("You chose to open this file: " +
                   //     chooser.getSelectedFile().getName());        
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    tf1.setData(chooser.getSelectedFile().getName());
                }
            }
        });
        /*
        tf1.addListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("ggg");
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                     System.out.println("You chose to open this file: " +
                         chooser.getSelectedFile().getName());
                    
                }
            }
        });*/
        c.gridx = 0;
        c.gridy = 3;
        gridbag.setConstraints(tf1.getTextField(), c);
        ct.add(tf1.getTextField());
        
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ps.chosenFile.isEmpty()) {
                    ps.errText = "Select the CloudCoin";
                    showScreen();
                    return;
                }
                
                if (!AppCore.isCoinOk(ps.chosenFile)) {
                    ps.errText = "The coin is invalid. Format error";
                    showScreen();
                    return;
                }
                
                String domain = tf0.getText();
                if (domain.isEmpty()) {
                    ps.errText = "Wallet name can't be empty";
                    showScreen();
                    return;
                }
                
                DNSSn d = new DNSSn(domain, wl);
                
                if (d.recordExists()) {
                    ps.errText = "Wallet is already taken";
                    showScreen();
                    return;
                }
                
                if (!d.setRecord(ps.chosenFile, sm.getSR())) {
                    ps.errText = "Failed to set record. Check if the coin is valid";
                    showScreen();
                    return;
                }
                
                String newFileName = domain + ".stack";
                if (!AppCore.moveToFolderNewName(ps.chosenFile, Config.DIR_ID, Config.DIR_DEFAULT_USER, newFileName)) {
                    ps.errText = "Failed to move coin";
                    showScreen();
                    return;
                }
                
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
        
        AppUI.hr(subInnerCore, 20);
        subInnerCore.add(bp);  
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
                    if (!Validator.walletName(fwalletName.getText())) {
                        ps.errText = "Wallet name is empty";
                        showScreen();
                        return;
                    }
                    
                    ps.typedWalletName = fwalletName.getText();
                }
                
                if (ps.cwalletPasswordRequested) {
                    ps.currentScreen = ProgramState.SCREEN_SET_PASSWORD;
                    showScreen();
                    return;
                } 
                
                if (ps.cwalletRecoveryRequested) {
                    ps.currentScreen = ProgramState.SCREEN_SET_EMAIL;
                    showScreen();
                    return;
                }
                
                initSystemUser();  
                ps.currentScreen = ProgramState.SCREEN_WALLET_CREATED;
                showScreen();
            }
        });
        
        AppUI.hr(subInnerCore, 20);
        subInnerCore.add(bp); 
    }
    
    public JPanel getTwoButtonPanel(ActionListener al) {
        JPanel bp = new JPanel();
     //   AppUI.setBoxLayout(bp, false);
        AppUI.noOpaque(bp);
       
        bp.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 1;
        
        MyButton cb = new MyButton("Cancel");
        cb.addListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
        
        bp.add(cb.getButton(), gbc);           
        AppUI.vr(bp, 26);

        cb = new MyButton("Continue");
        cb.addListener(al);
        bp.add(cb.getButton(), gbc);
        
        continueButton = cb;
        
        return bp;
    }
    
    public JPanel getOneButtonPanel() {
        JPanel bp = new JPanel();
        AppUI.noOpaque(bp);
        
        bp.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 1;

        MyButton cb = new MyButton("Continue");
        cb.addListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
        
        bp.add(cb.getButton(), gbc);           
        AppUI.vr(bp, 26);
        
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
        
        updateWalletAmount();
        
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
        
        JLayeredPane walletWidget;
        
        // List wallets
        wallets = sm.getWallets();
        for (int i = 0; i < wallets.length; i++) {
            walletWidget = getWallet(wallets[i]);
            wpanel.add(walletWidget);
        }
 
        // "Add" Button
        wpanel.add(getWallet(null));
        
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
    
    public JLayeredPane getWallet(Wallet wallet) {
        boolean isDisabled = true;
        System.out.println("w="+wallet);
        if (wallet != null)
            isDisabled = !isActiveWallet(wallet);
        
        // Pane
        JLayeredPane lpane = new JLayeredPane();
        AppUI.noOpaque(lpane);
        AppUI.setSize(lpane, 200, 140);
        AppUI.alignLeft(lpane);

        // Rounded Background
        Color color = isDisabled ? AppUI.getColor3() : AppUI.getColor4();
        
        JButton addBtn = new JButton("");
        addBtn.setBorder(new RoundedBorder(40, color));
        addBtn.setFocusPainted(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setBounds(0, 0, 200, 120);


        // Wrapper for label
        JPanel cx = new JPanel();
        AppUI.noOpaque(cx);
        AppUI.setBoxLayout(cx, true);
        cx.setBounds(0,12,200,100);

        // Space
        AppUI.hr(cx, 10);
        
        // Label
        String name = (wallet == null) ? "Add Wallet" : wallet.getName();
        
        JLabel l = new JLabel(name);
        AppUI.setFont(l, 22);
        if (isDisabled)
            AppUI.setColor(l, AppUI.getDisabledColor2());
        
        AppUI.alignCenter(l);
        cx.add(l);
          
        // Space
        AppUI.hr(cx, 12);
       
        // Line wrapper (2 icons + string of coins)
        JPanel inner = new JPanel();
        AppUI.setBoxLayout(inner, false);
        AppUI.noOpaque(inner);

        final JButton faddBtn = addBtn;    
        final boolean fisDisabled = isDisabled;
        
        if (wallet != null) {
            // Horizontal Space
            AppUI.vr(inner, 10);
        
            String iconNameL, iconNameR;
            ImageJPanel icon;
            if (wallet.isSkyWallet()) {
                iconNameL = "Cloud Icon.png";
            } else if (wallet.isEncrypted()) {
                if (isDisabled)
                    iconNameL = "Lock Icon Disabled.png";
                else
                    iconNameL = "Lock Icon.png";
            } else {
                iconNameL = "dummy.png";
            }
        
            if (!wallet.getEmail().equals("")) {
                if (isDisabled)
                    iconNameR = "Image 41.png";
                else
                    iconNameR = "Envelope.png";
            } else {
                iconNameR = "dummy.png";
            }
           
            JLabel iconl, iconr;
            try {
                Image img;
            
                img = ImageIO.read(getClass().getClassLoader().getResource("resources/" + iconNameL));
                iconl = new JLabel(new ImageIcon(img));
            
                img = ImageIO.read(getClass().getClassLoader().getResource("resources/" + iconNameR));
                iconr = new JLabel(new ImageIcon(img));
                AppUI.setMargin(iconr, 5, 0, 0, 0);
            } catch (Exception ex) {
                return null;
            }
        
            inner.add(iconl);
 
            // Amount of coins
            JPanel amWrapper = new JPanel();
            amWrapper.setLayout(new GridBagLayout());
            AppUI.noOpaque(amWrapper);
        
            // Amount (empty)
            JLabel jxl = new JLabel("");
            AppUI.setFont(jxl, 18);
            AppUI.alignCenter(jxl);
            AppUI.noOpaque(jxl);
            amWrapper.add(jxl);
            inner.add(amWrapper);

            // Set ref between wallet and its ui
            wallet.setuiRef(jxl);
        
            inner.add(iconr);
            AppUI.vr(inner, 10);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setActiveWallet(wallet.getName());
                    ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                    showScreen();
                }   
                
                public void mouseEntered(MouseEvent e) {
                    AppUI.roundCorners(faddBtn, AppUI.getColor5(), 40);
                }
            
                public void mouseExited(MouseEvent e) {
                    Color color = fisDisabled ? AppUI.getColor3() : AppUI.getColor4();
                    
                    AppUI.roundCorners(faddBtn, color, 40);
                }
            };
            
            cx.addMouseListener(ma);
        } else {
            final JLabel plus = new JLabel("+");
            AppUI.setFont(plus, 64);
            if (isDisabled)
                AppUI.setColor(plus, AppUI.getDisabledColor2());

            inner.add(plus);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    ps.currentScreen = ProgramState.SCREEN_PREPARE_TO_ADD_WALLET;
                    ps.isAddingWallet = true;
                    showScreen();
                }  
                
                public void mouseEntered(MouseEvent e) {
                    AppUI.roundCorners(faddBtn, AppUI.getColor5(), 40);
                }
            
                public void mouseExited(MouseEvent e) {
                    Color color = fisDisabled ? AppUI.getColor3() : AppUI.getColor4();
                    
                    AppUI.roundCorners(faddBtn, color, 40);
                }
            };
         
            cx.addMouseListener(ma);
        }
          
        cx.add(inner);        
        
        lpane.add(addBtn, new Integer(1));
        lpane.add(cx, new Integer(2));

        AppUI.setHandCursor(lpane);
        
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
                ps.currentScreen = ProgramState.SCREEN_CREATE_WALLET;
                ps.isDefaultWalletBeingCreated = true;
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
    
    
    */
    
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
                   UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    

   
                   // javax.swing.UIManager.setLookAndFeel(info.getClassName());
                   break;
                } 
           }   
           //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
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
    /*
    class EchoCb implements CallbackInterface {
	public void callback(Object result) {
            if (!sm.getActiveWallet().isSkyWallet())
                sm.startFrackFixerService(new FrackFixerCb());
	}  
    }
    */
    class ShowCoinsCb implements CallbackInterface {
	public void callback(final Object result) {
            final Object fresult = result;
            ShowCoinsResult scresult = (ShowCoinsResult) fresult;
                 
        //    cbState = CB_STATE_DONE;
        //    cbDone();
            showCoinsDone(scresult.counters);
        //    setCounters(scresult.counters);  
        }
    }
    /*
    class UnpackerCb implements CallbackInterface {
	public void callback(Object result) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
            
            importState = IMPORT_STATE_IMPORT;
            setRAIDAProgress(0, 0, AppCore.getFilesCount(Config.DIR_SUSPECT, currentWallet));
            sm.startAuthenticatorService(new AuthenticatorCb());
 
        }
    }

    class AuthenticatorCb implements CallbackInterface {
	public void callback(Object result) {
            final Object fresult = result;
	
            AuthenticatorResult ar = (AuthenticatorResult) fresult;

            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                importState = IMPORT_STATE_INIT;
                setScreen(SCREEN_MAIN_WALLET);
                setError("Import failed");
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                sm.startGraderService(new GraderCb());
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_CANCELLED) {
                importState = IMPORT_STATE_INIT;
                setError("Cancelled");
                setScreen(SCREEN_MAIN_WALLET);
                return;
            }

            setRAIDAProgress(ar.totalRAIDAProcessed, ar.totalFilesProcessed, ar.totalFiles);
	}
    }
    
    class GraderCb implements CallbackInterface {
	public void callback(Object result) {
            GraderResult gr = (GraderResult) result;

            statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
            statToBank = gr.totalAuthentic + gr.totalFracked;
            statFailed = gr.totalLost + gr.totalCounterfeit + gr.totalUnchecked;

            sm.getActiveWallet().appendTransaction("Import", statToBankValue);
            
            if (!sm.getActiveWallet().isEncrypted()) {
                importState = IMPORT_STATE_DONE;
            } else {
                sm.startVaulterService(new VaulterCb());
            }       
	}
    }

    class FrackFixerCb implements CallbackInterface {
	public void callback(Object result) {
            FrackFixerResult fr = (FrackFixerResult) result;

            if (fr.status == FrackFixerResult.STATUS_ERROR) {
                wl.error(ltag, "Failed to fix");
		return;
            }

            if (fr.status == FrackFixerResult.STATUS_FINISHED) {
		if (fr.fixed + fr.failed > 0) {
                    wl.debug(ltag, "Fracker fixed: " + fr.fixed + ", failed: " + fr.failed);
                    return;
		}
            }
            
            sm.startLossFixerService(new LossFixerCb());
        }
    }
         
    class ExporterCb implements CallbackInterface {
	public void callback(Object result) {
            ExporterResult er = (ExporterResult) result;
            if (er.status == ExporterResult.STATUS_ERROR) {
                cbState = CB_STATE_DONE;
		setError("Failed to export");
		return;
            }

            if (er.status == ExporterResult.STATUS_FINISHED) {
		exportedFilenames = er.exportedFileNames;
                cbState = CB_STATE_DONE;
                
                sm.getActiveWallet().appendTransaction("Export", er.totalExported * -1);
		return;
            }
	}
    }
    
    class VaulterCb implements CallbackInterface {
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            
            importState = IMPORT_STATE_DONE;
            cbState = CB_STATE_DONE;

	}
    }
    
    class LossFixerCb implements CallbackInterface {
	public void callback(final Object result) {
            LossFixerResult lr = (LossFixerResult) result;
            
            wl.debug(ltag, "LossFixer finished");
            sm.startEraserService(new EraserCb());
        }
    }
				
    class BackupperCb implements CallbackInterface {
	public void callback(final Object result) {
            BackupperResult br = (BackupperResult) result;
            
            wl.debug(ltag, "Backupper finished");
	}
    }

    class EraserCb implements CallbackInterface {
        public void callback(final Object result) {
            EraserResult er = (EraserResult) result;

            wl.debug(ltag, "Eraser finished");
	}
    }
    
    class SenderCb implements CallbackInterface {
	public void callback(Object result) {
            SenderResult sr = (SenderResult) result;
            
            wl.debug(ltag, "Sender finished: " + sr.status);
            cbState = CB_STATE_DONE;

            if (sr.amount > 0) {
                sm.getActiveWallet().appendTransaction(sr.memo, sr.amount * -1);
                Wallet dstWallet = sm.getWallet(currentDstWallet);
                if (dstWallet != null) {
                    dstWallet.appendTransaction(sr.memo, sr.amount);
                    if (dstWallet.isEncrypted()) {
                        wl.debug(ltag, "Set wallet to " + currentDstWallet);
                        sm.changeServantUser("Vaulter", currentDstWallet);
                        sm.startVaulterService(new VaulterCb());          
                    }
                }            
            }
   
            if (sr.status == SenderResult.STATUS_ERROR) {
		setError("Failed to send. Please check amount of coins in the Wallet");
		return;
            }        
	}
    }
    
    class ReceiverCb implements CallbackInterface {
	public void callback(Object result) {
            ReceiverResult sr = (ReceiverResult) result;
            
            wl.debug(ltag, "Receiver finished");
            
            currentWallet = currentDstWallet;
            if (!sm.getActiveWallet().isEncrypted()) {
                cbState = IMPORT_STATE_DONE;
            } else {
                sm.startVaulterService(new VaulterCb());
            }  
            
	}
    }
   */
}