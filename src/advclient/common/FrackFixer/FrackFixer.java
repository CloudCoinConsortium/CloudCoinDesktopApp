package global.cloudcoin.ccbank.FrackFixer;

import global.cloudcoin.ccbank.Authenticator.AuthenticatorResponse;
import global.cloudcoin.ccbank.LossFixer.LossFixerResponse;
import global.cloudcoin.ccbank.LossFixer.LossFixerResult;
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

        raida.flushStatuses();
        
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

        sideSize = (int) Math.sqrt(RAIDA.TOTAL_RAIDA_COUNT);
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
            } else if (protocol == 3) {   
                trustedTriads[i][0] = new int[] { trustedServers[i][0], trustedServers[i][1], trustedServers[i][3] };
                trustedTriads[i][1] = new int[] { trustedServers[i][1], trustedServers[i][2], trustedServers[i][4] };
                trustedTriads[i][2] = new int[] { trustedServers[i][3], trustedServers[i][5], trustedServers[i][6] };
                trustedTriads[i][3] = new int[] { trustedServers[i][4], trustedServers[i][6], trustedServers[i][7] };
            } else if (protocol == 5) {
                trustedTriads[i] = new int[Config.FIX_MAX_REGEXPS][];
                // Five raida servers. Each number is an index in trustedServers array. 
                int j = 0;
                
                // Corners
                trustedTriads[i][j++] = new int[] { 0, 3, 5, 6, 7 };
                trustedTriads[i][j++] = new int[] { 0, 1, 2, 3, 5 };
                trustedTriads[i][j++] = new int[] { 0, 1, 2, 4, 7 };
                trustedTriads[i][j++] = new int[] { 2, 4, 5, 6, 7 };
                
                // Boomerang
                trustedTriads[i][j++] = new int[] { 0, 1, 3, 5, 6 };
                trustedTriads[i][j++] = new int[] { 0, 1, 2, 3, 4 };
                trustedTriads[i][j++] = new int[] { 1, 2, 4, 6, 7 };
                trustedTriads[i][j++] = new int[] { 3, 4, 5, 6, 7 };
                
                // Bowl
                trustedTriads[i][j++] = new int[] { 0, 1, 4, 5, 6 };
                trustedTriads[i][j++] = new int[] { 0, 2, 3, 4, 6 };
                trustedTriads[i][j++] = new int[] { 1, 2, 3, 6, 7 };
                trustedTriads[i][j++] = new int[] { 1, 3, 4, 5, 7 };
                
                // Spaceship
                trustedTriads[i][j++] = new int[] { 0, 1, 2, 5, 7 };
                trustedTriads[i][j++] = new int[] { 0, 2, 4, 5, 7 };
                trustedTriads[i][j++] = new int[] { 0, 2, 5, 6, 7 };
                trustedTriads[i][j++] = new int[] { 0, 2, 3, 5, 7 };
                
                // Creeper
                trustedTriads[i][j++] = new int[] { 0, 1, 2, 5, 6 };
                trustedTriads[i][j++] = new int[] { 0, 2, 3, 4, 7 };
                trustedTriads[i][j++] = new int[] { 1, 2, 5, 6, 7 };
                trustedTriads[i][j++] = new int[] { 0, 3, 4, 5, 7 };
                                
                // One
                trustedTriads[i][j++] = new int[] { 0, 1, 2, 6, 7 };
                trustedTriads[i][j++] = new int[] { 2, 3, 4, 5, 7 };
                trustedTriads[i][j++] = new int[] { 0, 1, 5, 6, 7 };
                trustedTriads[i][j++] = new int[] { 0, 2, 3, 4, 5 };
                
                
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
        int failed;
        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "Moving coin " + cc.sn);
            cnt = 0;
            failed = 0;
            for (int i = RAIDA.TOTAL_RAIDA_COUNT - 1; i >= 0; i--) {
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_FAIL)
                    failed++;
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    cnt++;
            }

            cc.setPownStringFromDetectStatus();
            fr.pownStrings[j] = cc.getPownString();
            logger.debug(ltag, "cc " + cc.sn + " pownstring " + cc.getPownString());
            if (failed == 0) {
                logger.info(ltag, "Coin " + cc.sn + " is fixed. No failed responses. Moving to bank");
                if (!AppCore.moveToBank(cc.originalFile, user)) {
                    logger.error(ltag, "Failed to move coin in the Bank. Moving to Trash");
                    AppCore.moveToTrash(cc.originalFile, user);
                } else {
                    fr.fixed++;
                    fr.fixedValue += cc.getDenomination();
                }
            } else {
                logger.debug(ltag, "Not ready to move. Failed to fix. Only passed:" + cnt + " failed="+failed);
            }
            
            j++;
        }   
    }
    
    public void doFrackFix(boolean needExtensive, String email, HashMap<Integer, String[]> tickets) {  
        int protocol = needExtensive ? 5 : 4;
        logger.debug(ltag, "Initializing neighbourx for Protocol " + protocol);
        if (!initNeighbours(protocol)) {
            fr.status = FrackFixerResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(fr);

            return;
        }

        raida.setDefaultUrls();
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

        
        
        
        
        // Fixing lost
        ArrayList<CloudCoin> ccactiveLost = new ArrayList<CloudCoin>();
        logger.debug(ltag, "Will try to fix lost first");
        fr.round = 0;
        fr.totalCoinsProcessed = 0;
        int curValProcessedL = 0;
        for (CloudCoin tcc : ccall) {
            ccactiveLost.add(tcc);
            curValProcessedL += tcc.getDenomination();
                
            if (ccactiveLost.size() == maxCoins) {
                logger.info(ltag, "Doing preloss. maxCoins " + maxCoins);
                processLossfix(ccactiveLost);
                ccactiveLost.clear();
                                
                nfr = new FrackFixerResult();
                fr.totalRAIDAProcessed = 0;
                fr.totalFilesProcessed += maxCoins;
                fr.totalCoinsProcessed = curValProcessedL;

                copyFromMainFr(nfr);
                if (cb != null)
                    cb.callback(nfr);   
            }
        }
        
        if (ccactiveLost.size() > 0) {
            logger.info(ltag, "Doing rest prelostfix.  " + ccactiveLost.size());
            processLossfix(ccactiveLost);         
            ccactiveLost.clear();  
        }         
        
        
        
        logger.debug(ltag, "Fixing Round1");
        
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
                                
                    //if (!needExtensive) {
                    nfr = new FrackFixerResult();
                    fr.totalRAIDAProcessed = 0;
                    fr.totalFilesProcessed += maxCoins;
                    fr.totalCoinsProcessed = curValProcessed;

                    copyFromMainFr(nfr);
                    if (cb != null)
                        cb.callback(nfr);   
                    //}
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
                     
                    //if (!needExtensive) {
                    nfr = new FrackFixerResult();
                    fr.totalRAIDAProcessed = 0;
                    fr.totalFilesProcessed += maxCoins;
                    fr.totalCoinsProcessed = curValProcessed;

                    copyFromMainFr(nfr);
                    if (cb != null)
                        cb.callback(nfr);   
                    
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

    private void processLossfix(ArrayList<CloudCoin> ccs) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;

        int i;
        boolean first = true;
        
        logger.debug(ltag, "Checking if we need to call LossFixer");
        
        HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            for (CloudCoin cc : ccs) {
                if (cc.getDetectStatus(i) != CloudCoin.STATUS_NORESPONSE)
                    continue;
                
                if (cc.ans[i] == cc.pans[i]) {
                    logger.debug(ltag, "Pan is not present or equals to An. Skipping");
                    continue;
                }

                logger.debug(ltag, "Will try to lossfix coin " + cc.sn + " on raida " + i);
                hm.put(i, i);
            }
        }
        
        if (hm.size() == 0) {
            logger.debug(ltag, "PreLossFixer is unnecessary");
            return;
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

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                int step = RAIDA.TOTAL_RAIDA_COUNT / rsize;
            /*    lr.totalRAIDAProcessed += step;
                if (myCb != null) {
                    LossFixerResult lr = new LossFixerResult();
                    copyFromGlobalResult(lr);
                    myCb.callback(lr);
                }
            */
            }
        }, rlist);

        if (results == null) {
            logger.error(ltag, "Failed to query fix_lost");
            return;
        }

        CommonResponse errorResponse;
        LossFixerResponse[][] lfrs;
        Object[] o;

        lfrs = new LossFixerResponse[rsize][];
        for (i = 0; i < rsize; i++) {
            int raidaIdx = rlist[i];
            logger.info(ltag, "i="+i+ " r="+results[i] + " raidx="+raidaIdx);

            // Do not set error, it will be set in fix_fracked
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + raidaIdx);
                    //setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + raidaIdx);
                    //setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            // Set noresponse
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + raidaIdx);
                setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_NORESPONSE);
                raida.setFailed(raidaIdx);
                continue;
            }

            o = parseArrayResponse(results[i], LossFixerResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                //setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
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
                    ccs.get(j).ans[raidaIdx] =  ccs.get(j).pans[raidaIdx];
                } else if (strStatus.equals("neither")) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + raidaIdx + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(raidaIdx, status);
                logger.info(ltag, "raida" + raidaIdx + " v=" + lfrs[i][j].status + " m="+lfrs[i][j].message + " j=" + j + " st=" + status);
            }
        }

        return;
    }
    
    private void doRealFix(int raidaIdx, ArrayList<CloudCoin> ccs, boolean needExtensive, String email, HashMap<Integer, String[]> tickets) {
        int corner;
        
        boolean haveTickets = tickets == null ? false : true;
        //processLossfix(ccs);   
        
        logger.debug(ltag, "Fixing " + ccs.size() + " coins on the RAIDA" + raidaIdx + " needExtensive " + needExtensive + " have tickets " + haveTickets);
        if (needExtensive) {
            logger.debug(ltag, "Doing Intensive Fix");
            for (corner = 0; corner < Config.FIX_MAX_REGEXPS; corner++) {
                if (fixCoinsForRegexString(raidaIdx, corner, ccs, email)) {
                    logger.debug(ltag, "Fixed successfully");
                    syncCoinsTickets(ccs);
                    break;
                }
            }
          
            return;
        }

        logger.debug(ltag, "Doing fix");
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
    
    private void setTicket(CloudCoin cc, int idx, String ticket, HashMap<Integer, String[]> tickets) {
        String[] data;
        if (tickets.containsKey(cc.sn)) {
            logger.debug(ltag, "putting existing " + cc.sn + " raida " + idx +" ticket " + ticket);
            data = tickets.get(cc.sn);
        } else {
            logger.debug(ltag, "putting new " + cc.sn + " raida " + idx +" ticket " + ticket);
            data = new String[RAIDA.TOTAL_RAIDA_COUNT]; 
        }
            
        data[idx] = ticket;
        tickets.put(cc.sn, data);
    }
    
    private void setTickets(ArrayList<CloudCoin> ccs, int idx, String ticket, HashMap<Integer, String[]> tickets) {
        for (CloudCoin cc : ccs) {
            setTicket(cc, idx, ticket, tickets);
        }
    }
    
    public boolean fixCoinsInCorner(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email) {
        int[] triad;
        int[] raidaFix;
        int neighIdx;

        logger.debug(ltag, "Fixing in corner " + corner + " on raida " + raidaIdx);
        if (raida.isFailed(raidaIdx)) {
            logger.error(ltag, "RAIDA " + raidaIdx + " is failed. Skipping it");
            return false;
        }

        HashMap<Integer, String[]> tickets = new HashMap<Integer, String[]>();
        
        raidaFix = new int[1];
        raidaFix[0] = raidaIdx;

        String[] requests = new String[triadSize];
        String[] posts = new String[triadSize];
        StringBuilder[] sbs = new StringBuilder[triadSize];
        triad = trustedTriads[raidaIdx][corner];
        
        for (int i = 0; i < triadSize; i++) {
            neighIdx = triad[i];

            logger.debug(ltag, "Checking neighbour: " + neighIdx);
            if (raida.isFailed(neighIdx)) {
                logger.error(ltag, "Neighbour " + neighIdx + " is unavailable. Skipping it");
                return false;
            }

            requests[i] = "multi_detect";
            sbs[i] = new StringBuilder();
            sbs[i].append("b=t");
            for (CloudCoin cc : ccs) {
                sbs[i].append("&nns[]=");
                sbs[i].append(cc.nn);
                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);
                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());
                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[neighIdx]);
                sbs[i].append("&pans[]=");
                sbs[i].append(cc.ans[neighIdx]);           
            }

            posts[i] = sbs[i].toString();
        }

        String[] results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                fr.totalRAIDAProcessed++;
                if (myCb != null) {
                    FrackFixerResult nfr = new FrackFixerResult();
                    copyFromMainFr(nfr);
                    myCb.callback(nfr);
                }
            }
        }, triad);

        if (results == null) {
            logger.error(ltag, "Failed to query multi_detect");
            return false;
        }

        CommonResponse errorResponse;
        Object o;
        

        //ar = new AuthenticatorResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < results.length; i++) {
            int rIdx = triad[i];
            logger.info(ltag, "raidaIdx="+rIdx+ " r="+results[i]);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Failed to get ticket from raida" + rIdx);
                    raida.setFailed(rIdx);
                    return false;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + i);
                    raida.setFailed(rIdx);
                    return false;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Zero response from Raida: " + rIdx);
                raida.setFailed(rIdx);
                return false;
            }
  
            //o = parseArrayResponse(results[i], AuthenticatorResponse.class);
            o = parseResponse(results[i], AuthenticatorResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    raida.setFailed(rIdx);
                    return false;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                raida.setFailed(rIdx);
                return false;
            }
            
            AuthenticatorResponse ars = (AuthenticatorResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("allpass")) {
                logger.debug(ltag, "allpass");
                setTickets(ccs, rIdx, ars.ticket, tickets);
                continue;
            } else if (ars.status.equals("allfail")) {
                logger.debug(ltag, "allfail");
                logger.debug(ltag, "Will mark all coins as 'f'");
                setCoinStatus(ccs, rIdx, CloudCoin.STATUS_FAIL);
                return false;
            } else if (ars.status.equals("mixed")) {
                logger.debug(ltag, "mixed " + ars.message);
                String[] rss = ars.message.split(",");
                if (rss.length != ccs.size()) {
                    logger.error(ltag, "Invalid length returned: " + rss.length + ", expected: " + ccs.size());
                    raida.setFailed(rIdx);
                    return false;
                }
                
                for (int j = 0; j < rss.length; j++) {
                    String strStatus = rss[j];
                    if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                        setTicket(ccs.get(j), rIdx, ars.ticket, tickets);
                    } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                        logger.error(ltag, "Marking coin " + ccs.get(j).sn + " as 'f'");
                        ccs.get(j).setDetectStatus(rIdx, CloudCoin.STATUS_FAIL);

                    } else {

                    }
                }
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                raida.setFailed(rIdx);
                return false;
            }
        }
        
        logger.debug(ltag, "Multi_detect done. Tickets received");

        
        boolean rv = fixCoinsInCornerWithTickets(raidaIdx, corner, ccs, email, tickets);
        
        logger.debug(ltag, "fixCoinsWith tickets rv " + rv);
        
        return rv;
    }
    
    private Character getRegexChar(int idx, int[] fivetouches) {
        for (int j = 0; j < fivetouches.length; j++) {
            if (idx == fivetouches[j])
                return 'p';
        }
        
        return '.';
    }
    
    private String getRegexString(int[] fivetouches) {
        StringBuilder sb = new StringBuilder(".............");

        // -6
        sb.setCharAt(0, getRegexChar(0, fivetouches));
        
        // -5
        sb.setCharAt(1, getRegexChar(1, fivetouches));
        
        // -4
        sb.setCharAt(2, getRegexChar(2, fivetouches));
        
        // -3 always '.'
        sb.setCharAt(3, '.');
        
        // -2 always '.'
        sb.setCharAt(4, '.');
        
        // -1
        sb.setCharAt(5, getRegexChar(3, fivetouches));
        
        // 0
        sb.setCharAt(6, '0');
        
        // 1
        sb.setCharAt(7, getRegexChar(4, fivetouches));
        
        // 2 always '.'
        sb.setCharAt(8, '.');
        
        // 3 always '.'
        sb.setCharAt(9, '.');
        
        // 4
        sb.setCharAt(10, getRegexChar(5, fivetouches));
        
        // 5
        sb.setCharAt(11, getRegexChar(6, fivetouches));
        
        // 6
        sb.setCharAt(12, getRegexChar(7, fivetouches));
        
        
        
        return sb.toString();

    }

    public boolean fixCoinsForRegexString(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email) {
        int[] raidaFix;
        int neighIdx;

        logger.debug(ltag, "Fixing with index " + corner + " on raida " + raidaIdx);
        if (raida.isFailed(raidaIdx)) {
            logger.error(ltag, "RAIDA " + raidaIdx + " is failed. Skipping it");
            return false;
        }
        
        if (corner >= trustedTriads[raidaIdx].length) {
            logger.error(ltag, "Misconfiguration. Index " + corner + " is too big");
            return false;
        }
        
        
        int[] fivetouches = trustedTriads[raidaIdx][corner];
        if (fivetouches == null) {
            logger.error(ltag, "five touches in index " + corner + " have been visited already. Skipping them");
            return false;
        }

        HashMap<Integer, String[]> tickets = new HashMap<Integer, String[]>();
        
        raidaFix = new int[1];
        raidaFix[0] = raidaIdx;

        int msize = fivetouches.length;
        String[] requests = new String[msize];
        String[] posts = new String[msize];
        StringBuilder[] sbs = new StringBuilder[msize];
        
        for (int i = 0; i < msize; i++) {
            logger.debug(ltag, "raida " + raidaIdx +  " got neighbour index " + fivetouches[i]);
        }
        
        int[] rlist = new int[msize];
        for (int i = 0; i < msize; i++) {
            neighIdx = trustedServers[raidaIdx][fivetouches[i]];

            logger.debug(ltag, "Checking neighbour: " + neighIdx);
            if (raida.isFailed(neighIdx)) {
                logger.error(ltag, "Neighbour " + neighIdx + " is unavailable. Skipping it");
                return false;
            }

            requests[i] = "multi_detect";
            sbs[i] = new StringBuilder();
            sbs[i].append("b=t");
            for (CloudCoin cc : ccs) {
                sbs[i].append("&nns[]=");
                sbs[i].append(cc.nn);
                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);
                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());
                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[neighIdx]);
                sbs[i].append("&pans[]=");
                sbs[i].append(cc.ans[neighIdx]);           
            }

            posts[i] = sbs[i].toString();
            rlist[i] = neighIdx;
        }
        
        String regexString  = getRegexString(fivetouches);
        logger.debug(ltag, "Regex string " + regexString);

        String[] results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                fr.totalRAIDAProcessed++;
                if (myCb != null) {
                    FrackFixerResult nfr = new FrackFixerResult();
                    copyFromMainFr(nfr);
                    myCb.callback(nfr);
                }
            }
        }, rlist);

        if (results == null) {
            logger.error(ltag, "Failed to query multi_detect");
            return false;
        }

        CommonResponse errorResponse;
        Object o;
        

        //ar = new AuthenticatorResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < results.length; i++) {
            int rIdx = rlist[i];
            logger.info(ltag, "raidaIdx="+rIdx+ " r="+results[i]);

            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Failed to get ticket from raida" + rIdx);
                    raida.setFailed(rIdx);
                    return false;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + i);
                    raida.setFailed(rIdx);
                    return false;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Zero response from Raida: " + rIdx);
                raida.setFailed(rIdx);
                return false;
            }
  
            //o = parseArrayResponse(results[i], AuthenticatorResponse.class);
            o = parseResponse(results[i], AuthenticatorResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    raida.setFailed(rIdx);
                    return false;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                raida.setFailed(rIdx);
                return false;
            }
            
            AuthenticatorResponse ars = (AuthenticatorResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("allpass")) {
                logger.debug(ltag, "allpass");
                setTickets(ccs, rIdx, ars.ticket, tickets);
                continue;
            } else if (ars.status.equals("allfail")) {
                logger.debug(ltag, "allfail");
                return false;
            } else if (ars.status.equals("mixed")) {
                logger.debug(ltag, "mixed " + ars.message);
                String[] rss = ars.message.split(",");
                if (rss.length != ccs.size()) {
                    logger.error(ltag, "Invalid length returned: " + rss.length + ", expected: " + ccs.size());
                    raida.setFailed(rIdx);
                    return false;
                }
                
                for (int j = 0; j < rss.length; j++) {
                    String strStatus = rss[j];
                    if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                        setTicket(ccs.get(j), rIdx, ars.ticket, tickets);
                    } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                        
                    } else {

                    }
                }
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                raida.setFailed(rIdx);
                return false;
            }
        }
        
        logger.debug(ltag, "Multi_detect done. Tickets received");

        
        boolean rv = fixCoinsInCornerWithTicketsAndRegex(raidaIdx, corner, ccs, email, tickets, regexString);
        
        logger.debug(ltag, "fixCoinsWith tickets rv " + rv);
        

        return rv;
    }

    
    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }
    
    public boolean fixCoinsInCornerWithTicketsAndRegex(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email, HashMap<Integer, String[]> tickets, String regex) {
      
        String[] tickets5 = new String[5];
        HashMap<String, ArrayList<CloudCoin>> mh = new HashMap<String, ArrayList<CloudCoin>>();
        

        int[] fivetouches = trustedTriads[raidaIdx][corner];
        int aIdx = trustedServers[raidaIdx][fivetouches[0]];
        int bIdx = trustedServers[raidaIdx][fivetouches[1]];
        int cIdx = trustedServers[raidaIdx][fivetouches[2]];
        int dIdx = trustedServers[raidaIdx][fivetouches[3]];
        int eIdx = trustedServers[raidaIdx][fivetouches[4]];
        
        int trRaidaIdx = aIdx;

        logger.debug(ltag, "Fixing in index " + corner + "(" + aIdx + ", " + bIdx + ", " + cIdx + ", " + dIdx + ", " + eIdx + ")");
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
        if (total == 0) {
            logger.debug(ltag, "Nothing to fix");
            return true;
        }
            
        
        int progressWeight = RAIDA.TOTAL_RAIDA_COUNT / total;
        if (progressWeight < 1)
            progressWeight = 1;
        
        logger.debug(ltag, "keyset size " + total + ", weight = " + progressWeight);
        boolean result = true;
        for (String key : mh.keySet()) {
            ArrayList<CloudCoin> cca = (ArrayList<CloudCoin>) mh.get(key);
            CloudCoin fcc = cca.get(0);
            tickets5[0] = tickets.get(fcc.sn)[aIdx];
            tickets5[1] = tickets.get(fcc.sn)[bIdx];
            tickets5[2] = tickets.get(fcc.sn)[cIdx];
            tickets5[3] = tickets.get(fcc.sn)[dIdx];
            tickets5[4] = tickets.get(fcc.sn)[eIdx];
            
            logger.debug(ltag, "a=" + tickets5[0] + ", b=" + tickets5[1] + ", c=" + tickets5[2] + ", d=" + tickets5[3] + ", e="+ tickets5[4]);
            for (CloudCoin cc : cca) {
                logger.debug(ltag, "cc.sn " + cc.sn + " thash " + key);
            }

            if (tickets5[0] == null || tickets5[1] == null || tickets5[2] == null || tickets5[3] == null || tickets5[4] == null) {
                logger.debug(ltag, "One of the tickets is zero. Continue");
                result = false;
                continue;
            }
            
                
            if (!fixCoinsInCornerWithTicketsReal(raidaIdx, corner + 1, cca, email, tickets5, progressWeight, regex))
                result = false;

        }
        return result;
    }

    
    public boolean fixCoinsInCornerWithTickets(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email, HashMap<Integer, String[]> tickets) {
      
        int[] triad;
        String[] tickets4 = new String[4];
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
        if (total == 0) {
            logger.debug(ltag, "Nothing to fix");
            return true;
        }
            
        
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
            
                
            if (!fixCoinsInCornerWithTicketsReal(raidaIdx, corner + 1, cca, email, tickets4, progressWeight, null))
                result = false;

        }
        
        return result;
    }
    
    public boolean fixCoinsInCornerWithTicketsReal(int raidaIdx, int corner, ArrayList<CloudCoin> ccs, String email, String[] tickets, int progressWeight, String regex) {
        int[] raidaFix;


        logger.debug(ltag, "Doing real fix in corner " + corner + " regex " + regex);
        
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
        if (email.equals(Config.SKY_EMAIL_PLACEHOLDER)) {
            logger.debug(ltag, "Generating pan");
            cc.createAn(raidaIdx, email);
        } else {
            logger.debug(ltag, "Choosing pan");
        }
        
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
        if (tickets.length == 5) {
            sb.append("&e=");
            sb.append(tickets[4]);
            sb.append("&regex=");
            sb.append(regex);
        }
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
        } else if (frs.status.equals("allfail") || frs.status.equals("fail")) {
            logger.debug(ltag, "allfail");
            setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_FAIL);
            return false;
        } else if (frs.status.equals("mixed")) {
            logger.debug(ltag, "mixed " + frs.message);
            String[] rss = frs.message.split(",");
            if (rss.length != ccs.size()) {
                logger.error(ltag, "Invalid length returned: " + rss.length + ", expected: " + ccs.size());
                // leave status as it was
                //setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
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
                    // leave status as it was
                    //status = CloudCoin.STATUS_ERROR;
                    status = CloudCoin.STATUS_FAIL;
                    logger.error(ltag, "Unknown coin status from RAIDA" + raidaIdx + ": " + strStatus);
                }
                    
                ccs.get(j).setDetectStatus(raidaIdx, status);
            }
            
            return true;
        } 
        
        logger.error(ltag, "Invalid status: " + frs.status);
        // leave status as it was
        //setCoinStatus(ccs, raidaIdx, CloudCoin.STATUS_ERROR);
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
