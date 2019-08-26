package global.cloudcoin.ccbank.Authenticator;

import org.json.JSONException;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;


import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
//import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;

//import global.cloudcoin.ccbank.common.core.Authenticator.AuthenticatorResult;

public class Authenticator extends Servant {

    String ltag = "Authencticator";
    AuthenticatorResult globalResult;
    String email;

    public Authenticator(String rootDir, GLogger logger) {
        super("Authenticator", rootDir, logger);    
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;
    
        globalResult = new AuthenticatorResult();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Authenticator");
                doAuthencticate();
                
                raida.setReadTimeout(Config.READ_TIMEOUT);
            }
        });
    }

    public void setConfig() {
        logger.debug(ltag, "Setting config");
        //putConfigValue("max-coins-to-multi-detect", "" + Config.DEFAULT_MAX_COINS_MULTIDETECT);
    }
    
    private void copyFromGlobalResult(AuthenticatorResult aResult) {
        aResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        aResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        aResult.totalFiles = globalResult.totalFiles;
        aResult.totalCoins = globalResult.totalCoins;
        aResult.totalCoinsProcessed = globalResult.totalCoinsProcessed;
        aResult.status = globalResult.status;
        aResult.errText = globalResult.errText;
    }

    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }

    public boolean processDetect(ArrayList<CloudCoin> ccs) {
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
            requests[i] = "multi_detect";
            sbs[i] = new StringBuilder();
        }

        for (CloudCoin cc : ccs) {
            cc.generatePans(this.email);

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
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    AuthenticatorResult ar = new AuthenticatorResult();
                    copyFromGlobalResult(ar);
                    myCb.callback(ar);
                }
            }
        });

        if (results == null) {
            logger.error(ltag, "Failed to query multi_detect");
            return false;
        }

        CommonResponse errorResponse;
        AuthenticatorResponse[][] ar;
        Object[] o;

        ar = new AuthenticatorResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], AuthenticatorResponse.class);
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

                ar[i] = new AuthenticatorResponse[o.length];
                ar[i][j] = (AuthenticatorResponse) o[j];

                strStatus = ar[i][j].status;

                if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(i, status);
                logger.info(ltag, "raida" + i + " v=" + ar[i][j].status + " m="+ar[i][j].message + " j= " + j + " st=" + status);
            }
        }

        moveCoins(ccs);

        return true;
    }

    private void moveCoinsToLost(ArrayList<CloudCoin> ccs) {
        String dir = AppCore.getUserDir(Config.DIR_LOST, user);
        String file;

        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "cc " + cc.sn + " pown " + cc.getPownString());
            if (!cc.originalFile.equals("")) {
                file = dir + File.separator + cc.getFileName();
                logger.info(ltag, "Saving coin to Lost " + file);
                if (!AppCore.saveFile(file, cc.getJson())) {
                    logger.error(ltag, "Failed to move coin to move to Lost: " + cc.getFileName());
                }

                logger.debug(ltag, "Moving to Trash " + cc.sn);
                AppCore.moveToTrash(cc.originalFile, user);
            }
        }
    }

    private void moveCoins(ArrayList<CloudCoin> ccs) {
        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "pre cc " + cc.sn + " pown " + cc.getPownString());
            cc.setPownStringFromDetectStatus();
            logger.debug(ltag, "post cc " + cc.sn + " pown " + cc.getPownString());

            String ccFile = AppCore.getUserDir(Config.DIR_DETECTED, user) +
                    File.separator + cc.getFileName();

            logger.info(ltag, "Saving " + ccFile);
            if (!AppCore.saveFile(ccFile, cc.getJson())) {
                logger.error(ltag, "Failed to save file: " + ccFile);
                continue;
            }

            AppCore.deleteFile(cc.originalFile);
        }
    }

    public void doAuthencticate() {
        if (!updateRAIDAStatus()) {
            globalResult.status = AuthenticatorResult.STATUS_ERROR;
            globalResult.errText = AppCore.raidaErrText;
            if (cb != null)
                cb.callback(globalResult);

            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }

        String fullPath = AppCore.getUserDir(Config.DIR_SUSPECT, user);

        CloudCoin cc;
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();

        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        String email = getConfigValue("email");
        if (email != null)
            this.email = email.toLowerCase();
        else
            this.email = "";

        globalResult.totalFiles = AppCore.getFilesCount(Config.DIR_SUSPECT, user);
        if (globalResult.totalFiles == 0) {
            logger.info(ltag, "The Suspect folder is empty");
            globalResult.status = AuthenticatorResult.STATUS_FINISHED;
            cb.callback(globalResult);
            return;
        }

        raida.setReadTimeout(Config.MULTI_DETECT_TIMEOUT);
        logger.info(ltag, "total files "+ globalResult.totalFiles);

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
            
            globalResult.totalCoins += cc.getDenomination();
        }
        
        int curValProcessed = 0;
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

            if (isCancelled()) {
                logger.info(ltag, "Cancelled");

                resume();

                AuthenticatorResult ar = new AuthenticatorResult();
                globalResult.status = AuthenticatorResult.STATUS_CANCELLED;
                copyFromGlobalResult(ar);
                if (cb != null)
                    cb.callback(ar);

                return;
            }

            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");

                AuthenticatorResult ar = new AuthenticatorResult();
                if (!processDetect(ccs)) {
                    moveCoinsToLost(ccs);
                    globalResult.status = AuthenticatorResult.STATUS_ERROR;
                    copyFromGlobalResult(ar);
                    if (cb != null)
                        cb.callback(ar);

                    return;
                }

                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;
                globalResult.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(ar);
                if (cb != null)
                    cb.callback(ar);
            }
        }

        AuthenticatorResult ar = new AuthenticatorResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processDetect(ccs)) {
                moveCoinsToLost(ccs);
                globalResult.status = AuthenticatorResult.STATUS_ERROR;
            } else {
                globalResult.status = AuthenticatorResult.STATUS_FINISHED;
                globalResult.totalFilesProcessed += ccs.size();
                globalResult.totalCoinsProcessed = curValProcessed;
            }
        } else {
            globalResult.status = AuthenticatorResult.STATUS_FINISHED;
        }

        copyFromGlobalResult(ar);
        if (cb != null)
            cb.callback(ar);

    }
}
