/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.GLoggerInterface;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Александр
 */
public class WLogger extends GLogger implements GLoggerInterface {

    Pattern pattern;
    
    
    public WLogger() {
        super();

        pattern = Pattern.compile("^.*?\\[(.+)\\]");
        channels = new HashMap<String, PrintWriter>();
        fileNames = new HashMap<String, String>();

        //channels.put("Echoer", null);
        channels.put("ShowCoins", null);
        channels.put("ShowEnvelopeCoins", null);
          
    }
    
    @Override
    public void onLog(int level, String tag, String message) {
        String levelStr; 
        Matcher matcher;
        MatchResult result;
        String tidx;
        
        //if (1==1)
          //  return;
        
        if (level == GL_DEBUG) {
         //   System.out.println("tag: " + tag + " " + message);
            levelStr = "[DEBUG]";
        } else if (level == GL_VERBOSE) {
//            System.out.println("tag: " + tag + " " + message);
            levelStr = "[VERBOSE]";
        } else if (level == GL_ERROR) {
  //          System.out.println("tag: " + tag + " " + message);
            levelStr = "[ERROR]";
        } else {
            levelStr = "[INFO]";
        }
        
        
        matcher = pattern.matcher(tag);
        result = matcher.toMatchResult();

        tidx = tag;
        if (matcher.find()) {
            if (matcher.group().length() > 1) {
                tidx = matcher.group(1);
            }
        }
        
        StackTraceElement[] es = Thread.currentThread().getStackTrace();
        boolean found = false;
        for (int i = 0; i < es.length; i++) {
                String className = es[i].getClassName();
                for (Map.Entry<String,PrintWriter> entry : channels.entrySet()) {
                    String key = entry.getKey();
                    String rex = ".+\\." + key + "\\..+";                    
                    if (className.matches(rex)) {
                        tidx = key;
                        break;
                    }
                }

                if (found)
                    break;
        }
        
        if (tag.startsWith(":")) {
            String[] parts = tag.split(":");
            if (parts.length > 2) {
                boolean done = false;
                for (Map.Entry<String,PrintWriter> entry : channels.entrySet()) {
                    String key = entry.getKey();
                    String rex = ".+\\." + key + "\\..+";                    
                    if (parts[1].matches(rex)) {
                        tag = parts[2];
                        tidx = key;
                        done = true;
                        break;
                    }
                }
                
                if (!done) {
                    tag = parts[2];
                    tidx = tag;
                }
                    
            }
            
        }
        
        
        //System.out.println("tidx="+tidx+ " t="+tag+ " m="+message);
        
        logCommon(tidx, tag + " " + levelStr + " " + message);
    }
}
