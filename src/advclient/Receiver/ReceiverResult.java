package global.cloudcoin.ccbank.Receiver;

public class ReceiverResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int amount;
    public int status;
    
    public String memo;
    
    public ReceiverResult() {
        amount = 0;
        memo = "Receive";
        status = STATUS_PROCESSING;
    }
}
