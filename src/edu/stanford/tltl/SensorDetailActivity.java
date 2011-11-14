package edu.stanford.tltl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SensorDetailActivity extends ListActivity {
	// map keys
	public static final String SENSOR_COMPONENT_NAME = "SENSOR_COMPONENT_NAME";
	public static final String REC_SENSOR_COMPONENT = "REC_SENSOR_COMPONENT";
	public static final String PREF_KEY = "PREF_KEY";
	
	private List<Map<String, Object>> valuesList;
	
	private void initializeValuesList() {
		if (valuesList == null) {
			valuesList = new LinkedList<Map<String, Object>>();
			if (mSensorType == Sensor.TYPE_ACCELEROMETER) {
				addEntry(MainMenuActivity.REC_ACCELEROMETER_X, "X-axis");
				addEntry(MainMenuActivity.REC_ACCELEROMETER_Y, "Y-axis");
				addEntry(MainMenuActivity.REC_ACCELEROMETER_Z, "Z-axis");
			} else if (mSensorType == Sensor.TYPE_MAGNETIC_FIELD) {
				addEntry(MainMenuActivity.REC_MAGNETIC_X, "X-axis");
				addEntry(MainMenuActivity.REC_MAGNETIC_Y, "Y-axis");
				addEntry(MainMenuActivity.REC_MAGNETIC_Z, "Z-axis");
			} else if (mSensorType == Sensor.TYPE_TEMPERATURE) {
				addEntry(MainMenuActivity.REC_TEMPERATURE, "Temperature");
			} else if (mSensorType == Sensor.TYPE_LIGHT) {
				addEntry(MainMenuActivity.REC_AMBIENT_LIGHT, "Ambient Light");
			} else if (mSensorType == Sensor.TYPE_PROXIMITY) {
				addEntry(MainMenuActivity.REC_PROXIMITY, "Proximity");
			}
		}
	}
	
	private void addEntry(String prefKey, String componentName) {
		SharedPreferences settings = getSharedPreferences(MainMenuActivity.PREFS_NAME, 0);
		Map<String, Object> entry = new HashMap<String, Object>(2);
		entry.put(PREF_KEY, prefKey);
		entry.put(SENSOR_COMPONENT_NAME, componentName);
		entry.put(REC_SENSOR_COMPONENT, settings.getBoolean(prefKey, false));
		valuesList.add(entry);
	}
	private String mSensorName;
	private int mSensorType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensor_components_list);
		
		Bundle extras = getIntent().getExtras();
		if (extras.containsKey(MainMenuActivity.SENSOR_TYPE)) {
			mSensorType = extras.getInt(MainMenuActivity.SENSOR_TYPE);
			mSensorName = extras.getString(MainMenuActivity.SENSOR_NAME);
			TextView v = (TextView)findViewById(R.id.sensorName);
			v.setText(mSensorName);
			initializeValuesList();
		} else {
			// TODO: show error, go back
		}
		
		// create the grid item mapping
        String[] from = new String[] {SENSOR_COMPONENT_NAME, REC_SENSOR_COMPONENT};
		int[] to = new int[] { R.id.sensorComponentName, R.id.recordCheckBox };
		SimpleAdapter simpleAdapter = new SimpleAdapter(this, valuesList, R.layout.sensor_component_list_item, from, to);
		setListAdapter(simpleAdapter);
		
		Button cancelButton = (Button) findViewById(R.id.cancelButton);	// go back
		cancelButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		Button doneButton = (Button) findViewById(R.id.doneButton);	// save and go back
		doneButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				finish();
			}
		});
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long rowId) {
		super.onListItemClick(l, v, position, rowId);
		Log.v(MainMenuActivity.LOG_TAG, "Row " + position + " was tapped");
		CheckBox c = (CheckBox) v.findViewById(R.id.recordCheckBox);
		c.toggle();
		
		// write change
		String prefKey = valuesList.get(position).get(PREF_KEY).toString();
		SharedPreferences settings = getSharedPreferences(MainMenuActivity.PREFS_NAME, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putBoolean(prefKey, c.isChecked());
	    editor.commit();
	}
	
}
