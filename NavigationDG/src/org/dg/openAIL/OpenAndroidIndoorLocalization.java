package org.dg.openAIL;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.dg.camera.Preview;
import org.dg.camera.QRCode;
import org.dg.camera.QRCodeDecoderClass;
import org.dg.camera.VisualPlaceRecognition;
import org.dg.graphManager.GraphManager;
import org.dg.graphManager.Vertex;
import org.dg.graphManager.wiFiMeasurement;
import org.dg.inertialSensors.InertialSensors;
import org.dg.main.LocalizationView;
import org.dg.wifi.WifiScanner;
import org.opencv.core.Mat;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

// This is a class that is supposed to provide the interface for all available solutions
public class OpenAndroidIndoorLocalization {
	private static final String moduleLogName = "OpenAIL";

	// Parameters
	static boolean WiFiFeatures = true;
	ConfigurationReader.Parameters parameters;

	// Inertial sensors
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

	// Prior map
	public PriorMapHandler priorMapHandler;
	
	// View to draw localization
	LocalizationView localizationView;
	
	// QR Code decoding
	public QRCodeDecoderClass qrCodeDecoder;
	
	// Context for toasts
	Context context;

	/*
	 * Creates OpenAIL, loads settings and initializes graph optimization, 
	 * inertial sensors and WiFi scanner
	 */
	public OpenAndroidIndoorLocalization(Context _context, SensorManager sensorManager,
			WifiManager wifiManager) {
		// Save context
		context = _context;
		
		// Create directories if needed
		createNeededDirectories();

		// Reading settings
		parameters = readParametersFromXML("settings.xml");

		// Preparing prior map
		priorMapHandler = new PriorMapHandler();
		
		// Init graph
		graphManager = new GraphManager(parameters.graphManager);

		// Init inertial sensors
		if (parameters.inertialSensors.useModule)
			inertialSensors = new InertialSensors(sensorManager, parameters.inertialSensors);

		// Init WiFi
		wifiScanner = new WifiScanner(wifiManager,
				parameters.wifiPlaceRecognition);
		
		// Create QRCodeDecoder
		qrCodeDecoder = new QRCodeDecoderClass(context);

	}

	/**
	 * Part of the code that can be only called after OpenCV was loaded inside application
	 */
	public void initAfterOpenCV() {
		Log.d(moduleLogName,
				"Initializing OpenAIL submodules after OpenCV initialization");

		// Init Visual Place Recognition
		visualPlaceRecognition = new VisualPlaceRecognition();
	}

	/*
	 * Starting the localization task:
	 * - creating graph structure
	 * - loading prior map
	 * - start checking for new measurments
	 */
	public void startLocalization() {
		// Creating new graph
		graphManager.start();

		// Load prior map
		if (parameters.mainProcessing.usePriorMap) {
			List<MapPosition> mapPositions = priorMapHandler.loadPriorMap(parameters.mainProcessing.priorMapName);
			
			// For all positions in a map add to graph and initially add to visualization
			List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
			for (MapPosition mapPos: mapPositions) {
				
				// Add a node to the graph
				graphManager.addVertexWithKnownPosition(mapPos.id, mapPos.X, mapPos.Y, mapPos.Z);
				
				// Add a position for initial Visualization
				Pair<Double, Double> tmpPair = new Pair<Double, Double>(mapPos.X, mapPos.Y);
				wifiScanLocations.add(tmpPair);

				// Add new scan to WiFi Place Recognition 
				//	(TODO: Could be extended to use X, Y, Z, angle)
				wifiScanner.addScanToRecognition(mapPos.id, mapPos.scannedWiFiList);
				
				// Add new image to VPR 
				//	(TODO: Could be extended to use X, Y, Z, angle)
				//visualPlaceRecognition.addPlace(mapPos.id, mapPos.image);
			}
			localizationView.setWiFiScanLocations(wifiScanLocations);
		}

		// Start WiFi recognition
		wifiScanner.startNewPlaceRecognitionThread();

		// Start FABMAP recognition
		// visualPlaceRecognition.start(_preview);

		// Check for new information at fixed rate
		long delay = (long) (1000.0 / parameters.mainProcessing.frequencyOfNewDataQuery);
		updateGraphTimer.scheduleAtFixedRate(new UpdateGraph(), 0, delay);

		// Check if there is an issue with WiFi
		detectWiFiIssue = 0;
		
		// Save first image for FABMAP
		if (preview != null) {
			Log.d(moduleLogName, "Mobicase version: preview OK");
			Mat image = preview.getCurPreviewImage();
			visualPlaceRecognition.savePlace(0, 0, 0, image);
		}
	}

	/*
	 * Stopping the localization
	 */
	public void stopLocalization() {

		// Stop checking for new data
		updateGraphTimer.cancel();

		// Stop WiFi recognition thread
		wifiScanner.stopNewPlaceRecognitionThread();

		// Stop VPR thread
		// visualPlaceRecognition.stop()

		// Stop the optimization thread
		graphManager.stop(); // TODO: Remove it!
		graphManager.stopOptimizationThread();

		// Getting the estimates
//		List<Vertex> list = graphManager.getVerticesEstimates();
	}

	/*
	 * Optimizing the graph stored in a file - "offline mode"
	 */
	public void optimizeGraphInFile(String name)
	{
		graphManager.optimizeGraphInFile("lastCreatedGraph.g2o");
		
		List<Vertex> listOfVertices = graphManager.getVerticesEstimates();
		List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();
		
		for (Vertex v : listOfVertices) {
			Log.d(moduleLogName, "Vertex " + v.id + " - pos = (" + v.X + ", " + v.Y + ", " + v.Z + ")");
			if ( v.id < 10000 )
				userLocations.add(new Pair<Double,Double>(v.X, v.Y));
		}
		
		localizationView.setUserLocations(userLocations);
		
		// For all positions in a map add to graph and initially add to visualization
		List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
		if (parameters.mainProcessing.usePriorMap) {
			List<MapPosition> mapPositions = priorMapHandler.loadPriorMap(parameters.mainProcessing.priorMapName);
			
			// For all positions in a map
			for (MapPosition mapPos: mapPositions) {
				// Add a position for initial Visualization
				Pair<Double, Double> tmpPair = new Pair<Double, Double>(mapPos.X, mapPos.Y);
				wifiScanLocations.add(tmpPair);
			}
			// Add to the visualization
			localizationView.setWiFiScanLocations(wifiScanLocations);
		}
		
	}
	
	/*
	 * Setting the view to draw current localization estimates
	 */
	public void setLocalizationView(LocalizationView _localizationView) {
		// Save localization view for drawing
		localizationView = _localizationView;
	}
	
	/**
	* Class used to check if there is new sensor data to add to graph
	* The mentioned method is performed with a frequency set in settings.xml
	*/
	class UpdateGraph extends TimerTask {
		int iterationCounter = 0;
		int lastImageSaveIterationCounter = 0;

		// Called to check new data
		public void run() {
		
			// Stepometer
			double distance = inertialSensors.getStepometerStepDistance();
			if (distance > 0.01) {
				
				// get angle from our estimation
				// TODO: RIGHT NOW WE USE ANDROID ORIENTATION
				float yawZ = -inertialSensors.getYawForStepometer();

				// We need to change yawZ into radians and change direction
				yawZ = (float) (yawZ * Math.PI / 180.0f);

				graphManager.addStepometerMeasurement(distance, yawZ);
			}
			
			
			// Visual place recognition - take the image if acc variance is reasonable
			float accVariance = inertialSensors.getAccVariance();
			if (iterationCounter - lastImageSaveIterationCounter >= parameters.mainProcessing.imageCaptureStep
					&& accVariance < parameters.mainProcessing.imageCaptureVarianceThreshold)
			{
				Log.d(moduleLogName, "Mobicase version: saving image when in motion");
				if (preview != null) {
					Log.d(moduleLogName, "Mobicase version: preview OK");
					Mat image = preview.getCurPreviewImage();
					
					int poseId = graphManager.getCurrentPoseId();
					visualPlaceRecognition.savePlace(0, 0, 0, image, poseId);
				}
				else
					Log.d(moduleLogName, "Mobicase version: preview FAILED");
				
				lastImageSaveIterationCounter = iterationCounter;
			}

			// WiFi place recognition
			List<IdPair<Integer, Integer>> recognizedPlaces = wifiScanner
					.getAndClearRecognizedPlacesList();
			Log.d(moduleLogName, "The placeRecognition thread found "
					+ recognizedPlaces.size() + " connections");
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
				if (detectWiFiIssue > 20) {
					Log.d(moduleLogName,
							"Restarting WiFi due to continuous scan issue");
					wifiScanner.startScanning();
				}

			}

			// Save current image
//			Log.d(moduleLogName, "Processing camera ...");
//			if (preview != null && iterationCounter % 5 == 0) {
//				Log.d(moduleLogName, "Getting and saving camera image");
//				Mat image = preview.getCurPreviewImage();
//				visualPlaceRecognition.savePlace(0, 0, 0, image);
//			}
//
//			List<IdPair<Integer, Integer>> vprList = visualPlaceRecognition
//					.getAndClearVPRMatchedList();
			
			// QR Codes
			ca
			
			// TODO: Get current estimate of vertices
			if (graphManager.changeInOptimizedData)
			{
				graphManager.changeInOptimizedData = false;
				
				Log.d(moduleLogName, "Adding user positions to visualization");
				List<Vertex> listOfVertices = graphManager.getVerticesEstimates();
				List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();	
				for (Vertex v : listOfVertices) {
					if ( v.id < 10000 )
						userLocations.add(new Pair<Double,Double>(v.X, v.Y));
				}
				
				localizationView.setUserLocations(userLocations);
			}
			
			
			iterationCounter++;
		}

	}
	
	/**
	 *  Method that can be used to save a position in a map
	 */
	public void saveMapPoint(String mapName, int id, double X, double Y, double Z) {
		Log.d(moduleLogName, "Called saveMapPoint with: " + mapName + " id: " + id + " " + X + " " + Y + " " + Z);
		
		// Let's create a new map position
		MapPosition mapPos = new MapPosition();
		
		// Fill position with provided data
		mapPos.id = id;
		mapPos.X = X;
		mapPos.Y = Y;
		mapPos.Z = Z;
		
		// Filling missing information with current data
		
		// Angle in global coordinate system
		mapPos.angle = inertialSensors.getGlobalYaw();

		// Getting the last WiFi scan
		mapPos.scannedWiFiList = wifiScanner.getLastScan();

		// Getting the last image
		mapPos.image = preview.getCurPreviewImage();
		
		// Save this point to files
		priorMapHandler.saveMapPoint(mapName, mapPos);
	}
	
	/**
	 *  Method used to read all of the parameters from settings.xml
	 */
	private ConfigurationReader.Parameters readParametersFromXML(String fileName) {
		String configFileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/"
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


	/**
	 *  Method used to create directories if those do not exist
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
}
