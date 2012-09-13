package de.jockels.netioswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import de.jockels.forms.Content;
import de.jockels.forms.ContentHelper;

/**
 * 
 * @author Jockel
 *
 * TODO zu implementierende Broadcasts:
 * - Intent.ACTION_AIRPLANE_MODE_CHANGED
 * - Intent.ACTION_BATTERY_LOW;
 * - Intent.ACTION_BATTERY_OKAY;
 * - Intent.ACTION_BOOT_COMPLETED;
 * - Intent.ACTION_DOCK_EVENT;
 * - Intent.ACTION_HEADSET_PLUG;
 * - Intent.ACTION_POWER_CONNECTED;
 * - Intent.ACTION_POWER_DISCONNECTED;
 * - Intent.ACTION_SCREEN_OFF;
 * - Intent.ACTION_SCREEN_ON;
 * - Intent.ACTION_SHUTDOWN;
 * - Intent.ACTION_TIME_TICK;
 * 
 * TODO f�r WLAN:
 * file:///C:/Android/sdk/docs/training/monitoring-device-state/connectivity-monitoring.html
 * 
 */

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

	private Context mCtx;
	private boolean mRunning = false;
	private Ticktack mRunnable = null;
	private Connection mParameter = null;
	private BroadcastReceiver mReceiver = null;
	
	/**
	 * ein leerer Event
	 */
	public Event() {
		super(_SUM);
		s[NAME] = s[EXT1] = s[EXT2] = s[OUT] = "";
	}
	
	
	/**
	 * Get-Helfer
	 */
	public int getExt1Int(int def) {
		try { return Integer.parseInt(s[EXT1]);} 
		catch (NumberFormatException e) { return def; }
	}

	public int getExt2Int(int def) {
		try { return Integer.parseInt(s[EXT2]);} 
		catch (NumberFormatException e) { return def; }
	}
	
	
	/**
	 * Parameterverwaltung -- provisorisch
	 */
	public void setParameter(Connection p) {
		mParameter = p;
	}


	/**
	 * Validierung
	 */
	@Override
	public boolean isValid(int id) {
		switch (id) {
		
		// Name ist g�ltig, wenn er nicht leer ist
		case NAME: return !TextUtils.isEmpty(s[NAME]);
		
		// Output ist g�ltig, wenn er 4 Zeichen lang ist und nur 1, 0, i, u
		case OUT:
			String o = s[OUT];
			if (TextUtils.isEmpty(o) || o.length() != 4) return false;
			for (int i=0; i<o.length(); i++) {
				char c = o.charAt(i);
				if (c!='1' && c!='0' && c!='i' && c!='u') return false;
			}
			return true;
			
		// Type ist g�ltig, wenn im vorgegebenen Rahmen
		case TYPE: return i[TYPE] < EventDb.NAMEN.length;
		
		// EXT1 ist g�ltig, wenn nicht leer oder wenn leer erlaubt
		case EXT1:
			return isValid(TYPE) && ( !TextUtils.isEmpty(s[EXT1]) || EventDb.UNUSED.equals(EventDb.NAMEN[i[TYPE]][0]));
			
		// EXT2 ist g�ltig, wenn nicht leer oder wenn leer erlaubt
		case EXT2:
			return isValid(TYPE) && ( !TextUtils.isEmpty(s[EXT2]) || EventDb.UNUSED.equals(EventDb.NAMEN[i[TYPE]][1]));
		
		default: return true;
		}
	}

	
	/**
	 * l�uft der Event?
	 * @return
	 */
	public boolean isRunning() { return mRunning; }
	
	
	private void fireEvent() {
		Intent i = new Intent(mCtx, CommService.class);
		i.setAction(CommService.ACTION_SET);
		i.putExtra(CommService.EXTRA_OUT, s[OUT]);
		i.putExtra(CommService.EXTRA_CONNECTION, 0); // TODO mehrere Steckdosen
//		mCtx.startService(i);
	}
	
	
	/**
	 * Broadcast-Receiver oder �hnliches starten. Sollte dann mit stop() wieder angehalten werden.
	 */
	public void start(Context aCtx) {
		if (i[ACTIVE]==0) return;
		Log.v(TAG, "Start Event "+s[NAME]);
		mCtx = aCtx;
		switch (i[TYPE]) {

		case EventDb.TYP_WLAN_BETRETEN: 
		case EventDb.TYP_WLAN_VERLASSEN:
			IntentFilter ifl = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.v(TAG, "Intent "+intent);
					fireEvent(); 
				}
			};
			Intent sticky = mCtx.registerReceiver(mReceiver, ifl);
			if (sticky!=null) Log.v(TAG, "sticky: "+sticky);
			break;

		case EventDb.TYP_SECONDS:
			int t = getExt1Int(0);
			if (t>0) mRunnable = new Ticktack(t);
			break;

		default:
			Log.e(TAG, "Eventtyp nicht implementiert: "+i[TYPE]);
		}
		mRunning = true;
	}

	
	/**
	 * Broadcast-Receiver oder �hnliches wieder stoppen
	 */
	public void stop() {
		if (!mRunning) return;
		Log.v(TAG, "Stop Event "+s[NAME]);
		switch (i[TYPE]) {

		case EventDb.TYP_WLAN_BETRETEN: 
		case EventDb.TYP_WLAN_VERLASSEN:
			if (mReceiver!=null)	 mCtx.unregisterReceiver(mReceiver);
			break;

		case EventDb.TYP_SECONDS:
			if (mRunnable!=null) mRunnable.stop();
			break;

		default:
			Log.e(TAG, "Eventtyp nicht implementiert: "+i[TYPE]);
		}
		mReceiver = null;
		mRunnable = null;
		mRunning = false;
	}

	
	private class Ticktack implements Runnable {
		int mTime;
		Handler mHandler;
		Ticktack(int t) {
			mTime = t*1000;
			mHandler = new Handler();
			mHandler.postDelayed(this, mTime);
		}
		
		void stop() {
			Log.v(TAG, "Runnable-Stop");
			mHandler.removeCallbacks(this);
			mHandler = null;
		}

		public void run() {
			Log.v(TAG, "Event "+s[NAME]);
			fireEvent();
			if (mHandler!=null) mHandler.postDelayed(this, mTime);
		}
		
	}
}
