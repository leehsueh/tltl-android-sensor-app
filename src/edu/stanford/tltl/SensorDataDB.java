package edu.stanford.tltl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class manages a connection to the database, providing
 * convenience methods to create/update/delete, and centralizing the
 * constants used in the database.
 * 
 * It should be possible to adapt this class for common android/db applications
 * by changing the constants and a few methods.
 * 
 * This class is released into the public domain, free for any purpose.
 * Nick Parlante 2011
 * 
 * modified for custom use by Hain-Lee Hsueh 2011
 *
 */
public class SensorDataDB {
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "sensordatadb";
	public static final String DATABASE_TABLE = "sensordata";

	// Field names -- use the KEY_XXX constants here and in
	// client code, so it's all consistent and checked at compile-time.

	public static final String KEY_ROWID = "_id";  // Android requires exactly this key name
	public static final int INDEX_ROWID = 0;
	public static final String KEY_TITLE = "title";
	public static final int INDEX_TITLE = 1;
	public static final String KEY_NOTES = "notes";
	public static final int INDEX_NOTES = 2;
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final int INDEX_TIMESTAMP = 3;
	public static final String KEY_DATA = "data";
	public static final int INDEX_DATA = 4;
	public static final String KEY_SENSOR_TYPES = "sensor_types";
	public static final int INDEX_SENSOR_TYPES = 5;
	
	public static final String[] KEYS_ALL = {
		SensorDataDB.KEY_ROWID, 
		SensorDataDB.KEY_TITLE, 
		SensorDataDB.KEY_NOTES, 
		SensorDataDB.KEY_TIMESTAMP, 
		SensorDataDB.KEY_DATA,
		SensorDataDB.KEY_SENSOR_TYPES
	};


	private Context mContext;
	private SQLiteDatabase mDatabase;
	private SensorDataDBHelper mHelper;

	/** Construct DB for this activity context. */
	public SensorDataDB(Context context) {
		mContext = context;
	}

	/** Opens up a connection to the database. Do this before any operations. */
	public void open() throws SQLException {
		mHelper = new SensorDataDBHelper(mContext);
		mDatabase = mHelper.getWritableDatabase();
	}

	/** Closes the database connection. Operations are not valid after this. */
	public void close() {
		mHelper.close();
		mHelper = null;
		mDatabase = null;
	}

	
	/**
	  Creates and inserts a new row using the given values.
	  Returns the rowid of the new row, or -1 on error.
	  todo: values should not include a rowid I assume.
	 */
	public long createRow(ContentValues values) {
		return mDatabase.insert(DATABASE_TABLE, null, values);
	}

	/**
	 Updates the given rowid with the given values.
	 Returns true if there was a change (i.e. the rowid was valid).
	 */
	public boolean updateRow(long rowId, ContentValues values) {
		return mDatabase.update(DATABASE_TABLE, values,
				SensorDataDB.KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 Deletes the given rowid.
	 Returns true if any rows were deleted (i.e. the id was valid).
	*/
	public boolean deleteRow(long rowId) {
		return mDatabase.delete(DATABASE_TABLE,
				SensorDataDB.KEY_ROWID + "=" + rowId, null) > 0;
	}

	
	/** Returns a cursor for all the rows. Caller should close or manage the cursor. */
	public Cursor queryAll() {
		return mDatabase.query(DATABASE_TABLE,
			KEYS_ALL,  // i.e. return all 4 columns 
			null, null, null, null,
			SensorDataDB.KEY_TIMESTAMP + " DESC"  // order-by, "DESC" for descending
		);
		
		// Could pass for third arg to filter in effect:
		// TodoDatabaseHelper.KEY_STATE + "=0"
		
		// query() is general purpose, here we show the most common usage.
	}

	/** Returns a cursor for the given row id. Caller should close or manage the cursor. */
	public Cursor query(long rowId) throws SQLException {
		Cursor cursor = mDatabase.query(true, DATABASE_TABLE,
			KEYS_ALL,
			KEY_ROWID + "=" + rowId,  // select the one row we care about
			null, null, null, null, null);
		
		// cursor starts before first -- move it to the row itself.
		cursor.moveToFirst();
		return cursor;
	}
	
	/**
	 * Converts any serializable object, namely sensorData data structure, into a byte array
	 * so it can be stored in the sqlite database.
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	public byte[] serializableObjectToByteArray(Serializable obj) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(bos);
	    oos.writeObject(obj);
	    byte[] byteArray = bos.toByteArray();
	    oos.close();
	    return byteArray;
	}
	
	/**
	 * Gets the sensorData data structure back from byte array stored in the database
	 * @param byteArray
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Map<Integer, Map<String, List<Float> > > byteArrayToSensorDataMap(byte[] byteArray) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream bis;
	    ObjectInputStream ois;
		bis = new ByteArrayInputStream(byteArray);
	    ois = new ObjectInputStream(bis);
	    Map<Integer, Map<String, List<Float> > > recoveredSensorData = (Map<Integer, Map<String, List<Float> > >) ois.readObject();
	    ois.close();
	    return recoveredSensorData;
	}

	/** Creates a ContentValues hash for our data. Pass in to create/update. */
	public ContentValues createContentValues(String title, String notes, long timestamp, byte[] data, Set<Integer> sensorTypes) {
		ContentValues values = new ContentValues();
		values.put(SensorDataDB.KEY_TITLE, title);
		values.put(SensorDataDB.KEY_NOTES, notes);
		values.put(SensorDataDB.KEY_TIMESTAMP, timestamp);
		values.put(SensorDataDB.KEY_DATA, data);
		
		String sensorTypesList = "";
		for (Integer type : sensorTypes) {
			sensorTypesList += type + " ";
		}
		sensorTypesList = sensorTypesList.trim();
		values.put(SensorDataDB.KEY_SENSOR_TYPES, sensorTypesList);
		
		return values;
	}
	
	/** Creates a ContentValues hash for updating name and notes. Pass in to create/update. */
	public ContentValues createContentValues(String title, String notes) {
		ContentValues values = new ContentValues();
		values.put(SensorDataDB.KEY_TITLE, title);
		values.put(SensorDataDB.KEY_NOTES, notes);
		
		return values;
	}
	
	// Helper for database open, create, upgrade.
	// Here written as a private inner class to TodoDB.
	private static class SensorDataDBHelper extends SQLiteOpenHelper {
		// SQL text to create table (basically just string or integer)
		private static final String DATABASE_CREATE =
			"create table " + DATABASE_TABLE + " (" +
					SensorDataDB.KEY_ROWID + " integer primary key autoincrement, " +
					SensorDataDB.KEY_TITLE + " text not null, " +
					SensorDataDB.KEY_NOTES + " text," +
					SensorDataDB.KEY_TIMESTAMP + " integer not null," +
					SensorDataDB.KEY_DATA + " blob, " +
					SensorDataDB.KEY_SENSOR_TYPES + " text not null " +
			");";
		
		public SensorDataDBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/** Creates the initial (empty) database. */
		@Override
		public void onCreate(SQLiteDatabase database) {
			database.execSQL(DATABASE_CREATE);
		}

		
		/** Called at version upgrade time, in case we want to change/migrate
		 the database structure. Here we just do nothing. */
		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			// we do nothing for this case
		}
	}
}

