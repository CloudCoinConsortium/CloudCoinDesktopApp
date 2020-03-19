package global.cloudcoin.ccbank.Receiver;

public class ReceiverResult {
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;
    
        
    public int totalCoins;
    public int totalCoinsProcessed;
    
    public boolean needExtra;
    
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public int amount;
    public int status;
    
    public String memo;
    
    public String receiptId;   
    
    public String errText;

    public String[] files;
    
    public ReceiverResult() {
        amount = 0;
        memo = "Receive";
        status = STATUS_PROCESSING;
        needExtra = false;
    }
}
