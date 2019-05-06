package global.cloudcoin.ccbank.Receiver;

import java.io.File;
import java.net.URLEncoder;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class Receiver extends Servant {
    String ltag = "Receiver";
    ReceiverResult rr;

    public Receiver(String rootDir, GLogger logger) {
        super("Receiver", rootDir, logger);
    }

    public void launch(String user, int tosn, int[] nns, int[] sns, String envelope, CallbackInterface icb) {
        this.cb = icb;

        final int ftosn = tosn;
        final String fuser = user;
        final int[] fsns = sns;
        final int[] fnns = nns;
        final String fenvelope = envelope;

        rr = new ReceiverResult();

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Receiver");
                doReceive(fuser, ftosn, fnns, fsns, fenvelope);


                if (cb != null)
                    cb.callback(rr);
            }
        });
    }

    public void doReceive(String user, int sn, int[] nns, int[] sns, String envelope) {
        CloudCoin cc;
        String[] results;
        Object[] o;
        CommonResponse errorResponse;
        ReceiverResponse[][] rrs;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;
        CloudCoin[] ccs;

        if (nns.length != sns.length) {
            logger.error(ltag, "Invalid parameters");
            rr.status = ReceiverResult.STATUS_ERROR;
            return;
        }


        /*
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            rr.status = ReceiverResult.STATUS_ERROR;
            return;
        }*/

        setSenderRAIDA();
        cc = getIDcc(user, sn);
        if (cc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            rr.status = ReceiverResult.STATUS_ERROR;
            return;
        }

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "receive";

            sbs[i] = new StringBuilder();
            sbs[i].append("sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());
            sbs[i].append("&envelope_name=");
            sbs[i].append(URLEncoder.encode(envelope));
            for (int j = 0; j < sns.length; j++) {
                sbs[i].append("&nns[]=");
                sbs[i].append(nns[j]);
                sbs[i].append("&sns[]=");
                sbs[i].append(sns[j]);
            }

            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts);
        if (results == null) {
            logger.error(ltag, "Failed to query receive");
            rr.status = ReceiverResult.STATUS_ERROR;
            return;
        }

        ccs = new CloudCoin[sns.length];
        rrs = new ReceiverResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], ReceiverResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }

            if (o.length != ccs.length) {
                logger.error(ltag, "RAIDA " + i + " wrong number of coins: " + o.length);
                continue;
            }

            for (int j = 0; j < o.length; j++) {
                String strStatus;
                int rnn, rsn;
                String ran;
                boolean found;

                rrs[i] = new ReceiverResponse[o.length];
                rrs[i][j] = (ReceiverResponse) o[j];

                strStatus = rrs[i][j].status;

                found = false;
                if (strStatus.equals("received")) {
                    rsn = rrs[i][j].sn;
                    ran = rrs[i][j].an;
                    rnn = rrs[i][j].nn;
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

                    logger.info(ltag, " sn=" + rsn + " nn=" + rnn + " an=" + ran);
                } else {
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                }

                logger.info(ltag, "raida" + i + " v=" + rrs[i][j].status + " m="+rrs[i][j].message);
            }
        }

        String dir = AppCore.getUserDir(Config.DIR_IMPORT, user);
        String file;
        for (i = 0; i < ccs.length; i++) {
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

        logger.info(ltag, "Receiving " + sn);
        rr.status = ReceiverResult.STATUS_FINISHED;
    }
}
