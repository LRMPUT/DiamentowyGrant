package org.dg.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;

public class CameraSaver implements PictureCallback {
	final String TAG = "CameraSaver";

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		Log.d(TAG, "CameraSaver::onPictureTaken - jpeg");

		try {

			File folder = new File(Environment.getExternalStorageDirectory()
					+ "/DG");
			boolean success = true;
			if (!folder.exists()) {
				success = folder.mkdir();
			}

			// Folder exists so we can store new image
			if (success) {
				// Write to SD Card
				FileOutputStream outStream = null;
				String fileName = String.format(folder + "/%d.jpg",
						System.currentTimeMillis());
				outStream = new FileOutputStream(fileName);
				outStream.write(data);
				outStream.close();
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
			} else {
				Log.d(TAG, "Folder 'DG' creation failed!");
			}
			camera.startPreview();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}

	}

}
