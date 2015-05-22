package org.dg.graphManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

import org.dg.wifi.WiFiPlaceRecognition.IdPair;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;


public class GraphManager {
	
	private static final String moduleLogName = "GraphManager.java";
	
	// Calls to the native part of the code
	public native long NDKGraphCreate();
	public native void NDKGraphAddVertexEdge(long addrGraph, String g2oStream);
	public native void NDKGraphOptimize(long addrGraph, int iterationCount, String path);
	public native void NDKGraphDestroy(long addrGraph);

	// It is called on the class initialization
	static {
		System.loadLibrary("GraphOptimizationModule");
	}
	
	// Address of graph
	long addrGraph = 0;
	int currentPoseId = 0;
	boolean started = false;
	
	// File to save current graph
	PrintStream graphStream = null;
	
	// CONSTRUCTORS / DESTRUCTORS
	public GraphManager() {
	}
	
	public void destroyGraphManager() {
		if (addrGraph != 0)
			NDKGraphDestroy(addrGraph);
	}
	
	public void start() {
		started = true;
		
		
		try {
			File folder = new File(Environment.getExternalStorageDirectory()
					+ "/DG");

			if (!folder.exists()) {
				folder.mkdir();
			}

			File dir = new File(String.format(
					Environment.getExternalStorageDirectory() + "/DG/testGraph/"));
			if (!dir.exists()) {
				dir.mkdirs();
			}

			String fileName = dir.toString() + "/lastCreatedGraph.g2o";
			FileOutputStream fgraphStream;
			fgraphStream = new FileOutputStream(fileName);
			graphStream = new PrintStream(fgraphStream);
			
		} catch (FileNotFoundException e) {
			graphStream = null;
			e.printStackTrace();
		}
	}
	
	public void stop() {
		started = false;
		
		if (graphStream != null)
		{
			graphStream.close();
			graphStream = null;
		}
	}
	
	public boolean started() {
		return started;
	}
	
	private void save2file(String g2oString)
	{
		if (graphStream!=null)
		{
			graphStream.print(g2oString);
		}
	}
	
	
	// PUBLIC CALLS
	public void optimizeGraphInFile(String fileName) {
		Log.d("Main::Activity", "Creating graph");
		addrGraph = NDKGraphCreate();

		Log.d("Main::Activity", "Calling test from file");
		String path2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DG/testGraph/";
		String filePath = path2 + fileName;
		
		
		String fileContent = "";
		try {
			File fl = new File(filePath);
			FileInputStream fin = new FileInputStream(fl);
		
		    BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
		    StringBuilder sb = new StringBuilder();
		    String line = null;
		    while ((line = reader.readLine()) != null) {
		      sb.append(line).append("\n");
		    }
		    reader.close();
		    fileContent = sb.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("Main::Activity", "File test : " + fileContent);
	   
		NDKGraphAddVertexEdge(addrGraph, fileContent);
		
		optimize(100);
	}
	
	
//	public void addMultipleWiFiFingerprints(List<Integer> placesId) {
//		String g2oString = "";
//		for (Integer placeId : placesId)
//		{
//			String edgeWiFiFingerprint = createWiFiFingerprintEdgeString(currentPoseId, placeId);
//			g2oString = g2oString + edgeWiFiFingerprint;
//		}
//		save2file(g2oString);
//		
//		NDKGraphAddVertexEdge(addrGraph, g2oString);
//	}
	
	public void addMultipleWiFiFingerprints(List<IdPair<Integer, Integer>> foundWiFiFingerprintLinks) {
		String g2oString = "";
		for (IdPair<Integer, Integer> placeIds : foundWiFiFingerprintLinks)
		{
			String edgeWiFiFingerprint = createWiFiFingerprintEdgeString(placeIds.getFirst(), placeIds.getSecond());
			g2oString = g2oString + edgeWiFiFingerprint;
		}
		save2file(g2oString);
		
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	
	public void addMultipleWiFiMeasurements(List<wiFiMeasurement> wifiList) {
		checkGraphExistance();
		
		String g2oString = "";
		for (wiFiMeasurement measurement: wifiList)
		{
			String edgeWiFi = createWiFiEdgeString(measurement.id, measurement.distance);
			
			g2oString = g2oString + edgeWiFi;
		}
		
		
		save2file(g2oString);
		//NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
		
	public void addWiFiMeasurement(int id, double distance) {	
		checkGraphExistance();
		
		String edgeWiFi = createWiFiEdgeString(id, distance);
		save2file(edgeWiFi);
		NDKGraphAddVertexEdge(addrGraph, edgeWiFi);
	}
	
	public int getCurrentPoseId()
	{
		return currentPoseId;
	}
	
	
	public void addWiFiPlaceRecognitionVertex(int id, float X, float Y, float Z) {
		checkGraphExistance();
		
		String g2oString = "VERTEX_SE2 " + id + " " + X + " " + Y + " " + Z +"\n";
		save2file(g2oString);
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	
	public void addStepometerMeasurement(double distance, double theta) {
		checkGraphExistance();
		
		currentPoseId++;
		// The information value of angle depends on angle
		float infValueTheta = (float) (Math.PI/2 - theta);
		if ( infValueTheta  < 0.1f )
			infValueTheta = 0.1f;
		
		String informatiomMatrixOfStep = "1.0 0.0 " + infValueTheta;
		String edgeStep ="EDGE_SE2:STEP " + (currentPoseId-1) + " " + currentPoseId + " " + distance + " " + theta + " " + informatiomMatrixOfStep + "\n";
		
		save2file(edgeStep);
		NDKGraphAddVertexEdge(addrGraph, edgeStep);
	}
	
	
	public void optimize(final int iterationCount) {
		final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DG/testGraph/";
		
		Thread t = new Thread() {
		public void run() {
			NDKGraphOptimize(addrGraph, iterationCount, path);
			Log.d(moduleLogName, "Optimization ended");
		
			NDKGraphDestroy(addrGraph);
			Log.d("Main::Activity", "Destroyed graph");
			
			} ;
		};
		t.start();
		
	}
	
	// PRIVATE
	private void checkGraphExistance() {
		if ( addrGraph == 0)
			addrGraph = NDKGraphCreate();
	}
	
	/**
	 * @param measurement
	 * @return
	 */
	private String createWiFiEdgeString(int id, double distance) {
		//double informatiomMatrixOfWifi = 0.3*distance;
		double informatiomMatrixOfWifi = 1.0f;
		String edgeWiFi ="EDGE_SE2:WIFI " + currentPoseId + " " + id + " " + distance + " " + informatiomMatrixOfWifi + "\n";
		return edgeWiFi;
	}
	
	private String createWiFiFingerprintEdgeString(int id, int id2) {
		final float deadBandRadius = 6;
		final float informationMatrixOfWiFiFingerprint = 1.0f;
		String edgeWiFi ="EDGE_SE2:WIFI_FINGERPRINT " + id2 + " " + id + " " + deadBandRadius +  " " + informationMatrixOfWiFiFingerprint + "\n";
		return edgeWiFi;
	}
	
}
