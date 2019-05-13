/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import global.cloudcoin.ccbank.core.Wallet;

/**
 *
 * @author Alexander
 */
public class ProgramState {
    final public static int SCREEN_AGREEMENT = 0x1;
    final public static int SCREEN_CREATE_WALLET = 0x2;
    final public static int SCREEN_DEFAULT = 0x3;
    final public static int SCREEN_SET_PASSWORD = 0x4;
    final public static int SCREEN_UNDERSTAND_PASSWORD = 0x5;
    final public static int SCREEN_SET_EMAIL = 0x6;
    final public static int SCREEN_WALLET_CREATED = 0x7;
    final public static int SCREEN_PREPARE_TO_ADD_WALLET = 0x8;
    final public static int SCREEN_CREATE_SKY_WALLET = 0x9;
    final public static int SCREEN_SHOW_TRANSACTIONS = 0x10;
    
    
    final static int CB_STATE_INIT = 1;
    final static int CB_STATE_RUNNING = 2;
    final static int CB_STATE_DONE = 3;
    
    public int currentScreen;
    
    public boolean cwalletRecoveryRequested, cwalletPasswordRequested;
    
    String errText;
    String typedPassword;
    String typedEmail;
    String typedWalletName;
    String currentWalletName;
    int currentWalletIdx;
    //Wallet currentWallet;
    
    int[][] counters;
    int cbState;
    
    boolean isDefaultWalletBeingCreated;
    boolean isAddingWallet;
    
    String chosenFile;
    
    public ProgramState() {
        currentScreen = SCREEN_AGREEMENT;
        cwalletRecoveryRequested = cwalletPasswordRequested = false;
        errText = "";
        typedPassword = "";
        typedEmail = "";
        typedWalletName = "";
        currentWalletName = "";
        counters = null;
        cbState = CB_STATE_INIT;
        currentWalletIdx = -1;
        chosenFile = "";
        
        isDefaultWalletBeingCreated = false;
        isAddingWallet = false;
    }
    
}
