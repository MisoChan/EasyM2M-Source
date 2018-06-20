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
package com.launchpersiomonseeds.interfaces;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author MisoChan
 */
public interface iTools {
      /**
    * パラメータおよびアイテム名をMap<アイテム名,パラメータ>で返す
    *
    * @return
    */
   public Map<String, String> getParamList();

   /**
    * パラメータ内のアイテム名一覧をArrayListで返却する。
    *
    * @return
    */
   public default ArrayList<String> getItems() {
	ArrayList<String> item = new ArrayList<>();
	getParamList().entrySet().forEach((param) -> {
	   item.add(param.getKey());
	});
	return item;
   }

   public default String getParamSetting(String item) {
	return getParamList().get(item);
   }
   
   
}
