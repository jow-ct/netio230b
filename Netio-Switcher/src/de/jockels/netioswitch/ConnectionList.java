package de.jockels.netioswitch;

import java.util.ArrayList;

import android.content.Context;
import android.preference.PreferenceManager;

public class ConnectionList {
	private static ArrayList<Connection> mConnection;
	
	public static void initConnections(Context ctx) {
		mConnection = new ArrayList<Connection>(1);
		mConnection.add(new Connection(PreferenceManager.getDefaultSharedPreferences(ctx)));
	}

	public static Connection getConnection(int i) {
		return mConnection.get(i);
	}
	
	public static int getSize() {
		return mConnection.size();
	}
	
	
}
