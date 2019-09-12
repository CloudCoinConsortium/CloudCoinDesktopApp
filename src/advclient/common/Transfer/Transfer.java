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
            logger.debug(ltag, "Not enough coins in the cloudfor amount " + amount);
            
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
        
        //setSenderRAIDA();
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
        
        
        ArrayList<CloudCoin> ccs;
        ccs = new ArrayList<CloudCoin>();
        
        int maxCoins = getIntConfigValue("max-coins-to-multi-detect");
        if (maxCoins == -1)
            maxCoins = Config.DEFAULT_MAX_COINS_MULTIDETECT;
        
        logger.debug(ltag, "Maxcoins: " + maxCoins);
               
        globalResult.totalFiles = coinsPicked.size();
        globalResult.totalRAIDAProcessed = 0;
        globalResult.totalFilesProcessed = 0;
        globalResult.totalCoinsProcessed = 0;
        
        
        copyFromGlobalResult(tr);
        if (cb != null)
            cb.callback(tr);
        
        
        logger.info(ltag, "total files "+ globalResult.totalFiles);
        
        for (CloudCoin cc : coinsPicked) {
            globalResult.totalCoins += cc.getDenomination();
        }
        
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
    
        if (extraCoin != null) {
            int remained = amount - curValProcessed;
            logger.debug(ltag, "Changing coin " + extraCoin.sn + " alreadyProcessed=" + curValProcessed + " needed=" + amount);
            if (!processTransferWithChange(extraCoin, idcc, tag, tosn, remained)) {
                tr = new TransferResult();
                globalResult.status = TransferResult.STATUS_ERROR;
                copyFromGlobalResult(tr);
                if (cb != null)
                    cb.callback(tr);
                
                return;
            }
        }
        
        globalResult.status = TransferResult.STATUS_FINISHED;
        copyFromGlobalResult(tr);
        if (cb != null)
            cb.callback(tr);
        
    }
    
    public boolean processTransfer(ArrayList<CloudCoin> ccs, CloudCoin cc, String tag, int tosn)  {       
        String[] results;
        Object[] o;
        CommonResponse errorResponse;
        TransferResponse[][] trs;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;
        CloudCoin[] sccs;

        logger.debug(ltag, "Transferring to " + tosn);
        
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "transfer";

            sbs[i] = new StringBuilder();
            sbs[i].append("nn=");
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

        sccs = new CloudCoin[ccs.size()];
        trs = new TransferResponse[RAIDA.TOTAL_RAIDA_COUNT][];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }

            o = parseArrayResponse(results[i], TransferResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }

            if (o.length != sccs.length) {
                logger.error(ltag, "RAIDA " + i + " wrong number of coins: " + o.length);
                continue;
            }

            for (int j = 0; j < o.length; j++) {
                String strStatus;
                int rnn, rsn;
                String ran;
                boolean found;

                trs[i] = new TransferResponse[o.length];
                trs[i][j] = (TransferResponse) o[j];

                strStatus = trs[i][j].status;

                found = false;
                int cstatus;
                if (strStatus.equals(Config.REQUEST_STATUS_PASS)) {
                    logger.info(ltag, "OK response from raida " + i);
                    cstatus = CloudCoin.STATUS_PASS;
                } else if (strStatus.equals(Config.REQUEST_STATUS_FAIL)) {
                    logger.error(ltag, "Counterfeit response from raida " + i);
                    cstatus = CloudCoin.STATUS_FAIL;
                } else {
                    logger.error(ltag, "Unknown coin status from RAIDA" + i + ": " + strStatus);
                    cstatus = CloudCoin.STATUS_ERROR;
                }
                
                
            }
        }

        logger.info(ltag, "Transferred");

        return true;
    }
    
    
    public boolean processTransferWithChange(CloudCoin tcc, CloudCoin cc, String tag, int tosn, int amount)  {       
        String[] results;
        CommonResponse errorResponse;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;
        CloudCoin[] sccs;

        logger.debug(ltag, "Transferring with change to " + tosn);

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        posts = new String[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            requests[i] = "transfer_with_change";

            sbs[i] = new StringBuilder();
            sbs[i].append("nn=");
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
            sbs[i].append("&payment_envelope=");
            sbs[i].append(tag);

            sbs[i].append("&payment_required=");
            sbs[i].append(amount);

            sbs[i].append("&sns[]=");
            sbs[i].append(tcc.sn);
            
            sbs[i].append("&public_change_maker=");
            sbs[i].append(Config.PUBLIC_CHANGE_MAKER_ID);

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

        sccs = new CloudCoin[1];
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }

            TransferResponse o = (TransferResponse) parseResponse(results[i], TransferResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }
            
            if (!o.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Invalid status from raida " + i + " status: " + o.status);
                continue;
            }

            logger.debug(ltag, "Change from RAIDA " + i + " OK");
        }

        logger.info(ltag, "Change Transferred");

        return true;
    }
    
}
