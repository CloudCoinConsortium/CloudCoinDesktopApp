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
import java.util.HashMap;
//import global.cloudcoin.ccbank.Authenticator.AuthenticatorResult;

//import global.cloudcoin.ccbank.common.core.Authenticator.AuthenticatorResult;

public class Authenticator extends Servant {

    String ltag = "Authencticator";
    AuthenticatorResult globalResult;
    String email;

    public Authenticator(String rootDir, GLogger logger) {
        super("Authenticator", rootDir, logger);    
    }

    public void launch(CallbackInterface icb, String dir) {
        this.cb = icb;
    
        final String fdir = dir;
        
        
        globalResult = new AuthenticatorResult();
        globalResult.tickets = new HashMap<Integer, String[]>();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Authenticator");
                doAuthenticate(fdir);
                
                raida.setReadTimeout(Config.READ_TIMEOUT);
            }
        });
    }

    
    public void launch(CloudCoin cc, CallbackInterface icb) {
        this.cb = icb;
        final CloudCoin fcc = cc;

        globalResult = new AuthenticatorResult();
        globalResult.tickets = new HashMap<Integer, String[]>();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN CloudCoin Authenticator for " + fcc.sn);

                ArrayList<CloudCoin> ccs = new ArrayList<CloudCoin>();
                ccs.add(fcc);

                AuthenticatorResult ar = new AuthenticatorResult();
                if (!processDetect(ccs, false)) {
                    logger.error(ltag, "Failed to detect");
                    globalResult.status = AuthenticatorResult.STATUS_ERROR;
                } else {
                    globalResult.status = AuthenticatorResult.STATUS_FINISHED;
                }

                copyFromGlobalResult(ar);
                if (cb != null)
                    cb.callback(ar);
            }
        });
    }
    
    public void launch(ArrayList<CloudCoin> ccs, CallbackInterface icb) {
        this.cb = icb;
        final ArrayList<CloudCoin> fccs = ccs;

        globalResult = new AuthenticatorResult();
        globalResult.tickets = new HashMap<Integer, String[]>();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN CloudCoins Authenticator");

                AuthenticatorResult ar = new AuthenticatorResult();
                if (!processDetect(fccs, false)) {
                    logger.error(ltag, "Failed to detect");
                    globalResult.status = AuthenticatorResult.STATUS_ERROR;
                } else {
                    globalResult.status = AuthenticatorResult.STATUS_FINISHED;
                }

                copyFromGlobalResult(ar);
                if (cb != null)
                    cb.callback(ar);
            }
        });
    }
    
    
    
    // Need this
    public void setConfig() {

    }
    
    private void copyFromGlobalResult(AuthenticatorResult aResult) {
        aResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        aResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        aResult.totalFiles = globalResult.totalFiles;
        aResult.totalCoins = globalResult.totalCoins;
        aResult.totalCoinsProcessed = globalResult.totalCoinsProcessed;
        aResult.status = globalResult.status;
        aResult.errText = globalResult.errText;
        aResult.hcValid = globalResult.hcValid;
        aResult.hcCounterfeit = globalResult.hcCounterfeit;
        aResult.hcFracked = globalResult.hcFracked;
        aResult.tickets = globalResult.tickets;
    }

    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }
    
    private void setTicket(CloudCoin cc, int idx, String ticket) {
        String[] data;
        //System.out.println("cc="+cc.sn);
        if (globalResult.tickets.containsKey(cc.sn)) {
            //System.out.println("putting exi " + cc.sn + " raida " + idx + " ticket " + ticket);
            logger.debug(ltag, "putting existing " + cc.sn + " raida " + idx +" ticket " + ticket);
            data = globalResult.tickets.get(cc.sn);
        } else {
            logger.debug(ltag, "putting new " + cc.sn + " raida " + idx +" ticket " + ticket);
            //System.out.println("putting new " + cc.sn + " raida " + idx + " ticket " + ticket);
            data = new String[RAIDA.TOTAL_RAIDA_COUNT]; 
        }
            
        data[idx] = ticket;
        globalResult.tickets.put(cc.sn, data);
    }
    
    private void setTickets(ArrayList<CloudCoin> ccs, int idx, String ticket) {
        for (CloudCoin cc : ccs) {
            setTicket(cc, idx, ticket);
        }
    }

    public boolean processDetect(ArrayList<CloudCoin> ccs, boolean needGeneratePans) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;

        int i;
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "multi_detect";
            sbs[i] = new StringBuilder();
            sbs[i].append("b=t");
        }

        for (CloudCoin cc : ccs) {
            if (needGeneratePans)
                cc.generatePans(this.email);
            else
                cc.setPansToAns();

            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                //if (!first)
                //    sbs[i].append("&");

                sbs[i].append("&nns[]=");
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
        //AuthenticatorResponse[][] ar;
        //Object[] o;
        Object o;
        

        //ar = new AuthenticatorResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                setCoinStatus(ccs, i, CloudCoin.STATUS_NORESPONSE);
                continue;
            }
  
            //o = parseArrayResponse(results[i], AuthenticatorResponse.class);
            o = parseResponse(results[i], AuthenticatorResponse.class);
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
            
            AuthenticatorResponse ars = (AuthenticatorResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("allpass")) {
                logger.debug(ltag, "allpass");
                setCoinStatus(ccs, i, CloudCoin.STATUS_PASS);
                setTickets(ccs, i, ars.ticket);
                continue;
            } else if (ars.status.equals("allfail")) {
                logger.debug(ltag, "allfail");
                setCoinStatus(ccs, i, CloudCoin.STATUS_FAIL);
                continue;
            } else if (ars.status.equals("mixed")) {
                logger.debug(ltag, "mixed " + ars.message);
                String[] rss = ars.message.split(",");
                if (rss.length != ccs.size()) {
                    logger.error(ltag, "Invalid length returned: " + rss.length + ", expected: " + ccs.size());
                    setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                    continue;
                }
                
                for (int j = 0; j < rss.length; j++) {
                    String strStatus = rss[j];
                    int status;
                    if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                        status = CloudCoin.STATUS_PASS;
                        setTicket(ccs.get(j), i, ars.ticket);
                    } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                        status = CloudCoin.STATUS_FAIL;
                    } else {
                        status = CloudCoin.STATUS_ERROR;
                        logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                    }
                    
                    ccs.get(j).setDetectStatus(i, status);
                }
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                continue;
            }
        }

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
                    continue;
                }

                logger.debug(ltag, "Deleting " + cc.sn);
                AppCore.deleteFile(cc.originalFile);
            }
        }
    }
    
    private void markCoins (ArrayList<CloudCoin> ccs) {
        for (CloudCoin cc : ccs) {
            cc.setPownStringFromDetectStatus();
            logger.debug(ltag, "cc " + cc.sn + " pown " + cc.getPownString());
         
            if (!cc.canbeRecoveredFromLost()) {
                logger.debug(ltag, "Moving cc as counterfeit " + cc.sn);
                globalResult.hcCounterfeit += cc.getDenomination();
                String ccFile = AppCore.getUserDir(Config.DIR_COUNTERFEIT, user) + File.separator + cc.getFileName();
                if (!AppCore.saveFile(ccFile, cc.getJson(false))) {
                    logger.error(ltag, "Failed to save file: " + ccFile);
                    return;
                }

                AppCore.deleteFile(cc.originalFile);  
                continue;
            }
            
            boolean foundCounterfeit = false;
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_FAIL) {
                    foundCounterfeit = true;
                    break;
                }
            }
            
            if (foundCounterfeit) {
                logger.debug(ltag, "Moving cc to fracked " + cc.sn);
                globalResult.hcFracked += cc.getDenomination();
                
                String ccFile = AppCore.getUserDir(Config.DIR_FRACKED, user) + File.separator + cc.getFileName();
                if (!AppCore.saveFile(ccFile, cc.getJson(false))) {
                    logger.error(ltag, "Failed to save file: " + ccFile);
                    return;
                }

                AppCore.deleteFile(cc.originalFile);               
                continue;
            }
            
            logger.debug(ltag, "Coin is ok. " + cc.sn);
            globalResult.hcValid += cc.getDenomination();
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

    public void doAuthenticate(String sdir) {
        if (!updateRAIDAStatus()) {
            globalResult.status = AuthenticatorResult.STATUS_ERROR;
            globalResult.errText = AppCore.raidaErrText;
            if (cb != null)
                cb.callback(globalResult);

            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }
        
        
        boolean needGeneratePans = true;
        boolean keepCoins = false;
        
        if (sdir == null)
            sdir = Config.DIR_SUSPECT;
        else {
            needGeneratePans = false;
            keepCoins = true;
        }
        
        logger.debug(ltag, "Do authenticate, need pans " + needGeneratePans + " dir=" + sdir);

        String fullPath = AppCore.getUserDir(sdir, user);

        CloudCoin cc;
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();

        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        String email = getConfigValue("email");
        logger.debug(ltag, "email:" + email);
        if (email != null)
            this.email = email.toLowerCase();
        else
            this.email = "";

        globalResult.totalFiles = AppCore.getFilesCount(sdir, user);
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
                if (!processDetect(ccs, needGeneratePans)) {
                    if (!keepCoins)
                        moveCoinsToLost(ccs);
                    globalResult.status = AuthenticatorResult.STATUS_ERROR;
                    copyFromGlobalResult(ar);
                    if (cb != null)
                        cb.callback(ar);

                    return;
                }

                if (!keepCoins)
                    moveCoins(ccs);
                else 
                    markCoins(ccs);
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
            if (!processDetect(ccs, needGeneratePans)) {
                if (!keepCoins)
                    moveCoinsToLost(ccs);
                globalResult.status = AuthenticatorResult.STATUS_ERROR;
            } else {
                if (!keepCoins)
                    moveCoins(ccs);
                else
                    markCoins(ccs);
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
