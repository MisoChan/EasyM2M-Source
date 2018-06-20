package com.launchpersimmonseeds.easym2m;

import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersimmonseeds.easym2m.libs.GPIOSwitcher;
import com.launchpersiomonseeds.interfaces.iActions;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * リスト内のホストのIP、受信した文字列によってオブジェクトを分類し格納するClass `
 *
 * @author MisoChan
 */
public class NetRuleSet {

   private NetRuleSet() {

   }
   private static final Map<InetAddress, Map<String, ArrayList<iActions>>> NET_CLIENTLIST = new HashMap<>();

   public static boolean addInputNet(String recvaddr, String command) {
	boolean status = false;
	try {
	   InetAddress clientaddr = InetAddress.getByName(recvaddr);
	   if (!NET_CLIENTLIST.containsKey(clientaddr)) {
		NET_CLIENTLIST.put(clientaddr, new HashMap<>());
		NET_CLIENTLIST.get(clientaddr).put(command, new ArrayList<>());
		status = true;
	   }

	} catch (UnknownHostException ex) {
	   Checker.stdout("IP Address is wrong. please check this ->", recvaddr);
	}
	return status;
   }

   public static boolean addOut(String recvaddr, String command, iActions act) throws UnknownHostException {
	InetAddress reciveaddr = InetAddress.getByName(recvaddr);
	if (NET_CLIENTLIST.containsKey(reciveaddr)) {
	   try {

		input_address_check:
		if (NET_CLIENTLIST.get(reciveaddr).containsKey(command)) {
		   NET_CLIENTLIST.get(reciveaddr).get(command).add(act);
		} else {
		   //入力アドレス-コマンド を追加して、trueが帰ってきたら…アドレス-コマンドセットがあるか再評価。
		   //ジャンプ使うのはちょっとどうなの…

		   if (addInputNet(recvaddr, command)) {
			break input_address_check;
		   }

		}

	   } catch (IllegalArgumentException ex) {
		Logger.getLogger(NetRuleSet.class.getName()).log(Level.SEVERE, null, ex);
		e("ADD FAILED: " + ex);
		return false;
	   }

	} else {
	   System.err.println("Node Not Found");
	   return false;
	}
	return true;
   }

   public static void clearRules() {
	NET_CLIENTLIST.clear();
   }

   private static void e(Object o) {
	System.err.println(o);
   }

   public static ArrayList<InetAddress> getClientAddressList() {
	ArrayList<InetAddress> array = new ArrayList<>();
	InetAddress addr;
	for (Map.Entry<InetAddress, Map<String, ArrayList<iActions>>> entry : NET_CLIENTLIST.entrySet()) {
	   addr = entry.getKey();
	   array.add(addr);

	}
	return array;
   }

   public static ArrayList<String> getClientCommandList(InetAddress key) {

	ArrayList<String> array = new ArrayList<>();
	Map<String, ArrayList<iActions>> comm_list;

	comm_list = NET_CLIENTLIST.get(key);

	for (Map.Entry<String, ArrayList<iActions>> chentry : comm_list.entrySet()) {
	   array.add(chentry.getKey());
	}

	return array;
   }

   public static ArrayList<iActions> getClientDeviceList(InetAddress key, String command) {
	ArrayList<iActions> devices;
	if (NET_CLIENTLIST.containsKey(key)) {
	   devices = NET_CLIENTLIST.get(key).get(command);
	} else {
	   devices = null;
	}
	return devices;
   }

   public static void showNetRules() {
	System.out.println("---List:NET_Rules---");
	for (InetAddress addr : getClientAddressList()) {
	   System.out.println(addr.getHostAddress() + "-");
	   for (String coms : getClientCommandList(addr)) {
		Checker.stdout("\t comannd-" + coms);
		for (iActions dev : getClientDeviceList(addr, coms)) {
		   Checker.stdout("\t\t" + getClassID(dev));
		}
	   }
	}
   }

   public static boolean isClientContainsList(InetAddress addr) {
	return NET_CLIENTLIST.containsKey(addr);
   }

   public static String getClassID(iActions cls) {

	return cls.getClass().getAnnotation(PluginInfo.class).classID();
   }

   public static boolean isClientHasCommand(InetAddress addr) {
	return !NET_CLIENTLIST.get(addr).isEmpty();
   }

   public static boolean doCommandExist(InetAddress addr, String command) {

	

	if (getClientDeviceList(addr, command) != null) {
	   ArrayList<iActions> devs = getClientDeviceList(addr, command);
	   if (!devs.isEmpty()) {
		for (iActions action : devs) {
		   action.go();
		}
		return true;
	   }
	}

	return false;
   }

}
