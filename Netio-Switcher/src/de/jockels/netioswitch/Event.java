package de.jockels.netioswitch;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import de.jockels.forms.Content;
import de.jockels.forms.ContentHelper;
import de.jockels.lib.StringTools;

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
	public long getExt1Long(long def) { return StringTools.tryParseLong(s[EXT1], def); }
	public long getExt2Long(long def) { return StringTools.tryParseLong(s[EXT2], def); }
	
	
	/**
	 * Validierung
	 */
	@Override
	public int error(int id) {
		switch (id) {
		
		// Name ist gültig, wenn er nicht leer ist
		case NAME: return TextUtils.isEmpty(s[NAME]) ? R.string.eventerror_name : 0;
		
		// Output ist gültig, wenn er 4 Zeichen lang ist und nur 1, 0, i, u
		case OUT:
			String o = s[OUT];
			if (TextUtils.isEmpty(o) || o.length() != 4) return R.string.eventerror_out1;
			for (int i=0; i<o.length(); i++) {
				char c = o.charAt(i);
				if (c!='1' && c!='0' && c!='i' && c!='u') return R.string.eventerror_out2;
			}
			return 0;
			
		// Type ist gültig, wenn im vorgegebenen Rahmen
		case TYPE: return l[TYPE] >= 0 && l[TYPE] < EventDb.MAX_TYPE ? 0 : R.string.eventerror_typ;
		
		// EXT1 ist gültig, wenn nicht leer oder wenn leer erlaubt
		case EXT1:
			if (error(TYPE) != 0) return R.string.eventerror_typ;
			if (TextUtils.isEmpty(s[EXT1]) && !EventDb.UNUSED.equals(EventDb.NAMEN[getInt(TYPE)][0])) return R.string.eventerror_ext11;
			// sonstige typabhängige Überprüfungen
			switch (getInt(TYPE)) {
			case EventDb.TYP_TIME: return getExt1Long(0)>0 ? 0 : R.string.eventerror_ext12; // nur Zahlen ab 1 erlaubt
			}
			return 0;
			
		// EXT2 ist gültig, wenn nicht leer oder wenn leer erlaubt
		case EXT2:
			if (error(TYPE) != 0) return R.string.eventerror_typ;
			if (TextUtils.isEmpty(s[EXT2]) && !EventDb.UNUSED.equals(EventDb.NAMEN[getInt(TYPE)][1])) return R.string.eventerror_ext21;
			// sonstige typabhängige Überprüfungen
			return 0;
		
		default: return 0;
		}
	}

	
	/**
	 * Feuert den Event ab. Sollte nur von den BroadcastReceivern aufgerufen werden,
	 * aber z.B. zu Testzwecken auch sonst möglich
	 */
	public void fireEvent(Context ctx) {
		Intent i = new Intent(ctx, CommService.class);
		i.setAction(CommService.ACTION_SETALL);
		i.putExtra(CommService.EXTRA_CONNECTION, 0); 
		i.putExtra(CommService.EXTRA_OUT, s[OUT]);
		ctx.startService(i);
	}
	
}
