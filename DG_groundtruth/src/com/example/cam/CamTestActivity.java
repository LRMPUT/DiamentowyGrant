package com.example.cam;

/**
 * @author Jose Davis Nidhin
 */

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.example.cam.IP_PORT;
import com.example.cam.TCPClient;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
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
	Button buttonClick;
	Camera camera;
	String fileName;
	Activity act;
	Context ctx;
	
	byte[] last_img;
	
	private TCPClient mTcpClient;
	SensorManager sensorManager;
	boolean connected = false;
	Sensor accelerometer, gyro, magnetic, acc_wo_gravity, orientation;

	public static InetAddress selected_ip = null;
	public static int selected_port = 0;

	FileOutputStream faccStream, fgyroStream, fmagStream, faccwogStream, forientStream; 
	DataOutputStream accStream, gyroStream, magStream, accwogStream, orientStream; 
	
	private SensorEventListener sensorEventListener = new SensorEventListener() {

	    public void onAccuracyChanged(Sensor sensor, int accuracy) {

	    }

	    public void onSensorChanged(SensorEvent event) {
	    	long timestamp = event.timestamp;
	    	String nl = System.getProperty("line.separator");
	    	if ( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER )
	    	{
		        float x = event.values[0];
		        float y = event.values[1];
		        float z = event.values[2];
		        
		        Log.v("ACC", "ACC: x: " + x + " y: " + y + " z: " + z);
		        
		        try {
					accStream
							.writeUTF(Long.toString(System.currentTimeMillis())
									+ " " + Float.toString(x) + " "
									+ Float.toString(y) + " "
									+ Float.toString(z) + nl);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		       
	    	}
	    	else if ( event.sensor.getType() == Sensor.TYPE_GYROSCOPE )
	    	{
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];
				Log.v("GYR", "GYR: x: " + x + " y: " + y + " z: " + z);
				
				try {
					gyroStream.writeUTF(Long.toString(System
							.currentTimeMillis())
							+ " "
							+ Float.toString(x)
							+ " "
							+ Float.toString(y)
							+ " "
							+ Float.toString(z)
							+ nl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];
				Log.v("MAG", "MAG: x: " + x + " y: " + y + " z: " + z);

				try {
					magStream
							.writeUTF(Long.toString(System.currentTimeMillis())
									+ " " + Float.toString(x) + " "
									+ Float.toString(y) + " "
									+ Float.toString(z) + nl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];
				Log.v("LINACC", "LINACC: x: " + x + " y: " + y + " z: " + z);

				try {
					accwogStream.writeUTF(Long.toString(System
							.currentTimeMillis())
							+ " "
							+ Float.toString(x)
							+ " "
							+ Float.toString(y)
							+ " "
							+ Float.toString(z)
							+ nl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
	    	}
	    	else if ( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR )
	    	{
				float x = event.values[0];
				float y = event.values[1];
				float z = event.values[2];
				Log.v("ROT", "ROT: x: " + x + " y: " + y + " z: " + z);
				
				try {
					orientStream.writeUTF(Long.toString(System
							.currentTimeMillis())
							+ " "
							+ Float.toString(x)
							+ " "
							+ Float.toString(y)
							+ " "
							+ Float.toString(z)
							+ nl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
	    	}
	    }
	};
	
	
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
  	      	
			Log.e("TCP", "C: " + msg[0]);
			
			// Connection is established properly
  	      	if(msg[0] == "Connected" && connected == false)
			{
				connected = true;
				try {
					activateSensors();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
  	      	
  	      	// Message -> show as toast
  	      	int duration = Toast.LENGTH_LONG;
  	      	Toast.makeText(getApplicationContext(), msg[0], duration).show();

			if ( msg[0].contains("_X") )
			{
				connected = false;
				try {
					sensorManager.unregisterListener(sensorEventListener);
					accStream.close();
					gyroStream.close();
					magStream.close();
					accwogStream.close();
					orientStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			camera.takePicture(shutterCallback, rawCallback, jpegCallback);

		}
	}
	
	
	void activateSensors() throws FileNotFoundException
	{
	
		fileName = String.format("/sdcard/_exp/acc.log");
		faccStream = new FileOutputStream(fileName);
		accStream = new DataOutputStream(faccStream);
		
		fileName = String.format("/sdcard/_exp/gyro.log");
		fgyroStream = new FileOutputStream(fileName);
		gyroStream = new DataOutputStream(fgyroStream);
		
		fileName = String.format("/sdcard/_exp/mag.log");
		fmagStream = new FileOutputStream(fileName);
		magStream = new DataOutputStream(fmagStream);
		
		fileName = String.format("/sdcard/_exp/accwog.log");
		faccwogStream = new FileOutputStream(fileName);
		accwogStream = new DataOutputStream(faccwogStream);
		
		fileName = String.format("/sdcard/_exp/orient.log");
		forientStream = new FileOutputStream(fileName);
		orientStream = new DataOutputStream(forientStream);
		
		sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, gyro, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, acc_wo_gravity, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(sensorEventListener, orientation, SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);
		
		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.preview)).addView(preview);
		preview.setKeepScreenOn(true);
		
		buttonClick = (Button) findViewById(R.id.buttonClick);
		
		buttonClick.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//				preview.camera.takePicture(shutterCallback, rawCallback, jpegCallback);
				// camera.takePicture(shutterCallback, rawCallback, jpegCallback);
				
				

				if (connected == false) {
					
					InetAddress selected_ip;
					try {
						selected_ip = InetAddress.getByName("192.168.1.130");
						IP_PORT adres = new IP_PORT(selected_ip, 3000);

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
		
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
	    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	    magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    acc_wo_gravity = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
	    orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
	    
	}

	@Override
	protected void onResume() {
		super.onResume();
		//      preview.camera = Camera.open();
		if ( connected )
		{
			//try {
			//	activateSensors();
			//} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
		}
		camera = Camera.open();
		camera.startPreview();
		preview.setCamera(camera);
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		//sensorManager.unregisterListener(sensorEventListener);
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
				
				
				// Write to SD Card
				fileName = String.format("/sdcard/_exp/%d.jpg",
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

				mTcpClient.sendMessage("READY");

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);

				resetCam();

			} finally {
			}
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};
}
