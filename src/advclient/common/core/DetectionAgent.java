package global.cloudcoin.ccbank.core;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

class DetectionAgent {

    private GLogger logger;
    String ltag = "DetectionAgent";

    private int connectionTimeout;
    private int readTimeout;
    private long dms;
    private String fullURL;

    private int RAIDANumber;

    private int lastStatus;

    public DetectionAgent(int RAIDANumber, int timeout, GLogger logger) {

        this.RAIDANumber = RAIDANumber;

        // TODO: remove +5 seconds. Now it is a workaround for slow RAIDAs
        this.readTimeout = timeout + 5000;
        this.connectionTimeout = timeout;
        this.ltag += "" + this.RAIDANumber;

        this.logger = logger;

        lastStatus = RAIDA.STATUS_OK;

        setDefaultFullUrl();
    }

    public void setExactFullUrl(String url) {
        this.fullURL = url;
    }

    public void setDefaultFullUrl() {
        this.fullURL = "https://RAIDA" + this.RAIDANumber + ".cloudcoin.global";
    //    this.fullURL = "https://RAIDA" + this.RAIDANumber + ".lab.shurafom.eu";
    }

    public void setFullUrl(String ip, int basePortArg) {
        int basePort = basePortArg + this.RAIDANumber;

        this.fullURL = "https://" + ip + ":" + basePort;
    }

    public String getFullURL() {
        return this.fullURL;
    }

    public int getStatus() {
        if (fullURL == null)
            lastStatus = RAIDA.STATUS_FAILED;

        return lastStatus;
    }

    public void setStatus(int status) {
        lastStatus = status;
    }


    public long getLastLatency() {
	return dms;
    }

    public String doRequest(String url, String post) {
	long tsBefore, tsAfter;
	int c;
	String data;
	StringBuilder sb = new StringBuilder();

	if (fullURL == null) {
            logger.error(ltag, "Skipping raida: " + RAIDANumber);
            lastStatus = RAIDA.STATUS_FAILED;
            return "";
	}

	//String urlIn = fullURL + "/service/" + url;
        String urlIn = fullURL + url;
	String method = (post == null) ? "GET" : "POST";

	logger.debug(ltag, method + " url " + urlIn);

	tsBefore = System.currentTimeMillis();

	disableSSLCheck();

	URL cloudCoinGlobal;
	HttpURLConnection urlConnection = null;
	try {
            cloudCoinGlobal = new URL(urlIn);
            urlConnection = (HttpURLConnection) cloudCoinGlobal.openConnection();
            urlConnection.setConnectTimeout(connectionTimeout);
            urlConnection.setReadTimeout(readTimeout);
            urlConnection.setRequestProperty("User-Agent", "Android CloudCoin App");

            if (post != null) {
                logger.debug(ltag, post);

                byte[] postDataBytes = post.getBytes("UTF-8");

                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                urlConnection.setDoOutput(true);
                urlConnection.getOutputStream().write(postDataBytes);
            }

            if (urlConnection.getResponseCode() != 200) {
                logger.error(ltag, "Invalid response from server " + urlIn + " -> " + urlConnection.getResponseCode());
                lastStatus = RAIDA.STATUS_FAILED;
                return null;
            }

            InputStream input = urlConnection.getInputStream();
            while (((c = input.read()) != -1)) { 
                sb.append((char) c);
            }

            input.close();
            logger.debug(ltag, "Response: "+ sb.toString() + " url " + urlIn);

            tsAfter = System.currentTimeMillis();
            dms = tsAfter - tsBefore;

            lastStatus = RAIDA.STATUS_OK;

            return sb.toString();
	} catch (MalformedURLException e) {
            logger.error(ltag, "Failed to fetch. Malformed URL " + urlIn);
            lastStatus = RAIDA.STATUS_FAILED;
            return null;
	} catch (IOException e) {
            logger.error(ltag, "Failed to fetch URL: " + e.getMessage());
            lastStatus = RAIDA.STATUS_FAILED;
            return null;
	} finally {
            if (urlConnection != null)
                urlConnection.disconnect();
	}
    }

    private void disableSSLCheck() {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

            }
        };
        
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            logger.error(ltag, "Failed to get SSL context: " + e.getMessage());
            return;
        } catch (KeyManagementException e) {
             logger.error(ltag, "Failed to get SSL (KM) context: " + e.getMessage());
            return;
        }
        
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
    	HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
		return true;
            }
	};

		// Install the all-trusting host verifier
	HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

    }

}


