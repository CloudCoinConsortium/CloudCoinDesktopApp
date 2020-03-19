/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.ServantManager;

import advclient.ProgramState;
import advclient.common.core.RequestChange;
import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Backupper.Backupper;
import global.cloudcoin.ccbank.ChangeMaker.ChangeMaker;
import global.cloudcoin.ccbank.ChangeMaker.ChangeMakerResult;
import global.cloudcoin.ccbank.Echoer.Echoer;
import global.cloudcoin.ccbank.Eraser.Eraser;
import global.cloudcoin.ccbank.Exporter.Exporter;
import global.cloudcoin.ccbank.FrackFixer.FrackFixer;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.Grader.Grader;
import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.LossFixer.LossFixer;
import global.cloudcoin.ccbank.LossFixer.LossFixerResult;
import global.cloudcoin.ccbank.Receiver.Receiver;
import global.cloudcoin.ccbank.Receiver.ReceiverResult;
import global.cloudcoin.ccbank.Sender.Sender;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.Transfer.Transfer;
import global.cloudcoin.ccbank.Transfer.TransferResult;
import global.cloudcoin.ccbank.Emailer.Emailer;
import global.cloudcoin.ccbank.Emailer.EmailerResult;
import global.cloudcoin.ccbank.ShowCoins.ShowCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoinsResult;
import global.cloudcoin.ccbank.Unpacker.Unpacker;
import global.cloudcoin.ccbank.Vaulter.Vaulter;
import global.cloudcoin.ccbank.Vaulter.VaulterResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.DNSSn;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.ServantRegistry;
import global.cloudcoin.ccbank.core.Wallet;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeMap;
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
    
    ArrayList<ShowEnvelopeCoins> secs;
    
    public ServantManager(GLogger logger, String home) {
        this.logger = logger;
        this.home = home;
        this.sr = new ServantRegistry();
        this.user = Config.DIR_DEFAULT_USER;
        this.wallets = new Hashtable<String, Wallet>();
        
        initIsolatedSec();
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
            sr.changeUser(Config.DIR_DEFAULT_USER);
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
    
    public String getHomeDir() {
        return this.home;
    }
    
    public boolean init() {
        boolean rv; 
        
        AppCore.initPool();
        
        try {
            rv = AppCore.initFolders(new File(home), logger);
            if (!rv) {
                logger.error(ltag, "Failed to create folders");
                return false;
            }
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
                "Backupper",
                "Transfer",
                "Emailer"
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
        }
        
        checkIDCoins();
    }
    
    
    public void checkIDCoins() {
        String[] idCoins = AppCore.getFilesInDir(AppCore.getIDDir(), null);
        String coinExt = ".stack";
        
        for (int i = 0; i < idCoins.length; i++) {
            if (!idCoins[i].endsWith(coinExt)) {
                logger.info(ltag, "Skipping non-stack file in the ID folder: " + idCoins[i]);
                continue;
            }
            
            CloudCoin cc;
            try {
                cc = new CloudCoin(AppCore.getIDDir() + File.separator + idCoins[i]);
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse ID coin: " + idCoins[i] + " error: " + e.getMessage());
                continue;
            }
            
            int dots = 0;
            
            String wname = idCoins[i].substring(0, idCoins[i].length() - coinExt.length());
            for (int y = 0; y < wname.length(); y++) {
                if (wname.charAt(y) == '.')
                    dots++;
            }

            if (dots < 2)
                wname += "." + Config.DDNS_DOMAIN;

            initCloudWallet(cc, wname);
        }     
    }
    
    public void initCloudWallet(CloudCoin cc, String name) {
        Wallet wobj = new Wallet(name, "", false, "", logger);
        wobj.setIDCoin(cc);
        
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
    
    public void startAuthenticatorService(CloudCoin cc, CallbackInterface cb) {
        if (sr.isRunning("Authenticator"))
            return;

	Authenticator at = (Authenticator) sr.getServant("Authenticator");
	at.launch(cc, cb);
    }

    public void startGraderService(CallbackInterface cb, ArrayList<CloudCoin> duplicates, String source) {
        if (sr.isRunning("Grader"))
            return;
        
	Grader gd = (Grader) sr.getServant("Grader");
	gd.launch(cb, duplicates, source);
    }
    
    
    public void startShowSkyCoinsService(CallbackInterface cb, int sn) {
        while (sr.isRunning("ShowEnvelopeCoins")) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
        }

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
    
    public void startSenderService(int sn, String dstFolder, int amount, String memo, String remoteWalletName, CallbackInterface cb) {
	Sender s = (Sender) sr.getServant("Sender");
	s.launch(sn, dstFolder, null, amount, memo, remoteWalletName, cb);
    }
     
    
    public void startReceiverService(int sn, int sns[], 
            String dstFolder, int amount, String memo, CallbackInterface cb) {
	Receiver r = (Receiver) sr.getServant("Receiver");
	//r.launch(, new int[]{1,1}, new int[] {7050330, 7050331}, memo, cb);
        r.launch(sn, sns, dstFolder, amount, false, cb);
    }

    public void startSenderServiceForChange(int sn, int[] values, String memo, CallbackInterface cb) {
        Sender s = (Sender) sr.getServant("Sender");
	s.launch(sn, null, values, 0, memo, Config.CHANGE_SKY_DOMAIN, cb);
    }
    
    public void startChangeMakerService(int method, CloudCoin cc, CallbackInterface cb) {
        ChangeMaker cm = (ChangeMaker) sr.getServant("ChangeMaker");
        cm.launch(method, cc, cb);
    }
    
    public void startTransferService(int fromsn, int tosn, int sns[], int amount, String tag, CallbackInterface cb) {
        logger.debug(ltag, "Transfer from " + fromsn + " to " + tosn + " amount " + amount);
        Transfer tr = (Transfer) sr.getServant("Transfer");
        tr.launch(fromsn, tosn, sns, amount, tag, cb);
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
            String memo, String remoteWalletName, CallbackInterface scb, CallbackInterface rcb) {
        
        logger.debug(ltag, "Transferring " + amount + " from " + srcWallet + " to " + dstWallet + " rn=" + remoteWalletName);
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
               new rVaulterCb(sn, dstWallet, amount, memo, remoteWalletName, scb));
             
            return true;
        }

        logger.debug(ltag, "send to sn " + sn + " dstWallet " + dstWallet);
        startSenderService(sn, dstWallet, amount, memo, remoteWalletName, scb);
        
        return true;
        
    }

    
    public void startVaulterService(CallbackInterface cb) {
        String password = getActiveWallet().getPassword();
        if (password.isEmpty()) {
            logger.error(ltag, "Empty password");
            return;
        }
        
        //logger.debug(ltag, "Vaulter password " + password);
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.vault(password, 0, null, cb);
    }
    
    public void startVaulterService(CallbackInterface cb, String password) {
        //logger.debug(ltag, "Vaulter password " + password);
        if (password.isEmpty()) {
            logger.error(ltag, "Empty password");
            return;
        }
        
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.vault(password, 0, null, cb);
    }
    
    
    
    public void startEmailerService(String[] emails, String[] subjects, String[] bodies, String[][] attachments, CallbackInterface icb) {
        Emailer el = (Emailer) sr.getServant("Emailer");
        el.launch(emails, subjects, bodies, attachments, icb);
    }
    
    public void startExporterService(int exportType, int amount, String tag, String dir, boolean keepSrc, CallbackInterface cb) {
        if (sr.isRunning("Exporter"))
            return;
                
        Exporter ex = (Exporter) sr.getServant("Exporter");
	ex.launch(exportType, amount, tag, dir, keepSrc, cb);
    }
    
    public void startExporterService(int exportType, String[] values, String tag, String dir, CallbackInterface cb) {
        if (sr.isRunning("Exporter"))
            return;
                
        Exporter ex = (Exporter) sr.getServant("Exporter");
	ex.launch(exportType, values, tag, dir, false, cb);
    }
    
    
    public void startSecureExporterService(int exportType, int amount, String tag, String dir, boolean keepSrc, CallbackInterface cb) {
        logger.debug(ltag, "Secure Exporter " + getActiveWallet().getName());
        String password = getActiveWallet().getPassword();
        if (password.isEmpty()) {
            logger.error(ltag, "Empty password");
            return;
        }
        
        //logger.debug(ltag, "Vaulter password " + password);
	Vaulter v = (Vaulter) sr.getServant("Vaulter");
	v.unvault(password, amount, null, new eVaulterCb(exportType, amount, tag, dir, keepSrc, cb));
    }
    
    public boolean checkCoins(int amount) {
        Exporter er = (Exporter) sr.getServant("Exporter");
        return er.checkCoins(amount);
    }
    
    public String getEmailerError() {
        String error = null;
        
        Emailer el = (Emailer) sr.getServant("Emailer");   
        if (!el.doChecks()) {
            error = el.ger.errText;
            return error;
        }

        return error;
    }
    
    public int findNoteToChange(int needed, int b250, int b100, int b25, int b5, int b1) {
        int totalReturn = 0;
        int totalNeeded = needed;
        int bankTotal = b250 * 250 + b100 * 100 + b25 * 25 + b5 * 5 + b1;
        
        logger.debug(ltag, "Finding note to change: " + needed + " 250s:" + b250 
                + ", 100s: " + b100 + ", 25s: " +  b25 + ", 5s: " + b5 + ", 1s: " + b1);
        
        if (bankTotal < totalNeeded) {
            logger.error(ltag, "Not enough bank total: " + bankTotal + " Wanted to send " + totalNeeded);
            return -1;
        }
        
        while (totalNeeded != totalReturn) {
            if (b250 > 0 && totalNeeded >= 250) {
                b250--;
                totalNeeded -= 250;
                totalReturn += 250;
            } else if (b100 > 0 && totalNeeded >= 100) {
                b100--;
                totalNeeded -= 100;
                totalReturn += 100;
            } else if (b25 > 0 && totalNeeded >= 25) {
                b25--;
                totalNeeded -= 25;
                totalReturn += 25;
            } else if (b5 > 0 && totalNeeded >= 5) {
                b5--;
                totalNeeded -= 5;
                totalReturn += 5;
            } else if (b1 > 0 && totalNeeded >= 1) {
                b1--;
                totalNeeded -= 1;
                totalReturn += 1;
            } else {
                if (b5 > 0)
                    return 5;
                
                if (b25 > 0)
                    return 25;
                
                if (b100 > 0)
                    return 100;
                
                if (b250 > 0)
                    return 250;
                
                logger.error(ltag, "Unable to find change");
                return -1;
            }
        }
        
        logger.error(ltag, "No change required. Why did you call me?");
        
        return 0;
    }
    
    public boolean makeChange(Wallet w, int amount, int skySN, CallbackInterface cb) {
        int min5sn, min25sn, min100sn, min250sn;
        int b250, b100, b25, b5, b1;
        
        makeChangeResult mcr = new makeChangeResult();
                
        logger.debug(ltag, "Make Change");
      
        int sns[] = w.getSNs();
        
        logger.debug(ltag, "Making change for " + w.getName() + " amount: " + amount + " skySN: " + skySN);
        
        if (w.isSkyWallet()) {
            mcr.errText = "Can't make change in SkyWallet";
            if (cb != null)
                cb.callback(mcr);
            logger.error(ltag, "Can't make change in SkyWallet");
            return false;
        }      

        min5sn = min25sn = min100sn = min250sn = 0;
        b250 = b100 = b25 = b5 = b1 = 0;
        for (int i = 0; i < sns.length; i++) {
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, sns[i]);
            int denomination = cc.getDenomination();
            
            if (denomination == 1)
                continue;
            
            switch (denomination) {
                case 5:
                    b5++;
                    if (min5sn == 0 || min5sn < denomination) 
                        min5sn = sns[i];
                    break;
                case 25:
                    b25++;
                    if (min25sn == 0 || min25sn < denomination)
                        min25sn = sns[i];
                    break;
                case 100:
                    b100++;
                    if (min100sn == 0 || min100sn < denomination)
                        min100sn = sns[i];
                    break;
                case 250:
                    b250++;
                    if (min250sn == 0 || min250sn < denomination)
                        min250sn = sns[i];
                    break;
            }
        }
        
        int rqDenom = findNoteToChange(amount, b250, b100, b25, b5, b1);
        logger.debug(ltag, "rqDenom = " + rqDenom);
        if (rqDenom <= 0) {
            mcr.errText = "Failed to find a coin (SN) to make change";
            if (cb != null)
                cb.callback(mcr);
            logger.error(ltag, "Failed to find a coin (SN) to change");
            return false;
        }
        
        int sn = 0;
        for (int i = 0; i < sns.length; i++) {
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, sns[i]);
            int denomination = cc.getDenomination();
            
            if (rqDenom == denomination) {
                logger.debug(ltag, "Found denomination");
                sn = cc.sn;
                break;
            }
        }
        
        /*
        int[] ds = { min5sn, min25sn, min100sn, min250sn };
        int idx;
        if (amount < 5)
            idx = 0;
        else if (amount < 25)
            idx = 1;
        else if (amount < 100)
            idx = 2;
        else 
            idx = 3;
               
        logger.debug(ltag, "idx = " + idx + " ds " + Arrays.toString(ds));
        
        int sn = 0;
        while (idx >= 0) {   
            for (int i = idx; i < ds.length; i++) {
                if (ds[i] != 0) {
                    sn = ds[i];
                    break;
                }
            }
            
            logger.debug(ltag, "idx " + idx + " sn = " + sn);
            if (sn != 0)
                break;
            
            idx--;
        }
        */
        
        if (sn == 0) {
            mcr.errText = "Failed to find a coin to make change";
            if (cb != null)
                cb.callback(mcr);
            logger.error(ltag, "Failed to find SN to change");
            return false;
        }

        if (cb != null) {
            mcr.status = 0;
            mcr.text = "Breaking coin #" + sn;
            cb.callback(mcr);
        }
        
        CloudCoin cc;
        
        String user = w.getName();
        if (w.isEncrypted()) {
            cc = AppCore.findCoinBySN(Config.DIR_VAULT, user, sn);
        } else {
            cc = AppCore.findCoinBySN(Config.DIR_BANK, user, sn);
        }
        
        if (cc == null) {
            logger.debug(ltag, "Failed to find in the Main Folder. Searching in Fracked");
            cc = AppCore.findCoinBySN(Config.DIR_FRACKED, user, sn);
            if (cc == null) {
                mcr.errText = "Failed to find a coin (SN " + sn + ") to change";
                if (cb != null)
                    cb.callback(mcr);
                logger.error(ltag, "Failed to find coin");
                return false;
            }
        }
        
        if (w.isEncrypted()) {
            logger.debug(ltag, "Wallet is encrypted");
            
            String password = w.getPassword();
            if (password.isEmpty()) {
                mcr.errText = "Empty password. Internal error";
                if (cb != null)
                    cb.callback(mcr);
                logger.error(ltag, "Empty password. Internal error");
                return false;      
            }
            
            Vaulter v = (Vaulter) sr.getServant("Vaulter");
            v.unvault(password, 0, cc, new eVaulterChangeCb(cc, w, skySN, cb));
            
            return true;
        }
     
        return sendToChange(cc, w, skySN, cb);
    }
    
    public boolean sendToChange(CloudCoin cc, Wallet w, int skySN, CallbackInterface cb) {
        makeChangeResult mcr = new makeChangeResult();
        mcr.status = 1;
        mcr.text = "Making change for coin " + cc.sn;
        mcr.progress = 0;
        if (cb != null) {
            cb.callback(mcr);
        }
               
        logger.debug(ltag, "Sending to the ChangeMaker " + cc.sn + " denomination " + cc.getDenomination());
        int method = AppCore.getChangeMethod(cc.getDenomination());
        if (method == 0) {
            logger.error(ltag, "Can't find suitable method");
            return false;
        }

        logger.debug(ltag, "Method chosen: " + method);
        startChangeMakerService(method, cc, new CallbackInterface() {
            public void callback(Object o) {
                final ChangeMakerResult cmr = (ChangeMakerResult) o;

                logger.debug(ltag, "ChangeMaker finished: " + cmr.status);
                
                makeChangeResult mcr = new makeChangeResult();
                
                if (cmr.status == ChangeMakerResult.STATUS_PROCESSING) {                
                    mcr.text = "Making change for coin " + cc.sn;
                    mcr.progress = cmr.totalRAIDAProcessed;
                    mcr.status = 1;
                    if (cb != null) {
                        cb.callback(mcr);
                    }
                    
                    return;
                }
                
                if (cmr.status == ChangeMakerResult.STATUS_ERROR) {
                    logger.debug(ltag, "Error in making Change");
                    if (!cmr.errText.isEmpty())
                        mcr.errText = cmr.errText;
                    else
                        mcr.errText = "Failed to make Change. Please check the main.log file";
                    mcr.status = 0;
                    if (cb != null) {
                        cb.callback(mcr);
                    }
                    return;
                }

                if (cmr.status == ChangeMakerResult.STATUS_FINISHED) {
                
                    w.appendTransaction("Sent to Public Change", cc.getDenomination() * -1, cmr.receiptId);
                    mcr.text = "Authenticating Coins from Public Change";
                    if (cb != null)
                        cb.callback(mcr);
            
                    startAuthenticatorService(new CallbackInterface() {
                        public void callback(Object result) {
                            logger.debug(ltag, "Authenticator for Change finished");
                        
                            final Object fresult = result;
                            final AuthenticatorResult ar = (AuthenticatorResult) fresult;
                            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                                logger.debug(ltag, "Error in making Change");
                                mcr.errText = "Failed to authenticate coins from Public Change";
                                mcr.status = 0;
                                if (cb != null) {
                                    cb.callback(mcr);
                                }
                            
                                return;
                            } else if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                                mcr.text = "Grading Coins from Public Change";
                                if (cb != null)
                                    cb.callback(mcr);
                            
                                startGraderService(new eGraderCb(cb, w), null, w.getName());
                                return;
                            }
                        
                            mcr.text = "Authenticating Coins from Public Change";
                            mcr.progress = ar.totalRAIDAProcessed;
                            mcr.status = 1;
                            if (cb != null) {
                                cb.callback(mcr);
                            }
                        }
                    });
                }
                
            }
        });
        
        return true;
        
    }
    
    class eGraderCb implements CallbackInterface {
        Wallet w;
        makeChangeResult mcr;
        CallbackInterface cb;
        String receiptId;
        
        
        public eGraderCb(CallbackInterface cb, Wallet w) {
            this.w = w;
            this.cb = cb;
            mcr = new makeChangeResult();
        }
        
        public void callback(Object result) {
            GraderResult gr = (GraderResult) result;
            
            logger.debug(ltag, "Change Grader finished");
            
            //receiverReceiptId = rr.receiptId;
            int total = gr.totalAuthenticValue + gr.totalFrackedValue;
            String receiptId = gr.receiptId;
            
            w.appendTransaction("Received from Public Change", total, receiptId);
            
            mcr.text = "Fixing coins";
            if (cb != null)
                cb.callback(mcr);
            
            startFrackFixerService(new eFrackFixerCb(cb, w));
        }
    }
    
    class eFrackFixerCb implements CallbackInterface {
        Wallet w;
        makeChangeResult mcr;
        CallbackInterface cb;
        
        
        public eFrackFixerCb(CallbackInterface cb, Wallet w) {
            this.w = w;
            this.cb = cb;
            mcr = new makeChangeResult();
        }
        
        public void callback(Object result) {
            FrackFixerResult fr = (FrackFixerResult) result;
            
            if (fr.status == FrackFixerResult.STATUS_PROCESSING) {
                logger.debug(ltag, "Still processing coins");
                
                //String txt
                
                //fr.totalRAIDAProcessed, fr.totalFilesProcessed, fr.totalFiles, fr.fixingRAIDA, fr.round
                mcr.status = 1;
                mcr.progress = fr.totalRAIDAProcessed;
                mcr.text = "Fixing on RAIDA#" + fr.fixingRAIDA + ", round #" + fr.round + " Coins: " + fr.totalCoinsProcessed + "/" + fr.totalCoins;
                if (this.cb != null)
                    this.cb.callback(mcr);
                
		return;
            }

            if (fr.status == FrackFixerResult.STATUS_ERROR) {
                mcr.errText = "FrackFixer Error";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                
                return;
            }
            
            logger.debug(ltag, "Change FrackFixer finished: " + fr.status);
            
            mcr.text = "Recovering Lost Coins";
            if (cb != null)
                cb.callback(mcr);
            
            startLossFixerService(new eLossFixerCb(cb, w));
        }
    }
    
    
    class eLossFixerCb implements CallbackInterface {
        Wallet w;
        makeChangeResult mcr;
        CallbackInterface cb;
        
        
        public eLossFixerCb(CallbackInterface cb, Wallet w) {
            this.w = w;
            this.cb = cb;
            mcr = new makeChangeResult();
        }
        
        public void callback(Object result) {
            LossFixerResult lr = (LossFixerResult) result;
            
            if (lr.status == LossFixerResult.STATUS_PROCESSING) {
                
                mcr.status = 1;
                mcr.progress = lr.totalRAIDAProcessed;
                //mcr.text = "Recovering Lost Coins. Processed Notes: " + lr.totalFilesProcessed + "/" + lr.totalFiles;
                mcr.text = "Recovering Lost Coins";
                if (this.cb != null)
                    this.cb.callback(mcr);
                return;
            }
            
            logger.debug(ltag, "Change LossFixer finished: " + lr.status);
            
            
            if (w.isEncrypted()) {
                mcr.text = "Encrypting Change";
                if (cb != null)
                    cb.callback(mcr);
                
                logger.debug(ltag, "Ecnrypting change");
                startVaulterService(new ecVaulterCb(cb, w), w.getPassword());
                
                return;
            }
            
            mcr.text = "Change Done";
            mcr.status = 2;
            if (cb != null)
                cb.callback(mcr);
        }
    }
    
    class ecVaulterCb implements CallbackInterface {
        Wallet w;
        makeChangeResult mcr;
        CallbackInterface cb;
        
        
        public ecVaulterCb(CallbackInterface cb, Wallet w) {
            this.w = w;
            this.cb = cb;
            mcr = new makeChangeResult();
        }
        
        public void callback(Object result) {
            VaulterResult vr = (VaulterResult) result;
            
            logger.debug(ltag, "Change Vaulter finished: " + vr.status);
            
            mcr.text = "Change Done";
            mcr.status = 2;
            if (cb != null)
                cb.callback(mcr);
            
        }
    }
    
    class eVaulterChangeCb implements CallbackInterface {
        CallbackInterface mcb;
        CloudCoin cc;
        int skySN;
        Wallet w;
        
        
        public eVaulterChangeCb(CloudCoin cc, Wallet w, int skySN, CallbackInterface mcb) {
            this.mcb = mcb;
            this.cc = cc;
            this.skySN = skySN;
            this.w = w;
        }
                 
        public void callback(final Object result) {
            final Object fresult = result;
            VaulterResult vresult = (VaulterResult) fresult;
            
            logger.debug(ltag, "ChangeVault finished with status " + vresult.status);
            
            if (vresult.status == VaulterResult.STATUS_ERROR) {
                if (this.mcb != null) {
                    makeChangeResult mcr = new makeChangeResult();
                    mcr.errText = "Failed to decrypt coin";
                    this.mcb.callback(mcr);
                }
                
                return;
            }
            
            if (vresult.status == VaulterResult.STATUS_FINISHED) {
                CloudCoin ncc = AppCore.findCoinBySN(Config.DIR_BANK, w.getName(), cc.sn);
                if (ncc == null) {
                    makeChangeResult mcr = new makeChangeResult();
                    mcr.errText = "Failed to decrypt coin. The resulting coin is missing in the Bank";
                    this.mcb.callback(mcr);
                    return;
                }
                sendToChange(ncc, w, skySN, mcb);
            }
        }    
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
        String remoteWalletName;
    
        public rVaulterCb(int sn, String dstFolder, int amount, 
                String memo, String remoteWalletName, CallbackInterface cb) {
            this.cb = cb;
            this.amount = amount;
            this.memo = memo;
            this.sn = sn;
            this.dstFolder = dstFolder;
            this.remoteWalletName = remoteWalletName;

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
                    sr.errText = vresult.errText;
                    
                    cb.callback(sr);
                }
                
                return;
            }
            
            logger.debug(ltag, "send sn " + sn + " dstWallet " + dstFolder);
            startSenderService(sn, dstFolder, amount, memo, remoteWalletName, cb);
	}
    }
    
    public Wallet[] getWallets() {
        TreeMap<String, Wallet> sorted = new TreeMap<>(wallets);
        
        int size = sorted.size();
        Collection c = sorted.values();
        Wallet[] ws = new Wallet[size];
        
        int i = 0;
        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
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
        }
        
        return null;
    }
    
    public void openTransactions() {
        Collection c = wallets.values();

        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet w = (Wallet) itr.next();
            
            if (w.isSkyWallet())
                continue;

            if (w.getTotal() != 0)
                w.appendTransaction("Opening Balance", w.getTotal(), "openingbalance");      
        }
    }
    
    public int[] getRAIDAStatuses() {
        Servant e = sr.getServant("Echoer");
        
        e.updateRAIDAStatus();
        
        String[] urls = e.getRAIDA().getRAIDAURLs();
        int[] latencies = e.getRAIDA().getLatencies();
        int[] rv = new int[RAIDA.TOTAL_RAIDA_COUNT];
        
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (urls[i] == null)
                rv[i] = -1;
            else
                rv[i] = latencies[i];
        }
        
        return rv;
    }
 
    public void resumeAll() {
        sr.resumeAll();
    }
    
    
    public boolean isRAIDAOK() {
        return sr.getServant("Echoer").updateRAIDAStatus();
    }
    
    
    public class makeChangeResult {
        public int status;
        
        public String text;
        public String errText = "";
        
        public int progress;
    }

    
    public Wallet getFirstNonSkyWallet() {
        Collection c = wallets.values();

        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            
            if (tw.isSkyWallet()) {
               continue;
            }           
            
            return tw;
        }
        
        return null;
    }
    
    public Wallet getFirstFullNonSkyWallet() {
        Collection c = wallets.values();

        Iterator itr = c.iterator();
        while (itr.hasNext()) {
            Wallet tw = (Wallet) itr.next();
            
            if (tw.isSkyWallet()) {
               continue;
            }           
            
            if (tw.getTotal() == 0)
                continue;
            
            if (tw.isEncrypted())
                continue;
            
            return tw;
        }
        
        return null;
    }
    
    
    public void initIsolatedSec() {
        secs = new ArrayList<ShowEnvelopeCoins>();
    }
    
    public void addSec(ShowEnvelopeCoins sec) {
        secs.add(sec);
    }
    
    public void cancelSecs() {
        for (ShowEnvelopeCoins sec : secs)
            sec.cancelForce();
        
        initIsolatedSec();
    }
    
}
