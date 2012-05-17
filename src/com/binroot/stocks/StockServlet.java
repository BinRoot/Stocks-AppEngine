package com.binroot.stocks;
import java.io.IOException;
import java.util.ArrayList;
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
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

/**
 * POST /stock?op=c&name=iPhone&desc=blah&initial=33
 * @author Nishant
 *
 */
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
			stockEntity.setProperty("openingDayVal", initial);
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
			String userId = (String) req.getParameter("userId");
			Entity userEntity = getUserEntity(ds, userId);
			
			Gson gst = new Gson();
			
			String masterJSON = "{data:[";
			
			if(userEntity!=null) {
				
				String stockList = (String) userEntity.getProperty("stockList");
				
				System.out.println("> stocklist: "+stockList);
				
				String[] stockListArr = stockList.split(";");
				
				for(int i=0; i<stockListArr.length; i+=2) {
					if(stockListArr.length<=1) {
						break;
					}
					long stockL = Long.parseLong(stockListArr[i]);
					long numSharesL = Long.parseLong(stockListArr[i+1]);
					// print out
					//resp.getWriter().append("("+stockL+", "+ numSharesL+") ");

					BagOfInfo bgi = new BagOfInfo();
					Entity stockEnt = getStockEntity(ds, stockL);
					String name = (String) stockEnt.getProperty("name");
					long id = (Long) stockEnt.getProperty("id");
					long currentHourVal = (Long) stockEnt.getProperty("currentHourVal");
					String hourlyPointList = (String) stockEnt.getProperty("hourlyPointList");
					long zeroHourVal = Long.parseLong(hourlyPointList.split(";")[1]);
					double trend = (Double) stockEnt.getProperty("trend");
					String picture = (String) stockEnt.getProperty("picture");

					bgi.name = name;
					bgi.id = id;
					bgi.currentHourVal = currentHourVal;
					bgi.zeroHourVal = zeroHourVal;
					bgi.numShares = numSharesL;
					bgi.trend = trend;
					bgi.picture = picture;
					
					masterJSON = masterJSON + gst.toJson(bgi) +", ";
				}
				
			}
			masterJSON = masterJSON + "{}]}";
			
			resp.getWriter().append(masterJSON);
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
	
	public Entity getStockEntity(DatastoreService ds, long stockId) {
		System.out.println("searching for stock "+stockId);
		Query q = new Query("Stock");
		PreparedQuery pq = ds.prepare(q);
		for(Entity e : pq.asIterable()) {
			System.out.println("found "+e.getProperty("id"));
			if(((Long)e.getProperty("id"))==stockId) {
				return e;
			}
		}
		System.out.println("failed to find "+stockId);
		return null;
	}
	
	class BagOfInfo {
		public String name;
		public long id;
		public long currentHourVal;
		public long zeroHourVal;
		public long numShares;
		public double trend;
		public String picture;
	}
}


