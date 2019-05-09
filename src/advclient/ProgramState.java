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
    
    public int currentScreen;
    
    public ProgramState() {
        currentScreen = SCREEN_AGREEMENT;
    }
    
}
