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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

/**
 * POST /sell?userId=FB1234&stockId=0&numShares=2&shareVal=3
 *
 */
@SuppressWarnings("serial")
public class SellServlet extends HttpServlet {
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
		userCredits += cost;
		if(updateUserShares(userEnt, stockEnt, numShares)) {
			
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
			histEnt.setProperty("type", (int)2);
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
			
			// update user credits
			userEnt.setProperty("credits", userCredits);
			

			// update last seen/transaction
			userEnt.setProperty("lastSeen", Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			stockEnt.setProperty("lastTransaction", Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			
			// increment sells
			long sells = (Long)stockEnt.getProperty("sells");
			stockEnt.setProperty("sells", sells+1);
			
			Key stockKey = ds.put(stockEnt);
			System.out.println("stockKey: "+stockKey.getId());
			ds.put(userEnt);
			resp.getWriter().write("0: Success!");
		}
		else {
			resp.getWriter().write("1: Insufficent shares");
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
	
	public boolean updateUserShares(Entity userEnt, Entity stockEnt, int sharesSelling) {
		String userId = (String) userEnt.getProperty("id");
		long stockId = (Long) stockEnt.getProperty("id");
		String stockList = (String) userEnt.getProperty("stockList");
		String [] stockListArr = stockList.split(";");
		
		System.out.println("in updateUserShares: "+userId+", "+stockId+", "+stockList);
	
		if(stockList.equals("")) {
			return false;
		}
		
		// compute net shares owned after sell
		for(int i=0; i<stockListArr.length-1; i+=2) {
			long stockIdA = Long.parseLong(stockListArr[i]);
			System.out.println(stockId+"=?="+stockIdA+", "+(stockId==stockIdA));
			if(stockId==stockIdA) {
				int sharesOwned = Integer.parseInt(stockListArr[i+1]);
				int netSharesOwned = sharesOwned - sharesSelling;
				
				System.out.println("netSharesOwned: "+netSharesOwned);
				
				if(netSharesOwned<0) {
					return false;
				}
				else {
					stockListArr[i+1] = netSharesOwned+"";
					break;
				}
			}
		}
		
		// update newly built stockList
		stockList = "";
		for(int i=0; i<stockListArr.length-1; i+=2) {
			
			System.out.println(Long.parseLong(stockListArr[i+1])+"=??="+0+", "+(Long.parseLong(stockListArr[i+1])==0));
			
			if(Long.parseLong(stockListArr[i+1])==0) {
				
				// remove user from stocklist
				String shareHolderList = (String) stockEnt.getProperty("shareHolderList");
				String []shareHolderListArr = shareHolderList.split(";");
				String shareHolderListOut = "";
				for(int j=0; j<shareHolderListArr.length; j++) {
					String userIdA = (shareHolderListArr[j]);
					System.out.println(userIdA+"=???="+userId+", "+(userIdA.equals(userId)));
					if(userIdA.equals(userId)) {
						continue;
					}
					shareHolderListOut = userIdA+";"+shareHolderListOut;
				}
				
				stockEnt.setProperty("shareHolderList", shareHolderListOut);
				
				System.out.println("created new shareHolderList: "+shareHolderListOut);
				
				continue;
			}
			stockList = stockListArr[i] +";"+stockListArr[i+1]+";" + stockList;
		}
		
		System.out.println("created new stockList: "+stockList);
		
		userEnt.setProperty("stockList", stockList); // THIS MIGHT BE THE PROBLEM!
		
		return true;
				
	}
}
