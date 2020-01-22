package global.cloudcoin.ccbank.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AppCore {

    static private String ltag = "AppCore";

    static private File rootPath;
    static private GLogger logger;

    static private ExecutorService service;
    
    static public String raidaErrText = "Cannot Connect to the RAIDA. "
            + "Check that local routers are not blocking your connection.";

    static public boolean createDirectory(String dirName) {
        String idPath;

        idPath = rootPath + File.separator + dirName;
        
        File idPathFile = new File(idPath);
        if (idPathFile.exists())
            return true;
        
        logger.info(ltag, "Creating " + idPath);
        if (!idPathFile.mkdirs()) {
            logger.error(ltag, "Can not create directory " + dirName);
            return false;
        }

        logger.info(ltag, "CREATED " + idPath);
        return true;
    }

    static public void createDirectoryPath(String path) {
        File idPathFile = new File(path);
        if (idPathFile.exists())
            return;
        
        if (!idPathFile.mkdirs()) {
            logger.error(ltag, "Can not create directory " + path);
            return;
        }
    }
   
    static public boolean initFolders(File path, GLogger logger) throws Exception {
        rootPath = path;
        AppCore.logger = logger;

        if (!createDirectory(Config.DIR_ROOT))
            return false;
        
        rootPath = new File(path, Config.DIR_ROOT);

        if (!createDirectory(Config.DIR_EMAIL_TEMPLATES))
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
        
        if (!createDirectory(Config.DIR_EMAIL_TEMPLATES))
            return false;
        
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
            Config.DIR_EMAILOUT,
            Config.DIR_FRACKED,
            Config.DIR_GALLERY,
            Config.DIR_IMPORT,
            Config.DIR_IMPORTED,
            Config.DIR_LOGS,
            Config.DIR_LOST,
            Config.DIR_MIND,
            Config.DIR_PARTIAL,
            Config.DIR_PAYFORWARD,
            Config.DIR_PREDETECT,
            Config.DIR_RECEIPTS,
            Config.DIR_REQUESTS,
            Config.DIR_REQUESTRESPONSE,
            Config.DIR_SENT,
            Config.DIR_SUSPECT,
            Config.DIR_TEMPLATES,
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

   static public String getBackupDir() {
       File f = new File(rootPath, Config.DIR_BACKUPS);
       
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
                logger.error(ltag, "Failed to rename file " + fileName);
                return false;
            }
        } catch (Exception e) {
            logger.error(ltag, "Failed to move file: " + e.getMessage());
            return false;
        }
        
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

    static public void moveToBank(String fileName, String user) { 
        moveToFolderNoTs(fileName, Config.DIR_BANK, user); 
    }

    static public void moveToImported(String fileName, String user) {
        moveToFolderNoTs(fileName, Config.DIR_IMPORTED, user, true);
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

    static public int getFilesCount(String dir, String user) {
        String path = getUserDir(dir, user);
        File rFile;
        int rv;

        try {
            rFile = new File(path);
            //rv = rFile.listFiles().length;
           
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
    
    
    public static void copyTemplatesFromJar(String user) {
        int d;
        String templateDir, fileName;

	templateDir = AppCore.getUserDir(Config.DIR_TEMPLATES, user);
	fileName = templateDir + File.separator + "jpeg1.jpg";
	File f = new File(fileName);
	if (!f.exists()) {
            for (int i = 0; i < AppCore.getDenominations().length; i++) {
                d = AppCore.getDenominations()[i];
                fileName = "jpeg" + d + ".jpg";
                   
                URL u = AppCore.class.getClassLoader().getResource("resources/" + fileName);
                if (u == null)
                    continue;
                
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
                
                try {
                    if (url.startsWith(JAR_URI_PREFIX) && bang != -1) {
                        jf = new JarFile(url.substring(JAR_URI_PREFIX.length(), bang)) ;
                    } else {
                        logger.error(ltag, "Invalid jar");
                        return;
                    }
                
                    for (Enumeration<JarEntry> entries = jf.entries(); entries.hasMoreElements();) {
                        JarEntry entry = entries.nextElement();

                        if (entry.getName().equals("resources/" + fileName)) {
                            InputStream in = jf.getInputStream(entry);
                            String dst = AppCore.getUserDir(Config.DIR_TEMPLATES, user);
                            dst += File.separator + fileName;

                            AppCore.copyFile(in, dst);
                        }
                    }
                } catch (IOException e) {
                    logger.error(ltag, "Failed to copy templates: " + e.getMessage());
                    return ;
                }          
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
            sb.append("</b></p><br><br><p>Details:</p><br>");
            
            JSONArray a = o.getJSONArray("receipt_detail");
            
            for (int i = 0; i < a.length(); i++) {
                JSONObject io = a.getJSONObject(i);
                
                String nnsn = io.getString("nn.sn");
                String status = io.getString("status");
                String pown = io.getString("pown");
                String note = io.getString("note");

                sb.append("<p>nn.sn: ");
                sb.append(nnsn);
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
    
    static public void moveFolderContentsToTrash(String folder, String user) {
        String path = AppCore.getUserDir(folder, user);
        
        File dirObj = new File(path);
        if (!dirObj.exists()) {
            logger.error(ltag, "Path " + path + " doesn't exist");
            return;
        }
        
        for (File file: dirObj.listFiles()) {
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
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse config file: " + e.getMessage());
            return;
        }
        
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
                + "\"ddnssn_server\": \"" + Config.DDNSSN_SERVER + "\""
                + "}";
        
        File f = new File(globalConfigFilename);
        f.delete();
        
        return AppCore.saveFile(globalConfigFilename, data);
    }
    
    public static int[] getSNSOverlap(int[][] sns) {
        int[] vsns;
        
        logger.debug(ltag, "Getting overlapped sns");
        HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();

        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            for (int j = 0; j < sns[i].length; j++) {
                int sn = sns[i][j];
                
                if (!hm.containsKey(sn)) {
                    hm.put(sn, 1);                 
                    continue;
                }
                
                int cnt = hm.get(sn);
                cnt++;
                
                hm.put(sn, cnt);
            }
        }
             
        int valid = 0;
        Iterator it = hm.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            
            int sn = (int) pair.getKey();
            int cnt = (int) pair.getValue();
            
            if (cnt < RAIDA.TOTAL_RAIDA_COUNT - Config.MAX_FAILED_RAIDAS_TO_SEND) {
                logger.debug(ltag, "Skipping coin " + sn + ". Only " + cnt + " RAIDAs think it is ok");
                continue;
            }
            
            valid++;
        }

        logger.debug(ltag, "Number of valid coins: " + valid);
        
        int rsns[] = new int[valid];
        
        it = hm.entrySet().iterator();
        int idx = 0;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            
            int sn = (int) pair.getKey();
            int cnt = (int) pair.getValue();
            
            if (cnt < RAIDA.TOTAL_RAIDA_COUNT - Config.MAX_FAILED_RAIDAS_TO_SEND) {
                logger.debug(ltag, "Skipping coin " + sn + ". Only " + cnt + " RAIDAs think it is ok");
                continue;
            }
            
            rsns[idx] = sn;
            idx++;
        }
        
        return rsns;
    }  
    
    public static int getChangeMethod(int denomination) {
        int method = 0;
        
        switch (denomination) {
            case 250:
                method = Config.CHANGE_METHOD_250E;
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
    
    public static String[][] getSentCoins() {
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
            j++;
        }

        int r = 0;
        String[][] rv = new String[j][];
        for (int i = 0; i < parts.length; i++) {
            tmp = parts[i].split(",");
            if (tmp.length != 7) {
                continue;
            }

            rv[r++] = tmp;
        }
        
        return rv;         
    }
    
    
    public static boolean hasCoinExtension(File file) {
        String f = file.toString().toLowerCase();
        if (f.endsWith(".stack") || f.endsWith(".jpg") || f.endsWith(".jpeg"))
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
 
    public static String[][] parseBillPayCsv(String filename) {
        String [][] rvs;
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
            rvs = new String[as.size()][];
            for (String s : as) {
                String[] parts = s.split(",");
                if (parts.length != 9) {
                    logger.debug(ltag, "Failed to parse string: " + s);
                    return null;
                }
                
                rvs[i++] = parts;
            }
            
	} catch (IOException e) {
            logger.debug(ltag, "Failed to read file: " + filename + ": " +e.getMessage());
            return null;
	}
        
        return rvs;
    }
    
    public static Map<String, Properties> parseINI(Reader reader) throws IOException {
        Map<String, Properties> result = new HashMap();
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
        return result;
    }
    
    public static String getEmailTemplate(String template, int amount) {
        String fname = AppCore.rootPath + File.separator + Config.DIR_EMAIL_TEMPLATES + File.separator + template;
        File f = new File(fname);
        
        String fdata = AppCore.loadFile(fname);
        if (fdata == null) {
            logger.debug(ltag, "Failed to load email template: " + fname);
            return null;
        }
        
        fdata = fdata.replaceAll("%amountToSend%", "" + amount);
      
        return fdata;
    }
    
    public static boolean checkEmailTemplate(String template) {
        String fname = AppCore.rootPath + File.separator + Config.DIR_EMAIL_TEMPLATES + File.separator + template;
        File f = new File(fname);
        if (!f.exists()) {
            logger.debug(ltag, "FileTemplate " + fname + " doesn't exist");
            return false;
        }
        
        return true;
    }
    
    public static String checkBillPays(Wallet w, String[][] s) {
        for (int i = 0; i < s.length; i++) {
            String[] line = s[i];
            
            int total, s1, s5, s25, s100, s250;
            total = s1 = s5 = s25 = s100 = s250 = 0;
            
            try {
                total = Integer.parseInt(line[1]);
                s1 = Integer.parseInt(line[2]);
                s5 = Integer.parseInt(line[3]);
                s25 = Integer.parseInt(line[4]);
                s100 = Integer.parseInt(line[5]);
                s250 = Integer.parseInt(line[6]);
            } catch (NumberFormatException e) {
                return "Failed to numbers. Line: " + (i + 1);
            }
            
            if (total < 0 || s1 < 0 || s5 < 0 || s25 < 0 || s100 < 0 || s250 < 0) {
                return "Invalid amount value. Line " + (i + 1);
            }
            
            if (total != 0 && (s1 > 0 || s5 > 0 || s25 > 0 || s100 > 0 || s250 > 0)) {
                return "Both total and denominations are set";
            }
            
            if (total == 0)
                total = s1 + s5 + s25 + s100 + s250;
            
            if (w.getTotal() < total) {
                return "Not enough funds. Required: " + total;
            }
            
            if (!AppCore.checkEmailTemplate(line[8])) {
                return "Template " + line[8] + " doesn't exist. Line " + (i + 1);
            }
           // if (total != 0 && (s1 ))
            
        }
        
        return null;
    }
    
    public static int getTotalToSend(String[] line) {
        int total, s1, s5, s25, s100, s250;
        total = s1 = s5 = s25 = s100 = s250 = 0;
            
        try {
            total = Integer.parseInt(line[1]);
            s1 = Integer.parseInt(line[2]);
            s5 = Integer.parseInt(line[3]);
            s25 = Integer.parseInt(line[4]);
            s100 = Integer.parseInt(line[5]);
            s250 = Integer.parseInt(line[6]);
        } catch (NumberFormatException e) {
            return -1;
        }
            
        if (total == 0)
            total = s1 + s5 + s25 + s100 + s250;
        
        return total;
    }
    
    
}
