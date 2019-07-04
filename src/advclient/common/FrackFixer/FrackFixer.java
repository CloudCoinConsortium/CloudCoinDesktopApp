package global.cloudcoin.ccbank.FrackFixer;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class FrackFixer extends Servant {
    String ltag = "FrackFixer";
    FrackFixerResult fr;

    int triadSize;

    private int[][] trustedServers;
    private int[][][] trustedTriads;

    public FrackFixer(String rootDir, GLogger logger) {
        super("FrackFixer", rootDir, logger);
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        fr = new FrackFixerResult();
        if (isCancelled()) {
            logger.info(ltag, "Start Cancelled");

            resume();
            fr.status = FrackFixerResult.STATUS_CANCELLED;
            if (cb != null)
                cb.callback(fr);

            return;
        }
        
        launchDetachedThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN (Detached) FrackFixer");
                doFrackFix();
                
                raida.setReadTimeout(Config.READ_TIMEOUT);
            }
        });
    }

    private int getNeightbour(int raidaIdx, int offset) {
        int result = raidaIdx + offset;

        if (result < 0)
            result += RAIDA.TOTAL_RAIDA_COUNT;

        if (result >= RAIDA.TOTAL_RAIDA_COUNT)
            result -= RAIDA.TOTAL_RAIDA_COUNT;

        return result;

    }

    public boolean initNeighbours() {
        int sideSize;

        sideSize= (int) Math.sqrt(RAIDA.TOTAL_RAIDA_COUNT);
        if (sideSize * sideSize != RAIDA.TOTAL_RAIDA_COUNT) {
            logger.error(ltag, "Wrong RAIDA configuration");
            return false;
        }

        trustedServers = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        trustedTriads = new int[RAIDA.TOTAL_RAIDA_COUNT][][];

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            trustedServers[i] = new int[8];

            trustedServers[i][0] = getNeightbour(i, -sideSize - 1);
            trustedServers[i][1] = getNeightbour(i, -sideSize);
            trustedServers[i][2] = getNeightbour(i, -sideSize + 1);
            trustedServers[i][3] = getNeightbour(i, -1);
            trustedServers[i][4] = getNeightbour(i, 1);
            trustedServers[i][5] = getNeightbour(i, sideSize - 1);
            trustedServers[i][6] = getNeightbour(i, sideSize);
            trustedServers[i][7] = getNeightbour(i, sideSize + 1);

            trustedTriads[i] = new int[4][];
            
            if (Config.FIX_PROTOCOL_VERSION == 4) {
                trustedTriads[i][0] = new int[] { trustedServers[i][0], trustedServers[i][1], trustedServers[i][3], trustedServers[i][7] };
                trustedTriads[i][1] = new int[] { trustedServers[i][1], trustedServers[i][2], trustedServers[i][4], trustedServers[i][5] };
                trustedTriads[i][2] = new int[] { trustedServers[i][3], trustedServers[i][5], trustedServers[i][6], trustedServers[i][2] };
                trustedTriads[i][3] = new int[] { trustedServers[i][4], trustedServers[i][6], trustedServers[i][7], trustedServers[i][0] };
            } else {   
                trustedTriads[i][0] = new int[] { trustedServers[i][0], trustedServers[i][1], trustedServers[i][3] };
                trustedTriads[i][1] = new int[] { trustedServers[i][1], trustedServers[i][2], trustedServers[i][4] };
                trustedTriads[i][2] = new int[] { trustedServers[i][3], trustedServers[i][5], trustedServers[i][6] };
                trustedTriads[i][3] = new int[] { trustedServers[i][4], trustedServers[i][6], trustedServers[i][7] };
            }
            

        }

        triadSize = trustedTriads[0][0].length;

        return true;
    }

    public void copyFromMainFr(FrackFixerResult nfr) {
        nfr.failed = fr.failed;
        nfr.fixed = fr.fixed;
        nfr.status = fr.status;
        nfr.totalFilesProcessed = fr.totalFilesProcessed;
        nfr.totalFiles = fr.totalFiles;
        nfr.totalRAIDAProcessed = fr.totalRAIDAProcessed;
        nfr.fixingRAIDA = fr.fixingRAIDA;
        nfr.round = fr.round;
    }

    
    public void doMove(ArrayList<CloudCoin> ccs) {
        int cnt = 0;
        
        logger.info(ltag, "Maybe moving " + ccs.size() + " coins");
        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "Moving coin " + cc.sn);
            cnt = 0;
            for (int i = RAIDA.TOTAL_RAIDA_COUNT - 1; i >= 0; i--) {
                if (cc.getDetectStatus(i) != CloudCoin.STATUS_FAIL)
                    cnt++;
            }

            if (cnt == RAIDA.TOTAL_RAIDA_COUNT) {
                logger.info(ltag, "Coin " + cc.sn + " is fixed. Moving to bank");
                AppCore.moveToBank(cc.originalFile, user);
                fr.fixed++;
            } else {
                logger.debug(ltag, "Not ready to move. Failed to fix. Only passed:" + cnt);
            }
        }   
    }
    
    public void doFrackFix() {
        if (!initNeighbours()) {
            fr.status = FrackFixerResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(fr);

            return;
        }

        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            fr.status = FrackFixerResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(fr);

            return;
        }
        
        raida.setReadTimeout(Config.FIX_FRACKED_TIMEOUT);
        
        String fullPath = AppCore.getUserDir(Config.DIR_FRACKED, user);
        CloudCoin cc;
        ArrayList<CloudCoin> ccall = new ArrayList<CloudCoin>();

        File dirObj = new File(fullPath);
        File[] listOfFiles = dirObj.listFiles();
        if (listOfFiles == null) {
            logger.error(ltag, "Can't proceed. Fracked dir does not exist");
            fr.status = FrackFixerResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(fr);
        }
        
        for (File file : listOfFiles) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                fr.failed++;
                continue;
            }

            ccall.add(cc);
        }

        fr.totalFiles = AppCore.getFilesCount(Config.DIR_FRACKED, user);
        FrackFixerResult nfr = new FrackFixerResult();
        copyFromMainFr(nfr);
        if (cb != null)
            cb.callback(nfr);
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;

        logger.debug(ltag, "maxcoins " + maxCoins);

        ArrayList<CloudCoin> ccactive = new ArrayList<CloudCoin>();
        int corner, i;

        logger.debug(ltag, "Round1");
        
        // Round 1
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            fr.round = 1;
            fr.fixingRAIDA = i;
            fr.totalFilesProcessed = 0;
            fr.totalRAIDAProcessed = 0;
            for (CloudCoin tcc : ccall) {
                if (tcc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    continue;
                
                if (isCancelled()) {
                    logger.info(ltag, "Cancelled");

                    resume();
                    nfr = new FrackFixerResult();
                    fr.status = FrackFixerResult.STATUS_CANCELLED;
                    copyFromMainFr(nfr);
                    if (cb != null)
                        cb.callback(nfr);

                    return;
                }
                
                ccactive.add(tcc);
                if (ccactive.size() == maxCoins) {
                    logger.info(ltag, "Doing fix. maxCoins " + maxCoins);
                    doRealFix(i, ccactive);
                    ccactive.clear();
                                        
                    nfr = new FrackFixerResult();
                    fr.totalRAIDAProcessed = 0;
                    fr.totalFilesProcessed += maxCoins;
                    
                    copyFromMainFr(nfr);
                    if (cb != null)
                        cb.callback(nfr);   
                }
            }

            if (ccactive.size() > 0) {
                doRealFix(i, ccactive);            
                ccactive.clear();
            }         
        }
        
        logger.debug(ltag, "Round2");
        ccactive = new ArrayList<CloudCoin>();
        
        // Round 2
        for (i = RAIDA.TOTAL_RAIDA_COUNT - 1; i >= 0; i--) {
            fr.round = 2;
            fr.fixingRAIDA = i;
            fr.totalFilesProcessed = 0;
            fr.totalRAIDAProcessed = 0;
            for (CloudCoin tcc : ccall) {
                if (tcc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    continue;
                
                if (isCancelled()) {
                    logger.info(ltag, "Cancelled");

                    resume();
                    nfr = new FrackFixerResult();
                    fr.status = FrackFixerResult.STATUS_CANCELLED;
                    copyFromMainFr(nfr);
                    if (cb != null)
                        cb.callback(nfr);

                    return;
                }
                
                ccactive.add(tcc);
                if (ccactive.size() == maxCoins) {
                    logger.info(ltag, "Doing fix. maxCoins " + maxCoins);
                    doRealFix(i, ccactive);
                    ccactive.clear();
                                        
                    nfr = new FrackFixerResult();
                    fr.totalRAIDAProcessed = 0;
                    fr.totalFilesProcessed += maxCoins;
                    
                    copyFromMainFr(nfr);
                    if (cb != null)
                        cb.callback(nfr);   
                }
            }

            if (ccactive.size() > 0) {
                doRealFix(i, ccactive);            
                ccactive.clear();
            }         
        }
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        logger.debug(ltag, "Time to move coins");
        doMove(ccall);
        
        
        
        
        
        
        
        
        
        
        
        
        

        nfr = new FrackFixerResult();
        fr.failed = AppCore.getFilesCount(Config.DIR_FRACKED, user);
        fr.status = FrackFixerResult.STATUS_FINISHED;
        copyFromMainFr(nfr);
        if (cb != null)
            cb.callback(nfr);
    }

    private void doRealFix(int raidaIdx, ArrayList<CloudCoin> ccs) {
        int corner;

        logger.debug(ltag, "Fixing " + ccs.size() + " coins on the RAIDA" + raidaIdx);
        for (corner = 0; corner < 4; corner++) {
            logger.debug(ltag, "corner=" + corner);

            if (fixCoinsInCorner(raidaIdx, corner, ccs)) {
                logger.debug(ltag, "Fixed successfully");
                syncCoins(raidaIdx, ccs);
                break;
            }
        }
    }

    public boolean fixCoinsInCorner(int raidaIdx, int corner, ArrayList<CloudCoin> ccs) {
        int[] triad;
        int[] raidaFix;
        int neighIdx;
        boolean first = true;

        String[] results;
        String[] requests;
        String[] posts;
        StringBuilder[] sbs;

        if (raida.isFailed(raidaIdx)) {
            logger.error(ltag, "RAIDA " + raidaIdx + " is failed. Skipping it");
            return false;
        }

        raidaFix = new int[1];
        raidaFix[0] = raidaIdx;

        requests = new String[triadSize];
        posts = new String[triadSize];
        sbs = new StringBuilder[triadSize];
        triad = trustedTriads[raidaIdx][corner];
        
        int nn, prevnn;
        nn = prevnn = 0;
        
        for (int i = 0; i < triadSize; i++) {
            neighIdx = triad[i];

            logger.debug(ltag, "Checking neighbour: " + neighIdx);
            if (raida.isFailed(neighIdx)) {
                logger.error(ltag, "Neighbour " + neighIdx + " is unavailable. Skipping it");
                return false;
            }

            requests[i] = "multi_get_ticket";
            sbs[i] = new StringBuilder();
            for (CloudCoin cc : ccs) {
                if (!first)
                    sbs[i].append("&");

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);
                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);
                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[neighIdx]);
                sbs[i].append("&pans[]=");
                sbs[i].append(cc.ans[neighIdx]);
                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                if (nn == 0) {
                    nn = cc.nn;
                } else {
                    if (nn != cc.nn) {
                        logger.error(ltag, "We do not support multi nn[] for the time being");
                        return false;
                    }
                }
                    
                first = false;
            }

            posts[i] = sbs[i].toString();
            first = true;
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                fr.totalRAIDAProcessed += 2;
                if (myCb != null) {
                    FrackFixerResult nfr = new FrackFixerResult();
                    copyFromMainFr(nfr);
                    myCb.callback(nfr);
                }
            }
        }, triad);
        
        if (results == null) {
            logger.error(ltag, "Failed to get tickets. Setting triad to failed");
            for (int i = 0; i < triadSize; i++)
                raida.setFailed(triad[i]);

            return false;
        }

        if (results.length != triadSize) {
            logger.error(ltag, "Invalid response size: " + results.length);
            for (int i = 0; i < triadSize; i++)
                raida.setFailed(triad[i]);

            return false;
        }

        CommonResponse errorResponse;
        GetTicketResponse[][] gtr;
        Object[] o;

        requests = new String[1];
        requests[0] = "multi_fix";

        posts = new String[1];
        posts[0] = "";
     
        gtr = new GetTicketResponse[triadSize][];
        for (int i = 0; i < results.length; i++) {
            logger.info(ltag, "rai=" + i + " res=" + results[i]);

            o = parseArrayResponse(results[i], GetTicketResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    raida.setFailed(triad[i]);

                    return false;
                }

                logger.error(ltag, "Failed to get ticket. Status: " + errorResponse.status);
                raida.setFailed(triad[i]);

                return false;
            }

            if (o.length != ccs.size()) {
                logger.error(ltag, "Return size mismatch: " + o.length + " vs " + ccs.size());
                raida.setFailed(triad[i]);
                return false;
            }

            for (int j = 0; j < o.length; j++) {
                String strStatus, message;

                gtr[i] = new GetTicketResponse[o.length];
                gtr[i][j] = (GetTicketResponse) o[j];
                if (gtr[i][j] == null) {
                    logger.error(ltag, "Failed to parse response from: " + triad[i]);
                    raida.setFailed(triad[i]);
                    return false;
                }

                strStatus = gtr[i][j].status;
                message = gtr[i][j].message;
                if (!strStatus.equals("ticket")) {
                    logger.error(ltag, "Failed to get ticket from RAIDA" + triad[i]);
                    return false;
                }

                if (message == null || message.length() != 44) {
                    logger.error(ltag, "Invalid ticket from RAIDA" + triad[i]);
                    return false;
                }

                if (i != 0 || j != 0)
                    posts[0] += "&";
                    
                //posts[0] += "fromserver" + (i + 1) + "[]=" + triad[i] + "&message" + (i + 1) + "[]=" + message;
                posts[0] += "message" + (i + 1) + "[]=" + message;

                logger.info(ltag, "raida" + triad[i] + " v=" + strStatus + " m="+message);
            }

        }

        for (CloudCoin cc : ccs) {
            cc.createAn(raidaIdx);
            posts[0] += "&pans[]=" + cc.ans[raidaIdx];
            //posts[0] += "&pans[]=" + cc.ans[raidaIdx];
        }
             
        posts[0] += "&nn=" + nn;
        for (int i = 0; i < results.length; i++) {
            posts[0] += "&fromserver" + (i + 1) + "=" + triad[i];
        }
                
        logger.debug(ltag, "Doing actual fix on raida " + raidaFix[0] + " post " + posts[0]);
        results = raida.query(requests, posts, null, raidaFix);
        if (results == null) {
            logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        o = parseArrayResponse(results[0], FixResponse.class);
        if (o == null) {
            logger.error(ltag, "Failed to parse fix response" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        if (o.length != ccs.size()) {
            logger.error(ltag, "Fix Return size mismatch: " + o.length + " vs " + ccs.size());
            raida.setFailed(raidaIdx);
            return false;
        }

        FixResponse[] fresp = new FixResponse[o.length];

        for (int j = 0; j < o.length; j++) {
            String strStatus, message;

            fresp[j] = (FixResponse) o[j];
            if (fresp[j] == null) {
                logger.error(ltag, "empty response for idx " + j);
                continue;
            }

            strStatus = fresp[j].status;
            message = fresp[j].message;
            if (!strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx + ": " + message);
                return false;
            }

            logger.debug(ltag, "result " + strStatus +  " mes " + message);
        }

        logger.debug(ltag, "Fixed on RAIDA" + raidaIdx);

        return true;

    }

    private void syncCoins(int raidaIdx, ArrayList<CloudCoin> ccs) {
        for (CloudCoin cc : ccs)
            syncCoin(raidaIdx, cc);
    }
    
    private void syncCoin(int raidaIdx, CloudCoin cc) {
        logger.info(ltag, "Syncing " + cc.originalFile);

        cc.setDetectStatus(raidaIdx, CloudCoin.STATUS_PASS);
        cc.setPownStringFromDetectStatus();
        cc.calcExpirationDate();

        
        AppCore.moveToFolder(cc.originalFile, Config.DIR_TRASH, user);
        logger.debug(ltag, "Saving file: " + cc.originalFile + " pown " + cc.getPownString());
        if (!AppCore.saveFile(cc.originalFile, cc.getJson(false))) {
            logger.error(ltag, "Failed to save file: " + cc.originalFile);
            logger.debug(ltag, "Coin details: " + cc.getJson());
            return;
        }

        logger.info(ltag, "saved");
    }

}
