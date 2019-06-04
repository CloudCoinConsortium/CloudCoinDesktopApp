package global.cloudcoin.ccbank.Grader;

public class GraderResult {
    public int totalFracked;

    public int totalAuthentic;
    public int totalCounterfeit;

    public int totalLost;

    public int totalUnchecked;

    public int totalAuthenticValue;

    public int totalFrackedValue;
    
    public String receiptId;

    public GraderResult() {
        totalFracked = 0;
        totalAuthentic = 0;
        totalCounterfeit = 0;
        totalLost = 0;
        totalUnchecked = 0;
        totalAuthenticValue = 0;
        totalFrackedValue = 0;
    }
}
