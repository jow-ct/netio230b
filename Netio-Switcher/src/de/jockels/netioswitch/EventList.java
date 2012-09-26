package de.jockels.netioswitch;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class EventList {
	private static final String TAG = "EventList";
	private static final boolean DEBUG = true;
	private static ArrayList<Event> mList = new ArrayList<Event>(20);
	private static boolean[] mTypes = new boolean[EventDb.MAX_TYPE];
	private static boolean mActive = true;
	private static Context mCtx;
	private static ConnectivityManager mCm = null;
	private static WifiManager mWm = null;
	private static PackageManager mPm = null;
	private static String currentWLAN = null;
	private static int currentType;
	private static BroadcastReceiver mTimer = null;

	/**
	 * 
	 */
	public final static void onCreate(Context ctx) {
		mCtx = ctx;
		mCm = (ConnectivityManager)mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
		mWm = (WifiManager)mCtx.getSystemService(Context.WIFI_SERVICE);
		mPm = mCtx.getPackageManager();
	}
	
	
	/**
	 * von außen: EventList erzeugen/refreshen
	 */
	public final static void loadEvents() {
		// alte Liste löschen
		mList.clear();
		for (int i=0; i<mTypes.length; i++) mTypes[i] = false;
		// neue Liste einlesen
		EventDb db = new EventDb(mCtx);
		db.open();
		Cursor c = db.queryEvents(null, EventDb.ACTIVE+"=1"); 
		if (c.moveToFirst()) do {
			Event e = Event.eventHelper.createFromCursor(c);
			// nochmal überprüfen und ggf. inaktiv setzen
			if (e.error()!=0) e.setBoolean(Event.ACTIVE, false);
			// für die Broadcast-Aktivierungen die Typen zählen
			if (e.getBoolean(Event.ACTIVE)) mTypes[e.getInt(Event.TYPE)] = true;
			mList.add(e);
		} while (c.moveToNext());
		if (DEBUG) Log.v(TAG, mList.size()+" Events erzeugt");
		c.close();
		db.close();
	}
	
	
	public final static boolean isActive() { return mActive; }
	public final static int getEventCount() { return mList.size(); }

	
	public final static void switchBoot(boolean boot) {
		mPm.setComponentEnabledSetting(
				new ComponentName(mCtx, BootReceiver.class), 
				(boot ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
				PackageManager.DONT_KILL_APP);
	}
	
	
	public final static void switchEvents(boolean einaus, boolean anzeige) {
		if (einaus) startEvents(anzeige); else stopEvents(anzeige);
	}
	
	
	public final static void startEvents(boolean anzeige) {
		if (DEBUG) Log.v(TAG, "startEvents");
		mActive = true;
		
		// WLAN-Receiver ein- oder ausschalten
		mPm.setComponentEnabledSetting(
				new ComponentName(mCtx, WlanReceiver.class), 
				(mTypes[EventDb.TYP_WLAN_BETRETEN] || mTypes[EventDb.TYP_WLAN_VERLASSEN]
					? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
					: PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
				PackageManager.DONT_KILL_APP);
		// Timer-Receiver aktivieren
		if (mTypes[EventDb.TYP_TIME]) {
			mTimer = new TimerReceiver();
			mCtx.registerReceiver(mTimer, new IntentFilter(Intent.ACTION_TIME_TICK));
		} else {
			if (mTimer!=null) mCtx.unregisterReceiver(mTimer);
			mTimer = null;
		}
		// WLAN-Name etc. merken
		initNetwork();
		// Anzeige
		if (anzeige) Toast.makeText(mCtx, mList.size()+" Events gestartet", Toast.LENGTH_SHORT).show();
	}

	
	public final static void stopEvents(boolean anzeige) {
		if (DEBUG) Log.v(TAG, "stopEvents");
		mActive = false;
		
		// Receiver ausschalten
		mPm.setComponentEnabledSetting(
				new ComponentName(mCtx, WlanReceiver.class), 
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
				PackageManager.DONT_KILL_APP);
		if (mTimer!=null) mCtx.unregisterReceiver(mTimer);
		mTimer = null;

		if (anzeige) Toast.makeText(mCtx, mList.size()+" Events gestoppt", Toast.LENGTH_SHORT).show();
	}
	

	public final static void initNetwork() {
		currentType = mCm.getActiveNetworkInfo().getType();
		if (currentType==ConnectivityManager.TYPE_WIFI) {
			currentWLAN = mWm.getConnectionInfo().getSSID();
			if (DEBUG) Log.v(TAG, "im WLAN "+currentWLAN);
		} else
			currentWLAN = null;
	}
	
	
	public final static String getCurrentWLAN() {
		return currentWLAN;
	}
	
	
	public final static void checkNetwork() {
		NetworkInfo net = mCm.getActiveNetworkInfo();
		if (net==null) {
			// keine Verbindung
			if (DEBUG) Log.v(TAG, "checkNetwork: keine Verbindung");
			if (currentWLAN!=null) checkLeaveWLAN();
			currentType = 0;
			currentWLAN = null;
		} else if (net.getType()==ConnectivityManager.TYPE_WIFI) {
			// WLAN betreten
			String newWLAN = mWm.getConnectionInfo().getSSID();
			if (DEBUG) Log.v(TAG, "checkNetwork: WLAN "+newWLAN);
			if (!TextUtils.equals(currentWLAN, newWLAN)) {
				if (currentWLAN!=null) checkLeaveWLAN();
				currentWLAN = newWLAN;
				checkEnterWLAN();
			}
			currentType = net.getType();
		} else {
			// andere Verbindung
			if (DEBUG) Log.v(TAG, "checkNetwork: "+net.getTypeName());
			if (currentWLAN!=null) checkLeaveWLAN();
			currentWLAN = null;
			currentType = net.getType();
		}
	}
	
	
	private final static void checkEnterWLAN() {
		if (DEBUG) Log.v(TAG, "WLAN betreten: "+currentWLAN);
		for (Event e : mList) {
			if (e.getBoolean(Event.ACTIVE)
					&& (e.getInt(Event.TYPE) == EventDb.TYP_WLAN_BETRETEN)
					&& (TextUtils.equals(e.getString(Event.EXT1), currentWLAN))) {
				if (DEBUG) Log.v(TAG, "Event "+e.getString(Event.NAME)+": "+e.getString(Event.OUT));
				e.fireEvent(mCtx);
			}
		}
	}
	
	
	private final static void checkLeaveWLAN() {
		if (DEBUG) Log.v(TAG, "WLAN verlassen: "+currentWLAN);
		for (Event e : mList) {
			if (e.getBoolean(Event.ACTIVE)
					&& (e.getLong(Event.TYPE) == EventDb.TYP_WLAN_VERLASSEN)
					&& (TextUtils.equals(e.getString(Event.EXT1), currentWLAN))) {
				if (DEBUG) Log.v(TAG, "Event "+e.getString(Event.NAME)+": "+e.getString(Event.OUT));
				e.fireEvent(mCtx);
			}
		}
	}
	
	
	public final static void checkTime() {
		if (DEBUG) Log.v(TAG, "checkTime");
		long now = System.currentTimeMillis();
		for (Event e : mList) {
			if (e.getBoolean(Event.ACTIVE) && (e.getLong(Event.TYPE) == EventDb.TYP_TIME)) {
				long last = e.getLong(Event.NAME);
				long diff = e.getExt1Long(1) * 60 * 900; // 900 statt 1000 wg. Ungenauigkeit des TICK
				Log.v(TAG, e.getString(Event.NAME)+": "+now+" > "+(last+diff));
				if (now > last+diff) {
					if (DEBUG) Log.v(TAG, "Event "+e.getString(Event.NAME)+": "+e.getString(Event.OUT));
					e.setLong(Event.NAME, now);
					e.fireEvent(mCtx);
				}
			}
		}
	}
}
