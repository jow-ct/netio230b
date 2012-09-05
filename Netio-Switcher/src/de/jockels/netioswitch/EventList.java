package de.jockels.netioswitch;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class EventList {
	private static final String TAG = "EventList";
	private static ArrayList<Event> mList = null;
	
	/**
	 * von außen: EventList erzeugen/refreshen
	 */
	public final static void refresh(Context ctx) {
		// alte Liste löschen
		if (mList!=null) {
			for (Event i : mList) {i.stop();}
			mList = null;
		}
		
		// neue einlesen
		mList = new ArrayList<Event>();
		EventDb db = new EventDb(ctx);
		db.open();
		Cursor c = db.queryEvents(null, EventDb.ACTIVE+"=1"); 
		if (c.moveToFirst()) do {
			mList.add( Event.eventHelper.createFromCursor(c) );
		} while (c.moveToNext());
		Log.v(TAG, mList.size()+" Events erzeugt");
		c.close();
		db.close();
		
		int s = 0;
		for (Event e : mList) {
			if (e.isValid()) {
				e.start();
				s++;
			} else {
				e.i[Event.ACTIVE] = 0;
				// TODO update
			}
		}
		Log.i(TAG, s+" Events gestartet.");
	}
	
	
	public static void startEvent(int id) {
	}
	
	
	public static void stopEvent(int id) {
		
	}
}
