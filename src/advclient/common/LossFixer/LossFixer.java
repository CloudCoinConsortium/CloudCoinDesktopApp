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
        nlr.status = lr.status;
        nlr.totalFiles = lr.totalFiles;
        nlr.totalFilesProcessed = lr.totalFilesProcessed;
        nlr.totalRAIDAProcessed = lr.totalRAIDAProcessed;
        nlr.totalCoins = lr.totalCoins;
        nlr.totalCoinsProcessed = lr.totalCoinsProcessed;
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

        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "fix_lost";
            sbs[i] = new StringBuilder();
        }

        for (CloudCoin cc : ccs) {
            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (!first)
                    sbs[i].append("&");

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);

                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);

                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[i]);

                sbs[i].append("&pans[]=");
                sbs[i].append(cc.pans[i]);
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
                lr.totalRAIDAProcessed++;
                if (myCb != null) {
                    LossFixerResult lr = new LossFixerResult();
                    copyFromGlobalResult(lr);
                    myCb.callback(lr);
                }
            }
        });

        if (results == null) {
            logger.error(ltag, "Failed to query multi_detect");
            return false;
        }

        CommonResponse errorResponse;
        LossFixerResponse[][] lfrs;
        Object[] o;

        lfrs = new LossFixerResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], LossFixerResponse.class);
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

                lfrs[i] = new LossFixerResponse[o.length];
                lfrs[i][j] = (LossFixerResponse) o[j];

                strStatus = lfrs[i][j].status;

                if (strStatus.equals("an")) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals("pan")) {
                    status = CloudCoin.STATUS_PASS;
                    ccs.get(j).ans[i] =  ccs.get(j).pans[i];
                } else if (strStatus.equals("neither")) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(i, status);
                logger.info(ltag, "raida" + i + " v=" + lfrs[i][j].status + " m="+lfrs[i][j].message + " j= " + j + " st=" + status);
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
    /*
    public void doRecoverCoin(CloudCoin cc) {
        logger.debug(ltag, "Recovering " + cc.sn + " pown " + cc.getPownString());

        String[] results;
        String[] requests;
        int[] rlist, trlist;
        int failed = 0, rcnt = 0;
        boolean[] brlist;
        int i, j;

        brlist = new boolean[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            brlist[i] = false;

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            int status = cc.getDetectStatus(i);

            logger.debug(ltag, " i " + i + " status=" + status);

            if (status == CloudCoin.STATUS_PASS)
                continue;

            if (status == CloudCoin.STATUS_FAIL) {
                failed++;
                continue;
            }

            if (status == CloudCoin.STATUS_UNTRIED || status == CloudCoin.STATUS_ERROR) {
                brlist[i] = true;
                rcnt++;
                continue;
            }
        }

        if (failed > RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
            logger.error(ltag, "Too many counterfeit responses");
            AppCore.moveToTrash(cc.originalFile, user);
            addCoinToReceipt(cc, "counterfeit", Config.DIR_COUNTERFEIT);
            lr.failed++;
            return;
        }

        if (rcnt == 0) {
            logger.error(ltag, "Coin can not be recovered: " + cc.sn);
            AppCore.moveToTrash(cc.originalFile, user);
            addCoinToReceipt(cc, "counterfeit", Config.DIR_COUNTERFEIT);
            lr.failed++;
            return;
        }

        rlist = new int[rcnt];
        requests = new String[rcnt];
        for (i = 0, j = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (brlist[i]) {
                rlist[j] = i;
                requests[j] = "detect?nn="+ cc.nn + "&sn=" + cc.sn + "&an=" + cc.ans[i] + "&pan=" + cc.ans[i] + "&denomination=" + cc.getDenomination();
                j++;
            }
        }
        
        CallbackInterface lcb = new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                lr.totalRAIDAProcessed++;
                if (myCb != null) {
                    LossFixerResult lfr = new LossFixerResult();
                    copyFromMainFr(lfr);
                    myCb.callback(lfr);
                }
            }
        };

        results = raida.query(requests, null, lcb, rlist);
        if (results == null) {
            logger.error(ltag, "Failed to query raida");
            lr.failed++;
            return;
        }

        if (results.length != rcnt) {
            logger.error(ltag, "Invalid response length: " + results.length + " vs " + rcnt);
            lr.failed++;
            return;
        }
        
        LossFixerResponse lfr;
        boolean changed = false;
        int ridx;

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            brlist[i] = false;

        rcnt = 0;
        for (i = 0; i < results.length; i++) {
            ridx = rlist[i];
            
            logger.info(ltag, "res=" + results[i] + " from raida " + ridx);
            lfr = (LossFixerResponse) parseResponse(results[i], LossFixerResponse.class);
            if (lfr == null) {
                logger.error(ltag, "Failed to parse response from: " + i + " raida " + ridx);
                cc.setDetectStatus(ridx, CloudCoin.STATUS_ERROR);
                changed = true;
                continue;
            }

            if (lfr.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.debug(ltag, "RAIDA " + ridx + " returned OK");
                cc.setDetectStatus(ridx, CloudCoin.STATUS_PASS);
                changed = true;
                continue;
            }

            if (lfr.status.equals(Config.REQUEST_STATUS_FAIL)) {
                logger.debug(ltag, "RAIDA " + ridx + " returned Counterfeit");
                cc.setDetectStatus(ridx, CloudCoin.STATUS_FAIL);
                changed = true;
                brlist[ridx] = true;
                rcnt++;
                continue;
            }
        }

        if (!changed) {
            logger.error(ltag, "No changes from RAIDA");
            lr.failed++;
            return;
        }

        logger.info(ltag, "Doing the next round. Failed raidas: " + rcnt);

        rlist = new int[rcnt];
        requests = new String[rcnt];
        for (i = 0, j = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (brlist[i]) {
                rlist[j] = i;
                requests[j] = "detect?nn="+ cc.nn + "&sn=" + cc.sn + "&an=" + cc.pans[i] + "&pan=" + cc.pans[i] + "&denomination=" + cc.getDenomination();
                j++;
            }
        }

        lr.totalRAIDAProcessed = 0;
        
        results = raida.query(requests, null, lcb, rlist);
        if (results == null) {
            logger.error(ltag, "Failed to query raida");
            lr.failed++;
            return;
        }

        for (i = 0; i < results.length; i++) {
            ridx = rlist[i];
            
            logger.info(ltag, "res=" + results[i] + " from raida " + ridx);

            lfr = (LossFixerResponse) parseResponse(results[i], LossFixerResponse.class);
            if (lfr == null) {
                logger.error(ltag, "Failed to parse response from: " + i + " raida " + ridx);
                cc.setDetectStatus(ridx, CloudCoin.STATUS_ERROR);
                continue;
            }

            if (lfr.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.debug(ltag, "Round2. RAIDA " + i + " returned OK");
                cc.setDetectStatus(ridx, CloudCoin.STATUS_PASS);
                cc.ans[ridx] = cc.pans[ridx];
                continue;
            }
        }

        cc.setPownStringFromDetectStatus();
        logger.debug(ltag, "Coin Pown after " + cc.getPownString());


        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            int status = cc.getDetectStatus(i);
            
            if (status == CloudCoin.STATUS_FAIL) {
                logger.debug(ltag, "Counterfeit on RAIDA " + i + " Moving to Fracked");
                moveCoinToFracked(cc);
                lr.recoveredValue += cc.getDenomination();
                addCoinToReceipt(cc, "fracked", Config.DIR_FRACKED);
                return;
            }
        }
        
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            int status = cc.getDetectStatus(i);

            if (status == CloudCoin.STATUS_UNTRIED || status == CloudCoin.STATUS_ERROR) {
                logger.debug(ltag, "We still have a error on RAIDA " + i);
                return;
            }
        }
        
        moveCoin(cc);
        lr.recovered++;
        lr.recoveredValue += cc.getDenomination();
        addCoinToReceipt(cc, "bank", Config.DIR_BANK);
    }
    */
    

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
            
            if (failed > RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
                logger.debug(ltag, "Coin " + cc.sn + " can't be restored: " + failed);
                AppCore.moveToTrash(cc.originalFile, user);
                addCoinToReceipt(cc, "counterfeit", Config.DIR_COUNTERFEIT);
                lr.failed++;
                continue;
            }
            
            if (failed > 0) {
                logger.debug(ltag, "Coin " + cc.sn + " move to fracked " + failed);
                moveCoinToFracked(cc);
                lr.recovered++;
                lr.recoveredValue += cc.getDenomination();
                addCoinToReceipt(cc, "fracked", Config.DIR_FRACKED);
                continue;
            }
            
            moveCoin(cc);
            logger.debug(ltag, "Moving to bank: " + cc.sn);
            lr.recovered++;
            lr.recoveredValue += cc.getDenomination();
            addCoinToReceipt(cc, "bank", Config.DIR_BANK);
            
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

        AppCore.moveToImported(cc.originalFile, user);
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

        AppCore.moveToImported(cc.originalFile, user);
    }
}
