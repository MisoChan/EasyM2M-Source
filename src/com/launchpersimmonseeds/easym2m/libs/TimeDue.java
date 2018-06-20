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
package com.launchpersimmonseeds.easym2m.libs;

import com.launchpersiomonseeds.interfaces.PluginInfo;
import com.launchpersiomonseeds.interfaces.iStates;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author MisoChan
 */
@PluginInfo(classID = "TIME", author = "MisoChan", version = "1.3")

public class TimeDue implements iStates {

   private String FROM, TO;
   
   public TimeDue(String from,String to){
	FROM = from;
	TO = to;
   }
   
   public TimeDue(Map<String, String> param) {
	FROM = param.get("FROM");
	TO = param.get("TO");
   }

   /**
    * 現在時刻（RTC）が設定した時間内なのか判定する
    *
    * @return
    */
   private static boolean isNowDuringTime(String from, String to) {
	LocalTime now = LocalTime.now();
	String fromtimes[] = from.split(":");
	LocalTime fromtime = LocalTime.of(Integer.parseInt(fromtimes[0]), Integer.parseInt(fromtimes[1]), 0);
	String totimes[] = to.split(":");
	LocalTime totime = LocalTime.of(Integer.parseInt(totimes[0]), Integer.parseInt(totimes[1]), 0);
	
	//24時またぎ用条件
	if(fromtime.isAfter(totime))return (now.isAfter(fromtime) || now.isBefore(totime)) || fromtime.equals(now) || totime.equals(now);
	
	return (now.isAfter(fromtime) && now.isBefore(totime)) || fromtime.equals(now) || totime.equals(now);
   }

   @Override
   public Map<String, String> getParamList() {
	Map<String, String> params = new HashMap<>();
	params.put("FROM", FROM);
	params.put("TO", TO);
	return params;
   }

   @Override
   public ArrayList<String> getItems() {
	return iStates.super.getItems(); 
   }

   @Override
   public boolean getState() {
	return isNowDuringTime(FROM, TO);
   }

}
