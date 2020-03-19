package global.cloudcoin.ccbank.Receiver;

import java.io.File;
import java.net.URLEncoder;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.util.ArrayList;

public class Receiver extends Servant {
    String ltag = "Receiver";
    ReceiverResult rr;
    ReceiverResult globalResult;
    
    int a, c, e, f;
    boolean isSkyWithdraw;
    ArrayList<String> ff;

    public Receiver(String rootDir, GLogger logger) {
        super("Receiver", rootDir, logger);
    }

    //public void launch(String user, int tosn, int[] nns, int[] sns, String envelope, CallbackInterface icb) {
    public void launch(int fromsn, int[] sns, String dstFolder, int amount, boolean needReceipt, CallbackInterface icb) {
        this.cb = icb;

        final int ffromsn = fromsn;
        final int[] fsns = sns;
        final String fdstFolder = dstFolder;
        final int famount = amount;
        final boolean fneedReceipt = needReceipt;

        isSkyWithdraw = false;
        if (dstFolder.equals("SkyWithdraw")) {
            isSkyWithdraw = true;
            needReceipt = false;
        }
        
        rr = new ReceiverResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];
        globalResult = new ReceiverResult();
        
        csb = new StringBuilder();
        receiptId = AppCore.generateHex();
        globalResult.receiptId = receiptId;

        ff = new ArrayList<String>();
        a = c = e = f = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Receiver");
                doReceive(ffromsn, fsns, fdstFolder, famount, fneedReceipt);
            }
        });
    }
    
    private void copyFromGlobalResult(ReceiverResult rResult) {
        rResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        rResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        rResult.totalCoins = globalResult.totalCoins;
        rResult.totalCoinsProcessed = globalResult.totalCoinsProcessed;
        rResult.totalFiles = globalResult.totalFiles;
        rResult.status = globalResult.status;
        rResult.amount = globalResult.amount;
        rResult.errText = globalResult.errText;
        rResult.receiptId = globalResult.receiptId;
        rResult.needExtra = globalResult.needExtra;
    }

    public void doReceive(int sn, int[] sns, String fdstFolder, int amount, boolean needReceipt) {
        int i;

        globalResult.needExtra = false;
        
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            rr.status = ReceiverResult.STATUS_ERROR;
            rr.errText = AppCore.raidaErrText;
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }

        CloudCoin extraCoin = null;
        if (!pickCoinsAmountFromArray(sns, amount)) {
            logger.debug(ltag, "Not enough coins in the cloudfor amount " + amount + ". Picking extra");
            
            coinsPicked = new ArrayList<CloudCoin>();
            extraCoin = pickCoinsAmountFromArrayWithExtra(sns, amount);
            if (extraCoin == null) {
                globalResult.status = ReceiverResult.STATUS_ERROR;
                globalResult.errText = "Failed to find coins in the Sky Wallet";
                //globalResult.errText = Config.PICK_ERROR_MSG;
                rr = new ReceiverResult();
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);          
                return;
            }
            
            //globalResult.needExtra = true;
            logger.debug(ltag, "Got extra coin " + extraCoin.sn + " denomination: " + extraCoin.getDenomination());
        }
        
        CloudCoin idcc = getIDcc(sn);
        if (idcc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            rr.status = ReceiverResult.STATUS_ERROR;
            rr.errText = "Failed to find coin ID";
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }
        
        
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        logger.debug(ltag, "Maxcoins: " + maxCoins);
        
        globalResult.totalFiles = coinsPicked.size();
        if (extraCoin != null)
            globalResult.totalFiles++;
        
        globalResult.totalRAIDAProcessed = 0;
        globalResult.totalFilesProcessed = 0;
        globalResult.totalCoinsProcessed = 0;
        
        
        rr = new ReceiverResult();
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);
        
        
        logger.info(ltag, "total files "+ globalResult.totalFiles);
        
        for (CloudCoin cc : coinsPicked) {
            globalResult.totalCoins += cc.getDenomination();
        }
        globalResult.totalCoins = amount;
        
        int curValProcessed = 0;      
        for (CloudCoin cc : coinsPicked) {
            logger.debug(ltag, "Receiving from SN " + sn);         
            if (isCancelled()) {
                logger.info(ltag, "Cancelled");
                resume();
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_CANCELLED;
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);

                return;
            }
            
            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");
                rr = new ReceiverResult();
                
                if (!processReceive(ccs, idcc, needReceipt, fdstFolder)) {
                   rr = new ReceiverResult();
                   globalResult.status = ReceiverResult.STATUS_ERROR;
                   copyFromGlobalResult(rr);
                   if (cb != null)
                        cb.callback(rr);
                
                   return;
                }
          
                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;
                globalResult.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);         
            }  
        }
        
        rr = new ReceiverResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processReceive(ccs, idcc, needReceipt, fdstFolder)) {
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_ERROR;
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);
                
                return;
            } else {
                //globalResult.status = ReceiverResult.STATUS_FINISHED;
                globalResult.totalFilesProcessed += ccs.size();
            }
        }

        
        if (extraCoin != null) {
            int remained = amount - curValProcessed;
            logger.debug(ltag, "Changing coin " + extraCoin.sn + " alreadyProcessed=" + curValProcessed + " needed=" + amount);
            if (!processReceiveWithChange(extraCoin, idcc, needReceipt, remained)) {
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_ERROR;
                globalResult.errText = "Failed to make change";
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);
                
                return;
            }
        }
 
        globalResult.status = ReceiverResult.STATUS_FINISHED;
        
        if (!isSkyWithdraw)
            saveReceipt(user, a, c, 0, 0, e, 0);      
        
        
        rr.files = new String[ff.size()];
        for (i = 0; i < ff.size(); i++) {
            rr.files[i] = ff.get(i);
        }
        
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);
        
    } 
        
        
    public boolean processReceive(ArrayList<CloudCoin> ccs, CloudCoin cc, boolean needReceipt, String fdstFolder)  {
        String[] results;
        Object[] o;
        CommonResponse errorResponse;
        ReceiverResponse[][] rrs;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;
        CloudCoin[] sccs;
        

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "receive";

            sbs[i] = new StringBuilder();
            sbs[i].append("nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());

            for (CloudCoin tcc : ccs) {
                sbs[i].append("&sns[]=");
                sbs[i].append(tcc.sn);
            }

            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    ReceiverResult rrlocal = new ReceiverResult();
                    copyFromGlobalResult(rrlocal);
                    myCb.callback(rrlocal);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query receive");
            return false;
        }

        sccs = new CloudCoin[ccs.size()];
        rrs = new ReceiverResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], ReceiverResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }

            if (o.length != sccs.length) {
                logger.error(ltag, "RAIDA " + i + " wrong number of coins: " + o.length);
                continue;
            }

            for (int j = 0; j < o.length; j++) {
                String strStatus;
                int rnn, rsn;
                String ran;
                boolean found;

                rrs[i] = new ReceiverResponse[o.length];
                rrs[i][j] = (ReceiverResponse) o[j];

                strStatus = rrs[i][j].status;

                found = false;
                int cstatus;
                if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                    logger.info(ltag, "OK response from raida " + i);
                    cstatus = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                    logger.error(ltag, "Counterfeit response from raida " + i);
                    cstatus = CloudCoin.STATUS_FAIL;
                } else {
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                    cstatus = CloudCoin.STATUS_ERROR;
                }
                
                rsn = rrs[i][j].sn;
                ran = (cstatus == CloudCoin.STATUS_PASS) ? rrs[i][j].message : null;
                rnn = rrs[i][j].nn;
                for (int k = 0; k < sccs.length; k++) {
                    if (sccs[k] == null)
                        continue;

                    if (sccs[k].sn == rsn) {
                        sccs[k].ans[i] = ran;
                        sccs[k].setDetectStatus(i, cstatus);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    for (int k = 0; k < sccs.length; k++) {
                        if (sccs[k] == null) {
                            found = true;
                            sccs[k] = new CloudCoin(rnn, rsn);
                            sccs[k].ans[i] = ran;
                            sccs[k].setDetectStatus(i, cstatus);
                            break;
                        }
                    }

                    if (!found) {
                        logger.error(ltag, "Can't find a coin for rsn=" + rsn);
                        continue;
                    }
                }
    
                logger.info(ltag, " sn=" + rsn + " nn=" + rnn + " an=" + ran);
                logger.info(ltag, "raida" + i + " v=" + rrs[i][j].status + " m="+rrs[i][j].message);
            }
        }

        String dir = AppCore.getUserDir(Config.DIR_DETECTED, user);
        if (isSkyWithdraw)
            dir = AppCore.getDownloadsDir();
        
        String file;
        
        int passed, failed;
        for (i = 0; i < sccs.length; i++) {
            if (sccs[i] == null) {
                logger.error(ltag, "Skipping as counterfeit coin: " + ccs.get(i));
                if (needReceipt)
                    addCoinToReceipt(ccs.get(i), "counterfeit", "None");
                c++;
                continue;
            }

            sccs[i].setPownStringFromDetectStatus();
            file = dir + File.separator + sccs[i].getFileName();
            logger.info(ltag, "Saving coin " + file);
            if (!AppCore.saveFile(file, sccs[i].getJson(false))) {
                logger.error(ltag, "Failed to move coin to Detected: " + sccs[i].getFileName());
                if (needReceipt)
                    addCoinToReceipt(sccs[i], "error", "None");
                e++;
                continue;
            }
            ff.add(file);

            logger.info(ltag, "cc="+sccs[i].sn + " v=" + sccs[i].getJson(false));
            
            globalResult.amount += sccs[i].getDenomination();
            a++;
            if (needReceipt)
                addCoinToReceipt(sccs[i], "authentic", Config.DIR_BANK + " from " + fdstFolder);
        }

        logger.info(ltag, "Received " + cc.sn);

        return true;
    }
    
    
    public boolean processReceiveWithChange(CloudCoin tcc, CloudCoin cc, boolean needReceipt, int amount)  {       
        String[] results;
        CommonResponse errorResponse;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;
        CloudCoin[] sccs;

        logger.debug(ltag, "Receiving with change. Target coin " + tcc.sn);
        
        int method = AppCore.getChangeMethod(tcc.getDenomination());
        if (method == 0) {
            logger.error(ltag, "Can't find suitable method");
            return false;
        }
   
        logger.debug(ltag, "Method chosen: " + method);
        
        int sns[] = showChange(method, tcc);
        if (sns == null) {
            logger.error(ltag, "Not enough coins to make change");
            return false;
        }

        for (i = 0; i < sns.length; i++) {
            CloudCoin ccx = new CloudCoin(1, sns[i]);
            logger.info(ltag, "sn "+ sns[i] + " d="+ccx.getDenomination());
        }
        
        coinsPicked = new ArrayList<CloudCoin>();
        ArrayList<CloudCoin> coinsToChange = new ArrayList<CloudCoin>();
        valuesPicked = new int[sns.length];
        if (!pickCoinsAmountFromArray(sns, amount)) {
            logger.error(ltag, "Failed to pick amount: " + amount + " from the resulting coins from all RAIDAs");
            return false;
        }
                
        boolean present;
        int totalToSend = 0;
        int totalChange = 0;
        for (i = 0; i < sns.length; i++) {
            present = false;
            for (CloudCoin vcc : coinsPicked) {
                if (vcc.sn == sns[i]) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                CloudCoin changeCC = new CloudCoin(Config.DEFAULT_NN, sns[i]);
                coinsToChange.add(changeCC);
                totalChange += changeCC.getDenomination();
                
                logger.debug(ltag, "to change: " + changeCC.sn + " d=" + changeCC.getDenomination());
            }
        }

        for (CloudCoin vcc : coinsPicked) {
            totalToSend += vcc.getDenomination();
            logger.debug(ltag, "to send: " + vcc.sn + " d=" + vcc.getDenomination());
        }
        
        logger.debug(ltag, "Total to send: " + totalToSend + " change: " + totalChange);
        if (totalToSend != amount) {
            logger.error(ltag, "Wrong amount: " + amount + " collected=" + totalToSend);
            return false;
        }
        
        if (totalToSend + totalChange != tcc.getDenomination()) {
            logger.error(ltag, "Incorrectly broken: toSend=" + totalToSend + " change=" 
                    + totalChange + " Coin: " + tcc.getDenomination());
            return false;
        }

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "receive_with_change";

            sbs[i] = new StringBuilder();
            sbs[i].append("nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());

            sbs[i].append("&withdrawal_required=");
            sbs[i].append(amount);

            sbs[i].append("&sns[]=");
            sbs[i].append(tcc.sn);
            
            sbs[i].append("&public_change_maker=");
            sbs[i].append(Config.PUBLIC_CHANGE_MAKER_ID);

            for (CloudCoin vcc : coinsPicked) {
                sbs[i].append("&paysns[]=");
                sbs[i].append(vcc.sn);
            }
            
            for (CloudCoin vcc : coinsToChange) {
                sbs[i].append("&chsns[]=");
                sbs[i].append(vcc.sn);
            }
            
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    ReceiverResult trlocal = new ReceiverResult();
                    copyFromGlobalResult(trlocal);
                    myCb.callback(trlocal);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query Receiver");
            return false;
        }

        sccs = new CloudCoin[1];
        

        ReceiverResponse[][] rr;
        Object[] o;

        for (CloudCoin vcc : coinsPicked) {
            logger.debug(ltag, "receiving for " + vcc.sn);
        }
        
        rr = new ReceiverResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(coinsPicked, i, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            o = parseArrayResponse(results[i], ReceiverResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                setCoinStatus(coinsPicked, i, CloudCoin.STATUS_ERROR);
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

                rr[i] = new ReceiverResponse[o.length];
                rr[i][j] = (ReceiverResponse) o[j];

                strStatus = rr[i][j].status;

                if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                coinsPicked.get(j).setDetectStatus(i, status);
                coinsPicked.get(j).ans[i] = rr[i][j].message;
                logger.info(ltag, "raida" + i + " v=" + rr[i][j].status + " m="+rr[i][j].message + " j= " + j + " st=" + status);          
            }

        }

        boolean failed = false;
        String dir = AppCore.getUserDir(Config.DIR_DETECTED, user);
        if (isSkyWithdraw)
            dir = AppCore.getDownloadsDir();
        String file;
        for (CloudCoin vcc: coinsPicked) {
            int cnt = AppCore.getPassedCount(vcc);
            logger.debug(ltag, "recv " + vcc.sn + " passed: " + cnt);
            if (cnt < Config.PASS_THRESHOLD) {
                logger.error(ltag, "Receive Change Failed for sn " + vcc.sn);
                failed = true;
                continue;
            }
            vcc.setPownStringFromDetectStatus();
            file = dir + File.separator + vcc.getFileName();
            if (!AppCore.saveFile(file, vcc.getJson(false))) {
                logger.error(ltag, "Failed to move coin to downloads");
                failed = true;
                continue;
            }
            
            globalResult.amount += vcc.getDenomination();
            ff.add(file);
        }
           
        if (failed) {
            logger.debug(ltag, "Failed to change");
            return false;
        }
    
        
        logger.info(ltag, "Change Received");

        return true;
    }
    
    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }
}
