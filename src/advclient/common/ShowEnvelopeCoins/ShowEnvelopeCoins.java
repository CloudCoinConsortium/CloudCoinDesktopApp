package global.cloudcoin.ccbank.ShowEnvelopeCoins;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import global.cloudcoin.ccbank.ShowCoins.ShowCoinsResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class ShowEnvelopeCoins extends Servant {
    String ltag = "ShowEnvelopeCoins";
    ShowEnvelopeCoinsResult result;

    public ShowEnvelopeCoins(String rootDir, GLogger logger) {
        super("ShowEnvelopeCoins", rootDir, logger);
    }

    public void launch(int sn, String envelope, CallbackInterface icb) {
        this.cb = icb;

        final int fsn = sn;

        result = new ShowEnvelopeCoinsResult();
        result.coins = new int[0];
        result.tags = new String[0];
        result.envelopes = new Hashtable<String, String[]>();
        result.counters = new int[Config.IDX_FOLDER_LAST][5];
        result.totalRAIDAProcessed = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowEnvelopeCoins");

                doShowSkyCoins(fsn, false);

                if (cb != null)
                    cb.callback(result);
            }
        });
    }
    
    public void launch(int sn, String envelope, boolean needFix, CallbackInterface icb) {
        this.cb = icb;

        final int fsn = sn;

        result = new ShowEnvelopeCoinsResult();
        result.coins = new int[0];
        result.tags = new String[0];
        result.envelopes = new Hashtable<String, String[]>();
        result.counters = new int[Config.IDX_FOLDER_LAST][5];
        result.totalRAIDAProcessed = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowEnvelopeCoins. NeedFix");

                doShowSkyCoins(fsn, needFix);

                if (cb != null)
                    cb.callback(result);
            }
        });
    }
    
    
    public void launch(int sn, CallbackInterface icb) {
        this.cb = icb;

        final int fsn = sn;

        result = new ShowEnvelopeCoinsResult();
        result.coins = new int[0];
        result.tags = new String[0];
        result.envelopes = new Hashtable<String, String[]>();
        result.counters = new int[Config.IDX_FOLDER_LAST][5];
        result.totalRAIDAProcessed = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowEnvelopeCoins (Balance)");

                doShowBalance(fsn);

                if (cb != null)
                    cb.callback(result);
            }
        });
    }
    
    public void doShowBalance(int sn) {
        CloudCoin cc;
        String[] results;
        Object[] o;
        StringBuilder[] sbs;
        String[] requests;

        cc = getIDcc(sn);
        if (cc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sbs[i] = new StringBuilder();
            sbs[i].append("show_transfer_balance?nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());
            
            requests[i] = sbs[i].toString();
        }

        results = raida.query(requests, null, null);
        if (results == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope balance");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        if (isCancelled()) {
            result.status = ShowEnvelopeCoinsResult.STATUS_CANCELLED;
            logger.error(ltag, "ShowCoins cancelled");
            return;
        }
        
        /*
        HashMap[] hashmaps = new HashMap[RAIDA.TOTAL_RAIDA_COUNT];
        //int[][] sns = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            hashmaps[i] = new HashMap<String, ShowTransferBalanceResponse>();
        }*/
              
        HashMap hm = new HashMap<String, ShowTransferBalanceResponse>();
        HashMap hm2 = new HashMap<String, Integer>();
        CloudCoin fakeCC = new CloudCoin(Config.DEFAULT_NN, 1);
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {        
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    fakeCC.setDetectStatus(i, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "Error raida" + i);
                    fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_NORESPONSE);
                continue;
            }
            
            ShowTransferBalanceResponse cr = (ShowTransferBalanceResponse) parseResponse(results[i], ShowTransferBalanceResponse.class);
            if (cr == null) {
                logger.error(ltag, "Failed to get response coin. RAIDA: " + i);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                continue;
            }

            if (!cr.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Failed to show env coins. RAIDA: " + i + " Result: " + cr.message);  
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_FAIL);
                continue;
            }
            
            fakeCC.setDetectStatus(i, CloudCoin.STATUS_PASS);
                        
            logger.debug(ltag, "raida " + i + ". Returned total " + cr.total);
            String idx = "" + cr.total;
            
            hm.put(idx, cr);
            if (!hm2.containsKey(idx)) {
                hm2.put(idx, 0);
            }
            
            int val = (int) hm2.get(idx);
            hm2.put(idx, val + 1);

            
        }
        fakeCC.setPownStringFromDetectStatus();
        logger.debug(ltag, "ShowBalance pownstring " + fakeCC.getPownString());
        //System.out.println("ps="+ fakeCC.getPownString());
      
        Iterator it = hm2.entrySet().iterator();
        int max = 0;
        String keyTotal = "0";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            
            if ((int) pair.getValue() >= max) {
                keyTotal = (String) pair.getKey();
                max = (int) pair.getValue();
            }
            
            it.remove(); // avoids a ConcurrentModificationException
        }
        
        logger.debug(ltag, "Maximum Total: " + keyTotal + " Number of Raida servers reported this total: " + max);
        
        ShowTransferBalanceResponse cr2 = (ShowTransferBalanceResponse) hm.get(keyTotal);
        if (cr2 == null) {
            logger.debug(ltag, "Failed to find key " + keyTotal);
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }
        
        logger.debug(ltag, "Picked: 250s:" + cr2.d250 + ", 100s:" + cr2.d100 + ", 25s:" + cr2.d25 + ", 5s:" + cr2.d5 + ", 1s:" + cr2.d1 + ", total: " + keyTotal);
        //System.out.println("Picked: 250s:" + cr2.d250 + ", 100s:" + cr2.d100 + ", 25s:" + cr2.d25 + ", 5s:" + cr2.d5 + ", 1s:" + cr2.d1 + ", total: " + keyTotal);
        result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;
        //System.out.println("Maximum Total: " + keyTotal + " Number of Raida servers reported this total: " + max);
        /*
        if (1==1)
            return;*/
        
        int idx = Config.IDX_FOLDER_BANK;
        result.counters[idx][Config.IDX_1] = cr2.d1;
        result.counters[idx][Config.IDX_5] = cr2.d5;
        result.counters[idx][Config.IDX_25] = cr2.d25;
        result.counters[idx][Config.IDX_100] = cr2.d100;
        result.counters[idx][Config.IDX_250] = cr2.d250;
        
     
    }
    
    public void doShowSkyCoins(int sn, boolean needFix) {
        CloudCoin cc;
        String[] results;
        Object[] o;
        StringBuilder[] sbs;
        String[] requests;

        System.out.println("nf="+needFix);
        
        cc = getIDcc(sn);
        if (cc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sbs[i] = new StringBuilder();
            sbs[i].append("show?nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());
            
            requests[i] = sbs[i].toString();
        }

        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object oresult) {
                result.totalRAIDAProcessed++;
                //result.status = ShowEnvelopeCoinsResult.STATUS_PROCESSING;
                if (myCb != null) {
                    ShowEnvelopeCoinsResult ser = new ShowEnvelopeCoinsResult();
                    ser.status = ShowEnvelopeCoinsResult.STATUS_PROCESSING;
                    ser.totalRAIDAProcessed = result.totalRAIDAProcessed;
                    
                    myCb.callback(ser);
                }
            }
        });
        if (results == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        if (isCancelled()) {
            result.status = ShowEnvelopeCoinsResult.STATUS_CANCELLED;
            logger.error(ltag, "ShowCoins cancelled");
            return;
        }
        
        HashMap[] hashmaps = new HashMap[RAIDA.TOTAL_RAIDA_COUNT];
        int[][] sns = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            hashmaps[i] = new HashMap<String, ShowEnvelopeCoinsResponse>();
        }
              
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sns[i] = new int[0];         
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }
            
            CommonResponse cr = (CommonResponse) parseResponse(results[i], CommonResponse.class);
            if (cr == null) {
                logger.error(ltag, "Failed to get response coin. RAIDA: " + i);
                continue;
            }

            if (!cr.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Failed to show env coins. RAIDA: " + i + " Result: " + cr.message);       
                continue;
            }
            
            o = parseArrayResponse(cr.message, ShowEnvelopeCoinsResponse.class);
            if (o == null) {
                logger.error(ltag, "Failed to parse message: " + cr.message);       
                continue;
            }
            
            logger.debug(ltag, "Returned length " + o.length);

            ShowEnvelopeCoinsResponse[] er = new ShowEnvelopeCoinsResponse[o.length];
            sns[i] = new int[o.length];
            for (int j = 0; j < o.length; j++) {
                String tag;
                int rsn;
                int ts;
            
                er[j] = (ShowEnvelopeCoinsResponse) o[j];
                tag = er[j].tag;
                rsn = er[j].sn;
                ts = er[j].created;
                
                
                hashmaps[i].put("" + rsn, er[j]);
                sns[i][j] = rsn;
                
                logger.debug(ltag, "Wallet " + sn + " raida" + i + " coin#" + j + " sn=" +rsn + " t=" + tag + " ts="+ts);
            }  
        }
        
        if (isCancelled()) {
            result.status = ShowEnvelopeCoinsResult.STATUS_CANCELLED;
            logger.error(ltag, "ShowCoins cancelled p. 2");
            return;
        }
      
        int vsns[] = AppCore.getSNSOverlap(sns);
        if (vsns == null) {
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            logger.error(ltag, "Failed to get coins");
            return;
        }
         
        
        if (needFix) {
            logger.info(ltag, "Calculating fix_transfer set of coins");
            ArrayList<Integer>[] rarr = new ArrayList[RAIDA.TOTAL_RAIDA_COUNT];
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                rarr[i] = new ArrayList<Integer>();
                int[] snst = sns[i];
                for (int j = 0; j < vsns.length; j++) {
                    boolean found = false;
                    for (int k = 0; k < snst.length; k++) {
                        if (snst[k] == vsns[j]) {
                            found = true;
                            break;
                        }                  
                    }
                
                    if (!found) {
                        logger.debug(ltag, user + ": SN " + vsns[j] + " wasn't in the common set for raida " + i + ". Adding it to fix_transfer");
                        //System.out.println(user + ": SN " + vsns[j] + " wasn't in the common set for raida " + i + ". Adding it to fix_transfer");
                        rarr[i].add(vsns[j]);
                    }
                }
            
            }
            
            fixTransfer(rarr);
        }
        
        
        
        result.coins = new int[vsns.length];
        result.tags = new String[vsns.length];
        
        String key;
        ShowEnvelopeCoinsResponse er;
        for (int j = 0; j < vsns.length; j++) {
            String tag;
            int rsn;
            int ts;
            int ats;
            CloudCoin rcc;
 
            rsn = vsns[j];
            String rsnKey = "" + rsn;
            boolean found = false;
            int ex;
            for (ex = 0; ex < RAIDA.TOTAL_RAIDA_COUNT; ex++) {
                if (hashmaps[ex].containsKey(rsnKey)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                logger.error(ltag, "Can't find hash for key " + rsnKey);
                continue;
            }
    
            er = (ShowEnvelopeCoinsResponse) hashmaps[ex].get(rsnKey);
            
            tag = er.tag;
            rsn = er.sn;
            ts = er.created;

            ats = ts / Config.SECONDS_TO_AGGREGATE_ENVELOPES;

            rcc = new CloudCoin(cc.nn, rsn);
            int idx = Config.IDX_FOLDER_BANK;
            switch (rcc.getDenomination()) {
                case 1:
                    result.counters[idx][Config.IDX_1]++;
                    break;
                case 5:
                    result.counters[idx][Config.IDX_5]++;
                    break;
                case 25:
                    result.counters[idx][Config.IDX_25]++;
                    break;
                case 100:
                    result.counters[idx][Config.IDX_100]++;
                    break;
                case 250:
                    result.counters[idx][Config.IDX_250]++;
                    break;
            }    
            
            result.coins[j] = rsn;
            result.tags[j] = tag;
            key = ats + "." + tag;
            
            String[] coinData = new String[3];
            coinData[0] = tag;
            coinData[1] = "" + rcc.getDenomination();
            coinData[2] = AppCore.getDate("" + ts);

            if (!result.envelopes.containsKey(key)) {
                result.envelopes.put(key, coinData);
            } else {
                String[] accum = result.envelopes.get(key);
                
                int wehave;
                try {
                    wehave = Integer.parseInt(accum[1]);
                } catch (NumberFormatException e) {
                    logger.error(ltag, "Failed to parse amount: " + accum[1]);
                    continue;
                }
                
                wehave += rcc.getDenomination();                
                accum[1] = "" + wehave;
            
                result.envelopes.put(key, accum);
            }
            
        }

        result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;

        logger.info(ltag, "us=" + user + " sn=" +sn + " r="+results[0]);
                
    }

}
