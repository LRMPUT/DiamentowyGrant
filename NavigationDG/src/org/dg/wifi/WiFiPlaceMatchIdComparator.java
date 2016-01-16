package org.dg.wifi;

import java.util.Comparator;

// Method used by priority queue to order elements
// Now it is based on ids
public class WiFiPlaceMatchIdComparator implements Comparator<WiFiPlaceMatch> {
	@Override
	public int compare(WiFiPlaceMatch x, WiFiPlaceMatch y) {
		if (x.indexA < y.indexA)
			return -1;
		else if (x.indexA > y.indexA)
			return 1;
		else
			return 0;
	}
}
