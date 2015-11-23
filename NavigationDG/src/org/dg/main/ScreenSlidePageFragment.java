package org.dg.main;

import java.util.ArrayList;
import java.util.List;

import org.dg.camera.CameraSaver;
import org.dg.camera.Preview;
import org.dg.main.MainActivity.UpdateGraph;
import org.dg.main.MainActivity.UpdateWiFiSRecognitionGUI;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy
 * title indicating the page number, along with some dummy text.
 * 
 * <p>
 * This class is used by the {@link CardFlipActivity} and
 * {@link ScreenSlideActivity} samples.
 * </p>
 */
public class ScreenSlidePageFragment extends Fragment {
    private final String TAG = "ScreenSlidePageFragment";
    
	/**
	 * The argument key for the page number this fragment represents.
	 */
	public static final String ARG_PAGE = "page";
//	public static final String DEVICE_ORIENTATION = "deviceOrientation";

	/**
	 * The fragment's page number, which is set to the argument value for
	 * {@link #ARG_PAGE}.
	 */
	private int mPageNumber;
	
	/**
	 * The orientation of device, which is set to the argument value for
	 * {@link #DEVICE_ORIENTATION}.
	 * 
	 */
//	private int mDeviceOrientation;

	/**
	 * Handlers to change GUI
	 */
	public Handler mHandlerOrient, mHandlerWiFiRecognition;

	/**
	 * 
	 */
	public Preview preview = null;
	
	/**
	 * 
	 */
	LocalizationView localizationView;

	Camera camera = null;

	/**
	 * Factory method for this fragment class. Constructs a new fragment for the
	 * given page number.
	 */
	public static ScreenSlidePageFragment create(int pageNumber) {//, int deviceOrientation
		ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_PAGE, pageNumber);
		//args.putInt(DEVICE_ORIENTATION, deviceOrientation);
		fragment.setArguments(args);
		return fragment;
	}

	public ScreenSlidePageFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPageNumber = getArguments().getInt(ARG_PAGE);
	//mDeviceOrientation = getArguments().getInt(DEVICE_ORIENTATION);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout containing a title and body text.
		final ViewGroup rootView;
		if (mPageNumber == 0) {
			Log.d("TEST", "Created camera view");
			
	    	Log.d(TAG, String.format("mPageNumber = %d", mPageNumber));
	    	
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page0, container, false);
			
//			Camera.Parameters parameters = camera.getParameters();
//			parameters.setPreviewSize(640, 480);
//			camera.setParameters(parameters);
			
			// Camera preview stuff
			SurfaceView surfView = (SurfaceView)rootView.findViewById(R.id.SurfaceView01);
			preview = new Preview(surfView);
			
//			android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
//		    android.hardware.Camera.getCameraInfo(0, info);
//		    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
			
			preview.setCamera(camera);
			
			preview.measure(surfView.getWidth(), surfView.getHeight());
			
			preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT));

//			((FrameLayout) rootView.findViewById(R.id.previewFrameLayout)).addView(preview);
			preview.setKeepScreenOn(true);

		} else if (mPageNumber == 1) {
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page1, container, false);

			// 1. Take picute
			initButtonTakePicture(rootView, R.id.buttonMainView1);

			// 2. Start presenting orientation
			initButtonStartOrientation(rootView, R.id.buttonMainView2,
					R.id.buttonMainView3);

			// 3. Record inertial sensors
			initButtonRecordinertialSensors(rootView, R.id.buttonMainView3,
					R.id.buttonMainView2);

			// 4. Do a single WiFi scan
			initButtonRecordSingleWiFiScan(rootView, R.id.buttonMainView4);

			// 5. Record continuous WiFi scans
			initButtonRecordContinuousWiFiScans(rootView, R.id.buttonMainView5,
					R.id.buttonMainView4);

			// 6. Add WiFi scan to recognition list
			initButtonAddWiFiScanToRecognition(rootView, R.id.buttonMainView6);

			// 7. Run stepometer
			initButtonRunStepometer(rootView, R.id.buttonMainView7);

			// Side View 1
			initButtonStartFloorDetection(rootView, R.id.buttonSideView1);

			// Side View 2
			initButtonStartGraphOnline(rootView, R.id.buttonSideView2);

			// Side View 3
			initButtonStartGraphTestFromFile(rootView, R.id.buttonSideView3);

			// Side View 4 - Add magnetic place to recognition
			initButtonAddMagneticPlaceToRecognition(rootView,
					R.id.buttonSideView4);

			// Side View 5 - Run/Stop complementary filter
			initButtonStartStopComplementaryFilter(rootView,
					R.id.buttonSideView5);
			
			// Side View 6 - Process orientation estimation data from file
			initButtonStartOrientationEstimationFromFile(rootView, R.id.buttonSideView6);
			
			// Side View 7 - Process Visual Place Recognition
			initButtonVisualPlaceRecognition(rootView, R.id.buttonSideView7);
			
			// Save WiFi Map
			initButtonSaveMapPoint(rootView, R.id.buttonSaveMapPoint);
			
			// Save VPR
			initButtonSaveVPR(rootView, R.id.buttonSaveVPR);
			

		} else if (mPageNumber == 2 ) {
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page2, container, false);
		} else if (mPageNumber == 3 || true) {
			Log.d("TEST", "Created localization view");
			
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page_visualization, container, false);
			
			localizationView = (LocalizationView) rootView.findViewById(R.id.SurfaceViewLocalization);
			
//            // Add some data for testing
//			List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
//			
//            Pair<Double, Double> x = new Pair<Double, Double>(10.0, 10.0);
//            wifiScanLocations.add(x);
//            
////            
//            x = new Pair<Double, Double>(-10.0, -10.0);
//            wifiScanLocations.add(x);
//            
//            x = new Pair<Double, Double>(10.0, -10.0);
//            wifiScanLocations.add(x);
//            
//            x = new Pair<Double, Double>(-10.0, 10.0);
//            wifiScanLocations.add(x);
//            
//			localizationView.setWiFiScanLocations(wifiScanLocations);
//			
//			List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();
//			
//			x = new Pair<Double, Double>(0.0, 0.0);
//	        userLocations.add(x);
//	        x = new Pair<Double, Double>(0.5, 0.5);
//	        userLocations.add(x);
//	        x = new Pair<Double, Double>(-0.5, 0.5);
//	        userLocations.add(x);
//	            
//			localizationView.setUserLocations(userLocations);
		}


		// Create those handlers
		mHandlerOrient = new Handler();
		mHandlerWiFiRecognition = new Handler();

		return rootView;
	}

	private void initButtonTakePicture(final ViewGroup rootView, int id) {
		Button buttonTakePicture = (Button) rootView.findViewById(id);
		buttonTakePicture.setText("Take picture");
		buttonTakePicture.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSomeClick(v, "Take picture");
			}
		});
	}

	/**
	 * @param rootView
	 */
	private void initButtonStartOrientation(final ViewGroup rootView,
			final int id, final int idToBlock) {
		Button buttonStartOrientation = (Button) rootView.findViewById(id);
		buttonStartOrientation.setText("Run inertial sensors");
		buttonStartOrientation.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button inertialSensors = (Button) rootView
						.findViewById(idToBlock);
				Button buttonStartOrientation = (Button) rootView
						.findViewById(id);
				if (inertialSensors.isEnabled()) {
					inertialSensors.setEnabled(false);
					buttonStartOrientation.setText("Stop inertial sensors");
					onSomeClick(v, "Run inertial sensors");
				} else {
					inertialSensors.setEnabled(true);
					buttonStartOrientation.setText("Run inertial sensors");
					onSomeClick(v, "Stop inertial sensors");
				}

			}
		});
	}

	/**
	 * 
	 */
	private void initButtonRecordinertialSensors(final ViewGroup rootView,
			final int id, final int idToBlock) {
		Button buttonRecordinertialSensors = (Button) rootView.findViewById(id);
		buttonRecordinertialSensors.setText("Record inertial sensors");

		buttonRecordinertialSensors.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button buttonRecordinertialSensors = (Button) rootView
						.findViewById(id);
				Button buttonStartOrientation = (Button) rootView
						.findViewById(idToBlock);
				if (buttonStartOrientation.isEnabled()) {
					buttonStartOrientation.setEnabled(false);
					buttonRecordinertialSensors
							.setText("Stop record inertial sensors");
					onSomeClick(v, "Start record inertial sensors");

				} else {
					buttonStartOrientation.setEnabled(true);
					buttonRecordinertialSensors
							.setText("Record inertial sensors");
					onSomeClick(v, "Stop record inertial sensors");
				}

			}
		});
	}

	private void initButtonRecordSingleWiFiScan(final ViewGroup rootView, int id) {
		Button buttonRecordSingleWiFiScan = (Button) rootView.findViewById(id);
		buttonRecordSingleWiFiScan.setText("Do a single WiFi scan");
		buttonRecordSingleWiFiScan.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSomeClick(v, "Do a single WiFi scan");
			}
		});
	}

	private void initButtonRecordContinuousWiFiScans(final ViewGroup rootView,
			final int id, final int idToBlock) {
		Button buttonRecordContinuousWiFiScans = (Button) rootView
				.findViewById(id);
		buttonRecordContinuousWiFiScans.setText("Start WiFi scans");
		buttonRecordContinuousWiFiScans
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Button buttonRecordContinuousWiFiScans = (Button) rootView
								.findViewById(id);
						Button buttonRecordSingleWiFiScan = (Button) rootView
								.findViewById(idToBlock);

						if (buttonRecordContinuousWiFiScans.getText()
								.toString().equals("Stop WiFi scans")) {
							buttonRecordContinuousWiFiScans
									.setText("Start WiFi scans");
							buttonRecordSingleWiFiScan.setEnabled(true);
							onSomeClick(v, "Stop WiFi scans");

						} else {
							buttonRecordContinuousWiFiScans
									.setText("Stop WiFi scans");
							buttonRecordSingleWiFiScan.setEnabled(false);
							onSomeClick(v, "Start WiFi scans");
						}
					}
				});
	}

	/**
	 * 
	 */
	private void initButtonRunStepometer(final ViewGroup rootView, final int id) {
		Button buttonRunStepometer = (Button) rootView.findViewById(id);
		buttonRunStepometer.setText("Run stepometer");
		buttonRunStepometer.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button buttonRunStepometer = (Button) rootView.findViewById(id);
				if (buttonRunStepometer.getText().toString()
						.equals("Stop stepometer")) {
					buttonRunStepometer.setText("Start stepometer");
					onSomeClick(v, "Stop stepometer");
				} else {
					buttonRunStepometer.setText("Stop stepometer");
					onSomeClick(v, "Start stepometer");
				}

			}
		});
	}

	/**
	 * 
	 */
	private void initButtonAddWiFiScanToRecognition(final ViewGroup rootView,
			int id) {
		Button buttonAddWiFiScanToRecognition = (Button) rootView
				.findViewById(id);
		buttonAddWiFiScanToRecognition.setText("Add WiFi to recognition");
		buttonAddWiFiScanToRecognition
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						onSomeClick(v, "Add WiFi to recognition");
					}
				});
	}

	/**
	 * 
	 */
	private void initButtonStartFloorDetection(final ViewGroup rootView,
			final int id) {
		Button buttonStartFloorDetection = (Button) rootView.findViewById(id);
		buttonStartFloorDetection.setText("Start barometer");
		buttonStartFloorDetection.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Button buttonStartFloorDetection = (Button) rootView
						.findViewById(id);
				if (buttonStartFloorDetection.getText().toString()
						.equals("Stop barometer")) {
					buttonStartFloorDetection.setText("Start barometer");
					onSomeClick(v, "Stop barometer");
				} else {
					buttonStartFloorDetection.setText("Stop barometer");
					onSomeClick(v, "Start barometer");
				}

			}
		});
	}

	/**
	 * 
	 */
	private void initButtonStartGraphOnline(final ViewGroup rootView,
			final int id) {
		Button buttonStartGraphOnline = (Button) rootView.findViewById(id);
		buttonStartGraphOnline.setText("Start graph");
		buttonStartGraphOnline.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				{
					Button buttonStartGraphOnline = (Button) rootView
							.findViewById(id);
					if (buttonStartGraphOnline.getText().toString()
							.equals("Start graph")) {
						buttonStartGraphOnline.setText("Optimize graph");
						onSomeClick(v, "Start graph");
					} else {
						buttonStartGraphOnline.setText("Start graph");
						onSomeClick(v, "Optimize graph");
					}

				}
			}
		});
	}

	/**
	 * 
	 */
	private void initButtonStartGraphTestFromFile(final ViewGroup rootView,
			final int id) {
		Button buttonStartGraphTestFromFile = (Button) rootView
				.findViewById(id);
		buttonStartGraphTestFromFile.setText("Graph from file");
		buttonStartGraphTestFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				{
					onSomeClick(v, "Graph from file");

				}
			}
		});
	}

	/**
	 * 
	 */
	private void initButtonAddMagneticPlaceToRecognition(
			final ViewGroup rootView, final int id) {
		Button buttonAddMagneticPlaceToRecognition = (Button) rootView
				.findViewById(id);
		buttonAddMagneticPlaceToRecognition
				.setText("Add magnetic place to recognition");
		buttonAddMagneticPlaceToRecognition
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						onSomeClick(v, "Add magnetic place to recognition");
					}
				});
	}

	/**
	 * 
	 */
	private void initButtonStartStopComplementaryFilter(
			final ViewGroup rootView, final int id) {
		Button buttonStartStopComplementaryFilter = (Button) rootView
				.findViewById(id);
		buttonStartStopComplementaryFilter
				.setText("Start complementary filter");
		buttonStartStopComplementaryFilter
				.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						Button buttonStartStopComplementaryFilter = (Button) rootView
								.findViewById(id);
						if (buttonStartStopComplementaryFilter.getText()
								.toString()
								.equals("Start complementary filter")) {
							buttonStartStopComplementaryFilter
									.setText("Stop complementary filter");
							onSomeClick(v, "Start complementary filter");
						} else {
							buttonStartStopComplementaryFilter
									.setText("Start complementary filter");
							onSomeClick(v, "Stop complementary filter");
						}
					}
				});
	}
	
	/**
	 * 
	 */
	// Side View 6 - Process orientation estimation data from file
	private void initButtonStartOrientationEstimationFromFile(
			final ViewGroup rootView, int id) {
		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Orient file test");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSomeClick(v, "Orient file test");
			}
		});
	}
	
	/**
	 * 
	 */
	// Side View 7 - Process Visual Place Recognition
	private void initButtonVisualPlaceRecognition(
			final ViewGroup rootView, int id) {
		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Visual Place Recognition");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onSomeClick(v, "Visual Place Recognition");
			}
		});
	}
	
	// Save map point
	private void initButtonSaveMapPoint(
			final ViewGroup rootView, int id) {
		EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
		EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
		EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
		EditText idText = (EditText) rootView.findViewById(R.id.editTextMapPosID);
		x.setText("0.0");
		y.setText("0.0");
		z.setText("0.0");
		idText.setText("10000");
		
		EditText mapName = (EditText) rootView.findViewById(R.id.editTextMapName);
		mapName.setText("newMap");
		
		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Save map point");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
				EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
				EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
				EditText mapName = (EditText) rootView.findViewById(R.id.editTextMapName);
				EditText id = (EditText) rootView.findViewById(R.id.editTextMapPosID);
				onSomeClick(v, "Save map point :&" + mapName.getText() + "&" + id.getText() + "&" + x.getText() + "&" + y.getText() + "&" + z.getText());
				
				int idNum = Integer.parseInt(id.getText().toString());
				id.setText(String.format("%d", idNum+1)); 
			}
		});
	}
	
	// Save VPR image
	private void initButtonSaveVPR(final ViewGroup rootView, int id)
	{
		EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
		EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
		EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
		
		x.setText("0.0");
		y.setText("0.0");
		z.setText("0.0");
		
		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Save VPR place");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
				EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
				EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
				onSomeClick(v, "Save VPR place: " + x.getText() + " " + y.getText() + " " + z.getText());
			}
		});
	}

	/**
	 * Returns the page number represented by this fragment object.
	 */
	public int getPageNumber() {
		return mPageNumber;
	}

	/*
	 * Returns the localizationView for updating purposes
	 */
	public LocalizationView getLocalizationView() {
		return localizationView;
	}
	
	// ...
	// Define the listener of the interface type
	// listener is the activity itself
	private OnItemSelectedListener listener;

	// Define the events that the fragment will use to communicate
	public interface OnItemSelectedListener {
		public void onButtonPressed(String link);
	}

	// Store the listener (activity) that will have events fired once the
	// fragment is attached
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnItemSelectedListener) {
			listener = (OnItemSelectedListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement MyListFragment.OnItemSelectedListener");
		}
	}

	// Now we can fire the event when the user selects something in the fragment
	public void onSomeClick(View v, String value) {
		listener.onButtonPressed(value);
	}

	public void updateGUIData(float[] _orient, float[] _compOrient,
			String _strongestWiFi, int _wiFiCount, float _foundFreq,
			float _stepCount, float _stepDistance, int _currentFloor,
			float _estimatedHeight, float _accVariance, int _deviceOrientation) {
		UpdateMeasurementsInGUI obj = new UpdateMeasurementsInGUI(_orient,
				_compOrient, _strongestWiFi, _wiFiCount, _foundFreq,
				_stepCount, _stepDistance, _currentFloor, _estimatedHeight, _accVariance, _deviceOrientation);
		mHandlerOrient.post(obj);
	}

	public void updateGUIPlaceRecognize(int _placeId, int _sizeOfPlaceDatabase,
			int _magneticPlaceId, int _sizeOfMagneticPlaceDatabase) {
		UpdateWiFiInGUI obj = new UpdateWiFiInGUI(_placeId,
				_sizeOfPlaceDatabase, _magneticPlaceId,
				_sizeOfMagneticPlaceDatabase);
		mHandlerWiFiRecognition.post(obj);
	}

	class UpdateMeasurementsInGUI implements Runnable {
		float[] orient, compOrient;
		String strongestWiFi;
		int wiFiCount;
		float foundFreq;
		float stepCount, stepDistance;
		int currentFloor;
		float estimatedHeight;
		float accVariance;
		int deviceOrientation;

		public UpdateMeasurementsInGUI(float[] _orient, float[] _compOrient,
				String _strongestWiFi, int _wiFiCount, float _foundFreq,
				float _stepCount, float _stepDistance, int _currentFloor,
				float _estimatedHeight, float _accVariance, int _deviceOrientation) {
			orient = _orient.clone();
			compOrient = _compOrient.clone();
			strongestWiFi = _strongestWiFi;
			wiFiCount = _wiFiCount;
			foundFreq = _foundFreq;
			stepCount = _stepCount;
			stepDistance = _stepDistance;
			currentFloor = _currentFloor;
			estimatedHeight = _estimatedHeight;
			accVariance = _accVariance;
			deviceOrientation = _deviceOrientation;
		}

		public void run() {

			if (mPageNumber == 2) {

				// ORIENTATION X, Y, Z
				TextView mTextViewRollX = (TextView) getView().findViewById(
						R.id.textViewOrient1);
				TextView mTextViewPitchY = (TextView) getView().findViewById(
						R.id.textViewOrient2);
				TextView mTextViewYawZ = (TextView) getView().findViewById(
						R.id.textViewOrient3);

				mTextViewRollX.setText("Roll (X): "
						+ String.format("%.2f", orient[0]) + " deg");
				mTextViewPitchY.setText("Pitch (Y): "
						+ String.format("%.2f", orient[1]) + " deg");
				mTextViewYawZ.setText("Yaw (Z): "
						+ String.format("%.2f", orient[2]) + " deg");

				// ORIENTATION COMPLEMENTARY X, Y, Z
				TextView mTextViewCompRollX = (TextView) getView()
						.findViewById(R.id.textViewOrientComp1);
				TextView mTextViewCompPitchY = (TextView) getView()
						.findViewById(R.id.textViewOrientComp2);
				TextView mTextViewCompYawZ = (TextView) getView().findViewById(
						R.id.textViewOrientComp3);

				mTextViewCompRollX.setText("Comp Roll (X): "
						+ String.format("%.2f", compOrient[0]) + " deg");
				mTextViewCompPitchY.setText("Comp Pitch (Y): "
						+ String.format("%.2f", compOrient[1]) + " deg");
				mTextViewCompYawZ.setText("Comp Yaw (Z): "
						+ String.format("%.2f", compOrient[2]) + " deg");

				TextView mTextViewNetworkCount = (TextView) getView()
						.findViewById(R.id.textViewWiFi1);
				TextView mTextViewStrongestWiFi = (TextView) getView()
						.findViewById(R.id.textViewWiFi2);

				mTextViewNetworkCount.setText("Number of found networks: "
						+ Integer.toString(wiFiCount));
				mTextViewStrongestWiFi.setText("Strongest WiFi: "
						+ strongestWiFi);

				TextView mTextViewFoundFrequency = (TextView) getView()
						.findViewById(R.id.textViewStepometer1);
				TextView mTextViewStepCounter = (TextView) getView()
						.findViewById(R.id.textViewStepometer2);
				TextView mTextViewStepDistance = (TextView) getView()
						.findViewById(R.id.textViewStepometer3);
				TextView mTextViewAccVariance = (TextView) getView()
						.findViewById(R.id.textViewStepometer4);

				mTextViewFoundFrequency.setText("Found freq: "
						+ String.format("%.2f", foundFreq) + " Hz");
				mTextViewStepCounter.setText("Step counter: "
						+ String.format("%.2f", stepCount));
				mTextViewStepDistance.setText("Distance: "
						+ String.format("%.2f", stepDistance) + " m");
				mTextViewAccVariance.setText("Acc var: "
						+ String.format("%.2f", accVariance) );

				TextView mTextViewCurrentFloor = (TextView) getView()
						.findViewById(R.id.textViewBarometer1);
				TextView mTextViewEstimatedHeight = (TextView) getView()
						.findViewById(R.id.textViewBarometer2);
				TextView mTextViewDeviceOrientation = (TextView) getView()
						.findViewById(R.id.textViewDeviceOrientation);
				
				mTextViewCurrentFloor.setText("Floor: "
						+ Integer.toString(currentFloor));
				mTextViewEstimatedHeight.setText("Height: "
						+ String.format("%.2f", estimatedHeight) + " m");
				
				if ( deviceOrientation == 0)
					mTextViewDeviceOrientation.setText("devOrient: VERT");
				else if ( deviceOrientation == 1)
					mTextViewDeviceOrientation.setText("devOrient: HOR LEFT");
				else if ( deviceOrientation == 2)
					mTextViewDeviceOrientation.setText("devOrient: HOR RIGHT");
				else if ( deviceOrientation == 3)
					mTextViewDeviceOrientation.setText("devOrient: UNKNOWN");
			}
		}
	}
	
	public void setCamera(Camera icamera){
//    	Log.d(TAG, String.format("setCamera, mPageNumber = %d", mPageNumber));
    	
    	camera = icamera;
    	if(preview != null){
    		preview.setCamera(camera);
    	}
    	
//		Camera.Parameters parameters = camera.getParameters();
//		parameters.setPreviewSize(640, 480);
//		camera.setParameters(parameters);
	}

	class UpdateWiFiInGUI implements Runnable {
		int placeId, sizeOfPlaceDatabase;
		int magneticPlaceId, sizeOfMagneticPlaceDatabase;

		public UpdateWiFiInGUI(int _placeId, int _sizeOfPlaceDatabase,
				int _magneticPlaceId, int _sizeOfMagneticDatabase) {
			placeId = _placeId;
			sizeOfPlaceDatabase = _sizeOfPlaceDatabase;
			magneticPlaceId = _magneticPlaceId;
			sizeOfMagneticPlaceDatabase = _sizeOfMagneticDatabase;
		}

		public void run() {
			if (mPageNumber == 2) {

				TextView mTextViewRecognizedPlace = (TextView) getView()
						.findViewById(R.id.textViewWiFi3);

				mTextViewRecognizedPlace.setText("Recognized place id: "
						+ Integer.toString(placeId) + " (out of "
						+ Integer.toString(sizeOfPlaceDatabase) + " places)");

				TextView mTextViewMagneticRecognizedPlace = (TextView) getView()
						.findViewById(R.id.textViewMagnetic);

				mTextViewMagneticRecognizedPlace
						.setText("Recognized mag place id: "
								+ Integer.toString(magneticPlaceId)
								+ " (out of "
								+ Integer.toString(sizeOfMagneticPlaceDatabase)
								+ " places)");
			}
		}
	}
}
