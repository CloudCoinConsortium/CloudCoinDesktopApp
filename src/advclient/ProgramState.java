/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

/**
 *
 * @author Alexander
 */
public class ProgramState {
    final public static int SCREEN_AGREEMENT = 0x1;
    final public static int SCREEN_CREATE_FIRST_WALLET = 0x2;
    final public static int SCREEN_DEFAULT = 0x3;
    final public static int SCREEN_SET_PASSWORD = 0x4;
    final public static int SCREEN_UNDERSTAND_PASSWORD = 0x5;
    final public static int SCREEN_SET_EMAIL = 0x6;
    final public static int SCREEN_WALLET_CREATED = 0x7;
    
    public int currentScreen;
    
    public boolean cwalletRecoveryRequested, cwalletPasswordRequested;
    
    String errText;
    String typedPassword;
    String typedEmail;
    String typedWalletName;
    String currentWalletName;
    
    boolean isDefaultWalletBeingCreated;
    
    public ProgramState() {
        currentScreen = SCREEN_AGREEMENT;
        cwalletRecoveryRequested = cwalletPasswordRequested = false;
        errText = "";
        typedPassword = "";
        typedEmail = "";
        typedWalletName = "";
        currentWalletName = "";
        
        isDefaultWalletBeingCreated = false;
    }
    
}
