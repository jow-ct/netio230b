package de.jockels.netioswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WlanReceiver extends BroadcastReceiver {
	private final static String TAG = "WlanReceiver";

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.v(TAG, "onReceive");
	}

}
