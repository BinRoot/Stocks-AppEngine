package com.binroot.stocks;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

/**
 * POST /sell?userId=FB1234&stockId=0&numShares=2&shareVal=3
 *
 */
@SuppressWarnings("serial")
public class PingServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		long stockId = Long.parseLong(req.getParameter("stockId"));
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Entity stockEnt = getStockEntity(ds, stockId);
		
		Date lastTransaction = (Date)stockEnt.getProperty("lastTransaction");
		Date nowDate = Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime();
		long currentHourVal = (Long)stockEnt.getProperty("currentHourVal");
		double trend = (Double)stockEnt.getProperty("trend");
		long buys = (Long)stockEnt.getProperty("buys");
		long sells = (Long)stockEnt.getProperty("sells");
		
		System.out.println("lastTransaction: "+lastTransaction.getTime());
		System.out.println("nowDate: "+nowDate.getTime());

		int hourFactor = 1000 * 60 * 60;
		while(lastTransaction.getTime()/hourFactor != nowDate.getTime()/hourFactor) {
			//System.out.println("time: "+lastTransaction.getHours());
			
			// shift right agnostically from trend and last hour
			int nextHour = lastTransaction.getHours()+1;
			long nextHourVal = computeNextHourVal(currentHourVal, trend);
			trend = computeNextTrend(buys, sells, currentHourVal);
			
			if(nextHour>=24) { // at next day
				// update day point list
				updateDailyPointList(stockEnt, nextHourVal);
				
				// clear hour point list
				clearHourlyPointList(stockEnt);
				updateHourlyPointList(stockEnt, 0, nextHourVal);
				stockEnt.setProperty("openingDayVal", nextHourVal);
			}
			else {
				updateHourlyPointList(stockEnt, nextHour, nextHourVal);
			}
			
			// clear buys and sells
			buys = 0;
			sells = 0;
			
			currentHourVal = nextHourVal;
			lastTransaction.setTime(lastTransaction.getTime()+hourFactor);
		}
		
		stockEnt.setProperty("currentHourVal", currentHourVal);
		
		resetBuysSells(stockEnt);
		stockEnt.setProperty("lastTransaction", lastTransaction);
		
		printHourlyPointList(stockEnt);	
		printDailyPointList(stockEnt);
		
		ds.put(stockEnt);
	}
	
	public void printHourlyPointList(Entity stockEnt) {
		String hourlyPointList = (String)stockEnt.getProperty("hourlyPointList");
		System.out.println("hourlyPointList: "+hourlyPointList);
	}
	
	public void printDailyPointList(Entity stockEnt) {
		String dailyPointList = (String)stockEnt.getProperty("dailyPointList");
		System.out.println("dailyPointList: "+dailyPointList);
	}
	
	public void updateHourlyPointList(Entity stockEnt, long nextHour, long nextHourVal) {
		String hourlyPointList = (String)stockEnt.getProperty("hourlyPointList");
		hourlyPointList = hourlyPointList + nextHour + ";" + nextHourVal +";";
		stockEnt.setProperty("hourlyPointList", hourlyPointList);
	}
	
	public void resetBuysSells(Entity stockEnt) {
		stockEnt.setProperty("buys", 0);
		stockEnt.setProperty("sells", 0);
	}
	
	public void updateDailyPointList(Entity stockEnt, long nextHourVal) {
		String dailyPointList = (String)stockEnt.getProperty("dailyPointList");
		
		String [] dailyPointListArr = dailyPointList.split(";");
		
		long newDay = 0;
		if(dailyPointListArr.length-2 > 0) {
			long oldDay = Long.parseLong(dailyPointListArr[dailyPointListArr.length-2]);
			newDay = oldDay+1;
		}
		
		dailyPointList = dailyPointList + ";" + newDay + ";" +nextHourVal;
		stockEnt.setProperty("dailyPointList", dailyPointList);
	}
	
	public void clearHourlyPointList(Entity stockEnt) {
		stockEnt.setProperty("hourlyPointList", "");
	}
	
	public long computeNextHourVal(long currentHourVal, double trend) {
		return (long) (currentHourVal + trend);
	}
	
	public double computeNextTrend(long buys, long sells, long currentHourVal) {
		long volume = buys+sells;
		long diff = buys-sells;

		// System.out.println("b="+buys+", s="+sells+", c="+currentHourVal);
		
		double trendOut = (diff*Math.log(volume+1)/Math.log(currentHourVal) );
		
		// System.out.println("trend: "+trendOut);
		
		return trendOut;
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
	
	public Entity getConfigEntity(DatastoreService ds) {
		Query q = new Query("Config");
		PreparedQuery pq = ds.prepare(q);
		for(Entity e : pq.asIterable()) {
			return e;
		}
		return null;
	}
	
	public boolean containsShareHolder(String userId, Entity stockEnt) {
		String userIdList = (String) stockEnt.getProperty("shareHolderList");
		String userIdArr[] = userIdList.split(";");
		for(int i=0; i<userIdArr.length; i++) {
			if(userIdArr[i].equals(userId)) {
				return true;
			}
		}
		return false;
	}
	
	public Entity getUserEntity(DatastoreService ds, String id) {
		System.out.println("searching for user "+id);
		Query q = new Query("User");
		PreparedQuery pq = ds.prepare(q);
		for(Entity e : pq.asIterable()) {
			System.out.println("found "+e.getProperty("id"));
			if(e.getProperty("id").equals(id)) {
				return e;
			}
		}
		System.out.println("failed to find "+id);
		return null;
	}
	
}
