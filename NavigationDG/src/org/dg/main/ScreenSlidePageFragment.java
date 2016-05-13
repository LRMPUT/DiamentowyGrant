// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.dg.main;

import java.util.ArrayList;
import java.util.List;

import org.dg.camera.CameraSaver;
import org.dg.camera.Preview;

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
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
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
	// public static final String DEVICE_ORIENTATION = "deviceOrientation";

	/**
	 * f The fragment's page number, which is set to the argument value for
	 * {@link #ARG_PAGE}.
	 */
	private int mPageNumber;

	/**
	 * The orientation of device, which is set to the argument value for
	 * {@link #DEVICE_ORIENTATION}.
	 * 
	 */
	// private int mDeviceOrientation;

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
	public static ScreenSlidePageFragment create(int pageNumber) {// , int
																	// deviceOrientation
		ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_PAGE, pageNumber);
		// args.putInt(DEVICE_ORIENTATION, deviceOrientation);
		fragment.setArguments(args);
		return fragment;
	}

	public ScreenSlidePageFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPageNumber = getArguments().getInt(ARG_PAGE);
		// mDeviceOrientation = getArguments().getInt(DEVICE_ORIENTATION);
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

			// Camera.Parameters parameters = camera.getParameters();
			// parameters.setPreviewSize(640, 480);
			// camera.setParameters(parameters);

			// Camera preview stuff
			SurfaceView surfView = (SurfaceView) rootView
					.findViewById(R.id.SurfaceView01);
			preview = new Preview(surfView);

			// If you you touch for longer time -> do the QR code scanning
			surfView.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					Log.d("Camera::preview", "onLongClick");

					onSomeClick(v, "Decode QR");
					return false;

				}
			});
			
			// If you you touch for short time -> take image
			surfView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d("Camera::preview", "onClick");

					onSomeClick(v, "Take picture");
				}
			});

			// android.hardware.Camera.CameraInfo info = new
			// android.hardware.Camera.CameraInfo();
			// android.hardware.Camera.getCameraInfo(0, info);
			// int rotation =
			// activity.getWindowManager().getDefaultDisplay().getRotation();

			preview.setCamera(camera);

			preview.measure(surfView.getWidth(), surfView.getHeight());

			preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT));

			// ((FrameLayout)
			// rootView.findViewById(R.id.previewFrameLayout)).addView(preview);
			preview.setKeepScreenOn(true);

		} else if (mPageNumber == 1) {
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page1, container, false);
			
			
			// Start/stop localization
			initButtonStartLocalization(rootView, R.id.buttonStartLocalization);

			// Barometer on/off
			initSwitchBarometer(rootView, R.id.switchBarometer);

			// Inertial sensors on/off
			initSwitchInertialSensors(rootView, R.id.switchInertialSensors);

			// Stepometer on/off
			initSwitchStepometer(rootView, R.id.switchStepometer);

			// WiFi on/off
			initSwitchWiFi(rootView, R.id.switchWiFi);

			/*
			 *  --------------------
			 */
			
			// Optimize graph from file
			initButtonStartGraphTestFromFile(rootView,
					R.id.buttonOptimizeGraphFromFile);

			// Process orientation estimation data from file
			initButtonStartOrientationEstimationFromFile(rootView,
					R.id.buttonOrientationTestFromFile);

			// Save WiFi Map
			initButtonSaveMapPoint(rootView, R.id.buttonSaveMapPoint);

			// Save VPR
			initButtonSaveVPR(rootView, R.id.buttonSaveVPR);

			// Record All
			initButtonRecordAll(rootView, R.id.buttonRecordAll);

			// Take picute
//			initButtonTakePicture(rootView, R.id.buttonTakePicture);

			// Record inertial sensors
//			initButtonRecordinertialSensors(rootView, R.id.buttonRecordInertial);
			
			// Clear map
			initButtonClearNewMap(rootView, R.id.buttonClearNewMap);
			
			// Playback
			initButtonPlayback(rootView, R.id.buttonPlayback);
			
			// Test new functions
			initButtonTest(rootView, R.id.buttonTest);

			/**
			 * OLD: 
			 * 
			 * onSomeClick(v, "Add magnetic place to recognition");
			 * 
			 * onSomeClick(v, "Start complementary filter"); onSomeClick(v,
			 * "Stop complementary filter");
			 * 
			 * onSomeClick(v, "Visual Place Recognition");
			 * 
			 * onSomeClick(v, "Decode QR");
			 * 
			 * onSomeClick(v, "Do a single WiFi scan");
			 * 
			 * onSomeClick(v, "Add WiFi to recognition");
			 * 
			 * onSomeClick(v, "Take picture");
			 * 
			 * onSomeClick(v, "Start record inertial sensors");
			 * onSomeClick(v, "Stop record inertial sensors");
			 */

		} else if (mPageNumber == 2) {
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page2, container, false);
		} else if (mPageNumber == 3 || true) {
			Log.d("TEST", "Created localization view");

			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page_visualization,
					container, false);

			localizationView = (LocalizationView) rootView
					.findViewById(R.id.SurfaceViewLocalization);

			// // Add some data for testing
			// List<Pair<Double, Double>> wifiScanLocations = new
			// ArrayList<Pair<Double, Double>>();
			//
			// Pair<Double, Double> x = new Pair<Double, Double>(10.0, 10.0);
			// wifiScanLocations.add(x);
			//
			// //
			// x = new Pair<Double, Double>(-10.0, -10.0);
			// wifiScanLocations.add(x);
			//
			// x = new Pair<Double, Double>(10.0, -10.0);
			// wifiScanLocations.add(x);
			//
			// x = new Pair<Double, Double>(-10.0, 10.0);
			// wifiScanLocations.add(x);
			//
			// localizationView.setWiFiScanLocations(wifiScanLocations);
			//
			// List<Pair<Double, Double>> userLocations = new
			// ArrayList<Pair<Double, Double>>();
			//
			// x = new Pair<Double, Double>(0.0, 0.0);
			// userLocations.add(x);
			// x = new Pair<Double, Double>(0.5, 0.5);
			// userLocations.add(x);
			// x = new Pair<Double, Double>(-0.5, 0.5);
			// userLocations.add(x);
			//
			// localizationView.setUserLocations(userLocations);
		}

		// Create those handlers
		mHandlerOrient = new Handler();
		mHandlerWiFiRecognition = new Handler();

		return rootView;
	}


	/**
	 * @param rootView
	 */
	private void initSwitchInertialSensors(final ViewGroup rootView,
			final int id) {

		Switch switchInterialSensors = (Switch) rootView.findViewById(id);
		switchInterialSensors.setEnabled(false);
		switchInterialSensors.setChecked(false);
		switchInterialSensors.setText("Inertial sensors");
		switchInterialSensors
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked)
							onSomeClick(buttonView, "Run inertial sensors");
						else
							onSomeClick(buttonView, "Stop inertial sensors");
					}
				});
	}

	
	
	/**
	 * 
	 */
	private void initButtonClearNewMap(final ViewGroup rootView,
			final int id) {
		Button buttonClearNewMap = (Button) rootView.findViewById(id);
		buttonClearNewMap.setText("Clear new map");

		buttonClearNewMap.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					EditText mapName = (EditText) rootView
						.findViewById(R.id.editTextMapName);
					onSomeClick(v, "Clear new map &"+mapName.getText());
			}
		});
	}
	
	/**
	 * 
	 */
	private void initButtonPlayback(final ViewGroup rootView,
			final int id) {
		Button buttonPlayback = (Button) rootView.findViewById(id);
		buttonPlayback.setText("Playback");

		buttonPlayback.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					onSomeClick(v, "Playback");
			}
		});
	}
	
	/**
	 * 
	 */
	private void initButtonTest(final ViewGroup rootView,
			final int id) {
		Button buttonTest = (Button) rootView.findViewById(id);
		buttonTest.setText("Test sth new!");

		buttonTest.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					onSomeClick(v, "Test");
			}
		});
	}

	private void initSwitchWiFi(final ViewGroup rootView, final int id) {

		Switch switchWiFi = (Switch) rootView.findViewById(id);
//		switchWiFi.setEnabled(false);
		switchWiFi.setChecked(false);
		switchWiFi.setText("WiFi");
		switchWiFi.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked)
					onSomeClick(buttonView, "Start WiFi scans");
				else
					onSomeClick(buttonView, "Stop WiFi scans");
			}
		});
	}

	/**
	 * 
	 */
	private void initSwitchStepometer(final ViewGroup rootView, final int id) {

		Switch switchStepometer = (Switch) rootView.findViewById(id);
		switchStepometer.setEnabled(false);
		switchStepometer.setChecked(false);
		switchStepometer.setText("Stepometer");
		switchStepometer
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked)
							onSomeClick(buttonView, "Start stepometer");
						else
							onSomeClick(buttonView, "Stop stepometer");
					}
				});
	}

	/**
	 * 
	 */
	private void initSwitchBarometer(final ViewGroup rootView, final int id) {
		Switch switchStartFloorDetection = (Switch) rootView.findViewById(id);
		switchStartFloorDetection.setEnabled(false);
		switchStartFloorDetection.setChecked(false);
		switchStartFloorDetection.setText("Barometer");
		switchStartFloorDetection
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked)
							onSomeClick(buttonView, "Start barometer");
						else
							onSomeClick(buttonView, "Stop barometer");
					}
				});

	}

	/**
	 * 
	 */
	private void initButtonStartLocalization(final ViewGroup rootView,
			final int id) {
		Button buttonStartGraphOnline = (Button) rootView.findViewById(id);
		buttonStartGraphOnline.setText("Start Localization");
		buttonStartGraphOnline.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				{
					Button buttonStartGraphOnline = (Button) rootView
							.findViewById(id);

					Switch switchStartFloorDetection = (Switch) rootView
							.findViewById(R.id.switchBarometer);
					Switch switchInterialSensors = (Switch) rootView
							.findViewById(R.id.switchInertialSensors);
					Switch switchStepometer = (Switch) rootView
							.findViewById(R.id.switchStepometer);
//					Switch switchWiFi = (Switch) rootView
//							.findViewById(R.id.switchWiFi);

					if (buttonStartGraphOnline.getText().toString()
							.equals("Start Localization")) {
						buttonStartGraphOnline.setText("Stop Localization");

						switchStartFloorDetection.setEnabled(true);
						switchInterialSensors.setEnabled(true);
						switchStepometer.setEnabled(true);
//						switchWiFi.setEnabled(true);

						onSomeClick(v, "Start graph");
					} else {
						buttonStartGraphOnline.setText("Start Localization");
						onSomeClick(v, "Optimize graph");

						switchStartFloorDetection.setEnabled(false);
						switchInterialSensors.setEnabled(false);
						switchStepometer.setEnabled(false);
//						switchWiFi.setEnabled(false);
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

	// Save map point
	private void initButtonSaveMapPoint(final ViewGroup rootView, int id) {
		EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
		EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
		EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
		EditText idText = (EditText) rootView
				.findViewById(R.id.editTextMapPosID);
		x.setText("0.0");
		y.setText("0.0");
		z.setText("0.0");
		idText.setText("10000");

		EditText mapName = (EditText) rootView
				.findViewById(R.id.editTextMapName);
		mapName.setText("newMap");

		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Save map point");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText x = (EditText) rootView
						.findViewById(R.id.editTextWiFiPosX);
				EditText y = (EditText) rootView
						.findViewById(R.id.editTextWiFiPosY);
				EditText z = (EditText) rootView
						.findViewById(R.id.editTextWiFiPosZ);
				EditText mapName = (EditText) rootView
						.findViewById(R.id.editTextMapName);
				EditText id = (EditText) rootView
						.findViewById(R.id.editTextMapPosID);
				onSomeClick(v, "Save map point :&" + mapName.getText() + "&"
						+ id.getText() + "&" + x.getText() + "&" + y.getText()
						+ "&" + z.getText());

				int idNum = Integer.parseInt(id.getText().toString());
				id.setText(String.format("%d", idNum + 1));
			}
		});
	}

	// Save VPR image
	private void initButtonSaveVPR(final ViewGroup rootView, int id) {
		EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
		EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
		EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);

		x.setText("0.0");
		y.setText("0.0");
		z.setText("0.0");

		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Save image");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText x = (EditText) rootView
						.findViewById(R.id.editTextWiFiPosX);
				EditText y = (EditText) rootView
						.findViewById(R.id.editTextWiFiPosY);
				EditText z = (EditText) rootView
						.findViewById(R.id.editTextWiFiPosZ);
				onSomeClick(v,
						"Save VPR place: " + x.getText() + " " + y.getText()
								+ " " + z.getText());
			}
		});
	}

	// Button record all
	private void initButtonRecordAll(final ViewGroup rootView, final int id) {
		Button buttonRecordAll = (Button) rootView.findViewById(id);
		buttonRecordAll.setText("Start record all");
		buttonRecordAll.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				Button buttonRecordAll = (Button) rootView.findViewById(id);
				if (buttonRecordAll.getText().toString()
						.equals("Start record all")) {
					buttonRecordAll.setText("Stop record all");
				} else {
					buttonRecordAll.setText("Start record all");
				}
				onSomeClick(v, "Record all");
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
			float _estimatedHeight, float _accVariance, int _deviceOrientation,
			float _stepometerAngle, float _gyroVariance) {
		UpdateMeasurementsInGUI obj = new UpdateMeasurementsInGUI(_orient,
				_compOrient, _strongestWiFi, _wiFiCount, _foundFreq,
				_stepCount, _stepDistance, _currentFloor, _estimatedHeight,
				_accVariance, _deviceOrientation, _stepometerAngle,
				_gyroVariance);
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
		float accVariance, gyroVariance, stepometerAngle;
		int deviceOrientation;

		public UpdateMeasurementsInGUI(float[] _orient, float[] _compOrient,
				String _strongestWiFi, int _wiFiCount, float _foundFreq,
				float _stepCount, float _stepDistance, int _currentFloor,
				float _estimatedHeight, float _accVariance,
				int _deviceOrientation, float _stepometerAngle,
				float _gyroVariance) {
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
			stepometerAngle = _stepometerAngle;
			gyroVariance = _gyroVariance;

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
				TextView mTextViewAngle = (TextView) getView().findViewById(
						R.id.textViewStepometer4);
				TextView mTextViewAccVariance = (TextView) getView()
						.findViewById(R.id.textViewStepometer5);
				TextView mTextViewGyroVariance = (TextView) getView()
						.findViewById(R.id.textViewStepometer6);

				mTextViewFoundFrequency.setText("Freq: "
						+ String.format("%.2f", foundFreq) + " Hz");
				mTextViewStepCounter.setText("Steps: "
						+ String.format("%.2f", stepCount));
				mTextViewStepDistance.setText("Dist: "
						+ String.format("%.2f", stepDistance) + " m");
				mTextViewAngle.setText("Raw angle: "
						+ String.format("%.2f", stepometerAngle));
				mTextViewAccVariance.setText("Acc var: "
						+ String.format("%.2f", accVariance));
				mTextViewGyroVariance.setText("Gyro var: "
						+ String.format("%.2f", gyroVariance));

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

				if (deviceOrientation == 0)
					mTextViewDeviceOrientation.setText("devOrient: VERT");
				else if (deviceOrientation == 1)
					mTextViewDeviceOrientation.setText("devOrient: HOR LEFT");
				else if (deviceOrientation == 2)
					mTextViewDeviceOrientation.setText("devOrient: HOR RIGHT");
				else if (deviceOrientation == 3)
					mTextViewDeviceOrientation.setText("devOrient: UNKNOWN");
			}
		}
	}

	public void setCamera(Camera icamera) {
		// Log.d(TAG, String.format("setCamera, mPageNumber = %d",
		// mPageNumber));

		camera = icamera;
		if (preview != null) {
			preview.setCamera(camera);
		}

		// Camera.Parameters parameters = camera.getParameters();
		// parameters.setPreviewSize(640, 480);
		// camera.setParameters(parameters);
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
