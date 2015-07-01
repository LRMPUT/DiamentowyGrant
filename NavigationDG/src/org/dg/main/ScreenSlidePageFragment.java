package org.dg.main;

import org.dg.camera.CameraSaver;
import org.dg.camera.Preview;
import org.dg.main.MainActivity.UpdateGraph;
import org.dg.main.MainActivity.UpdateWiFiSRecognitionGUI;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
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
	/**
	 * The argument key for the page number this fragment represents.
	 */
	public static final String ARG_PAGE = "page";

	/**
	 * The fragment's page number, which is set to the argument value for
	 * {@link #ARG_PAGE}.
	 */
	private int mPageNumber;

	/**
	 * Handlers to change GUI
	 */
	public Handler mHandlerOrient, mHandlerWiFiRecognition;

	/**
	 * 
	 */
	Preview preview;

	/**
	 * Factory method for this fragment class. Constructs a new fragment for the
	 * given page number.
	 */
	public static ScreenSlidePageFragment create(int pageNumber) {
		ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_PAGE, pageNumber);
		fragment.setArguments(args);
		return fragment;
	}

	public ScreenSlidePageFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPageNumber = getArguments().getInt(ARG_PAGE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout containing a title and body text.
		final ViewGroup rootView;
		if (mPageNumber == 0) {
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page0, container, false);
			
			// Camera preview stuff
//			preview = new Preview((SurfaceView) rootView.findViewById(R.id.SurfaceView01));
//
//			preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
//					LayoutParams.FILL_PARENT));
//
//			((FrameLayout) rootView.findViewById(R.id.preview)).addView(preview);
//			preview.setKeepScreenOn(true);

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
			initButtonSaveWiFiMap(rootView, R.id.buttonSaveWiFiMap);
			

		} else if (mPageNumber == 2 || true) {
			rootView = (ViewGroup) inflater.inflate(
					R.layout.fragment_screen_slide_page2, container, false);
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
	
	// Save WiFi Map
	private void initButtonSaveWiFiMap(
			final ViewGroup rootView, int id) {
		EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
		EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
		EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
		
		x.setText("0.0");
		y.setText("0.0");
		z.setText("0.0");
		
		Button buttonStartOrientFromFile = (Button) rootView.findViewById(id);
		buttonStartOrientFromFile.setText("Save WiFi place");
		buttonStartOrientFromFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText x = (EditText) rootView.findViewById(R.id.editTextWiFiPosX);
				EditText y = (EditText) rootView.findViewById(R.id.editTextWiFiPosY);
				EditText z = (EditText) rootView.findViewById(R.id.editTextWiFiPosZ);
				onSomeClick(v, "Save WiFi place: " + x.getText() + " " + y.getText() + " " + z.getText());
			}
		});
	}

	/**
	 * Returns the page number represented by this fragment object.
	 */
	public int getPageNumber() {
		return mPageNumber;
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
			float _estimatedHeight) {
		UpdateMeasurementsInGUI obj = new UpdateMeasurementsInGUI(_orient,
				_compOrient, _strongestWiFi, _wiFiCount, _foundFreq,
				_stepCount, _stepDistance, _currentFloor, _estimatedHeight);
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

		public UpdateMeasurementsInGUI(float[] _orient, float[] _compOrient,
				String _strongestWiFi, int _wiFiCount, float _foundFreq,
				float _stepCount, float _stepDistance, int _currentFloor,
				float _estimatedHeight) {
			orient = _orient.clone();
			compOrient = _compOrient.clone();
			strongestWiFi = _strongestWiFi;
			wiFiCount = _wiFiCount;
			foundFreq = _foundFreq;
			stepCount = _stepCount;
			stepDistance = _stepDistance;
			currentFloor = _currentFloor;
			estimatedHeight = _estimatedHeight;
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
						+ String.format("%.2f", orient[0]) + '°');
				mTextViewPitchY.setText("Pitch (Y): "
						+ String.format("%.2f", orient[1]) + '°');
				mTextViewYawZ.setText("Yaw (Z): "
						+ String.format("%.2f", orient[2]) + '°');

				// ORIENTATION COMPLEMENTARY X, Y, Z
				TextView mTextViewCompRollX = (TextView) getView()
						.findViewById(R.id.textViewOrientComp1);
				TextView mTextViewCompPitchY = (TextView) getView()
						.findViewById(R.id.textViewOrientComp2);
				TextView mTextViewCompYawZ = (TextView) getView().findViewById(
						R.id.textViewOrientComp3);

				mTextViewCompRollX.setText("Comp Roll (X): "
						+ String.format("%.2f", compOrient[0]) + '°');
				mTextViewCompPitchY.setText("Comp Pitch (Y): "
						+ String.format("%.2f", compOrient[1]) + '°');
				mTextViewCompYawZ.setText("Comp Yaw (Z): "
						+ String.format("%.2f", compOrient[2]) + '°');

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

				mTextViewFoundFrequency.setText("Found freq: "
						+ String.format("%.2f", foundFreq) + " Hz");
				mTextViewStepCounter.setText("Step counter: "
						+ String.format("%.2f", stepCount));
				mTextViewStepDistance.setText("Distance: "
						+ String.format("%.2f", stepDistance) + " m");

				TextView mTextViewCurrentFloor = (TextView) getView()
						.findViewById(R.id.textViewBarometer1);
				TextView mTextViewEstimatedHeight = (TextView) getView()
						.findViewById(R.id.textViewBarometer2);

				mTextViewCurrentFloor.setText("Floor: "
						+ Integer.toString(currentFloor));
				mTextViewEstimatedHeight.setText("Height: "
						+ String.format("%.2f", estimatedHeight) + " m");
			}
		}
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
