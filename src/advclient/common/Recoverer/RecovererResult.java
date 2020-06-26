package global.cloudcoin.ccbank.Recoverer;

public class RecovererResult {
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;

    public int totalCoins;
    public int totalCoinsProcessed;
    
    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public String errText;
    
    public int recoveredFailedCoins;
    public int recoveredCoins;
    
    public String pownString;
    
    public RecovererResult() {
        totalFilesProcessed = totalRAIDAProcessed = 0;
        totalFiles = 0;
        totalCoins = totalCoinsProcessed = 0;
        status = STATUS_PROCESSING;
        recoveredFailedCoins = 0;
        recoveredCoins = 0;
        pownString = "";
    }
}
