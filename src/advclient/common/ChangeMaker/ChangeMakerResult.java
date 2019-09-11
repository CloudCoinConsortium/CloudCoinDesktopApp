package global.cloudcoin.ccbank.ChangeMaker;


public class ChangeMakerResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public String errText;
    public int totalRAIDAProcessed;

    public String receiptId;
    public int status;
}
