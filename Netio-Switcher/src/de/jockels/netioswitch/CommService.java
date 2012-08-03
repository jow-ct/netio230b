package de.jockels.netioswitch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

public class CommService extends Service {
	@SuppressWarnings("unused")
	private final static String TAG = "CommService";

	/*
	 * Die gültigen Befehle
	 */
	public final static String cAlias = "alias";
	public final static String cStatus = "status";
	public final static String cSet = "set";
	public final static String cError = "error";

	
	/*
	 * Callback
	 */
	public interface CommunicatorListener {
		public abstract void onError(String error); // voriges Kommando oder Connect nicht ausgeführt
		public abstract void onCommand(String command, String result); // Kommando richtig ausgeführt
	}

	
	/*
	 * Über diese Parameter wird eine Steckdose angesprochen
	 * - bei mir: 192.168.1.60 oder jockulator.dyndns.org
	 * - Name/PW default: admin/admin
	 * - Port default: 1234
	 * - Timeout: 1s im LAN, 5-10s per UMTS
	 */
	public static class Parameter {
		private String ip, username, password;
		private int port, timeout;
		public Parameter(String aIp, String aPort, String aUsername, String aPassword, String aTimeout) {
			ip = aIp;
			try {
				port = Integer.parseInt(aPort);
			} catch (NumberFormatException e) {
				port = 0;
			}
			username = aUsername;
			password = aPassword;
			try {
				timeout = Integer.parseInt(aTimeout);
			} catch (NumberFormatException e) {
				timeout = 0;
			}
		}
	}

	
	/*
	 * Binder-Krams
	 */
	private final IBinder mBinder = new LocalBinder();
	public class LocalBinder extends Binder {
		CommService getService() {return CommService.this;}
	}
	@Override public IBinder onBind(Intent intent) { return mBinder; }

	/*
	 * internes
	 */
	private Parameter param;
	private CommunicatorListener listener;

	
	public CommService setParameter(Parameter aParam) {
		this.param = aParam;
		return this;
	}
	
	
	public CommService setListener(CommunicatorListener aListen) {
		listener = aListen;
		return this;
	}
	
	
	public void clearListener() {
		listener = null;
	}
	

	public CommService start() {
		if (param.port>0 || !TextUtils.isEmpty(param.ip)) {
			new Commander().execute(
					cStatus, "port list", "250 ",
					cAlias, "alias", "250 ");
		}  
		return this;
	}

	
	public void doSwitch(int b, boolean state) {
		new Commander().execute(
				cSet, "port"+" "+b+" "+(state ? "1" : "0"), null,
				cStatus, "port list", "250 ");
	}
	
	
	public void doSwitch(String command) {
		new Commander().execute(
				cSet, "port list "+command, null,
				cStatus, "port list", "250 ");
	}

	
	private class Commander extends AsyncTask<String, String, Void> {
		private PrintWriter out;
		private BufferedReader in;
				
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
				s.connect(new InetSocketAddress(param.ip, param.port), param.timeout*1000);
				out = new PrintWriter(s.getOutputStream(), true);
	        	in = new BufferedReader(new InputStreamReader(s.getInputStream()), 1024);
	        	command(null, "100 HELLO", null);
	        	command("login "+param.username+" "+param.password, "250 OK", null);
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
			if (CommService.this.listener != null)
				if (values.length==1) {
					CommService.this.listener.onError(values[0]);
				} else {
					CommService.this.listener.onCommand(values[0], values[1]);
				}
		}

	}
	
}
