package com.example.cam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

public class ConversionYUV2RGB implements Runnable {
	private File parentDir;
	private List<File> dirToProcess;
	
	
	public ConversionYUV2RGB(File _parentDir) {
		parentDir = _parentDir;
		dirToProcess = new ArrayList<File>();
	}

	void convertFilesInDirectory(File dir) {
		
		dirToProcess.add(dir);
		while(!dirToProcess.isEmpty())
		{
			Log.d("Conversion", "Size:" + dirToProcess.size());
			dir = dirToProcess.get(0);
			dirToProcess.remove(0);
			List<File> files = getListFiles(dir); 
		
			for (File file : files)
			{
				convertFile(file);
			}
		}
	}
	
	private void creatingSaveDir(File parentDir) {
		if (!parentDir.exists()) {
		    parentDir.mkdir();
		}
	}
	
	private List<File> getListFiles(File parentDir) {
	    ArrayList<File> inFiles = new ArrayList<File>();
	    File[] files = parentDir.listFiles();
	    
	    for (File file : files) {
	        if (!file.isDirectory()) {
				inFiles.add(file);
			}
	        else
	        {
	        	Log.d("Conversion", "Directory: " + file.getAbsolutePath());
	        	dirToProcess.add(file);
	        }
		}
		return inFiles;
	}

	void convertFile(File file) {

		if (!file.getAbsolutePath().contains("converted"))
		{
			
			try {
				// Read stream
				FileInputStream inStream = new FileInputStream(file);
				
				// Reading data
				byte fileContent[] = new byte[(int) file.length()];
				inStream.read(fileContent);
				YuvImage image = new YuvImage(fileContent, ImageFormat.NV21, 1920,
						1080, null);
	
				// Save Stream
				//String absPath = file.getAbsolutePath().replaceAll("ConvertIN", "ConvertOut");
				
				Log.d("Conversion", "File: " + file.getAbsolutePath());
				
				String dir = file.getParent().replaceAll("convertIn", "convertOut");
				Log.d("Conversion", "Dir " + dir);
				creatingSaveDir(new File(dir));
				
				String fileName = dir + "/" + file.getName();
				FileOutputStream outStream = new FileOutputStream(fileName);
				
				// Conversion and save to file
				image.compressToJpeg(new Rect(0, 0, 1920, 1080), 100, outStream);
				
				// Closing used streams
				inStream.close();
				outStream.close();
				
				Log.d("Conversion", "File " + file.getName() + " converted successfully");
				
			} catch (FileNotFoundException e) {
				Log.e("Conversion", "Error : " + e.toString() );
			} catch (IOException e) {
				Log.e("Conversion", "Error : " + e.toString() );
			}

		}
	}

	@Override
	/*
	 * Converting YUV images in given directory to RGB images and storing them in
	 * subdirectory /converted/ using separate thread
	 * 
	 * Assumptions:
	 * - size of all images is 1920x1080
	 */
	public void run() {
		convertFilesInDirectory(parentDir);
		Log.d("Conversion", "Conversion of whole directory ended successfully");
	}
}
