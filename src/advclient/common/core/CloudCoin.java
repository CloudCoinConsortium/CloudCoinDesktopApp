package global.cloudcoin.ccbank.core;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudCoin {

    public int nn; 
    public int sn; 
    public String[] ans; 
    public String[] pans;
    private int[] detectStatus;
 
    public String fileName;
    
    private String ed;
    private String pownString;
    private String[] aoid;

    public static final int YEARSTILEXPIRE = 2;
    public String tag;

    public String originalFile = "";

    final public static int STATUS_PASS = 1;
    final public static int STATUS_FAIL = 2;
    final public static int STATUS_ERROR = 3;
    final public static int STATUS_UNTRIED = 4;
    final public static int STATUS_NORESPONSE = 5;
        
    String ls = System.getProperty("line.separator");

    int sideSize;
    
    int type;
    
    public void initCommon() {
	pans = new String[RAIDA.TOTAL_RAIDA_COUNT];
	detectStatus = new int[RAIDA.TOTAL_RAIDA_COUNT];
	setPansToAns();
        sideSize = (int) Math.sqrt(RAIDA.TOTAL_RAIDA_COUNT);
    }

    public CloudCoin(String fileName) throws JSONException {
        String data;
        if (fileName.endsWith(".png")) {
            data = AppCore.extractStackFromPNG(fileName);
            type = Config.TYPE_PNG;
        }  else {
            data = AppCore.loadFile(fileName);
            type = Config.TYPE_STACK;
        }
        if (data == null)
            throw(new JSONException("Failed to open file"));

        JSONObject o = new JSONObject(data);
        JSONArray incomeJsonArray = o.getJSONArray("cloudcoin");

        JSONObject childJSONObject = incomeJsonArray.getJSONObject(0);

        nn = childJSONObject.getInt("nn");
        sn = childJSONObject.getInt("sn");

        if (sn < 0 || sn > 16777217)
            throw(new JSONException("Invalid SN number: " + sn));

        if (nn < 0 || nn > 65535)
            throw(new JSONException("Invalid NN number: " + nn));

        JSONArray an = childJSONObject.getJSONArray("an");


        ed = childJSONObject.optString("ed");
        JSONArray aoidJson = childJSONObject.optJSONArray("aoid");
        if (aoidJson != null)
            aoid = toStringArray(aoidJson);
            
        ans = toStringArray(an);
        if (ans.length != RAIDA.TOTAL_RAIDA_COUNT)
            throw(new JSONException("Wrong an count"));

        // Direct validation instead of Validator
        Pattern p = Pattern.compile("^[A-Fa-f0-9]{32}$", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m; 
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            m = p.matcher(ans[i]);
            if (!m.matches()) {
                ans[i] = "00000000000000000000000000000000";
            }
        }

        pownString = childJSONObject.optString("pown");
        originalFile = fileName;

        initCommon();
        if (pownString != null && !pownString.isEmpty())
           	setDetectStatusFromPownString();

        JSONArray pan = childJSONObject.optJSONArray("pan");
        if (pan != null) {
            pans = toStringArray(pan);
            if (pans.length != RAIDA.TOTAL_RAIDA_COUNT)
               	throw(new JSONException("Wrong pan count"));
        }
    }

    public CloudCoin(int nn, int sn) {
	this.nn = nn;
	this.sn = sn;
	this.ans = new String[RAIDA.TOTAL_RAIDA_COUNT];

	this.fileName = getFileName();

	initCommon();

	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            detectStatus[i] = STATUS_UNTRIED;
    }

    public CloudCoin(int nn, int sn, String[] ans, String ed, String[] aoid, String tag) {
	this.nn = nn;
	this.sn = sn;
	this.ans = ans;
	this.ed = ed;
	this.aoid = aoid;
	this.tag = tag;
	this.fileName = getFileName();

	initCommon();

	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            detectStatus[i] = STATUS_UNTRIED;
    }

    public void setDetectStatus(int idx, int status) {
	detectStatus[idx] = status;
    }

    public int getDetectStatus(int idx) {
	return detectStatus[idx];
    }

    public String getFileName() {
	String result;

	result = getDenomination() + ".CloudCoin." + this.nn + "." + this.sn + ".";
	if (this.tag != null && !this.tag.isEmpty()) {
            result += this.tag + ".";
	}

	result += "stack";

	return result;
    }

    public static String[] toStringArray(JSONArray array) {
	if (array == null)
            return null;

	String[] arr = new String[array.length()];
	for (int i = 0; i < arr.length; i++) {
            arr[i] = array.optString(i);
	}

	return arr;
    }

    public String getJson() {
	return getJson(true);
    }

    public int getType() {
        return this.type;
    }
    
    public void setEd() {
       	Date date = new Date();
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);

	int month = cal.get(Calendar.MONTH);
	int year = cal.get(Calendar.YEAR);
	year = year + YEARSTILEXPIRE;

	ed = month + "-" + year;
    }
        
    public void setMissingANs() {
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (ans[i] == null) {
                ans[i] = generatePan();
                if (detectStatus[i] == CloudCoin.STATUS_PASS || detectStatus[i] == CloudCoin.STATUS_UNTRIED)
                    detectStatus[i] = CloudCoin.STATUS_ERROR;
            }
        }
            
        setPownStringFromDetectStatus();
    }
        
    public void setMissingPANs() {
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (pans[i] == null) {
                pans[i] = generatePan();
            }
        }
    }
        
        
    public String getJson(boolean includePans) {
	String json;

        ls = System.getProperty("line.separator");
                
        setMissingANs();
        setEd();

	json = "{\"cloudcoin\":[{" + ls + "\t\t\"nn\":\"" + nn + "\", " 
            + ls + "\t\t\"sn\":\"" + sn + "\"," + ls + "\t\t\"an\":[" + ls + "\"";
	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            String an = ans[i];

            json += an;
            if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
                json += "\",";
                if (((i + 1) % 5) == 0)
                    json += ls;
                        
                    json += "\"";
            }
        }

	if (includePans) {
            setMissingPANs();
            json += "\"]," + ls + "\t\t\"pan\":[" + ls + "\"";
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                String pan = pans[i];
                            
                json += pan;
                if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
                    json += "\",";
                    if (((i + 1) % 5) == 0)
                        json += ls;
                                
                    json += "\"";
                }
            }
	}

	//String pownString = getPownString();
        String aoidString = getAoidString();
	json += "\"], " + ls + "\t\t\"ed\": \"" + ed + "\", " + ls + "\t\t\"pown\": \"" + getPownString() + "\"," + ls 
            + "\t\t\"aoid\": [" + aoidString + "] } " + ls + "]}";

	return json;
    }

    public String getSimpleJson() {
	String json;
        ls = System.getProperty("line.separator");                
        setMissingANs();
                
	json = "{" + ls + "\t\t\"nn\":\"" + nn + "\"," + ls + "\t\t\"sn\":\"" + sn + "\"," + ls + "\t\t\"an\":[\"";
	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            String an = ans[i];

            json += an;
            if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
                json += "\",";
                if (((i + 1) % 5) == 0)
                    json += ls;
                        
                    json += "\"";
            }            
	}

        if (ed == null)
            setEd();
                
	json += "\"]," + ls;
        json += "\t\t\"ed\" : \"" + ed + "\"," + ls;
        json += "\t\t\"pown\": \"" + getPownString() + "\"," + ls;
        json += "\t\t\"aoid\": [" + getAoidString() + "]" + ls;
        json += "\t}";

	return json;
    }

    public String getPownString() {
	return pownString;
    }

    public String getAoidString() {
        String v = "";
        if (aoid == null)
            return v;
            
        for (int i = 0; i < aoid.length; i++) {
            if (i != 0)
                v += ", ";
            v += "\"" + aoid[i] + "\"";
        }
            
        return v;
    }
        
    public void setPownStringFromDetectStatus() {
	String s;

	s = "";
	for (int i = 0; i < detectStatus.length; i++) {
            switch (detectStatus[i]) {
		case STATUS_ERROR:
                    s += "e";
                    break;
		case STATUS_FAIL:
                    s += "f";
                    break;
		case STATUS_PASS:
                    s += "p";
                    break;
		case STATUS_UNTRIED:
                    s += "u";
                    break;
                case STATUS_NORESPONSE:
                    s += "n";
                    break;
		default:
                    s += "e";
            }
        }

	pownString = s;
    }

    private void setDetectStatusFromPownString() {
	if (pownString.length() != RAIDA.TOTAL_RAIDA_COUNT)
            return;

	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            switch (pownString.charAt(i)) {
		case 'u':
                    detectStatus[i] = STATUS_UNTRIED;
                    break;
		case 'e':
                    detectStatus[i] = STATUS_ERROR;
                    break;
		case 'f':
                    detectStatus[i] = STATUS_FAIL;
                    break;
		case 'p':
                    detectStatus[i] = STATUS_PASS;
                    break;
                case 'n':
                    detectStatus[i] = STATUS_NORESPONSE;
                    break;
		default:
                    detectStatus[i] = STATUS_UNTRIED;
                    break;
            }
	}
    }

    public void generatePans(String email) {
	for (int i = 0; i < ans.length; i++) {
            pans[i] = generatePan(i, email);
    	}
    }
    
    public String generatePan(int idx, String email) {
        String pan = AppCore.generateHex().toLowerCase();
        String component;
	String p0, p1;
        
        if (email == null)
            return pan;
        
        if (email.equals(""))
            return pan;
        
        component = "" + sn + "" + idx + email;
	component = AppCore.getMD5(component);
	if (component == null)
            return pan;

	p0 = pan.substring(0, 24);
	p1 = component.substring(0, 8).toLowerCase();
        
        pan = p0 + p1;
        
        return pan;
    }

    private String generatePan() {
        return AppCore.generateHex();
    }
    
    public void createAns(String email) {
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            createAn(i, email);
    }
        
    public void createAn(int idx, String email) {
        ans[idx] = generatePan(idx, email);
    }
        
    public int getDenomination() {  
	if (this.sn < 1 )
            return 0;
	else if (this.sn < 2097153) 
            return 1;
	else if (this.sn < 4194305) 
            return 5;
        else if (this.sn < 6291457) 
            return 25;
	else if (this.sn < 14680065) 
            return 100;
	else if (this.sn < 16777217) 
            return 250;

	return 0;
    }

    public void setPansToAns(){
	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            pans[i] = ans[i];
	}
    }

    public void setAnsToPansIfPassed() {
	for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (detectStatus[i] == STATUS_PASS) {
                ans[i] = pans[i];
            }
	}
    }

    public void calcExpirationDate() {
	Date date = new Date();
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
		
	int month = cal.get(Calendar.MONTH);
	int year = cal.get(Calendar.YEAR);
	year = year + YEARSTILEXPIRE;

	ed = month + "-" + year;
	//edHex = Integer.toHexString(month);
	//edHex += Integer.toHexString(year);
    }
    
        
    
    
    public boolean isSentFixable() {
        return isSentFixableRows(false) && isSentFixableColumns(false);
    }
    
    public boolean canbeRecoveredFromLost() {
        return isSentFixableRows(true) && isSentFixableColumns(true);
    }
    
    
    private boolean _isSentFixable(int[] statuses, boolean includeNs) {
        int badRows, goodRows;
        boolean seenGoodRows = false;
        
        badRows = 0;
        goodRows = 0;
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (statuses[i] == CloudCoin.STATUS_PASS || 
                    (includeNs && (statuses[i] == CloudCoin.STATUS_NORESPONSE || statuses[i] == CloudCoin.STATUS_ERROR))) {
                goodRows++;
                badRows = 0;
                if (goodRows == sideSize + 1)
                    seenGoodRows = true;
            } else {
                goodRows = 0;
                badRows++;
                if (badRows == sideSize)
                    return false;
            }
        }
        
        if (seenGoodRows)
            return true;
        
        return false;
    }
    
    public boolean isSentFixableRows(boolean includeNs) {
        return _isSentFixable(detectStatus, includeNs);
    }

    public boolean isSentFixableColumns(boolean includeNs) {
        if (sideSize * sideSize != RAIDA.TOTAL_RAIDA_COUNT)
            return false;
        
        int[] rotatedStatuses = new int[RAIDA.TOTAL_RAIDA_COUNT];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            int idx = i * sideSize;
            int multiplier = idx / RAIDA.TOTAL_RAIDA_COUNT;
            idx -= (RAIDA.TOTAL_RAIDA_COUNT - 1) * multiplier;
            rotatedStatuses[i] = getDetectStatus(idx);           
        }
        
        return _isSentFixable(rotatedStatuses, includeNs);
    }

    public void setNoResponseForEmpty() {
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            if (this.getDetectStatus(i) == CloudCoin.STATUS_UNTRIED)
                this.setDetectStatus(i, CloudCoin.STATUS_NORESPONSE);
    }
    
    public void setPownString(String pownstring) {
        this.pownString = pownstring;
        this.setDetectStatusFromPownString();
    }
    
}
