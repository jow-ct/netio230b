package de.jockels.netioswitch;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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

	private static final String TAG = "EventEdit";
	private static final boolean DEBUG = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDb = new EventDb(this);
		mDb.open();
		
		setContentView(R.layout.eventdialog);
		name1 = (EditText)findViewById(R.id.editText1);
		active1 = (CheckBox)findViewById(R.id.checkBox1);
		typ1 = (Spinner)findViewById(R.id.spinner1);
		label1 = (TextView)findViewById(R.id.textView3);
		ext1 = (EditText)findViewById(R.id.editText2);
		label2 = (TextView)findViewById(R.id.textView4);
		ext2 = (EditText)findViewById(R.id.editText3);
		save1 = (Button)findViewById(R.id.button1);
		out1 = (EditText)findViewById(R.id.editText4);
		
		typ1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				switchNames(position);
			}
			public void onNothingSelected(AdapterView<?> arg0) { }
		});
		save1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (DEBUG) Log.v(TAG, "save.onClick");
				setResult(RESULT_OK);
				finish();
			}
		});

		mRow = savedInstanceState==null ? null : (Long)savedInstanceState.getSerializable(EventDb.ID);
		if (mRow==null) {
			Bundle extras = getIntent().getExtras();
			mRow = extras == null ? null : extras.getLong(EventDb.ID);
		}
		populateFields();
	}

	
	@Override protected void onDestroy() {
		mDb.close();
		super.onDestroy();
	}

	
	@Override public void onBackPressed() {
		super.onBackPressed();
		// TODO "sollen Änderungen nicht gespeichert werden?"
	}


	private void switchNames(int i) {
		label1.setText(EventDb.NAMEN[i][0]);
		label2.setText(EventDb.NAMEN[i][1]);
	}


	private void populateFields() {
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
			if (!TextUtils.isEmpty(n1) && !TextUtils.isEmpty(o1)) {
				long id = mDb.insertOrThrowEvent(v);
				if (id > 0) mRow = id;
			}
		} else {
			mDb.updateEvents(v, EventDb.ID+"="+mRow);
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
	}

	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putSerializable(EventDb.ID, mRow);
	}

	
}
