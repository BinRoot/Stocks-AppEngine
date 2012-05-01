package com.binroot.stocks;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

/**
 * POST /config?op=setup&si=0&sc=1000&histId=0
 *
 */
@SuppressWarnings("serial")
public class ConfigServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		doPost(req, resp);
	}
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		String operation = (String) req.getParameter("op");
		String stockId = (String) req.getParameter("si");
		String startingCredits = (String) req.getParameter("sc");
		String histId = (String) req.getParameter("histId");
		
		
		
		if(operation.equals("setup")) {
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			
			Entity ce = getConfigEntity(ds);
			if(ce==null) 
				ce = new Entity("Config");
			
			
			if(stockId!=null)
				ce.setProperty("stockId", Integer.parseInt(stockId));
			else ce.setProperty("stockId", 0);
			
			if(startingCredits!=null)
				ce.setProperty("startingCredits", Integer.parseInt(startingCredits));
			else ce.setProperty("startingCredits", 1000);
			
			if(histId!=null)
				ce.setProperty("histId", Integer.parseInt(histId));
			else ce.setProperty("histId", 0);
			
			ds.put(ce);
			

		}
		else if(operation.equals("reset")) {
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			Query q = new Query("Stock");
			PreparedQuery pq = ds.prepare(q);
			for(Entity e : pq.asIterable()) {
				ds.delete(e.getKey());
			}
			Query q2 = new Query("History");
			PreparedQuery pq2 = ds.prepare(q2);
			for(Entity e : pq2.asIterable()) {
				ds.delete(e.getKey());
			}
			Entity newConfig = getConfigEntity(ds);
			newConfig.setProperty("stockId", 0);
			newConfig.setProperty("histId", 0);
			ds.put(newConfig);
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
}
