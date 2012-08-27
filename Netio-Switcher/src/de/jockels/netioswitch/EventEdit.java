package de.jockels.netioswitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * @author Jockel
 *
 */
public class EventEdit extends Activity implements OnFocusChangeListener {

	private TextView label1, label2;
	private EditText name1, ext1, ext2, out1;
	private CheckBox active1;
	private Spinner typ1;
	private Button save1;
	
	private Long mRow;
	private EventDb mDb; 
	private Event mContent;
	
	private static final String TAG = "EventEdit";
	private static final boolean DEBUG = true;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
		
		// Validierungen
		name1.setOnFocusChangeListener(this);
		out1.setOnFocusChangeListener(this);
		name1.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) { testName1(); }
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
		});
		out1.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) { testOut1(); }
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
		});
		
		// neuer Typ bewirkt, dass geänderte Namen angezeigt werden
		typ1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { switchNames(position); }
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		// Klick auf Save bewirkt, dass der Dialog beendet wird
		save1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_OK);
				finish();
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
	}

	
	@Override protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}

	
	@Override public void onBackPressed() {
		if (mContent.equals(getEvent())) {
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
					setResult(RESULT_CANCELED);
					setEvent(mContent); // Felder von gespeichertem Event laden
					EventEdit.this.onBackPressed();
				}})
			.setNegativeButton("Speichern", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					setResult(RESULT_OK);
					EventEdit.this.finish();
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
	}

	
	/**
	 * zwischen Feldern und Event schaufeln
	 */
	private Event getEvent() {
		Event e = new Event();
		e.name = name1.getText().toString();
		e.active = active1.isChecked();
		e.type = typ1.getSelectedItemPosition();
		e.ext1 = ext1.getText().toString();
		e.ext2 = ext2.getText().toString();
		e.out = out1.getText().toString();
		return e;
	}

	private void setEvent(Event e) {
		name1.setText(e.name);
		active1.setChecked(e.active);
		typ1.setSelection(e.type);
		ext1.setText(e.ext1);
		ext2.setText(e.ext2);
		out1.setText(e.out);
		switchNames(e.type);
	}
	
	
	/**
	 * Validierung
	 */
	public void onFocusChange(View v, boolean hasFocus) {
		if (v==null || v==name1) testName1();
		if (v==null || v==out1) testOut1();
	}

	private void testName1() {
		if (!Event.isNameValid(name1.getText().toString())) name1.setError("darf nicht leer sein");
	}
	
	private void testOut1() {
		if (!Event.isOutValid(out1.getText().toString())) out1.setError("genau vier Zeichen, nur 0, 1, i, u erlaubt");
	}
	

	// Felder aus Datenbank lesen
	private void populateFields() {
		if (mRow==null) {
			setTitle("neuen Event anlegen");
			mContent = new Event();
		} else {
			setTitle("Event bearbeiten");
			Cursor event = mDb.queryEvents(null, EventDb.ID+"="+mRow);
			event.moveToFirst();
			startManagingCursor(event);
			mContent = new Event(event);
		}
		setEvent(mContent);
		onFocusChange(null, true); // Validierung
	}

	
	// Felder in Datenbank speichern
	private void saveState() {
		Event current = getEvent();
		if (mContent.equals(current)) return; // keine Änderung
		if (!current.isOutValid()) current.active = false; // abschalten, wenn String falsch ist
		ContentValues v = current.createContentValues();
		
		if (mRow == null) {
			long id = mDb.insertOrThrowEvent(v);
			if (id > 0) mRow = id;
		} else {
			mDb.updateEvents(v, EventDb.ID+"="+mRow);
		}
		mContent = current;
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
