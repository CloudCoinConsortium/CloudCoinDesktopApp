package global.cloudcoin.ccbank.Sender;

public class SenderResult {
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;
    
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public int status;

    public int amount;
    public String memo;
    
    public String receiptId;   
    public String errText;
    
    public int totalAuthentic;
    public int totalCounterfeit;
    public int totalUnchecked;
    public int totalFracked;
    
    public int totalAuthenticValue;

    public int totalFrackedValue;
    
    public SenderResult() {
        memo = "Send";
        amount = 0;
        totalFilesProcessed = totalRAIDAProcessed = 0;
        totalFiles = 0;
        status = STATUS_PROCESSING;
        errText = "";
    }
}
