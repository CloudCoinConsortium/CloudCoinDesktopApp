package global.cloudcoin.ccbank.FrackFixer;

public class FrackFixerResult {
    public int fixed;
    public int failed;
    public int status;
    
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;
    public int fixingRAIDA;
    public int round;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public FrackFixerResult() {
        fixed = failed = 0;
        status = STATUS_PROCESSING;
    }
}
