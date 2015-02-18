package org.dg.openAIL;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.dg.graphManager.GraphManager;
import org.dg.graphManager.wiFiMeasurement;
import org.dg.inertialSensors.InertialSensors;
import org.dg.wifi.WiFiPlaceRecognition.IdPair;
import org.dg.wifi.WifiScanner;

import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.util.Log;

// This is a class that is supposed to provide the interface for all available solutions
public class OpenAndroidIndoorLocalization {
	
	private static final String moduleLogName = "OpenAIL";
	
	// Parameters
	static boolean WiFiFeatures = true;
	
	// Inertial sensors
	public SensorManager sensorManager;
	public InertialSensors inertialSensors;
	
	// WiFi
	public WifiScanner wifiScanner;

	// Graph
	public GraphManager graphManager;
	
	// Graph update 
	private Timer updateGraphTimer = new Timer();
	
	public OpenAndroidIndoorLocalization(SensorManager sensorManager, WifiManager wifiManager) {
		
		// Init graph
		graphManager = new GraphManager();

		// Init Sensor Managers
		inertialSensors = new InertialSensors(sensorManager);

		// Init WiFi
		wifiScanner = new WifiScanner(wifiManager);
		
	}
	
	public void startGraph() {
		graphManager.start();
		wifiScanner.startNewPlaceRecognitionThread();
		
		updateGraphTimer.scheduleAtFixedRate(new UpdateGraph(),
				1000, 200);
	}
	
	
	public void stopAndOptimizeGraph() {
		updateGraphTimer.cancel();
		wifiScanner.stopNewPlaceRecognitionThread();
		
		graphManager.stop();
		graphManager.optimize(100);
	}
	
	
	
	class UpdateGraph extends TimerTask {
		public void run() {

			 // get distance
			 double distance = inertialSensors.getGraphStepDistance();
			
			 // Adding WiFi measurement
			 if ( distance > 0.01 )
			 {
				 // get angle from our estimation
				 // TODO: RIGHT NOW WE USE ANDROID ORIENTATION
				 float yawZ = inertialSensors.getYawForStepometer();
				  
				 // We need to change yawZ into radians and change direction
				 yawZ = (float) (-yawZ * Math.PI / 180.0f);
				 
				 graphManager.addStepometerMeasurement(distance, yawZ);
			 }
			
			 // Check if there is new measurement
			 if ( wifiScanner.isNewMeasurement() )
			 {
				 // New measurement had been read
				 wifiScanner.newMeasurement(false);
				 
				 // WiFi place recognition
				 int currentPoseId = graphManager.getCurrentPoseId();
				 wifiScanner.setGraphPoseId(currentPoseId);			 
				 
				 // Find all matching places
//				 List<Integer> placesIds =  wifiScanner.returnAllMatchingPlacesToLastScan();
//				 for (Integer id : placesIds)
//					 Log.d("Graph", "WiFi fingerprint - matching id (current " + currentPoseId + "): " + id);
				 List<IdPair<Integer, Integer>> recognizedPlaces = wifiScanner.getRecognizedPlacesList();
				 Log.d(moduleLogName, "The placeRecognition thread found " + recognizedPlaces.size() + " connections");
				 
				 // Add new pose to search
				 wifiScanner.addLastScanToRecognition(currentPoseId);
				 
				 
				 // Add found results to the final graph
//				 graphManager.addMultipleWiFiFingerprints(placesIds);
				 graphManager.addMultipleWiFiFingerprints(recognizedPlaces);
				 
				 
				 //TODO!!!!!!
				 // TEMPORAILY DISABLED OPTIMIZATION WITH WIFI FEATURES (BUT STILL SAVE TO FILE)
				 if (WiFiFeatures)
				 {
					 // Adding WiFi measurements
					 List<wiFiMeasurement> wifiList = wifiScanner.getGraphWiFiList();
					 if ( wifiList != null)
						 graphManager.addMultipleWiFiMeasurements(wifiList);
				 }
			 }

		}

	}
	
	
}
