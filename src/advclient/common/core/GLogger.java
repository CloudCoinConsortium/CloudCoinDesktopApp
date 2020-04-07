package global.cloudcoin.ccbank.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


public abstract class GLogger implements GLoggerInterface {

    PrintWriter channel;
    String fileName;

    protected Map<String, PrintWriter> channels;
    protected Map<String, String> fileNames;
    
    //protected String[] channelKeys;
    
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

    public PrintWriter openCommonFile(String file) {
        File f = new File(file);
        BufferedWriter br;
        FileWriter fr;
        PrintWriter c;
        
        if (f.exists()) {
            double bytes = f.length();
            if (bytes > Config.MAX_COMMON_LOG_LENGTH_MB * 1024 * 1024)
                f.delete();
            
            f = new File(file);
        }

        try {
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr, Config.LOG_BUFFER_SIZE);
            c = new PrintWriter(br);
        } catch (IOException e) {
            return null;
        }

        return c;
    }

    public synchronized void logCommon(String key, String text) {
        PrintWriter curChannel = null;
        
        if (channels.containsKey(key)) {
            PrintWriter subChannel = channels.get(key);
            if (subChannel == null) {
                String fname = AppCore.getLogDir() +  File.separator + key + File.separator + key + ".log";                    
                subChannel = openCommonFile(fname);
                if (subChannel != null) {
                    channels.put(key, subChannel);
                    fileNames.put(key, fname);
                    curChannel = subChannel;
                }          
            } else {
                curChannel = channels.get(key);
            }     
        }

        if (curChannel == null) {
            if (channel == null) {
                String fname = AppCore.getLogDir() +  File.separator + Config.MAIN_LOG_FILENAME;
                curChannel = openCommonFile(fname);
                if (curChannel == null)
                    return;
             
                fileName = fname;
                channel = curChannel;
            } else {
                curChannel = channel;
            }
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String dateText = dateFormat.format(date);

        text = dateText + " " + text;

        curChannel.println(text);
        curChannel.flush();
    }

    public void killMe() {
        for (Map.Entry<String,PrintWriter> entry : channels.entrySet()) {
            String key = entry.getKey();
            PrintWriter sc = entry.getValue();
            
            if (sc == null)
                continue;
            
            sc.close();
            
            String path = fileNames.get(key);
            if (path == null)
                continue;
            
            File f = new File(path);
            if (f.exists())
                f.delete();    
        }

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
        
        String dfileName = AppCore.getCurrentBackupDir(AppCore.getBackupDir(), "__all");
        File fd = new File(dfileName);
        fd.mkdirs();
        
        dfileName += File.separator + f.getName();
        return AppCore.copyFile(f.getAbsolutePath(), dfileName);
    }
}
