package global.cloudcoin.ccbank.Recoverer;

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


public class Recoverer extends Servant {

    String ltag = "Recoverer";
    RecovererResult globalResult;
    String email;

    public Recoverer(String rootDir, GLogger logger) {
        super("Recoverer", rootDir, logger);    
    }

    
   public void launch(String email, CloudCoin cc, CallbackInterface icb) {
        this.cb = icb;
    
        globalResult = new RecovererResult();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Recoverer");
                doRecover(email, cc);
                
                raida.setReadTimeout(Config.READ_TIMEOUT);
            }
        });
    }
    
    

    // Need this
    public void setConfig() {

    }
    
    private void copyFromGlobalResult(RecovererResult aResult) {
        aResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        aResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        aResult.totalFiles = globalResult.totalFiles;
        aResult.totalCoins = globalResult.totalCoins;
        aResult.totalCoinsProcessed = globalResult.totalCoinsProcessed;
        aResult.status = globalResult.status;
        aResult.errText = globalResult.errText;
        aResult.recoveredFailedCoins = globalResult.recoveredFailedCoins;
        aResult.recoveredCoins = globalResult.recoveredCoins;
    }

    public boolean processRecovery(String email, CloudCoin rcc, ArrayList<CloudCoin> ccs, boolean needGeneratePans) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        int i;


        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sbs[i] = new StringBuilder();
            sbs[i].append("recover_by_email?");
            sbs[i].append("nn=");
            sbs[i].append(rcc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(rcc.sn);
            sbs[i].append("&an=");
            sbs[i].append(rcc.ans[i]);
            sbs[i].append("&email=");
            sbs[i].append(email);
        }

        for (CloudCoin cc : ccs) {
            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);
            }
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = sbs[i].toString();
        }

        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    RecovererResult ar = new RecovererResult();
                    copyFromGlobalResult(ar);
                    myCb.callback(ar);
                }
            }
        });

        if (results == null) {
            logger.error(ltag, "Failed to query recover");
            return false;
        }

        CommonResponse errorResponse;
        //AuthenticatorResponse[][] ar;
        //Object[] o;
        Object o;
        

        //ar = new AuthenticatorResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    rcc.setDetectStatus(i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                rcc.setDetectStatus(i, CloudCoin.STATUS_NORESPONSE);
                continue;
            }
  
            //o = parseArrayResponse(results[i], AuthenticatorResponse.class);
            o = parseResponse(results[i], RecovererResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                rcc.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }
            
            RecovererResponse ars = (RecovererResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("success")) {
                logger.debug(ltag, "pass");
                rcc.setDetectStatus(i, CloudCoin.STATUS_PASS);
                continue;
            } else if (ars.status.equals("fail")) {
                logger.debug(ltag, "fail");
                rcc.setDetectStatus(i, CloudCoin.STATUS_FAIL);
                continue;
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                rcc.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                continue;
            }

        }
        
        rcc.setPownStringFromDetectStatus();
        
        String pownString = rcc.getPownString();
        logger.debug(ltag, "Pownstring " + pownString);
        
        for (CloudCoin cc : ccs) {
            if (rcc.isSentFixable()) {
                globalResult.recoveredCoins += cc.getDenomination(); 
                File f = new File(cc.originalFile);
                if (f.exists())
                    AppCore.renameFile(cc.originalFile, AppCore.getPaidRecoveredDir() + File.separator + cc.getFileName());
            } else {
                globalResult.recoveredFailedCoins += cc.getDenomination();
            } 
        }
        
        globalResult.pownString = pownString;

        return true;
    }

    private void moveCoins(ArrayList<CloudCoin> ccs) {
        System.out.println("moving" + ccs.size());
        if (1==1)
            return;
        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "pre cc " + cc.sn + " pown " + cc.getPownString());
            cc.setPownStringFromDetectStatus();
            logger.debug(ltag, "post cc " + cc.sn + " pown " + cc.getPownString());

            String ccFile = AppCore.getRecoveredDir() + File.separator + cc.getFileName();
            logger.info(ltag, "Saving " + ccFile);
            if (!AppCore.saveFile(ccFile, cc.getJson())) {
                logger.error(ltag, "Failed to save file: " + ccFile);
                continue;
            }

            AppCore.deleteFile(cc.originalFile);
        }
    }

    public void doRecover(String email, CloudCoin rcc) {
        logger.debug(ltag, "Doing recovery for email " + email + " cc " + rcc.sn);
        if (!updateRAIDAStatus()) {
            globalResult.status = RecovererResult.STATUS_ERROR;
            globalResult.errText = AppCore.raidaErrText;
            if (cb != null)
                cb.callback(globalResult);

            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }

        String fullPath = AppCore.getRecoveryDir();
        CloudCoin cc;
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();

        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        globalResult.totalFiles = AppCore.getFilesCountPath(fullPath);
        if (globalResult.totalFiles == 0) {
            logger.info(ltag, "The Recovery folder is empty");
            globalResult.status = RecovererResult.STATUS_FINISHED;
            cb.callback(globalResult);
            return;
        }

        raida.setReadTimeout(Config.MULTI_DETECT_TIMEOUT);
        logger.info(ltag, "total files "+ globalResult.totalFiles);

        File dirObj = new File(fullPath);
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
            
            globalResult.totalCoins += cc.getDenomination();
        }
        
        int curValProcessed = 0;
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

            if (isCancelled()) {
                logger.info(ltag, "Cancelled");

                resume();

                RecovererResult ar = new RecovererResult();
                globalResult.status = RecovererResult.STATUS_CANCELLED;
                copyFromGlobalResult(ar);
                if (cb != null)
                    cb.callback(ar);

                return;
            }

            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");

                RecovererResult ar = new RecovererResult();
                if (!processRecovery(email, rcc, ccs, true)) {
                    globalResult.status = RecovererResult.STATUS_ERROR;
                    copyFromGlobalResult(ar);
                    if (cb != null)
                        cb.callback(ar);

                    return;
                }

                moveCoins(ccs);
                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;
                globalResult.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(ar);
                if (cb != null)
                    cb.callback(ar);
            }
        }

        RecovererResult ar = new RecovererResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processRecovery(email, rcc, ccs, true)) {
                globalResult.status = RecovererResult.STATUS_ERROR;
            } else {
                moveCoins(ccs);
                globalResult.status = RecovererResult.STATUS_FINISHED;
                globalResult.totalFilesProcessed += ccs.size();
                globalResult.totalCoinsProcessed = curValProcessed;
            }
        } else {
            globalResult.status = RecovererResult.STATUS_FINISHED;
        }

        copyFromGlobalResult(ar);
        if (cb != null)
            cb.callback(ar);

    }

}
