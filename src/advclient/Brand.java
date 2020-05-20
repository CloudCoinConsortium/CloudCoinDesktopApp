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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.JComponent;
import javax.swing.JLabel;
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
    String versionOffset;

    String supportEmail;
    String supportPage;
    String supportTime;
    String supportPhone;
    String supportPortal;
    
    String backgroundColor;  
    String headerBackgroundColor;
    String selectedWalletBorderColor;
    String topMenuHoverColor;
    String mainTextColor;
    String secondTextColor;
    String thirdTextColor;
    String tableHeaderTextColor;
    String tableGridColor;
    String dropdownHoverColor;
    String inputBackgroundColor;
    String scrollbarTrackColor;
    String scrollbarThumbColor;
    String inventoryBackgroundColor;
    String errorColor;
    String panelBackgroundColor;
    String inactiveWalletBackgroundColor;
    String activeWalletBackgroundColor;
    String progressbarBackgroundColor;
    String progressbarColor;
    String primaryButtonColor;
    String secondaryButtonColor;
    String dropfilesBackgroundColor;
    String disabledButtonColor;
    String hyperlinkColor;
    String settingsMenuHoverColor;
    String settingsMenuBackgroundColor;
    String titleTextColor;
    
    Font _regFont, _semiBoldFont, _boldFont;
    Font _osRegFont, _osSemiBoldFont;
    
    Map<String, dlResult> datamap;
    
    String needVersion;

    public Brand(String name, GLogger logger) {
        
        this.logger = logger;

        this.setBrand(name);
        this.datamap = new HashMap<String, dlResult>();
       
        String[] vals = {
            "terms","logo","logoText","backgroundImage","icon","depositIcon",
            "withdrawIcon","transferIcon","depositIconHover","withdrawIconHover","transferIconHover",
            "supportIcon","settingsIcon","coinsIcon","supportHtmlIcon","supportTimeIcon","supportPhoneIcon",
            "supportEmailIcon","supportPortalIcon","walletLocalIcon","walletSkyIcon","vaultIcon",
            "vaultIconActive","lockIcon","lockIconActive","cloudIcon","cloudIconActive","coinsInventoryIcon",
            "templatePng","templateJpeg1","templateJpeg5","templateJpeg25","templateJpeg100",
            "templateJpeg250","mainFont","mainFontSemiBold","mainFontBold","secondFont","secondFontSemiBold",
            "dropdownArrow", "arrowLeft", "arrowRight", "toggleyes", "toggleno", "lookingGlass", "eye",
            "emailIcon", "emailIconActive"
        };
        
        for (String s : vals) {
            datamap.put(s, new dlResult());
        }
        
        setDefaultVariables();
    }
    
    public void setBrand(String name) {
        this.name = name;
        this.brandDir = AppCore.getBrandsDir() + File.separator + this.name;
    }
    
    public void setDefaultVariables() {
        this.needVersion = null;
        
        String defaultColor = "#f0f0f0";
        
        this.backgroundColor = defaultColor;
        this.headerBackgroundColor = defaultColor;
        this.selectedWalletBorderColor = defaultColor;
        this.topMenuHoverColor = defaultColor;
        this.mainTextColor = "#000000";
        this.secondTextColor = "#000000";
        this.thirdTextColor = "#000000";
        this.tableHeaderTextColor = "#000000";
        this.tableGridColor = defaultColor;
        this.dropdownHoverColor = defaultColor;
        this.inputBackgroundColor = defaultColor;
        this.scrollbarTrackColor = defaultColor;
        this.scrollbarThumbColor = "#000000";
        this.inventoryBackgroundColor = defaultColor;
        this.errorColor = "#000000";
        this.inactiveWalletBackgroundColor = defaultColor;
        this.activeWalletBackgroundColor = defaultColor;
        this.panelBackgroundColor = defaultColor;
        this.progressbarBackgroundColor = defaultColor;
        this.progressbarColor = "#000000";
        this.primaryButtonColor = defaultColor;
        this.secondaryButtonColor = defaultColor;
        this.disabledButtonColor = defaultColor;
        this.dropfilesBackgroundColor = defaultColor;
        this.hyperlinkColor = "#000000";
        this.titleTextColor = "#000000";
        this.settingsMenuBackgroundColor = defaultColor;
        this.settingsMenuHoverColor = defaultColor;
        
        this.title = "Wallet";
        this.versionOffset = "0.0.0";
    }

    public String getConfigPath() {
        return this.brandDir + File.separator + Config.BRAND_CONFIG_NAME;
    }
    
    public String getAssetPath(String file) {
        return this.brandDir + File.separator + file;
    }
    
    public String getAvailableVersion() {
        return this.needVersion;
    }
    
    public boolean updateAvailable() {
        if (this.needVersion == null)
            return false;
        
        String[] nparts = this.needVersion.split("\\.");     
        String[] parts = AdvancedClient.version.split("\\.");
        if (parts.length != 3 || nparts.length != 3) {
            logger.error(ltag, "Invalid version format");
            return false;
        }
     
       
        int nmajor, nminor, nbuildNumber;
        int major, minor, buildNumber;
        try {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
            buildNumber = Integer.parseInt(parts[2]);
            
            nmajor = Integer.parseInt(nparts[0]);
            nminor = Integer.parseInt(nparts[1]);
            nbuildNumber = Integer.parseInt(nparts[2]);
        } catch (NumberFormatException e) {
            logger.error(ltag, "Failed to parse version");
            return false;
        }
        
        if (nmajor > major) {
            return true;
        } else if (nmajor == major) {
            if (nminor > minor) {
                return true;
            } else if (nminor == minor) {
                if (nbuildNumber > buildNumber) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    public void copyTemplate(String name, String dst) {
        dlResult dl = datamap.get(name);
        if (dl == null) {
            logger.debug(ltag, "Template " + name + " is missing");
            return;
        }
        
        if (dl.name == null) {
            logger.debug(ltag, "Template " + name + " wasn't downloaded");
            return;
        }
        
        String path = getAssetPath(dl.name);
        File f = new File(path);
        if (!f.exists()) {
            logger.debug(ltag, "Template " + name + " doesn't exist");
            return;
        }
        
        File fd = new File(dst);
        if (fd.exists()) {
            return;
        }
        
        logger.debug(ltag, "Copying " + path + " to " + dst);
        AppCore.copyFile(path, dst);
    }
    
    public void copyTemplates() {
        String templateDir;


	templateDir = AppCore.getRootPath() + File.separator + Config.DIR_TEMPLATES;
        
        copyTemplate("templateJpeg1", templateDir + File.separator + "jpeg1.jpg");
        copyTemplate("templateJpeg5", templateDir + File.separator + "jpeg5.jpg");
        copyTemplate("templateJpeg25", templateDir + File.separator + "jpeg25.jpg");
        copyTemplate("templateJpeg100", templateDir + File.separator + "jpeg100.jpg");
        copyTemplate("templateJpeg250", templateDir + File.separator + "jpeg250.jpg");
        copyTemplate("templatePng", templateDir + File.separator + Config.PNG_TEMPLATE_NAME);
   
    }
    
    public String getAssetPathByNameText(String name) {
        dlResult dl = datamap.get(name);
        if (dl == null) {
            return "";
        }

        if (dl.name == null) {
            return "";
        }
        
        String path = getAssetPath(dl.name);
        File f = new File(path);
        if (!f.exists()) {
            return "";
        }
        
        String text = AppCore.loadFile(path);
        if (text == null)
            return "";

        return text;
    }
        
        
        
    
    public URL getAssetPathByName(String name) {
        String fallbackResuource = "resources/clear.gif";
        dlResult dl = datamap.get(name);
        if (dl == null) {
            return getClass().getClassLoader().getResource(fallbackResuource);
        }
        
        if (dl.name == null) {
            return getClass().getClassLoader().getResource(fallbackResuource);
        }
        
        String path = getAssetPath(dl.name);
        File f = new File(path);
        if (!f.exists()) {
            return getClass().getClassLoader().getResource(fallbackResuource);
        }
        
        URL url;
        URI uri = f.toURI();
        try {
           url = uri.toURL();
        } catch (MalformedURLException e) {
            return getClass().getClassLoader().getResource(fallbackResuource);
        }

        return url;
    }
    
    public boolean downloadConfig() {
        String url = Config.BRAND_URL + "/" + name + "/" + Config.BRAND_CONFIG_NAME;
        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_COUNT * 10000, logger);
        daFake.setExactFullUrl(url);

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
    
    public boolean checkFile(String file) {
        String filename = this.brandDir + File.separator + file;
        File f = new File(filename);
        if (f.exists()) 
            return true;
        
        return false;
    }
    
    public String downloadFileNoSave(String purl) {
        String url = Config.BRAND_URL + "/" + purl;
        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_COUNT * 10000, logger);
        daFake.setExactFullUrl(url);

        byte[] bytes = daFake.doBinaryRequest("");
        if (bytes == null) {
            logger.error(ltag, "Failed to receive response from Brand Server");
            return null;
        }
        
        return new String(bytes);
    }
    
    public boolean downloadFile(String file) {
        String filename = this.brandDir + File.separator + file;

        String url = Config.BRAND_URL + "/" + name + "/" + file;
        DetectionAgent daFake = new DetectionAgent(RAIDA.TOTAL_RAIDA_COUNT * 10000, logger);
        daFake.setExactFullUrl(url);


        byte[] bytes = daFake.doBinaryRequest("");
        if (bytes == null) {
            logger.error(ltag, "Failed to receive response from Brand Server");
            return false;
        }

        if (!AppCore.saveFileFromBytes(filename, bytes)) {
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
        File brandDir;
        
        brandDir = new File(this.brandDir);
        if (!brandDir.exists()) {
            if (!AppCore.createDirectoryPath(this.brandDir))
                return false;
        }

        //try {             Thread.sleep(2000);        } catch (InterruptedException e) {}
        //writeDefaultConfigIfNeeded();

        File configFile = new File(getConfigPath());
        if (!configFile.exists()) {
            logger.debug(ltag, "Downloading config file");
            br.text = "Downloading Config File";
            cb.callback(br);
            if (!downloadConfig()) {
                logger.debug(ltag, "Failed to download config");
                if (!this.name.equals(Config.DEFAULT_BRAND_NAME)) {
                    logger.debug(ltag, "Switching to Default Brand");
                    this.setBrand(Config.DEFAULT_BRAND_NAME);
                    brandDir = new File(this.brandDir);
                    if (!brandDir.exists()) {
                        if (!AppCore.createDirectoryPath(this.brandDir))
                            return false;
                    }
                    
                    configFile = new File(getConfigPath());
                    if (!configFile.exists()) {
                        br.text = "Downloading Default Config File";
                        cb.callback(br);
                                        
                        if (!downloadConfig()) {
                            logger.debug(ltag, "Failed to download config");
                            br.text = "Failed to download default config file";
                            br.isError = true;
                
                            cb.callback(br);
                            return false;
                        }
                    }
                } else {
                    br.text = "Failed to download config file";
                    br.isError = true;
                
                    cb.callback(br);
                    return false;
                }
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
        
        br.totalSteps = datamap.entrySet().size();
        int i = 0;
        for (Map.Entry<String,dlResult> entry : datamap.entrySet()) {
            String key = entry.getKey();
            dlResult dr = entry.getValue();          
            if (dr.name == null) {
                i++;
                continue;
            }
            
            br.step = i;
            br.text = "Checking file " + dr.name;
            cb.callback(br);
            if (checkFile(dr.name)) {
                i++;
                continue;
            }
            

            br.text = "Downloading file " + dr.name;
            cb.callback(br);

            downloadFile(dr.name);            
            i++;
        }
        
        br.step = 0;
        br.totalSteps = 0;
        br.text = "Checking for updates";
        cb.callback(br);

        needVersion = downloadFileNoSave("versions/dark_currentversion.txt");
        if (needVersion != null)
            needVersion = needVersion.trim().replaceAll("\r", "").replaceAll("\n", "");
        logger.debug(ltag, "Version available on the Server: " + needVersion);

        br.text = "Starting program";

        cb.callback(br);
        initInternal();
              
        br.text = "done";
        cb.callback(br);
        return true;
    }
    
    public void initInternal() {
        initFonts();
    }
    
    public Font getFont(String name) {
        logger.debug(ltag, "Loading font " + name);
        
        Font defaultFont = new JLabel().getFont();
        Font font;
        String filename;
        filename = this.datamap.get(name).name;
        if (filename == null) {
            logger.debug(ltag, "Font is not defined");
            return defaultFont;
        } else {
            filename = getAssetPath(filename);
            File f = new File(filename);
            if (!f.exists()) {
                logger.debug(ltag, "Font doesn't exist: " + filename);
                return defaultFont;
            } else {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, f);
                } catch (Exception e) {
                    logger.debug(ltag, "Error loading font: " + e.toString());
                    return defaultFont;
                }
            }
        }
        
        return font;
        
    }
    
    public void initFonts() {
        _semiBoldFont = getFont("mainFontSemiBold");
        _boldFont = getFont("mainFontBold");
        _regFont = getFont("mainFont");
        _osRegFont = getFont("secondFont");
        _osSemiBoldFont = getFont("secondFontSemiBold");
    }
    

    public boolean readConfig() {
        String configFile = getConfigPath();
        File f = new File(configFile);
        if (!f.exists()) {
            logger.error(ltag, "Failed to read config: " + configFile);
            return false;
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
        
        p = getProperty(ms, "versionoffset");
        if (p != null)
            versionOffset = p;
        
        p = getProperty(ms, "terms");
        if (p != null)
            datamap.get("terms").name = p;
        
        // Images
        ms = data.get("images");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: images section not defined");
            return false;
        }

        p = getProperty(ms, "logo");
        if (p != null)
            datamap.get("logo").name = p;
        
        p = getProperty(ms, "logotext");
        if (p != null)
            datamap.get("logoText").name = p;
        
        p = getProperty(ms, "backgroundimage");
        if (p != null)
            datamap.get("backgroundImage").name = p;
        
        p = getProperty(ms, "icon");
        if (p != null)
            datamap.get("icon").name = p;
        
        p = getProperty(ms, "depositicon");
        if (p != null)
            datamap.get("depositIcon").name = p;
        
        p = getProperty(ms, "withdrawicon");
        if (p != null)
            datamap.get("withdrawIcon").name = p;
        
        p = getProperty(ms, "transfericon");
        if (p != null)
            datamap.get("transferIcon").name = p;
        
        p = getProperty(ms, "depositiconhover");
        if (p != null)
            datamap.get("depositIconHover").name = p;
        
        p = getProperty(ms, "transfericonhover");
        if (p != null)
            datamap.get("transferIconHover").name = p;
       
        p = getProperty(ms, "withdrawiconhover");
        if (p != null)
            datamap.get("withdrawIconHover").name = p;
        
        p = getProperty(ms, "supporticon");
        if (p != null)
            datamap.get("supportIcon").name = p;
        
        p = getProperty(ms, "settingsicon");
        if (p != null)
            datamap.get("settingsIcon").name = p;
        
        p = getProperty(ms, "coinsicon");
        if (p != null)
            datamap.get("coinsIcon").name = p;
        
        p = getProperty(ms, "supporthtmlicon");
        if (p != null)
            datamap.get("supportHtmlIcon").name = p;
        
        p = getProperty(ms, "supporttimeicon");
        if (p != null)
            datamap.get("supportTimeIcon").name = p;
        
        p = getProperty(ms, "supportphoneicon");
        if (p != null)
            datamap.get("supportPhoneIcon").name = p;
        
        p = getProperty(ms, "supportemailicon");
        if (p != null)
            datamap.get("supportEmailIcon").name = p;
        
        p = getProperty(ms, "supportportalicon");
        if (p != null)
            datamap.get("supportPortalIcon").name = p;
        
        p = getProperty(ms, "walletskyicon");
        if (p != null)
            datamap.get("walletSkyIcon").name = p;
        
        p = getProperty(ms, "walletlocalicon");
        if (p != null)
            datamap.get("walletLocalIcon").name = p;
        
        p = getProperty(ms, "vaulticon");
        if (p != null)
            datamap.get("vaultIcon").name = p;
        
        p = getProperty(ms, "vaulticonactive");
        if (p != null)
            datamap.get("vaultIconActive").name = p;
        
        p = getProperty(ms, "lockicon");
        if (p != null)
            datamap.get("lockIcon").name = p;
        
        p = getProperty(ms, "lockiconactive");
        if (p != null)
            datamap.get("lockIconActive").name = p;
        
        p = getProperty(ms, "cloudicon");
        if (p != null)
            datamap.get("cloudIcon").name = p;
        
        p = getProperty(ms, "cloudiconactive");
        if (p != null)
            datamap.get("cloudIconActive").name = p;
        
        p = getProperty(ms, "emailicon");
        if (p != null)
            datamap.get("emailIcon").name = p;
        
        p = getProperty(ms, "emailiconactive");
        if (p != null)
            datamap.get("emailIconActive").name = p;
        
        
        
        
        p = getProperty(ms, "coinsinventoryicon");
        if (p != null)
            datamap.get("coinsInventoryIcon").name = p;
        
        p = getProperty(ms, "templatepng");
        if (p != null)
            datamap.get("templatePng").name = p;
        
        p = getProperty(ms, "templatejpeg1");
        if (p != null)
            datamap.get("templateJpeg1").name = p;
        
        p = getProperty(ms, "templatejpeg5");
        if (p != null)
            datamap.get("templateJpeg5").name = p;
        
        p = getProperty(ms, "templatejpeg25");
        if (p != null)
            datamap.get("templateJpeg25").name = p;
        
        p = getProperty(ms, "templatejpeg100");
        if (p != null)
            datamap.get("templateJpeg100").name = p;
        
        p = getProperty(ms, "templatejpeg250");
        if (p != null)
            datamap.get("templateJpeg250").name = p;
        
        
        p = getProperty(ms, "dropdownarrow");
        if (p != null)
            datamap.get("dropdownArrow").name = p;
        
        p = getProperty(ms, "arrowleft");
        if (p != null)
            datamap.get("arrowLeft").name = p;
        
        p = getProperty(ms, "arrowright");
        if (p != null)
            datamap.get("arrowRight").name = p;
        
        p = getProperty(ms, "toggleyes");
        if (p != null)
            datamap.get("toggleyes").name = p;
        
        p = getProperty(ms, "toggleno");
        if (p != null)
            datamap.get("toggleno").name = p;
        
        p = getProperty(ms, "lookingglass");
        if (p != null)
            datamap.get("lookingGlass").name = p;
        
        p = getProperty(ms, "eye");
        if (p != null)
            datamap.get("eye").name = p;

        
        // Colors
        ms = data.get("colors");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: colors section not defined");
            return false;
        }

        p = getProperty(ms, "backgroundcolor");
        if (p != null)
            backgroundColor = p;
        
        p = getProperty(ms, "headerbackgroundcolor");
        if (p != null)
            headerBackgroundColor = p;
        
        p = getProperty(ms, "selectedwalletbordercolor");
        if (p != null)
            selectedWalletBorderColor = p;
        
        
        p = getProperty(ms, "topmenuhovercolor");
        if (p != null)
            topMenuHoverColor = p;
        
        p = getProperty(ms, "maintextcolor");
        if (p != null)
            mainTextColor = p;
        
        p = getProperty(ms, "secondtextcolor");
        if (p != null)
            secondTextColor = p;
        
        p = getProperty(ms, "thirdtextcolor");
        if (p != null)
            thirdTextColor = p;
        
        p = getProperty(ms, "tableheadertextcolor");
        if (p != null)
            tableHeaderTextColor = p;
        
        p = getProperty(ms, "tablegridcolor");
        if (p != null)
            tableGridColor = p;
        
        p = getProperty(ms, "dropdownhovercolor");
        if (p != null)
            dropdownHoverColor = p;
        
        p = getProperty(ms, "inputbackgroundcolor");
        if (p != null)
            inputBackgroundColor = p;
        
        p = getProperty(ms, "scrollbartrackcolor");
        if (p != null)
            scrollbarTrackColor = p;
        
        p = getProperty(ms, "scrollbarthumbcolor");
        if (p != null)
            scrollbarThumbColor = p;
        
        p = getProperty(ms, "inventorybackgroundcolor");
        if (p != null)
            inventoryBackgroundColor = p;
        
        p = getProperty(ms, "errorcolor");
        if (p != null)
            errorColor = p;
        
        p = getProperty(ms, "panelbackgroundcolor");
        if (p != null)
            panelBackgroundColor = p;
        
        p = getProperty(ms, "inactivewalletbackgroundcolor");
        if (p != null)
            inactiveWalletBackgroundColor = p;
        
        p = getProperty(ms, "activewalletbackgroundcolor");
        if (p != null)
            activeWalletBackgroundColor = p;
        
        p = getProperty(ms, "progressbarbackgroundcolor");
        if (p != null)
            progressbarBackgroundColor = p;
        
        p = getProperty(ms, "progressbarcolor");
        if (p != null)
            progressbarColor = p;
        
        p = getProperty(ms, "primarybuttoncolor");
        if (p != null)
            primaryButtonColor = p;
        
        p = getProperty(ms, "secondarybuttoncolor");
        if (p != null)
            secondaryButtonColor = p;
        
        p = getProperty(ms, "disabledbuttoncolor");
        if (p != null)
            disabledButtonColor = p;

        p = getProperty(ms, "dropfilesbackgroundcolor");
        if (p != null)
            dropfilesBackgroundColor = p;
        
        p = getProperty(ms, "hyperlinkcolor");
        if (p != null)
            hyperlinkColor = p;
        
        p = getProperty(ms, "settingsmenubackgroundcolor");
        if (p != null)
            settingsMenuBackgroundColor = p;
        
        p = getProperty(ms, "settingsmenuhovercolor");
        if (p != null)
            settingsMenuHoverColor = p;
        
        p = getProperty(ms, "titletextcolor");
        if (p != null)
            titleTextColor = p;
        
        
        
        // Fonts
        ms = data.get("fonts");
        if (ms == null) {
            logger.error(ltag, "Failed to parse config: fonts section not defined");
            return false;
        }

        p = getProperty(ms, "mainfont");
        if (p != null)
            datamap.get("mainFont").name = p;
        
        p = getProperty(ms, "mainfontsemibold");
        if (p != null)
            datamap.get("mainFontSemiBold").name = p;
        
        p = getProperty(ms, "mainfontbold");
        if (p != null)
            datamap.get("mainFontBold").name = p;
                
        p = getProperty(ms, "secondfont");
        if (p != null)
            datamap.get("secondFont").name = p;
                        
        p = getProperty(ms, "secondfontsemibold");
        if (p != null)
            datamap.get("secondFontSemiBold").name = p;
        
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
    
    public String getResultingVersion(String version) {
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
    
    
    public Image getScaledImage(Image img, int maxWidth, int maxHeight) {
        BufferedImage bimg = (BufferedImage) img;
        Image resultImage = (Image) bimg;
        int height = bimg.getHeight();
        int width = bimg.getWidth();
        
        
        double ratio = (double) width / (double) height;
        
        if (height > maxHeight) {
            height = maxHeight;
            width = (int) (height * ratio);
            if (width > maxWidth) {
                width = maxWidth;
                height = (int) (width / ratio);
            }
        } else if (width > maxWidth) {
            width = maxWidth;
            height = (int) (width / ratio);
            if (height > maxHeight) {
                height = maxHeight;
                width = (int) (height / ratio);
            }
        }
    
        resultImage = resultImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return resultImage;
    }

    public Image scaleLogo(Image img) {
        return getScaledImage(img, 54, 54);
    }
    
    public Image scaleLogoText(Image img) {
        return getScaledImage(img, 135, 23);
    }
    
    public Image scaleMainMenuIcon(Image img) {
        return getScaledImage(img, 45, 28);
    }
    
    
    public Color colorFromHex(String colorStr) {
        return new Color(
            Integer.valueOf(colorStr.substring( 1, 3 ), 16 ),
            Integer.valueOf(colorStr.substring( 3, 5 ), 16 ),
            Integer.valueOf(colorStr.substring( 5, 7 ), 16 ) );
    }
    
    
    
    public Font getMainFont() {
        return this._regFont;
    }
    
    public Font getMainSemiBoldFont() {
        return this._semiBoldFont;
    }
    
    public Font getMainBoldFont() {
        return this._boldFont;
    }
    
    public Font getSecondFont() {
        return this._osRegFont;
    }
    
    public Font getSecondSemiBoldFont() {
        return this._osSemiBoldFont;
    }
    
    
    public Color getBackgroundColor() {
        return colorFromHex(this.backgroundColor);
    }
    
    public Color getHeaderBackgroundColor() {
        return colorFromHex(this.headerBackgroundColor);
    }
    
    public Color getSelectedWalletBorderColor() {
        return colorFromHex(this.selectedWalletBorderColor);
    }
    
    public Color getTopMenuHoverColor() {
        return colorFromHex(this.topMenuHoverColor);
    }
    
    public Color getMainTextColor() {
        return colorFromHex(this.mainTextColor);
    }
    
    public Color getSecondTextColor() {
        return colorFromHex(this.secondTextColor);
    }
    
    public Color getThirdTextColor() {
        return colorFromHex(this.thirdTextColor);
    }
    
    public Color getTableHeaderTextColor() {
        return colorFromHex(this.tableHeaderTextColor);
    }
    
    public Color getTableGridColor() {
        return colorFromHex(this.tableGridColor);
    }
    
    public Color getDropdownHoverColor() {
        return colorFromHex(this.dropdownHoverColor);
    }
    
    public Color getInputBackgroundColor() {
        return colorFromHex(this.inputBackgroundColor);
    }
    
    public Color getScrollbarTrackColor() {
        return colorFromHex(this.scrollbarTrackColor);
    }
    
    public Color getScrollbarThumbColor() {
        return colorFromHex(this.scrollbarThumbColor);
    }
    
    public Color getInventoryBackgroundColor() {
        return colorFromHex(this.inventoryBackgroundColor);
    }
    
    public Color getErrorColor() {
        return colorFromHex(this.errorColor);
    }
    
    public Color getPanelBackgroundColor() {
        return colorFromHex(this.panelBackgroundColor);
    }
    
    public Color getInactiveWalletBackgroundColor() {
        return colorFromHex(this.inactiveWalletBackgroundColor);
    }
    
    public Color getActiveWalletBackgroundColor() {
        return colorFromHex(this.activeWalletBackgroundColor);
    }
    
    public Color getProgressbarBackgroundColor() {
        return colorFromHex(this.progressbarBackgroundColor);
    }
    
    public Color getProgressbarColor() {
        return colorFromHex(this.progressbarColor);
    }
    
    public Color getPrimaryButtonColor() {
        return colorFromHex(this.primaryButtonColor);
    }
    
    public Color getSecondaryButtonColor() {
        return colorFromHex(this.secondaryButtonColor);
    }
    
    public Color getDisabledButtonColor() {
        return colorFromHex(this.disabledButtonColor);
    }
    
    public Color getDropfilesBackgroundColor() {
        return colorFromHex(this.dropfilesBackgroundColor);
    }
    
    public Color getHyperlinkColor() {
        return colorFromHex(this.hyperlinkColor);
    }
    
    public Color getSettingsMenuHoverColor() {
        return colorFromHex(this.settingsMenuHoverColor);
    }
    
    public Color getSettingsMenuBackgroundColor() {
        return colorFromHex(this.settingsMenuBackgroundColor);
    }
    
    public Color getTitleTextColor() {
        return colorFromHex(this.titleTextColor);
    }
    
    
    
    
    
    
    
    
    
    public URL getImgIcon() {
        return getAssetPathByName("icon");
    }
    
    public URL getImgDropdownArrow() {
        return getAssetPathByName("dropdownArrow");
    }
    
    public URL getImgLookingGlass() {
        return getAssetPathByName("lookingGlass");
    }
    
    public URL getImgEye() {
        return getAssetPathByName("eye");
    }
    
    public URL getImgToggleYes() {
        return getAssetPathByName("toggleyes");
    }
    
    public URL getImgToggleNo() {
        return getAssetPathByName("toggleno");
    }
    
    public URL getImgSettingsIcon() {
        return getAssetPathByName("settingsIcon");
    }
    
    public URL getImgSupportIcon() {
        return getAssetPathByName("supportIcon");
    }
    
    public URL getImgLogo() {
        return getAssetPathByName("logo");
    }
    
    public URL getImgLogoText() {
        return getAssetPathByName("logoText");
    }
    
    public URL getImgDepositIcon() {
        return getAssetPathByName("depositIcon");
    }
    
    public URL getImgDepositIconHover() {
        return getAssetPathByName("depositIconHover");
    }
    
    public URL getImgWithdrawIcon() {
        return getAssetPathByName("withdrawIcon");
    }
    
    public URL getImgWithdrawIconHover() {
        return getAssetPathByName("withdrawIconHover");
    }
    
    public URL getImgTransferIcon() {
        return getAssetPathByName("transferIcon");
    }
    
    public URL getImgTransferIconHover() {
        return getAssetPathByName("transferIconHover");
    }
    public URL getImgCoinsIcon() {
        return getAssetPathByName("coinsIcon");
    }
    
    public URL getImgBackgroundImage() {
        return getAssetPathByName("backgroundImage");
    }
    
    public URL getImgSupportHtmlIcon() {
        return getAssetPathByName("supportHtmlIcon");
    }
    
    public URL getImgSupportTimeIcon() {
        return getAssetPathByName("supportTimeIcon");
    }
    
    public URL getImgSupportPhoneIcon() {
        return getAssetPathByName("supportPhoneIcon");
    }
    
    public URL getImgSupportEmailIcon() {
        return getAssetPathByName("supportEmailIcon");
    }
    
    public URL getImgSupportPortalIcon() {
        return getAssetPathByName("supportPortalIcon");
    }

    public URL getImgCoinsInventoryIcon() {
        return getAssetPathByName("coinsInventoryIcon");
    }

    public URL getImgArrowLeftIcon() {
        return getAssetPathByName("arrowLeft");
    }
    
    public URL getImgArrowRightIcon() {
        return getAssetPathByName("arrowRight");
    }
    
    public URL getImgWalletLocalIcon() {
        return getAssetPathByName("walletLocalIcon");
    }
    
    public URL getImgWalletSkyIcon() {
        return getAssetPathByName("walletSkyIcon");
    }
    
    public URL getImgVaultIcon() {
        return getAssetPathByName("vaultIcon");
    }
    
    public URL getImgLockIcon() {
        return getAssetPathByName("lockIcon");
    }
    
    public URL getImgLockIconActive() {
        return getAssetPathByName("lockIconActive");
    }
    
    public URL getImgEmailIcon() {
        return getAssetPathByName("emailIcon");
    }
    
    public URL getImgEmailIconActive() {
        return getAssetPathByName("emailIconActive");
    }
    
    public URL getImgCloudIcon() {
        return getAssetPathByName("cloudIcon");
    }
    
    public URL getImgCloudIconActive() {
        return getAssetPathByName("cloudIconActive");
    }
    
    
    public String getTerms() {
        return getAssetPathByNameText("terms");
    }
    
    
    
    
    
    
    
    public String getSupportEmail() {
        return this.supportEmail;
    }
    
    public String getSupportPage() {
        return this.supportPage;
    }
    
    public String getSupportTime() {
        return this.supportTime;
    }
    
    public String getSupportPhone() {
        return this.supportPhone;
    }
    
    public String getSupportPortal() {
        return this.supportPortal;
    }
    

    
    public String getTitle(String version) {
        if (version == null)
            return this.title;
        
        return this.title + " " + this.getResultingVersion(version);
    }
    
    
    class dlResult {
        String name;
        byte[] data;
    }
}
