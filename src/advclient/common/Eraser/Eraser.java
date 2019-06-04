package global.cloudcoin.ccbank.Eraser;

import java.io.File;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.Servant;

public class Eraser extends Servant {
    String ltag = "Eraser";
    EraserResult er;


    public Eraser(String rootDir, GLogger logger) {
        super("Eraser", rootDir, logger);
    }

    public void launch(CallbackInterface icb, boolean needBackup) {
        this.cb = icb;

        final boolean fneedBackup = needBackup;
        
        er = new EraserResult();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Eraser");

                doErase(fneedBackup);
                if (cb != null)
                    cb.callback(er);
            }
        });
    }

    public void doErase(boolean needBackup) {
        logger.info(ltag, "ERASE needBackup " + needBackup);

    //    emptyDir(Config.DIR_LOGS);
    //    emptyDir(Config.DIR_RECEIPTS);

        String bdir = AppCore.getCurrentBackupDir(AppCore.getBackupDir(), user);
        if (needBackup) {
            logger.info(ltag, "Creating backup dir " + bdir);

            File f = new File(bdir);
            f.mkdirs();
            if (!f.exists()) {
                logger.error(ltag, "Failed to create dir " + bdir);
                er.status = EraserResult.STATUS_ERROR;
                return;
            }     
        }
        
        String fullPath = AppCore.getUserDir(Config.DIR_RECEIPTS, user);
        File d = new File(fullPath);
        for (File file : d.listFiles()) {
            if (file.isDirectory())
                continue;

            if (needBackup) {
                logger.debug(ltag, "Copy " + file.getAbsolutePath() + " to " +  bdir);                
                if (!AppCore.copyFile(file.getAbsolutePath(), bdir + File.separator + file.getName())) {
                    logger.error(ltag, "Failed to copy");
                    er.status = EraserResult.STATUS_ERROR;
                    return;
                }
            }
            
            logger.debug(ltag, "Deleting " + file.toString());
            file.delete();
        }
        
        fullPath = AppCore.getRootUserDir(user);
        d = new File(fullPath);
        for (File file : d.listFiles()) {
            if (file.isDirectory())
                continue;
            
            if (file.getName().startsWith(Config.TRANSACTION_FILENAME)) {
                if (needBackup) {
                    logger.debug(ltag, "Copy " + file.getAbsolutePath() + " to " +  bdir);
                    if (!AppCore.copyFile(file.getAbsolutePath(), bdir + File.separator + file.getName())) {
                        logger.error(ltag, "Failed to copy");
                        er.status = EraserResult.STATUS_ERROR;
                        return;
                    }
                }
                
                logger.debug(ltag, "Deleting " + file.toString());
                file.delete();
            }            
        }
        
        er.status = EraserResult.STATUS_FINISHED;
    }

    public void emptyDir(String dir) {
        String fullPath = AppCore.getUserDir(dir, user);

        File d = new File(fullPath);
        for (File file : d.listFiles()) {
            if (file.isDirectory())
                continue;

            logger.debug(ltag, "Deleting " + file.toString());
            file.delete();
        }
    }
}
