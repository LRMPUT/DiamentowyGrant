package org.dg.wifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

import android.net.wifi.ScanResult;
import android.os.Environment;
import android.util.Log;

public class WiFiPlaceRecognition implements Runnable {

	private static final String moduleLogName = "WiFiPlaceRecognition";

	// The sanity percent
	static final float wiFiSanityPercent = 0.75f;

	// The average error threshold
	static final float wiFiAvgErrorThreshold = 8f;

	// The database of places
	final int maxPlaceDatabaseSize = 500;
	List<List<ScanResult>> placeDatabase = new ArrayList<List<ScanResult>>();
	List<Integer> placeIds = new ArrayList<Integer>();

	// File to save data
	PrintStream outStreamPlaceRecognitionData = null;

	// Queue Mutex
	final int WiFiPlaceRecognitionSize = 50;
	PriorityQueue<WiFiPlaceLink> queue;
	private final Semaphore queueMtx = new Semaphore(1, true);
	
	// List of recognized places
	List<IdPair<Integer, Integer>> recognizedPlaces = new ArrayList<IdPair<Integer, Integer>>();
	private final Semaphore recognizedPlacesMtx = new Semaphore(1, true);
	
	// Recognition thread
	Thread recognizePlacesThread;
	boolean performRecognition = false;
	boolean newPlaceToProcess = false;
	int indexOfNewPlaceToProcess = -1;

	public WiFiPlaceRecognition() {
		// QUEUE
		Comparator<WiFiPlaceLink> wiFiPlaceComparator = new WiFiPlaceComparator();
		queue = new PriorityQueue<WiFiPlaceLink>(WiFiPlaceRecognitionSize,
				wiFiPlaceComparator);

		// Getting place to save data
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/DG");

		if (!folder.exists()) {
			folder.mkdir();
		}

		// File to save results
		String fileName = "";
		fileName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/DG"
				+ "/WiFiPlaceRecognition.list");

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

	public void addPlace(List<ScanResult> wiFiList, int id) {

		Log.d(moduleLogName,
				"Added new place - now recognition thread needs to process the data");
		sortListByMAC(wiFiList);

		indexOfNewPlaceToProcess = placeDatabase.size();
		placeDatabase.add(wiFiList);
		placeIds.add(id);
		
		
		newPlaceToProcess = true;
	}

	public int recognizePlace(List<ScanResult> wiFiList) {
		sortListByMAC(wiFiList);

		int bestIndex = 0, index = 0;
		float bestValue = 0.0f;

		// Check all places in database
		for (List<ScanResult> place : placeDatabase) {
			// Computing similarity measure
			float value = computeBSSIDSimilarity(place, wiFiList);

			// Finding the most probable place
			if (value > bestValue) {
				bestValue = value;
				bestIndex = index;
			}
			index = index + 1;
		}

		// Sanity check - if the best place does not have 75% of the check place
		// wifis
		if (bestValue < wiFiSanityPercent * wiFiList.size())
			return -1;

		return placeIds.get(bestIndex);
	}

	public List<Integer> returnAllMatchingPlaces(List<ScanResult> wiFiList,
			int currentPoseId) {
		List<Integer> result = new ArrayList<Integer>();

		sortListByMAC(wiFiList);

		// Check all places in database
		int index = 0;
		for (List<ScanResult> place : placeDatabase) {
			// Computing similarity measure
			float value = computeBSSIDSimilarity(place, wiFiList);

			// Compute average error
			float avgError = computeAverageMeanSquaredBSSIDError(place,
					wiFiList);

			// Finding the most probable place
			if (currentPoseId != placeIds.get(index)) {
				outStreamPlaceRecognitionData.print(placeIds.get(index) + " "
						+ currentPoseId + " " + value + " " + wiFiSanityPercent
						+ " " + wiFiList.size() + " " + avgError + " "
						+ wiFiAvgErrorThreshold + "\n");

				if (value > wiFiSanityPercent * wiFiList.size()
						&& avgError < wiFiAvgErrorThreshold) {

					result.add(placeIds.get(index));
				}
			}
			index = index + 1;
		}

		return result;
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

	private void sortListByMAC(List<ScanResult> wiFiList) {
		Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
			@Override
			public int compare(ScanResult lhs, ScanResult rhs) {
				return lhs.BSSID.compareTo(rhs.BSSID);
			}
		};
		Collections.sort(wiFiList, comparator);
	}

	// Compute similarity as the number of shared WiFi networks
	private int computeBSSIDSimilarity(List<ScanResult> listA,
			List<ScanResult> listB) {
		int result = 0;
		for (ScanResult scanA : listA) {
			for (ScanResult scanB : listB) {
				if (scanA.BSSID.compareTo(scanB.BSSID) == 0) {
					result++;
					break;
				}
			}
		}
		return result;
	}

	// Compute average error over shared networks
	private float computeAverageMeanSquaredBSSIDError(List<ScanResult> listA,
			List<ScanResult> listB) {
		float result = 0.0f;
		int count = 0;
		for (ScanResult scanA : listA) {
			for (ScanResult scanB : listB) {
				if (scanA.BSSID.compareTo(scanB.BSSID) == 0) {
					result += Math.pow(scanA.level - scanB.level, 2);
					count += 1;
				}
			}
		}
		return (float) Math.sqrt(result / count);
	}

	// Method used in priority queue
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

	public void addToQueue(WiFiPlaceLink linkToTest) {
		
		// We wont compare wifi links connected to the same node in graph
		if ( linkToTest.indexA != linkToTest.indexB)
		{
			try {
				queueMtx.acquire();
				
				if (queue.size() == WiFiPlaceRecognitionSize)
				{
					Log.d(moduleLogName, "There is a need to clear queue, size : "
							+ queue.size());
					
					Comparator<WiFiPlaceLink> wiFiPlaceComparator = new WiFiPlaceComparator();
					PriorityQueue<WiFiPlaceLink> newQueue = new PriorityQueue<WiFiPlaceLink>(
							WiFiPlaceRecognitionSize, wiFiPlaceComparator);
					for (int i = 0; i < WiFiPlaceRecognitionSize / 2; i++) {
						newQueue.add(queue.poll());
					}
					queue = newQueue;
					Log.d(moduleLogName, "Reduced queue size: " + queue.size());
				}
				
				queue.add(linkToTest);
				
				queueMtx.release();
			} catch (InterruptedException e) {
				Log.d(moduleLogName, "Failed to access queue mutex");
			}
		}
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
	}
	
	List<IdPair<Integer, Integer>> getRecognizedPlacesList() {
		try {
			recognizedPlacesMtx.acquire();
			List<IdPair<Integer, Integer>> returnList = new ArrayList<IdPair<Integer, Integer>>(recognizedPlaces);
			recognizedPlacesMtx.release();
			return returnList;
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "Failed to acquire recognizedList Mtx in getRecognizedPlaces");
		}
		return new ArrayList<IdPair<Integer, Integer>>();
	}

	class WiFiPlaceLink {
		List<ScanResult> listA;
		List<ScanResult> listB;
		int indexA, indexB;
		float positionDifferance;

		public WiFiPlaceLink(List<ScanResult> _listA, int _indexA, 
				List<ScanResult> _listB, int _indexB, float _positionDifferance) {
			listA = _listA;
			listB = _listB;
			indexA = _indexA;
			indexB = _indexB;
			
			positionDifferance = _positionDifferance;
		}
	}
	
	public class IdPair<A, B> {
	    private A first;
	    private B second;

	    public IdPair(A first, B second) {
	   
	    	this.first = first;
	    	this.second = second;
	    }
	    public A getFirst() {
	    	return first;
	    }

	    public void setFirst(A first) {
	    	this.first = first;
	    }

	    public B getSecond() {
	    	return second;
	    }

	    public void setSecond(B second) {
	    	this.second = second;
	    }
	} 

	@Override
	public void run() {
		while (performRecognition) {
			 
			try {
				// Just sleep so we do not use CPU
				while (queue.size() == 0 && performRecognition == true) {
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
				
				
				queueMtx.acquire();
				WiFiPlaceLink linkToTest = queue.poll();
				queueMtx.release();
				
				// Computing similarity measure
				float value = computeBSSIDSimilarity(linkToTest.listA,
						linkToTest.listB);

				// Compute average error
				float avgError = computeAverageMeanSquaredBSSIDError(
						linkToTest.listA, linkToTest.listB);

				// Finding the most probable place
				outStreamPlaceRecognitionData.print(linkToTest.indexA + " "
						+ linkToTest.indexB + " " + value + " "
						+ wiFiSanityPercent + " " + linkToTest.listA.size()
						+ " " + linkToTest.listB.size() + " " + avgError + " "
						+ wiFiAvgErrorThreshold + "\n");

				if (value > 7 && value > wiFiSanityPercent * linkToTest.listA.size()
						&& value > wiFiSanityPercent * linkToTest.listB.size()
						&& avgError < wiFiAvgErrorThreshold) {

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
		List<ScanResult> wiFiList = placeDatabase.get(indexOfNewPlaceToProcess);
		int indexA = placeIds.get(indexOfNewPlaceToProcess);
		
		int index = 0;
		for (List<ScanResult> list : placeDatabase)	
		{
			int indexB = placeIds.get(index);
			
			WiFiPlaceLink linkToTest = new WiFiPlaceLink(wiFiList, indexA, list, indexB, 0.0f);
			addToQueue(linkToTest);	
			index++;
		}
		newPlaceToProcess = false;
	}

}
