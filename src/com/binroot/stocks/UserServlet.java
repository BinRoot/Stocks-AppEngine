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

@SuppressWarnings("serial")
public class UserServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		String id = (String) req.getParameter("id");
		String name = (String) req.getParameter("name");
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity ue = getUserEntity(ds, id);
		
		if(ue == null) {
			// create new user
			Entity e = new Entity("User");
			e.setProperty("id", id);
			e.setProperty("name", name);
			e.setProperty("credits", getConfigEntity(ds).getProperty("startingCredits"));
			e.setProperty("lastSeen", Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
			e.setProperty("stockList", "");
			e.setProperty("followList", "");
			e.setProperty("badges", "");
			e.setProperty("historyList", "");
			
			ds.put(e);
		}
		else {
			Long credits = Long.parseLong(req.getParameter("credits"));
			String stockList = req.getParameter("stockList");
			boolean modified = false;
			if(id!=null) {
				ue.setProperty("id", id);
				modified = true;
			}
			if(name!=null) {
				ue.setProperty("name", name);
				modified = true;
			}
			if(credits!=null) {
				ue.setProperty("credits", credits);
				modified = true;
			}
			if(stockList!=null) {
				ue.setProperty("stockList", stockList);
				modified = true;
			}
			
			if(modified)
				ds.put(ue);
			
			// output credits, stockList, badges
			resp.getWriter().append(ue.getProperty("credits")+", "+ue.getProperty("stockList")+", "+ue.getProperty("historyList"));
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
	
	public Entity getUserEntity(DatastoreService ds, String id) {
		Query q = new Query("User");
		PreparedQuery pq = ds.prepare(q);
		for(Entity e : pq.asIterable()) {
			if(e.getProperty("id").equals(id)) {
				return e;
			}
		}
		return null;
	}
}
