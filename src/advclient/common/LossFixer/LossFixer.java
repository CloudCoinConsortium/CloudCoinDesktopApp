package global.cloudcoin.ccbank.LossFixer;

import org.json.JSONException;

import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

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


        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN LossFixer");

                doLossFix();
            }
        });
    }

    public void copyFromMainFr(LossFixerResult nlr) {
        nlr.failed = lr.failed;
        nlr.recovered = lr.recovered;
        nlr.status = lr.status;
    }

    public void doLossFix() {
        logger.debug(ltag, "Lossfix started");

        String fullLostPath = AppCore.getUserDir(Config.DIR_LOST, user);

        CloudCoin cc;

        int totalToFix = AppCore.getFilesCount(Config.DIR_LOST, user);
        
        logger.debug(ltag, "Need to check: " + totalToFix + " coins");
        
        File dirObj = new File(fullLostPath);
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
                lfr.status = LossFixerResult.STATUS_CANCELLED;
                copyFromMainFr(lfr);
                if (cb != null)
                    cb.callback(lfr);

                return;
            }
            

            logger.info(ltag, "doing recovery " + cc.sn);
            doRecoverCoin(cc);

            LossFixerResult lfr = new LossFixerResult();
            copyFromMainFr(lfr);
            if (cb != null)
                cb.callback(lfr);
        }

        LossFixerResult lfr = new LossFixerResult();
        lr.status = LossFixerResult.STATUS_FINISHED;
        copyFromMainFr(lfr);
        if (cb != null)
            cb.callback(lfr);
    }

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
            lr.failed++;
            return;
        }

        if (rcnt == 0) {
            logger.error(ltag, "Coin can not be recovered: " + cc.sn);
            AppCore.moveToTrash(cc.originalFile, user);
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

        results = raida.query(requests, null, null, rlist);
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

        results = raida.query(requests, null, null, rlist);
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

            if (status == CloudCoin.STATUS_UNTRIED || status == CloudCoin.STATUS_ERROR) {
                logger.debug(ltag, "We still have a error on RAIDA " + i);
                return;
            }
        }
    
        moveCoin(cc);
        lr.recovered++;
    }

    public void moveCoin(CloudCoin cc) {
        String dir = AppCore.getUserDir(Config.DIR_BANK, user);
        String file;

        file = dir + File.separator + cc.getFileName();
        logger.info(ltag, "Saving coin " + file);
        if (!AppCore.saveFile(file, cc.getJson(false))) {
            logger.error(ltag, "Failed to move coin to Imported: " + cc.getFileName());
            return;
        }

        AppCore.moveToImported(cc.originalFile, user);
    }
}
