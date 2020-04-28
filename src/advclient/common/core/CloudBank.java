package global.cloudcoin.ccbank.core;

import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.ServantManager.ServantManager;
import global.cloudcoin.ccbank.ServantManager.ServantManager.makeChangeResult;
import java.io.File;
import java.util.Base64;
import java.util.Map;

public class CloudBank {
    public String ltag = "CloudBank";
    GLogger logger;
    ServantManager sm;
    
    public CloudBank(GLogger logger, ServantManager sm) {
        this.logger = logger;
        this.sm = sm;
    }
    
    public String getAccountDir() {
        return AppCore.getCloudbankDir() + File.separator + Config.CLOUDBANK_ACCOUNT + "." + Config.CLOUDBANK_PASSWORD;
    }
    
    public void init() {
        String dir = getAccountDir();
        logger.debug(ltag, "Initializing CloudBank: " + dir);

        System.out.println("dir=" +dir);
        AppCore.createDirectory(dir);        
    }
    
    public void startCloudbankService(String service, Map params, CallbackInterface cb) {
        logger.debug(ltag, "Starting CloudBank service " + service);
        CloudbankResult cr;
        String wHash;
  
        Wallet lw = sm.getWalletByName(Config.CLOUDBANK_LWALLET);
        Wallet rw = sm.getWalletByName(Config.CLOUDBANK_RWALLET);
        if (lw == null || rw == null) {
            cr = getCrError("CloudBank is not set up properly");
            cb.callback(cr);
            return;
        }
        
        String pk = (String) params.get("pk");
        String account = (String) params.get("account");
        if (pk == null || account == null) {
            cr = getCrError("Account and pk required");
            cb.callback(cr);
            return;
        }
        
        if (!account.equals(Config.CLOUDBANK_ACCOUNT) || !pk.equals(Config.CLOUDBANK_PASSWORD)) {
            cr = getCrError("Auth Failed");
            cb.callback(cr);
            return;
        }
        
        if (lw.isEncrypted()) {
            wHash = lw.getPasswordHash();
            if (wHash == null) {
                cr = getCrError("Local Wallet is corrupted");
                cb.callback(cr);
                return;
            }
            
            String password = Config.CLOUDBANK_LWALLET_PASSWORD;
            String ek = (String) params.get("ek");
            if (ek != null)
                password = ek;
            
            String providedHash = AppCore.getMD5(password);
            if (!wHash.equals(providedHash)) {
                cr = getCrError("Incorrect Password for Local Wallet");
                cb.callback(cr);
                return;
            }
                        
            lw.setPassword(password);
        }
        
        if (service.equals("withdraw_one_stack")) {
            cr = withdrawOneStack(params, lw, rw, cb);
        } else {
            cr = getCrError("Invalid service");
        }

        if (cr != null) {
            cb.callback(cr);
            return;
        }
        
        return;
        
    }

    public CloudbankResult getCrError(String errTxt) {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_ERROR;
        cr.message = errTxt;
        
        return cr;
    }

    public CloudbankResult withdrawOneStack(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
        Wallet wallet = null;
        String amountStr = (String) params.get("amount");
        if (amountStr == null) 
            return getCrError("Amount must be set");
      
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return getCrError("Invalid amount");
        }
        
        logger.debug(ltag, "Requested: " + amount + " lw: " + lw.getTotal() + " rw: " + rw.getTotal());
        
        if (lw.getTotal() >= amount) {
            logger.debug(ltag, "Choosing local wallet: " + lw.getName());
            wallet = lw;
        } else if (rw.getTotal() >= amount) {
            logger.debug(ltag, "Choosing sky wallet: " + rw.getName());
            //wallet = rw;
            return getCrError("Insufficient funds");
        } else {
            logger.debug(ltag, "Insufficient funds");
            return getCrError("Insufficient funds in Local Wallet");
        }
        
        if (!wallet.isSkyWallet()) {
            int cnt = AppCore.getFilesCount(Config.DIR_DETECTED, wallet.getName());
            if (cnt != 0) {
                logger.error(ltag, "Detected dir isn't empty");
                return getCrError("Failed to withdraw. Detected dir isn't empty");
            }
        }
       
        String tag = Config.DEFAULT_TAG;
        String ptag = (String) params.get("base64");
        if (ptag != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(ptag);
            tag = new String(decodedBytes);
        }
        
        String dir = getAccountDir();
        System.out.println("dir="+dir);
        logger.debug(ltag, "Exporting to " + dir);
        
        final Wallet fwallet = wallet;
        final int famount = amount;
        final String ftag = tag;
        final boolean keepSrc = true;
        sm.changeServantUser("Exporter", wallet.getName());
        
         
        sm.startExporterService(Config.TYPE_STACK, amount, tag, dir, keepSrc, new ExporterCb(wallet, amount, tag, false, cb));
        if (wallet.isEncrypted()) {
            sm.startSecureExporterService(Config.TYPE_STACK, amount, tag, dir, false, new ExporterCb(wallet, amount, tag, true, cb));
        } else {
            sm.startExporterService(Config.TYPE_STACK, amount, tag, dir, false, new ExporterCb(wallet, amount, tag, true, cb));
        }
        
        System.out.println("ret null");
        return null;
    }


    class ExporterCb implements CallbackInterface {
        Wallet wallet;
        String tag;
        int amount;
        String dir;
        CallbackInterface cb;
        boolean fromChange;
        
        public ExporterCb(Wallet wallet, int amount, String tag, boolean fromChange, CallbackInterface cb) {
            this.dir = getAccountDir();
            this.wallet = wallet;
            this.amount = amount;
            this.tag = tag;
            this.cb = cb;        
            this.fromChange = fromChange;
        }
        
        public void callback(Object o) {
            ExporterResult er = (ExporterResult) o;    
            if (er.status == ExporterResult.STATUS_ERROR) {
                if (er.errText == Config.PICK_ERROR_MSG) {
                    if (this.fromChange) {
                        logger.debug(ltag, "From change true, giving up");
                        cb.callback(getCrError("Not enough coins after change"));
                        return;
                    }
                    logger.debug(ltag, "Pick error. Will make Change"); 
                    System.out.println("ready for change");
                    sm.makeChange(wallet, amount, new CallbackInterface() {
                        public void callback(Object o) {
                            makeChangeResult mcr = (makeChangeResult) o;                               
                            if (!mcr.errText.isEmpty()) {
                                logger.error(ltag, "Failed to make change: " + mcr.errText);
                                cb.callback(getCrError("Failed to make change"));
                                return;
                            }
                        
                            if (mcr.status == 1) { 
                            } else if (mcr.status == 2) {
                                logger.debug(ltag, "Change done successfully. Retrying");                                                             
                                if (wallet.isEncrypted()) {
                                    sm.startSecureExporterService(Config.TYPE_STACK, amount, tag, dir, false, new ExporterCb(wallet, amount, tag, true, cb));
                                } else {
                                    sm.startExporterService(Config.TYPE_STACK, amount, tag, dir, false, new ExporterCb(wallet, amount, tag, true, cb));
                                }
                            }                                    
                        }
                    });                           
                }                   
            }

            System.out.println("exp=" + er.status);
                
        }
    
    }
}