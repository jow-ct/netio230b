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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import de.jockels.netioswitch.Connection.Listener;

public class CommService extends IntentService {
	@SuppressWarnings("unused")
	private final static String TAG = "CommService";

	/*
	 * Die gültigen Befehle
	 */
	public final static String cAlias = "alias";
	public final static String cStatus = "status";
	public final static String cSet = "set";
	public final static String cError = "error";
	
	public final static String ACTION_SET = cSet;
	public final static String EXTRA_OUT = EventDb.OUTPUT;
	public final static String EXTRA_CONNECTION = "connection";


	/**
	 * Binder-Krams
	 * 
	 * Die von außen erreichbaren Funktionen sind nicht einfach das Interface des Service,
	 * sondern pro Binder wird ein Set von Steckdosen-Parametern und einem Listener
	 * abgespeichert
	 */
	public class CommBinder extends Binder {
		private int mConnection;
		public void setConnectionIndex(int i) { mConnection = i; }
		
		public CommBinder start() { 
			CommService.this.start(mConnection); 
			return this;
		}
		
		public void doSwitch(int b, boolean state) {
			CommService.this.doSwitch(mConnection, b, state); 
		}

		public void doSwitch(String command) { 
			CommService.this.doSwitch(mConnection, command); 
		}

		public void doStatus() { 
			CommService.this.doStatus(mConnection);
		}
	}
	
	
	@Override public IBinder onBind(Intent intent) { 
		Log.v(TAG, "onBind");
		return new CommBinder();
	}

	
	public CommService() { 
		super("CommService"); 
	}
	
	
	@Override
	protected void onHandleIntent(Intent arg0) {
		Log.v(TAG, "Intent "+arg0.getAction());
		if (ACTION_SET.equals(arg0.getAction())) {
			doSwitch(arg0.getIntExtra(EXTRA_CONNECTION, 0), arg0.getStringExtra(EXTRA_OUT));
		}
	}


	protected void start(int connection) {
//		if (param.port>0 || !TextUtils.isEmpty(param.ip)) {
			new Commander(connection).execute(
					cStatus, "port list", "250 ",
					cAlias, "alias", "250 ");
	}
	
	
	protected void doSwitch(int connection, int b, boolean state) {
		new Commander(connection).execute(
				cSet, "port"+" "+b+" "+(state ? "1" : "0"), null,
				cStatus, "port list", "250 ");
	}
	
	
	protected void doSwitch(int connection, String command) {
		new Commander(connection).execute(
				cSet, "port list "+command, null,
				cStatus, "port list", "250 ");
	}

	
	private void doStatus(int connection) {
		new Commander(connection).execute(
				cStatus, "port list", "250 ");
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
			Log.v(TAG, "doIt!");
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
			Log.v(TAG, "onUpdate "+l);
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
