package global.cloudcoin.ccbank.Transfer;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.CommonResponse;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import java.util.ArrayList;

/**
 *
 * @author Александр
 */
public class Transfer extends Servant {
    String ltag = "Transfer";
    TransferResult tr;
    TransferResult globalResult;

    public Transfer(String rootDir, GLogger logger) {
        super("Transfer", rootDir, logger);
    }

    public void launch(int fromsn, int tosn, int[] sns, int amount, String tag, CallbackInterface icb) {
        this.cb = icb;

        final int ffromsn = fromsn;
        final int ftosn = tosn;
        final int[] fsns = sns;
        final int famount = amount;
        final String ftag = tag;

        tr = new TransferResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];
        globalResult = new TransferResult();
        
        csb = new StringBuilder();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Transfer");
                doTransfer(ffromsn, ftosn, fsns, ftag, famount);
            }
        });
    }
    
    private void copyFromGlobalResult(TransferResult trResult) {
        trResult.totalFilesProcessed = globalResult.totalFilesProcessed;
        trResult.totalRAIDAProcessed = globalResult.totalRAIDAProcessed;
        trResult.totalCoins = globalResult.totalCoins;
        trResult.totalCoinsProcessed = globalResult.totalCoinsProcessed;
        trResult.totalFiles = globalResult.totalFiles;
        trResult.status = globalResult.status;
        trResult.amount = globalResult.amount;
        trResult.errText = globalResult.errText;
        trResult.step = globalResult.step;
    }
    
    public void doTransfer(int fromsn, int tosn, int sns[], String tag, int amount) {
        tr = new TransferResult();
        
        if (!updateRAIDAStatus()) {
            logger.error(ltag, "Can't proceed. RAIDA is unavailable");
            tr.status = TransferResult.STATUS_ERROR;
            tr.errText = AppCore.raidaErrText;
            copyFromGlobalResult(tr);
            if (cb != null)
                cb.callback(tr);
            return;
        }

        CloudCoin extraCoin = null;
        if (!pickCoinsAmountFromArray(sns, amount)) {
            logger.debug(ltag, "Not enough coins in the cloud for amount " + amount);
            
            coinsPicked = new ArrayList<CloudCoin>();
            extraCoin = pickCoinsAmountFromArrayWithExtra(sns, amount);
            if (extraCoin == null) {
                globalResult.status = TransferResult.STATUS_ERROR;
                globalResult.errText = "Failed to pick coins from the Sky Wallet";
                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);  
                        
                return;
            }

            logger.debug(ltag, "Got extra coin " + extraCoin.sn + " denomination: " + extraCoin.getDenomination());
        }
        
        CloudCoin idcc = getIDcc(fromsn);
        if (idcc == null) {
            logger.error(ltag, "NO ID Coin found for SN: " + fromsn);
            tr.status = TransferResult.STATUS_ERROR;
            tr.errText = "Failed to find coin ID";
            copyFromGlobalResult(tr);
            if (cb != null)
                cb.callback(tr);
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
                        TransferResult trlocal = new TransferResult();
                        copyFromGlobalResult(trlocal);
                        myCb.callback(trlocal);
                    }
                }
            });
            
            if (csns == null) {
                tr = new TransferResult();
                globalResult.status = TransferResult.STATUS_ERROR;
                globalResult.errText = "Failed to break coin in Bank";
                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);
                
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
                tr = new TransferResult();
                globalResult.status = TransferResult.STATUS_ERROR;
                globalResult.errText = "Failed to collect coins after breaking change";
                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);
                
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
        
        
        copyFromGlobalResult(tr);
        if (cb != null)
            cb.callback(tr);
        
        
        logger.info(ltag, "total files "+ globalResult.totalFiles);
        globalResult.totalCoins = amount;
        
        int curValProcessed = 0;      
        for (CloudCoin cc : coinsPicked) {
            logger.debug(ltag, "Transferring from SN " + fromsn + " to SN " + tosn);         
            if (isCancelled()) {
                logger.info(ltag, "Cancelled");
                resume();
                tr = new TransferResult();
                globalResult.status = TransferResult.STATUS_CANCELLED;
                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);

                return;
            }
            
            ccs.add(cc);
            curValProcessed += cc.getDenomination();
            if (ccs.size() == maxCoins) {
                logger.info(ltag, "Processing");
                tr = new TransferResult();
                
                if (!processTransfer(ccs, idcc, tag, tosn)) {
                   tr = new TransferResult();
                   globalResult.status = TransferResult.STATUS_ERROR;
                   copyFromGlobalResult(tr);
                   if (cb != null)
                        cb.callback(tr);
                
                   return;
                }
          
                ccs.clear();

                globalResult.totalRAIDAProcessed = 0;
                globalResult.totalFilesProcessed += maxCoins;
                globalResult.totalCoinsProcessed = curValProcessed;

                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);         
            }  
        }
        
        tr = new TransferResult();
        if (ccs.size() > 0) {
            logger.info(ltag, "adding + " + ccs.size());
            if (!processTransfer(ccs, idcc, tag, tosn)) {
                tr = new TransferResult();
                globalResult.status = TransferResult.STATUS_ERROR;
                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);
                
                return;
            } else {
                globalResult.totalFilesProcessed += ccs.size();
            }
        } 
            
        globalResult.status = TransferResult.STATUS_FINISHED;
        copyFromGlobalResult(tr);
        if (cb != null)
            cb.callback(tr);
        
    }
    
    private void setCoinStatus(ArrayList<CloudCoin> ccs, int idx, int status) {
        for (CloudCoin cc : ccs) {
            cc.setDetectStatus(idx, status);
        }
    }
    
    public boolean processTransfer(ArrayList<CloudCoin> ccs, CloudCoin cc, String tag, int tosn)  {       
        String[] results;
        //Object[] o;
        Object o;
        CommonResponse errorResponse;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;

        logger.debug(ltag, "Transferring to " + tosn);
        
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "transfer";

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
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());
            sbs[i].append("&to_sn=");
            sbs[i].append(tosn);
            sbs[i].append("&tag=");
            sbs[i].append(tag);

            for (CloudCoin tcc : ccs) {
                sbs[i].append("&sns[]=");
                sbs[i].append(tcc.sn);
            }

            posts[i] = sbs[i].toString();
        }

        results = raida.query(requests, posts, new CallbackInterface() {
            final GLogger gl = logger;
            final CallbackInterface myCb = cb;

            @Override
            public void callback(Object result) {
                globalResult.totalRAIDAProcessed++;
                if (myCb != null) {
                    TransferResult trlocal = new TransferResult();
                    copyFromGlobalResult(trlocal);
                    myCb.callback(trlocal);
                }
            }
        });
        
        if (results == null) {
            logger.error(ltag, "Failed to query Transfer");
            return false;
        }

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    setCoinStatus(ccs, i, CloudCoin.STATUS_UNTRIED);
                    continue;
                }
            }
            
            if (results[i] == null) {
                logger.error(ltag, "No response: " + i);
                setCoinStatus(ccs, i, CloudCoin.STATUS_NORESPONSE);
                continue;
            }

            //o = parseArrayResponse(results[i], TransferResponse.class);
            o = parseResponse(results[i], TransferResponse.class);
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
            
            TransferResponse ars = (TransferResponse) o;           
            logger.debug(ltag, "raida" + i + " status: " + ars.status);
            if (ars.status.equals("allpass")) {
                logger.debug(ltag, "allpass");
                setCoinStatus(ccs, i, CloudCoin.STATUS_PASS);
                continue;
            } else if (ars.status.equals("allfail")) {
                logger.debug(ltag, "allfail");
                setCoinStatus(ccs, i, CloudCoin.STATUS_FAIL);
                continue;
            } else if (ars.status.equals("mixed")) {
                logger.debug(ltag, "mixed " + ars.message);
                String[] rss = ars.message.split(",");
                if (rss.length != ccs.size()) {
                    logger.error(ltag, "Invalid length returned: " + rss.length + ", expected: " + ccs.size());
                    setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                    continue;
                }
                
                for (int j = 0; j < rss.length; j++) {
                    String strStatus = rss[j];
                    int status;
                    if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                        status = CloudCoin.STATUS_PASS;
                    } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                        status = CloudCoin.STATUS_FAIL;
                    } else {
                        status = CloudCoin.STATUS_ERROR;
                        logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                    }
                    
                    ccs.get(j).setDetectStatus(i, status);
                }
            } else {
                logger.error(ltag, "Invalid status: " + ars.status);
                setCoinStatus(ccs, i, CloudCoin.STATUS_ERROR);
                continue;
            }


        }

        boolean failed = false;
        for (CloudCoin tcc : ccs) {
            tcc.setNoResponseForEmpty();
            tcc.setPownStringFromDetectStatus();
            
            //int cnt = AppCore.getPassedCount(tcc);
            
            logger.info(ltag, "cc " + tcc.sn + " " + tcc.getPownString());
            
            //if (cnt < Config.PASS_THRESHOLD) {
            if (!cc.isSentFixable()) {
                logger.error(ltag, "Coin " + tcc.sn + " was not transferred. PassCount: " + tcc.sn);
                failed = true;
            }
        }
        
        if (failed) {
            tr.errText = "Some coins were transferred with errors. Please check the main.log file";
            logger.error(ltag, "Transfer failed");
            return false;
        }
        
        logger.info(ltag, "Transferred");

        return true;
    }
    
    
}
