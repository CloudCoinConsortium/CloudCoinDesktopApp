package advclient;

import advclient.common.core.Validator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Backupper.BackupperResult;
import global.cloudcoin.ccbank.Eraser.EraserResult;
import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.LossFixer.LossFixerResult;
import global.cloudcoin.ccbank.Receiver.ReceiverResult;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.ServantManager.ServantManager;
import global.cloudcoin.ccbank.ServantManager.ServantManager.makeChangeResult;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoinsResult;
import global.cloudcoin.ccbank.Transfer.TransferResult;
import global.cloudcoin.ccbank.Unpacker.UnpackerResult;
import global.cloudcoin.ccbank.Vaulter.VaulterResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.DNSSn;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Wallet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.MenuItemUI;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * 
 */
public class AdvancedClient  {
    String version = "2.1.27";

    JPanel headerPanel;
    JPanel mainPanel;
    JPanel corePanel;
    JPanel wpanel;
    
    JPanel lwrapperPanel;
    
    String ltag = "Advanced Client";
    JLabel totalText;
    
    //Sets the default screen width and height
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
    JLabel pbarText;
    
    JFrame mainFrame;
       
    final static int TYPE_ADD_BUTTON = 1;
    final static int TYPE_ADD_SKY = 2;
    
    JLabel trTitle, trInventory;
    
    public AdvancedClient() {
        initSystem();
                
        AppUI.init(tw, th); 
        AppCore.logSystemInfo(version);
        
        headerHeight = th / 10;
        
        initMainScreen();
        
        if (!ps.errText.equals("")) {
            JLabel x = new JLabel(ps.errText);
            AppUI.setFont(x, 16);
            AppUI.setMargin(x, 8);
            AppUI.alignCenter(x);
            mainPanel.add(x);
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
        
        String home = System.getProperty("user.home");
        //home += File.separator + "CloudCoinWallet";
            
        sm = new ServantManager(wl, home);
        if (!sm.init()) {
            resetState();
            ps.errText = "Failed to init program. Make sure you have correct folder permissions (" + home + ")";
            return;
        }
    
        AppCore.readConfig();
        resetState();
    }
   
    public void echoDone() {
        ps.isEchoFinished = true;
    }
       
    
    public void setEnvelopes(Hashtable<String, String[]> envelopes) {
        if (ps.currentWalletIdx <= 0)
            return;
        
        Wallet w = wallets[ps.currentWalletIdx - 1];
        
        w.setEnvelopes(envelopes);
    }
    
    public void setSNs(int[] sns) {
        if (ps.currentWalletIdx <= 0)
            return;
        
        Wallet w = wallets[ps.currentWalletIdx - 1];
        
        w.setSNs(sns);
    }
    
    public void walletSetTotal(Wallet w, int total) {
        JLabel cntLabel = (JLabel) w.getuiRef();
             
        wl.debug(ltag, "Set Total: " + w.getName() + " total = " + total);
        
        w.setTotal(total);
        w.setUpdated();
        String strCnt = AppCore.formatNumber(total);
        if (cntLabel == null)
            return;
        
        cntLabel.setText(strCnt);        
        int fsize;
        if (total < 9999)
            fsize = 18;
        else if (total < 999999999)
            fsize = 16;
        else 
            fsize = 14;
            
        if (ps.currentWallet == w) {
            AppUI.setBoldFont(cntLabel, fsize);
        } else {
            AppUI.setFont(cntLabel, fsize);
        }
        cntLabel.repaint();
        
        if (ps.currentScreen == ProgramState.SCREEN_SHOW_TRANSACTIONS) {
            if (w == sm.getActiveWallet()) {
                updateTransactionWalletData(w);
            }
        }
    }
    
    public String getSkyIDError(String name, String pownString) {
        return "<html><div style='width:520px;text-align:center'>Your Sky Coin ID <b>" + name + "</b> is counterfeit. It is not safe to use it.<br><br>"
            + "The Pown String is <b>" + pownString + "</b></div></html>";
    }
    
    public String getSkyIDErrorIfRAIDAFailed() {
        return "<html><div style='width:520px;text-align:center'>Creating a Skywallet requires that all RAIDA are available. "
                + "However, One or more could not be contaced.  Please try again later.</div></html>";
    }
    
    public String getPickError(Wallet w) {
        String errText = "<html><div style='width: 520px; text-align: center'>"
                + "Cannot make change<br><br>This version of software does not support making change<br>"
                + "You may only choose amounts that match your exact notes.<br>Please check to see if"
                + " there is a newer version of the software.<br>In the meantime, you may transfer amounts "
                + "that match the CloudCoin notes.<br>There are people who make change and you can find their addresses by contacting support@cloudcoinmail.com";
                
        
        int[][] counters = w.getCounters();
        
        if (counters == null || counters.length == 0) {
            errText += "</div></html>";
            return errText;
        }
        
        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);
        
        errText += "<br><br>Here is your inventory:<br><br>";
        int t1, t5, t25, t100, t250;
        
        t1 = counters[Config.IDX_FOLDER_BANK][Config.IDX_1] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_1] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_1];
        
        t5 = counters[Config.IDX_FOLDER_BANK][Config.IDX_5] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_5] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_5];
        
        t25 = counters[Config.IDX_FOLDER_BANK][Config.IDX_25] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_25] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_25];
        
        t100 =  counters[Config.IDX_FOLDER_BANK][Config.IDX_100] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_100] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_100];
            
        t250 = counters[Config.IDX_FOLDER_BANK][Config.IDX_250] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_250] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_250];
        
        
        errText += "1 CloudCoin Notes: " + t1 + "<br>";
        errText += "5 CloudCoin Notes: " + t5 + "<br>";
        errText += "25 CloudCoin Notes: " + t25 + "<br>";
        errText += "100 CloudCoin Notes: " + t100 + "<br>";
        errText += "250 CloudCoin Notes: " + t250 + "<br>";

        errText += "</div></html>";

        return errText;
    }
    
    public void setTotalCoins() {
        int total = 0;
        for (int i = 0; i < wallets.length; i++) {
            total += wallets[i].getTotal();
            totalText.setText("" + AppCore.formatNumber(total));
        }
        
        totalText.repaint();
    }
      
    public void setCounters(int[][] counters) {
        ps.counters = counters;
    }
    
    public void cbDone() {
        ps.cbState = ProgramState.CB_STATE_DONE;
    }
    
    public void initSystemUser() {  
        if (!sm.initUser(ps.typedWalletName, ps.typedEmail, ps.typedPassword)) {
            ps.errText = "Failed to init Wallet"; 
            return;
        }

        //AppCore.setDefaultWallet(ps.typedWalletName);
            
        //AppCore.copyTemplatesFromJar(ps.typedWalletName);
    }
    
    public boolean isActiveWallet(Wallet wallet) {
        if (ps.currentWallet == null)
            return false;
        
        return ps.currentWallet.getName().equals(wallet.getName());
    }
    
    public void setActiveWallet(Wallet wallet) {    
        ps.currentWallet = wallet;
        
        sm.cancelSecs();
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
        //creates the background panel 
        mainPanel = new JPanel();
       
        AppUI.setBoxLayout(mainPanel, true);
        AppUI.setSize(mainPanel, tw, th);
        AppUI.setBackground(mainPanel, AppUI.getColor1());
    
        mainFrame = AppUI.getMainFrame(version);
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
    
    public boolean isConfirmingScreen() {
        if (ps.currentScreen == ProgramState.SCREEN_CONFIRM_CLEAR ||
                ps.currentScreen == ProgramState.SCREEN_CONFIRM_DELETE_WALLET ||
                ps.currentScreen == ProgramState.SCREEN_CONFIRM_TRANSFER)
            return true;
        
        return false;
    }
    
    public boolean isMakingChange() {
        if (ps.currentScreen == ProgramState.SCREEN_MAKING_CHANGE)
            return true;
        
        return false;
    }
    
    public boolean isDepositing() {
        if (ps.currentScreen == ProgramState.SCREEN_PREDEPOSIT || 
                ps.currentScreen == ProgramState.SCREEN_DEPOSIT ||
                ps.currentScreen == ProgramState.SCREEN_IMPORTING ||
                ps.currentScreen == ProgramState.SCREEN_IMPORT_DONE ||
                ps.currentScreen == ProgramState.SCREEN_DEPOSIT_LEFTOVER)
            
            return true;
        
        return false;
    }
    
    public boolean isWithdrawing() {
        if (ps.currentScreen == ProgramState.SCREEN_WITHDRAW ||
                ps.currentScreen == ProgramState.SCREEN_CONFIRM_TRANSFER ||
                ps.currentScreen == ProgramState.SCREEN_SENDING ||
                ps.currentScreen == ProgramState.SCREEN_TRANSFER_DONE)
            return true;
        
        return false;
    }
    
    public boolean isFixing() {
        if (ps.currentScreen == ProgramState.SCREEN_FIX_FRACKED ||
                ps.currentScreen == ProgramState.SCREEN_FIXING_FRACKED ||
                ps.currentScreen == ProgramState.SCREEN_FIX_DONE)
            return true;
        
        return false;
    }
    
    public boolean isBackupping() {
        if (ps.currentScreen == ProgramState.SCREEN_BACKUP ||
                ps.currentScreen == ProgramState.SCREEN_BACKUP_DONE ||
                ps.currentScreen == ProgramState.SCREEN_DOING_BACKUP)
            return true;
        
        return false;
    }
    
    public void fillHeaderPanel() {
        //fills header with objects
        JPanel p = new JPanel();
        AppUI.noOpaque(p);
        GridBagLayout gridbag = new GridBagLayout();
       
        p.setLayout(gridbag);
        
        GridBagConstraints c = new GridBagConstraints();      
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 10, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;


        JLabel icon0, icon1, icon2, icon3;
        ImageIcon ii;
        try {
            Image img;
                   
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/Gear icon.png"));
            icon0 = new JLabel(new ImageIcon(img));
            
            ii = new ImageIcon(img);
            
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/Help_Support Icon.png"));
            icon1 = new JLabel(new ImageIcon(img));

            img = ImageIO.read(getClass().getClassLoader().getResource("resources/CloudCoinLogo2.png"));
            icon3 = new JLabel(new ImageIcon(img));
            
        } catch (Exception ex) {
            return;
        }
        
        headerPanel.add(p);
        gridbag.setConstraints(icon3, c);
        p.add(icon3);
        
        if (ps.currentScreen == ProgramState.SCREEN_AGREEMENT) {
             // Init Label
            JLabel titleText = new JLabel("CloudCoin Wallet " + version);
            AppUI.setTitleSemiBoldFont(titleText, 32);
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 10, 0, tw ); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = 0;
 
            
            gridbag.setConstraints(titleText, c);
            p.add(titleText);
            
            return;
        } else {
            JLabel titleText = new JLabel("Total Coins: ");
            AppUI.setTitleSemiBoldFont(titleText, 32);
            c.insets = new Insets(0, 10, 0, 0); 
            gridbag.setConstraints(titleText, c);
            p.add(titleText);
            
            
            c.insets = new Insets(15, 10, 0, 0); 
            JPanel wrp = new JPanel();
            AppUI.setBoxLayout(wrp, false);
            AppUI.setSize(wrp, 216, 32);
            AppUI.noOpaque(wrp);
            
            
            totalText = new JLabel("0");
            AppUI.setTitleFont(totalText, 32);
            //AppUI.setSize(totalText, 100, 32);
            //gridbag.setConstraints(totalText, c);
            //p.add(totalText);
           
            
            wrp.add(totalText);
            
            c.anchor = GridBagConstraints.NORTH;
            titleText = new JLabel("cc");
            AppUI.setTitleFont(titleText, 16);
            AppUI.setSize(titleText, 40, 40);
            AppUI.alignBottom(titleText);
            AppUI.setMargin(titleText, 0, 6, 0, 0);
            
            wrp.add(titleText);
            
            gridbag.setConstraints(wrp, c);
            p.add(wrp);
            
            c.anchor = GridBagConstraints.CENTER;
     
            // Pad
            c.weightx = 1;
            JLabel padd0 = new JLabel();
            gridbag.setConstraints(padd0, c);
            //p.add(padd0);
                        
            c.weightx = 0;
            c.insets = new Insets(0, 10, 0, 60); 
            
            // Deposit Button 
            final JButton b0 = new JButton("Deposit");
            b0.setContentAreaFilled(false);
            b0.setFocusPainted(false);
            b0.setBorderPainted(false);
            AppUI.noOpaque(b0);
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

            // Transfer Button
            final JButton b1 = new JButton("Transfer");
            
            b1.setContentAreaFilled(false);
            b1.setBorderPainted(false);
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
                    
                    resetState();
                    ps.currentScreen = ProgramState.SCREEN_PREDEPOSIT;
                    showScreen();
                }
            };
            
            ActionListener al1 = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JButton b = (JButton) e.getSource();
                    
                    resetState();
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
        }

        c.weightx = 1;
        JLabel padd = new JLabel();
        gridbag.setConstraints(padd, c);
        p.add(padd);
        

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NORTH;
        
        // Icon Gear
        AppUI.setHandCursor(icon0); 
        
        gridbag.setConstraints(icon0, c);
        AppUI.setSize(icon0, 52, 70);
        p.add(icon0);
 
        
        final Color savedColor = icon0.getBackground();
        final JLabel ficon = icon0;
   
        // Do stuff popup menu
        final int mWidth = 212;
        final int mHeight = 60;
        final JPopupMenu popupMenu = new JPopupMenu() {
            @Override
            public void paintComponent(final Graphics g) {
                g.setColor(AppUI.getColor6());
                g.fillRect(0,0,getWidth(), getHeight());
            } 
        };
 
        String[] items = {"Backup", "List serials", "Clear History", "Fix Fracked", 
            "Delete Wallet", "Show Folders", "Echo RAIDA", "Settings", "Sent Coins", "Export Keys"};
        for (int i = 0; i < items.length; i++) {
            JMenuItem menuItem = new JMenuItem(items[i]);
            menuItem.setActionCommand("" + i);
            AppUI.setHandCursor(menuItem);
    
            MouseAdapter ma = new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
                    ps.popupVisible = true;
                    JMenuItem jMenuItem = (JMenuItem) evt.getSource();
                    jMenuItem.setBackground(AppUI.getColor0());
                }
                
                public void mouseExited(MouseEvent evt) {
                    JMenuItem jMenuItem = (JMenuItem) evt.getSource();
                    jMenuItem.setBackground(AppUI.getColor6());
                    
                    ps.popupVisible = false;
                    EventQueue.invokeLater(new Runnable() {
                        public void run(){
                            try {
                                 Thread.sleep(40);
                            } catch(InterruptedException ex) {
                            }

                            if (ps.popupVisible)
                                return;

                            ficon.setOpaque(false);
                            ficon.repaint();
                            popupMenu.setVisible(false);
                        }
                    });                  
                }
                
                public void mouseReleased(MouseEvent evt) {
                    JMenuItem jMenuItem = (JMenuItem) evt.getSource();
                    popupMenu.setVisible(false);
                    
                    resetState();
                    
                    String action = jMenuItem.getActionCommand();
                    if (action.equals("0")) {
                        ps.currentScreen = ProgramState.SCREEN_BACKUP;
                    } else if (action.equals("1")) {
                        ps.currentScreen = ProgramState.SCREEN_LIST_SERIALS;
                    } else if (action.equals("2")) {
                        ps.currentScreen = ProgramState.SCREEN_CLEAR;
                    } else if (action.equals("3")) {
                        ps.currentScreen = ProgramState.SCREEN_FIX_FRACKED;
                    } else if (action.equals("4")) {
                        ps.currentScreen = ProgramState.SCREEN_DELETE_WALLET;
                    } else if (action.equals("5")) {
                        ps.currentScreen = ProgramState.SCREEN_SHOW_FOLDERS;
                    } else if (action.equals("6")) {
                        ps.currentScreen = ProgramState.SCREEN_ECHO_RAIDA;
                    } else if (action.equals("7")) {
                        ps.currentScreen = ProgramState.SCREEN_SETTINGS;
                    } else if (action.equals("8")) {
                        ps.currentScreen = ProgramState.SCREEN_SHOW_SENT_COINS;                        
                    } else if (action.equals("9")) {
                        ps.currentScreen = ProgramState.SCREEN_SHOW_BACKUP_KEYS; 
                    }
                    showScreen();
                }
            };
            
            menuItem.addMouseListener(ma);
            
            AppUI.setSize(menuItem, mWidth, mHeight);
            AppUI.setFont(menuItem, 28);
            menuItem.setOpaque(true);

            menuItem.setBackground(AppUI.getColor6());
            menuItem.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
            menuItem.setUI(new MenuItemUI() {
                public void paint (final Graphics g, final JComponent c) {
                    final Graphics2D g2d = (Graphics2D) g;
                    final JMenuItem menuItem = (JMenuItem) c;
                    String ftext = menuItem.getText();
 
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int width = g.getFontMetrics().stringWidth(ftext);
                    int cHeight = c.getHeight();

                    g.setColor(Color.WHITE);     
                    g.drawChars(ftext.toCharArray(), 0, ftext.length(), 12, cHeight/2 + 6);
                }
            });

            popupMenu.add(menuItem);
        }
        
        AppUI.setMargin(popupMenu, 0);
        AppUI.noOpaque(popupMenu);
        AppUI.setHandCursor(popupMenu);

        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                ps.popupVisible = true;
            }
            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                AppUI.setBackground(ficon, savedColor);
                ficon.setOpaque(false);
                ficon.repaint();
                ps.popupVisible = false;
            }
            
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
                AppUI.setBackground(ficon, savedColor);
                ficon.setOpaque(false);
                ficon.repaint();
                ps.popupVisible = false;
            }
        });

        icon0.addMouseListener(new MouseAdapter() {
            /*
            public void mouseReleased(MouseEvent e) {
                ficon.setOpaque(true);
                AppUI.setBackground(ficon, AppUI.getColor6());

                ficon.repaint();              
                popupMenu.show(ficon, 0 - mWidth  + ficon.getWidth(), ficon.getHeight());
            }*/
            
            public void mouseEntered(MouseEvent e) {
                ficon.setOpaque(true);
                AppUI.setBackground(ficon, AppUI.getColor6());

                ficon.repaint();              
                popupMenu.show(ficon, 0 - mWidth  + ficon.getWidth(), ficon.getHeight());
            }
            
            public void mouseExited(MouseEvent e) {
                ps.popupVisible = false;
                EventQueue.invokeLater(new Runnable() {
                    public void run(){
                        try {
                            Thread.sleep(40);
                        } catch(InterruptedException ex) {
                        }

                        if (ps.popupVisible)
                            return;

                        ficon.setOpaque(false);
                        ficon.repaint();
                        popupMenu.setVisible(false);
                    }
                });
            }
        });
        
        
        // Icon Support
        c.insets = new Insets(0, 10, 0, 20); 
        AppUI.noOpaque(icon1);
        AppUI.setHandCursor(icon1);
        gridbag.setConstraints(icon1, c);
        p.add(icon1);
        icon1.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                ps.currentScreen = ProgramState.SCREEN_SUPPORT;
                showScreen();
            }
        });
 
        // Version
        c.insets = new Insets(0, 0, 20, 10);
        JLabel vl = new JLabel("v. " + version);
        AppUI.noOpaque(vl);
        AppUI.setTitleFont(vl, 14);
        gridbag.setConstraints(vl, c);
        p.add(vl);
        
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
        if (sm == null) 
            return;

        if (sm.getWallets().length != 0) {
            setActiveWallet(sm.getWallets()[0]);
            ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
        }
    }
    
    public void showScreen() {
        wl.debug(ltag, "SCREEN " + ps.currentScreen + ": " + ps.toString());
        clear();

        if (ps.needInitWallets) {
            sm.initWallets();
            ps.needInitWallets = false;
        }
        
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
            case ProgramState.SCREEN_SETTING_DNS_RECORD:
                showSetDNSRecordScreen();
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
                showTransferScreen();
                break;
            case ProgramState.SCREEN_IMPORTING:
                showImportingScreen();
                break;
            case ProgramState.SCREEN_IMPORT_DONE:
                showImportDoneScreen();
                break;
            case ProgramState.SCREEN_SUPPORT:
                showSupportScreen();
                break;
            case ProgramState.SCREEN_CONFIRM_TRANSFER:
                showConfirmTransferScreen();
                break;
            case ProgramState.SCREEN_TRANSFER_DONE:
                showTransferDoneScreen();
                break;
            case ProgramState.SCREEN_BACKUP:
                showBackupScreen();
                break;
            case ProgramState.SCREEN_DOING_BACKUP:
                showDoingBackupScreen();
                break;
            case ProgramState.SCREEN_SENDING:
                showSendingScreen();
                break;
            case ProgramState.SCREEN_BACKUP_DONE:
                showBackupDoneScreen();
                break;
            case ProgramState.SCREEN_LIST_SERIALS:
                showListSerialsScreen();
                break;
            case ProgramState.SCREEN_CLEAR:
                showClearScreen();
                break;
            case ProgramState.SCREEN_LIST_SERIALS_DONE:
                showListSerialsDone();
                break;
            case ProgramState.SCREEN_CONFIRM_CLEAR:
                showConfirmClearScreen();
                break;
            case ProgramState.SCREEN_CLEAR_DONE:
                showClearDoneScreen();
                break;
            case ProgramState.SCREEN_FIX_FRACKED:
                showFixfrackedScreen();
                break;
            case ProgramState.SCREEN_FIXING_FRACKED:
                showFixingfrackedScreen();
                break;
            case ProgramState.SCREEN_FIX_DONE:
                showFixDoneScreen();
                break;
            case ProgramState.SCREEN_DELETE_WALLET:
                showDeleteWalletScreen();
                break;
            case ProgramState.SCREEN_CONFIRM_DELETE_WALLET:
                showConfirmDeleteWalletScreen();
                break;
            case ProgramState.SCREEN_DELETE_WALLET_DONE:
                showDeleteWalletDoneScreen();
                break;
            case ProgramState.SCREEN_SKY_WALLET_CREATED:
                showSkyWalletCreatedScreen();
                break;
            case ProgramState.SCREEN_PREDEPOSIT:
                showPredepositScreen();
                break;
            case ProgramState.SCREEN_SHOW_FOLDERS:
                showFoldersScreen();
                break;
            case ProgramState.SCREEN_ECHO_RAIDA:
                showEchoRAIDAScreen();
                break;
            case ProgramState.SCREEN_ECHO_RAIDA_FINISHED:
                showEchoRAIDAFinishedScreen();
                break;
            case ProgramState.SCREEN_MAKING_CHANGE:
                showMakingChangeScreen();
                break;
            case ProgramState.SCREEN_EXPORTING:
                showExportingScreen();
                break;
            case ProgramState.SCREEN_CHECKING_SKYID:
                showCheckingSkyIDScreen();
                break;
            case ProgramState.SCREEN_SETTINGS:
                showSettingsScreen();
                break;
            case ProgramState.SCREEN_SETTINGS_SAVED:
                showSettingsDoneScreen();
                break;
            case ProgramState.SCREEN_WARN_FRACKED_TO_SEND:
                showWarnFrackedToSend();
                break;
            case ProgramState.SCREEN_DEPOSIT_LEFTOVER:
                showDepositLeftover();
                break;
            case ProgramState.SCREEN_SHOW_SENT_COINS:
                showSentCoins();
                break;
            case ProgramState.SCREEN_SHOW_BACKUP_KEYS:
                showBackupKeys();
                break;
            case ProgramState.SCREEN_SHOW_BACKUP_KEYS_DONE:
                showBackupKeysDone();
                break;
        }
        
        
     //   headerPanel.repaint();
     //   headerPanel.revalidate();

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
            if (ps.srcWallet != null)
                ps.srcWallet.setNotUpdated();
            
            if (ps.dstWallet != null)
                ps.dstWallet.setNotUpdated();
            
            p.revalidate();
            p.repaint();
        }
        
    }
    
    private void setRAIDAProgressCoins(int raidaProcessed, int totalCoinsProcessed, int totalCoins) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        if (totalCoins == 0)
            return;
        
        String stc = AppCore.formatNumber(totalCoinsProcessed);
        String tc = AppCore.formatNumber(totalCoins);
        
        pbarText.setText("Deposited " + stc + " / " + tc + " CloudCoins");
        pbarText.repaint();
        
    }
    
    /*
    private void setRAIDAProgress(int raidaProcessed, int totalFilesProcessed, int totalFiles) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        pbarText.setText(totalFilesProcessed + " / " + totalFiles + " Notes Deposited");
        pbarText.repaint();
    }
    */
    /*
    private void setRAIDATransferProgress(int raidaProcessed, int totalFilesProcessed, int totalFiles) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        pbarText.setText(totalFilesProcessed + " / " + totalFiles + " Notes Transferred");
        pbarText.repaint();
    }
    */
    private void setRAIDATransferProgressCoins(int raidaProcessed, int totalCoinsProcessed, int totalCoins) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        String stc = AppCore.formatNumber(totalCoinsProcessed);
        String tc = AppCore.formatNumber(totalCoins);
        
        pbarText.setText("Transferred " + stc + " / " + tc + " CloudCoins");
        pbarText.repaint();
    }
    
    private void setRAIDAFixingProgressCoins(int raidaProcessed, int totalCoinsProcessed, int totalCoins, int fixingRAIDA, int round) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        String stc = AppCore.formatNumber(totalCoinsProcessed);
        String tc = AppCore.formatNumber(totalCoins);
        
        pbarText.setText("<html><div style='text-align:center'>Round #" + round + " Fixing on RAIDA " + 
                fixingRAIDA + "<br>" + stc + " / " + tc + " CloudCoins Fixed</div></html>");
        
        pbarText.repaint();
    }
    
    /*
    private void setRAIDAFixingProgress(int raidaProcessed, int totalFilesProcessed, int totalFiles, int fixingRAIDA, int round) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        pbarText.setText("<html><div style='text-align:center'>Round #" + round + " Fixing on RAIDA " + 
                fixingRAIDA + "<br>" + totalFilesProcessed + " / " + totalFiles + " Notes Fixed</div></html>");
        pbarText.repaint();
    }
    */
    
    public void showFixingfrackedScreen() {
        JPanel subInnerCore = getModalJPanel("Fixing in Progress");
        maybeShowError(subInnerCore);

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        // Text Label
        pbarText = new JLabel("");
        AppUI.setCommonFont(pbarText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(pbarText, c);
        ct.add(pbarText);
        
        y++;
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
        c.gridy = y;
        gridbag.setConstraints(pbar, c);
        ct.add(pbar);
        
        JPanel bp = getOneButtonPanelCustom("Cancel", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sm.cancel("FrackFixer");

                showScreen();
            }
        });
       
        subInnerCore.add(bp);  
        
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    while (!ps.isEchoFinished) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {}
                
                wl.debug(ltag, "Fixing coins in " + ps.srcWallet.getName());
                
                sm.setActiveWalletObj(ps.srcWallet);            
                sm.startFrackFixerService(new FrackFixerOnPurposeCb());
            }
        });
        
        t.start();
    }
    
    public void showEchoRAIDAScreen() {
        JPanel subInnerCore = getModalJPanel("Checking RAIDA");
        maybeShowError(subInnerCore);

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        AppUI.hr(ct, 60);
        
        JLabel x = new JLabel("Please wait...");
        AppUI.setCommonFont(x);
        AppUI.alignCenter(x);
        ct.add(x);

        sm.startEchoService(new EchoCb());
    }
    
    public void showEchoRAIDAFinishedScreen() {
        JPanel subInnerCore = getModalJPanel("RAIDA Status");
        maybeShowError(subInnerCore);

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        AppUI.hr(ct, 2);
        JLabel x;

        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        /*
        x = new JLabel("");
        AppUI.setFont(x, 15);
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);*/
        
        int[] statuses = sm.getRAIDAStatuses();
        
        int y = 1;
        int fontSize = 16;
        boolean isfailed = false;
        for (int i = 0; i < statuses.length / 2 + 1; i ++) {
            String status;
            
            x = new JLabel(AppCore.getRAIDAString(i));
            AppUI.setCommonTableFontSize(x, fontSize);
            AppUI.setColor(x, AppUI.getColor14());
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 2, 0); 
            c.gridx = 0;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
               
            x = new JLabel("");
            if (statuses[i] == -1) {
                status = "TIMED OUT";
                isfailed = true;
                AppUI.setColor(x, AppUI.getErrorColor());
            } else {
                status = AppCore.getMS(statuses[i]);
                AppUI.setColor(x, AppUI.getColor14());

            }
            
            x.setText(status);

            AppUI.setCommonTableFontSize(x, fontSize);
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(0, 40, 2, 0); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
       
            int j = i + RAIDA.TOTAL_RAIDA_COUNT / 2 + 1;
            if (j == RAIDA.TOTAL_RAIDA_COUNT)
                break;
            
            x = new JLabel(AppCore.getRAIDAString(j));
            AppUI.setColor(x, AppUI.getColor14());
            AppUI.setCommonTableFontSize(x, fontSize);
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 100, 2, 0); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
               
            x = new JLabel("");
            if (statuses[j] == -1) {
                status = "TIMED OUT";
                isfailed = true;
                AppUI.setColor(x, AppUI.getErrorColor());
            } else {
                status = AppCore.getMS(statuses[j]);
                AppUI.setColor(x, AppUI.getColor14());
            }
            
            
            x.setText(status);
            AppUI.setCommonTableFontSize(x, fontSize);
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(0, 40, 2, 0); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);

            y++;
        }

        if (isfailed) {
            y++;
            String txt = "<html><div style='width:460px'>TIMED OUT means the response exceeded the " + Config.ECHO_TIMEOUT / 1000 + " seconds allowed. "
                + "This could be caused by a slow network or because the RAIDA was blocked (usually by office routers). "
                + "It could also be caused by your computer being old and unable to handle 25 threads at once. "
                + "Try changing your settings to increase the Timeout. Or try using a more powerful computer.</div></html>";  

            x = new JLabel(txt);
            AppUI.setCommonTableFontSize(x, fontSize);      
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(8, 0, 2, 0); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            c.gridwidth = 4;
            gridbag.setConstraints(x, c);
            ct.add(x);
        }

        
    }
    
    public int getSkyWalletSN() {
        int skySN = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!wallets[i].isSkyWallet())
                continue;
        
            skySN = wallets[i].getIDCoin().sn;
            break;
        }
        
        return skySN;
    }
    
    public void showMakingChangeScreen() {
        JPanel subInnerCore = getModalJPanel("Request Change");
        maybeShowError(subInnerCore);

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
               
        pbarText = new JLabel("");
        AppUI.setCommonFont(pbarText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(pbarText, c);
        ct.add(pbarText);
        
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

        subInnerCore.add(AppUI.hr(120));
        
        pbar.setVisible(false);
        int cnt = AppCore.getFilesCount(Config.DIR_DETECTED, ps.srcWallet.getName());
        if (cnt != 0) {
            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
            ps.errText = getNonEmptyFolderError("Detected");
            showScreen();
            return;
        }
            
        final int skySN = getSkyWalletSN();
        if (skySN == 0) {
            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
            ps.errText = "<html><div style='width:690; text-align:center'>Transaction cannot be completed. You must have the exact denominations of CloudCoin notes or use the Change Maker.  You must have at least one Sky Wallet created to access the Change Maker</div></html>";
            showScreen();
            return;
        }

        Thread t = new Thread(new Runnable() {
            public void run() {
                if (!ps.isEchoFinished) {
                    pbarText.setText("Checking RAIDA ...");
                    pbarText.repaint();
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                
                while (!ps.isEchoFinished) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                }
                
                pbarText.setText("Querying coins ...");
                pbarText.repaint();

                sm.setActiveWalletObj(ps.srcWallet);
                boolean rv = sm.makeChange(ps.srcWallet, ps.typedAmount, skySN, new CallbackInterface() {
                    public void callback(Object o) {
                        makeChangeResult mcr = (makeChangeResult) o;
                        
                        pbarText.setText(mcr.text);
                        if (mcr.status == 1) {
                            pbar.setVisible(true);
                            pbar.setValue(mcr.progress);
                        } else if (mcr.status == 2) {
                            wl.debug(ltag, "Change done successfully. Retrying");
                            if (ps.changeFromExport) {
                                EventQueue.invokeLater(new Runnable() {         
                                    public void run() {                                    
                                        if (ps.srcWallet.isEncrypted()) {
                                            sm.startSecureExporterService(Config.TYPE_STACK, ps.typedAmount, ps.typedMemo, ps.chosenFile, false, new ExporterCb());
                                        } else {
                                            sm.startExporterService(Config.TYPE_STACK, ps.typedAmount, ps.typedMemo, ps.chosenFile, false, new ExporterCb());
                                        }                                    
                                    }
                                });
                            } else {
                                EventQueue.invokeLater(new Runnable() {         
                                    public void run() {  
                                        String dstName =  (ps.foundSN == 0) ? ps.dstWallet.getName() : "" + ps.foundSN;
                                        
                                        sm.transferCoins(ps.srcWallet.getName(), dstName, 
                                            ps.typedAmount, ps.typedMemo, ps.typedRemoteWallet, new SenderCb(), new ReceiverCb());
                                    }
                                });
                            }
                        } else {
                            pbar.setVisible(false);
                        }
                        
                        if (!mcr.errText.isEmpty()) {
                            EventQueue.invokeLater(new Runnable() {         
                                    public void run() {  
                                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                                        ps.errText = mcr.errText;
                                        showScreen();
                                    }
                            });
                            return;
                        }
                    }                   
                });
            
            }
        });
        
        t.start();
        
    }
    
    public void showSendingScreen() {
        JPanel subInnerCore = getModalJPanel("Transfer in Progress");
        maybeShowError(subInnerCore);

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        String fwallet = ps.srcWallet.getName();
        String twallet;
        if (ps.sendType == ProgramState.SEND_TYPE_REMOTE) {              
            twallet = ps.typedRemoteWallet;
        } else {
            twallet = ps.dstWallet.getName();
        }
        
        int y = 0;
        
        // Info
        JLabel x = new JLabel("<html><div style='width:480px;text-align:center'>From Wallet <b>" + fwallet + " </b> To Wallet <b>" + twallet + "</b></div></html>");
        AppUI.setFont(x, 18);
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(4, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        // Warning Label
        x = new JLabel("<html><div style='width:480px;text-align:center'>Do not close the application until all CloudCoins are transferred!</div></html>");
        AppUI.setCommonFont(x);
        AppUI.setColor(x, AppUI.getErrorColor());
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(20, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        pbarText = new JLabel("");
        AppUI.setCommonFont(pbarText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(pbarText, c);
        ct.add(pbarText);
        
        y++;
        
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
        c.gridy = y;
        gridbag.setConstraints(pbar, c);
        ct.add(pbar);
        
        y++;

        subInnerCore.add(AppUI.hr(120));
        
        Thread t = new Thread(new Runnable() {
            public void run(){
                pbar.setVisible(false);
                if (!ps.isEchoFinished) {
                    pbarText.setText("Checking RAIDA ...");
                    pbarText.repaint();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}

                while (!ps.isEchoFinished) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                }

                if (!sm.isRAIDAOK()) {
                    ps.errText = "<html><div style='width:520px;text-align:center'>RAIDA cannot be contacted. "
                            + "This is usually caused by company routers blocking outgoing traffic. "
                            + "Please Echo RAIDA and try again.</div></html>";
                    ps.isEchoFinished = false;
                    ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    showScreen();
                    return;
                }

                CloudCoin skyCC = null;
                ps.coinIDinFix = null;
                if (ps.srcWallet.isSkyWallet()) {
                    ps.isCheckingSkyID = true;
                    skyCC = ps.srcWallet.getIDCoin();
                 
                    pbarText.setText("Checking Your Source SkyWallet ID");
                    pbarText.repaint();

                    sm.startAuthenticatorService(skyCC, new AuthenticatorForSkyCoinCb());
                    while (ps.isCheckingSkyID) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {}
                    }
                    
                    if (AppCore.getErrorCount(skyCC) > Config.MAX_FAILED_RAIDAS_TO_SEND) {
                        ps.errText = getSkyIDErrorIfRAIDAFailed();
                        showScreen();
                        return;
                    } else if (AppCore.getPassedCount(skyCC) < Config.PASS_THRESHOLD) {
                        ps.errText = getSkyIDError(ps.srcWallet.getName(), ps.srcWallet.getIDCoin().getPownString());
                        showScreen();
                        return;
                    } else if (AppCore.getPassedCount(skyCC) != RAIDA.TOTAL_RAIDA_COUNT) {
                        ps.currentScreen = ProgramState.SCREEN_WARN_FRACKED_TO_SEND;
                        showScreen();
                        return;
                    }
                } 
                
                if (ps.dstWallet != null && ps.dstWallet.isSkyWallet()) {
                    ps.isCheckingSkyID = true;
                    skyCC = ps.dstWallet.getIDCoin();
                 
                    pbarText.setText("Checking Your Destination SkyWallet ID");
                    pbarText.repaint();

                    sm.startAuthenticatorService(skyCC, new AuthenticatorForSkyCoinCb());
                    while (ps.isCheckingSkyID) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {}
                    }
                    
                    if (AppCore.getErrorCount(skyCC) > Config.MAX_FAILED_RAIDAS_TO_SEND) {
                        ps.errText = getSkyIDErrorIfRAIDAFailed();
                        showScreen();
                        return;
                    } else if (AppCore.getPassedCount(skyCC) < Config.PASS_THRESHOLD) {
                        ps.errText = getSkyIDError(ps.dstWallet.getName(), ps.dstWallet.getIDCoin().getPownString());
                        showScreen();
                        return;
                    } else if (AppCore.getPassedCount(skyCC) != RAIDA.TOTAL_RAIDA_COUNT) {
                        ps.srcWallet = ps.dstWallet;
                        ps.currentScreen = ProgramState.SCREEN_WARN_FRACKED_TO_SEND;
                        showScreen();
                        return;
                    }
                }
                
                String dstName =  (ps.foundSN == 0) ? ps.dstWallet.getName() : "" + ps.foundSN;

                // Remote wallet
                if (ps.srcWallet.isSkyWallet()) {
                    if ((ps.dstWallet == null && !ps.typedRemoteWallet.isEmpty()) || ps.dstWallet.isSkyWallet()) {
                        wl.debug(ltag, "Transfer requested");
                        
                        int fromsn = ps.srcWallet.getIDCoin().sn;
                        int tosn;
                        if (ps.dstWallet != null) {
                            tosn = ps.dstWallet.getIDCoin().sn;
                        } else {
                            tosn = ps.foundSN;
                        }
                        
                        sm.startTransferService(fromsn, tosn, ps.srcWallet.getSNs(), ps.typedAmount, ps.typedMemo, new TransferCb());
                        
                        return;
                    }
                }
 
                if (ps.isSkyDeposit) {
                    wl.debug(ltag, "sky deposit");
                    if (ps.srcWallet.getIDCoin() == null) {
                        pbarText.setText("Failed to find ID coin");
                        pbarText.repaint();
                        return;
                    }
                    int sn = ps.srcWallet.getIDCoin().sn;
                    
                    pbarText.setText("Querying coins ...");
                    pbarText.repaint();
                    
                    sm.startShowSkyCoinsService(new ShowEnvelopeCoinsForReceiverCb(), sn);
                    return;
                }
                
                

                wl.debug(ltag, "Sending to dst " + dstName);
                sm.transferCoins(ps.srcWallet.getName(), dstName, 
                        ps.typedAmount, ps.typedMemo, ps.typedRemoteWallet, new SenderCb(), new ReceiverCb());
            }
        });
        
        t.start();
    }
    
    
    public void showImportingScreen() {
        JPanel subInnerCore = getModalJPanel("Deposit in Progress");
        maybeShowError(subInnerCore);

        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);

        JLabel x = new JLabel("<html><div style='width:480px;text-align:center'>Do not close the application until all CloudCoins are deposited!</div></html>");
        AppUI.setCommonFont(x);
        //AppUI.setBoldFont(x, 16);
        AppUI.setColor(x, AppUI.getErrorColor());
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        pbarText = new JLabel("");
        AppUI.setCommonFont(pbarText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(pbarText, c);
        ct.add(pbarText);
        
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
        
        subInnerCore.add(AppUI.hr(120));
        
        Thread t = new Thread(new Runnable() {
            public void run(){
                pbar.setVisible(false);
                if (!ps.isEchoFinished) {
                    pbarText.setText("Checking RAIDA ...");
                    pbarText.repaint();
                }
                
                wl.debug(ltag, "Going here");
                while (!ps.isEchoFinished) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}

                }
                
                if (!sm.isRAIDAOK()) {
                    ps.errText = "<html><div style='width:520px;text-align:center'>RAIDA cannot be contacted. "
                            + "This is usually caused by company routers blocking outgoing traffic. "
                            + "Please Echo RAIDA and try again.</div></html>";
                    ps.isEchoFinished = false;
                    ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    showScreen();
                    return;
                }

                ps.dstWallet.setPassword(ps.typedPassword);
                sm.setActiveWalletObj(ps.dstWallet);
                
                pbarText.setText("Moving coins ...");
                for (String filename : ps.files) {
                    String name = sm.getActiveWallet().getName();
                    AppCore.moveToFolderNoTs(filename, Config.DIR_IMPORT, name);
                }

                pbarText.setText("Unpacking coins ...");
                pbarText.repaint();
                
                wl.debug(ltag, "issky " + ps.isSkyDeposit);
                if (ps.isSkyDeposit) {
                    sm.startUnpackerService(new UnpackerSenderCb());
                } else {
                    sm.startUnpackerService(new UnpackerCb());
                }
            }
        });
        
        t.start();
        
    }
    
    public void showFixDoneScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        //String total = AppCore.formatNumber(ps.statTotalFracked);
        String totalFixed = AppCore.formatNumber(ps.statTotalFixed);
        //String totalFailedToFix = AppCore.formatNumber(ps.statFailedToFix);
        
        
        String total = AppCore.formatNumber(ps.statTotalFrackedValue);
        String totalFixedValue = AppCore.formatNumber(ps.statTotalFixedValue);
        //String totalFailedToFixValue = AppCore.formatNumber(ps.statTotalFrackedValue - ps.statTotalFixedValue);

        subInnerCore = getModalJPanel("Fix Complete");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);

        
        String txt;
        if (!totalFixed.equals("" + total)) {
            txt = "Not all CloudCoins from <b>" + ps.srcWallet.getName() + "</b> had been fixed. Try it again later";
        } else {
            txt = "Your CloudCoins from <b>" + ps.srcWallet.getName() + "</b> have been fixed";
        }
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>" + txt + "</div></html>");
          
        AppUI.setCommonFont(x);
 
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
 
        x = new JLabel("Total Fracked Coins:");
        AppUI.setCommonFont(x);
        
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(50, 0, 4, 10);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(total);
        AppUI.setCommonBoldFont(x);
        
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel("Total Fixed Coins:");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 4, 10);
        c.gridx = 0;
        c.gridy = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(totalFixedValue);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
                 
        JPanel bp = getOneButtonPanel();
  
        resetState();
        
        subInnerCore.add(bp);       
    }
    
    public void showBackupKeysDone() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        subInnerCore = getModalJPanel("Export Complete");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>" +
            "Your ID keys have been backed up into" +
            "</div></html>");
          
        AppUI.setCommonFont(x);
 
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        
        final String fdir = ps.chosenFile;
        JLabel sl = AppUI.getHyperLink(fdir, "javascript:void(0); return false", 20);
        sl.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (!Desktop.isDesktopSupported())
                    return;
                try {
                    Desktop.getDesktop().open(new File(fdir));
                } catch (IOException ie) {
                    wl.error(ltag, "Failed to open browser: " + ie.getMessage());
                }
            }
        });
        
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(sl, c);
        ct.add(sl);
        
        y++;

        JPanel bp = this.getOneButtonPanel();
        resetState();
        
        subInnerCore.add(bp);     
    
    }
    
    public void showBackupDoneScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        subInnerCore = getModalJPanel("Backup Complete");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>" +
            "Your CloudCoins from <b>" + ps.srcWallet.getName() + "</b> have been backed up into" +
            "</div></html>");
          
        AppUI.setCommonFont(x);
 
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        
        final String fdir = ps.chosenFile;
        JLabel sl = AppUI.getHyperLink(fdir, "javascript:void(0); return false", 20);
        sl.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (!Desktop.isDesktopSupported())
                    return;
                try {
                    Desktop.getDesktop().open(new File(fdir));
                } catch (IOException ie) {
                    wl.error(ltag, "Failed to open browser: " + ie.getMessage());
                }
            }
        });
        
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(sl, c);
        ct.add(sl);
        
        y++;
    
        x = new JLabel("<html><div style='width:400px; text-align:center'>" +
           "All backups are unencrypted. You should save them in a secure location. The CloudCoin Consortium "
                + "recommends opening a free account at </div></html>"
                + "to store backups and passwords.</div></html>");
          
        AppUI.setFont(x, 18);
        c.insets = new Insets(20, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        
        y++;
        
        x = AppUI.getHyperLink("https://SecureSafe.com", "https://securesafe.com", 18);
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        x = new JLabel("<html><div style='width:400px; text-align:center'>to store backups and passwords.</div></html>");
        AppUI.setFont(x, 18);
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        JPanel bp = this.getOneButtonPanel();
        
        resetState();
        
        subInnerCore.add(bp);     
    }
    
    public void showTransferDoneScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
 
        ps.srcWallet.setNotUpdated();
        if (ps.dstWallet != null)
            ps.dstWallet.setNotUpdated();
        
        subInnerCore = getModalJPanel("Transfer Complete");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        String to;
        if (ps.sendType == ProgramState.SEND_TYPE_WALLET) {
            to = ps.dstWallet.getName();
        } else if (ps.sendType == ProgramState.SEND_TYPE_REMOTE) {
            to = ps.typedRemoteWallet;
        } else if (ps.sendType == ProgramState.SEND_TYPE_FOLDER) {
            to = ps.chosenFile;
        } else {
            to = "?";
        }
        
        String name = ps.srcWallet.getName();
        //if (ps.srcWallet.isSkyWallet())
        //    name += "." + Config.DDNS_DOMAIN;
        
        JLabel x;
        
        if (ps.needExtra) {
            String total = AppCore.formatNumber(ps.statToBankValue + ps.statFailedValue + ps.statLostValue);
            x = new JLabel("<html><div style='width:400px; text-align:center'>"
                + "You wanted <b>" + AppCore.formatNumber(ps.typedAmount) + " CC</b> but you got <b>" + total + " CC</b> "
                + " because you did not have perfect change. The coins have been transferred to <b>" + to + "</b> from <b>"
                + name + "</b></div></html>");           
        } else {
            x = new JLabel("<html><div style='width:400px; text-align:center'>"
            + "<b>" + AppCore.formatNumber(ps.typedAmount) + " CC</b> have been transferred to <b>" + to + "</b> from <b>"
            + name + "</b></div></html>");
        }
        
        AppUI.setCommonFont(x);
 
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        JPanel bp = getTwoButtonPanelCustom("Next Transfer", "Done", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetState();
                ps.currentScreen = ProgramState.SCREEN_WITHDRAW;
                showScreen();
            }
        },  new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                if (ps.srcWallet != null) {
                    setActiveWallet(ps.srcWallet);
                    ps.sendType = 0;
                    ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                } else {
                    ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                }
                showScreen();
            }
        });
  
        //resetState();
        
        subInnerCore.add(bp);        
    }
    
    public void showImportDoneScreen() {
        
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        ps.dstWallet.setNotUpdated();
        if (ps.srcWallet != null)
            ps.srcWallet.setNotUpdated();
             
        subInnerCore = getModalJPanel("Deposit Complete");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        String total = AppCore.formatNumber(ps.statToBankValue + ps.statFailedValue + ps.statLostValue);
        String totalBankValue = AppCore.formatNumber(ps.statToBankValue);
        String totalFailedValue = AppCore.formatNumber(ps.statFailedValue);
        String totalLostValue = AppCore.formatNumber(ps.statLostValue);
        String totalFailedFiles = AppCore.formatNumber(ps.failedFiles);
        
        JLabel x;
        x = new JLabel("<html><div style='width:400px; text-align:center'>Deposited <b>" +  total +  " CloudCoins</b> to <b>" + ps.dstWallet.getName() + " </b></div></html>");
        
        AppUI.setCommonFont(x);
 
        int y = 0;
        
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        // Auth
        x = new JLabel("Total Authentic Coins:");
        AppUI.setCommonFont(x);
        
        c.weightx = 1;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(50, 0, 4, 10);
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(totalBankValue);
        AppUI.setCommonBoldFont(x);
        
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        // Counterfeit
        x = new JLabel("Total Counterfeit Coins:");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 4, 10);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(totalFailedValue);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
        
        // Lost
        x = new JLabel("Total Lost Coins:");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(10, 0, 4, 10);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        x = new JLabel(totalLostValue);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        y++;
 
        // Previously imported
        if (ps.duplicates.size() > 0) {
            x = new JLabel("Previously Imported Coins:");
            AppUI.setCommonFont(x);
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(10, 0, 4, 10);
            c.gridx = 0;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
        
            x = new JLabel("" + ps.duplicates.size());
            AppUI.setCommonBoldFont(x);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
        }
        
        if (ps.failedFiles > 0) {
            x = new JLabel("Corrupted files:");
            AppUI.setCommonFont(x);
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(10, 0, 4, 10);
            c.gridx = 0;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
        
            x = new JLabel(totalFailedFiles);
            AppUI.setCommonBoldFont(x);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            gridbag.setConstraints(x, c);
            ct.add(x);
        }
        
        
        JPanel bp = getTwoButtonPanelCustom("Next Deposit", "Continue", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetState();
                ps.currentScreen = ProgramState.SCREEN_PREDEPOSIT;
                showScreen();
            }
        },  new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setActiveWallet(ps.dstWallet);
                ps.sendType = 0;
                ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                //ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
        
        subInnerCore.add(bp);          
    }
    
    public void showDeleteWalletDoneScreen() {
        boolean isError = !ps.errText.equals("");      
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        subInnerCore = getModalJPanel("Confirmation");
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
   
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;


        String txt = "Your Wallet <b>" + ps.srcWallet.getName() + "</b> has been deleted.";
 
        y++;
        // Q
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>" + txt + "</div></html>");
        AppUI.setCommonBoldFont(x);
        c.insets = new Insets(42, 0, 4, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++; 
             
        resetState();
        
        JPanel bp = getOneButtonPanel();
        subInnerCore.add(bp);  
        
    }
    
    public void showClearDoneScreen() {
        boolean isError = !ps.errText.equals("");      
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        subInnerCore = getModalJPanel("Confirmation");
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
   
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;


        String txt;
        
        if (!ps.needBackup)
            txt = "Your Log Files and Transaction History have been permanently deleted.";
        else 
            txt = "Your Log Files and Transaction History have been backed up<br><br>They have been permanently deleted from Advanced Client.";
        
        y++;
        // Q
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>" + txt + "</div></html>");
        AppUI.setCommonBoldFont(x);
        c.insets = new Insets(42, 0, 4, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++; 
        
        resetState();
             
        JPanel bp = getOneButtonPanel();
        subInnerCore.add(bp);       
    }
    
    public void showDepositLeftover() {
        JPanel subInnerCore;

        subInnerCore = getModalJPanel("Deposit");
     
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
                
        int y = 0;
        // From Label
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>Left over coins found from last time. Click to import them</div></html>");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        y++; 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wl.debug(ltag, "Leftover deposit");

                ps.currentScreen = ProgramState.SCREEN_IMPORTING;
                ps.srcWallet = null;
                ps.files = new ArrayList<String>();
                ps.typedMemo = "Recovered from Last time";
                
                if (!ps.typedDstPassword.isEmpty()) {
                    if (ps.typedPassword.isEmpty()) {
                        ps.typedPassword = ps.typedDstPassword;
                    }
                }

                showScreen();
            }
        }, "Continue");
         
        subInnerCore.add(bp); 
        subInnerCore.revalidate();
        subInnerCore.repaint();
    }
    
    public void showWarnFrackedToSend() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }
        
        subInnerCore = getModalJPanel("Fix Fracked Coin");
     
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
                
        int y = 0;
        // From Label
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>Your Address Coin for " + ps.srcWallet.getName() + 
                " is fracked. This means that one or more of the RAIDA think that it is not authentic. Please fix it before sending</div></html>");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        //ct.add(new JLabel("sss2"));
        ct.add(x);

        y++; 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Wallet w = sm.getFirstNonSkyWallet();
                if (w == null) {
                    ps.errText = "Address Coin can't be fixed bacuse you do not have any wallets";
                    showScreen();
                    return;
                }
             
                wl.debug(ltag, "Chosen wallet for fixing: " + w.getName());
                
                ps.coinIDinFix = ps.srcWallet;
                ps.srcWallet = w;
         
                int cnt = AppCore.getFilesCount(Config.DIR_FRACKED, ps.srcWallet.getName());
                if (cnt != 0) {
                    ps.errText = "Coin can't be fixed. Fracked forder of the wallet " + ps.srcWallet.getName() + " is not empty";
                    showScreen();
                    return;
                }
                
                String newFileName = AppCore.getUserDir(Config.DIR_FRACKED, ps.srcWallet.getName()) + File.separator + ps.coinIDinFix.getName() + ".stack";
                wl.debug(ltag, "New name: " + newFileName);
                
                if (!AppCore.saveFile(newFileName, ps.coinIDinFix.getIDCoin().getJson(false))) {
                    ps.errText = "Failed to move ID Coin. Please, check main.log file";
                    showScreen();
                    return;
                }
                
                AppCore.deleteFile(ps.coinIDinFix.getIDCoin().originalFile);
                //AppCore.moveToFolder(ps.coinIDinFix.getIDCoin().originalFile, Config.DIR_TRASH, ps.srcWallet.getName());
             
                wl.debug(ltag, "Set fixing");
                ps.currentScreen = ProgramState.SCREEN_FIXING_FRACKED;
                showScreen();
                
            }
        }, "Fix Coin");
         
        subInnerCore.add(bp); 
        subInnerCore.revalidate();
        subInnerCore.repaint();
    }
    
    public void showConfirmClearScreen() {
        JPanel subInnerCore = getModalJPanel("Confirmation");
     
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        // From Label
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>This is permanent and irreversible!</div></html>");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);

        String txt;
        
        if (ps.needBackup)
            txt = "Are you sure you want to delete Log files and Transaction history?";
        else 
            txt = "Are you sure you want to delete Log files and Transaction history without backup?";
        
        y++;
        // Q
        x = new JLabel("<html><div style='width:460px;text-align:center'><b>" + txt + "</b></div></html>");
        AppUI.setCommonBoldFont(x);
        c.insets = new Insets(42, 0, 4, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++; 
             
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                launchErasers();
            }
        });
         
        subInnerCore.add(bp);            
    }
    
    public void showConfirmDeleteWalletScreen() {
        JPanel subInnerCore = getModalJPanel("Confirmation");
        maybeShowError(subInnerCore);
     
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
   
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        // From Label
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>This is permanent and irreversible!</div></html>");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);

        y++;
        
        String txt;
        txt = "Are you sure you want to delete Wallet <b>" + ps.srcWallet.getName() + "</b> ?";
 
        final Wallet dstWallet = sm.getFirstNonSkyWallet();
        if (ps.srcWallet.isSkyWallet()) {
            txt += "<br><br>Your ID coin will be put back into your Bank (Wallet <b>" + dstWallet.getName() + "</b>). You will not be able to receive Coins at this address again";    
        }
           
        // Q
        x = new JLabel("<html><div style='width:460px;text-align:center'>" + txt + "</div></html>");
        AppUI.setCommonBoldFont(x);
        c.insets = new Insets(32, 0, 4, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++; 
              
        MyTextField tf0 = null;
        if (dstWallet.isEncrypted() && ps.srcWallet.isSkyWallet()) {
            // Password Label
            JLabel n = new JLabel("Password");
            AppUI.setCommonFont(n);
            c.anchor = GridBagConstraints.EAST;
            c.insets = new Insets(24, 32, 4, 0); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
            c.gridwidth = 1;
            gridbag.setConstraints(n, c);
            ct.add(n);

            tf0 = new MyTextField("Wallet Password");
            c.insets = new Insets(24, 10, 4, 0);
            c.gridx = GridBagConstraints.RELATIVE;
            c.anchor = GridBagConstraints.WEST;
            c.gridy = y;
            c.gridwidth = 1;
            gridbag.setConstraints(tf0.getTextField(), c);
            ct.add(tf0.getTextField());
            
            y++;
                
            c.gridwidth = 2;
        }
        
        final MyTextField mtf0 = tf0;
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ps.srcWallet.getTotal() != 0) {
                    ps.errText = "The Wallet is not empty";
                    showScreen();
                    return;
                }
                
                wl.debug(ltag, "Deleting Wallet " + ps.srcWallet.getName());
                              
                if (ps.srcWallet.isSkyWallet()) {
                    String defWalletName = dstWallet.getName();
                    String origName = ps.srcWallet.getIDCoin().originalFile;
                    String name = ps.srcWallet.getIDCoin().getFileName();
                     
                    wl.debug(ltag, "Moving id " + origName + " to " + defWalletName + " name");
                    
                    if (dstWallet.isEncrypted()) {
                        if (mtf0 == null)
                            return;
                        
                        if (mtf0.getText().isEmpty()) {
                            ps.errText = "Password is empty";
                            showScreen();
                            return;
                        }
                    
                        String wHash = dstWallet.getPasswordHash();
                        String providedHash = AppCore.getMD5(mtf0.getText());
                        if (wHash == null) {
                            ps.errText = "To Wallet is corrupted";
                            showScreen();
                            return;
                        }
                    
                        if (!wHash.equals(providedHash)) {
                            ps.errText = "Password is incorrect";
                            showScreen();
                            return;
                        } 
                    
                        dstWallet.setPassword(mtf0.getText());                        
                    }

                    if (!AppCore.moveToFolderNewName(origName, Config.DIR_BANK, defWalletName, name)) {
                        ps.errText = "Failed to delete Wallet";
                        showScreen();
                        return;
                    }

                    dstWallet.appendTransaction("Coin from Deleted Sky Wallet", ps.srcWallet.getIDCoin().getDenomination(), "skymove");
                                        
                    final String wname = ps.srcWallet.getSkyName();
                    String wdomain = ps.srcWallet.getDomain();
                    final DNSSn d = new DNSSn(wname, wdomain, wl);                  
                    Thread t = new Thread(new Runnable() {
                        public void run(){
                            if (!d.deleteRecord(wname, ps.srcWallet.getIDCoin(), sm.getSR())) {
                                wl.error(ltag, "Failed to delete coin. DNS error");
                                return;
                            } 
                        }
                    });     
                    
                    t.start();
               
                    if (dstWallet.isEncrypted()) {
                        wl.debug(ltag, "Start Vaulter");
                        
                        sm.changeServantUser("Vaulter", dstWallet.getName());
                        sm.startVaulterService(new CallbackInterface() {
                            public void callback(Object o) {
                                VaulterResult vr = (VaulterResult) o;
                                
                                wl.debug(ltag, "DeleteWallet Vaulter finished: " + vr.status);
                                if (vr.status != VaulterResult.STATUS_FINISHED) {
                                    EventQueue.invokeLater(new Runnable() {         
                                        public void run() {
                                            ps.errText = "Failed to encrypt ID coin";
                                            ps.currentScreen = ProgramState.SCREEN_DELETE_WALLET_DONE;
                                            showScreen();
                                        }
                                    });
                                
                                    return;
                                }

                                EventQueue.invokeLater(new Runnable() {         
                                    public void run() {
                                        sm.initWallets();                
                                        ps.currentScreen = ProgramState.SCREEN_DELETE_WALLET_DONE;
                                        showScreen();
                                    }
                                });
                            }
                        }, dstWallet.getPassword());
                        
                        return;
                    }
                    
                } else {
                    if (!AppCore.moveFolderToTrash(ps.srcWallet.getName())) {
                        ps.errText = "Failed to delete Wallet";
                        showScreen();
                        return;
                    }
                }
                
                sm.initWallets();                
                ps.currentScreen = ProgramState.SCREEN_DELETE_WALLET_DONE;
                showScreen();
            }
        });
  
        
        subInnerCore.add(bp); 
    }
    
    public void showConfirmTransferScreen() {
        JPanel subInnerCore = getModalJPanel("Transfer Confirmation");
     
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        // From Label
        JLabel x = new JLabel("From:   ");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        String name = ps.srcWallet.getName();
        //if (ps.srcWallet.isSkyWallet())
        //    name += "." + Config.DDNS_DOMAIN;
        
        x = new JLabel(name);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++;
        
        String to;
        if (ps.sendType == ProgramState.SEND_TYPE_WALLET) {
            name = ps.dstWallet.getName();
            //if (ps.dstWallet.isSkyWallet())
            //    name += "." + Config.DDNS_DOMAIN;
            
            to = name;
            
        } else if (ps.sendType == ProgramState.SEND_TYPE_REMOTE) {
            to = ps.typedRemoteWallet;
        } else if (ps.sendType == ProgramState.SEND_TYPE_FOLDER) {
            to = ps.chosenFile;

        } else {
            to = "?";
        }
        
        if (to.length() > 24) {
            to = to.substring(0, 24) + "...";
        }
        
        
        // To Label
        x = new JLabel("To:   ");
        AppUI.setCommonFont(x);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        x = new JLabel(to);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++;
        
        
        
        // Amount
        x = new JLabel("Amount:   ");
        AppUI.setCommonFont(x);
        c.insets = new Insets(32, 0, 4, 0);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        x = new JLabel(ps.typedAmount + " CC");
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++;
        
        String memo = ps.typedMemo;
        if (memo.length() > 24) {
            memo = memo.substring(0, 24) + "...";
        }
        
        // Memo
        x = new JLabel("Memo:   ");
        AppUI.setCommonFont(x);
        c.insets = new Insets(0, 0, 4, 0);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        x = new JLabel(memo);
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++; 
        
        
        // Q
        x = new JLabel("Do you wish to continue?");
        AppUI.setCommonFont(x);
        c.insets = new Insets(32, 0, 4, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        c.gridwidth = 2;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++; 
             
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.srcWallet.setPassword(ps.typedSrcPassword);
                
                sm.setActiveWalletObj(ps.srcWallet);
                if (ps.sendType == ProgramState.SEND_TYPE_FOLDER) {                
                    if (ps.srcWallet.isEncrypted()) {
                        sm.startSecureExporterService(Config.TYPE_STACK, ps.typedAmount, ps.typedMemo, ps.chosenFile, false, new ExporterCb());
                    } else {
                        sm.startExporterService(Config.TYPE_STACK, ps.typedAmount, ps.typedMemo, ps.chosenFile, false, new ExporterCb());
                    }
                    ps.currentScreen = ProgramState.SCREEN_EXPORTING;
                    ps.isSkyDeposit = false;
                    showScreen();
                } else if (ps.sendType == ProgramState.SEND_TYPE_WALLET) {
                    ps.dstWallet.setPassword(ps.typedDstPassword);              
                    ps.currentScreen = ProgramState.SCREEN_SENDING;
                    if (ps.srcWallet.isSkyWallet()) {
                        ps.isSkyDeposit = true;
                    } else {
                        ps.isSkyDeposit = false;
                    }
                    showScreen();
                } else if (ps.sendType == ProgramState.SEND_TYPE_REMOTE) {              
                    DNSSn d = new DNSSn(ps.typedRemoteWallet, null, wl);
                    int sn = d.getSN();
                    if (sn < 0) {
                        ps.errText = "Failed to query receiver. Check that the name is valid";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                        return;
                    }
                    
                    ps.foundSN = sn;
                    ps.isSkyDeposit = false;
                    ps.currentScreen = ProgramState.SCREEN_SENDING;
                    showScreen();
                }
            }
        });
  
        
        subInnerCore.add(bp);    
    }
    
    
    public void showSetEmailScreen() {
        JPanel subInnerCore = getModalJPanel("Type Coin Recovery Email");
        maybeShowError(subInnerCore);
        
        // Space
        //AppUI.hr(subInnerCore, 20);
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        int y = 0;
        
        // Text
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;


        JLabel l = new JLabel("<html><div style='width:460px; text-align:center'>The CloudCoin Consortium recommends you use<br>"
                + "a free encrypted email account from</div></html>");
        AppUI.setFont(l, 16);
        gridbag.setConstraints(l, c); 
        ct.add(l);
        
        
        y++;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        
        l = AppUI.getHyperLink("www.protonmail.com", "www.protonmail.com", 16);
        AppUI.setFont(l, 16);
        gridbag.setConstraints(l, c); 
        ct.add(l);
        
        
        y++;
         
        // Email Label
        JLabel x = new JLabel("Email");
        AppUI.setCommonFont(x);
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 120, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);

        y++;
        
        MyTextField tf0 = new MyTextField("Email", false);
        c.insets = new Insets(0, 120, 16, 0);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(tf0.getTextField(), c);
        tf0.requestFocus();
        ct.add(tf0.getTextField());
               
        y++;
        
        // Confirm Email Label
        x = new JLabel("Confirm Email");
        AppUI.setCommonFont(x);
        c.insets = new Insets(0, 120, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        
        y++;
        
        MyTextField tf1 = new MyTextField("Confirm Email", false);
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
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
                   
                p0 = p0.toLowerCase();
                p1 = p1.toLowerCase();
                
                if (!Validator.email(p0)) {
                    ps.errText = "Email is invalid";
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
            resetState();
            return;
        }

        subInnerCore = getModalJPanel("Wallet " + ps.typedWalletName + " created");
        AppUI.hr(subInnerCore, 42);
        
        JLabel res;
        if (!ps.typedPassword.equals("")) {
            res = AppUI.getCommonLabel("The coins in this wallet are ");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);  
            
            res = AppUI.getCommonLabel("protected from theft by password. ");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);  
      
            res = AppUI.getCommonLabel("Please record your password in a secure location.");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);  
            
            if (!ps.typedEmail.equals("")) {
                res = AppUI.getCommonLabel("Your coins in this wallet are protected from loss.");
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12);
                
                res = AppUI.getCommonLabel("Your loss recovery email is:");
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12);
                
                
                res = AppUI.getCommonBoldLabel(ps.typedEmail);
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12);               
            } else {
                res = AppUI.getCommonLabel("No recovery email was set.");
                subInnerCore.add(res);
                AppUI.hr(subInnerCore, 12); 
            }
        } else if (!ps.typedEmail.equals("")) {
            res = AppUI.getCommonLabel("Your coins in this wallet are protected from loss.");
            subInnerCore.add(res);
            AppUI.hr(subInnerCore, 12);
                
            res = AppUI.getCommonLabel("Your loss recovery email is:");
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
            
        //resetState();
        
        //JPanel bp = getOneButtonPanel();  
        JPanel bp = getOneButtonPanelCustom("Continue", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setActiveWallet(sm.getWalletByName(ps.typedWalletName));
                ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                showScreen();
            }
        });  
        subInnerCore.add(bp);      
    }
    
    public void showSkyWalletCreatedScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            resetState();
            return;
        }

        subInnerCore = getModalJPanel("Sky Wallet created");
        AppUI.hr(subInnerCore, 32);
        

        JLabel res = new JLabel("<html><div style='width:500px; text-align:center'>"
                + "You have now created a sky wallet on a third party trusted transfer server.<br>" 
                + "Your address is <b>" + ps.domain + "." + ps.trustedServer + "</b>.<br><br> "
                + "This sky wallet allows you to receive CloudCoins "
                + "that you know are authentic. <br>The sender will also know that "
                + "the coins they send are authentic.<br><br>  NOTE: The CloudCoin Consortium owns the "
                + "SkyWallet.cc domain name but it does not own the trusted transfer server "
                + "that the name is pointing to. The trusted transfer server that "
                + "SkyWallet.cc points to is called \"TeleportNow.\" TeleportNow has data supremacy "
                + "just like the RAIDA. Teleport now cannot be brought down or hacked "
                + "and there is no information about transactions that are stored.</div></html> ");
        
        AppUI.setFont(res, 18);
        AppUI.alignCenter(res);
        
        subInnerCore.add(res);

        JPanel bp = getOneButtonPanelCustom("Continue", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setActiveWallet(sm.getWalletByName(ps.domain + "." + ps.trustedServer));
                ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                showScreen();
            }
        });     
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
        MyCheckBox cb0 = new MyCheckBox("<html>I understand that if I lose my password<br>I will lose my coins and will need to recover them</html>");
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
        tf0.requestFocus();
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
        
        for (int i = 0; i < wallets.length; i++) {
            JLabel cntLabel = (JLabel) wallets[i].getuiRef();
            if (cntLabel == null)
                continue;
            
            if (wallets[i].isUpdated()) {
                walletSetTotal(wallets[i], wallets[i].getTotal());
                continue;
            }
            
            cntLabel.setText("Counting");
            cntLabel.invalidate();
            cntLabel.repaint();
        }
        
        setTotalCoins();

        sm.initIsolatedSec();
        for (int i = 0; i < wallets.length; i++) {
            final Wallet w = wallets[i];
            
            if (w.isUpdated())
                continue;
            
            String rpath = AppCore.getRootPath() + File.separator + w.getName();
            wl.debug(ltag, "Counting for " + w.getName());
            if (w.isSkyWallet()) {
                
                while (!ps.isEchoFinished) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                }
                
                ShowEnvelopeCoins sc = new ShowEnvelopeCoins(rpath, wl);
                sm.addSec(sc);
                int snID = w.getIDCoin().sn;
                sc.launch(snID, "", new CallbackInterface() {
                    public void callback(Object o) {
                        ShowEnvelopeCoinsResult scresult = (ShowEnvelopeCoinsResult) o;
            
                        if (scresult.status != ShowEnvelopeCoinsResult.STATUS_FINISHED)
                            return;

                        wl.debug(ltag, "ShowEnvelopeCoins done");
                        w.setSNs(scresult.coins);
                        w.setEnvelopes(scresult.envelopes);
                                
                        int[][] counters = scresult.counters;                        
                        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);

                        walletSetTotal(w, totalCnt);
                        w.setCounters(counters);
                        setTotalCoins();
                        
                        wl.debug(ltag, "ShowEnvelopeCoins return");
                    }
                });
            } else {             
                ShowCoins sc = new ShowCoins(rpath, wl);
                sc.launch(new CallbackInterface() {
                    public void callback(Object o) {
                        ShowCoinsResult scresult = (ShowCoinsResult) o;
                        w.setSNs(scresult.coins);
                                
                        int[][] counters = scresult.counters;                        
                        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);

                        walletSetTotal(w, totalCnt);
                        w.setCounters(counters);
                        setTotalCoins();
                    }
                });              
            }
        }     
    }
    
    public void eraserGoNext() {
        if (wallets.length > ps.currentWalletIdx) {
            if (!wallets[ps.currentWalletIdx].isSkyWallet()) {
                sm.changeServantUser("Eraser", wallets[ps.currentWalletIdx].getName());
                sm.startEraserService(new EraserCb(), ps.needBackup);
                ps.currentWalletIdx++;
            } else {
                ps.currentWalletIdx++;
                eraserGoNext();
            }
            
        } else {
            ps.currentScreen = ProgramState.SCREEN_CLEAR_DONE;
            sm.openTransactions();
            
            if (ps.needBackup) {
                if (!wl.copyMe()) {
                    ps.errText = "Failed to copy main logs";
                    showScreen();
                    return;
                } 
            }
            
            wl.killMe();
            AppCore.rmSentCoins();
            showScreen();
        }
    }
    
    public void launchErasers() {
        if (wallets.length  == 0) 
            return;
     
        ps.currentWalletIdx = 0;
        eraserGoNext();   
    }
    
    
    public void showDefaultScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();     
    }
    
    public String getNonEmptyFolderError(String folder) {
        return "<html><div style='font-size:10px; text-align:center'>" + folder + " Folder is not empty. Please remove coins from your " + folder + " Folder and try again. "
                + "Go to tools and then Show Folders to see the location of your " + folder + " Folder. Then cut your coins from the folder and put them in a place where you can find them. "
                + "Please click cancel below to reset. Then deposit them again.</div></html>";
    }
    
    
    
    public void showTransferScreen() {
        showLeftScreen();
        JPanel rightPanel = getRightPanel();    
    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Transfer");   
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
        
        
        // Transfer from
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        JLabel x = new JLabel("         Transfer From");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
 
        
        final optRv rvFrom = setOptionsForWalletsCommon(false, false, true, null);
        if (rvFrom.idxs.length == 0) {
            ps.errText = "No Wallets to Transfer From";
            maybeShowError(ct);
            return;
        }
        
        final optRv rvTo = setOptionsForWalletsAll(null);
        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", rvFrom.options);
        gridbag.setConstraints(cboxfrom.getComboBox(), c);
        oct.add(cboxfrom.getComboBox());
        
        y++;

        // Password From
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        final JLabel spText = new JLabel("Password From");
        gridbag.setConstraints(spText, c);
        AppUI.setCommonFont(spText);
        oct.add(spText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField passwordSrc = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(passwordSrc.getTextField(), c);
        oct.add(passwordSrc.getTextField());     
        y++;
        
        passwordSrc.getTextField().setVisible(false);
        spText.setVisible(false);
        
         // Transfer to
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        x = new JLabel("Transfer To");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     

        final RoundedCornerComboBox cboxto = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", rvTo.options);
        cboxto.addOption(AppUI.getRemoteUserOption());
        cboxto.addOption(AppUI.getLocalFolderOption());
        gridbag.setConstraints(cboxto.getComboBox(), c);
        oct.add(cboxto.getComboBox());   
        y++;
        
        
        // Password To
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        final JLabel dpText = new JLabel("Password To");
        gridbag.setConstraints(dpText, c);
        AppUI.setCommonFont(dpText);
        oct.add(dpText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField passwordDst = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(passwordDst.getTextField(), c);
        oct.add(passwordDst.getTextField());     
        y++;
        
        passwordDst.getTextField().setVisible(false);
        dpText.setVisible(false);

        
        
        
        // Remote User
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        final JLabel rwText = new JLabel("To Sky Wallet");
        gridbag.setConstraints(rwText, c);
        AppUI.setCommonFont(rwText);
        oct.add(rwText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField remoteWalledId = new MyTextField("JohnDoe.SkyWallet.cc", false);
        gridbag.setConstraints(remoteWalledId.getTextField(), c);
        if (!ps.typedRemoteWallet.isEmpty())
            remoteWalledId.setData("" + ps.typedRemoteWallet);
        oct.add(remoteWalledId.getTextField());     
        y++;
        
        remoteWalledId.getTextField().setVisible(false);
        rwText.setVisible(false);
        
        
        
        // Local folder
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        final JLabel lfText = new JLabel("Local folder");
        gridbag.setConstraints(lfText, c);
        AppUI.setCommonFont(lfText);
        oct.add(lfText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        
        if (ps.chosenFile.isEmpty())
            ps.chosenFile = Config.DEFAULT_EXPORT_DIR;

        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
     
        final MyTextField localFolder = new MyTextField("Select Folder", false, true);
        localFolder.setData(new File(ps.chosenFile).getName());
        localFolder.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!Config.DEFAULT_EXPORT_DIR.isEmpty())
                    chooser.setCurrentDirectory(new File(Config.DEFAULT_EXPORT_DIR));
                
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    localFolder.setData(chooser.getSelectedFile().getName());
                    Config.DEFAULT_EXPORT_DIR = ps.chosenFile;
                    AppCore.writeConfig();
                }
            }
        });
        gridbag.setConstraints(localFolder.getTextField(), c);
        oct.add(localFolder.getTextField());     
        y++;
        
        localFolder.getTextField().setVisible(false);
        lfText.setVisible(false);
        
        // Amount
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        x = new JLabel("Amount");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField amount = new MyTextField("0 CC", false);
        gridbag.setConstraints(amount.getTextField(), c);
        if (ps.typedAmount > 0)
            amount.setData("" + ps.typedAmount);
        oct.add(amount.getTextField());    
        y++;
                    
        // Memo
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        x = new JLabel("Memo (Note)");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField memo = new MyTextField("Optional", false);
        gridbag.setConstraints(memo.getTextField(), c);
        if (!ps.typedMemo.isEmpty())
            memo.setData(ps.typedMemo);
        oct.add(memo.getTextField());   
        y++;

        rightPanel.add(oct);

        cboxfrom.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int srcIdx = cboxfrom.getSelectedIndex() - 1;
                if (srcIdx < 0 || srcIdx >= wallets.length) 
                    return;
                
                srcIdx = rvFrom.idxs[srcIdx];
                Wallet srcWallet = wallets[srcIdx];
                if (srcWallet == null)
                    return;

                if (srcWallet.isEncrypted()) {                 
                    passwordSrc.getTextField().setVisible(true);
                    spText.setVisible(true);
                } else {
                    passwordSrc.getTextField().setVisible(false);
                    spText.setVisible(false);
                }

                        
                String value = cboxto.getSelectedValue();
                optRv lrvTo = setOptionsForWalletsAll(srcWallet.getName());
                rvTo.idxs = lrvTo.idxs;
                rvTo.options = lrvTo.options;
                cboxto.setOptions(rvTo.options);
                cboxto.addOption(AppUI.getRemoteUserOption());
                cboxto.addOption(AppUI.getLocalFolderOption());
                cboxto.setDefault(value);              
            }
        });
        
        cboxto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int dstIdx = cboxto.getSelectedIndex() - 1;
                
                localFolder.getTextField().setVisible(false);
                lfText.setVisible(false);
          
                // Remote Wallet
                if (dstIdx == rvTo.idxs.length) {
                    remoteWalledId.getTextField().setVisible(true);
                    rwText.setVisible(true);
                    
                    passwordDst.getTextField().setVisible(false);
                    dpText.setVisible(false);
                    
                    localFolder.getTextField().setVisible(false);
                    lfText.setVisible(false);
                    
                    memo.setPlaceholder("From John Doe for May rent");
                    
                    return;
                } else {
                    memo.setPlaceholder("Optional");
                } 
                
                remoteWalledId.getTextField().setVisible(false);
                rwText.setVisible(false);
                  
                // Local
                if (dstIdx == rvTo.idxs.length + 1) {    
                    passwordDst.getTextField().setVisible(false);
                    dpText.setVisible(false);
                    
                    localFolder.getTextField().setVisible(true);
                    lfText.setVisible(true);
                    
                    return;
                }
   
                if (dstIdx < 0 || dstIdx >= rvTo.idxs.length) 
                    return;
                           
                
                dstIdx = rvTo.idxs[dstIdx];             
                Wallet dstWallet = wallets[dstIdx];             
                if (dstWallet == null)
                    return;
                
                if (dstWallet.isSkyWallet()) {
                    memo.setPlaceholder("From John Doe for May rent");
                } else {
                    memo.setPlaceholder("Optional");
                }
                
                if (dstWallet.isEncrypted()) {  
                    passwordDst.getTextField().setVisible(true);
                    dpText.setVisible(true);
                } else {
                    passwordDst.getTextField().setVisible(false);
                    dpText.setVisible(false);
                }
            }
        });

        
        if (ps.srcWallet != null && ps.selectedFromIdx > 0) {
            cboxfrom.setDefaultIdx(ps.selectedFromIdx);
        }
        
        if (ps.selectedToIdx > 0) {
            cboxto.setDefaultIdx(ps.selectedToIdx);
        }
        
        // Space
        AppUI.hr(oct, 22);
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                ps.typedMemo = memo.getText().trim();
              
                int srcIdx = cboxfrom.getSelectedIndex() - 1;
                int dstIdx = cboxto.getSelectedIndex() - 1;
                
                ps.selectedFromIdx = cboxfrom.getSelectedIndex();
                ps.selectedToIdx =  cboxto.getSelectedIndex();
                ps.typedRemoteWallet = remoteWalledId.getText();
       
                if (srcIdx < 0 || srcIdx >= rvFrom.idxs.length) {                    
                    ps.errText = "Please select From Wallet";
                    showScreen();
                    return;
                }
                
                srcIdx = rvFrom.idxs[srcIdx];                  
                Wallet srcWallet = wallets[srcIdx];
                ps.srcWallet = srcWallet;
            
                if (dstIdx < 0 || dstIdx >= rvTo.idxs.length + 2) {             
                    ps.errText = "Please select To Wallet";
                    showScreen();
                    return;
                }
       
                try {
                    ps.typedAmount = Integer.parseInt(amount.getText());
                } catch (NumberFormatException ex) {
                    ps.errText = "Invalid amount";
                    showScreen();
                    return;   
                };
                
                if (ps.typedAmount <= 0) {
                    ps.errText = "Transfer must be higher than zero";
                    showScreen();
                    return;  
                }
                
                if (!Validator.memoLength(ps.typedMemo)) {
                    ps.errText = "Memo too long. Only 64 characters are allowed";
                    showScreen();
                    return;
                }
                
                ps.typedMemo = ps.typedMemo.trim();
                if (!ps.typedMemo.isEmpty()) {
                    if (!Validator.memo(ps.typedMemo)) {
                        ps.errText = "Memo: special characters not allowed! Use numbers and letters only";
                        showScreen();
                        return;
                    }
                }

                if (srcWallet.isEncrypted()) {
                    if (passwordSrc.getText().isEmpty()) {
                        ps.errText = "From Password is empty";
                        showScreen();
                        return;
                    }
                    
                    String wHash = srcWallet.getPasswordHash();
                    String providedHash = AppCore.getMD5(passwordSrc.getText());
                    if (wHash == null) {
                        ps.errText = "From Wallet is corrupted";
                        showScreen();
                        return;
                    }
                    
                    if (!wHash.equals(providedHash)) {
                        ps.errText = "From Password is incorrect";
                        showScreen();
                        return;
                    } 
                    
                    ps.typedSrcPassword = passwordSrc.getText();
                }
                
                if (ps.typedAmount > srcWallet.getTotal()) {
                    ps.errText = "Insufficient funds";
                    showScreen();
                    return;
                }
                      
                              
                if (dstIdx == rvTo.idxs.length) {
                    // Remote User
                    if (remoteWalledId.getText().isEmpty()) {
                        ps.errText = "Remote Wallet is empty";
                        showScreen();
                        return;
                    }
                    
                    if (ps.typedMemo.isEmpty()) {
                        ps.errText = "Memo cannot be empty";
                        showScreen();
                        return; 
                    }
                    
                    String dstName = remoteWalledId.getText().trim();
                    if (ps.srcWallet.isSkyWallet() && ps.srcWallet.getName().equals(dstName)) {
                        ps.errText = "Wallets cannot be the same";
                        showScreen();
                        return;
                    }
                    
                    ps.dstWallet = null;
                    ps.typedRemoteWallet = dstName;
                    ps.sendType = ProgramState.SEND_TYPE_REMOTE;
                    
                    DNSSn d = new DNSSn(ps.typedRemoteWallet, null, wl);
                    if (!d.recordExists()) {
                        ps.errText = "<html><div style='width:660px; text-align:center'>Sky Wallet " + ps.typedRemoteWallet + " doesn't exist. If it is a newly created wallet please wait and try again later</div></html>";
                        showScreen();
                        return;
                    }         
                } else if (dstIdx == rvTo.idxs.length + 1) {
                    if (ps.typedMemo.isEmpty()) {
                        ps.errText = "Memo cannot be empty";
                        showScreen();
                        return;
                    }

                    if (!Validator.memo(ps.typedMemo)) {
                        ps.errText = "Memo: special characters not allowed! Use numbers and letters only";
                        showScreen();
                        return;
                    }
                    
                    ps.typedMemo = ps.typedMemo.trim();
                    
                    // Local folder
                    if (ps.chosenFile.isEmpty()) {
                        ps.errText = "Folder is not chosen";
                        showScreen();
                        return;
                    }
                    
                    if (ps.srcWallet.isSkyWallet()) {
                        ps.errText = "Transfer from Sky Wallet to Local Folder is not supported";
                        showScreen();
                        return;
                    }
                    
                    if (ps.typedMemo.isEmpty())
                        ps.typedMemo = "Export";
                    
                    String filename = ps.chosenFile + File.separator + ps.typedAmount + ".CloudCoin." + ps.typedMemo + ".stack";
                    File f = new File(filename);
                    if (f.exists()) {
                        ps.errText = "<html><div style='width:660px;text-align:center'>File with the same Memo already exists in " 
                                + ps.chosenFile + " folder. Use different Memo or change folder for your transfer</div></html>";
                        showScreen();
                        return;
                    }
                    
                    ps.sendType = ProgramState.SEND_TYPE_FOLDER;    
                    
                    setActiveWallet(ps.srcWallet);
                    ps.currentScreen = ProgramState.SCREEN_CONFIRM_TRANSFER;
                    showScreen();
                    
                    return;                    
                } else {
                    dstIdx = rvTo.idxs[dstIdx];
                    if (srcIdx == dstIdx) {
                        ps.errText = "You can not transfer to the same wallet";
                        showScreen();
                        return;
                    }
                                       
                    // Wallet
                    Wallet dstWallet = wallets[dstIdx];
                    if (dstWallet.isEncrypted()) {
                        if (passwordDst.getText().isEmpty()) {
                            ps.errText = "To Password is empty";
                            showScreen();
                            return;
                        }
                    
                        String wHash = dstWallet.getPasswordHash();
                        String providedHash = AppCore.getMD5(passwordDst.getText());
                        if (wHash == null) {
                            ps.errText = "To Wallet is corrupted";
                            showScreen();
                            return;
                        }
                    
                        if (!wHash.equals(providedHash)) {
                            ps.errText = "To Password is incorrect";
                            showScreen();
                            return;
                        } 
                        
                        ps.typedDstPassword = passwordDst.getText();
                    }
   
                    /*
                    if (srcWallet.isSkyWallet() && dstWallet.isSkyWallet()) {
                        ps.errText = "Transfer from Sky Wallet to Sky Wallet is not supported";
                        showScreen();
                        return;
                    }*/
                    
                    if (dstWallet.isSkyWallet()) {
                        if (ps.typedMemo.isEmpty()) {
                            ps.errText = "Memo must not be empty";
                            showScreen();
                            return;
                        }          
                    }
                    
                    
                    ps.dstWallet = dstWallet;
                    ps.sendType = ProgramState.SEND_TYPE_WALLET;
                }
                
                if (ps.typedMemo.isEmpty())
                    ps.typedMemo = "Transfer";
                
                ps.currentScreen = ProgramState.SCREEN_CONFIRM_TRANSFER;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);   
    }
    
    public void showPredepositScreen() {
               
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
       
        
        JLabel tw = new JLabel("<html><div style='width:690px; text-align:center'>Deposit from Sky Wallet will deposit all coins from that Sky wallet! \n" +
"<br>You will be not able to select just part of the Sky Wallet coins.</div></html>");
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.NORTH; 
        c.gridwidth = 2;
        gridbag.setConstraints(tw, c);
        AppUI.setFont(tw, 16);
        AppUI.alignCenter(tw);
        AppUI.setMargin(tw, 18);
        rightPanel.add(tw);
        
        y++;
        
        // Deposit From
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        c.gridwidth = 1;
        JLabel x = new JLabel("Deposit From");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
 
        int cnt = 0, scnt = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (wallets[i].isSkyWallet()) {
                scnt++;
            }
            if (wallets[i].isSkyWallet() && wallets[i].getTotal() > 0)
                cnt++;
        }
           
        final String[] options = new String[cnt + 1];
        final String[] doptions = new String[wallets.length - scnt];
        int j = 0, k = 0;
        
        final int fidxs[], tidxs[];
        
        fidxs = new int[cnt + 1];
        tidxs = new int[wallets.length - scnt];
        for (int i = 0; i < wallets.length; i++) {
            if (wallets[i].isSkyWallet()) {
                if (wallets[i].getTotal() > 0) {
                    options[j] = wallets[i].getName() + " - " + wallets[i].getTotal() + " CC";
                    fidxs[j] = i;
                    j++;
                }
            } else {
                doptions[k] = wallets[i].getName() + " - " + wallets[i].getTotal() + " CC";
                tidxs[k] = i;
                k++;
            }
        }
        
        if (j == 0) {
            ps.isSkyDeposit = false;
            ps.currentScreen = ProgramState.SCREEN_DEPOSIT;
            showScreen();
            return;
        } 
        
        options[j++] = "- FileSystem";
        //options[j++] = "- Add Sky Wallet";
      
        final RoundedCornerComboBox cbox = new RoundedCornerComboBox(AppUI.getColor2(), "Select Source", options);
        gridbag.setConstraints(cbox.getComboBox(), c);
        oct.add(cbox.getComboBox());

        
        y++;
        
        rightPanel.add(oct);
        
        // Deposit To
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        final JLabel dto = new JLabel("Deposit To");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);
         
        //final optRv rv = setOptionsForWallets(false, false);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        final RoundedCornerComboBox cboxto = new RoundedCornerComboBox(AppUI.getColor2(), "Select Destination", doptions);
        gridbag.setConstraints(cboxto.getComboBox(), c);
        oct.add(cboxto.getComboBox());

        if (doptions.length == 1) 
            cboxto.setDefaultIdx(1);
               
        cboxto.getComboBox().setVisible(false);
        dto.setVisible(false);
        
        y++;
        
        // Password
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST;   
        final JLabel pText = new JLabel("Password");
        gridbag.setConstraints(pText, c);
        AppUI.setCommonFont(pText);
        oct.add(pText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;   
        final MyTextField password = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(password.getTextField(), c);
        oct.add(password.getTextField());
        
        password.getTextField().setVisible(false);
        pText.setVisible(false);
        
        y++;
         
        cbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cidx = cbox.getSelectedIndex();
                if (cidx > options.length - 1) {
                    cboxto.getComboBox().setVisible(false);
                    dto.setVisible(false);
                    
                    password.getTextField().setVisible(false);
                    pText.setVisible(false);
                } else {
                    cboxto.getComboBox().setVisible(true);
                    dto.setVisible(true);
                    
                    int cidxn = cboxto.getSelectedIndex();
                    if (cidxn != 0) {
                        cidxn = tidxs[cidxn - 1];
                        Wallet w = wallets[cidxn];
                        if (w.isEncrypted()) {
                            password.getTextField().setVisible(true);
                            pText.setVisible(true);
                        } else {
                            password.getTextField().setVisible(false);
                            pText.setVisible(false);
                        }
                    }
                }
            }
        });
        
        cboxto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cidx = cboxto.getSelectedIndex();
                
                if (!dto.isVisible())
                    return;
                
                cidx = tidxs[cidx - 1];
                Wallet w = wallets[cidx];
                if (w.isEncrypted()) {
                    password.getTextField().setVisible(true);
                    pText.setVisible(true);
                } else {
                    password.getTextField().setVisible(false);
                    pText.setVisible(false);
                }
            }
        });
       
        if (ps.selectedFromIdx > 0)
            cbox.setDefaultIdx(ps.selectedFromIdx);
        
        if (ps.selectedToIdx > 0)
            cboxto.setDefaultIdx(ps.selectedToIdx);
        // Space
        AppUI.hr(oct, 22);
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cidx = cbox.getSelectedIndex();
                if (cidx == options.length) {
                    ps.isSkyDeposit = false;
                    ps.currentScreen = ProgramState.SCREEN_DEPOSIT;
                    showScreen();
                    return;
                }

                if (cidx == 0) {
                    ps.errText = "Wallet is not selected";
                    showScreen();
                    return;
                }
                
                ps.selectedFromIdx = cidx;
                cidx = fidxs[cidx - 1];
                Wallet w = wallets[cidx];
                ps.srcWallet = w;
                
                cidx = cboxto.getSelectedIndex();
                if (cidx == 0) {
                    ps.errText = "Destination Wallet is not selected";
                    showScreen();
                    return;
                }
                
                ps.selectedToIdx = cidx;
                cidx = tidxs[cidx - 1];
                
                w = wallets[cidx];
                if (w == null) {
                    ps.errText = "Destination Wallet is not selected";
                    showScreen();
                    return;
                }
                
                ps.dstWallet = w;

                int total;
                total = ps.srcWallet.getTotal();
                if (total == 0) {
                    ps.errText = ps.srcWallet.getName() + " is empty";
                    showScreen();
                    return;
                }
                
                if (ps.dstWallet.isEncrypted()) {               
                    if (password.getText().isEmpty()) {
                        ps.errText = "From Password is empty";
                        showScreen();
                        return;
                    }
                    
                    String wHash = ps.dstWallet.getPasswordHash();
                    String providedHash = AppCore.getMD5(password.getText());
                    if (wHash == null) {
                        ps.errText = "From Wallet is corrupted";
                        showScreen();
                        return;
                    }
                    
                    if (!wHash.equals(providedHash)) {
                        ps.errText = "From Password is incorrect";
                        showScreen();
                        return;
                    } 
                    
                    ps.typedDstPassword = password.getText();
                    ps.dstWallet.setPassword(ps.typedDstPassword);
                }
                
                                
                int cnt = AppCore.staleFiles(w.getName());
                if (cnt > 0) {
                    wl.debug(ltag, "Stale files: " + cnt);
                    ps.currentScreen = ProgramState.SCREEN_DEPOSIT_LEFTOVER;
                    showScreen();
                    return;
                }
                
                
                ps.typedAmount = total;
                ps.isSkyDeposit = true;
                ps.sendType = ProgramState.SEND_TYPE_WALLET;
                ps.currentScreen = ProgramState.SCREEN_SENDING;
                
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);    
        
    }
    
    public void showDepositScreen() {
        boolean isError = !ps.errText.equals("");
        
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
        
        AppUI.hr(ct, 2);
        
        if (sm.getFirstNonSkyWallet() == null) {
            ps.errText = "You have no Local Wallets";
            maybeShowError(ct);
            return;
        }
        
        
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
        c.anchor = GridBagConstraints.EAST; 
        JLabel x = new JLabel("Deposit To");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
 
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
      
        final RoundedCornerComboBox cbox = new RoundedCornerComboBox(AppUI.getColor2(), "Select Destination", options);
        gridbag.setConstraints(cbox.getComboBox(), c);
        oct.add(cbox.getComboBox());


        y++;
        // Password
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST;   
        final JLabel pText = new JLabel("Password");
        gridbag.setConstraints(pText, c);
        AppUI.setCommonFont(pText);
        oct.add(pText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;   
        final MyTextField password = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(password.getTextField(), c);
        oct.add(password.getTextField());
        
        password.getTextField().setVisible(false);
        pText.setVisible(false);
        
        y++;
        // Memo
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        x = new JLabel("Memo (Note)");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;   
        final MyTextField memo = new MyTextField("Optional", false);
        gridbag.setConstraints(memo.getTextField(), c);

        if (!ps.typedMemo.isEmpty())
            memo.setData(ps.typedMemo);
        
        oct.add(memo.getTextField());
        
        

        // Total files selected
        String totalCloudCoins = AppCore.calcCoinsFromFilenames(ps.files);
        final JLabel tl = new JLabel("Selected " + ps.files.size() + " files - " + totalCloudCoins + " CloudCoins");
        AppUI.setCommonFont(tl);
        c.insets = new Insets(22, 18, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = y + 3;  
        gridbag.setConstraints(tl, c);
        oct.add(tl);

        cbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String walletName = cbox.getSelectedValue();             
                Wallet w = sm.getWalletByName(walletName);
                if (w == null)
                    return;
                
                if (w.isEncrypted()) {          
                    password.getTextField().setVisible(true);
                    pText.setVisible(true);
                } else {
                    password.getTextField().setVisible(false);
                    pText.setVisible(false);
                }
            } 
        });
        
        if (ps.dstWallet != null) {
            cbox.setDefault(ps.dstWallet.getName());
        }
        
        
        int ddWidth = 701;
        
        // Drag and Drop
        JPanel ddPanel = new JPanel();
       // AppUI.setBackground(ddPanel, AppUI.getColor0());
        ddPanel.setLayout(new GridBagLayout());
        
        JLabel l = new JLabel("<html><div style='text-align:center; width:" + ddWidth  +"'><b>Drop files here or click<br>to select files</b></div></html>");
        AppUI.setColor(l, AppUI.getColor13());
        AppUI.setBoldFont(l, 40);
        AppUI.noOpaque(ddPanel);
        AppUI.setHandCursor(ddPanel);
        ddPanel.setBorder(new DashedBorder(40, AppUI.getColor13()));
        ddPanel.add(l);
        
        c.insets = new Insets(8, 18, 0, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.gridy = y + 4;  
        
        AppUI.setSize(ddPanel, (int) ddWidth, 150);
        gridbag.setConstraints(ddPanel, c);
        new FileDrop(null, ddPanel, new FileDrop.Listener() {
            public void filesDropped( java.io.File[] files ) {   
                for( int i = 0; i < files.length; i++ ) {
                    if (!AppCore.hasCoinExtension(files[i])) {
                        ps.errText = "File must have .jpeg or .stack extension";
                        maybeShowError(ct);
                        return;
                    }
                    ps.files.add(files[i].getAbsolutePath());
                }
                
                
                String totalCloudCoins = AppCore.calcCoinsFromFilenames(ps.files);
                String text = "Selected " + ps.files.size() + " files - " + totalCloudCoins + " CloudCoins";
                              
                tl.setText(text);            
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
                if (!Config.DEFAULT_DEPOSIT_DIR.isEmpty())
                    chooser.setCurrentDirectory(new File(Config.DEFAULT_DEPOSIT_DIR));

                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = chooser.getSelectedFiles();
                    for (int i = 0; i < files.length; i++) {
                        ps.files.add(files[i].getAbsolutePath());
                    }
                    
                    String totalCloudCoins = AppCore.calcCoinsFromFilenames(ps.files);
                    String text = "Selected " + ps.files.size() + " files - " + totalCloudCoins + " CloudCoins";
                              
                    tl.setText(text);      
                    
                    Config.DEFAULT_DEPOSIT_DIR = chooser.getCurrentDirectory().getAbsolutePath();
                    AppCore.writeConfig();

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
                
                ps.typedMemo = memo.getText().trim();
                ps.typedPassword = password.getText();
                
                w = sm.getWalletByName(walletName);
                if (w == null) {
                    ps.errText = "Wallet is not selected";
                    showScreen();
                    return;
                }
                
                ps.dstWallet = w;                        
                if (ps.files.size() == 0) {
                    ps.errText = "No files selected";
                    showScreen();
                    return;
                }

                if (w.isSkyWallet()) {
                    ps.errText = "The program cannot deposit to Sky Wallets. Use Transfer Screen";
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
                
                
                if (!w.isSkyWallet()) {
                    int cnt = AppCore.staleFiles(w.getName());
                    
                    wl.debug(ltag, "Stale files: " + cnt);
                    if (cnt > 0) {
                        ps.currentScreen = ProgramState.SCREEN_DEPOSIT_LEFTOVER;
                        showScreen();
                        return;
                    }       
                }
                
                ps.currentScreen = ProgramState.SCREEN_IMPORTING;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);       
    }
    
    public void showFoldersScreen() {
        showLeftScreen();
 
        Wallet w = sm.getActiveWallet();     

        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Show Folders");   
        ct.add(ltitle);
       // AppUI.hr(ct, 20);
            
        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;

        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 24, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.gridwidth = 2;


        JLabel sl = new JLabel("Folders:");
        AppUI.setCommonFont(sl);
        gridbag.setConstraints(sl, c); 
        gct.add(sl);
        y++;
            
        c.gridwidth = 1;
        for (int i = 0; i < wallets.length; i++) {
            if (wallets[i].isSkyWallet()) 
                continue;
            
            sl = new JLabel(wallets[i].getName());
            AppUI.setBoldFont(sl, 18);
            c.insets = new Insets(0, 0, 4, 0); 
            c.anchor = GridBagConstraints.EAST;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
        //    c.weightx = 2;
            gridbag.setConstraints(sl, c); 
            gct.add(sl);
            
            sl = new JLabel(AppCore.getRootUserDir(wallets[i].getName()));
            AppUI.setFont(sl, 18);
            
            final String fdir = AppCore.getRootUserDir(wallets[i].getName());
            sl = AppUI.getHyperLink(fdir, "javascript:void(0); return false", 20);
            sl.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (!Desktop.isDesktopSupported())
                        return;
                    try {
                        Desktop.getDesktop().open(new File(fdir));
                    } catch (IOException ie) {
                        wl.error(ltag, "Failed to open browser: " + ie.getMessage());
                    }
                }
            });

            c.insets = new Insets(0, 10, 4, 0); 
            c.anchor = GridBagConstraints.WEST;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;
        //    c.weightx = 3;
            gridbag.setConstraints(sl, c); 
            gct.add(sl);
            
            y++;
        }
            
        ct.add(gct);
    }
    
    public void showSupportScreen() {
        showLeftScreen();
 
        Wallet w = sm.getActiveWallet();     

        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Help & Support");   
        ct.add(ltitle);
       // AppUI.hr(ct, 20);
            
        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(10, 0, 14, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        JLabel vl = new JLabel("Version: " + this.version);
        AppUI.setFont(vl, 16);
        gridbag.setConstraints(vl, c); 
        gct.add(vl);
        y++;
               
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;

        /*
        int tsw = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (wallets[i].isSkyWallet()) {
                tsw++;
            }
        }
         
        int topMargin = 0;
        if (tsw != 0) {
            JLabel sl = new JLabel("Your Sky Wallets:");
            AppUI.setFont(sl, 22);
            gridbag.setConstraints(sl, c); 
            gct.add(sl);
            y++;
            
            for (int i = 0; i < wallets.length; i++) {
                if (wallets[i].isSkyWallet()) {
                    sl = new JLabel(wallets[i].getName());
                    AppUI.setFont(sl, 18);
                    c.insets = new Insets(0, 0, 4, 0); 
                    c.gridx = GridBagConstraints.RELATIVE;
                    c.gridy = y;
                    gridbag.setConstraints(sl, c); 
                    gct.add(sl);
                    y++;
                }
            }
            
            topMargin = 26;
        }       
        */
        
        int topMargin = 26;
        
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(topMargin, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;

        String urlName = "http://cloudcoinconsortium.com/use.html";
        JLabel l = AppUI.getHyperLink(urlName, urlName, 0);
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        
        l = new JLabel("<html><div style='width:460px; text-align:center'><br>"
                + "Support: 9 AM to 3 AM California Time (PST)<br> "
                + "Tel: +1(530)762-1361 <br>"
                + "Email: Support@cloudcoinmail.com</div></html>");
        c.insets = new Insets(0, 0, 0, 0); 
        AppUI.alignCenter(l);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c);
        gct.add(l);
        
        y++;
        
        l = new JLabel("<html><div style='width:480px; text-align:center; font-size: 14px'>"
                + "(Secure if you get a free encrypted email account at ProtonMail.com)</div></html>");
      
        AppUI.setMargin(l, 0);
        AppUI.setFont(l, 12);
        AppUI.alignCenter(l);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c);
        gct.add(l);

        y++;
        
        // Get proton
        l = AppUI.getHyperLink("Get Protonmail", "https://www.protonmail.com", 14);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        
        // Get proton
        l = AppUI.getHyperLink("Instructions, Terms and Conditions", "javascript:void(0); return false", 20);
        l.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                final JDialog f = new JDialog(mainFrame, "Instructions, Terms and Conditions", true);
                f.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
                AppUI.noOpaque((JComponent) f.getContentPane());
                AppUI.setSize(f, (int) (tw / 1.2), (int) (th / 1.2)); 

                JTextPane tp = new JTextPane();
                
                
                AppUI.setFont(tp, 12);
                String fontfamily = tp.getFont().getFamily();

                tp.setContentType("text/html"); 
                tp.setText("<html><div style=' font-family:"+fontfamily+"; font-size: 12px'>" + AppUI.getAgreementText() + "</div></html>"); 
                tp.setEditable(false); 
                tp.setBackground(null);
                tp.setCaretPosition(0);

                JScrollPane scrollPane = new JScrollPane(tp);

                f.add(scrollPane);
                f.pack();
                f.setLocationRelativeTo(mainFrame);
                f.setVisible(true);      
                
                scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMinimum());
                scrollPane.getViewport().setViewPosition(new java.awt.Point(0, 100));
                scrollPane.repaint();
            }
        });
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.insets = new Insets(40, 0, 4, 0); 
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        
        // Support Portal
        vl = new JLabel("Support Portal");
        AppUI.setCommonFont(vl);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.insets = new Insets(40, 0, 4, 0); 
        gridbag.setConstraints(vl, c); 
        gct.add(vl);
        y++;
        
        
        urlName = "https://cloudcoinsupport.atlassian.net/servicedesk/customer/portals";
        l = AppUI.getHyperLink(urlName, urlName, 14);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        c.insets = new Insets(4, 0, 4, 0); 
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        
        
    
        
        
        
        
        
        
        
        
        
        
        
        
        ct.add(gct);
        
        
        
        //rightPanel
        
        
        
    }
    
    public void showClearScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Clear History");   
        ct.add(ltitle);
        AppUI.hr(ct, 12);
        
        maybeShowError(ct);
        
        JLabel ntext = new JLabel("Clearing history will ensure your privacy if somebody gains access to your computer");
        AppUI.setFont(ntext, 15);
        AppUI.alignCenter(ntext);
        ct.add(ntext);
        
        // Space
        AppUI.hr(ct, 34);
        
        ntext = new JLabel("WARNING!");
        AppUI.setBoldFont(ntext, 21);
        AppUI.alignCenter(ntext);
        ct.add(ntext);
        
        // Space
        AppUI.hr(ct, 4);
        
        ntext = new JLabel("Clearing the History is a permanent and irreversible process");
        AppUI.setBoldFont(ntext, 21);
        AppUI.alignCenter(ntext);
        ct.add(ntext);
        
          // Space
        AppUI.hr(ct, 28);
        
        ntext = new JLabel("Do not proceed unless these files will never be needed again.");
        AppUI.setFont(ntext, 20);
        AppUI.alignCenter(ntext);
        ct.add(ntext);
        
        ntext = new JLabel("Otherwise, copy the history and store it elsewhere before deleting.");
        AppUI.setFont(ntext, 20);
        AppUI.alignCenter(ntext);
        ct.add(ntext);
        
        // Space
        AppUI.hr(ct, 20);
        
        ntext = new JLabel("No coins will be deleted, only transaction history and log files.");            
        AppUI.setFont(ntext, 20);
        AppUI.alignCenter(ntext);
        ct.add(ntext);
        
        
        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        
        // Checkbox
        MyCheckBox cb0 = new MyCheckBox("I have read and understand the Warning");
        cb0.setBoldFontSize(21);
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(cb0.getCheckBox(), c);       
        gct.add(cb0.getCheckBox());
        
        y++;
        final MyCheckBox cb1 = new MyCheckBox("Create Backup of History and Log files");
        cb1.setBoldFontSize(21);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        gridbag.setConstraints(cb1.getCheckBox(), c);       
        gct.add(cb1.getCheckBox());

           
        ct.add(gct);
  
        // Space
        AppUI.hr(ct, 20);

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    

                ps.needBackup = cb1.isChecked();
                ps.currentScreen = ProgramState.SCREEN_CONFIRM_CLEAR;
                showScreen();
            }
        });
        
        
        continueButton.disable();
        
        cb0.addListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object o = e.getSource();
                
                if (o instanceof JCheckBox) {
                    JCheckBox cb = (JCheckBox) o;
                    if (cb.isSelected()) {
                        continueButton.enable();
                    } else {
                        continueButton.disable();
                    }  
                }
            }
        });
       
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp); 
    }
    
    public void showSentCoins() {
        showLeftScreen();

        JPanel rightPanel = getRightPanel(AppUI.getColor4());    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
              
        JLabel ltitle = AppUI.getTitle("Sent Coins");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        // Scrollbar & Table  
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            JLabel lbl;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                lbl = (JLabel) this;
                if (row % 2 == 0) {
                    AppUI.setBackground(lbl, AppUI.getColor3());
                } else {
                    AppUI.setBackground(lbl, AppUI.getColor4());
                }

                if (column == 0) {             
                    lbl.setHorizontalAlignment(JLabel.LEFT);
                } else if (column == 4) {
                    String d = (String) value;
                    try {
                        String total = AppCore.formatNumber(Integer.parseInt(d));
                        lbl.setText(total);
                    } catch (NumberFormatException e) {
                        
                    }
                  
                    lbl.setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(JLabel.CENTER);
                } 
                
                //lbl.setHorizontalAlignment(JLabel.CENTER);
                AppUI.setMargin(lbl, 8);
  
                return lbl;
            }
        };
              
        String[][] serials = AppCore.getSentCoins();
        if (serials == null) {
            return;
        }

        //serial number table
        final JTable table = new JTable();
        final JScrollPane scrollPane = AppUI.setupTable(table, new String[] {"Date", "From", "To", "SN", "Amount", "Memo"}, serials, r);
        AppUI.setSize(scrollPane, 830, 325);
 
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(10);
        table.getColumnModel().getColumn(4).setPreferredWidth(10);
        
        ct.add(scrollPane);
        JPanel bp = getOneButtonPanelCustom("Print", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    try {
                        table.print();
                    } catch (PrinterException pe) {
                        System.out.println("Failed to print");
                    }
                }
            }
        );
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);  
    }
    
    public void showListSerialsScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("List Serials");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        maybeShowError(ct);
        
        final optRv rv = setOptionsForWallets(false, false);
        if (rv.idxs.length == 0) {
            JLabel nx = new JLabel("You have no coins to list serial numbers");
            AppUI.setSemiBoldFont(nx, 20);
            AppUI.alignCenter(nx);
            ct.add(nx);
            return;
        }
        
        

        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        // Text
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 80, 0); 
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;

        JLabel l = new JLabel("List Serials will show you Serial Numbers of your CloudCoins");
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        c.gridwidth = 1;
        c.insets = new Insets(0, 16, 10, 0); 
        c.anchor = GridBagConstraints.EAST;
         // Transfer from
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        JLabel x = new JLabel("         From Wallet");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        gct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;

        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", rv.options);
        gridbag.setConstraints(cboxfrom.getComboBox(), c);
        cboxfrom.addOption(AppUI.getLocalFolderOption());
        gct.add(cboxfrom.getComboBox());

        y++;
        
        
         // Local folder
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST;
        final JLabel lfText = new JLabel("         Local folder");
        gridbag.setConstraints(lfText, c);
        AppUI.setCommonFont(lfText);
        gct.add(lfText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
     
        final MyTextField localFolder = new MyTextField("Select Folder", false, true);
        localFolder.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!Config.DEFAULT_EXPORT_DIR.isEmpty())
                    chooser.setCurrentDirectory(new File(Config.DEFAULT_EXPORT_DIR));
                
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    localFolder.setData(chooser.getSelectedFile().getName());
                }
            }
        });
        
        gridbag.setConstraints(localFolder.getTextField(), c);
        gct.add(localFolder.getTextField());     
        y++;
        
        localFolder.getTextField().setVisible(false);
        lfText.setVisible(false);

        rightPanel.add(gct); 
        
        
        cboxfrom.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int dstIdx = cboxfrom.getSelectedIndex() - 1;
                
                localFolder.getTextField().setVisible(false);
                lfText.setVisible(false);
          
                // Local folder
                if (dstIdx == rv.idxs.length) {
                    localFolder.getTextField().setVisible(true);
                    lfText.setVisible(true);
                } 
            }
        });
        
        

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {              
                int srcIdx = cboxfrom.getSelectedIndex() - 1;   
                if (srcIdx == rv.idxs.length) {
                    ps.currentScreen = ProgramState.SCREEN_LIST_SERIALS_DONE;
                    showScreen();
                    return;
                }
                
                if (srcIdx < 0 || srcIdx >= rv.idxs.length) {
                    ps.errText = "Please select Wallet";
                    showScreen();
                    return;
                }
                
                srcIdx = rv.idxs[srcIdx];

                Wallet srcWallet = wallets[srcIdx];
                ps.srcWallet = srcWallet;            
                
                ps.currentScreen = ProgramState.SCREEN_LIST_SERIALS_DONE;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp); 

    }
    
    
    public void showFixfrackedScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Fix Fracked");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        maybeShowError(ct);
        
        final optRv rv = setOptionsForWallets(true, false);
        if (rv.idxs.length == 0) {
            JLabel nx = new JLabel("You have no fracked coins");
            AppUI.setSemiBoldFont(nx, 20);
            AppUI.alignCenter(nx);
            ct.add(nx);
            return;
        }

        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        // Text
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 80, 0); 
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;

        JLabel l = new JLabel("Fix Fracked CloudCoins");
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        c.gridwidth = 1;
        c.insets = new Insets(0, 16, 10, 0); 
        c.anchor = GridBagConstraints.EAST;
         // Transfer from
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        JLabel x = new JLabel("         From Wallet");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        gct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;

        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", rv.options);
        gridbag.setConstraints(cboxfrom.getComboBox(), c);
        gct.add(cboxfrom.getComboBox());

        y++;
        
        
        // Password From
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        final JLabel spText = new JLabel("Password");
        gridbag.setConstraints(spText, c);
        AppUI.setCommonFont(spText);
        gct.add(spText);
        
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField passwordSrc = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(passwordSrc.getTextField(), c);
        gct.add(passwordSrc.getTextField());     
        y++;
        
        passwordSrc.getTextField().setVisible(false);
        spText.setVisible(false);
              
        cboxfrom.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int srcIdx = cboxfrom.getSelectedIndex() - 1;              
                if (srcIdx < 0 || srcIdx >= rv.idxs.length) 
                    return;
                
                srcIdx = rv.idxs[srcIdx];
                                
                Wallet srcWallet = wallets[srcIdx];


                if (srcWallet.isEncrypted()) {                 
                    passwordSrc.getTextField().setVisible(true);
                    spText.setVisible(true);
                } else {
                    passwordSrc.getTextField().setVisible(false);
                    spText.setVisible(false);
                }
            }
        });
    
        rightPanel.add(gct); 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {              
                int srcIdx = cboxfrom.getSelectedIndex() - 1;              
                if (srcIdx < 0 || srcIdx >= rv.idxs.length) {
                    ps.errText = "Please select Wallet";
                    showScreen();
                    return;
                }

                srcIdx = rv.idxs[srcIdx];
                Wallet srcWallet = wallets[srcIdx];

                if (srcWallet == null)
                    return;
                if (srcWallet.isEncrypted()) {
                    if (passwordSrc.getText().isEmpty()) {
                        ps.errText = "Password is empty";
                        showScreen();
                        return;
                    }
                    
                    String wHash = srcWallet.getPasswordHash();
                    String providedHash = AppCore.getMD5(passwordSrc.getText());
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
                    
                    ps.typedPassword = passwordSrc.getText();
                    srcWallet.setPassword(ps.typedPassword);
                }
               
                ps.srcWallet = srcWallet;             
                ps.currentScreen = ProgramState.SCREEN_FIXING_FRACKED;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);
    }
    

    public optRv setOptionsForWallets(boolean checkFracked, boolean needEmpty) {
        return setOptionsForWalletsCommon(checkFracked, needEmpty, false, null);
    }
    
    
    public optRv setOptionsForWalletsAll(String name) {
        optRv rv = new optRv();
 
        int len = wallets.length;
        if (name != null)
            len -= 1;
        
        if (len < 0)
            len = 0;
        
        rv.options = new String[len];
        rv.idxs = new int[len];

        if (len == 0)
            return rv;
        
        for (int i = 0, j = 0; i < wallets.length; i++) {  
            if (name != null && wallets[i].getName().equals(name))
                continue;
            
            rv.options[j] = wallets[i].getName() + " - " + AppCore.formatNumber(wallets[i].getTotal()) + " CC";
            rv.idxs[j] = i;
            j++;
        }
     
        return rv;     
    }
    
    public optRv setOptionsForWalletsCommon(boolean checkFracked, boolean needEmpty, boolean includeSky, String name) {
        optRv rv = new optRv();
        
        int cnt = 0;
        int fc = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!includeSky && wallets[i].isSkyWallet())
                continue;
            
            if (wallets[i].getTotal() == 0) {
                if (needEmpty) 
                    cnt++;  
                
                continue;
            } else {
                if (needEmpty)
                    continue;
            }

            if (wallets[i].getTotal() == 0) 
                continue;
            
            if (checkFracked) {
                fc = AppCore.getFilesCount(Config.DIR_FRACKED, wallets[i].getName());
                if (fc == 0) 
                    continue;  
            }
            
            if (name != null && wallets[i].getName().equals(name))
                continue;
            
            cnt++;
        }
      
        rv.options = new String[cnt];
        rv.idxs = new int[cnt];
        
        if (cnt == 0)
            return rv;
              
        int j = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!includeSky && wallets[i].isSkyWallet())
                continue;
            
            if (wallets[i].getTotal() == 0) {
                if (!needEmpty)
                    continue;
            } else {
                if (needEmpty)
                    continue;
            }
            
            int wTotal = wallets[i].getTotal();
            if (checkFracked) {
                fc = AppCore.getFilesCount(Config.DIR_FRACKED, wallets[i].getName());
                if (fc == 0) 
                    continue;  
                
                int[][] counters = wallets[i].getCounters();
                wTotal = AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]);
                //wTotal = fc;
            }
            
            if (name != null && wallets[i].getName().equals(name))
                continue;
            
            
            rv.options[j] = wallets[i].getName() + " - " + AppCore.formatNumber(wTotal) + " CC";
            rv.idxs[j] = i;
            j++;
        }
     
        return rv;
    }

    public void showDeleteWalletScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Delete Wallet");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        maybeShowError(ct);
           
        final optRv rv = setOptionsForWalletsCommon(false, true, true, null);
        if (rv.idxs.length == 0) {
            JLabel nx = new JLabel("You have no empty wallets to delete");
            AppUI.setSemiBoldFont(nx, 20);
            AppUI.alignCenter(nx);
            ct.add(nx);
            return;
        }

        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        // Text
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 80, 0); 
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;

        JLabel l = new JLabel("You can only delete wallets that are empty");
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        c.gridwidth = 1;
        c.insets = new Insets(0, 16, 10, 0); 
        c.anchor = GridBagConstraints.EAST;
         // Transfer from
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        JLabel x = new JLabel("         Wallet");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        gct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;

        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", rv.options);
        gridbag.setConstraints(cboxfrom.getComboBox(), c);
        gct.add(cboxfrom.getComboBox());

        y++;

        rightPanel.add(gct); 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {  
                int srcIdx = cboxfrom.getSelectedIndex() - 1;              
                if (srcIdx < 0 || srcIdx >= rv.idxs.length) {
                    ps.errText = "Please select Wallet";
                    showScreen();
                    return;
                }
       
                srcIdx = rv.idxs[srcIdx];
                Wallet srcWallet = wallets[srcIdx];
                
                if (srcWallet.isSkyWallet()) {
                    Wallet dstWallet = sm.getFirstNonSkyWallet();
                    if (dstWallet == null) {
                        ps.errText = "You cannot delete a Sky Wallet because you do not have a Local Wallet";
                        showScreen();
                        return;
                    }
                }
                
                ps.srcWallet = srcWallet;
                ps.currentScreen = ProgramState.SCREEN_CONFIRM_DELETE_WALLET;
                showScreen();

            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp); 
        
    }
    
    public void showBackupKeys() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Export Keys");   
        ct.add(ltitle);
        
        maybeShowError(ct);
           
        String[] idCoins = AppCore.getFilesInDir(AppCore.getIDDir(), null);
        if (idCoins.length == 0) {
            JLabel nx = new JLabel("You have no keys to backup");
            AppUI.setSemiBoldFont(nx, 20);
            AppUI.alignCenter(nx);
            ct.add(nx);
            return;
        }

        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        // Text
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 80, 0); 
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;

        JLabel l = new JLabel("Keys will not be encrypted. You should put them in a secure location");
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        c.gridwidth = 1;
 
        
        
        // Backup folder
        JLabel txt = new JLabel("Backup Folder");
        AppUI.setCommonFont(txt);
        AppUI.alignCenter(txt);
        c.insets = new Insets(0, 116, 10, 0);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy = y;
        
        gridbag.setConstraints(txt, c);
        gct.add(txt);
              
        
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
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
        
        c.insets = new Insets(0, 16, 10, 0);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(tf1.getTextField(), c);
        gct.add(tf1.getTextField());
        

        
        rightPanel.add(gct); 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {                      
                if (ps.chosenFile.isEmpty()) {
                    ps.errText = "Folder is not chosen";
                    showScreen();
                    return;
                }
                
                for (int i = 0; i < idCoins.length; i++) {
                    String src = AppCore.getIDDir() + File.separator + idCoins[i];
                    String dst = ps.chosenFile + File.separator + idCoins[i];
                    
                    if (!AppCore.copyFile(src, dst)) {
                        ps.errText = "Failed to copy file: " + idCoins[i];
                        showScreen();
                        return;
                    }

                }
                
                ps.currentScreen = ProgramState.SCREEN_SHOW_BACKUP_KEYS_DONE;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp); 
        
    }
    
    public void showBackupScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Backup");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        maybeShowError(ct);
           
        final optRv rv = setOptionsForWallets(false, false);
        if (rv.idxs.length == 0) {
            JLabel nx = new JLabel("You have no coins to backup");
            AppUI.setSemiBoldFont(nx, 20);
            AppUI.alignCenter(nx);
            ct.add(nx);
            return;
        }

        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        int y = 0;
        
        // Text
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 80, 0); 
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;

        JLabel l = new JLabel("Backup will allow you to create Backup of your CloudCoins");
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        y++;
        c.gridwidth = 1;
        c.insets = new Insets(0, 16, 10, 0); 
        c.anchor = GridBagConstraints.EAST;
         // Transfer from
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        JLabel x = new JLabel("         Wallet");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        gct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;

        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", rv.options);
        gridbag.setConstraints(cboxfrom.getComboBox(), c);
        gct.add(cboxfrom.getComboBox());

        y++;

        // Password From
        c.anchor = GridBagConstraints.EAST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        final JLabel spText = new JLabel("Password");
        gridbag.setConstraints(spText, c);
        AppUI.setCommonFont(spText);
        gct.add(spText);
        
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField passwordSrc = new MyTextField("Wallet Password", true);
        gridbag.setConstraints(passwordSrc.getTextField(), c);
        gct.add(passwordSrc.getTextField());     
        y++;
        
        passwordSrc.getTextField().setVisible(false);
        spText.setVisible(false);
              
        cboxfrom.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int srcIdx = cboxfrom.getSelectedIndex() - 1;              
                if (srcIdx < 0 || srcIdx >= rv.idxs.length) 
                    return;

                srcIdx = rv.idxs[srcIdx];
                
                Wallet srcWallet = wallets[srcIdx];
                if (srcWallet == null)
                    return;

                if (srcWallet.isEncrypted()) {                 
                    passwordSrc.getTextField().setVisible(true);
                    spText.setVisible(true);
                } else {
                    passwordSrc.getTextField().setVisible(false);
                    spText.setVisible(false);
                }
            }
        });
        
        
        
        // Backup folder
        JLabel txt = new JLabel("Backup Folder");
        AppUI.setCommonFont(txt);
        AppUI.alignCenter(txt);
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 0;
        c.gridy = y;
        
        gridbag.setConstraints(txt, c);
        gct.add(txt);
              
        
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
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

        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(tf1.getTextField(), c);
        gct.add(tf1.getTextField());
        

        
        rightPanel.add(gct); 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {              
                int srcIdx = cboxfrom.getSelectedIndex() - 1;              
                if (srcIdx < 0 || srcIdx >= rv.idxs.length) {
                    ps.errText = "Please select Wallet";
                    showScreen();
                    return;
                }

                srcIdx = rv.idxs[srcIdx];
   
                Wallet srcWallet = wallets[srcIdx];
                if (srcWallet.isEncrypted()) {
                    if (passwordSrc.getText().isEmpty()) {
                        ps.errText = "Password is empty";
                        showScreen();
                        return;
                    }
                    
                    String wHash = srcWallet.getPasswordHash();
                    String providedHash = AppCore.getMD5(passwordSrc.getText());
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
                    
                    ps.typedPassword = passwordSrc.getText();
                }
                     
                ps.srcWallet = srcWallet;              
                if (ps.chosenFile.isEmpty()) {
                    ps.errText = "Folder is not chosen";
                    showScreen();
                    return;
                }
                
                sm.setActiveWalletObj(ps.srcWallet);
                ps.srcWallet.setPassword(ps.typedPassword);
                int total = ps.srcWallet.getTotal();
                
                String tag = Config.BACKUP_TAG + "-" + System.currentTimeMillis();
                
                if (ps.srcWallet.isEncrypted()) {
                    sm.startSecureExporterService(Config.TYPE_STACK, total, tag, ps.chosenFile, true, new ExporterBackupCb());
                } else {
                    sm.startExporterService(Config.TYPE_STACK, total, tag, ps.chosenFile, true, new ExporterBackupCb());
                }
                
                ps.currentScreen = ProgramState.SCREEN_DOING_BACKUP;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp); 
    }
    
    public void showListSerialsDone() {
        showLeftScreen();
 
        final Wallet w = ps.srcWallet;     

        JPanel rightPanel = getRightPanel(AppUI.getColor4());    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        int[] sns;
        if (w != null) {     
            JLabel ltitle = AppUI.getTitle("List Serials - " + w.getName() + " - " + w.getTotal() + " CC");   
            ct.add(ltitle);
            AppUI.hr(ct, 20);
        
            sns = w.getSNs();
            if (sns.length == 0) {
                JLabel trLabel = new JLabel("No Serials");
                AppUI.setSemiBoldFont(trLabel, 20);
                AppUI.alignCenter(trLabel);
                ct.add(trLabel);
                return;
            }
        } else if (ps.chosenFile == null) {
            JLabel trLabel = new JLabel("No Folder or Wallet chosen");
            AppUI.setSemiBoldFont(trLabel, 20);
            AppUI.alignCenter(trLabel);
            ct.add(trLabel);
            return;
        } else {
            CloudCoin[] ccs = AppCore.getCoinsInDir(ps.chosenFile);
            if (ccs == null) {
                JLabel trLabel = new JLabel("Failed to read folder: " + ps.chosenFile);
                AppUI.setSemiBoldFont(trLabel, 20);
                AppUI.alignCenter(trLabel);
                ct.add(trLabel);
                return;
            }
            
            sns = new int[ccs.length];
            for (int i = 0; i < ccs.length; i++) {
                sns[i] = ccs[i].sn;
            }
        }
        
        
        // Scrollbar & Table  
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            JLabel lbl;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                lbl = (JLabel) this;
                if (row % 2 == 0) {
                    AppUI.setBackground(lbl, AppUI.getColor3());
                } else {
                    AppUI.setBackground(lbl, AppUI.getColor4());
                }

                lbl.setHorizontalAlignment(JLabel.CENTER);
                AppUI.setMargin(lbl, 8);
  
                return lbl;
            }
        };
              
        String[][] serials = new String[sns.length][];
        for (int i = 0; i < sns.length; i++) {
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, sns[i]);
            
            serials[i] = new String[3];
            serials[i][0] = "" + sns[i];
            serials[i][1] = "" + cc.getDenomination();
            serials[i][2] = ""; // We need it
        }
        
        //serial number table
        final JTable table = new JTable();
        final JScrollPane scrollPane = AppUI.setupTable(table, new String[] {"Serial Number", "Denomination"}, serials, r);
        AppUI.setSize(scrollPane, 260, 325);
 
        ct.add(scrollPane);

        JPanel bp = getTwoButtonPanelCustom("Print", "Export List", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    try {
                        table.print();
                    } catch (PrinterException pe) {
                        System.out.println("Failed to print");
                    }
                }
            }, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser c = new JFileChooser();
                c.setSelectedFile(new File(w.getName() + "-serials.csv"));

                int rVal = c.showSaveDialog(null);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                    w.saveSerials(c.getSelectedFile().getAbsolutePath());
                }
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);     
    }
    
    public synchronized void updateTransactionWalletData(Wallet w) {
        if (trTitle != null) {
            trTitle.setText(w.getName() + " : " + w.getTotal() + " CC");
            trTitle.validate();
            trTitle.repaint();
        }
        
        if (trInventory != null) {
            int[][] counters = w.getCounters();   
            if (counters != null && counters.length != 0) {
                int t1, t5, t25, t100, t250;
        
                t1 = counters[Config.IDX_FOLDER_BANK][Config.IDX_1] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_1] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_1];
        
                t5 = counters[Config.IDX_FOLDER_BANK][Config.IDX_5] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_5] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_5];
        
                t25 = counters[Config.IDX_FOLDER_BANK][Config.IDX_25] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_25] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_25];
        
                t100 =  counters[Config.IDX_FOLDER_BANK][Config.IDX_100] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_100] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_100];
            
                t250 = counters[Config.IDX_FOLDER_BANK][Config.IDX_250] + 
                counters[Config.IDX_FOLDER_FRACKED][Config.IDX_250] + 
                counters[Config.IDX_FOLDER_VAULT][Config.IDX_250];
            
                String s;
            
                s = "1's: <b>" + t1 + "</b> |  5's: <b>" + t5 + "</b> |  25's: <b>" + t25 
                    + "</b> | 100's: <b>" + t100 + "</b> | 250's: <b>" + t250 + " </b>";
            
                trInventory.setText("<html><div style='text-align:center; width:668px'>" + s + "</div></html>");
            }
        }
    }
    
    public void showTransactionsScreen() {      
        
        trTitle = AppUI.getTitle("");   
        trInventory = new JLabel("");
        Wallet w = sm.getActiveWallet(); 
        updateTransactionWalletData(w);
        
        boolean isSky = sm.getActiveWallet().isSkyWallet() ? true : false;
    
        showLeftScreen();
 
        JPanel rightPanel = getRightPanel(AppUI.getColor4());    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);

        ct.add(trTitle);
   
        AppUI.hr(ct, 10);
        
        if (!w.getEmail().isEmpty()) {

            // Email
            JLabel el = new JLabel("Recovery Email: " + w.getEmail());
            AppUI.setFont(el, 14);
            AppUI.alignCenter(el);
            ct.add(el);
            
            AppUI.hr(ct, 10);
        }
  
        // Coins
        int[][] counters = w.getCounters();   
        if (counters != null && counters.length != 0) { 
            AppUI.alignCenter(trInventory);
            AppUI.setFont(trInventory, 14);
            ct.add(trInventory);
            
            AppUI.hr(ct, 20);
            
            
        }
        
        
        
        // File saver
        if (ps.sendType == ProgramState.SEND_TYPE_FOLDER) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(new File(ps.chosenFile));
                } catch (IOException e) {
                    wl.error(ltag, "Failed to open browser: " + e.getMessage());
                }
            }
        }
        
        /*
        if (ps.sendType == ProgramState.SEND_TYPE_FOLDER) {
            Thread t = new Thread(new Runnable() {
                public void run(){
                    //UIManager.put("FileChooser.readOnly", Boolean.TRUE);  
                    JFileChooser c = new JFileChooser(ps.chosenFile);
                   // UIManager.put("FileChooser.readOnly", Boolean.FALSE);  
                    c.setSelectedFile(new File(ps.typedAmount + ".CloudCoin." + ps.typedMemo + ".stack"));

                    int rVal = c.showSaveDialog(null);
                    if (rVal == JFileChooser.APPROVE_OPTION) {
                        String file = c.getSelectedFile().getAbsolutePath();
                        sm.setActiveWalletObj(ps.srcWallet);
                        ps.srcWallet.setPassword(ps.typedSrcPassword);
                        if (ps.srcWallet.isEncrypted()) {
                            sm.startSecureExporterService(Config.TYPE_STACK, ps.typedAmount, ps.typedMemo, file, false, new ExporterCb());
                        } else {
                            sm.startExporterService(Config.TYPE_STACK, ps.typedAmount, ps.typedMemo, file, false, new ExporterCb());
                        }
                    }
                }
            });
        
            t.start();  
        }
        */
        
        // Create transactions
        final String[][] trs;
        JLabel trLabel;
        String[] headers;
        
        if (isSky) {
            Hashtable<String, String[]> envelopes = sm.getActiveWallet().getEnvelopes();
            trLabel = new JLabel("Skywallet Contents. Click Transfer to Download");
            if (envelopes == null || envelopes.size() == 0) {
                trLabel = new JLabel("No Coins");
                AppUI.setSemiBoldFont(trLabel, 20);
                AppUI.alignCenter(trLabel);
                ct.add(trLabel);
                return;
            }
 
            
            Enumeration<String> enumeration = envelopes.keys();
            ArrayList<String> hlist = Collections.list(enumeration);
            Collections.sort(hlist);

            trs = new String[envelopes.size()][];
            int i = 0;
           // while (enumeration.hasMoreElements()) {
           //     String key = enumeration.nextElement();
            for (String key : hlist) {
                String[] data = envelopes.get(key);

                trs[i] = new String[4];
                trs[i][0] = data[0];
                trs[i][1] = data[2];
                trs[i][2] = data[1];
                trs[i][3] = "";
       
                i++;
            }
                
            headers = new String[] {
                "Memo (note)",
                "Date",
                "Amount"
            };        
             
        } else {          
            trs = sm.getActiveWallet().getTransactions();
        
            if (trs == null || trs.length == 0) {
                trLabel = new JLabel("No transactions");
                AppUI.setSemiBoldFont(trLabel, 20);
                AppUI.alignCenter(trLabel);
                ct.add(trLabel);
                return;
            }
        
            trLabel = new JLabel("Transaction History");
            
            headers = new String[] {
                "Memo (note)",
                "Date",
                "Deposit",
                "Withdraw",
                "Total"
            };
        }
        AppUI.setSemiBoldFont(trLabel, 20);
        AppUI.alignCenter(trLabel);
        ct.add(trLabel);
        
        //margin above transaction table
        AppUI.hr(ct, 10);
 
        // Scrollbar & Table  
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            JLabel lbl;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                lbl = (JLabel) this;
                if (column == 0) {                          
                    String hash = trs[row][trs[0].length - 1];                
                    String html = AppCore.getReceiptHtml(hash, sm.getActiveWallet().getName());
                    if (html != null) {
                        AppUI.setColor(lbl, AppUI.getColor0());
                        AppUI.underLine(lbl);
                    }
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
     
        final JTable table = new JTable();
        final JScrollPane scrollPane = AppUI.setupTable(table, headers, trs, r);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        
        MouseAdapter ma = new MouseAdapter() {
            private int prevcolumn = -1;

            @Override
            public void mouseReleased(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                
                if (column != 0)
                    return;
                
                String hash = trs[row][trs[0].length - 1];                
                String html = AppCore.getReceiptHtml(hash, ps.currentWallet.getName());
                if (html == null)
                    return;
                
                
                final JDialog f = new JDialog(mainFrame, "Receipt", true);
                AppUI.noOpaque((JComponent) f.getContentPane());
                AppUI.setSize(f, (int) (tw / 1.6), (int) (th / 1.6)); 

                JTextPane tp = new JTextPane();
                
                AppUI.setFont(tp, 12);
                String fontfamily = tp.getFont().getFamily();

                tp.setContentType("text/html"); 
                tp.setText("<html><div style=' font-family:"+fontfamily+"; font-size: 12px'>" + html + "</div></html>"); 
                tp.setEditable(false); 
                tp.setBackground(null);

                JScrollPane scrollPane = new JScrollPane(tp);

                f.add(scrollPane);
                f.pack();
                f.setLocationRelativeTo(mainFrame);
                f.setVisible(true);       
            
               
            
            }   //end mouse release
            
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
               
                if (prevcolumn == column)
                    return;
               
                prevcolumn = column;
                
                if (column == 0) {
                    AppUI.setHandCursor(table);
                } else {
                    AppUI.setDefaultCursor(table);
                }
            }
            
            public void mouseExited(MouseEvent e) {
            }//end mouse exited
        };//end mouse Adapter
        
        
        
        
        table.addMouseListener(ma);
        table.addMouseMotionListener(ma);
 
        ct.add(scrollPane);
    
        //print and export history from wallet 
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
                
                File f = new File(w.getName() + "-transactions.csv");
                c.setSelectedFile(f);

                int rVal = c.showSaveDialog(null);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                    if (w.isSkyWallet())
                        w.saveEnvelopes(c.getSelectedFile().getAbsolutePath());
                    else            
                        w.saveTransations(c.getSelectedFile().getAbsolutePath());
                    if (!Desktop.isDesktopSupported())
                        return;
                    try {
                        Desktop.getDesktop().open(c.getSelectedFile().getParentFile());
                    } catch (IOException ie) {
                        wl.error(ltag, "Failed to open browser: " + ie.getMessage());
                    }
                }
            }
        });
        
        AppUI.hr(rightPanel, 5);
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
        JPanel subInnerCore = AppUI.createRoundedPanel(ct, AppUI.getColor11(), 20, 40);
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
                AppUI.roundCorners(p, AppUI.getColor11(), 40);
            }
        };
            
        subInnerCore.addMouseListener(ma);
              
        // Space between buttons
        AppUI.hr(ct, 30);

        // Sky wallet button
        subInnerCore = AppUI.createRoundedPanel(ct, AppUI.getColor11(), 20, 40);
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
                AppUI.roundCorners(p, AppUI.getColor11(), 40);
            }
        };
            
        subInnerCore.addMouseListener(ma);
 
        rightPanel.add(gct);
    }
    
    public void showCheckingSkyIDScreen() {
        JPanel subInnerCore = getModalJPanel("Checking your Sky Wallet ID Coin...");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        
        pbarText = new JLabel("");
        AppUI.setCommonFont(pbarText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(pbarText, c);
        ct.add(pbarText);
        
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
        
      
        AppUI.hr(subInnerCore, 4);
    }
    
    public void showExportingScreen() {
        boolean isError = !ps.errText.equals("");
        
        JPanel subInnerCore = getModalJPanel("Exporting Coins. Please wait...");
        maybeShowError(subInnerCore);
      
        AppUI.hr(subInnerCore, 4);
    }
    
    public void showDoingBackupScreen() {
        boolean isError = !ps.errText.equals("");
        
        JPanel subInnerCore = getModalJPanel("Doing Backup. Please wait...");
        maybeShowError(subInnerCore);
      
        AppUI.hr(subInnerCore, 4);
    }
    
    public void showSetDNSRecordScreen() {
        boolean isError = !ps.errText.equals("");
        
        JPanel subInnerCore = getModalJPanel("Setting DNS Record. Please wait...");
        maybeShowError(subInnerCore);
      
        AppUI.hr(subInnerCore, 4);
    }
    
    public void showSettingsDoneScreen() {
        JPanel subInnerCore;       
        subInnerCore = getModalJPanel("Settings");
        subInnerCore.add(AppUI.hr(32));
        
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
  
        //ct.setLayout(gridbag);

        String txt = "Configuration Saved";

        // Q
        JLabel x = new JLabel("<html><div style='width:460px;text-align:center'>" + txt + "</div></html>");
        AppUI.setCommonBoldFont(x);
        ct.add(x);

             
        resetState(); 
    }
    
    public void showSettingsScreen() {
        showLeftScreen();

        JPanel rightPanel = getRightPanel();    
    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        JLabel ltitle = AppUI.getTitle("Settings");   
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
        c.insets = new Insets(10, 18, 0, 0); 
        oct.setLayout(gridbag);
        
        
        // Echo Timeout
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        JLabel x = new JLabel("Echo Timeout, s");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        oct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 

        final MyTextField echoTimeout = new MyTextField("Echo Timeout", false);
        echoTimeout.setData(Integer.toString(Config.ECHO_TIMEOUT / 1000));
        gridbag.setConstraints(echoTimeout.getTextField(), c);
        oct.add(echoTimeout.getTextField());
 
        y++;
        
        rightPanel.add(oct);
        
        // Send/Receive
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        JLabel dto = new JLabel("Send/Receive Timeout, s");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        final MyTextField readTimeout = new MyTextField("Send/Receive Timeout", false);
        readTimeout.setData(Integer.toString(Config.READ_TIMEOUT / 1000));
        gridbag.setConstraints(readTimeout.getTextField(), c);
        oct.add(readTimeout.getTextField());        
        y++;
        
       
        // Detect timeout
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        dto = new JLabel("Detect Timeout, s");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        final MyTextField detectTimeout = new MyTextField("Detect Timeout", false);
        detectTimeout.setData(Integer.toString(Config.MULTI_DETECT_TIMEOUT / 1000));
        gridbag.setConstraints(detectTimeout.getTextField(), c);
        oct.add(detectTimeout.getTextField());        
        y++;
        
        
        
        // Fix timeout
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        dto = new JLabel("Fix Timeout, s");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        final MyTextField fixTimeout = new MyTextField("Fix Timeout", false);
        fixTimeout.setData(Integer.toString(Config.FIX_FRACKED_TIMEOUT / 1000));
        gridbag.setConstraints(fixTimeout.getTextField(), c);
        oct.add(fixTimeout.getTextField());        
        y++;
        
        
        // Max Coins
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        dto = new JLabel("Max Notes to Check");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        final MyTextField maxNotes = new MyTextField("Max Notes", false);
        maxNotes.setData(Integer.toString(Config.DEFAULT_MAX_COINS_MULTIDETECT));
        gridbag.setConstraints(maxNotes.getTextField(), c);
        oct.add(maxNotes.getTextField());        
        y++;
        
        
        // DDNS Server
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        dto = new JLabel("DDNS Server");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);

        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        final MyTextField ddnsServer = new MyTextField("DDNS Server", false);
        ddnsServer.setData(Config.DDNSSN_SERVER);
        gridbag.setConstraints(ddnsServer.getTextField(), c);
        oct.add(ddnsServer.getTextField());        
        y++;
        
        /*
        // Export Dir
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;   
        c.anchor = GridBagConstraints.EAST; 
        dto = new JLabel("Export Folder");
        gridbag.setConstraints(dto, c);
        AppUI.setCommonFont(dto);
        oct.add(dto);

        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
     
        final MyTextField localFolder = new MyTextField("Export Folder", false, true);
        localFolder.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!Config.DEFAULT_EXPORT_DIR.isEmpty())
                    chooser.setCurrentDirectory(new File(Config.DEFAULT_EXPORT_DIR));
                
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    localFolder.setData(chooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        localFolder.setData(new File(Config.DEFAULT_EXPORT_DIR).getAbsolutePath());
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST; 
        gridbag.setConstraints(localFolder.getTextField(), c);
        oct.add(localFolder.getTextField());   
   
        y++;
        */
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                int echot = Validator.getIntFromString(echoTimeout.getText(), Config.MIN_ECHO_TIMEOUT / 1000, Config.MAX_ECHO_TIMEOUT / 1000);
                if (echot < 0) {
                    ps.errText = "Echo timeout must be in the range (" + Config.MIN_ECHO_TIMEOUT/1000 + ", " + Config.MAX_ECHO_TIMEOUT + ")";
                    showScreen();
                    return;
                }
                
                int detectt = Validator.getIntFromString(detectTimeout.getText(), Config.MIN_DETECT_TIMEOUT / 1000, Config.MAX_DETECT_TIMEOUT / 1000);
                if (detectt < 0) {
                    ps.errText = "Detect timeout must be in the range (" + Config.MIN_DETECT_TIMEOUT/1000 + ", " + Config.MAX_DETECT_TIMEOUT + ")";
                    showScreen();
                    return;
                }
                
                int fixt = Validator.getIntFromString(fixTimeout.getText(), Config.MIN_FIX_TIMEOUT / 1000, Config.MAX_FIX_TIMEOUT / 1000);
                if (fixt < 0) {
                    ps.errText = "Fix timeout must be in the range (" + Config.MIN_FIX_TIMEOUT/1000 + ", " + Config.MAX_FIX_TIMEOUT + ")";
                    showScreen();
                    return;
                }
                
                int readt = Validator.getIntFromString(readTimeout.getText(), Config.MIN_READ_TIMEOUT / 1000, Config.MAX_READ_TIMEOUT / 1000);
                if (fixt < 0) {
                    ps.errText = "Send/Receive timeout must be in the range (" + Config.MIN_READ_TIMEOUT/1000 + ", " + Config.MAX_READ_TIMEOUT/1000 + ")";
                    showScreen();
                    return;
                }
                
                int notes = Validator.getIntFromString(maxNotes.getText(), Config.MIN_MULTI_NOTES, Config.MAX_MULTI_NOTES);
                if (notes < 0) {
                    ps.errText = "Max Notes must be in the range (" + Config.MIN_MULTI_NOTES + ", " + Config.MAX_MULTI_NOTES + ")";
                    showScreen();
                    return;
                }
                
                if (fixt < echot) {
                    ps.errText = "Fix timeout can not be less than Echo timeout";
                    showScreen();
                    return;
                }

                if (detectt < echot) {
                    ps.errText = "Detect timeout can not be less than Echo timeout";
                    showScreen();
                    return;
                }

                if (readt < echot) {
                    ps.errText = "Send/Receive timeout can not be less than Echo timeout";
                    showScreen();
                    return;
                }

                
                String ddnssn = ddnsServer.getText();
                if (!Validator.domain(ddnssn)) {
                    ps.errText = "Invalid DDNS Server";
                    showScreen();
                    return;
                }
                
                
                Config.DDNSSN_SERVER = ddnssn;
                //Config.DEFAULT_EXPORT_DIR = ps.chosenFile;
                Config.DEFAULT_MAX_COINS_MULTIDETECT = notes;
                Config.FIX_FRACKED_TIMEOUT = fixt * 1000;
                Config.MULTI_DETECT_TIMEOUT = detectt * 1000;
                Config.READ_TIMEOUT = readt * 1000;
                Config.ECHO_TIMEOUT = echot * 1000;
                        
                if (AppCore.writeConfig() == false) {
                    ps.errText = "Failed to write config file";
                    showScreen();
                    return;
                }
                
                ps.currentScreen = ProgramState.SCREEN_SETTINGS_SAVED;
                showScreen();

            }
        }, "Save");

        //rightPanel.add(oct);
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);    
    }
    
    public void showCreateSkyWalletScreen() {
        boolean isError = !ps.errText.equals("");

        
        final JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "CloudCoins", "jpg", "jpeg", "stack", "json", "txt");
        chooser.setFileFilter(filter);
        
        
        JPanel subInnerCore = getModalJPanel("Create Sky Wallet");
        maybeShowError(subInnerCore);
      
        AppUI.hr(subInnerCore, 4);
        
        // Outer Container
        JPanel oct = new JPanel();
        AppUI.setBoxLayout(oct, true);
        AppUI.noOpaque(oct);
        subInnerCore.add(oct);
               
        // Container
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        oct.add(ct);
        
        int y = 0;
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        
        JLabel x0, x1, x2, x3, x4;
        
        // Radio
        ButtonGroup nwGroup = new ButtonGroup();
        final MyRadioButton rb0 = new MyRadioButton();
        
        c.insets = new Insets(0, 0, 4, 0); 
        c.weightx = 0;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(rb0.getRadioButton(), c);
        ct.add(rb0.getRadioButton());
        rb0.attachToGroup(nwGroup);
        if (isError && !ps.isCreatingNewSkyWallet)
            rb0.select();
        
        x0 = new JLabel("Add Existing");
        c.insets = new Insets(0, 14, 4, 0); 
        AppUI.setFont(x0, 16);
        gridbag.setConstraints(x0, c);
        ct.add(x0);
        
        
        final MyRadioButton rb1 = new MyRadioButton();
        c.insets = new Insets(0, 44, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(rb1.getRadioButton(), c);
        ct.add(rb1.getRadioButton());
        rb1.attachToGroup(nwGroup);
        if (!isError || ps.isCreatingNewSkyWallet)
            rb1.select();
        
        x0 = new JLabel("Create New");
        c.insets = new Insets(0, 4, 4, 0); 
        AppUI.setFont(x0, 16);
        gridbag.setConstraints(x0, c);
        ct.add(x0);
        
        y++;
        
        c.gridwidth = 4;
        
        
        x1 = new JLabel("DNS Name or IP Address of Trusted Server");
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        AppUI.setFont(x1, 16);
        gridbag.setConstraints(x1, c);
        ct.add(x1);
        
        y++;
        
        String[] options = {
            Config.DDNS_DOMAIN,
        //    "Enter Custom Server"
        };
        
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        
        final RoundedCornerComboBox cbox = new RoundedCornerComboBox(AppUI.getColor2(), "Select Server", options);
        gridbag.setConstraints(cbox.getComboBox(), c);
        ct.add(cbox.getComboBox());
        cbox.setDefault(null);
        
        y++;
        
        // Name Label
        x2 = new JLabel("Your Proposed Address");
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;
        AppUI.setFont(x2, 16);

        gridbag.setConstraints(x2, c);
        ct.add(x2);

        y++;
        
        final MyTextField tf0 = new MyTextField("JohnDoe", false);
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(tf0.getTextField(), c);
        ct.add(tf0.getTextField());
        if (!ps.skyVaultDomain.isEmpty())
            tf0.setData(ps.skyVaultDomain);
        
        y++;
        // Text
        x3 = new JLabel("Select CloudCoin to be used as ID");
        AppUI.alignCenter(x3);
        c.insets = new Insets(4, 0, 4, 0);
        c.gridx = 0;
        c.gridy = y;
        AppUI.setFont(x3, 16);
        gridbag.setConstraints(x3, c);
        ct.add(x3);
              
        y++;
        final MyTextField tf1 = new MyTextField("", false, true);
        tf1.disable();
        tf1.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (rb1.isSelected()) {
                    Wallet w = sm.getFirstFullNonSkyWallet();
                    if (w != null) {
                        String dir = AppCore.getUserDir(Config.DIR_BANK, w.getName());
                        chooser.setCurrentDirectory(new File(dir));
                    } else {
                        chooser.setCurrentDirectory(null);
                    }   
                } else {
                    chooser.setCurrentDirectory(null);
                }
                
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    tf1.setData(chooser.getSelectedFile().getName());
                }
            }
        });
        
        if (!ps.chosenFile.isEmpty()) 
            tf1.setData(new File(ps.chosenFile).getName());
        
        

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(tf1.getTextField(), c);
        ct.add(tf1.getTextField());
        
        y++;

        x4 = new JLabel("Please put your ID coin here:                           ");
        AppUI.setFont(x4, 16);
        JLabel sl = AppUI.getHyperLink(AppCore.getIDDir(), "javascript:void(0); return false", 20);
        sl.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (!Desktop.isDesktopSupported())
                    return;
                try {
                    Desktop.getDesktop().open(new File(AppCore.getIDDir()));
                    ps.needInitWallets = true;
                } catch (IOException ie) {
                    wl.error(ltag, "Failed to open browser: " + ie.getMessage());
                }
            }
        });

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(x4, c);
        ct.add(x4);
        
        y++;
        
        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 0;
        c.gridy = y;
        gridbag.setConstraints(sl, c);
        ct.add(sl);
        
        
       // AppUI.getGBRow(subInnerCore, fname4, sl, y, gridbag);
       // AppUI.setColor(sl, AppUI.getColor2());
        //AppUI.underLine(sl);
        y++;

        if (ps.isCreatingNewSkyWallet) {
            x4.setVisible(false);
            sl.setVisible(false);
        } else {
            x4.setVisible(true);
            sl.setVisible(true);
        }
        
        
        
        JLabel tw = new JLabel("<html><div style='width:360px'>Note: The coin that you use for ID "
                + " will be placed into your ID folder. It will not be available for spending until you close"
                + " your SkyWallet</div></html>");
        c.insets = new Insets(4, 0, 0, 0);
        c.gridx = 0;
        c.gridy = y;
        //c.gridwidth = 1;
        gridbag.setConstraints(tw, c);
        ct.add(tw);
        AppUI.setFont(tw, 10);
        
        y++;
        
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                ps.isCreatingNewSkyWallet = rb1.isSelected();
                ps.skyVaultDomain = tf0.getText();
                
                int srcIdx = cbox.getSelectedIndex();
                if (srcIdx != 1) {
                    ps.errText = "Trusted Server is not selected";
                    showScreen();
                    return;
                }
                  
                ps.trustedServer = cbox.getSelectedValue();         
                if (ps.chosenFile.isEmpty()) {
                    ps.errText = "Select the CloudCoin";
                    showScreen();
                    return;
                }
                
                CloudCoin cc = AppCore.getCoin(ps.chosenFile);
                if (cc == null) {
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
                
                domain = domain.toLowerCase();
                if (domain.endsWith(ps.trustedServer)) {
                    domain = domain.substring(0, domain.length() - ps.trustedServer.length() - 1);
                    if (domain.isEmpty()) {
                        ps.errText = "Wallet name can't be empty";
                        showScreen();
                        return;
                    }                  
                }

                if (!Validator.domain(domain + "." + ps.trustedServer)) {
                    ps.errText = "Account name does not follow the rules of a DNS Host name";
                    showScreen();
                    return;
                }
                
                if (domain.indexOf(".") != -1) {
                    ps.errText = "Dots are not allowed in Sky Wallet names";
                    showScreen();
                    return;
                }
 
                final String newFileName = domain + "." + ps.trustedServer + ".stack";
                final DNSSn d = new DNSSn(domain, ps.trustedServer, wl);
                int sn = d.getSN();
                
                ps.domain = domain;
                if (rb1.isSelected()) {                 
                    if (sn >= 0) {
                        ps.errText = "DNS name already exists";
                        showScreen();
                        return;
                    }
                    
                    Thread t = new Thread(new Runnable() {
                        public void run(){
                            if (!d.setRecord(ps.chosenFile, sm.getSR())) {
                                ps.errText = "Failed to set record. Check if the coin is valid";
                                showScreen();
                                return;
                            } 

                            if (!AppCore.moveToFolderNewName(ps.chosenFile, AppCore.getIDDir(), null, newFileName)) {
                                ps.errText = "Failed to move ID Coin to the Default Wallet";
                                showScreen();
                                return;
                            }
                    
                            sm.initWallets();                          
                            ps.currentScreen = ProgramState.SCREEN_SKY_WALLET_CREATED;
                            showScreen();    
                        }
                    });
        
                    t.start();

                    EventQueue.invokeLater(new Runnable() {         
                        public void run() {
                            ps.currentScreen = ProgramState.SCREEN_SETTING_DNS_RECORD;
                            showScreen();
                            return;
                        }
                    });
                } else {
                    if (sn <= 0) {
                        ps.errText = "Wallet does not exist or network error occured";
                        showScreen();
                        return;
                    }
                    
                    if (cc.sn != sn) {
                        ps.errText = "Sky Coin SN does not match your Coin SN";
                        showScreen();
                        return;
                    }

                    final CloudCoin fcc = cc;
                    final String wname = domain + "." + ps.trustedServer;
                    sm.startAuthenticatorService(fcc, new CallbackInterface() {
                        public void callback(Object result) {
                            wl.debug(ltag, "AuthenticatorSkyCoin finished");
            
                            final Object fresult = result;
                            final AuthenticatorResult ar = (AuthenticatorResult) fresult;
                            if (ar.status == AuthenticatorResult.STATUS_ERROR || ar.status == AuthenticatorResult.STATUS_CANCELLED) {
                                ps.errText = "Failed to check your Coin";
                                showScreen();
                                return;
                            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                                ps.isCheckingSkyID = false;
                            } else {
                                setRAIDAProgressCoins(ar.totalRAIDAProcessed, 0, 0);
                                return;
                            }
                            
                            if (AppCore.getErrorCount(fcc) > Config.MAX_FAILED_RAIDAS_TO_SEND) {
                                ps.errText = getSkyIDErrorIfRAIDAFailed();
                                ps.currentScreen = ProgramState.SCREEN_SKY_WALLET_CREATED;
                                showScreen();
                                return;
                            } else if (AppCore.getPassedCount(fcc) < Config.PASS_THRESHOLD) {
                                ps.currentScreen = ProgramState.SCREEN_SKY_WALLET_CREATED;
                                ps.errText = getSkyIDError(wname, fcc.getPownString());
                                showScreen();
                                return;
                            }
                            
                            if (!AppCore.moveToFolderNewName(ps.chosenFile, AppCore.getIDDir(), null, newFileName)) {
                                ps.errText = "Failed to move coin";
                                showScreen();
                                return;
                            }
                
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    sm.initWallets();
                                    ps.currentScreen = ProgramState.SCREEN_SKY_WALLET_CREATED;
                                    showScreen();
                                }
                            });
                        }  
                    });

                    ps.currentScreen = ProgramState.SCREEN_CHECKING_SKYID;
                    showScreen();
                }

            }
        });
        
        rb0.addListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean isChecked = e.getStateChange() == ItemEvent.SELECTED;

                if (isChecked) {
                    x1.setVisible(false);
                    x2.setVisible(false);
                    x3.setVisible(false);
                    tf0.getTextField().setVisible(false);
                    tf1.getTextField().setVisible(false);
                    cbox.getComboBox().setVisible(false);
                    x4.setVisible(true);
                    sl.setVisible(true);
                    continueButton.disable();
                } else {
                    x1.setVisible(true);
                    x2.setVisible(true);
                    x3.setVisible(true);
                    tf0.getTextField().setVisible(true);
                    tf1.getTextField().setVisible(true);
                    cbox.getComboBox().setVisible(true);
                    x4.setVisible(false);
                    sl.setVisible(false);
                    continueButton.enable();
                }
            }
        });

        AppUI.hr(subInnerCore, 12);
        subInnerCore.add(bp);  
    }
    
    public void showCreateWalletScreen() {       
        JLabel x;
        String str;
        MyTextField walletName = null;

        str = "Create Wallet";
        
        JPanel subInnerCore = getModalJPanel(str);
        maybeShowError(subInnerCore);
        
        // Outer Container
        JPanel oct = new JPanel();
        AppUI.setBoxLayout(oct, true);
        AppUI.noOpaque(oct);
        subInnerCore.add(oct);
        
        // Space
        AppUI.hr(oct, 22);
        
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
        walletName.requestFocus();
        hct.add(walletName.getTextField());

        oct.add(hct);
        

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
        
        
        MyRadioButton rb1 = new MyRadioButton();
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y + 2;
        gridbag.setConstraints(rb1.getRadioButton(), c);
        ct.add(rb1.getRadioButton());
        rb1.attachToGroup(passwordGroup);
        rb1.select();
      
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
        
        
        MyRadioButton rb3 = new MyRadioButton();
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y + 3;
        gridbag.setConstraints(rb3.getRadioButton(), c);
        ct.add(rb3.getRadioButton());
        rb3.attachToGroup(recoveryGroup);
        rb3.select();
              
        // Buttons
        final MyRadioButton frb0 = rb0;
        final MyRadioButton frb2 = rb2;
        final MyTextField fwalletName = walletName;
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ps.cwalletPasswordRequested = frb0.isSelected();
                ps.cwalletRecoveryRequested = frb2.isSelected();
                
                if (!Validator.walletName(fwalletName.getText())) {
                    ps.errText = "Wallet name is incorrect";
                    showScreen();
                    return;
                }
                    
                ps.typedWalletName = fwalletName.getText().trim();
                
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
        return getTwoButtonPanel(al, null);
    }
    
    public JPanel getTwoButtonPanel(ActionListener al, String name) {
        JPanel bp = new JPanel();
     //   AppUI.setBoxLayout(bp, false);
        AppUI.noOpaque(bp);
       
        bp.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weightx = 1;
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

        String text = "Continue";
        if (isConfirmingScreen())
            text = "Confirm";
        
        if (name != null)
            text = name;
        
        cb = new MyButton(text);
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
        //AppUI.vr(bp, 26);
        
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
        //AppUI.vr(bp, 26);
        
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
        
        if (!ps.isEchoFinished)
            sm.startEchoService(new EchoCb());
        
        Thread t = new Thread(new Runnable() {
            public void run(){
                updateWalletAmount();
            }
        });
        
        t.start();

        return subInnerCore;
    }
    
    public void showLeftScreen() {
        lwrapperPanel = new JPanel();
        
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
        //    if (wallets[i].isSkyWallet())
        //        continue;
            
            walletWidget = getWallet(wallets[i], 0);
            wpanel.add(walletWidget);
        }
 
        // "Add" Button
        wpanel.add(getWallet(null, TYPE_ADD_BUTTON));
        
        // "Add Sky Wallet" Button
        wpanel.add(getWallet(null, TYPE_ADD_SKY));
 
        
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
        /*
        //scrollBar.setValue(200);
        InputMap im = scrollBar.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actMap = scrollBar.getActionMap();
        //InputMap im = scrollBar.getInputMap();

 
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Up");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Down");
        actMap.put("Up", new UpDownAction("Up", scrollPane.getVerticalScrollBar().getModel(), 42));
        actMap.put("Down", new UpDownAction("Down", scrollPane.getVerticalScrollBar().getModel(), 42));

        */
        //lwrapperPanel.requestFocus();
        //lwrapperPanel.requestFocusInWindow();
        //lwrapperPanel.setFocusable(true);
        
        corePanel.add(lwrapperPanel);
    }

    public JLayeredPane getWallet(Wallet wallet, int type) {
        boolean isDisabled = true;

        if (wallet != null)
            isDisabled = !isActiveWallet(wallet);
        
        // Pane
        JLayeredPane lpane = new JLayeredPane();
        AppUI.noOpaque(lpane);
        AppUI.setSize(lpane, 200, 140);
        AppUI.alignLeft(lpane);

        // Rounded Background
        Color color = isDisabled ? AppUI.getColor3() : AppUI.getColor4();
        
        final JButton addBtn = new JButton("");
        addBtn.setBorder(new RoundedBorder(20, color));
        addBtn.setFocusPainted(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setBounds(0, 0, 200, 120);


        JPanel cx = new JPanel();
        cx.setBounds(0,12,200,100);
        AppUI.noOpaque(cx);

        GridBagLayout gridbag = new GridBagLayout();   
        cx.setLayout(gridbag);
        
        GridBagConstraints c = new GridBagConstraints();      
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0); 
 
        int y = 0;
        
        String name;
        if (wallet == null) {
            if (type == TYPE_ADD_BUTTON)
                name = "Add Wallet";
            else if (type == TYPE_ADD_SKY)
                name = "Add Sky Wallet";
            else
                name = "";
        } else {
            name = wallet.getName();
        }
    
        JLabel l = new JLabel(name);
        
        if (isDisabled) {
            AppUI.setColor(l, AppUI.getDisabledColor2());
            AppUI.setFont(l, 22);
        } else {
            AppUI.setBoldFont(l, 22);
        }
        
        AppUI.alignCenter(l);    
        c.gridwidth = 3;
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;        
        gridbag.setConstraints(l, c);
        cx.add(l);
        
        y++;
        
        final JButton faddBtn = addBtn;    
        final boolean fisDisabled = isDisabled;
        
        if (wallet != null) {
            String iconNameL, iconNameR;
            ImageJPanel icon;
            if (wallet.isSkyWallet()) {
                if (isDisabled)
                    iconNameL = "Cloud Icon.png";
                else 
                    iconNameL = "Cloud Icon Acitve.png";
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
            c.insets = new Insets(24, 12, 0, 8); 
            c.gridwidth = 1;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;  
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(iconl, c);
            cx.add(iconl);

            c.insets = new Insets(32, 0, 0, 0); 
            // Amount (empty)
            JLabel jxl = new JLabel("");
            AppUI.setFont(jxl, 18);
            AppUI.alignCenter(jxl);
            AppUI.noOpaque(jxl);
            //amWrapper.add(jxl);
            //inner.add(amWrapper);
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y; 
            c.weightx = 1;
            c.anchor = GridBagConstraints.CENTER;
            gridbag.setConstraints(jxl, c);
            cx.add(jxl);

            // Set ref between wallet and its ui
            wallet.setuiRef(jxl);
        
            c.insets = new Insets(24, 8, 0, 12); 
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y; 
            c.weightx = 0;
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(iconr, c);
            cx.add(iconr);

            final Wallet fwallet = wallet;
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setActiveWallet(fwallet);
                    ps.sendType = 0;
                    ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                    fwallet.setNotUpdated();
                    showScreen();
                }   
                
                public void mouseEntered(MouseEvent e) {
                    if (!fisDisabled)
                        return;
                    
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

            c.gridwidth = 3;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;        
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, 0, 0, 0);
            gridbag.setConstraints(plus, c);
            cx.add(plus);

            final int ftype = type;
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    ps.currentWallet = null;
                    //ps.currentScreen = ProgramState.SCREEN_PREPARE_TO_ADD_WALLET;
                    if (ftype == TYPE_ADD_BUTTON) {
                        resetState();
                        ps.currentScreen = ProgramState.SCREEN_CREATE_WALLET;
                    } else if (ftype == TYPE_ADD_SKY) {  
                        resetState();
                        ps.currentScreen = ProgramState.SCREEN_CREATE_SKY_WALLET;
                    }
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
        
        lpane.addMouseListener( new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) { 
                addBtn.requestFocus();
            }         
        });

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
                
        
        String aText = AppUI.getAgreementText();
        aText = aText.replaceAll("<span class=\"instructions\">.+</span>", "");
        
        // Text
        text = new JLabel("<html><div style='padding-right: 20px; width: 720px'>" + aText + "</div></html>");
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
      //  scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);     
        agreementPanel.add(scrollPane);
      
        subInnerCore.add(agreementPanel);
    }
    
    public JPanel getModalJPanel(String title) {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();

        JPanel xpanel = new JPanel(new GridBagLayout());
        AppUI.noOpaque(xpanel);
        rightPanel.add(xpanel); 
        
        JPanel subInnerCore = AppUI.createRoundedPanel(xpanel, AppUI.getColor12(), 20);
        AppUI.setSize(subInnerCore, 718, 446);

        AppUI.hr(subInnerCore, 14);
        
        // Title
        JLabel ltitle = AppUI.getTitle(title);
        subInnerCore.add(ltitle);
        
        return subInnerCore;
    }
 
    
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        /*
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        
        Locale.setDefault(new Locale("en", "US"));
        System.setProperty("user.language","en-US");
        
        try {
           
           boolean isSet = false;
           for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                   UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    

   
                   // javax.swing.UIManager.setLookAndFeel(info.getClassName());
                   isSet = true;
                   break;
                } 
           }   
           if (!isSet)
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
          
        } catch (InstantiationException ex) {
 
           
        } catch (IllegalAccessException ex) {
       
        } catch (javax.swing.UnsupportedLookAndFeelException ex) { 
      
        }

        UIManager.put("ScrollBar.background", new ColorUIResource(AppUI.getColor0()));
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
            
            if (ps.currentScreen == ProgramState.SCREEN_ECHO_RAIDA) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_ECHO_RAIDA_FINISHED;
                        showScreen();
                    }
                });
            }
	}  
    }

    class UnpackerSenderCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "Unpacker (Sender) finished");
            
            final Object fresult = result;
            final UnpackerResult ur = (UnpackerResult) fresult;

            if (ur.status == UnpackerResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        if (!ur.errText.isEmpty())
                            ps.errText = ur.errText;
                        else  
                            ps.errText = "Failed to Unpack file(s). Please check the logs";

                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                
                return;
            }
            
            int sn = sm.getActiveWallet().getIDCoin().sn;
            wl.debug(ltag, "UnpackerForSender sn=" + sn);

            setRAIDAProgressCoins(0, 0, 0);
            sm.startSenderService(sn, null, 0, ps.typedMemo, sm.getActiveWallet().getName(), new SenderDepositCb());

        }
    }
    
    class UnpackerCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "Unpacker finisheed");
            
            final Object fresult = result;
            final UnpackerResult ur = (UnpackerResult) fresult;

            if (ur.status == UnpackerResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        if (!ur.errText.isEmpty())
                            ps.errText = ur.errText;
                        else  
                            ps.errText = "Failed to Unpack file(s). Please check the logs";

                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                
                return;
            }

            ps.duplicates = ur.duplicates;
            ps.failedFiles = ur.failedFiles;
            
            setRAIDAProgressCoins(0, 0, 0);
            sm.startAuthenticatorService(new AuthenticatorCb());
        }
    }
    
    class AuthenticatorForSkyCoinCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "AuthenticatorSkyCoin finished");
            
            final Object fresult = result;
            if (isWithdrawing())
                ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
            else
                ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
            
            final AuthenticatorResult ar = (AuthenticatorResult) fresult;
            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Failed to Authencticate SkyID Coin";
                        showScreen();
                    }
                });
                ps.isCheckingSkyID = false;
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                ps.isCheckingSkyID = false;
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_CANCELLED) {
                ps.isCheckingSkyID = false;
                return;
            }

            setRAIDAProgressCoins(ar.totalRAIDAProcessed, 0, 0);
	}
    }
    

    class AuthenticatorCb implements CallbackInterface {
	public void callback(Object result) {
            wl.debug(ltag, "Authenticator finished");
            
            final Object fresult = result;
            final AuthenticatorResult ar = (AuthenticatorResult) fresult;
            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        if (!ar.errText.isEmpty())
                            ps.errText = "<html><div style='text-align:center; width: 520px'>" + ar.errText + "</div></html>";
                        else
                            ps.errText = "Failed to Authencticate Coins";

                        //ps.errText = "Failed to Authencticate Coins";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                sm.startGraderService(new GraderCb(), ps.duplicates, null);
                return;
            } else if (ar.status == AuthenticatorResult.STATUS_CANCELLED) {
                sm.resumeAll();
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                return;
            }

            setRAIDAProgressCoins(ar.totalRAIDAProcessed, ar.totalCoinsProcessed, ar.totalCoins);
            //setRAIDAProgress(ar.totalRAIDAProcessed, ar.totalFilesProcessed, ar.totalFiles);
	}
    }
    
    class GraderCb implements CallbackInterface {
	public void callback(Object result) {
            GraderResult gr = (GraderResult) result;

            ps.statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
            ps.statFailedValue = gr.totalCounterfeitValue;
            ps.statLostValue = gr.totalLostValue;
            ps.statToBank = gr.totalAuthentic + gr.totalFracked;
            ps.statFailed = gr.totalCounterfeit;
            ps.statLost = gr.totalLost + gr.totalUnchecked;
            ps.receiptId = gr.receiptId;
                     
            Wallet w = sm.getActiveWallet();
            if (ps.statToBankValue != 0) {
                Wallet wsrc = ps.srcWallet;
                if (wsrc != null && wsrc.isSkyWallet()) {
                    wl.debug(ltag, "Appending sky transactions");
                      
                    StringBuilder nsb = new StringBuilder();
                    int wholeTotal = 0;
                    
                    Enumeration<String> enumeration = ps.cenvelopes.keys();
                    while (enumeration.hasMoreElements()) {
                        String key = enumeration.nextElement();
                        String[] data = ps.cenvelopes.get(key);

                        int total = 0;
                        try {
                            total = Integer.parseInt(data[1]);
                        } catch (NumberFormatException e) {
                            wl.error(ltag, "Failed to parse number " + e.getMessage());
                            continue;
                        }
 
                        wholeTotal += total;
                        if (!nsb.toString().equals(""))
                            nsb.append(",");
                                    
                        nsb.append(data[0]);
                    }

                    if (ps.typedMemo.isEmpty()) {
                        ps.dstWallet.appendTransaction(nsb.toString(), wholeTotal, ps.receiptId); 
                    } else {
                        ps.dstWallet.appendTransaction(ps.typedMemo, ps.statToBankValue, ps.receiptId); 
                    }
                    
                } else {
                    w.appendTransaction(ps.typedMemo, ps.statToBankValue, ps.receiptId);
                }
            } else {
                // StatToBank == 0
                String memo = "";
                if (ps.statFailedValue > 0) {
                    memo = AppCore.formatNumber(ps.statFailedValue) + " Counterfeit";                   
                } else {
                    memo = "Failed to Import";
                }
                w.appendTransaction(memo, Config.NEGATIVE_AMOUNT_FOR_COUNTERFEIT, "dummy");
            }
            
            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    pbarText.setText("Fixing fracked coins ...");
                    pbarText.repaint();
                }
            });

            sm.startFrackFixerService(new FrackFixerCb());           
	}
    }

    class FrackFixerCb implements CallbackInterface {
	public void callback(Object result) {
            FrackFixerResult fr = (FrackFixerResult) result;
            
            if (fr.status == FrackFixerResult.STATUS_PROCESSING) {
                wl.debug(ltag, "Processing coin");
                //setRAIDAFixingProgress(fr.totalRAIDAProcessed, fr.totalFilesProcessed, fr.totalFiles, fr.fixingRAIDA, fr.round);
                setRAIDAFixingProgressCoins(fr.totalRAIDAProcessed, fr.totalCoinsProcessed, fr.totalCoins, fr.fixingRAIDA, fr.round);
		return;
            }

            if (fr.status == FrackFixerResult.STATUS_ERROR) {
                ps.errText = "Failed to fix coins";
                wl.error(ltag, "Failed to fix");
            }
            
            if (fr.status == FrackFixerResult.STATUS_CANCELLED) {
                wl.error(ltag, "Frack cancelled");
                sm.resumeAll();
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        if (isWithdrawing())
                            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        else
                            ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                return;
            }

            if (fr.status == FrackFixerResult.STATUS_FINISHED) {
		if (fr.fixed + fr.failed > 0) {
                    wl.debug(ltag, "Fracker fixed: " + fr.fixed + ", failed: " + fr.failed);
		}
            }

            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    pbarText.setText("Recovering lost coins ...");
                    pbarText.repaint();
                }
            });
    
            sm.startLossFixerService(new LossFixerCb());
        }
    }
    
    
    class FrackFixerOnPurposeCb implements CallbackInterface {
	public void callback(Object result) {
            FrackFixerResult fr = (FrackFixerResult) result;

            ps.statTotalFracked = fr.failed + fr.fixed;
            ps.statTotalFixed = fr.fixed;
            ps.statFailedToFix = fr.failed;
                      
            ps.statTotalFrackedValue = fr.totalCoins;
            ps.statTotalFixedValue = fr.fixedValue;
            
            if (fr.status == FrackFixerResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "<html><div style='text-align:center; width: 520px'>Failed to Fix Coins. Please see logs at<br> " + AppCore.getLogPath() + "</div></html>";
                        ps.currentScreen = ProgramState.SCREEN_FIX_DONE;
                        showScreen();
                    }
                });
                return;
            } else if (fr.status == FrackFixerResult.STATUS_FINISHED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        pbarText.setText("Recovering lost coins ...");
                        pbarText.repaint();
                    }
                });
    
                sm.startLossFixerService(new LossFixerCb());
                return;
            } else if (fr.status == FrackFixerResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_FIX_DONE;
                        showScreen();
                    }
                });
                return;
            }

            //setRAIDAFixingProgress(fr.totalRAIDAProcessed, fr.totalFilesProcessed, fr.totalFiles, fr.fixingRAIDA, fr.round);
            setRAIDAFixingProgressCoins(fr.totalRAIDAProcessed, fr.totalCoinsProcessed, fr.totalCoins, fr.fixingRAIDA, fr.round);
        }
    }
    
         
    class ExporterCb implements CallbackInterface {
	public void callback(Object result) {
            final Object eresult = result;
            final ExporterResult er = (ExporterResult) eresult;
            
            if (er.status == ExporterResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!er.errText.isEmpty()) {
                            if (er.errText.equals(Config.PICK_ERROR_MSG)) {
                                if (ps.triedToChange) {
                                    //ps.errText = getPickError(ps.srcWallet);
                                    ps.errText = "Failed to change coins";
                                } else { 
                                    ps.changeFromExport = true;
                                    ps.triedToChange = true;
                                    ps.currentScreen = ProgramState.SCREEN_MAKING_CHANGE;
                                    showScreen();
                                    return;
                                }
                                //ps.errText = getPickError(ps.srcWallet);
                            } else {
                                ps.errText = er.errText;
                            }
                        } else {
                            ps.errText = "Failed to export coins";
                        }
                        
                        showScreen();
                    }
                });
                
                return;
            }

            if (er.status == ExporterResult.STATUS_FINISHED) {
                if (er.totalExported != ps.typedAmount) {
                    EventQueue.invokeLater(new Runnable() {         
                        public void run() {
                            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                            ps.errText = "Some of the coins were not exported";
                            showScreen();
                        }
                    });
                }
                
                sm.getActiveWallet().appendTransaction(ps.typedMemo, er.totalExported * -1, er.receiptId);
                sm.getActiveWallet().setNotUpdated();
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
                        //ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                    }
                });
                
		return;
            }
	}
    }
    
    class ExporterBackupCb implements CallbackInterface {
	public void callback(Object result) {
            final Object eresult = result;
            final ExporterResult er = (ExporterResult) eresult;
            
            if (er.status == ExporterResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_BACKUP_DONE;
                        if (!er.errText.isEmpty())
                            ps.errText = er.errText;
                        else
                            ps.errText = "Failed to backup coins";
                        
                        showScreen();
                    }
                });
                
                return;
            }

            if (er.status == ExporterResult.STATUS_FINISHED) {              
                if (ps.srcWallet.isEncrypted()) {
                    wl.debug(ltag, "Ecrypting back");
                    sm.startVaulterService(new VaulterCb());
                    return;
                }
                
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_BACKUP_DONE;
                        showScreen();
                    }
                });
                
		return;
            }
	}
    }
    
    
    class VaulterCb implements CallbackInterface {
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            if (vresult.status != VaulterResult.STATUS_FINISHED)
                ps.errText = "Failed to decrypt/encrypt coins";
            
            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    if (isDepositing()) {
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                    } else if (isWithdrawing() || isMakingChange()) {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    } else if (isFixing()) {
                        ps.currentScreen = ProgramState.SCREEN_FIX_DONE;
                    } else if (isBackupping()) {
                        ps.currentScreen = ProgramState.SCREEN_BACKUP_DONE;
                    } 
                    
                    showScreen();
                }
            });
	}
    }
    
    class LossFixerCb implements CallbackInterface {
	public void callback(final Object result) {
            LossFixerResult lr = (LossFixerResult) result;
            
            if (lr.status == LossFixerResult.STATUS_PROCESSING) {
                wl.debug(ltag, "Processing lossfixer");
                return;
            }
            
            if (lr.status == LossFixerResult.STATUS_CANCELLED) {
                ps.errText = "Operation Cancelled";
                sm.resumeAll();
            }
            
            wl.debug(ltag, "LossFixer finished");
            
            
            if (ps.coinIDinFix != null) {
                wl.debug(ltag, "We are fixing coin id: " + ps.coinIDinFix.getName());
                ps.coinIDinFix.setNotUpdated();
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        wl.debug(ltag, "Looking for: " + ps.srcWallet.getName() + " sn: " + ps.coinIDinFix.getIDCoin().sn);
           
                        boolean fixed = true;
                        CloudCoin cc = AppCore.findCoinBySN(Config.DIR_BANK, ps.srcWallet.getName(), ps.coinIDinFix.getIDCoin().sn);
                        if (cc == null) {
                            wl.debug(ltag, "Failed to fix. Trying to move the coin back");
                            cc = AppCore.findCoinBySN(Config.DIR_FRACKED, ps.srcWallet.getName(), ps.coinIDinFix.getIDCoin().sn);
                            if (cc == null) {
                                ps.currentScreen = ProgramState.SCREEN_WITHDRAW;
                                ps.errText = "Failed to find fixed coin. Please, check main.log file";
                                showScreen();
                                return;
                            }
                            
                            fixed = false;
                        }
                        
                        wl.debug(ltag, "Found cc: " + cc.originalFile);
                        
                        if (!AppCore.moveToFolderNewName(cc.originalFile, AppCore.getIDDir(), null, ps.coinIDinFix.getName() + ".stack")) {
                            ps.currentScreen = ProgramState.SCREEN_WITHDRAW;
                            ps.errText = "Failed to move ID Coin. Please, check main.log file";
                            showScreen();
                            return;
                        }
                        
                        ps.currentScreen = ProgramState.SCREEN_WITHDRAW;
                        if (fixed)
                            ps.errText = "ID Coin has been fixed. Please try again";
                        else
                            ps.errText = "Failed to fix ID Coin. Please try again later";
                        
                        ps.coinIDinFix.setIDCoin(cc);
                        showScreen();   
                    }
                });
                return;
            }
            
            if (lr.recovered > 0) {
                sm.getActiveWallet().appendTransaction("LossFixer Recovered", lr.recoveredValue, lr.receiptId);
            }
            

            if (sm.getActiveWallet().isEncrypted()) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        pbarText.setText("Encrypting coins ...");
                        pbarText.repaint();
                    }
                });
                
                sm.startVaulterService(new VaulterCb());
            } else {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        if (isFixing()) {
                            ps.currentScreen = ProgramState.SCREEN_FIX_DONE;
                        } else {
                            if (isWithdrawing())
                                ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                            else
                                ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        }
                        showScreen();
                    }
                });
            }
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

            if (er.status == EraserResult.STATUS_ERROR) {              
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Erasing failed. Please check the logs";
                        ps.currentScreen = ProgramState.SCREEN_CLEAR_DONE;
                        showScreen();
                    }
                }); 
                
                return;
            }
            
            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    eraserGoNext();
                }
            });         
	}
    }
    
    class SenderCb implements CallbackInterface {
	public void callback(Object result) {
            final SenderResult sr = (SenderResult) result;
            
            wl.debug(ltag, "Sender finished: " + sr.status);
            if (sr.status == SenderResult.STATUS_PROCESSING) {
                setRAIDATransferProgressCoins(sr.totalRAIDAProcessed, sr.totalCoinsProcessed, sr.totalCoins);
                return;
            }
            
            
            if (sr.status == SenderResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        sm.resumeAll();
                        showScreen();
                    }
                });
                return;
            }
      
            if (sr.status == SenderResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!sr.errText.isEmpty()) {
                            if (sr.errText.equals(Config.PICK_ERROR_MSG)) {
                                if (ps.triedToChange) {
                                    ps.errText = "Failed to change coins";
                                } else { 
                                    ps.changeFromExport = false;
                                    ps.triedToChange = true;
                                    ps.currentScreen = ProgramState.SCREEN_MAKING_CHANGE;
                                    showScreen();
                                    return;
                                }
                                //ps.errText = getPickError(ps.srcWallet);
                            } else {
                                ps.errText = "<html><div style='text-align:center; width: 520px'>" + sr.errText + "</div></html>";
                                //ps.errText = sr.errText;
                            }
                        } else {
                            ps.errText = "Error occurred. Please check the logs";
                        }
                        
                        showScreen();
                    }
                });
		return;
            }   
            
            if (sr.amount > 0) {
                wl.debug(ltag, "sramount " + sr.amount + " typed " + ps.typedAmount);
                
                AppCore.appendSkySentCoinTransaction(ps.srcWallet.getName(), 
                        ps.dstWallet.getName(), ps.dstWallet.getIDCoin().sn, sr.amount, ps.typedMemo);
                
                if (ps.typedAmount != sr.amount) {
                    ps.typedAmount = sr.amount;
                    ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    ps.errText = "Not all coins were sent. Please check the logs";
                    showScreen();
                    return;
                }
                
                ps.typedAmount = sr.amount;
                
                Wallet srcWallet = ps.srcWallet;
                Wallet dstWallet = ps.dstWallet;
                
                srcWallet.appendTransaction(ps.typedMemo, sr.amount * -1, sr.receiptId);
                if (dstWallet != null) {
                    dstWallet.appendTransaction(ps.typedMemo, sr.amount, sr.receiptId);
                    if (dstWallet.isEncrypted()) {
                        sm.changeServantUser("Vaulter", dstWallet.getName());
                        sm.startVaulterService(new VaulterCb(), dstWallet.getPassword());
                    } else {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                    }
                } else {
                    ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    showScreen();
                }
                
                return;
            } else {
                ps.typedAmount = 0;
                ps.errText = "Coins were not sent. Please check the logs";     
            }
            
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    showScreen();
                }
            });              
	}
    }
    
    class SenderDepositCb implements CallbackInterface {
	public void callback(Object result) {
            final SenderResult sr = (SenderResult) result;
            
            wl.debug(ltag, "Sender (Depostit) finished: " + sr.status);
            if (sr.status == SenderResult.STATUS_PROCESSING) {
                //setRAIDAProgress(sr.totalRAIDAProcessed, sr.totalFilesProcessed, sr.totalFiles);
                setRAIDAProgressCoins(sr.totalRAIDAProcessed, sr.totalCoinsProcessed, sr.totalCoins);
                return;
            }
            
            
            if (sr.status == SenderResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        sm.resumeAll();
                        showScreen();
                    }
                });
                return;
            }
      
            if (sr.status == SenderResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        if (!sr.errText.isEmpty())
                            ps.errText = "<html><div style='text-align:center; width: 520px'>" + sr.errText + "</div>";
                            //ps.errText = sr.errText;
                        else  
                            ps.errText = "Error occurred. Please check the logs";
                        
                        showScreen();
                    }
                });
		return;
            }   
           
            ps.statToBankValue = sr.totalAuthenticValue + sr.totalFrackedValue;
            ps.statToBank = sr.totalAuthentic + sr.totalFracked;
            ps.statFailed = sr.totalCounterfeit + sr.totalUnchecked;
            ps.statLost = 0;
            
            ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
            showScreen();              
	}
    }
    
    class ShowEnvelopeCoinsForReceiverCb implements CallbackInterface {
	public void callback(Object result) {
            ShowEnvelopeCoinsResult er = (ShowEnvelopeCoinsResult) result;
 
            ps.cenvelopes = er.envelopes;            
            wl.debug(ltag, "Sending from sc to " + ps.dstWallet.getName());
            
            Thread t = new Thread(new Runnable() {
                public void run(){
                    sm.transferCoins(ps.srcWallet.getName(), ps.dstWallet.getName(), 
                        ps.typedAmount, ps.typedMemo, ps.typedRemoteWallet, new SenderCb(), new ReceiverCb());
                }
            });
        
            t.start();
        }
    }
      
    class ReceiverCb implements CallbackInterface {
	public void callback(Object result) {
            final ReceiverResult rr = (ReceiverResult) result;
            
            
            ps.receiverReceiptId = rr.receiptId;
            
            wl.debug(ltag, "Receiver finished: " + rr.status);
            if (rr.status == ReceiverResult.STATUS_PROCESSING) {
                setRAIDATransferProgressCoins(rr.totalRAIDAProcessed, rr.totalCoinsProcessed, rr.totalCoins);
                return;
            }

            if (rr.status == ReceiverResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        sm.resumeAll();
                        showScreen();
                    }
                });
                return;
            }
      
            if (rr.status == ReceiverResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!rr.errText.isEmpty()) {
                            if (rr.errText.equals(Config.PICK_ERROR_MSG)) {
                                ps.errText = getPickError(ps.srcWallet);
                            } else {
                                ps.errText = "<html><div style='text-align:center; width: 520px'>" + rr.errText + "</div></html>";
                            }
                        }
                        else  
                            ps.errText = "Error occurred. Please check the logs";
                        
                        showScreen();
                    }
                });
		return;
            } 
            
            if (rr.amount <= 0) {
                ps.typedAmount = 0;
                ps.errText = "Coins were not received. Please check the logs";
                ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                showScreen();
                return;
            }

            String name = null;
            if (ps.srcWallet != null)
                name = ps.srcWallet.getName();
            
            ps.needExtra = rr.needExtra;
            ps.rrAmount = rr.amount;
            
            wl.debug(ltag, "rramount " + rr.amount + " typed " + ps.typedAmount + " name=" + name);
            sm.startGraderService(new GraderCb(), null, name);
	}
    }
    
    class TransferCb implements CallbackInterface {
	public void callback(Object result) {
            final TransferResult tr = (TransferResult) result;

            wl.debug(ltag, "Transfer finished: " + tr.status);
            if (tr.status == TransferResult.STATUS_PROCESSING) {
                setRAIDATransferProgressCoins(tr.totalRAIDAProcessed, tr.totalCoinsProcessed, tr.totalCoins);
                return;
            }

            if (tr.status == TransferResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        sm.resumeAll();
                        showScreen();
                    }
                });
                return;
            }
      
            if (tr.status == ReceiverResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!tr.errText.isEmpty()) {
                            if (tr.errText.equals(Config.PICK_ERROR_MSG)) {
                                ps.errText = getPickError(ps.srcWallet);
                            } else {
                                ps.errText = tr.errText;
                            }
                        }
                        else  
                            ps.errText = "Error occurred. Please check the logs";
                        
                        showScreen();
                    }
                });
		return;
            } 
            
            String dstName;
            int sn;
            
            if (ps.dstWallet != null) {
                sn = ps.dstWallet.getIDCoin().sn;
                dstName = ps.dstWallet.getName();
            } else {
                sn = ps.foundSN;
                dstName = ps.typedRemoteWallet;
            }
            
            AppCore.appendSkySentCoinTransaction(ps.srcWallet.getName(), dstName, sn, ps.typedAmount, ps.typedMemo);
            
            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
            showScreen();
	}
    }
    
    class optRv {
        int[] idxs;
        String[] options;
    }
   
}


