package global.cloudcoin.ccbank.ShowEnvelopeCoins;

import java.util.Hashtable;

public class ShowEnvelopeCoinsResult {
    public int[] coins;
    public String[] tags;
    public static int STATUS_PROCESSING = 1;
    public static int STATUS_FINISHED = 2;
    public static int STATUS_ERROR = 3;

    public int status;
    public int[][] counters;
    public Hashtable<String, String[]> envelopes;
}
