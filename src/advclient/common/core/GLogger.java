package global.cloudcoin.ccbank.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public abstract class GLogger implements GLoggerInterface {

    PrintWriter channel;
    String fileName;

    public void info(String tag, String message) {
        onLog(GLoggerInterface.GL_INFO, tag, message);
    }

    public void debug(String tag, String message) {
        onLog(GLoggerInterface.GL_DEBUG, tag, message);
    }

    public void verbose(String tag, String message) {
        onLog(GLoggerInterface.GL_VERBOSE, tag, message);
    }

    public void error(String tag, String message) {
        onLog(GLoggerInterface.GL_ERROR, tag, message);
    }

    public boolean openCommonFile(String file) {
        File f = new File(file);
        BufferedWriter br;
        FileWriter fr;
        

        if (f.exists()) {
            double bytes = f.length();
            if (bytes > Config.MAX_COMMON_LOG_LENGTH_MB * 1024 * 1024)
                f.delete();
        }

        fileName = file;
        try {
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);
            channel = new PrintWriter(br);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public synchronized void logCommon(String text) {
        if (channel == null) {
            if (!openCommonFile(AppCore.getLogDir() +  File.separator + Config.MAIN_LOG_FILENAME))
                return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String dateText = dateFormat.format(date);

        text = dateText + " " + text;

        channel.println(text);
        channel.flush();
    }

    public void killMe() {
        if (channel == null)
            return;
        
        channel.close();
        
        File f = new File(fileName);
        if (f.exists())
            f.delete();        
        
        channel = null;
    }
    
    public boolean copyMe() {
        channel.flush();
        
        File f = new File(fileName);
        if (!f.exists())
            return false;
        
        String dfileName = AppCore.getCurrentBackupDir(AppCore.getBackupDir(), Config.DIR_DEFAULT_USER);
        dfileName += File.separator + f.getName();
        
        return AppCore.copyFile(f.getAbsolutePath(), dfileName);
    }
}
