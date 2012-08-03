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
		Cursor c = db.queryEvents(null, null); // TODO null,"active"
		if (c.moveToFirst()) do {
			mList.add(new Event(c));
		} while (c.moveToNext());
		Log.v(TAG, mList.size()+" Events erzeugt");
		c.close();
		db.close();
	}
	
}
