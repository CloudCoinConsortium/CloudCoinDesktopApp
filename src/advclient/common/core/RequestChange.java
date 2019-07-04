/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient.common.core;

import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.DetectionAgent;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.ServantRegistry;


/**
 *
 * @author Александр
 */
public class RequestChange {
    GLogger logger;
    int sn;
    int total;
    String tag;
    String ltag = "RequestChange";

    
    public RequestChange(int sn, String tag, int total, GLogger logger) {
        this.logger = logger;
        this.tag = tag;
        this.sn = sn;
        this.total = total;
    }

    
    public boolean request(ServantRegistry sr) {
        int raidaNum = Config.RAIDANUM_TO_QUERY_REQUEST_CHANGE;
        
        StringBuilder sb = new StringBuilder();
        sb.append("sn=");
        sb.append(sn);
        sb.append("&tag=");
        sb.append(tag);
        sb.append("&total_coins_sent=");
        sb.append(total);

        logger.debug(ltag, "Quety raida " + raidaNum + ": " + sb.toString());
        
        DetectionAgent da = new DetectionAgent(raidaNum, logger);  
        da.setReadTimeout(Config.REQUEST_CHANGE_READ_TIMEOUT);
        String result = da.doRequest("/service/request_change?" + sb.toString(), null);
        if (result == null) {
            logger.error(ltag, "Failed to query RequestChange");
            return false;
        }

        CommonResponse cr = (CommonResponse) sr.getServant("FrackFixer").parseResponse(result, CommonResponse.class);
        if (cr == null) {
            logger.error(ltag, "Failed to parse result " + result);
            return false;
        }

        logger.debug(ltag, " message " + cr.message);
        if (!cr.status.equals("success")) {
            logger.error(ltag, "Failed to break coin. Status: " + cr.status);
            return false;
        }
        
        return true;
    }
    
    
    
}
