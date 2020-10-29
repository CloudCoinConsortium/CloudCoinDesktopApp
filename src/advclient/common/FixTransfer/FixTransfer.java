package global.cloudcoin.ccbank.FixTransfer;


import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;
import global.cloudcoin.ccbank.core.GLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class FixTransfer extends Servant {
    String ltag = "FixTransfer";

    public FixTransfer(String rootDir, GLogger logger) {
        super("FixTransfer", rootDir, logger);
    }

    public void launch(HashMap<String, Integer> hm, CallbackInterface icb) {
        this.cb = icb;
 
        initRarr();
        
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Echoer");
                
                doFixTransfer(hm);
                

            }
        });
    }

    public void doFixTransfer(HashMap<String, Integer> hm) {
        Iterator it = hm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String sn = (String) pair.getKey();
            int v = (int) pair.getValue();       
            
            int d; 
            try {
                d = Integer.parseInt(sn);
            } catch (NumberFormatException e) {
                continue;
            }
            
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, d);
            int presentCounter = 0;
            for (int j = 0; j < RAIDA.TOTAL_RAIDA_COUNT; j++) {
                //int rj = (RAIDA.TOTAL_RAIDA_COUNT - j - 1);
                
                boolean present = (((v >> j) & 1) > 0);
                if (present)
                    presentCounter++;
            }
            
            boolean needToAddCoin = false;
            if (presentCounter >= (RAIDA.TOTAL_RAIDA_COUNT / 2) + 1) {
                logger.debug(ltag, "Coin is present on the majority of RAIDA servers. Will try to add it to the other Raida servers");
                needToAddCoin = true;
            } else {
                logger.debug(ltag, "Coin is NOT present on the majority of RAIDA servers. Will try to delete it from the other Raida servers");
                needToAddCoin = false;
            }
            
            for (int j = 0; j < RAIDA.TOTAL_RAIDA_COUNT; j++) {
                //System.out.println("sn " + sn + " r="+j + " p="+present);
                boolean present = (((v >> j) & 1) > 0);
                
                if (needToAddCoin) {
                    if (!present) {
                        logger.debug(ltag, "To add: " + cc.sn);
                        addCoinToRarr(j, cc);
                    }
                } else {
                    if (present) {
                        logger.debug(ltag, "To remove: " + cc.sn);
                        addCoinToRarr(j, cc);
                    }
                }
                
                
            }
        }   
        
        fixTransfer(rarr);
    }

}
