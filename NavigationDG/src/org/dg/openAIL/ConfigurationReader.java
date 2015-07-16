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
		}	
		
		public class InertialSensors {
			public boolean useModule;
			public boolean stepometer;
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
			public boolean usePriorDatabase;
			public String priorDatabaseFile;
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
				"\n--- frequencyOfNewDataQuery=" + parameters.mainProcessing.frequencyOfNewDataQuery);
		
		Log.d(moduleLogName, "InertialSensors:" +
				"\n--- useModule=" + parameters.inertialSensors.useModule + 
				"\n--- stepometer=" + parameters.inertialSensors.stepometer);		
	
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
				"\n--- maxAvgErrorThreshold=" + parameters.wifiPlaceRecognition.maxAvgErrorThreshold + 
				"\n--- usePriorDatabase=" + parameters.wifiPlaceRecognition.usePriorDatabase + 
				"\n--- priorDatabaseFile=" + parameters.wifiPlaceRecognition.priorDatabaseFile);		
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

		// Logging those values
		Log.d(moduleLogName, "frequencyOfNewDataQuery = " + frequencyOfNewDataQueryString);

		// Storing read values
		parameters.mainProcessing.frequencyOfNewDataQuery =  Double.parseDouble(frequencyOfNewDataQueryString);

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
		
		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);
		Log.d(moduleLogName, "stepometer = " + stepometerString);
		
		// Storing read values
		parameters.inertialSensors.useModule = useModuleString.equals("True");
		parameters.inertialSensors.stepometer = useModuleString.equals("True");
		
		parser.nextTag();
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
		String usePriorDatabaseString = parser.getAttributeValue(null, "usePriorDatabase");
		parameters.wifiPlaceRecognition.priorDatabaseFile = parser.getAttributeValue(null, "priorDatabaseFile");
		
		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);
		Log.d(moduleLogName, "maxPlaceDatabaseSize = " + maxPlaceDatabaseSizeString);
		Log.d(moduleLogName, "maxQueueSize = " + maxQueueSizeString);
		Log.d(moduleLogName, "fractionOfQueueAfterReduction = " + fractionOfQueueAfterReductionString);
		Log.d(moduleLogName, "minNumberOfSharedNetworks = " + minNumberOfSharedNetworksString);
		Log.d(moduleLogName, "minPercentOfSharedNetworks = " + minPercentOfSharedNetworksString);
		Log.d(moduleLogName, "maxAvgErrorThreshold = " + maxAvgErrorThresholdString);
		Log.d(moduleLogName, "usePriorDatabase = " + parameters.wifiPlaceRecognition.usePriorDatabase);
		Log.d(moduleLogName, "priorDatabaseFile = " + parameters.wifiPlaceRecognition.priorDatabaseFile);
	
		// Storing read values
		parameters.wifiPlaceRecognition.useModule = useModuleString.equals("True");
		parameters.wifiPlaceRecognition.maxPlaceDatabaseSize = Integer.parseInt(maxPlaceDatabaseSizeString);
		parameters.wifiPlaceRecognition.maxQueueSize = Integer.parseInt(maxQueueSizeString);
		parameters.wifiPlaceRecognition.fractionOfQueueAfterReduction = Double.parseDouble(fractionOfQueueAfterReductionString);
		parameters.wifiPlaceRecognition.minNumberOfSharedNetworks = Integer.parseInt(minNumberOfSharedNetworksString);
		parameters.wifiPlaceRecognition.minPercentOfSharedNetworks = Double.parseDouble(minPercentOfSharedNetworksString);
		parameters.wifiPlaceRecognition.maxAvgErrorThreshold = Double.parseDouble(maxAvgErrorThresholdString);
		parameters.wifiPlaceRecognition.usePriorDatabase = usePriorDatabaseString.equals("True");
				
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
