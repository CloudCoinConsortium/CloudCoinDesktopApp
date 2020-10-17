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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Receiver extends Servant {
    String ltag = "Receiver";
    ReceiverResult rr;
    ReceiverResult globalResult;
    
    int a, c, e, f, av;
    boolean isSkyWithdraw;
    ArrayList<String> ff;

    public Receiver(String rootDir, GLogger logger) {
        super("Receiver", rootDir, logger);
    }

    //public void launch(String user, int tosn, int[] nns, int[] sns, String envelope, CallbackInterface icb) {
    public void launch(int fromsn, int[] sns, String dstFolder, int amount, boolean needReceipt, String rn, CallbackInterface icb) {
        this.cb = icb;

        final int ffromsn = fromsn;
        final int[] fsns = sns;
        final String fdstFolder = dstFolder;
        final int famount = amount;
        final boolean fneedReceipt = needReceipt;

        isSkyWithdraw = false;
        if (dstFolder.equals("SkyWithdraw")) {
            isSkyWithdraw = true;
            needReceipt = false;
        }
        
        rr = new ReceiverResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];
        globalResult = new ReceiverResult();
        
        csb = new StringBuilder();
        
        initRarr();
        
        receiptId = AppCore.generateHex();
        if (rn != null)
            receiptId = rn;
        
        globalResult.receiptId = receiptId;

        ff = new ArrayList<String>();
        a = c = e = f = 0;
        av = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Receiver");
                doReceive(ffromsn, fsns, fdstFolder, famount, fneedReceipt);
            }
        });
    }
    
    public void launch(int fromsn, ArrayList<String> files, String dir, CallbackInterface icb) {
        this.cb = icb;

        final int ffromsn = fromsn;
        final ArrayList<String> ffiles = files;
        final String fdir = dir;

        rr = new ReceiverResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];
        globalResult = new ReceiverResult();
        
        csb = new StringBuilder();
        receiptId = AppCore.generateHex();
        globalResult.receiptId = receiptId;

        ff = new ArrayList<String>();
        a = c = e = f = 0;
        av = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Receiver for Partial");
                doReceivePartial(ffromsn, fdir, ffiles);
            }
        });
    }
    
    
    public void launch(int fromsn, String memo, CallbackInterface icb) {
        this.cb = icb;

        final int ffromsn = fromsn;
        final String fmemo = memo;


        isSkyWithdraw = true;
 
        rr = new ReceiverResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];
        globalResult = new ReceiverResult();
        
        csb = new StringBuilder();
        
        initRarr();

        ff = new ArrayList<String>();
        a = c = e = f = 0;
        av = 0;
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Receiver (Envelope)");
                doReceiveEnvelope(ffromsn, fmemo);
            }
        });
    }
    
    private void copyFromGlobalResult(ReceiverResult rResult) {
        rResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        rResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        rResult.totalCoins = globalResult.totalCoins;
        rResult.totalCoinsProcessed = globalResult.totalCoinsProcessed;
        rResult.totalFiles = globalResult.totalFiles;
        rResult.status = globalResult.status;
        rResult.amount = globalResult.amount;
        rResult.errText = globalResult.errText;
        rResult.receiptId = globalResult.receiptId;
        rResult.needExtra = globalResult.needExtra;
        rResult.step = globalResult.step;
    }
    
    public void doReceivePartial(int sn, String dir, ArrayList<String> files) {
        int total = 0;
        ArrayList<CloudCoin> allccs = new ArrayList<CloudCoin>();
        for (String f : files) {
            logger.debug(ltag, "Will try to fix " + f);
            CloudCoin cc = new CloudCoin(f);
            
            //System.out.println("receiving " + f + " cc="+cc.sn + " p="+cc.getPownString() + " st="+cc.getDetectStatus(0)+  " st="+cc.getDetectStatus(14) + " d="+dir);
            total += cc.getDenomination();
            allccs.add(cc);
        }
        CloudCoin idcc = getIDcc(sn);
        if (idcc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            rr.status = ReceiverResult.STATUS_ERROR;
            rr.errText = "Failed to find coin ID";
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }
        
        globalResult.step = 1;        
        globalResult.totalFiles = files.size();
        globalResult.totalCoins = total;
        globalResult.totalRAIDAProcessed = 0;
        globalResult.totalFilesProcessed = 0;
        globalResult.totalCoinsProcessed = 0;
        
        
        rr = new ReceiverResult();
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);
        
        
        ArrayList<CloudCoin> ccs = new ArrayList<CloudCoin>();
        int curValProcessed = 0;
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        logger.debug(ltag, "Maxcoins: " + maxCoins);
        for (CloudCoin cc : allccs) {
            logger.debug(ltag, "Receiving from SN " + sn);         
            
            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");
                rr = new ReceiverResult();
                
                if (!processReceivePartial(ccs, dir, idcc)) {
                    rr = new ReceiverResult();
                    globalResult.status = ReceiverResult.STATUS_ERROR;
                    copyFromGlobalResult(rr);
                    if (cb != null)
                        cb.callback(rr);
                
                    return;
                }
          
                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;
                globalResult.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);         
            }  
        }
        
        rr = new ReceiverResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processReceivePartial(ccs, dir, idcc)) {
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_ERROR;
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);
                
               return;
            } else {
                globalResult.totalFilesProcessed += ccs.size();
            }
        }
        
        
        
        
        globalResult.status = ReceiverResult.STATUS_FINISHED;      
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);
        
    }

    
     public boolean processReceivePartial(ArrayList<CloudCoin> ccs, String dir, CloudCoin cc)  {
        String[] results;
        Object o;
        CommonResponse errorResponse;
        ReceiverResponse[][] rrs;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;

        
        String pang = AppCore.generateHex().toLowerCase();
        logger.debug(ltag, "Generated pang " + pang);

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "receive";

            sbs[i] = new StringBuilder();
            sbs[i].append("b=t");
            sbs[i].append("&nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&dn=");
            sbs[i].append(cc.getDenomination());

            for (CloudCoin tcc : ccs) {
                sbs[i].append("&sns[]=");
                sbs[i].append(tcc.sn);
            }

            sbs[i].append("&pang=");
            sbs[i].append(pang);
            
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    ReceiverResult rrlocal = new ReceiverResult();
                    copyFromGlobalResult(rrlocal);
                    myCb.callback(rrlocal);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query receive");
            return false;
        }

        rrs = new ReceiverResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("") || results[i].equals("E")) {
                    logger.error(ltag, "Skipped raida or error raida" + i);
                    continue;
                } 
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                continue;
            }

            o = parseResponse(results[i], ReceiverResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }
            
            ReceiverResponse ars = (ReceiverResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("received")) {
                logger.debug(ltag, "received");
                setCoinStatus(ccs, i, CloudCoin.STATUS_PASS);
                for (CloudCoin tcc : ccs) {
                    String seed = "" + i + "" + tcc.sn + "" + pang;
                    String pan = AppCore.getMD5(seed);
                    logger.debug(ltag, "sn " + tcc.sn + " seed " + seed + " pan " + pan);
                    tcc.ans[i] = pan;
                }
                continue;
            } else if (ars.status.equals("fail")) {
                logger.debug(ltag, "fail");
                continue;
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                continue;
            }

        }
        
        String file;        
        for (CloudCoin tcc : ccs) {
            tcc.setPownStringFromDetectStatus();
            logger.debug(ltag, "cc " + cc.sn + " pown " + tcc.getPownString());
                                    
            if (!tcc.isSentFixable()) {
                logger.debug(ltag, "Coin SN " + cc.sn + " isn't sent fixable. Keep it in the Partial folder");               
                c++;
                continue;
            }
            
            file = dir + File.separator + tcc.getFileName();
            logger.info(ltag, "Saving coin " + file);
            File f = new File(file);
            if (f.exists())
                f.delete();
            
            if (!AppCore.saveFile(file, tcc.getJson(false))) {
                logger.error(ltag, "Failed to move coin to Detected: " + tcc.getFileName());
                e++;
                continue;
            }
            
            globalResult.coinsFixed += tcc.getDenomination();
            AppCore.deleteFile(tcc.originalFile);

            
            logger.info(ltag, "cc=" + tcc.sn + " v=" + tcc.getJson(false));
            globalResult.amount += tcc.getDenomination();
            a++;
            av += tcc.getDenomination();
        }

        

        logger.info(ltag, "Received " + cc.sn);

        return true;
    }

    public void doReceive(int sn, int[] sns, String fdstFolder, int amount, boolean needReceipt) {
        int i;

        globalResult.needExtra = false;
        
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            globalResult.status = ReceiverResult.STATUS_ERROR;
            globalResult.errText = AppCore.raidaErrText;
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }

        CloudCoin extraCoin = null;
        if (amount == 0) {
            logger.debug(ltag, "Amount is zero. Special casse. Downloading everything");
            
            for (int ii = 0; ii < sns.length; ii++) {
                CloudCoin xcc = new CloudCoin(Config.DEFAULT_NN, sns[ii]);
                amount += xcc.getDenomination();
            }

            logger.debug(ltag, "New amount " + amount);
        } 
        
        if (!pickCoinsAmountFromArray(sns, amount)) {
            logger.debug(ltag, "Not enough coins in the cloudfor amount " + amount + ". Picking extra");
            
            coinsPicked = new ArrayList<CloudCoin>();
            extraCoin = pickCoinsAmountFromArrayWithExtra(sns, amount);
            if (extraCoin == null) {
                globalResult.status = ReceiverResult.STATUS_ERROR;
                globalResult.errText = "Failed to find coins in the Sky Wallet";
                //globalResult.errText = Config.PICK_ERROR_MSG;
                rr = new ReceiverResult();
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);          
                return;
            }
            
            //globalResult.needExtra = true;
            logger.debug(ltag, "Got extra coin " + extraCoin.sn + " denomination: " + extraCoin.getDenomination());
        }
        
        CloudCoin idcc = getIDcc(sn);
        if (idcc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            rr.status = ReceiverResult.STATUS_ERROR;
            rr.errText = "Failed to find coin ID";
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }
        
        if (extraCoin != null) {
            logger.debug(ltag, "Need change");
            int[] csns = breakInBank(extraCoin, idcc, new CallbackInterface() {
                final GLogger gl = logger;
                final CallbackInterface myCb = cb;

                @Override
                public void callback(Object result) {
                    globalResult.step = 0;
                    globalResult.totalRAIDAProcessed++;
                    if (myCb != null) {
                        ReceiverResult trlocal = new ReceiverResult();
                        copyFromGlobalResult(trlocal);
                        myCb.callback(trlocal);
                    }
                }
            });
            
            if (csns == null) {
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_ERROR;
                globalResult.errText = "Failed to break coin in Bank";
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);
                
                return; 
                
            }
            
            int[] nsns = new int[sns.length + csns.length];
            for (int j = 0; j < sns.length; j++) {
                logger.debug(ltag, "Existing coins " + sns[j]);
                nsns[j] = sns[j];              
            }
            
            for (int j = 0; j < csns.length; j++) {
                logger.debug(ltag, "Coins from Break " + csns[j]);
                nsns[j + sns.length] = csns[j];
            }

            coinsPicked = new ArrayList<CloudCoin>();
            if (!pickCoinsAmountFromArray(nsns, amount)) {
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_ERROR;
                globalResult.errText = "Failed to collect coins after breaking change";
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);
                
                return;  
            }
        }

        globalResult.step = 1;
        globalResult.totalRAIDAProcessed = 0;
        
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        logger.debug(ltag, "Maxcoins: " + maxCoins);
        
        globalResult.totalFiles = coinsPicked.size();
        if (extraCoin != null)
            globalResult.totalFiles++;
        
        globalResult.totalRAIDAProcessed = 0;
        globalResult.totalFilesProcessed = 0;
        globalResult.totalCoinsProcessed = 0;
        
        
        rr = new ReceiverResult();
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);
        
        
        logger.info(ltag, "total files "+ globalResult.totalFiles);
        
        for (CloudCoin cc : coinsPicked) {
            globalResult.totalCoins += cc.getDenomination();
        }
        globalResult.totalCoins = amount;
        
        int curValProcessed = 0;      
        for (CloudCoin cc : coinsPicked) {
            logger.debug(ltag, "Receiving from SN " + sn);         
            if (isCancelled()) {
                logger.info(ltag, "Cancelled");
                resume();
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_CANCELLED;
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);

                return;
            }
            
            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");
                rr = new ReceiverResult();
                
                if (!processReceive(ccs, idcc, needReceipt, fdstFolder)) {
                   rr = new ReceiverResult();
                   globalResult.status = ReceiverResult.STATUS_ERROR;
                   copyFromGlobalResult(rr);
                   if (cb != null)
                        cb.callback(rr);
                
                   return;
                }
          
                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;
                globalResult.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);         
            }  
        }
        
        rr = new ReceiverResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processReceive(ccs, idcc, needReceipt, fdstFolder)) {
                rr = new ReceiverResult();
                globalResult.status = ReceiverResult.STATUS_ERROR;
                copyFromGlobalResult(rr);
                if (cb != null)
                    cb.callback(rr);
                
                return;
            } else {
                //globalResult.status = ReceiverResult.STATUS_FINISHED;
                globalResult.totalFilesProcessed += ccs.size();
            }
        }

        globalResult.status = ReceiverResult.STATUS_FINISHED;      
        if (!isSkyWithdraw)
            saveReceipt(user, a, c, 0, 0, e, 0, av);      
        
        
        rr.files = new String[ff.size()];
        for (i = 0; i < ff.size(); i++) {
            rr.files[i] = ff.get(i);
        }
        
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);
        
        fixTransfer(rarr);
    } 
        
        
    public boolean processReceive(ArrayList<CloudCoin> ccs, CloudCoin cc, boolean needReceipt, String fdstFolder)  {
        String[] results;
        Object o;
        CommonResponse errorResponse;
        ReceiverResponse[][] rrs;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;

        

        String pang = AppCore.generateHex().toLowerCase();
        
        logger.debug(ltag, "Generated pang " + pang);

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "receive";

            sbs[i] = new StringBuilder();
            sbs[i].append("b=t");
            sbs[i].append("&nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&pan=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&dn=");
            sbs[i].append(cc.getDenomination());

            for (CloudCoin tcc : ccs) {
                sbs[i].append("&sns[]=");
                sbs[i].append(tcc.sn);
            }

            sbs[i].append("&pang=");
            sbs[i].append(pang);
            
            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    ReceiverResult rrlocal = new ReceiverResult();
                    copyFromGlobalResult(rrlocal);
                    myCb.callback(rrlocal);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query receive");
            return false;
        }

        rrs = new ReceiverResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                setCoinStatus(ccs, i, CloudCoin.STATUS_NORESPONSE);
                continue;
            }

            o = parseResponse(results[i], ReceiverResponse.class);
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
            
            ReceiverResponse ars = (ReceiverResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("received")) {
                logger.debug(ltag, "received");
                setCoinStatus(ccs, i, CloudCoin.STATUS_PASS);

                /*
                if (i==14 || i==15 || i==16||i==17||i==18||i==19)
                    setCoinStatus(ccs, i, CloudCoin.STATUS_NORESPONSE);
                */
                continue;
            } else if (ars.status.equals("fail")) {
                logger.debug(ltag, "fail");
                setCoinStatus(ccs, i, CloudCoin.STATUS_FAIL);
                addCoinsToRarr(i, ccs);
                continue;
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                continue;
            }

        }
        
        String file;
        String dir = AppCore.getUserDir(Config.DIR_DETECTED, user);
        if (isSkyWithdraw)
            dir = AppCore.getDownloadsDir();
        
        for (CloudCoin tcc : ccs) {
            tcc.setPownStringFromDetectStatus();
            logger.debug(ltag, "cc " + tcc.sn + " pown " + tcc.getPownString());
                                    
            if (!tcc.isSentFixable()) {
                logger.debug(ltag, "Coin SN " + cc.sn + " isn't sent fixable. Checking if we can put it in the Partial folder");
                if (tcc.canbeRecoveredFromLost()) {
                    String f = AppCore.getPartialDir() + File.separator + cc.sn + "-" + tcc.getFileName();
                    logger.debug(ltag, "Saving coin as partial: " + f);
                    String seed, pan;
                    for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                        seed = "" + i + "" + tcc.sn + "" + pang;
                        pan = AppCore.getMD5(seed);
                        logger.debug(ltag, "sn " + tcc.sn + " seed " + seed + " pan " + pan);
                        tcc.ans[i] = pan;
                    }
                    if (!AppCore.saveFile(f, tcc.getJson(false))) {
                        logger.error(ltag, "Failed to move coin to Partial: " + tcc.getFileName());
                        if (needReceipt)
                            addCoinToReceipt(tcc, "error", "None");
                        e++;
                        continue;
                    }
                }
                if (needReceipt)
                    addCoinToReceipt(ccs.get(i), "counterfeit", "None");
                c++;
                continue;
            }
            
            String seed, pan;
            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                seed = "" + i + "" + tcc.sn + "" + pang;
                pan = AppCore.getMD5(seed);
                logger.debug(ltag, "sn " + tcc.sn + " seed " + seed + " pan " + pan);
                tcc.ans[i] = pan;
            }

            file = dir + File.separator + tcc.getFileName();
            logger.info(ltag, "Saving coin " + file);
            if (!AppCore.saveFile(file, tcc.getJson(false))) {
                logger.error(ltag, "Failed to move coin to Detected: " + tcc.getFileName());
                if (needReceipt)
                    addCoinToReceipt(tcc, "error", "None");
                e++;
                continue;
            }
            ff.add(file);
            
            logger.info(ltag, "cc=" + tcc.sn + " v=" + tcc.getJson(false));
            globalResult.amount += tcc.getDenomination();
            a++;
            av += tcc.getDenomination();
            if (needReceipt)
                addCoinToReceipt(tcc, "authentic", Config.DIR_BANK + " from " + fdstFolder);
        }

        

        logger.info(ltag, "Received " + cc.sn);

        return true;
    }
    
    
    
    
    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }
    
    
    public void doReceiveEnvelope(int sn, String memo) {
        int i;

        globalResult.needExtra = false;
        
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            globalResult.status = ReceiverResult.STATUS_ERROR;
            globalResult.errText = AppCore.raidaErrText;
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }
  
        CloudCoin idcc = getIDcc(sn);
        if (idcc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + sn);
            rr.status = ReceiverResult.STATUS_ERROR;
            rr.errText = "Failed to find coin ID";
            rr = new ReceiverResult();
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
            return;
        }
        
        globalResult.step = 1;
        globalResult.totalRAIDAProcessed = 0;
        
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        logger.debug(ltag, "Maxcoins: " + maxCoins);
        
        globalResult.totalFiles = 0;   
        globalResult.totalFilesProcessed = 0;
        globalResult.totalCoinsProcessed = 0;
        
        
        rr = new ReceiverResult();
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);

        if (!processReceiveEnvelope(idcc, memo)) {
            rr = new ReceiverResult();
            globalResult.status = ReceiverResult.STATUS_ERROR;
            copyFromGlobalResult(rr);
            if (cb != null)
                cb.callback(rr);
                
            return;
        }

        rr = new ReceiverResult();
                
        rr.files = new String[ff.size()];
        for (i = 0; i < ff.size(); i++) {
            rr.files[i] = ff.get(i);
        }
        globalResult.status = ReceiverResult.STATUS_FINISHED;              
        copyFromGlobalResult(rr);
        if (cb != null)
            cb.callback(rr);

    } 
        
        
    public boolean processReceiveEnvelope(CloudCoin cc, String memo)  {
        String[] results;
        Object o;
        CommonResponse errorResponse;

        ReceiverEnvelopeResponse rer;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;

        

        String pang = AppCore.generateHex().toLowerCase();
        
        logger.debug(ltag, "Generated pang " + pang);

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "receive_envelope";

            sbs[i] = new StringBuilder();
            sbs[i].append("?b=t");
            sbs[i].append("&nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(cc.sn);
            sbs[i].append("&an=");
            sbs[i].append(cc.ans[i]);
            sbs[i].append("&tag=");
            sbs[i].append(URLEncoder.encode(memo));
            sbs[i].append("&pang=");
            sbs[i].append(pang);
            
            requests[i] += sbs[i].toString();
            //posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, null, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    ReceiverResult rrlocal = new ReceiverResult();
                    copyFromGlobalResult(rrlocal);
                    myCb.callback(rrlocal);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query receive");
            return false;
        }

        CloudCoin fakeCC = new CloudCoin(Config.DEFAULT_NN, 1);
        HashMap hm2 = new HashMap<String, Integer>();
        rer = new ReceiverEnvelopeResponse();
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    fakeCC.setDetectStatus(i, CloudCoin.STATUS_UNTRIED);
                    continue;
                } else if (results[i].equals("E")) {
                    logger.error(ltag, "ERROR RAIDA " + i);
                    fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "Skipped raida due to zero response: " + i);
                fakeCC.setDetectStatus(i,  CloudCoin.STATUS_NORESPONSE);
                continue;
            }

            o = parseResponse(results[i], ReceiverEnvelopeResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }
            
            rer = (ReceiverEnvelopeResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + rer.status);
            if (rer.status.equals("done")) {
                logger.debug(ltag, "received: " + rer.message);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_PASS);
                
                String[] sns = rer.message.split(",");
                for (int c = 0; c < sns.length; c++) {                   
                    String idx = sns[c];
                    if (!hm2.containsKey(idx)) {
                        hm2.put(idx, 0);
                    }
            
                    int val = (int) hm2.get(idx);
                    hm2.put(idx, val + 1);
                }
                continue;
            } else if (rer.status.equals("fail")) {
                logger.debug(ltag, "fail");
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_FAIL);
                continue;
            } else {
                logger.error(ltag, "Invalid status: " + rer.status);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                continue;
            }

        }
        
        fakeCC.setPownStringFromDetectStatus();
        logger.debug(ltag, "receive pown " + fakeCC.getPownString());
        if (!fakeCC.canbeRecoveredFromLost()) {
            logger.debug(ltag, "Too many errors from receive");
            return false;            
        }

        String dir = AppCore.getDownloadsDir();
        
        
        Iterator it = hm2.entrySet().iterator();
        int max;
        String keySN;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            keySN = (String) pair.getKey();
            max = (int) pair.getValue();
            if (max < Config.MIN_PASSED_NUM_TO_BE_AUTHENTIC) {
                logger.debug(ltag, "SN " + keySN + " has only " + max + " passed raidas. Skipping it");
                continue;
            }
            
            logger.debug(ltag, "sn received " + keySN);
            int sn;
            try {
                sn = Integer.parseInt(keySN);
            } catch (NumberFormatException e) {
                logger.error(ltag, "Failed to parse SN: " + keySN);
                continue;
            }
            
            CloudCoin mcc = new CloudCoin(Config.DEFAULT_NN, sn);
            String seed, pan;
            for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                seed = "" + i + "" + mcc.sn + "" + pang;
                pan = AppCore.getMD5(seed);
                logger.debug(ltag, "sn " + mcc.sn + " seed " + seed + " pan " + pan);
                mcc.ans[i] = pan;
            }
            
            String sfile = dir + File.separator + mcc.getFileName();
            logger.info(ltag, "Saving coin " + sfile);
            if (!AppCore.saveFile(sfile, mcc.getJson(false))) {
                logger.error(ltag, "Failed to save coin: " + mcc.getFileName());
                continue;
            }
            ff.add(sfile);
            globalResult.amount += mcc.getDenomination();
            it.remove(); // avoids a ConcurrentModificationException
        }

       

        return true;
    }
}
