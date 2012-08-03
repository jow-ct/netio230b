package de.jockels.netioswitch;

import android.database.Cursor;
import android.util.Log;

public class Event {
	private final static String TAG = "Event";
	private String name, ext1, ext2, out;
	private boolean active;
	private int type;
	
	public Event(Cursor c) {
		name = c.getString(c.getColumnIndex(EventDb.NAME));
		active = c.getInt(c.getColumnIndex(EventDb.ACTIVE))>0;
		type = c.getInt(c.getColumnIndex(EventDb.TYPE));
		ext1 = c.getString(c.getColumnIndex(EventDb.EXT1));
		ext2 = c.getString(c.getColumnIndex(EventDb.EXT2));
		out = c.getString(c.getColumnIndex(EventDb.OUTPUT));

		if (active) {
			switch (type) {
			case EventDb.TYP_WLAN_BETRETEN: case EventDb.TYP_WLAN_VERLASSEN:
				// TODO
				break;
			default:
				Log.v(TAG, "Eventtyp nicht implementiert: "+type);
			}
		}
	}

	// TODO BroadcastReceiver erzeugen
	// TODO in onReceive muss der einen Service mit StartService lostreten

	public void stop() {
		// TODO
	}

}
