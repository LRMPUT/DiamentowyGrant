package org.dg.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.highgui.Highgui;
import org.dg.camera.DetectDescript;
import org.dg.camera.Preview;
import org.dg.camera.VisualOdometry;
import org.dg.main.R;
import org.dg.tcp.ConnectionIPPort;







import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String    TAG = "Main::Activity";

    private static final int       VIEW_MODE_RGBA     = 0;
    private static final int       VIEW_MODE_GRAY     = 1;
    private static final int       VIEW_MODE_CANNY    = 2;
    private static final int       VIEW_MODE_FEATURES = 5;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewCanny;
    private MenuItem               mItemPreviewFeatures;

    private CameraBridgeViewBase   mOpenCvCameraView;
    
    
    Preview preview;
	Button buttonClick;
	Camera camera;
	String fileName;
    Activity act;
	Context ctx;
	public long startTime = 0, startTimeGlobal = 0;
	
	public static org.dg.tcp.TCPClient mTcpClient;
	boolean connected = false;
	public int synchronizationTime = 0;
	
	public class connectionTCP extends AsyncTask<ConnectionIPPort,String,Void> {
	   	 
		@Override
		protected Void doInBackground(ConnectionIPPort... adres) {

			
			mTcpClient = new org.dg.tcp.TCPClient(adres[0],new org.dg.tcp.TCPClient.OnMessageReceived() {
				@Override
				// here the messageReceived method is implemented
				public void messageReceived(String message) {
					// this method calls the onProgressUpdate
					publishProgress(message);
				}
			});
			mTcpClient.run();
			
			return null;
		}

		@Override
		protected void onProgressUpdate(String... msg) {
  	      	
			long timeTaken = (System.nanoTime() - startTimeGlobal) - startTime;
			
			Log.e("TCP", "Progress: " + msg[0]);

			// Connection is established properly
			if (connected == false) {
				// We start something
				connected = true;
				startTime =0 ;
				startTimeGlobal =  System.nanoTime();
			}

			// Message -> show as toast
			int duration = Toast.LENGTH_LONG;
			Toast.makeText(getApplicationContext(), msg[0], duration).show();

			if (msg[0].contains("SYN")) {
				String[] separated = msg[0].split(" ");
				long compTime =  Long.parseLong(separated[1].trim());
				Log.d("TCP", "timeTaken: " + timeTaken + "\n");
				Log.d("TCP", "Computer time: " + compTime + "\n");
				Log.d("TCP", "Time for ping/pong in ns: " + (timeTaken-compTime) + "\n");
				Log.d("TCP", "Time for ping/pong in ms: " + (timeTaken-compTime)/1000000 + "\n");
				
				if ((timeTaken-compTime)/1000000 <= 3)
				{
					mTcpClient.sendMessage("END " + (timeTaken-compTime)/2 + "\n");
				}
			}

			if (msg[0].contains("START")) {
				startTime = System.nanoTime() - startTimeGlobal;
				Log.d("TCP", "Sending: " + "SYN " + startTime + "\n");
				mTcpClient.sendMessage("SYN " + startTime + "\n");
			}

			if (msg[0].contains("X")) {
				// We end something
				connected = false;
			}

		}
	}

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Loading libraries
                    // System.loadLibrary("imu");
                    //System.loadLibrary("nonfree");
                    // System.loadLibrary("scale_estimation");
                    // System.loadLibrary("visual_odometry");

                    Toast.makeText(MainActivity.this,"Loaded all libraries", 5000).show();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	// Loading OpenCV
    	//if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_7, this, mLoaderCallback))
        //{
    	//	Log.e(TAG, "Cannot connect to OpenCV Manager");
        //}
    	
    	if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_7, this, mLoaderCallback))
        {
    		Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
    	
    	
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
       
  
        

		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main_surface_view);
		
		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.preview)).addView(preview);
		preview.setKeepScreenOn(true);
		
		buttonClick = (Button) findViewById(R.id.buttonClick);
		
		buttonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				 	camera.takePicture(shutterCallback, rawCallback, jpegCallback);
			}
		});
		
		
		Button buttonExp = (Button) findViewById(R.id.buttonExp);
		
		buttonExp.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				/*File root = Environment.getExternalStorageDirectory();
				File file = new File(root, "_exp/desk/0001.png");
				
				Toast.makeText(MainActivity.this,"xx", 5000).show();
				
				Mat img = Highgui.imread(file.getAbsolutePath());
				Toast.makeText(MainActivity.this,"Height: " + img.cols() + " Width: " + img.rows(), 5000).show();
				*/
				DetectDescript det = new DetectDescript();
				
				(new Thread(det)).start();
				//ExtendedKalmanFilter EKF = new ExtendedKalmanFilter();
				
			}
		});
		
		Button buttonTimeSync = (Button) findViewById(R.id.ButtonTimeSync);
		buttonTimeSync.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				if (connected == false) {
					
					InetAddress selected_ip;
					try {
//						selected_ip = InetAddress.getByName("192.168.1.132");
						selected_ip = InetAddress.getByName("192.168.0.11");
						ConnectionIPPort adres = new ConnectionIPPort(selected_ip, 3000);
						
						//selected_ip = InetAddress.getByName("192.168.2.222");
						//IP_PORT adres = new IP_PORT(selected_ip, 27000);

						new connectionTCP().execute(adres);
						
						Log.e("TCP activity", "Connecting to : " + selected_ip.toString()
								+ ":" + 3000);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 
			}
		});
		
		Button buttonFivePoint= (Button) findViewById(R.id.buttonFivePoint);

		buttonFivePoint.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				VisualOdometry vo = new VisualOdometry();
				
				(new Thread(vo)).start();
				//ExtendedKalmanFilter EKF = new ExtendedKalmanFilter();
				
			}
		});
		
		
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        return true;
    }

    
    @Override
	protected void onResume() {
		super.onResume();
		//OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_7, this, mLoaderCallback);
		
		//camera = Camera.open();
		//camera.startPreview();
		//preview.setCamera(camera);
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			//camera.stopPreview();
			//preview.setCamera(null);
			//camera.release();
			camera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		//camera.startPreview();
		//preview.setCamera(camera);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			// Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			// Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			FileOutputStream outStream = null;
			try {
				// Write to SD Card
				fileName = String.format("/sdcard/camtest/%d.bmp", System.currentTimeMillis());
				outStream = new FileOutputStream(fileName);
				outStream.write(data);
				outStream.close();
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);

				/**FeatureDetector detector = FeatureDetector.create(0);
				MatOfKeyPoint keypoints = null;

				Mat newImg = new Mat(480, 640, CvType.CV_8UC3);
				newImg.put(0, 0, data);
				detector.detect(newImg, keypoints);*/
				
				String fileName = String.format("/sdcard/desk/0001.png");
				
				Mat img = Highgui.imread(fileName);
				
				resetCam();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};
    

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        }

        return true;
    }
}
