package de.jockels.netioswitch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
	
	private boolean changed = false;
	private TextWatcher watcher;
	
	private Long mRow;
	private EventDb mDb; 

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
		
		// Listener setzen

		// Änderungen mitbekommen
		watcher = new TextWatcher() {
			public void afterTextChanged(Editable s) { onChanged(); }
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		};
		name1.addTextChangedListener(watcher);
		ext1.addTextChangedListener(watcher);
		ext2.addTextChangedListener(watcher);
		active1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { onChanged(); }
		});
		
		// Validierung
		out1.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) { 
				// TODO über Filter o.ä.
				int i = 0;
				while (i<s.length()) {
					char c = s.charAt(i);
					if (c=='1' || c=='0' || c=='i' || c=='u')
						i++;
					else {
						s.delete(i, i+1);
					}
				}
				onChanged();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Abfrage beim Speichern
				out1.setError( s.length()==4 ? null : "genau vier Zeichen, nur 0, 1, i, u erlaubt");
			}
		});
		
		// neuer Typ bewirkt, dass geänderte Namen angezeigt werden
		typ1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			int oldpos = -1;
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switchNames(position);
				if (oldpos!=-1) onChanged();
				oldpos = position;
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		
		// Klick auf Save bewirkt, dass der Dialog beendet wird
		save1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (DEBUG) Log.v(TAG, "save.onClick");
				setResult(RESULT_OK);
				finish();
			}
		});
		
		// Inhalt ggf. aus savedInstanceState auffüllen
		mRow = savedInstanceState==null ? null : (Long)savedInstanceState.getSerializable(EventDb.ID);
		if (mRow==null) {
			Bundle extras = getIntent().getExtras();
			mRow = extras == null ? null : extras.getLong(EventDb.ID);
		}
	}

	
	@Override protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}

	
	@Override public void onBackPressed() {
		if (changed) {
			Log.v(TAG, "änderungen!!");
			new AlertDialog.Builder(this)
			.setTitle("Event-Daten geändert")
			.setMessage("Wollen Sie die Änderungen wirklich verwerfen?")
			.setCancelable(false)
			.setPositiveButton("Verwerfen", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					onResetChanged();
					onBackPressed();
				}})
			.setNegativeButton("Speichern", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					saveState();
					onBackPressed();
			}})
			.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
			}})
			.show();
		} else 
			super.onBackPressed();
	}


	private void switchNames(int i) {
		label1.setText(EventDb.NAMEN[i][0]);
		label2.setText(EventDb.NAMEN[i][1]);
	}


	// ein Edit-Dings gibt bekannt, dass was geändert wurde
	private void onChanged() {
		changed = true;
	}
	
	
	// neue Daten geladen, Änderungen zurücksetzen 
	private void onResetChanged() {
		changed = false;
	}

	private void populateFields() {
		if (DEBUG) Log.v(TAG, "populateFields");
		if (mRow==null) {
			setTitle("neuen Event anlegen");
			active1.setChecked(true);
		} else {
			setTitle("Event bearbeiten");
			Cursor event = mDb.queryEvents(null, EventDb.ID+"="+mRow);
			event.moveToFirst();
			startManagingCursor(event );
			name1.setText(event.getString(event.getColumnIndex(EventDb.NAME)));
			active1.setChecked(event.getInt(event.getColumnIndex(EventDb.ACTIVE))>0);
			typ1.setSelection(event.getInt(event.getColumnIndex(EventDb.TYPE)));
			ext1.setText(event.getString(event.getColumnIndex(EventDb.EXT1)));
			ext2.setText(event.getString(event.getColumnIndex(EventDb.EXT2)));
			out1.setText(event.getString(event.getColumnIndex(EventDb.OUTPUT)));
		}
		switchNames(typ1.getSelectedItemPosition());
		onResetChanged();
	}

	
	private void saveState() {
		if (DEBUG) Log.v(TAG, "saveState; mRow:"+mRow);
		ContentValues v = new ContentValues();
		
		String n1 = name1.getText().toString();
		String o1 = out1.getText().toString();
		v.put(EventDb.NAME, n1);
		v.put(EventDb.ACTIVE, active1.isChecked());
		v.put(EventDb.TYPE, typ1.getSelectedItemId());
		v.put(EventDb.EXT1, ext1.getText().toString());
		v.put(EventDb.EXT2, ext2.getText().toString());
		v.put(EventDb.OUTPUT, o1);
		
		if (mRow == null) {
			// Speichern nur, wenn Name&Output eingegeben wurden
			if (!TextUtils.isEmpty(n1)) {
				long id = mDb.insertOrThrowEvent(v);
				if (id > 0) mRow = id;
			}
		} else {
			mDb.updateEvents(v, EventDb.ID+"="+mRow);
		}
		onResetChanged();
	}

	
	@Override
	protected void onPause() {
		super.onPause();
		if (changed) saveState(); // ohne if (changed) entstehen sonst leere Einträge
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
	}

	
}
