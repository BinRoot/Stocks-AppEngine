package com.binroot.stocks;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;

/**
 * POST /leaderboard?id=FB1234
 * Server reply: 
 */
@SuppressWarnings("serial")
public class LeaderboardServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		String userId = req.getParameter("userId");
		String stockId = req.getParameter("stockId");
		Gson gs = new Gson();

		StringWriter sw = new StringWriter();
		JsonWriter jw = new JsonWriter(sw);
		
		
		if(userId != null) {
			// list id's friends
			Entity userEnt = getUserEntity(ds, userId);
			String followList = (String) userEnt.getProperty("followList");
			String followListArr[] = followList.split(";");
			
			jw.beginObject().name("data");
			jw.beginArray();
			
			for(int i=0; i<followListArr.length; i++) {
				String userIdA = followListArr[i];
				System.out.println("friend: "+userIdA);
				Entity e = getUserEntity(ds, userIdA);
				if(e==null) {
					continue;
				}
				long credits = (Long) e.getProperty("credits");
				// compute net worth from e
				String name = (String) e.getProperty("name");
				long netWorth = getNetWorth(e, ds) + credits;
				String targetId = userIdA;

				// [targetId] [name] [netWorth]
				jw.beginObject();
				jw.name("id").value(targetId);
				jw.name("name").value(name);
				jw.name("net").value(netWorth);
				jw.endObject();
				
			}
			jw.endArray();
			jw.endObject();
			jw.close();
			
			resp.getWriter().write(sw.toString());
		}
		else if(stockId != null) {
			// list stock's shareholders with num shares
			Entity stockEnt = getStockEntity(ds, Long.parseLong(stockId));
			String shareHolderList = (String)stockEnt.getProperty("shareHolderList");
			String [] shareHolderListArr = shareHolderList.split(";");
			
			jw.beginObject().name("data");
			jw.beginArray();
			
			for(int i=0; i<shareHolderListArr.length; i++) {
				String shareHolderA = shareHolderListArr[i];
				Entity userEnt = getUserEntity(ds, shareHolderA);
				String stockList = (String) userEnt.getProperty("stockList");
				String [] stockListArr = stockList.split(";");
				for(int j=0; j<stockListArr.length-1; j+=2) {
					long stockIdA = Long.parseLong(stockListArr[j]);
					if(stockIdA == Long.parseLong(stockId)) {
						int numShares = Integer.parseInt(stockListArr[j+1]);
						jw.beginObject();
						jw.name("userId").value(shareHolderA);
						jw.name("name").value((String) userEnt.getProperty("name"));
						jw.name("numShares").value(numShares);
						jw.endObject();
						break;
					}
				}
			}
			jw.endArray();
			jw.endObject();
			jw.close();
			
			resp.getWriter().write(sw.toString());
		}
		else {
			
			// list global leaderboard
			Query q = new Query("User");
			PreparedQuery pq = ds.prepare(q);
			
			jw.beginObject().name("data");
			jw.beginArray();
			
			for(Entity e: pq.asIterable()) {
				
				/*
				String stockList = (String) e.getProperty("stockList");
				String [] stockListArr = stockList.split(";");
				
				if(stockId != null) {
					boolean found = false;
					for(int i=0; i<stockListArr.length; i++) {
						long stockIdA = Long.parseLong(stockListArr[i]);
						if(stockIdA==Long.parseLong(stockId)) {
							found = true;
						}
					}
					if(!found) {
						continue;
					}
				}
				*/
				
				// compute net worth from e
				long credits = (Long) e.getProperty("credits");
				String name = (String) e.getProperty("name");
				long netWorth = getNetWorth(e, ds) + credits;
				String targetId = (String) e.getProperty("id");

				// [targetId] [name] [netWorth]
				jw.beginObject();
				jw.name("id").value(targetId);
				jw.name("name").value(name);
				jw.name("net").value(netWorth);
				jw.endObject();
				
			}
			
			jw.endArray();
			jw.endObject();
			jw.close();
			
			
			
			resp.getWriter().write(sw.toString());
		}

	}

	long getNetWorth(Entity friendEnt, DatastoreService ds) {
		String stockList = (String) friendEnt.getProperty("stockList");
		String [] stockListArr = stockList.split(";");
		long networth = 0;
		for(int i=0; i<stockListArr.length-1; i+=2) {
			String stockIdA = stockListArr[i];
			int stockNumShares = Integer.parseInt(stockListArr[i+1]);
			networth += getNetStockWorth(ds, Long.parseLong(stockIdA), stockNumShares);
		}
		return networth;
	}

	public long getNetStockWorth(DatastoreService ds, long stockIdA, int stockNumShares) {
		Entity friendStockEnt = getStockEntity(ds, stockIdA);
		long currentHourVal = (Long) friendStockEnt.getProperty("currentHourVal");
		return currentHourVal*stockNumShares;

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


