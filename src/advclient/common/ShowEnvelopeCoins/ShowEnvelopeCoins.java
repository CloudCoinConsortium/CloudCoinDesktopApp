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
import java.util.Hashtable;

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
        CommonResponse errorResponse;
        ShowEnvelopeCoinsResponse srs;

        setSenderRAIDA();
        cc = getIDcc(sn);
        if (cc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("show?nn=");
        sb.append(cc.nn);
        sb.append("&sn=");
        sb.append(cc.sn);
        sb.append("&an=");
        sb.append(cc.ans[Config.RAIDANUM_TO_QUERY_BY_DEFAULT]);
        sb.append("&pan=");
        sb.append(cc.ans[Config.RAIDANUM_TO_QUERY_BY_DEFAULT]);
        sb.append("&denomination=");
        sb.append(cc.getDenomination());

        results = raida.query(new String[] { sb.toString() }, null, null, new int[] {Config.RAIDANUM_TO_QUERY_BY_DEFAULT});
        if (results == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }

        String resultMain = results[0];
        if (resultMain == null) {
            logger.error(ltag, "Failed to query showcoinsinenvelope");
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            return;
        }
            
        CommonResponse cr;
        cr = (CommonResponse) parseResponse(resultMain, CommonResponse.class);
        if (cr == null) {
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            logger.error(ltag, "Failed to show env coins. Invalid response");       
            return;
        }
        
        if (!cr.status.equals(Config.REQUEST_STATUS_PASS)) {
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            logger.error(ltag, "Failed to show env coins. Result: " + cr.message);       
            return;
        }
        
        o = parseArrayResponse(cr.message, ShowEnvelopeCoinsResponse.class);
        if (o == null) {
            result.status = ShowEnvelopeCoinsResult.STATUS_ERROR;
            logger.error(ltag, "Failed to parse message: " + cr.message);       
            return;
        }
        
        logger.debug(ltag, "Returned length " + o.length);
        
        result.coins = new int[o.length];
        result.tags = new String[o.length];
        
        String key;
        ShowEnvelopeCoinsResponse[] er;
        er = new ShowEnvelopeCoinsResponse[o.length];
        for (int j = 0; j < o.length; j++) {
            String tag;
            int rsn;
            int ts;
            int ats;
            CloudCoin rcc;
 
            er[j] = (ShowEnvelopeCoinsResponse) o[j];
            tag = er[j].tag;
            rsn = er[j].sn;
            ts = er[j].created;

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
