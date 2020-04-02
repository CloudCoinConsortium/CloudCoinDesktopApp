/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.GLogger;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 *
 * @author Alexander
 */
public class Brand {
    String name;
    String ltag = "Brand";
    GLogger logger;
    
    public Brand(GLogger logger) {
        this.name = Config.DEFAULT_BRAND_NAME;
        this.logger = logger;
    }
    
    public Brand(String name, GLogger logger) {
        this.name = name;
        this.logger = logger;
    }

    public boolean init() {
        
        writeDefaultConfigIfNeeded();
        
        System.out.println(this.logger);
        if (!readConfig()) {
            logger.error(ltag, "Failed to read Config");
            return false;
        }
        
        return true;
    }
    
    public void writeDefaultConfigIfNeeded() {
        String configFile = getConfigPath();
        File f = new File(configFile);
        if (f.exists()) 
            return;
              
        String ls = System.getProperty("line.separator");
        logger.debug(ltag, "Saving Brand Config " + configFile);
        
        StringBuilder sb = new StringBuilder();
        sb.append("[general]");
        sb.append(ls);
        sb.append("name=CloudCoin");
        sb.append(ls);
        sb.append("title=CloudCoin Wallet");
        sb.append(ls);
        sb.append("versionoffset=0");
        sb.append(ls);
        sb.append("terms=internal:TermsAndConditions.html");
        sb.append(ls);
        sb.append(ls);
        sb.append("[images]");
        sb.append(ls);
        sb.append("logo=internal:CloudCoinLogo.png");
        sb.append(ls);
        sb.append("logotext=internal:CloudCoinText.png");
        sb.append(ls);
        sb.append("backgroundimage=internal:bglogo.png");
        sb.append(ls);
        sb.append("icon=internal:CloudCoinLogo.png");
        sb.append(ls);
        sb.append("depositicon=internal:depositicon.png");
        sb.append(ls);
        sb.append("withdrawicon=internal:depositicon.png");
        sb.append(ls);
        sb.append("transfericon=internal:transfericon.png");
        sb.append(ls);
        sb.append("depositiconhover=internal:depositiconlight.png");
        sb.append(ls);
        sb.append("withdrawiconhover=internal:depositiconlight.png");
        sb.append(ls);
        sb.append("transfericonhover=internal:transfericonlight.png");
        sb.append(ls);
        sb.append("supporticon=internal:supporticon.png");
        sb.append(ls);
        sb.append("settingsicon=internal:gears.png");
        sb.append(ls);
        sb.append("coinsicon=internal:coinsicon.png");
        sb.append(ls);
        sb.append("supporthtmlicon=internal:support0.png");
        sb.append(ls);
        sb.append("supporttimeicon=internal:support1.png");
        sb.append(ls);
        sb.append("supportphoneicon=internal:support2.png");
        sb.append(ls);
        sb.append("supportemailicon=internal:support3.png");
        sb.append(ls);
        sb.append("supporteportalicon=internal:support0.png");
        sb.append(ls);
        sb.append("walleticon=internal:walleticon.png");
        sb.append(ls);
        sb.append("walleticonactive=internal:walleticonlight.png");
        sb.append(ls);
        sb.append("vaulticon=internal:vaulticon.png");
        sb.append(ls);
        sb.append("vaulticonactive=internal:vaulticonlight.png");
        sb.append(ls);
        sb.append(ls);
        sb.append("[colors]");
        sb.append(ls);
        sb.append("color1=#ffffff");
        sb.append(ls);
        sb.append(ls);
        sb.append("[fonts]");
        sb.append(ls);
        sb.append("mainfont=internal:Montserrat-Regular.otf");
        sb.append(ls);
        sb.append("mainfontsemibold=internal:Montserrat-SemiBold.otf");
        sb.append(ls);
        sb.append("mainfontbold=internal:Montserrat-Bold.otf");
        sb.append(ls);
        sb.append("secondfont=internal:OpenSans-Regular.ttf");
        sb.append(ls);
        sb.append("secondfontsemibold=internal:OpenSans-Semibold.ttf");
        sb.append(ls);
        sb.append(ls);
        sb.append("[support]");
        sb.append(ls);
        sb.append("supportemail=support@cloudcoinmail.com");
        sb.append(ls);
        sb.append("supportpage=http://cloudcoinconsortium.com/use.html");
        sb.append(ls);
        sb.append("supporttime=9AM to 3AM California time (PST)");
        sb.append(ls);
        sb.append("supportphone=+1 (530) 762-1361");
        sb.append(ls);
        sb.append("supportportal=https://cloudcoinsupport.atlassian.net/servicedesk/customer/portals");
        sb.append(ls);

        

        AppCore.saveFile(configFile, sb.toString());
    }
    
    public String getConfigPath() {
        return AppCore.getRootPath() + File.separator + this.name + ".ini";
    }
    
    public boolean readConfig() {
        String configFile = getConfigPath();
        File f = new File(configFile);
        if (!f.exists()) {
            try {
                f.createNewFile();
                writeDefaultConfigIfNeeded();
            } catch (IOException e) {
                logger.error(ltag, "Failrf to write config: " + e.getMessage());
                return false;
            }
        }

        FileReader fr;
        try {
            fr = new FileReader(f);
        } catch (Exception e) {
            logger.error(ltag, "Failed to read mail config: " + configFile);
            return false;
        }
        
        Map<String, Properties> data;
        try {
            data = AppCore.parseINI(fr);
        } catch (IOException e) {
            logger.error(ltag, "Failed to parse mail config: " + configFile);
            return false;
        }
        
        Properties ms = data.get("general");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: general section not defined");
            return false;
        }
        
        return true;
    }
    
    
}
