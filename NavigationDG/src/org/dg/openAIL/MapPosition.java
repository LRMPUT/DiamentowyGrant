package org.dg.openAIL;

import java.util.List;

import org.dg.wifi.MyScanResult;
import org.opencv.core.Mat;

public class MapPosition {
	public int id;
	public double X, Y, Z, angle;
	public Mat image;
	public List<MyScanResult> scannedWiFiList;
}
