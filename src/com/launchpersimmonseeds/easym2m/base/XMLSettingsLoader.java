package com.launchpersimmonseeds.easym2m.base;

import com.launchpersimmonseeds.easym2m.ActionRuleSet;
import com.launchpersimmonseeds.easym2m.Checker;
import com.launchpersimmonseeds.easym2m.NetRuleSet;
import com.launchpersimmonseeds.easym2m.iStatesContainer;
import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersimmonseeds.easym2m.libs.GPIOSwitcher;
import com.launchpersiomonseeds.interfaces.iActions;
import com.launchpersiomonseeds.interfaces.iStates;
import com.launchpersiomonseeds.interfaces.iTools;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * XMLから設定をロード/生成するクラス
 *
 * @author MisoChan
 */


public class XMLSettingsLoader {

   private static final File XML_FILE = new File("IoT_Config.xml");

   private GPIOSwitcher INDICATOR = null;

   private XMLSettingsLoader() throws Exception {

   }

   public boolean isXmlExist() {
	System.err.println("XMLFILE DOES NOT EXIST");
	return XML_FILE.exists();
   }

   public GPIOSwitcher getIndicator() {
	System.out.println(INDICATOR);
	return INDICATOR;
   }

   /**
    * XML読み込み
    *
    * @return
    * @throws Exception
    */
   public static boolean readXml() throws Exception {

	//XMLの読み込み
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	DocumentBuilder builder = factory.newDocumentBuilder();
	Document xmldoc = builder.parse(new FileInputStream(XML_FILE));
	Element docElement = xmldoc.getDocumentElement();

	//メモリ上の既存ルールをクリアする
	ActionRuleSet.clearRules();
	NetRuleSet.clearRules();

	//ルートノード（Setting）を読み込む。
	Node settingNode = docElement.getFirstChild();

	while (settingNode != null) {
	   //各ノードの読み出し
	   String nodename = settingNode.getNodeName();

	   switch (nodename) {

		//GPIOノードの読み込み
		case "LOCAL":

		   Node PluginNode = settingNode.getFirstChild();
		   inputNodeReader(PluginNode);
		   break;

		//NETノードの読み込み    
		case "NET":
		   Node netNode = settingNode.getFirstChild();
		   NETNodeReader(netNode);
		   break;
	   }

	   settingNode = settingNode.getNextSibling();

	}

	return true;
   }

   /**
    * Plugins入力ノードの読み込み
    *
    * @param Node node
    * @throws IllegalArgumentException
    * @throws IOException
    */
   private static void inputNodeReader(Node node) throws IllegalArgumentException, IOException, InstantiationException {
	/**
	 * -読み込み動作手順-　2018/04/12
	 *
	 * 1.引数でノードを受け取る 2.ノード名を読む 3.もし後ろ"_IN"が入っていたらJarClassLoader(JCL)を呼出
	 * 4.JCLから使用可能クラス検索しClassを渡す 5.ここでiState型で生成、子ノードに親入力iState渡す
	 *
	 * これをwhileでNull出るまで回す
	 */
	Node childnode;
	String in_nodename;
	try {
	   while (node != null) {

		//読み込んだノード名に_INが語尾についているか？
		if (node.getNodeName().endsWith("_IN")) {
		   //ノード名から"_IN"を無視して読み込み
		   in_nodename = Checker.deleteLastWords(node.getNodeName(), 3);
		   Class<?> plugin = JarClassLoader.getPluginClass(in_nodename);
		   //iStates実装してるか？
		   if (Arrays.asList(plugin.getInterfaces()).contains(iStates.class)) {
			//インスタンス生成する
			iStates input = (iStates) plugin.getConstructor(Map.class).newInstance(getAttributeMap(node));
			//OutputNode（子ノード）に遷移
			childnode = node.getFirstChild();
			outNodeReader(childnode, input);
		   }
		}

		node = node.getNextSibling();
	   }
	} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
	   Logger.getLogger(XMLSettingsLoader.class.getName()).log(Level.SEVERE, null, ex);
	}
   }

   /**
    * まとめます。よろしくおねがいします。
    *
    * @param outNode
    * @param input
    */
   private static void outNodeReader(Node node, iStates input) throws NoSuchMethodException, InstantiationException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	iStates instate;
	String out_nodename, in_nodename;
	ArrayList<iStates> inputs = new ArrayList<>();
	ArrayList<iActions> acts = new ArrayList<>();
	while (node != null) {
	   out_nodename = node.getNodeName();

	   if (node.getNodeName().endsWith("_OUT")) {
		//ノード名から_OUTを無視して
		out_nodename = Checker.deleteLastWords(node.getNodeName(), 4);
		Class<?> plugin = JarClassLoader.getPluginClass(out_nodename);
		//JCRから読み出し
		if (Arrays.asList(plugin.getInterfaces()).contains(iActions.class)) {
		   //インスタンス生成する
		   iActions output = (iActions) plugin.getConstructor(Map.class).newInstance(getAttributeMap(node));
		   acts.add(output);
		}

	   }

	   if (node.getNodeName().endsWith("_IN")) {
		//ノード名から"_IN"を無視して読み込み
		in_nodename = Checker.deleteLastWords(node.getNodeName(), 3);
		Class<?> plugin = JarClassLoader.getPluginClass(in_nodename);
		//iStates実装してるか？
		if (Arrays.asList(plugin.getInterfaces()).contains(iStates.class)) {
		   //インスタンス生成する
		   instate = (iStates) plugin.getConstructor(Map.class).newInstance(getAttributeMap(node));
		   inputs.add(instate);
		}
	   }
	   node = node.getNextSibling();
	}

	//もし読み込み済みINノードが２つ以上あったら、コンテナに追加し格納
	if (!inputs.isEmpty()) {
	   
	   inputs.add(0,input);
	   for(iStates st :inputs){
		Checker.stdout(st.getClass().getAnnotation(PluginInfo.class).classID());
	   }
	   iStatesContainer container = new iStatesContainer(inputs);
	   ActionRuleSet.addInput(container);
	   ActionRuleSet.addOutActionList(container, acts);
	} else {
	   //ActionRuleSet追加
	   ActionRuleSet.addInput(input);
	   ActionRuleSet.addOutActionList(input, acts);
	   
	}

	p("Action Loaded!!");

   }

   private static void NETNodeReader(Node netNode) {
	String nodename, addr, command;
	Node devnode;
	try {
	   while (netNode != null) {
		nodename = netNode.getNodeName();
		switch (nodename) {
		   case "TCP_IN":
			addr = getAttribute("IP", netNode);
			command = getAttribute("COMMAND", netNode);
			NetRuleSet.addInputNet(addr, command);
			devnode = netNode.getFirstChild();

			outNodeNETReader(devnode, addr, command);
			break;

		}

		netNode = netNode.getNextSibling();
	   }
	} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
	   Logger.getLogger(XMLSettingsLoader.class.getName()).log(Level.SEVERE, null, ex);
	}
   }

   private static void outNodeNETReader(Node node, String addr, String command) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	String out_nodename;

	while (node != null & !addr.isEmpty()) {
	   out_nodename = node.getNodeName();
	   p(out_nodename);

	   out_nodename = node.getNodeName();

	   if (out_nodename.endsWith("_OUT")) {
		//ノード名から_OUTを無視して
		out_nodename = Checker.deleteLastWords(node.getNodeName(), 4);
		Class<?> plugin = JarClassLoader.getPluginClass(out_nodename);
		//JCRから読み出し
		if (Arrays.asList(plugin.getInterfaces()).contains(iActions.class)) {
		   //インスタンス生成する
		   iActions output = (iActions) plugin.getConstructor(Map.class).newInstance(getAttributeMap(node));

		   try {
			//格納
			NetRuleSet.addOut(addr, command, output);
		   } catch (UnknownHostException ex) {
			Logger.getLogger(XMLSettingsLoader.class.getName()).log(Level.SEVERE, null, ex);
		   }
		}

	   }

	   node = node.getNextSibling();
	}

   }

   /**
    * ノード内属性をMap（Key:属性名 Value:値）で返す
    *
    * @param node
    * @return
    */
   private static Map<String, String> getAttributeMap(Node node) {
	String attrname, value;
	Map<String, String> map = new HashMap<>();
	NamedNodeMap attributes = node.getAttributes();
	Node attribute;

	for (int i = 0; i < attributes.getLength(); i++) {
	   attribute = attributes.item(i);
	   attrname = attribute.getNodeName();
	   value = attribute.getNodeValue();
	   map.put(attrname, value);
	}

	return map;
   }

   /**
    * ノードからテキスト部分を取得する。
    *
    * @param node
    * @return
    */
   private static String getTextNode(Node node) {
	Node textNode = node.getFirstChild();
	while (node.getNodeType() == Node.TEXT_NODE) {

	   node.getNextSibling();
	}
	return textNode.getNodeValue();

   }

   private static String getClassID(iTools act) {
	return act.getClass().getAnnotation(PluginInfo.class).classID();
   }

   private static String getAttribute(String attrName, Node node) {
	NamedNodeMap attributes = node.getAttributes();
	Node attribute;
	if (attributes != null) {
	   attribute = attributes.getNamedItem(attrName);
	   return attribute.getNodeValue();
	}

	return null;
   }

   private static boolean writeXml(File XML_File, Document DOCUMENT) {
	Transformer transformer;

	TransformerFactory tFactory = TransformerFactory.newInstance();
	try {
	   transformer = tFactory.newTransformer();
	} catch (TransformerConfigurationException ex) {
	   Logger.getLogger(XMLSettingsLoader.class.getName()).log(Level.SEVERE, null, ex);
	   return false;
	}
	transformer.setOutputProperty("indent", "yes");
	transformer.setOutputProperty("encoding", "UTF-8");

	// XMLファイルの生成
	try {
	   transformer.transform(new DOMSource(DOCUMENT), new StreamResult(XML_File));
	} catch (TransformerException e) {

	   return false;
	}

	return true;
   }

   public static boolean generateXml() throws Exception {
	boolean status;

	DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	Document DOCUMENT = builder.newDocument();
	Element outPlugins ,andPlugins,inPLUGINS;
	Element setting = DOCUMENT.createElement("Settings");
	DOCUMENT.appendChild(setting);

	//XML中身の生成:LOCAL部
	Element LOCAL = DOCUMENT.createElement("LOCAL");
	setting.appendChild(LOCAL);

	ArrayList<iStates> inputlist = ActionRuleSet.getRules();
	ArrayList<String> items;

	iStatesContainer container;
	if (!inputlist.isEmpty()) {

	   for (iStates in : inputlist) {
		
		if(in.getClass().equals(iStatesContainer.class)){
		   container = (iStatesContainer) in;
		   inPLUGINS = DOCUMENT.createElement(getClassID(container.getList().get(0)) + "_IN");
		   for (String name : container.getList().get(0).getItems()) {
			inPLUGINS.setAttribute(name, container.getList().get(0).getParamSetting(name));
		   }
		   //まだ未検証2018/4/12
		   for(int i = 1;i<container.getList().size();i++){
			andPlugins = DOCUMENT.createElement(getClassID(container.getList().get(i)) + "_OUT");
			for (String inputs : container.getList().get(i).getItems()) {
			   andPlugins.setAttribute(inputs, container.getList().get(i).getParamSetting(inputs));
			}
			inPLUGINS.appendChild(andPlugins);
		   }
		   
		}else{
		   inPLUGINS = DOCUMENT.createElement(getClassID(in) + "_IN");

		   for (String name : in.getItems()) {
			inPLUGINS.setAttribute(name, in.getParamSetting(name));
		   }
		}
		if (!ActionRuleSet.getOutActionList(in).isEmpty()) {

		   for (iActions act : ActionRuleSet.getOutActionList(in)) {
			outPlugins = DOCUMENT.createElement(getClassID(act) + "_OUT");
			for (String outs : act.getItems()) {
			   
			   outPlugins.setAttribute(outs, act.getParamSetting(outs));
			}

			inPLUGINS.appendChild(outPlugins);
		   }

		}
		LOCAL.appendChild(inPLUGINS);
	   }

	}

	//XML中身の生成:NET部
	Element NET = DOCUMENT.createElement("NET");

	ArrayList<InetAddress> clientlist = NetRuleSet.getClientAddressList();
	ArrayList<String> commandlist;
	ArrayList<iActions> devices;
	if (!clientlist.isEmpty()) {

	   for (InetAddress addr : clientlist) {

		commandlist = NetRuleSet.getClientCommandList(addr);
		if (!commandlist.isEmpty()) {
		   for (String comm : commandlist) {
			devices = NetRuleSet.getClientDeviceList(addr, comm);

			Element inNet = DOCUMENT.createElement("TCP_IN");
			inNet.setAttribute("IP", addr.getHostAddress());
			inNet.setAttribute("COMMAND", comm);

			for (iActions dev : devices) {

			   Element out = DOCUMENT.createElement(getClassID(dev) + "_OUT");
			   items = dev.getItems();
			   for (String name : items) {
				out.setAttribute(name, dev.getParamSetting(name));
			   }
			   inNet.appendChild(out);

			}
			NET.appendChild(inNet);
		   }
		}

	   }

	}
	//XML中身の生成:インジケーター部
	Element Indicators = DOCUMENT.createElement("Indicator");
	if (ActionRuleSet.getIndicator() != null) {
//            Indicators.setAttribute("port", Integer.toString(ActionRuleSet.getIndicator().PORT_NUMBER));
	} else {
	   Indicators.setAttribute("port", "none");
	}

	LOCAL.appendChild(Indicators);

	setting.appendChild(NET);
	setting.appendChild(LOCAL);
	//ファイル生成
	status = writeXml(XML_FILE, DOCUMENT);

	return status;
   }

   private static void p(Object obj) {
	System.out.println(obj);
   }

}
