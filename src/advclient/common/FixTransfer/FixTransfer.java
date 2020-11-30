package global.cloudcoin.ccbank.FixTransfer;


import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.GLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class FixTransfer extends Servant {
    String ltag = "FixTransfer";
    FixTransferResult globalResult;

    public FixTransfer(String rootDir, GLogger logger) {
        super("FixTransfer", rootDir, logger);
    }

    public void launch(HashMap<String, Integer> hm, CallbackInterface icb) {
        this.cb = icb;
 
        initRarr();
        globalResult = new FixTransferResult();
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Echoer");
                
                doFixTransfer(hm);
                

            }
        });
    }

    public void doFixTransfer(HashMap<String, Integer> hm) {
        Iterator it = hm.entrySet().iterator();
        globalResult.totalNotes = hm.keySet().size();
        
        int processedNotes = 0;
        while (it.hasNext()) {
            
            Map.Entry pair = (Map.Entry)it.next();
            String sn = (String) pair.getKey();
            int v = (int) pair.getValue();       
            
            int d; 
            try {
                d = Integer.parseInt(sn);
            } catch (NumberFormatException e) {
                continue;
            }
            
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, d);
            int presentCounter = 0;
            for (int j = 0; j < RAIDA.TOTAL_RAIDA_COUNT; j++) {
                //int rj = (RAIDA.TOTAL_RAIDA_COUNT - j - 1);
                
                boolean present = (((v >> j) & 1) > 0);
                if (present)
                    presentCounter++;
            }
            
            boolean needToAddCoin = false;
            if (presentCounter >= (RAIDA.TOTAL_RAIDA_COUNT / 2) + 1) {
                logger.debug(ltag, "Coin " + sn + " is present on the majority of RAIDA servers. Will try to add it to the other Raida servers");
                needToAddCoin = true;
            } else {
                logger.debug(ltag, "Coin " + sn + " is NOT present on the majority of RAIDA servers. Will try to delete it from the other Raida servers");
                needToAddCoin = false;
            }
            
            boolean needCall = false;
            boolean added = false;
            
            for (int j = 0; j < RAIDA.TOTAL_RAIDA_COUNT; j++) {
                //System.out.println("sn " + sn + " r="+j + " p="+present);
                boolean present = (((v >> j) & 1) > 0);
                
                if (needToAddCoin) {
                    if (!present) {
                        logger.debug(ltag, "To add: " + cc.sn);
                        addCoinToRarr(j, cc);
                        added = true;
                        if (rarr[j].size() >= Config.MAX_NOTES_TO_FIXTRANSFER - 1)
                            needCall = true;
                    }
                } else {
                    if (present) {
                        logger.debug(ltag, "To remove: " + cc.sn);
                        addCoinToRarr(j, cc);
                        added = true;
                        if (rarr[j].size() >= Config.MAX_NOTES_TO_FIXTRANSFER - 1)
                            needCall = true;
                    }
                } 
            }
            
            if (added) {
                processedNotes++;
            }
            
            if (needCall) {
                logger.debug(ltag, "Calling fixtransfer");
                doFixTransferReal();
                initRarr();
                globalResult.totalNotesProcessed += processedNotes;
                globalResult.totalRAIDAProcessed = 0;
                processedNotes = 0;
                
                if (this.cb != null)
                    this.cb.callback(globalResult);
            }
            
        }   
        
        doFixTransferReal();
        globalResult.status = FixTransferResult.STATUS_FINISHED;
        if (this.cb != null)
            this.cb.callback(globalResult);
        
    }

     public void doFixTransferReal() {
        String[] results;
        StringBuilder[] sbs;         
         
        logger.debug(ltag, "Fixing Transfer");
        
        int i;
        int cnt = 0;
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            ArrayList<Integer> r = rarr[i];
            if (r.size() == 0) 
                continue;
            
            cnt++;
        }
        
        if (cnt == 0) {
            logger.debug(ltag, "Nothing to fix");
            return;
        }
        int[] rlist = new int[cnt];
        String[] requests = new String[cnt];
        String[] posts = new String[cnt];
        StringBuilder sb, sb2;
        
        int corner = AppCore.getRandomCorner();
        logger.debug(ltag, "Picked random corner " + corner);
        int j = 0;
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            ArrayList<Integer> r = rarr[i];
            if (r.size() == 0) 
                continue;
            
            rlist[j] = i;
            sb = new StringBuilder();
            sb2 = new StringBuilder();
            sb2.append("sync=true");
            //sb.append("sync/fix_transfer?sync=true&corner=" + corner);
            sb.append("sync/fix_transfer");
            for (int sn : r) {
                sb2.append("&sn[]=");
                sb2.append(sn);
            }
            
            requests[j] = sb.toString();
            posts[j] = sb2.toString();
            j++;
        }
        
        int step = RAIDA.TOTAL_RAIDA_COUNT / cnt;
        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed += step;
                if (myCb != null) {
                    FixTransferResult ftr = new FixTransferResult();
                    ftr.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
                    ftr.totalNotesProcessed = globalResult.totalNotesProcessed;
                    ftr.status = FixTransferResult.STATUS_PROCESSING;
                    myCb.callback(ftr);
                }
            }
        }, rlist);
        
        if (results == null) {
            logger.error(ltag, "Failed to query fixtransfer");
            return;
        }

        for (i = 0; i < rlist.length; i++) {       
            int rIdx = rlist[i];
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + rIdx);
                    continue;
                }
            }
            
            logger.debug(ltag, "Raida " + rIdx + " r=" + results[i]);                       
        }

    }
    
}
