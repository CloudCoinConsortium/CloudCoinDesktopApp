package global.cloudcoin.ccbank.core;

import java.io.File;
import java.util.HashMap;


public class BillPayItem {
    public static int SEND_METHOD_EMAIL = 1;
    public static int SEND_FORMAT_STACK = 1;
    
    public static int SEND_STATUS_READY = 1;
    public static int SEND_STATUS_SKIP = 2;
    public static int SEND_STATUS_SENT = 3;
    public static int SEND_STATUS_STUCK = 4;
    
    public int method;
    public int format;
    public int idx;
    
    public int total;
    public int s1, s5, s25, s100, s250;
    public String address;
    public String data;
    
    public String filename;
    //public String metadata;

    public int status;

    public HashMap<String,String> metadata;
    
    public BillPayItem(int idx) {
        metadata = new HashMap<String, String>();
        this.idx = idx;
    }
    
    public void calcTotal() {
        total = s1 + s5 * 5 + s25 * 25 + s100 * 100 + s250 * 250;
    }
    
    public void setMetadataItem(String key, String value) {
        metadata.put(key, value);
    }
    
    public String getMetadataItem(String key) {
        return metadata.get(key);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String method = "unknown";
        String format = "unknown";
        String status = "unknown";
        
        if (this.status == BillPayItem.SEND_STATUS_READY) {
            status = "ready";
        } else if (this.status == BillPayItem.SEND_STATUS_SKIP) {
            status = "skip";
        } else if (this.status == BillPayItem.SEND_STATUS_SENT) {
            status = "sent";
        } else if (this.status == BillPayItem.SEND_STATUS_STUCK) {
            status = "stuck";
        }
        
        if (this.method == BillPayItem.SEND_METHOD_EMAIL)
            method = "email";
        
        if (this.format == this.SEND_FORMAT_STACK)
            format = "stack";
        
        sb.append(method);
        sb.append(",");
        sb.append(format);
        sb.append(",");
        sb.append(total);
        sb.append(",");
        sb.append(address);
        sb.append(",");
        sb.append(data);
        sb.append(",");
        sb.append(status);

        return sb.toString();
    }
    
    public String getStuckFilename(String walletName) {
        String fname = new File(filename).getName();
        
        String doneDir = AppCore.getUserDir(Config.DIR_EMAIL_OUT, walletName) + File.separator + fname; 
        String tag = idx + "_" + address.replaceAll("\\.", "_");
        
        return doneDir + File.separator + total + ".CloudCoin." + tag + ".stack";    
    }
    
    public String getSentFilename(String walletName) {
        String fname = new File(filename).getName();
        
        String outDir = AppCore.getUserDir(Config.DIR_EMAIL_SENT, walletName) + File.separator + fname;
        String tag = idx + "_" + address.replaceAll("\\.", "_");
        
        return outDir + File.separator + total + ".CloudCoin." + tag + ".stack";
        
    }
    
}
