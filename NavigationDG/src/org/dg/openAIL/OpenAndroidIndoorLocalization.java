package org.dg.openAIL;

import org.dg.graphManager.GraphManager;
import org.dg.inertialSensors.InertialSensors;
import org.dg.wifi.WifiScanner;

import android.hardware.SensorManager;

// This is a class that is supposed to provide the interface for all available solutions
public class OpenAndroidIndoorLocalization {
	
	// Inertial sensors
	public SensorManager sensorManager;
	public InertialSensors inertialSensors;
	
	// WiFi
	public WifiScanner wifiScanner;

	// Graph
	public GraphManager graphManager;
	
	
}
