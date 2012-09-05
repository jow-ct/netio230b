package de.jockels.netioswitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import de.jockels.forms.DialogHelper;

/**
 * @author Jockel
 *
 */
public class EventEdit extends Activity {

	private TextView label1, label2;
	private EditText name1, ext1, ext2, out1;
	private CheckBox active1;
	private Spinner typ1;
	private Button save1;
	
	private Long mRow;
	private EventDb mDb; 
	private Event mContent;
	private DialogHelper<Event> mHelper;
	private boolean mCanceled = false;
	
	private static final String TAG = "EventEdit";
	private static final boolean DEBUG = true;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate");
		
		mDb = new EventDb(this);
		mDb.open();
		
		// Elemente finden
		setContentView(R.layout.eventdialog);
		name1 = (EditText)findViewById(R.id.editText1);
		active1 = (CheckBox)findViewById(R.id.checkBox1);
		typ1 = (Spinner)findViewById(R.id.spinner1);
		label1 = (TextView)findViewById(R.id.textView3);
		ext1 = (EditText)findViewById(R.id.editText2);
		label2 = (TextView)findViewById(R.id.textView4);
		ext2 = (EditText)findViewById(R.id.editText3);
		out1 = (EditText)findViewById(R.id.editText4);
		save1 = (Button)findViewById(R.id.button1);
		
		// DialogHelper
		mHelper = new DialogHelper<Event>(this, Event.eventHelper, Event.class)
				.addDescription(Event.NAME, name1, "darf nicht leer sein")
				.addDescription(Event.EXT1, ext1, "darf nicht leer sein")
				.addDescription(Event.EXT2, ext2, "darf nicht leer sein")
				.addDescription(Event.OUT, out1, "genau vier Zeichen, nur 1, 0, i, u")
				.addDescription(Event.ACTIVE, active1)
				.addDescription(Event.TYPE, typ1);
		
		// neuer Typ bewirkt, dass geänderte Namen angezeigt werden
		typ1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { 
				switchNames(position); 
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		// Klick auf Save bewirkt, dass der Dialog beendet wird
		save1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResultCode(RESULT_OK);
			}
		});
		
		// Gültigkeit überprüfen
		active1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && mHelper.getErrorLogging()) testActive();
			}
		});
		
		// Inhalt ggf. aus savedInstanceState auffüllen
		mRow = savedInstanceState==null ? null : (Long)savedInstanceState.getSerializable(EventDb.ID);
		// nicht im Bundle, also ggf. im Intent?
		if (mRow==null) {
			Bundle extras = getIntent().getExtras();
			mRow = extras == null ? null : extras.getLong(EventDb.ID);
		}
		
		// populateFields kommt dann in onResume, sonst wäre das doppelt
		Log.v(TAG, "onCreate end");
	}

	
	@Override protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}


	public void setResultCode(int aResultCode) {
		mCanceled = aResultCode==RESULT_CANCELED;
		setResult(aResultCode);
		finish();
	}
	
	
	@Override public void onBackPressed() {
		if (mCanceled || mHelper.isEqual(mContent)) {
			// keine Änderungen
			super.onBackPressed();
		} else {
			// Änderungen nicht gespeichert
			new AlertDialog.Builder(this)
			.setTitle("Event-Daten geändert")
			.setMessage("Wollen Sie die Änderungen wirklich verwerfen?")
			.setCancelable(false)
			.setPositiveButton("Verwerfen", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					setResultCode(RESULT_CANCELED);
				}})
			.setNegativeButton("Speichern", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					setResultCode(RESULT_OK);
			}})
			.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
			}})
			.show();
		}
	}
	
	
	// Namen der beiden Extra-Felder ändern
	private void switchNames(int i) {
		label1.setText(EventDb.NAMEN[i][0]);
		label2.setText(EventDb.NAMEN[i][1]);
		mHelper.readFromView(Event.TYPE);
		mHelper.setErrorMessage(Event.EXT1);
		mHelper.setErrorMessage(Event.EXT2);
	}
	

	// Felder aus Datenbank lesen
	private void populateFields() {
		Log.v(TAG, "populateFields");
		if (mRow==null) {
			setTitle("neuen Event anlegen");
			mContent = new Event();
		} else {
			setTitle("Event bearbeiten");
			Cursor event = mDb.queryEvents(null, EventDb.ID+"="+mRow);
			event.moveToFirst();
//			startManagingCursor(event);
			mContent = Event.eventHelper.createFromCursor(event);
			event.close();
		}
		mHelper.writeToView(mContent);
		mHelper.setErrorMessages();
		Log.v(TAG, "populateFields end");
	}

	
	// Felder in Datenbank speichern
	private void saveState() {
		if (mCanceled || mHelper.isEqual(mContent)) return; // keine Änderung
		testActive();
		ContentValues v = mHelper.writeToContentValues();
		
		if (mRow == null) {
			long id = mDb.insertOrThrowEvent(v);
			if (id > 0) mRow = id;
		} else {
			mDb.updateEvents(v, EventDb.ID+"="+mRow);
		}
		mContent = mHelper.current;
	}

	
	public void testActive() {
		if (!mHelper.isValid()) {
			active1.setChecked(false);
			mHelper.current.i[Event.ACTIVE] = 0;
			Toast.makeText(getApplicationContext(), 
					"Event kann nicht aktiviert werden,  da nicht alle Felder korrekt ausgefüllt sind.", 
					Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		saveState(); 
	}


	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
		mHelper.setErrorLogging(true);
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putSerializable(EventDb.ID, mRow);
		if (DEBUG) {
			Log.v(TAG, "outState nach super: "+outState.toString());
			Bundle b = outState.getBundle("android:viewHierarchyState");
			if (b==null) return;
			SparseArray s = b.getSparseParcelableArray("android:views");
			if (s==null) return;
			Log.v(TAG, s.toString()+", "+s.size()+" Elemente:");
			for (int i=0; i<s.size(); i++) {Log.v(TAG, s.valueAt(i).toString());}
		}
	}

	
}
