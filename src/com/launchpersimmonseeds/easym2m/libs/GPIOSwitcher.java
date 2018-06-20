/*
*This program is Prototype.
 */
package com.launchpersimmonseeds.easym2m.libs;

import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersiomonseeds.interfaces.iStates;
import com.launchpersiomonseeds.interfaces.iActions;
import com.launchpersimmonseeds.easym2m.Checker;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GPIO操作全般を行う。
 *
 * @author MisoChan
 *
 *
 */
@PluginInfo(classID = "GPIO", author = "MisoChan", version = "1.0")
public class GPIOSwitcher implements iActions, iStates {

   /**
    * 使用するGPIOポート番号
    *
    */
   private final int PORT_NUMBER;

   /**
    * 書き込み（出力）可否のフラグ。TRUEで書き込み可能。FALUSEで読み込み専用
    */
   private final String DIRECTION;
   private final String REGISTANCE_PULL;

   /**
    * GPIO操作ディレクトリの指定
    */
   private final String GPIO_DIR = "/sys/class/gpio/";

   private long GO_DURING = 1000;
   private int GO_LOOP = 1;

   private volatile boolean INIT = false, DUE = false;

   /**
    * GPIOポート操作全般の制御を行うクラス
    *
    *
    * @param number
    * @param direction 書き込み可否(String) in（入力） out（出力）(それ以外ならinになる)
    * @param pull プルアップorダウン抵抗の設定（入力モード時のみ機能します) up = プルアップ down = プルダウン
    * (それら以外であればupになる)
    * @throws java.io.IOException 予約不可のときに投げられる
    */
   public GPIOSwitcher(int number, String direction, String pull) throws IOException, IllegalArgumentException {
	try {
	   this.PORT_NUMBER = number;
	   this.DIRECTION = direction.equals("in") || direction.equals("out") ? direction : "in";

	   this.REGISTANCE_PULL = pull.equals("up") || pull.equals("down") ? pull : "up";
	   this.INIT = this.existenceGPIO(true);

	} catch (Exception e) {
	   throw new IllegalArgumentException("Param ERROR");
	}

	//シャットダウンフックの設定。アプリケーションの終了時呼び出される
	//終了時にGPIO予約を解く
	Runtime.getRuntime().addShutdownHook(new Thread() {
	   @Override
	   public void run() {
		try {
		   off();
		   existenceGPIO(false);
		} catch (IOException ex) {

		}

	   }
	});

   }

   /**
    * MAP読み込み専用
    *
    * @param params
    * @throws IOException
    * @throws IllegalArgumentException
    */
   public GPIOSwitcher(Map<String, String> params) throws IOException, IllegalArgumentException {
	//数字チェック
	if (Checker.isStringNumber(params.get("PORT")) | Checker.isStringNumber(params.get("LOOP")) | Checker.isStringNumber(params.get("DURING"))) {

	}
	try {
	   this.PORT_NUMBER = Integer.parseInt(params.get("PORT"));
	   this.DIRECTION = params.get("DIRECTION");

	   this.REGISTANCE_PULL = params.get("PULL");
	   this.GO_DURING = Long.parseLong(params.get("DURING"));
	   this.GO_LOOP = Integer.parseInt(params.get("LOOP"));

	   if (!Checker.isFileExist(GPIO_DIR + "gpio" + PORT_NUMBER)) {
		this.INIT = this.existenceGPIO(true);
	   }
	} catch (Exception e) {
	   throw new IllegalArgumentException("Param ERROR");

	}

	//シャットダウンフックの設定。アプリケーションの終了時呼び出される
	//終了時にGPIO予約を解く
	Runtime.getRuntime().addShutdownHook(new Thread() {
	   @Override
	   public void run() {
		try {
		   off();
		   existenceGPIO(false);
		} catch (IOException ex) {

		}

	   }
	});

   }

   /**
    * equalsオーバーライド ポート番号とOn時間が同一であれば等価であると判定する。
    *
    * @return
    */
   @Override
   public boolean equals(Object obj) {
	if (obj == this) {
	   return true;
	}
	if (obj == null) {

	   return false;
	}
	if (!(obj instanceof GPIOSwitcher)) {
	   return false;
	}
	GPIOSwitcher nyancat = (GPIOSwitcher) obj;
	return nyancat.PORT_NUMBER == this.PORT_NUMBER & nyancat.GO_DURING == this.GO_DURING;
   }

   @Override
   public int hashCode() {
	int hash = 5;
	hash = 3123 * hash + this.PORT_NUMBER;
	return hash;
   }

   /**
    * GPIOポートを予約し、使用可能にする。
    *
    * @param isExport
    * @return 成功可否
    * @throws IOException
    */
   public final synchronized boolean existenceGPIO(boolean isExport) throws IOException {

	String existence;

	//if (isExport){existence = "export";}else{existence = "unexport";}
	existence = isExport ? "export" : "unexport";

	/**
	 * PINが予約されている and これから予約する == true ならば 実行不可 booleanが同値のときに処理を行わないようにする
	 */
	if (isExistPIN() == isExport) {
	   System.out.println("This Port is using or inavilid.");
	   if (isExport) {
		restartExport();
	   }
	} else {
	   System.out.println("GPIO PORT " + PORT_NUMBER + "RESERVE");
	   try {
		try (PrintWriter exp = new PrintWriter(new FileWriter(GPIO_DIR + existence, true))) {
		   //書き込み(String型のみ受け付けるのでint->Stringに変換が必要)
		   exp.write(Integer.toString(PORT_NUMBER));
		} catch (IOException e) {
		   System.err.println(e);
		   throw new IllegalStateException(e);

		}

		//isExport(export処理時のみ予約をかける)
		if (isExport) {
		   if (isExistPIN() == false) {
			commandExistGPIO(existence);
		   }
		   //入出力方向の書き込みを行う。
		   try (PrintWriter port_direct = new PrintWriter(new FileWriter(GPIO_DIR + "gpio" + PORT_NUMBER + "/direction", true))) {
			port_direct.write(DIRECTION);
		   }

		}
	   } catch (IOException e) {
		System.err.println(e);
		return false;
	   }
	}
	return true;
   }

   private void commandExistGPIO(String existence) {
	//GPIO_DIR+"gpio"+PORT_NUMBER
	try {
	   Runtime runtime = Runtime.getRuntime();
	   runtime.exec("sh -c" + "\" echo " + PORT_NUMBER + " > " + GPIO_DIR + existence + "\"");
	   System.out.println("sh -c" + "\" echo " + PORT_NUMBER + " > " + GPIO_DIR + existence + "\"");
	} catch (IOException ex) {
	   System.out.println(ex);
	}

   }

   private void restartExport() {
	try {
	   existenceGPIO(false);
	   existenceGPIO(true);
	} catch (IOException e) {

	}
   }

   /**
    * GPIOポートが予約されているかどうかを出力する。
    *
    * @return GPIOポート予約有無
    */
   private boolean isExistPIN() {
	boolean result = false;
	File chkExists = new File(GPIO_DIR + "gpio" + PORT_NUMBER);
	if (chkExists.exists()) {
	   result = true;
	}
	return result;
   }

   /**
    * GPIOポートの状態を返す。 "1"ならTrue."0"ならFalse。
    *
    * @return 状態
    */
   @Override
   public boolean getState() {
	boolean status = false;
	if (isExistPIN()) {
	   try {
		File file = new File(GPIO_DIR + "gpio" + PORT_NUMBER + "/value");
		try (FileReader reader = new FileReader(file)) {
		   status = reader.read() == '1';
		   reader.close();
		}
	   } catch (FileNotFoundException ex) {
		Logger.getLogger(GPIOSwitcher.class.getName()).log(Level.SEVERE, null, ex);
	   } catch (IOException ex) {
		Logger.getLogger(GPIOSwitcher.class.getName()).log(Level.SEVERE, null, ex);
	   }
	}
	if (DIRECTION.equals("in") && REGISTANCE_PULL.equals("up")) {
	   status = !status;
	}
	return status;
   }

   public void on() throws IOException {
	if (DIRECTION.equals("out")) {
	   try (PrintWriter value = new PrintWriter(new FileWriter(GPIO_DIR + "gpio" + PORT_NUMBER + "/value"))) {
		value.write('1');
	   }
	}

   }

   public boolean fileExistCheck(String filename) {
	return new File(filename).exists();
   }

   public void off() throws IOException {
	if (DIRECTION.equals("out")) {
	   try (PrintWriter value = new PrintWriter(new FileWriter(GPIO_DIR + "gpio" + PORT_NUMBER + "/value"))) {
		value.write('0');
	   }
	}
   }

   /**
    * GPIOを1にしたのち、指定引数時間（ミリ秒）後に0にする動作をloop回繰り返す
    *
    * @param during onにする時間（ミリ秒）
    * @param loop 1,0を繰り返す回数
    * @throws IOException
    */
   public final void go(final long during, final int loop) throws IOException {

	Thread th = new Thread() {

	   @Override
	   public void run() {
		startGoProcessing();
		try {

		   for (int i = 0; i < loop; i++) {
			on();
			Thread.sleep(during);
			off();
			if (i < loop - 1) {
			   Thread.sleep(during);
			}
		   }

		} catch (IOException | InterruptedException ex) {
		   Logger.getLogger(GPIOSwitcher.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
		   finGoProcessing();
		}

	   }
	};
	if (!DUE) {
	   th.start();
	}
   }

   /**
    * setDefaultDuringで設定したミリ秒だけ（デフォルトは1000ms） GPIO出力を1にしたのち、0に戻す。
    *
    * @return 正常終了の可否
    */
   @Override
   public boolean go() {
	try {
	   
		go(GO_DURING, GO_LOOP);
	   
	} catch (IOException ex) {
	   Logger.getLogger(GPIOSwitcher.class.getName()).log(Level.SEVERE, null, ex);
	   return false;
	}
	return true;
   }

   public void setDefaultDuring(long during) {
	GO_DURING = during;

   }

   public long getDefaultDuring() {
	return GO_DURING;
   }

   public void setDefaultLoops(int loop) {
	GO_LOOP = loop;
   }


   private void finGoProcessing() {
	DUE = false;
   }

   private void startGoProcessing() {
	DUE = true;
   }

   @Override
   public Map<String, String> getParamList() {
	final Map<String, String> param = new HashMap<>();
	param.put("PORT", Integer.toString(PORT_NUMBER));
	param.put("DIRECTION", DIRECTION);
	param.put("PULL", REGISTANCE_PULL);
	param.put("DURING", Long.toString(GO_DURING));
	param.put("LOOP", Integer.toString(GO_LOOP));
	return param;

   }

   @Override
   protected void finalize() {
	System.err.println("There isn't a cat which is explosive. Sorry! ");

   }

}
