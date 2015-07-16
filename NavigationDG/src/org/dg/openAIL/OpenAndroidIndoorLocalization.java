package org.dg.openAIL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.dg.camera.Preview;
import org.dg.camera.VisualPlaceRecognition;
import org.dg.graphManager.GraphManager;
import org.dg.graphManager.Vertex;
import org.dg.graphManager.wiFiMeasurement;
import org.dg.inertialSensors.InertialSensors;
import org.dg.wifi.MyScanResult;
import org.dg.wifi.WifiScanner;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.xmlpull.v1.XmlPullParserException;

import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

// This is a class that is supposed to provide the interface for all available solutions
public class OpenAndroidIndoorLocalization {
	private static final String moduleLogName = "OpenAIL";

	// Parameters
	static boolean WiFiFeatures = true;
	ConfigurationReader.Parameters parameters;

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

	// Preview
	public Preview preview;
	
	// Value used when storing map
	int mapPointId;

	public OpenAndroidIndoorLocalization(SensorManager sensorManager,
			WifiManager wifiManager) {
		
		// Create directories if needed
		createNeededDirectories();

		// Reading settings
		parameters = readParametersFromXML("settings.xml");

		// Init graph
		graphManager = new GraphManager(parameters.graphManager);

		// Init inertial sensors
		if (parameters.inertialSensors.useModule)
			inertialSensors = new InertialSensors(sensorManager);

		// Init WiFi
		wifiScanner = new WifiScanner(wifiManager,
				parameters.wifiPlaceRecognition);
		
		// The initial id of read map point
		mapPointId = 10000;
	}

	/**
	 * 
	 */
	private void createNeededDirectories() {
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/");

		if (!folder.exists()) {
			folder.mkdir();
		}
		
		folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/PriorData/");

		if (!folder.exists()) {
			folder.mkdir();
		}
	}

	public void initAfterOpenCV() {
		Log.d(moduleLogName,
				"Initializing OpenAIL submodules after OpenCV initialization");

		// Init Visual Place Recognition
		visualPlaceRecognition = new VisualPlaceRecognition();
	}

	public void startLocalization() {
		// Creating new graph
		graphManager.start();

		// TODO: Read also images
		// Load WiFi place recognition database
		if (parameters.wifiPlaceRecognition.usePriorDatabase) {
			loadWiFiPlaceDatabase(parameters.wifiPlaceRecognition.priorDatabaseFile);
		}

		// Start WiFi recognition
		wifiScanner.startNewPlaceRecognitionThread();

		// Start FABMAP recognition
//		visualPlaceRecognition.start(_preview);
		
		// Check for new information at fixed rate
		long delay = (long) (1000.0 / parameters.mainProcessing.frequencyOfNewDataQuery);
		updateGraphTimer.scheduleAtFixedRate(new UpdateGraph(), 0, delay);
		
		// Check if there is an issue with WiFi
		detectWiFiIssue = 0;
		//.graphManager.optimize(5);
	}

	public void stopAndOptimizeGraph() {
		
		// Stop checking for new data
		updateGraphTimer.cancel();
		
		// Stop WiFi recognition thread
		wifiScanner.stopNewPlaceRecognitionThread();

		// Stop VPR thread
//		visualPlaceRecognition.stop()
		
		// Stop the optimization thread 
		graphManager.stop(); // TODO: Remove it!
		graphManager.stopOptimizationThread();
		
		// Getting the estimates
		List<Vertex> list = graphManager.getPositionsOfVertices();
	}

	private void loadWiFiPlaceDatabase(String filename) {
		// Reading database
		String readFileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/PriorData/" + filename);

		try {
			Scanner placeDatabaseScanner = new Scanner(new BufferedReader(
					new FileReader(readFileName)));
			placeDatabaseScanner.useLocale(Locale.US);

			int id = 5000;
			while (placeDatabaseScanner.hasNext()) {
				float X = placeDatabaseScanner.nextFloat();
				float Y = placeDatabaseScanner.nextFloat();
				float Z = placeDatabaseScanner.nextFloat();
				int wifiCount = placeDatabaseScanner.nextInt();

				Log.d(moduleLogName, "WiFiDatabase 1st line: " + id + " " + X
						+ " " + Y + " " + Z + " " + wifiCount);
				String dummy = placeDatabaseScanner.nextLine();

				List<MyScanResult> wifiScan = new ArrayList<MyScanResult>();
				for (int i = 0; i < wifiCount; i++) {
					String line = placeDatabaseScanner.nextLine();

					String[] values = line.split("\\t+");

					String BSSID = values[0];
					int level;
					if (values.length == 3)
						level = Integer.parseInt(values[2]);
					else
						level = Integer.parseInt(values[1]);

					MyScanResult scan = new MyScanResult(BSSID, level);
					wifiScan.add(scan);

					Log.d(moduleLogName, "WiFiDatabase data: " + BSSID + " "
							+ level);
				}

				// Adding to graph
				graphManager.addVertexWithKnownPosition(id, X, Y, Z);

				// Add new scan to datbase
				wifiScanner.addScanToRecognition(id, wifiScan);

				id++;
			}
			placeDatabaseScanner.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public void saveMapPoint(String mapName, double posX, double posY, double posZ)
	{
		// Create directory to save new Map
		String pathName = creatingSaveMapDirectory(mapName);
				
		// Getting the orientation // We need to change direction of yawZ (to match stepometer)
		float yawZ = -inertialSensors.getYawForStepometer();

		// Getting the last WiFi scan
		List<MyScanResult> wifiList = wifiScanner.getLastScan();
	
		// Getting the last image
		Mat image = preview.getCurPreviewImage();
		
		//// Saving image
		saveImageOfMapPoint(image, pathName);
		
		//// Saving WiFis
		saveWiFiScansOfMapPoint(wifiList, pathName);
		
		//// Saving pos
		savePositionsOfMapPoint(posX, posY, posZ, yawZ, pathName);		
		
		// Increase Id for next point
		mapPointId++;
	}

	/**
	 * @param mapName
	 * @return
	 */
	private String creatingSaveMapDirectory(String mapName) {
		// Setting the directory of output
		String pathName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/PriorData/" + mapName + "/");
		
		// Create directory if it doesn't exist
		File folder = new File(pathName);
		if (!folder.exists()) {
			folder.mkdir();
		}
		return pathName;
	}

	/**
	 * @param image
	 * @param pathName
	 */
	private void saveImageOfMapPoint(Mat image, String pathName) {
		// Creating directory if needed
		File folder = new File(pathName + "images/");
		if (!folder.exists()) {
			folder.mkdir();
		}

		String imagePath = pathName + "images/" + String.format("%05d.png", mapPointId);		
		Highgui.imwrite(imagePath, image);
	}

	/**
	 * @param posX
	 * @param posY
	 * @param posZ
	 * @param yawZ
	 * @param pathName
	 */
	private void savePositionsOfMapPoint(double posX, double posY, double posZ,
			float yawZ, String pathName) {
		FileOutputStream foutStream;
		PrintStream outStreamRawData;
		outStreamRawData = null;
		try {
			foutStream = new FileOutputStream(pathName + "positions.list", true);
			outStreamRawData = new PrintStream(foutStream);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		// Save positions (placeId, X, Y, Z, Yaw)
		outStreamRawData.print(mapPointId + " " + posX + " " + posY + " " + posZ + " " + yawZ + "\n");

		// Close stream
		outStreamRawData.close();
	}

	/**
	 * @param wifiList
	 * @param pathName
	 */
	private void saveWiFiScansOfMapPoint(List<MyScanResult> wifiList,
			String pathName) {
		
		// Create directory if needed
		File folder = new File(pathName + "wifiScans/");
		if (!folder.exists()) {
			folder.mkdir();
		}
		
		FileOutputStream foutStream;
		PrintStream outStreamRawData = null;
		try {
			foutStream = new FileOutputStream(pathName + "wifiScans/" + String.format("%05d.wifiscan", mapPointId), false);
			outStreamRawData = new PrintStream(foutStream);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		// Save the initial line of scan (placeId, number of WiFi networks)
		outStreamRawData.print(mapPointId + " " + wifiList.size() + "\n");

		// Save BSSID (MAC), SSID (network name), lvl (in DBm)
		for (int i = 0; i < wifiList.size(); i++) {
			MyScanResult scanResult = wifiList.get(i);
			outStreamRawData.print(scanResult.BSSID + "\t"
					+ scanResult.networkName + "\t" + scanResult.level + "\n");
		}
	}
	
	/*
	 * Method used to save last WiFi scan with wanted position (X,Y,Z) in a file
	 * that can be used as prior map
	 */
	public void saveWiFiMapPoint(double posX, double posY, double posZ,
			String fileName) {
		
		// Getting the last WiFi scan
		List<MyScanResult> wifiList = wifiScanner.getLastScan();

		// File to save results
		String pathName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/PriorData/" + fileName);
		FileOutputStream foutStream;
		PrintStream outStreamRawData = null;
		try {
			foutStream = new FileOutputStream(pathName, true);
			outStreamRawData = new PrintStream(foutStream);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Save the initial line of scan (posX, posY, posZ, number of WiFi
		// networks)
		outStreamRawData.print(posX + " " + posY + " " + posZ + " "
				+ wifiList.size() + "\n");

		// Save BSSID (MAC), SSID (network name), lvl (in DBm)
		for (int i = 0; i < wifiList.size(); i++) {
			MyScanResult scanResult = wifiList.get(i);
			outStreamRawData.print(scanResult.BSSID + "\t"
					+ scanResult.networkName + "\t" + scanResult.level + "\n");
		}

		// Close stream
		outStreamRawData.close();
	}

	class UpdateGraph extends TimerTask {
		int iterationCounter = 0;
		
		public void run() {
			Log.d(moduleLogName, "Starting query cycle");
			
			// get distance
			Log.d(moduleLogName, "Processing stepometer ...");
			double distance = inertialSensors.getGraphStepDistance();

			
			// Adding WiFi measurement
			Log.d(moduleLogName, "Processing WiFi ...");
			if (distance > 0.01) {
				// get angle from our estimation
				// TODO: RIGHT NOW WE USE ANDROID ORIENTATION
				float yawZ = inertialSensors.getYawForStepometer();

				// We need to change yawZ into radians and change direction
				yawZ = (float) (-yawZ * Math.PI / 180.0f);

				graphManager.addStepometerMeasurement(distance, yawZ);
			}

			// WiFi read all found connections
			List<IdPair<Integer, Integer>> recognizedPlaces = wifiScanner
					.getAndClearRecognizedPlacesList();
			
			Log.d(moduleLogName, "The placeRecognition thread found "
					+ recognizedPlaces.size() + " connections");
			
			// Add found results to the final graph
			// graphManager.addMultipleWiFiFingerprints(placesIds);
			graphManager.addMultipleWiFiFingerprints(recognizedPlaces);
			
			// Check if there is new measurement
			if (wifiScanner.isNewMeasurement()) {
				// All ok
				detectWiFiIssue = 0;

				// New measurement had been read
				wifiScanner.newMeasurement(false);

				// WiFi place recognition
				int currentPoseId = graphManager.getCurrentPoseId();
				wifiScanner.setGraphPoseId(currentPoseId);

				// Add new pose to search
				wifiScanner.addLastScanToRecognition(currentPoseId);

				// TODO!!!!!!
				// TEMPORAILY DISABLED OPTIMIZATION WITH WIFI FEATURES (BUT
				// STILL SAVE TO FILE)
				if (WiFiFeatures) {
					// Adding WiFi measurements
					List<wiFiMeasurement> wifiList = wifiScanner
							.getGraphWiFiList();
					// if (wifiList != null)
					// graphManager.addMultipleWiFiMeasurements(wifiList);
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
			
			// Save current image
			Log.d(moduleLogName, "Processing camera ...");
			if ( preview != null && iterationCounter % 5 == 0)
			{
				Log.d(moduleLogName, "Getting and saving camera image");
				Mat image = preview.getCurPreviewImage();
				visualPlaceRecognition.savePlace(0, 0, 0, image);
			}
			
			List<IdPair<Integer, Integer>> vprList = visualPlaceRecognition.getAndClearVPRMatchedList();
			
			
			iterationCounter++;
		}

	}

	/**
	 * 
	 */
	private ConfigurationReader.Parameters readParametersFromXML(String fileName) {
		String configFileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL"
				+ "/"
				+ fileName);

		ConfigurationReader configReader = new ConfigurationReader();

		try {
			ConfigurationReader.Parameters params = configReader
					.readParameters(configFileName);
			return params;
		} catch (XmlPullParserException e) {
			Log.e(moduleLogName, "Failed to parse the config file");
		} catch (IOException e) {
			Log.e(moduleLogName, "Missing config file");
		}
		return null;
	}

}
