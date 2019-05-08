/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package advclient;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JComponent;

/**
 *
 * @author Alexander
 */
interface UICallBack {
    public boolean doWork(Graphics g, JComponent c);
}
