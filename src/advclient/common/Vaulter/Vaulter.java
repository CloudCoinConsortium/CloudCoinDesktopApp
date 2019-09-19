package global.cloudcoin.ccbank.Vaulter;

import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import global.cloudcoin.ccbank.ChangeMaker.ChangeMakerResult;
import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class Vaulter extends Servant {
    String ltag = "Vaulter";
    VaulterResult vr;


    public Vaulter(String rootDir, GLogger logger) {
        super("Vaulter", rootDir, logger);
    }

    public void launch(final int operation, String password, int amount, CloudCoin cc, CallbackInterface icb) {
        this.cb = icb;

        final int famount = amount;
        final String fpassword = password;
        final CloudCoin fcc = cc;

        vr = new VaulterResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];
        
        vr.errText = "";

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Vaulter " + operation);

                if (operation == Config.VAULTER_OP_VAULT)
                    doVault(fpassword, famount, fcc);
                else if (operation == Config.VAULTER_OP_UNVAULT)
                    doUnvault(fpassword, famount, fcc);
                else
                    vr.status = VaulterResult.STATUS_ERROR;

                if (cb != null)
                    cb.callback(vr);
            }
        });
    }

    public void vault(String password, int amount, CloudCoin cc, CallbackInterface icb) {
        launch(Config.VAULTER_OP_VAULT, password, amount, cc, icb);
    }

    public void unvault(String password, int amount, CloudCoin cc, CallbackInterface icb) {
        launch(Config.VAULTER_OP_UNVAULT, password, amount, cc, icb);
    }

    public void doVault(String password, int amount, CloudCoin cc) {
        String fullVaultPath = AppCore.getUserDir(Config.DIR_VAULT, user);

        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);
        if (!getCoins(fullBankPath, amount, cc, null)) {
            logger.error(ltag, "Failed to get coins");
            vr.status = VaulterResult.STATUS_ERROR;
            return;
        }
        
        if (password.isEmpty()) {
            logger.error(ltag, "Password is empty");
            vr.status = VaulterResult.STATUS_ERROR;
            return;
        }
      
        String hash = AppCore.getMD5(password);
        logger.debug(ltag, "password hash " + hash);
        for (CloudCoin tcc : coinsPicked) {
            ArrayList<String> vans = new ArrayList<>();

            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                String an = tcc.ans[i];
                
                if (an.length() != 32) {
                    logger.error(ltag, "Invalid an for coin " + tcc.sn + ". Skip it " + an);
                    continue;
                }

                // Get decimals from AN
                long octet = Long.parseLong(an.substring(8, 16), 16);
                long decimal1 = Long.parseLong(Long.toString(octet, 10));
                octet = Long.parseLong(an.substring(16, 24), 16);
                long decimal2 = Long.parseLong(Long.toString(octet, 10));

                
                octet = Long.parseLong(hash.substring(0, 8), 16);
                long decimal3 = Long.parseLong(Long.toString(octet, 10));
                octet = Long.parseLong(hash.substring(8, 16), 16);
                long decimal4 = Long.parseLong(Long.toString(octet, 10));

                
                long van1 = decimal1 - decimal3;
                long van2 = decimal2 - decimal4;
                String hex1 = AppCore.padString(Long.toHexString(Math.abs(van1)), 8, '0');
                String hex2 = AppCore.padString(Long.toHexString(Math.abs(van2)), 8, '0');

                logger.debug(ltag, hex1 + ", " + hex2);

                // Update new AN
                StringBuilder builder = new StringBuilder();
                builder.append(an, 0, 8);
                if (van1 < 0) {
                    builder.append('(');
                    builder.append(hex1);
                    builder.append(')');
                }
                else
                    builder.append(hex1);
                if (van2 < 0) {
                    builder.append('(');
                    builder.append(hex2);
                    builder.append(')');
                }
                else
                    builder.append(hex2);

                
                try {
                    builder.append(an, 24, 32);
                } catch (Exception e) {
                    logger.error(ltag, "Exception in vault: " + e.getMessage());
                    vr.status = VaulterResult.STATUS_ERROR;
                    return;
                }

                
                
                logger.info(ltag, "cc=" + tcc.sn + " was AN" + i + ":" + tcc.ans[i]);
                tcc.ans[i] = builder.toString().toLowerCase();
                logger.info(ltag, "cc=" + tcc.sn + " is AN" + i + ":" + tcc.ans[i]);
            }

            String vpath = fullVaultPath + File.separator + tcc.getFileName();
            logger.info(ltag, "p="+vpath);
            if (!AppCore.saveFile(fullVaultPath + File.separator + tcc.getFileName(), tcc.getJson(false))) {
                logger.error(ltag, "Failed to save file: " + vpath);
                vr.status = VaulterResult.STATUS_ERROR;
                return;
            }

            logger.debug(ltag, "File has been successfully encrypted. Moving the original file to Trash");
            AppCore.moveToFolder(tcc.originalFile, Config.DIR_TRASH, user);
        }

        vr.status = VaulterResult.STATUS_FINISHED;
    }

    public void doUnvault(String password, int amount, CloudCoin cc) {
        String fullVaultPath = AppCore.getUserDir(Config.DIR_VAULT, user);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);
        String fullFrackedPath = AppCore.getUserDir(Config.DIR_FRACKED, user);

        if (!getCoins(fullVaultPath, amount, cc, fullFrackedPath)) {
            logger.error(ltag, "Failed to get coins");
            vr.errText = Config.PICK_ERROR_MSG;
            vr.status = VaulterResult.STATUS_ERROR;
            return;
        }

        String hash = AppCore.getMD5(password);
        logger.debug(ltag, "password hash " + hash);
        for (CloudCoin tcc : coinsPicked) {
            String ccFile = tcc.originalFile;
            File cf = new File(ccFile);
            File parentCf = cf.getParentFile();
            if (parentCf == null) {
                logger.error(ltag, "Can't find coins folder");
                continue;
            }   
            
            String coinFolder = cf.getParentFile().getName();
            if (coinFolder.equals(Config.DIR_FRACKED)) {
                logger.debug(ltag, "Skipping fracked");
                continue;
            }
            
            
            ArrayList<String> ans = new ArrayList<>();

            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                String van = tcc.ans[i];
                
                                
                if (van.length() < 32) {
                    logger.error(ltag, "Invalid van for coin " + tcc.sn + ". Skip it " + van);
                    continue;
                }
                
              
                int index1 = 8, index2 = 16;
                int lastByte = 32;
                int parenthesisCount = AppCore.charCount(van, '(');
                if (parenthesisCount == 1) {
                    if (van.indexOf('(') == 8) {
                        index1 = 9;
                        index2 = 18;
                        
                    } else {
                        index2 = 17;
                    }
                    lastByte = 34;
                } else if (parenthesisCount == 2) {
                    index1 = 9;
                    index2 = 19;
                    lastByte = 36;
                }
                
                // Get decimals from VAN
                long octet = Long.parseLong(van.substring(index1, index1 + 8), 16);
                long decimal1 = Long.parseLong(Long.toString(octet, 10));
                octet = Long.parseLong(van.substring(index2, index2 + 8), 16);
                long decimal2 = Long.parseLong(Long.toString(octet, 10));

                // Get decimals from MD5 Hash
                octet = Long.parseLong(hash.substring(0, 8), 16);
                long decimal3 = Long.parseLong(Long.toString(octet, 10));
                octet = Long.parseLong(hash.substring(8, 16), 16);
                long decimal4 = Long.parseLong(Long.toString(octet, 10));
      
                // Perform negative calculations based on parenthesis
                if (index1 == 9)
                    decimal1 *= -1;
                if (index2 == 17 || index2 == 19)
                    decimal2 *= -1;

                logger.debug(ltag, decimal1 + ", " + decimal2);
                logger.debug(ltag, decimal3 + ", " + decimal4);

                // Subtract numbers and convert to Hex
                long van1 = decimal1 + decimal3;
                long van2 = decimal2 + decimal4;
                String hex1 = AppCore.padString(Long.toHexString(van1), 8, '0');
                String hex2 = AppCore.padString(Long.toHexString(van2), 8, '0');

                logger.debug(ltag, van1 + ", " + van2);

                // Update new AN
                StringBuilder builder = new StringBuilder();
                builder.append(van, 0, 8);
                builder.append(hex1);
                builder.append(hex2);
                

                try {
                    builder.append(van, index2 + 8, lastByte);
                } catch (Exception e) {
                    logger.error(ltag, "Exception in unvault: " + e.getMessage());
                    vr.status = VaulterResult.STATUS_ERROR;
                    return;
                }

                logger.info(ltag, "cc=" + tcc.sn + " was AN" + i + ":" + tcc.ans[i]);
                tcc.ans[i] = builder.toString().replace(")", "");
                logger.info(ltag, "cc=" + tcc.sn + " is AN" + i + ":" + tcc.ans[i] + " move " + tcc.originalFile + " d=" + Config.DIR_VAULT);
            }
      
            String vpath = fullBankPath + File.separator + tcc.getFileName();
            logger.info(ltag, "p="+vpath);
            if (!AppCore.saveFile(fullBankPath + File.separator + tcc.getFileName(), tcc.getJson(false))) {
                logger.error(ltag, "Failed to save file: " + vpath);
                vr.status = VaulterResult.STATUS_ERROR;
                return;
            }

            logger.debug(ltag, "File has been successfully decrypted. Moving the original file to Trash");
            AppCore.moveToFolder(tcc.originalFile, Config.DIR_TRASH, user);
        }
        
        vr.status = VaulterResult.STATUS_FINISHED;
    }

    private boolean getCoins(String folder, int amount, CloudCoin cc, String addFolder) {
        if (cc != null) {
            coinsPicked.add(cc);
            return true;
        }

        if (amount != 0) {          
            if (addFolder != null) {
                logger.debug(ltag, "Checking with fracked. Dir " + addFolder);
                if (!pickCoinsAmountInDirs(folder, addFolder, amount)) {
                    logger.error(ltag, "Failed to collect coins: " + amount);
                    return false;
                }
                
                return true;
            }
            
            logger.debug(ltag, "Picking from one folder only " + folder);
            if (!pickCoinsAmountInDir(folder, amount))
                return false;

            return true;
        }

        pickAll(folder);

        return true;
    }
    
    public void pickAll(String folder) {
        CloudCoin cc;
        
        File dirObj = new File(folder);
        for (File file : dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                continue;
            }

            coinsPicked.add(cc);
        }
        
    }
}
