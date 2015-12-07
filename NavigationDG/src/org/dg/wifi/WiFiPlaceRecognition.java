package org.dg.wifi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

import org.dg.openAIL.IdPair;
import org.dg.wifi.MyScanResult;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;


public class WiFiPlaceRecognition implements Runnable {

	private static final String moduleLogName = "WiFiPlaceRecognition";

	org.dg.openAIL.ConfigurationReader.Parameters.WiFiPlaceRecognition parameters;
	
	// The database of places
	List<List<MyScanResult>> placeDatabase = new ArrayList<List<MyScanResult>>();
	List<Integer> placeIds = new ArrayList<Integer>();

	// File to save data
	PrintStream outStreamPlaceRecognitionData = null;

	// Queue Mutex
	PriorityQueue<WiFiPlaceLink> queueOfWiFiMatchesToCheck;
	private final Semaphore queueMtx = new Semaphore(1, true);
	
	// List of recognized places
	List<IdPair<Integer, Integer>> recognizedPlaces = new ArrayList<IdPair<Integer, Integer>>();
	private final Semaphore recognizedPlacesMtx = new Semaphore(1, true);
	
	// Recognition thread
	Thread recognizePlacesThread;
	boolean performRecognition = false;
	boolean newPlaceToProcess = false;
	int indexOfNewPlaceToProcess = -1;

	public WiFiPlaceRecognition(org.dg.openAIL.ConfigurationReader.Parameters.WiFiPlaceRecognition wifiPlaceRecognitionParameters) {
		
		parameters = wifiPlaceRecognitionParameters;
				
		// Creating queue
		Comparator<WiFiPlaceLink> wiFiPlaceComparator = new WiFiPlaceComparator();
		queueOfWiFiMatchesToCheck = new PriorityQueue<WiFiPlaceLink>(parameters.maxQueueSize,
				wiFiPlaceComparator);

		// Getting place to save data
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/OpenAIL/WiFi");

		if (!folder.exists()) {
			folder.mkdir();
		}
		
		// File to save results
		String fileName = "";
		fileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/WiFi/WiFiPlaceRecognition.list");

		// RawMeasurements
		FileOutputStream foutStream;
		try {
			foutStream = new FileOutputStream(fileName);
			outStreamPlaceRecognitionData = new PrintStream(foutStream);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void addPlace(List<MyScanResult> wiFiList, int id, boolean newThread) {

		Log.d(moduleLogName,
				"Added new place - now recognition thread needs to process the data");
		sortListByMAC(wiFiList);

		indexOfNewPlaceToProcess = placeDatabase.size();
		placeDatabase.add(wiFiList);
		placeIds.add(id);
		
		newPlaceToProcess = true;
		
		if ( !newThread ) {
			addNewPlaceInRecognitionThread();
		}
	}



	

	public int getSizeOfPlaceDatabase() {
		return placeDatabase.size();
	}

	public void closeStream() {
		if (outStreamPlaceRecognitionData != null) {
			outStreamPlaceRecognitionData.close();
			outStreamPlaceRecognitionData = null;
		}
	}

	private void sortListByMAC(List<MyScanResult> wiFiList) {
		Comparator<MyScanResult> comparator = new Comparator<MyScanResult>() {
			@Override
			public int compare(MyScanResult lhs, MyScanResult rhs) {
				return lhs.BSSID.compareTo(rhs.BSSID);
			}
		};
		Collections.sort(wiFiList, comparator);
	}

	
	
	// Compute average error over shared networks
	// TODO: Can be done faster as lists are sorted!
	private Pair<Integer, Double> computeBSSIDSimilarity(List<MyScanResult> listA,
			List<MyScanResult> listB) {
		float result = 0.0f;
		int count = 0;
		for (MyScanResult scanA : listA) {
			for (MyScanResult scanB : listB) {
				if (scanA.BSSID.compareTo(scanB.BSSID) == 0) {
					result += Math.pow(scanA.level - scanB.level, 2);
					count += 1;
				}
			}
		}
		
		return new Pair<Integer, Double>(count, Math.sqrt(result / count));
	}

	// Method used by priority queue to order elements
	// TODO: X, Y !!!
	// Now it is based on indices
	public class WiFiPlaceComparator implements Comparator<WiFiPlaceLink> {
		@Override
		public int compare(WiFiPlaceLink x, WiFiPlaceLink y) {
//			if (x.positionDifferance < y.positionDifferance)
//				return -1;
//			else if (x.positionDifferance > y.positionDifferance)
//				return 1;
//			else return 0;
			if (x.indexA < y.indexA)
				return -1;
			else if (x.indexA > y.indexA)
				return 1;
			else
				return 0;
		}
	}

	// We try to add element to queue
	public void addToQueue(WiFiPlaceLink linkToTest) {

		try {
			queueMtx.acquire();

			// We reached the maximal size of queue
			if (queueOfWiFiMatchesToCheck.size() >= parameters.maxQueueSize) {
				reduceTheSizeOfQueue();
			}
			
			// We add the element
			queueOfWiFiMatchesToCheck.add(linkToTest);

			queueMtx.release();
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "Failed to access queue mutex");
		}

	}

	/**
	 * Method used to reduce the size of queue 
	 */
	private void reduceTheSizeOfQueue() {
		Log.d(moduleLogName, "There is a need to clear queue, size : "
				+ queueOfWiFiMatchesToCheck.size());

		Comparator<WiFiPlaceLink> wiFiPlaceComparator = new WiFiPlaceComparator();
		PriorityQueue<WiFiPlaceLink> newQueue = new PriorityQueue<WiFiPlaceLink>(
				parameters.maxQueueSize, wiFiPlaceComparator);
		for (int i = 0; i < parameters.fractionOfQueueAfterReduction * parameters.maxQueueSize; i++) {
			newQueue.add(queueOfWiFiMatchesToCheck.poll());
		}
		queueOfWiFiMatchesToCheck = newQueue;
		Log.d(moduleLogName, "Reduced queue size: " + queueOfWiFiMatchesToCheck.size());
	}
	
	void startRecognition() {
		
		performRecognition = true;
		recognizePlacesThread = new Thread(this, "Recognition thread");
		recognizePlacesThread.start();
	}
	
	void stopRecognition() {
		performRecognition = false;
		try {
			recognizePlacesThread.join();
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "Failed to join recognizePlacesThread");
		}
		
		queueOfWiFiMatchesToCheck.clear();
		indexOfNewPlaceToProcess = -1;
		placeDatabase.clear();
		placeIds.clear();
	}
	
	List<IdPair<Integer, Integer>> getAndClearRecognizedPlacesList() {
		try {
			recognizedPlacesMtx.acquire();
			List<IdPair<Integer, Integer>> returnList = new ArrayList<IdPair<Integer, Integer>>(recognizedPlaces.size());
			for(IdPair<Integer, Integer> item: recognizedPlaces) 
				returnList.add( new IdPair<Integer, Integer>(item) );
			recognizedPlaces.clear();
			
			recognizedPlacesMtx.release();
			return returnList;
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "Failed to acquire recognizedList Mtx in getRecognizedPlaces");
		}
		return new ArrayList<IdPair<Integer, Integer>>();
	}

	class WiFiPlaceLink {
		List<MyScanResult> listA;
		List<MyScanResult> listB;
		int indexA, indexB;
		float positionDifferance;

		public WiFiPlaceLink(List<MyScanResult> _listA, int _indexA, 
				List<MyScanResult> _listB, int _indexB, float _positionDifferance) {
			listA = _listA;
			listB = _listB;
			indexA = _indexA;
			indexB = _indexB;
			
			positionDifferance = _positionDifferance;
		}
	}
	
	

	@Override
	public void run() {
		// We start with some delay
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (performRecognition) {
			 
			try {
				// Just sleep so we do not use CPU
				while (queueOfWiFiMatchesToCheck.size() == 0 && performRecognition == true) {
					Log.d(moduleLogName, "Sleeping ...");
					Thread.sleep(500);
					
					// NEW place had been added
					if ( newPlaceToProcess )
					{
						addNewPlaceInRecognitionThread();
					}
				}
				
				if (!performRecognition)
					break;
				
				
				
				// parameters.maxPlaceDatabaseSize
				
				
				// We need to reduce the size of the database
//				if (placeDatabase.size() > maxPlaceDatabaseSize) {
//					int i = 0;
//					Iterator<Integer> itPlace = placeIds.iterator();
//					for (Iterator<List<ScanResult>> it = placeDatabase.iterator(); it
//							.hasNext(); i++) {
//						if (i % 3 == 0) {
//							it.remove();
//							itPlace.remove();
//						}
//					}
		//
//				}
						
				// NEW place had been added
				if ( newPlaceToProcess )
				{
					addNewPlaceInRecognitionThread();
				}
				
				Log.d(moduleLogName, "Queue size : " + queueOfWiFiMatchesToCheck.size());
				
				queueMtx.acquire();
				WiFiPlaceLink linkToTest = queueOfWiFiMatchesToCheck.poll();
				queueMtx.release();
				
				// Computing similarity measure
				Pair<Integer, Double> x = computeBSSIDSimilarity(linkToTest.listA,
						linkToTest.listB);
				
				// Compute average error
				float value = x.first;
				Double avgError = x.second;

				// Finding the most probable place
				outStreamPlaceRecognitionData.print(linkToTest.indexA + " "
						+ linkToTest.indexB + " " + value + " "
						+ parameters.minPercentOfSharedNetworks + " " + linkToTest.listA.size()
						+ " " + linkToTest.listB.size() + " " + avgError + " "
						+ parameters.maxAvgErrorThreshold + "\n");

				if (value > parameters.minNumberOfSharedNetworks && value > parameters.minPercentOfSharedNetworks * linkToTest.listA.size()
						&& value > parameters.minPercentOfSharedNetworks * linkToTest.listB.size()
						&& avgError < parameters.maxAvgErrorThreshold) {

					Log.d(moduleLogName, "Found local connection - adding it to list of recognized places");
					recognizedPlacesMtx.acquire();
					recognizedPlaces.add(new IdPair<Integer, Integer>(linkToTest.indexA, linkToTest.indexB));
					recognizedPlacesMtx.release();
				}
			} catch (InterruptedException e) {
				Log.d("RecognizedPlaces", "Failed to acquire mutex");
			}
		}
	}

	/**
	 * 
	 */
	private void addNewPlaceInRecognitionThread() {
		List<MyScanResult> wiFiList = placeDatabase.get(indexOfNewPlaceToProcess);
		int indexA = placeIds.get(indexOfNewPlaceToProcess);
		
		int index = 0;
		for (List<MyScanResult> list : placeDatabase)	
		{
			int indexB = placeIds.get(index);
			
			if ( indexA != indexB && indexA < 10000) {
				WiFiPlaceLink linkToTest = new WiFiPlaceLink(wiFiList, indexA, list, indexB, 0.0f);
				addToQueue(linkToTest);
			}
				
			index++;
		}
		newPlaceToProcess = false;
	}

}
