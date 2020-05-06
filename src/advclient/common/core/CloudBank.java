package global.cloudcoin.ccbank.core;

import advclient.AdvancedClient;
import advclient.common.core.Validator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.LossFixer.LossFixerResult;
import global.cloudcoin.ccbank.ServantManager.ServantManager;
import global.cloudcoin.ccbank.ServantManager.ServantManager.makeChangeResult;
import global.cloudcoin.ccbank.Unpacker.UnpackerResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class CloudBank {
    public String ltag = "CloudBank";
    GLogger logger;
    ServantManager sm;
    Wallet tmpWallet;
    
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

        AppCore.createDirectoryPath(dir);        
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
        
        if (!service.equals("echo")) {
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
                     
                logger.debug(ltag, "Setting password " + password);
                System.out.println("setting password " + password);
                lw.setPassword(password);
            }
        }
        
        if (service.equals("withdraw_one_stack")) {
            cr = withdrawOneStack(params, lw, rw, cb);
        } else if (service.equals("deposit_one_stack")) {
            cr = depositOneStack(params, lw, rw, cb);
        } else if (service.equals("echo")) {
            cr = echo();
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
        cr.keepWallet = false;
        
        return cr;
    }
    
    public CloudbankResult getCrSuccess(String text) {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_OK;
        cr.message = text.replaceAll("\"", "\\\\\"").replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
        cr.keepWallet = false;
        
        return cr;
    }

    public CloudbankResult echo() {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_OK_CUSTOM;
        cr.message = "Server is ready";
        cr.ownStatus = "ready";
        return cr;
    }

    private boolean checkFolders(Wallet wallet) {
        if (wallet.isSkyWallet())
                return true;
        
        int cnt = AppCore.getFilesCount(Config.DIR_DETECTED, wallet.getName());
        if (cnt != 0) {
            logger.debug(ltag, "Detected dir cnt files: " + cnt);
            return false;
        }
        
        cnt = AppCore.getFilesCount(Config.DIR_SUSPECT, wallet.getName());
        if (cnt != 0) {
            logger.debug(ltag, "Suspect dir cnt files: " + cnt);
            return false;
        }
        
        cnt = AppCore.getFilesCount(Config.DIR_IMPORT, wallet.getName());
        if (cnt != 0) {
            logger.debug(ltag, "Import dir cnt files: " + cnt);
            return false;
        }
        
        return true;
    }
    
    private String getReceiptName(Wallet wallet, String rn) {
        return AppCore.getUserDir(Config.DIR_RECEIPTS, wallet.getName()) + File.separator + "r" + rn + ".txt";
    }
    
    public void setReceipt(Wallet wallet, int total, String status, String message, String rn) {
        String rfile = getReceiptName(wallet, rn);
        File f = new File(rfile);
        if (f.exists())
            f.delete();
        
        StringBuilder rsb = new StringBuilder();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:a");
        SimpleDateFormat formatterTz = new SimpleDateFormat("z");
        Date date = new Date(System.currentTimeMillis());

        String cdate = formatter.format(date);
        String cdateFormat = formatterTz.format(date);

        rsb.append("{\"receipt_id\": \"");
        rsb.append(rn);
        rsb.append("\", \"time\": \"");
        rsb.append(cdate);
        rsb.append("\", \"timezone\": \"");
        rsb.append(cdateFormat);
        rsb.append("\", \"status\": ");
        rsb.append(status);
        rsb.append("\", \"message\": ");
        rsb.append(message);
        rsb.append("\", \"total_authentic\": ");
        rsb.append(0);
        rsb.append(", \"total_fracked\": ");
        rsb.append(0);
        rsb.append(", \"total_counterfeit\": ");
        rsb.append(0);
        rsb.append(", \"total_lost\": ");
        rsb.append(0);
        rsb.append(", \"total_unchecked\": ");
        rsb.append(total);
        rsb.append(", \"prev_imported\": ");
        rsb.append(0);
        rsb.append(", \"receipt_detail\": [");
        //rsb.append(csb);
        rsb.append("]}");

        if (!AppCore.saveFile(rfile, rsb.toString())) {
            logger.error(ltag, "Failed to save file " + rfile);
        } 
        
    }
    
    public CloudbankResult depositOneStack(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
        Wallet wallet = lw;
        
        if (!checkFolders(wallet)) {
            logger.error(ltag, "Detected, Import or Suspect dir aren't empty");
            return getCrError("Failed to deposit. Detected, Import or Suspect dir aren't empty");
        }
        
        String rn = (String) params.get("rn");
        if (rn == null) {
            rn = AppCore.generateHex();
        }
        
        rn = rn.toUpperCase();
        
        final String rfile = AppCore.getUserDir(Config.DIR_RECEIPTS, lw.getName()) + File.separator + rn + ".txt";
        File f = new File(rfile);
        if (f.exists()) {
            logger.debug(ltag, "Recipt " + rfile + " already exists");
            return getCrError("Receipt already exists");
        }
        
        String tag = Config.DEFAULT_TAG;
        String ptag = (String) params.get("memo");
        if (ptag != null) {
            tag = ptag;
        }
        
        String stack = (String) params.get("stack");
        if (stack == null) {
            logger.debug(ltag, "No stack defined");
            return getCrError("Stack is required");
        }
        
        final String filename = AppCore.getUserDir(Config.DIR_IMPORT, wallet.getName()) + File.separator + rn + ".stack";
        System.out.println("d="+filename);
        System.out.println("s="+stack);
        logger.debug(ltag, "Saving file " + filename);
        if (!AppCore.saveFile(filename, stack)) {
            logger.error(ltag, "Failed to save file");
            return getCrError("Failed to save file");
        }
        
        sm.setActiveWalletObj(wallet);
   
        final CallbackInterface fcb = cb;
        final String frn = rn;
        final String ftag = tag;
        sm.startUnpackerService(new CallbackInterface() {
            public void callback(Object o) {
                final UnpackerResult ur = (UnpackerResult) o;
                logger.debug(ltag, "Unpacker finished " + ur.status);
                
                System.out.println("up=" + ur.status + " ff="+ur.failedFiles);
                if (ur.status == UnpackerResult.STATUS_ERROR || ur.failedFiles > 0) {
                    AppCore.deleteFile(filename);
                    logger.error(ltag, "err " + ur.errText);
                    fcb.callback(getCrError("Unpack failed. Make sure stack file is not corrupted"));
                    return;
                }
                
                if (ur.status == UnpackerResult.STATUS_FINISHED) {
                    logger.error(ltag, "Finished");
                    CloudbankResult cr = getCrSuccess(frn);
                    cr.status = CloudbankResult.STATUS_OK_CUSTOM;
                    cr.ownStatus = "importing";
                    cr.keepWallet = true;
                    
                    final int fsize = ur.unpacked.size();
                    setReceipt(wallet, fsize, "importing", "Importing CloudCoins", frn);
                    fcb.callback(cr);                    
                    sm.startAuthenticatorService(new AuthenticatorCb(wallet, frn, ftag, ur.duplicates));
                    
                    return;
                }
                
                fcb.callback(getCrError("Unpack unknown error"));
                return;
                
            }
        });
        
        return null;
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
        
        logger.debug(ltag, "Requested: " + amount + "CC, lw: " + lw.getTotal() + "CC, rw: " + rw.getTotal() + "CC");
        
        if (lw.getTotal() >= amount) {
            logger.debug(ltag, "Choosing local wallet: " + lw.getName());
            wallet = lw;
        } else if (rw.getTotal() >= amount) {
            logger.debug(ltag, "Choosing sky wallet: " + rw.getName());
            //wallet = rw;
            return getCrError("Insufficient funds in Local Wallet");
        } else {
            logger.debug(ltag, "Insufficient funds");
            return getCrError("Insufficient funds in Local Wallet");
        }
        
        if (!checkFolders(wallet)) {
            logger.error(ltag, "Detected, Import or Suspect dir aren't empty");
            return getCrError("Failed to withdraw. Detected, Import or Suspect dir aren't empty");
        }
       
        String tag = Config.DEFAULT_TAG;
        String ptag = (String) params.get("base64");
        if (ptag != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(ptag);
            tag = new String(decodedBytes);
        }
        
        if (!Validator.memo(tag)) {
            logger.error(ltag, "Invalid tag");
            return getCrError("Invalid memo");
        }
        
        String dir = getAccountDir();
        logger.debug(ltag, "Exporting to " + dir);

        logger.debug(ltag, "Setting wallet to  " + wallet.getName());
        sm.setActiveWalletObj(wallet);

        final CallbackInterface fcb = cb;
        CallbackInterface cbi = new CallbackInterface() {
            public void callback(Object o) {
                logger.debug(ltag, "Ready to return result");

                fcb.callback(o);
            }
        };

        if (wallet.isEncrypted()) {
            sm.startSecureExporterService(Config.TYPE_STACK, amount, tag, dir, false, new ExporterCb(wallet, amount, tag, false, cbi));
        } else {
            sm.startExporterService(Config.TYPE_STACK, amount, tag, dir, false, new ExporterCb(wallet, amount, tag, false, cbi));
        }
        
        return null;
    }


    class AuthenticatorCb implements CallbackInterface {
        ArrayList<CloudCoin> duplicates;
        Wallet wallet;
        String tag;
        String rn;
        
        public AuthenticatorCb(Wallet wallet, String rn, String tag, ArrayList<CloudCoin> duplicates) {
            this.duplicates = duplicates;
            this.wallet = wallet;
            this.tag = tag;
            this.rn = rn;
        }
        
        public void callback(Object o) {
            final AuthenticatorResult ar = (AuthenticatorResult) o;
            logger.debug(ltag, "Authenticator finished " + ar.status);
            
            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                logger.error(ltag, "Authenticator failed");
                return;
            }
            
            if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                logger.debug(ltag, "Auth Finished. Launching grader");
                sm.startGraderService(new CallbackInterface() {
                    public void callback(Object o) {
                        final GraderResult gr = (GraderResult) o;
                        logger.debug(ltag, "Grader finished " + ar.status);
                        
                        int statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
                        int statFailedValue = gr.totalCounterfeitValue;
                        int statLostValue = gr.totalLostValue;
                        int statToBank = gr.totalAuthentic + gr.totalFracked;
                        int statFailed = gr.totalCounterfeit;
                        int statLost = gr.totalLost + gr.totalUnchecked;
                        String receiptId = gr.receiptId;
   

                        if (statToBankValue != 0) {
                            wallet.appendTransaction(tag, statToBankValue, receiptId);
                        } else {
                            String memo = "";
                            if (statFailedValue > 0) {
                                memo = AppCore.formatNumber(statFailedValue) + " Counterfeit";
                            } else {
                                memo = "Failed to Import";
                            }
                            
                            wallet.appendTransaction(memo, 0, "COUNTERFEIT");
                        }
                        
                        logger.debug(ltag, "Grader finished");
                        
                        sm.startFrackFixerService(new CallbackInterface() {
                            public void callback(Object o) {
                                FrackFixerResult fr = (FrackFixerResult) o;
                                logger.debug(ltag, "Frackfixer finished: " + fr.status);
                                
                                if (fr.status == FrackFixerResult.STATUS_ERROR) {
                                    logger.debug(ltag, "Failed to fix");
                                }
                                
                                sm.startLossFixerService(new CallbackInterface() {
                                    public void callback(Object o) {
                                        LossFixerResult lr = (LossFixerResult) o;
                                        logger.debug(ltag, "Lossfixer finished: " + lr.status);
                                        
                                        if (lr.recovered > 0) {
                                            wallet.appendTransaction("LossFixer Recovered", lr.recoveredValue, lr.receiptId);
                                        }
                                        
                                        if (wallet.isEncrypted()) {
                                            logger.debug(ltag, "Encrypting again");
                                            sm.startVaulterService(null, wallet.getPassword());
                                        }
                                    }
                                });
                                
                            }
                        }, false, wallet.getEmail());          
                    }
                }, duplicates, null, rn);
            }
        }
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
                    
                    return;
                } 
                
                cb.callback(getCrError("Failed to export Coins"));
                return;
                
            }

            if (er.status == ExporterResult.STATUS_FINISHED) {
                logger.debug(ltag, "We are finally done");
                
                String filename = er.exportedFileNames.get(0);
                
                logger.debug(ltag, "Exported = " + filename);
                String stack = AppCore.loadFile(filename);
                if (stack == null) {
                    logger.error(ltag, "Failed to load exported file");
                    cb.callback(getCrError("Failed to load exported stack"));
                    return;
                }
                
                wallet.appendTransaction(tag, er.totalExported * -1, er.receiptId);
                wallet.setNotUpdated();
                
                AppCore.deleteFile(filename);
                cb.callback(getCrSuccess(stack));                
                
            }
                
        }
    
    }
}