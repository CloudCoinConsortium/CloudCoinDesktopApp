package global.cloudcoin.ccbank.FixTransfer;

import java.util.HashMap;

public class FixTransferResult {
    public int totalNotesProcessed;
    public int totalNotes;
    public int totalRAIDAProcessed;

    
    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public String errText;
    
    
    public FixTransferResult() {
        status = STATUS_PROCESSING;
    }
}
