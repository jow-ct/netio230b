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
	public final static int refreshEvents(EventDb db, Connection p) {
		// alte Liste löschen
		if (mList!=null) stopEvents();
		
		// neue einlesen
		mList = new ArrayList<Event>();
		Cursor c = db.queryEvents(null, EventDb.ACTIVE+"=1"); 
		if (c.moveToFirst()) do {
			mList.add(Event.eventHelper.createFromCursor(c));
		} while (c.moveToNext());
		Log.v(TAG, mList.size()+" Events erzeugt");
		c.close();
		return mList.size();
	}


	public final static int startEvents(Context ctx) {
		int s = 0;
		for (Event e : mList) {
			if (e.error()==0) {
				e.start(ctx);
				s++;
			} else {
				e.i[Event.ACTIVE] = 0;
			}
		}
		Log.i(TAG, s+" Events gestartet.");
		return s;
	}
	
	
	public final static int stopEvents() {
		int s=0;
		for (Event e : mList) 
			if (e.isRunning()) {
				e.stop();
				s++;
			}
		return s;
	}

	
	public final static int pauseEvents() {
		int s=0;
		for (Event e : mList) 
			if (e.isRunning()) {
				e.pause();
				s++;
			}
		return s;
	}
	
}
