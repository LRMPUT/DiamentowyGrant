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
	
	public MyScanResult(String _BSSID, int _level, String _networkName, int _frequency) {
		BSSID = _BSSID;
		level = _level;
		networkName = _networkName;
		frequency = _frequency;
	}
	
	
	public int level, frequency;	
	public String BSSID;
	public String networkName;
}