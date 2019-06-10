package global.cloudcoin.ccbank.LossFixer;

public class LossFixerResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public int recovered;
    public int failed;
    public int status;

    public LossFixerResult() {
        status = STATUS_PROCESSING;
    }
}
