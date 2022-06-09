package global.cloudcoin.ccbank.ShowEnvelopeCoins;



import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.util.Arrays;
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


                doShowSkyCoinsWithContent(fsn);

                if (cb != null)
                    cb.callback(result);
            }
        });
    }
    
    
    public void launch(int sn, boolean needFix, CallbackInterface icb) {
        this.cb = icb;

        final int fsn = sn;
        final boolean fneedFix = needFix;

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

                doShowBalance(fsn, fneedFix);

                if (cb != null)
                    cb.callback(result);
            }
        });
    }
    
    public void doShowBalance(int sn, boolean needFix) {
        CloudCoin cc;
        String[] results;
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

        ShowEnvelopeCoinsResult globalResult = new ShowEnvelopeCoinsResult();
        globalResult.totalRAIDAProcessed = 0;
        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    ShowEnvelopeCoinsResult ser = new ShowEnvelopeCoinsResult();
                    ser.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
                    ser.status = ShowEnvelopeCoinsResult.STATUS_PROCESSING;
                    
                    myCb.callback(ser);
                }
            }
        });
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

        if (isDebug())
            result.debugBalances = new int[RAIDA.TOTAL_RAIDA_COUNT];
        
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
            
            
            if (cr.status.equals(Config.REQUEST_STATUS_FAIL)) {
                logger.error(ltag, "Failed (counterfeit) to show env coins. RAIDA: " + i + " Result: " + cr.message);  
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_FAIL);
                continue;
            
            }


            if (!cr.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Failed to show env coins. RAIDA: " + i + " Result: " + cr.message);  
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                continue;
            }
            
            fakeCC.setDetectStatus(i, CloudCoin.STATUS_PASS);           
            logger.debug(ltag, "raida " + i + ". Returned total " + cr.total);
            String idx = "" + cr.total;
                        
            if (isDebug())
                result.debugBalances[i] = cr.total;
            
            hm.put(idx, cr);
            if (!hm2.containsKey(idx)) {
                hm2.put(idx, 0);
            }
            
            int val = (int) hm2.get(idx);
            hm2.put(idx, val + 1);

            
        }
        fakeCC.setPownStringFromDetectStatus();
        fakeCC.countResponses();
        result.idPownString = fakeCC.getPownString();
        logger.debug(ltag, "ShowBalance pownstring " + fakeCC.getPownString());
        

        if (!fakeCC.isAuthentic()) {
            logger.error(ltag, "Not enough valid responses. Balance is zero");
            //result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            if (fakeCC.isCounterfeit()) {
                result.idCoinStatus = CloudCoin.STATUS_FAIL;
            } else {
                result.idCoinStatus = CloudCoin.STATUS_ERROR;
            }
            return;
        } 
        

        //System.out.println("ps="+ fakeCC.getPownString());
      
        int totalBalances = hm2.entrySet().size();
        result.idCoinStatus = CloudCoin.STATUS_PASS;
        
        logger.debug(ltag, "totalBalances " + totalBalances);
        Iterator it = hm2.entrySet().iterator();
        int max = 0;
        String keyTotal = "0";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            
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
        result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;

        
        int idx = Config.IDX_FOLDER_BANK;
        result.counters[idx][Config.IDX_1] = cr2.d1;
        result.counters[idx][Config.IDX_5] = cr2.d5;
        result.counters[idx][Config.IDX_25] = cr2.d25;
        result.counters[idx][Config.IDX_100] = cr2.d100;
        result.counters[idx][Config.IDX_250] = cr2.d250;
        
        /*
        if (totalBalances > 1 && needFix) {
            logger.debug(ltag, "Will run fix_Transfer");
            Thread t = new Thread(new Runnable() {
                public void run() {
                    doShowSkyCoins(sn, true);
                }
            });
            
            t.start();           
        }
        */
        
        result.ccResult = fakeCC;
        
     
    }
    
    public void doShowSkyCoinsWithContent(int sn) {
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
            sbs[i].append("&content=1");
            
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
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            hashmaps[i] = new HashMap<String, ShowEnvelopeCoinsResponse>();
        }
        HashMap hm2 = new HashMap<String, Integer>();
              
        if (isDebug()) {
            result.debugContentBalances = new int[RAIDA.TOTAL_RAIDA_COUNT];
        }
        
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

            
            ShowEnvelopeCoinsResponse er = (ShowEnvelopeCoinsResponse) parseResponse(results[i], ShowEnvelopeCoinsResponse.class);
           
            if (er == null) {
                logger.error(ltag, "Failed to get response coin. RAIDA: " + i);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_NORESPONSE);
                continue;
            }

            if (!er.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Failed to show env coins. RAIDA: " + i + " Result: " + er.message);  
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_FAIL);
                continue;
            }
            
            fakeCC.setDetectStatus(i, CloudCoin.STATUS_PASS);
            o = parseArrayResponse(er.contents, ShowEnvelopeCoinsContentsResponse.class);
            if (o == null) {
                logger.error(ltag, "Failed to parse contents on raida" + i + ": " + er.contents);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                continue;
            }
            
            logger.debug(ltag, "raida" + i + ", contents length: " + o.length);
            for (int j = 0; j < o.length; j++) {                
                ShowEnvelopeCoinsContentsResponse seccr = (ShowEnvelopeCoinsContentsResponse) o[j];
                //int ts = seccr.created / Config.SECONDS_TO_AGGREGATE_ENVELOPES;
                //String hkey = seccr.amount + "." + seccr.tag ;
                String hkey = seccr.tag ;
                
                hkey = AppCore.getGuidForKeyFromObj(hkey);                
                if (hashmaps[i].containsKey(hkey)) {
                    logger.debug(ltag, "Duplicate key from the same raida " + i + ": " + hkey);
                    continue;
                }
                
                hashmaps[i].put(hkey, seccr);                
                if (!hm2.containsKey(hkey)) {
                    hm2.put(hkey, 0);
                }
            
                int val = (int) hm2.get(hkey);
                hm2.put(hkey, val + 1);
                
                if (isDebug()) {
                    result.debugContentBalances[i] += seccr.amount;
                }
                    
            }
            
        }
        
        if (isCancelled()) {
            result.status = ShowEnvelopeCoinsResult.STATUS_CANCELLED;
            logger.error(ltag, "ShowCoins cancelled p. 2");
            return;
        }
        
        fakeCC.setPownStringFromDetectStatus();
        fakeCC.countResponses();
        result.idPownString = fakeCC.getPownString();
        logger.debug(ltag, "ShowBalance pownstring " + fakeCC.getPownString());
        if (!fakeCC.isAuthentic()) {
            logger.error(ltag, "Not enough valid responses. Balance is zero");
            //result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            if (fakeCC.isCounterfeit()) {
                result.idCoinStatus = CloudCoin.STATUS_FAIL;
            } else {
                result.idCoinStatus = CloudCoin.STATUS_ERROR;
            }
            return;
        }
        
        result.idCoinStatus = CloudCoin.STATUS_PASS;    
        Iterator it = hm2.entrySet().iterator();
        int max = 0;
        String keyTotal = "0";
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            keyTotal = (String) pair.getKey();
            max = (int) pair.getValue();
                        
            if (max < Config.MIN_PASSED_NUM_TO_BE_AUTHENTIC) {
                logger.debug(ltag, "Envelope " + keyTotal + " has only " + max + " passed raidas. Skipping it");
                continue;
            }
            
            String[] tags = new String[RAIDA.TOTAL_RAIDA_COUNT];
            String[] coinData = new String[5];
            String key = null;
            for (int r = 0; r < RAIDA.TOTAL_RAIDA_COUNT; r++) {
                if (hashmaps[r].containsKey(keyTotal)) {
                    ShowEnvelopeCoinsContentsResponse seccr = (ShowEnvelopeCoinsContentsResponse) hashmaps[r].get(keyTotal);
                    
                    if (key == null) {
                        String tkey = AppCore.getGuidForKeyFromObj(seccr.tag);   
                        String memo = AppCore.getMemoFromObj(seccr.tag);
                        
                        key = seccr.created + "." + tkey;
                    
                        coinData[0] = memo;
                        coinData[1] = "" + seccr.amount;
                        coinData[2] = seccr.created;//AppCore.getDate("" + seccr.created);AppCore.getDateFromStr(seccr.created) 
                        coinData[3] = tkey;
                    }
                    
                    tags[r] = seccr.tag;
                    //break;
                } else {
                    tags[r] = null;
                }
               
            }
            
            if (key != null) {
                String[][] rd = new String[RAIDA.TOTAL_RAIDA_COUNT][];
                for (int r = 0; r < RAIDA.TOTAL_RAIDA_COUNT; r++) {
                    if (tags[r] == null) {
                        rd[r] = null;
                        continue;
                    }

                    rd[r] = AppCore.getPartsFromObj(tags[r]);
                }
                
                coinData[4] = AppCore.assembleMessage(rd);                
                result.envelopes.put(key, coinData);
            }

            it.remove(); // avoids a ConcurrentModificationException
        }
        

        result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;
     
                
    }
    
    
    public boolean doRAIDAShowCoinsPage(CloudCoin cc, int page, int[][] sns) {
        String[] requests;
        StringBuilder[] sbs;
        String[] results;

        
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
            sbs[i].append("&page=");
            sbs[i].append(page);
            
            
            requests[i] = sbs[i].toString();
        }


        result.totalRAIDAProcessed = 0;
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
                    ser.page = page;
                    
                    myCb.callback(ser);
                }
            }
        });
        if (results == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return false;
        }

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sns[i] = new int[0];         
            if (results[i] != null) {
                if (results[i].equals("") || results[i].equals("E")) {
                    logger.error(ltag, "Skipped raida or error raida" + i);
                    continue;
                }
            }

            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                continue;
            }
            
            String rsns[] = results[i].split(",");
            sns[i] = new int[rsns.length];
            for (int s =0; s < rsns.length; s++) {
                try {
                    int sn = Integer.parseInt(rsns[s]);
                    sns[i][s] = sn;
                                       
                } catch (NumberFormatException e) {
                    sns[i] = new int[0];
                    System.out.println("bad " + i);
                    logger.error(ltag, "invalid response from raida " + i + ": failed to parse CSV SNs");
                    continue;
                    
                }
                
               
                
            
            }
            
            /*
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
*/

        }
    
        return true;
    }
    
    public void doShowSkyCoins(int sn, boolean needFix) {
        CloudCoin cc;
                
        cc = getIDcc(sn);
        if (cc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }


        


        
        //HashMap[] hashmaps = new HashMap[RAIDA.TOTAL_RAIDA_COUNT];
        int[][] sns = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sns[i] = new int[0]; 
        }
        //for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
        //    hashmaps[i] = new HashMap<String, ShowEnvelopeCoinsResponse>();
        //}
        
        // hardcoded in raida
        int pageSize = 4000;
        int page = 0;
        for (;;) {
            boolean brv;
            int[][] lsns = new int[RAIDA.TOTAL_RAIDA_COUNT][];
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
               lsns[i] = new int[0]; 
            }
            brv = doRAIDAShowCoinsPage(cc, page, lsns);
            if (!brv) {
                logger.error(ltag, "Failed to show result for page ");
                return;        
            }
            
        
            for (int l = 0; l < RAIDA.TOTAL_RAIDA_COUNT; l++) {
                int[] both = Arrays.copyOf(sns[l], sns[l].length + lsns[l].length);
                System.arraycopy(lsns[l], 0, both, sns[l].length, lsns[l].length);
                
                sns[l] = both;

            }
            
            int counts=0;

            for (int p = 0; p < lsns.length; p++) {
                if (lsns[p].length == pageSize) {
                    counts++;
                }
            
            }
            logger.debug(ltag, "counts " + counts);
            System.out.println("counts="+counts);
            if (counts < 13) {
                break;
            }
            
            page++;
            
            
            // 
            
        }

        if (isCancelled()) {
            result.status = ShowEnvelopeCoinsResult.STATUS_CANCELLED;
            logger.error(ltag, "ShowCoins cancelled");
            return;
        }
        
        if (isDebug()) {
            //System.out.println("max="+max);
            result.debugSNs = new HashMap<String, Integer>();
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                for (int j = 0; j < sns[i].length; j++) { 
                    String key = "" + sns[i][j];
                    //System.out.println("k="+key);
              
                    if (!result.debugSNs.containsKey(key)) {
                        result.debugSNs.put(key, 0);
                    }
            
                    int val = (int) result.debugSNs.get(key);
                    int shift = 1 << i;
                    val |= shift;
                    result.debugSNs.put(key, val);
                }
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
            initRarr();
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                int[] snst = sns[i];
                logger.debug(ltag, "Looking for coins to add");
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
                        //System.out.println(user + ": SN1 " + vsns[j] + " wasn't in the common set for raida " + i + ". Adding it to fix_transfer");
                        addSnToRarr(i, vsns[j]);
                        //rarr[i].add(vsns[j]);
                    }
                }
                
                logger.debug(ltag, "Looking for coins to delete");
                for (int j = 0; j < snst.length; j++) {
                    boolean found = false;
                    for (int k = 0; k < vsns.length; k++) {
                        if (snst[j] == vsns[k]) {
                            found = true;
                            break;
                        }                  
                    }
                
                    if (!found) {
                        logger.debug(ltag, user + ": SN2 " + snst[j] + " is a coin on rada " + i + ". Other raida servers don't have a quorum on it. Adding it to fix_transfer");
                        //System.out.println(user + ": SN2 " + snst[j] + " wasn't in the common set for raida " + i + ". Adding it to fix_transfer");
                        addSnToRarr(i, snst[j]);
                    }
                }
            
            }
            
            fixTransfer(rarr);
        }
        
        
        
        result.coins = new int[vsns.length];
        result.tags = new String[vsns.length];
        

        for (int j = 0; j < vsns.length; j++) {
            CloudCoin rcc;
            int rsn = vsns[j];
            result.coins[j] = rsn;
            /*
            String tag;
            int rsn;
            int ts;
            int ats;
            
 
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
*/
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
            /*
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
            
            */
            
        }

        result.status = ShowEnvelopeCoinsResult.STATUS_FINISHED;

        logger.info(ltag, "us=" + user + " sn=" +sn + " ");
                
        System.out.println("done");

    }
    
    
}
