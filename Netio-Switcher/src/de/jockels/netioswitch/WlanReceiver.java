package de.jockels.netioswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WlanReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context ctx, Intent intent) {
		EventList.checkNetwork();
	}

}
