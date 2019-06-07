package global.cloudcoin.ccbank.core;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.Calendar;

public class CloudCoin {

	public int nn; 
	public int sn; 
	public String[] ans; 
	public String[] pans;
	private int[] detectStatus;
 
	private String ed;
	private String edHex;
	private String pownString;
	private String[] aoid;
	private String fileName;

	public static final int YEARSTILEXPIRE = 2;
	public String tag;

	public String originalFile = "";

	final public static int STATUS_PASS = 1;
	final public static int STATUS_FAIL = 2;
	final public static int STATUS_ERROR = 3;
	final public static int STATUS_UNTRIED = 4;
        
        String ls = System.getProperty("line.separator");

	public void initCommon() {
		pans = new String[RAIDA.TOTAL_RAIDA_COUNT];
		detectStatus = new int[RAIDA.TOTAL_RAIDA_COUNT];
		setPansToAns();
	}

	public CloudCoin(String fileName) throws JSONException {
            String data = AppCore.loadFile(fileName);

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

        public void setEd() {
            	Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		int month = cal.get(Calendar.MONTH);
		int year = cal.get(Calendar.YEAR);
		year = year + YEARSTILEXPIRE;

		ed = month + "-" + year;
        }
        
	public String getJson(boolean includePans) {
		String json;

                setEd();

		json = "{\"cloudcoin\":[{\"nn\":" + nn + ",\"sn\":" + sn + ",\"an\":[\"";
		for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                    String an = ans[i];
                    
                    if (an == null)
                        an = "00000000000000000000000000000000";
                    
                    json += an;
                    if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
			json += "\",\"";
                    }
		}

		if (includePans) {
			json += "\"], \"pan\":[\"";
			for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                            String pan = pans[i];
                            
                            if (pan == null)
                                pan = "00000000000000000000000000000000";
                            
                            json += pan;
                            if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
				json += "\",\"";
                            }
			}
		}

		String pownString = getPownString();
                String aoidString = getAoidString();
		json += "\"], \"ed\": \"" + ed + "\", \"pown\": \"" + pownString + "\","
                        + " \"aoid\": [" + aoidString + "] }]}";

		return json;
	}

	public String getSimpleJson() {
		String json;

		json = "{" + ls + "\t\t\"nn\":\"" + nn + "\"," + ls + "\t\t\"sn\":\"" + sn + "\"," + ls + "\t\t\"an\":[\"";
		for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                    String an = ans[i];
                    
                    if (an == null)
                        an = "00000000000000000000000000000000";
                    
                    json += an;
                    if (i != RAIDA.TOTAL_RAIDA_COUNT - 1) {
			json += "\", \"";
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
				default:
					detectStatus[i] = STATUS_UNTRIED;
					break;
			}
		}
	}

	public void generatePans(String email) {
		String component;
		String p0, p1;

		for (int i = 0; i < ans.length; i++) {
			pans[i] = generatePan().toLowerCase();

			if (!email.equals("")) {
				component = "" + sn + "" + i + email;
				component = AppCore.getMD5(component);
				if (component == null)
					continue;

				p0 = pans[i].substring(0, 24);
				p1 = component.substring(0, 8).toLowerCase();

				pans[i] = p0 + p1;
			}
		}
	}

	private String generatePan() {
		return AppCore.generateHex();
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
		edHex = Integer.toHexString(month);
		edHex += Integer.toHexString(year);
	}

}
