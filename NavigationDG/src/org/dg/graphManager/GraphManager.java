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

import org.opencv.core.Point3;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;


public class GraphManager {
	// Name for logger
	static final String moduleLogName = "GraphManager.java";
	
	// Loading NDK graph module
	static {
		System.loadLibrary("GraphOptimizationModule");
	}
	
	/* Calls to the native part of the code
	 * - long NDKGraphCreate() 
	 * 		-> Creates graph 
	 * - void NDKGraphAddVertexEdge(long addrGraph, String g2oStream) 
	 * 		-> adds vertex/add to graph
	 * - double[] NDKGraphGetVertexPosition(long addrGraph, int id) 
	 * 		-> gets position of single vertex
	 * - double[] NDKGraphGetPositionOfAllVertices(long addrGraph) 
	 * 		-> gets positions of all vertices
	 * - int NDKGraphOptimize(long addrGraph, int iterationCount, String path) 
	 * 		-> optimizes graph for set of iterations and saves file
	 * - void NDKGraphDestroy(long addrGraph) 
	 * 		-> cleaning 
	 */
	public native long NDKGraphCreate();
	public native void NDKGraphAddVertexEdge(long addrGraph, String g2oStream);
	public native double[] NDKGraphGetVertexPosition(long addrGraph, int id);
	public native double[] NDKGraphGetPositionOfAllVertices(long addrGraph);
	public native int NDKGraphOptimize(long addrGraph, int iterationCount, String path);
	public native void NDKGraphDestroy(long addrGraph);
	
	// Address in memory of graph structure
	long addrGraph = 0;
	
	// Id of the last user vertex
	int currentPoseId = 0;
	
	// The starting ids of qr edges
	int qrCodeId = 20000;
	
	// Start/Stop information
	boolean optimizationInProgress = false;
	
	// If there is new information about current estimates
	public boolean changeInOptimizedData = false;
	
	// Thread used in optimization 
	Thread optimizationThread;
	
	// Parameters
	org.dg.openAIL.ConfigurationReader.Parameters.GraphManager parameters;
	
	// File to save current graph
	PrintStream graphStream = null;
	
	// Current estimate
	static final Object currentEstimateMtx = new Object();
	List<Vertex> currentEstimate;
	
	///
	/// Methods -- creation/start/destruction
	///
	
	/*
	 * Creates graph class and loads parameters
	 */
	public GraphManager(org.dg.openAIL.ConfigurationReader.Parameters.GraphManager _parameters) {
		parameters = _parameters;
	}
	
	/*
	 * Cleaning
	 */
	public void destroyGraph() {
		if (addrGraph != 0)
			NDKGraphDestroy(addrGraph);
	}
	
	/*
	 * Prepares graph for processing - creates graph in NDK and start optimization thread
	 */
	public void start() {
		Log.d(moduleLogName, "start()");
		
		try {
			File folder = new File(Environment.getExternalStorageDirectory()
					+ "/OpenAIL");

			if (!folder.exists()) {
				folder.mkdir();
			}

			File dir = new File(String.format(
					Environment.getExternalStorageDirectory() + "/OpenAIL/GraphLog/"));
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
		
		startOptimizeOnlineThread();
	}
	
	/*
	 * Can be used to check if processing has been started
	 */
	public boolean isOptimizationInProgress() {
		return optimizationInProgress;
	}
	
	/*
	 * We stop the optimization thread
	 */
	public void stopOptimizationThread() {
			
		optimizationInProgress = false;
		try {
			if ( optimizationThread != null )
				optimizationThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		destroyGraph();
		
		if (graphStream != null)
		{
			graphStream.close();
			graphStream = null;
		}
		
	}
	
	/*
	 * Optimizes graph stored in a file provided as a parameter 
	 */
	public void optimizeGraphInFile(String fileName) {
		Log.d(moduleLogName, "Creating graph");
		addrGraph = NDKGraphCreate();

		Log.d(moduleLogName, "Calling test from file");
		String path2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenAIL/GraphLog/";
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
		
		optimizeLoadedGraph(parameters.optimizeFromFileIterationCount);
		
		destroyGraph();
	}
	
	///
	/// Methods -- adding positions and measurements
	/// 
	
	/* 
	 * adds Vertex with position (X,Y,Z), e.g. for map points
	 */
	public void addVertexWithKnownPosition(int id, double X, double Y, double Z) {
		Log.d(moduleLogName, "addVertexWithKnownPosition("+id + ", " + X + ", " + Y + ", " + Z + ")");
		
		checkGraphExistance();
		
		String g2oString = "VERTEX_SE2 " + id + " " + X + " " + Y + " " + Z +"\n";
		save2file(g2oString);
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	/*
	 * adds the stepometer measurement to the graph
	 */
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
	
	/*
	 * adds WiFi fingerprint matches to the graph
	 */
	public void addMultipleWiFiFingerprints(List<org.dg.openAIL.IdPair<Integer, Integer>> foundWiFiFingerprintMatches) {
		String g2oString = "";
		for (org.dg.openAIL.IdPair<Integer, Integer> placeIds : foundWiFiFingerprintMatches)
		{
			String edgeWiFiFingerprint = createWiFiFingerprintEdgeString(placeIds.getFirst(), placeIds.getSecond());
			g2oString = g2oString + edgeWiFiFingerprint;
		}
		save2file(g2oString);
		
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	/*
	 * adds VPR matches to the graph
	 */
	public void addMultipleVPRMatches(List<org.dg.openAIL.IdPair<Integer, Integer>> foundVPRMatches) {
		String g2oString = "";
		for (org.dg.openAIL.IdPair<Integer, Integer> placeIds : foundVPRMatches)
		{
			String edgeWiFiFingerprint = createVPRVicinityEdgeString(placeIds.getFirst(), placeIds.getSecond());
			g2oString = g2oString + edgeWiFiFingerprint;
		}
		save2file(g2oString);
		
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	/*
	 * adds QR matches to the graph
	 */
	public void addMultipleQRCodes(List<Pair<Integer, Point3>> listOfPositions) {
		checkGraphExistance();
		
		String g2oString = "";
		for (Pair<Integer, Point3> measurement: listOfPositions)
		{
			addVertexWithKnownPosition(qrCodeId, measurement.second.x, measurement.second.y, measurement.second.z);
			String edgeQR = createQREdgeString(qrCodeId, measurement.first);
			
			g2oString = g2oString + edgeQR;
			qrCodeId++;
		}
		
		Log.d(moduleLogName, g2oString);
		
		save2file(g2oString);
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
	
	/*
	 * adds WiFi measurements to the graph
	 */
	public void addMultipleWiFiMeasurements(List<wiFiMeasurement> wiFiMeasurementList) {
		checkGraphExistance();
		
		String g2oString = "";
		for (wiFiMeasurement measurement: wiFiMeasurementList)
		{
			String edgeWiFi = createWiFiEdgeString(measurement.id, measurement.distance);
			
			g2oString = g2oString + edgeWiFi;
		}
		
		
		save2file(g2oString);
		NDKGraphAddVertexEdge(addrGraph, g2oString);
	}
		
	///
	/// Methods -- getting ids and estiamtes
	///
	
	/*
	 * getting the id of the current user position
	 */
	public int getCurrentPoseId()
	{
		return currentPoseId;
	}
	
	/*
	 * returns the current estimates of vertices
	 */
	public List<Vertex> getVerticesEstimates() {
		synchronized (currentEstimateMtx) {
			return currentEstimate;
		}
	}

	///
	/// Methods -- private
	///
	
	/*
	 * Checks and creates graph if id does not exist
	 */
	private void checkGraphExistance() {
		Log.d(moduleLogName, "checkGraphExistance()");
		if ( addrGraph == 0)
			addrGraph = NDKGraphCreate();
	}
	
	/*
	 * Method used to optimize loaded graph in separate thread
	 */
	private void optimizeLoadedGraph(final int iterationCount) {
		checkGraphExistance();
		final String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/OpenAIL/GraphLog/";
		optimizationInProgress = true;

		optimizationThread = new Thread() {
			public void run() {

				int res = NDKGraphOptimize(addrGraph, iterationCount, path);

				synchronized (currentEstimateMtx) {
					currentEstimate = getVerticesEstimatesFromNDK();
					changeInOptimizedData = true;
				}

				Log.d(moduleLogName, "Optimization ended");

				destroyGraph();

			};
		};
		optimizationThread.start();

		try {
			Thread.sleep(100, 0);
			optimizationThread.join();
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "optimizationThread.join() - failed");
		}
	}
	
	/*
	 * Method used to start online optimization
	 */
	private void startOptimizeOnlineThread() {
		Log.d(moduleLogName, "startOptimizeOnlineThread()");
		
		// Create graph if needed
		checkGraphExistance();
		
		final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenAIL/GraphLog/";
		
		
		// New thread
		optimizationThread = new Thread() {
		public void run() {
			optimizationInProgress = true;
			
			// While we did not get the stop condition
			while ( optimizationInProgress )
			{
				// Perform one iteration
				int res = NDKGraphOptimize(addrGraph, 1, path);
				
				// Save estimate
				synchronized(currentEstimateMtx)
				{
					currentEstimate = getVerticesEstimatesFromNDK();
					changeInOptimizedData = true;
				}
				
				// Should we sleep (not enough optimization change)
				if (res == 0)
				{
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				
				Log.d(moduleLogName, "OptimizationOnline: iteration ended");
			}
			Log.d(moduleLogName, "OptimizationOnline ended");
			} ;
		};
		
		// Start new optimization thread
		optimizationThread.start();
	}
	
	/*
	 * returns the list of estimates from NDK
	 */
	private List<Vertex> getVerticesEstimatesFromNDK() {
		
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
	
	/*
	 * save string to g2o file
	 */
	private void save2file(String g2oString)
	{
		if (graphStream!=null)
		{
			graphStream.print(g2oString);
		}
	}

	/**
	 * Creates a WiFi measurement string from values
	 */
	private String createWiFiEdgeString(int id, double distance) {
		String edgeWiFi ="EDGE_SE2:WIFI " + currentPoseId + " " + id + " " + distance + " " + parameters.informationMatrixOfWiFi + "\n";
		return edgeWiFi;
	}
	
	/**
	 * Creates a QR measurement string from values
	 */
	private String createQREdgeString(int qrId, int posId) {
		String edgeWiFi ="EDGE_SE2:QR " + qrId + " " + posId + " 0.0 999999999.0\n";
		return edgeWiFi;
	}
	
	/**
	 * Creates a WiFi Fingerprint string from values
	 */
	private String createWiFiFingerprintEdgeString(int id, int id2) {
		String edgeWiFi ="EDGE_SE2:WIFI_FINGERPRINT " + id2 + " " + id + " " + parameters.wifiFingerprintDeadBandRadius +  " " + parameters.informationMatrixOfWiFiFingerprint + "\n";
		return edgeWiFi;
	}
	
	/**
	 * Creates a VPR measurement string from values
	 */
	private String createVPRVicinityEdgeString(int id, int id2) {
		String edgeWiFi ="EDGE_SE2:VPR_VICINITY " + id2 + " " + id + " " + parameters.vprVicinityDeadBandRadius +  " " + parameters.informationMatrixOfVPRVicinity + "\n";
		return edgeWiFi;
	}
	
}
