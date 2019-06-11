/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient.common.core;

/**
 *
 * @author Alexander
 */
public class Validator {
    public static boolean walletName(String wallet) {
        if (wallet.isEmpty())
            return false;
        
        if (wallet.length() > global.cloudcoin.ccbank.core.Config.MAX_WALLET_LENGTH) {
            return false;
        }
        
        if (wallet.contains("\\") || wallet.contains("/")) {
            return false;
        }
        
        return true;
    }
    
    public static boolean email(String email) {
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        
        return email.matches(regex);
    }
    
    public static boolean memo(String tag) {
        if (tag.indexOf('.') != -1 || tag.indexOf('/') != -1 || tag.indexOf('\\') != -1)
            return false;
        
        return true;
    }
    
}
