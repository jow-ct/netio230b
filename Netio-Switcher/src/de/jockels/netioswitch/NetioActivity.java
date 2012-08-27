package de.jockels.netioswitch;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.jockels.netioswitch.CommService.CommunicatorListener;
import de.jockels.netioswitch.CommService.LocalBinder;

/**
 * 
 * TODO Validierung des OUT-Felds entweder direkt bei der Eingabe oder zumindest beim EventList
 * 
 * Stichworte für Artikel
 *  -	in Service gepackt, damit BroadcastReceiver direkt was damit machen können
 *  -	auf den Befehl zum Lesen eines einzelnen Status verzichtet, aber doSwitch kennt beide Varianten
 *  -	das "Asynchrone" steckt im Service. Die Komponenten, die das auswerten, müssen nur ein Listener
 *  	implementieren
 */

public class NetioActivity extends ListActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	private final static String TAG = "NetioActivity";
	private final static boolean DEBUG = true;
	private CommService mComm;
	private boolean mBound = false;
	private SharedPreferences mCfg;
	private boolean mReconnectNeeded = false;
	private EventDb mDb;
	
	private TextView id, error;
	private ToggleButton[] b = new ToggleButton[4];
	private Button[] a = new Button[2];
	
	public static final int ACTIVITY_CREATE = 0;
	public static final int ACTIVITY_EDIT = 1;
	

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Event-Liste erzeugen und anpassen
		setContentView(R.layout.main);
		registerForContextMenu(getListView());
		
		// Event-Datenbank öffnen
		mDb = new EventDb(this);
		mDb.open();
		fillData();
		
		// Konfigurationsdatei öffnen
		mCfg = PreferenceManager.getDefaultSharedPreferences(this);
		mCfg.registerOnSharedPreferenceChangeListener(this);
		
		// Zeiger auf Steuerelement basteln und Knöppe auf inaktiv schalten 
		id = (TextView)findViewById(R.id.textView0);
		error = (TextView)findViewById(R.id.textView1);
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
		if (mCfg.getString("ip", "").equals("")) {
			startActivity(new Intent(this, Einstellungen.class));
		} else {
			mReconnectNeeded = true;
		}
	}

	
	@Override protected void onStart() {
		super.onStart();
		if (mReconnectNeeded) {
			if (mCfg.getBoolean("autoconnect", false)) 
				startComm();
			else {
				stopComm();
				setDisable("(kein Autostart)");
			}
			mReconnectNeeded = false;
		}
	}

	@Override protected void onDestroy() {
		stopComm();
		mDb.close();
		super.onDestroy();
	}

	
	/**
	 * Service-Connection herstellen
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mComm = binder.getService();
			mBound = true;
			if (DEBUG) Log.v(TAG, "onServiceConnected");
			
			// jetzt Verbindung herstellen
			String ip = mCfg.getString("ip", "");
			setDisable("verbinde mit "+ip+" ...");
			mComm.setParameter(new CommService.Parameter(
					ip, 
					mCfg.getString("kshell", ""), 
					mCfg.getString("username", ""), 
					mCfg.getString("password", ""),
					mCfg.getString("timeout", "")
			)).setListener(new CommListener()).start();
		}

		public void onServiceDisconnected(ComponentName name) {
			if (DEBUG) Log.v(TAG, "onServiceDisconnected");
			mBound = false;
		}
	};

	
	/**
	 * Kommunikation mit einer Steckdose beginnen
	 */
	private void startComm() {
		if (DEBUG) Log.v(TAG, "startComm");
		stopComm();
		bindService(new Intent(this, CommService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	
	/**
	 * Kommunikation stoppen
	 */
	private void stopComm() {
		if (DEBUG) Log.v(TAG, "stopComm "+mBound);
		if (mBound) {
			unbindService(mConnection);
			setDisable("Verbindung getrennt");
			mComm.clearListener();
		}
		mBound = false;
	}

	
	/**
	 * Eventliste zusammenbasteln
	 */
	private void fillData() {
		if (DEBUG) Log.v(TAG, "fillData");
		// Events starten
		EventList.refresh(this);
		
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
			startComm();
			return true;
		case R.id.itemStop:
			stopComm();
			return true;
		case R.id.itemEventNew:
			startActivityForResult(new Intent(this, EventEdit.class), ACTIVITY_CREATE);
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
			return true;
		}
		return super.onContextItemSelected(item);
	}


	@Override protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.v(TAG, "onListItemClick");
		Intent i = new Intent(this, EventEdit.class);
		i.putExtra(EventDb.ID, id);
		startActivityForResult(i, ACTIVITY_EDIT);
	}


	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		fillData();
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
					mComm.doSwitch(i+1, ((ToggleButton)v).isChecked());
					v.setEnabled(false);
					return;
				}
			}
			// keiner der vier Einzelknöpfe gedrückt, also "alle ein" oder "alle aus"
			for (ToggleButton t : b) {t.setEnabled(false);} // alle deaktivieren
			if (v==a[0]) mComm.doSwitch("1111");
			if (v==a[1]) mComm.doSwitch("0000");
		}
	}


	/**
	 * Callback der Steckdose
	 */
	private class CommListener implements CommunicatorListener{
		
		public void onError(String err) {
			// nicht weiter definierte Fehlermeldung, i.Allg. = Verbindung kaputt
			Log.v(TAG, "onError "+err);
			setDisable("nicht verbunden");
			error.setText(err);
			error.setVisibility(View.VISIBLE);
		}

		public void onCommand(String command, String result) {
			Log.v(TAG, "onCommand '"+command+"': "+result);
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
				
			} else if (CommService.cSet.equals(command)) {
				// keine Aktion
				
			}
		}
	};
}
