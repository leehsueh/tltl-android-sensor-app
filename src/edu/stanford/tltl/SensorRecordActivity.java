package edu.stanford.tltl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SensorRecordActivity extends Activity implements SensorEventListener  {
	public static final String TIME_KEY = "Time";
	public static final String[] COMPONENT_KEYS = {"Values[0]", "Values[1]", "Values[2]"};
	public static final int DIALOG_SAVE_ID = 1;
	public static final int DIALOG_SAVE_ERROR_ID = 2;
	
	private ListView mParamsToRecord;
	private Map<Integer, Map<String, List<Float> > > sensorData;	// sensor type maps to another map where "Time" -> time values; "SensorComponent" -> sensor values
	private Map<Integer, ArrayList<Integer>> componentsToRecord;	// sensor type maps to list of components of the sensor to record
	
	private long mStartTime, mStopTime;
	private boolean mRecording;
	private SensorManager mSensorManager;
	private int mSampleRate;
	
	private Button mStopButton, mSaveButton;
	private TextView mCountDown;
	private Chronometer mChronometer;
	
	private SensorDataDB mDB;
	
	/**
	 * Set up the sensors and initial data structures for data to be recorded
	 */
	private void setup() {
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		Map<Integer, String[]> sensorPrefKeys = new HashMap<Integer, String[]>();
		sensorPrefKeys.put(Sensor.TYPE_ACCELEROMETER, 
				new String[] {MainMenuActivity.REC_ACCELEROMETER_X,
				MainMenuActivity.REC_ACCELEROMETER_Y,
				MainMenuActivity.REC_ACCELEROMETER_Z,});
		sensorPrefKeys.put(Sensor.TYPE_GYROSCOPE, 
				new String[] {MainMenuActivity.REC_GYROSCOPE_X,
				MainMenuActivity.REC_GYROSCOPE_Y,
				MainMenuActivity.REC_GYROSCOPE_Z,});
		sensorPrefKeys.put(Sensor.TYPE_MAGNETIC_FIELD, 
				new String[] {MainMenuActivity.REC_MAGNETIC_X,
				MainMenuActivity.REC_MAGNETIC_Y,
				MainMenuActivity.REC_MAGNETIC_Z,});
		sensorPrefKeys.put(Sensor.TYPE_TEMPERATURE, 
				new String[] {MainMenuActivity.REC_TEMPERATURE});
		sensorPrefKeys.put(Sensor.TYPE_LIGHT, 
				new String[] {MainMenuActivity.REC_AMBIENT_LIGHT});
		sensorPrefKeys.put(Sensor.TYPE_PROXIMITY, 
				new String[] {MainMenuActivity.REC_PROXIMITY});
		sensorPrefKeys.put(Sensor.TYPE_PRESSURE, 
				new String[] {MainMenuActivity.REC_PRESSURE});
		
		SharedPreferences settings = getSharedPreferences(MainMenuActivity.PREFS_NAME, 0);
		int[] sensorTypes = {
				Sensor.TYPE_ACCELEROMETER,
				Sensor.TYPE_GYROSCOPE,
				Sensor.TYPE_MAGNETIC_FIELD,
				Sensor.TYPE_TEMPERATURE,
				Sensor.TYPE_PRESSURE,
				Sensor.TYPE_PROXIMITY,
				Sensor.TYPE_LIGHT
		};
		componentsToRecord = new HashMap<Integer, ArrayList<Integer>>();
		sensorData = new HashMap<Integer, Map<String, List<Float> >>();
		for (int sensorType : sensorTypes) {
			// look through pref to see if this sensor needs to be recorded
			boolean sensorNeeded = false;
			ArrayList<Integer> valueIndices = null;	// value index corresponds to the SensorEvent.values array
			String[] keys = sensorPrefKeys.get(sensorType);
			for (int i = 0; i < keys.length; i++) {
				boolean record = settings.getBoolean(keys[i], false);
				if (record) {
					sensorNeeded = true;
					if (valueIndices == null) valueIndices = new ArrayList<Integer>();
					valueIndices.add(i);
				}
			}
			// register this as sensor listener
			if (sensorNeeded) {
				ArrayList<Float> timeValues = new ArrayList<Float>();
				componentsToRecord.put(sensorType, valueIndices);
				
				// instantiate data structure to store data points
				// map contains sets of data indexed by a key; each set of sensor data includes time, so put that in first
				HashMap<String, List<Float>> m = new HashMap<String, List<Float>>();
				m.put(TIME_KEY, timeValues);
				for (Integer i : valueIndices) {
					m.put(COMPONENT_KEYS[i], new ArrayList<Float>());
				}
				sensorData.put(sensorType, m);
				
				Sensor s = mSensorManager.getDefaultSensor(sensorType);
				if (s != null) {
					boolean registerSuccess = mSensorManager.registerListener(this,s, mSampleRate);
					if (registerSuccess) {
						Log.v(MainMenuActivity.LOG_TAG, "Sensor " + s.getName() + " registered.");
					}
				} else {
					//TODO: handle this scenario (should never happen in theory)
				}
			}
		}
	}
	
	/* Activity lifecycle */
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record);
		
		// get the sampling rate saved from the home screen
		Bundle extras = getIntent().getExtras();
		mSampleRate = extras.getInt(MainMenuActivity.RATE_KEY);
		setup();
		
		// set up UI
		TextView tv = (TextView) findViewById(R.id.textView2);
		tv.setText("");
		for (Map.Entry<Integer, ArrayList<Integer>> e : componentsToRecord.entrySet()) {
			for (Integer i : e.getValue()) {
				Log.v(MainMenuActivity.LOG_TAG, MainMenuActivity.getSensorTypeToName().get(e.getKey()) + " " + COMPONENT_KEYS[i]);
				String text = tv.getText().toString();
				text += "\n" + MainMenuActivity.getSensorTypeToName().get(e.getKey()) + " " + COMPONENT_KEYS[i];
				tv.setText(text);
			}
		}
		
		// set up the save button
		mSaveButton = (Button) findViewById(R.id.saveButton);
		mSaveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// bring up an alert dialog to prompt user to type a name
				showDialog(DIALOG_SAVE_ID);
				
			}
		});
				
		// set up the stop button
		mStopButton = (Button) findViewById(R.id.stopButton);
		mStopButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// stop recording
				mRecording = false;
				
				// unregister sensors
				mSensorManager.unregisterListener(SensorRecordActivity.this);
				
				Log.v(MainMenuActivity.LOG_TAG, "Recording stopped.");
				
				mCountDown.setText("Done.");
				mChronometer.stop();
				mStopTime = System.nanoTime();
				
				// display number of data points for recorded for each sensor component
				TextView tv = (TextView) findViewById(R.id.textView2);
				tv.setText("");
				for (Map.Entry<Integer, ArrayList<Integer>> e : componentsToRecord.entrySet()) {
					for (Integer i : e.getValue()) {
						Log.v(MainMenuActivity.LOG_TAG, MainMenuActivity.getSensorTypeToName().get(e.getKey()) + " " + COMPONENT_KEYS[i]);
						String text = tv.getText().toString();
						text += "\n" + MainMenuActivity.getSensorTypeToName().get(e.getKey()) + " " + COMPONENT_KEYS[i] + ": " + sensorData.get(e.getKey()).get(COMPONENT_KEYS[i]).size() + " points";
						tv.setText(text);
					}
				}
				
				// hide the stop button; show the save button
				mStopButton.setVisibility(Button.INVISIBLE);
				mSaveButton.setVisibility(Button.VISIBLE);
			}
		});		
		
		// start countdown timer and chronometer timer
		mChronometer = (Chronometer) findViewById(R.id.chronometer1);
		mCountDown = (TextView) findViewById(R.id.countDown);
		new CountDownTimer(5000, 1000) {

		     public void onTick(long millisUntilFinished) {
		         mCountDown.setText("" + (millisUntilFinished / 1000 - 1));
		     }

		     public void onFinish() {
		    	 mCountDown.setText("Recording");
		    	 mStopButton.setVisibility(View.VISIBLE);
		    	 mRecording = true;
		    	 mStartTime = System.nanoTime();
		    	 
		    	 // set up chronometer for recording time
//		         mChronometer.setOnChronometerTickListener(new OnChronometerTickListener(){
//		             @Override
//		             public void onChronometerTick(Chronometer arg0) {
//		                 long countUp = (SystemClock.elapsedRealtime() - arg0.getBase()) / 1000;
//		                 String asText = (countUp / 60) + ":" + (countUp % 60); 
//		             }
//		         });
		         mChronometer.setBase(SystemClock.elapsedRealtime());
		         mChronometer.start();
		     }
		}.start();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// unregister sensors
		mSensorManager.unregisterListener(SensorRecordActivity.this);
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
	    	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.record_save_dialog,
			                               (ViewGroup) findViewById(R.id.layout_root));

			builder.setView(layout);
	    	builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.v(MainMenuActivity.LOG_TAG, "Cancel!");
					dialog.cancel();
					removeDialog(which);
				}
			});
	    	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AlertDialog ad = (AlertDialog) dialog;
					EditText nameField = (EditText) ad.findViewById(R.id.saveName);
					EditText notesField = (EditText) ad.findViewById(R.id.saveNotes);

					// save data
					Log.v(MainMenuActivity.LOG_TAG, "Save with name " + nameField.getText() + "!");
					long timestamp = System.currentTimeMillis();
					String title = nameField.getText().toString();
					String notes = notesField.getText().toString();
				    
					try {
					    // set up and open the database
					    mDB = new SensorDataDB(SensorRecordActivity.this);
					    
						// serialize sensor data into bytes
						byte[] byteArray = mDB.serializableObjectToByteArray((Serializable)sensorData);
					    
					    mDB.open();
					    
					    // add row
					    mDB.createRow(mDB.createContentValues(title, notes, timestamp, byteArray, sensorData.keySet()));
					    mDB.close();
	
					} catch (IOException ioe) {
						Log.v(MainMenuActivity.LOG_TAG, "Error in byte operations!\n" + ioe.getMessage());
						showDialog(DIALOG_SAVE_ERROR_ID);
					} finally {
						finish();
					}
				}
			});
			alertDialog = builder.create();

			break;
	    case DIALOG_SAVE_ERROR_ID:
	    	builder.setMessage("There was an error saving data!");
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
	
	/* Sensor stuff */
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	@Override
	/**
	 * onSensorChanged override for the hardware event listener
	 */
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			if (mRecording) {
				int sensorType = event.sensor.getType();
				recordPoint(sensorType, event.values, event.timestamp);
			}
		}
		
	}
	
	private void recordPoint(int sensorType, float[] values, long timestamp) {
		ArrayList<Integer> valueIndices = componentsToRecord.get(sensorType);
		sensorData.get(sensorType).get(TIME_KEY).add(Long.valueOf((timestamp - mStartTime)).floatValue()/1000000.0f);
		for (Integer i : valueIndices) {
			sensorData.get(sensorType).get(COMPONENT_KEYS[i]).add(values[i]);
		}
	}
}
