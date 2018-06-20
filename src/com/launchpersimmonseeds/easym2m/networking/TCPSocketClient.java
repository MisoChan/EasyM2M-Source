/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.launchpersimmonseeds.easym2m.networking;

import com.launchpersimmonseeds.easym2m.Checker;
import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersiomonseeds.interfaces.iActions;
import java.net.Socket;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MisoChan
 */
@PluginInfo(classID = "TCP", author = "MisoChan", version = "1.0")
public class TCPSocketClient extends Thread implements iActions {

   private Socket SOCKET = null;

   private BufferedReader in;
   private InetAddress ADDRESS;
   private String ST_ADDRESS;
   private int PORT = 0;
   public String RECIEVE;
   private boolean LOOP = true;
   private String DEFAULT = "GO";

   /**
    * TCPソケット通信クライアント
    *
    * @param addr 接続先IPアドレス
    * @param port 接続先ポート
    */
   public TCPSocketClient(String addr, int port) {
	try {
	   this.ST_ADDRESS = addr;
	   this.ADDRESS = InetAddress.getByName(addr);
	} catch (UnknownHostException ex) {
	   Checker.stdout("WRONG ADDR" + ex);
	}
	this.PORT = port;

   }

   public TCPSocketClient(Map<String, String> map) {

	this.DEFAULT = map.get("SEND");
	this.ST_ADDRESS = map.get("IP_ADDR");
	try {
	   this.ADDRESS = InetAddress.getByName(ST_ADDRESS);
	} catch (UnknownHostException ex) {
	   Logger.getLogger(TCPSocketClient.class.getName()).log(Level.SEVERE, null, ex);
	}
	if (Checker.isStringNumber(map.get("PORT"))) {
	   this.PORT = Integer.parseInt(map.get("PORT"));
	} else {
	   throw new IllegalArgumentException("PORT NUMBER IS WRONG");
	}
   }

   public Socket getSocket() {
	return this.SOCKET;
   }

   /**
    * ソケットが維持されている場合 Trueを返す
    *
    * @return ソケットの有無
    */
   public boolean getStatus() {
	boolean status = false;

	if (SOCKET != null) {
	   status = true;
	}
	return status;
   }

   public String getIPAddress() {
	return ST_ADDRESS;
   }

   public String getDefaultString() {
	return DEFAULT;
   }

   public int getPortNumber() {
	return PORT;
   }

   public boolean go(String sendstr) {

	String line;
	boolean state = false;

	PrintWriter out;
	InetSocketAddress endpoint = new InetSocketAddress(ST_ADDRESS, PORT);

	try {

	   this.SOCKET = new Socket();

	   //ソケットのタイムアウトを350ミリ秒にする
	   SOCKET.setSoTimeout(350);

	   for (int retry = 1; retry < 4; retry++) {
		try {
		   SOCKET.connect(endpoint);

		} catch (ConnectException ce) {
		   printInfo(ce.getMessage() + "(ConnectException)..Trying ReConnect  " + retry + "/ 3", ADDRESS);
		} catch (NoRouteToHostException ne) {
		   printInfo(ne.getMessage(), ADDRESS);
		   return false;
		} catch (SocketException ex) {
		   printInfo(ex.getMessage() + "(SocketException)..Trying ReConnect  " + retry + "/ 3", ADDRESS);

		} finally {
		   //キャッチ抑制エラーは出るものの、コネクションチェックを挟むため無問題?
		   if (SOCKET.isConnected()) {
			break;
		   }

		}
	   }

	   if (SOCKET == null) {
		return false;
	   }

	   this.in = new BufferedReader(new InputStreamReader(SOCKET.getInputStream()));

	   while ((line = in.readLine()) != null) {
		if (line.equals("WELCOME")) {
		   out = new PrintWriter(SOCKET.getOutputStream());
		   if (in != null) {

			out.println(sendstr);

			out.flush();

			if (in.readLine().equals("OK")) {
			   state = true;
			   printInfo("TCP_SEND COMPLETE " + sendstr, ADDRESS);
			}
			out.close();
			in.close();
		   }
		   break;
		} else {
		   Checker.stdout(line);
		   state = false;
		}
	   }

	   if (SOCKET != null) {
		SOCKET.close();
	   }

	} catch (SocketException | InterruptedIOException ex) {
	   printInfo(ex.getMessage(), ADDRESS);

	} catch (IOException ex) {
	   printInfo("IOE" + ex.getMessage(), ADDRESS);

	} finally {
	   try {
		if (SOCKET != null) {

		   SOCKET.close();

		}
		if (in != null) {
		   in.close();
		}

	   } catch (IOException ex) {
		printInfo(ex.getMessage(), ADDRESS);
	   }

	   return state;
	}
   }

   @Override
   public boolean go() {
	return go(DEFAULT);
   }

   public boolean setDefaultGoString(String str) {
	DEFAULT = str;
	return true;
   }

   public void interruptClient() {
	LOOP = false;
   }

   public void offConnection() {
	try {
	   LOOP = false;

	   if (in != null) {

		this.in.close();

		if (!SOCKET.isClosed()) {
		   this.SOCKET.close();
		}

	   }
	} catch (IOException e) {
	}
   }

   @Override
   public Map<String, String> getParamList() {
	final Map<String, String> param = new HashMap<>();
	param.put("IP_ADDR", this.ST_ADDRESS);
	param.put("SEND", this.DEFAULT);
	param.put("PORT", Integer.toString(this.PORT));
	return param;

   }

   @Override
   public boolean equals(Object obj) {
	TCPSocketClient comp;
	if (!(obj instanceof TCPSocketClient)) {
	   return false;
	} else {
	   comp = (TCPSocketClient) obj;
	   return getParamList().equals(comp.getParamList());
	}

   }

   private static void printInfo(String str, InetAddress address) {
	System.out.println("[TCP_OUT] " + "IP:" + address.getHostAddress() + " MSG:\n\t" + str);
   }

}
