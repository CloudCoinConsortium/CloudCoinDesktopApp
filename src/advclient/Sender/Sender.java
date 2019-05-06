package global.cloudcoin.ccbank.Sender;

import org.json.JSONException;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;

import global.cloudcoin.ccbank.Authenticator.AuthenticatorResponse;
import global.cloudcoin.ccbank.Sender.SenderResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class Sender extends Servant {
    String ltag = "Sender";
    SenderResult sr;

    public Sender(String rootDir, GLogger logger) {
        super("Sender", rootDir, logger);
    }

    public void launch(int tosn, String dstFolder, int[] values, int amount, String envelope, CallbackInterface icb) {
        this.cb = icb;

        final int ftosn = tosn;
        final int[] fvalues = values;
        final String fenvelope = envelope;
        final int famount = amount;
        final String fdstFolder = dstFolder;

        sr = new SenderResult();
        sr.memo = envelope;

        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Sender");
                
                if (fdstFolder != null) {
                    doSendLocal(famount, fdstFolder);
                } else {
                    doSend(ftosn, null, famount, fenvelope);
                }


                if (cb != null)
                    cb.callback(sr);
            }
        });
    }

    public void doSendLocal(int amount, String dstUser) {
        logger.debug(ltag, "Sending " + amount + " to " + dstUser);
        
        String dstPath = AppCore.getUserDir(Config.DIR_BANK, dstUser);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);
        if (!pickCoinsAmountInDir(fullBankPath, amount)) {
            logger.debug(ltag, "Not enough coins in the bank dir for amount " + amount);
            sr.status = SenderResult.STATUS_ERROR;
            return;
        }
        
        for (CloudCoin cc : coinsPicked) {
            String ccFile = cc.originalFile;
        
            if (!AppCore.moveToFolder(cc.originalFile, Config.DIR_BANK, dstUser)) {
                logger.error(ltag, "Failed to move coin " + cc.originalFile);
                sr.status = SenderResult.STATUS_ERROR;
                continue;
            }
            
            sr.amount += cc.getDenomination();
        }
        
        if (sr.status != SenderResult.STATUS_ERROR)
            sr.status = SenderResult.STATUS_FINISHED;
        
        System.out.println("ss="+dstUser+ " dstpath="+dstPath);
    }
    
    public void doSend(int tosn, int[] values, int amount, String envelope) {
        /*
        if (!updateRAIDAStatus()) {
            sr.status = SenderResult.STATUS_ERROR;
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            return;
        }*/

        String fullSentPath = AppCore.getUserDir(Config.DIR_SENT, user);
        String fullFrackedPath = AppCore.getUserDir(Config.DIR_FRACKED, user);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);

        if (values != null) {
            if (values.length != AppCore.getDenominations().length) {
                logger.error(ltag, "Invalid params");
                sr.status = SenderResult.STATUS_ERROR;
                return;
            }

            if (!pickCoinsInDir(fullBankPath, values)) {
                logger.debug(ltag, "Not enough coins in the bank dir");
                if (!pickCoinsInDir(fullFrackedPath, values)) {
                   logger.error(ltag, "Not enough coins in the Fracked dir");
                   sr.status = SenderResult.STATUS_ERROR;
                   return;
                }
            }
        } else {
            if (!pickCoinsAmountInDir(fullBankPath, amount)) {
                logger.debug(ltag, "Not enough coins in the bank dir for amount " + amount);
                sr.status = SenderResult.STATUS_ERROR;
                return;
            }
        }

        setSenderRAIDA();

        logger.debug(ltag, "Sending to SN " + tosn);
        if (!processSend(coinsPicked, tosn, envelope)) {
            sr.status = SenderResult.STATUS_ERROR;
            return;
        }

        sr.status = SenderResult.STATUS_FINISHED;
    }

    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }

    private void moveCoins(ArrayList<CloudCoin> ccs) {
        int passed, failed;

        for (CloudCoin cc : ccs) {
            String ccFile = cc.originalFile;
            passed = failed = 0;
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                    passed++;
                else if (cc.getDetectStatus(i) == CloudCoin.STATUS_FAIL)
                    failed++;
            }

            logger.info(ltag, "Doing " + cc.originalFile + " pass="+passed + " f="+failed);
            if (passed >= Config.PASS_THRESHOLD) {
                logger.info(ltag, "Moving to Sent: " + cc.sn);
                sr.amount += cc.getDenomination();
                AppCore.moveToFolder(cc.originalFile, Config.DIR_SENT, user);
            } else if (failed > 0) {
                if (failed >= RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
                    logger.info(ltag, "Moving to Counterfeit: " + cc.sn);
                    AppCore.moveToFolder(cc.originalFile, Config.DIR_COUNTERFEIT, user);
                } else {
                    logger.info(ltag, "Moving to Fracked: " + cc.sn);
                    AppCore.moveToFolder(cc.originalFile, Config.DIR_FRACKED, user);
                }
            }
        }
    }


    public boolean processSend(ArrayList<CloudCoin> ccs, int tosn, String envelope) {
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;

        int i;
        boolean first = true;

        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "send";
            sbs[i] = new StringBuilder();
        }

        for (CloudCoin cc : ccs) {
            logger.debug(ltag, "Processing coin " + cc.sn);
            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                if (!first) {
                    sbs[i].append("&");
                } else {
                    sbs[i].append("to_sn=");
                    sbs[i].append(tosn);
                    sbs[i].append("&tag=");
                    sbs[i].append(URLEncoder.encode(envelope));
                    sbs[i].append("&");
                }

                sbs[i].append("nns[]=");
                sbs[i].append(cc.nn);

                sbs[i].append("&sns[]=");
                sbs[i].append(cc.sn);

                sbs[i].append("&denomination[]=");
                sbs[i].append(cc.getDenomination());

                sbs[i].append("&ans[]=");
                sbs[i].append(cc.ans[i]);
            }

            first = false;
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                //globalResult.totalRAIDAProcessed++;
                //if (myCb != null) {
                //    AuthenticatorResult ar = new AuthenticatorResult();
                //    copyFromGlobalResult(ar);
                //    myCb.callback(ar);
                //}
            }
        });

        if (results == null) {
            logger.error(ltag, "Failed to query send");
            return false;
        }

        CommonResponse errorResponse;
        SenderResponse[][] ar;
        Object[] o;

        ar = new SenderResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.debug(ltag, "Parsing result from RAIDA" + i + " r: " + results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], SenderResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }

            for (int j = 0; j < o.length; j++) {
                String strStatus;
                int status;

                ar[i] = new SenderResponse[o.length];
                ar[i][j] = (SenderResponse) o[j];

                strStatus = ar[i][j].status;

                if (strStatus.equals("pass")) {
                    status = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals("fail")) {
                    status = CloudCoin.STATUS_FAIL;
                } else {
                    status = CloudCoin.STATUS_ERROR;
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                ccs.get(j).setDetectStatus(i, status);
                logger.info(ltag, "raida" + i + " v=" + ar[i][j].status + " m="+ar[i][j].message);
            }
        }

        moveCoins(ccs);

        return true;
    }


}
