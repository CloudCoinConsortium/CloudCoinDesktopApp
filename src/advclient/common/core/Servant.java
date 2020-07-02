package global.cloudcoin.ccbank.core;


import global.cloudcoin.ccbank.ChangeMaker.ChangeMakerResult;
import global.cloudcoin.ccbank.ChangeMaker.ShowChangeResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;


public class Servant {

    private String ltag = "Servant";

    public  String user;
    
    private String name;

    protected RAIDA raida;

    protected GLogger logger;

    protected Config config;

    private Hashtable<String, String> configHT;

    protected String logDir, privateLogDir;

    protected CallbackInterface cb;

    protected Thread thread;

    protected boolean cancelRequest;

    protected ArrayList<CloudCoin> coinsPicked;

    protected int[] valuesPicked;
    
    protected boolean isUserBound;
    
    protected StringBuilder csb;
    
    protected String receiptId;

    public Servant(String name, String rootDir, GLogger logger) {
        this.name = name;
        this.logger = logger;
        this.config = null;
        this.cancelRequest = false;
      
        configHT = new Hashtable<String, String>();

        this.raida = new RAIDA(logger);
        
        File f = new File(rootDir);
        this.user = f.getName();
        setLtag();
                
        this.privateLogDir = AppCore.getPrivateLogDir(this.user) + File.separator + name;
        //AppCore.createDirectoryPath(this.privateLogDir);
        
        AppCore.createDirectory(Config.DIR_MAIN_LOGS + File.separator + name);

        this.isUserBound = true;
        this.logDir = AppCore.getLogDir() + File.separator + name;
        
        logger.info(ltag, "Instantiated servant " + name + " for " + this.user); 
    }

    public RAIDA getRAIDA() {
        return raida;
    }
    
    public void setLtag() {
        ltag = "Servant [" + getClass().getSimpleName() + "] " + this.user + ": ";
    }
    
    public void noUserBound() {
        isUserBound = false;
    }
    
    public boolean isUserBound() {
        return isUserBound;
    }
    
    public void cancelForce() {
        if (raida == null)
            return;

        cancel();
        raida.cancel();
    }
    
    public void cancel() {
        this.cancelRequest = true;
    }

    public void resume() {
        this.cancelRequest = false;
    }

    public boolean isCancelled() {
        return this.cancelRequest;
    }

    public void launch() {
        launch(null);
    }

    public void changeUser(String user) {
        //logger.debug(ltag, "Servant " + name + " changing user to " + user);
        
        this.user = user;
        this.privateLogDir = AppCore.getPrivateLogDir(this.user) + File.separator + name;
        
        configHT = new Hashtable<String, String>();
        
        setLtag();
        readConfig();
        setConfig();
    }
    
    public void launch(CallbackInterface cb) {}

    public void launchDetachedThread(Runnable runnable) {
        thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }

    public void launchThread(Runnable runnable) {
        thread = new Thread(runnable);
        thread.start();
    }

    private boolean checkLatency(int latency, int intLatency) {
        if (latency + intLatency > Config.MAX_ALLOWED_LATENCY) {
            return false;
        }
        return true;
    }

    public boolean updateRAIDAStatus() {
        String[] urls;
        String echoerLogDIr = AppCore.getLogDir() + File.separator + "Echoer";
        String[] parts;
        int raidaNumber;
        String status;
        int latency, intLatency;
        int cntValid = 0;

        urls = new String[RAIDA.TOTAL_RAIDA_COUNT];

        File logDirObj = new File(echoerLogDIr);
        for (File file : logDirObj.listFiles()) {
            if (!file.isDirectory()) {
                String fileName = file.getName();
                logger.debug(ltag, "Checking " + file);

                parts = fileName.toString().split("_");
                if (parts.length != 4) {
                    logger.error(ltag, "Invalid file, skip it: " + file);
                    continue;
                }

                try {
                    raidaNumber = Integer.parseInt(parts[0]);
                    status = parts[1];
                    latency = Integer.parseInt(parts[2]);

                    String[] sparts = parts[3].split("\\.");
                    intLatency = Integer.parseInt(sparts[0]);
                } catch (NumberFormatException e) {
                    logger.error(ltag, "Can't parse file name: " + fileName);
                    continue;
                }

                if (raidaNumber < 0 || raidaNumber > RAIDA.TOTAL_RAIDA_COUNT - 1) {
                    logger.error(ltag, "Invalid raida number: " + raidaNumber);
                    continue;
                }

                if (!status.equals(Config.RAIDA_STATUS_READY)) {
                    logger.error(ltag, "RAIDA" + raidaNumber + " is not ready. Skip it");
                    continue;
                }

                if (!checkLatency(latency, intLatency)) {
                    logger.error(ltag, "RAIDA" + raidaNumber + ". The latency is too high: " + latency + ", " + intLatency);
                    continue;
                }

                String url;
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String data = new String(bytes);

                    JSONObject o = new JSONObject(data);
                    url = o.getString("url");
                } catch (JSONException e) {
                    logger.error(ltag, "Failed to parse JSON " + fileName + ": " + e.getMessage());
                    continue;
                } catch (IOException e) {
                    logger.error(ltag, "Failed to read file " + fileName + ": " + e.getMessage());
                    continue;
                }

                urls[raidaNumber] = url;
                cntValid++;
                
                raida.setLatency(raidaNumber, latency, intLatency);
            }
        }

        raida.setExactUrls(urls);
        
        if (RAIDA.TOTAL_RAIDA_COUNT - cntValid > Config.MAX_ALLOWED_FAILED_RAIDAS) {
            logger.error(ltag, "Only " + cntValid + " raidas are online. Can't proceed");
            return false;
        }
        
        return true;
    }

    public void putConfigValue(String key, String value) {
        configHT.put(key, value);
    }
    
    
    
    public String getConfigText() {
        String data = "<" + name.toUpperCase() + ">";
        
        Set<String> keys = configHT.keySet();
        for (String v : keys) {
            data += v + ":" + configHT.get(v) + "\r\n";
        }
        
        data += "</" +name.toUpperCase() + ">";
        
        return data;
    }
    
    private boolean readConfig() {
        String configFilename = AppCore.getUserConfigDir(user) + File.separator + "config.txt";
        byte[] data;
        String xmlData;

        //logger.debug(ltag, "reading " + configFilename);
        File file = new File(configFilename);
        try {
            if (!file.exists()) {
                // No probleam, actually. Will use default values
                return false;
            } 
            
            data = Files.readAllBytes(Paths.get(configFilename));
            xmlData = new String(data);
        } catch (IOException e) {
            logger.error(ltag, "Failed to read config file: " + e.getMessage());
            return false;
        }

        return parseConfigData(xmlData);
    }

    private boolean parseConfigData(String xmlData) {
        String tagName = this.name.toUpperCase();
        if (xmlData.indexOf("<" + tagName + ">") == -1)
            return true;
        
        String regex = ".*?<" + tagName + ">(.*)</" + tagName + ">.*";

        xmlData = xmlData.replaceAll("\\n", "***");
        xmlData = xmlData.replaceAll("\\r", "");
        xmlData = xmlData.replaceAll("\\t", "");
        xmlData = xmlData.replaceAll(" ", "");
        
        xmlData = xmlData.replaceAll(regex, "$1");
        
        String[] parts = xmlData.split("\\*\\*\\*");   
        for (String item : parts) {
            if (item.equals(""))
                continue;

            String[] subParts = item.split(":");
            if (subParts.length < 2) {
                logger.error(ltag, "Failed to parse config value " + item);
                continue;
            }

            String k, rest = "";

            k = subParts[0].trim();
            rest = subParts[1].trim();

            for (int i = 2; i < subParts.length; i++)
                rest += ":" + subParts[i].trim();

            logger.debug(ltag, "serv " + name + "-> " + k + ":" + rest);
            configHT.put(k, rest);
        }

        return true;
    }
    
    public void setConfig() {
        
    }

    public String getConfigValue(String key) {
        return configHT.get(key);
    }

    public int getIntConfigValue(String key) {
        String val = getConfigValue(key);
        if (val == null)
            return -1;

        int value;
        try {
            value = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return -1;
        }

        return value;
    }

    public void setLogger(GLogger logger) {
        this.logger = logger;
    }


    private boolean doSetField(Field f, JSONObject o, Object targetObject) throws IllegalAccessException, JSONException {
        String name = f.getName();

        f.setAccessible(true);
        if (f.getType() == int.class) {
            int value = o.optInt(name);
            f.set(targetObject, value);
        } else if (f.getType() == String.class) {
            String value = o.optString(name);
            f.set(targetObject, value);
        } else if (f.getType() == int[].class) {
            int length;
            JSONArray a = o.optJSONArray(name);

            if (a != null)
                length = a.length();
            else
                length = 0;

            int[] ia = new int[length];
            for (int i = 0; i < length; i++) {
                ia[i] = a.getInt(i);
            }

            f.set(targetObject, ia);
        } else if (f.getType() == String[].class) {
            int length;
            JSONArray a = o.optJSONArray(name);

            if (a != null)
                length = a.length();
            else
                length = 0;

            String[] ia = new String[length];
            for (int i = 0; i < length; i++) {
                ia[i] = a.getString(i);
            }

            f.set(targetObject, ia);
        } else {
            logger.error(ltag, "Invalid type: " + f.getType());
            return false;
        }



        return true;
    }

    private Object setFields(Class c, JSONObject o) {
        Object targetObject;

        try {
            targetObject = c.newInstance();
            for (Field f : c.getDeclaredFields()) {
                if (!doSetField(f, o, targetObject))
                    return null;
            }

            if (c.getSuperclass() != null && c.getSuperclass() == CommonResponse.class) {
                for (Field f : c.getSuperclass().getDeclaredFields()) {
                    if (!doSetField(f, o, targetObject))
                        return null;
                }
            }
        } catch (IllegalAccessException e) {
            logger.error(ltag, "Illegal access exception");
            return null;
        } catch (InstantiationException e) {
            logger.error(ltag, "Illegal instantiation");
            return null;
        } catch (IllegalArgumentException e) {
            logger.error(ltag, "Illegal argument: " + e.getMessage());
            return null;
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse JSON: " + e.getMessage());
            return null;
        }

        return targetObject;
    }

    public Object[] parseArrayResponse(String string, Class c) {
        Object[] newObjectArray;
        JSONArray a;
        JSONObject o;

        if (string == null)
            return null;

        try {
            a = new JSONArray(string.trim());
            newObjectArray = new Object[a.length()];
            for (int i = 0; i < a.length(); i++) {
                o = a.getJSONObject(i);
                newObjectArray[i] = setFields(c, o);
                if (newObjectArray[i] == null)
                    return null;
            }
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse json: " + e.getMessage());
            return null;
        }

        return newObjectArray;
    }

    public Object parseResponse(String string, Class c) {
        JSONObject o;
        Object newObject;

        if (string == null)
            return null;

        try {
            o = new JSONObject(string.trim());
            newObject = setFields(c, o);
            if (newObject == null)
                return null;
        } catch (JSONException e) {
            logger.error(ltag, "Failed to parse json: " + e.getMessage());
            return null;
        }

        return newObject;
    }

    protected void cleanDir(String dir) {
        File dirObj = new File(dir);
        if (dirObj == null) {
            logger.error(ltag, "No dir found: " + dir);
            return;
        }

        File[] files = dirObj.listFiles();
        if (files == null) {
            //logger.debug(ltag, "Skipping dir " + dir);
            return;
        }
        
        for (File file: files) {
            if (!file.isDirectory()) {
                //logger.debug(ltag, "Deleting " + file);
                file.delete();
            }
        }
    }

    protected void cleanLogDir() {
        cleanDir(logDir);
    }

    protected void cleanPrivateLogDir() {
        cleanDir(privateLogDir);
    }

    protected CloudCoin getIDcc(int sn) {
        String fullDirIDPath = AppCore.getIDDir();
        CloudCoin cc = null;

        File dirObj = new File(fullDirIDPath);
        for (File file: dirObj.listFiles()) {
            if (file.isDirectory())
                continue;

            if (!AppCore.hasCoinExtension(file))
                continue;
            
            logger.debug(ltag, "Parsing " + file);

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse JSON: " + e.getMessage());
                continue;
            }

            logger.info(ltag, "Found SN: " + cc.sn);
            if (cc.sn != sn)
                continue;

            break;
        }

        return cc;
    }

    protected boolean collectedEnough(int[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != valuesPicked[i]) {
                return false;
            }
        }

        return true;
    }

    protected void pickCoin(int idx, int[] values, CloudCoin cc) {
        if (values[idx] > valuesPicked[idx]) {
            logger.debug(ltag, "Picking coin " + cc.sn);

            valuesPicked[idx]++;
            coinsPicked.add(cc);
        }
    }

    public boolean pickCoinsInDir(String dir, int[] values) {
        logger.debug(ltag, "Looking into dir: " + dir);

        CloudCoin cc;
        int denomination;

        File dirObj = new File(dir);
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

            denomination = cc.getDenomination();
            if (denomination == 1) {
                pickCoin(Config.IDX_1, values, cc);
            } else if (denomination == 5) {
                pickCoin(Config.IDX_5, values, cc);
            } else if (denomination == 25) {
                pickCoin(Config.IDX_25, values, cc);
            } else if (denomination == 100) {
                pickCoin(Config.IDX_100, values, cc);
            } else if (denomination == 250) {
                pickCoin(Config.IDX_250, values, cc);
            }

            if (collectedEnough(values)) {
                logger.debug(ltag, "Collected enough. Stop");
                return true;
            }
        }

        return false;
    }
    
    public int[] countCoinsFromArray(int[] coins) {
        int[] totals = new int[6];
        CloudCoin cc;
        int denomination;

        for (int i = 0; i < coins.length; i++) {
            cc = new CloudCoin(Config.DEFAULT_NN, coins[i]);

            denomination = cc.getDenomination();
            if (denomination == 1)
                totals[Config.IDX_1]++;
            else if (denomination == 5)
                totals[Config.IDX_5]++;
            else if (denomination == 25)
                totals[Config.IDX_25]++;
            else if (denomination == 100)
                totals[Config.IDX_100]++;
            else if (denomination == 250)
                totals[Config.IDX_250]++;
            else
                continue;

            totals[Config.IDX_TOTAL] += denomination;
        }

        return totals;
    }
    

    public int[] countCoins(String dir) {
        int[] totals = new int[6];
        CloudCoin cc;
        int denomination;

        File dirObj = new File(dir);
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

            denomination = cc.getDenomination();
            if (denomination == 1)
                totals[Config.IDX_1]++;
            else if (denomination == 5)
                totals[Config.IDX_5]++;
            else if (denomination == 25)
                totals[Config.IDX_25]++;
            else if (denomination == 100)
                totals[Config.IDX_100]++;
            else if (denomination == 250)
                totals[Config.IDX_250]++;
            else
                continue;

            totals[Config.IDX_TOTAL] += denomination;
        }

        return totals;
    }

    public int[] getExpCoins(int amount, int[] totals) {
        return getExpCoins(amount, totals, false);
    }
    
    public int[] getExpCoins(int amount, int[] totals, boolean loose) {
        int savedAmount = amount;
   
        if (amount > totals[Config.IDX_TOTAL]) {
            logger.error(ltag, "Not enough coins");
            return null;
        }

        if (amount < 0)
            return null;
        
        if (loose)
            logger.debug(ltag, "isLoose " + loose);
        
        for (int i = 0; i < totals.length; i++)
            logger.debug(ltag, "v=" + totals[i]);

        int exp_1, exp_5, exp_25, exp_100, exp_250;

        exp_1 = exp_5 = exp_25 = exp_100 = exp_250 = 0;
        for (int i = 0; i < 2; i++) {
            exp_1 = exp_5 = exp_25 = exp_100 = 0;

            if (i == 0 && amount >= 250 && totals[Config.IDX_250] > 0) {
                exp_250 = ((amount / 250) < (totals[Config.IDX_250])) ? (amount / 250) : (totals[Config.IDX_250]);
                amount -= (exp_250 * 250);
            }

            if (amount >= 100 && totals[Config.IDX_100] > 0) {
                exp_100 = ((amount / 100) < (totals[Config.IDX_100])) ? (amount / 100) : (totals[Config.IDX_100]);
                amount -= (exp_100 * 100);
            }

            if (amount >= 25 && totals[Config.IDX_25] > 0) {
                exp_25 = ((amount / 25) < (totals[Config.IDX_25])) ? (amount / 25) : (totals[Config.IDX_25]);
                amount -= (exp_25 * 25);
            }

            if (amount >= 5 && totals[Config.IDX_5] > 0) {
                exp_5 = ((amount / 5) < (totals[Config.IDX_5])) ? (amount / 5) : (totals[Config.IDX_5]);
                amount -= (exp_5 * 5);
            }

            if (amount >= 1 && totals[Config.IDX_1] > 0) {
                exp_1 = (amount < (totals[Config.IDX_1])) ? amount : (totals[Config.IDX_1]);
                amount -= (exp_1);
            }
            
            logger.debug(ltag, "Denom: " + exp_1 + "/" + exp_5 + "/" + exp_25 + "/" + exp_100 + "/" + exp_250 + " amount = " + amount); 
            if (amount == 0)
                break;
            
            if (i == 1 || exp_250 == 0) {
                if (loose)
                    break;
                
                logger.error(ltag, "Can't collect needed amount of coins. rest: " + amount);
                return null;
            }
            
            exp_250--;
            amount = savedAmount - exp_250 * 250;
        }
       
        int[] rv = new int[5];
        
        rv[Config.IDX_1] = exp_1;
        rv[Config.IDX_5] = exp_5;
        rv[Config.IDX_25] = exp_25;
        rv[Config.IDX_100] = exp_100;
        rv[Config.IDX_250] = exp_250;
        
        return rv;
        
    }
    
    public void pickCoins(File[] files, int[] exps) {
        int denomination;
        CloudCoin cc;
        
        for (File file : files) {
            if (file.isDirectory())
                continue;

            try {
                cc = new CloudCoin(file.toString());
            } catch (JSONException e) {
                logger.error(ltag, "Failed to parse coin: " + file.toString() +
                        " error: " + e.getMessage());

                continue;
            }

            denomination = cc.getDenomination();
            if (denomination == 1) {
                if (exps[Config.IDX_1]-- > 0) {
                    logger.info(ltag, "Adding 1: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            } else if (denomination == 5) {
                if (exps[Config.IDX_5]-- > 0) {
                    logger.info(ltag, "Adding 5: " + cc.sn);
                    coinsPicked.add(cc);
                }
            } else if (denomination == 25) {
                if (exps[Config.IDX_25]-- > 0) {
                    logger.info(ltag, "Adding 25: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            } else if (denomination == 100) {
                if (exps[Config.IDX_100]-- > 0) {
                    logger.info(ltag, "Adding 100: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            } else if (denomination == 250) {
                if (exps[Config.IDX_250]-- > 0) {
                    logger.info(ltag, "Adding 250: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            }
        }
    }
    
    public CloudCoin pickCoinsAmountFromArrayWithExtra(int[] coins, int amount) {
        int[] totals, exps;
        int denomination;
        CloudCoin cc = null;
        
        totals = countCoinsFromArray(coins);
        exps = getExpCoins(amount, totals, true);
        
        int collected, rest;
        
        collected = rest = 0;
        for (int i = 0; i < coins.length; i++) {
            cc = new CloudCoin(Config.DEFAULT_NN, coins[i]);
            denomination = cc.getDenomination();
            if (denomination == 1) {
                if (exps[Config.IDX_1]-- > 0) {
                    logger.info(ltag, "Adding 1: " + cc.sn);
                    coinsPicked.add(cc);
                    collected += cc.getDenomination();
                } 
            } else if (denomination == 5) {
                if (exps[Config.IDX_5]-- > 0) {
                    logger.info(ltag, "Adding 5: " + cc.sn);
                    coinsPicked.add(cc);
                    collected += cc.getDenomination();
                }
            } else if (denomination == 25) {
                if (exps[Config.IDX_25]-- > 0) {
                    logger.info(ltag, "Adding 25: " + cc.sn);
                    coinsPicked.add(cc);
                    collected += cc.getDenomination();
                } 
            } else if (denomination == 100) {
                if (exps[Config.IDX_100]-- > 0) {
                    logger.info(ltag, "Adding 100: " + cc.sn);
                    coinsPicked.add(cc);
                    collected += cc.getDenomination();
                } 
            } else if (denomination == 250) {
                if (exps[Config.IDX_250]-- > 0) {
                    logger.info(ltag, "Adding 250: " + cc.sn);
                    coinsPicked.add(cc);
                    collected += cc.getDenomination();
                } 
            } 
        }
                    
        boolean isAdded;
        rest = amount - collected;
        logger.debug(ltag, "rest = " + rest);
        for (int i = 0; i < coins.length; i++) {
            cc = new CloudCoin(Config.DEFAULT_NN, coins[i]);
            denomination = cc.getDenomination();
            
            if (rest > denomination)
                continue;
            
            isAdded = false;
            for (CloudCoin xcc : coinsPicked) {
                if (xcc.sn == cc.sn) {
                    isAdded = true;
                    break;
                }
            }
            
            if (isAdded) {
                logger.debug(ltag, "Adding change: skipp added sn " + cc.sn);
                continue;
            }
            
            logger.debug(ltag, "Chosen for change: " + cc.sn + " denomination " + cc.getDenomination());
            break;
        }

        return cc;
    }
    
    public boolean pickCoinsAmountFromArray(int[] coins, int amount) {
        int[] totals, exps;
        CloudCoin cc;
        int denomination;
        
        totals = countCoinsFromArray(coins);
        exps = getExpCoins(amount, totals);
        if (exps == null) {
            logger.error(ltag, "Failed to pick coins");
            return false;
        }
        
        for (int i = 0; i < coins.length; i ++) {
            cc = new CloudCoin(Config.DEFAULT_NN, coins[i]);
            denomination = cc.getDenomination();
            if (denomination == 1) {
                if (exps[Config.IDX_1]-- > 0) {
                    logger.info(ltag, "Adding 1: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            } else if (denomination == 5) {
                if (exps[Config.IDX_5]-- > 0) {
                    logger.info(ltag, "Adding 5: " + cc.sn);
                    coinsPicked.add(cc);
                }
            } else if (denomination == 25) {
                if (exps[Config.IDX_25]-- > 0) {
                    logger.info(ltag, "Adding 25: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            } else if (denomination == 100) {
                if (exps[Config.IDX_100]-- > 0) {
                    logger.info(ltag, "Adding 100: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            } else if (denomination == 250) {
                if (exps[Config.IDX_250]-- > 0) {
                    logger.info(ltag, "Adding 250: " + cc.sn);
                    coinsPicked.add(cc);
                } 
            }
        }
      
        return true;
    }
    
    
    public boolean pickCoinsAmountInDir(String dir, int amount) {
        int[] totals, exps;
 

        totals = countCoins(dir);
        exps = getExpCoins(amount, totals);
        if (exps == null) {
            logger.error(ltag, "Failed to pick coins");
            return false;
        }

        File dirObj = new File(dir);
        File[] files = dirObj.listFiles();
        if (files == null) {
            logger.error(ltag, "Can't read dir " + dir);
            return false;
        }
        
        pickCoins(dirObj.listFiles(), exps);
        
        return true;
    }
    
    
    public boolean pickCoinsAmountInDirs(String dir0, String dir1, int amount) {
        int[] totals, totals0, totals1, exps;
        File dirObj0, dirObj1;
        File[] files0, files1;

        totals0 = countCoins(dir0);
        totals1 = countCoins(dir1);
        
        totals = new int[6];
        
        totals[Config.IDX_TOTAL] = totals0[Config.IDX_TOTAL] + totals1[Config.IDX_TOTAL];
        totals[Config.IDX_1] = totals0[Config.IDX_1] + totals1[Config.IDX_1];
        totals[Config.IDX_5] = totals0[Config.IDX_5] + totals1[Config.IDX_5];
        totals[Config.IDX_25] = totals0[Config.IDX_25] + totals1[Config.IDX_25];
        totals[Config.IDX_100] = totals0[Config.IDX_100] + totals1[Config.IDX_100];
        totals[Config.IDX_250] = totals0[Config.IDX_250] + totals1[Config.IDX_250];
               
        exps = getExpCoins(amount, totals);
        if (exps == null) {
            logger.error(ltag, "Failed to pick coins");
            return false;
        }
       
        logger.debug(ltag, "Looking into dir: " + dir0 + " and " + dir1 + " for " + amount + " coins: " + totals[5]);
    
        dirObj0 = new File(dir0);
        files0 = dirObj0.listFiles();
        if (files0 == null) {
            logger.error(ltag, "Can't read dir " + dir0);
            return false;
        }
       
        dirObj1 = new File(dir1);
        files1 = dirObj1.listFiles();
        if (files1 == null) {
            logger.error(ltag, "Can't read dir " + dir1);
            return false;
        }
        
        File[] allFiles = new File[files0.length + files1.length];
        int pos = 0;
        for (File e : files0) {
            allFiles[pos] = e;
            pos++;
        }
        
        for (File e : files1) {
            allFiles[pos] = e;
            pos++;
        }
        
        pickCoins(allFiles, exps);
             
        return true;
    }

    public void addCoinToReceipt(CloudCoin cc, String status, String dstFolder) {
        if (!csb.toString().equals(""))
            csb.append(",");

        csb.append("{\"nn.sn\": \"");
        csb.append(cc.nn);
        csb.append(".");
        csb.append(cc.sn);
        csb.append("\", \"status\": \"");
        csb.append(status);
        csb.append("\", \"pown\": \"");
        csb.append(cc.getPownString());
        csb.append("\", \"note\": \"Moved to ");
        csb.append(dstFolder);
        csb.append("\"}");

    }
    
    public void saveReceipt(String duser, int a, int c, int f, int l, int u, int dups) {
        logger.debug(ltag, "Saving receipt " + duser + ": " + a + "," + c + "," + f + "," + l + "," + u + "," + dups);
        
        StringBuilder rsb = new StringBuilder();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:a");
        SimpleDateFormat formatterTz = new SimpleDateFormat("z");
        Date date = new Date(System.currentTimeMillis());

        String cdate = formatter.format(date);
        String cdateFormat = formatterTz.format(date);

        rsb.append("{\"receipt_id\": \"");
        rsb.append(receiptId);
        rsb.append("\", \"time\": \"");
        rsb.append(cdate);
        rsb.append("\", \"timezone\": \"");
        rsb.append(cdateFormat);
        rsb.append("\", \"total_authentic\": ");
        rsb.append(a);
        rsb.append(", \"total_fracked\": ");
        rsb.append(f);
        rsb.append(", \"total_counterfeit\": ");
        rsb.append(c);
        rsb.append(", \"total_lost\": ");
        rsb.append(l);
        rsb.append(", \"total_unchecked\": ");
        rsb.append(u);
        rsb.append(", \"prev_imported\": ");
        rsb.append(dups);
        rsb.append(", \"receipt_detail\": [");
        rsb.append(csb);
        rsb.append("]}");

        String fileName = AppCore.getUserDir(Config.DIR_RECEIPTS, duser) + File.separator + receiptId + ".txt";
        File fn = new File(fileName);
        if (fn.exists()) {
            logger.debug(ltag, "Deleting previous receipt " + fileName);
            fn.delete();
        }
        
        if (!AppCore.saveFile(fileName, rsb.toString())) {
            logger.error(ltag, "Failed to save file " + fileName);
        } 
    }
    
    
    protected int[] getA(int[] a, int cnt) {
        int[] sns;
        int i, j;

        sns = new int[cnt];
        for (i = 0, j = 0; i < a.length; i++) {
            if (a[i] == 0)
                continue;

            sns[j] = a[i];
            a[i] = 0;
            j++;

            if (j == cnt)
                break;
        }

        if (j != cnt)
            return null;

        return sns;
    }

    protected int[] get25B(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[9];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 5] = sns[i];

        return rsns;
    }

    protected int[] get25C(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[17];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 5] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 10] = sns[i];

        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i + 15] = sns[i];

        return rsns;
    }

    protected int[] get25D(int[] sb) {
        int[] sns, rsns;
        int j;

        rsns = new int[25];
        for (j = 0; j < 5; j++) {
            sns = getA(sb, 5);
            if (sns == null)
                return null;

            for (int i = 0; i < 5; i++)
                rsns[j * 5 + i] = sns[i];
        }

        return rsns;
    }

    protected int[] get100B(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[8];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 3);
        if (sns == null)
            return null;

        for (int i = 0; i < 3; i++)
            rsns[i + 5] = sns[i];

        return rsns;
    }

    protected int[] get100C(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[16];

        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 9] = sns[i];

        sns = getA(sb, 2);
        for (int i = 0; i < 2; i++)
            rsns[i + 14] = sns[i];

        return rsns;
    }

    protected int[] get100D(int[] sb) {
        int[] sns, rsns;
        int j;

        rsns = new int[100];
        for (j = 0; j < 4; j++) {
            sns = get25D(sb);
            if (sns == null)
                return null;

            for (int i = 0; i < 25; i++)
                rsns[j * 25 + i] = sns[i];
        }

        return rsns;
    }
    
    protected int[] get100E(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[12];

        sns = getA(sb, 3);
        if (sns == null)
            return null;

        for (int i = 0; i < 3; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 3] = sns[i];
        
        sns = getA(sss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 7] = sns[i];

        return rsns;
    }
    

    protected int[] get250A(int[] sb, int[] ss) {
        int[] sns, rsns;

        rsns = new int[4];

        sns = getA(ss, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i] = sns[i];

        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i + 2] = sns[i];

        return rsns;
    }

    protected int[] get250B(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[22];
        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 9] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 14] = sns[i];

        sns = getA(sb, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 18] = sns[i];

        return rsns;
    }

    protected int[] get250C(int[] sb, int[] ss, int[] sss) {
        int[] sns, rsns;

        rsns = new int[42];
        sns = get25C(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 17; i++)
            rsns[i] = sns[i];

        sns = get25B(ss, sss);
        if (sns == null)
            return null;

        for (int i = 0; i < 9; i++)
            rsns[i + 17] = sns[i];

        sns = get100B(sb, ss);
        if (sns == null)
            return null;

        for (int i = 0; i < 8; i++)
            rsns[i + 26] = sns[i];

        sns = get100B(sb, ss);
        if (sns == null)
            return null;

        for (int i = 0; i < 8; i++)
            rsns[i + 34] = sns[i];

        return rsns;
    }

    protected int[] get250D(int[] sb) {
        int[] sns, rsns;

        rsns = new int[250];

        sns = get25D(sb);
        if (sns == null)
           return null;

        for (int i = 0; i < 25; i++)
           rsns[i] = sns[i];

        sns = get25D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 25; i++)
            rsns[i + 25] = sns[i];


        sns = get100D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 100; i++)
            rsns[i + 50] = sns[i];

        sns = get100D(sb);
        if (sns == null)
            return null;

        for (int i = 0; i < 100; i++)
            rsns[i + 150] = sns[i];

        return rsns;
    }
    
    protected int[] get250E(int[] sb, int[] ss, int[] sss, int[] ssss) {
        int[] sns, rsns;

        rsns = new int[12];
        sns = getA(sb, 2);
        if (sns == null)
            return null;

        for (int i = 0; i < 2; i++)
            rsns[i] = sns[i];

        sns = getA(ss, 1);
        if (sns == null)
            return null;

        rsns[2] = sns[0];

        sns = getA(sss, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 3] = sns[i];

        sns = getA(ssss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 7] = sns[i];

        return rsns;
    }
    
    protected int[] get250F(int[] sb, int[] ss, int[] sss, int[] ssss) {
        int[] sns, rsns;

        rsns = new int[15];
        sns = getA(sb, 1);
        if (sns == null)
            return null;        
        rsns[0] = sns[0];


        sns = getA(ss, 5);
        if (sns == null)
            return null;
        for (int i = 0; i < 5; i++)
            rsns[i + 1] = sns[i];

        sns = getA(sss, 4);
        if (sns == null)
            return null;

        for (int i = 0; i < 4; i++)
            rsns[i + 6] = sns[i];

        sns = getA(ssss, 5);
        if (sns == null)
            return null;

        for (int i = 0; i < 5; i++)
            rsns[i + 10] = sns[i];

        return rsns;
    }
    
    
    protected int[] showChange(int method, CloudCoin cc) {
        String resultMain;
        String[] results;
        String[] requests;
        StringBuilder[] sbs;
        
        String seed = AppCore.generateHex().substring(0, 8);
        
        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            sbs[i] = new StringBuilder();
            sbs[i].append("show_change?nn=");
            sbs[i].append(cc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(Config.PUBLIC_CHANGE_MAKER_ID);
            sbs[i].append("&seed=");
            sbs[i].append(seed);
            sbs[i].append("&denomination=");
            sbs[i].append(cc.getDenomination());
            
            requests[i] = sbs[i].toString();
        }
        
        results = raida.query(requests, null, null);
        if (results == null) {
            logger.error(ltag, "Failed to query showchange");
            return null;
        }

        int[][] rsns1 = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        int[][] rsns5 = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        int[][] rsns25 = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        int[][] rsns100 = new int[RAIDA.TOTAL_RAIDA_COUNT][];
        for (int i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            rsns1[i] = new int[0];  
            rsns5[i] = new int[0];
            rsns25[i] = new int[0];
            rsns100[i] = new int[0];
            if (results[i] != null) {
                if (results[i].equals("")) {
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }
            
            ShowChangeResponse scr = (ShowChangeResponse) parseResponse(results[i], ShowChangeResponse.class);
            if (scr == null) {
                logger.error(ltag, "Failed to get response coin. RAIDA: " + i);
                continue;
            }
        
            if (!scr.status.equals(Config.REQUEST_STATUS_PASS)) {
                logger.error(ltag, "Failed to show env coins. RAIDA: " + i + " Result: " + scr.status);       
                continue;
            }

            rsns1[i] = scr.d1;
            rsns5[i] = scr.d5;
            rsns25[i] = scr.d25;
            rsns100[i] = scr.d100;
        }
        
        int[] vsns1, vsns5, vsns25, vsns100;      
        vsns1 = AppCore.getSNSOverlap(rsns1);
        if (vsns1 == null) {
            logger.error(ltag, "Failed to get coins for denomination #1");
            return null;
        }
        
        vsns5 = AppCore.getSNSOverlap(rsns5);
        if (vsns5 == null) {
            logger.error(ltag, "Failed to get coins for denomination #5");
            return null;
        }
        
        vsns25 = AppCore.getSNSOverlap(rsns25);
        if (vsns25 == null) {
            logger.error(ltag, "Failed to get coins for denomination #25");
            return null;
        }
        
        vsns100 = AppCore.getSNSOverlap(rsns100);
        if (vsns100 == null) {
            logger.error(ltag, "Failed to get coins for denomination #100");
            return null;
        }
        
        int[] sns;
        switch (method) {
            case Config.CHANGE_METHOD_5A:
                sns = getA(vsns1, 5);
                break;
            case Config.CHANGE_METHOD_25A:
                sns = getA(vsns5, 5);
                break;
            case Config.CHANGE_METHOD_25B:
                sns = get25B(vsns5, vsns1);
                break;
            case Config.CHANGE_METHOD_25C:
                sns = get25C(vsns5, vsns1);
                break;
            case Config.CHANGE_METHOD_25D:
                sns = get25D(vsns1);
                break;
            case Config.CHANGE_METHOD_100A:
                sns = getA(vsns25, 4);
                break;
            case Config.CHANGE_METHOD_100B:
                sns = get100B(vsns25, vsns5);
                break;
            case Config.CHANGE_METHOD_100C:
                sns = get100C(vsns25, vsns5, vsns1);
                break;
            case Config.CHANGE_METHOD_100D:
                sns = get100D(vsns1);
                break;
            case Config.CHANGE_METHOD_100E:
                sns = get100E(vsns25, vsns5, vsns1);
                break;    
            case Config.CHANGE_METHOD_250A:
                sns = get250A(vsns100, vsns25);
                break;
            case Config.CHANGE_METHOD_250B:
                sns = get250B(vsns25, vsns5, vsns1);
                break;
            case Config.CHANGE_METHOD_250C:
                sns = get250C(vsns25, vsns5, vsns1);
                break;
            case Config.CHANGE_METHOD_250D:
                sns = get250D(vsns1);
                break;
            case Config.CHANGE_METHOD_250E:
                sns = get250E(vsns100, vsns25, vsns5, vsns1);
                break;
            case Config.CHANGE_METHOD_250F:
                sns = get250F(vsns100, vsns25, vsns5, vsns1);
                break;
            default:
                logger.error(ltag, "Invalid method: " + method);
                return null;
        }
        
        if (sns == null) {
            logger.error(ltag, "Failed to break coin");
            return null;
        }
        
        return sns;
    }
    
    public int[] breakInBank(CloudCoin tcc, CloudCoin idcc, CallbackInterface cb)  {       
        String[] results;
        CommonResponse errorResponse;
        String[] requests;
        StringBuilder[] sbs;
        String[] posts;
        int i;
        CloudCoin[] sccs;

        logger.debug(ltag, "Breaking coin " + tcc.sn + ". ID coin " + idcc.sn);
        
        int method = AppCore.getChangeMethod(tcc.getDenomination());
        if (method == 0) {
            logger.error(ltag, "Can't find suitable method");
            return null;
        }
   
        logger.debug(ltag, "Method chosen: " + method);
        
        int sns[] = showChange(method, tcc);
        if (sns == null) {
            logger.error(ltag, "Not enough coins to make change");
            return null;
        }

        for (i = 0; i < sns.length; i++) {
            CloudCoin ccx = new CloudCoin(1, sns[i]);
            logger.info(ltag, "sn "+ sns[i] + " d="+ccx.getDenomination());
        }

        for (i = 0; i < sns.length; i++) {
            CloudCoin xcc = new CloudCoin(Config.DEFAULT_NN, sns[i]);
            logger.debug(ltag, "Picked for change: " + xcc.sn + " " + xcc.getDenomination());            
        }

        sbs = new StringBuilder[RAIDA.TOTAL_RAIDA_COUNT];
        requests = new String[RAIDA.TOTAL_RAIDA_COUNT];

        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            //idcc.ans[i] = "fa08fa80ba6dba05165eb670b0f3beb0";
            sbs[i] = new StringBuilder();
            sbs[i].append("break_in_bank?");
            sbs[i].append("id_nn=");
            sbs[i].append(idcc.nn);
            sbs[i].append("&id_sn=");
            sbs[i].append(idcc.sn);
            sbs[i].append("&id_an=");
            sbs[i].append(idcc.ans[i]);
            sbs[i].append("&id_dn=");
            sbs[i].append(idcc.getDenomination());
            sbs[i].append("&nn=");
            sbs[i].append(tcc.nn);
            sbs[i].append("&sn=");
            sbs[i].append(tcc.sn);
            sbs[i].append("&dn=");
            sbs[i].append(tcc.getDenomination());            
            sbs[i].append("&change_server=");
            sbs[i].append(Config.PUBLIC_CHANGE_MAKER_ID);

            for (int j = 0; j < sns.length; j++) {
                sbs[i].append("&csn[]=");
                sbs[i].append(sns[j]);
            }

            requests[i] = sbs[i].toString();
        }
        
        results = raida.query(requests, null, cb);      
        if (results == null) {
            logger.error(ltag, "Failed to query Break in Bank");
            return null;
        }
        
        CloudCoin fakeCC = new CloudCoin(Config.DEFAULT_NN, 1);
        for (i = 0; i < RAIDA.TOTAL_RAIDA_COUNT; i++) {
            logger.info(ltag, "i="+i+ " r="+results[i]);
            if (results[i] != null) {
                if (results[i].equals("")) {
                    fakeCC.setDetectStatus(i, CloudCoin.STATUS_NORESPONSE);
                    logger.error(ltag, "Skipped raida" + i);
                    continue;
                }
            }
            
            if (results[i] == null) {
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_NORESPONSE);
                logger.error(ltag, "Skipped raida due to empty response " + i);
                continue;
            }

            CommonResponse o = (CommonResponse) parseResponse(results[i], CommonResponse.class);
            if (o == null) {
                errorResponse = (CommonResponse) parseResponse(results[i], CommonResponse.class);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                if (errorResponse == null) {
                    logger.error(ltag, "Failed to get error");
                    continue;
                }

                logger.error(ltag, "Failed to auth coin. Status: " + errorResponse.status);
                continue;
            }
            
            if (o.status.equals("success")) {
                logger.debug(ltag, "Change from RAIDA " + i + " OK");
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_PASS);
                continue;
            } else if (o.status.equals(Config.REQUEST_STATUS_FAIL)) {
                logger.error(ltag, "Failed status from raida " + i + " status: " + o.status);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_FAIL);
                continue;
            } else {
                logger.error(ltag, "Invalid status from raida " + i + " status: " + o.status);
                fakeCC.setDetectStatus(i, CloudCoin.STATUS_ERROR);
                continue;
            }
        }

        fakeCC.setPownStringFromDetectStatus();
        logger.debug(ltag, "Change: " + fakeCC.getPownString());

        
        if (fakeCC.isSentFixable()) {
            logger.debug(ltag, "Break completed successfully");
            return sns;
        }
        
        
        logger.info(ltag, "Break failed");

        //for (i = 0; i < sns.length; i++) { coinsPicked.add(new CloudCoin(Config.DEFAULT_NN, sns[i]));     }  return true;
        //return false;
        
        return null;
        //return null;
    }
    
}
