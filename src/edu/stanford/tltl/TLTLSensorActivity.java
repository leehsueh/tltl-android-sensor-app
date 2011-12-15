package edu.stanford.tltl;


import android.app.Activity;

// for hardware
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
// end for hardware

// for simulator
//import org.openintents.sensorsimulator.hardware.Sensor;
//import org.openintents.sensorsimulator.hardware.SensorEvent;
//import org.openintents.sensorsimulator.hardware.SensorEventListener;
import org.openintents.sensorsimulator.hardware.SensorManagerSimulator;
// end for simulator

import android.os.Bundle;
import android.widget.TextView;

public class TLTLSensorActivity extends Activity implements SensorEventListener, org.openintents.sensorsimulator.hardware.SensorEventListener {
	boolean mUserSimulator = false;
	int mSensorSamplingRate = SensorManager.SENSOR_DELAY_UI;

	// for simulator
	SensorManagerSimulator mSensorManagerSimulator = null;

	// for hardware
	SensorManager mSensorManager = null;
	
	// UI components
	TextView tv_xAccel, tv_yAccel, tv_zAccel;	// accelerometer
	TextView tv_xGravity, tv_yGravity, tv_zGravity;	// gravity sensor
	TextView tv_xGyro, tv_yGyro, tv_zGyro;	// gyroscope
	TextView tv_xRotVec, tv_yRotVec, tv_zRotVec;	// rotation vector
	TextView tv_xLinAccel, tv_yLinAccel, tv_zLinAccel;	// linear acceleration
	TextView tv_xMag, tv_yMag, tv_zMag;	// magnetic field
	TextView tv_temperature, tv_proximity, tv_pressure, tv_light, tv_humidity;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensor_readings_list);
		
		// get sampling rate from main view
		Bundle extras = getIntent().getExtras();
		if (extras.containsKey(MainMenuActivity.RATE_KEY)) {
			mSensorSamplingRate = extras.getInt(MainMenuActivity.RATE_KEY);
		}

		// set up the sensor manager
		if (mUserSimulator) {
			mSensorManagerSimulator = SensorManagerSimulator.getSystemService(
					this, SENSOR_SERVICE);
			mSensorManagerSimulator.connectSimulator();
		} else {
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		}
		
		// get references to UI components
		tv_xAccel = (TextView) findViewById(R.id.xAccel);
		tv_yAccel = (TextView) findViewById(R.id.yAccel);
		tv_zAccel = (TextView) findViewById(R.id.zAccel);
		tv_xGravity = (TextView) findViewById(R.id.xGravity);
		tv_yGravity = (TextView) findViewById(R.id.yGravity);
		tv_zGravity = (TextView) findViewById(R.id.zGravity);
		tv_xGyro = (TextView) findViewById(R.id.xGyro);
		tv_yGyro = (TextView) findViewById(R.id.yGyro);
		tv_zGyro = (TextView) findViewById(R.id.zGyro);
		tv_xRotVec = (TextView) findViewById(R.id.xRotVec);
		tv_yRotVec = (TextView) findViewById(R.id.yRotVec);
		tv_zRotVec = (TextView) findViewById(R.id.zRotVec);
		tv_xLinAccel = (TextView) findViewById(R.id.xLinAccel);
		tv_yLinAccel = (TextView) findViewById(R.id.yLinAccel);
		tv_zLinAccel = (TextView) findViewById(R.id.zLinAccel);
		tv_xMag = (TextView) findViewById(R.id.xMag);
		tv_yMag = (TextView) findViewById(R.id.yMag);
		tv_zMag = (TextView) findViewById(R.id.zMag);
		tv_temperature = (TextView) findViewById(R.id.temperature);
		tv_pressure = (TextView) findViewById(R.id.pressure);
		tv_light = (TextView) findViewById(R.id.light);
		tv_proximity = (TextView) findViewById(R.id.proximity);
		tv_humidity = (TextView) findViewById(R.id.humidity);

	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (mUserSimulator) {
			int[] sensorTypes = {
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_ACCELEROMETER,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_GRAVITY,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_GYROSCOPE,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_ROTATION_VECTOR,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_MAGNETIC_FIELD,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_TEMPERATURE,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_PRESSURE,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_PROXIMITY,
					org.openintents.sensorsimulator.hardware.Sensor.TYPE_LIGHT
			};
			
			for (int sensorType : sensorTypes) {
				org.openintents.sensorsimulator.hardware.Sensor s = mSensorManagerSimulator.getDefaultSensor(sensorType);
				if (s != null) {
					boolean supported = mSensorManagerSimulator.registerListener(
							(org.openintents.sensorsimulator.hardware.SensorEventListener)this,
							s, mSensorSamplingRate);
					if (!supported) {
						System.out.println("Sensor " + sensorType + " unsupported!");
					}
				}
			}
		} else {
			int[] sensorTypes = {
					Sensor.TYPE_ACCELEROMETER,
					Sensor.TYPE_GYROSCOPE,
					Sensor.TYPE_MAGNETIC_FIELD,
					Sensor.TYPE_TEMPERATURE,
					Sensor.TYPE_PRESSURE,
					Sensor.TYPE_PROXIMITY,
					Sensor.TYPE_LIGHT
			};
			for (int sensorType : sensorTypes) {
				Sensor s = mSensorManager.getDefaultSensor(sensorType);
				if (s != null) {
					mSensorManager.registerListener(this,s, mSensorSamplingRate);
				}
			}
					
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mUserSimulator) {
			mSensorManagerSimulator.unregisterListener((org.openintents.sensorsimulator.hardware.SensorEventListener)this);
		} else {
			mSensorManager.unregisterListener(this);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			int sensorType = event.sensor.getType();
			updateSensorValues(sensorType, event.values);
		}
	}

	@Override
	/**
	 * onAccuracyChanged override for the simulator event listener
	 */
	public void onAccuracyChanged(
			org.openintents.sensorsimulator.hardware.Sensor sensor, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	/**
	 * onSensorChanged override for the simulator event listener
	 */
	public void onSensorChanged(
			org.openintents.sensorsimulator.hardware.SensorEvent event) {
		synchronized (this) {
			int sensorType = event.type;
			updateSensorValues(sensorType, event.values);
		}
		
	}
	
	private void updateSensorValues(int sensorType, float[] values) {
		switch (sensorType) {
//		case Sensor.TYPE_GRAVITY:
//			tv_xGravity.setText(Float.toString(values[0]));
//			tv_yGravity.setText(Float.toString(values[1]));
//			tv_zGravity.setText(Float.toString(values[2]));
//			break;
		case Sensor.TYPE_ACCELEROMETER:
			tv_xAccel.setText(Float.toString(values[0]));
			tv_yAccel.setText(Float.toString(values[1]));
			tv_zAccel.setText(Float.toString(values[2]));
			break;
//		case Sensor.TYPE_LINEAR_ACCELERATION:
//			tv_xLinAccel.setText(Float.toString(values[0]));
//			tv_yLinAccel.setText(Float.toString(values[1]));
//			tv_zLinAccel.setText(Float.toString(values[2]));
//			break;
//		case Sensor.TYPE_ROTATION_VECTOR:
//			tv_xRotVec.setText(Float.toString(values[0]));
//			tv_yRotVec.setText(Float.toString(values[1]));
//			tv_zRotVec.setText(Float.toString(values[2]));
//			break;
		case Sensor.TYPE_GYROSCOPE:
			tv_xGyro.setText(Float.toString(values[0]));
			tv_yGyro.setText(Float.toString(values[1]));
			tv_zGyro.setText(Float.toString(values[2]));
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			tv_xMag.setText(Float.toString(values[0]));
			tv_yMag.setText(Float.toString(values[1]));
			tv_zMag.setText(Float.toString(values[2]));
			break;
		case Sensor.TYPE_PRESSURE:
			tv_pressure.setText(Float.toString(values[0]));
			break;
		case Sensor.TYPE_TEMPERATURE:
			tv_temperature.setText(Float.toString(values[0]));
			break;
		case Sensor.TYPE_LIGHT:
			tv_light.setText(Float.toString(values[0]));
			break;
		case Sensor.TYPE_PROXIMITY:
			tv_proximity.setText(Float.toString(values[0]));
			break;
		}
	}
}
