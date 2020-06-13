package global.cloudcoin.ccbank.Echoer;

import global.cloudcoin.ccbank.EchoResult.EchoResult;
import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.GLogger;


public class Echoer extends Servant {

    String ltag = "Echoer";
    EchoResponse[] ers;
    long[] latencies;
    EchoResult globalResult;

    public Echoer(String rootDir, GLogger logger) {
        super("Echoer", rootDir, logger);
        noUserBound();
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        ers = new EchoResponse[RAIDA.TOTAL_RAIDA_COUNT];
        latencies = new long[RAIDA.TOTAL_RAIDA_COUNT];

        globalResult = new EchoResult();
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Echoer");
                
                raida.setReadTimeout(Config.ECHO_TIMEOUT);
                doEcho();
                raida.setReadTimeout(Config.READ_TIMEOUT);

                if (cb != null)
                    cb.callback(globalResult);
            }
        });
    }

    private void copyFromGlobalResult(EchoResult aResult) {
        aResult.status = globalResult.status;
        aResult.errText = globalResult.errText;
        aResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
    }
    
    private void setRAIDAUrl(String ip, int basePort) {
        raida.setUrl(ip, basePort);
    }

    public void doEcho() {
        cleanLogDir();
        
        if (Config.USE_CUSTOM_DOMAIN) {
            logger.debug(ltag, "Custom RAIDA Domain is set to: " + Config.CUSTOM_RAIDA_DOMAIN);
            
            raida.setUrlWithDomain(Config.CUSTOM_RAIDA_DOMAIN);
            //saveResults();
            //return;
        } else {
            raida.setDefaultUrls();
        }
        
        if (!doEchoReal()) {
            logger.info(ltag, "RAIDA Failed");
            
            globalResult.status = EchoResult.STATUS_ERROR;

            /*
            setRAIDAUrl(Config.BACKUP0_RAIDA_IP, Config.BACKUP0_RAIDA_PORT);
            if (!doEchoReal()) {
                logger.info(ltag, "Switching to the Backup1 RAIDA");

                setRAIDAUrl(Config.BACKUP1_RAIDA_IP, Config.BACKUP1_RAIDA_PORT);
                if (!doEchoReal()) {
                    logger.info(ltag, "Switching to the Backup2 RAIDA");

                    setRAIDAUrl(Config.BACKUP2_RAIDA_IP, Config.BACKUP2_RAIDA_PORT);
                    if (!doEchoReal()) {
                        logger.error(ltag, "All attempts failed. Giving up");
                    }
                }
            }
            */
        } else {
            globalResult.status = EchoResult.STATUS_FINISHED;
        }

        saveResults();
    }

    public boolean doEchoReal() {
        String[] results;
        String[] requests;

        int cntErr = 0;
        int i;
     
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
            requests[i] = "echo";

        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    EchoResult ar = new EchoResult();
                    copyFromGlobalResult(ar);
                    myCb.callback(ar);
                }
            }
        });
        if (results == null) {
            logger.error(ltag, "Failed to query echo");
            return false;
        }

        latencies = raida.getLastLatencies();

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "RAIDA " + i + ": " + results[i] + " latency=" + latencies[i]);
            if (results[i] == null) {
                logger.error(ltag, "Failed to get result. RAIDA " + i + " is not ready");
                cntErr++;
                continue;
            }

            logger.info(ltag, "parsing " + i);
            ers[i] = (EchoResponse) parseResponse(results[i], EchoResponse.class);
            if (ers[i] == null) {
                logger.error(ltag, "Failed to parse response");
                cntErr++;
                continue;
            }

            logger.info(ltag, "parsing2 " + ers[i]);
            if (!ers[i].status.equals(Config.RAIDA_STATUS_READY)) {
                logger.error(ltag, "RAIDA " + i + " is not ready");
                cntErr++;
                continue;
            }
        }

        if (cntErr == RAIDA.TOTAL_RAIDA_COUNT) {
            logger.debug(ltag, "Failed: " + cntErr);

            return false;
        }

        return true;
    }

    private boolean saveResults() {
        String status;

        long latency;
        String intLatency;
        String data;

        String[] raidaURLs;

        raidaURLs = raida.getRAIDAURLs();

        cleanLogDir();

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (ers[i] == null) {
                status = "notready";
                latency = 0;
                intLatency = "U";
            } else {
                status = ers[i].status;
                latency = latencies[i];
                intLatency = ers[i].message.replaceAll("Execution Time.*=\\s*(.+)", "$1");
            }

            String fileName = i + "_" + status + "_" + latency + "_" + intLatency + ".txt";

            data = "{\"url\":\"" + raidaURLs[i] + "\"}";

            if (!AppCore.saveFile(logDir + File.separator + fileName, data)) {
                logger.error(ltag, "Failed to write echoer logfile: " + fileName);
                continue;
            }

            logger.debug(ltag, "file " + fileName + " data " + data);
        }

        return true;
    }

}
