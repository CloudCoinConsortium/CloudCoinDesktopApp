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
import global.cloudcoin.ccbank.Unpacker.UnpackerResult;
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
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
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
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;






/**
 * 
 */
public class AdvancedClient implements ActionListener, ComponentListener {

    String version = "2.0.0";

    JPanel headerPanel;
    JPanel mainPanel;
    JPanel corePanel;
    JPanel wpanel;
    
    String ltag = "Advanced Client";
    
    JLabel totalText;
    
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
    
    JProgressBar pbar;
    JLabel depositText;
    
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
        if (!sm.init()) {
            ps.errText = "Failed to init ServantManager";
            return;
        }
        
        if (sm.getWallets().length != 0) {
            ps.currentScreen = ProgramState.SCREEN_DEFAULT;
        }
    }
   
    public void echoDone() {
        ps.isEchoFinished = true;
    }
    
    public void unpackerDone() {
        
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
            //sm.setActiveWalletObj(wallets[ps.currentWalletIdx]);
            System.out.println("chagin show coins to " + wallets[ps.currentWalletIdx].getName());
            sm.changeServantUser("ShowCoins", wallets[ps.currentWalletIdx].getName());
            System.out.println("chagined show coins to " + wallets[ps.currentWalletIdx].getName());
            if (!wallets[ps.currentWalletIdx].isSkyWallet()) {
                System.out.println("go next");
                sm.startShowCoinsService(new ShowCoinsCb());
            } else {
                System.out.println("new service sky");
                JLabel cntLabel = (JLabel) wallets[ps.currentWalletIdx].getuiRef();
                cntLabel.setText("25,500");
                showCoinsGoNext();
            }       
        } else {
            totalText.setText("25,500");
            ps.isShowCoinsFinished = true;
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
        
        System.out.println("typed=" + ps.typedWalletName);
        if (!sm.initUser(ps.typedWalletName, ps.typedEmail, ps.typedPassword)) {
            ps.errText = "Failed to init Wallet"; 
            return;
        }
            
        AppCore.copyTemplatesFromJar(ps.typedWalletName);
    }
    
    public boolean isActiveWallet(Wallet wallet) {
        if (ps.currentWallet == null)
            return false;
        
        return ps.currentWallet.getName().equals(wallet.getName());
    }
    
    public void setActiveWallet(Wallet wallet) {    
        ps.currentWallet = wallet;
        
        sm.setActiveWalletObj(wallet);
    }
    
    public void clear() {
        headerPanel.removeAll();
        fillHeaderPanel();
        headerPanel.repaint();
        headerPanel.revalidate();
        
        
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
        
        fillHeaderPanel();
    }
    
    public boolean isDepositing() {
        if (ps.currentScreen == ProgramState.SCREEN_DEPOSIT)
            return true;
        
        return false;
    }
    
    public boolean isWithdrawing() {
        if (ps.currentScreen == ProgramState.SCREEN_WITHDRAW)
            return true;
        
        return false;
    }
    
    public void fillHeaderPanel() {
        JPanel p = new JPanel();
        AppUI.noOpaque(p);
        GridBagLayout gridbag = new GridBagLayout();
       
        p.setLayout(gridbag);
        
        GridBagConstraints c = new GridBagConstraints();      
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 10, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        
        
        
        JLabel icon0, icon1, icon2, icon3;
        try {
            Image img;
                   
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/Gear icon.png"));
            icon0 = new JLabel(new ImageIcon(img));
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/Help_Support Icon.png"));
            icon1 = new JLabel(new ImageIcon(img));
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/Brithish flag.png"));
            icon2 = new JLabel(new ImageIcon(img));
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/CloudCoinLogo2.png"));
            icon3 = new JLabel(new ImageIcon(img));
            

        } catch (Exception ex) {
            return;
        }
        
        gridbag.setConstraints(icon3, c);
        p.add(icon3);
        
        
        // Space
       // AppUI.vr(headerPanel, tw * 0.0082 * 2);
            
        if (ps.currentScreen == ProgramState.SCREEN_AGREEMENT) {
             // Init Label
            JLabel titleText = new JLabel("CloudCoin Wallet " + version);
            AppUI.setTitleSemiBoldFont(titleText, 32);
            gridbag.setConstraints(titleText, c);
            p.add(titleText);
        } else {
            
            //String strCnt = AppCore.formatNumber(totalCnt);
            
            JLabel titleText = new JLabel("Total Coins: ");
            AppUI.setTitleSemiBoldFont(titleText, 32);
            gridbag.setConstraints(titleText, c);
            p.add(titleText);
            
            totalText = new JLabel("0");
            AppUI.setTitleFont(totalText, 32);
            gridbag.setConstraints(totalText, c);
            p.add(totalText);
            
            c.anchor = GridBagConstraints.NORTH;
            titleText = new JLabel("cc");
            AppUI.alignBottom(titleText);
            AppUI.setTitleFont(titleText, 16);
            gridbag.setConstraints(titleText, c);
            p.add(titleText);
            
            c.anchor = GridBagConstraints.CENTER;
     
            // Pad
            c.weightx = 1;
            JLabel padd0 = new JLabel();
            gridbag.setConstraints(padd0, c);
            p.add(padd0);
            
            
            c.weightx = 0;
            c.insets = new Insets(0, 10, 0, 60); 
            
            // Deposit Button 
            final JButton b0 = new JButton("Deposit");
            b0.setContentAreaFilled(false);
            b0.setFocusPainted(false);
            AppUI.setTitleBoldFont(b0, 32);
            AppUI.setHandCursor(b0);
            if (isDepositing()) {
                AppUI.underLine(b0);
            } else {
                AppUI.noUnderLine(b0);
            }
            
            gridbag.setConstraints(b0, c);
            p.add(b0);
            c.insets = new Insets(0, 10, 0, 0); 
            //headerPanel.add(b0);

            // Space
            //AppUI.vr(headerPanel, tw * 0.0082 * 8);
            
            // Transfer Button
            final JButton b1 = new JButton("Transfer");
            b1.setContentAreaFilled(false);
            b1.setFocusPainted(false);
            AppUI.setTitleBoldFont(b1, 32);
            AppUI.setHandCursor(b1);
            if (isWithdrawing()) {
                AppUI.underLine(b1);
            } else {
                AppUI.noUnderLine(b1);
            }
            
            gridbag.setConstraints(b1, c);
            p.add(b1);

            ActionListener al0 = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    ps.currentScreen = ProgramState.SCREEN_DEPOSIT;
                    showScreen();
                }
            };
            
            ActionListener al1 = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    ps.currentScreen = ProgramState.SCREEN_WITHDRAW;
                    showScreen();
                }
            };
            
            MouseAdapter ma0 = new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    AppUI.underLine(b);
                }
                public void mouseExited(MouseEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    if (!isDepositing())
                        AppUI.noUnderLine(b);
                }
            };
            
            MouseAdapter ma1 = new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    AppUI.underLine(b);
                }
                public void mouseExited(MouseEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    if (!isWithdrawing())
                        AppUI.noUnderLine(b);
                }
            };
                   
            b0.addActionListener(al0);
            b1.addActionListener(al1);
            b0.addMouseListener(ma0);
            b1.addMouseListener(ma1);
                    
            // Pad 
           // JPanel jpad = new JPanel();
            //AppUI.noOpaque(jpad);
            //AppUI.setSize(jpad, 400, 50);
            //headerPanel.add(jpad);  
            // Space
          //  AppUI.vr(headerPanel, tw * 0.0082 * 8);
        }
        
     /*   JPanel ppad = new JPanel();
        
        GridBagLayout gridbag0 = new GridBagLayout();
        GridBagConstraints c0 = new GridBagConstraints();      
        ppad.setLayout(gridbag0);
        
        headerPanel.add(ppad); 
       */ 

        
        
        

        
        
        

        
      //  c.fill = GridBagConstraints.HORIZONTAL;
      //  c.gridwidth = 8;
        c.weightx = 1;
        JLabel padd = new JLabel();
        gridbag.setConstraints(padd, c);
        p.add(padd);
        
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weightx = 0;
        // Icon Gear
//        ImageJPanel icon = new ImageJPanel("");
 //       AppUI.setSize(icon, tw / 34.51);
        AppUI.noOpaque(icon0);
        //p.add(icon);
      
        
        gridbag.setConstraints(icon0, c);
        p.add(icon0);
        
         // Space
        //AppUI.vr(headerPanel, tw * 0.0082 * 2);
        
        
        // Icon Support
       // icon = new ImageJPanel("");
       // AppUI.setSize(icon, tw / 30.51);
        AppUI.noOpaque(icon1);
        //headerPanel.add(icon);
        gridbag.setConstraints(icon1, c);
        p.add(icon1);
        
        c.insets = new Insets(0, 10, 0, 20); 
         AppUI.noOpaque(icon2);
        //headerPanel.add(icon);
        gridbag.setConstraints(icon2, c);
        p.add(icon2);
        
        // Space
       // AppUI.vr(headerPanel, tw * 0.0082 * 2);
        

            
        // Icon Flag Wrapper
        /*
        JPanel flagWrapper = new JPanel();
        AppUI.noOpaque(flagWrapper);
        AppUI.setBoxLayout(flagWrapper, true);
        AppUI.hr(flagWrapper, 14);       
        icon = new ImageJPanel( "");
        AppUI.setSize(icon, tw / 28.23);
        AppUI.noOpaque(icon);
        flagWrapper.add(icon);*/
 //       headerPanel.add(flagWrapper);
                      
        // Space
      //  AppUI.vr(headerPanel, tw * 0.0082 * 2);  
        
        
        headerPanel.add(p);
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
     //   ps.currentScreen = ProgramState.SCREEN_IMPORTING;  
        clear();
        //  showImportDoneScreen();
       // showDepositScreen();
   //     System.out.println("show");
       //   showImportingScreen();
    //    showTransactionsScreen();
    //    showCreateSkyWalletScreen();
     //   showPrepareToAddWalletScreen();
    //    showDefaultScreen();
      //  showWalletCreatedScreen();
      //  showSetEmailScreen();
     //   showUnderstandPasswordScreen();
        //ps.isDefaultWalletBeingCreated = true;
        //showCreateWalletScreen();
     //   showSetPasswordScreen();
    //    if (1==1)             return;
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
            case ProgramState.SCREEN_DEPOSIT:
                ps.currentWallet = null;
                showDepositScreen();
                break;
            case ProgramState.SCREEN_WITHDRAW:
                ps.currentWallet = null;
                //showTransactionsScreen();
                break;
            case ProgramState.SCREEN_IMPORTING:
                showImportingScreen();
                break;
            case ProgramState.SCREEN_IMPORT_DONE:
                showImportDoneScreen();
                break;
        }
        
        System.out.println("EXIT " + ps.currentScreen);
        headerPanel.repaint();
        headerPanel.revalidate();

        corePanel.repaint();
        corePanel.revalidate();
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
    
    private void setRAIDAProgress(int raidaProcessed, int totalFilesProcessed, int totalFiles) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        depositText.setText(totalFilesProcessed + " / " + totalFiles + " Deposited");
     //   setJraida(totalFilesProcessed, totalFiles);
    }
    
    public void showImportingScreen() {
        JPanel subInnerCore = getModalJPanel("Deposit in Progress");
        maybeShowError(subInnerCore);
        
        //ps.dstWallet = sm.getWalletByName("Default Wallet");

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        // Password Label
        JLabel x = new JLabel("Do not close the application until all CloudCoins are deposited!");
        AppUI.setBoldFont(x, 16);
        AppUI.setColor(x, AppUI.getErrorColor());
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(20, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        depositText = new JLabel("");
        AppUI.setCommonFont(depositText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(depositText, c);
        ct.add(depositText);
        
        // ProgressBar
        pbar = new JProgressBar();
        pbar.setStringPainted(true);
        AppUI.setMargin(pbar, 0);
        AppUI.setSize(pbar, (int) (tw / 2.6f) , 50);
        pbar.setMinimum(0);
        pbar.setMaximum(24);
        pbar.setValue(0);
        pbar.setUI(new FancyProgressBar());
        AppUI.noOpaque(pbar);
        
        c.insets = new Insets(20, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        gridbag.setConstraints(pbar, c);
        ct.add(pbar);
        
        JPanel bp = getOneButtonPanelCustom("Cancel", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sm.cancel("Unpacker");
                sm.cancel("Authenticator");
                
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
  
        
        subInnerCore.add(bp);  
        
        Thread t = new Thread(new Runnable() {
            public void run(){
                pbar.setVisible(false);
                if (!ps.isEchoFinished) {
                    depositText.setText("Checking RAIDA ...");
                    depositText.repaint();
                }
                
                wl.debug(ltag, "Going here");
                System.out.println("Go here");
                while (!ps.isEchoFinished || !ps.isShowCoinsFinished) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}
                    System.out.println("Not " + ps.isEchoFinished + " " + ps.isShowCoinsFinished);
                }
                
                System.out.println("Go here2");
                ps.dstWallet.setPassword(ps.typedPassword);
                sm.setActiveWalletObj(ps.dstWallet);
                
                System.out.println("Go here3");
                depositText.setText("Moving coins ...");
                for (String filename : ps.files) {
                    AppCore.moveToFolder(filename, Config.DIR_IMPORT, sm.getActiveWallet().getName());
                }
                
                System.out.println("Go here4");
                depositText.setText("Unpacking coins ...");
                sm.startUnpackerService(new UnpackerCb());
                
                 System.out.println("Go here5");
              //  showCoinsGoNext();
            }
        });
        
        t.start();
        
    }
    
    public void showImportDoneScreen() {
        JPanel subInnerCore = getModalJPanel("Deposit Complete");
        maybeShowError(subInnerCore);
        
//        System.out.println("Show import done");
        

     //   ps.dstWallet = sm.getWalletByName("Default Wallet");
       // sm.setActiveWalletObj(ps.dstWallet);
  
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        

        ps.dstWallet = sm.getWalletByName("Default Wallet");
        ps.statToBankValue = 0;
        String total = AppCore.formatNumber(ps.statToBankValue);
        String totalBank = AppCore.formatNumber(ps.statToBank);
        String totalFailed = AppCore.formatNumber(ps.statFailed);
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>Deposited <b>" +  total +  " CloudCoins</b> to <b>" + ps.dstWallet.getName() + " </b></span></html>");
        AppUI.setCommonFont(x);
 
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        
        x = new JLabel("Total Authentic Coins:");
        AppUI.setCommonFont(x);
        
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(50, 0, 4, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(totalBank);
        AppUI.setCommonBoldFont(x);
       // c.fill = GridBagConstraints.HORIZONTAL;
        
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel("Total Counterfeit Coins:");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 4, 10);
        c.gridx = 0;
        c.gridy = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(totalFailed);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        
        JPanel bp = getTwoButtonPanelCustom("Next Deposit", "Continue", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.currentScreen = ProgramState.SCREEN_DEPOSIT;
                showScreen();
            }
        },  new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
  
        
        subInnerCore.add(bp);  
        
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
        System.out.println("zssa");
   
        if (wallets == null)
            wallets = sm.getWallets();

        if (wallets.length > 0) {
            ps.currentWalletIdx = 0;
            System.out.println("start " +wallets[ps.currentWalletIdx].getName());
            //sm.setActiveWallet(wallets[ps.currentWalletIdx].getName());
            sm.changeServantUser("ShowCoins", wallets[ps.currentWalletIdx].getName());
            System.out.println("finish " +wallets[ps.currentWalletIdx].getName());
            if (!wallets[ps.currentWalletIdx].isSkyWallet()) {
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
    
    public void showDepositScreen() {
        
        showLeftScreen();

        JPanel rightPanel = getRightPanel();    
    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Deposit");   
        ct.add(ltitle);
        AppUI.alignTop(ct);
        AppUI.alignTop(ltitle);
        
        AppUI.hr(ct, 10);
        
        maybeShowError(ct);
        
        // Outer Container
        JPanel oct = new JPanel();
        AppUI.noOpaque(oct);
        
        int y = 0;
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints(); 
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(12, 18, 0, 0); 
        oct.setLayout(gridbag);
        
        
        // Deposit To
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        JLabel x = new JLabel("Deposit To");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
 
        int nonSkyCnt = 0;
        for (int i = 0; i < wallets.length; i++)
            if (!wallets[i].isSkyWallet())
                nonSkyCnt++;
           
        String[] options = new String[nonSkyCnt];
        int j = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!wallets[i].isSkyWallet()) {
                options[j] = wallets[i].getName();
                j++;
            }
        }
      
        RoundedCornerComboBox cbox = new RoundedCornerComboBox(AppUI.getColor2(), "Select Destination", options);
        gridbag.setConstraints(cbox.getComboBox(), c);
        oct.add(cbox.getComboBox());
        
        // Memo
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y + 1;   
        x = new JLabel("Memo (Note)");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y + 1;     
        MyTextField memo = new MyTextField("Optional", false);
        gridbag.setConstraints(memo.getTextField(), c);
        System.out.println("m="+ps.typedMemo);
        if (!ps.typedMemo.isEmpty())
            memo.setData(ps.typedMemo);
        oct.add(memo.getTextField());
        
        // Password
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y + 2;   
        x = new JLabel("Password");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y + 2;     
        MyTextField password = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(password.getTextField(), c);
        oct.add(password.getTextField());
        

        // Total files selected
        final JLabel tl = new JLabel("Total files selected: " + ps.files.size());
        AppUI.setCommonFont(tl);
        c.insets = new Insets(28, 18, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = y + 3;  
        gridbag.setConstraints(tl, c);
        oct.add(tl);

        // Drag and Drop
        JPanel ddPanel = new JPanel();
        ddPanel.setLayout(new GridBagLayout());
        
        JLabel l = new JLabel("Drop files here or click to select files");
        AppUI.setColor(l, AppUI.getColor10());
        AppUI.setBoldFont(l, 22);
        AppUI.noOpaque(ddPanel);
        AppUI.setHandCursor(ddPanel);
        ddPanel.setBorder(new DashedBorder(40, AppUI.getColor10()));
        ddPanel.add(l);
        
        c.insets = new Insets(8, 18, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = y + 4;  
        
        AppUI.setSize(ddPanel, (int) (tw / 2.2), 160);
        gridbag.setConstraints(ddPanel, c);
        new FileDrop( System.out, ddPanel, new FileDrop.Listener() {
            public void filesDropped( java.io.File[] files ) {   
                for( int i = 0; i < files.length; i++ ) {
                    ps.files.add(files[i].getAbsolutePath());
                }
                
                tl.setText("Total files selected: " + ps.files.size());            
            } 
        }); 
        
        final JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CloudCoins", "jpg", "jpeg", "stack", "json", "txt");
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(true);
        
        ddPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = chooser.getSelectedFiles();
                    for (int i = 0; i < files.length; i++) {
                        ps.files.add(files[i].getAbsolutePath());
                    }
                    
                    tl.setText("Total files selected: " + ps.files.size());   
                }
            }   
        });

        oct.add(ddPanel);
        rightPanel.add(oct);
        
        // Space
        AppUI.hr(oct, 22);
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String walletName = cbox.getSelectedValue();
                Wallet w;
                
                ps.typedMemo = memo.getText();
                
                w = sm.getWalletByName(walletName);
                if (w == null) {
                    ps.errText = "Wallet is not selected";
                    showScreen();
                    return;
                }
                
                if (ps.files.size() == 0) {
                    ps.errText = "No files selected";
                    showScreen();
                    return;
                }

                if (w.isEncrypted()) {
                    if (password.getText().isEmpty()) {
                        ps.errText = "Password is empty";
                        showScreen();
                        return;
                    }
                    
                    String wHash = w.getPasswordHash();
                    String providedHash = AppCore.getMD5(password.getText());
                    
                    System.out.println("xxxpass=" + w.getPasswordHash() + " n="+w.getName() + " p=" + providedHash);
                    if (wHash == null) {
                        ps.errText = "Wallet is corrupted";
                        showScreen();
                        return;
                    }
                    
                    if (!wHash.equals(providedHash)) {
                        ps.errText = "Password is incorrect";
                        showScreen();
                        return;
                    } 
                }
                
                ps.dstWallet = w;          
                ps.typedPassword = password.getText();
                ps.currentScreen = ProgramState.SCREEN_IMPORTING;
                
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);       
    }
    
    
    public void showTransactionsScreen() {
        showLeftScreen();
 
        Wallet w = sm.getActiveWallet();     

        JPanel rightPanel = getRightPanel(AppUI.getColor4());    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        //AppUI.setBackground(ct, AppUI.getColor3());
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle(w.getName() + " - " + w.getTotal() + " CC");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);

        
        // Create transactions
        JLabel trLabel;
        String[][] trs = sm.getActiveWallet().getTransactions();
        
        if (trs == null || trs.length == 0) {
            trLabel = new JLabel("No transactions");
            AppUI.setSemiBoldFont(trLabel, 20);
            AppUI.alignCenter(trLabel);
            ct.add(trLabel);
            return;
        }
        
        trLabel = new JLabel("Transaction History");
        AppUI.setSemiBoldFont(trLabel, 20);
        AppUI.alignCenter(trLabel);
        ct.add(trLabel);
                
        AppUI.hr(ct, 20);
 
        // Scrollbar & Table
        UIManager.put("ScrollBar.background", new ColorUIResource(AppUI.getColor0()));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            JLabel lbl;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                lbl = (JLabel) this;
                
                
               
                
                if (column == 0) {
                    AppUI.setColor(lbl, AppUI.getColor0());
                    AppUI.underLine(lbl);
                    //AppUI.setHandCursor();
                   
                    
                } else {
                    AppUI.setColor(lbl, Color.BLACK);
                }
                
                if (row % 2 == 0) {
                    AppUI.setBackground(lbl, AppUI.getColor3());
                } else {
                    AppUI.setBackground(lbl, AppUI.getColor4());
                }
                
                if (column == 0) {
                    
                    lbl.setHorizontalAlignment(JLabel.LEFT);
                } else if (column == 1) {
                    lbl.setHorizontalAlignment(JLabel.CENTER);
                } else {
                    String d = (String) value;
                    try {
                        String total = AppCore.formatNumber(Integer.parseInt(d));
                        lbl.setText(total);
                    } catch (NumberFormatException e) {
                        
                    }
                  
                    lbl.setHorizontalAlignment(JLabel.RIGHT);
                }
                
                AppUI.setHandCursor(lbl);
                AppUI.setMargin(lbl, 8);
  
                return lbl;
            }
        };
  
        DefaultTableModel model = new DefaultTableModel(trs.length, trs[0].length - 1) {
            String[] columnNames = {
                "Memo (note)",
                "Date",
                "Deposit",
                "Withdraw",
                "Total",
                //    ""
            };
            
            @Override
            public String getColumnName(int col) {        
                return columnNames[col];
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
            
            public boolean isCellEditable(int row, int column){  
               return false;  
            }
        };
        
        final JTable table = new JTable(model);
    //    table.setCellSelectionEnabled(true);
         //final JTable table = new JTable(data, columnNames);
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                model.setValueAt(trs[row][col], row, col);
            }
        }
 
        table.setRowHeight(table.getRowHeight() + 15);
        table.setDefaultRenderer(String.class, r);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        //table.setBorder(BorderFactory.createLineBorder(AppUI.getColor0()));
        table.setGridColor(AppUI.getColor7());
       // table.setAutoResizeMode(JTable.AUTO_RESIZE_ON);
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        
        
        
        MouseAdapter ma = new MouseAdapter() {
            private int prevcolumn = -1;

            @Override
            public void mouseReleased(MouseEvent e) {
              //  ps.isDefaultWalletBeingCreated = false;
               // ps.currentScreen = ProgramState.SCREEN_CREATE_WALLET;
                //showScreen();
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                System.out.println("xxx=" + row + "," + column + " x=" + trs[row][5]);
                
                
                
            }   
            
            public void mouseMoved(MouseEvent e) {

                
              //  AppUI.setHandCursor(table);
              
                int column = table.columnAtPoint(e.getPoint());
               
                System.out.println("prev="+prevcolumn + " c="+column);
                if (prevcolumn == column)
                    return;
               
                prevcolumn = column;
                
                if (column == 0) {
                    AppUI.setHandCursor(table);
                } else {
                    AppUI.setDefaultCursor(table);
                }
                
                
                
             //   table.revalidate();
               // table.repaint();
              //  int row = table.rowAtPoint(e.getPoint());
           //     table.getColumnModel().getColumn(0).
                //System.out.println("xx=" + column + " r=" + row);
                //JPanel p = (JPanel) e.getSource();
                //p = (JPanel) p.getParent();
                //AppUI.roundCorners(p, AppUI.getColor5(), 40);
            }
            
            public void mouseExited(MouseEvent e) {
                
                //AppUI.setDefaultCursor(table);
                //JPanel p = (JPanel) e.getSource();
                //p = (JPanel) p.getParent();
                //AppUI.roundCorners(p, AppUI.getColor3(), 40);
            }
        };
        
        
        table.addMouseListener(ma);
        table.addMouseMotionListener(ma);
        
        
        
        //table.setIntercellSpacing(new Dimension(20, 20));
        //table.setPreferredScrollableViewportSize(table.getPreferredSize());
        
        //table.setPreferredScrollableViewportSize(new Dimension(500, 200));
        
        // Header
        JTableHeader header = table.getTableHeader();    
        AppUI.setColor(header, Color.WHITE);
        AppUI.noOpaque(header);
        //AppUI.setFont(table, 14);
        AppUI.setCommonTableFont(table);
        AppUI.setCommonTableFont(header);
        
        
        //header.setIntercellSpacing(new Dimension(1, 1));
        
        final TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
        header.setDefaultRenderer(new TableCellRenderer() {
            private JLabel lbl;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                
               // if (selectedColumn == value) {
                    lbl = (JLabel) hr.getTableCellRendererComponent(table, value, true, true, row, column);
                   // lbl.setBorder(BorderFactory.createCompoundBorder(lbl.getBorder(), BorderFactory.createLineBorder(Color.red, 1)));
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
               /* } else {
                    lbl = (JLabel) hr.getTableCellRendererComponent(table, value, false, false, row, column);
                    lbl.setBorder(BorderFactory.createCompoundBorder(lbl.getBorder(), BorderFactory.createEmptyBorder(0, 5, 0, 0)));
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }
                if (column == 0) {
                    lbl.setForeground(Color.red);
                } else {
                    lbl.setForeground(header.getForeground());
                }
*/
               // lbl.setBorder(BorderFactory.createLineBorder(Color.WHITE));
                AppUI.setMargin(lbl, 2, 10, 2, 2);
                AppUI.setBackground(lbl, AppUI.getColor0());
                return lbl;
            }
        });
        //header.
        
        //AppUI.setSize(header, 200, table.getRowHeight() + 15);
        //AppUI.setBoldFont(header, 14);

        // ScrollPane
        JScrollPane scrollPane = new JScrollPane(table);
        AppUI.setSize(scrollPane, 660, 325);
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
                this.trackColor = AppUI.getColor6();
                this.thumbColor = AppUI.getColor7();
            }
        });
        
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        ct.add(scrollPane);


        
        

        JPanel bp = getTwoButtonPanelCustom("Print", "Export History", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    try {
                        table.print();
                    } catch (PrinterException pe) {
                        System.out.println("Failed to print");
                    }
                }
            }, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Wallet w = sm.getActiveWallet();
                JFileChooser c = new JFileChooser();
                c.setSelectedFile(new File(w.getName() + "-transactions.csv"));

                int rVal = c.showSaveDialog(null);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                    w.saveTransations(c.getSelectedFile().getAbsolutePath());
                }
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);     
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
              
        final MyTextField tf1 = new MyTextField("", false, true);
        tf1.disable();
        tf1.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    tf1.setData(chooser.getSelectedFile().getName());
                }
            }
        });

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
                /*
                if (d.recordExists()) {
                    ps.errText = "Wallet is already taken";
                    showScreen();
                    return;
                }
                */
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
            JPanel hct = new JPanel();
        
            AppUI.setBoxLayout(hct, false);
            AppUI.setMargin(hct, 0, 28, 0, 0);
            AppUI.noOpaque(hct);
         
            AppUI.setSize(hct, tw / 2, 50);
        
            // Name Label        
            x = new JLabel("Name");
            AppUI.setCommonFont(x);
            hct.add(x);
        
            AppUI.vr(hct, 50);
        
            walletName = new MyTextField("Wallet Name", false);
            hct.add(walletName.getTextField());

            oct.add(hct);
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
    
    public JPanel getTwoButtonPanelCustom(String name0, String name1, ActionListener al0, ActionListener al1) {
        JPanel bp = new JPanel();
        AppUI.noOpaque(bp);
       
        bp.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        
        MyButton cb = new MyButton(name0);
        cb.addListener(al0);
        
        bp.add(cb.getButton(), gbc);           
        AppUI.vr(bp, 26);

        cb = new MyButton(name1);
        cb.addListener(al1);
        bp.add(cb.getButton(), gbc);
        
        continueButton = cb;
        
        return bp;
    }
    
    public JPanel getOneButtonPanelCustom(String name0, ActionListener al0) {
        JPanel bp = new JPanel();
        AppUI.noOpaque(bp);
        
        bp.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weighty = 1;

        MyButton cb = new MyButton(name0);
        cb.addListener(al0);
        
        bp.add(cb.getButton(), gbc);           
        AppUI.vr(bp, 26);
        
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
         return getRightPanel(AppUI.getColor2());
    }
    
    public JPanel getRightPanel(Color color) {
        JPanel mwrapperPanel = new JPanel();
        
        AppUI.setBoxLayout(mwrapperPanel, true);
        AppUI.noOpaque(mwrapperPanel);
        AppUI.alignLeft(mwrapperPanel);
        AppUI.alignTop(mwrapperPanel);
        AppUI.setSize(mwrapperPanel, tw - 260, th);

        JPanel subInnerCore = AppUI.createRoundedPanel(mwrapperPanel, color, 20);
        AppUI.setSize(subInnerCore, tw - 260, th - headerHeight - 120);
        
        corePanel.add(mwrapperPanel);
        
        //System.out.println("DATA= " + ps.isShowCoinsFinished + " / " + ps.isEchoFinished);
        updateWalletAmount();
        
        sm.startEchoService(new EchoCb());
        
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
        addBtn.setBorder(new RoundedBorder(20, color));
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
                    System.out.println("set wallet " + wallet);
                    setActiveWallet(wallet);
                    ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                    showScreen();
                }   
                
                public void mouseEntered(MouseEvent e) {
                    AppUI.roundCorners(faddBtn, AppUI.getColor5(), 20);
                }
            
                public void mouseExited(MouseEvent e) {
                    Color color = fisDisabled ? AppUI.getColor3() : AppUI.getColor4();
                    
                    AppUI.roundCorners(faddBtn, color, 20);
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
                    ps.currentWallet = null;
                    ps.currentScreen = ProgramState.SCREEN_PREPARE_TO_ADD_WALLET;
                    ps.isAddingWallet = true;
                    showScreen();
                }  
                
                public void mouseEntered(MouseEvent e) {
                    AppUI.roundCorners(faddBtn, AppUI.getColor5(), 20);
                }
            
                public void mouseExited(MouseEvent e) {
                    Color color = fisDisabled ? AppUI.getColor3() : AppUI.getColor4();
                    
                    AppUI.roundCorners(faddBtn, color, 20);
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
      //  AppUI.setSize(subInnerCore, (int) tw/2, (int) (th/1.8));
         AppUI.setSize(subInnerCore, 718, 446);
      //  AppUI.alignCenter(subInnerCore);
        
        AppUI.hr(subInnerCore, 14);
        
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
    
    class EchoCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "Echo finisheed");
            
            echoDone();

         //   sm.startUnpackerService(new UnpackerCb());
           // if (!sm.getActiveWallet().isSkyWallet())
             //   sm.startFrackFixerService(new FrackFixerCb());
	}  
    }
    
    class ShowCoinsCb implements CallbackInterface {
	public void callback(final Object result) {
            final Object fresult = result;
            ShowCoinsResult scresult = (ShowCoinsResult) fresult;
                 
            showCoinsDone(scresult.counters);
        }
    }
    
    class UnpackerCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "Unpacker finisheed");
            System.out.println("unpacker fffinish");
            
            final Object fresult = result;
            UnpackerResult ur = (UnpackerResult) fresult;

            System.out.println("="+ ur.status);
            if (ur.status == UnpackerResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Failed to Unpack file(s). Please check the logs";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                
                return;
            }
                   
            unpackerDone();
            setRAIDAProgress(0, 0, AppCore.getFilesCount(Config.DIR_SUSPECT, sm.getActiveWallet().getName()));
            sm.startAuthenticatorService(new AuthenticatorCb());
        }
    }

    class AuthenticatorCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "Authenticator finisheed");
            
            final Object fresult = result;
	
            AuthenticatorResult ar = (AuthenticatorResult) fresult;

            //if (ar.status == AuthenticatorResult.STATUS_FINISHED)
//                ar.status = AuthenticatorResult.STATUS_ERROR;
            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Failed to Authencticate Coins";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                sm.startGraderService(new GraderCb());
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                return;
            }

            setRAIDAProgress(ar.totalRAIDAProcessed, ar.totalFilesProcessed, ar.totalFiles);
	}
    }
    
    class GraderCb implements CallbackInterface {
	public void callback(Object result) {
            GraderResult gr = (GraderResult) result;

            ps.statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
            ps.statToBank = gr.totalAuthentic + gr.totalFracked;
            ps.statFailed = gr.totalLost + gr.totalCounterfeit + gr.totalUnchecked;
            ps.receiptId = gr.receiptId;
            
            if (ps.statToBankValue != 0) {
                sm.getActiveWallet().appendTransaction(ps.typedMemo, ps.statToBankValue, ps.receiptId);
            }
 
            if (!sm.getActiveWallet().isEncrypted()) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
               // importState = IMPORT_STATE_DONE;
                

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
         
    /*
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
    */
    
    class VaulterCb implements CallbackInterface {
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            
            System.out.println("Vaulter finished");
            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    showScreen();
                }
            });
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
    
    /*
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


