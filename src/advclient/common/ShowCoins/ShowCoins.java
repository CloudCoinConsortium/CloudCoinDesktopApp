package global.cloudcoin.ccbank.ShowCoins;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.Servant;
import java.util.ArrayList;

public class ShowCoins extends Servant {
    String ltag = "ShowCoins";

    ShowCoinsResult result;
    ArrayList<CloudCoin> ccs;

    public ShowCoins(String rootDir, GLogger logger) {
        super("ShowCoins", rootDir, logger);

        result = new ShowCoinsResult();
        ccs = new ArrayList<CloudCoin>();

    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        result.counters = new int[Config.IDX_FOLDER_LAST][5];
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN ShowCoins");
                doShowCoins();

                if (cb != null)
                    cb.callback(result);
            }
        });
    }

    public void doShowCoins() {

        cleanPrivateLogDir();

        logger.info(ltag, "Show Coins");

        showCoinsInFolder(Config.IDX_FOLDER_BANK, Config.DIR_BANK);
        showCoinsInFolder(Config.IDX_FOLDER_FRACKED, Config.DIR_FRACKED);
        //showCoinsInFolder(Config.IDX_FOLDER_LOST, Config.DIR_LOST);
        showCoinsInFolder(Config.IDX_FOLDER_VAULT, Config.DIR_VAULT);
        
        result.coins = new int[ccs.size()];
        int i = 0;
        for (CloudCoin tcc : ccs) {
            result.coins[i] = tcc.sn;
            i++;
        }
    }

    public void showCoinsInFolder(int idx, String folder) {
        String fullPath = AppCore.getUserDir(folder, user);

        CloudCoin cc;

        File dirObj = new File(fullPath);
        if (dirObj.listFiles() == null) {
            logger.error(ltag, "No such dir " + fullPath);
            return;
        }
        
        
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            logger.debug(ltag, "Parsing " + file);

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse JSON: " + e.getMessage());
                continue;
            }

            switch (cc.getDenomination()) {
                case 1:
                    result.counters[idx][Config.IDX_1]++;
                    break;
                case 5:
                    result.counters[idx][Config.IDX_5]++;
                    break;
                case 25:
                    result.counters[idx][Config.IDX_25]++;
                    break;
                case 100:
                    result.counters[idx][Config.IDX_100]++;
                    break;
                case 250:
                    result.counters[idx][Config.IDX_250]++;
                    break;
            }
            
            ccs.add(cc);
        }

        logger.debug(ltag, "Total coins in " + folder + ": " + ccs.size());

        createStatFile(folder, result.counters[idx]);
    }

    public void createStatFile(String folder, int[] counters) {
        String fileName = folder + "_" + AppCore.getTotal(counters) + "_" + counters[Config.IDX_1] +
                "_" + counters[Config.IDX_5] + "_" + counters[Config.IDX_25] + "_" +
                counters[Config.IDX_100] + "_" + counters[Config.IDX_250] + ".txt";

        File file = new File(privateLogDir + File.separator + fileName);
        try {
            if (!file.createNewFile()) {
                logger.error(ltag, "Failed to create file " + fileName);
            }
        } catch (IOException e) {
            logger.error(ltag, "Failed to create file " + fileName);
        }
    }

}
