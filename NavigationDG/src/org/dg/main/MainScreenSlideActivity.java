package org.dg.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.dg.camera.CameraSaver;
import org.dg.camera.Preview;
import org.dg.camera.VisualPlaceRecognition;
import org.dg.inertialSensors.InertialSensors;
import org.dg.inertialSensors.ProcessRecorded;
import org.dg.openAIL.OpenAndroidIndoorLocalization;
import org.dg.wifi.WifiScanner;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainScreenSlideActivity extends Activity implements
		ScreenSlidePageFragment.OnItemSelectedListener {

	private static final String TAG = "Main::Activity";
	/**
	 * 
	 * 
	 */
	OpenAndroidIndoorLocalization openAIL;

	/*
	 * 
	 * 
	 */
	Preview preview;
	Camera camera;

	// Orient in main update
	private Timer orientAndWiFiScanUpdateTimer = new Timer();

	// WiFi Recognition
	boolean wiFiRecognitionStarted = false;
	private Timer wiFiRecognitionTimer = new Timer();

	/**
	 * The number of pages (wizard steps) to show in this demo.
	 */
	private static final int NUM_PAGES = 3;

	/**
	 * The pager widget, which handles animation and allows swiping horizontally
	 * to access previous and next wizard steps.
	 */
	private ViewPager mPager;

	/**
	 * The pager adapter, which provides the pages to the view pager widget.
	 */
	private PagerAdapter mPagerAdapter;

	// Methods
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				Toast.makeText(MainScreenSlideActivity.this,
						"Loaded all libraries", Toast.LENGTH_LONG).show();
				
				
				openAIL.initAfterOpenCV();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	// Now we can define the action to take in the activity when the fragment
	// event fires
	@Override
	public void onButtonPressed(String link) {
		Log.d(TAG, "Button pressed and send: " + link);

		// 1. Take picture
		if (link.contains("Take picture")) {
			camera.takePicture(null, null, new CameraSaver());
		}

		// 2. Start/Stop Orientation
		if (link.contains("Run inertial sensors")
				|| link.contains("Stop inertial sensors")) {
			if (openAIL.inertialSensors.getState() == false) {
				openAIL.inertialSensors.save2file(false);
				openAIL.inertialSensors.start();
			} else {
				openAIL.inertialSensors.stop();
			}
		}

		// 3. Start/Stop record inertial sensors
		if (link.contains("Start record inertial sensors")
				|| link.contains("Stop record inertial sensors")) {
			if (openAIL.inertialSensors.getState() == false) {

				openAIL.inertialSensors.save2file(true);
				openAIL.inertialSensors.start();

			} else {

				openAIL.inertialSensors.stop();
			}
		}

		// 4. Do a single WiFi Scan
		if (link.contains("Do a single WiFi scan")) {
			if (openAIL.inertialSensors.getState()) {
				openAIL.wifiScanner
						.startTimestampOfGlobalTime(openAIL.inertialSensors
								.getTimestamp());
			}
			openAIL.wifiScanner.singleScan(true).continuousScanning(false);
			openAIL.wifiScanner.startScanning();
		}
		// 5. Record continuous WiFi Scans
		if (link.contains("Stop WiFi scans")
				|| link.contains("Start WiFi scans")) {
			if (openAIL.wifiScanner.getRunningState()) {
				openAIL.wifiScanner.stopScanning();
			} else {
				if (openAIL.inertialSensors.getState()) {
					openAIL.wifiScanner
							.startTimestampOfGlobalTime(openAIL.inertialSensors
									.getTimestamp());
				}
				openAIL.wifiScanner.singleScan(false).continuousScanning(true);
				openAIL.wifiScanner.startScanning();
			}
		}

		// 6. Add WiFi scan to recognition list
		if (link.contains("Add WiFi to recognition")) {
			
			
			// TODO!!!
			//openAIL.wifiScanner.addLastScanToRecognition();

			if (wiFiRecognitionStarted == false) {
				wiFiRecognitionTimer.scheduleAtFixedRate(
						new UpdateWiFiSRecognitionGUI(), 1000, 4000);
				wiFiRecognitionStarted = false;
			}

		}
		//
		// // 7. Run stepometer
		if (link.contains("Stop stepometer")
				|| link.contains("Start stepometer")) {
			if (openAIL.inertialSensors.isStepometerStarted()) {
				openAIL.inertialSensors.stopStepometer();
			} else {
				openAIL.inertialSensors.startStepometer();
			}
		}

		// Side View 1 - Floor detection
		if (link.contains("Start barometer") || link.contains("Stop barometer")) {
			if (openAIL.inertialSensors.isBarometerProcessingStarted()) {
				openAIL.inertialSensors.stopBarometerProcessing();
			} else {
				openAIL.inertialSensors.startBarometerProcessing();
			}
		}

		// Side View 2 - Start/Optimize Graph
		if (link.contains("Start graph") || link.contains("Optimize graph")) {
			{

				if (!openAIL.graphManager.started()) {
					openAIL.startGraph();
				} else {
					openAIL.stopAndOptimizeGraph();
				}

			}
		}
		
		// Side View 3 - Optimize graph from file
		if (link.contains("Graph from file")) {
			openAIL.graphManager.optimizeGraphInFile("lastCreatedGraph.g2o");
		}
		
		// Side View 4 - Add magnetic place to recognition
		if (link.contains("Add magnetic place to recognition")) {
			openAIL.inertialSensors.addMagneticRecognitionPlace();
		}
		
		// Side View 5 - Start/Stop complementary filter
		if (link.contains("Start complementary filter") || link.contains("Stop complementary filter")) {
			// TODO !!! 
		}
		
		// Side View 6 - Orientation From file test
		if (link.contains("Orient file test")) {
		
			Thread thread = new Thread() {
			    @Override
			    public void run() {
			    	File folder = new File(Environment.getExternalStorageDirectory()
							+ "/DG");

					if (!folder.exists()) {
						folder.mkdir();
					}

					File dir = new File(String.format(
							Environment.getExternalStorageDirectory() + "/DG/xSenseTel3"));
					if (!dir.exists()) {
						dir.mkdirs();
					}
					
					try{
						PrintStream paramOutStream = new PrintStream(new FileOutputStream(dir.toString() + "/param.log"));
						float param = 0.000001f;
						while(param < 0.02f){
	//						ProcessRecorded.process(dir, 0.999325f);
							double score = ProcessRecorded.process(dir, 1.0f - param);
							
							paramOutStream.print(Float.toString(param) + " " + Double.toString(score));
							paramOutStream.print(System.getProperty("line.separator"));
							
							Log.d(TAG, String.format("param = %f, score = %f", param, score));
							
							param *= 2;
						}
						paramOutStream.close();
					}
					catch(FileNotFoundException e){
						e.printStackTrace();
					}
			    }
			};
			thread.start();
		}
		
		// Side View 7 - TESTING VisualPlaceRecognition
		if (link.contains("Visual Place Recognition")) {
			openAIL.visualPlaceRecognition.callAndVerifyAllMethods();
		}
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Loading OpenCV
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_screen_slide);

		List<ScreenSlidePageFragment> fragments = new ArrayList<ScreenSlidePageFragment>();
		fragments.add(ScreenSlidePageFragment.create(0));
		fragments.add(ScreenSlidePageFragment.create(1));
		fragments.add(ScreenSlidePageFragment.create(2));

		// Instantiate a ViewPager and a PagerAdapter.
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager(),
				fragments);
		mPager.setAdapter(mPagerAdapter);
		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				// When changing pages, reset the action bar actions since they
				// are dependent
				// on which page is currently active. An alternative approach is
				// to have each
				// fragment expose actions itself (rather than the activity
				// exposing actions),
				// but for simplicity, the activity provides the actions in this
				// sample.
				invalidateOptionsMenu();
			}
		});


		
		
		// Init Sensor Managers
		SensorManager sensorManager;
		sensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);

		// Init WiFi
		WifiManager wifiManager;
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		// Init library
		openAIL = new OpenAndroidIndoorLocalization(sensorManager, wifiManager);
		
		// Reguster wifi scanner
		registerReceiver(openAIL.wifiScanner, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		// Initialize update of orient in GUI
		orientAndWiFiScanUpdateTimer.scheduleAtFixedRate(
				new UpdateOrientAndWiFiScanGUI(), 2000, 100);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_screen_slide, menu);

		if (mPager.getCurrentItem() != 0) {
			MenuItem item2 = menu.add(Menu.NONE, R.id.action_previous,
					Menu.NONE, R.string.action_previous);
			item2.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
					| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}

		// Add either a "next" or "finish" button to the action bar, depending
		// on which page
		// is currently selected.
		if (mPager.getCurrentItem() != NUM_PAGES - 1) {
			MenuItem item = menu.add(Menu.NONE, R.id.action_next, Menu.NONE,
					R.string.action_next);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
					| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Navigate "up" the demo structure to the launchpad activity.
			// See http://developer.android.com/design/patterns/navigation.html
			// for more.
			mPager.setCurrentItem(0);
			return true;

		case R.id.action_previous:
			// Go to the previous step in the wizard. If there is no previous
			// step,
			// setCurrentItem will do nothing.
			mPager.setCurrentItem(mPager.getCurrentItem() - 1);
			return true;

		case R.id.action_next:
			// Advance to the next step in the wizard. If there is no next step,
			// setCurrentItem
			// will do nothing.
			mPager.setCurrentItem(mPager.getCurrentItem() + 1);

			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	
	

	class UpdateOrientAndWiFiScanGUI extends TimerTask {
		public void run() {
			float[] orient = openAIL.inertialSensors.getCurrentAEKFOrient();
			float[] compOrient = openAIL.inertialSensors.getCurrentComplementaryOrient();
			
			String strongestWiFiNetwork = openAIL.wifiScanner
					.getStrongestNetwork();
			int WiFiCount = openAIL.wifiScanner.getNetworkCount();
			float foundFreq = openAIL.inertialSensors
					.getLastDetectedFrequency();
			float stepCount = openAIL.inertialSensors
					.getDetectedNumberOfSteps();
			float stepDistance = openAIL.inertialSensors
					.getCovertedStepDistance();
			int currentFloor = openAIL.inertialSensors.getCurrentFloor();
			float estimatedHeight = openAIL.inertialSensors
					.getEstimatedHeight();

			// Passing to fragment for update
			int id = mPager.getCurrentItem();
			ScreenSlidePageFragment x = (ScreenSlidePageFragment) ((ScreenSlidePagerAdapter) mPagerAdapter)
					.getItem(id);
			x.updateGUIData(orient, compOrient, strongestWiFiNetwork, WiFiCount, foundFreq,
					stepCount, stepDistance, currentFloor, estimatedHeight);

		}

	}

	class UpdateWiFiSRecognitionGUI extends TimerTask {
		public void run() {
			int recognizedPlaceId = openAIL.wifiScanner
					.recognizePlaceBasedOnLastScan();
			int sizeOfPlaceDatabase = openAIL.wifiScanner
					.getSizeOfPlaceDatabase();

			int recognizedMagneticPlaceId = openAIL.inertialSensors
					.recognizePlaceBasedOnMagneticScan();
			int sizeOfMagneticPlaceDatabase = openAIL.inertialSensors
					.getSizeOfPlaceDatabase();

			// Passing to fragment for update
			int id = mPager.getCurrentItem();
			ScreenSlidePageFragment x = (ScreenSlidePageFragment) ((ScreenSlidePagerAdapter) mPagerAdapter)
					.getItem(id);
			x.updateGUIPlaceRecognize(recognizedPlaceId, sizeOfPlaceDatabase,
					recognizedMagneticPlaceId, sizeOfMagneticPlaceDatabase);
		}

	}

	/**
	 * A simple pager adapter that represents 5 {@link ScreenSlidePageFragment}
	 * objects, in sequence.
	 */
	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
		private List<ScreenSlidePageFragment> fragments;

		public ScreenSlidePagerAdapter(FragmentManager fm,
				List<ScreenSlidePageFragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
			// return ScreenSlidePageFragment.create(position);
		}

		@Override
		public int getCount() {
			return NUM_PAGES;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback);

		camera = Camera.open();
		//preview.setCamera(camera);
		//camera.startPreview();

	}

	@Override
	protected void onPause() {
		if (camera != null) {
			camera.stopPreview();
			// preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}
}
