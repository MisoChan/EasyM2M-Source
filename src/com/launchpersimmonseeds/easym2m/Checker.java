/*
 * Copyright (C) 2018 MisoChan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.launchpersimmonseeds.easym2m;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 各種文字列等が規定のフォーマットに則っているか、ファイルが存在しているか等 チェック項目を纏めたclass
 * …のはずなんだけど最近標準出力させたりとごちゃまぜになってしまった
 *
 * @author MisoChan
 */
public class Checker {

   static Pattern NUMBER = Pattern.compile("^[0-9]*$");

   private Checker() {
	throw new AssertionError("There is not a cat which is explosive. Sorry!");

   }

   public static boolean isFileExist(String filename) {
	return new File(filename).exists();
   }

   /**
    * 引数（String）で受け取った文字列がIPアドレスのフォーマットに則っているか確認する。
    *
    * @param addr
    * @return
    */
   public static boolean isIPAddress(String addr) {
	/*
        Stringとして受け取ったIPアドレス（仮）をsplit(".",0)で配列に切り分け格納　>> chkaddr[]
        chkaddr[] をcharに変換。CLASS_A-CまでのIPアドレス(0.0.0.0 - 239.255.255.255)を有効と判定する。
        
        -補足-
        InetAddress.getByName()を使用すればIPアドレスとして使用不可能な場合UnknownHostError例外が投げられるため判別可能…（TCPSocketClient.javaはこの手法）
        ただし例外を使用する前提で書くとソースコード自体の行数が増えてしまう（個別catchする項目が増える）等、不都合が多いため、ここでは使用しない方針でいきます。
        
        by MisoChan 2018 3/2
	 */

	boolean status = false;
	int ipnum;
	String[] chkaddr;

	chkaddr = addr.split("\\.", 0);

	//IPアドレスは127.0.0.1 のように 数字(10進数)部が必ず4つとなる。満たせばチェック開始
	if (chkaddr.length == 4) {
	   for (int octed = 0; octed < chkaddr.length; octed++) {

		status = false;
		if (isStringNumber(chkaddr[octed])) {
		   ipnum = Integer.parseInt(chkaddr[octed]);
		   //将来のCLASS毎処理分岐の為if分岐
		   if (octed == 0 & ipnum > 0 & ipnum < 224) {
			status = true;
		   }

		   if (octed != 0 & ipnum >= 0 & ipnum < 256) {
			status = true;
		   }
		}

		if (!status) {
		   break;
		}

	   }

	} else {

	   return false;
	}

	return status;
   }

   /**
    * MACアドレスか否かの判定を行う 引数で受け取った文字列がMACアドレスとして使用可能であればtrueを返す
    *
    * @param addr 判定アドレス文
    * @return 判定結果
    */
   public static boolean isMACAddress(String addr) {

	int cnt = 0, splace = 2;  //cnt:有効文字数カウント splace:区切り（":"）の場所 
	boolean result = false;

	addr = addr.toUpperCase();
	char cmacaddr[] = addr.toCharArray();

	if (cmacaddr.length == 17) {
	   // ":"または"-"が来るのは 2 5 8 11 14 番目 -> 3n+2
	   for (int i = 0; i < cmacaddr.length; i++) {

		//実装方法がちょっと愚直すぎるので要改善
		if (i == splace & cmacaddr[i] == ':') {
		   cnt++;
		   splace = splace + 3;

		   //A-F若しくは0-9の間であればカウントアップ
		} else if ((cmacaddr[i] >= 'A' & cmacaddr[i] <= 'F') | (cmacaddr[i] >= '0' & cmacaddr[i] <= '9')) {
		   cnt++;

		} else {
		   break;
		}
	   }
	   //カウント結果が17であれば正解

	   result = cnt == 17 ? true : false;
	}

	return result;
   }

   /**
    * 引数のString文字列が数字として使用可能な場合、trueを返却する。
    *
    * @param str
    * @return 判定結果
    */
   public static boolean isStringNumber(String str) {

	Matcher m = NUMBER.matcher(str);

	return m.find();
   }

   /**
    * strの語尾がendと一致している場合trueを返す。
    *
    * @param str
    * @param end
    * @return
    */
   public static boolean isEndMatch(String str, String end) {

	return str.endsWith(end);
   }

   /**
    * regex中からwords文字列をすべて削除(空文字""に置き換え)する。
    *
    * @param regex 元文字列
    * @param words 削除文字列
    * @return
    */
   public static String deleteWords(String regex, String... words) {
	for (String delete : words) {
	   regex = regex.replaceAll(delete, "");
	}
	return regex;
   }

   public static String deleteLastWords(String regex, int count) {
	return regex.substring(0, regex.length() - count);
   }

   /**
    * デバッグ用stdout
    *
    * @param name
    * @param s
    */
   public static void stdout(Object name, Object s) {
	System.out.println("[DEBUG]: " + name + " : " + s);
   }

   public static void stdout(Object s) {
	System.out.println("[DEBUG]:" + s.getClass() + ": " + s);
   }

   /**
    * Mapの内容を一覧表示する。
    *
    * @param map
    */
   public static void printMap(Map<?, ?> map, String... map_name) {
	if (map_name.length > 0) {
	   System.out.println("[DEBUG_MAP]:" + map_name[0]);
	} else {
	   System.out.println("[DEBUG_MAP]:");
	}
	map.keySet().forEach((key) -> {
	   System.out.println("[DEBUG]: KEY:" + key + "\t-\tVALUE:" + map.get(key));
	});
	System.out.println("[DEBUG_MAP_END]");
   }

   /**
    * 時刻フォーマットに則っているか判定
    *
    * @param time
    * @return
    */
   public static boolean isTime(String time) {
	boolean state = false;
	if (time.charAt(2) == ':') {
	   String times[] = time.split(":");
	   if (isStringNumber(times[0]) && isStringNumber(times[1]) && times.length == 2) {
		if (Integer.parseInt(times[0]) < 24 && Integer.parseInt(times[1]) < 59) {
		   state = true;
		}
	   }
	}
	return state;
   }

}
