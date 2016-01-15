package org.dg.openAIL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.dg.main.Edge;
import org.dg.main.Node;
import org.dg.wifi.MyScanResult;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class PriorMapHandler {
	private static final String moduleLogName = "PriorMapHandler";

	// The id of currently saved position (starts at 10000 to distinguish anchor
	// nodes from normal nodes)
	// int mapPointId;

	public PriorMapHandler() {

	}

	/**
	 * Loads prior map from the file
	 * 
	 * @param mapName
	 *            - name of the directory with the map (wifi scans, images,
	 *            position) to be read
	 */
	public List<MapPosition> loadWiFiAndImageMap(String mapName) {
		List<MapPosition> mapPositions = new ArrayList<MapPosition>();

		// The path of the main directory of the map
		String mapDirectoryPath = String.format(Locale.getDefault(),
				Environment.getExternalStorageDirectory().toString()
						+ "/OpenAIL/PriorData/" + mapName + "/");

		// Listing files in the directory of the WiFi
		File wifiDir = new File(mapDirectoryPath + "wifiScans/");
		File wifiScanFiles[] = wifiDir.listFiles();
		Arrays.sort(wifiScanFiles, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});
		Log.d(moduleLogName, "WiFi Scans length: " + wifiScanFiles.length);

		// Listing files in the directory of the images
		File imageDir = new File(mapDirectoryPath + "images/");
		File imageFiles[] = imageDir.listFiles();
		Arrays.sort(imageFiles, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});
		Log.d(moduleLogName, "Image directory length: " + imageFiles.length);

		// Check that the we have the same number of WiFi scans and images
		if (wifiScanFiles.length != imageFiles.length) {
			Log.e(moduleLogName,
					"Different number of wifi scans and images -- something went wrong and map was not read");
			return new ArrayList<MapPosition>();
		}

		// Preparations to read positions
		Scanner positionScanner = null;
		try {
			positionScanner = new Scanner(new BufferedReader(new FileReader(
					mapDirectoryPath + "positions.list")));
			positionScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
			Log.e(moduleLogName, "Missing positions.list");
			return new ArrayList<MapPosition>();
		}

		// For all positions, wifi scans and images
		for (int i = 0; i < wifiScanFiles.length; i++) {
			MapPosition mapPosition = new MapPosition();

			Log.d(moduleLogName, "Processing: " + wifiScanFiles[i].getName()
					+ " & " + imageFiles[i].getName());

			try {
				// Reading position
				mapPosition.id = positionScanner.nextInt();
				mapPosition.X = positionScanner.nextDouble();
				mapPosition.Y = positionScanner.nextDouble();
				mapPosition.Z = positionScanner.nextDouble();
				mapPosition.angle = positionScanner.nextDouble();
				String dummy = positionScanner.nextLine();
				Log.d(moduleLogName, "READ: " + mapPosition.id + " "
						+ mapPosition.X + " " + mapPosition.Y + " "
						+ mapPosition.Z + " " + mapPosition.angle);

				// Reading image
				Mat image = Highgui.imread(mapDirectoryPath + "images/"
						+ imageFiles[i].getName(), 0);

				// Reading WiFi
				Scanner wifiScanScanner = new Scanner(new BufferedReader(
						new FileReader(mapDirectoryPath + "wifiScans/"
								+ wifiScanFiles[i].getName())));
				wifiScanScanner.useLocale(Locale.US);

				int idWiFi = wifiScanScanner.nextInt();
				int wifiCount = wifiScanScanner.nextInt();
				dummy = wifiScanScanner.nextLine();
				mapPosition.scannedWiFiList = extractListOfWiFiNetworksFromFile(
						wifiScanScanner, wifiCount);

				// Checking that ids of position and WiFi are correctly matched
				if (mapPosition.id != idWiFi) {
					Log.e(moduleLogName,
							"Different ids of wifi scans and images -- something went wrong and map was not read");
					return new ArrayList<MapPosition>();
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			mapPositions.add(mapPosition);
		}

		return mapPositions;

	}

	/*
	 * Loads prior map of corridors and building plan
	 */
	public BuildingPlan loadCorridorMap(String mapName) {
		Log.d(moduleLogName, "loadCorridorMap");

		// Storing building plan
		BuildingPlan buildingPlan = new BuildingPlan();

		// background image
		String path = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/PriorData/" + mapName);
		String backgroundFilePath = path + "/buildingPlan.jpg";
		buildingPlan.mBackgroundImage = BitmapFactory
				.decodeFile(backgroundFilePath);

		// reading map file
		String mapFilePath = path + "/buildingPlan.map";

		Scanner mapScanner = null;
		try {
			mapScanner = new Scanner(new BufferedReader(new FileReader(
					mapFilePath)));
			mapScanner.useLocale(Locale.US);
		} catch (FileNotFoundException e1) {
		}

		mapScanner.useDelimiter("\\s|\\n");
		while (mapScanner.hasNext()) {
			String keyword = mapScanner.next();

			// The data about origin
			if (keyword.compareTo("ORIGIN") == 0) {

				readingBuildingPlanOrigin(buildingPlan, mapScanner);

				// The data about scale
			} else if (keyword.compareTo("SCALE") == 0) {

				readingBuildingPlanMetricScale(buildingPlan, mapScanner);
			}
			// The data about node
			else if (keyword.compareTo("NODE") == 0) {
				readingBuildingPlanNodes(buildingPlan, mapScanner);
			}
			// The data about edge
			else if (keyword.compareTo("EDGE") == 0) {
				readingBuildingPlanEdges(buildingPlan, mapScanner);
			} else
				Log.d(moduleLogName, "Unrecognized line tag");
			mapScanner.nextLine();
		}

		return buildingPlan;
	}

	/**
	 * @param buildingPlan
	 * @param mapScanner
	 */
	private void readingBuildingPlanEdges(BuildingPlan buildingPlan,
			Scanner mapScanner) {
		double idFrom = mapScanner.nextInt();
		double idTo = mapScanner.nextInt();
		double distance = mapScanner.nextDouble();
		Log.d(moduleLogName, "EDGE from " + idFrom + " to " + idTo + " dist: " + distance);

		Node first = null, second = null;
		for (Node n : buildingPlan.nodeLocations) {
			if (n.getId() == idFrom)
				first = n;
			else if (n.getId() == idTo)
				second = n;
		}
		buildingPlan.edgeLocations.add(new Edge(first, second, distance));
	}

	/**
	 * @param buildingPlan
	 * @param mapScanner
	 */
	private void readingBuildingPlanNodes(BuildingPlan buildingPlan,
			Scanner mapScanner) {
		int id = mapScanner.nextInt();
		double px = mapScanner.nextDouble();
		double py = mapScanner.nextDouble();
		double x = mapScanner.nextDouble();
		double y = mapScanner.nextDouble();
		Log.d(moduleLogName, "NODE id=" + id + " pixel pos (" + px + ", " + py
				+ "), metre pos (" + x + ", " + y + ")");
		buildingPlan.nodeLocations.add(new Node(id, px, py, x, y));
	}

	/**
	 * @param buildingPlan
	 * @param mapScanner
	 */
	private void readingBuildingPlanMetricScale(BuildingPlan buildingPlan,
			Scanner mapScanner) {
		buildingPlan.oldMapPixels2Metres = mapScanner.nextDouble();
		Log.d(moduleLogName, "SCALE is " + buildingPlan.oldMapPixels2Metres);
	}

	/**
	 * @param buildingPlan
	 * @param mapScanner
	 */
	private void readingBuildingPlanOrigin(BuildingPlan buildingPlan,
			Scanner mapScanner) {
		buildingPlan.originX = mapScanner.nextDouble();
		buildingPlan.originY = mapScanner.nextDouble();
		double angle0 = mapScanner.nextDouble();

		Log.d(moduleLogName, "ORIGIN is in (" + buildingPlan.originX + ", "
				+ buildingPlan.originY + ") with angle = " + angle0);
	}

	/**
	 * Method used to store current position in a map. Saves image, wifi scan,
	 * angle and position (has to be provided by user)
	 * 
	 * @param mapName
	 * @param posX
	 * @param posY
	 * @param posZ
	 */
	public void saveMapPoint(String mapName, MapPosition mapPosition) {
		// Create directory to save new Map
		String pathName = creatingSaveMapDirectory(mapName);

		// Saving image
		saveImageOfMapPoint(mapPosition.id, mapPosition.image, pathName);

		// Saving WiFis
		saveWiFiScansOfMapPoint(mapPosition.id, mapPosition.scannedWiFiList,
				pathName);

		// Saving pos
		savePositionsOfMapPoint(mapPosition.id, mapPosition.X, mapPosition.Y,
				mapPosition.Z, mapPosition.angle, pathName);

		// Increase Id for next point
		// mapPointId++;
	}
	
	public void clearNewMap(String mapName) {
		String pathName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/PriorData/" + mapName + "/");
		
		Log.d(moduleLogName, "mapName = " + mapName);
		
		File dir = new File(pathName + "images/");
		File[] children = dir.listFiles();
		for (int i = 0; i < children.length; i++) {
			children[i].delete();
		}
		
		dir = new File(pathName + "wifiScans/");
		children = dir.listFiles();
		for (int i = 0; i < children.length; i++) {
			children[i].delete();
		}
		
		dir = new File(pathName + "positions.list");
		dir.delete();
	}

	/**
	 * Reads the Wifi list from map-compatible format
	 * 
	 * @param wifiScanScanner
	 * @param wifiCount
	 * @param scannedWiFisList
	 */
	public static List<MyScanResult> extractListOfWiFiNetworksFromFile(
			Scanner wifiScanScanner, int wifiCount) {

		List<MyScanResult> scannedWiFisList = new ArrayList<MyScanResult>();
		for (int j = 0; j < wifiCount; j++) {
			String line = wifiScanScanner.nextLine();

			String[] values = line.split("\\t+");

			String BSSID = values[0];
			int level;
			if (values.length == 3)
				level = Integer.parseInt(values[2]);
			else
				level = Integer.parseInt(values[1]);

			MyScanResult wifiNetwork = new MyScanResult(BSSID, level);
			scannedWiFisList.add(wifiNetwork);

			Log.d(moduleLogName, "WiFiDatabase data: " + BSSID + " " + level);
		}
		return scannedWiFisList;
	}

	/**
	 * Checks and creates a directory for a new map
	 * 
	 * @param mapName
	 * @return
	 */
	private String creatingSaveMapDirectory(String mapName) {
		// Setting the directory of output
		String pathName = String.format(Locale.getDefault(), Environment
				.getExternalStorageDirectory().toString()
				+ "/OpenAIL/PriorData/" + mapName + "/");

		// Create directory if it doesn't exist
		File folder = new File(pathName);
		if (!folder.exists()) {
			folder.mkdir();
		}
		return pathName;
	}

	/**
	 * Saves the provided image in a map-compatible format
	 * 
	 * @param image
	 * @param pathName
	 */
	private void saveImageOfMapPoint(int mapPointId, Mat image, String pathName) {
		// Creating directory if needed
		File folder = new File(pathName + "images/");
		if (!folder.exists()) {
			folder.mkdir();
		}

		String imagePath = pathName + "images/"
				+ String.format("%05d.png", mapPointId);
		Highgui.imwrite(imagePath, image);
	}

	/**
	 * Saves the provided position in a map-compatible format
	 * 
	 * @param posX
	 * @param posY
	 * @param posZ
	 * @param angle
	 * @param pathName
	 */
	private void savePositionsOfMapPoint(int mapPointId, double posX,
			double posY, double posZ, double angle, String pathName) {
		FileOutputStream foutStream;
		PrintStream outStreamRawData;
		outStreamRawData = null;
		try {
			foutStream = new FileOutputStream(pathName + "positions.list", true);
			outStreamRawData = new PrintStream(foutStream);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		// Save positions (placeId, X, Y, Z, Yaw)
		outStreamRawData.print(mapPointId + " " + posX + " " + posY + " "
				+ posZ + " " + angle + "\n");

		// Close stream
		outStreamRawData.close();
	}

	/**
	 * Saves the provided scan list in a map-compatible format
	 * 
	 * @param wifiList
	 * @param pathName
	 */
	private void saveWiFiScansOfMapPoint(int mapPointId,
			List<MyScanResult> wifiList, String pathName) {

		// Create directory if needed
		File folder = new File(pathName + "wifiScans/");
		if (!folder.exists()) {
			folder.mkdir();
		}

		FileOutputStream foutStream;
		PrintStream outStreamRawData = null;
		try {
			foutStream = new FileOutputStream(pathName + "wifiScans/"
					+ String.format("%05d.wifiscan", mapPointId), false);
			outStreamRawData = new PrintStream(foutStream);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		// Save the initial line of scan (placeId, number of WiFi networks)
		outStreamRawData.print(mapPointId + " " + wifiList.size() + "\n");

		// Save BSSID (MAC), SSID (network name), lvl (in DBm)
		for (int i = 0; i < wifiList.size(); i++) {
			MyScanResult scanResult = wifiList.get(i);
			outStreamRawData.print(scanResult.BSSID + "\t"
					+ scanResult.networkName + "\t" + scanResult.level + "\n");
		}
	}

}
