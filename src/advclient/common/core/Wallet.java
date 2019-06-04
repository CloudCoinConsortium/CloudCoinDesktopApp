/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package global.cloudcoin.ccbank.core;

import java.io.File;


/**
 *
 * @author Alexander
 */
public class Wallet {
    String ltag = "Wallet";
    GLogger logger;
    String name;
    String lsep;
    String password;
    boolean isEncrypted;
    String email;
    CloudCoin cc;
    Object uiRef;
    int total;
    Wallet parent;
    String passwordHash;
    int[] sns;
    int[][] counters;
    
    public Wallet(String name, String email, boolean isEncrypted, String password, GLogger logger) {
        this.name = name;
        this.email = email;
        this.isEncrypted = isEncrypted;
        this.password = password;
        this.ltag += " " + name;
        this.logger = logger;
        this.parent = null;
        this.sns = new int[0];
        
        
        logger.debug(ltag, "wallet " + name + " e=" + email + " is="+isEncrypted+ " p="+password);
        lsep = System.getProperty("line.separator");
    }
    
    public boolean isDefaultWallet() {
        return name.equals(Config.DIR_DEFAULT_USER);
    }
    
    public int[] getSNs() {
        return this.sns;
    }
    
    public void setSNs(int[] sns) {
        this.sns = sns;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public int getTotal() {
        return this.total;
    }
    
    public void setuiRef(Object uiRef) {
        this.uiRef = uiRef;
    }
    
    public Object getuiRef() {
        return uiRef;
    }
    
    public void setCounters(int[][] counters) {
        this.counters = counters;
    }
    
    public int[][] getCounters() {
        return this.counters;
    }
    
    
    public boolean isSkyWallet() {
        return this.cc != null;
    }
    
    public void setIDCoin(CloudCoin cc, Wallet parent) {
        this.cc = cc;
        this.parent = parent;
    }
    
    public CloudCoin getIDCoin() {
        return this.cc;
    }
    
    public Wallet getParent() {
        return this.parent;
    }
    
    public boolean isEncrypted() {
        return this.isEncrypted;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getPasswordHash() {
        return this.passwordHash;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public String getTransactionsFileName() {
        String tfFileName = Config.TRANSACTION_FILENAME;
        String rname;
        
        if (isSkyWallet()) {
            tfFileName += "-" + cc.sn;
            rname = parent.getName();
        } else {
            rname = name;
        }
        
        String fileName = AppCore.getUserDir(tfFileName, rname);
        
        return fileName;
    }
    
    public void saveTransations(String dstPath) {
        String fileName = getTransactionsFileName();
        
        AppCore.copyFile(fileName, dstPath);
    }
    
    public String[][] getTransactions() {
        String fileName = getTransactionsFileName();
        
        String data = AppCore.loadFile(fileName);
        if (data == null)
            return null;
        
        String[] parts = data.split("\\r?\\n");
        String[][] rv = new String[parts.length][];
        
        for (int i = 0; i < parts.length; i++) {
            rv[i] = parts[i].split(",");
            if (rv[i].length != 6) {
                logger.error(ltag, "Transaction parse error: " + parts[i]);
                return null;
            }
            
            rv[i][3] = rv[i][3].replace("-", "");
        }
        
        return rv;            
    }
    
    public void appendTransaction(String memo, int amount, String receiptId) {
        appendTransaction(memo, amount, receiptId, AppCore.getCurrentDate());
    }
    
    public void appendTransaction(String memo, int amount, String receiptId, String date) { 
        logger.debug(ltag, "Append transaction " + receiptId + " amount " + amount + " wallet " + getName());
        
        String fileName = getTransactionsFileName();
        String rMemo = memo.replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll(",", " ");
        //String sAmount = Integer.toString(amount);
        
        int rest = 0;
        String[][] tr = getTransactions();
        if (tr != null) {
        
            String[] last = tr[tr.length - 1];
            String rRest = last[4];
            
            try {
                rest = Integer.parseInt(rRest);
            } catch (NumberFormatException e) {
                rest = 0;
            }
            if (rest <= 0)
                rest = 0;
            
        }
                
        String result = rMemo + "," + date + ",";
        if (amount > 0) {
            result += amount + ",,";
        } else {
            result += "," + amount + ",";
        }
        
        rest += amount;   
        result += rest;
        result += "," + receiptId + lsep;
        
        logger.debug(ltag, "Saving " + result);
        AppCore.saveFileAppend(fileName, result, true);              
    }
    
    public void saveSerials(String fileName) {
        
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
}
