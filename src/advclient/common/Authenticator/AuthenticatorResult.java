package global.cloudcoin.ccbank.Authenticator;

public class AuthenticatorResult {
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;

    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public AuthenticatorResult() {
        totalFilesProcessed = totalRAIDAProcessed = 0;
        totalFiles = 0;
        status = STATUS_PROCESSING;
    }
}
