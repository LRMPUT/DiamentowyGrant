package org.dg.openAIL;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.dg.camera.VisualPlaceRecognition;
import org.dg.graphManager.GraphManager;
import org.dg.graphManager.wiFiMeasurement;
import org.dg.inertialSensors.InertialSensors;
import org.dg.wifi.WiFiPlaceRecognition.IdPair;
import org.dg.wifi.MyScanResult;
import org.dg.wifi.WifiScanner;
import org.xmlpull.v1.XmlPullParserException;

import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

// This is a class that is supposed to provide the interface for all available solutions
public class OpenAndroidIndoorLocalization {
	
	
	
	ConfigurationReader.Parameters parameters;
	

	private static final String moduleLogName = "OpenAIL";

	// Parameters
	static boolean WiFiFeatures = true;

	// Inertial sensors
	public SensorManager sensorManager;
	public InertialSensors inertialSensors;

	// WiFi
	public WifiScanner wifiScanner;
	int detectWiFiIssue;
	
	// Visual Place Recognition
	public VisualPlaceRecognition visualPlaceRecognition;

	// Graph
	public GraphManager graphManager;

	// Graph update
	private Timer updateGraphTimer = new Timer();

	public OpenAndroidIndoorLocalization(SensorManager sensorManager,
			WifiManager wifiManager) {

		Log.d(moduleLogName, "XxX");
		
		String configFileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL"
				+ "/settings.xml");
		
		Log.d(moduleLogName, "XxX2");
		ConfigurationReader configReader = new ConfigurationReader();
		
		Log.d(moduleLogName, "XxX3");
		try {
			parameters = configReader.readParameters(configFileName);
		} catch (XmlPullParserException e) {
			Log.e(moduleLogName, "Failed to parse the config file");
		} catch (IOException e) {
			Log.e(moduleLogName, "Missing config file");
		}
		Log.d(moduleLogName, "XxX4");
		
		Log.d(moduleLogName, "TEST: " + parameters.inertialSensors.stepometer);
		
		// Init graph
		graphManager = new GraphManager();

		// Init Sensor Managers
		inertialSensors = new InertialSensors(sensorManager);

		// Init WiFi
		wifiScanner = new WifiScanner(wifiManager);
		
	}
	
	public void initAfterOpenCV() {
		Log.d(moduleLogName, "Initializing OpenAIL submodules after OpenCV initialization");
		
		// Init Visual Place Recognition
		visualPlaceRecognition = new VisualPlaceRecognition();
	}

	public void startGraph() {
		graphManager.start();
		
		// Load WiFi place recognition database
		loadWiFiPlaceDatabase();
		
		wifiScanner.startNewPlaceRecognitionThread();

		updateGraphTimer.scheduleAtFixedRate(new UpdateGraph(), 1000, 200);
		detectWiFiIssue = 0;
	}

	public void stopAndOptimizeGraph() {
		updateGraphTimer.cancel();
		wifiScanner.stopNewPlaceRecognitionThread();

		graphManager.stop();
		graphManager.optimize(100);
	}

	private void loadWiFiPlaceDatabase() {
		// Reading database
		String readFileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/DG"
				+ "/WiFiPlace.database");

		try {
			Scanner placeDatabaseScanner = new Scanner(new BufferedReader(
					new FileReader(readFileName)));

			while (placeDatabaseScanner.hasNext()) {
				int id = placeDatabaseScanner.nextInt();
				float X = placeDatabaseScanner.nextFloat();
				float Y = placeDatabaseScanner.nextFloat();
				float Z = placeDatabaseScanner.nextFloat();
				int wifiCount = placeDatabaseScanner.nextInt();

				Log.d(moduleLogName, "WiFiDatabase 1st line: " + id + " " + X + " " + Y
						+ " " + Z + " " + wifiCount);
				String dummy = placeDatabaseScanner.nextLine();

				List<MyScanResult> wifiScan = new ArrayList<MyScanResult>();
				for (int i = 0; i < wifiCount; i++) {
					String line = placeDatabaseScanner.nextLine();
					String[] values = line.split("\\s+");

					String BSSID = values[0];
					int level = Integer.parseInt(values[3]);
					MyScanResult scan = new MyScanResult(BSSID, level);
					wifiScan.add(scan);
					
					Log.d(moduleLogName, "WiFiDatabase data: " + BSSID + " " + level);
				}

				// Adding to graph
				graphManager.addWiFiPlaceRecognitionVertex(id, X, Y, Z);

				// Add new scan to datbase
				wifiScanner.addScanToRecognition(id, wifiScan);
			}
			placeDatabaseScanner.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class UpdateGraph extends TimerTask {
		public void run() {

			// get distance
			double distance = inertialSensors.getGraphStepDistance();

			// Adding WiFi measurement
			if (distance > 0.01) {
				// get angle from our estimation
				// TODO: RIGHT NOW WE USE ANDROID ORIENTATION
				float yawZ = inertialSensors.getYawForStepometer();

				// We need to change yawZ into radians and change direction
				yawZ = (float) (-yawZ * Math.PI / 180.0f);

				graphManager.addStepometerMeasurement(distance, yawZ);
			}

			// Check if there is new measurement
			if (wifiScanner.isNewMeasurement()) {
				// All ok 
				detectWiFiIssue = 0;
				
				// New measurement had been read
				wifiScanner.newMeasurement(false);

				// WiFi place recognition
				int currentPoseId = graphManager.getCurrentPoseId();
				wifiScanner.setGraphPoseId(currentPoseId);

				// Find all matching places
				// List<Integer> placesIds =
				// wifiScanner.returnAllMatchingPlacesToLastScan();
				// for (Integer id : placesIds)
				// Log.d("Graph", "WiFi fingerprint - matching id (current " +
				// currentPoseId + "): " + id);
				List<IdPair<Integer, Integer>> recognizedPlaces = wifiScanner
						.getRecognizedPlacesList();
				Log.d(moduleLogName, "The placeRecognition thread found "
						+ recognizedPlaces.size() + " connections");

				// Add new pose to search
				wifiScanner.addLastScanToRecognition(currentPoseId);

				// Add found results to the final graph
				// graphManager.addMultipleWiFiFingerprints(placesIds);
				graphManager.addMultipleWiFiFingerprints(recognizedPlaces);

				// TODO!!!!!!
				// TEMPORAILY DISABLED OPTIMIZATION WITH WIFI FEATURES (BUT
				// STILL SAVE TO FILE)
				if (WiFiFeatures) {
					// Adding WiFi measurements
					List<wiFiMeasurement> wifiList = wifiScanner
							.getGraphWiFiList();
					if (wifiList != null)
						graphManager.addMultipleWiFiMeasurements(wifiList);
				}
			} else if (wifiScanner.getRunningState()) {

				detectWiFiIssue++;

				// Bad scan received - need to restart
				if (detectWiFiIssue > 40) {
					Log.d(moduleLogName,
							"Restarting WiFi due to continuous scan issue");
					wifiScanner.startScanning();
				}

			}

		}

	}

}
