package org.dg.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
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
import android.view.Surface;
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
					openAIL.startLocalization();
				} else {
					openAIL.stopLocalization();
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
		
		// Save WiFi place
		if (link.contains("Save map point")) {
			
			Log.d(TAG, "SMP TEST: " +link);
			
			String[] separated = link.split("&");
			
			Log.d(TAG, "SMP TEST 1: " + separated[0]);
			Log.d(TAG, "SMP TEST 2: " + separated[1]);
			Log.d(TAG, "SMP TEST 3: " + separated[2]);
			Log.d(TAG, "SMP TEST 4: " + separated[3]);
			Log.d(TAG, "SMP TEST 5: " + separated[4]);
			
			NumberFormat nf = NumberFormat.getInstance(Locale.US);
			
			double[] pos = new double[3];
			for(int i=0;i<3;i++)
			{
				try {
					Number myNumber = nf.parse(separated[2+i]);
					pos[i] = myNumber.doubleValue();
				} catch (ParseException e) {
					e.printStackTrace();
				}	
			}
			

			Log.d(TAG, "Save map point::" + pos[0] + "::" + pos[1] + "::" + pos[2] + "::");
			openAIL.saveMapPoint(separated[1], pos[0], pos[1], pos[2]);
			
		}
		
		// Save VPR place
		if (link.contains("Save VPR place")) {
			
			Scanner scanner = new Scanner(link);

			// use US locale to be able to identify doubles in the string
			scanner.useLocale(Locale.US);

			int i = 0;
			double[] pos = new double[3];
			while (scanner.hasNext()) {

				// if the next is a double, print found and the double
				if (scanner.hasNextDouble()) {
					pos[i++] = scanner.nextDouble();
				} else
					scanner.next();
			}
			
			if ( i == 3){
				Log.d(TAG, "Save VPR position::" + pos[0] + "::" + pos[1] + "::" + pos[2] + "::");
			}
			else{
				Log.d(TAG, "Save VPR position - could not find 3 numbers");
			}
			
			Camera.Parameters params = camera.getParameters();
			int size = params.getPreviewSize().width * params.getPreviewSize().height * 
		            ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
			
	        byte buffer[] = new byte[size];
	        camera.addCallbackBuffer(buffer);
			
			Mat image = getCurPreviewImage();
			
			if(image != null){
				openAIL.visualPlaceRecognition.savePlace(pos[0], pos[1], pos[2], image);
				Toast.makeText(this, "Saved image",
						Toast.LENGTH_LONG).show();
			}
			else{
				Log.e(TAG, "Save VPR position - image == null");
			}
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
		mPager.setOffscreenPageLimit(3);
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
//			int recognizedPlaceId = openAIL.wifiScanner
//					.recognizePlaceBasedOnLastScan();
			int recognizedPlaceId = 0;
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
	public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
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
		Log.d(TAG, "onResume");
		super.onResume();
//		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
//				mLoaderCallback);

		camera = Camera.open();
		
		// Rotating the view according to device 
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
	    android.hardware.Camera.getCameraInfo(0, info);
	    int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
	    int degrees = 0;
	    switch (rotation)
	    {
	    case Surface.ROTATION_0:
	        degrees = 0;
	        break;
	    case Surface.ROTATION_90:
	        degrees = 90;
	        break;
	    case Surface.ROTATION_180:
	        degrees = 180;
	        break;
	    case Surface.ROTATION_270:
	        degrees = 270;
	        break;
	    }

	    int result;
	    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
	    {
	        result = (info.orientation + degrees) % 360;
	        result = (360 - result) % 360; // compensate the mirror
	    }
	    else {
	        result = (info.orientation - degrees + 360) % 360;
	    }
	    camera.setDisplayOrientation(result);
	    
	    // Settings the focus to some fixed value
		Camera.Parameters parameters = camera.getParameters();		
		List<String> modes = parameters.getSupportedFocusModes();
		if ( modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
		}
		else if ( modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		}
		camera.setParameters(parameters);
		
		//fragment with camera preview
		ScreenSlidePageFragment cameraFragment = (ScreenSlidePageFragment)((ScreenSlidePagerAdapter)mPagerAdapter).getItem(0);
		cameraFragment.setCamera(camera);
		
		preview = cameraFragment.preview;
		camera.setPreviewCallback(preview);
		
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		if (camera != null) {
			
			//fragment with camera preview
			ScreenSlidePageFragment cameraFragment = (ScreenSlidePageFragment)((ScreenSlidePagerAdapter)mPagerAdapter).getItem(0);
			camera.stopPreview();
			camera.setPreviewCallback(null);
			cameraFragment.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}
	
	protected Mat getCurPreviewImage(){
		ScreenSlidePageFragment cameraFragment = (ScreenSlidePageFragment)((ScreenSlidePagerAdapter)mPagerAdapter).getItem(0);

		openAIL.preview = cameraFragment.preview;
		return cameraFragment.preview.getCurPreviewImage();
	}
	
}
