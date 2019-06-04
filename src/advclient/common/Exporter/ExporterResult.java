package global.cloudcoin.ccbank.Exporter;

import java.util.ArrayList;

public class ExporterResult {
    public int status;

    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public ArrayList<String> exportedFileNames;
    public int totalExported;
    
    public String receiptId;
    
    public String errText;

    public ExporterResult() {
        status = STATUS_PROCESSING;
        exportedFileNames = new ArrayList<String>();
        errText = "";
    }
}
