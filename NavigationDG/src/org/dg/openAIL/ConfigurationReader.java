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
		INERTIALSENSORS, GRAPHMANAGER, WIFIPLACERECOGNITION, VISUALODOMETRY, VISUALPLACERECOGNITION,
	}
	
	// Class to store all parameters used in OpenAIL
	public class Parameters {
		public class InertialSensors {
			boolean useModule;
			boolean stepometer;
		}
		
		public class WiFiPlaceRecognition {
			boolean useModule;
		}

		InertialSensors inertialSensors = new InertialSensors();
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
		
		Log.d(moduleLogName, "Listing all parameters");
		Log.d(moduleLogName, "InertialSensors: useModule=" + parameters.inertialSensors.useModule + " stepometer=" + parameters.inertialSensors.stepometer);
		Log.d(moduleLogName, "WiFiPlaceRecognition: useModule=" + parameters.wifiPlaceRecognition.useModule);
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
			case INERTIALSENSORS:
				readInertialSensors(parser);
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
		parameters.inertialSensors.useModule = useModuleString.equals("On");
		parameters.inertialSensors.stepometer = useModuleString.equals("On");
		
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, ns, "InertialSensors");
		Log.d(moduleLogName, "</InertialSensors>");
	}

	private void readWiFiPlaceRecognition(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(moduleLogName, "<WiFiPlaceRecognition>");
		parser.require(XmlPullParser.START_TAG, ns, "WiFiPlaceRecognition");

		// Reading attributes
		String useModuleString = parser.getAttributeValue(null, "useModule");

		// Logging those values
		Log.d(moduleLogName, "useModule = " + useModuleString);

		// Storing read values
		parameters.wifiPlaceRecognition.useModule = useModuleString.equals("On");
				
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
