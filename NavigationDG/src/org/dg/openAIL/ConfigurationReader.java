package org.dg.openAIL;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class ConfigurationReader {

	private static final String moduleLogName = "ConfigurationReader";

	public enum Modules {
		MAINPROCESSING, INERTIALSENSORS, GRAPHMANAGER, WIFIPLACERECOGNITION, VISUALODOMETRY, VISUALPLACERECOGNITION,
	}
	
	// Class to store all parameters used in OpenAIL
	public class Parameters {
		public class MainProcessing {
			double frequencyOfNewDataQuery;
			String priorMapName;
			public boolean usePriorMap;
			int imageCaptureStep;
			double imageCaptureVarianceThreshold;
		}	
		
		public class InertialSensors {
			public class Record {
				public boolean accelerometer, gyroscope, magnetometer,
						barometer, accelerometerWithoutGravity;
				public boolean orientationAndroid, orientationAndroidEuler,
						orientationAEKF, orientationAEKFEuler, orientationCF,
						orientationCFEuler;
			}

			public boolean useModule;
			public boolean stepometer;
			public double priorMapStepometerBias;
			public Record record = new Record();
		}
		
		public class GraphManager {
			public double vprVicinityDeadBandRadius;
			public double informationMatrixOfVPRVicinity;
			public double wifiFingerprintDeadBandRadius;
			public double informationMatrixOfWiFiFingerprint;
			public double informationMatrixOfWiFi;
			public int optimizeFromFileIterationCount;
		}
		
		public class WiFiPlaceRecognition {
			public boolean useModule;
			public int	maxPlaceDatabaseSize;
			public int	maxQueueSize; 
			public double fractionOfQueueAfterReduction;
			public int	minNumberOfSharedNetworks;
			public double minPercentOfSharedNetworks;
			public double maxAvgErrorThreshold;
		}

		MainProcessing mainProcessing = new MainProcessing();
		InertialSensors inertialSensors = new InertialSensors();
		GraphManager graphManager = new GraphManager();
		WiFiPlaceRecognition wifiPlaceRecognition = new WiFiPlaceRecognition();
	}

	Parameters parameters = new Parameters();

	Parameters getParameters() {
		return parameters;
	}
	 

	private static final String ns = null;

	public Parameters readParameters(String path)
			throws XmlPullParserException, IOException {

		Log.d(moduleLogName, "Starting ...");
		
		FileInputStream ifs = new FileInputStream(new File(path));
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(ifs, null);
			parser.nextTag();

			readFeed(parser);

		} catch (Exception e) {
			return null;
		} finally {
			ifs.close();
		}
		Log.d(moduleLogName, "Finishing ...");
		
		logAllParameters();
		
		return parameters;
	}
	
	
	private void logAllParameters() {
		
		Log.d(moduleLogName, "MainProcessing:" +
				"\n--- frequencyOfNewDataQuery=" + parameters.mainProcessing.frequencyOfNewDataQuery + 
				"\n--- priorMapName=" + parameters.mainProcessing.priorMapName + 
				"\n--- usePriorMap=" + parameters.mainProcessing.usePriorMap +
				"\n--- imageCaptureStep=" + parameters.mainProcessing.imageCaptureStep +
				"\n--- imageCaptureVarianceThreshold=" + parameters.mainProcessing.imageCaptureVarianceThreshold);
		
		Log.d(moduleLogName, "InertialSensors:" +
				"\n--- useModule=" + parameters.inertialSensors.useModule + 
				"\n--- stepometer=" + parameters.inertialSensors.stepometer +
				"\n--- priorMapStepometerBias=" + parameters.inertialSensors.priorMapStepometerBias);	

		Log.d(moduleLogName, "--- Record:" +
				"\n------ accelerometer=" + parameters.inertialSensors.record.accelerometer + 
				"\n------ gyroscope=" + parameters.inertialSensors.record.gyroscope + 
				"\n------ magnetometer=" + parameters.inertialSensors.record.magnetometer + 
				"\n------ barometer=" + parameters.inertialSensors.record.barometer + 
				"\n------ accelerometerWithoutGravity=" + parameters.inertialSensors.record.accelerometerWithoutGravity + 
				
				"\n------ orientationAndroid=" + parameters.inertialSensors.record.orientationAndroid + 
				"\n------ orientationAndroidEuler=" + parameters.inertialSensors.record.orientationAndroidEuler + 
				"\n------ orientationAEKF=" + parameters.inertialSensors.record.orientationAEKF + 
				"\n------ orientationAEKFEuler=" + parameters.inertialSensors.record.orientationAEKFEuler + 
				"\n------ orientationCF=" + parameters.inertialSensors.record.orientationCF + 
				"\n------ orientationCFEuler=" + parameters.inertialSensors.record.orientationCFEuler);
	
		Log.d(moduleLogName, "GraphManager:" +
				"\n--- vprVicinityDeadBandRadius=" + parameters.graphManager.vprVicinityDeadBandRadius + 
				"\n--- informationMatrixOfVPRVicinity=" + parameters.graphManager.informationMatrixOfVPRVicinity + 
				"\n--- wifiFingerprintDeadBandRadius=" + parameters.graphManager.wifiFingerprintDeadBandRadius + 
				"\n--- informationMatrixOfWiFiFingerprint=" + parameters.graphManager.informationMatrixOfWiFiFingerprint +
				"\n--- informationMatrixOfWiFi=" + parameters.graphManager.informationMatrixOfWiFi +
				"\n--- optimizeFromFileIterationCount=" + parameters.graphManager.optimizeFromFileIterationCount);
				
		Log.d(moduleLogName, "WiFiPlaceRecognition:" +
				"\n--- useModule=" + parameters.wifiPlaceRecognition.useModule +
				"\n--- maxPlaceDatabaseSize=" + parameters.wifiPlaceRecognition.maxPlaceDatabaseSize +
				"\n--- maxQueueSize=" + parameters.wifiPlaceRecognition.maxQueueSize +
				"\n--- fractionOfQueueAfterReduction=" + parameters.wifiPlaceRecognition.fractionOfQueueAfterReduction +
				"\n--- minNumberOfSharedNetworks=" + parameters.wifiPlaceRecognition.minNumberOfSharedNetworks +
				"\n--- minPercentOfSharedNetworks=" + parameters.wifiPlaceRecognition.minPercentOfSharedNetworks +
				"\n--- maxAvgErrorThreshold=" + parameters.wifiPlaceRecognition.maxAvgErrorThreshold);		
	}


	private void readFeed(XmlPullParser parser) throws XmlPullParserException,
			IOException {

		parser.require(XmlPullParser.START_TAG, ns, "OpenAIL");
		while (parser.next() != XmlPullParser.END_TAG) {
			
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();

			// Starts by looking for the entry tag
			Modules currentModule = Modules.valueOf(name.toUpperCase());

			switch (currentModule) {
			case MAINPROCESSING:
				readMainProcessing(parser);
				break;
			case INERTIALSENSORS:
				readInertialSensors(parser);
				break;
			case GRAPHMANAGER:
				readGraphManager(parser);
				break;
			case WIFIPLACERECOGNITION:
				readWiFiPlaceRecognition(parser);
				break;
			default:
				Log.d(moduleLogName, "Unrecognized tag : " + name);
				skip(parser);
			}

		}
	}
	
	// Parses the contents of an entry.
	private void readMainProcessing(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(moduleLogName, "<MainProcessing>");
		parser.require(XmlPullParser.START_TAG, ns, "MainProcessing");
	
		// Reading attributes
		String frequencyOfNewDataQueryString = parser.getAttributeValue(null, "frequencyOfNewDataQuery");
		parameters.mainProcessing.priorMapName = parser.getAttributeValue(null, "priorMapName");
		String usePriorMapString = parser.getAttributeValue(null, "usePriorMap");
		String imageCaptureStepString = parser.getAttributeValue(null, "imageCaptureStep");
		String imageCaptureVarianceThresholdString = parser.getAttributeValue(null, "imageCaptureVarianceThreshold");
		
		// Logging those values
		Log.d(moduleLogName, "frequencyOfNewDataQuery = " + frequencyOfNewDataQueryString);
		Log.d(moduleLogName, "priorMapName = " + parameters.mainProcessing.priorMapName);
		Log.d(moduleLogName, "usePriorMap = " + usePriorMapString);
		Log.d(moduleLogName, "imageCaptureStep = " + imageCaptureStepString);
		Log.d(moduleLogName, "imageCaptureVarianceThreshold = " + imageCaptureVarianceThresholdString);

		// Storing read values
		parameters.mainProcessing.frequencyOfNewDataQuery =  Double.parseDouble(frequencyOfNewDataQueryString);
		parameters.mainProcessing.usePriorMap = usePriorMapString.equals("True");
		parameters.mainProcessing.imageCaptureStep = Integer.parseInt(imageCaptureStepString);
		parameters.mainProcessing.imageCaptureVarianceThreshold =  Double.parseDouble(imageCaptureVarianceThresholdString);
		
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, ns, "MainProcessing");
		Log.d(moduleLogName, "</MainProcessing>");
	}
		

	// Parses the contents of an entry.
	private void readInertialSensors(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(moduleLogName, "<InertialSensors>");
		parser.require(XmlPullParser.START_TAG, ns, "InertialSensors");

		// Reading attributes
		String useModuleString = parser.getAttributeValue(null, "useModule");
		String stepometerString = parser.getAttributeValue(null, "stepometer");
		String priorMapStepometerBiasString = parser.getAttributeValue(null, "priorMapStepometerBias");
		
		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);
		Log.d(moduleLogName, "stepometer = " + stepometerString);
		Log.d(moduleLogName, "priorMapStepometerBias = " + priorMapStepometerBiasString);
				
		// Storing read values
		parameters.inertialSensors.useModule = useModuleString.equals("True");
		parameters.inertialSensors.stepometer = useModuleString.equals("True");
		parameters.inertialSensors.priorMapStepometerBias =  Double.parseDouble(priorMapStepometerBiasString);
		
		// Reading record parameters
		while (parser.next() != XmlPullParser.END_TAG) {
			
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			
			Log.d(moduleLogName, "</Record>");
			parser.require(XmlPullParser.START_TAG, ns, "Record");
			
			// Reading attributes
			String accelerometerString = parser.getAttributeValue(null, "accelerometer");
			String gyroscopeString = parser.getAttributeValue(null, "gyroscope");
			String magnetometerString = parser.getAttributeValue(null, "magnetometer");
			String barometerString = parser.getAttributeValue(null, "barometer");
			String accelerometerWithoutGravityString = parser.getAttributeValue(null, "accelerometerWithoutGravity");
			
			String orientationAndroidString = parser.getAttributeValue(null, "orientationAndroid");
			String orientationAndroidEulerString = parser.getAttributeValue(null, "orientationAndroidEuler");
			String orientationAEKFString = parser.getAttributeValue(null, "orientationAEKF");
			String orientationAEKFEulerString = parser.getAttributeValue(null, "orientationAEKFEuler");
			String orientationCFString = parser.getAttributeValue(null, "orientationCF");
			String orientationCFEulerString = parser.getAttributeValue(null, "orientationCFEuler");
			
			// Logging those values
			Log.d(moduleLogName, "accelerometer = " + accelerometerString);
			Log.d(moduleLogName, "gyroscope = " + gyroscopeString);
			Log.d(moduleLogName, "magnetometer = " + magnetometerString);
			Log.d(moduleLogName, "barometer = " + barometerString);
			Log.d(moduleLogName, "accelerometerWithoutGravity = " + accelerometerWithoutGravityString);
			
			Log.d(moduleLogName, "orientationAndroid = " + orientationAndroidString);
			Log.d(moduleLogName, "orientationAndroidEuler = " + orientationAndroidEulerString);
			Log.d(moduleLogName, "orientationAEKF = " + orientationAEKFString);
			Log.d(moduleLogName, "orientationAEKFEuler = " + orientationAEKFEulerString);
			Log.d(moduleLogName, "orientationCF = " + orientationCFString);
			Log.d(moduleLogName, "orientationCFEuler = " + orientationCFEulerString);
			
			// Storing read values
			parameters.inertialSensors.record.accelerometer = accelerometerString.equals("True");
			parameters.inertialSensors.record.gyroscope = gyroscopeString.equals("True");
			parameters.inertialSensors.record.magnetometer = magnetometerString.equals("True");
			parameters.inertialSensors.record.barometer = barometerString.equals("True");
			parameters.inertialSensors.record.accelerometerWithoutGravity = accelerometerWithoutGravityString.equals("True");
			
			parameters.inertialSensors.record.orientationAndroid = orientationAndroidString.equals("True");
			parameters.inertialSensors.record.orientationAndroidEuler = orientationAndroidEulerString.equals("True");
			parameters.inertialSensors.record.orientationAEKF = orientationAEKFString.equals("True");
			parameters.inertialSensors.record.orientationAEKFEuler = orientationAEKFEulerString.equals("True");
			parameters.inertialSensors.record.orientationCF = orientationCFString.equals("True");
			parameters.inertialSensors.record.orientationCFEuler = orientationCFEulerString.equals("True");
			
			parser.nextTag();
			parser.require(XmlPullParser.END_TAG, ns, "Record");
			Log.d(moduleLogName, "</Record>");
		}
		
		parser.require(XmlPullParser.END_TAG, ns, "InertialSensors");
		Log.d(moduleLogName, "</InertialSensors>");
	}
	
	// Parses the contents of an entry.
	private void readGraphManager(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(moduleLogName, "<GraphManager>");
		parser.require(XmlPullParser.START_TAG, ns, "GraphManager");

		// Reading attributes
		String vprVicinityDeadBandRadiusString = parser.getAttributeValue(null, "vprVicinityDeadBandRadius");
		String informationMatrixOfVPRVicinityString = parser.getAttributeValue(null, "informationMatrixOfVPRVicinity");
		String wifiFingerprintDeadBandRadiusString = parser.getAttributeValue(null, "wifiFingerprintDeadBandRadius");
		String informationMatrixOfWiFiFingerprintString = parser.getAttributeValue(null, "informationMatrixOfWiFiFingerprint");
		String informationMatrixOfWiFiString = parser.getAttributeValue(null, "informationMatrixOfWiFi");
		String optimizeFromFileIterationCountString = parser.getAttributeValue(null, "optimizeFromFileIterationCount");

		// Logging those values
		Log.d(moduleLogName, "vprVicinityDeadBandRadius = " + vprVicinityDeadBandRadiusString);
		Log.d(moduleLogName, "informationMatrixOfVPRVicinity = " + informationMatrixOfVPRVicinityString);
		Log.d(moduleLogName, "wifiFingerprintDeadBandRadius = " + wifiFingerprintDeadBandRadiusString);
		Log.d(moduleLogName, "informationMatrixOfWiFiFingerprint = " + informationMatrixOfWiFiFingerprintString);
		Log.d(moduleLogName, "informationMatrixOfWiFi = " + informationMatrixOfWiFiString);
		Log.d(moduleLogName, "optimizeFromFileIterationCount = " + optimizeFromFileIterationCountString);

		// Storing read values
		parameters.graphManager.vprVicinityDeadBandRadius = Double.parseDouble(vprVicinityDeadBandRadiusString);
		parameters.graphManager.informationMatrixOfVPRVicinity = Double.parseDouble(informationMatrixOfVPRVicinityString);
		parameters.graphManager.wifiFingerprintDeadBandRadius = Double.parseDouble(wifiFingerprintDeadBandRadiusString);
		parameters.graphManager.informationMatrixOfWiFiFingerprint = Double.parseDouble(informationMatrixOfWiFiFingerprintString);
		parameters.graphManager.informationMatrixOfWiFi = Double.parseDouble(informationMatrixOfWiFiString);
		parameters.graphManager.optimizeFromFileIterationCount = Integer.parseInt(optimizeFromFileIterationCountString);

		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, ns, "GraphManager");
		Log.d(moduleLogName, "</GraphManager>");
	}

	private void readWiFiPlaceRecognition(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(moduleLogName, "<WiFiPlaceRecognition>");
		parser.require(XmlPullParser.START_TAG, ns, "WiFiPlaceRecognition");

		// Reading attributes
		String useModuleString = parser.getAttributeValue(null, "useModule");
		String maxPlaceDatabaseSizeString = parser.getAttributeValue(null, "maxPlaceDatabaseSize"); 
		String maxQueueSizeString = parser.getAttributeValue(null, "maxQueueSize"); 
		String fractionOfQueueAfterReductionString = parser.getAttributeValue(null, "fractionOfQueueAfterReduction");
		String minNumberOfSharedNetworksString = parser.getAttributeValue(null, "minNumberOfSharedNetworks");
		String minPercentOfSharedNetworksString = parser.getAttributeValue(null, "minPercentOfSharedNetworks");
		String maxAvgErrorThresholdString = parser.getAttributeValue(null, "maxAvgErrorThreshold");
		
		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);
		Log.d(moduleLogName, "maxPlaceDatabaseSize = " + maxPlaceDatabaseSizeString);
		Log.d(moduleLogName, "maxQueueSize = " + maxQueueSizeString);
		Log.d(moduleLogName, "fractionOfQueueAfterReduction = " + fractionOfQueueAfterReductionString);
		Log.d(moduleLogName, "minNumberOfSharedNetworks = " + minNumberOfSharedNetworksString);
		Log.d(moduleLogName, "minPercentOfSharedNetworks = " + minPercentOfSharedNetworksString);
		Log.d(moduleLogName, "maxAvgErrorThreshold = " + maxAvgErrorThresholdString);
		
		// Storing read values
		parameters.wifiPlaceRecognition.useModule = useModuleString.equals("True");
		parameters.wifiPlaceRecognition.maxPlaceDatabaseSize = Integer.parseInt(maxPlaceDatabaseSizeString);
		parameters.wifiPlaceRecognition.maxQueueSize = Integer.parseInt(maxQueueSizeString);
		parameters.wifiPlaceRecognition.fractionOfQueueAfterReduction = Double.parseDouble(fractionOfQueueAfterReductionString);
		parameters.wifiPlaceRecognition.minNumberOfSharedNetworks = Integer.parseInt(minNumberOfSharedNetworksString);
		parameters.wifiPlaceRecognition.minPercentOfSharedNetworks = Double.parseDouble(minPercentOfSharedNetworksString);
		parameters.wifiPlaceRecognition.maxAvgErrorThreshold = Double.parseDouble(maxAvgErrorThresholdString);
				
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, ns, "WiFiPlaceRecognition");
		Log.d(moduleLogName, "</WiFiPlaceRecognition>");
	}
	

	private void skip(XmlPullParser parser) throws XmlPullParserException,
			IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}
}
