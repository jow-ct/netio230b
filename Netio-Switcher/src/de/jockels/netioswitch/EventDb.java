package de.jockels.netioswitch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class EventDb {

	/** Feldnamen */
	public final static String ID = "_id";
	public final static String TYPE = "type";
	public final static String EXT1 = "ext1";
	public final static String EXT2 = "ext2";
	public final static String NAME = "name";
	public final static String LAST_MAN = "last_man";
	public final static String LAST_AUTO = "last_auto";
	public final static String COUNT_MAN = "count_man";
	public final static String COUNT_AUTO = "count_auto";
	public final static String ACTIVE = "active";
	public final static String OUTPUT = "output";
	
	/** Typen */
	public final static int TYP_WLAN_BETRETEN = 0;
	public final static int TYP_WLAN_VERLASSEN = 1;
	public final static int TYP_SECONDS = 2;
	
	public final static String UNUSED = "-";
	public final static String[][] NAMEN = new String[][] {
		{"WLAN-Name", UNUSED},		// WLAN betreten
		{"Test1", "Test2"},					// WLAN verlassen
		{"Sekunden", UNUSED}					// alle 5 Sekunden
	};
	
	/** Internes */
	private final static String TAG = "EventDb";
	private final static String DB_NAME = "events";
	private final static int DB_VERSION = 3;
	private final static String DB_TABLE_EVENTS = "events";

    private  SQLiteDatabase mDb;				
    private DatabaseHelper mDbHelper;	
    private Context mCtx;								

    
    private static class DatabaseHelper extends SQLiteOpenHelper {
    	DatabaseHelper(Context context) {
    		super(context, DB_NAME, null, DB_VERSION);
    	}
    
    	/**
    	 * Datenbank erzeugen -- mehr nicht
    	 */
    	@Override public void onCreate(SQLiteDatabase db) {
    		Log.i(TAG, "erzeuge Datenbank");
    		db.execSQL("create table "+DB_TABLE_EVENTS+" ("+
    				ID+"  integer primary key, "+
    				TYPE+" integer, "+
    				EXT1+" text, "+
    				EXT2+" text, "+
    				NAME+" text not null, "+
    				LAST_MAN+" integer, "+
    				LAST_AUTO+" integer, "+
    				COUNT_MAN+" integer, "+
    				COUNT_AUTO+" integer, "+
    				ACTIVE+" integer, "+
    				OUTPUT+" text);");
    	}

    	/**
    	 * Datenbank in neues Format übernehmen. Noch ist nichts wichtiges gespeichert...
    	 */
    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		Log.w(TAG, "ct V" + oldVersion + " -> V" + newVersion+ " (Datenbank wird neu aufgebaut)");
    		db.execSQL("DROP TABLE IF EXISTS "+DB_TABLE_EVENTS);
    		onCreate(db);
    	}
    }

    
    /**
     * Variablen initialisieren, Datenbank noch nicht öffnen
     * @param context Aufrufer
     */
    public EventDb (Context context) {
    	mCtx = context;
    }

    
    /**
     * Datenbank öffnen und ggf. erzeugen
     * @return this (für Verkettung)
     * @throws SQLException wenn weder Öffnen noch Erzeugen klappen
     */
    public void open() throws SQLException {
    	mDbHelper = new DatabaseHelper(mCtx);
    	mDb = mDbHelper.getWritableDatabase();
    }
  
    
    /**
     * Datenbank schließen
     */
    public void close() {
    	mDb.close();
    }
    
  
    public Cursor queryEvents(String[] projection, String selection) {
    	return mDb.query(DB_TABLE_EVENTS, projection, selection, null, null, null, NAME+" DESC");
    }
    
    public int updateEvents(ContentValues values, String where) {
    	return mDb.update(DB_TABLE_EVENTS, values, where, null);
    }
    
    public long insertOrThrowEvent(ContentValues values) throws SQLException {
    	return mDb.insertOrThrow(DB_TABLE_EVENTS, null, values);
    }

    public int deleteEvent(long id) {
    	return mDb.delete(DB_TABLE_EVENTS, ID+"="+id, null);
    }
    
    
}
