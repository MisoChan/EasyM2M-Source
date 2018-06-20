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
package com.launchpersimmonseeds.easym2m.base;

import com.launchpersimmonseeds.easym2m.Checker;
import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersiomonseeds.interfaces.iActions;
import com.launchpersiomonseeds.interfaces.iStates;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MisoChan
 */
public class JarClassLoader {

    private final static String LIBS_PACKAGE = "com.launchpersimmonseeds.easym2m.libs";
    private final static String NET_PACKAGE = "com.launchpersimmonseeds.easym2m.networking";
    private static URL RESOURCE;
    private static Map<String, Class<?>> PLUGIN_LIBS = new HashMap<>();
    private static ClassLoader LOADER;

    /**
     * インスタンス生成不可
     */
    private JarClassLoader() {
        throw new AssertionError("There is no cat which is so dangerous. Sorry!!");
    }

    /**
     * 引数に受け取ったクラスをプラグインリストに追加してもよいか判断する。
     *
     * @param classtype
     * @return
     */
    private static boolean canAddClassList(Class<?> classtype) {
        /**
         * プラグインリスト追加条件(2018/4/11)
         *
         * 1.PluginInfoを使ったアノテーションを用いている. 2.classIDがきっちり入っている（isEmpty使用）.
         * 3.IFのiActons、iStatesを実装している.
         *
         */

        //ちょっと条件長すぎやしませんかね…！
        return classtype.isAnnotationPresent(PluginInfo.class)
                && !classtype.getAnnotation(PluginInfo.class).classID().isEmpty()
                && (Arrays.asList(classtype.getInterfaces()).contains(iStates.class)
                || Arrays.asList(classtype.getInterfaces()).contains(iActions.class));
    }

    /**
     * JAR内のクラスID名と呼び出しクラスを紐付けたMapを生成し返す。 クラスIDをKeyとして、class型のオブジェクトをValueにする。
     *
     * @return
     */
    private static Map<String, Class<?>> getBaseClassIDList(String packname) throws IOException {
        Map<String, Class<?>> classlist = new HashMap<>();

        //このスレッドのクラスローダを取得する。
        LOADER = Thread.currentThread().getContextClassLoader();

        URL url = LOADER.getResource(packname.replace('.', '/'));

        switch (url.getProtocol()) {
            case "jar":
                classlist.putAll(findJarClass(packname, url));
                break;
            case "file":
                classlist.putAll(findFileClass(packname, url));
                break;
            default:
                Checker.stdout("ERROR:", url + " can not read");
                break;
        }
        return classlist;

    }

    private static Map<String, Class<?>> findJarClass(String rootpackname, URL jarurl) throws IOException {
        Map<String, Class<?>> classlist = new HashMap<>();
        JarFile jarfile = null;
        JarURLConnection jarconnection = (JarURLConnection) jarurl.openConnection();
        JarEntry jarEntry;
        String classname;
        Class<?> classtype;
        jarfile = jarconnection.getJarFile();
        PluginInfo annotate;

        Enumeration<JarEntry> jenum = jarfile.entries();
        try {
            while (jenum.hasMoreElements()) {
                jarEntry = jenum.nextElement();

                //そもそもクラスなのか判定
                if (Checker.isEndMatch(jarEntry.getName(), ".class") && !jarEntry.getName().contains("$")) {
                    classname = jarEntry.getName();
                    
                    //リフレクションに用いる上で余計な文字列を削除
                    classtype = LOADER.loadClass(Checker.deleteLastWords(jarEntry.getName(), 6).replace("/","."));
                    if (canAddClassList(classtype)) {
                        //アノテーション、ClassID取得
                        
                        annotate = (PluginInfo) classtype.getAnnotation(PluginInfo.class);

                        classlist.put(annotate.classID(), classtype);

                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            Checker.stdout("[ERROR]", "Class not found!! " + ex);
            Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return classlist;
    }

    /**
     * File型からJarを読み出し、Classを読み出し、プラグインリストをつくる
     *
     * @param packname
     * @param fileurl
     * @return
     * @throws IOException
     */
    private static Map<String, Class<?>> findFileClass(String packname, URL fileurl) throws IOException {
        //読み込みディレクトリ
        File read_dir = new File(fileurl.getFile());

        //ディレクトリ内ファイル読み込み
        File read;
        Map<String, Class<?>> classlist = new HashMap<>();
        try {
            for (String path : read_dir.list()) {

                read = new File(read_dir, path);

                //classファイルである&&ファイルであるかの判定&&class名に$を含まない
                if (Checker.isEndMatch(path, ".class") && read.isFile() && !read.getName().contains("$")) {

                    Class<?> loadClass;

                    loadClass = LOADER.loadClass(packname + "." + Checker.deleteLastWords(read.getName(), 6));

                    if (canAddClassList(loadClass)) {
                        classlist.put(loadClass.getAnnotation(PluginInfo.class).classID(), loadClass);
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return classlist;

    }

    /**
     * classIDを引数にとり、初回呼び出し時にプラグインリストをロード、検索し、クラスアノテーション属性:ClassIDと符合するclassを返す。
     * classIDに符合するclassがなければnullを返す
     *
     * @param classid
     * @return
     */
    public static Class<?> getPluginClass(String classid) {

        if (PLUGIN_LIBS.isEmpty()) {
            try {
                PLUGIN_LIBS = getBaseClassIDList(LIBS_PACKAGE);
                PLUGIN_LIBS.putAll(getBaseClassIDList(NET_PACKAGE));
            } catch (IOException ex) {
                Logger.getLogger(JarClassLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return PLUGIN_LIBS.get(classid);
    }

}
