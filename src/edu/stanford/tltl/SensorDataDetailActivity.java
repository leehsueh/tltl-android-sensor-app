package edu.stanford.tltl;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SensorDataDetailActivity extends Activity {
	
	public static final int DIALOG_SAVE_ID = 1;
	public static final int DIALOG_SAVE_ERROR_ID = 2;
	
	private SensorDataDB mDB;
	Long mRowId;

	private EditText mNameEditText, mNotesEditText;
	private TextView mDataSensorsTextView;
	private Button mWriteToFileButton;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setContentView(R.layout.sensor_saved_data_detail);

		mNameEditText = (EditText) findViewById(R.id.editName);
		mNotesEditText = (EditText) findViewById(R.id.editNotes);
		mDataSensorsTextView = (TextView) findViewById(R.id.dataComponents);
		mWriteToFileButton = (Button) findViewById(R.id.writeToFileButton);

		mRowId = null;

		if (bundle == null) { // initially, Intent -> extras -> rowID
			Bundle extras = getIntent().getExtras();
			if (extras != null
					&& extras.containsKey(SensorDataListActivity.EXTRA_ROWID)) {
				mRowId = extras.getLong(SensorDataListActivity.EXTRA_ROWID);
			}
		} else { // tricky: recover mRowId from kill destroy/create cycle
			mRowId = bundle.getLong(SAVE_ROW);
		}

		mDB = new SensorDataDB(this);
		mDB.open();

		dbToUI();

		mWriteToFileButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mRowId != null) {
					Cursor cursor = mDB.query(mRowId);
					byte[] sensorData = cursor.getBlob(SensorDataDB.INDEX_DATA);
					try {
						Map<Integer, Map<String, List<Float>>> recoveredSensorData = mDB
								.byteArrayToSensorDataMap(sensorData);
						
						// test out the generated string
						for (Integer sensorType : recoveredSensorData.keySet()) {
							//System.out.println(generateTextDataForSensor(sensorType, recoveredSensorData.get(sensorType)));
							boolean success = writeDataToFile(sensorType, recoveredSensorData.get(sensorType), cursor.getLong(SensorDataDB.INDEX_TIMESTAMP));
							if (!success) {
								showDialog(DIALOG_SAVE_ERROR_ID);
								cursor.close();
								return;
							}
						}
						showDialog(DIALOG_SAVE_ID);
					} catch (Exception e) {
						showDialog(DIALOG_SAVE_ERROR_ID);
					} finally {
						cursor.close();
					}
				}
			}
		});
		
		Button button = (Button) findViewById(R.id.okButton);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				finish(); // same as "back" .. either way we get onPause() to
							// save
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

			// Could use cursor.getColumnIndex(columnName) to look up 0, 1, ...
			// index
			// for each column name. Here use INDEX_ consts from TodoDB.
			mNameEditText.setText(cursor.getString(SensorDataDB.INDEX_TITLE));
			mNotesEditText.setText(cursor.getString(SensorDataDB.INDEX_NOTES));
			byte[] sensorData = cursor.getBlob(SensorDataDB.INDEX_DATA);

			try {
				Map<Integer, Map<String, List<Float>>> recoveredSensorData = mDB
						.byteArrayToSensorDataMap(sensorData);

				// display number of data points for recorded for each sensor
				// component
				mDataSensorsTextView.setText("");
				for (Map.Entry<Integer, Map<String, List<Float>>> e : recoveredSensorData
						.entrySet()) {
					for (Map.Entry<String, List<Float>> component : e
							.getValue().entrySet()) {
						String text = mDataSensorsTextView.getText().toString();
						text += "\n"
								+ MainMenuActivity.getSensorTypeToName().get(
										e.getKey()) + " " + component.getKey()
								+ ": " + component.getValue().size()
								+ " points";
						mDataSensorsTextView.setText(text);

					}
				}
				
				
			} catch (Exception e) {
				mDataSensorsTextView
						.setText("Error retrieving sensor data components");
				
			} finally {
				cursor.close();
			}
		}
	}

	/**
	 * Generates a CSV string of data from a sensor and its components
	 * @param sensorType
	 * @param dataForSensor
	 * @return
	 */
	public String generateTextDataForSensor(int sensorType,
			Map<String, List<Float>> dataForSensor) {
		// start to generate text of data with column names
		StringBuffer sensorDataString = new StringBuffer();
		sensorDataString
				.append("Data from "
						+ MainMenuActivity.getSensorTypeToName()
								.get(sensorType) + "\n");
		int numberOfPoints = 0;
		for (String componentName : dataForSensor.keySet()) {
			numberOfPoints = dataForSensor.get(componentName).size();
			sensorDataString.append(componentName + ",");
		}
		// remove last comma
		sensorDataString.deleteCharAt(sensorDataString.length() - 1);
		sensorDataString.append("\n");

		for (int i = 0; i < numberOfPoints; i++) {
			for (String componentName : dataForSensor.keySet()) {
				List<Float> data = dataForSensor.get(componentName);
				sensorDataString.append(data.get(i) + ",");
			}
			sensorDataString.deleteCharAt(sensorDataString.length() - 1);
			sensorDataString.append("\n");
		}

		return sensorDataString.toString();
	}
	
	/**
	 * Method that writes sensor data to file
	 * @param sensorType
	 * @param dataForSensor
	 * @param timestamp
	 * @return
	 */
	private boolean writeDataToFile(int sensorType, Map<String, List<Float>> dataForSensor, long timestamp) {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
				
		if (mExternalStorageWriteable) {
			String csvData = generateTextDataForSensor(sensorType, dataForSensor);
			
			// generate filename based on time stamp and sensor
			String fileName = timestamp + "_" + MainMenuActivity.getSensorTypeToName().get(sensorType);
			
			// get filepath of where to write the file, creating a dir if needed
			String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + MainMenuActivity.DATA_DIR + "/";
			File dir = new File(filePath);
			if (!dir.exists()) {
				boolean success = dir.mkdir();
				if (!success) {
					Log.v(MainMenuActivity.LOG_TAG, "Could not create directiory " + dir.getPath());
					return false;
				}
			}
			Log.v(MainMenuActivity.LOG_TAG, filePath);
			
			// write the file
		    try {
		    	FileOutputStream out = new FileOutputStream(filePath + fileName);
		    	out.write(csvData.getBytes());
		    	out.close();
		    	Log.v(MainMenuActivity.LOG_TAG, "File saved in " + getFilesDir().getAbsolutePath());
		    	return true;
		    } catch (Exception e) {
		    	Log.v(MainMenuActivity.LOG_TAG, "Error writing data! " + e.getMessage());
		    }
		}
		
		return false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		save();
	}

	/**
	 * Save the state in the UI to the database, creating a new row or updating
	 * an existing row.
	 */
	private void save() {
		String title = mNameEditText.getText().toString();
		String notes = mNotesEditText.getText().toString();

		// Not null = edit of existing row, or it's new but we saved it
		// previously,
		// so now it has a rowId anyway.
		if (mRowId != null) {
			mDB.updateRow(mRowId, mDB.createContentValues(title, notes));
		} else {
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
	
	/* dialog stuff */
	@Override
	protected Dialog onCreateDialog(int id) {
		Context mContext = getApplicationContext();		
		AlertDialog.Builder builder;
		AlertDialog alertDialog;
		builder = new AlertDialog.Builder(this);
		
		switch(id) {
	    case DIALOG_SAVE_ID:
	    	builder.setMessage("File(s) created in " + MainMenuActivity.DATA_DIR + "!");
	    	builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
	    	alertDialog = builder.create();

			break;
	    case DIALOG_SAVE_ERROR_ID:
	    	builder.setMessage("There was an error creating the files!");
	    	builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
	    	alertDialog = builder.create();
	    	break;
	    default:
	        alertDialog = null;
	    }
		
		return alertDialog;
	}
}
