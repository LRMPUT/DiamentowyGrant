package org.dg.openAIL;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class ConfigurationReader {
	
	private static final String moduleLogName = "ConfigurationReader";
	
	public class Parameters {
		boolean accelerometer;
		int stepometer;
	}
	
	Parameters parameters = new Parameters();
	
	Parameters getParameters() {
		return parameters;
	}
	

	private static final String ns = null;

	public Parameters readParameters(String path) throws XmlPullParserException,
			IOException {
		
		Log.d(moduleLogName, "A0");
		
		FileInputStream ifs = new FileInputStream(new File(path));
		
		Log.d(moduleLogName, "A");
		 
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(ifs, null);
			parser.nextTag();
			
			Log.d(moduleLogName, "A2");
			
			readFeed(parser);
			
			Log.d(moduleLogName, "A3");
		} catch (Exception e) {
			return null;
		}
		finally {
			ifs.close();
		}
		
		return parameters;
	}

	private void readFeed(XmlPullParser parser) throws XmlPullParserException,
			IOException {
		
		parser.require(XmlPullParser.START_TAG, ns, "OpenAIL");
		while (parser.next() != XmlPullParser.END_TAG) {
			Log.d(moduleLogName, "A21");
			
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				Log.d(moduleLogName, "WTF");
				continue;
			}
			Log.d(moduleLogName, "A21_A");
			String name = parser.getName();
		
			Log.d(moduleLogName, "A21_B: " + name);
			// Starts by looking for the entry tag
			if (name.equals("InertialSensors")) {
				readInertialSensors(parser);
			} else {
			//	skip(parser);
			}
			
			Log.d(moduleLogName, "A22");
		}
		Log.d(moduleLogName, "A23");
	}
	  
	// Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
	// to their respective "read" methods for processing. Otherwise, skips the tag.
	private void readInertialSensors(XmlPullParser parser) throws XmlPullParserException, IOException {
		
		 Log.d(moduleLogName, "A21_1");
		 
	    parser.require(XmlPullParser.START_TAG, ns, "InertialSensors");
	    String stepometerString = parser.getAttributeValue(null, "stepometer");  
	    
	    Log.d(moduleLogName, "A21_2 " + stepometerString);
	    
	    String val = readText(parser);
	    
	    
	    Log.d(moduleLogName, "A21_3 " + val);
	    
	  //  parameters.stepometer = Integer.parseInt(stepometerString);
	    parser.require(XmlPullParser.END_TAG, ns, "InertialSensors");
	    
	    Log.d(moduleLogName, "A21_4");
	   
	}

	// Read text values.
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}


private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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
