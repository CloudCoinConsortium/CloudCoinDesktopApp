package global.cloudcoin.ccbank.core;

public class CloudbankResult {
    public static int STATUS_OK = 0;
    public static int STATUS_ERROR = 1;
    public static int STATUS_OK_CUSTOM = 2;
    
    public int status;

    public String arefWallet;


    public String ownStatus;
    public String message;
    
    boolean keepWallet;

}
