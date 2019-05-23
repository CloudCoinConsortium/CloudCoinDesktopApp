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
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoinsResult;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.Unpacker.UnpackerResult;
import global.cloudcoin.ccbank.Vaulter.Vaulter;
import global.cloudcoin.ccbank.Vaulter.VaulterResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.MenuItemUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import org.json.JSONException;
import org.json.JSONObject;





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
    JLabel depositText, transferText;
    
    JFrame mainFrame;
    
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
        home += File.separator + "DebugX";
        
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
       
    
    public void setSNs(int[] sns) {
        Wallet w = wallets[ps.currentWalletIdx];
        
        w.setSNs(sns);
    }
    
    public void showCoinsDone(int[][] counters) {
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
    
    
    public void setTotalCoins() {
        int total = 0;
        for (int i = 0; i < wallets.length; i++) {
            total += wallets[i].getTotal();
            totalText.setText("" + total);
        }
        
        totalText.repaint();
    }
    
    public void showCoinsGoNext() {
        if (wallets.length > ps.currentWalletIdx + 1) {
            ps.currentWalletIdx++;
            
            if (!wallets[ps.currentWalletIdx].isSkyWallet()) {
                sm.changeServantUser("ShowCoins", wallets[ps.currentWalletIdx].getName());
                sm.startShowCoinsService(new ShowCoinsCb());
            } else {
                sm.changeServantUser("ShowEnvelopeCoins", wallets[ps.currentWalletIdx].getParent().getName());
                sm.startShowSkyCoinsService(new ShowEnvelopeCoinsCb(), wallets[ps.currentWalletIdx].getIDCoin().sn);
            }         
        } else {
            ps.isShowCoinsFinished = true;
        }
        
        setTotalCoins();
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
    
        mainFrame = AppUI.getMainFrame();
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
        if (ps.currentScreen == ProgramState.SCREEN_DEPOSIT ||
                ps.currentScreen == ProgramState.SCREEN_IMPORTING ||
                ps.currentScreen == ProgramState.SCREEN_IMPORT_DONE)
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
        ImageIcon ii;
        try {
            Image img;
                   
            img = ImageIO.read(getClass().getClassLoader().getResource("resources/Gear icon.png"));
            icon0 = new JLabel(new ImageIcon(img));
            
            ii = new ImageIcon(img);
            
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
            AppUI.alignTop(titleText);
            AppUI.setTitleFont(titleText, 16);
            AppUI.setSize(titleText, 40, 40);
            
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
            b0.setBorderPainted(false);
            AppUI.noOpaque(b0);
            AppUI.setTitleBoldFont(b0, 32);
            AppUI.setHandCursor(b0);
            if (isDepositing()) {
           //     AppUI.underLine(b0);
            } else {
          //      AppUI.noUnderLine(b0);
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
        }

        c.weightx = 1;
        JLabel padd = new JLabel();
        gridbag.setConstraints(padd, c);
        p.add(padd);
        
        
        
        
        
        
        
        
        
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.weightx = 0;
       // c.weighty = 1;
        c.fill = GridBagConstraints.NORTH;
        
        // Icon Gear
      //  AppUI.noOpaque(icon0);
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
 
        String[] items = {"Backup", "List serials", "Clear History" };
        for (int i = 0; i < items.length; i++) {
            JMenuItem menuItem = new JMenuItem(items[i]);
            menuItem.setActionCommand("" + i);
    
            MouseAdapter ma = new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
                    JMenuItem jMenuItem = (JMenuItem) evt.getSource();
                    jMenuItem.setBackground(AppUI.getColor0());
                }
                
                public void mouseExited(MouseEvent evt) {
                    JMenuItem jMenuItem = (JMenuItem) evt.getSource();
                    jMenuItem.setBackground(AppUI.getColor6());
                }
                
                public void mouseReleased(MouseEvent evt) {
                    JMenuItem jMenuItem = (JMenuItem) evt.getSource();
                    popupMenu.setVisible(false);
                    
                    String action = jMenuItem.getActionCommand();
                    if (action.equals("0")) {
                        ps.currentScreen = ProgramState.SCREEN_BACKUP;
                    } else if (action.equals("1")) {
                        ps.currentScreen = ProgramState.SCREEN_LIST_SERIALS;
                    } else if (action.equals("2")) {
                        ps.currentScreen = ProgramState.SCREEN_CLEAR;
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
                
            }
            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                AppUI.setBackground(ficon, savedColor);
                ficon.setOpaque(false);
                ficon.repaint();
            }
            
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
                AppUI.setBackground(ficon, savedColor);
                ficon.setOpaque(false);
                ficon.repaint();
            }
        });
        
        
        
        
        
        
        icon0.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                ficon.setOpaque(true);
                AppUI.setBackground(ficon, AppUI.getColor6());

                ficon.repaint();              
                popupMenu.show(ficon, 0 - mWidth  + ficon.getWidth(), ficon.getHeight());
            }
        });
        
        
        // Icon Support
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
        
        
        
        // Icon lang
        c.insets = new Insets(0, 10, 0, 20); 
        AppUI.noOpaque(icon2);
        AppUI.setHandCursor(icon2);
        gridbag.setConstraints(icon2, c);
        p.add(icon2);
 
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
        clear();
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
                ps.isUpdatedWallets = false;
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
                showTransferScreen();
                break;
            case ProgramState.SCREEN_IMPORTING:
                showImportingScreen();
                break;
            case ProgramState.SCREEN_IMPORT_DONE:
                ps.isUpdatedWallets = false;
                showImportDoneScreen();
                break;
            case ProgramState.SCREEN_SUPPORT:
                resetState();
                showSupportScreen();
                break;
            case ProgramState.SCREEN_CONFIRM_TRANSFER:
                showConfirmTransferScreen();
                break;
            case ProgramState.SCREEN_TRANSFER_DONE:
                ps.isUpdatedWallets = false;
                showTransferDoneScreen();
                break;
            case ProgramState.SCREEN_BACKUP:
                showBackupScreen();
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
                
        }
        
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
    }
    
    private void setRAIDATransferProgress(int raidaProcessed, int totalFilesProcessed, int totalFiles) {
        pbar.setVisible(true);
        pbar.setValue(raidaProcessed);
        
        transferText.setText(totalFilesProcessed + " / " + totalFiles + " Transferred");
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
        
        // Password Label
        JLabel x = new JLabel("<html><div style='width:480px;text-align:center'>Do not close the application until all CloudCoins are transferred!</div></html>");
        AppUI.setCommonFont(x);
        AppUI.setColor(x, AppUI.getErrorColor());
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(20, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        transferText = new JLabel("");
        AppUI.setCommonFont(transferText);
        c.insets = new Insets(40, 20, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        gridbag.setConstraints(transferText, c);
        ct.add(transferText);
        
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
                sm.cancel("Sender");

                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
       
        subInnerCore.add(bp);  
        
        Thread t = new Thread(new Runnable() {
            public void run(){
                pbar.setVisible(false);
                if (!ps.isEchoFinished) {
                    transferText.setText("Checking RAIDA ...");
                    transferText.repaint();
                }
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                
                while (!ps.isEchoFinished) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                }
                

                String dstName =  (ps.foundSN == 0) ? ps.dstWallet.getName() : "" + ps.foundSN;

                wl.debug(ltag, "Sending to dst " + dstName);
                sm.transferCoins(ps.srcWallet.getName(), dstName, 
                        ps.typedAmount, ps.typedMemo,  new SenderCb(), new ReceiverCb());
            }
        });
        
        t.start();
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
        //JLabel x = new JLabel("Do not close the application until all CloudCoins are deposited!");
        JLabel x = new JLabel("<html><div style='width:480px;text-align:center'>Do not close the application until all CloudCoins are deposited!</div></html>");
        AppUI.setCommonFont(x);
        //AppUI.setBoldFont(x, 16);
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
                while (!ps.isEchoFinished || !ps.isShowCoinsFinished) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}

                }
                
                ps.dstWallet.setPassword(ps.typedPassword);
                sm.setActiveWalletObj(ps.dstWallet);
                
                depositText.setText("Moving coins ...");
                for (String filename : ps.files) {
                    AppCore.moveToFolder(filename, Config.DIR_IMPORT, sm.getActiveWallet().getName());
                }

                depositText.setText("Unpacking coins ...");
                depositText.repaint();
                sm.startUnpackerService(new UnpackerCb());
            }
        });
        
        t.start();
        
    }
    
    public void showBackupDoneScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
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
        
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>" +
            "Your CloudCoins from " + ps.srcWallet.getName() + " have been backed up into" +
            " " + ps.chosenFile + "</div></html>");
          
        AppUI.setCommonFont(x);
 
        c.insets = new Insets(0, 0, 4, 0);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;
        gridbag.setConstraints(x, c);
        ct.add(x);
        
        JPanel bp = getTwoButtonPanelCustom("Show Folder", "Continue", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetState();
                ps.currentScreen = ProgramState.SCREEN_WITHDRAW;
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
    
    public void showTransferDoneScreen() {
        boolean isError = !ps.errText.equals("");
        JPanel subInnerCore;
        
        if (isError) {
            subInnerCore = getModalJPanel("Error");
            AppUI.hr(subInnerCore, 32);
            maybeShowError(subInnerCore);
            return;
        }
 
        
        
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
        
        
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>"
                + "<b>" + AppCore.formatNumber(ps.typedAmount) + " CC</b> have been transferred to <b>" + to + "</b> from <b>"
                + ps.srcWallet.getName() + "</b></div></html>");
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
                ps.currentScreen = ProgramState.SCREEN_DEFAULT;
                showScreen();
            }
        });
  
        
        subInnerCore.add(bp);  
        
        
        
    }
    
    public void showImportDoneScreen() {
        JPanel subInnerCore = getModalJPanel("Deposit Complete");
        maybeShowError(subInnerCore);
        
        JPanel ct = new JPanel();
        AppUI.noOpaque(ct);
        subInnerCore.add(ct);
        
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();      
        ct.setLayout(gridbag);
        
        String total = AppCore.formatNumber(ps.statToBankValue);
        String totalBank = AppCore.formatNumber(ps.statToBank);
        String totalFailed = AppCore.formatNumber(ps.statFailed);
        
        JLabel x = new JLabel("<html><div style='width:400px; text-align:center'>Deposited <b>" +  total +  " CloudCoins</b> to <b>" + ps.dstWallet.getName() + " </b></div></html>");
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
                resetState();
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

        x = new JLabel(ps.srcWallet.getName());
        AppUI.setCommonBoldFont(x);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++;
        
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
        c.gridx = GridBagConstraints.RELATIVE;;
        c.gridy = y;
        gridbag.setConstraints(x, c);
        ct.add(x);
               
        y++;
        
        String memo = ps.typedMemo;
        if (memo.length() > 24) {
            memo = to.substring(0, 24) + "...";
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
                        sm.startSecureExporterService(Config.TYPE_STACK, ps.typedAmount, Config.DEFAULT_TAG, ps.chosenFile, new ExporterCb());
                    } else {
                        sm.startExporterService(Config.TYPE_STACK, ps.typedAmount, Config.DEFAULT_TAG, ps.chosenFile, new ExporterCb());
                    }
                } else if (ps.sendType == ProgramState.SEND_TYPE_WALLET) {
                    ps.dstWallet.setPassword(ps.typedDstPassword);              
                    ps.currentScreen = ProgramState.SCREEN_SENDING;
                    showScreen();
                } else if (ps.sendType == ProgramState.SEND_TYPE_REMOTE) {              
                    DNSSn d = new DNSSn(ps.typedRemoteWallet, wl);
                    int sn = d.getSN();
                    if (sn < 0) {
                        ps.errText = "Failed get receiver. Check that the name is valid";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                        return;
                    }
                    
                    ps.foundSN = sn;
                    
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
       
        if (ps.isUpdatedWallets) {
            for (int i = 0; i < wallets.length; i++) {
                JLabel cntLabel = (JLabel) wallets[i].getuiRef();
                String strCnt = AppCore.formatNumber(wallets[i].getTotal());
                cntLabel.setText(strCnt);
            }
            
            setTotalCoins();
            return;
        }

        ps.isUpdatedWallets = true;
        
        if (wallets.length > 0) {
            ps.currentWalletIdx = 0;
            if (!wallets[ps.currentWalletIdx].isSkyWallet()) {
                sm.changeServantUser("ShowCoins", wallets[ps.currentWalletIdx].getName());
                sm.startShowCoinsService(new ShowCoinsCb());
            } else {
                sm.changeServantUser("ShowEnvelopeCoins", wallets[ps.currentWalletIdx].getParent().getName());
                sm.startShowSkyCoinsService(new ShowEnvelopeCoinsCb(), wallets[ps.currentWalletIdx].getIDCoin().sn);
            }      
        }
    }
    
    public void showDefaultScreen() {
        showLeftScreen();
        
        JPanel rightPanel = getRightPanel();     
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
 
        int nonSkyCnt = 0;
        for (int i = 0; i < wallets.length; i++)
            if (!wallets[i].isSkyWallet())
                nonSkyCnt++;
        
        /*
        final String[] nonSkyOptions = new String[nonSkyCnt];
        int j = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!wallets[i].isSkyWallet()) {
                nonSkyOptions[j] = wallets[i].getName() + " - " + AppCore.formatNumber(wallets[i].getTotal()) + " CC";
                j++;
            }
        }
        */
        final String[] options = new String[wallets.length];
        for (int i = 0; i < wallets.length; i++) {
            options[i] = wallets[i].getName() + " - " + AppCore.formatNumber(wallets[i].getTotal()) + " CC";
        }
        
      
        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", options);
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

        final RoundedCornerComboBox cboxto = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", options);
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
        final JLabel rwText = new JLabel("Remote Wallet");
        gridbag.setConstraints(rwText, c);
        AppUI.setCommonFont(rwText);
        oct.add(rwText);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        final MyTextField remoteWalledId = new MyTextField("Remote Wallet", false);
        gridbag.setConstraints(remoteWalledId.getTextField(), c);
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
        
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
     
        final MyTextField localFolder = new MyTextField("Select Folder", false, true);
        localFolder.setFilepickerListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {       
                    ps.chosenFile = chooser.getSelectedFile().getAbsolutePath();
                    localFolder.setData(chooser.getSelectedFile().getName());
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
                /*
                if (srcWallet.isSkyWallet()) {
                    if (ps.isDstBoxFull) {
                        cboxto.setOptions(nonSkyOptions);
                        ps.isDstBoxFull = false;
                    }
                } else {
                    if (!ps.isDstBoxFull) {
                        cboxto.setOptions(options);
                        ps.isDstBoxFull = true;
                    }
                }
                */
            }
        });
        
        cboxto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int dstIdx = cboxto.getSelectedIndex() - 1;
                
                localFolder.getTextField().setVisible(false);
                lfText.setVisible(false);
                
                if (dstIdx == wallets.length) {
                    remoteWalledId.getTextField().setVisible(true);
                    rwText.setVisible(true);
                    
                    passwordDst.getTextField().setVisible(false);
                    dpText.setVisible(false);
                    
                    localFolder.getTextField().setVisible(false);
                    lfText.setVisible(false);
                    
                    return;
                } 
                
                remoteWalledId.getTextField().setVisible(false);
                rwText.setVisible(false);
                                   
                if (dstIdx == wallets.length + 1) {    
                    passwordDst.getTextField().setVisible(false);
                    dpText.setVisible(false);
                    
                    localFolder.getTextField().setVisible(true);
                    lfText.setVisible(true);
                    
                    return;
                }
   
                if (dstIdx < 0 || dstIdx >= wallets.length) 
                    return;
                           
                Wallet dstWallet = wallets[dstIdx];             
                if (dstWallet == null)
                    return;
                
                if (dstWallet.isEncrypted()) {  
                    passwordDst.getTextField().setVisible(true);
                    dpText.setVisible(true);
                } else {
                    passwordDst.getTextField().setVisible(false);
                    dpText.setVisible(false);
                }
                /*
                 if (dstWallet.isSkyWallet()) {
                    if (ps.isSrcBoxFull) {
                        cboxfrom.setOptions(nonSkyOptions);
                        ps.isSrcBoxFull = false;
                    }
                } else {
                    if (!ps.isSrcBoxFull) {
                        cboxfrom.setOptions(options);
                        ps.isSrcBoxFull = true;
                    }
                }
                */
            }
        });

        // Space
        AppUI.hr(oct, 22);
        
        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                ps.typedMemo = memo.getText();
                if (ps.typedMemo.isEmpty())
                    ps.typedMemo = "Transfer";
                
                int srcIdx = cboxfrom.getSelectedIndex() - 1;
                int dstIdx = cboxto.getSelectedIndex() - 1;
                
                if (srcIdx < 0 || srcIdx >= wallets.length || dstIdx < 0 || dstIdx >= wallets.length + 2) {
                    ps.errText = "Please select from and to Wallet";
                    showScreen();
                    return;
                }
                
                if (srcIdx == dstIdx) {
                    ps.errText = "You can not transfer to the same wallet";
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
                    ps.errText = "Invalid amount";
                    showScreen();
                    return;  
                }
                          
                Wallet srcWallet = wallets[srcIdx];
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
                      
                ps.srcWallet = srcWallet;              
                if (dstIdx == wallets.length) {
                    // Remote User
                    if (remoteWalledId.getText().isEmpty()) {
                        ps.errText = "Remote Wallet is empty";
                        showScreen();
                        return;
                    }
                    ps.typedRemoteWallet = remoteWalledId.getText();
                    ps.sendType = ProgramState.SEND_TYPE_REMOTE;
                } else if (dstIdx == wallets.length + 1) {
                    // Local folder
                    if (ps.chosenFile.isEmpty()) {
                        ps.errText = "Folder is not chosen";
                        showScreen();
                        return;
                    }
                    
                    ps.sendType = ProgramState.SEND_TYPE_FOLDER;      
                } else {
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
   
                    if (srcWallet.isSkyWallet() && dstWallet.isSkyWallet()) {
                        ps.errText = "Transfer from Sky Wallet to Sky Wallet is not supported";
                        showScreen();
                        return;
                    }
                    
                    ps.dstWallet = dstWallet;
                    ps.sendType = ProgramState.SEND_TYPE_WALLET;
                }
                
                ps.currentScreen = ProgramState.SCREEN_CONFIRM_TRANSFER;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp);   
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
        final JLabel tl = new JLabel("Total files selected: " + ps.files.size());
        AppUI.setCommonFont(tl);
        c.insets = new Insets(28, 18, 0, 0); 
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
        
        AppUI.setSize(ddPanel, (int) ddWidth, 179);
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
        AppUI.hr(ct, 20);
            
        // GridHolder Container
        JPanel gct = new JPanel();
        AppUI.noOpaque(gct);
   
        GridBagLayout gridbag = new GridBagLayout();
        gct.setLayout(gridbag);   
        GridBagConstraints c = new GridBagConstraints();    
        
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 4, 0); 
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 0;

        JLabel l = new JLabel("http://cloudcoinconsortium.com/use.html");
        AppUI.setCommonFont(l);
        AppUI.setColor(l, AppUI.getColor0());
        AppUI.underLine(l);
        AppUI.setHandCursor(l);
        l.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        URI uri = new URI("http://cloudcoinconsortium.com/use.html");
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException ex) { 
                    } catch (URISyntaxException ex) {
                        
                    }
                } else { 
                }   
            }
        });
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        
        l = new JLabel("<html><div style='width:460px; text-align:center'><br>"
                + "Support: 9 AM to 3 AM California Time (PST)<br> "
                + "Tel: +1(530)762-1361 <br>"
                + "Email: Support@cloudcoinmail.com</div></html>");
        c.insets = new Insets(0, 0, 0, 0); 
        AppUI.alignCenter(l);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 1;
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c);
        gct.add(l);
        
        
        l = new JLabel("<html><div style='width:480px; text-align:center; font-size: 14px'>"
                + "(Secure if you get a free encrypted email account at ProtonMail.com)</div></html>");
      
        AppUI.setMargin(l, 0);
        AppUI.setFont(l, 12);
        AppUI.alignCenter(l);
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 2;
        AppUI.setCommonFont(l);
        gridbag.setConstraints(l, c);
        gct.add(l);

        // Get proton
        l = new JLabel("Get Protonmail");
        AppUI.setFont(l, 14);
        AppUI.setColor(l, AppUI.getColor0());
        AppUI.underLine(l);
        AppUI.setHandCursor(l);
        l.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        URI uri = new URI("https://www.protonmail.com");
                        Desktop.getDesktop().browse(uri);
                    } catch (IOException ex) { 
                    } catch (URISyntaxException ex) {
                        
                    }
                } else { 
                }   
            }
        });
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = 3;
        gridbag.setConstraints(l, c); 
        gct.add(l);
        
        
        
        
        
        ct.add(gct);
        
        
        
        //rightPanel
        
        
        
    }
    
    public void showClearScreen() {
        
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
 
        final String[] options = new String[wallets.length];
        for (int i = 0; i < wallets.length; i++) {
            options[i] = wallets[i].getName() + " - " + AppCore.formatNumber(wallets[i].getTotal()) + " CC";       
        }

        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", options);
        gridbag.setConstraints(cboxfrom.getComboBox(), c);
        gct.add(cboxfrom.getComboBox());

        y++;
        
        rightPanel.add(gct); 

        JPanel bp = getTwoButtonPanel(new ActionListener() {
            public void actionPerformed(ActionEvent e) {              
                int srcIdx = cboxfrom.getSelectedIndex() - 1;              
                if (srcIdx < 0 || srcIdx >= wallets.length) {
                    ps.errText = "Please select Wallet";
                    showScreen();
                    return;
                }

                Wallet srcWallet = wallets[srcIdx];
                ps.srcWallet = srcWallet;            
                
                ps.currentScreen = ProgramState.SCREEN_LIST_SERIALS_DONE;
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
        JLabel x = new JLabel("         From Wallet");
        gridbag.setConstraints(x, c);
        AppUI.setCommonFont(x);
        gct.add(x);
        
        c.gridx = GridBagConstraints.RELATIVE;
        c.gridy = y;     
        c.anchor = GridBagConstraints.WEST;
 
        int nonSkyCnt = 0;
        for (int i = 0; i < wallets.length; i++)
            if (!wallets[i].isSkyWallet())
                nonSkyCnt++;
             
        final String[] nonSkyOptions = new String[nonSkyCnt];
        final int[] idxs = new int[nonSkyCnt];
        int j = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!wallets[i].isSkyWallet()) {
                nonSkyOptions[j] = wallets[i].getName() + " - " + AppCore.formatNumber(wallets[i].getTotal()) + " CC";
                idxs[j] = i;
                j++;
            }
        }

        final RoundedCornerComboBox cboxfrom = new RoundedCornerComboBox(AppUI.getColor2(), "Make Selection", nonSkyOptions);
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
                if (srcIdx < 0 || srcIdx >= idxs.length) 
                    return;
                

                srcIdx = idxs[srcIdx];
                
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
                System.out.println("sss="+srcIdx);
                if (srcIdx < 0 || srcIdx >= idxs.length) {
                    ps.errText = "Please select Wallet";
                    showScreen();
                    return;
                }

                srcIdx = idxs[srcIdx];
   
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
                    
                ps.currentScreen = ProgramState.SCREEN_BACKUP_DONE;
                showScreen();
            }
        });
        
        AppUI.hr(rightPanel, 20);
        rightPanel.add(bp); 
    }
    
    public void showListSerialsDone() {
        showLeftScreen();
 
        Wallet w = ps.srcWallet;     

        JPanel rightPanel = getRightPanel(AppUI.getColor4());    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
             
        JLabel ltitle = AppUI.getTitle("List Serials - " + w.getName() + " - " + w.getTotal() + " CC");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);
        
        int[] sns = w.getSNs();
        if (sns.length == 0) {
            JLabel trLabel = new JLabel("No Serials");
            AppUI.setSemiBoldFont(trLabel, 20);
            AppUI.alignCenter(trLabel);
            ct.add(trLabel);
            return;
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
        
        
     
        final JTable table = new JTable();
        final JScrollPane scrollPane = AppUI.setupTable(table, new String[] {"Serial Number", "Denomination"}, serials, r);
        AppUI.setSize(scrollPane, 260, 325);
    //    table.getColumnModel().getColumn(0).setPreferredWidth(240);
      //  table.getColumnModel().getColumn(1).setPreferredWidth(100);
 
        
   

        ct.add(scrollPane);
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        JPanel bp = getTwoButtonPanelCustom("Print", "Export History", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                    try {
                   //     table.print();
                    } catch (PrinterException pe) {
                        System.out.println("Failed to print");
                    }*/
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
    
    public void showTransactionsScreen() {
        
       // sm.startFrackFixerService(new FrackFixerCb());
        
        showLeftScreen();
 
        Wallet w = sm.getActiveWallet();     

        JPanel rightPanel = getRightPanel(AppUI.getColor4());    
        JPanel ct = new JPanel();
        AppUI.setBoxLayout(ct, true);
        AppUI.noOpaque(ct);
        rightPanel.add(ct);
        
        
        
        JLabel ltitle = AppUI.getTitle(w.getName() + " - " + w.getTotal() + " CC");   
        ct.add(ltitle);
        AppUI.hr(ct, 20);

        
        // Create transactions
        JLabel trLabel;
        final String[][] trs = sm.getActiveWallet().getTransactions();
        
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
     
        final JTable table = new JTable();
        final JScrollPane scrollPane = AppUI.setupTable(table, new String[] {
            "Memo (note)",
            "Date",
            "Deposit",
            "Withdraw",
            "Total"
        }, trs, r);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        
        MouseAdapter ma = new MouseAdapter() {
            private int prevcolumn = -1;

            @Override
            public void mouseReleased(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                
                if (column != 0)
                    return;
                
                String hash = trs[row][5];
                
                String html = AppCore.getReceiptHtml(hash, ps.currentWallet.getName());
                if (html == null)
                    return;
                
                
                final JDialog f = new JDialog(mainFrame, "Receipt", true);
                AppUI.setBackground((JComponent) f.getContentPane(), Color.RED);
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
            }   
            
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
            }
        };
        
        
        table.addMouseListener(ma);
        table.addMouseMotionListener(ma);
 
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
                AppUI.roundCorners(p, AppUI.getColor11(), 40);
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
                
                sm.initWallets();
                
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

        
        
        
        

        JPanel cx = new JPanel();
        cx.setBounds(0,12,200,100);
        AppUI.noOpaque(cx);
       
        
        
        
        
        
        
        GridBagLayout gridbag = new GridBagLayout();
       
        cx.setLayout(gridbag);
        
        GridBagConstraints c = new GridBagConstraints();      
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0); 
 
        int y = 0;
        
        String name = (wallet == null) ? "Add Wallet" : wallet.getName();        
        JLabel l = new JLabel(name);
        AppUI.setFont(l, 22);
        if (isDisabled)
            AppUI.setColor(l, AppUI.getDisabledColor2());
        
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
                System.out.println("err " + ex.getMessage());
                return null;
            }
            c.insets = new Insets(24, 16, 0, 16); 
            c.gridwidth = 1;
            c.gridx = GridBagConstraints.RELATIVE;
            c.gridy = y;  
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(iconl, c);
            cx.add(iconl);

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
                    ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
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
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        

        // Wrapper for label
     
       // AppUI.setBoxLayout(cx, true);
      //  cx.setBounds(0,12,200,100);

        // Space
        //AppUI.hr(cx, 10);
        

       /*
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

            final Wallet fwallet = wallet;
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    setActiveWallet(fwallet);
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
            
        }
          
        cx.add(inner);        
        */

        
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
        
        JPanel subInnerCore = AppUI.createRoundedPanel(xpanel, AppUI.getColor12(), 20);
        AppUI.setSize(subInnerCore, 718, 446);

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
        /*
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        
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
            
            final Object fresult = result;
            UnpackerResult ur = (UnpackerResult) fresult;

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
            
            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    depositText.setText("Fixing fracked coins ...");
                    depositText.repaint();
                }
            });

            
            sm.startFrackFixerService(new FrackFixerCb());
            /*
            if (!sm.getActiveWallet().isEncrypted()) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
                        showScreen();
                    }
                });
                
             //   sm.startFrackFixerService(new FrackFixerCb());
            } else {
                sm.startVaulterService(new VaulterCb());
            } 
            */
            
	}
    }

    class FrackFixerCb implements CallbackInterface {
	public void callback(Object result) {
            FrackFixerResult fr = (FrackFixerResult) result;
            
            if (fr.status == FrackFixerResult.STATUS_PROCESSING) {
                wl.debug(ltag, "Processing coin");
		return;
            }

            if (fr.status == FrackFixerResult.STATUS_ERROR) {
                wl.error(ltag, "Failed to fix");
		//return;
            }

            if (fr.status == FrackFixerResult.STATUS_FINISHED) {
		if (fr.fixed + fr.failed > 0) {
                    wl.debug(ltag, "Fracker fixed: " + fr.fixed + ", failed: " + fr.failed);
                 //   return;
		}
            }

            EventQueue.invokeLater(new Runnable() {         
                public void run() {
                    depositText.setText("Fixing lost coins ...");
                    depositText.repaint();
                }
            });
    
            sm.startLossFixerService(new LossFixerCb());
        }
    }
         
    class ExporterCb implements CallbackInterface {
	public void callback(Object result) {
            ExporterResult er = (ExporterResult) result;
            if (er.status == ExporterResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!er.errText.isEmpty())
                            ps.errText = er.errText;
                        else
                            ps.errText = "Failed to export coins";
                        
                        showScreen();
                    }
                });
                
                return;
            }

            if (er.status == ExporterResult.STATUS_FINISHED) {
		//exportedFilenames = er.exportedFileNames;

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
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
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
                    } else if (isWithdrawing()) {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
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
            
            wl.debug(ltag, "LossFixer finished");

            if (sm.getActiveWallet().isEncrypted()) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        depositText.setText("Encrypting coins ...");
                        depositText.repaint();
                    }
                });
                
                sm.startVaulterService(new VaulterCb());
            } else {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_IMPORT_DONE;
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

            wl.debug(ltag, "Eraser finished");
	}
    }
    
    class SenderCb implements CallbackInterface {
	public void callback(Object result) {
            SenderResult sr = (SenderResult) result;
            
            wl.debug(ltag, "Sender finished: " + sr.status);
            if (sr.status == SenderResult.STATUS_PROCESSING) {
                setRAIDATransferProgress(sr.totalRAIDAProcessed, sr.totalFilesProcessed, sr.totalFiles);
                return;
            }
            
            
            if (sr.status == SenderResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                    }
                });
                return;
            }
      
            if (sr.status == SenderResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!sr.errText.isEmpty())
                            ps.errText = sr.errText;
                        else  
                            ps.errText = "Error occurred. Please check the logs";
                        
                        showScreen();
                    }
                });
		return;
            }   
            
            if (sr.amount > 0) {
                wl.debug(ltag, "sramount " + sr.amount + " typed " + ps.typedAmount);
                if (ps.typedAmount != sr.amount) {
                    ps.typedAmount = sr.amount;
                    ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    ps.errText = "Not all coins were sent. Please check the logs";
                    showScreen();
                    return;
                }
                
                ps.typedAmount = sr.amount;
                sm.getActiveWallet().appendTransaction(ps.typedMemo, sr.amount * -1, sr.receiptId);
                Wallet srcWallet = ps.srcWallet;
                Wallet dstWallet = ps.dstWallet;
                if (dstWallet != null) {
                    dstWallet.appendTransaction(ps.typedMemo, sr.amount, sr.receiptId);
                    if (dstWallet.isEncrypted()) {
                        sm.changeServantUser("Vaulter", dstWallet.getName());
                        sm.startVaulterService(new VaulterCb(), dstWallet.getPassword());          
                    } else {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                    }
                }      
                
                return;
            } else {
                ps.typedAmount = 0;
                ps.errText = "Coins were not sent. Please check the logs";     
            }
            
            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
            showScreen();
                 
	}
    }
    
    
    class ShowEnvelopeCoinsCb implements CallbackInterface {
	public void callback(Object result) {
            ShowEnvelopeCoinsResult er = (ShowEnvelopeCoinsResult) result;
 
            setSNs(er.coins);
            showCoinsDone(er.counters);
        }
    }
    
    
    class ReceiverCb implements CallbackInterface {
	public void callback(Object result) {
            ReceiverResult rr = (ReceiverResult) result;
            
            wl.debug(ltag, "Receiver finished: " + rr.status);
            if (rr.status == ReceiverResult.STATUS_PROCESSING) {
                setRAIDATransferProgress(rr.totalRAIDAProcessed, rr.totalFilesProcessed, rr.totalFiles);
                return;
            }
            
            
            wl.debug(ltag, "Receiver finished");
            if (rr.status == ReceiverResult.STATUS_CANCELLED) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.errText = "Operation Cancelled";
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                    }
                });
                return;
            }
      
            if (rr.status == ReceiverResult.STATUS_ERROR) {
                EventQueue.invokeLater(new Runnable() {         
                    public void run() {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        if (!rr.errText.isEmpty())
                            ps.errText = rr.errText;
                        else  
                            ps.errText = "Error occurred. Please check the logs";
                        
                        showScreen();
                    }
                });
		return;
            } 
            
            if (rr.amount > 0) {
                wl.debug(ltag, "rramount " + rr.amount + " typed " + ps.typedAmount);
                if (ps.typedAmount != rr.amount) {
                    ps.typedAmount = rr.amount;
                    ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                    ps.errText = "Not all coins were received. Please check the logs";
                    showScreen();
                    return;
                }
                
                ps.typedAmount = rr.amount;
                
                Wallet srcWallet = ps.srcWallet;
                Wallet dstWallet = ps.dstWallet;
                
                srcWallet.appendTransaction(ps.typedMemo, rr.amount * -1, rr.receiptId);
                if (dstWallet != null) {
                    dstWallet.appendTransaction(ps.typedMemo, rr.amount, rr.receiptId);
                    if (dstWallet.isEncrypted()) {
                        sm.changeServantUser("Vaulter", dstWallet.getName());
                        sm.startVaulterService(new VaulterCb(), dstWallet.getPassword());          
                    } else {
                        ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
                        showScreen();
                    }
                }      
                
                return;
            } else {
                ps.typedAmount = 0;
                ps.errText = "Coins were not sent. Please check the logs";     
            }
            
            ps.currentScreen = ProgramState.SCREEN_TRANSFER_DONE;
            showScreen();
            
            
            
            
            
            
            
            
            
            
            
            
            
            /*
            currentWallet = currentDstWallet;
            if (!sm.getActiveWallet().isEncrypted()) {
                cbState = IMPORT_STATE_DONE;
            } else {
                sm.startVaulterService(new VaulterCb());
            }  
           */ 
	}
    }
   
}


