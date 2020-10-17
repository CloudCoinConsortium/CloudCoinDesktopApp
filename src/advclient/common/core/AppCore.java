package global.cloudcoin.ccbank.core;

import advclient.AppUI;
import advclient.common.core.Validator;
import global.cloudcoin.ccbank.ServantManager.ServantManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import static java.lang.Integer.max;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AppCore {

    static private String ltag = "AppCore";

    static private File rootPath;
    public static GLogger logger;

    static private ExecutorService service;
    
    static public String raidaErrText = "Cannot Connect to the RAIDA. "
            + "Check that local routers are not blocking your connection.";
    
    public static int currentMode;
    
    public static HashMap<String, String> coinsList;
    
    
    static public void initFilter(String file)  {
        logger.debug(ltag, "Reading filter");
        
        AppCore.coinsList = new HashMap<String, String>();
        int c = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                int snc = Integer.parseInt(line);
                CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, snc);
                if (cc.getDenomination() == 0) {
                    logger.debug(ltag, "Warning: invalid SN in the filter file: " + line);
                    continue;
                }
                
                AppCore.coinsList.put(line, line);
                c++;
                
                // process the line.
            }
        } catch (Exception e) {
            AppCore.coinsList = null;
        }
        
        if (c == 0) {
            logger.debug(ltag, "No coins collected in filter");
            AppCore.coinsList = null;
            return;
        }
        
        
        logger.debug(ltag, c + " coins collected in filter");
    }

    static public boolean createDirectory(String dirName) {
        String idPath;

        idPath = rootPath + File.separator + dirName;
        
        File idPathFile = new File(idPath);
        if (idPathFile.exists())
            return true;
        
        logger.info(ltag, "Creating " + idPath);
        try {
            if (!idPathFile.mkdirs()) {
                logger.error(ltag, "Can not create directory " + dirName);
                return false;
            }
        } catch (SecurityException e) {
            logger.error(ltag, "Failed to create dir: " + e.getMessage());
            return false;
        }

        logger.info(ltag, "CREATED " + idPath);
        return true;
    }

    static public boolean createDirectoryPath(String path) {
        File idPathFile = new File(path);
        if (idPathFile.exists())
            return false;
        
        if (!idPathFile.mkdirs()) {
            logger.error(ltag, "Can not create directory " + path);
            return false;
        }
        
        return true;
    }
   
    static public boolean initFolders(File path) {
        try {
            rootPath = path;

            if (!createDirectory(Config.DIR_ROOT))
                return false;
        
            rootPath = new File(path, Config.DIR_ROOT);

            if (!createDirectory(Config.DIR_TEMPLATES))
                return false;
        
            if (!createDirectory(Config.DIR_ACCOUNTS))
                return false;
        
            if (!createDirectory(Config.DIR_ID))
                return false;
        
            if (!createDirectory(Config.DIR_MAIN_LOGS))
                return false;
        
            if (!createDirectory(Config.DIR_MAIN_TRASH))
                return false;
        
            if (!createDirectory(Config.DIR_BACKUPS))
                return false;
        
            if (!createDirectory(Config.DIR_DOWNLOADS))
                return false;

            if (!createDirectory(Config.DIR_EMAIL_TEMPLATES))
                return false;
        
            if (!createDirectory(Config.DIR_BRANDS))
                return false;
            
            if (!createDirectory(Config.DIR_CLOUDBANK))
                return false;
            
            if (!createDirectory(Config.DIR_RECOVERY))
                return false;
            
            if (!createDirectory(Config.DIR_RECOVERED))
                return false;
            
            if (!createDirectory(Config.DIR_PAID_FOR_RECOVERED))
                return false;
            
            if (!createDirectory(Config.DIR_PARTIAL))
                return false;
            
            if (!createDirectory(Config.DIR_CLOUDBANK + File.separator + Config.DIR_CLOUDBANK_KEYS))
                return false;
            
        } catch (Exception e) {
            logger.error(ltag, "Exception " + e.toString());
            return false;
        }
        
        return true;
    }
   
    static public void initUserFolders(String user) throws Exception {
        String[] folders = new String[]{
            Config.DIR_BANK,
            Config.DIR_COUNTERFEIT,
            Config.DIR_CONFIG,
            Config.DIR_DEPOSIT,
            Config.DIR_DETECTED,
            Config.DIR_EXPORT,
            Config.DIR_EMAIL_OUT,
            Config.DIR_EMAIL_SENT,
            Config.DIR_FRACKED,
            Config.DIR_OTHER,
            Config.DIR_GALLERY,
            Config.DIR_IMPORT,
            Config.DIR_IMPORTED,
            Config.DIR_LOGS,
            Config.DIR_LOST,
            Config.DIR_MIND,
            Config.DIR_PAYFORWARD,
            Config.DIR_PREDETECT,
            Config.DIR_RECEIPTS,
            Config.DIR_REQUESTS,
            Config.DIR_REQUESTRESPONSE,
            Config.DIR_SENT,
            Config.DIR_SUSPECT,
            Config.DIR_TRASH,
            Config.DIR_TRUSTEDTRANSFER,
            Config.DIR_VAULT
        };

        createDirectory(Config.DIR_ACCOUNTS + File.separator + user);

        for (String dir : folders) {
            createDirectory(Config.DIR_ACCOUNTS + File.separator + user + File.separator + dir);
        }
    }

    static public String getRootPath() {
       return rootPath.toString();
    }
  
    static public String getUserConfigDir(String user) {
       File f;

       f = new File(rootPath, Config.DIR_ACCOUNTS);
       f = new File(f, user);
       f = new File(f, Config.DIR_CONFIG);

       return f.toString();
    }
    
    static public String getRecoveryDir() {
       File f = new File(rootPath, Config.DIR_RECOVERY);
       
       return f.toString();
    }
    
    static public String getRecoveredDir() {
       File f = new File(rootPath, Config.DIR_RECOVERED);
       
       return f.toString();
    }
    
    static public String getPaidRecoveredDir() {
       File f = new File(rootPath, Config.DIR_PAID_FOR_RECOVERED);
       
       return f.toString();
    }

    static public String getBackupDir() {
       File f = new File(rootPath, Config.DIR_BACKUPS);
       
       return f.toString();
    }
   
    static public String getBrandsDir() {
        File f = new File(rootPath, Config.DIR_BRANDS);
       
        return f.toString();
    }
    
    static public String getCloudbankDir() {
       File f = new File(rootPath, Config.DIR_CLOUDBANK);
       
       return f.toString();
    }
    
    static public String getCloudbankKeysDir() {
       File f = new File(rootPath, Config.DIR_CLOUDBANK + File.separator + Config.DIR_CLOUDBANK_KEYS);
       
       return f.toString();
    }
    
 
    static public String getDownloadsDir() {
       File f = new File(rootPath, Config.DIR_DOWNLOADS);
       
       return f.toString();
   }
   
    static public String getTemplateDir() {
       File f = new File(rootPath, Config.DIR_TEMPLATES);
       
       return f.toString();
    }
   
    static public String getPartialDir() {
       File f = new File(rootPath, Config.DIR_PARTIAL);
       
       return f.toString();
    }
   
   static public String getIDDir() {
       File f = new File(rootPath, Config.DIR_ID);
       
       return f.toString();
   }
   
   static public String getMainTrashDir() {
       File f = new File(rootPath, Config.DIR_MAIN_TRASH);
       
       return f.toString();
   }
    
   static public void initPool() {
       service = Executors.newFixedThreadPool(Config.THREAD_POOL_SIZE);
   }

   static public void shutDownPool() {
       service.shutdown();
   }

   static public ExecutorService getServiceExecutor() {
       return service;
   }

   static public String getLogDir() {
       File f;

       f = new File(rootPath, Config.DIR_MAIN_LOGS);

       return f.toString();
   }

   static public String getPrivateLogDir(String user) {
       File f;

       f = new File(rootPath, Config.DIR_ACCOUNTS);
       f = new File(f, user);
       f = new File(f, Config.DIR_LOGS);

       return f.toString();
   }

   static public String getUserDir(String folder, String user) {
        File f;

        f = new File(rootPath, Config.DIR_ACCOUNTS);
        f = new File(f, user);
        f = new File(f, folder);

        return f.toString();
   }

   static public String getRootUserDir(String user) {
       File f;

       f = new File(rootPath, Config.DIR_ACCOUNTS);
       f = new File(f, user);
        
       return f.toString();
   }
   
   static public String formatNumber(int number) {
       NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
       DecimalFormat formatter = (DecimalFormat) nf;
       formatter.applyPattern("#,###,###");

       return formatter.format(number);
   }

   static public int getTotal(int[] counters) {
       return counters[Config.IDX_1] + counters[Config.IDX_5] * 5 +
                counters[Config.IDX_25] * 25 + counters[Config.IDX_100] * 100 +
                counters[Config.IDX_250] * 250;
    }

    static public int[] getDenominations() {
        int[] denominations = {1, 5, 25, 100, 250};

        return denominations;
    }
    
    static public boolean moveToFolderNewName(String fileName, String folder, String user, String newFileName) {
        logger.info(ltag, "Moving to " + folder + " -> " + fileName + " new " + newFileName);
        
        try {
            File fsource = new File(fileName);
            String target;
            if (user != null)
                target = AppCore.getUserDir(folder, user) + File.separator + newFileName;
            else 
                target = folder + File.separator + newFileName;
            
            File ftarget = new File(target);
            if (!fsource.renameTo(ftarget)) {
                logger.error(ltag, "Failed to rename file " + fileName + " to " + ftarget.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file: " + e.getMessage());
            return false;
        }
        
        return true;
        
    }

    static public boolean moveToFolderNoTs(String fileName, String folder, String user) {
        return moveToFolderNoTs(fileName, folder, user, false);
    }
    
    static public boolean moveToFolderNoTs(String fileName, String folder, String user, boolean isOverwrite) {
        logger.info(ltag, "Moving no Ts to " + folder + " -> " + fileName);

        try {
            File fsource = new File(fileName);
            String target = AppCore.getUserDir(folder, user) + File.separator + fsource.getName();

            File ftarget = new File(target);
            if (ftarget.exists()) {
                if (isOverwrite) {
                    logger.info(ltag, "Overwriting file: " + target);
                    ftarget.delete();
                } else {
                    logger.error(ltag, "File exists. Leaving " + fileName);
                    return false;
                }
            }

            if (!fsource.renameTo(ftarget)) {
                logger.error(ltag, "Failed to rename file " + fileName + " to " + target);
                return false;
            }
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file: " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    static public boolean updatePNG(String oldFile, String newFile) {
        logger.info(ltag, "Update PNG now (dummy: ans already set in FrackFixer): " + oldFile + " -> " + newFile);
        /*
        String context = AppCore.loadFile(oldFile);
        if (context == null) {
            return false;
        }
        */
        
        //AppCore.deleteFile(newFile);
        
        return true;
    }

    static public boolean renameFile(String oldFile, String newFile) {
        logger.info(ltag, "Renaming " + oldFile + " -> " + newFile);
        try {
            File fsource = new File(oldFile);
            File ftarget = new File(newFile);
            if (ftarget.exists()) {
                logger.error(ltag, "Target File exists. Leaving");
                return false;
            }

            if (!fsource.renameTo(ftarget)) {
                logger.error(ltag, "Failed to rename file");
                return false;
            }
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file: " + e.getMessage());
            return false;
        }
        
        return true;
    }

    static public void moveToTrash(String fileName, String user) {
        moveToFolderNoTs(fileName, Config.DIR_TRASH, user, true);
    }

    static public boolean moveToBank(String fileName, String user) { 
        return moveToFolderNoTs(fileName, Config.DIR_BANK, user); 
    }

    static public void moveToImported(String fileName, String user) {
        moveToFolderNoTs(fileName, Config.DIR_IMPORTED, user, true);
    }
    
    static public void moveToSent(String fileName, String user) {
        moveToFolderNoTs(fileName, Config.DIR_SENT, user, true);
    }

    static public boolean copyFile(InputStream is, String fdst) {
        File dest = new File(fdst);
        OutputStream os = null;
        
        try {
            os = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error(ltag, "Failed to copy file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (is != null)
                    is.close();

                if (os != null)
                    os.close();

            } catch (IOException e) {
                logger.error(ltag, "Failed to copy file: " + e.getMessage());
            }
        }
        
        return true;
    }
    
    static public boolean copyFile(String fsrc, String fdst) {
        File source = new File(fsrc);
        File dest = new File(fdst);
        InputStream is = null;
        OutputStream os = null;
        
        logger.debug(ltag, "Copy " + fsrc + " to " + fdst);
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error(ltag, "Failed to copy file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (is != null)
                    is.close();

                if (os != null)
                    os.close();

            } catch (IOException e) {
                logger.error(ltag, "Failed to finally copy file: " + e.getMessage());
            }
        }

        return true;
    }

    static public String loadFile(String fileName) {
        String jsonData = "";
        BufferedReader br = null;
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            jsonData = new String(encoded);
            
        } catch (IOException e) {
            logger.error(ltag, "Failed to load file: " + e.getMessage());
            return null;
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                logger.error(ltag, "Failed to finally load file: " + e.getMessage());
                return null;
            }
        }

        return jsonData.toString();
    }

    static public byte[] loadFileToBytes(String path) {
        byte[] getBytes = {};
        try {
            File file = new File(path);
            getBytes = new byte[(int) file.length()];
            InputStream is = new FileInputStream(file);
            is.read(getBytes);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getBytes;
    }

    static public String loadFileFromInputStream(InputStream is) {
        StringBuilder out = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }          
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }
    
    static public boolean saveFile(String path, String data) {
        return saveFileAppend(path, data, false);
    }
    
    static public boolean saveFileAppend(String path, String data, boolean isAppend) {
        File f = new File(path);
        if (f.exists() && !isAppend) {
            logger.error(ltag, "File " + path + " already exists");
            return false;
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path, isAppend));
            writer.write(data);
        } catch (IOException e){
            logger.error(ltag, "Failed to write file: " + e.getMessage());
            return false;
        } finally {
            try{
                if (writer != null)
                    writer.close();
            } catch (IOException e){
                logger.error(ltag, "Failed to close buffered writer");
                return false;
            }
        }

        return true;
    }

    static public boolean saveFileFromBytes(String path, byte[] bytes) {
       try {
           File file = new File(path);
           if (file.exists()) {
               logger.error(ltag, "File exists: " + path);
               return false;
           }

           FileOutputStream fos = new FileOutputStream(file);
           fos.write(bytes);
           fos.close();
       } catch (IOException e) {
           logger.error(ltag, "Failed to write file: " + e.getMessage());
           return false;
       }

       return true;
    }

    static public void deleteFile(String path) {
        File f = new File(path);

        logger.debug(ltag, "Deleting " + path);
        f.delete();
    }
    
    static public int getFilesCountPath(String path) {
        File rFile;
        int rv;

        try {
            rFile = new File(path);
            rv = 0;
            for (File file: rFile.listFiles()) {
                if (file.isDirectory())
                    continue;
            
                if (!AppCore.hasCoinExtension(file))
                    continue;
                
                rv++;
            }               
        } catch (Exception e) {
           logger.error(ltag, "Failed to read directory: " + e.getMessage());
           return 0;
        }

        return rv;
    }

    public static String[] getDirsInDir(String directory) {
        String[] rv;
        int c = 0;

        File dirObj = new File(directory);
        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory())
                continue;
            
            c++;
        }
        
        rv = new String[c];
        c = 0;
        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory())
                continue;
            
            rv[c++] = file.getAbsolutePath();
        }
        
        return rv;       
    }
    
    
    static public int getFilesCount(String dir, String user) {
        String path = getUserDir(dir, user);
        File rFile;
        int rv;

        try {
            rFile = new File(path);
            rv = 0;
            for (File file: rFile.listFiles()) {
                if (file.isDirectory())
                    continue;
            
                if (!AppCore.hasCoinExtension(file))
                    continue;
                
                rv++;
            }               
        } catch (Exception e) {
           logger.error(ltag, "Failed to read directory: " + e.getMessage());
           return 0;
        }

        return rv;
    }

    static public String getMD5(String data) {
        byte[] bytesOfMessage = data.getBytes();
        byte[] digest;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            digest = md.digest(bytesOfMessage);
        } catch (NoSuchAlgorithmException e) {
            logger.error(ltag, "No such algorithm MD5: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error(ltag, "MD5 error: " + e.getMessage());
            return null;
        }

        return toHexString(digest);
    }

    static public String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    static public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                    Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }


    static public String generateHex() {
        String AB = "0123456789ABCDEF";

        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));

        return sb.toString();
    }


    public static String padString(String string, int length, char padding) {
        return String.format("%" + length + "s", string).replace(' ', padding);
    }

    public static int charCount(String pown, char character) {
        return pown.length() - pown.replace(Character.toString(character), "").length();
    }
    
    public static String[] getFilesInDir(String dir, String user) {
        String path;
        if (user != null)
            path = AppCore.getUserDir(dir, user);
        else
            path = dir;
        
        String[] rv;
        int c = 0;

        File dirObj = new File(path);
        if (!dirObj.exists()) {
            return new String[0];
        }
        
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            c++;
        }
        
        rv = new String[c];
        c = 0;
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            rv[c++] = file.getName();
        }
        
        return rv;       
    }
    
    public static String[] getDirs() {
        String[] rv;
        int c = 0;

        File dirObj = new File(rootPath + File.separator + Config.DIR_ACCOUNTS);
        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory())
                continue;
            
            c++;
        }
        
        rv = new String[c];
        c = 0;
        for (File file: dirObj.listFiles()) {
            if (!file.isDirectory())
                continue;
            
            rv[c++] = file.getName();
        }
        
        return rv;       
    }
    
    
    public static void copyTemplatesFromJar() {
        int d;
        String templateDir;

        String[] templates = new String[] {
            "jpeg1.jpg",
            "jpeg5.jpg",
            "jpeg25.jpg",
            "jpeg100.jpg",
            "jpeg250.jpg",
            Config.PNG_TEMPLATE_NAME
        };
        
	templateDir = AppCore.rootPath + File.separator + Config.DIR_TEMPLATES;

        for (int i = 0; i < templates.length; i++) {
            String fileName = templates[i];
            
            URL u = AppCore.class.getClassLoader().getResource("resources/" + fileName);
            if (u == null) {
                logger.debug(ltag, "Failed to find resource " + fileName);
                continue;
            }
            
            String url;
            try {
                url = URLDecoder.decode(u.toString(), "UTF-8");
            }  catch (UnsupportedEncodingException e) {
                logger.error(ltag, "Failed to decode url");
                return;
            }

            int bang = url.indexOf("!");
            String JAR_URI_PREFIX = "jar:file:";
            JarFile jf;
                
            logger.debug(ltag, "jurl " + url);
            try {
                if (url.startsWith(JAR_URI_PREFIX) && bang != -1) {
                    jf = new JarFile(url.substring(JAR_URI_PREFIX.length(), bang)) ;
                } else if (url.startsWith("file:/")) {
                    String file = url.substring(6, url.length());
                    logger.debug(ltag, "template file " + file);
                    String dst = templateDir + File.separator + fileName;
                    
                    File f = new File(dst);
                    if (f.exists())
                        continue;
                    
                    AppCore.copyFile(file, dst);                    
                    continue;
                } else {
                    logger.error(ltag, "Invalid jar");
                    return;
                }
                
                for (Enumeration<JarEntry> entries = jf.entries(); entries.hasMoreElements();) {
                    JarEntry entry = entries.nextElement();

                    if (entry.getName().equals("resources/" + fileName)) {
                        InputStream in = jf.getInputStream(entry);
                        String dst = templateDir + File.separator + fileName;
                        
                        File f = new File(dst);
                        if (f.exists())
                            continue;
                        
                        AppCore.copyFile(in, dst);
                    }
                }
            } catch (IOException e) {
                logger.error(ltag, "Failed to copy templates: " + e.getMessage());
                return ;
            }                      
        }
    }
    
    public static String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd hh:mma");
	Date date = new Date();
        
        String strDate = dateFormat.format(date).replaceAll("AM$", "am");
        strDate = strDate.replaceAll("PM$", "pm");
        
        return strDate;
    }
    
    
    public static String getCurrentBackupDir(String broot, String user) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-d h-mma", Locale.US);
        Date date = new Date();
        
        String dsuffix = dateFormat.format(date);
        
        String bdir = broot + File.separator + user + File.separator + "CloudCoinBackup-" + dsuffix;
        
        return bdir;
    }
    
    
    public static String getDate(String ts) {
        int lts;
        
        try {
            lts = Integer.parseInt(ts);
        } catch (NumberFormatException e) {
            logger.error(ltag, "Failed to parse ts: " + ts);
            return "";
        }
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-d h-mma");
        Date date = new Date((long) lts * 1000);
        
        String dateTime = dateFormat.format(date);
        
      
        return dateTime;
    }
    
    
    public static CloudCoin getCoin(String path) {
        CloudCoin cc;
        try {
            cc = new CloudCoin(path);
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse coin: " + path + " error: " + e.getMessage());
            return null;
        }
        
        return cc;
    }
    
    public static CloudCoin findCoinBySN(String dir, String user, int sn) {
        String dirPath = AppCore.getUserDir(dir, user);
        logger.debug(ltag, "Looking for sn " + sn + " into dir: " + dirPath);    
        CloudCoin cc;

        File dirObj = new File(dirPath);
        if (dirObj.listFiles() == null) 
            return null;
        
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            if (!AppCore.hasCoinExtension(file))
                continue;
            
            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                continue;
            }
            
            if (cc.sn == sn)
                return cc;
        }
        
        return null;
    }
    
    public static String getReceiptHtml(String hash, String user) {
        if (hash.equals("dummy") || hash.equals("COUNTERFEIT") || hash.isEmpty() || hash.equals("AutoAdjusted"))
            return null;
        
        String receiptsFile = AppCore.getUserDir(Config.DIR_RECEIPTS, user);
        receiptsFile += File.separator + hash + ".txt";
                
        String data = AppCore.loadFile(receiptsFile);
        if (data == null) {
            logger.error(ltag, "File " + receiptsFile + " failed to open");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try {
            JSONObject o = new JSONObject(data);
                    
            String rId = o.getString("receipt_id");
            String time = o.getString("time");
            
            int total_authentic = o.optInt("total_authentic");
            int total_fracked = o.optInt("total_fracked");
            int total_counterfeit = o.optInt("total_counterfeit");
            int total_lost = o.optInt("total_lost");
            int total_unchecked = o.optInt("total_unchecked");
            int total_prevs = o.optInt("prev_imported");
            int total_value = o.optInt("total_value");
            
            
            sb.append("<p>Receipt <b>#");
            sb.append(rId);
            sb.append("</b></p><br><br><p>");
            sb.append(time);
            sb.append("</p><br><br>");
            sb.append("<p>Total Authentic: <b>");
            sb.append(total_authentic);
            sb.append("</b></p><p>Total Fracked: <b>");
            sb.append(total_fracked);
            sb.append("</b></p><p>Total Counterfeit: <b>");
            sb.append(total_counterfeit);
            sb.append("</b></p><p>Total Lost: <b>");
            sb.append(total_lost);
            sb.append("</b></p><p>Total Unchecked: <b>");
            sb.append(total_unchecked);
            sb.append("</b></p><p>Total Previously Deposited: <b>");
            sb.append(total_prevs);
            sb.append("</b></p><p>Total Value: <b>");
            sb.append(total_value);
            sb.append("</b></p><br><br><p>Details:</p><br>");
            
            JSONArray a = o.getJSONArray("receipt_detail");
            
            for (int i = 0; i < a.length(); i++) {
                JSONObject io = a.getJSONObject(i);
                
                String sn = io.optString("sn");
                if (sn == "") {
                    sn = io.getString("nn.sn");
                }
                
                String status = io.getString("status");
                String pown = io.getString("pown");
                String note = io.getString("note");

                sb.append("<p>sn: ");
                sb.append(sn);
                sb.append("</p><p>status: ");
                sb.append(status);
                sb.append("</p><p>pown: ");
                sb.append(pown);
                sb.append("</p><p>note: ");
                sb.append(note);
                sb.append("</p><br>");                
            }
                  
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse receipt: " + e.getMessage());
            return null;
        }
        
        return sb.toString();
    }
    
    static public String getLogPath() {
        return getLogDir() + File.separator + Config.MAIN_LOG_FILENAME;
    }
    
    static public boolean moveFolderToTrash(String folder) {
        String path = AppCore.getRootUserDir(folder);
        
        File fsrc = new File(path);
        if (!fsrc.exists()) {
            logger.error(ltag, "Path " + path + " doesn't exist");
            return false;
        }
        
        String mainTrashDir = AppCore.getMainTrashDir() + File.separator + 
                folder + "-" + System.currentTimeMillis();
        
        File fdst = new File(mainTrashDir);
        
        logger.debug(ltag, "Deleting dir " + path + " to " + mainTrashDir);
        try {
            Files.move(fsrc.toPath(), fdst.toPath(),  StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error(ltag, "Failed to move dir " + e.getMessage());
            return false;
        }
        return true;
    }
    
    static public void cleanPreauthDirs(String user) {
        moveFolderContentsToTrash(Config.DIR_SUSPECT, user);
        moveFolderContentsToTrash(Config.DIR_DETECTED, user);
        moveFolderContentsToTrash(Config.DIR_IMPORT, user);        
    }
    
    static public void moveFolderContentsToTrash(String folder, String user) {
        String path = AppCore.getUserDir(folder, user);
        
        File dirObj = new File(path);
        if (!dirObj.exists()) {
            logger.error(ltag, "Path " + path + " doesn't exist");
            return;
        }
        
        for (File file: dirObj.listFiles()) {
            logger.debug(ltag, "Move to trash " + file.getAbsolutePath());
            AppCore.moveToTrash(file.getAbsolutePath(), user);
        }       
    }
 
    public static int maxCoinsWorkAround(int maxCoins) {
        String javaVersion = System.getProperty("java.version");
        
        logger.debug(ltag, "Java version: " + javaVersion);
        
        if (javaVersion.equals("1.8.0_211")) {
            logger.debug(ltag, "MaxCoins WorkAround applied");
            return 20;
        }

        return maxCoins;
    }
    
    
    public static void logSystemInfo(String version) {
        logger.info(ltag, "CloudCoin Wallet v." + version);
        
        String javaVersion = System.getProperty("java.runtime.version");
        String javaName = System.getProperty("java.runtime.name");
        
        String osName = System.getProperty("os.name");
        
        int cpus = Runtime.getRuntime().availableProcessors();
        long totalMemory =  Runtime.getRuntime().totalMemory();
        long freeMemory =  Runtime.getRuntime().freeMemory();
        
        totalMemory /= (1024 * 1024);
        freeMemory /= (1024 * 1024);
        
        logger.info(ltag, "JAVA " + javaName + " " + javaVersion);
        logger.info(ltag, osName);
        logger.info(ltag, "CPUS: " + cpus + " Memory for JVM, (free/avail): " + freeMemory + "/" + totalMemory + " MB");
    }
    
    public static String calcCoinsFromFilenames(ArrayList<String> files) {
        int total = 0;
        
        for (String filepath : files) {
            File f = new File(filepath);
            String filename = f.getName();
            
            String[] parts = filename.split("\\.");
            if (parts.length < 3)
                return "?";
            
            int ltotal;
            try {
                ltotal = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return "?";
            }
           
            String identifier = parts[1].toLowerCase();
            if (!identifier.equals("cloudcoin") && !identifier.equals("cloudcoins"))
                return "?";
                     
            total += ltotal;
        }
        
        String totalCoins = AppCore.formatNumber(total);
        
        return totalCoins;
    }
    
    public static String getMS(int ms) {
        double dms = (double) ms / 1000;
        String s = String.format(Locale.US, "%.2f", dms);

        if (s.charAt(0) == '0')
            s = s.substring(1, s.length());

        return s + " sec";
    }

    public static String getRAIDAString(int idx) {
        String sidx;

        if (idx < 10)
            sidx = "0" + idx;
        else
            sidx = "" + idx;

        String[] countries = {
            "Australia",
            "Macedonia",
            "Philippines",
            "Serbia",
            "Switzerland",
            "South Korea",
            "Japan",
            "UK",
            "India",
            "India",
            "Germany",
            "USA",
            "India",
            "Taiwan",
            "Russia",
            "Russia",
            "UK",
            "Singapore",
            "USA",
            "Argentina",
            "France",
            "India",
            "USA",
            "Germany",
            "Canada"
        };

        if (idx > countries.length) {
            return sidx + " RAIDA";
        }

        return sidx + " " + countries[idx];

    }
    
    public static int getCounterfeitCount(CloudCoin cc) {
        int c = 0;

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (cc.getDetectStatus(i) == CloudCoin.STATUS_FAIL)
                c++;
        }
        
        cc.setPownStringFromDetectStatus();
        logger.debug(ltag, "Counterfeit count " + c + " cc " + cc.sn + " pown=" + cc.getPownString());
        
        return c;
    }

    public static int getErrorCount(CloudCoin cc) {
        int error = 0;

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (cc.getDetectStatus(i) == CloudCoin.STATUS_ERROR || cc.getDetectStatus(i) == CloudCoin.STATUS_UNTRIED)
                error++;
        }
        
        cc.setPownStringFromDetectStatus();
        logger.debug(ltag, "Error count " + error + " cc " + cc.sn + " pown=" + cc.getPownString());
        
        return error;
    }
    
    public static int getPassedCount(CloudCoin cc) {
        int passed = 0;

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            if (cc.getDetectStatus(i) == CloudCoin.STATUS_PASS)
                passed++;
        }
        
        cc.setPownStringFromDetectStatus();
        logger.debug(ltag, "Passed count " + passed + " cc " + cc.sn + " pown=" + cc.getPownString());
        
        return passed;
    }
    
    public static String getMailConfigFilename() {
        return rootPath + File.separator + Config.MAIL_CONFIG_FILENAME;
    }
    
    
    public static void readConfig() {
        String globalConfigFilename = rootPath + File.separator + Config.GLOBAL_CONFIG_FILENAME;
        
        logger.debug(ltag, "Reading config file " + globalConfigFilename);
        
        String data = AppCore.loadFile(globalConfigFilename);
        if (data == null) {
            logger.debug(ltag, "Failed to read config file. Maybe it doesn't exist yet");
            return;
        }
        
        String os;
        int oi;
        boolean ob;
        try {
            JSONObject o = new JSONObject(data);
                    
            oi = o.optInt("echo_timeout", -1);
            if (oi != -1) {
                logger.debug(ltag, "Echo timeout: " + oi);
                Config.ECHO_TIMEOUT = oi;
            }
            
            oi = o.optInt("read_timeout", -1);
            if (oi != -1) {
                logger.debug(ltag, "Read timeout: " + oi);
                Config.READ_TIMEOUT = oi;
            }
            
            oi = o.optInt("detect_timeout", -1);
            if (oi != -1) {
                logger.debug(ltag, "Detect timeout: " + oi);
                Config.MULTI_DETECT_TIMEOUT = oi;
            }
            
            oi = o.optInt("fix_timeout", -1);
            if (oi != -1) {
                logger.debug(ltag, "Fix timeout: " + oi);
                Config.FIX_FRACKED_TIMEOUT = oi;
            }
            
            oi = o.optInt("max_coins", -1);
            if (oi != -1) {
                logger.debug(ltag, "Max coins: " + oi);
                Config.DEFAULT_MAX_COINS_MULTIDETECT = oi;
            }
            
            os = o.optString("export_dir");
            if (os != null) {
                logger.debug(ltag, "Export dir: " + os);
                Config.DEFAULT_EXPORT_DIR = os;
            }

            os = o.optString("deposit_dir");
            if (os != null) {
                logger.debug(ltag, "Deposit dir: " + os);
                
                Config.DEFAULT_DEPOSIT_DIR = os;
            }
            
            os = o.optString("ddnssn_server");
            if (os != null) {
                logger.debug(ltag, "DDNSSN Server: " + os);
                Config.DDNSSN_SERVER = os;
            }
            
            os = o.optString("cloudbank_lwallet");
            if (os != null) {
                logger.debug(ltag, "CloudBank Local Wallet: " + os);
                Config.CLOUDBANK_LWALLET = os;
            }
            
            os = o.optString("cloudbank_lwallet_password");
            if (os != null) {
                logger.debug(ltag, "CloudBank Local Wallet Password: " + os);
                Config.CLOUDBANK_LWALLET_PASSWORD = os;
            }
            
            os = o.optString("cloudbank_rwallet");
            if (os != null) {
                logger.debug(ltag, "CloudBank Sky Wallet: " + os);
                Config.CLOUDBANK_RWALLET = os;
            }
            
            ob = o.optBoolean("cloudbank_enabled");
            if (ob != false) {
                logger.debug(ltag, "CloudBank Enabled: " + ob);
                Config.CLOUDBANK_ENABLED = ob;
            }
            
            oi = o.optInt("cloudbank_port");
            if (oi != -1) {
                logger.debug(ltag, "CloudBank Enabled: " + oi);
                Config.CLOUDBANK_PORT = oi;
            }
            
            os = o.optString("cloudbank_account");
            if (os != null) {
                logger.debug(ltag, "CloudBank Account: " + os);
                Config.CLOUDBANK_ACCOUNT = os;
            }
            
            os = o.optString("cloudbank_password");
            if (os != null) {
                logger.debug(ltag, "CloudBank Password: " + os);
                Config.CLOUDBANK_PASSWORD = os;
            }
            
            ob = o.optBoolean("use_custom_domain");
            if (ob != false) {
                logger.debug(ltag, "Use Custom Domain: " + ob);
                Config.USE_CUSTOM_DOMAIN = ob;
            }
            
            os = o.optString("custom_raida_domain");
            if (os != null) {
                logger.debug(ltag, "Custom RAIDA Domain: " + os);
                Config.CUSTOM_RAIDA_DOMAIN = os;
            }
            
            oi = o.optInt("mode");
            if (oi != -1) {
                logger.debug(ltag, "Custom Mode: " + oi);
                Config.REQUESTED_MODE = oi;
            }
            
            ob = o.optBoolean("advanced_view");
            if (ob != false) {
                logger.debug(ltag, "Use Advanced Domain: " + ob);
                Config.REQUESTED_ADVANCED_VIEW = ob;
            }
            
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse config file: " + e.getMessage());
            return;
        }
        
    }
    
    public static void syncMode() {
        if (Config.REQUESTED_MODE == Config.OPERATION_MODE_FAST)
            AppCore.currentMode = Config.OPERATION_MODE_FAST;
        else if (Config.REQUESTED_MODE == Config.OPERATION_MODE_SLOW) {
            AppCore.currentMode = Config.OPERATION_MODE_SLOW;
        } else if (Config.REQUESTED_MODE == Config.OPERATION_MODE_AUTO) {
            AppCore.currentMode = Config.OPERATION_MODE_FAST;
        }
        
        logger.debug(ltag, "Mode Set: " + AppCore.currentMode);
    }
 
    public static String getModeStr() {
        if (AppCore.currentMode == Config.OPERATION_MODE_FAST)
            return "Parallel";
        
        if (AppCore.currentMode == Config.OPERATION_MODE_SLOW)
            return "Serial";
     
        return "Unknown";
    }
    
    public static boolean writeConfig() {
        String globalConfigFilename = rootPath + File.separator + Config.GLOBAL_CONFIG_FILENAME;
        
        logger.debug(ltag, "Saving config " + globalConfigFilename);
        
        String edir = Config.DEFAULT_EXPORT_DIR.replace("\"", "\\\"").replace("\\", "\\\\");
        String ddir = Config.DEFAULT_DEPOSIT_DIR.replace("\"", "\\\"").replace("\\", "\\\\");
        String data = "{\"echo_timeout\":" + Config.ECHO_TIMEOUT + ", "
                + "\"detect_timeout\": " + Config.MULTI_DETECT_TIMEOUT + ", "
                + "\"read_timeout\": " + Config.READ_TIMEOUT + ", "
                + "\"fix_timeout\": " + Config.FIX_FRACKED_TIMEOUT + ", "
                + "\"max_coins\": " + Config.DEFAULT_MAX_COINS_MULTIDETECT + ", "
                + "\"export_dir\": \"" + edir + "\", "
                + "\"deposit_dir\": \"" + ddir + "\", "
                + "\"cloudbank_enabled\": " + Config.CLOUDBANK_ENABLED +  ", "
                + "\"cloudbank_lwallet\": \"" + Config.CLOUDBANK_LWALLET +  "\", "
                + "\"cloudbank_lwallet_password\": \"" + Config.CLOUDBANK_LWALLET_PASSWORD +  "\", "
                + "\"cloudbank_rwallet\": \"" + Config.CLOUDBANK_RWALLET +  "\", "
                + "\"cloudbank_port\": " + Config.CLOUDBANK_PORT +  ", "
                + "\"cloudbank_account\": \"" + Config.CLOUDBANK_ACCOUNT +  "\", "
                + "\"cloudbank_password\": \"" + Config.CLOUDBANK_PASSWORD +  "\", "
                + "\"ddnssn_server\": \"" + Config.DDNSSN_SERVER + "\", "
                + "\"custom_raida_domain\": \"" + Config.CUSTOM_RAIDA_DOMAIN + "\", "
                + "\"use_custom_domain\": " + Config.USE_CUSTOM_DOMAIN + ", "
                + "\"advanced_view\": " + Config.REQUESTED_ADVANCED_VIEW + ", "
                + "\"mode\": " + Config.REQUESTED_MODE + ""
                + "}";
        
        File f = new File(globalConfigFilename);
        f.delete();
        
        return AppCore.saveFile(globalConfigFilename, data);
    }
    
    public static int[] getSNSOverlap(int[][] sns) {
        logger.debug(ltag, "Getting overlapped sns");
        HashMap<Integer, CloudCoin> hm = new HashMap<Integer, CloudCoin>();
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            for (int j = 0; j < sns[i].length; j++) {
                int sn = sns[i][j];
                CloudCoin cc;
                
                if (!hm.containsKey(sn)) {
                    cc = new CloudCoin(Config.DEFAULT_NN, sn);
                    cc.setDetectStatus(i, CloudCoin.STATUS_PASS);
                    hm.put(sn, cc);                 
                    continue;
                }
                
                cc = hm.get(sn);
                cc.setDetectStatus(i, CloudCoin.STATUS_PASS);
            }
        }

        ArrayList<CloudCoin> coins = new ArrayList<CloudCoin>();
        Iterator it = hm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            CloudCoin cc = (CloudCoin) pair.getValue();            
            cc.setPownStringFromDetectStatus();
            /*
            if (!cc.getPownString().endsWith("ppppppppppppppp")) {
                continue;
            }
            */
            
            if (!cc.isSentFixable()) {
                logger.debug(ltag, "Skipping coin: " + cc.sn + " p=" + cc.getPownString());
                continue;
            }
            
            //System.out.println("add="+cc.sn);
            coins.add(cc);           
        }
        
        int[] rsns = new int[coins.size()];
        for (int i = 0; i < coins.size(); i++)
            rsns[i] = coins.get(i).sn;
        

        return rsns;

    }  
    
    public static int getChangeMethod(int denomination) {
        int method = 0;
        
        switch (denomination) {
            case 250:
                method = Config.CHANGE_METHOD_250F;
                break;
            case 100:
                method = Config.CHANGE_METHOD_100E;
                break;
            case 25:
                method = Config.CHANGE_METHOD_25B;
                break;
            case 5:
                method = Config.CHANGE_METHOD_5A;
                break;
        }
        
        return method;
    }
    
    public static String maskStr(String key, String data) {
        String result = data.replaceAll(key + "([A-Fa-f0-9]{28})", key + "***");
        return result;
    }
    
    public static int staleFiles(String user) {
        int cnt;
        
        cnt = AppCore.getFilesCount(Config.DIR_IMPORT, user);
        if (cnt != 0)
            return 1;
        
        cnt = AppCore.getFilesCount(Config.DIR_SUSPECT, user);
        if (cnt != 0) 
            return 2;

        cnt = AppCore.getFilesCount(Config.DIR_DETECTED, user);
        if (cnt != 0)
            return 3;
      
        return 0;
    }
    
    public static void appendSkySentCoinTransaction(String from, String to, int tosn, int amount, String memo) {
        String rMemo = memo.replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll(",", " ");
        String fstr, result;
        String date = AppCore.getCurrentDate();
        
        fstr = date + "," + from + "," + to + "," + tosn + "," + amount + "," +memo + ", closed";
        
        String fileName = AppCore.getLogDir() + File.separator + Config.SENT_SKYCOINS_FILENAME; 
        logger.debug(ltag, "Append transaction: " + fstr + " s="+fileName);
        
        result = "\r\n" + fstr;
        
        if (!AppCore.saveFileAppend(fileName, result, true)) {
            logger.debug(ltag, "Failed to write to the SkySent log. Maybe this file is open");
            return;
        }
    }

    public static void rmSentCoins() {
        String fileName = AppCore.getLogDir() + File.separator + Config.SENT_SKYCOINS_FILENAME;
        File f = new File(fileName);
        f.delete();
    }
    
    public static String[] getSentSkyWallets() {
        String fileName = AppCore.getLogDir() + File.separator + Config.SENT_SKYWALLETS_FILENAME;
        
        String data = AppCore.loadFile(fileName);
        if (data == null)
            return null;
        
        String[] parts = data.split("\\r?\\n");
        Arrays.sort(parts);
        
        return parts;
        
    }
    
    public static void appendSentSkyWallet(String walletName, String[] wallets) {
        String fileName = AppCore.getLogDir() + File.separator + Config.SENT_SKYWALLETS_FILENAME;
        
        String appStr = "";
        if (wallets != null) {
            for (int i = 0; i < wallets.length; i++) {
                if (walletName.equals(wallets[i]))
                    return;
            }   
            
            appStr += "\r\n";
        }
        
        appStr += walletName;
        
        if (!AppCore.saveFileAppend(fileName, appStr, true)) {
            logger.debug(ltag, "Failed to write to the SkySent log. Maybe this file is open");
            return;
        }
    }
    
    public static String[][] getSentCoins(String destwallet) {
        String fileName = AppCore.getLogDir() + File.separator + Config.SENT_SKYCOINS_FILENAME;
        
        String data = AppCore.loadFile(fileName);
        if (data == null)
            return null;
        
        String[] parts = data.split("\\r?\\n");
        
        String[] tmp;
        
        int j = 0;
        for (int i = 0; i < parts.length; i++) {
            
            tmp = parts[i].split(",");
            if (tmp.length != 7) {
                continue;
            }
            
            String name = tmp[1];
            if (!name.equals(destwallet))
                continue;
            
            j++;
        }

        int r = 0;
        String[][] rv = new String[j][];
        //for (int i = 0; i < parts.length; i++) {
        for (int i = parts.length - 1; i >= 0; i--) {
            tmp = parts[i].split(",");
            if (tmp.length != 7) {
                continue;
            }
            
            String name = tmp[1];
            if (!name.equals(destwallet))
                continue;

            rv[r] = new String[6];
            rv[r][0] = tmp[0];
            rv[r][1] = tmp[2];
            rv[r][2] = tmp[3];
            rv[r][3] = tmp[4];
            rv[r][4] = tmp[5];
            rv[r][5] = tmp[6];
            r++;
        }
        
        return rv;         
    }
    
    
    public static boolean hasCoinExtension(File file) {
        String f = file.toString().toLowerCase();
        if (f.endsWith(".stack") || f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".zip"))
            return true;
        
        logger.debug(ltag, "Ignoring invalid extension " + file.toString());
        
        return false;        
    }
    
    public static CloudCoin[] getCoinsInDir(String dir) {
        File dirObj = new File(dir);
        if (!dirObj.exists()) {
            return null;
        }
        
        int c = 0;
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            if (!AppCore.hasCoinExtension(file))
                continue;
               
            CloudCoin cc;
            try {
                cc = new CloudCoin(file.getAbsolutePath());
            } catch (JSONException e) {
                continue;
            }
                    
            c++;
        }
        
        int i = 0;
        CloudCoin[] ccs = new CloudCoin[c];
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            if (!AppCore.hasCoinExtension(file))
                continue;
            
            CloudCoin cc;
            try {
                 cc = new CloudCoin(file.getAbsolutePath());
            } catch (JSONException e) {
                continue;
            }
                
            ccs[i] = cc;
            i++;
        }
        
        return ccs;
    }
    
    public static String toHexadecimal(byte[] digest) {
        String hash = "";

        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) hash += "0";
            hash += Integer.toHexString(b);
        }

        return hash;
    }
    
    public static CloudCoin parseJpeg(String data) {

        int startAn = 40;
        int endAn = 72;
        String[] ans = new String[RAIDA.TOTAL_RAIDA_COUNT];
        String aoid, ed;
        int nn, sn;

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            ans[i] = data.substring(startAn + (i * 32), endAn + (i * 32));
        }

        aoid = AppCore.pownHexToString(data.substring(840, 895));
        ed = AppCore.expirationDateHexToString(data.substring(900, 902));
        try {
            nn = Integer.parseInt(data.substring(902, 904), 16);
            sn = Integer.parseInt(data.substring(904, 910), 16);
        } catch (NumberFormatException e) {
            logger.error(ltag, "Failed to parse numbers: " + e.getMessage());
            return null;
        }

        CloudCoin cc = new CloudCoin(nn, sn, ans, ed, new String[] { aoid }, Config.DEFAULT_TAG);

        return cc;
    }
    
    public static String pownHexToString(String hexString) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0, j = hexString.length(); i < j; i++) {
            if ('0' == hexString.charAt(i))
                stringBuilder.append('p');
            else if ('1' == hexString.charAt(i))
                stringBuilder.append('9');
            else if ('2' == hexString.charAt(i))
                stringBuilder.append('n');
            else if ('E' == hexString.charAt(i))
                stringBuilder.append('e');
            else if ('F' == hexString.charAt(i))
                stringBuilder.append('f');
        }

        return stringBuilder.toString();
    }

    public static String expirationDateHexToString(String edHex) {
        int monthsAfterZero = Integer.valueOf(edHex, 16);

        Date date;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            date = sdf.parse("13-08-2016");
        } catch (ParseException e) {
            return "08-2016";
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.add(Calendar.MONTH, monthsAfterZero);
        int m = cal.get(Calendar.MONTH) + 1;

        return m + "-" + cal.get(Calendar.YEAR);
    }
    
    public static int[] getCoinsSNInDir(String dir) {
        File dirObj = new File(dir);
        if (!dirObj.exists()) {
            return null;
        }
        
        ArrayList<Integer> sns = new ArrayList<Integer>();
        
        int c = 0;
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;
            
            if (!AppCore.hasCoinExtension(file))
                continue;

            String fname = file.getName().toLowerCase();
            if (fname.endsWith(".png")) {
                CloudCoin cc;
            
                try {
                    cc = new CloudCoin(file.getAbsolutePath());
                } catch (Exception e) {
                    logger.debug(ltag, "Unable to open " + file.getName() + ", skipping it");
                    continue;
                }

                sns.add(cc.sn);
                continue;
            }
            
            if (fname.endsWith(".jpg") || fname.endsWith(".jpeg")) {
                FileInputStream fis;
                byte[] jpegHeader = new byte[455];
                String data;
                CloudCoin cc;
                

                try {
                    fis = new FileInputStream(file.getAbsolutePath());
                    fis.read(jpegHeader);
                    data = toHexadecimal(jpegHeader);
                    fis.close();
                    cc = AppCore.parseJpeg(data);
                    if (cc == null) {
                        logger.debug(ltag, "Failed to parse jpeg");
                        continue;
                    }
                } catch (IOException e) {
                    logger.error(ltag, "Failed to read file: " + e.getMessage());
                    continue;
                }
                
                sns.add(cc.sn);
                continue;
            }
            
            if (fname.endsWith(".zip")) {
                byte[] buffer = new byte[64 * 1024 * 1024];
                try {
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(file.getAbsolutePath()));
                    ZipEntry zipEntry = zis.getNextEntry();
                    while (zipEntry != null) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                        bos.close();
            
                        CloudCoin cc = new CloudCoin(bos.toByteArray());
                        sns.add(cc.sn);
                        zipEntry = zis.getNextEntry();
                    }
                    zis.closeEntry();
                    zis.close();
                } catch (Exception e) {
                    logger.debug(ltag, "Failed to unzip file " + e.getMessage());
                    continue;
                }
                continue;
            }
            
            String[] parts = file.getName().split("\\.");
            if (parts.length != 5) {
                logger.debug(ltag, "Maybe it is a stack file. Will try to parse it");
                CloudCoin[] sccs = AppCore.parseStack(file.getAbsolutePath());
                if (sccs == null) {
                    logger.debug(ltag, "It is not, continue");
                    continue;
                }
                for (int i = 0; i < sccs.length; i++) {
                    sns.add(sccs[i].sn);
                }
                
                continue;
            }
            
            int sn;            
            try {
                sn = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                logger.debug(ltag, "Maybe it is a stack file. Will try to parse it");
                CloudCoin[] sccs = AppCore.parseStack(file.getAbsolutePath());
                for (int i = 0; i < sccs.length; i++) {
                    sns.add(sccs[i].sn);
                }
                continue;
            }
            
            sns.add(sn);
        }
        
        int asns[] = new int[sns.size()];
        for (int i = 0; i < asns.length; i++)
            asns[i] = sns.get(i).intValue();
        
        return asns;
    }
    
    public static CloudCoin[] parseStack(String file) {
        JSONArray incomeJsonArray;
        int sn, nn;
        CloudCoin cc;
        CloudCoin[] ccs = new CloudCoin[0];
        
        String data = AppCore.loadFile(file);
        if (data == null) {
            logger.debug(ltag, "Failed to load file " + file);
            return ccs;
        }
        
        try {
            JSONObject o = new JSONObject(data);
            incomeJsonArray = o.getJSONArray("cloudcoin");

            ccs = new CloudCoin[incomeJsonArray.length()];
            for (int i = 0; i < incomeJsonArray.length(); i++) {
                JSONObject childJSONObject = incomeJsonArray.getJSONObject(i);

                sn = childJSONObject.getInt("sn");
                nn = childJSONObject.getInt("nn");

                JSONArray ans = childJSONObject.getJSONArray("an");
                String[] strAns = AppCore.toStringArray(ans);
                String ed = childJSONObject.optString("ed");
                JSONArray aoidJson = childJSONObject.optJSONArray("aoid");
                String[] strAoid = AppCore.toStringArray(aoidJson);

                cc = new CloudCoin(nn, sn, strAns, ed, strAoid, Config.DEFAULT_TAG);
                ccs[i] = cc;
            }
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse stack: " + e.getMessage());
            return null;
        }

        return ccs;
    }
    
    public static String[] toStringArray(JSONArray jsonArray) {
        if (jsonArray == null)
            return null;

        String[] arr = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            arr[i] = jsonArray.optString(i);
        }

        return arr;
    }
    
    public static void saveSerialsFromSNs(String fileName, int[] sns) {
        
        logger.debug(ltag, "Saving serials to " + fileName);
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("Serial,Denomination\r\n");
        for (int sn : sns) {
            CloudCoin cc = new CloudCoin(Config.DEFAULT_NN, sn);
            sb.append("" + sn);
            sb.append(",");
            sb.append("" + cc.getDenomination());
            sb.append("\r\n");
        }

        AppCore.saveFile(fileName, sb.toString());
    }
    
 
    public static BillPayItem[] parseBillPayCsv(String filename) throws Exception {
        BillPayItem[] rvs;
        BufferedReader reader;
        ArrayList<String> as = new ArrayList<String>();
	try {            
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            while (line != null) {
                as.add(line);
		line = reader.readLine();
            }
            reader.close();
            
            int i = 0;
            rvs = new BillPayItem[as.size()];
            for (String s : as) {
                String[] parts = s.split(",");
                if (parts.length != 12) {
                    logger.debug(ltag, "Failed to parse string: " + s);
                    throw new Exception("Invalid Number of Fields");
                }
                
                BillPayItem bi = new BillPayItem(i);
                String sendMethodStr = parts[0].trim();
                String sendFormatStr = parts[1].trim();
                String sendStatusStr = parts[11].trim();
                
                if (!sendMethodStr.equals("email")) 
                    throw new Exception("Line: " + (i + 1) + ". Only email methods are supported");
                
                if (!sendFormatStr.equals("stack"))
                    throw new Exception("Line: " + (i + 1) + ". Only stack formats are supported");

                if (sendStatusStr.equals("ready")) {
                    bi.status = BillPayItem.SEND_STATUS_READY;
                } else if (sendStatusStr.equals("skip")) {
                    bi.status = BillPayItem.SEND_STATUS_SKIP;
                } else {
                    throw new Exception("Line: " + (i + 1) + ". Only ready and skip statuses are supported");
                }
                
                bi.method = BillPayItem.SEND_METHOD_EMAIL;
                bi.format = BillPayItem.SEND_FORMAT_STACK;
                bi.filename = filename;

                try {
                    bi.total = Integer.parseInt(parts[2].trim());
                    bi.s1 = Integer.parseInt(parts[3].trim());
                    bi.s5 = Integer.parseInt(parts[4].trim());
                    bi.s25 = Integer.parseInt(parts[5].trim());
                    bi.s100 = Integer.parseInt(parts[6].trim());
                    bi.s250 = Integer.parseInt(parts[7].trim());
                } catch (NumberFormatException e) {
                    throw new Exception("Line: " + (i + 1) + ". Failed to numbers. Line: " + (i + 1));
                }
                
                if (bi.total < 0 || bi.s1 < 0 || bi.s5 < 0 || bi.s25 < 0 || bi.s100 < 0 || bi.s250 < 0) 
                    throw new Exception("Line: " + (i + 1) + ". Negative amount value. Line " + (i + 1));
                
                if (bi.total != 0 && (bi.s1 > 0 || bi.s5 > 0 || bi.s25 > 0 || bi.s100 > 0 || bi.s250 > 0)) 
                    throw new Exception("Line: " + (i + 1) + ". Both total and denominations are set");
                
                if (bi.total == 0)
                    bi.calcTotal();
                
                bi.address = parts[8].trim();
                if (!Validator.email(bi.address)) 
                    throw new Exception("Invalid Email " + bi.address);
                
                bi.data = parts[9].trim();
                String[] metadata = parts[10].trim().split("&");
                for (int j = 0; j < metadata.length; j++) {
                    String[] p = metadata[j].split("=");
                    if (p.length != 2) {
                        throw new Exception("Line: " + (i + 1) + ". Invalid Metadata Format");
                    }
                    
                    bi.setMetadataItem(p[0], p[1]);
                }
                
                String templatePath = bi.getMetadataItem("body");
                if (templatePath == null)
                    throw new Exception("Line: " + (i + 1) + ". Body is not defined");
                
                File f = new File(templatePath);
                if (!f.exists()) 
                    throw new Exception("Line: " + (i + 1) + ". Body template does not exist");
                                
                rvs[i++] = bi;
            }
            
	} catch (IOException e) {
            logger.debug(ltag, "Failed to read file: " + filename + ": " +e.getMessage());
            return null;
	}
        
        return rvs;
    }
    
    public static Map<String, Properties> parseINI(Reader reader) throws IOException {
        Map<String, Properties> result = new HashMap();
        try {
            new Properties() {
                private Properties section;

                @Override
                public Object put(Object key, Object value) {
                    String header = (((String) key) + " " + value).trim();
                    if (header.startsWith("[") && header.endsWith("]")) {
                        return result.put(header.substring(1, header.length() - 1), 
                            section = new Properties());
                    }
                    else {
                        return section.put(key, value);
                    }
                }
            }.load(reader);
        } catch(Exception e) {
            logger.debug(ltag, "INI File is corrupted");
            return null;
        }
        
        return result;
    }
    
    public static String getEmailTemplate(String template, int amount) {
        String fname = template;
        File f = new File(fname);
        
        String fdata = AppCore.loadFile(fname);
        if (fdata == null) {
            logger.debug(ltag, "Failed to load email template: " + fname);
            return null;
        }
        
        fdata = fdata.replaceAll("%amountToSend%", "" + amount);
      
        return fdata;
    }
    
    public static String checkBillPays(ServantManager sm, Wallet w, BillPayItem[] bis) {
        int globalTotal = 0;

        for (int i = 0; i < bis.length; i++) {
            if (bis[i].status != BillPayItem.SEND_STATUS_READY)
                continue;

            String fileName = bis[i].getSentFilename(w.getName());
            String fileNameStuck = bis[i].getStuckFilename(w.getName());

            File f = new File(fileName);
            if (f.exists()) {
                bis[i].status = BillPayItem.SEND_STATUS_SENT;
                continue;
            }
            
            f = new File(fileNameStuck);
            if (f.exists()) {
                bis[i].status = BillPayItem.SEND_STATUS_STUCK;
                continue;
            }

            globalTotal += bis[i].total;
        }

        if (w.getTotal() < globalTotal) {
            return "Not enough funds. Required: " + globalTotal;
        }
        
        if (!sm.checkCoins(globalTotal)) {
            return "Failed to collect denominations";
        }
        
        return null;
    }

    
    
    public static String extractStackFromPNG(String fileName) {
        byte[] bytes = AppCore.loadFileToBytes(fileName);
        if (bytes == null) {
            logger.error(ltag, "Failed to load file " + fileName);
            return null;
        }
        
        return AppCore.extractStackFromPNGBytes(bytes);
    }
        
    public static String extractStackFromPNGBytes(byte[] bytes) {

        int idx = AppCore.basicPngChecks(bytes);
        if (idx == -1) {
            logger.error(ltag, "PNG is corrupted");
            return null;
        }

        int i = 0;
        long length;
                        
        while (true) {
            length = AppCore.getUint32(bytes, idx + 4 + i);
            if (length == 0) {
                i += 12;
                if (i > bytes.length) {
                    logger.error(ltag, "CloudCoin was not found");
                    return null;
                }
            }
            
            if (idx + 4 + i + 7 > bytes.length) {
                logger.error(ltag, "CloudCoin was not found");
                return null;
            }
            
                       
            StringBuilder sb = new StringBuilder();
            sb.append(Character.toChars(bytes[idx + 4 + i + 4]));
            sb.append(Character.toChars(bytes[idx + 4 + i + 5]));
            sb.append(Character.toChars(bytes[idx + 4 + i + 6]));
            sb.append(Character.toChars(bytes[idx + 4 + i + 7]));
            String signature = sb.toString();

            logger.debug(ltag, "sig " + signature);
            if (signature.equals("cLDc")) {
                long crcSig = AppCore.getUint32(bytes, idx + 4 + i + 8 + (int) length);
                long calcCrc = AppCore.crc32(bytes, idx + 4 + i + 4, (int)(length + 4));

                if (crcSig != calcCrc) {
                    logger.error(ltag, "Invalid CRC32");
                    return null;
                }

                break;
            }

            i += length + 12;
            if (i > bytes.length) {
                logger.error(ltag, "CloudCoin was not found");
                return null;
            }
        }
        
        byte[] nbytes =  Arrays.copyOfRange(bytes, idx + 4 + i + 8, idx + 4 + i + 8 + (int)length);
        String sdata = new String(nbytes);
        
        return sdata;
    }
    
    public static int basicPngChecks(byte[] bytes) {    
        if (bytes[0] != 0x89 && bytes[1] != 0x50 && bytes[2] != 0x4e && bytes[3] != 0x45 
                && bytes[4] != 0x0d && bytes[5] != 0x0a && bytes[6] != 0x1a && bytes[7] != 0x0a) {
            logger.error(ltag, "Invalid PNG signature");
            return -1;
        }

        long chunkLength = AppCore.getUint32(bytes, 8);
        long headerSig = AppCore.getUint32(bytes, 12);
        if (headerSig != 0x49484452) {
            logger.error(ltag, "Invalid PNG header");
            return -1;
        }
   
        int idx = (int)(16 + chunkLength);        
        long crcSig = AppCore.getUint32(bytes, idx);
        long calcCrc = AppCore.crc32(bytes, 12, (int)(chunkLength + 4));
        if (crcSig != calcCrc) {
            logger.error(ltag, "Invalid PNG Crc32 checksum");
            return -1;
        }
        
        return idx;
    }
    
    public static long getUint32(byte[] bytes, int offset) {
        byte[] nbytes = Arrays.copyOfRange(bytes, offset, offset + 4);
        ByteBuffer buffer = ByteBuffer.allocate(8).put(new byte[]{0, 0, 0, 0}).put(nbytes);
        buffer.position(0);

        return buffer.getLong();
    }
    
    public static void setUint32(byte[] data, int offset, long value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(value);

        data[offset] = bytes[4];
        data[offset + 1] = bytes[5];
        data[offset + 2] = bytes[6];
        data[offset + 3] = bytes[7];
        
        return;
    }

    public static long crc32(byte[] data, int offset, int length) {
        byte[] nbytes = Arrays.copyOfRange(data, offset, offset + length);
        Checksum checksum = new CRC32();
        checksum.update(nbytes, 0, nbytes.length);
        long checksumValue = checksum.getValue();
        
        return checksumValue;
    }

    public static boolean zipFile(String filename) {
        logger.debug(ltag, "Zipping " + filename);
        File f = new File(filename);
        if (!f.exists()) {
            logger.error(ltag, "File doesn't exist");
            return false;
        }

        String zippedFilename = filename + ".zip";
        try {
            FileOutputStream fos = new FileOutputStream(zippedFilename);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            FileInputStream fis = new FileInputStream(f);
            ZipEntry zipEntry = new ZipEntry(f.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.close();
            fis.close();
            fos.close();
            
            logger.debug(ltag, "Deleting source");
            f.delete();
        } catch (IOException e) {
            logger.error(ltag, "Failed to zip: " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    public static boolean zipFiles(ArrayList<String> files, String outputFilename) {
        try {
            FileOutputStream fos = new FileOutputStream(outputFilename);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (String srcFile : files) {
                File fileToZip = new File(srcFile);
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zipOut.putNextEntry(zipEntry);
 
                byte[] bytes = new byte[1024];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }
            zipOut.close();
            fos.close();
        } catch (IOException e) {
            logger.error(ltag, "Failed to zip files: " + e.getMessage());
            return false;         
        }
        
        return true;
    }
 
    
    public static int skyCoinThere(ArrayList<CloudCoin> ccs, Wallet[] wallets, String user) {     
        int rcc = 0;
        for (int i = 0; i < wallets.length; i++) {
            if (!wallets[i].isSkyWallet())
                continue;
            
            for (CloudCoin cc : ccs) {
                if (cc.sn == wallets[i].getIDCoin().sn ) {
                    rcc = cc.sn;
                    logger.debug(ltag, "ID coin was tried to be used. Moving to Trash " + cc.sn);
                    
                    String dir = AppCore.getUserDir(Config.DIR_SUSPECT, user);
                    if (dir == null) {
                        logger.error(ltag, "Failed to find suspect dir");
                        continue;
                    }
                    
                    String filename = dir + File.separator + cc.getFileName();                  
                    AppCore.moveToTrash(filename, user);

                }
            }
        }
        //System.exit(1);
        return rcc;
        
        
    }
    
    public static String getHwID() {
        Enumeration<NetworkInterface> nis = null;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e)  {
            logger.debug(ltag, "Failed to get interfaces");
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        if (nis != null) {
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                try {
                    if (!ni.isUp())
                        continue;
                    
                    byte[] macBytes = ni.getHardwareAddress();
                    if (macBytes == null || macBytes.length == 0)
                        continue;
                    
                    StringBuilder ssb = new StringBuilder();
                    for (byte b : macBytes) {
                        ssb.append(String.format("%02x:", b));
                    }
                
                    String mac = ssb.toString();
                    sb.append(mac);
                } catch (SocketException e)  {
                    logger.debug(ltag, "Socket exception: " +e.getMessage());
                }
            }
        }
        
        String hwId = AppCore.getMD5(sb.toString());
        logger.debug(ltag, "hwID " + hwId);
        
        return hwId;
        
        
    }
    
    public static String formatMemo(String memo, String from) {
        //if(1==1)
        //    return memo;
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(memo);
        sb.append(Config.MEMO_METADATA_SEPARATOR);
        
        StringBuilder sbx = new StringBuilder();
        sbx.append("from=");
        sbx.append(from);
        
        String b64 =  Base64.getEncoder().encodeToString((sbx.toString()).getBytes());
        
        sb.append(b64);
        
        return sb.toString();
    }
    
    public static String extractTag(String metadata) {
        String[] parts = metadata.split(Config.MEMO_METADATA_SEPARATOR);
        
        return parts[0];
    }
    
    public static String extractFromTag(String metadata) {
        String[] parts = metadata.split(Config.MEMO_METADATA_SEPARATOR);
        if (parts.length <= 1) {
            return "";
        }
        
        String b64 =  new String(Base64.getDecoder().decode(parts[1]));

        b64 = "[main]\n" + b64;
        StringReader sr = new StringReader(b64);
        Map<String, Properties> data;
        try {
            data = AppCore.parseINI(sr);
        } catch (IOException e) {
            logger.error(ltag, "Failed to parse metadata" + parts[1]);
            return "";
        }
        
        
        Properties p = data.get("main");
        if (p == null)
            return "";
             
        String s = p.getProperty("from");
        if (s == null)
            return "";
        
        return s;
    }
    
    public static String generatePansAndCardNumber(int sn, String pin, String[] pans) {
        logger.debug(ltag, "Generating pans for " + sn);
        
        String AB = "0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 12; i++) {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        
        String rand = sb.toString();       
        logger.debug(ltag, "rand " + rand);
        
        String preCardNumber = "401" + rand;
        String reversed = new StringBuilder(preCardNumber).reverse().toString();
        int total = 0;
        for (int i = 0; i < reversed.length(); i++) {
            int c = reversed.charAt(i) - '0';
            if (((i + 3) % 2) != 0) {
                c *= 2;
                if (c > 9)
                    c -= 9;
            }
            
            total += c;
        }

        int remainder = 10 - (total % 10);
        if (remainder == 10)
            remainder = 0;
        
        String cardNumber = preCardNumber + "" + remainder;
        
        logger.debug(ltag, "Generated Card Number " + cardNumber);
        logger.debug(ltag, "reversed " + reversed + " total " + total + " rem " + remainder);
        
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            String seed = "" + i + "" + sn + "" + rand + "" + pin;            
            pans[i] = AppCore.getMD5(seed);
        }
                
        return cardNumber;
        
    }
    
    public static boolean saveCard(String filename, String wallet, String data, String cardNumber, String pin, String ip) {
        String templateFileName = AppCore.getTemplateDir() + File.separator + Config.CARD_TEMPLATE_NAME;        
        byte[] bytes = AppCore.loadFileToBytes(templateFileName);
        if (bytes == null) {
            logger.error(ltag, "Failed to load template " + templateFileName);
            return false;
        }
        
        int idx = AppCore.basicPngChecks(bytes);
        if (idx == -1) {
            logger.error(ltag, "PNG checks failed");
            return false;
        }
        
        logger.info(ltag, "Loaded, bytes: " + bytes.length);

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);     
        BufferedImage bi;
        try {
            bi = ImageIO.read(bais);
        } catch (IOException e) {
            logger.debug(ltag, "Failed to read image");
            return false;
        }
        
        LocalDate currentdate = LocalDate.now();
        int currentMonth = currentdate.getMonthValue();
        int currentYear = currentdate.getYear() - 2000;
        
        currentYear += 5;
        
        String date = currentMonth + "/" + currentYear;
        
        cardNumber = cardNumber.replaceAll("(\\d{4})","$1 ").trim();
        

    
        //Font f = AppUI.brand.getCardFont();
        
        
        
        Graphics g = bi.getGraphics();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(44f));
        g.drawString(cardNumber, 64, 285);
        
        g.setFont(g.getFont().deriveFont(35f));
        g.drawString(date, 450, 362);
        g.setFont(g.getFont().deriveFont(44f));
        g.drawString(wallet, 64, 425);
        
        g.setColor(Color.decode("#dddddd"));
        g.setFont(g.getFont().deriveFont(17f));
        g.drawString("Keep these numbers secret. Do not give to merchants.", 64, 320);
        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(35f));
        g.drawString("CVV (Keep Secret): " + pin, 64, 675);
        g.setColor(Color.decode("#ffffff"));
        g.setFont(g.getFont().deriveFont(18f));
        g.drawString( "IP " + ip, 174, 736);
        

        g.dispose();
    
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bi, "png", baos);
            baos.flush();
        } catch (IOException e) {
            logger.debug(ltag, "Failed to write image");
            return false;
        }
        
        bytes = baos.toByteArray();
  
        logger.info(ltag, "Generated, bytes: " + bytes.length);
        int dl = data.length();
        int chunkLength = dl + 12;
        logger.debug(ltag, "data length " + dl);
        byte[] nbytes = new byte[bytes.length + chunkLength];
        for (int i = 0; i < idx + 4; i++) {
            nbytes[i] = bytes[i];
        }

        // Setting up the chunk
        // Set length
        AppCore.setUint32(nbytes, idx + 4, dl);
        
        // Header cLDc
        nbytes[idx + 4 + 4] = 0x63;
        nbytes[idx + 4 + 5] = 0x4c;
        nbytes[idx + 4 + 6] = 0x44;
        nbytes[idx + 4 + 7] = 0x63;
                
        for (int i = 0; i < dl; i++) {
            nbytes[i + idx + 4 + 8] = (byte) data.charAt(i);
        }

        // crc
        long crc32 = AppCore.crc32(nbytes, idx + 8, dl + 4);
        AppCore.setUint32(nbytes, idx + 8 + dl + 4, crc32);
        logger.debug(ltag, "crc32 " + crc32);
        
        // Rest data
        for (int i = 0; i < bytes.length - idx - 4; i++) {
            nbytes[i + idx + 8 + dl + 4 + 4] = bytes[i + idx + 4];
        }


        if (!AppCore.saveFileFromBytes(filename, nbytes)) {
            logger.error(ltag, "Failed to write file");
            return false;
        }
        
        return true;
    }
 
    public static int getRandomCorner() {
        Random rand = new Random();
        
        return rand.nextInt(4) + 1;
    }
    
    public static ArrayList<String> getPartialCoins(int sn) {
        ArrayList<String> files = new ArrayList<String>();
        try {
            File rFile = new File(AppCore.getPartialDir());
            for (File file: rFile.listFiles()) {
                if (file.isDirectory())
                    continue;
            
                if (!AppCore.hasCoinExtension(file))
                    continue;
                
                if (!file.getName().startsWith(sn + "-"))
                    continue;
                
                files.add(file.getAbsolutePath());
            }         
        } catch (Exception e) {
           logger.error(ltag, "Failed to read directory: " + e.getMessage());
           return files;
        }
        
        return files;
    }
}
