package org.dg.inertialSensors;

import android.hardware.Sensor;

public class MySensorEvent {
	public long timestamp;
	public float[] values;
	public int sensorType;
	
	public MySensorEvent(long _timestamp, int _sensorType, float[] _values ) {
		timestamp = _timestamp;
		sensorType = _sensorType;
		values = _values.clone();
	}
}
