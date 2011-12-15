package edu.stanford.tltl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainMenuActivity extends ListActivity {
	public static final String LOG_TAG = "TLTL Sensor App";
	public static final String RATE_KEY = "RATE";
	public static final String PREFS_NAME = "RECORD_SETTINGS";
	public static final String SENSOR_TYPE = "SENSOR_TYPE";
	public static final String SENSOR_NAME = "SENSOR_NAME";
	public static final String REC_ACCELEROMETER_X = "REC_ACCELEROMETER_X";
	public static final String REC_ACCELEROMETER_Y = "REC_ACCELEROMETER_Y";
	public static final String REC_ACCELEROMETER_Z = "REC_ACCELEROMETER_Z";
	public static final String REC_GYROSCOPE_X = "REC_GYROSCOPE_X";
	public static final String REC_GYROSCOPE_Y = "REC_GYROSCOPE_Y";
	public static final String REC_GYROSCOPE_Z = "REC_GYROSCOPE_Z";
	public static final String REC_MAGNETIC_X = "REC_MAGNETIC_X";
	public static final String REC_MAGNETIC_Y = "REC_MAGNETIC_Y";
	public static final String REC_MAGNETIC_Z = "REC_MAGNETIC_Z";
	public static final String REC_TEMPERATURE = "REC_TEMPERATURE";
	public static final String REC_PRESSURE = "REC_PRESSURE";
	public static final String REC_PROXIMITY = "REC_PROXIMITY";
	public static final String REC_AMBIENT_LIGHT = "REC_AMBIENT_LIGHT";
	
	// directory where data files can be stored
	public static final String DATA_DIR = "TLTL_Sensor_Data";
	
	
	public static final int[] SAMPLE_RATES = {
		SensorManager.SENSOR_DELAY_UI,
		SensorManager.SENSOR_DELAY_NORMAL,
		SensorManager.SENSOR_DELAY_GAME,
		SensorManager.SENSOR_DELAY_FASTEST
	};
	
	private static Map<Integer, String> sensorTypesToNames;
	public static Map<Integer, String> getSensorTypeToName() {
		if (sensorTypesToNames == null) {
			sensorTypesToNames = new HashMap<Integer, String>();
			sensorTypesToNames.put(Sensor.TYPE_ACCELEROMETER, "Accelerometer");
			sensorTypesToNames.put(Sensor.TYPE_GYROSCOPE, "Gyroscope");
			sensorTypesToNames.put(Sensor.TYPE_LIGHT, "Light");
			sensorTypesToNames.put(Sensor.TYPE_MAGNETIC_FIELD, "Magnetic Field");
			sensorTypesToNames.put(Sensor.TYPE_ORIENTATION, "Orientation");
			sensorTypesToNames.put(Sensor.TYPE_PRESSURE, "Pressure");
			sensorTypesToNames.put(Sensor.TYPE_PROXIMITY, "Proximity");
			sensorTypesToNames.put(Sensor.TYPE_TEMPERATURE, "Temperature");
		}
		return sensorTypesToNames;
	}
	
	private int mSampleRate = SAMPLE_RATES[0];
	
	ListView mSensorList;
	RadioGroup mSampleRateRadioGroup;
	SeekBar mSampleSlider;
	Button mListOutputsButton, mRecordButton, mListDataButton;
	
	private String[] sensorNames;
	private int[] sensorTypes;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
		sensorNames = new String[sensorList.size()];
		sensorTypes = new int[sensorList.size()];
		for (int i = 0; i < sensorList.size(); i++) {
			Log.v(LOG_TAG, sensorList.get(i).getName());
			sensorNames[i] = sensorList.get(i).getName();
			sensorTypes[i] = sensorList.get(i).getType();
		}
		
		ListAdapter listAdapter = new ArrayAdapter<String>(this, R.layout.sensor_list_item, sensorNames);
		setListAdapter(listAdapter);
		mSampleSlider = (SeekBar) findViewById(R.id.seekBar1);
		mSampleSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				Log.v(LOG_TAG, "Seek bar progress: " + seekBar.getProgress());
				setmSampleRate(seekBar.getProgress());
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				TextView v = (TextView) findViewById(R.id.rateText);
				switch (progress) {
				case 0:
					v.setText("Slow");
					break;
				case 1:
					v.setText("Normal");
					break;
				case 2:
					v.setText("Fast");
					break;
				case 3:
					v.setText("Fastest");
					break;
				default:
					v.setText("Rate");
				}
			}
		});
		
		mListDataButton = (Button) findViewById(R.id.listDataButton);
		mListDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainMenuActivity.this, SensorDataListActivity.class);
				startActivity(intent);
			}
		});
		
		mListOutputsButton = (Button) findViewById(R.id.button2);
		mListOutputsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainMenuActivity.this, TLTLSensorActivity.class);
				intent.putExtra(RATE_KEY, mSampleRate);
				startActivity(intent);
			}
		});
		mRecordButton = (Button) findViewById(R.id.button3);
		mRecordButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.v(MainMenuActivity.LOG_TAG, "Record button presed!");
				Intent intent = new Intent(MainMenuActivity.this, SensorRecordActivity.class);
				intent.putExtra(RATE_KEY, mSampleRate);
				startActivity(intent);
			}
		});
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long rowId) {
		super.onListItemClick(l, v, position, rowId);
		Log.v(MainMenuActivity.LOG_TAG, "Row " + position + " was tapped");
		Intent intent = new Intent(MainMenuActivity.this, SensorDetailActivity.class);
		intent.putExtra(SENSOR_NAME, getSensorTypeToName().get(sensorTypes[position]));
		intent.putExtra(SENSOR_TYPE, sensorTypes[position]);
		startActivity(intent);
	}

	public int getmSampleRate() {
		return mSampleRate;
	}

	public void setmSampleRate(int mSampleRate) {
		if (mSampleRate >= SAMPLE_RATES.length) {
			mSampleRate = SAMPLE_RATES.length-1;
		}
		if (mSampleRate < 0) {
			mSampleRate = 0;
		}
		this.mSampleRate = SAMPLE_RATES[mSampleRate];
		TextView v = (TextView) findViewById(R.id.rateText);
		switch (mSampleRate) {
		case 0:
			v.setText("Slow");
			break;
		case 1:
			v.setText("Normal");
			break;
		case 2:
			v.setText("Fast");
			break;
		case 3:
			v.setText("Fastest");
			break;
		default:
			v.setText("Rate");
		}
	}
}
