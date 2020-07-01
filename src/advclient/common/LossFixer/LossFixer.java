package global.cloudcoin.ccbank.LossFixer;

import org.json.JSONException;

import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.util.ArrayList;
import java.util.HashMap;

public class LossFixer extends Servant {
    String ltag = "LossFixer";
    LossFixerResult lr;

    public LossFixer(String rootDir, GLogger logger) {
        super("LossFixer", rootDir, logger);
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        lr = new LossFixerResult();
        if (isCancelled()) {
            logger.info(ltag, "Start Cancelled");

            resume();
            lr.status = LossFixerResult.STATUS_CANCELLED;
            if (cb != null)
                cb.callback(lr);

            return;
        }

        receiptId = AppCore.generateHex();
        lr.receiptId = receiptId;
        csb = new StringBuilder();

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN LossFixer");

                doLossFix();
            }
        });
    }

    public void copyFromGlobalResult(LossFixerResult nlr) {
        nlr.failed = lr.failed;
        nlr.recovered = lr.recovered;
        nlr.recoveredValue = lr.recoveredValue;
        nlr.status = lr.status;
        nlr.totalFiles = lr.totalFiles;
        nlr.totalFilesProcessed = lr.totalFilesProcessed;
        nlr.totalRAIDAProcessed = lr.totalRAIDAProcessed;
        nlr.totalCoins = lr.totalCoins;
        nlr.totalCoinsProcessed = lr.totalCoinsProcessed;
        nlr.receiptId = lr.receiptId;
    }

    public void doLossFix() {
        logger.debug(ltag, "Lossfix started");

        String fullLostPath = AppCore.getUserDir(Config.DIR_LOST, user);

        CloudCoin cc;
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();

        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        int totalToFix = AppCore.getFilesCount(Config.DIR_LOST, user);       
        logger.debug(ltag, "Need to check: " + totalToFix + " coins. Max coins: " + maxCoins);    
        lr.totalFiles = totalToFix;
        raida.setReadTimeout(Config.MULTI_DETECT_TIMEOUT);

        File dirObj = new File(fullLostPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            if (!AppCore.hasCoinExtension(file))
                continue;
            
            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                AppCore.moveToTrash(file.toString(), user);
                continue;
            }
            
            lr.totalCoins += cc.getDenomination();
        }

        int curValProcessed = 0;
        for (File file : dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                lr.failed++;
                continue;
            }
            
             if (isCancelled()) {
                logger.info(ltag, "Cancelled");

                resume();
                LossFixerResult lfr = new LossFixerResult();
                lr.status = LossFixerResult.STATUS_CANCELLED;
                copyFromGlobalResult(lfr);
                if (cb != null)
                    cb.callback(lfr);

                return;
            }
            
            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");

                LossFixerResult lfr = new LossFixerResult();
                if (!processLossfix(ccs)) {
                    lr.status = LossFixerResult.STATUS_ERROR;
                    copyFromGlobalResult(lfr);
                    if (cb != null)
                        cb.callback(lfr);

                    return;
                }

                ccs.clear();

                lr.totalRAIDAProcessed = 0;
                lr.totalFilesProcessed += maxCoins;
                lr.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(lfr);
                if (cb != null)
                    cb.callback(lfr);
            }
        }

        LossFixerResult lfr = new LossFixerResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processLossfix(ccs)) {
                lr.status = LossFixerResult.STATUS_ERROR;
            } else {
                lr.status = LossFixerResult.STATUS_FINISHED;
                lr.totalFilesProcessed += ccs.size();
                lr.totalCoinsProcessed = curValProcessed;
            }
        } else {
            lr.status = LossFixerResult.STATUS_FINISHED;
        }
        
        copyFromGlobalResult(lfr);
        if (cb != null)
            cb.callback(lfr);
    }

    public boolean processLossfix(ArrayList<CloudCoin> ccs) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;

        int i;
        boolean first = true;
        
        HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            for (CloudCoin cc : ccs) {
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    continue;
                
                hm.put(i, i);
            }
        }

        int[] rlist = new int[hm.size()];
        i = 0;
        for (HashMap.Entry<Integer, Integer> set : hm.entrySet()) {
            rlist[i]= set.getKey();
            i++;
        }
        
        for (i = 0; i < rlist.length; i++) {
            logger.debug(ltag, "Will query raida " + rlist[i]);
        }
        
        int rsize = rlist.length;
        
        posts = new String[rsize];
        requests = new String[rsize];
        sbs = new StringBuilder[rsize];
        //for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
        for (i = 0; i < rlist.length; i++) {
            requests[i] = "fix_lost";
            sbs[i] = new StringBuilder();
        }
        
        

        for (CloudCoin cc : ccs) {
            //for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            for (i = 0; i < rlist.length; i++) {
                int raidaIdx = rlist[i];
                if (!first)
                    sbs[i].append("&");

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);

                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);

                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[raidaIdx]);

                sbs[i].append("&pans[]=");
                sbs[i].append(cc.pans[raidaIdx]);
            }

            logger.debug(ltag, "cc="+cc.sn + " x="+cc.getPownString());
            first = false;
        }

        //for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
        for (i = 0; i < rlist.length; i++) {
            posts[i] = sbs[i].toString();
        }

        results = raida.querySync(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                int step = RAIDA.TOTAL_RAIDA_COUNT / rsize;
                lr.totalRAIDAProcessed += step;
                if (myCb != null) {
                    LossFixerResult lr = new LossFixerResult();
                    copyFromGlobalResult(lr);
                    myCb.callback(lr);
                }
            }
        }, rlist);

        if (results == null) {
            logger.error(ltag, "Failed to query multi_detect");
            return false;
        }

        CommonResponse errorResponse;
        LossFixerResponse[][] lfrs;
        Object[] o;

        lfrs = new LossFixerResponse[rsize][];
        for (i = 0; i < rsize; i++) {
            int raidaIdx = rlist[i];
            logger.info(ltag, "i="+i+ " r="+results[i] + " raidx="+raidaIdx);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + raidaIdx);
                    setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + raidaIdx);
                    setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + raidaIdx);
                setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_NORESPONSE);
                continue;
            }

            o = parseArrayResponse(results[i], LossFixerResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
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

                lfrs[i] = new LossFixerResponse[o.length];
                lfrs[i][j] = (LossFixerResponse) o[j];

                strStatus = lfrs[i][j].status;

                if (strStatus.equals("an")) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals("pan")) {
                    status = CloudCoin.STATUS_PASS;
                    ccs.get(j).ans[raidaIdx] =  ccs.get(j).pans[i];
                } else if (strStatus.equals("neither")) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + raidaIdx + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(raidaIdx, status);
                logger.info(ltag, "raida" + raidaIdx + " v=" + lfrs[i][j].status + " m="+lfrs[i][j].message + " j= " + j + " st=" + status);
            }
        }

        moveCoins(ccs);

        return true;
    }
    
    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }

    private void moveCoins(ArrayList<CloudCoin> ccs) {
        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "pre cc " + cc.sn + " pown " + cc.getPownString());
            cc.setPownStringFromDetectStatus();
            logger.debug(ltag, "post cc " + cc.sn + " pown " + cc.getPownString());
            
            int failed = 0;
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                int status = cc.getDetectStatus(i);
                
                if (status == CloudCoin.STATUS_PASS)
                    continue;
                
                failed++;
            }
            
            if (cc.isSentFixable()) {
                if (failed == 0) {
                    moveCoin(cc);
                    logger.debug(ltag, "Moving to bank: " + cc.sn);
                    lr.recovered++;
                    lr.recoveredValue += cc.getDenomination();
                    addCoinToReceipt(cc, "bank", Config.DIR_BANK);
                    
                } else {
                    logger.debug(ltag, "Coin " + cc.sn + " move to fracked " + failed);
                    moveCoinToFracked(cc);
                    lr.recovered++;
                    lr.recoveredValue += cc.getDenomination();
                    addCoinToReceipt(cc, "fracked", Config.DIR_FRACKED);
                    
                }
            } else {
                if (!cc.canbeRecoveredFromLost()) {
                    logger.debug(ltag, "Coin " + cc.sn + " can't be restored: " + failed);
                    AppCore.moveToTrash(cc.originalFile, user);
                    addCoinToReceipt(cc, "counterfeit", Config.DIR_COUNTERFEIT);
                    lr.failed++;                    
                } else {
                    logger.debug(ltag, "Coin " + cc.sn + " will stay with us for a while");
                    continue;
                }
            }

            
        }
    }
    
    public void moveCoin(CloudCoin cc) {
        String dir = AppCore.getUserDir(Config.DIR_BANK, user);
        String file;
       
        file = dir + File.separator + cc.getFileName();
        logger.info(ltag, "Saving coin " + file);

        if (!AppCore.saveFile(file, cc.getJson(false))) {
            logger.error(ltag, "Failed to move coin to Bank: " + cc.getFileName());
            return;
        }

        AppCore.deleteFile(cc.originalFile);
    }
    
    public void moveCoinToFracked(CloudCoin cc) {
        String dir = AppCore.getUserDir(Config.DIR_FRACKED, user);
        String file;

        file = dir + File.separator + cc.getFileName();
        logger.info(ltag, "Saving coin " + file);
        if (!AppCore.saveFile(file, cc.getJson(false))) {
            logger.error(ltag, "Failed to move coin to Fracked: " + cc.getFileName());
            return;
        }

        AppCore.deleteFile(cc.originalFile);
    }
}
