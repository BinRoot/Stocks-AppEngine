package com.binroot.stocks;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class StockServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		String operation = (String) req.getParameter("op");
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		if(operation.equals("c")) { // c = create
			Entity configEnt = getConfigEntity(ds);
			long stockId = (Long) configEnt.getProperty("stockId");
			configEnt.setProperty("stockId", stockId+1);
			ds.put(configEnt);
			
			String name = (String) req.getParameter("name");
			String desc = (String) req.getParameter("desc");
			
			int initial = Integer.parseInt(req.getParameter("initial"));
			
			
			
			int currentHourVal = initial;
			String shareHolderList = "";
			
			int hour = Calendar.getInstance(TimeZone.getTimeZone("GMT")).get(Calendar.HOUR_OF_DAY); 
			String hourlyPoint1 = hour+";"+initial+";";
			String hourlyPointList = hourlyPoint1; 
			String dailyPointList = "";
			
			double trend = 0;
			
			String historyList = "";
			
			
			Entity stockEntity = new Entity("Stock");
			stockEntity.setProperty("id", stockId);
			stockEntity.setProperty("name", name);
			stockEntity.setProperty("desc", desc);
			stockEntity.setProperty("initial", initial);
			stockEntity.setProperty("openingDayVal", hour);
			stockEntity.setProperty("currentHourVal", currentHourVal);
			stockEntity.setProperty("lastTransaction", Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			stockEntity.setProperty("shareHolderList", shareHolderList);
			stockEntity.setProperty("hourlyPointList", hourlyPointList);
			stockEntity.setProperty("dailyPointList", dailyPointList);
			stockEntity.setProperty("trend", trend);
			stockEntity.setProperty("historyList", historyList);
			stockEntity.setProperty("picture", "pic01");
			stockEntity.setProperty("buys", (int)0);
			stockEntity.setProperty("sells", (int)0);
			ds.put(stockEntity);
		}
		else if(operation.equals("r")) { // r = read
			String userId = (String) req.getParameter("id");
			Entity userEntity = getUserEntity(ds, userId);
			if(userEntity!=null) {
				String stockList = (String) userEntity.getProperty("stockList");
				String[] stockListArr = stockList.split(";");
				for(int i=0; i<stockListArr.length; i+=2) {
					String stockL = stockListArr[i];
					String numSharesL = stockListArr[i+1];
					// print out
					resp.getWriter().append("("+stockL+", "+ numSharesL+") ");

				}
			}
		}

	}
	
	public Entity getConfigEntity(DatastoreService ds) {
		Query q = new Query("Config");
		PreparedQuery pq = ds.prepare(q);
		for(Entity e : pq.asIterable()) {
			return e;
		}
		return null;
	}
	
	public Entity getUserEntity(DatastoreService ds, String userId) {
		Query q = new Query("User");
		PreparedQuery pq = ds.prepare(q);
		for(Entity e : pq.asIterable()) {
			if(e.getProperty("id").equals(userId)) {
				return e;
			}
		}
		return null;
	}
}
