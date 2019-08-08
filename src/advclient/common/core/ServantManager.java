/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.ServantManager;

import advclient.ProgramState;
import advclient.common.core.RequestChange;
import global.cloudcoin.ccbank.Authenticator.Authenticator;
import global.cloudcoin.ccbank.Backupper.Backupper;
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
    
    public void startReceiverServiceForChange(int sn, int sns[], 
            String dstFolder, int amount, String memo, CallbackInterface cb) {
	Receiver r = (Receiver) sr.getServant("Receiver");
        r.launch(sn, sns, dstFolder, amount, true, cb);
    }
    
    public void startSenderServiceForChange(int sn, int[] values, String memo, CallbackInterface cb) {
        Sender s = (Sender) sr.getServant("Sender");
	s.launch(sn, null, values, 0, memo, Config.CHANGE_SKY_DOMAIN, cb);
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
    
    
    public boolean makeChange(Wallet w, int amount, int skySN, CallbackInterface cb) {
        int min5sn, min25sn, min100sn, min250sn;
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
        for (int i = 0; i < sns.length; i++) {
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, sns[i]);
            int denomination = cc.getDenomination();
            
            if (denomination == 1)
                continue;
            
            switch (denomination) {
                case 5:
                    if (min5sn == 0 || min5sn < denomination) 
                        min5sn = sns[i];
                    break;
                case 25:
                    if (min25sn == 0 || min25sn < denomination)
                        min25sn = sns[i];
                    break;
                case 100:
                    if (min100sn == 0 || min100sn < denomination)
                        min100sn = sns[i];
                    break;
                case 250:
                    if (min250sn == 0 || min250sn < denomination)
                        min250sn = sns[i];
                    break;
            }
        }
        
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
        mcr.text = "Sending coin " + cc.sn + " to the SkyWallet";
        mcr.progress = 0;
        if (cb != null) {
            cb.callback(mcr);
        }
               
        logger.debug(ltag, "Sending to the Change.skywallet.cc " + cc.sn);
        
        DNSSn d = new DNSSn(Config.CHANGE_SKY_DOMAIN, null, logger);
        int sn = d.getSN();
        if (sn < 0) {
            logger.error(ltag, "Failed to query change service");
            mcr.errText = "Failed to query " + Config.CHANGE_SKY_DOMAIN;
            if (cb != null) {
                cb.callback(mcr);
            }
       
            return false;
        }
        
        String memoUUID = AppCore.generateHex();
        
        logger.debug(ltag, "Generated hex " + memoUUID);
        
        int[] denominations = AppCore.getDenominations();
        int[] values = new int[denominations.length];
        for (int i = 0; i < denominations.length; i++) {
            if (cc.getDenomination() == denominations[i]) {
                values[i] = 1;
                break;
            }
        }
        
        logger.debug(ltag, "values " + Arrays.toString(values));
        
        //logger.debug(ltag, "send sn " + sn + " dstWallet " + dstFolder);
        //startSenderService(sn, dstFolder, amount, memo, remoteWalletName, cb);
        
        startSenderServiceForChange(sn, values, memoUUID, new eSenderChangeCb(cb, w, memoUUID, cc.getDenomination(), skySN));

        
        return true;
        
    }
    
    public void requestChange(CallbackInterface cb, Wallet w, String memoUUID, int denomination, int skySN) {
        makeChangeResult mcr = new makeChangeResult();
        mcr.status = 0;
        mcr.text = "Requesting Change from the ChangeServer";
        if (cb != null) {
            cb.callback(mcr);
        }
        
        logger.debug(ltag, "request change " + memoUUID + " d=" + denomination + " sky=" + skySN);

        
        RequestChange rc = new RequestChange(skySN, memoUUID, denomination, logger);
        if (!rc.request(getSR())) {
            mcr.errText = "Failed to query RequestChange service";
            if (cb != null) {
               cb.callback(mcr);
            }
            
            return;
        }
        
        mcr.text = "Downloading Change";
        if (cb != null) {
            cb.callback(mcr);
        }
    
        startShowSkyCoinsService(new eShowSkyCoinsCb(memoUUID, w, denomination, skySN, cb), skySN);
        
        //startReceiverService(skySN, sns, 0, 0, "", rcb);
        
    }
    
    public void receiveFromSkyWallet(CallbackInterface cb, Wallet w, ArrayList<Integer> coins, int total, int skySN) {
        makeChangeResult mcr = new makeChangeResult();
        mcr.status = 0;
        mcr.text = "Receiving change";
        if (cb != null) {
            cb.callback(mcr);
        }
        
        if (coins.size() == 0) {
            logger.error(ltag, "Size is zero");
            mcr.errText = "No coins received from your wallet";
            if (cb != null) {
                cb.callback(mcr);
            }
            return;
        }
        
        logger.debug(ltag, "Receive from Skywallet: " + skySN + " to " + w.getName());
        
        //sr.getServant("Receiver").changeUser(w.getName());
        
        int[] sns = new int[coins.size()];
        int i = 0;
        
        for (int sn : coins) {
            sns[i] = sn;
            i++;
        }
        
        startReceiverServiceForChange(skySN, sns, Config.CHANGE_SKY_DOMAIN, total, "Receive change", new eReceiverChangeCb(cb, w));
        //public void startReceiverService(int sn, int sns[], 
          //  String dstFolder, int amount, String memo, CallbackInterface cb) {
    }
    
    class eReceiverChangeCb implements CallbackInterface {
        CallbackInterface cb;
        makeChangeResult mcr;
        Wallet w;

        
        public eReceiverChangeCb(CallbackInterface cb, Wallet w) {
            this.cb = cb;
            this.w = w;
            mcr = new makeChangeResult();
            
        }
        public void callback(final Object result) {
            ReceiverResult rr = (ReceiverResult) result;
            
            logger.debug(ltag, "Change Receiver finished: " + rr.status);
            if (rr.status == ReceiverResult.STATUS_PROCESSING) {
                mcr.status = 1;
                mcr.progress = rr.totalRAIDAProcessed;
                mcr.text = "Receiving change from the SkyWallet";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                return;
            }
            
            if (rr.status == ReceiverResult.STATUS_CANCELLED) {
                mcr.errText = "Receiver Cancelled";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                
                return;
            }
            
            if (rr.status == ReceiverResult.STATUS_ERROR) {
                mcr.errText = "Receiver Error";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                
                return;
            }
            
            mcr.text = "Grading coins";
            if (cb != null)
                cb.callback(mcr);
            
            startGraderService(new eGraderCb(cb, w, rr.receiptId), null, w.getName());
        }
    }
    
    class eGraderCb implements CallbackInterface {
        Wallet w;
        makeChangeResult mcr;
        CallbackInterface cb;
        String receiptId;
        
        
        public eGraderCb(CallbackInterface cb, Wallet w, String receiptId) {
            this.w = w;
            this.cb = cb;
            this.receiptId = receiptId;
            mcr = new makeChangeResult();
        }
        
        public void callback(Object result) {
            GraderResult gr = (GraderResult) result;
            
            logger.debug(ltag, "Change Grader finished");
            
            //receiverReceiptId = rr.receiptId;
            int total = gr.totalAuthenticValue + gr.totalFrackedValue;
            
            w.appendTransaction("Received from ChangeMaker", total, receiptId);
            
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
    
    
    class eShowSkyCoinsCb implements CallbackInterface {
        
        String hash;
        CallbackInterface cb;
        int denomination;
        makeChangeResult mcr;
        int skySN;
        Wallet w;
        
        public eShowSkyCoinsCb(String hash, Wallet w, int denomination, int skySN, CallbackInterface cb) {
            this.hash = hash;
            this.cb = cb;
            this.denomination = denomination;
            this.skySN = skySN;
            this.w = w;
            mcr = new makeChangeResult();
        }
        
        public void callback(final Object result) {
            ShowEnvelopeCoinsResult sc = (ShowEnvelopeCoinsResult) result;


            ArrayList<Integer> coins = new ArrayList<Integer>();
            
            logger.debug(ltag, "ShowSky finished: " + sc.status + " coins: " + sc.coins.length);
            for (int i = 0; i < sc.tags.length; i++) {
                if (sc.tags[i].equals(hash)) {
                    logger.debug(ltag, "Our coin: " + sc.coins[i]);
                    coins.add(sc.coins[i]);
                }
            }
            
            int total = 0;
            for (int sn : coins) {
                CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, sn);
                
                logger.debug(ltag, "sn: " + sn + " d: " + cc.getDenomination());
                
                total += cc.getDenomination();
            }
            
            
            if (total != denomination) {
                logger.error(ltag, "Amount mismatch");
                mcr.errText = "Failed to find coins in your SkyWallet. We found only " + total;
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                
                return;
            }
       
            receiveFromSkyWallet(this.cb, w, coins, total, skySN);
        }
    }
    
    class eSenderChangeCb implements CallbackInterface {
        CallbackInterface cb;
        makeChangeResult mcr;
        String memoUUID;
        int denomination;
        int skySN;
        Wallet w;
        
        public eSenderChangeCb(CallbackInterface cb, Wallet w, String memoUUID, int denomination, int skySN) {
            this.cb = cb;
            this.memoUUID = memoUUID;
            this.denomination = denomination;
            this.skySN = skySN;
            this.w = w;
            mcr = new makeChangeResult();
        }
        
        public void callback(final Object result) {
            final SenderResult sr = (SenderResult) result;
            
            logger.debug(ltag, "Sender (Change) finished: " + sr.status);
            if (sr.status == SenderResult.STATUS_PROCESSING) {
                mcr.status = 1;
                mcr.progress = sr.totalRAIDAProcessed;
                mcr.text = "Sending coin to SkyChange";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                return;
            }
            
            
            if (sr.status == SenderResult.STATUS_CANCELLED) {
                mcr.errText = "Sender Cancelled";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
                
                return;
            }
      
            if (sr.status == SenderResult.STATUS_ERROR || sr.amount != denomination) {
                mcr.errText = "Failed to send coins to the Change Wallet. Check if they were valid";
                if (this.cb != null) {
                    this.cb.callback(mcr);
                }
		return;
            }   
            
            w.appendTransaction("Sent to ChangeMaker", sr.amount * -1, sr.receiptId);
            
            requestChange(this.cb, w, memoUUID, denomination, skySN);
                 
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
                sendToChange(cc, w, skySN, mcb);
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
    
    
    public boolean isRAIDAOK() {
        return sr.getServant("Echoer").updateRAIDAStatus();
    }
    
    
    public class makeChangeResult {
        public int status;
        
        public String text;
        public String errText = "";
        
        public int progress;
    }

}
