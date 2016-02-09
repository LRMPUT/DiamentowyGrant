package org.dg.wifi;

import java.util.ArrayList;
import java.util.List;

public class WiFiDirect {
	
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
	
	// NetworkMacAddr to idAP
	List<String> bssid2name = new ArrayList<String>();

	
	public void processNewScan(List<MyScanResult> scan, int idPos) {

		for (int i = 0; i < scan.size(); i++) {

			// Getting network
			MyScanResult network = scan.get(i);
			
			// Convert MAC to index
			int idAP = bssid2name.indexOf(network.BSSID);
			if (idAP == -1) {
				bssid2name.add(network.BSSID);
				idAP = bssid2name.indexOf(network.BSSID);
			}
			
			// Convert lvl to meters
			double distance = convertLevelToMeters(network.level);
			
			// Add to list
			directMeasurements.add(new DirectMeasurement(idAP, idPos, distance, network.level));
		}
	}
	
	public List<DirectMeasurement> getReverseTest() {
		List<DirectMeasurement> positionOne = new ArrayList<DirectMeasurement>();
		for (DirectMeasurement dm : directMeasurements) {
			positionOne.add(new DirectMeasurement(dm.idAP, dm.idPos-8000, dm.distance, dm.level));
		}
		return positionOne;
	}
	
	/*
	 * Return results
	 */
	public List<DirectMeasurement> getGraphWiFiList() {
		return new ArrayList<DirectMeasurement>(directMeasurements);
	}
	
	public int getNumberOfNetworks() {
		return bssid2name.size();
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
