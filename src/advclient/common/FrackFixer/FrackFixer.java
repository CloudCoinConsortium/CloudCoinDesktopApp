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
import java.util.HashMap;

public class FrackFixer extends Servant {
    String ltag = "FrackFixer";
    FrackFixerResult fr;

    int triadSize;

    private int[][] trustedServers;
    private int[][][] trustedTriads;

    public FrackFixer(String rootDir, GLogger logger) {
        super("FrackFixer", rootDir, logger);
    }

    public void launch(CallbackInterface icb, boolean needExtensive, String email, HashMap<Integer, String[]> tickets) {
        this.cb = icb;

/*
        for (int key : tickets.keySet()) {
            String[] data = tickets.get(key);
            System.out.println("k="+key+ " ks="+data[0]);
    // ...
        }
        */
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
                doFrackFix(needExtensive, email, tickets);
                
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

    public boolean initNeighbours(int protocol) {
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
            
            if (protocol == 4) {
                trustedTriads[i][0] = new int[] { trustedServers[i][0], trustedServers[i][1], trustedServers[i][3], trustedServers[i][7] };
                trustedTriads[i][1] = new int[] { trustedServers[i][1], trustedServers[i][2], trustedServers[i][4], trustedServers[i][5] };
                trustedTriads[i][2] = new int[] { trustedServers[i][2], trustedServers[i][3], trustedServers[i][5], trustedServers[i][6] };
                trustedTriads[i][3] = new int[] { trustedServers[i][0], trustedServers[i][4], trustedServers[i][6], trustedServers[i][7]  };
                
                //System.out.println("raida " + i + " c1="+ trustedServers[i][0] + " "+ trustedServers[i][1] + " "+ trustedServers[i][3] + " "+trustedServers[i][7]);
                //System.out.println("raida " + i + " c1="+ trustedServers[i][1] + " "+ trustedServers[i][2] + " "+ trustedServers[i][4] + " "+trustedServers[i][5]);
                //System.out.println("raida " + i + " c1="+ trustedServers[i][3] + " "+ trustedServers[i][5] + " "+ trustedServers[i][6] + " "+trustedServers[i][2]);
                //System.out.println("raida " + i + " c1="+ trustedServers[i][4] + " "+ trustedServers[i][6] + " "+ trustedServers[i][7] + " "+trustedServers[i][0]);
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
        nfr.fixedValue = fr.fixedValue;
        nfr.status = fr.status;
        nfr.totalFilesProcessed = fr.totalFilesProcessed;
        nfr.totalFiles = fr.totalFiles;
        nfr.totalCoins = fr.totalCoins;
        nfr.totalCoinsProcessed = fr.totalCoinsProcessed;
        nfr.totalRAIDAProcessed = fr.totalRAIDAProcessed;
        nfr.fixingRAIDA = fr.fixingRAIDA;
        nfr.round = fr.round;
        nfr.pownStrings = fr.pownStrings;
    }

    
    public void doMove(ArrayList<CloudCoin> ccs) {
        int cnt = 0;
        
        logger.info(ltag, "Maybe moving " + ccs.size() + " coins");
        fr.pownStrings = new String[ccs.size()];
        int j = 0;
        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "Moving coin " + cc.sn);
            cnt = 0;
            for (int i = RAIDA.TOTAL_RAIDA_COUNT - 1; i >= 0; i--) {
                //if (cc.getDetectStatus(i) != CloudCoin.STATUS_FAIL)
                //    cnt++;
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    cnt++;
            }

            fr.pownStrings[j] = cc.getPownString();
            if (cnt == RAIDA.TOTAL_RAIDA_COUNT) {
                logger.info(ltag, "Coin " + cc.sn + " is fixed. Moving to bank");
                AppCore.moveToBank(cc.originalFile, user);
                fr.fixed++;
                fr.fixedValue += cc.getDenomination();
            } else {
                logger.debug(ltag, "Not ready to move. Failed to fix. Only passed:" + cnt);
            }
            
            j++;
        }   
    }
    
    public void doFrackFix(boolean needExtensive, String email, HashMap<Integer, String[]> tickets) {
        int protocol = needExtensive ? 3 : 4;
        if (!initNeighbours(protocol)) {
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

            if (!AppCore.hasCoinExtension(file))
                continue;
            
            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                fr.failed++;
                continue;
            }

            fr.totalCoins += cc.getDenomination();
            ccall.add(cc);
        }

        fr.totalFiles = AppCore.getFilesCount(Config.DIR_FRACKED, user);
        FrackFixerResult nfr = new FrackFixerResult();
        copyFromMainFr(nfr);
        //if (cb != null)
        //    cb.callback(nfr);
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
     
        logger.debug(ltag, "maxcoins " + maxCoins);

        ArrayList<CloudCoin> ccactive = new ArrayList<CloudCoin>();
        int i;

        logger.debug(ltag, "Round1");
        
        // Round 1
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            fr.round = 1;
            fr.fixingRAIDA = i;
            fr.totalFilesProcessed = 0;
            fr.totalCoinsProcessed = 0;
            fr.totalRAIDAProcessed = 0;
            
            int curValProcessed = 0;
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
                curValProcessed += tcc.getDenomination();
                
                if (ccactive.size() == maxCoins) {
                    logger.info(ltag, "Doing fix. maxCoins " + maxCoins);
                    doRealFix(i, ccactive, needExtensive, email, tickets);
                    ccactive.clear();
                                
                    if (!needExtensive) {
                        nfr = new FrackFixerResult();
                        fr.totalRAIDAProcessed = 0;
                        fr.totalFilesProcessed += maxCoins;
                        fr.totalCoinsProcessed = curValProcessed;

                        copyFromMainFr(nfr);
                        if (cb != null)
                            cb.callback(nfr);   
                    }
                }
            }

            if (ccactive.size() > 0) {
                logger.info(ltag, "Doing rest fix.  " + ccactive.size());
                doRealFix(i, ccactive, needExtensive, email, tickets);            
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
            fr.totalCoinsProcessed = 0;
            
            int curValProcessed = 0;
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
                curValProcessed += tcc.getDenomination();
                
                if (ccactive.size() == maxCoins) {
                    logger.info(ltag, "Doing fix. maxCoins " + maxCoins);
                    doRealFix(i, ccactive, needExtensive, email, tickets);
                    ccactive.clear();
                     
                    if (!needExtensive) {
                        nfr = new FrackFixerResult();
                        fr.totalRAIDAProcessed = 0;
                        fr.totalFilesProcessed += maxCoins;
                        fr.totalCoinsProcessed = curValProcessed;

                        copyFromMainFr(nfr);
                        if (cb != null)
                            cb.callback(nfr);   
                    }
                }
            }

            if (ccactive.size() > 0) {
                logger.info(ltag, "Doing rest fix.  " + ccactive.size());
                doRealFix(i, ccactive, needExtensive, email, tickets);            
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

    private void doRealFix(int raidaIdx, ArrayList<CloudCoin> ccs, boolean needExtensive, String email, HashMap<Integer, String[]> tickets) {
        int corner;
        
        boolean haveTickets = tickets == null ? false : true;
  
        logger.debug(ltag, "Fixing " + ccs.size() + " coins on the RAIDA" + raidaIdx + " needExtensive " + needExtensive + " have tickets " + haveTickets);
        if (needExtensive) {
            FrackFixerResult nfr = new FrackFixerResult();
            logger.debug(ltag, "Doing Fix");
            for (CloudCoin cc : ccs) {
                fr.totalRAIDAProcessed = 0;
                for (corner = 0; corner < 4; corner++) {
                    logger.debug(ltag, "corner=" + corner);
                    if (fixCoinInCorner(raidaIdx, corner, cc, email)) {
                        logger.debug(ltag, "Fixed successfully: " + cc.sn);
                        syncCoin(raidaIdx, cc);
                        break;
                    }
                }                
                fr.totalFilesProcessed++;
                fr.totalCoinsProcessed += cc.getDenomination();
                copyFromMainFr(nfr);
                if (cb != null)
                    cb.callback(nfr);                
            }
            return;
        }
        
        if (haveTickets) {
            logger.debug(ltag, "Doing multi-fix with tickets");
            for (corner = 0; corner < 4; corner++) {
                logger.debug(ltag, "corner=" + corner);

                if (fixCoinsInCornerWithTickets(raidaIdx, corner, ccs, email, tickets)) {
                    logger.debug(ltag, "Fixed successfully");
                    syncCoinsTickets(ccs);
                    break;
                }
            }
            return;
        }
        
        
        logger.debug(ltag, "Doing multi-fix");
        for (corner = 0; corner < 4; corner++) {
            logger.debug(ltag, "corner=" + corner);

            if (fixCoinsInCorner(raidaIdx, corner, ccs, email)) {
                logger.debug(ltag, "Fixed successfully");
                syncCoins(raidaIdx, ccs);
                break;
            }
        }
    }
    
    public boolean fixCoinInCorner(int raidaIdx, int corner, CloudCoin cc, String email) {
        int[] triad;
        int[] raidaFix;
        int neighIdx;

        String[] results;
        String[] requests;
        GetTicketResponse[] gtr;

        if (raida.isFailed(raidaIdx)) {
            logger.error(ltag, "RAIDA " + raidaIdx + " is failed. Skipping it");
            return false;
        }

        raidaFix = new int[1];
        raidaFix[0] = raidaIdx;

        requests = new String[triadSize];
        triad = trustedTriads[raidaIdx][corner];
        for (int i = 0; i < triadSize; i++) {
            neighIdx = triad[i];

            if (cc.getDetectStatus(neighIdx) == CloudCoin.STATUS_FAIL) {
                logger.error(ltag, "Neighbour " + neighIdx + " is counterfeit. Skipping it");
                return false;
            }
            
            logger.debug(ltag, "Checking neighbour: " + neighIdx);
            if (raida.isFailed(neighIdx)) {
                logger.error(ltag, "Neighbour " + neighIdx + " is unavailable. Skipping it");
                return false;
            }

            if (!cc.ans[neighIdx].equals(cc.pans[neighIdx])) {
                logger.error(ltag, "AN&PAN mismatch. The coin can't be fixed: " + i);
                return false;
            }

            requests[i] = "get_ticket?nn=" + cc.nn + "&sn=" + cc.sn + "&an=" + cc.ans[neighIdx] +
                    "&pan=" + cc.pans[neighIdx] + "&denomination=" + cc.getDenomination();
        }

        results = raida.query(requests, null, new CallbackInterface() {
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

        requests = new String[1];
        requests[0] = "fix?";

        gtr = new GetTicketResponse[triadSize];
        for (int i = 0; i < results.length; i++) {
            logger.info(ltag, "res=" + results[i]);

            gtr[i] = (GetTicketResponse) parseResponse(results[i], GetTicketResponse.class);
            if (gtr[i] == null) {
                logger.error(ltag, "Failed to parse response from: " + triad[i]);
                raida.setFailed(triad[i]);
                return false;
            }

            if (!gtr[i].status.equals("ticket")) {
                logger.error(ltag, "Failed to get ticket from RAIDA" + triad[i]);
                raida.setFailed(triad[i]);
                return false;
            }

            if (gtr[i].message == null || gtr[i].message.length() != 44) {
                logger.error(ltag, "Invalid ticket from RAIDA" + triad[i]);
                raida.setFailed(triad[i]);
                return false;
            }

            if (i != 0)
                requests[0] += "&";

            requests[0] += "fromserver" + (i + 1) + "=" + triad[i] + "&message" + (i + 1) + "=" + gtr[i].message;
        }

        cc.createAn(raidaIdx, email);
        requests[0] += "&pan=" + cc.ans[raidaIdx];

        logger.debug(ltag, "Doing actual fix on RAIDA" + raidaIdx);
        results = raida.query(requests, null, null, raidaFix);
        if (results == null) {
            logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        FixResponse fresp = (FixResponse) parseResponse(results[0], FixResponse.class);
        if (fresp == null) {
            logger.error(ltag, "Failed to parse fix response" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        if (!fresp.status.equals("success")) {
            logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx + ": " + fresp.message);
            raida.setFailed(raidaIdx);
            return false;
        }

        
        logger.debug(ltag, "Fixed on RAIDA" + raidaIdx);

        return true;
    }

    public boolean fixCoinsInCorner(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email) {
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
        
        int nn = 0;
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
                    //continue;
                }

                if (i != 0 || j != 0)
                    posts[0] += "&";
                //if (!posts[0].isEmpty())
                //    posts[0] += "&";
                    
                //posts[0] += "fromserver" + (i + 1) + "[]=" + triad[i] + "&message" + (i + 1) + "[]=" + message;
                posts[0] += "message" + (i + 1) + "[]=" + message;

                logger.info(ltag, "raida" + triad[i] + " v=" + strStatus + " m="+message);
                //logger.debug(ltag, "adding " + j);
                
            }

        }

        for (CloudCoin cc : ccs) {
            cc.createAn(raidaIdx, email);
            posts[0] += "&pans[]=" + cc.ans[raidaIdx];
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

        //if (o.length != ccs.size()) {
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
    
    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }
    
    public boolean fixCoinsInCornerWithTickets(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email, HashMap<Integer, String[]> tickets) {
       
        int[] triad;
        String[] tickets4 = new String[4];
        ArrayList<CloudCoin> nccs = new ArrayList<CloudCoin>();
        ArrayList<CloudCoin> restccs = new ArrayList<CloudCoin>();
        HashMap<String, ArrayList<CloudCoin>> mh = new HashMap<String, ArrayList<CloudCoin>>();
        
        triad = trustedTriads[raidaIdx][corner];
        int trRaidaIdx = triad[0];
        int aIdx = triad[0];
        int bIdx = triad[1];
        int cIdx = triad[2];
        int dIdx = triad[3];
        

        logger.debug(ltag, "Fixing in corner " + corner + "(" + aIdx + ", " + bIdx + ", " + cIdx + ", " + dIdx + ")");
        String thash;
        for (CloudCoin cc : ccs) {
            String[] thasha = tickets.get(cc.sn);
            if (thasha == null) {
                logger.debug(ltag, "No ticket for SN " + cc.sn + ", skipping it");
                continue;
            }
            
            thash = thasha[trRaidaIdx];
            
            if (!mh.containsKey(thash)) {
                logger.debug(ltag, "Setting new item for hash " + thash);
                ArrayList<CloudCoin> cca = new ArrayList<CloudCoin>();
                mh.put(thash, cca);
            }

            ArrayList<CloudCoin> cca = (ArrayList<CloudCoin>) mh.get(thash);
            logger.debug(ltag, "pushing " + cc.sn + " to " + thash);
            cca.add(cc);
            mh.put(thash, cca);
        }
            
        int total = mh.size();
        int progressWeight = RAIDA.TOTAL_RAIDA_COUNT / total;
        if (progressWeight < 1)
            progressWeight = 1;
        
        logger.debug(ltag, "keyset size " + total + ", weight = " + progressWeight);
        boolean result = true;
        for (String key : mh.keySet()) {
            ArrayList<CloudCoin> cca = (ArrayList<CloudCoin>) mh.get(key);
            CloudCoin fcc = cca.get(0);
            tickets4[0] = tickets.get(fcc.sn)[aIdx];
            tickets4[1] = tickets.get(fcc.sn)[bIdx];
            tickets4[2] = tickets.get(fcc.sn)[cIdx];
            tickets4[3] = tickets.get(fcc.sn)[dIdx];
            
            logger.debug(ltag, "a=" + tickets4[0] + ", b=" + tickets4[1] + ", c=" + tickets4[2] + ", d=" + tickets4[3]);
            for (CloudCoin cc : cca) {
                logger.debug(ltag, "cc.sn " + cc.sn + " thash " + key);
            }

            if (tickets4[0] == null || tickets4[1] == null || tickets4[2] == null || tickets4[3] == null) {
                logger.debug(ltag, "One of the tickets is zero. Continue");
                result = false;
                continue;
            }
            
                
            if (!fixCoinsInCornerWithTicketsReal(raidaIdx, corner + 1, cca, email, tickets4, progressWeight))
                result = false;
                //cc.setDetectStatus(5, CloudCoin.STATUS_NORESPONSE);

            //System.out.println("k="+key+ " ks="+data[0]);
    // ...
        }
        /*
        System.out.println("post");
        for (CloudCoin cc : ccs) {
            System.out.println("cc="+cc.getDetectStatus(5));
        }
        */
        return result;
        //return fixCoinsInCornerWithTicketsReal(raidaIdx, corner, nccs, email, tickets4, 4);
    }
    
    public boolean fixCoinsInCornerWithTicketsReal(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email, String[] tickets, int progressWeight) {
        int[] raidaFix;


        //System.out.println("corner="+corner);
        
        String[] results;
        String[] requests;
        if (raida.isFailed(raidaIdx)) {
            logger.error(ltag, "RAIDA " + raidaIdx + " is failed. Skipping it");
            return false;
        }

        if (ccs.size() == 0) {
            logger.error(ltag, "Nothing to fix");
            return false;
        }
        
        raidaFix = new int[1];
        raidaFix[0] = raidaIdx;

        //triad = trustedTriads[raidaIdx][corner];

        requests = new String[1];
        //requests[0] = "fix";
        
        CloudCoin cc = ccs.get(0);
        cc.createAn(raidaIdx, email);
        
        String pan = cc.ans[raidaIdx];
        logger.debug(ltag, "Generated pan " + pan);
        for (CloudCoin ccx : ccs) {
            ccx.ans[raidaIdx] = pan;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("fix?");
        sb.append("nn=" + Config.DEFAULT_NN);
        sb.append("&corner=");
        sb.append(corner);
        sb.append("&a=");
        sb.append(tickets[0]);
        sb.append("&b=");
        sb.append(tickets[1]);
        sb.append("&c=");
        sb.append(tickets[2]);
        sb.append("&d=");
        sb.append(tickets[3]);
        sb.append("&pan=");
        sb.append(pan);        
        for (CloudCoin ccx : ccs) {
            sb.append("&sn[]=");
            sb.append(ccx.sn);
        }
             
        requests[0] = sb.toString();
        
        logger.debug(ltag, "Doing actual fix on raida " + raidaFix[0]);
        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                if (fr.totalRAIDAProcessed < RAIDA.TOTAL_RAIDA_COUNT)
                    fr.totalRAIDAProcessed += progressWeight;
                
                if (myCb != null) {
                    FrackFixerResult nfr = new FrackFixerResult();
                    copyFromMainFr(nfr);
                    myCb.callback(nfr);
                }
            }
        }, raidaFix);
        if (results == null) {
            logger.error(ltag, "Failed to fix on RAIDA" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }

        FixResponse frs = (FixResponse) parseResponse(results[0], FixResponse.class);
        if (frs == null) {
            logger.error(ltag, "Failed to parse fix response" + raidaIdx);
            raida.setFailed(raidaIdx);
            return false;
        }
         
        logger.debug(ltag, "raida" + raidaIdx + " status: " + frs.status);
        if (frs.status.equals("allpass")) {
            logger.debug(ltag, "allpass");
            setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_PASS);
            return true;
        } else if (frs.status.equals("allfail")) {
            logger.debug(ltag, "allfail");
            setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_FAIL);
            return false;
        } else if (frs.status.equals("mixed")) {
            logger.debug(ltag, "mixed " + frs.message);
            String[] rss = frs.message.split(",");
            if (rss.length != ccs.size()) {
                logger.error(ltag, "Invalid length returned: " + rss.length + ", expected: " + ccs.size());
                setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
                return false;
            }
                
            for (int j = 0; j < rss.length; j++) {
                String strStatus = rss[j];
                int status;
                if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + raidaIdx + ": " + strStatus);
                }
                    
                ccs.get(j).setDetectStatus(raidaIdx, status);
            }
            
            return true;
        } 
        
        logger.error(ltag, "Invalid status: " + frs.status);
        setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
        return false;
    }
    
    
    
    private void syncCoinsTickets(ArrayList<CloudCoin> ccs) {
        for (CloudCoin cc : ccs)
            syncCoinTicket(cc);
    }
    
    private void syncCoinTicket(CloudCoin cc) {
        logger.info(ltag, "Syncing " + cc.originalFile + " (ticket mode)");

        cc.setPownStringFromDetectStatus();
        cc.calcExpirationDate();
        cc.setPansToAns();

        String tmpFile = cc.originalFile + ".tmp";
        logger.debug(ltag, "Saving file: " + tmpFile + " pown " + cc.getPownString());
        if (!AppCore.saveFile(tmpFile, cc.getJson(false))) {
            logger.error(ltag, "Failed to save file: " + cc.originalFile);
            logger.debug(ltag, "Coin details: " + cc.getJson());
            return;
        }

        AppCore.deleteFile(cc.originalFile);
        
        if (!AppCore.renameFile(tmpFile, cc.originalFile)) {
            logger.error(ltag, "Failed to rename file");
            logger.debug(ltag, "Coin details: " + cc.getJson());
            return;
        }
        
        logger.info(ltag, "Coin saved");
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
        cc.setPansToAns();

        String tmpFile = cc.originalFile + ".tmp";
        logger.debug(ltag, "Saving file: " + tmpFile + " pown " + cc.getPownString());
        if (!AppCore.saveFile(tmpFile, cc.getJson(false))) {
            logger.error(ltag, "Failed to save file: " + cc.originalFile);
            logger.debug(ltag, "Coin details: " + cc.getJson());
            return;
        }

        AppCore.deleteFile(cc.originalFile);
        
        if (!AppCore.renameFile(tmpFile, cc.originalFile)) {
            logger.error(ltag, "Failed to rename file");
            logger.debug(ltag, "Coin details: " + cc.getJson());
            return;
        }
        
        logger.info(ltag, "Coin saved");
    }

}
