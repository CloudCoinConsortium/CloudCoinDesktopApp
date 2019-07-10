package global.cloudcoin.ccbank.Sender;

import org.json.JSONException;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Sender extends Servant {
    String ltag = "Sender";
    SenderResult globalResult;
    String remoteWalletName;
    
    int a, c, e, f;
    
    int av, fv;

    public Sender(String rootDir, GLogger logger) {
        super("Sender", rootDir, logger);
    }

    public void launch(int tosn, String dstFolder, int[] values, int amount, 
            String envelope, String remoteWalletName, CallbackInterface icb) {
        this.cb = icb;

        final int ftosn = tosn;
        final int[] fvalues = values;
        final String fenvelope = envelope;
        final int famount = amount;
        final String fdstFolder = dstFolder;

        this.remoteWalletName = remoteWalletName;

        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];

        globalResult = new SenderResult();
        globalResult.memo = envelope;
        
        csb = new StringBuilder();
        receiptId = AppCore.generateHex();
        globalResult.receiptId = receiptId;
     
        av = fv = 0;        
        a = c = e = f = 0;
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Sender");
                
                if (fdstFolder != null) {
                    doSendLocal(famount, fdstFolder);
                } else {
                    doSend(ftosn, fvalues, famount, fenvelope);
                }
            }
        });
    }
    
    private void copyFromGlobalResult(SenderResult sResult) {
        sResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        sResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        sResult.totalFiles = globalResult.totalFiles;
        sResult.status = globalResult.status;
        sResult.amount = globalResult.amount;
        sResult.errText = globalResult.errText;
        sResult.receiptId = globalResult.receiptId;
        
        sResult.totalAuthentic = globalResult.totalAuthentic ;
        sResult.totalCounterfeit = globalResult.totalCounterfeit;
        sResult.totalUnchecked = globalResult.totalUnchecked;
        sResult.totalFracked = globalResult.totalFracked;
        
        sResult.totalAuthenticValue = globalResult.totalAuthenticValue;
        sResult.totalFrackedValue = globalResult.totalFrackedValue;
    }
    
    
    public void doSendLocal(int amount, String dstUser) {
        logger.debug(ltag, "Sending locally " + amount + " to " + dstUser);
        
        String dstPath = AppCore.getUserDir(Config.DIR_BANK, dstUser);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);
        String fullFrackedPath = AppCore.getUserDir(Config.DIR_FRACKED, user);
        if (!pickCoinsAmountInDirs(fullBankPath, fullFrackedPath, amount)) {
            logger.debug(ltag, "Not enough coins in the bank dir for amount " + amount);
            globalResult.status = SenderResult.STATUS_ERROR;
            globalResult.errText = Config.PICK_ERROR_MSG;
            SenderResult sr = new SenderResult();
            copyFromGlobalResult(sr);
            if (cb != null)
                cb.callback(sr);
            
            return;
        }

        for (CloudCoin cc : coinsPicked) {
            String ccFile = cc.originalFile;
        
            File cf = new File(ccFile);
            File parentCf = cf.getParentFile();
            
            if (parentCf == null) {
                logger.error(ltag, "Can't find coins folder");
                globalResult.status = SenderResult.STATUS_ERROR;
                addCoinToReceipt(cc, "error", "None");
                e++;
                continue;
            }
            
            String coinFolder = cf.getParentFile().getName();
            
            if (!coinFolder.equals(Config.DIR_BANK) && !coinFolder.equals(Config.DIR_FRACKED)) {
                logger.error(ltag, "Coin was in the invalid folder: " + coinFolder);
                addCoinToReceipt(cc, "error", "None");
                globalResult.status = SenderResult.STATUS_ERROR;
                e++;
                continue;
            }
            
            if (!AppCore.moveToFolderNoTs(cc.originalFile, coinFolder, dstUser)) {
                logger.error(ltag, "Failed to move coin " + cc.originalFile);
                addCoinToReceipt(cc, "error", "None");
                globalResult.status = SenderResult.STATUS_ERROR;
                e++;
                continue;
            }
            
            a++;
            addCoinToReceipt(cc, "authentic", Config.DIR_BANK);
            
            globalResult.totalAuthentic = a;
            globalResult.totalCounterfeit = 0;
            globalResult.totalUnchecked = e;
            globalResult.amount += cc.getDenomination();
        }
        

        saveReceipt(user, a, 0, 0, 0, e, 0);
        saveReceipt(dstUser, a, 0, 0, 0, e, 0);
        
        SenderResult sr = new SenderResult();
        if (globalResult.status != SenderResult.STATUS_ERROR)
            globalResult.status = SenderResult.STATUS_FINISHED;
        copyFromGlobalResult(sr);
        
        if (cb != null)
            cb.callback(sr);

    }
    
    public void pickCoinsFromSuspect() {
        CloudCoin cc;
        String fullPath = AppCore.getUserDir(Config.DIR_SUSPECT, user);
        
        File dirObj = new File(fullPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                AppCore.moveToTrash(file.toString(), user);
                continue;
            }
            
            coinsPicked.add(cc);
        }
    }
    
    public void doSend(int tosn, int[] values, int amount, String envelope) {
        boolean isSuspect = false;
        logger.debug(ltag, "Sending remotely " + amount + " to " + tosn + " memo=" + envelope + " values=" + values);
        
        SenderResult sr = new SenderResult();
        if (!updateRAIDAStatus()) {
            globalResult.status = SenderResult.STATUS_ERROR;
            globalResult.errText = AppCore.raidaErrText;
            sr = new SenderResult();
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            copyFromGlobalResult(sr);
            if (cb != null)
                cb.callback(sr);
            return;
        }

        String fullSentPath = AppCore.getUserDir(Config.DIR_SENT, user);
        String fullFrackedPath = AppCore.getUserDir(Config.DIR_FRACKED, user);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);

        if (values != null) {
            if (values.length != AppCore.getDenominations().length) {
                logger.error(ltag, "Invalid params");
                sr = new SenderResult();
                globalResult.status = SenderResult.STATUS_ERROR;
                copyFromGlobalResult(sr);
                if (cb != null)
                    cb.callback(sr);
                return;
            }

            if (!pickCoinsInDir(fullBankPath, values)) {
                logger.debug(ltag, "Not enough coins in the bank dir");
                if (!pickCoinsInDir(fullFrackedPath, values)) {
                   logger.error(ltag, "Not enough coins in the Fracked dir");
                   sr = new SenderResult();
                   globalResult.status = SenderResult.STATUS_ERROR;
                   globalResult.errText = Config.PICK_ERROR_MSG;
                   copyFromGlobalResult(sr);
                   if (cb != null)
                        cb.callback(sr);
                   
                   return;
                }
            }
        } else {
            if (amount != 0) {
                logger.debug(ltag, "Pick amount " + amount);
                if (!pickCoinsAmountInDirs(fullBankPath, fullFrackedPath, amount)) {
                    logger.debug(ltag, "Not enough coins in the bank dir for amount " + amount);
                    sr = new SenderResult();
                    globalResult.status = SenderResult.STATUS_ERROR;
                    globalResult.errText = Config.PICK_ERROR_MSG;
                    copyFromGlobalResult(sr);
                    if (cb != null)
                        cb.callback(sr);
                    return;
                }
            } else {
                logger.debug(ltag, "Pick from suspect");
                isSuspect = true;
                pickCoinsFromSuspect();
            }
        }

        setSenderRAIDA();
        
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        logger.debug(ltag, "Maxcoins: " + maxCoins);
        globalResult.totalFiles = coinsPicked.size();
        globalResult.totalRAIDAProcessed = 0;
        globalResult.totalFilesProcessed = 0;
        
        sr = new SenderResult();
        copyFromGlobalResult(sr);
        if (cb != null)
            cb.callback(sr);
                
        logger.info(ltag, "total files "+ globalResult.totalFiles);
        
        for (CloudCoin cc : coinsPicked) {
            logger.debug(ltag, "Sending to SN " + tosn);
            
            if (isCancelled()) {
                logger.info(ltag, "Cancelled");

                resume();

                sr = new SenderResult();
                globalResult.status = SenderResult.STATUS_CANCELLED;
                copyFromGlobalResult(sr);
                if (cb != null)
                    cb.callback(sr);

                return;
            }
            
            ccs.add(cc);
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");
                sr = new SenderResult();
                
                if (!processSend(ccs, tosn, envelope)) {
                   sr = new SenderResult();
                   globalResult.status = SenderResult.STATUS_ERROR;
                   copyFromGlobalResult(sr);
                   if (cb != null)
                        cb.callback(sr);
                
                   return;
                }
                
                
                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;

                copyFromGlobalResult(sr);
                if (cb != null)
                    cb.callback(sr);         
            }  
        }
        
        sr = new SenderResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processSend(ccs, tosn, envelope)) {
                sr = new SenderResult();
                globalResult.status = SenderResult.STATUS_ERROR;
                copyFromGlobalResult(sr);
                if (cb != null)
                    cb.callback(sr);
            } else {
                globalResult.status = SenderResult.STATUS_FINISHED;
                globalResult.totalFilesProcessed += ccs.size();
            }
        } else {
            globalResult.status = SenderResult.STATUS_FINISHED;
        }

        
        globalResult.totalAuthentic = a;
        globalResult.totalCounterfeit = c;
        globalResult.totalUnchecked = e;
        globalResult.totalFracked = f;
        globalResult.totalAuthenticValue = av;
        globalResult.totalFrackedValue = fv;
        
        saveReceipt(user, a, c, f, 0, e, 0);
        
        copyFromGlobalResult(sr);
        if (cb != null)
            cb.callback(sr);
    }

    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }

    private void moveCoins(ArrayList<CloudCoin> ccs) {
        int passed, failed;

        for (CloudCoin cc : ccs) {
            String ccFile = cc.originalFile;
            passed = failed = 0;
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    passed++;
                else if (cc.getDetectStatus(i) == CloudCoin.STATUS_FAIL)
                    failed++;
            }

            logger.info(ltag, "Doing " + cc.originalFile + " pass="+passed + " f="+failed);
            if (passed >= Config.PASS_THRESHOLD) {
                logger.info(ltag, "Moving to Sent: " + cc.sn);
                globalResult.amount += cc.getDenomination();
                addCoinToReceipt(cc, "authentic", "Remote Wallet " + remoteWalletName);
                a++;
                av += cc.getDenomination();
                AppCore.moveToFolder(cc.originalFile, Config.DIR_SENT, user);
            } else if (failed > 0) {
                if (failed >= RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
                    logger.info(ltag, "Moving to Counterfeit: " + cc.sn);
                    addCoinToReceipt(cc, "counterfeit", Config.DIR_COUNTERFEIT);
                    c++;
                    
                    //AppCore.moveToFolder(cc.originalFile, Config.DIR_COUNTERFEIT, user);
                } else {
                    logger.info(ltag, "Moving to Lost: " + cc.sn);
                    addCoinToReceipt(cc, "error", Config.DIR_LOST);
                    e++;
                    //fv += cc.getDenomination();
                    //AppCore.moveToFolder(cc.originalFile, Config.DIR_LOST, user);
                }
            }
        }      
    }


    public boolean processSend(ArrayList<CloudCoin> ccs, int tosn, String envelope) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;

        int i;
        boolean first = true;

        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "send";
            sbs[i] = new StringBuilder();
        }

        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "Processing coin " + cc.sn);

            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (!first) {
                    sbs[i].append("&");
                } else {
                    sbs[i].append("to_sn=");
                    sbs[i].append(tosn);
                    sbs[i].append("&tag=");
                    sbs[i].append(URLEncoder.encode(envelope));
                    sbs[i].append("&");
                }

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);

                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);

                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[i]);
            }

            first = false;
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    SenderResult srlocal = new SenderResult();
                    copyFromGlobalResult(srlocal);
                    myCb.callback(srlocal);
                }
            }
        });

        if (results == null) {
            logger.error(ltag, "Failed to query send");
            return false;
        }

        CommonResponse errorResponse;
        SenderResponse[][] ar;
        Object[] o;
        
        ar = new SenderResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.debug(ltag, "Parsing result from RAIDA" + i + " r: " + results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], SenderResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }
   
            for (int j = 0; j < o.length; j++) {
                String strStatus;
                int status;

                ar[i] = new SenderResponse[o.length];
                ar[i][j] = (SenderResponse) o[j];

                strStatus = ar[i][j].status;

                if (strStatus.equals("pass")) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals("fail")) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(i, status);
                
                logger.info(ltag, "raida" + i + " v=" + ar[i][j].status + " m="+ar[i][j].message);
            }
        }


        moveCoins(ccs);

        return true;
    }

    

}
