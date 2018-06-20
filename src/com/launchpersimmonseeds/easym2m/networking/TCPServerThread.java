/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.launchpersimmonseeds.easym2m.networking;

import com.launchpersimmonseeds.easym2m.ActionRuleSet;
import com.launchpersimmonseeds.easym2m.Checker;
import com.launchpersimmonseeds.easym2m.NetRuleSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * サーバー側メイン処理部分
 *
 * @author MisoChan
 */
public class TCPServerThread extends Thread {

   private final Socket SOCKET;
   private boolean LOOP = true;
   private long THREAD_ID;
   private InetAddress ADDRESS;
   private final TCPSocketServer SUPER_THREAD;
   private static volatile HashMap<InetAddress, ArrayList<String>> EXECUTING_COMM_LIST = new HashMap<>();

   /**
    * サーバスレッド部分 実際にサーバーとして実行される。
    *
    * @param socket 接続ソケット
    * @param supe 呼び出し元インスタンス
    */
   protected TCPServerThread(Socket socket, TCPSocketServer supe) {
	this.SOCKET = socket;
	SUPER_THREAD = supe;
	ADDRESS = SOCKET.getInetAddress();

	Runtime.getRuntime().addShutdownHook(new Thread() {
	   @Override
	   public void run() {
		stopServer();

	   }
	});
   }

   @Override
   public void run() {
	THREAD_ID = Thread.currentThread().getId();

	BufferedReader input = null;
	PrintWriter out = null;
	String line = null;
	try {
	   SOCKET.setSoTimeout(1000);
	   //読み書きストリームの宣言
	   input = new BufferedReader(new InputStreamReader(SOCKET.getInputStream()));
	   out = new PrintWriter(SOCKET.getOutputStream(), true);
	   
	   System.out.println("SERVER_ESTABILISHED on TID:" + THREAD_ID + "IP:" + ADDRESS.getHostAddress());
	   ActionRuleSet.indicatorState(6, 17);
	   out.println("WELCOME");
	   out.flush();

	   while (LOOP) {

		if ((line = input.readLine()) != null) {
		   ActionRuleSet.indicatorState(2, 17);

		   //コマンドを受け取り解釈、実行する部分
		   
		   //BYEを受け取ったorロック失敗
		   if (line.equals("BYE") || !processLock(ADDRESS, line)) {
			Checker.stdout("SERVER_COMMS_LOCK_FAILED");
			LOOP = false;
			break;
		   }
		   if (!NetRuleSet.getClientCommandList(ADDRESS).isEmpty()) {

			if (NetRuleSet.doCommandExist(ADDRESS, line)) {
			   out.println("OK");
			   System.out.println("[COMMAND]:" + line);

			} else {
			   out.println("WRONG COMMS");
			}
			out.flush();			
			break;

		   }

		} else {
		   System.out.println("close");
		   stopServer();
		}
	   }

	} catch (IOException ex) {
	   Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
	} finally {
	   try {
		if (SOCKET != null) {
		   SOCKET.close();
		}
		if (input != null) {
		   input.close();
		}
		if (out != null) {
		   out.close();
		}
		EXECUTING_COMM_LIST.get(ADDRESS).remove(line);
		System.out.println("SERVER_CLOSED on TID:" + THREAD_ID + ADDRESS.getHostAddress());
		SUPER_THREAD.delServerInstance(this);
	   } catch (SocketTimeoutException ex) {
		System.out.println("SERVER_CONNECTION_TIMED OUT on TID" + THREAD_ID + ADDRESS.getHostAddress());
		LOOP = false;

	   } catch (IOException ex) {
		Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
	   }
	}

   }

   private static synchronized boolean processLock(InetAddress addr, String comm) {
	ArrayList<String> commlist = new ArrayList<>();
	if (!EXECUTING_COMM_LIST.containsKey(addr)) {
	   commlist.add(comm);
	   EXECUTING_COMM_LIST.put(addr, commlist);
	} else if (EXECUTING_COMM_LIST.get(addr).contains(comm)) {
	   return false;
	} else {
	   if(!EXECUTING_COMM_LIST.get(addr).isEmpty()){
		commlist.addAll(EXECUTING_COMM_LIST.get(addr));
	   }
	   commlist.add(comm);
	   EXECUTING_COMM_LIST.remove(addr);
	   EXECUTING_COMM_LIST.put(addr, commlist);

	}
	return true;
   }

   @Override
   public boolean equals(Object obj) {
	if (obj == this) {
	   return true;
	}
	if (obj == null) {
	   return false;
	}
	if (!(obj instanceof TCPServerThread)) {
	   return false;
	}
	TCPServerThread neko = (TCPServerThread) obj;
	return this.SOCKET == neko.SOCKET | this.THREAD_ID == neko.THREAD_ID;
   }

   @Override
   public int hashCode() {
	int hash = 7;
	hash = 83 * hash + Objects.hashCode(this.SOCKET);
	hash = 83 * hash + (int) (this.THREAD_ID ^ (this.THREAD_ID >>> 32));
	return hash;
   }

   /**
    * サーバーを停止させる部分。
    */
   public void stopServer() {
	
	LOOP = false;
   }

   @Override
   public void finalize() {
   }

}
