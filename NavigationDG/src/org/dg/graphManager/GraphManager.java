package org.dg.graphManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;


public class GraphManager {
	private static final String moduleLogName = "GraphManager.java";
	
	// Calls to the native part of the code
	public native long NDKGraphCreate();
	public native void NDKGraphAddVertexEdge(long addrGraph, String g2oStream);
	public native double[] NDKGraphGetVertexPosition(long addrGraph, int id);
	public native double[] NDKGraphGetPositionOfAllVertices(long addrGraph);
	public native int NDKGraphOptimize(long addrGraph, int iterationCount, String path);
	public native void NDKGraphDestroy(long addrGraph);

	// It is called on the class initialization
	static {
		System.loadLibrary("GraphOptimizationModule");
	}
	
	// Address of graph
	long addrGraph = 0;
	int currentPoseId = 0;
	boolean started = false;
	boolean continueOptimization = true;
	
	// Thread used in optimization 
	Thread optimizationThread;
	
	// Parameters
	org.dg.openAIL.ConfigurationReader.Parameters.GraphManager parameters;
	
	// File to save current graph
	PrintStream graphStream = null;
	
	// CONSTRUCTORS / DESTRUCTORS
	public GraphManager(org.dg.openAIL.ConfigurationReader.Parameters.GraphManager _parameters) {
		parameters = _parameters;
	}
	
	public void destroyGraphManager() {
		if (addrGraph != 0)
			NDKGraphDestroy(addrGraph);
	}
	
	public void start() {
		started = true;
		
		
		try {
			File folder = new File(Environment.getExternalStorageDirectory()
					+ "/OpenAIL");

			if (!folder.exists()) {
				folder.mkdir();
			}

			File dir = new File(String.format(
					Environment.getExternalStorageDirectory() + "/OpenAIL/Log/"));
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
		Log.d(moduleLogName, "Creating graph");
		addrGraph = NDKGraphCreate();

		Log.d(moduleLogName, "Calling test from file");
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
		Log.d(moduleLogName, "File test : " + fileContent);
	   
		NDKGraphAddVertexEdge(addrGraph, fileContent);
		
		optimize(parameters.optimizeFromFileIterationCount);
	}
	
	
	public void addMultipleWiFiFingerprints(List<org.dg.openAIL.IdPair<Integer, Integer>> foundWiFiFingerprintLinks) {
		String g2oString = "";
		for (org.dg.openAIL.IdPair<Integer, Integer> placeIds : foundWiFiFingerprintLinks)
		{
			String edgeWiFiFingerprint = createWiFiFingerprintEdgeString(placeIds.getFirst(), placeIds.getSecond());
			g2oString = g2oString + edgeWiFiFingerprint;
		}
		save2file(g2oString);
		
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	public void addMultipleVPRMatches(List<org.dg.openAIL.IdPair<Integer, Integer>> foundVPRmatches) {
		String g2oString = "";
		for (org.dg.openAIL.IdPair<Integer, Integer> placeIds : foundVPRmatches)
		{
			String edgeWiFiFingerprint = createVPRVicinityEdgeString(placeIds.getFirst(), placeIds.getSecond());
			g2oString = g2oString + edgeWiFiFingerprint;
		}
		save2file(g2oString);
		
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	
	// Currently disabled - correct it !
	public void addMultipleWiFiMeasurements(List<wiFiMeasurement> wifiList) {
		checkGraphExistance();
		
		String g2oString = "";
		for (wiFiMeasurement measurement: wifiList)
		{
			String edgeWiFi = createWiFiEdgeString(measurement.id, measurement.distance);
			
			g2oString = g2oString + edgeWiFi;
		}
		
		
		save2file(g2oString);
		NDKGraphAddVertexEdge(addrGraph, g2oString);
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
	
	public void getVertexPosition(int id) {
		double [] pos = NDKGraphGetVertexPosition(addrGraph, id);
		
		Log.d(moduleLogName, "Vertex estimate of wanted id: " + pos[0] + " " + pos[1] + " " + pos[2] + " " + pos[3]);
	}
	
	public List<Vertex> getPositionsOfVertices() {
		
		// The list of all vertices
		List<Vertex> vertices = new ArrayList<Vertex>();
		
		// Getting the estimates
		double [] pos = NDKGraphGetPositionOfAllVertices(addrGraph);
		
		// For all estimates
		for (int i=0;i<pos.length;i+=4)
		{
			// Cast id to int
			Double idDouble = pos[i];
			Vertex vertex = new Vertex(idDouble.intValue(), pos[i+1], pos[i+2], pos[i+3]);
			// Add new vertex
			vertices.add(vertex);
		}
		// Return the list of vertices
		return vertices;
	}
	
	public void addVertexWithKnownPosition(int id, double X, double Y, double Z) {
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
		checkGraphExistance();
		final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenAIL/Log/";
		continueOptimization = true;
		
		optimizationThread = new Thread() {
		public void run() {
			
			while ( continueOptimization )
			{
				int res = NDKGraphOptimize(addrGraph, iterationCount, path);
				if (res == 0)
				{
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			Log.d(moduleLogName, "Optimization ended");
		
			//NDKGraphDestroy(addrGraph);
			//Log.d("Main::Activity", "Destroyed graph");
			
			} ;
		};
		optimizationThread.start();
	}
	
	public void stopOptimizationThread() {
		continueOptimization = false;
		try {
			if ( optimizationThread != null )
				optimizationThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		String edgeWiFi ="EDGE_SE2:WIFI " + currentPoseId + " " + id + " " + distance + " " + parameters.informationMatrixOfWiFi + "\n";
		return edgeWiFi;
	}
	
	private String createWiFiFingerprintEdgeString(int id, int id2) {
		String edgeWiFi ="EDGE_SE2:WIFI_FINGERPRINT " + id2 + " " + id + " " + parameters.wifiFingerprintDeadBandRadius +  " " + parameters.informationMatrixOfWiFiFingerprint + "\n";
		return edgeWiFi;
	}
	
	private String createVPRVicinityEdgeString(int id, int id2) {
		String edgeWiFi ="EDGE_SE2:VPR_VICINITY " + id2 + " " + id + " " + parameters.vprVicinityDeadBandRadius +  " " + parameters.informationMatrixOfVPRVicinity + "\n";
		return edgeWiFi;
	}
	
}
