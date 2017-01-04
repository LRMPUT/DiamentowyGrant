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

import org.dg.openAIL.IdPair;
import org.dg.wifi.MyScanResult;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;

public class WiFiPlaceRecognition implements Runnable {

	private static final String moduleLogName = "WiFiPlaceRecognition";

	org.dg.openAIL.ConfigurationReader.Parameters.WiFiPlaceRecognition parameters;

	// The database of places
	List<Pair<Integer, List<MyScanResult>>> placeDatabase = new ArrayList<Pair<Integer, List<MyScanResult>>>();

	// The waiting list of places to process
	List<Pair<Integer, List<MyScanResult>>> newPlaces = new ArrayList<Pair<Integer, List<MyScanResult>>>();

	// Queue of matches
	PriorityQueue<WiFiPlaceMatch> queueOfWiFiMatchesToCheck;

	// List of recognized places
	List<IdPair<Integer, Integer>> recognizedPlaces = new ArrayList<IdPair<Integer, Integer>>();
	
	// File to save data
	PrintStream outStreamPlaceRecognitionData = null;
		
	// Recognition thread
	Thread recognizePlacesThread;
	boolean performRecognition = false;

	public WiFiPlaceRecognition(
			org.dg.openAIL.ConfigurationReader.Parameters.WiFiPlaceRecognition wifiPlaceRecognitionParameters) {
		// Storing passed parameters
		parameters = wifiPlaceRecognitionParameters;

		// Creating queue
		Comparator<WiFiPlaceMatch> wiFiPlaceComparator = new WiFiPlaceMatchIdComparator();
		queueOfWiFiMatchesToCheck = new PriorityQueue<WiFiPlaceMatch>(
				parameters.maxQueueSize, wiFiPlaceComparator);		
	}

	/*
	 * Adds place with id and wiFiList
	 */
	public void addPlace(List<MyScanResult> wiFiList, int id) {
		Log.d(moduleLogName,
				"Added new place - now recognition thread needs to process the data");
		
		// Sorting WiFis by MACs
		sortListByMAC(wiFiList);

		// Adding to waiting list
		synchronized (newPlaces) {
			newPlaces.add(new Pair<Integer, List<MyScanResult>>(id, wiFiList));
		}
	}

	/*
	 * Returns the size of place database
	 * TODO: it is not thread save
	 */
	public int getSizeOfPlaceDatabase() {
		int placeDatabaseSize = -1;
		synchronized (placeDatabase) {
			placeDatabaseSize = placeDatabase.size();
		}
		return placeDatabaseSize;
	}
	

	/*
	 * Starts the recognition thread
	 */
	public void startRecognition() {

		openSaveStream();
		
		performRecognition = true;
		recognizePlacesThread = new Thread(this, "Recognition thread");
		recognizePlacesThread.start();
	}

	/*
	 * Stops the recognition thread
	 */
	public void stopRecognition() {
		performRecognition = false;
		try {
			recognizePlacesThread.join();
		} catch (InterruptedException e) {
			Log.d(moduleLogName, "Failed to join recognizePlacesThread");
		}

		queueOfWiFiMatchesToCheck.clear();
		newPlaces.clear();
		placeDatabase.clear();
		
		closeSaveStream();
	}

	/*
	 * Returns the list of recognized places (pairs of matched ids) and clears the recognized list
	 * TODO: we can omit new I think
	 */
	public List<IdPair<Integer, Integer>> getAndClearRecognizedPlacesList() {
		List<IdPair<Integer, Integer>> returnList = new ArrayList<IdPair<Integer, Integer>>();

		synchronized (recognizedPlaces) {
			// Copy each match
			for (IdPair<Integer, Integer> item : recognizedPlaces)
				returnList.add(new IdPair<Integer, Integer>(item));
			
			// Clear the list
			recognizedPlaces.clear();
		}

		return returnList;
	}

	/*
	 * Used to add new data to placeDatabase, find possible matches, verify them and put them in recognizedList
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// We look for potential matches as long as performRecognition is true
		while (performRecognition) {

			try {

				// There are 0 links to check, so we wait
				while (queueOfWiFiMatchesToCheck.size() == 0
						&& performRecognition == true) {

					Log.d(moduleLogName, "Sleeping ...");
					Thread.sleep(500);

					// Let's see if we there is a new place to add
					int newPlacesSize = -1;
					synchronized (newPlaces) {
						newPlacesSize = newPlaces.size();
					}
					
					Log.d(moduleLogName, "newPlacesSize = " + newPlacesSize);
					if (newPlacesSize > 0) {
						addNewPlaceInRecognitionThread();
					}
				}

				// We were asked to stop
				if (!performRecognition)
					break;

				Log.d(moduleLogName, "Queue size : "
						+ queueOfWiFiMatchesToCheck.size());
				WiFiPlaceMatch linkToTest = queueOfWiFiMatchesToCheck.poll();
				
				// Computing similarity measure
				Pair<Integer, Double> x = computeBSSIDSimilarity(
						linkToTest.listA, linkToTest.listB);

				// Compute average error
				float value = x.first;
				Double avgError = x.second;

				// Saving computed values to file
				outStreamPlaceRecognitionData.print(linkToTest.indexA + " "
						+ linkToTest.indexB + " " + value + " "
						+ parameters.minPercentOfSharedNetworks + " "
						+ linkToTest.listA.size() + " "
						+ linkToTest.listB.size() + " " + avgError + " "
						+ parameters.maxAvgErrorThreshold + "\n");

				// Should we add this match to the list of recognized places?
				if (value > parameters.minNumberOfSharedNetworks
//						&& value >= parameters.minPercentOfSharedNetworks
//								* linkToTest.listA.size()
						&& value >= parameters.minPercentOfSharedNetworks
								* linkToTest.listB.size()
						&& avgError < parameters.maxAvgErrorThreshold) {

					Log.d(moduleLogName,
							"Found local connection - adding it to list of recognized places");
					synchronized (recognizedPlaces) {
						recognizedPlaces.add(new IdPair<Integer, Integer>(
								linkToTest.indexA, linkToTest.indexB));
					}
					outStreamPlaceRecognitionData.print("  true");
				}
				else
					outStreamPlaceRecognitionData.print("  false");
				outStreamPlaceRecognitionData.print("\n");
			} catch (InterruptedException e) {
				Log.d("RecognizedPlaces", "Failed to acquire mutex");
			}
		}
	}

	
	/**
	 * Open stream to save all checked matches to /OpenAIL/WiFi/WiFiPlaceRecognition
	 */
	private void openSaveStream() {
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
	
	/*
	 * Close stream
	 */
	private void closeSaveStream() {
		if (outStreamPlaceRecognitionData != null) {
			outStreamPlaceRecognitionData.close();
			outStreamPlaceRecognitionData = null;
		}
	}
	
	/**
	 * Creates WiFi matches that new to be tested and adds place to database (if
	 * the proper parameter(addUserWiFiToRecognition) in settings.xml is set)
	 */
	private void addNewPlaceInRecognitionThread() {
		// Places to store information about place to add
		int indexA = -1;
		List<MyScanResult> wiFiList;

		// We process the places on the waiting list
		while(true) {
			synchronized (newPlaces) {
				if ( newPlaces.size() == 0)
					break;
				
				indexA = newPlaces.get(0).first;
				wiFiList = newPlaces.get(0).second;
				newPlaces.remove(0);
			}
	
			// look for potential matches
			if (indexA < 10000) {
				for (Pair<Integer, List<MyScanResult>> place : placeDatabase) {
					int indexB = place.first;
	
					// We skip if WiFis are from the same place
					if (indexA != indexB) {
	
						// Adding a WiFi match to queue in order to test it
						WiFiPlaceMatch linkToTest = new WiFiPlaceMatch(wiFiList,
								indexA, place.second, indexB, 0.0f);
						addToQueue(linkToTest);
					}
				}
			}
	
			// Depending on addUserWiFiToRecognition parameter, we add new user
			// place to placeDatabase
			if (indexA >= 10000 || parameters.addUserWiFiToRecognition) {
				Log.d(moduleLogName, "Adding to placeDatabase: id=" + indexA + " wifiList.size()=" + wiFiList.size());
				placeDatabase.add(new Pair<Integer, List<MyScanResult>>(indexA,
						wiFiList));
				

				// We need to reduce the size of the database
				if (placeDatabase.size() > parameters.maxPlaceDatabaseSize) {
					reduceTheSizeOfPlaceDatabase();
				}
	
			}
		}
	}

	/*
	 * Sorts the list of wifis according to MAC addresses
	 */
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
	private Pair<Integer, Double> computeBSSIDSimilarity(
			List<MyScanResult> listA, List<MyScanResult> listB) {
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

		Double avgError = 1000.0;
		if (count != 0)
			avgError = Math.sqrt(result / count);

		return new Pair<Integer, Double>(count, avgError);
	}

	// We try to add element to queue
	private void addToQueue(WiFiPlaceMatch linkToTest) {

		// We reached the maximal size of queue
		if (queueOfWiFiMatchesToCheck.size() >= parameters.maxQueueSize) {
			reduceTheSizeOfQueue();
		}

		// We add the element
		queueOfWiFiMatchesToCheck.add(linkToTest);

	}

	/**
	 * Method used to reduce the size of queue
	 */
	private void reduceTheSizeOfQueue() {
		Log.d(moduleLogName, "There is a need to clear queue, size : "
				+ queueOfWiFiMatchesToCheck.size());

		Comparator<WiFiPlaceMatch> wiFiPlaceComparator = new WiFiPlaceMatchIdComparator();
		PriorityQueue<WiFiPlaceMatch> newQueue = new PriorityQueue<WiFiPlaceMatch>(
				parameters.maxQueueSize, wiFiPlaceComparator);
		for (int i = 0; i < parameters.fractionOfQueueAfterReduction
				* parameters.maxQueueSize; i++) {
			newQueue.add(queueOfWiFiMatchesToCheck.poll());
		}
		queueOfWiFiMatchesToCheck = newQueue;
		Log.d(moduleLogName,
				"Reduced queue size: " + queueOfWiFiMatchesToCheck.size());
	}

	/**
	 * Method used to reduce the size of the place database
	 */
	private void reduceTheSizeOfPlaceDatabase() {
		Log.d(moduleLogName, "We need to reduce the database size as it is =" + placeDatabase.size());
		List<Pair<Integer, List<MyScanResult>>> tmp = new ArrayList<Pair<Integer, List<MyScanResult>>>();
		int i = 0;
		for (Pair<Integer, List<MyScanResult>> place : placeDatabase) {
			if (i++ % 2 == 0) {
				tmp.add(place);
			}
		}
		placeDatabase = tmp;
		Log.d(moduleLogName, "New database size=" + placeDatabase.size());
	}
}
