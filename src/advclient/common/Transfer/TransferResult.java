/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.Transfer;

/**
 *
 * @author Александр
 */
public class TransferResult {
    public int totalFilesProcessed;
    public int totalFiles;
    public int totalRAIDAProcessed;
    
    public int totalCoins;
    public int totalCoinsProcessed;
    
    public int step;
    
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;

    public int status;

    public int amount;
    public String memo;
     
    public String errText;
    
    public TransferResult() {
        memo = "Send";
        amount = 0;
        totalFilesProcessed = totalRAIDAProcessed = 0;
        totalFiles = 0;
        status = STATUS_PROCESSING;
        errText = "";
        step = 0;
    }
}
