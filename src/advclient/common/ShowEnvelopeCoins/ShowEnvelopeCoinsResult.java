package global.cloudcoin.ccbank.ShowEnvelopeCoins;

import java.util.HashMap;
import java.util.Hashtable;

public class ShowEnvelopeCoinsResult {
    public int[] coins;
    public String[] tags;
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;
    public static int STATUS_CANCELLED = 4;
    
    public int totalRAIDAProcessed = 0;

    public int status;
    public int idCoinStatus;
    public String idPownString;
    public int[][] counters;
    
    public int[] debugBalances;
    public HashMap<String, Integer> debugSNs;
    public int[] debugContentBalances;
    public Hashtable<String, String[]> envelopes;
}
