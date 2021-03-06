// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.dg.openAIL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.dg.camera.Preview;
import org.dg.camera.QRCodeDecoderClass;
import org.dg.camera.VisualCompass;
import org.dg.camera.VisualPlaceRecognition;
import org.dg.graphManager.GraphManager;
import org.dg.graphManager.Vertex;
import org.dg.graphManager.WiFiMeasurement;
import org.dg.inertialSensors.InertialSensors;
import org.dg.inertialSensors.InertialSensorsPlayback;
import org.dg.main.LocalizationView;
import org.dg.wifi.WiFiDirect;
import org.dg.wifi.WiFiDirect.DirectMeasurement;
import org.dg.wifi.WiFiPlayback;
import org.dg.wifi.WiFiScanner;
import org.opencv.core.Mat;
import org.opencv.core.Point3;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.view.View.MeasureSpec;

// This is a class that is supposed to provide the interface for all available solutions
public class OpenAndroidIndoorLocalization {
	private static final String moduleLogName = "OpenAIL";

	// Parameters
	static boolean WiFiFeatures = true;
	ConfigurationReader.Parameters parameters;

	// Inertial sensors
	public InertialSensors inertialSensors;

	// WiFi
	public WiFiScanner wifiScanner;
	int detectWiFiIssue;

	// Visual Place Recognition
	public VisualPlaceRecognition visualPlaceRecognition;
	
	// Visual Compass
	public VisualCompass visualCompass;

	// Graph
	public GraphManager graphManager;

	// Graph update
	private Timer updateGraphTimer;

	// Preview
	public Preview preview;

	// Prior map
	public PriorMapHandler priorMapHandler;

	// Navigation
	public Navigation navigation;

	// View to draw localization
	LocalizationView localizationView;

	// QR Code decoding
	public QRCodeDecoderClass qrCodeDecoder;

	// Context for toasts
	Context context;

	// Timestamp
	long processingStartTimestamp = 0;
	
	
	// STATE
	public enum STATE {STARTED, STOPPED};
	STATE state = STATE.STOPPED;

	// TODO STILL TESTING!
	WiFiPlayback wifiPlayback;
	InertialSensorsPlayback inertialSensorsPlayback;

	/*
	 * Creates OpenAIL, loads settings and initializes graph optimization,
	 * inertial sensors and WiFi scanner
	 */
	public OpenAndroidIndoorLocalization(Context _context,
			SensorManager sensorManager, WifiManager wifiManager) {
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
			inertialSensors = new InertialSensors(sensorManager,
					parameters.inertialSensors);

		// Init WiFi
		wifiScanner = new WiFiScanner(wifiManager,
				parameters.wifiPlaceRecognition);

		// Create QRCodeDecoder
		qrCodeDecoder = new QRCodeDecoderClass(context);
		
		

		// TODO
		wifiPlayback = new WiFiPlayback(parameters.playback, wifiScanner);
		inertialSensorsPlayback = new InertialSensorsPlayback(
				parameters.playback, inertialSensors);
	}

	/**
	 * Part of the code that can be only called after OpenCV was loaded inside
	 * application
	 */
	public void initAfterOpenCV() {
		Log.d(moduleLogName,
				"Initializing OpenAIL submodules after OpenCV initialization");

		// Init Visual Place Recognition
		visualPlaceRecognition = new VisualPlaceRecognition();
		
		// Creating visual compass
		visualCompass = new VisualCompass();
		
	}

	class AP {
		double X, Y, howMany;

		AP() {
			X = 0;
			Y = 0;
			howMany = 0;
		}
	}
	
	public void directWiFiTest() {
		Log.d(moduleLogName, "directWiFiTest()");
		WiFiDirect wifiDirect = new WiFiDirect();
		
		List<MapPosition> mapPositions = priorMapHandler
				.loadWiFiAndImageMap(parameters.mainProcessing.priorMapName);
		
		graphManager.openCreatedGraphStream();
		
		for (MapPosition mapPos : mapPositions) {
				wifiDirect.processNewScan(mapPos.scannedWiFiList, mapPos.id);
				
//				graphManager.addVertexXY(mapPos.id, mapPos.X,
//						mapPos.Y);
				graphManager.addVertexSE2(mapPos.id, mapPos.X,
						mapPos.Y, 0);
		}
		wifiDirect.filterDirectMeasurements();
		
		int numberOfNetworks = wifiDirect.getNumberOfNetworks();
		List<DirectMeasurement> measurements = wifiDirect.getGraphWiFiList();
		
		Log.d(moduleLogName, "directWiFiTest() - init for APs");
		
		AP [] apPositions = new AP[numberOfNetworks];
		for (int i=0;i<numberOfNetworks;i++)
			apPositions[i] = new AP();
		
		for (DirectMeasurement dm : measurements) {
			MapPosition mapPos = null;
			for ( MapPosition mp : mapPositions) {
				if (mp.id == dm.idPos) {
					mapPos = mp;
					break;
				}
					
			}
			
			
			apPositions[dm.idAP].X += mapPos.X;
			apPositions[dm.idAP].Y += mapPos.Y;
			apPositions[dm.idAP].howMany ++;
		}
		
		for ( int i=0;i<numberOfNetworks;i++) {
			AP ap = apPositions[i];
//			graphManager.addVertexSE2(i, ap.X / ap.howMany,
//					ap.Y / ap.howMany, 8.0);
			graphManager.addVertexXYZ(i, ap.X / ap.howMany,
					ap.Y / ap.howMany, 10.0);
		}
		
		Log.d(moduleLogName, "directWiFiTest() - add measurements");
		
		// TODO!
		graphManager.addMultipleWiFiMeasurements(measurements);
		
		Log.d(moduleLogName, "Reverse problem! ");
		
//		measurements = wifiDirect.getReverseTest();
//		
//		for (MapPosition mapPos : mapPositions) {
//			graphManager.addVertexSE2(mapPos.id-8000, mapPos.X,
//					mapPos.Y, 0);
//		}
//		
//		graphManager.addMultipleWiFiMeasurements(measurements);
		
		Log.d(moduleLogName, "directWiFiTest() - optimize");
		
		graphManager.optimizeGraph();
		
		
		Log.d(moduleLogName, "directWiFiTest() - end");
	}
	
	/*
	 * Starting the localization task: - creating graph structure - loading
	 * prior map - start checking for new measurements
	 */
	public void startLocalization() {
		Log.d(moduleLogName, "startLocalization()");
		state = STATE.STARTED;

		// Let's read map plan
		BuildingPlan buildingPlan = priorMapHandler
				.loadCorridorMap(parameters.mainProcessing.priorMapName);
		localizationView.setBuildingPlan(buildingPlan);

		if (parameters.mainProcessing.useNavigation)
			navigation = new Navigation(buildingPlan);

		// Creating new graph
		graphManager.start();
		graphManager.addVertexSE2(0, 0, 0, 0);

		// Load prior map
		if (parameters.mainProcessing.usePriorMap) {
			List<MapPosition> mapPositions = priorMapHandler
					.loadWiFiAndImageMap(parameters.mainProcessing.priorMapName);

			// For all positions in a map add to graph and initially add to
			// visualization
			List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
			for (MapPosition mapPos : mapPositions) {

				// Add a node to the graph
				graphManager.addVertexSE2(mapPos.id, mapPos.X,
						mapPos.Y, mapPos.Z);

				// Add a position for initial Visualization
				Pair<Double, Double> tmpPair = new Pair<Double, Double>(
						mapPos.X, mapPos.Y);
				wifiScanLocations.add(tmpPair);

				// Add new scan to WiFi Place Recognition
				// (TODO: Could be extended to use X, Y, Z, angle)
				wifiScanner.addScanToRecognition(mapPos.id,
						mapPos.scannedWiFiList);

				// Add new image to VPR
				// (TODO: Could be extended to use X, Y, Z, angle)
				// visualPlaceRecognition.addPlace(mapPos.id, mapPos.image);
			}
			localizationView.setWiFiScanLocations(wifiScanLocations);
		}

		// Synchronize times
		synchronizeModuleTime();

		// Start WiFi recognition
		wifiScanner.startNewPlaceRecognitionThread();

		// Start FABMAP recognition
		// visualPlaceRecognition.start(_preview);

		// Check for new information at fixed rate
		updateGraphTimer = new Timer();
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
	
	// Starts playback
	public void startPlayback() {
			
		wifiPlayback.start();

		inertialSensors.startPlayback();
		inertialSensors.startStepometer();
		inertialSensorsPlayback.start();
	}

	/**
	 * We set the same time for all modules
	 */
	public void synchronizeModuleTime() {
		processingStartTimestamp = getCurrentTimestamp();
		inertialSensors.setStartTime();
		wifiScanner.setStartTime();
		graphManager.setStartTime();
	}

	/*
	 * Stopping the localization
	 */
	public void stopLocalization() {
		state = STATE.STOPPED;
		
		// Stop checking for new data
		updateGraphTimer.cancel();
		updateGraphTimer = null;

		// Stop WiFi recognition thread
		wifiScanner.stopNewPlaceRecognitionThread();

		// Stop VPR thread
		// visualPlaceRecognition.stop()

		// Stop the optimization thread
		graphManager.stopOptimizationThread();

//		File folder = new File(Environment.getExternalStorageDirectory()
//				+ "/OpenAIL");
//		if (!folder.exists()) {
//			folder.mkdir();
//		}
//		File dirResult = new File(folder.getAbsolutePath() + "/result");
//		if (!dirResult.exists()) {
//			dirResult.mkdirs();
//		}
//
//		Bitmap localizationViewScreenshot = localizationView.getDrawingCache();
//
//		FileOutputStream imgStream;
//		try {
//			imgStream = new FileOutputStream(dirResult.getAbsolutePath()
//					+ "/positionEstimates.png");
//			localizationViewScreenshot.compress(CompressFormat.PNG, 10,
//					imgStream);
//			imgStream.close();
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// Getting the estimates
		// List<Vertex> list = graphManager.getVerticesEstimates();
	}

	/*
	 * Optimizing the graph stored in a file - "offline mode"
	 */
	public void optimizeGraphInFile(String name) {
		graphManager.optimizeGraphInFile("lastCreatedGraph.g2o");

		List<Vertex> listOfVertices = graphManager.getVerticesEstimates();
		List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();

		for (Vertex v : listOfVertices) {
			Log.d(moduleLogName, "Vertex " + v.id + " - pos = (" + v.X + ", "
					+ v.Y + ", " + v.Z + ")");
			if (v.id < 10000)
				userLocations.add(new Pair<Double, Double>(v.X, v.Y));
		}

		localizationView.setUserLocations(userLocations);

		// For all positions in a map add to graph and initially add to
		// visualization
		List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
		if (parameters.mainProcessing.usePriorMap) {
			List<MapPosition> mapPositions = priorMapHandler
					.loadWiFiAndImageMap(parameters.mainProcessing.priorMapName);

			// For all positions in a map
			for (MapPosition mapPos : mapPositions) {
				// Add a position for initial Visualization
				Pair<Double, Double> tmpPair = new Pair<Double, Double>(
						mapPos.X, mapPos.Y);
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
	 * Class used to check if there is new sensor data to add to graph The
	 * mentioned method is performed with a frequency set in settings.xml
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

				graphManager.createNewPose();
				graphManager.addStepometerMeasurement(distance, yawZ);
			}

			// Visual place recognition - take the image if acc variance is
			// reasonable
//			float accVariance = inertialSensors.getAccVariance();
//			if (iterationCounter - lastImageSaveIterationCounter >= parameters.mainProcessing.imageCaptureStep
//					&& accVariance < parameters.mainProcessing.imageCaptureVarianceThreshold) {
//				Log.d(moduleLogName,
//						"Mobicase version: saving image when in motion");
//				if (preview != null) {
//					Log.d(moduleLogName, "Mobicase version: preview OK");
//					Mat image = preview.getCurPreviewImage();
//
//					int poseId = graphManager.getCurrentPoseId();
//					visualPlaceRecognition.savePlace(0, 0, 0, image, poseId);
//				} else
//					Log.d(moduleLogName, "Mobicase version: preview FAILED");
//
//				lastImageSaveIterationCounter = iterationCounter;
//			}

			// WiFi place recognition
			List<IdPair<Integer, Integer>> recognizedPlaces = wifiScanner
					.getAndClearRecognizedPlacesList();
			Log.d(moduleLogName, "The placeRecognition thread found "
					+ recognizedPlaces.size() + " connections");
			if (recognizedPlaces != null && recognizedPlaces.size() > 0)
				graphManager.addMultipleWiFiFingerprints(recognizedPlaces);

			// Check if there is new measurement
			if (wifiScanner.isNewMeasurement()) {
				// All ok
				detectWiFiIssue = 0;

				// New measurement had been read
				wifiScanner.newMeasurement(false);

				// WiFi place recognition
				int currentPoseId = graphManager.getCurrentPoseId();

				// Add new pose to search and set it with the id of the graph
				// pose
				wifiScanner.addLastScanToRecognition(currentPoseId);

				// Should we add direct links to WiFi networks to the graph
				if (parameters.wifiPlaceRecognition.directWiFiMeasurements) {
					// Adding WiFi measurements
//					List<WiFiMeasurement> wifiList = wifiScanner
//							.getGraphWiFiList();
//					if (wifiList != null)
//						graphManager.addMultipleWiFiMeasurements(wifiList);
				}
			} else if (wifiScanner.getWaitingForScan()
					&& !wifiScanner.getPlaybackState()) {

				Log.d(moduleLogName,
						"WiFi waiting for scan = "
								+ wifiScanner.getWaitingForScan());
				detectWiFiIssue++;

				// Bad scan received - need to restart
				if (detectWiFiIssue > parameters.mainProcessing.frequencyOfNewDataQuery * 4) {
					Log.d(moduleLogName,
							"Restarting WiFi due to continuous scan issue");
					wifiScanner.startScanning();
				}

			}

			// Save current image
			// Log.d(moduleLogName, "Processing camera ...");
			// if (preview != null && iterationCounter % 5 == 0) {
			// Log.d(moduleLogName, "Getting and saving camera image");
			// Mat image = preview.getCurPreviewImage();
			// visualPlaceRecognition.savePlace(0, 0, 0, image);
			// }
			//
			// List<IdPair<Integer, Integer>> vprList = visualPlaceRecognition
			// .getAndClearVPRMatchedList();

			// QR Codes
			List<Pair<Integer, Point3>> recognizedQRCodes = qrCodeDecoder
					.getRecognizedQRCodes();
			Log.d(moduleLogName,
					"Recognized QR codes: " + recognizedQRCodes.size());
			if (recognizedQRCodes.size() > 0) {
				graphManager.addMultipleQRCodes(recognizedQRCodes);
			}

			// Should we show the background plan?
			boolean showBackgroundPlan = parameters.mainProcessing.showMapWithoutMapConnection
					|| graphManager.isMapConnected();
			localizationView.showBackgroundPlan(showBackgroundPlan);

			// TODO: Get current estimate of vertices
			if (graphManager.changeInOptimizedData) {
				Log.d(moduleLogName, "Adding user positions to visualization");
				List<Vertex> listOfVertices = graphManager
						.getVerticesEstimates();
				List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();

				if (listOfVertices == null) {
					Log.d(moduleLogName, "list of vertices is null");
				} else {
					Log.d(moduleLogName, "SIZE: " + listOfVertices.size());
				}

				for (Vertex v : listOfVertices) {
					if (v.id < 10000)
						userLocations.add(new Pair<Double, Double>(v.X, v.Y));
				}

				localizationView.setUserLocations(userLocations);

				graphManager.changeInOptimizedData = false;
			}

			// Navigation part TODO: STILL TESTING
			if (parameters.mainProcessing.useNavigation
					&& localizationView.isGoalSet()) {
				Pair<Double, Double> goal = localizationView.getGoal();

				Vertex v = graphManager.getCurrentPoseEstimate();
				if (v == null) {
					Log.d(moduleLogName, "Navigation: vertex is null");
					v = new Vertex(-1, 0, 0, 0);
				}

				List<Pair<Double, Double>> pathToGoal = navigation
						.startNavigation(v.X, v.Y, goal.first, goal.second);
				localizationView.setPathToGoal(pathToGoal);
			}

			iterationCounter++;
		}

	}

	/**
	 * Method that can be used to save a position in a map
	 */
	public void saveMapPoint(String mapName, int id, double X, double Y,
			double Z) {
		Log.d(moduleLogName, "Called saveMapPoint with: " + mapName + " id: "
				+ id + " " + X + " " + Y + " " + Z);

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

	public void clearNewMap(String mapName) {
		priorMapHandler.clearNewMap(mapName);
	}

	/**
	 * Method used to read all of the parameters from settings.xml
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

	private long getCurrentTimestamp() {
		return System.currentTimeMillis();
	}

	/**
	 * Method used to create directories if those do not exist
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
	
	public STATE getState() {
		return state;
	}
	
	public int getPreviewSize() {
		return parameters.camera.previewSize;
	}
}
