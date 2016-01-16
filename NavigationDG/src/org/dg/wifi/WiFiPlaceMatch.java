package org.dg.wifi;

import java.util.Comparator;
import java.util.List;

public class WiFiPlaceMatch {
	List<MyScanResult> listA;
	List<MyScanResult> listB;
	int indexA, indexB;
	float positionDifferance;

	public WiFiPlaceMatch(List<MyScanResult> _listA, int _indexA,
			List<MyScanResult> _listB, int _indexB,
			float _positionDifferance) {
		listA = _listA;
		listB = _listB;
		indexA = _indexA;
		indexB = _indexB;

		positionDifferance = _positionDifferance;
	}	
}