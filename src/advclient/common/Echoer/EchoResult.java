package global.cloudcoin.ccbank.EchoResult;

import java.util.ArrayList;

public class EchoResult {
    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    
    public int totalRAIDAProcessed;
    public String errText;

    public long[] latencies;
    
    public EchoResult() {
        status = STATUS_PROCESSING;
        errText = "";
    }
}