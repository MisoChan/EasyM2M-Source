/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.launchpersimmonseeds.easym2m.networking;

import com.launchpersimmonseeds.easym2m.NetRuleSet;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ソケット通信サーバーサイド
 *
 *
 * @author oyaki
 */
public final class TCPSocketServer extends Thread {

   private final int PORT_NUMBER, MAX_CONN;
   private boolean LOOP = true;
   private ServerSocket SERVER_SOCKET;
   private Socket SOCKET;
   private ArrayList<TCPServerThread> threads = new ArrayList<>();
   private TCPServerThread SERVER;
   private static volatile TCPSocketServer INSTANCE;
   
   /**
    * ソケット通信サーバー部分
    *
    * @param port ポート番号
    * @param maxclient 最大コネクション数
    */
   private TCPSocketServer(int port, int maxclient) {
	this.PORT_NUMBER = port;
	this.MAX_CONN = maxclient;

   }

   public static synchronized TCPSocketServer getServerInstance(int port, int maxclient) {
	if (INSTANCE == null) {
	   //インスタンスが唯一になるために二重検査する。
	   synchronized (TCPSocketServer.class) {
		if (INSTANCE == null) {
		   TCPSocketServer ins = new TCPSocketServer(port, maxclient);
		   INSTANCE = ins;
		}
	   }
	}
	return INSTANCE;
   }

   @Override
   public void run() {
	SERVER_SOCKET = null;
	TCPServerThread serverthread;
	System.out.println("SERVER START:" + LocalDateTime.now());

	try {
	   SERVER_SOCKET = new ServerSocket(this.PORT_NUMBER);

	   while (LOOP) {
		SOCKET = SERVER_SOCKET.accept();
		if (NetRuleSet.isClientContainsList(SOCKET.getInetAddress())) {

		   //インスタンス化したスレッド数が最大コネクション数(MAX_CONN)を超えないようする。
		   if (threads.size() < MAX_CONN) {
			SERVER = new TCPServerThread(SOCKET, this);
			threads.add(SERVER);
			serverthread = threads.get(threads.size() - 1);
			serverthread.start();
		   } else {
			sendString("BYE SERVER_IS_FULL");
			System.out.println("SERVER_IS_FULL");
			SOCKET.close();
		   }
		} else {

		   sendString("BYE ACCESS_DENIED");
		   SOCKET.close();
		}
	   }
	} catch (IOException e) {

	} finally {
	   if (SERVER_SOCKET != null) {
		try {

		   SERVER_SOCKET.close();
		   interruptServer();
		} catch (IOException ex) {
		   Logger.getLogger(TCPSocketServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	   }
	}

   }


   private boolean sendString(String sendstr) {
	boolean state = true;

	try {
	   try (PrintWriter out = new PrintWriter(SOCKET.getOutputStream(), true)) {
		System.out.println(sendstr);
		out.println(sendstr);
	   }
	} catch (IOException e) {
	   state = false;
	}
	return state;
   }

   /**
    * サーバースレッドをすべて終了させる。
    *
    */
   public void interruptServer() {
	threads.forEach((th) -> {
	   th.stopServer();
	});

	LOOP = false;

   }

   //サーバーのスレッドインスタンスをArrayList（threads）から消す。
   protected void delServerInstance(TCPServerThread th) {
	
	threads.remove(threads.indexOf(th));
	System.out.println(th.isAlive());

   }

}
