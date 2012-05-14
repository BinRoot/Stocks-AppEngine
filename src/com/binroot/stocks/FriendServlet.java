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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * POST /friend?op=r&?id=FB1234
 * POST /friend?op=c&?id=FB1234&?target=FB4321
 * POST /friend?op=d&?id=FB1234&?target=FB4321
 * Server reply: 
 */
@SuppressWarnings("serial")
public class FriendServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		String op = (String) req.getParameter("op");
		String userId = (String) req.getParameter("id");
		Entity userEnt = getUserEntity(ds, userId);
		String followList = (String) userEnt.getProperty("followList");
		
		
		if(op.equalsIgnoreCase("r")) {
			// not used, since LeaderboardServlet already does more
		}
		else if(op.equalsIgnoreCase("c")) {
			
			// TODO: do not allow duplicates!!
			
			String target = req.getParameter("target");
			
			boolean foundUser = false;
			String followListArr[] = followList.split(";");
			
			for(int i=0; i<followListArr.length; i++) {
				String userIdA = followListArr[i];
				
				if(userIdA.equals(target)) {
					foundUser = true;
					break;
				}
			}
			
			if(!foundUser) {
				followList = target + ";" + followList;
				userEnt.setProperty("followList", followList);
			}
			
			
		}
		else if(op.equalsIgnoreCase("d")) {
			String target = req.getParameter("target");

			String followListArr[] = followList.split(";");
			String newFollowList = "";
			for(int i=0; i<followListArr.length; i++) {
				String userIdA = followListArr[i];
				if(userIdA.equals("")) {
					continue;
				}
				if(userIdA.equals(target)) {
					continue;
				}
				newFollowList = userIdA+";"+newFollowList;
			}
			userEnt.setProperty("followList", newFollowList);
		}
		
		ds.put(userEnt);
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


