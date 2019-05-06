package global.cloudcoin.ccbank.Sender;

public class SenderResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int status;

    public int amount;
    public String memo;
    
    public SenderResult() {
        memo = "Send";
        amount = 0;
        status = STATUS_PROCESSING;
    }
}
