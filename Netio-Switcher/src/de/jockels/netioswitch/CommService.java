package de.jockels.netioswitch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import de.jockels.netioswitch.Connection.Listener;

public class CommService extends IntentService {
	private final static String TAG = "CommService";

	// die gültigen Steckdosen-Befehle
	public final static String cAlias = "alias";
	public final static String cStatus = "status";
	public final static String cSet = "set";
	public final static String cError = "error";
	
	// die Intents
	public final static String ACTION_START = "start";
	public final static String ACTION_STATUS = "status";
	public final static String ACTION_SETALL = "setall";
	public final static String ACTION_SETONE = "setone";
	public final static String EXTRA_CONNECTION = "connection";		// bei allen
	public final static String EXTRA_OUT = "out";									// bei SETALL
	public final static String EXTRA_PORT = "port";								// bei SETONE
	public final static String EXTRA_EA = "ea";										// bei SETONE
	
	
	public CommService() { 
		super("CommService"); 
	}
	
	
	@Override
	protected void onHandleIntent(Intent arg0) {
		Log.v(TAG, "Intent "+arg0.getAction());
		int idx = arg0.getIntExtra(EXTRA_CONNECTION, 0);
		String cmd = arg0.getAction();

		if (ACTION_SETALL.equals(cmd)) {
			new Commander(idx).execute(
					cSet, "port list "+arg0.getStringExtra(EXTRA_OUT), null,
					cStatus, "port list", "250 ");
		} else if (ACTION_SETONE.equals(cmd)) {
			new Commander(idx).execute(
					cSet, "port"+" "+arg0.getIntExtra(EXTRA_PORT, 0)+" "
							+(arg0.getBooleanExtra(EXTRA_EA, false) ? "1" : "0"), null,
					cStatus, "port list", "250 ");
		} else if (ACTION_START.equals(cmd)) {
			new Commander(idx).execute(
					cStatus, "port list", "250 ",
					cAlias, "alias", "250 ");
		} else if (ACTION_STATUS.equals(cmd)) {
			new Commander(idx).execute(
					cStatus, "port list", "250 ");
		}
	}


	private class Commander extends AsyncTask<String, String, Void> {
		private PrintWriter out;
		private BufferedReader in;
		private Connection c;
		
		private Commander(int connection) {
			super();
			c = ConnectionList.getConnection(connection);
		}
		
		
		@Override
		protected  Void doInBackground(String... params) {
			synchronized (CommService.this) {
				doIt(params);
			}
			return null;
		}
		
		private Void doIt(String... params) {
			Socket s = null;
			try {
				s = new Socket();
				s.connect(new InetSocketAddress(c.getIp(), c.getPort()), c.getTimeout()*1000);
				out = new PrintWriter(s.getOutputStream(), true);
	        	in = new BufferedReader(new InputStreamReader(s.getInputStream()), 1024);
	        	command(null, "100 HELLO", null);
	        	command("login "+c.getUsername()+" "+c.getPassword(), "250 OK", null);
	        	for (int i=0; i<params.length / 3; i++) {
	        		command(params[3*i+1], params[3*i+2], params[3*i]);
	        	}
	        	command("quit", null, null);
			} catch (IOException e) {
				publishProgress(e.getMessage());
			} finally {
				try {if (s!=null) s.close();} catch (IOException e) {}
				try {if (in!=null) in.close();} catch (IOException e) {}
				try {if (out!=null) out.close();} catch (Throwable e) {}
			}
			return null;
		}

		
		private void command(String cmd, String test, String id) throws IOException {
			if (cmd!=null) out.println(cmd);
			String r = in.readLine();
			if (test!=null) {
				if (r==null || !r.startsWith(test)) throw new IOException("unbekannte Antwort "+r);
				r = r.substring(test.length());
			}
			if (id!=null) publishProgress(id, r);
		}

	
		/**
		 * Wenn publishProgress mit einem einzelnen Parameter aufgerufen wurde, war das ein Fehler. Wenn
		 * mit zweien, ist alles richtig gelaufen und im zweiten steht die Rückmeldung 
		 */
		@Override protected void onProgressUpdate(String... values) {
			Listener l = c.getListener();
			if (l != null) {
				if (values.length==1) {
					l.onError(values[0]);
				} else {
					l.onCommand(values[0], values[1]);
				}
			}
		}

	}
	
}
