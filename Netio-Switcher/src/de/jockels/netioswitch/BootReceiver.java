package de.jockels.netioswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	private final static String TAG = "BootReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive");
		// Workaround für AsyncTask-Bug
		try { Class.forName("android.os.AsyncTask"); } catch (ClassNotFoundException e) { }

		ConnectionList.initConnections(context.getApplicationContext());
		EventList.onCreate(context.getApplicationContext());
		EventList.loadEvents();
		EventList.startEvents(false);
	}

}
