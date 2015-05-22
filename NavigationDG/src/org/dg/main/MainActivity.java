package org.dg.main;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.dg.camera.CameraSaver;
import org.dg.camera.Preview;
import org.dg.graphManager.GraphManager;
import org.dg.graphManager.wiFiMeasurement;
import org.dg.inertialSensors.InertialSensors;
import org.dg.main.R;
import org.dg.openAIL.OpenAndroidIndoorLocalization;
import org.dg.wifi.WifiScanner;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	// public class MainActivity extends ActionBarActivity {
	private static final String TAG = "Main::Activity";

	private static final int VIEW_MODE_PREVIEW = 0;
	private static final int VIEW_MODE_RECORD = 1;
	private static final int VIEW_MODE_RUN = 2;

	private int mViewMode = VIEW_MODE_PREVIEW;
	private MenuItem mItemPreviewMode;
	private MenuItem mItemRecordMode;
	private MenuItem mItemRunMode;
	private MenuItem mItemAbout;

	Preview preview;
	Camera camera;
	String fileName;
	Activity act;
	Context ctx;

	// Orient in main update
	private Timer orientAndWiFiScanUpdateTimer = new Timer();
	boolean wiFiRecognitionStarted = false;
	private Timer wiFiRecognitionTimer = new Timer();
	private Timer updateGraphTimer = new Timer();
	public Handler mHandlerOrient, mHandlerWiFiRecognition;

	// Class implementing the localization 
	OpenAndroidIndoorLocalization openAIL;
	
	
	



	

	// Methods
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				// Loading libraries
				// System.loadLibrary("imu");
				// System.loadLibrary("nonfree");
				// System.loadLibrary("scale_estimation");
				// System.loadLibrary("visual_odometry");
				// System.loadLibrary("AHRSModule");

				Toast.makeText(MainActivity.this, "Loaded all libraries",
						Toast.LENGTH_LONG).show();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Loading OpenCV
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}

		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);

		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main_surface_view);

		preview = new Preview(this,
				(SurfaceView) findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.preview)).addView(preview);
		preview.setKeepScreenOn(true);
		
		
		// Init Sensor Managers
		SensorManager sensorManager;
		sensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		// Init WiFi
				WifiManager wifiManager;
				wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				
				
		
		// Init library
		openAIL = new OpenAndroidIndoorLocalization(sensorManager, wifiManager);

		// Register 
		registerReceiver(openAIL.wifiScanner, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		
		// Init graph
//		openAIL.graphManager = new GraphManager();
//		openAIL.inertialSensors = new InertialSensors(sensorManager);
//		openAIL.wifiScanner = new WifiScanner(wifiManager);
		
		// Add buttons
		initializeButtons();

		// Initialize update of orient in GUI
		mHandlerOrient = new Handler();
		orientAndWiFiScanUpdateTimer.scheduleAtFixedRate(
				new UpdateOrientAndWiFiScanGUI(), 2000, 200);

		// Initialize update of WiFi recognition in GUI
		mHandlerWiFiRecognition = new Handler();

	}

	/**
	 * Method used to create buttons: 1. Take picture 2. Run inertial sensors 3.
	 * Record inertial sensors 4. Record one WiFi scan 5. Record continuous WiFi
	 * 6. Add WiFi scan to recognition list 7. About
	 * 
	 * Side: 1. Run Stepometer
	 */
	private void initializeButtons() {

	}

	
	/**
	 * 
	 */
	private void initButtonAddMagneticPlaceToRecognition(final int id) {
		Button buttonAddMagneticPlaceToRecognition = (Button) findViewById(id);
		buttonAddMagneticPlaceToRecognition .setText("Add magnetic place to recognition");
		buttonAddMagneticPlaceToRecognition .setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				openAIL.inertialSensors.addMagneticRecognitionPlace();
			}
		});
	}
	
	/**
	 * 
	 */
	private void initButtonStartFloorDetection(final int id) {
		Button buttonStartFloorDetection = (Button) findViewById(id);
		buttonStartFloorDetection.setText("Start barometer");
		buttonStartFloorDetection.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button buttonStartFloorDetection = (Button) findViewById(id);
				if (openAIL.inertialSensors.isBarometerProcessingStarted()) {
					buttonStartFloorDetection.setText("Start barometer");
					openAIL.inertialSensors.stopBarometerProcessing();
					;
				} else {
					buttonStartFloorDetection.setText("Stop barometer");
					openAIL.inertialSensors.startBarometerProcessing();
					;
				}

			}
		});
	}
	
	/**
	 * 
	 */
	private void initButtonStartGraphTestFromFile(final int id) {
		Button buttonStartGraphTestFromFile = (Button) findViewById(id);
		buttonStartGraphTestFromFile.setText("Graph from file");
		buttonStartGraphTestFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				{
					openAIL.graphManager.optimizeGraphInFile("graphFile.g2o");

				}
			}
		});
	}

	

	/**
	 * 
	 */
	private void initButtonStartGraphOnline(final int id) {
		Button buttonStartGraphOnline = (Button) findViewById(id);
		buttonStartGraphOnline.setText("Start graph");
		buttonStartGraphOnline.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				{
					Button buttonStartGraphOnline = (Button) findViewById(id);
					if (!openAIL.graphManager.started()) {
						openAIL.graphManager.start();
						updateGraphTimer.scheduleAtFixedRate(new UpdateGraph(),
								1000, 200);
						buttonStartGraphOnline.setText("Optimize graph");
					} else {
						openAIL.graphManager.stop();
						updateGraphTimer.cancel();
						openAIL.graphManager.optimize(100);
						buttonStartGraphOnline.setText("Start graph");
					}

				}
			}
		});
	}

	/**
	 * 
	 */
	private void initButtonRunStepometer(final int id) {
		Button buttonRunStepometer = (Button) findViewById(id);
		buttonRunStepometer.setText("Run stepometer");
		buttonRunStepometer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button buttonRunStepometer = (Button) findViewById(id);
				if (openAIL.inertialSensors.isStepometerStarted()) {
					buttonRunStepometer.setText("Start stepometer");
					openAIL.inertialSensors.stopStepometer();
				} else {
					buttonRunStepometer.setText("Stop stepometer");
					openAIL.inertialSensors.startStepometer();
				}

			}
		});
	}

	/**
	 * 
	 */
	private void initButtonAddWiFiScanToRecognition(int id) {
		Button buttonAddWiFiScanToRecognition = (Button) findViewById(id);
		buttonAddWiFiScanToRecognition.setText("Add WiFi to recognition");
		buttonAddWiFiScanToRecognition
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						
						// TODO !!!
						// openAIL.wifiScanner.addLastScanToRecognition();

						if (wiFiRecognitionStarted == false) {
							wiFiRecognitionTimer
									.scheduleAtFixedRate(
											new UpdateWiFiSRecognitionGUI(),
											1000, 4000);
							wiFiRecognitionStarted = false;
						}
					}
				});
	}

	/**
	 * 
	 */
	private void initButtonRecordSingleWiFiScan(int id) {
		Button buttonRecordSingleWiFiScan = (Button) findViewById(id);
		buttonRecordSingleWiFiScan.setText("Do a single WiFi scan");
		buttonRecordSingleWiFiScan.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (openAIL.inertialSensors.getState()) {
					openAIL.wifiScanner.startTimestampOfGlobalTime(openAIL.inertialSensors
							.getTimestamp());
				}
				openAIL.wifiScanner.singleScan(true).continuousScanning(false);
				openAIL.wifiScanner.startScanning();
			}
		});
	}

	/**
	 * 
	 */
	private void initButtonRecordContinuousWiFiScans(final int id,
			final int idToBlock) {
		Button buttonRecordContinuousWiFiScans = (Button) findViewById(id);
		buttonRecordContinuousWiFiScans.setText("Start WiFi scans");
		buttonRecordContinuousWiFiScans
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Button buttonRecordContinuousWiFiScans = (Button) findViewById(id);
						Button buttonRecordSingleWiFiScan = (Button) findViewById(idToBlock);

						if (openAIL.wifiScanner.getRunningState()) {
							buttonRecordContinuousWiFiScans
									.setText("Start WiFi scans");
							buttonRecordSingleWiFiScan.setEnabled(true);
							openAIL.wifiScanner.stopScanning();
						} else {
							buttonRecordContinuousWiFiScans
									.setText("Stop WiFi scans");
							buttonRecordSingleWiFiScan.setEnabled(false);
							if (openAIL.inertialSensors.getState()) {
								openAIL.wifiScanner
										.startTimestampOfGlobalTime(openAIL.inertialSensors
												.getTimestamp());
							}
							openAIL.wifiScanner.singleScan(false).continuousScanning(
									true);
							openAIL.wifiScanner.startScanning();
						}
					}
				});
	}

	/**
	 * 
	 */
	private void initButtonRecordinertialSensors(final int id,
			final int idToBlock) {
		Button buttonRecordinertialSensors = (Button) findViewById(id);
		buttonRecordinertialSensors.setText("Record inertial sensors");

		buttonRecordinertialSensors.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button buttonRecordinertialSensors = (Button) findViewById(id);
				Button buttonStartOrientation = (Button) findViewById(idToBlock);
				if (openAIL.inertialSensors.getState() == false) {
					buttonStartOrientation.setEnabled(false);
					buttonRecordinertialSensors
							.setText("Stop record inertial sensors");
					openAIL.inertialSensors.save2file(true);
					openAIL.inertialSensors.start();

				} else {
					buttonStartOrientation.setEnabled(true);
					buttonRecordinertialSensors
							.setText("Record inertial sensors");
					openAIL.inertialSensors.stop();
				}

			}
		});
	}

	/**
	 * 
	 */
	private void initButtonStartOrientation(final int id, final int idToBlock) {
		Button buttonStartOrientation = (Button) findViewById(id);
		buttonStartOrientation.setText("Run inertial sensors");
		buttonStartOrientation.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button inertialSensors = (Button) findViewById(idToBlock);
				Button buttonStartOrientation = (Button) findViewById(id);
				if (openAIL.inertialSensors.getState() == false) {
					inertialSensors.setEnabled(false);
					buttonStartOrientation.setText("Stop inertial sensors");
					openAIL.inertialSensors.save2file(false);
					openAIL.inertialSensors.start();
				} else {
					inertialSensors.setEnabled(true);
					buttonStartOrientation.setText("Run inertial sensors");
					openAIL.inertialSensors.stop();
				}

			}
		});
	}

	/**
	 * 
	 */
	private void initButtonTakePicture(int id) {
		Button buttonTakePicture = (Button) findViewById(id);
		buttonTakePicture.setText("Take picture");
		buttonTakePicture.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				camera.takePicture(null, null, new CameraSaver());
			}
		});
	}

	class UpdateGraph extends TimerTask {
		public void run() {

			 // get distance
			 double distance = openAIL.inertialSensors.getGraphStepDistance();
			
			 // Adding WiFi measurement
			 if ( distance > 0.1 )
			 {
				 // get angle
				 float yawZ = openAIL.inertialSensors.getYawForStepometer();
				 
				 Log.d(TAG, "Yaw for stepometer: " + yawZ + " in deg");
				 
				 // We need to change yawZ into radians and change direction
				 yawZ = (float) (-yawZ * Math.PI / 180.0f);
				 
				 openAIL.graphManager.addStepometerMeasurement(distance, yawZ);
			 }
			
			
			 // Adding WiFi measurements
			 List<wiFiMeasurement> wifiList = openAIL.wifiScanner.getGraphWiFiList();
			 if ( wifiList != null)
				 openAIL.graphManager.addMultipleWiFiMeasurements(wifiList);

		}

	}

	class UpdateWiFiSRecognitionGUI extends TimerTask {
		public void run() {
			int recognizedPlaceId = openAIL.wifiScanner.recognizePlaceBasedOnLastScan();
			int sizeOfPlaceDatabase = openAIL.wifiScanner.getSizeOfPlaceDatabase();

			int recognizedMagneticPlaceId = openAIL.inertialSensors.recognizePlaceBasedOnMagneticScan();
			int sizeOfMagneticPlaceDatabase = openAIL.inertialSensors.getSizeOfPlaceDatabase();
			
			UpdateWiFiInGUI obj = new UpdateWiFiInGUI(recognizedPlaceId,
					sizeOfPlaceDatabase, recognizedMagneticPlaceId, sizeOfMagneticPlaceDatabase);
			mHandlerWiFiRecognition.post(obj);
		}

	}

	class UpdateWiFiInGUI implements Runnable {
		int placeId, sizeOfPlaceDatabase;
		int magneticPlaceId, sizeOfMagneticPlaceDatabase;

		public UpdateWiFiInGUI(int _placeId, int _sizeOfPlaceDatabase, int _magneticPlaceId, int _sizeOfMagneticDatabase) {
			placeId = _placeId;
			sizeOfPlaceDatabase = _sizeOfPlaceDatabase;
			magneticPlaceId = _magneticPlaceId;
			sizeOfMagneticPlaceDatabase = _sizeOfMagneticDatabase;
		}

		public void run() {
			TextView mTextViewRecognizedPlace = (TextView) findViewById(R.id.textViewWiFi3);

			mTextViewRecognizedPlace.setText("Recognized place id: "
					+ Integer.toString(placeId) + " (out of "
					+ Integer.toString(sizeOfPlaceDatabase) + " places)");
			
			TextView mTextViewMagneticRecognizedPlace = (TextView) findViewById(R.id.textViewMagnetic);

			mTextViewMagneticRecognizedPlace.setText("Recognized mag place id: "
					+ Integer.toString(magneticPlaceId) + " (out of "
					+ Integer.toString(sizeOfMagneticPlaceDatabase) + " places)");
		}
	}

	class UpdateOrientAndWiFiScanGUI extends TimerTask {
		public void run() {
			float[] orient = openAIL.inertialSensors.getCurrentAEKFOrient();
			String strongestWiFiNetwork = openAIL.wifiScanner.getStrongestNetwork();
			int WiFiCount = openAIL.wifiScanner.getNetworkCount();
			float foundFreq = openAIL.inertialSensors.getLastDetectedFrequency();
			float stepCount = openAIL.inertialSensors.getDetectedNumberOfSteps();
			float stepDistance = openAIL.inertialSensors.getCovertedStepDistance();
			int currentFloor = openAIL.inertialSensors.getCurrentFloor();
			float estimatedHeight = openAIL.inertialSensors.getEstimatedHeight();

			UpdateMeasurementsInGUI obj = new UpdateMeasurementsInGUI(orient,
					strongestWiFiNetwork, WiFiCount, foundFreq, stepCount,
					stepDistance, currentFloor, estimatedHeight);
			mHandlerOrient.post(obj);
		}

	}

	class UpdateMeasurementsInGUI implements Runnable {
		float[] orient;
		String strongestWiFi;
		int wiFiCount;
		float foundFreq;
		float stepCount, stepDistance;
		int currentFloor;
		float estimatedHeight;

		public UpdateMeasurementsInGUI(float[] _orient, String _strongestWiFi,
				int _wiFiCount, float _foundFreq, float _stepCount,
				float _stepDistance, int _currentFloor, float _estimatedHeight) {
			orient = _orient.clone();
			strongestWiFi = _strongestWiFi;
			wiFiCount = _wiFiCount;
			foundFreq = _foundFreq;
			stepCount = _stepCount;
			stepDistance = _stepDistance;
			currentFloor = _currentFloor;
			estimatedHeight = _estimatedHeight;
		}

		public void run() {
			TextView mTextViewRollX = (TextView) findViewById(R.id.textViewOrient1);
			TextView mTextViewPitchY = (TextView) findViewById(R.id.textViewOrient2);
			TextView mTextViewYawZ = (TextView) findViewById(R.id.textViewOrient3);

			mTextViewRollX.setText("Roll (X): "
					+ String.format("%.2f", orient[0]) + '°');
			mTextViewPitchY.setText("Pitch (Y): "
					+ String.format("%.2f", orient[1]) + '°');
			mTextViewYawZ.setText("Yaw (Z): "
					+ String.format("%.2f", orient[2]) + '°');

			TextView mTextViewNetworkCount = (TextView) findViewById(R.id.textViewWiFi1);
			TextView mTextViewStrongestWiFi = (TextView) findViewById(R.id.textViewWiFi2);

			mTextViewNetworkCount.setText("Number of found networks: "
					+ Integer.toString(wiFiCount));
			mTextViewStrongestWiFi.setText("Strongest WiFi: " + strongestWiFi);

			TextView mTextViewFoundFrequency = (TextView) findViewById(R.id.textViewStepometer1);
			TextView mTextViewStepCounter = (TextView) findViewById(R.id.textViewStepometer2);
			TextView mTextViewStepDistance = (TextView) findViewById(R.id.textViewStepometer3);

			mTextViewFoundFrequency.setText("Found freq: "
					+ String.format("%.2f", foundFreq) + " Hz");
			mTextViewStepCounter.setText("Step counter: "
					+ String.format("%.2f", stepCount));
			mTextViewStepDistance.setText("Distance: "
					+ String.format("%.2f", stepDistance) + " m");

			TextView mTextViewCurrentFloor = (TextView) findViewById(R.id.textViewBarometer1);
			TextView mTextViewEstimatedHeight = (TextView) findViewById(R.id.textViewBarometer2);

			mTextViewCurrentFloor.setText("Floor: "
					+ Integer.toString(currentFloor));
			mTextViewEstimatedHeight.setText("Height: "
					+ String.format("%.2f", estimatedHeight) + " m");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemPreviewMode = menu.add("Preview");
		mItemRecordMode = menu.add("Record");
		mItemRunMode = menu.add("TODO: Run");
		mItemAbout = menu.add("About");
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == mItemPreviewMode) {
			mViewMode = VIEW_MODE_PREVIEW;
			camera.startPreview();
		} else if (item == mItemRecordMode) {
			mViewMode = VIEW_MODE_RECORD;
			camera.stopPreview();
		} else if (item == mItemRunMode) {
			mViewMode = VIEW_MODE_RUN;
			camera.stopPreview();
		} else if (item == mItemAbout) {
			camera.stopPreview();

			int duration = Toast.LENGTH_LONG;
			Toast.makeText(
					getApplicationContext(),
					"Project developed under the ''Diamond Grant'' program\r\nAuthor: Michal Nowicki\r\nmichal.nowicki@put.poznan.pl",
					duration).show();
		}

		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback);

		camera = Camera.open();
		preview.setCamera(camera);
		camera.startPreview();

	}

	@Override
	protected void onPause() {
		if (camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();
	}

}
