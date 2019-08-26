/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.core;

import global.cloudcoin.ccbank.FrackFixer.GetTicketResponse;
import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.json.JSONException;

/**
 *
 * @author 
 */
public class DNSSn {
    GLogger logger;
    String domain;
    String name;
    String ltag = "DNSSn";
    String path;
    
    public DNSSn(String name, String domain, GLogger logger) {
        this.logger = logger;
        this.name = name;
        this.domain = domain;
    }
    
        
    
    public boolean recordExists() {
        String rqdomain = name;
        
        if (this.domain != null)
            rqdomain = name + "." + this.domain;
        
        logger.debug(ltag, "Query " + rqdomain);
        InetAddress address;
        try {
            address = InetAddress.getByName(rqdomain);
        } catch (UnknownHostException e) {
            logger.debug(ltag, "Host not found");
            return false;
        }
        
        return true;
    }
    
    public int getSN() {
        String rqdomain = name;
        
        logger.debug(ltag, "Domain " + name);
        if (this.domain != null) {
            if (!rqdomain.endsWith("." + this.domain)) {          
                rqdomain += "." + this.domain;
            }
        }

        rqdomain = rqdomain.toLowerCase();
        logger.debug(ltag, "Domain final " + rqdomain);
        
        InetAddress address;
        int sn = -1;
        

        logger.debug(ltag, "Get sn for domain " + rqdomain);

        try {
            address = InetAddress.getByName(rqdomain);
            byte[] bytes = address.getAddress();
       
            logger.debug(ltag, "response " + address);
                           
            sn = (bytes[1] & 0xff)<< 16 | ((bytes[2] & 0xff) << 8) | bytes[3] & 0xff;
            if (sn < 0)
                return -1;
            
            logger.debug(ltag, "get SN " + sn);               
        } catch (UnknownHostException e) {
            logger.debug(ltag, "Host not found");
            return -1;
        }
        
        return sn;
    }
    
    public boolean setRecord(String path, ServantRegistry sr) {
        CloudCoin cc;

        logger.debug(ltag, "Setting record " + path);
        
        File f = new File(path);
        if (!f.exists()) {
            logger.error(ltag, "File " + path + " does not exist");
            return false;
        }    
        
        try {
            cc = new CloudCoin(f.toString());
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse coin: " + f.toString() +
                " error: " + e.getMessage());
            return false;
        }

        int raidaNum = Config.RAIDANUM_TO_QUERY_BY_DEFAULT;
        String message = getTicket(cc, sr);
        if (message == null) {
            logger.error(ltag, "Failed to get ticket");
            return false;
        }
               
        String rq = "/ddns.php?sn=" + cc.sn + "&username=" + name + "&ticket=" + message + "&raidanumber=" + raidaNum;

        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_CNT * 10000, logger);
        daFake.setExactFullUrl(Config.DDNSSN_SERVER + "/service/ddns");
        String result = daFake.doRequest(rq, null);
        if (result == null) {
            logger.error(ltag, "Failed to receive response from DDNSSN Server");
            return false;
        }
        
        CommonResponse cr = (CommonResponse) sr.getServant("FrackFixer").parseResponse(result, CommonResponse.class);
        if (!cr.status.equals("success")) {
            logger.error(ltag, "Invalid response from DDNSSN Server");
            return false;
        }
        
        return true;
    }
    
    public boolean deleteRecord(String name, CloudCoin cc, ServantRegistry sr) {
        logger.debug(ltag, "Delete record " + name + " SN: " + cc.sn);
        String message = getTicket(cc, sr);
        if (message == null) {
            logger.error(ltag, "Failed to get ticket");
            return false;
        }

        int raidaNum = Config.RAIDANUM_TO_QUERY_BY_DEFAULT;
        String rq = "/ddns_delete.php?sn=" + cc.sn + "&username=" + name + "&ticket=" + message + "&raidanumber=" + raidaNum;

        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_CNT * 10000, logger);
        daFake.setExactFullUrl(Config.DDNSSN_SERVER + "/service/ddns");
        String result = daFake.doRequest(rq, null);
        if (result == null) {
            logger.error(ltag, "Failed to receive response from DDNSSN Server");
            return false;
        }
        
        logger.debug(ltag, result);     
        CommonResponse cr = (CommonResponse) sr.getServant("FrackFixer").parseResponse(result, CommonResponse.class);
        if (!cr.status.equals("success")) {
            logger.error(ltag, "Invalid response from DDNSSN Server");
            return false;
        }
        
        return true;
    }
    
    public String getTicket(CloudCoin cc, ServantRegistry sr) {
        logger.debug(ltag, "Getting ticket for " + cc.sn);
        
        int raidaNum = Config.RAIDANUM_TO_QUERY_BY_DEFAULT;
        
        StringBuilder sb = new StringBuilder();
        sb.append("nns[]=");
        sb.append(cc.nn);
        sb.append("&sns[]=");
        sb.append(cc.sn);
        sb.append("&ans[]=");
        sb.append(cc.ans[raidaNum]);
        sb.append("&pans[]=");
        sb.append(cc.ans[raidaNum]);
        sb.append("&denomination[]=");
        sb.append(cc.getDenomination());


        DetectionAgent da = new DetectionAgent(raidaNum, logger);        
        String result = da.doRequest("/service/multi_get_ticket", sb.toString());
        if (result == null) {
            logger.error(ltag, "Failed to get tickets. Setting triad to failed");
            return null;
        }
        
        Object[] o = sr.getServant("FrackFixer").parseArrayResponse(result, GetTicketResponse.class);
        if (o == null) {
            logger.error(ltag, "Failed to parse result " + result);
            return null;
        }
        
        if (o.length != 1) {
            logger.error(ltag, "Failed to parse result (length is wrong) " + result);
            return null;
        }
        
        GetTicketResponse g = (GetTicketResponse) o[0];
        String message = g.message;
        
        logger.debug(ltag, " message " + g.message);
        if (!g.status.equals("ticket")) {
            logger.error(ltag, "Failed to get ticket for coin id " + cc.sn);
            return null;
        }
        
        if (message == null || message.length() != 44) {
            logger.error(ltag, "Invalid ticket from RAIDA");
            return null;
        }
        
        return message;
    }
}
