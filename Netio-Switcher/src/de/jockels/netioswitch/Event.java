package de.jockels.netioswitch;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import de.jockels.forms.Content;
import de.jockels.forms.ContentHelper;

public class Event extends Content {
	private final static String TAG = "Event";
	
	public static final int NAME = 0;
	public static final int EXT1 = 1;
	public static final int EXT2 = 2;
	public static final int OUT = 3;
	public static final int ACTIVE = 4;
	public static final int TYPE = 5;
	public static final int _SUM = 6;
	
	public static ContentHelper<Event> eventHelper = new ContentHelper<Event>(_SUM, Event.class)
		.addString(EventDb.NAME)
		.addString(EventDb.EXT1)
		.addString(EventDb.EXT2)
		.addString(EventDb.OUTPUT)
		.addBoolean(EventDb.ACTIVE)
		.addInt(EventDb.TYPE);

	private Handler aHandler = null;
	private Runnable aRunnable = null;
	
	/**
	 * ein leerer Event
	 */
	public Event() {
		super(_SUM);
		s[NAME] = s[EXT1] = s[EXT2] = s[OUT] = "";
	}
	
	
	/**
	 * Validierung
	 */
	@Override
	public boolean isValid(int id) {
		switch (id) {
		
		// Name ist gültig, wenn er nicht leer ist
		case NAME: return !TextUtils.isEmpty(s[NAME]);
		
		// Output ist gültig, wenn er 4 Zeichen lang ist und nur 1, 0, i, u
		case OUT:
			String o = s[OUT];
			if (TextUtils.isEmpty(o) || o.length() != 4) return false;
			for (int i=0; i<o.length(); i++) {
				char c = o.charAt(i);
				if (c!='1' && c!='0' && c!='i' && c!='u') return false;
			}
			return true;
			
		// Type ist gültig, wenn im vorgegebenen Rahmen
		case TYPE: return i[TYPE] < EventDb.NAMEN.length;
		
		// EXT1 ist gültig, wenn nicht leer oder wenn leer erlaubt
		case EXT1:
			return isValid(TYPE) && ( !TextUtils.isEmpty(s[EXT1]) || EventDb.UNUSED.equals(EventDb.NAMEN[i[TYPE]][0]));
			
		// EXT2 ist gültig, wenn nicht leer oder wenn leer erlaubt
		case EXT2:
			return isValid(TYPE) && ( !TextUtils.isEmpty(s[EXT2]) || EventDb.UNUSED.equals(EventDb.NAMEN[i[TYPE]][1]));
		
		default: return true;
		}
	}

	
	/**
	 * Broadcast-Receiver oder ähnliches starten. Sollte dann mit stop() wieder angehalten werden.
	 */
	public void start() {
		Log.v(TAG, "Start Event "+s[NAME]);
		if (i[ACTIVE]>0) {
			switch (i[TYPE]) {
			
			case EventDb.TYP_WLAN_BETRETEN: 
			case EventDb.TYP_WLAN_VERLASSEN:
				// TODO
				break;
				
			case EventDb.TYP_5S:
				aHandler = new Handler();
				aRunnable = new Runnable() {
					public void run() {
						Log.v(TAG, "ticktack");
						if (aHandler!=null) aHandler.postDelayed(aRunnable, 10000);
					}
				};
				aHandler.postDelayed(aRunnable, 10000);
				break;
			default:
				Log.v(TAG, "Eventtyp nicht implementiert: "+i[TYPE]);
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
