package global.cloudcoin.ccbank.Grader;


import org.json.JSONException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.util.ArrayList;

public class Grader extends Servant {
    String ltag = "Grader";
    GraderResult gr;

    public Grader(String rootDir, GLogger logger) {
        super("Grader", rootDir, logger);
    }

    public void launch(CallbackInterface icb, ArrayList<CloudCoin> duplicates, String source) {
        this.cb = icb;

        gr = new GraderResult();
        csb = new StringBuilder();

        receiptId = AppCore.generateHex();
        gr.receiptId = receiptId;
        
        final String fsource = source;
        
        final ArrayList<CloudCoin> fduplicates = duplicates;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Grader");
                doGrade(fduplicates, fsource);

                if (cb != null)
                    cb.callback(gr);
            }
        });
    }

    public void doGrade(ArrayList<CloudCoin> duplicates, String source) {
        String fullPath = AppCore.getUserDir(Config.DIR_DETECTED, user);
        CloudCoin cc;
        boolean graded = false;

        File dirObj = new File(fullPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            graded = true;
            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                gr.totalUnchecked++;
                continue;
            }

            gradeCC(cc, source);
        }
        
        int dups = 0;
        if (duplicates != null && duplicates.size() != 0) {
            graded = true;
            dups = duplicates.size();
            for (CloudCoin dcc : duplicates) {
                for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++)
                    dcc.setDetectStatus(i, CloudCoin.STATUS_UNTRIED);
                
                dcc.setPownStringFromDetectStatus();
                
                addCoinToReceipt(dcc, "Duplicate (The Coin has already been deposited)", Config.DIR_TRASH);
                logger.info(ltag, "Removing dup coin: " + dcc.sn);
                
                String ccFile = AppCore.getUserDir(Config.DIR_TRASH, user) + File.separator + dcc.getFileName();
                if (!AppCore.saveFile(ccFile, dcc.getJson(false))) {
                    logger.error(ltag, "Failed to save file: " + ccFile);
                    continue;
                }
                
                AppCore.deleteFile(dcc.originalFile);
            }
        }

        if (graded) {
            saveReceipt(user, gr.totalAuthentic, gr.totalCounterfeit, gr.totalFracked,
                gr.totalLost, gr.totalUnchecked, dups);
        }
    }

    public void gradeCC(CloudCoin cc, String fsource) {
        String dstFolder;
        int untried, counterfeit, passed, error;

        untried = counterfeit = passed = error = 0;
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            switch (cc.getDetectStatus(i)) {
                case CloudCoin.STATUS_ERROR:
                    error++;
                    break;
                case CloudCoin.STATUS_FAIL:
                    counterfeit++;
                    break;
                case CloudCoin.STATUS_UNTRIED:
                    untried++;
                    break;
                case CloudCoin.STATUS_PASS:
                    passed++;
                    break;
            }
        }

        boolean includePans = false;
        String ccFile;

        String dst = "";
        if (passed >= Config.PASS_THRESHOLD) {
            if (counterfeit != 0) {
                logger.debug(ltag, "Coin " + cc.sn + " is fracked");

                gr.totalFracked++;
                gr.totalFrackedValue += cc.getDenomination();
                dst = dstFolder = Config.DIR_FRACKED;
                if (fsource != null) 
                    dst += " from " + fsource;
        
                addCoinToReceipt(cc, "fracked", dst);
            } else {
                logger.debug(ltag, "Coin " + cc.sn + " is authentic");

                gr.totalAuthentic++;
                gr.totalAuthenticValue += cc.getDenomination();
                dst = dstFolder = Config.DIR_BANK;
                if (fsource != null) 
                    dst += " from " + fsource;

                addCoinToReceipt(cc, "authentic", dst);
            }

            cc.calcExpirationDate();

            ccFile = AppCore.getUserDir(dstFolder, user) + File.separator + cc.getFileName();
        } else {
            if (passed + counterfeit > Config.PASS_THRESHOLD) {
                logger.debug(ltag, "Coin " + cc.sn + " is counterfeit");

                gr.totalCounterfeit++;
                gr.totalCounterfeitValue += cc.getDenomination();
                dst = dstFolder = Config.DIR_COUNTERFEIT;
                if (fsource != null) 
                    dst += " from " + fsource;

                addCoinToReceipt(cc, "counterfeit", dst);
            } else {
                logger.debug(ltag, "Coin " + cc.sn + " is lost");

                gr.totalLost++;
                dst = dstFolder = Config.DIR_LOST;
                if (fsource != null) 
                    dst += " from " + fsource;

                gr.totalLostValue += cc.getDenomination();
                addCoinToReceipt(cc, "lost", dst);
                includePans = true;
            }

            ccFile = AppCore.getUserDir(dstFolder, user) + File.separator +
                + System.currentTimeMillis() + "-" + cc.getFileName();
        }

        cc.setAnsToPansIfPassed();

        logger.info(ltag, "Saving grader coin: " + ccFile + " include=" + includePans);
        if (!AppCore.saveFile(ccFile, cc.getJson(includePans))) {
            logger.error(ltag, "Failed to save file: " + ccFile);
            return;
        }

        AppCore.deleteFile(cc.originalFile);
    }
    
    
    
    
}
