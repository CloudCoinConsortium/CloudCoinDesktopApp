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

    public void launch(CallbackInterface icb) {
        this.cb = icb;

        er = new EraserResult();
        launchThread(new Runnable() {
            @Override
            public void run() {
                logger.info(ltag, "RUN Eraser");

                doErase();
                if (cb != null)
                    cb.callback(er);
            }
        });
    }

    public void doErase() {
        logger.info(ltag, "ERASE");

        emptyDir(Config.DIR_LOGS);
        emptyDir(Config.DIR_RECEIPTS);

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
