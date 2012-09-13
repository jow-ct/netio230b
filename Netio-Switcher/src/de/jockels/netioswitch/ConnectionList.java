package de.jockels.netioswitch;

import java.util.ArrayList;

public class ConnectionList {
	private static ArrayList<Connection> mConnection = new ArrayList<Connection>(5);
	
	public static int addConnection(Connection c) {
		mConnection.add(c);
		return mConnection.size();
	}
	
	public static Connection getConnection(int i) {
		return mConnection.get(i);
	}
	
	public static int getSize() {
		return mConnection.size();
	}
	
	public static void clearConnections() {
		mConnection = new ArrayList<Connection>(5);
	}
	
}
