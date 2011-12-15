package edu.stanford.tltl;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SensorDataDetailActivity extends Activity {
	private SensorDataDB mDB;
	Long mRowId;
	
	private EditText mNameEditText, mNotesEditText;
	private TextView mDataSensorsTextView;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setContentView(R.layout.sensor_saved_data_detail);
		
		mNameEditText = (EditText) findViewById(R.id.editName);
		mNotesEditText = (EditText) findViewById(R.id.editNotes);
		mDataSensorsTextView = (TextView) findViewById(R.id.dataComponents);

		mRowId = null;

		if (bundle == null) {  // initially, Intent -> extras -> rowID
			Bundle extras = getIntent().getExtras();
			if (extras != null && extras.containsKey(SensorDataListActivity.EXTRA_ROWID)) {
				mRowId = extras.getLong(SensorDataListActivity.EXTRA_ROWID);
			}
		}
		else {  // tricky: recover mRowId from kill destroy/create cycle
			mRowId = bundle.getLong(SAVE_ROW);
		}
		
		mDB = new SensorDataDB(this);
		mDB.open();

		dbToUI();
		
		Button button = (Button) findViewById(R.id.okButton);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish();  // same as "back" .. either way we get onPause() to save
			}
		});
		
	}
	
	// note: put this next to onCreate, to remember to balance things
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDB.close();
	}

	// Copies database state up to the UI.
	private void dbToUI() {
		if (mRowId != null) {
			Cursor cursor = mDB.query(mRowId);
			// Note: a cursor should be closed after use, or "managed".
			
			// Could use cursor.getColumnIndex(columnName) to look up 0, 1, ... index
			// for each column name. Here use INDEX_ consts from TodoDB.
			mNameEditText.setText(cursor.getString(SensorDataDB.INDEX_TITLE));
			mNotesEditText.setText(cursor.getString(SensorDataDB.INDEX_NOTES));
			byte[] sensorData = cursor.getBlob(SensorDataDB.INDEX_DATA);
			
			try {
				Map<Integer, Map<String, List<Float>>> recoveredSensorData = mDB.byteArrayToSensorDataMap(sensorData);
				
				// display number of data points for recorded for each sensor component
				mDataSensorsTextView.setText("");
				for (Map.Entry<Integer,Map<String,List<Float>>> e : recoveredSensorData.entrySet()) {
					for (Map.Entry<String, List<Float>> component : e.getValue().entrySet()) {
						Log.v(MainMenuActivity.LOG_TAG, MainMenuActivity.getSensorTypeToName().get(e.getKey()) + " " + component.getKey());
						String text = mDataSensorsTextView.getText().toString();
						text += "\n" + MainMenuActivity.getSensorTypeToName().get(e.getKey()) + " " + component.getKey() + ": " + component.getValue().size() + " points";
						mDataSensorsTextView.setText(text);
					}
				}
				
				cursor.close();
			} catch (Exception e) {
				mDataSensorsTextView.setText("Error retrieving sensor data components");
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		save();
	}


	/** Save the state in the UI to the database, creating a new row or updating
	 * an existing row.
	 */
	private void save() {
		String title = mNameEditText.getText().toString();
		String notes = mNotesEditText.getText().toString();

		// Not null = edit of existing row, or it's new but we saved it previously,
		// so now it has a rowId anyway.
		if (mRowId != null) {
			mDB.updateRow(mRowId, mDB.createContentValues(title, notes));
		}
		else {
			mRowId = mDB.createRow(mDB.createContentValues(title, notes));
		}
	}

	// Tricky: preserve mRowId var when this activity is killed.
	// Note that the UI state is all saved automatically, so we just have to
	// save mRowID. See code in onCreate() that matches this save.
	public static final String SAVE_ROW = "saverow";
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mRowId != null) {
			outState.putLong(SAVE_ROW, mRowId);
		}
	}
}
