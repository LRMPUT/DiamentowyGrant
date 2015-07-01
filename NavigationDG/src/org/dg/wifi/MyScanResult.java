package org.dg.wifi;

public class MyScanResult {
	public MyScanResult(String _BSSID, int _level) {
		BSSID = _BSSID;
		level = _level;
		networkName = "";
	}
	
	public MyScanResult(String _BSSID, int _level, String _networkName) {
		BSSID = _BSSID;
		level = _level;
		networkName = _networkName;
	}
	
	
	public int level;	
	public String BSSID;
	public String networkName;
}