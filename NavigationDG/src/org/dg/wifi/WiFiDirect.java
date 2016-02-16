package org.dg.wifi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

public class WiFiDirect {
	
	final String moduleLogName = "WiFiDirect";
	
	public class DirectMeasurement {
		public int idPos, idAP;
		public double distance, level;
		
		public DirectMeasurement(int _idAP, int _idPos, double _distance, double _level) {
			idAP = _idAP;
			idPos = _idPos;
			distance = _distance;
			level = _level;
		}
	}
	
	// List of measurements
	List<DirectMeasurement> directMeasurements = new ArrayList<DirectMeasurement>();
	List<Integer> posOccurrence= new ArrayList<Integer>();
	int lastIdPos = -1;
	
	// NetworkMacAddr to idAP
	List<String> bssid2id = new ArrayList<String>();
	List<Integer> bssidOccurrence= new ArrayList<Integer>();

	
	public void processNewScan(List<MyScanResult> scan, int idPos) {

		for (int i = 0; i < scan.size(); i++) {

			// Getting network
			MyScanResult network = scan.get(i);
			
			if ( (network.networkName.contains("mtp") || network.networkName.length() < 1) && network.level > -120)
			{
			
				// Convert MAC to index
				int idAP = bssid2id.indexOf(network.BSSID);
				
				if (idAP == -1) {
					bssid2id.add(network.BSSID);
					bssidOccurrence.add(Integer.valueOf(0));
					idAP = bssid2id.indexOf(network.BSSID);
				} else
					bssidOccurrence.set(idAP, bssidOccurrence.get(idAP)+1);
				
				// Convert lvl to meters
				double distance = convertLevelToMeters(network.level);
				
				// Add to list
				directMeasurements.add(new DirectMeasurement(idAP, idPos, distance, network.level));
				
				if ( lastIdPos != idPos) {
					lastIdPos = idPos;
					posOccurrence.add(Integer.valueOf(0));
				}
			}
			else
				Log.d(moduleLogName, "Rejected: " + network.networkName);
		}
	}
	
	public void filterDirectMeasurements() {
		Log.d(moduleLogName, "filterDirectMeasurements! " + directMeasurements.size());
		
		for (int i=0;i<bssidOccurrence.size();i++)
		{
			if ( bssidOccurrence.get(i) < 10 )
			{
				Iterator<DirectMeasurement> iter = directMeasurements.iterator();
				while (iter.hasNext()) {
				    if (iter.next().idAP == i) {
				        iter.remove();
				    }
				}
			}
			
		}
		Log.d(moduleLogName, "Filtered! " + directMeasurements.size());
	}
	
	public List<DirectMeasurement> getReverseTest() {
		Log.d(moduleLogName, "getReverseTest()! " + posOccurrence.size());
		
		List<DirectMeasurement> positionOne = new ArrayList<DirectMeasurement>();
		for (DirectMeasurement dm : directMeasurements) {
			positionOne.add(new DirectMeasurement(dm.idAP, dm.idPos-8000, dm.distance, dm.level));
			posOccurrence.set(dm.idPos-10000, posOccurrence.get(dm.idPos-10000) + 1);
		}
		
		for (Integer i : posOccurrence) {
			Log.d(moduleLogName, "posOccurance = " + i);
		}
		Log.d(moduleLogName, "getReverseTest()! ");
		
		
		return positionOne;
	}
	
	/*
	 * Return results
	 */
	public List<DirectMeasurement> getGraphWiFiList() {
		return new ArrayList<DirectMeasurement>(directMeasurements);
	}
	
	public int getNumberOfNetworks() {
		return bssid2id.size();
	}

	/*
	 * Convert measurement from dBm to meters with some propagation model
	 */
	private double convertLevelToMeters(double level) {
//		double tmp = -40 + Math.abs(level);
//		tmp = tmp / 40;
//		return Math.pow(10, tmp);
		 double exp = (27.55 - (20 * Math.log10(2412)) + Math.abs(level)) / 20.0;
		 return Math.pow(10.0, exp);
	}
}
