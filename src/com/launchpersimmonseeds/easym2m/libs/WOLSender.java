package com.launchpersimmonseeds.easym2m.libs;

import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersiomonseeds.interfaces.iActions;
import com.launchpersimmonseeds.easym2m.Checker;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MisoChan
 */
@PluginInfo(classID = "WOL", author = "MisoChan", version = "1.0")
public class WOLSender implements iActions {

   private final String MAC_ADDR;
   private final String IP_ADDR;

   private volatile boolean VALID_MAC = false, VALID_IP = false, initialized = false;

   /**
    * コンストラクタ。 Wake−On-Lan 送信先MACアドレスを設定する 初期化時MACアドレスとして使用不可能ならば
    *
    * @param mac_addr
    * @param ip_addr
    */
   public WOLSender(String mac_addr, String ip_addr) {
	MAC_ADDR = mac_addr;
	IP_ADDR = ip_addr;
	VALID_MAC = Checker.isMACAddress(MAC_ADDR);
	VALID_IP = Checker.isIPAddress(ip_addr);
	initialized = VALID_IP & VALID_MAC;
   }

   public WOLSender(Map<String, String> params) {
	MAC_ADDR = params.get("MAC_ADDR");
	IP_ADDR = params.get("IP_ADDR");
	VALID_MAC = Checker.isMACAddress(MAC_ADDR);
	VALID_IP = Checker.isIPAddress(IP_ADDR);
	initialized = VALID_IP & VALID_MAC;
   }

   /**
    * MagicPacketを送信する。
    *
    * @return
    */
   public boolean go() {

	byte[] sendbyte;

	//初期化が完了していない限り実行不可
	if (!initialized) {
	   return false;
	}

	try {
	   InetSocketAddress ipaddr = new InetSocketAddress(IP_ADDR, 9);
	   sendbyte = getMagicPacket(MAC_ADDR);
	   DatagramPacket sendpacket = new DatagramPacket(sendbyte, sendbyte.length, ipaddr);
	   new DatagramSocket().send(sendpacket);
	   //ここまでいけばTrue。
	   return true;
	} catch (SocketException ex) {
	   Logger.getLogger(WOLSender.class.getName()).log(Level.SEVERE, null, ex);
	} catch (IOException ex) {
	   Logger.getLogger(WOLSender.class.getName()).log(Level.SEVERE, null, ex);
	}
	return false;

   }

   private byte[] getMagicPacket(String macaddr) {
	/*
        -MagicPacketのルール-
        FF:FF:FF:FF:FF(全部１) +（任意のMACアドレス）* 16 = MAC総数は17コ

        1MACアドレスあたり… 6B * MAC総数17コ = 102Bとなる。
	 */
	byte[] send = new byte[102];
	byte[] macByte = getMACByte(macaddr);
	int count = 0;
	for (int i = 0; i < 6; i++) {
	   send[count++] = (byte) 0xff;
	}
	for (int j = 0; j < 16; j++) {

	   for (int k = 0; k < macByte.length; k++) {
		send[count++] = macByte[k];
	   }

	}

	return send;

   }

   private byte[] getMACByte(String macaddr) {

	byte[] mac = new byte[6];
	String[] num = macaddr.split(":");

	for (int i = 0; i < num.length; i++) {
	   mac[i] = (byte) Integer.parseInt(num[i], 16);
	}
	return mac;
   }

   public boolean checkAddresses() {
	return VALID_IP & VALID_MAC & initialized;
   }

   /**
    * ファイナライザ攻撃対策用。
    */
   @Override
   public final void finalize() {

   }

   @Override
   public Map<String, String> getParamList() {
	Map<String, String> params = new HashMap<>();
	params.put("IP_ADDR", IP_ADDR);
	params.put("MAC_ADDR", MAC_ADDR);

	return params;
   }

}
