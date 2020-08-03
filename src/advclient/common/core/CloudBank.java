package global.cloudcoin.ccbank.core;

import advclient.AdvancedClient;
import advclient.AppUI;
import advclient.common.core.Validator;
import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;
import global.cloudcoin.ccbank.Exporter.ExporterResult;
import global.cloudcoin.ccbank.FrackFixer.FrackFixerResult;
import global.cloudcoin.ccbank.Grader.GraderResult;
import global.cloudcoin.ccbank.LossFixer.LossFixerResult;
import global.cloudcoin.ccbank.Receiver.ReceiverResult;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.ServantManager.ServantManager;
import global.cloudcoin.ccbank.ServantManager.ServantManager.makeChangeResult;
import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoinsResult;
import global.cloudcoin.ccbank.Transfer.TransferResult;
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
    boolean isBusy;
    
    
    public CloudBank(GLogger logger, ServantManager sm) {
        this.logger = logger;
        this.sm = sm;
        this.isBusy = false;
    }
    
    public String getAccountDir() {
        return AppCore.getCloudbankDir() + File.separator + Config.CLOUDBANK_ACCOUNT + "." + Config.CLOUDBANK_PASSWORD;
    }
    
    public void init() {
        String dir = getAccountDir();
        logger.debug(ltag, "Initializing CloudBank: " + dir);

        AppCore.createDirectoryPath(dir);        
    }
    
    
    
    public void setBusy() {
        this.isBusy = true;
    }
    
    public void setNonBusy() {
        this.isBusy = false;
    }
    
    public boolean isBusy() {
        return this.isBusy;
    }
        
    public void startCloudbankService(String service, Map params, CallbackInterface cb) {
        logger.debug(ltag, "Starting CloudBank service " + service);
        CloudbankResult cr;
        String wHash;
  
        Wallet lw = sm.getWalletByName(Config.CLOUDBANK_LWALLET);
        Wallet rw = sm.getWalletByName(Config.CLOUDBANK_RWALLET);
        if (lw == null || rw == null) {
            cr = getCrErrorKeepWallet("CloudBank is not set up properly");
            cb.callback(cr);
            return;
        }
        
        if (!service.equals("print_welcome") && !service.equals("echo")) {
            
            String account = (String) params.get("account");
            if (account == null) {
                cr = getCrErrorKeepWallet("Account is required");
                cb.callback(cr);
                return;
            }
        
            if (!account.equals(Config.CLOUDBANK_ACCOUNT)) {
                cr = getCrErrorKeepWallet("Unknown Account");
                cb.callback(cr);
                return;
            }
            
            if (!service.equals("deposit_one_stack") && !service.equals("get_receipt")) {
                String pk = (String) params.get("pk");
                if (pk == null) {
                    cr = getCrErrorKeepWallet("Pk is required");
                    cb.callback(cr);
                    return;
                }
                
                if (!pk.equals(Config.CLOUDBANK_PASSWORD)) {
                    cr = getCrErrorKeepWallet("Auth Failed");
                    cb.callback(cr);
                    return;
                }
            }
            
        
            if (lw.isEncrypted()) {
                wHash = lw.getPasswordHash();
                if (wHash == null) {
                    cr = getCrErrorKeepWallet("Local Wallet is corrupted");
                    cb.callback(cr);
                    return;
                }
            
                String password = Config.CLOUDBANK_LWALLET_PASSWORD;
                String ek = (String) params.get("ek");
                if (ek != null)
                    password = ek;
            
                String providedHash = AppCore.getMD5(password);
                if (!wHash.equals(providedHash)) {
                    cr = getCrErrorKeepWallet("Incorrect Password for Local Wallet");
                    cb.callback(cr);
                    return;
                }
                     
                logger.debug(ltag, "Setting password " + password);
                lw.setPassword(password);
            }
        }
        
        if (service.equals("withdraw_one_stack")) {
            cr = withdrawOneStack(params, lw, rw, cb);
        } else if (service.equals("deposit_one_stack")) {
            cr = depositOneStack(params, lw, rw, cb);
        } else if (service.equals("get_receipt")) {
            cr = getReceipt(params, lw, rw, cb);
        } else if (service.equals("show_coins")) {
            cr = showCoins(params, lw, rw, cb);
        } else if (service.equals("send_to_skywallet")) {
            cr = sendToSkyWallet(params, lw, rw, cb);
        } else if (service.equals("transfer_between_skywallets")) {
            cr = transferBetweenSkyWallets(params, lw, rw, cb);
        } else if (service.equals("receive_from_skywallet")) {
            cr = receiveFromSkyWallet(params, lw, rw, cb);
        } else if (service.equals("print_welcome")) {
            cr = printWelcome();
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
    
    public CloudbankResult getCrErrorKeepWallet(String errTxt) {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_ERROR;
        cr.message = errTxt;
        cr.keepWallet = true;
        
        return cr;
    }
    
    
    public CloudbankResult getCrSuccess(String text) {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_OK;
        cr.message = text.replaceAll("\"", "\\\\\"").replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
        cr.keepWallet = false;
        
        return cr;
    }

    public CloudbankResult printWelcome() {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_OK_CUSTOM;
        cr.message = "CloudCoin Bank. Used to Authenticate, "
                + "Store and Payout CloudCoins. This Software is provided as is with all faults, "
                + "defects and errors, and without warranty of any kind. "
                + "Free from the CloudCoin Consortium";
        cr.ownStatus = "welcome";
        cr.keepWallet = true;
        return cr;
    }
    
    public CloudbankResult echo() {
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_OK_JSON;
        cr.message = "";
        cr.keepWallet = true;
        
        
        sm.startEchoService(new CallbackInterface() {
            public void callback(Object o) {
                String message = "The RAIDA is ready for counterfeit detection.";
                String status = "ready";
                int readyCount = 0;
                int notReadyCount = 0;
                int[] statuses = sm.getRAIDAStatuses();
                for (int i = 0; i < statuses.length; i++) {
                    if (statuses[i] == -1)
                        notReadyCount++;
                    else
                        readyCount++;
                }
                
                String dateStr = AppCore.getDate("" + (System.currentTimeMillis() /1000));
                StringBuilder sb = new StringBuilder();
        
                if (isBusy())
                    status = "busy";
                
                sb.append("{\"server\":\"");
                sb.append(AppUI.brand.getTitle(null));
                sb.append("\", \"status\":\"");
                sb.append(status);        
                sb.append("\", \"message\":\"");
                sb.append(message);
                sb.append("\", \"time\":\"");
                sb.append(dateStr);
                sb.append("\", \"version\": \"");
                sb.append(AppUI.brand.getResultingVersion(AdvancedClient.version));
                sb.append("\", \"readyCount\": ");
                sb.append(readyCount);
                sb.append(", \"readyCount\": ");
                sb.append(notReadyCount);
                sb.append("}");

                cr.message = sb.toString();
            }
        });
        
        while (cr.message.isEmpty()) {
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }
        
        
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
    
    private String getReceiptLocalName(Wallet wallet, String rn) {
        return AppCore.getUserDir(Config.DIR_RECEIPTS, wallet.getName()) + File.separator + rn + ".txt";
    }
    
    
    public void rmReceipt(Wallet wallet, String rn) {
        String rfile = getReceiptName(wallet, rn);
        File f = new File(rfile);
        if (f.exists())
            f.delete();
    }
    
    public void setErrorReceipt(Wallet wallet, int total, String message, String rn) {
        setReceipt(wallet, total, "error", message, rn);
    }
    
    
    public void setReceipt(Wallet wallet, int total, String status, String message, String rn) {
        setReceiptGeneral(wallet, total, status, message, 0, 0, 0, rn);
    }
    
    public void setReceiptGeneral(Wallet wallet, int total, String status, String message, int a, int f, int c, String rn) {
        String rfile = getReceiptName(wallet, rn);
        File fl = new File(rfile);
        if (fl.exists())
            fl.delete();
        
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
        rsb.append("\", \"status\": \"");
        rsb.append(status);
        rsb.append("\", \"message\": \"");
        rsb.append(message);
        rsb.append("\", \"total_authentic\": ");
        rsb.append("" + a);
        rsb.append(", \"total_fracked\": ");
        rsb.append("" + f);
        rsb.append(", \"total_counterfeit\": ");
        rsb.append("" + c);
        rsb.append(", \"total_lost\": ");
        rsb.append(0);
        rsb.append(", \"total_unchecked\": ");
        rsb.append(total);
        rsb.append(", \"prev_imported\": ");
        rsb.append(0);
        rsb.append(", \"total_value\": ");
        rsb.append(0);
        rsb.append(", \"receipt\": [");
        //rsb.append(csb);
        rsb.append("]}");

        if (!AppCore.saveFile(rfile, rsb.toString())) {
            logger.error(ltag, "Failed to save file " + rfile);
        } 
        
    }
    
    private void fillHeader(StringBuilder sb, String status) {
        String dateStr = AppCore.getDate("" + (System.currentTimeMillis() /1000));
        
        sb.append("{\"server\":\"");
        sb.append(AppUI.brand.getTitle(null));
        sb.append("\", \"status\":\"");
        sb.append(status);
        sb.append("\", \"time\":\"");
        sb.append(dateStr);
        sb.append("\", \"version\": \"");
        sb.append(AppUI.brand.getResultingVersion(AdvancedClient.version));
        sb.append("\"");
    }
    
    public CloudbankResult showCoins(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
        Wallet wallet = lw;
        
        int[][] counters = wallet.getCounters();
        if (counters == null || counters.length == 0) {
            return getCrErrorKeepWallet("Failed to find Coins");
        }
        
        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);

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
        
        StringBuilder sb = new StringBuilder();
        fillHeader(sb, "coins_shown");
        
        sb.append(",\"ones\": ");
        sb.append(t1);        
        sb.append(", \"fives\": ");
        sb.append(t5);
        sb.append(", \"twentyfives\": ");
        sb.append(t25);        
        sb.append(", \"hundreds\": ");
        sb.append(t100);
        sb.append(", \"twohundredfifties\": ");
        sb.append(t250);
        sb.append("}");
        
        CloudbankResult cr = new CloudbankResult();
        cr.status = CloudbankResult.STATUS_OK_JSON;
        cr.message = sb.toString();
        cr.keepWallet = true;
    
        return cr;
    }
    
    public CloudbankResult getReceipt(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
        Wallet wallet = lw;
        
        String rn = (String) params.get("rn");
        if (rn == null) {
            return getCrErrorKeepWallet("RN parameter is required");
        }
        
        String rfile = getReceiptName(wallet, rn);
        File f = new File(rfile);
        if (f.exists()) {
            String json = AppCore.loadFile(rfile);
            if (json == null) {
                logger.error(ltag, "Failed to load file " + rfile);
                return getCrErrorKeepWallet("Internal error");
            }
            
            json = json.replace("receipt_detail", "receipt");
            
            CloudbankResult cr = new CloudbankResult();
            cr.status = CloudbankResult.STATUS_OK_JSON;
            cr.message = json;
            cr.keepWallet = true;
            
            return cr;
        }
        
        String lfile = getReceiptLocalName(wallet, rn);
        f = new File(lfile);
        if (f.exists()) {
            String json = AppCore.loadFile(lfile);
            if (json == null) {
                logger.error(ltag, "Failed to load file " + lfile);
                return getCrErrorKeepWallet("Internal error");
            }
            
            json = json.replace("receipt_detail", "receipt");
            
            CloudbankResult cr = new CloudbankResult();
            cr.status = CloudbankResult.STATUS_OK_JSON;
            cr.message = json;
            cr.keepWallet = true;
            
            return cr;
        }
        
        return getCrErrorKeepWallet("Receipt not found");
    }
    
    public CloudbankResult receiveFromSkyWallet(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
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
        
        String memo = Config.DEFAULT_TAG;
        String ptag = (String) params.get("base64");
        if (ptag != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(ptag);
            memo = new String(decodedBytes);
            //memo = ptag;
        }
        
        if (!Validator.memo(memo)) {
            logger.error(ltag, "Invalid tag");
            return getCrError("Invalid memo");
        }

        if (rw.getTotal() == 0) {
            return getCrError("Remote wallet is empty");
        }

        
        logger.debug(ltag, "Requested To Receive: " + rw.getName());
        
        CloudbankResult cr = getCrSuccess(rn);
        cr.status = CloudbankResult.STATUS_OK_CUSTOM;
        cr.ownStatus = "receiving";
        cr.message = "Receiving will begin automatically. Please check your reciept.";
        cr.receipt = rn;
        cr.keepWallet = true;
                    
        setReceipt(lw, rw.getTotal(), "receiving", "Receiving CloudCoins", rn);


        sm.setActiveWalletObj(lw);
        final int total = rw.getTotal();
        final String frn = rn;
        final String fmemo = memo;
        sm.startReceiverService(rw.getIDCoin().sn, rw.getSNs(), lw.getName(), total, memo, rn, new CallbackInterface() {
            public void callback(Object result) {
                final ReceiverResult rr = (ReceiverResult) result;

                if (rr.status == ReceiverResult.STATUS_PROCESSING)
                    return;
            
                logger.debug(ltag, "Receiver finished: " + rr.status);
                if (rr.status == ReceiverResult.STATUS_ERROR || rr.status == ReceiverResult.STATUS_CANCELLED) {
                    setErrorReceipt(lw, total, "Receiver Failed", frn);
                    return;
                }
                
                logger.debug(ltag, "Starting Grader");
                sm.startGraderService(new CallbackInterface() {
                    public void callback(Object o) {
                        final GraderResult gr = (GraderResult) o;
                        logger.debug(ltag, "Grader finished");
                        
                        int statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
                        int statFailedValue = gr.totalCounterfeitValue;
                        String receiptId = gr.receiptId;
   
                        if (statToBankValue != 0) {
                            lw.appendTransaction(fmemo, statToBankValue, receiptId);
                        } else {
                            String memo = "";
                            if (statFailedValue > 0) {
                                memo = AppCore.formatNumber(statFailedValue) + " Counterfeit";
                            } else {
                                memo = "Failed to Receiver";
                            }
                            
                            lw.appendTransaction(memo, 0, "COUNTERFEIT");
                            //setErrorReceipt(lw, total, "All Coins are counterfeit", frn);
                            setReceiptGeneral(lw, 0, "error", "All Coins are counterfeit", 0, 0, total, frn);
                            return;
                        }
                        
                        logger.debug(ltag, "Grader finished 2");
                        rmReceipt(lw, frn);
                        
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
                                            lw.appendTransaction("LossFixer Recovered", lr.recoveredValue, lr.receiptId);
                                        }
                                        
                                        if (lw.isEncrypted()) {
                                            logger.debug(ltag, "Encrypting again");
                                            sm.startVaulterService(null, lw.getPassword());
                                        }
                                    }
                                });
                                
                            }
                        }, false, lw.getEmail());          
                    }
                }, null, null, frn);
             
            }
	});
        
        return cr;
        
    }
    
    
    public CloudbankResult transferBetweenSkyWallets(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
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
        
        String memo = Config.DEFAULT_TAG;
        String ptag = (String) params.get("base64");
        if (ptag != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(ptag);
            memo = new String(decodedBytes);
            //memo = ptag;
        }
        
        if (!Validator.memo(memo)) {
            logger.error(ltag, "Invalid tag");
            return getCrError("Invalid memo");
        }
        
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
        
        if (rw.getTotal() < amount) {
            return getCrError("Insufficient funds");
        }
        
        String to = (String) params.get("to");
        if (to == null) 
            return getCrError("To is a mandatory parameter");
        
        if (!Validator.domain(to))
            return getCrError("Invalid To SkyWallet");
        
        DNSSn d = new DNSSn(to, null, logger);
        int sn = d.getSN();
        if (sn < 0) 
            return getCrError("Failed to resolve Wallet");
        
        
        logger.debug(ltag, "Requested To Transfer: " + amount + " to " + to + " sn:" + sn);
        
        CloudbankResult cr = getCrSuccess(rn);
        cr.status = CloudbankResult.STATUS_OK_CUSTOM;
        cr.ownStatus = "sending";
        cr.message = "Sending will begin automatically. Please check your reciept.";
        cr.receipt = rn;
        cr.keepWallet = true;
                    
        setReceipt(lw, amount, "transferring", "Sending CloudCoins", rn);

        final String frn = rn;
        final String fto = to;

        sm.setActiveWalletObj(rw);

        sm.startTransferService(rw.getIDCoin().sn, sn, rw.getSNs(), amount, memo, new CallbackInterface() {
            public void callback(Object o) {
                TransferResult tr = (TransferResult) o;
                
                logger.debug(ltag, "Sender (Depostit) finished: " + tr.status);
                if (tr.status == TransferResult.STATUS_PROCESSING) {
                    return;
                }

                if (tr.status == TransferResult.STATUS_CANCELLED || tr.status == TransferResult.STATUS_ERROR) {
                    logger.debug(ltag, "Failed to transfer coins");
                    setErrorReceipt(lw, amount, "Transfer Failed or Cancelled", frn);
                    return;
                }
      
                
            
                if (tr.status == ExporterResult.STATUS_FINISHED) {
                    logger.debug(ltag, "We are finally done");
                
                    int total = tr.totalCoins;
                    setReceiptGeneral(lw, total, "sent", "Coins sent successfully to " + fto, 0, 0, 0, frn);
                }
            }
        }); 
        
        return cr;
        
    }
    
    
    public CloudbankResult sendToSkyWallet(Map params, Wallet lw, Wallet rw, CallbackInterface cb) {
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
        
        String memo = Config.DEFAULT_TAG;
        String ptag = (String) params.get("base64");
        if (ptag != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(ptag);
            memo = new String(decodedBytes);
            //memo = ptag;
        }
        
        if (!Validator.memo(memo)) {
            logger.error(ltag, "Invalid tag");
            return getCrError("Invalid memo");
        }
        
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
        
        if (lw.getTotal() < amount) {
            return getCrError("Insufficient funds");
        }
        
        String to = (String) params.get("to");
        if (to == null) 
            return getCrError("To is a mandatory parameter");
        
        if (!Validator.domain(to))
            return getCrError("Invalid To SkyWallet");
        
        DNSSn d = new DNSSn(to, null, logger);
        int sn = d.getSN();
        if (sn < 0) 
            return getCrError("Failed to resolve Wallet");
        
        
        logger.debug(ltag, "Requested To Send: " + amount + " to " + to + " sn:" + sn);
        
        CloudbankResult cr = getCrSuccess(rn);
        cr.status = CloudbankResult.STATUS_OK_CUSTOM;
        cr.ownStatus = "sending";
        cr.message = "Sending will begin automatically. Please check your reciept.";
        cr.receipt = rn;
        cr.keepWallet = true;
                    
        setReceipt(lw, amount, "sending", "Sending CloudCoins", rn);


        sm.setActiveWalletObj(lw);

        sm.startSenderServiceBank(lw, sn, amount, memo, to, rn, new SenderCb(lw, amount, sn, memo, rn, to, false, cb)); 
        
        return cr;
        
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
                    cr.message = "The stack file has been imported and detection will begin automatically "
                            + "so long as they are not already in bank. Please check your reciept.";
                    cr.receipt = frn;
                    cr.keepWallet = true;
                    
                    final int fsize = ur.unpacked.size();
                    setReceipt(wallet, fsize, "importing", "Importing CloudCoins", frn);
                    fcb.callback(cr);   

                    sm.startAuthenticatorService(new AuthenticatorCb(wallet, frn, ftag, ur.unpacked, ur.duplicates));                    
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
        String ptag = (String) params.get("memo");
        if (ptag != null) {
            //byte[] decodedBytes = Base64.getDecoder().decode(ptag);
            //tag = new String(decodedBytes);
            tag = ptag;
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
        int total;
        ArrayList<CloudCoin> coins;
        
        
        public AuthenticatorCb(Wallet wallet, String rn, String tag, ArrayList<CloudCoin> coins, ArrayList<CloudCoin> duplicates) {
            this.duplicates = duplicates;
            this.wallet = wallet;
            this.tag = tag;
            this.rn = rn;
            this.coins = coins;
            this.total = coins.size();
        }
        
        public void callback(Object o) {
            final AuthenticatorResult ar = (AuthenticatorResult) o;
            logger.debug(ltag, "Authenticator finished " + ar.status);
            
            if (ar.status == AuthenticatorResult.STATUS_ERROR) {
                setErrorReceipt(wallet, total, "Authenticator Failed", rn);
                logger.error(ltag, "Authenticator failed");
                return;
            }
            
            if (ar.status == AuthenticatorResult.STATUS_FINISHED) {
                logger.debug(ltag, "Auth Finished. Launching grader");
                sm.startGraderService(new CallbackInterface() {
                    public void callback(Object o) {
                        final GraderResult gr = (GraderResult) o;
                        logger.debug(ltag, "Grader finished");
                        
                        int statToBankValue = gr.totalAuthenticValue + gr.totalFrackedValue;
                        int statFailedValue = gr.totalCounterfeitValue;
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
                            //setErrorReceipt(wallet, total, "All Coins are counterfeit", rn);
                            setReceiptGeneral(wallet, 0, "error", "All Coins are counterfeit", 0, 0, total, rn);
                            return;
                        }
                        
                        logger.debug(ltag, "Grader finished 2");
                        rmReceipt(wallet, rn);
                        
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
                                        
                                        sm.startShowCoinsService(new CallbackInterface() {
                                            public void callback(Object o) {
                                                ShowCoinsResult scresult = (ShowCoinsResult) o;
                                                wallet.setSNs(scresult.coins);
                                
                                                int[][] counters = scresult.counters;                        
                                                int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
                                                AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                                                AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);
                        
                                                wallet.setTotal(totalCnt);
                                                wallet.setCounters(counters);
                                            }
                                        });
                                        
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
    
    
    class SenderCb implements CallbackInterface {
        Wallet wallet;
        String rn;
        int amount;
        String dir;
        CallbackInterface cb;
        boolean fromChange;
        int sn;
        String memo;
        String to;
        
        public SenderCb(Wallet wallet, int amount, int sn, String memo, String rn, String to, boolean fromChange, CallbackInterface cb) {
            this.dir = getAccountDir();
            this.wallet = wallet;
            this.amount = amount;
            this.rn = rn;
            this.cb = cb;       
            this.sn = sn;
            this.memo = memo;
            this.to = to;
            this.fromChange = fromChange;
        }
        
        public void callback(Object o) {
            SenderResult sr = (SenderResult) o;

            logger.debug(ltag, "Sender (Deposit) finished: " + sr.status);
            if (sr.status == SenderResult.STATUS_PROCESSING) {
                return;
            }

            if (sr.status == SenderResult.STATUS_CANCELLED) {
                logger.debug(ltag, "Failed to send coins");
                setErrorReceipt(wallet, amount, "Sender Cancelled", rn);
                return;
            }
      
            if (sr.status == SenderResult.STATUS_ERROR) {
                if (this.fromChange) {
                    logger.debug(ltag, "From change true, giving up");
                    setErrorReceipt(wallet, amount, "Failed to make change", rn);
                    cb.callback(getCrError("Not enough coins after change"));
                    if (wallet.isEncrypted())
                        sm.startVaulterService(null, wallet.getPassword());
                    return;
                }
                
                if (sr.errText != Config.PICK_ERROR_MSG) {
                    logger.debug(ltag, "Sender error " + sr.errText);
                    setErrorReceipt(wallet, amount, "Sender Error " + sr.errText, rn);
                    cb.callback(getCrError("Sender error"));
                    if (wallet.isEncrypted())
                        sm.startVaulterService(null, wallet.getPassword());
                    return;
                }
                
                logger.debug(ltag, "Pick error. Will make Change"); 
                /*
                sm.makeChange(wallet, amount, new CallbackInterface() {
                    public void callback(Object o) {
                        makeChangeResult mcr = (makeChangeResult) o;                               
                        if (!mcr.errText.isEmpty()) {
                            logger.error(ltag, "Failed to make change: " + mcr.errText);
                            setErrorReceipt(wallet, amount, "Failed to make change", rn);
                            cb.callback(getCrError("Failed to make change"));
                            return;
                        }
                        
                        if (mcr.status == 1) { 
                        } else if (mcr.status == 2) {
                            logger.debug(ltag, "Change done successfully. Retrying");                          
                            sm.startSenderServiceBank(wallet, sn, amount, memo, to, rn, new SenderCb(wallet, amount, sn, memo, rn, to, true, cb)); 
                        }
                
                        return;
                    }
                    
                    
                });
                */
                
                //cb.callback(getCrError("Failed to send Coins"));
                return;
                
            }  
            
            if (sr.status == ExporterResult.STATUS_FINISHED) {
                logger.debug(ltag, "We are finally done");
                
                int total = sr.totalCoins;
                int a = sr.totalAuthentic;
                int f = sr.totalFracked;
                int c = sr.totalCounterfeit;
                setReceiptGeneral(wallet, total, "sent", "Coins sent successfully to " + to, a, f, c, rn);
                
              
                wallet.appendTransaction(memo, sr.amount * -1, sr.receiptId);
                wallet.setNotUpdated();
                
                /*
                sm.startShowSkyCoinsService(new CallbackInterface() {
                    public void callback(Object o) {
                        ShowEnvelopeCoinsResult scresult = (ShowEnvelopeCoinsResult) o;

                        if (scresult.status != ShowEnvelopeCoinsResult.STATUS_FINISHED)
                            return;
                        
                        w.setSNs(scresult.coins);
                        w.setEnvelopes(scresult.envelopes);
                                
                        int[][] counters = scresult.counters;                        
                        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);

                        w.setCounters(counters);
                        walletSetTotal(w, totalCnt);
                        setTotalCoins();
                    }
                });
                */
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

                sm.startShowCoinsService(new CallbackInterface() {
                    public void callback(Object o) {
                        ShowCoinsResult scresult = (ShowCoinsResult) o;
                        wallet.setSNs(scresult.coins);
                                
                        int[][] counters = scresult.counters;                        
                        int totalCnt = AppCore.getTotal(counters[Config.IDX_FOLDER_BANK]) +
			AppCore.getTotal(counters[Config.IDX_FOLDER_FRACKED]) +
                        AppCore.getTotal(counters[Config.IDX_FOLDER_VAULT]);
                        
                        wallet.setTotal(totalCnt);
                        wallet.setCounters(counters);

                        CloudbankResult cr = new CloudbankResult();
                        cr.status = CloudbankResult.STATUS_OK_JSON;
                        cr.message = stack.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "");
                        cr.keepWallet = false;
                        
                        cb.callback(cr); 
                    }
                });
                
                              
                
            }
                
        }
    
    }
}