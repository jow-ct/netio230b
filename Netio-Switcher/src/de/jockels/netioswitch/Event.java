package de.jockels.netioswitch;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

public class Event {
	private final static String TAG = "Event";
	String name, ext1, ext2, out;
	boolean active;
	int type;
	
	
	/**
	 * ein leerer Event
	 */
	public Event() {
		name = ext1 = ext2 = out = ""; // nicht null vereinfacht ein paar Tests
		active = true;
		type = 0;
	}
	
	
	/**
	 * Event aus einem Cursor auslesen
	 * @param c
	 */
	public Event(Cursor c) {
		name = c.getString(c.getColumnIndex(EventDb.NAME));
		active = c.getInt(c.getColumnIndex(EventDb.ACTIVE))>0;
		type = c.getInt(c.getColumnIndex(EventDb.TYPE));
		ext1 = c.getString(c.getColumnIndex(EventDb.EXT1));
		ext2 = c.getString(c.getColumnIndex(EventDb.EXT2));
		out = c.getString(c.getColumnIndex(EventDb.OUTPUT));
	}
	

	/**
	 * Gegenstück zum Cursor-Constructor: Inhalt in einen ContentValues (für Datenbanken) packen
	 * @return neu erzeugtes ContentValues
	 */
	ContentValues createContentValues() {
		ContentValues v = new ContentValues();
		v.put(EventDb.NAME, name);
		v.put(EventDb.ACTIVE, active);
		v.put(EventDb.TYPE, type);
		v.put(EventDb.EXT1, ext1);
		v.put(EventDb.EXT2, ext2);
		v.put(EventDb.OUTPUT, out);
		return v;
	}


	/**
	 * Vergleich mit einem anderen Event; v.a. für Edit-Funktion wichtig
	 * @param c
	 * @return
	 */
	boolean equals(Event c) {
		boolean equal = TextUtils.equals(name, c.name) && TextUtils.equals(ext1, c.ext1) && TextUtils.equals(ext2, c.ext2)
				&& TextUtils.equals(out, c.out) && active==c.active && type == c.type;
		return equal;
	}
	
	
	/**
	 * Validierung
	 */
	public static boolean isNameValid(String name) {
		return !TextUtils.isEmpty(name);
	}
	
	public static boolean isOutValid(String out) {
		if (out==null || out.length()!=4) return false;
		for (int i=0; i<out.length(); i++) {
			char c = out.charAt(i);
			if (c!='1' && c!='0' && c!='i' && c!='u') return false;
		}
		return true;
	}
	
	public static boolean isTypeValid(int type) {
		return type < EventDb.NAMEN.length;
	}
	
	public static boolean isExt1Valid(String ext1, int type) {
		return isTypeValid(type) && ( !TextUtils.isEmpty(ext1) || EventDb.UNUSED.equals(EventDb.NAMEN[type][0]));
	}
	
	public static boolean isExt2Valid(String ext2, int type) {
		return isTypeValid(type) && ( !TextUtils.isEmpty(ext2) || EventDb.UNUSED.equals(EventDb.NAMEN[type][1]));
	}
	
	public boolean isValid() {
		return isNameValid(name) && isOutValid(out) && isTypeValid(type) && isExt1Valid(ext1, type) && isExt2Valid(ext2, type);
	}
	
	
	/**
	 * Broadcast-Receiver oder ähnliches starten. Sollte dann mit stop() wieder angehalten werden.
	 */
	public void start() {
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

	
	/**
	 * Broadcast-Receiver oder ähnliches wieder stoppen
	 */
	public void stop() {
		// TODO
	}

}
