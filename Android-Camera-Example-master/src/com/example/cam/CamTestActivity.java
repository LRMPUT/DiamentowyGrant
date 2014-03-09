package com.example.cam;

/**
 * @author Jose Davis Nidhin
 */

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.example.cam.IP_PORT;
import com.example.cam.TCPClient;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CamTestActivity extends Activity {
	private static final String TAG = "CamTestActivity";
	Preview preview;
	Button buttonClick, buttonClickCalibration;
	Camera camera;
	String fileName;
	Activity act;
	Context ctx;
	boolean starting = false;
	
	byte[] last_img;
	
	public static TCPClient mTcpClient;
	boolean connected = false;
	
	public static InetAddress selected_ip = null;
	public static int selected_port = 0;

	
	private int ile = 0;
	private long timestampStart;
	
	
	// Inertial sensors
	InertialSensors inertialSensors;
	
	// WiFi
	WifiManager wifiManager;
	WifiReceiver wifiReceiver;
	

	public class connectionTCP extends AsyncTask<IP_PORT,String,Void> {
   	 
		@Override
		protected Void doInBackground(IP_PORT... adres) {

			
			mTcpClient = new TCPClient(adres[0],new TCPClient.OnMessageReceived() {
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
  	      	
			Log.e("TCP", "Progress: " + msg[0]);
			
			// Connection is established properly
  	      	if(connected == false)
			{
			}
  	        connected = true;
  	      	
  	      	// Message -> show as toast
  	      	int duration = Toast.LENGTH_LONG;
  	      	Toast.makeText(getApplicationContext(), msg[0], duration).show();

			if ( msg[0].contains("X") )
			{
				connected = false;
//				sensorManager.unregisterListener(sensorEventListener);
//				accStream.close();
//				gyroStream.close();
//				magStream.close();
//				accwogStream.close();
//				orientStream.close();
			}
			/*else
			{*/
            //	if ( SharedData.globalInstance.write_flag  == true)
            //	{
            //		Log.e("TCP Client", "Saving is not fast enough !");
            //	}
            //	SharedData.globalInstance.write_flag = true;
			//}
		}
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
		
		//preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		//preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		//((FrameLayout) findViewById(R.id.preview)).addView(preview);
		//preview.setKeepScreenOn(true);
		
		buttonClick = (Button) findViewById(R.id.buttonClick);
		
		buttonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				if (connected == false) {
					
					InetAddress selected_ip;
					try {
						selected_ip = InetAddress.getByName("192.168.1.132");
						IP_PORT adres = new IP_PORT(selected_ip, 3000);
						
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
		
		buttonClickCalibration = (Button) findViewById(R.id.buttonClick2);

		buttonClickCalibration.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//camera.takePicture(shutterCallback, rawCallback, jpegCallback);

				// Acc
				/*fileName = String.format("/sdcard/_exp/acc_%d.log",
						System.currentTimeMillis());
				try {
					ile = 0 ;
					faccStream = new FileOutputStream(fileName);
					accStream = new DataOutputStream(faccStream);
					sensorManager.registerListener(sensorEventListener,
							accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/

//				try {
//					if (ile == 0) {
//
//						activateSensors();
//						ile = 1;
//
//					} else {
//						sensorManager.unregisterListener(sensorEventListener);
//						ile = 0;
//					}
//
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

			}
		});
		
		Button buttonConvertYUV2RGB = (Button) findViewById(R.id.buttonConvert);
		
		buttonConvertYUV2RGB.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//File parentDir = new File("/sdcard/_exp/Przejazd1/imgs/");
				File parentDir = new File(Environment.getExternalStorageDirectory().toString() + "/_exp/Przejazd3/");
				
				ConversionYUV2RGB conversion = new ConversionYUV2RGB(parentDir);
				(new Thread(conversion)).start();
				
				} 
			
		});
		
		
	    
		SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    inertialSensors = new InertialSensors(sensorManager);
		
	    // WiFi part
	    setupWiFi();
	    
	    // WiFi + Inertial sensors
	    Button buttonInertialWiFi = (Button) findViewById(R.id.buttonInertialWiFi);
	    buttonInertialWiFi.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( wifiReceiver.getRunningState() == false)
				{
					
					wifiReceiver.startScanning();
					inertialSensors.start();
					
					Button buttonWiFiStartScan = (Button) findViewById(R.id.buttonWiFi);
					buttonWiFiStartScan.setText("Stop scanning");
							
				}
				else
				{
					wifiReceiver.stopScanning();
					inertialSensors.stop();
			
					Button buttonWiFiStartScan = (Button) findViewById(R.id.buttonWiFi);
					buttonWiFiStartScan.setText("Start scanning");
				}
			}
		});
	    
	}

	
	private void setupWiFi()
	{
	    wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);   
	    wifiReceiver = new WifiReceiver(wifiManager);
	    
	    Button buttonWiFiStartScan = (Button) findViewById(R.id.buttonWiFi);
	    Button buttonSingleWiFi = (Button) findViewById(R.id.buttonSingleWiFi);
      
	
	    buttonWiFiStartScan.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if ( wifiReceiver.getRunningState() == false)
				{
					wifiReceiver.startScanning();
					
					Button buttonWiFiStartScan = (Button) findViewById(R.id.buttonWiFi);
					buttonWiFiStartScan.setText("Wifi stop scanning");
				}
				else
				{
					wifiReceiver.stopScanning();
			
					Button buttonWiFiStartScan = (Button) findViewById(R.id.buttonWiFi);
					buttonWiFiStartScan.setText("Wifi start scanning");
				}
			}
		});
	    
	    buttonSingleWiFi.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
					wifiReceiver.doSingleScan();
			}
			
		});
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		registerReceiver(wifiReceiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		
		// preview.camera = Camera.open();
		if (connected) {
			//try {
			//	activateSensors();
			//} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
		}
		//camera = Camera.open();
		//camera.startPreview();
		//preview.setCamera(camera);
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		unregisterReceiver(wifiReceiver);
		inertialSensors.unregister();
		super.onPause();
	}

	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);
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
				// Write to SD Card the calibration images
				fileName = String.format(Environment.getExternalStorageDirectory().toString() + "/_exp/calib/%d.jpg",
						System.currentTimeMillis());
				try {
					outStream = new FileOutputStream(fileName);
					outStream.write(data);
					outStream.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				Log.d(TAG, "Image saved" + data.length);
				
				resetCam();


			} finally {
			}
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};
}
