/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.ServantManager;

import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Backupper.Backupper;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Eraser.Eraser;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.LossFixer.LossFixer;
import global.cloudcoin.ccbank.Receiver.Receiver;
import global.cloudcoin.ccbank.Sender.Sender;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoins;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.Vaulter.Vaulter;
import global.cloudcoin.ccbank.Vaulter.VaulterResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.ServantRegistry;
import global.cloudcoin.ccbank.core.Wallet;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import org.json.JSONException;

/**
 *
 * @author Alexander
 */
public class ServantManager {
    String ltag = "ServantManager";
    ServantRegistry sr;
    GLogger logger;
    String home;
    String user;
    private Hashtable<String, Wallet> wallets;
    
    public ServantManager(GLogger logger, String home) {
        this.logger = logger;
        this.home = home;
        this.sr = new ServantRegistry();
        this.user = Config.DIR_DEFAULT_USER;
        this.wallets = new Hashtable<String, Wallet>();
    }
    
    public Wallet getActiveWallet() {
        if (!wallets.containsKey(user)) 
            return null;
        
        return wallets.get(user);
    }
    
    public ServantRegistry getSR() {
        return this.sr;
    }
    
    public Wallet getWallet(String wallet) {
        return wallets.get(wallet);
    }
    
    public void setActiveWalletObj(Wallet wallet) {
        logger.debug(ltag, "Set active wallet obj " + wallet.getName() + " isky "  + wallet.isSkyWallet());
        this.user =  wallet.getName();
        if (wallet.isSkyWallet()) {
            sr.changeUser(wallet.getParent().getName());
        } else {
            sr.changeUser(this.user);
        }  
    }
    
    public void setActiveWallet(String walletName) {     
        logger.debug(ltag, "Set active wallet obj " + walletName);
        
        this.user = walletName;
        sr.changeUser(this.user);   
    }
    
    public void changeServantUser(String servant, String wallet) {
        sr.changeServantUser(servant, wallet);
    }
    
    public boolean init() {
        AppCore.initPool();
        
        try {
            AppCore.initFolders(new File(home), logger);
        } catch (Exception e) {
            logger.error(ltag, "Failed to init root dir " + home);
            return false;
        }   
        
        initServants();
        
        return true;
    }
    
    public boolean initServants() {
        sr.registerServants(new String[]{
                "Echoer",
                "Authenticator",
                "ShowCoins",
                "Unpacker",
                "Authenticator",
                "Grader",
                "FrackFixer",
                "Exporter",
                "Sender",
                "Receiver",
                "Backupper",
                "LossFixer",
                "ChangeMaker",
                "Vaulter",
                "ShowEnvelopeCoins",
                "Eraser",
                "Backupper"
        }, AppCore.getRootPath() + File.separator + user, logger);
   

        initWallets();

        return true;
    }
    
    public void initWallets() {
        this.wallets = new Hashtable<String, Wallet>();
        
        String[] wallets = AppCore.getDirs();
        for (int i = 0; i < wallets.length; i++) {
            setActiveWallet(wallets[i]);
            initWallet(wallets[i], "");
            
            checkIDCoins(wallets[i]);
        }
    }
    
    
    public void checkIDCoins(String root) {
        String[] idCoins = AppCore.getFilesInDir(Config.DIR_ID, root);
        
        for (int i = 0; i < idCoins.length; i++) {
            CloudCoin cc;
            try {
                cc = new CloudCoin(AppCore.getUserDir(Config.DIR_ID, root) + File.separator + idCoins[i]);
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse ID coin: " + idCoins[i] + " error: " + e.getMessage());
                continue;
            }
            
            String wname = idCoins[i].substring(0, idCoins[i].lastIndexOf('.'));
            wname += "." + Config.DDNS_DOMAIN;

            initCloudWallet(root, cc, wname);
        }     
    }
    
    public void initCloudWallet(String wallet, CloudCoin cc, String name) {
        Wallet parent = wallets.get(wallet);
        
        //String name = wallet + ":" + cc.sn;
        
        //Wallet wobj = new Wallet(name, parent.getEmail(), parent.isEncrypted(), parent.getPassword(), logger);
        Wallet wobj = new Wallet(name, "", false, "", logger);
        wobj.setIDCoin(cc, parent);
        
        wallets.put(name, wobj);   
    }
    
    public void initWallet(String wallet, String password) {
        if (wallets.containsKey(wallet)) 
            return;
        
        logger.debug(ltag, "Initializing wallet " + wallet);
        Authenticator au = (Authenticator) sr.getServant("Authenticator");
        String email = au.getConfigValue("email");
        if (email == null)
            email = "";
            
        Vaulter v = (Vaulter) sr.getServant("Vaulter");
        String encStatus = v.getConfigValue("status");
        if (encStatus == null)
            encStatus = "off";
        
        String passwordHash = v.getConfigValue("password");
        Wallet wobj = new Wallet(wallet, email, encStatus.equals("on"), password, logger);
        if (passwordHash != null)
            wobj.setPasswordHash(passwordHash);
        
        wallets.put(wallet, wobj);    
    }
    
    public boolean initUser(String wallet, String email, String password) {
        logger.debug(ltag, "Init user " + wallet);
               
        try {
            AppCore.initUserFolders(wallet);
        } catch (Exception e) {
            logger.error(ltag, "Error: " + e.getMessage());
            return false;
        }
        
        this.user = wallet;
        sr.changeUser(wallet);

        
        if (!email.equals(""))
            sr.getServant("Authenticator").putConfigValue("email", email);

        if (!password.equals("")) {
            sr.getServant("Vaulter").putConfigValue("status", "on");
            sr.getServant("Vaulter").putConfigValue("password", AppCore.getMD5(password));
        }
        
        if (!writeConfig(wallet)) {
            logger.error(ltag, "Failed to write conifg");
            return false;
        }
              
        initWallet(wallet, password);
              
        return true;
    }

    public boolean writeConfig(String user) {
        String config = "", ct;
        
        for (String name : sr.getServantKeySet()) {
            ct = sr.getServant(name).getConfigText();
            config += ct;
        }

        String configFilename = AppCore.getUserConfigDir(user) + File.separator + "config.txt";
        
        if (!AppCore.saveFile(configFilename, config)) {
            logger.error(ltag, "Failed to save config");
            return false;
        }
        
        return true;
    }
    
    public void startEchoService(CallbackInterface cb) {
        if (sr.isRunning("Echoer"))
            return;
        
	Echoer e = (Echoer) sr.getServant("Echoer");
	e.launch(cb);
    }
    
    public boolean isEchoerFinished() {
        return !sr.isRunning("Echoer");
    }
    
    public void startFrackFixerService(CallbackInterface cb) {
        if (sr.isRunning("FrackFixer"))
            return;
        
        FrackFixer ff = (FrackFixer) sr.getServant("FrackFixer");
	ff.launch(cb);
    }
    
    public void startUnpackerService(CallbackInterface cb) {
        if (sr.isRunning("Unpacker"))
            return;
        
	Unpacker up = (Unpacker) sr.getServant("Unpacker");
	up.launch(cb);
    }
     
    public void startAuthenticatorService(CallbackInterface cb) {
        if (sr.isRunning("Authenticator"))
            return;

	Authenticator at = (Authenticator) sr.getServant("Authenticator");
	at.launch(cb);
    }
    
    public void startGraderService(CallbackInterface cb) {
        if (sr.isRunning("Grader"))
            return;
        
	Grader gd = (Grader) sr.getServant("Grader");
	gd.launch(cb);
    }
    
    
    public void startShowSkyCoinsService(CallbackInterface cb, int sn) {
        if (sr.isRunning("ShowEnvelopeCoins"))
            return;

	ShowEnvelopeCoins sc = (ShowEnvelopeCoins) sr.getServant("ShowEnvelopeCoins");
	sc.launch(sn, "sky", cb);
    }
    
    
    public void startShowCoinsService(CallbackInterface cb) {
        if (sr.isRunning("ShowCoins"))
            return;
                
	ShowCoins sc = (ShowCoins) sr.getServant("ShowCoins");
	sc.launch(cb);
    }
    
    public void startLossFixerService(CallbackInterface cb) {
        LossFixer l = (LossFixer) sr.getServant("LossFixer");
	l.launch(cb);
    }
    
    public void startEraserService(CallbackInterface cb, boolean needBackup) {
	Eraser e = (Eraser) sr.getServant("Eraser");
	e.launch(cb, needBackup);
    }
    
    public void startBackupperService(String dstDir, CallbackInterface cb) {
	Backupper b = (Backupper) sr.getServant("Backupper");
	b.launch(dstDir, cb);
    }
    
    public void startSenderService(int sn, String dstFolder, int amount, String memo, CallbackInterface cb) {
	Sender s = (Sender) sr.getServant("Sender");
	s.launch(sn, dstFolder, null, amount, memo, cb);
    }
     
    
    public void startReceiverService(int sn, int sns[], 
            String dstFolder, int amount, String memo, CallbackInterface cb) {
	Receiver r = (Receiver) sr.getServant("Receiver");
	//r.launch(, new int[]{1,1}, new int[] {7050330, 7050331}, memo, cb);
        r.launch(sn, sns, dstFolder, amount, cb);
    }
    
    public int getRemoteSn(String dstWallet) {
        int sn;
        
        try {
            sn = Integer.parseInt(dstWallet);
        } catch (NumberFormatException e) {
            return 0;
        }
        
        if (sn <= 0)
            return 0;
        
        return sn;
    }
    
    public void cancel(String servantName) {
        Servant s = sr.getServant(servantName);
        if (s == null)
            return;
        
        s.cancel();
    }
    
    
    public boolean transferCoins(String srcWallet, String dstWallet, int amount, 
            String memo, CallbackInterface scb, CallbackInterface rcb) {
        
        logger.debug(ltag, "Transferring " + amount + " from " + srcWallet + " to " + dstWallet);
        int sn = 0;
        
        Wallet srcWalletObj, dstWalletObj; 
        
        srcWalletObj = wallets.get(srcWallet);
        dstWalletObj = wallets.get(dstWallet);
        if (srcWalletObj == null) {
            logger.error(ltag, "Wallet not found");
            return false;
        }
        
        if (dstWalletObj == null) {
            sn = getRemoteSn(dstWallet);
            logger.debug(ltag, "Remote SkyWallet got SN " + sn);
            if (sn == 0) {
                logger.error(ltag, "Invalid dst wallet");
                return false;
            }
            
            if (srcWalletObj.isSkyWallet()) {
                logger.error(ltag, "We can't transfer from SKY to SKY");
                return false;
            }
            
            dstWallet = null;
        } else {
            if (srcWalletObj.isSkyWallet()) {
                logger.debug(ltag, "Receiving from SkyWallet");
                setActiveWallet(dstWallet);
                sn = srcWalletObj.getIDCoin().sn;
                int[] sns = srcWalletObj.getSNs();
                logger.debug(ltag, "Got SN " + sn);
                startReceiverService(sn, sns, dstWalletObj.getName(), amount, memo, rcb);
                return true;
            }
        }
                 
        if (dstWalletObj != null && dstWalletObj.isSkyWallet()) {
            logger.debug(ltag, "Dst wallet is sky");
            sn = dstWalletObj.getIDCoin().sn;
            dstWallet = null;
        }           

        if (srcWalletObj.isEncrypted()) {
            logger.debug(ltag, "Src wallet is encrypted");
            Vaulter v = (Vaulter) sr.getServant("Vaulter");
            v.unvault(srcWalletObj.getPassword(), amount, null, 
               new rVaulterCb(sn, dstWallet, amount, memo, scb));
             
            return true;
        }

        logger.debug(ltag, "send to sn " + sn + " dstWallet " + dstWallet);
        startSenderService(sn, dstWallet, amount, memo, scb);
        
        return true;
        
    }

    
    public void startVaulterService(CallbackInterface cb) {
        String password = getActiveWallet().getPassword();
        if (password.isEmpty()) {
            logger.error(ltag, "Empty password");
            return;
        }
        
        logger.debug(ltag, "Vaulter password " + password);
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.vault(password, 0, null, cb);
    }
    
    public void startVaulterService(CallbackInterface cb, String password) {
        logger.debug(ltag, "Vaulter password " + password);
        if (password.isEmpty()) {
            logger.error(ltag, "Empty password");
            return;
        }
        
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.vault(password, 0, null, cb);
    }
    
    
    public void startExporterService(int exportType, int amount, String tag, String dir, boolean keepSrc, CallbackInterface cb) {
        if (sr.isRunning("Exporter"))
            return;
                
        Exporter ex = (Exporter) sr.getServant("Exporter");
	ex.launch(exportType, amount, tag, dir, keepSrc, cb);
    }
    
    public void startSecureExporterService(int exportType, int amount, String tag, String dir, boolean keepSrc, CallbackInterface cb) {
        logger.debug(ltag, "Secure Exporter " + getActiveWallet().getName());
        String password = getActiveWallet().getPassword();
        if (password.isEmpty()) {
            logger.error(ltag, "Empty password");
            return;
        }
        
        logger.debug(ltag, "Vaulter password " + password);
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.unvault(password, amount, null, new eVaulterCb(exportType, amount, tag, dir, keepSrc, cb));
    }
    
    class eVaulterCb implements CallbackInterface {
        CallbackInterface cb;
        int exportType;
        int amount;
        String tag;
        String dir;
        boolean keepSrc;
        
        public eVaulterCb(int exportType, int amount, String tag, String dir, boolean keepSrc, CallbackInterface cb) {
            this.cb = cb;
            this.amount = amount;
            this.tag = tag;
            this.exportType = exportType;
            this.dir = dir;
            this.keepSrc = keepSrc;
        }
        
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            
            logger.debug(ltag, "Evaulter CB finished");

            Exporter ex = (Exporter) sr.getServant("Exporter");
            ex.launch(exportType, amount, tag, dir, keepSrc, cb);
	}
    }
    
    class rVaulterCb implements CallbackInterface {
        CallbackInterface cb;
        int amount;
        String memo;
        String dstFolder;
        int sn;
    
        public rVaulterCb(int sn, String dstFolder, int amount, 
                String memo, CallbackInterface cb) {
            this.cb = cb;
            this.amount = amount;
            this.memo = memo;
            this.sn = sn;
            this.dstFolder = dstFolder;
        }
        
	public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            
            logger.debug(ltag, "rVaulter CB finished");
            
            if (vresult.status == VaulterResult.STATUS_ERROR) {
                logger.error(ltag, "Error on Vaulter");
                if (cb != null)  {
                    SenderResult sr = new SenderResult();
                    sr.status = SenderResult.STATUS_ERROR;
                    cb.callback(sr);
                }
                
                return;
            }
            
            logger.debug(ltag, "send sn " + sn + " dstWallet " + dstFolder);
            startSenderService(sn, dstFolder, amount, memo, cb);
	}
    }
    
    public Wallet[] getWallets() {
        int size = wallets.size();
        Collection c = wallets.values();
        Wallet[] ws = new Wallet[size];
        
        String defaultWalletName = AppCore.getDefaultWalletName();
        if (defaultWalletName == null) {
            logger.info(ltag, "Can't find default wallet");
            defaultWalletName = Config.DIR_DEFAULT_USER;
        }
        
        int i = 0;
        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            if (tw.getName().equals(defaultWalletName)) {
                ws[i++] = tw;
                break;
            }
        }
        
        itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            if (tw.getName().equals(defaultWalletName))
                continue;
            
            ws[i++] = tw;
        }
                
        return ws;
    }
    
    public Wallet getWalletByName(String walletName) {
        Collection c = wallets.values();

        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            if (tw.getName().equals(walletName)) 
                return tw;
            
            if (tw.isSkyWallet()) {
                String name = tw.getName() + "." + Config.DDNS_DOMAIN;
                if (name.equals(walletName))
                    return tw;
            }                
        }
        
        return null;
    }
    
    public String[] getRAIDAStatuses() {
        Servant e = sr.getServant("Echoer");
        
        e.updateRAIDAStatus();
        
        String[] urls = e.getRAIDA().getRAIDAURLs();
        int[] latencies = e.getRAIDA().getLatencies();
        String[] rv = new String[RAIDA.TOTAL_RAIDA_COUNT];
        
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (urls[i] == null)
                rv[i] = null;
            else
                rv[i] = "" + latencies[i];
        }
        
        return rv;
    }
 
    public void resumeAll() {
        sr.resumeAll();
    }
}
