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

        channels.put("Echoer", null);
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
        
        logCommon(tidx, tag + " " + levelStr + " " + message);
    }
}
