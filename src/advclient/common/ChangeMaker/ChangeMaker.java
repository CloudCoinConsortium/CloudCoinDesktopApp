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

    public void launch(int method, CloudCoin cc, CallbackInterface icb) {
        this.cb = icb;
        final int fmethod = method;
        final CloudCoin fcc = cc;
        
        cr = new ChangeMakerResult();
        
        csb = new StringBuilder();
        receiptId = AppCore.generateHex();
        cr.receiptId = receiptId;
        cr.errText = "";
        
        launchThread(new Runnable() {
            @Override
            public void run() {
            logger.info(ltag, "RUN ChangeMaker");
            doChange(fmethod, fcc);

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
    
    public void doChange(int method, CloudCoin cc) {
        logger.info(ltag, "Method " + method);

        String resultMain;
        String[] results;
        Object[] o;
        CommonResponse cresponse;
        ShowEnvelopeCoinsResponse[] srs;
        String[] requests;
        String[] posts;

        
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Failed to query RAIDA");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        Config.RAIDANUM_TO_QUERY_REQUEST_CHANGE = 7;
        String seed = AppCore.generateHex().substring(0, 8);       
        results = raida.query(new String[] { 
            "show_change?sn=" + Config.PUBLIC_CHANGE_MAKER_ID + "&seed=" + seed 
        }, null, null, new int[] { Config.RAIDANUM_TO_QUERY_REQUEST_CHANGE });
        if (results == null) {
            logger.error(ltag, "Failed to query showchange");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        resultMain = results[0];
        if (resultMain == null) {
            logger.error(ltag, "Failed to query showchange");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        ShowChangeResponse scr = (ShowChangeResponse) parseResponse(resultMain, ShowChangeResponse.class);
        if (scr == null) {
            logger.error(ltag, "Failed to get response");
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }
        
        if (!scr.status.equals(Config.REQUEST_STATUS_PASS)) {
            logger.error(ltag, "Failed to get response: " + scr.status);
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }
        
        int[] sns;
        int rqDenom = 0;

        switch (method) {
            case Config.CHANGE_METHOD_5A:
                sns = getA(scr.d1, 5);
                rqDenom = 5;
                break;
            case Config.CHANGE_METHOD_25A:
                sns = getA(scr.d5, 5);
                rqDenom = 25;
                break;
            case Config.CHANGE_METHOD_25B:
                sns = get25B(scr.d5, scr.d1);
                rqDenom = 25;
                break;
            case Config.CHANGE_METHOD_25C:
                sns = get25C(scr.d5, scr.d1);
                rqDenom = 25;
                break;
            case Config.CHANGE_METHOD_25D:
                sns = get25D(scr.d1);
                rqDenom = 25;
                break;
            case Config.CHANGE_METHOD_100A:
                sns = getA(scr.d25, 4);
                rqDenom = 100;
                break;
            case Config.CHANGE_METHOD_100B:
                sns = get100B(scr.d25, scr.d5);
                rqDenom = 100;
                break;
            case Config.CHANGE_METHOD_100C:
                sns = get100C(scr.d25, scr.d5, scr.d1);
                rqDenom = 100;
                break;
            case Config.CHANGE_METHOD_100D:
                sns = get100D(scr.d1);
                rqDenom = 100;
                break;
            case Config.CHANGE_METHOD_100E:
                sns = get100E(scr.d25, scr.d5, scr.d1);
                rqDenom = 100;
                break;    
            case Config.CHANGE_METHOD_250A:
                sns = get250A(scr.d100, scr.d25);
                rqDenom = 250;
                break;
            case Config.CHANGE_METHOD_250B:
                sns = get250B(scr.d25, scr.d5, scr.d1);
                rqDenom = 250;
                break;
            case Config.CHANGE_METHOD_250C:
                sns = get250C(scr.d25, scr.d5, scr.d1);
                rqDenom = 250;
                break;
            case Config.CHANGE_METHOD_250D:
                sns = get250D(scr.d1);
                rqDenom = 250;
                break;
            case Config.CHANGE_METHOD_250E:
                sns = get250E(scr.d100, scr.d25, scr.d5, scr.d1);
                rqDenom = 250;
                break;
            default:
                logger.error(ltag, "Invalid method: " + method);
                cr.status = ChangeMakerResult.STATUS_ERROR;
                return;
        }

        if (sns == null) {
            logger.info(ltag, "No coins");
            cr.errText = "Not enough coins at the Public Change Maker";
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;
        }

        for (int i = 0; i < sns.length; i++) {
            CloudCoin ccx = new CloudCoin(1, sns[i]);
            logger.info(ltag, "sn "+sns[i] + " d="+ccx.getDenomination());
        }

        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "change";
            posts[i] = "nn=" + cc.nn + "&sn=" + cc.sn + "&an=" + cc.ans[i] + "&denomination=" + cc.getDenomination();
            for (int j = 0; j < sns.length; j++) {
                posts[i] += "&sns[]=" + sns[j];
            }
        }

        results = raida.query(requests, posts, new CallbackInterface() {
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
            logger.info(ltag, "RAIDA " + i + ": " + results[i]);
            if (results[i] == null) {
                logger.error(ltag, "Failed to get result. RAIDA " + i);
                cntErr++;
                continue;
            }

            logger.info(ltag, "parsing " + i);
            crs[i] = (ChangeResponse) parseResponse(results[i], ChangeResponse.class);
            if (crs[i] == null) {
                logger.error(ltag, "Failed to parse response");
                cntErr++;
                continue;
            }

            logger.info(ltag, "parsing2 " + crs[i]);
            if (!crs[i].status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "RAIDA " + i + ": wrong response: " + crs[i].status);
                cntErr++;
                continue;
            }

            if (ccs.length != crs[i].sns.length || ccs.length != crs[i].ans.length) {
                logger.error(ltag, "RAIDA " + i + ": wrong response cnt: " + crs[i].sns.length + "/" + crs[i].ans.length + " need " + ccs.length);
                cntErr++;
                continue;
            }

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
                            break;
                        }
                    }

                    if (!found) {
                        logger.error(ltag, "Can't find a coin for rsn=" + rsn);
                        continue;
                    }
                }
            }
        }

        logger.debug(ltag, "Error count: " + cntErr);
        if (cntErr >= RAIDA.TOTAL_RAIDA_COUNT - Config.PASS_THRESHOLD) {
            logger.error(ltag, "Too many errors: " + cntErr);
            cr.errText = "Failed to get change. Too many errors from RAIDA: " + cntErr;
            cr.status = ChangeMakerResult.STATUS_ERROR;
            return;    
        }
        
        String dir = AppCore.getUserDir(Config.DIR_SUSPECT, user);
        String file;
        for (int i = 0; i < ccs.length; i++) {
            if (ccs[i] == null)
                continue;

            file = dir + File.separator + ccs[i].getFileName();
            logger.info(ltag, "Saving coin " + file);
            if (!AppCore.saveFile(file, ccs[i].getJson(false))) {
                logger.error(ltag, "Failed to move coin to Import: " + ccs[i].getFileName());
                continue;
            }
   
            logger.info(ltag, "cc="+ccs[i].sn + " v=" + ccs[i].getJson(false));
        }

        AppCore.moveToFolder(cc.originalFile, Config.DIR_SENT, user);

        addCoinToReceipt(cc, "authentic", "Sent to Public Change");
        saveReceipt(user, 1, 0, 0, 0, 0, 0);

        cr.status = ChangeMakerResult.STATUS_FINISHED;
    }

    private int[] getA(int[] a, int cnt) {
        int[] sns;
        int i, j;

        sns = new int[cnt];
        for (i = 0, j = 0; i < a.length; i++) {
            if (a[i] == 0)
                continue;

            sns[j] = a[i];
            a[i] = 0;
            j++;

            if (j == cnt)
                break;
        }

        if (j != cnt)
            return null;

        return sns;
    }

    private int[] get25B(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[9];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 5] = sns[i];

        return rsns;
    }

    private int[] get25C(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[17];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 5] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 10] = sns[i];

        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i + 15] = sns[i];

        return rsns;
    }

    private int[] get25D(int[] sb) {
        int[] sns, rsns;
        int j;

        rsns = new int[25];
        for (j = 0; j < 5; j++) {
            sns = getA(sb, 5);
            if (sns == null)
                return null;

            for (int i = 0; i < 5; i++)
                rsns[j * 5 + i] = sns[i];
        }

        return rsns;
    }

    private int[] get100B(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[8];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 3);
        if (sns == null)
            return null;

        for (int i = 0; i < 3; i++)
            rsns[i + 5] = sns[i];

        return rsns;
    }

    private int[] get100C(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[16];

        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 9] = sns[i];

        sns = getA(sb, 2);
        for (int i = 0; i < 2; i++)
            rsns[i + 14] = sns[i];

        return rsns;
    }

    private int[] get100D(int[] sb) {
        int[] sns, rsns;
        int j;

        rsns = new int[100];
        for (j = 0; j < 4; j++) {
            sns = get25D(sb);
            if (sns == null)
                return null;

            for (int i = 0; i < 25; i++)
                rsns[j * 25 + i] = sns[i];
        }

        return rsns;
    }
    
    private int[] get100E(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[12];

        sns = getA(sb, 3);
        if (sns == null)
            return null;

        for (int i = 0; i < 3; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 3] = sns[i];
        
        sns = getA(sss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 7] = sns[i];

        return rsns;
    }
    

    private int[] get250A(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[4];

        sns = getA(ss, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i + 2] = sns[i];

        return rsns;
    }

    private int[] get250B(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[22];
        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 9] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 14] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 18] = sns[i];

        return rsns;
    }

    private int[] get250C(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[42];
        sns = get25C(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 17; i++)
            rsns[i] = sns[i];

        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i + 17] = sns[i];

        sns = get100B(sb, ss);
        if (sns == null)
            return null;

        for (int i = 0; i < 8; i++)
            rsns[i + 26] = sns[i];

        sns = get100B(sb, ss);
        if (sns == null)
            return null;

        for (int i = 0; i < 8; i++)
            rsns[i + 34] = sns[i];

        return rsns;
    }

    private int[] get250D(int[] sb) {
        int[] sns, rsns;

        rsns = new int[250];

        sns = get25D(sb);
        if (sns == null)
           return null;

        for (int i = 0; i < 25; i++)
           rsns[i] = sns[i];

        sns = get25D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 25; i++)
            rsns[i + 25] = sns[i];


        sns = get100D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 100; i++)
            rsns[i + 50] = sns[i];

        sns = get100D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 100; i++)
            rsns[i + 150] = sns[i];

        return rsns;
    }
    
    private int[] get250E(int[] sb, int[] ss, int[] sss, int[] ssss) {
        int[] sns, rsns;

        rsns = new int[12];
        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 1);
        if (sns == null)
            return null;

        rsns[2] = sns[0];

        sns = getA(sss, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 3] = sns[i];

        sns = getA(ssss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 7] = sns[i];

        return rsns;
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