/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import global.cloudcoin.ccbank.core.AppCore;
import global.cloudcoin.ccbank.core.CallbackInterface;
import global.cloudcoin.ccbank.core.Config;
import global.cloudcoin.ccbank.core.DetectionAgent;
import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.RAIDA;
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
    String brandDir;

    
    
    String title;
    String terms;
    String versionOffset;
    
    String logo;
    String logoText;
    String backgroundImage;
    String icon;
    String depositIcon;
    String withdrawIcon;
    String transferIcon;
    String depositIconHover;
    String withdrawIconHover;
    String transferIconHover;
    String supportIcon;
    String settingsIcon;
    String coinsIcon;
    String supportHtmlIcon;
    String supportTimeIcon;
    String supportPhoneIcon;
    String supportEmailIcon;
    String supportPortalIcon;
    String walletIcon;
    String walletIconActive;
    String vaultIcon;
    String vaultIconActive;
    String lockIcon;
    String lockIconActive;
    String cloudIcon;
    String cloudIconActive;
    
    String mainFont;
    String mainFontSemiBold;
    String mainFontBold;
    String secondFont;
    String secondFontSemiBold;
    
    String supportEmail;
    String supportPage;
    String supportTime;
    String supportPhone;
    String supportPortal;
    
           
    
    Color backgroundColor;
    
    public Brand(String name, GLogger logger) {
        this.name = name;
        this.logger = logger;
        this.brandDir = AppCore.getBrandsDir() + File.separator + this.name;
       
        setDefaultVariables();
    }
    
    public void setDefaultVariables() {
        this.backgroundColor = new Color(200, 200, 200);
        this.title = "Wallet";
        this.versionOffset = "0.0.0";
    }

    public String getConfigPath() {
        return this.brandDir + File.separator + Config.BRAND_CONFIG_NAME;
    }
    
    public boolean downloadConfig() {
        String url = Config.BRAND_URL + "/" + name + "/config.ini";
        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_COUNT * 10000, logger);
        daFake.setExactFullUrl(url);
        System.out.println(url);
        String result = daFake.doRequest("", null);
        if (result == null) {
            logger.error(ltag, "Failed to receive response from Brand Server");
            return false;
        }

        if (!AppCore.saveFile(getConfigPath(), result)) {
            logger.error(ltag, "Failed to save config file");
            return false;
        }
        
        return true;
    }
    
    
    public boolean init(CallbackInterface cb) { 
        BrandResult br = new BrandResult();
        br.step = 0;
        br.totalSteps = 0;
        br.isError = false;
        File brandDir = new File(this.brandDir);
        if (!brandDir.exists()) {
            if (!AppCore.createDirectoryPath(this.brandDir))
                return false;
        }
        
        //try {             Thread.sleep(2000);        } catch (InterruptedException e) {}
        
        
        File configFile = new File(getConfigPath());
        System.out.println("dddddddddd " +configFile.getAbsolutePath());
        if (!configFile.exists()) {
            logger.debug(ltag, "Downloading config file");
            br.text = "Downloading Config File";
            cb.callback(br);
            if (!downloadConfig()) {
                br.text = "Failed to download config file";
                br.isError = true;
                cb.callback(br);
                return false;
            }
        }

        br.text = "Parsing config file";
        cb.callback(br);

        if (!readConfig()) {
            logger.error(ltag, "Failed to read Config");
            br.text = "Failed to parse config file";
            br.isError = true;
            cb.callback(br);
            return false;
        }
        
        
        br.text = "done";
        return true;
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
        
        String p;
        
        // General
        Properties ms = data.get("general");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: general section not defined");
            return false;
        }

        p = getProperty(ms, "title");
        if (p != null)
            title = p;
        
        p = getProperty(ms, "versionOffset");
        if (p != null)
            versionOffset = p;
        
        p = getProperty(ms, "terms");
        if (p != null)
            terms = p;
        
        // Images
        ms = data.get("images");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: images section not defined");
            return false;
        }

        p = getProperty(ms, "logo");
        if (p != null)
            logo = p;
        
        p = getProperty(ms, "logotext");
        if (p != null)
            logoText = p;
        
        p = getProperty(ms, "backgroundimage");
        if (p != null)
            backgroundImage = p;
        
        p = getProperty(ms, "icon");
        if (p != null)
            icon = p;
        
        p = getProperty(ms, "depositicon");
        if (p != null)
            depositIcon = p;
        
        p = getProperty(ms, "withdrawicon");
        if (p != null)
            withdrawIcon = p;
        
        p = getProperty(ms, "transfericon");
        if (p != null)
            transferIcon = p;
        
        p = getProperty(ms, "depositiconhover");
        if (p != null)
            depositIconHover = p;
        
        p = getProperty(ms, "transfericonhover");
        if (p != null)
            transferIconHover = p;
       
        p = getProperty(ms, "withdrawiconhover");
        if (p != null)
            withdrawIconHover = p;
        
        p = getProperty(ms, "supporticon");
        if (p != null)
            supportIcon = p;
        
        p = getProperty(ms, "settingsicon");
        if (p != null)
            settingsIcon = p;
        
        p = getProperty(ms, "coinsicon");
        if (p != null)
            coinsIcon = p;
        
        p = getProperty(ms, "supporthtmlicon");
        if (p != null)
            supportHtmlIcon = p;
        
        p = getProperty(ms, "supporttimeicon");
        if (p != null)
            supportTimeIcon = p;
        
        p = getProperty(ms, "supportphoneicon");
        if (p != null)
            supportPhoneIcon = p;
        
        p = getProperty(ms, "supportemailicon");
        if (p != null)
            supportEmailIcon = p;
        
        p = getProperty(ms, "supportportalicon");
        if (p != null)
            supportPortalIcon = p;
        
        p = getProperty(ms, "walleticon");
        if (p != null)
            walletIcon = p;
        
        p = getProperty(ms, "walleticonactive");
        if (p != null)
            walletIconActive = p;
        
        p = getProperty(ms, "vaulticon");
        if (p != null)
            vaultIcon = p;
        
        p = getProperty(ms, "vaulticonactive");
        if (p != null)
            vaultIconActive = p;
        
        p = getProperty(ms, "lockicon");
        if (p != null)
            lockIcon = p;
        
        p = getProperty(ms, "lockiconactive");
        if (p != null)
            lockIconActive = p;
        
        p = getProperty(ms, "cloudicon");
        if (p != null)
            walletIcon = p;
        
        p = getProperty(ms, "cloudiconactive");
        if (p != null)
            cloudIconActive = p;
        
        // Colors
        ms = data.get("colors");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: colors section not defined");
            return false;
        }

        p = getProperty(ms, "backgroundcolor");
        if (p != null)
            backgroundColor = p;
        
        // Fonts
        ms = data.get("fonts");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: fonts section not defined");
            return false;
        }

        p = getProperty(ms, "mainfont");
        if (p != null)
            mainFont = p;
        
        p = getProperty(ms, "mainfontsemibold");
        if (p != null)
            mainFontSemiBold = p;
        
        p = getProperty(ms, "mainfontbold");
        if (p != null)
            mainFontBold = p;
                
        p = getProperty(ms, "secondfont");
        if (p != null)
            secondFont = p;
                        
        p = getProperty(ms, "secondfontsemibold");
        if (p != null)
            secondFontSemiBold = p;
        
        // Support
        ms = data.get("support");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: support section not defined");
            return false;
        }

        p = getProperty(ms, "supportemail");
        if (p != null)
            supportEmail = p;
        
        p = getProperty(ms, "supportpage");
        if (p != null)
            supportPage = p;
        
        p = getProperty(ms, "supporttime");
        if (p != null)
            supportTime = p;
        
        p = getProperty(ms, "supportphone");
        if (p != null)
            supportPhone = p;
        
        p = getProperty(ms, "supportportal");
        if (p != null)
            supportPortal = p;
        
        
        return true;
    }
    
    private String getProperty(Properties parent, String name) {
        String property = parent.getProperty(name);
        if (property == null) {
            logger.error(ltag, "Failed to get property " + name);
            return null;
        }
        
        return property;
    }
    
    private String getResultingVersion(String version) {
        String[] oparts = version.split("\\.");     
        String[] parts = this.versionOffset.split("\\.");
        if (parts.length != 3) {
            logger.error(ltag, "Invalid versionOffset format");
            return version;
        }
     
       
        int omajor, ominor, obuildNumber;
        int major, minor, buildNumber;
        try {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
            buildNumber = Integer.parseInt(parts[2]);
            
            omajor = Integer.parseInt(oparts[0]);
            ominor = Integer.parseInt(oparts[1]);
            obuildNumber = Integer.parseInt(oparts[2]);
        } catch (NumberFormatException e) {
            logger.error(ltag, "Failed to parse version offset");
            return version;
        }
        
        omajor += major;
        ominor += minor;
        obuildNumber += buildNumber;
        
        return "" + omajor + "." + ominor + "." + obuildNumber;
        
    }
    
    
    public Color getBackgroundColor() {
        return this.backgroundColor;
    }
    
    
    
    public String getTitle(String version) {
        return this.title + " " + this.getResultingVersion(version);
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
        sb.append("terms=TermsAndConditions.html");
        sb.append(ls);
        sb.append(ls);
        sb.append("[images]");
        sb.append(ls);
        sb.append("logo=CloudCoinLogo.png");
        sb.append(ls);
        sb.append("logotext=CloudCoinText.png");
        sb.append(ls);
        sb.append("backgroundimage=bglogo.png");
        sb.append(ls);
        sb.append("icon=CloudCoinLogo.png");
        sb.append(ls);
        sb.append("depositicon=depositicon.png");
        sb.append(ls);
        sb.append("withdrawicon=depositicon.png");
        sb.append(ls);
        sb.append("transfericon=transfericon.png");
        sb.append(ls);
        sb.append("depositiconhover=depositiconlight.png");
        sb.append(ls);
        sb.append("withdrawiconhover=depositiconlight.png");
        sb.append(ls);
        sb.append("transfericonhover=transfericonlight.png");
        sb.append(ls);
        sb.append("supporticon=supporticon.png");
        sb.append(ls);
        sb.append("settingsicon=gears.png");
        sb.append(ls);
        sb.append("coinsicon=coinsicon.png");
        sb.append(ls);
        sb.append("supporthtmlicon=support0.png");
        sb.append(ls);
        sb.append("supporttimeicon=support1.png");
        sb.append(ls);
        sb.append("supportphoneicon=support2.png");
        sb.append(ls);
        sb.append("supportemailicon=support3.png");
        sb.append(ls);
        sb.append("supporteportalicon=support0.png");
        sb.append(ls);
        sb.append("walleticon=walleticon.png");
        sb.append(ls);
        sb.append("walleticonactive=walleticonlight.png");
        sb.append(ls);
        sb.append("vaulticon=vaulticon.png");
        sb.append(ls);
        sb.append("vaulticonactive=vaulticonlight.png");
        sb.append(ls);
        sb.append(ls);
        sb.append("[colors]");
        sb.append(ls);
        sb.append("color1=#ffffff");
        sb.append(ls);
        sb.append(ls);
        sb.append("[fonts]");
        sb.append(ls);
        sb.append("mainfont=Montserrat-Regular.otf");
        sb.append(ls);
        sb.append("mainfontsemibold=Montserrat-SemiBold.otf");
        sb.append(ls);
        sb.append("mainfontbold=Montserrat-Bold.otf");
        sb.append(ls);
        sb.append("secondfont=OpenSans-Regular.ttf");
        sb.append(ls);
        sb.append("secondfontsemibold=OpenSans-Semibold.ttf");
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
}
