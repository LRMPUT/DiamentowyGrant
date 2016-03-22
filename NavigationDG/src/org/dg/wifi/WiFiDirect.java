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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

public class WiFiDirect {
	
	final String moduleLogName = "WiFiDirect";
	
	public class DirectMeasurement {
		public int idPos, idAP;
		public double distance, level, frequency;
		
		public DirectMeasurement(int _idAP, int _idPos, double _distance, double _level, double _frequency) {
			idAP = _idAP;
			idPos = _idPos;
			distance = _distance;
			level = _level;
			frequency = _frequency;
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
				double distance = convertLevelToMeters(network.level, network.frequency);
				
				// Add to list
				directMeasurements.add(new DirectMeasurement(idAP, idPos, distance, network.level, network.frequency));
				
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
			positionOne.add(new DirectMeasurement(dm.idAP, dm.idPos-8000, dm.distance, dm.level, dm.frequency));
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
	private double convertLevelToMeters(double level, double freq) {
//		double tmp = -40 + Math.abs(level);
//		tmp = tmp / 40;
//		return Math.pow(10, tmp);
		 double exp = (27.55 - (20 * Math.log10(freq)) + Math.abs(level)) / 20.0;
		 return Math.pow(10.0, exp);
	}
}
