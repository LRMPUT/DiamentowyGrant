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
import java.io.FileInputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class ConfigurationReader {

	private static final String moduleLogName = "ConfigurationReader";

	public enum Modules {
		MAINPROCESSING, PLAYBACK, INERTIALSENSORS, GRAPHMANAGER, WIFIPLACERECOGNITION, VISUALODOMETRY, VISUALPLACERECOGNITION,
	}

	// Class to store all parameters used in OpenAIL
	public class Parameters {
		public class MainProcessing {
			double frequencyOfNewDataQuery;
			String priorMapName;
			public boolean usePriorMap;
			int imageCaptureStep;
			double imageCaptureVarianceThreshold;
			public boolean useNavigation;
			public boolean showMapWithoutMapConnection;
		}

		public class Playback {
			public double simulationSpeed;
			public double inertialMaxDelay;
			public long inertialSleepTimeInMs;
			public long wifiSleepTimeInMs;
		}

		public class InertialSensors {
			public class Record {
				public boolean accelerometer, gyroscope, magnetometer,
						barometer, accelerometerWithoutGravity;
				public boolean orientationAndroid, orientationAndroidEuler,
						orientationAEKF, orientationAEKFEuler, orientationCF,
						orientationCFEuler;
			}

			public class Stepometer {
				public int verbose;
				public double minFrequency, maxFrequency;
				public double stepSize;
				public int windowSize;
			}

			public boolean useModule;
			public boolean useStepometer;
			public boolean verticalOrientation;
			public double priorMapStepometerBiasVertical;
			public double priorMapStepometerBiasHorizontal;
			public Record record = new Record();
			public Stepometer stepometer = new Stepometer();
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
			public boolean recordRawData;
			public int maxPlaceDatabaseSize;
			public int maxQueueSize;
			public double fractionOfQueueAfterReduction;
			public int minNumberOfSharedNetworks;
			public double minPercentOfSharedNetworks;
			public double maxAvgErrorThreshold;
			public boolean directWiFiMeasurements;
			public boolean addUserWiFiToRecognition;
		}

		MainProcessing mainProcessing = new MainProcessing();
		Playback playback = new Playback();
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

		Log.d(moduleLogName, "MainProcessing:"
				+ "\n--- frequencyOfNewDataQuery="
				+ parameters.mainProcessing.frequencyOfNewDataQuery
				+ "\n--- priorMapName="
				+ parameters.mainProcessing.priorMapName + "\n--- usePriorMap="
				+ parameters.mainProcessing.usePriorMap
				+ "\n--- imageCaptureStep="
				+ parameters.mainProcessing.imageCaptureStep
				+ "\n--- imageCaptureVarianceThreshold="
				+ parameters.mainProcessing.imageCaptureVarianceThreshold
				+ "\n--- useNavigation="
				+ parameters.mainProcessing.useNavigation
				+ "\n--- showMapWithoutMapConnection="
				+ parameters.mainProcessing.showMapWithoutMapConnection);

		Log.d(moduleLogName, "Playback:" + "\n--- SimulationSpeed="
				+ parameters.playback.simulationSpeed
				+ "\n--- inertialSleepTimeInMs="
				+ parameters.playback.inertialSleepTimeInMs
				+ "\n--- wifiSleepTimeInMs="
				+ parameters.playback.wifiSleepTimeInMs
				+ "\n--- inertialMaxDelay="
				+ parameters.playback.inertialMaxDelay);

		Log.d(moduleLogName, "InertialSensors:" + "\n--- useModule="
				+ parameters.inertialSensors.useModule + "\n--- useStepometer="
				+ parameters.inertialSensors.useStepometer
				+ "\n--- verticalOrientation="
				+ parameters.inertialSensors.verticalOrientation
				+ "\n--- priorMapStepometerBiasVertical="
				+ parameters.inertialSensors.priorMapStepometerBiasVertical
				+ "\n--- priorMapStepometerBiasHorizontal="
				+ parameters.inertialSensors.priorMapStepometerBiasHorizontal);

		Log.d(moduleLogName, "--- Record:" + "\n------ accelerometer="
				+ parameters.inertialSensors.record.accelerometer
				+ "\n------ gyroscope="
				+ parameters.inertialSensors.record.gyroscope
				+ "\n------ magnetometer="
				+ parameters.inertialSensors.record.magnetometer
				+ "\n------ barometer="
				+ parameters.inertialSensors.record.barometer
				+ "\n------ accelerometerWithoutGravity="
				+ parameters.inertialSensors.record.accelerometerWithoutGravity
				+

				"\n------ orientationAndroid="
				+ parameters.inertialSensors.record.orientationAndroid
				+ "\n------ orientationAndroidEuler="
				+ parameters.inertialSensors.record.orientationAndroidEuler
				+ "\n------ orientationAEKF="
				+ parameters.inertialSensors.record.orientationAEKF
				+ "\n------ orientationAEKFEuler="
				+ parameters.inertialSensors.record.orientationAEKFEuler
				+ "\n------ orientationCF="
				+ parameters.inertialSensors.record.orientationCF
				+ "\n------ orientationCFEuler="
				+ parameters.inertialSensors.record.orientationCFEuler);

		Log.d(moduleLogName, "--- Stepometer:" + "\n------ verbose="
				+ parameters.inertialSensors.stepometer.verbose
				+ "\n------ minFrequency="
				+ parameters.inertialSensors.stepometer.minFrequency
				+ "\n------ maxFrequency="
				+ parameters.inertialSensors.stepometer.maxFrequency
				+ "\n------ stepSize="
				+ parameters.inertialSensors.stepometer.stepSize
				+ "\n------ windowSize="
				+ parameters.inertialSensors.stepometer.windowSize);

		Log.d(moduleLogName, "GraphManager:"
				+ "\n--- vprVicinityDeadBandRadius="
				+ parameters.graphManager.vprVicinityDeadBandRadius
				+ "\n--- informationMatrixOfVPRVicinity="
				+ parameters.graphManager.informationMatrixOfVPRVicinity
				+ "\n--- wifiFingerprintDeadBandRadius="
				+ parameters.graphManager.wifiFingerprintDeadBandRadius
				+ "\n--- informationMatrixOfWiFiFingerprint="
				+ parameters.graphManager.informationMatrixOfWiFiFingerprint
				+ "\n--- informationMatrixOfWiFi="
				+ parameters.graphManager.informationMatrixOfWiFi
				+ "\n--- optimizeFromFileIterationCount="
				+ parameters.graphManager.optimizeFromFileIterationCount);

		Log.d(moduleLogName, "WiFiPlaceRecognition:" + "\n--- useModule="
				+ parameters.wifiPlaceRecognition.useModule
				+ "\n--- recordRawData="
				+ parameters.wifiPlaceRecognition.recordRawData
				+ "\n--- maxPlaceDatabaseSize="
				+ parameters.wifiPlaceRecognition.maxPlaceDatabaseSize
				+ "\n--- maxQueueSize="
				+ parameters.wifiPlaceRecognition.maxQueueSize
				+ "\n--- fractionOfQueueAfterReduction="
				+ parameters.wifiPlaceRecognition.fractionOfQueueAfterReduction
				+ "\n--- minNumberOfSharedNetworks="
				+ parameters.wifiPlaceRecognition.minNumberOfSharedNetworks
				+ "\n--- minPercentOfSharedNetworks="
				+ parameters.wifiPlaceRecognition.minPercentOfSharedNetworks
				+ "\n--- maxAvgErrorThreshold="
				+ parameters.wifiPlaceRecognition.maxAvgErrorThreshold
				+ "\n--- addUserWiFiToRecognition="
				+ parameters.wifiPlaceRecognition.addUserWiFiToRecognition);
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
			case PLAYBACK:
				readPlayback(parser);
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
		String frequencyOfNewDataQueryString = parser.getAttributeValue(null,
				"frequencyOfNewDataQuery");
		parameters.mainProcessing.priorMapName = parser.getAttributeValue(null,
				"priorMapName");
		String usePriorMapString = parser
				.getAttributeValue(null, "usePriorMap");
		String imageCaptureStepString = parser.getAttributeValue(null,
				"imageCaptureStep");
		String imageCaptureVarianceThresholdString = parser.getAttributeValue(
				null, "imageCaptureVarianceThreshold");
		String useNavigationString = parser.getAttributeValue(null,
				"useNavigation");
		String showMapWithoutMapConnectionString = parser.getAttributeValue(
				null, "showMapWithoutMapConnection");

		// Logging those values
		Log.d(moduleLogName, "frequencyOfNewDataQuery = "
				+ frequencyOfNewDataQueryString);
		Log.d(moduleLogName, "priorMapName = "
				+ parameters.mainProcessing.priorMapName);
		Log.d(moduleLogName, "usePriorMap = " + usePriorMapString);
		Log.d(moduleLogName, "imageCaptureStep = " + imageCaptureStepString);
		Log.d(moduleLogName, "imageCaptureVarianceThreshold = "
				+ imageCaptureVarianceThresholdString);
		Log.d(moduleLogName, "useNavigation = " + useNavigationString);
		Log.d(moduleLogName, "showMapWithoutMapConnection = "
				+ showMapWithoutMapConnectionString);

		// Storing read values
		parameters.mainProcessing.frequencyOfNewDataQuery = Double
				.parseDouble(frequencyOfNewDataQueryString);
		parameters.mainProcessing.usePriorMap = usePriorMapString
				.equals("True");
		parameters.mainProcessing.imageCaptureStep = Integer
				.parseInt(imageCaptureStepString);
		parameters.mainProcessing.imageCaptureVarianceThreshold = Double
				.parseDouble(imageCaptureVarianceThresholdString);
		parameters.mainProcessing.useNavigation = useNavigationString
				.equals("True");
		parameters.mainProcessing.showMapWithoutMapConnection = showMapWithoutMapConnectionString
				.equals("True");

		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, ns, "MainProcessing");
		Log.d(moduleLogName, "</MainProcessing>");
	}

	// Parses the contents of an entry.
	private void readPlayback(XmlPullParser parser)
			throws XmlPullParserException, IOException {

		Log.d(moduleLogName, "<Playback>");
		parser.require(XmlPullParser.START_TAG, ns, "Playback");

		// Reading attributes
		String simulationSpeedString = parser.getAttributeValue(null,
				"simulationSpeed");

		String inertialSleepTimeInMsString = parser.getAttributeValue(null,
				"inertialSleepTimeInMs");
		String wifiSleepTimeInMsString = parser.getAttributeValue(null,
				"wifiSleepTimeInMs");
		String inertialMaxDelayString = parser.getAttributeValue(null,
				"inertialMaxDelay");

		// Logging those values
		Log.d(moduleLogName, "simulationSpeed = " + simulationSpeedString);
		Log.d(moduleLogName, "inertialSleepTimeInMs = "
				+ inertialSleepTimeInMsString);
		Log.d(moduleLogName, "wifiSleepTimeInMs = " + wifiSleepTimeInMsString);
		Log.d(moduleLogName, "inertialMaxDelay = " + inertialMaxDelayString);

		// Storing read values
		parameters.playback.simulationSpeed = Double
				.parseDouble(simulationSpeedString);
		parameters.playback.inertialSleepTimeInMs = Long
				.parseLong(inertialSleepTimeInMsString);
		parameters.playback.wifiSleepTimeInMs = Long
				.parseLong(wifiSleepTimeInMsString);
		parameters.playback.inertialMaxDelay = Double
				.parseDouble(inertialMaxDelayString);

		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, ns, "Playback");
		Log.d(moduleLogName, "</Playback>");
	}

	// Parses the contents of an entry.
	private void readInertialSensors(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(moduleLogName, "<InertialSensors>");
		parser.require(XmlPullParser.START_TAG, ns, "InertialSensors");

		// Reading attributes
		String useModuleString = parser.getAttributeValue(null, "useModule");
		String useStepometerString = parser.getAttributeValue(null,
				"useStepometer");
		String verticalOrientationString = parser.getAttributeValue(null,
				"verticalOrientation");
		String priorMapStepometerBiasVerticalString = parser.getAttributeValue(
				null, "priorMapStepometerBiasVertical");
		String priorMapStepometerBiasHorizontalString = parser
				.getAttributeValue(null, "priorMapStepometerBiasHorizontal");

		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);
		Log.d(moduleLogName, "useStepometer = " + useStepometerString);
		Log.d(moduleLogName, "verticalOrientation = "
				+ verticalOrientationString);
		Log.d(moduleLogName, "priorMapStepometerBiasVertical = "
				+ priorMapStepometerBiasVerticalString);
		Log.d(moduleLogName, "priorMapStepometerBiasHorizontal = "
				+ priorMapStepometerBiasHorizontalString);

		// Storing read values
		parameters.inertialSensors.useModule = useModuleString.equals("True");
		parameters.inertialSensors.useStepometer = useModuleString
				.equals("True");
		parameters.inertialSensors.verticalOrientation = verticalOrientationString
				.equals("True");
		parameters.inertialSensors.priorMapStepometerBiasVertical = Double
				.parseDouble(priorMapStepometerBiasVerticalString);
		parameters.inertialSensors.priorMapStepometerBiasHorizontal = Double
				.parseDouble(priorMapStepometerBiasHorizontalString);

		// Reading record parameters
		while (parser.next() != XmlPullParser.END_TAG) {

			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			Log.d(moduleLogName, "<Record>");
			parser.require(XmlPullParser.START_TAG, ns, "Record");

			// Reading attributes
			String accelerometerString = parser.getAttributeValue(null,
					"accelerometer");
			String gyroscopeString = parser
					.getAttributeValue(null, "gyroscope");
			String magnetometerString = parser.getAttributeValue(null,
					"magnetometer");
			String barometerString = parser
					.getAttributeValue(null, "barometer");
			String accelerometerWithoutGravityString = parser
					.getAttributeValue(null, "accelerometerWithoutGravity");

			String orientationAndroidString = parser.getAttributeValue(null,
					"orientationAndroid");
			String orientationAndroidEulerString = parser.getAttributeValue(
					null, "orientationAndroidEuler");
			String orientationAEKFString = parser.getAttributeValue(null,
					"orientationAEKF");
			String orientationAEKFEulerString = parser.getAttributeValue(null,
					"orientationAEKFEuler");
			String orientationCFString = parser.getAttributeValue(null,
					"orientationCF");
			String orientationCFEulerString = parser.getAttributeValue(null,
					"orientationCFEuler");

			// Logging those values
			Log.d(moduleLogName, "accelerometer = " + accelerometerString);
			Log.d(moduleLogName, "gyroscope = " + gyroscopeString);
			Log.d(moduleLogName, "magnetometer = " + magnetometerString);
			Log.d(moduleLogName, "barometer = " + barometerString);
			Log.d(moduleLogName, "accelerometerWithoutGravity = "
					+ accelerometerWithoutGravityString);

			Log.d(moduleLogName, "orientationAndroid = "
					+ orientationAndroidString);
			Log.d(moduleLogName, "orientationAndroidEuler = "
					+ orientationAndroidEulerString);
			Log.d(moduleLogName, "orientationAEKF = " + orientationAEKFString);
			Log.d(moduleLogName, "orientationAEKFEuler = "
					+ orientationAEKFEulerString);
			Log.d(moduleLogName, "orientationCF = " + orientationCFString);
			Log.d(moduleLogName, "orientationCFEuler = "
					+ orientationCFEulerString);

			// Storing read values
			parameters.inertialSensors.record.accelerometer = accelerometerString
					.equals("True");
			parameters.inertialSensors.record.gyroscope = gyroscopeString
					.equals("True");
			parameters.inertialSensors.record.magnetometer = magnetometerString
					.equals("True");
			parameters.inertialSensors.record.barometer = barometerString
					.equals("True");
			parameters.inertialSensors.record.accelerometerWithoutGravity = accelerometerWithoutGravityString
					.equals("True");

			parameters.inertialSensors.record.orientationAndroid = orientationAndroidString
					.equals("True");
			parameters.inertialSensors.record.orientationAndroidEuler = orientationAndroidEulerString
					.equals("True");
			parameters.inertialSensors.record.orientationAEKF = orientationAEKFString
					.equals("True");
			parameters.inertialSensors.record.orientationAEKFEuler = orientationAEKFEulerString
					.equals("True");
			parameters.inertialSensors.record.orientationCF = orientationCFString
					.equals("True");
			parameters.inertialSensors.record.orientationCFEuler = orientationCFEulerString
					.equals("True");

			parser.nextTag();
			parser.require(XmlPullParser.END_TAG, ns, "Record");
			Log.d(moduleLogName, "</Record>");
			break;
		}

		// Reading stepometer parameters
		while (parser.next() != XmlPullParser.END_TAG) {

			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			Log.d(moduleLogName, "<Stepometer>");
			parser.require(XmlPullParser.START_TAG, ns, "Stepometer");

			// Reading attributes
			String verboseString = parser.getAttributeValue(null, "verbose");
			String minFrequencyString = parser.getAttributeValue(null,
					"minFrequency");
			String maxFrequencyString = parser.getAttributeValue(null,
					"maxFrequency");
			String stepSizeString = parser.getAttributeValue(null, "stepSize");
			String windowSizeString = parser.getAttributeValue(null, "windowSize");

			// Logging those values
			Log.d(moduleLogName, "verbose = " + verboseString);
			Log.d(moduleLogName, "minFrequency = " + minFrequencyString);
			Log.d(moduleLogName, "maxFrequency = " + maxFrequencyString);
			Log.d(moduleLogName, "stepSize = " + stepSizeString);
			Log.d(moduleLogName, "windowSize = " + windowSizeString);

			// Storing read values
			parameters.inertialSensors.stepometer.verbose = Integer
					.parseInt(verboseString);
			parameters.inertialSensors.stepometer.minFrequency = Double
					.parseDouble(minFrequencyString);
			parameters.inertialSensors.stepometer.maxFrequency = Double
					.parseDouble(maxFrequencyString);
			parameters.inertialSensors.stepometer.stepSize = Double
					.parseDouble(stepSizeString);
			parameters.inertialSensors.stepometer.windowSize = Integer
					.parseInt(windowSizeString);

			parser.nextTag();
			parser.require(XmlPullParser.END_TAG, ns, "Stepometer");
			Log.d(moduleLogName, "</Stepometer>");

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
		String vprVicinityDeadBandRadiusString = parser.getAttributeValue(null,
				"vprVicinityDeadBandRadius");
		String informationMatrixOfVPRVicinityString = parser.getAttributeValue(
				null, "informationMatrixOfVPRVicinity");
		String wifiFingerprintDeadBandRadiusString = parser.getAttributeValue(
				null, "wifiFingerprintDeadBandRadius");
		String informationMatrixOfWiFiFingerprintString = parser
				.getAttributeValue(null, "informationMatrixOfWiFiFingerprint");
		String informationMatrixOfWiFiString = parser.getAttributeValue(null,
				"informationMatrixOfWiFi");
		String optimizeFromFileIterationCountString = parser.getAttributeValue(
				null, "optimizeFromFileIterationCount");

		// Logging those values
		Log.d(moduleLogName, "vprVicinityDeadBandRadius = "
				+ vprVicinityDeadBandRadiusString);
		Log.d(moduleLogName, "informationMatrixOfVPRVicinity = "
				+ informationMatrixOfVPRVicinityString);
		Log.d(moduleLogName, "wifiFingerprintDeadBandRadius = "
				+ wifiFingerprintDeadBandRadiusString);
		Log.d(moduleLogName, "informationMatrixOfWiFiFingerprint = "
				+ informationMatrixOfWiFiFingerprintString);
		Log.d(moduleLogName, "informationMatrixOfWiFi = "
				+ informationMatrixOfWiFiString);
		Log.d(moduleLogName, "optimizeFromFileIterationCount = "
				+ optimizeFromFileIterationCountString);

		// Storing read values
		parameters.graphManager.vprVicinityDeadBandRadius = Double
				.parseDouble(vprVicinityDeadBandRadiusString);
		parameters.graphManager.informationMatrixOfVPRVicinity = Double
				.parseDouble(informationMatrixOfVPRVicinityString);
		parameters.graphManager.wifiFingerprintDeadBandRadius = Double
				.parseDouble(wifiFingerprintDeadBandRadiusString);
		parameters.graphManager.informationMatrixOfWiFiFingerprint = Double
				.parseDouble(informationMatrixOfWiFiFingerprintString);
		parameters.graphManager.informationMatrixOfWiFi = Double
				.parseDouble(informationMatrixOfWiFiString);
		parameters.graphManager.optimizeFromFileIterationCount = Integer
				.parseInt(optimizeFromFileIterationCountString);

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
		String recordRawDataString = parser.getAttributeValue(null,
				"recordRawData");
		String maxPlaceDatabaseSizeString = parser.getAttributeValue(null,
				"maxPlaceDatabaseSize");
		String maxQueueSizeString = parser.getAttributeValue(null,
				"maxQueueSize");
		String fractionOfQueueAfterReductionString = parser.getAttributeValue(
				null, "fractionOfQueueAfterReduction");
		String minNumberOfSharedNetworksString = parser.getAttributeValue(null,
				"minNumberOfSharedNetworks");
		String minPercentOfSharedNetworksString = parser.getAttributeValue(
				null, "minPercentOfSharedNetworks");
		String maxAvgErrorThresholdString = parser.getAttributeValue(null,
				"maxAvgErrorThreshold");
		String directWiFiMeasurementsString = parser.getAttributeValue(null,
				"directWiFiMeasurements");
		String addUserWiFiToRecognitionString = parser.getAttributeValue(null,
				"addUserWiFiToRecognition");

		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);
		Log.d(moduleLogName, "recordRawData = " + recordRawDataString);
		Log.d(moduleLogName, "maxPlaceDatabaseSize = "
				+ maxPlaceDatabaseSizeString);
		Log.d(moduleLogName, "maxQueueSize = " + maxQueueSizeString);
		Log.d(moduleLogName, "fractionOfQueueAfterReduction = "
				+ fractionOfQueueAfterReductionString);
		Log.d(moduleLogName, "minNumberOfSharedNetworks = "
				+ minNumberOfSharedNetworksString);
		Log.d(moduleLogName, "minPercentOfSharedNetworks = "
				+ minPercentOfSharedNetworksString);
		Log.d(moduleLogName, "maxAvgErrorThreshold = "
				+ maxAvgErrorThresholdString);
		Log.d(moduleLogName, "directWiFiMeasurements = "
				+ directWiFiMeasurementsString);
		Log.d(moduleLogName, "addUserWiFiToRecognition = "
				+ addUserWiFiToRecognitionString);

		// Storing read values
		parameters.wifiPlaceRecognition.useModule = useModuleString
				.equals("True");
		parameters.wifiPlaceRecognition.recordRawData = recordRawDataString
				.equals("True");
		parameters.wifiPlaceRecognition.maxPlaceDatabaseSize = Integer
				.parseInt(maxPlaceDatabaseSizeString);
		parameters.wifiPlaceRecognition.maxQueueSize = Integer
				.parseInt(maxQueueSizeString);
		parameters.wifiPlaceRecognition.fractionOfQueueAfterReduction = Double
				.parseDouble(fractionOfQueueAfterReductionString);
		parameters.wifiPlaceRecognition.minNumberOfSharedNetworks = Integer
				.parseInt(minNumberOfSharedNetworksString);
		parameters.wifiPlaceRecognition.minPercentOfSharedNetworks = Double
				.parseDouble(minPercentOfSharedNetworksString);
		parameters.wifiPlaceRecognition.maxAvgErrorThreshold = Double
				.parseDouble(maxAvgErrorThresholdString);
		parameters.wifiPlaceRecognition.directWiFiMeasurements = directWiFiMeasurementsString
				.equals("True");
		parameters.wifiPlaceRecognition.addUserWiFiToRecognition = addUserWiFiToRecognitionString
				.equals("True");

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
