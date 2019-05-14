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
    
    
    final static int CB_STATE_INIT = 1;
    final static int CB_STATE_RUNNING = 2;
    final static int CB_STATE_DONE = 3;
    
    public int currentScreen;
    
    public boolean cwalletRecoveryRequested, cwalletPasswordRequested;
    
    String errText;
    String typedPassword;
    String typedEmail;
    String typedWalletName;
    //String currentWallet;
    int currentWalletIdx;
    Wallet currentWallet;
    
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
        currentWallet = null;
        counters = null;
        cbState = CB_STATE_INIT;
        currentWalletIdx = -1;
        chosenFile = "";
        
        isDefaultWalletBeingCreated = false;
        isAddingWallet = false;
    }
    
}
