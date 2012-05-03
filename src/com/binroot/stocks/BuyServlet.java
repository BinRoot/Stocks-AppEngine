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
 * POST /buy?userId=FB1234&stockId=0&numShares=7&shareVal=3
 *
 */
@SuppressWarnings("serial")
public class BuyServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		String userId = (String) req.getParameter("userId");
		String stockId = (String) req.getParameter("stockId");
		int numShares = Integer.parseInt(req.getParameter("numShares"));
		int shareVal = Integer.parseInt(req.getParameter("shareVal"));
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		
		Entity stockEnt = getStockEntity(ds, Long.parseLong(stockId));
		Entity userEnt = getUserEntity(ds, userId);
		Entity configEnt = getConfigEntity(ds);
		
		
		
		
		long userCredits = (Long) userEnt.getProperty("credits");
		int cost = numShares*shareVal;
		userCredits -= cost;
		if(userCredits>=0) {
			
			// add History Entities 
			long histId = (Long) configEnt.getProperty("histId");
			configEnt.setProperty("histId", histId+1);
			ds.put(configEnt);
			
			Entity histEnt = new Entity("History");
			histEnt.setProperty("histId", histId);
			histEnt.setProperty("userId", userId);
			histEnt.setProperty("stockId", stockId);
			histEnt.setProperty("stockName", stockEnt.getProperty("name"));
			histEnt.setProperty("userName", userEnt.getProperty("name"));
			histEnt.setProperty("numShares", numShares);
			histEnt.setProperty("shareVal", shareVal);
			histEnt.setProperty("type", (int)1);
			histEnt.setProperty("meta", "");
			ds.put(histEnt);
			
			// connect history entity to user
			String userHistList = (String) userEnt.getProperty("historyList");
			userHistList = histId +";"+ userHistList;
			userEnt.setProperty("historyList", userHistList);
			
			// connect history entity to stock
			String stockHistList = (String) stockEnt.getProperty("historyList");
			stockHistList = histId +";"+ stockHistList;
			stockEnt.setProperty("historyList", stockHistList);
			
			// update user credits and stock numShares
			userEnt.setProperty("credits", userCredits);
			String stockList = (String) userEnt.getProperty("stockList");
			String [] stockListArr = stockList.split(";");
			boolean foundStockIdForShares = false;
			for(int i=0; i<stockListArr.length-1; i+=2) {
				String stockIdA = stockListArr[i];
				int stockNumShares = Integer.parseInt(stockListArr[i+1]);
				if(stockIdA.equals(stockId)) {
					stockNumShares += numShares;
					stockListArr[i+1] = stockNumShares+"";
					foundStockIdForShares = true;
					break;
				}
			}
			if(!foundStockIdForShares) {
				stockList += stockId+";"+numShares+";";
			}
			else {
				// convert that array to string with ; and save to Entity
				stockList = "";
				for(int i=0; i<stockListArr.length-1; i+=2) {
					stockList = stockListArr[i] +";"+stockListArr[i+1]+";" + stockList;
				}
			}
			userEnt.setProperty("stockList", stockList);

			// update stock's shareholder list
			if(!containsShareHolder(userId, stockEnt)) { 
				String userIdList = (String) stockEnt.getProperty("shareHolderList");
				stockEnt.setProperty("shareHolderList", userId+";"+userIdList);
			}
			
			
			// update last seen/transaction
			userEnt.setProperty("lastSeen", Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			stockEnt.setProperty("lastTransaction", Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			
			// increment buys
			long buys = (Long)stockEnt.getProperty("buys");
			stockEnt.setProperty("buys", buys+1);
			
			ds.put(stockEnt);
			ds.put(userEnt);
			resp.getWriter().write("0: Success!");
		}
		else {
			resp.getWriter().write("1: Insufficent funds");
		}
		
		
		// compute daily pointlist
		
		
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
