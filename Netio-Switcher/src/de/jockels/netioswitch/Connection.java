package de.jockels.netioswitch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import de.jockels.lib.StringTools;

/**
 * Über diese Parameter wird eine Steckdose angesprochen
 * - bei mir: 192.168.1.60 oder jockulator.dyndns.org
 * - Name/PW default: admin/admin
 * - Port default: 1234
 * - Timeout: 1s im LAN, 5-10s per UMTS
 * 
 * Der Listener ist das Callback-Interface
 */
public class Connection {
	private String ip, username, password;
	private int port, timeout;
	private Listener listener;
	
	
	/**
	 * Callback
	 */
	public interface Listener {
		public abstract void onError(String error); // voriges Kommando oder Connect nicht ausgeführt
		public abstract void onCommand(String command, String result); // Kommando richtig ausgeführt
	}
	

	/**
	 * Connection aus der Konfiguration auslesen
	 * @param cfg Konfigurationsdatei (müsste man für mehrere Steckdosen erweitern)
	 */
	public Connection(SharedPreferences cfg) {
		ip = cfg.getString("ip", "");
		port = StringTools.tryParseInt(cfg.getString("kshell", ""));
		username = cfg.getString("username", "");
		password = cfg.getString("password", "");
		timeout = StringTools.tryParseInt(cfg.getString("timeout", ""));
	}

	/**
	 * Connection aus Intent auslesen, der von einem BroadcastReceiver kommt
	 * @param intent der Intent 
	 *
	public Connection(Intent intent) {
		ip = intent.getStringExtra("ip");
		port = intent.getIntExtra("kshell", 0);
		username = intent.getStringExtra("username");
		password = intent.getStringExtra("password");
		timeout = intent.getIntExtra("timeout", 0);
	}*/

	
	/**
	 * Schreibt die Connection-Parameter in einen Event, der dann losgeschickt werden kann
	 * @param intent
	 *
	public void writeToIntent(Intent intent) {
		intent.putExtra("ip", ip);
		intent.putExtra("kshell", port);
		intent.putExtra("username", username);
		intent.putExtra("password", password);
		intent.putExtra("timeout", timeout);
	}*/

	
	// getter und setter ----------------------------------------------------------------------------
	public String getIp() { return ip; }
	public String getUsername() { return username; }
	public String getPassword() { return password; }
	public int getPort() { return port; }
	public int getTimeout() { return timeout; }
	public Listener getListener() { return listener; }
	public void setListener(Listener l) { listener = l; }
	public void clearListener() { listener = null; }
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (this==o) return true;
		if (!(o instanceof Connection)) return false;
		Connection oo = (Connection)o;
		return (port==oo.port) && (timeout==oo.timeout) && TextUtils.equals(ip, oo.ip)
				&& TextUtils.equals(username, oo.username) && TextUtils.equals(password, oo.password);
	}
}