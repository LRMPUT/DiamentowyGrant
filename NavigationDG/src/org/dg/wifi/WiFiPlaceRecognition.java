package org.dg.wifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.net.wifi.ScanResult;

public class WiFiPlaceRecognition {
	
	List<List<ScanResult>> placeDatabase = new ArrayList<List<ScanResult>>();

	
	public WiFiPlaceRecognition() {
		
		// TODO!
//		File databaseFile = null
//		if ( databaseFile.exists() )
//		{
//			try {
//				FileInputStream databaseStream = new FileInputStream(databaseFile);
//			
//				
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
	}
	
	public void addPlace(List<ScanResult> wiFiList)
	{
		sortListByMAC(wiFiList);
		placeDatabase.add(wiFiList);
	}
		
	public int recognizePlace(List<ScanResult> wiFiList)
	{
		sortListByMAC(wiFiList);
		
		int bestIndex = 0, index = 0;
		float bestValue = 0.0f;
		
		// Check all places in database
		for (List<ScanResult> place : placeDatabase)
		{
			// Computing similarity measure
			float value = computeBSSIDSimilarity(place, wiFiList);
			
			// Finding the most probable place
			if ( value > bestValue )
			{
				bestValue = value;
				bestIndex = index;
			}
			index = index + 1;
		}
		return bestIndex;
	}
	
	public int getSizeOfPlaceDatabase()
	{
		return placeDatabase.size();
	}
	
	
	
	private void sortListBySignalStrength(List<ScanResult> wiFiList)
	{
		Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
	        @Override
	        public int compare(ScanResult lhs, ScanResult rhs) {
	            return (lhs.level <rhs.level ? -1 : (lhs.level==rhs.level ? 0 : 1));
	        }
	     };
		Collections.sort(wiFiList, comparator);
	}
	
	private void sortListByMAC(List<ScanResult> wiFiList)
	{
		Comparator<ScanResult> comparator = new Comparator<ScanResult>() {
	        @Override
	        public int compare(ScanResult lhs, ScanResult rhs) {
	            return lhs.BSSID.compareTo(rhs.BSSID);
	        }
	     };
		Collections.sort(wiFiList, comparator);
	}
	
	private float computeBSSIDSimilarity(List<ScanResult> listA, List<ScanResult> listB)
	{
		float result = 0.0f;
		for (ScanResult scanA : listA)
		{
			for (ScanResult scanB : listB)
			{
				if ( scanA.BSSID.compareTo(scanB.BSSID) == 0)
				{
					result = result + 1.0f;
					break;
				}
			}
		}
		return result;
	}
	
}
