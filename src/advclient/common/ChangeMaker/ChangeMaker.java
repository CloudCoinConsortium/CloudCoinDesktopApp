package global.cloudcoin.ccbank.ChangeMaker;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;

import global.cloudcoin.ccbank.ChangeMaker.ChangeMakerResult;
import global.cloudcoin.ccbank.Echoer.EchoResponse;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoins;
import global.cloudcoin.ccbank.ShowEnvelopeCoins.ShowEnvelopeCoinsResponse;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class ChangeMaker extends Servant {
    String ltag = "ChangeMaker";
    ChangeMakerResult cr;


    public ChangeMaker(String rootDir, GLogger logger) {
        super("ChangeMaker", rootDir, logger);
    }

    public void launch(int method, CloudCoin cc, String email, CallbackInterface icb) {
        this.cb = icb;
        final int fmethod = method;
        final CloudCoin fcc = cc;
        final String femail = email;
        
        cr = new ChangeMakerResult();
        
        csb = new StringBuilder();
        receiptId = AppCore.generateHex();
        cr.receiptId = receiptId;
        cr.errText = "";
        
        launchThread(new Runnable() {
            @Override
            public void run() {
            logger.info(ltag, "RUN ChangeMaker");
            doChange(fmethod, femail, fcc);

            if (cb != null)
                cb.callback(cr);
            }
        });
    }

    private void copyFromGlobalResult(ChangeMakerResult cmr) {
        cmr.totalRAIDAProcessed = cr.totalRAIDAProcessed;
        cmr.status = cr.status;
        cmr.errText = cr.errText;
        cmr.receiptId = cr.receiptId;
    }
    
    public void setCoinsStatus(CloudCoin[] ccs, int raidaIdx, int status) {
        logger.debug(ltag, "Setting status: " + status);
        for (int i = 0; i < ccs.length; i++)
            ccs[i].setDetectStatus(raidaIdx, status);
    }
    
    public void doChange(int method, String email, CloudCoin cc) {
        logger.info(ltag, "Method " + method);

        String[] results;
        String[] requests;
        String[] posts;
       
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Failed to query RAIDA");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        int sns[] = showChange(method, cc);
        if (sns == null) {
            cr.errText = "Not enough coins at the Public Change Maker";
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        //for (int i =0; i < sns.length; i++) {
        //    System.out.println("xsn="+sns[i]);
       // }
        
        
        //System.exit(1);
        CloudCoin[] chccs = new CloudCoin[sns.length];
        for (int i = 0; i < sns.length; i++) {
            CloudCoin ccx = new CloudCoin(1, sns[i]);
            ccx.createAns(email);
            chccs[i] = ccx;
            logger.info(ltag, "sn "+ sns[i] + " d="+ccx.getDenomination());
            
            //System.out.println("coin=" + sns[i] + " pans:");
            //for (int x=0;x<25;x++)
            //    System.out.println("->" + chccs[i].ans[x]);
        }

        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "break?nn=" + cc.nn + "&sn=" + cc.sn + "&an=" + cc.ans[i] 
                    + "&dn=" + cc.getDenomination() + "&change_server=" + Config.PUBLIC_CHANGE_MAKER_ID;
            for (int j = 0; j < sns.length; j++) {
                requests[i] += "&csn[]=" + sns[j];
            }
            for (int j = 0; j < sns.length; j++) {
                requests[i] += "&cpan[]=" + chccs[j].ans[i];
            }
        }

        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                cr.totalRAIDAProcessed++;
                cr.status = ChangeMakerResult.STATUS_PROCESSING;
                if (myCb != null) {
                    ChangeMakerResult cmr = new ChangeMakerResult();
                    copyFromGlobalResult(cmr);
                    myCb.callback(cmr);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query change");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }
        
        int cntErr = 0;
        ChangeResponse[] crs = new ChangeResponse[RAIDA.TOTAL_RAIDA_COUNT];
        CloudCoin[] ccs = new CloudCoin[sns.length];

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "parsing " + i);
            //System.out.println("r=" + i + " res=" + results[i].trim());
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinsStatus(chccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + i);
                    setCoinsStatus(chccs, i, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
   
            if (results[i] == null) {
                logger.error(ltag, "Failed to get result. RAIDA " + i);
                setCoinsStatus(chccs, i, CloudCoin.STATUS_NORESPONSE);
                cntErr++;
                continue;
            }
            
            logger.info(ltag, "RAIDA " + i + ": " + results[i].trim());


            crs[i] = (ChangeResponse) parseResponse(results[i].trim(), ChangeResponse.class);
            if (crs[i] == null) {
                logger.error(ltag, "Failed to parse response");
                setCoinsStatus(chccs, i, CloudCoin.STATUS_ERROR);
                cntErr++;
                continue;
            }

            logger.info(ltag, "parsing2 " + crs[i]);
            if (crs[i].status.equals("error")) {
                logger.error(ltag, "RAIDA " + i + ": error response: " + crs[i].status);
                setCoinsStatus(chccs, i, CloudCoin.STATUS_ERROR);
                cntErr++;
                continue;
            }
            
            if (crs[i].status.equals("fail")) {
                logger.error(ltag, "RAIDA " + i + ": counterfeit response: " + crs[i].status);
                setCoinsStatus(chccs, i, CloudCoin.STATUS_FAIL);
                cntErr++;
                continue;
            }
            
            //if (!crs[i].status.equals(Config.REQUEST_STATUS_PASS)) {
            if (!crs[i].status.equals("success")) {
                logger.error(ltag, "RAIDA " + i + ": wrong response: " + crs[i].status);
                setCoinsStatus(chccs, i, CloudCoin.STATUS_ERROR);
                cntErr++;
                continue;
            }

            //System.out.println("PASS " + i);
            setCoinsStatus(chccs, i, CloudCoin.STATUS_PASS);

            /*
            if (ccs.length != crs[i].sns.length || ccs.length != crs[i].ans.length) {
                logger.error(ltag, "RAIDA " + i + ": wrong response cnt: " + crs[i].sns.length + "/" + crs[i].ans.length + " need " + ccs.length);
                cntErr++;
                continue;
            }
            */

            /*
            for (int j = 0; j < crs[i].sns.length; j++) {
                int rnn, rsn;
                String ran;
                boolean found;

                rsn = crs[i].sns[j];
                ran = crs[i].ans[j];
                rnn = crs[i].nn;

                logger.info(ltag, " sn="+ rsn + " nn="+rnn+ " an="+ran);
                found = false;

                for (int k = 0; k < ccs.length; k++) {
                    if (ccs[k] == null)
                        continue;

                    if (ccs[k].sn == rsn) {
                        ccs[k].ans[i] = ran;
                        ccs[k].setDetectStatus(i, CloudCoin.STATUS_PASS);
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    for (int k = 0; k < ccs.length; k++) {
                        if (ccs[k] == null) {
                            found = true;
                            ccs[k] = new CloudCoin(rnn, rsn);
                            ccs[k].ans[i] = ran;
                            ccs[k].setDetectStatus(i, CloudCoin.STATUS_PASS);
                            
                            break;
                        }
                    }

                    if (!found) {
                        logger.error(ltag, "Can't find a coin for rsn=" + rsn);
                        continue;
                    }
                }
            }
            */
        }

        logger.debug(ltag, "Error count: " + cntErr);
        /*
        if (cntErr >= RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
            logger.error(ltag, "Too many errors: " + cntErr);
            cr.errText = "Failed to get change. Too many errors from RAIDA: " + cntErr;
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;    
        }
        */
        //String dir = AppCore.getUserDir(Config.DIR_SUSPECT, user);
        String dir = AppCore.getUserDir(Config.DIR_DETECTED, user);
        String file;
        boolean isError = false;
        for (int i = 0; i < chccs.length; i++) {
//            if (ccs[i] == null)
  //              continue;

            chccs[i].setPansToAns();
            chccs[i].setPownStringFromDetectStatus();
            file = dir + File.separator + chccs[i].getFileName();  
            
            logger.info(ltag, "Saving coin " + file + " p=" + chccs[i].getPownString());
            
            if (!chccs[i].isSentFixable()) {
                logger.error(ltag, "Too many errors for coin: " + chccs[i].sn + ". Will skip it");
                isError = true;
                continue;
            }

            if (!AppCore.saveFile(file, chccs[i].getJson())) {
                logger.error(ltag, "Failed to move coin to Import: " + chccs[i].getFileName());
                continue;
            }
   
            //logger.info(ltag, "cc="+ccs[i].sn + " v=" + ccs[i].getJson(false));
        }

        AppCore.moveToFolderNoTs(cc.originalFile, Config.DIR_SENT, user, true);
        
        if (isError) {
            logger.error(ltag, "Error occured during making change");
            cr.errText = "Failed to get change. Too many errors from RAIDA servers";
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;    
        }

        

        addCoinToReceipt(cc, "authentic", "Sent to Public Change");
        saveReceipt(user, 1, 0, 0, 0, 0, 0, cc.getDenomination());

        cr.status = ChangeMakerResult.STATUS_FINISHED;
    }

}

/*
Method 5A: 1,1,1,1,1
Denomination 25

Method 25A: 5,5,5,5,5 (Min)
Method 25B: 5,5,5,5,5A
Method 25C: 5,5,5A,5A,5A
Method 25D: 5A,5A,5A,5A (Max)
Denomination 100

Method 100A: 25,25,25,25 (min)
Method 100B: 25,25,25,25A
Method 100C: 25,25,25A,25B
Method 100D: 25D,25D,25D,25D (Max)
Method 100E: 25,25,25,25B
Denomination 250

Method 250A: 100,100,25,25 (Min)
Method 250B: 100A,100A,25A,25B
Method 250C: 100B,100B,25B,25C
Method 250D: 100D,100D,25D,25D (Max)
*/