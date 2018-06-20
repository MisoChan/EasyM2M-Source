package com.launchpersimmonseeds.easym2m;

import com.launchpersiomonseeds.interfaces.iStates;
import com.launchpersiomonseeds.interfaces.iActions;
import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersimmonseeds.easym2m.libs.*;
import com.launchpersimmonseeds.easym2m.networking.TCPSocketClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GPIO入力があった際のルール格納部
 *
 *
 * @author MisoChan
 */
public class ActionRuleSet extends Thread {

   private static final Map<iStates, ArrayList<iActions>> GPIO_INPUTLIST = new HashMap<>();
   private static boolean LOOP = true;
   private static GPIOSwitcher INDICATOR;
   private static ActionRuleSet INSTANCE;

   private ActionRuleSet() {
   }

   protected static synchronized ActionRuleSet getInstance() {
	if (INSTANCE == null) {
	   //インスタンスが唯一になるために二重検査する。
	   synchronized (ActionRuleSet.class) {
		if (INSTANCE == null) {
		   ActionRuleSet ins = new ActionRuleSet();
		   INSTANCE = ins;
		}
	   }
	}
	return INSTANCE;
   }

   /**
    * 入力iStatesを定義する。
    *
    * @param nyan
    * @return
    */
   public static boolean addInput(iStates nyan) {

	//ActionListのInput内に同じ入力Objがあればそいつを使う(良い言い回しが思いつかないぞ！)
	GPIO_INPUTLIST.put(checkDupe(nyan), new ArrayList<>());

	return true;
   }

   /**
    * 入力(input)と紐付いたActionListに出力Obj(action)を追加する。
    *
    * @param input
    * @param action
    * @return
    */
   public static boolean addOutAction(iStates input, iActions action) {
	try {

	   if (GPIO_INPUTLIST.containsKey(input)) {
		ArrayList<iActions> dev = GPIO_INPUTLIST.get(input);

		dev.add(action);

	   } else {
		if (addInput(input)) {
		   addOutAction(input, action);
		}

		return false;
	   }
	} catch (IllegalArgumentException ex) {
	   System.err.println("Param_Error");
	   Logger.getLogger(ActionRuleSet.class.getName()).log(Level.SEVERE, null, ex);
	   return false;
	}

	return true;
   }

   public static boolean addOutActionList(iStates input, ArrayList<iActions> acts) {
	boolean state = false;
	if (GPIO_INPUTLIST.containsKey(input)) {
	   GPIO_INPUTLIST.put(input, acts);
	   state = true;
	}
	return state;
   }

   /**
    * デバッグ検証用 そのうち廃止
    *
    * @param input
    * @param addr
    * @param port
    * @param sendstr
    * @return
    */
   public static boolean addOutNet(iStates input, String addr, int port, String sendstr) {
	TCPSocketClient client = new TCPSocketClient(addr, port);
	if (GPIO_INPUTLIST.containsKey(input)) {
	   client.setDefaultGoString(sendstr);
	   GPIO_INPUTLIST.get(input).add(client);
	} else {
	   System.err.println("Node Not Found");
	   return false;
	}
	return true;
   }

   /**
    * 入力リスト中に同じオブジェクトがあれば そいつを設定する
    *
    * @param chkdupe
    * @return
    * @throws IOException
    */
   private static iStates checkDupe(iStates chkdupe) {
	iStates coms = chkdupe;
	for (Map.Entry<iStates, ArrayList<iActions>> entry : GPIO_INPUTLIST.entrySet()) {
	   ArrayList val = entry.getValue();
	   if (val.indexOf(chkdupe) != -1) {
		coms = (iStates) val.get(val.indexOf(chkdupe));
		System.out.println("Duped!");
	   } else {

	   }
	}

	return coms;
   }

   public static GPIOSwitcher getIndicator() {
	return INDICATOR;
   }

   @Override
   public void run() {

	printlist();
	indicatorState(5);

	ruleWatcher();
   }

   private static void ruleWatcher() {
	iStates runwatch = null;
	long l = 0;
	
	ArrayList<iActions> devs;
	Map<iStates, Thread> threadmap = new ConcurrentHashMap<>();
	boolean executing = false, before = false;

	while (LOOP) {
	   try {
		Thread.sleep(20L);
		for (iStates watch : getRules()) {
		   if(threadmap.containsKey(watch)){
			if (!threadmap.get(watch).isAlive()) {
			   threadmap.remove(watch);
			}
		   }
		   if (watch.getState() == true & watch != runwatch & !before) {
			devs = getOutActionList(watch);

			if (!threadmap.containsKey(watch)) {
			   threadmap.put(watch, ruleExecutor(devs, executing));
			}
			before = true;
			runwatch = watch;

		   } else if (runwatch != null) {
			if (runwatch.getState() == false) {

			   before = false;
			   runwatch = null;
			}

		   }

		}
	   } catch (InterruptedException ex) {
		Logger.getLogger(ActionRuleSet.class.getName()).log(Level.SEVERE, null, ex);
	   }
	}

   }

   private static Thread ruleExecutor(ArrayList<iActions> devs, boolean due) {
	Thread th;
	
	th = new Thread("LOCAL_EXECUTING") {
	   @Override
	   public void run() {

		for (iActions v : devs) {
		   if(!v.go())System.err.println("[RULE:]"+v.getClass().getAnnotation(PluginInfo.class).classID()+"is FAILED.");
		}
	   }
	};

	if (!due) {
	   th.start();
	}
	return th;
   }

   /**
    * ActionRuleList内部のロード状況を表示
    */
   public static synchronized void printlist() {
	Map<String, String> action;
	ArrayList<iActions> devs;
	iStatesContainer neko;
	iStates get;
	String classid,classids = "";
	p("---List:GPIO_Rules---");
	for (iStates watch : getRules()) {
	   
	   //クラスリスト表示
	   //もしコンテナが入っていたら…
	   if(watch.getClass().equals(iStatesContainer.class)){
		neko = (iStatesContainer) watch;
		for(int i = 0;i<neko.getList().size();i++){
		   get = neko.getList().get(i);
		   classid = get.getClass().getAnnotation(PluginInfo.class).classID();
		   classids = classids + classid;
		   if(i != neko.getList().size() - 1)classids = classids +" AND ";
		}
		p("INPUT: "+classids);
	   }else{
		p("INPUT:" + watch.getClass().getAnnotation(PluginInfo.class).classID() + "-");
	   }
	   devs = getOutActionList(watch);
	   for (iActions act : devs) {
		p("\tOUTPUT:" + act.getClass().getAnnotation(PluginInfo.class).classID() + "--");
		action = act.getParamList();
		for (String name : action.keySet()) {
		   p("\t\t" + name + ":" + action.get(name));
		}

	   }

	}
   }

   public static void terminateWatch() {
	LOOP = false;
   }

   /**
    * ロード中の入力ルール(iStates)をArrayListに出力する。
    *
    * @return
    */
   public static ArrayList<iStates> getRules() {
	ArrayList<iStates> keys = new ArrayList<>();
	iStatesContainer cont;
	for (Map.Entry<iStates, ArrayList<iActions>> entry : GPIO_INPUTLIST.entrySet()) {
	   iStates watch = entry.getKey();
	   
	   keys.add(watch);
	   
	}
	return keys;
   }

   public static ArrayList<iActions> getOutActionList(iStates key) {
	ArrayList<iActions> list = GPIO_INPUTLIST.get(key);

	return list;
   }

   public static void setIndicator(GPIOSwitcher indicator) {
	INDICATOR = indicator;
   }

   private static void indicatorState(int loop) {
	if (INDICATOR != null) {
	   try {
		INDICATOR.go(50, loop);
	   } catch (IOException ex) {
		Logger.getLogger(ActionRuleSet.class.getName()).log(Level.SEVERE, null, ex);
	   }
	}
   }

   public static void indicatorState(int loop, int during) {
	if (INDICATOR != null) {
	   try {
		INDICATOR.go(during, loop);
	   } catch (IOException ex) {
		Logger.getLogger(ActionRuleSet.class.getName()).log(Level.SEVERE, null, ex);
	   }
	}
   }

   private static void p(Object o) {
	System.out.println(o);
   }

   public static void clearRules() {
	GPIO_INPUTLIST.clear();
   }

}
