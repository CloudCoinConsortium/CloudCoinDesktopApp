/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import global.cloudcoin.ccbank.core.CloudCoin;
import global.cloudcoin.ccbank.core.Wallet;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 *
 * @author Alexander
 */
public class ProgramState {
    final public static int SCREEN_AGREEMENT = 1;
    final public static int SCREEN_CREATE_WALLET = 2;
    final public static int SCREEN_DEFAULT = 3;
    final public static int SCREEN_SET_PASSWORD = 4;
    final public static int SCREEN_UNDERSTAND_PASSWORD = 5;
    final public static int SCREEN_SET_EMAIL = 6;
    final public static int SCREEN_WALLET_CREATED = 7;
    final public static int SCREEN_PREPARE_TO_ADD_WALLET = 8;
    final public static int SCREEN_CREATE_SKY_WALLET = 9;
    final public static int SCREEN_SHOW_TRANSACTIONS = 10;
    final public static int SCREEN_DEPOSIT = 11;
    final public static int SCREEN_WITHDRAW = 12;
    final public static int SCREEN_IMPORTING = 13;
    final public static int SCREEN_IMPORT_DONE = 14;
    final public static int SCREEN_SUPPORT = 15;
    final public static int SCREEN_CONFIRM_TRANSFER = 16;
    final public static int SCREEN_TRANSFER_DONE = 17;
    final public static int SCREEN_TRANSFERRING = 18;
    final public static int SCREEN_BACKUP = 19;
    final public static int SCREEN_SENDING = 20;
    final public static int SCREEN_BACKUP_DONE = 21;
    final public static int SCREEN_CLEAR = 22;
    final public static int SCREEN_LIST_SERIALS = 23;
    final public static int SCREEN_LIST_SERIALS_DONE = 24;
    final public static int SCREEN_CONFIRM_CLEAR = 25;
    final public static int SCREEN_CLEAR_DONE = 26;
    final public static int SCREEN_FIX_FRACKED = 27;
    final public static int SCREEN_FIXING_FRACKED = 28;
    final public static int SCREEN_FIX_DONE = 29;
    final public static int SCREEN_DELETE_WALLET = 30;
    final public static int SCREEN_CONFIRM_DELETE_WALLET = 31;
    final public static int SCREEN_DELETE_WALLET_DONE = 32;
    final public static int SCREEN_SKY_WALLET_CREATED = 33;
    final public static int SCREEN_PREDEPOSIT = 34;
    final public static int SCREEN_DEPOSIT_SKY_WALLET = 35;
    final public static int SCREEN_SHOW_FOLDERS = 36;
    final public static int SCREEN_ECHO_RAIDA = 37;
    final public static int SCREEN_ECHO_RAIDA_FINISHED = 38;
    final public static int SCREEN_MAKING_CHANGE = 39;
    
    
    final static int CB_STATE_INIT = 1;
    final static int CB_STATE_RUNNING = 2;
    final static int CB_STATE_DONE = 3;
    
    final static int SEND_TYPE_WALLET = 1;
    final static int SEND_TYPE_REMOTE = 2;
    final static int SEND_TYPE_FOLDER = 3;
    
    public int currentScreen;
    
    public boolean cwalletRecoveryRequested, cwalletPasswordRequested;
    
    String errText;
    String typedPassword;
    String typedEmail;
    String typedWalletName;

    int currentWalletIdx;
    Wallet currentWallet;
    
    int[][] counters;
    int cbState;
    
    boolean isDefaultWalletBeingCreated;
    boolean isAddingWallet;

    ArrayList<String> files;
    
    String chosenFile;
    String typedMemo;
    
    Wallet dstWallet;
    Wallet srcWallet;
    
    boolean isEchoFinished;
    boolean isShowCoinsFinished;
    
    int statToBankValue, statToBank, statFailed, statLost;
    
    int statTotalFracked, statTotalFixed, statFailedToFix;
    
    String receiptId;
    
    boolean isSrcBoxFull, isDstBoxFull;
    
    String typedSrcPassword, typedDstPassword, typedRemoteWallet;
    int typedAmount;
    
    int sendType;
    
    boolean isUpdatedWallets;
    
    int foundSN;
    
    boolean needBackup;
    
    public Hashtable<String, String[]> cenvelopes;
    
    String domain;
    
    String trustedServer;
    
    boolean isSkyDeposit;
    
    boolean defaultWalletCreated;
    
    String defaultWalletName;
    
    boolean isCreatingNewSkyWallet;
    
    String skyVaultDomain;
    
    boolean isShowingCoins;
    
    int selectedFromIdx, selectedToIdx;

    ArrayList<CloudCoin> duplicates;
    
    String receiverReceiptId;
    
    boolean triedToChange;
    
    boolean changeFromExport;
    
    public ProgramState() {
        currentScreen = SCREEN_AGREEMENT;
        cwalletRecoveryRequested = cwalletPasswordRequested = false;
        errText = "";
        typedPassword = "";
        typedEmail = "";
        typedWalletName = "";
        currentWallet = null;
        counters = null;
        cbState = CB_STATE_INIT;
        currentWalletIdx = -1;
        chosenFile = "";
        typedMemo = "";
        files = new ArrayList<String>();
        dstWallet = srcWallet = null;
        isEchoFinished = false;
        isShowCoinsFinished = true;
        statToBankValue = statToBank = statFailed = 0;
        
        isDefaultWalletBeingCreated = false;
        isAddingWallet = false;
        
        receiptId = "";
        
        foundSN = 0;
        
        isDstBoxFull = isSrcBoxFull = true;
        typedSrcPassword = typedDstPassword = "";
        typedAmount = 0;
        typedRemoteWallet = "";
        
        sendType = SEND_TYPE_WALLET;
        
        isUpdatedWallets = false;
        
        needBackup = false;
        
        cenvelopes = null;
        
        statTotalFracked = statTotalFixed = statFailedToFix = 0;
        
        domain = "";
        trustedServer = "";
        
        isSkyDeposit = false;
                
        defaultWalletCreated = false;
        
        defaultWalletName = null;
        
        isCreatingNewSkyWallet = false;
        
        skyVaultDomain = "";
        
        isShowingCoins = false;
        
        selectedFromIdx = selectedToIdx = -1;
        
        duplicates = new ArrayList<CloudCoin>();
        
        receiverReceiptId = "";
        
        triedToChange = false;
        
        changeFromExport = false;
        
    }
 
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            result.append("  ");
            try {
                Character c = field.getName().charAt(0);
                if (Character.isUpperCase(c)) {
                    continue;
                }
                
                result.append(field.getName());
                result.append(": ");
                
                result.append(field.get(this));
            } catch (IllegalAccessException ex) {
            }
            result.append("; ");
        }

        return result.toString();
    }
}
