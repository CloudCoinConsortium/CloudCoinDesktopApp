package global.cloudcoin.ccbank.Emailer;

import java.util.ArrayList;

public class EmailerResult {
    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int totalEmails;
    public int sentEmails;
    
    public String errText;

    public EmailerResult() {
        status = STATUS_PROCESSING;
        errText = "";
        totalEmails = sentEmails = 0;
    }
}