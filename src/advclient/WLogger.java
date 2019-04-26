/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pbank;

import global.cloudcoin.ccbank.core.GLogger;
import global.cloudcoin.ccbank.core.GLoggerInterface;

/**
 *
 * @author Александр
 */
public class WLogger extends GLogger implements GLoggerInterface {

    @Override
    public void onLog(int level, String tag, String message) {
        if (level == GL_DEBUG) {
            System.out.println("tag: " + tag + " " + message);
        } else if (level == GL_VERBOSE) {
            System.out.println("tag: " + tag + " " + message);
        } else {
            System.out.println("tag: " + tag + " " + message);
        }
    }
}
