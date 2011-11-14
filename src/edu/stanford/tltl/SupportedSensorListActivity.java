package edu.stanford.tltl;

import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

//for hardware
import android.hardware.Sensor;
import android.hardware.SensorManager;
//end for hardware

//for simulator
//import org.openintents.sensorsimulator.hardware.Sensor;
//import org.openintents.sensorsimulator.hardware.SensorEvent;
//import org.openintents.sensorsimulator.hardware.SensorEventListener;
import org.openintents.sensorsimulator.hardware.SensorManagerSimulator;

public class SupportedSensorListActivity extends ListActivity {
	private SensorManager mSensorManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
		String[] sensorNames = new String[sensorList.size()];
		for (int i = 0; i < sensorList.size(); i++) {
			System.out.println(sensorList.get(i).getName());
			sensorNames[i] = sensorList.get(i).getName();
		}
		
		ListAdapter listAdapter = new ArrayAdapter<String>(this, R.layout.sensor_list_item, sensorNames);
		setListAdapter(listAdapter);
		ListView lv = (ListView) findViewById(R.id.supportedSensorList);
	}
}
