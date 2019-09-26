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
        final String fenvelope = envelope;

        result = new ShowEnvelopeCoinsResult();
        result.coins = new int[0];
        result.tags = new String[0];
        result.envelopes = new Hashtable<String, String[]>();
        result.counters = new int[Config.IDX_FOLDER_LAST][5];
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowEnvelopeCoins");

                doShowSkyCoins(fsn);

                if (cb != null)
                    cb.callback(result);
            }
        });
    }
    
    
    public void doShowSkyCoins(int sn) {
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
            
            requests[i] = sbs[i].toString();
        }

        results = raida.query(requests, null, null);
        if (results == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
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
                
            //System.out.println("r="+i+ " st="+cr.status + " length " + o.length);
              
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
      
        int vsns[] = AppCore.getSNSOverlap(sns);
        if (vsns == null) {
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            logger.error(ltag, "Failed to get coins");
            return;
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
