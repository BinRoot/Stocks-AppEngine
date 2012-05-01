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
		//Entity configEnt = getConfigEntity(ds);
		
		String hourlyPointList = (String)stockEnt.getProperty("hourlyPointList");
		String hourlyPointListArr[] = hourlyPointList.split(";");
		for(int i=0; i<hourlyPointListArr.length-1; i+=2) {
			String hour = hourlyPointListArr[0];
			String val = hourlyPointListArr[1];
			System.out.println("("+hour+", "+val+")");
		}
		
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
