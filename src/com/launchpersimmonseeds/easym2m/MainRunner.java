/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.launchpersimmonseeds.easym2m;

import com.launchpersimmonseeds.easym2m.base.XMLSettingsLoader;
import com.launchpersimmonseeds.easym2m.libs.GPIOSwitcher;
import com.launchpersimmonseeds.easym2m.libs.WOLSender;
import com.launchpersimmonseeds.easym2m.networking.TCPSocketServer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MisoChan
 */
public class MainRunner {

    public static final ActionRuleSet RULES = ActionRuleSet.getInstance();

    public static void main(String[] args) throws IOException {

        try {
            //起動チェックが通らない場合終了する。
            if (checkMainSystem()) {
                System.exit(1);
            }

            getProperty(args);

            //TCPソケットサーバーを開始。
            TCPSocketServer nekokan = TCPSocketServer.getServerInstance(1919, 40);
            nekokan.start();

            //監視スレッド立ち上げ
            NetRuleSet.showNetRules();

            RULES.start();

            nekokan.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(MainRunner.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * 設定ファイル(config.xml)をロードし、GPIOポート予約を行う。
     *
     */
    private static void getProperty(String[] args) {

        int indicator;

        try {

            if (args.length >= 2) {
                if (args[0].equals("-gen")) {
                    GPIOSwitcher cat = new GPIOSwitcher(79, "in", "up");
                    ActionRuleSet.addInput(cat);
                    ActionRuleSet.addOutAction(cat, new GPIOSwitcher(71, "out", "up"));
                    ActionRuleSet.addOutAction(cat, new WOLSender("00:25:90:09:31:d2", "192.168.0.4"));
                    indicator = Integer.parseInt(args[1]);

                    XMLSettingsLoader.generateXml();
                }
            }

            XMLSettingsLoader.readXml();

        } catch (Exception ex) {
            Logger.getLogger(MainRunner.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * パーミッションとシステムのチェックを行う。 ユーザーがrootかつOS名がLinuxの場合プログラムを続行
     * それ以外であれば終了ステータス1でプログラム終了となる
     */
    private static boolean checkMainSystem() {
        boolean flags = true;

        if (!System.getProperty("os.name").equals("Linux")) {
            System.err.println("This Program does not work on " + System.getProperty("os.name"));
            System.err.println("Please use on Linux OS.");
        } else if (!System.getProperty("user.name").equals("root")) {
            System.err.println("Permission denied. Please running as root.");
        } else {
            flags = false;
            System.out.println("All status is OK. Start Running....");
        }

        //もし検査項目に引っかかった場合、フラグがtrueのまま判定、終了する。
        return flags;

    }
}
