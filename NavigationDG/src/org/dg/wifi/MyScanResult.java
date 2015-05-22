package org.dg.wifi;

public class MyScanResult {
	public MyScanResult(String _BSSID, int _level) {
		BSSID = _BSSID;
		level = _level;
	}
	
	int level;	
	String BSSID;
}