package global.cloudcoin.ccbank.Exporter;


import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
import global.cloudcoin.ccbank.core.Servant;

public class Exporter extends Servant {
    String ltag = "Exporter";
    ExporterResult er;
    String ls;


    public Exporter(String rootDir, GLogger logger) {
        super("Exporter", rootDir, logger);
    }

    public void launch(CallbackInterface icb) {
        this.cb = icb;
    }

    public void launch(int type, int amount, String tag, String dir, boolean keepSrc, CallbackInterface icb) {
        this.cb = icb;

        final int ftype = type;
        final int famount = amount;
        final String ftag = tag;
        final String fdir = dir;
        final boolean fkeepSrc = keepSrc;

        er = new ExporterResult();
        receiptId = er.receiptId = AppCore.generateHex();
        csb = new StringBuilder();
        
        ls = System.getProperty("line.separator");
    
        coinsPicked = new ArrayList<CloudCoin>();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Exporter");

                doExport(ftype, null, famount, fdir, fkeepSrc, ftag);
            }
        });
    }
    
    public void launch(int type, int[] values, String tag, String dir, boolean keepSrc, CallbackInterface icb) {
        this.cb = icb;

        final int ftype = type;
        final int[] fvalues = values;
        final String ftag = tag;
        final String fdir = dir;
        final boolean fkeepSrc = keepSrc;

        er = new ExporterResult();
        coinsPicked = new ArrayList<CloudCoin>();
        valuesPicked = new int[AppCore.getDenominations().length];

        for (int i = 0; i < valuesPicked.length; i++)
            valuesPicked[i] = 0;

        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Exporter");

                doExport(ftype, fvalues, 0, fdir, fkeepSrc, ftag);
            }
        });
    }

    public void doExport(int type, int[] values, int amount, String dir, boolean keepSrc, String tag) {
        if (tag.equals(""))
            tag = Config.DEFAULT_TAG;

        logger.debug(ltag, "Export type " + type + " amount " + amount + " dir " + dir + " tag " + tag + " user " + user);

        if (tag.indexOf('.') != -1 || tag.indexOf('/') != -1 || tag.indexOf('\\') != -1) {
            logger.error(ltag, "Invalid tag");
            er.status = ExporterResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(er);
            
            return;
        }
      
        String fullExportPath = AppCore.getUserDir(Config.DIR_EXPORT, user);
        
        if (dir != null)
            fullExportPath = dir;
        
        String fullFrackedPath = AppCore.getUserDir(Config.DIR_FRACKED, user);
        String fullBankPath = AppCore.getUserDir(Config.DIR_BANK, user);

        if (values != null) {
            if (values.length != AppCore.getDenominations().length) {
                logger.error(ltag, "Invalid params");
                er.status = ExporterResult.STATUS_ERROR;
                if (cb != null)
                    cb.callback(er);

                return;
            }

            if (!pickCoinsInDir(fullBankPath, values)) {
                logger.debug(ltag, "Not enough coins in the bank dir");
                if (!pickCoinsInDir(fullFrackedPath, values)) {
                    logger.error(ltag, "Not enough coins in the Fracked dir");
                    er.status = ExporterResult.STATUS_ERROR;
                    er.errText = Config.PICK_ERROR_MSG;
                    if (cb != null)
                        cb.callback(er);

                    return;
                }
            }
        } else {
            if (!pickCoinsAmountInDirs(fullBankPath, fullFrackedPath, amount)) {
                logger.debug(ltag, "Not enough coins in the bank dir for amount " + amount);
                er.status = ExporterResult.STATUS_ERROR;
                er.errText = Config.PICK_ERROR_MSG;
                if (cb != null)
                    cb.callback(er);
                    
                return;
            }
        }

        logger.debug(ltag, "Export isbackup " + keepSrc + " type " + type + " amount " + amount);
        
        if (type == Config.TYPE_STACK) {
            if (!keepSrc) {
                int totalNotes = coinsPicked.size();
                if (totalNotes > Config.MAX_EXPORTED_NOTES) {
                    er.status = ExporterResult.STATUS_ERROR;
                    er.errText = "Exporting more than " +  Config.MAX_EXPORTED_NOTES 
                            + " notes is not allowed. Try to export fewer coins";
                    if (cb != null)
                        cb.callback(er);
                    
                    return;
                }
            }
            
            System.out.println("cp="+coinsPicked.size());
            System.exit(1);
            if (!exportStack(fullExportPath, tag)) {
                er.status = ExporterResult.STATUS_ERROR;
                if (cb != null)
                    cb.callback(er);

                return;
            }
        } else if (type == Config.TYPE_JPEG) {
            if (!exportJpeg(fullExportPath, user, tag)) {
                er.status = ExporterResult.STATUS_ERROR;
                if (cb != null)
                    cb.callback(er);

                return;
            }
        } else {
            logger.error(ltag, "Unsupported format");
            er.status = ExporterResult.STATUS_ERROR;
            if (cb != null)
                cb.callback(er);

            return;
        }
        
        if (!keepSrc) {
            logger.debug(ltag, "Deleting original files");
            deletePickedCoins();
            for (CloudCoin cc: coinsPicked) {
                addCoinToReceipt(cc, "authentic", Config.DIR_EXPORT);
            }
        
            saveReceipt(user, coinsPicked.size(), 0, 0, 0, 0, 0);
        }
        
        er.status = ExporterResult.STATUS_FINISHED;
        if (cb != null)
            cb.callback(er);

        logger.info(ltag, "EXPORT finished " + fullExportPath);
    }

    private void deletePickedCoins() {
        for (CloudCoin cc : coinsPicked) {
            AppCore.deleteFile(cc.originalFile);
        }
    }

    private boolean exportJpeg(String dir, String user, String tag) {
        String templateDir = AppCore.getUserDir(Config.DIR_TEMPLATES, user);
        String fileName;
        StringBuilder sb;

        sb = new StringBuilder();

        byte[] bytes;
        for (CloudCoin cc : coinsPicked) {
            logger.debug(ltag, "Exporting: " + cc.sn);
            fileName = templateDir + File.separator + "jpeg" + cc.getDenomination() + ".jpg";

            bytes = AppCore.loadFileToBytes(fileName);
            if (bytes == null) {
                logger.error(ltag, "Failed to load template");
                return false;
            }

            logger.info(ltag, "Loaded: " + bytes.length);

            // Ans
            for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
                sb.append(cc.ans[i]);
            }

            // AOID
            sb.append("00000000000000000000000000000000");

            // PownString
            sb.append("11111111111111111111111111");
            
            // Append HC
            sb.append("00");

            // Append ED
            sb.append("AC");//AC means nothing

            // NN
            sb.append("0" + cc.nn);

            // SN
            sb.append(AppCore.padString(Integer.toHexString(cc.sn).toUpperCase(), 6, '0'));


            byte[] ccArray = AppCore.hexStringToByteArray(sb.toString());
            int offset = 20;
            for (int j =0; j < ccArray.length; j++) {
                bytes[offset + j] = ccArray[j];
            }

            fileName = cc.getDenomination() + ".CloudCoin." + tag + ".jpeg";
            fileName = dir + File.separator + fileName;

            logger.info(ltag, "saving bytes " + bytes.length);
            if (!AppCore.saveFileFromBytes(fileName, bytes)) {
                logger.error(ltag, "Failed to write file");
                return false;
            }

            er.totalExported += cc.getDenomination();
            er.exportedFileNames.add(fileName);
        }

 

        return true;
    }


    private boolean exportStack(String dir, String tag) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        int total = 0;
        String fileName;

        sb.append("{" + ls + "\t\"cloudcoin\": [");
        for (CloudCoin cc : coinsPicked) {
            if (!first)
                sb.append(", ");

            sb.append(cc.getSimpleJson());
            first = false;

            total += cc.getDenomination();
        }

        sb.append("]" + ls + "}");

        File sdir = new File(dir);
        if (sdir.isDirectory()) {
            fileName = total + ".CloudCoin." + tag + ".stack";
            fileName = dir + File.separator + fileName;
        } else {
            fileName = dir;
        }

        File f = new File(fileName);
        if (f.exists()) {
            logger.error(ltag, "File exists: " + fileName);
            er.status = ExporterResult.STATUS_ERROR;
            er.errText = "Exported file with the same tag already exists";
            return false;
        }
        
        if (!AppCore.saveFile(fileName, sb.toString())) {
            logger.error(ltag, "Failed to save file " + fileName);
            return false;
        }

        er.exportedFileNames.add(fileName);
        er.totalExported = total;

        return true;
    }

}
