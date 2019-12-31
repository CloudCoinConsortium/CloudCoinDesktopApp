
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.Unpacker;

import global.cloudcoin.ccbank.core.CloudCoin;
import java.util.ArrayList;

/**
 *
 * @author Alexander
 */
public class UnpackerResult {
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;
    
    public String errText;
    
    public ArrayList<CloudCoin> duplicates;
    
    public int status;
    public int failedFiles;
    
    public UnpackerResult() {
        status = STATUS_PROCESSING;
        duplicates = new ArrayList<CloudCoin>();
        failedFiles = 0;
    }
    
}
