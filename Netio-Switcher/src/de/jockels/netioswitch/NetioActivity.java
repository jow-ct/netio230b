package de.jockels.netioswitch;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import de.jockels.lib.StringTools;
import de.jockels.netioswitch.Connection.Listener;

/**
 * 
 * TODO Visualisierung des Reloaders z.B. mit Icon am Rand
 * 
 * Stichworte für Artikel
 *  -	in Service gepackt, damit BroadcastReceiver direkt was damit machen können
 *  -	auf den Befehl zum Lesen eines einzelnen Status verzichtet, aber doSwitch kennt beide Varianten
 *  -	das "Asynchrone" steckt im Service. Die Komponenten, die das auswerten, müssen nur ein Listener
 *  	implementieren
 *  
 *  Ideen für Intents:
 *  http://developer.android.com/reference/android/content/Intent.html
 *  (ab Summary)
 */

public class NetioActivity extends ListActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "NetioActivity";
	private final static boolean DEBUG = true;
	private SharedPreferences mCfg;
	private boolean mReconnectNeeded = false;
	private boolean mConnected = false;
	private EventDb mDb;
	
	private TextView id, error, reload;
	private ProgressBar loading;
	private ToggleButton[] b = new ToggleButton[4];
	private Button[] a = new Button[2];

	private Reloader mReloader;
	
	public static final int ACTIVITY_CREATE = 0;
	public static final int ACTIVITY_EDIT = 1;
	

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate");
		
		// Event-Liste erzeugen und anpassen
		setContentView(R.layout.main);
		registerForContextMenu(getListView());
		
		// Bug?!
		try { Class.forName("android.os.AsyncTask"); } catch (ClassNotFoundException e) { }
		
		// Konfigurationsdatei öffnen
		mCfg = PreferenceManager.getDefaultSharedPreferences(this);
		mCfg.registerOnSharedPreferenceChangeListener(this);
		Log.v(TAG, "onCreate: connection-size: "+ConnectionList.getSize());
		ConnectionList.addConnection(new Connection(mCfg));
		Log.v(TAG, "onCreate: connection-size: "+ConnectionList.getSize());
		
		// Event-Datenbank öffnen
		mDb = new EventDb(this);
		mDb.open();
		fillData();
		if (mCfg.getBoolean("autoevent", false)) startEvents();
		
		// Zeiger auf Steuerelement basteln und Knöppe auf inaktiv schalten 
		id = (TextView)findViewById(R.id.textView0);
		error = (TextView)findViewById(R.id.textView1);
		reload = (TextView)findViewById(R.id.textView4);
		loading = (ProgressBar)findViewById(R.id.progressBar1);
		b[0] = (ToggleButton)findViewById(R.id.toggleButton1);
		b[1] = (ToggleButton)findViewById(R.id.toggleButton2);
		b[2] = (ToggleButton)findViewById(R.id.toggleButton3);
		b[3] = (ToggleButton)findViewById(R.id.toggleButton4);
		a[0] = (Button)findViewById(R.id.button1);
		a[1] = (Button)findViewById(R.id.button2);
		View.OnClickListener ocl = new ButtonListener();
		for (ToggleButton i : b) i.setOnClickListener(ocl);
		for (Button i : a) i.setOnClickListener(ocl);

		// Verbindung herstellen, falls gewünscht, bzw. Parameter abfragen
		if (TextUtils.isEmpty(mCfg.getString("ip", ""))) {
			startActivity(new Intent(this, Einstellungen.class));
		} else {
			mReconnectNeeded = true;
		}
	}

	
	@Override protected void onStart() {
		super.onStart();
		Log.v(TAG, "onStart");
		if (mReconnectNeeded) {
			if (mCfg.getBoolean("autoconnect", false)) 
				commStart();
			else {
				commStop();
				setDisable("(kein Autostart)");
			}
			mReconnectNeeded = false;
		}
	}

	
	@Override protected void onDestroy() {
		Log.v(TAG, "onDestroy");
		pauseEvents();
		commStop();
		mDb.close();
		super.onDestroy();
	}

	
	/**
	 * Kommunikation mit einer Steckdose beginnen
	 */
	private void commStart() {
		if (DEBUG) Log.v(TAG, "startComm");
		commStop();
		
		// jetzt Verbindung herstellen
		setDisable("verbinde mit "+mCfg.getString("ip", "")+" ...");
		ConnectionList.getConnection(0).setListener(new CommListener());
		mConnected = true;
		commStatus();

		// Reloader
		int r = StringTools.tryParseInt(mCfg.getString("reload", ""));
		if (r>0) {
			// Reloader anwerfen
			mReloader = new Reloader(r);
			mReloader.start();
			// Anzeige
			reload.setVisibility(View.VISIBLE);
			reload.setText(r+" s reload");
		} else {
			mReloader = null;
			reload.setVisibility(View.GONE);
			loading.setVisibility(View.GONE);
		}

	}
	
	
	/**
	 * Kommunikation stoppen
	 */
	private void commStop() {
		if (DEBUG) Log.v(TAG, "stopComm "+mConnected);
		if (!mConnected) return;
		setDisable("Verbindung getrennt");
		ConnectionList.getConnection(0).clearListener();
		if (mReloader!=null) {
			mReloader.stop();
			mReloader = null;
		}
		reload.setVisibility(View.GONE);
		mConnected = false;
	}

	
	private void commStatus() {
		if (!mConnected) return;
		Intent i = new Intent(this, CommService.class);
		i.putExtra(CommService.EXTRA_CONNECTION, 0); 
		i.setAction(CommService.ACTION_START);
		this.startService(i);
	}

	
	private void commSwitch(String o) {
		if (!mConnected) return;
		Intent i = new Intent(this, CommService.class);
		i.setAction(CommService.ACTION_SETALL);
		i.putExtra(CommService.EXTRA_CONNECTION, 0); 
		i.putExtra(CommService.EXTRA_OUT, o);
		this.startService(i);
	}

	
	private void commSwitch(int port, boolean ea) {
		if (!mConnected) return;
		Intent i = new Intent(this, CommService.class);
		i.setAction(CommService.ACTION_SETONE);
		i.putExtra(CommService.EXTRA_CONNECTION, 0); 
		i.putExtra(CommService.EXTRA_PORT, port);
		i.putExtra(CommService.EXTRA_EA, ea);
		this.startService(i);
	}

	
	/**
	 * Events starten/stoppen
	 */
	private void startEvents() {
		int c = EventList.refreshEvents(mDb, new Connection(mCfg));
		int s = EventList.startEvents(this);
		Toast.makeText(this, c+" Events geladen, davon "+s+" gestartet", Toast.LENGTH_SHORT).show();
		// TODO ChoiceFormat
		// http://docs.oracle.com/javase/tutorial/i18n/format/choiceFormat.html
	}
	
	
	private void stopEvents() {
		int s = EventList.stopEvents();
		Toast.makeText(this, s+" Events angehalten", Toast.LENGTH_SHORT).show();
	}

	
	private void pauseEvents() {
		int s = EventList.pauseEvents();
		Toast.makeText(this, s+" Events angehalten", Toast.LENGTH_SHORT).show();
	}
	

	/**
	 * Klasse zum regelmäßigen Abfragen einer Steckdose
	 *
	 */
	private class Reloader implements Runnable{
		int mReload;
		Handler mHandler;
		Reloader(int r) {
			super();
			if (r<5) r = 5;
			mReload = r*1000;
			mHandler = new Handler();
		}
		
		public void start() {
			Log.v(TAG, (mReload/1000)+"s reload started");
			mHandler.postDelayed(this, mReload);
		}
		
		public void stop() {
			Log.v(TAG, "reload stopped");
			mReload = 0;
			mHandler.removeCallbacks(this);
		}
		
		public void run() {
			Log.v(TAG, "ticktack");
			loading.setVisibility(View.VISIBLE);
			for (ToggleButton i : b) {
				i.setEnabled(false);
				i.setChecked(false);
			}
			commStatus();
			if (mReload>0) mHandler.postDelayed(this, mReload);
		}
	}
	
	
	/**
	 * Eventliste zusammenbasteln
	 */
	private void fillData() {
		if (DEBUG) Log.v(TAG, "fillData");
		
		// Anzeige zusammenbasteln
        Cursor c = mDb.queryEvents(null, null);
        startManagingCursor(c);
        String[] from = new String[] {EventDb.NAME, EventDb.ACTIVE};
        int[] to = new int[] { R.id.textName, R.id.textActive };
        SimpleCursorAdapter notes = new SimpleCursorAdapter(this, R.layout.event_line, c, from, to);
        notes.setViewBinder(new ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == cursor.getColumnIndex(EventDb.NAME)) {
					String s = cursor.getString(columnIndex);
					if (TextUtils.isEmpty(s)) s = "(kein Name)";
					((TextView)view).setText(s);
				} else if (columnIndex == cursor.getColumnIndex(EventDb.ACTIVE)) {
					((TextView)view).setText(cursor.getInt(columnIndex)>0 ? "(aktiv)" : "(inaktiv)");
				} else 
					return false;
				return true;
			}
        });
        setListAdapter(notes);
	}
	

	/**
	 * Verbindung trennen, Steckdosen-Schalter disablen
	 */
	private void setDisable(String stat) {
		error.setVisibility(View.GONE);
		id.setText(stat);
		for (ToggleButton i : b) {
			i.setEnabled(false);
			i.setChecked(false);
		}
		for (Button i : a) i.setEnabled(false);
	}
	
	
	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mainmenu, menu);    
		return true;	 
	}

	
	@Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (DEBUG) Log.v(TAG, "onCreateContextMenu");
		getMenuInflater().inflate(R.menu.contextmenu, menu);    
	}


	@Override public boolean onOptionsItemSelected(MenuItem item) {    
		switch (item.getItemId()) {    
		case R.id.itemConfig:
			startActivity(new Intent(this, Einstellungen.class));
			return true;    
		case R.id.itemStart:
			commStart();
			return true;
		case R.id.itemStop:
			commStop();
			return true;
		case R.id.itemEventNew:
			startActivityForResult(new Intent(this, EventEdit.class), ACTIVITY_CREATE);
			return true;
		case R.id.itemStartEvents:
			startEvents();
			return true;
		case R.id.itemStopEvents:
			stopEvents();
			return true;
		default:        
			return super.onOptionsItemSelected(item);    
		}
	}

	
	@Override public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.itemEventDel:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDb.deleteEvent(info.id);
			fillData();
			startEvents();
			return true;
		}
		return super.onContextItemSelected(item);
	}


	@Override protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.v(TAG, "onListItemClick");
		stopEvents();
		Intent i = new Intent(this, EventEdit.class);
		i.putExtra(EventDb.ID, id);
		startActivityForResult(i, ACTIVITY_EDIT);
	}


	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(TAG, "onActivityResult "+resultCode);
		super.onActivityResult(requestCode, resultCode, data);
		fillData();
		startEvents();
	}


	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.v(TAG, "onCfgChange");
		mReconnectNeeded = true;
	}
	
	
	/**
	 * auf die sechs Knöpfe horchen und entsprechend doSwitch der Steckdose aufrufen
	 */
	private class ButtonListener implements OnClickListener{
		public void onClick(View v) {
			for (int i=0; i<4; i++) {
				if (b[i]==v) {
					commSwitch(i+1, ((ToggleButton)v).isChecked());
					v.setEnabled(false);
					return;
				}
			}
			// keiner der vier Einzelknöpfe gedrückt, also "alle ein" oder "alle aus"
			for (ToggleButton t : b) {t.setEnabled(false);} // alle deaktivieren
			if (v==a[0]) commSwitch("1111");
			if (v==a[1]) commSwitch("0000");
		}
	}


	/**
	 * Callback der Steckdose
	 */
	private class CommListener implements Listener{
		
		public void onError(String err) {
			// nicht weiter definierte Fehlermeldung, i.Allg. = Verbindung kaputt
			Log.v(TAG, "onError "+err);
			setDisable("nicht verbunden");
			error.setText(err);
			error.setVisibility(View.VISIBLE);
		}

		public void onCommand(String command, String result) {
			if (DEBUG) Log.v(TAG, "onCommand '"+command+"': "+result);
			if (CommService.cAlias.equals(command)) {
				// Initialisierung erfolgreich, also Namen anzeigen und Knöppe aktivieren
				String ip = mCfg.getString("ip", "");
				id.setText(result+(ip.length()>15 ? "\n(" : " (")+ip+")");
				error.setVisibility(View.GONE);
				for (Button i : a) i.setEnabled(true);
				
			} else if (CommService.cStatus.equals(command)) {
				// Statusmeldung aller vier Buchsen; Status anzeigen und enablen
				for (int i=0; i<4; i++) {
					b[i].setEnabled(true);
					b[i].setChecked(result.charAt(i)=='1');
				}
				if (mReloader!=null) loading.setVisibility(View.GONE);
				
			} else if (CommService.cSet.equals(command)) {
				// keine Aktion
				
			}
		}
	};
}
