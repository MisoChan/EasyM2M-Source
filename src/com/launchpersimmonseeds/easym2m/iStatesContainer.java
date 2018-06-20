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

import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersiomonseeds.interfaces.iStates;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * リスト内のiStates(入力インスタンス)を纏め、すべての条件がTrue時にTrueを返すコンテナ
 *
 * @author MisoChan
 */

public class iStatesContainer implements iStates {

   private final ArrayList<iStates> CONTAINER;

   public iStatesContainer(ArrayList<iStates> container) {
	CONTAINER = container;
	Checker.stdout(CONTAINER.size());
   }

   public ArrayList<iStates> getList() {

	return CONTAINER;
   }

   @Override
   public boolean getState() {
	boolean state = true;
	//コンテナ内でどれか一つでもFalseが帰った時Falseを返す
	for (iStates input : CONTAINER) {
	   
	   try {
		if (!input.getState()) {
		   
		   state = false;
		}
		Thread.sleep(5L);
	   } catch (InterruptedException ex) {
		Logger.getLogger(iStatesContainer.class.getName()).log(Level.SEVERE, null, ex);
	   }
	}
	return state;
   }

   @Override
   public Map<String, String> getParamList() {
	Map<String, String> param = new HashMap<>();
	for (int i = 0; i < CONTAINER.size(); i++) {
	   param.put(Integer.toString(i), CONTAINER.get(i).getClass().getAnnotation(PluginInfo.class).classID());
	}
	return param;
   }

}
