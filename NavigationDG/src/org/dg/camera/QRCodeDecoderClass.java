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

package org.dg.camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import org.dg.openAIL.MapPosition;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.detector.WhiteRectangleDetector;
import com.google.zxing.qrcode.QRCodeReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

public class QRCodeDecoderClass {
    private final String TAG = "QRCode";
    
    private Context mContext;
    
    private List<Pair<Integer,String>> recognizedMessages = null;
    
    private final Semaphore recognizedMessagesMtx = new Semaphore(1, true);
    
    
    // Creating camera matrix
    private Mat cameraMatrix640, cameraMatrix1920;

	// Creaing distCoefss
	private MatOfDouble distCoeffs640, distCoeffs1920;
    
    public QRCodeDecoderClass (Context context) {
    	mContext = context;
    	recognizedMessages = new LinkedList<Pair<Integer,String>>();        
    	
    }
	
	public void decode(Integer positionId, Mat image) {
		Log.d(TAG, "creating async task");
		new DecodeAsyncTask(positionId, mContext).execute(image);
	}
	
	public List<Pair<Integer,Point3>> getRecognizedQRCodes() {
		
		List<Pair<Integer,Point3>> returnList = new LinkedList<Pair<Integer,Point3>>();
		
		try {
			
			
			
			recognizedMessagesMtx.acquire();
			for (Pair<Integer, String> pair : recognizedMessages) {
				
				// Preparations to read positions
				Scanner positionScanner = null;
				positionScanner = new Scanner(pair.second);
				positionScanner.useLocale(Locale.US);
			
				
				positionScanner.next("Position");
				double X = positionScanner.nextDouble();
				double Y = positionScanner.nextDouble();
				double Z = positionScanner.nextDouble();
				
				Point3 pos = new Point3(X, Y, Z);
				returnList.add(new Pair<Integer,Point3>(pair.first, pos));
			}
			recognizedMessages.clear();
			
			recognizedMessagesMtx.release();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return returnList;
		
	}
	
	private class DecodeAsyncTask extends AsyncTask<Mat, String, String> {
		
		Integer positionId = 0;
		
		Context context;

        /**
         * @param context
         */
        private DecodeAsyncTask(Integer _positionId, Context _context) {
        	context = _context;
        	positionId = _positionId;
        }
        
        @Override
		protected String doInBackground(Mat... image) {
        	  	
        	cameraMatrix640 = Mat.zeros(3, 3, CvType.CV_64F);
        	cameraMatrix640.put(0, 0, 573.61280);
        	cameraMatrix640.put(0, 2, 320.74271);
        	cameraMatrix640.put(1, 1, 575.86251);
        	cameraMatrix640.put(1, 2, 241.84638);
        	cameraMatrix640.put(2, 2, 1.0);
        	distCoeffs640 = new MatOfDouble(0.08371, -0.20442, 0.00016, -0.00077, 0.0000);
        	
        	cameraMatrix1920 = Mat.zeros(3, 3, CvType.CV_64F);
        	cameraMatrix1920.put(0, 0, 1728.16411);
        	cameraMatrix1920.put(0, 2, 962.62688);
        	cameraMatrix1920.put(1, 1, 1715.60377);
        	cameraMatrix1920.put(1, 2, 546.66063);
        	cameraMatrix1920.put(2, 2, 1.0);
        	distCoeffs1920 = new MatOfDouble(0.11443, -0.28006, 0.00070, -0.00019, 0.00000);
        	
//        	return decodeQRImage(image[0], cameraMatrix1920, distCoeffs1920);
        	
			File mainDir = new File(Environment.getExternalStorageDirectory()
					+ "/OpenAIL/QR/");
//        	File mainDir = new File(Environment.getExternalStorageDirectory()
//					+ "/OpenAIL/SPA_tests/6");

			FileOutputStream locStream = null;
			try {
				locStream = new FileOutputStream(
						mainDir.toString() + "/localization.log");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			PrintStream localizationStream = new PrintStream(locStream);

			File[] dirs = mainDir.listFiles();
			for (File dir : dirs) {
				if (dir.isDirectory()) {

					File[] files = dir.listFiles();
					for (File file : files) {
						Log.d(TAG, "Possible file: " + file.getAbsolutePath());
						
//						String [] s = file.getName().split("_");
//						int val = Integer.parseInt(s[0]);
//						Log.d(TAG, "Pose number: " + val);
						int val = 0;
						
						Mat img = Highgui.imread(file.getAbsolutePath());
						
						String posOrient = "";
						if ( dir.getName().contains("640"))
							posOrient = decodeQRImage(img, cameraMatrix640, distCoeffs640);
						else
							posOrient = decodeQRImage(img, cameraMatrix1920, distCoeffs1920);
							
						
						

						publishProgress(dir.getName() + "/" + file.getName()
								+ " " + posOrient);

						localizationStream.println(dir.getName() + "/"
								+ file.getName() + " " + posOrient);
//						localizationStream.println("EDGE_SE2:STEP " + val + " X "
//								+ posOrient + " 1.0 0.0 1.0");
					}

				}
			}
			localizationStream.close();

			// return decodeQRImage(image[0]);
			return "FINISHED!";
		}
        
        protected void onProgressUpdate(String... progress) {
            Toast.makeText(context, progress[0], Toast.LENGTH_LONG).show();
        }
        

        @Override
        protected void onPostExecute(String result) {
        	Log.d(TAG, "onPostExecute : " + result);
        	Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        	
        	if ( result != "Decode failed!") {
				try {
					recognizedMessagesMtx.acquire();
					
					recognizedMessages.add(new Pair<Integer,String>(positionId, result));
		        	
					recognizedMessagesMtx.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}

        }
        
        private String decodeQRImage(Mat image, Mat cameraMatrix, MatOfDouble distCoeffs) {
        	Log.d(TAG, "decodeQRImage in async");
        	
        	Mat undist = new Mat();
        	Imgproc.undistort(image, undist, cameraMatrix, distCoeffs);
        	image = undist;
        	
        	
        	
        	// Creating bitmap from OpenCV::Mat
        	Bitmap bMap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        	Utils.matToBitmap(image, bMap);
    		
    		// Getting the int Array from bitmap
    		int[] intArray = new int[bMap.getWidth()*bMap.getHeight()]; 
    		bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());  

    		// Creating binary bitmap for QR code detection
    		LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);
    		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    		
    		// QRreader - decoding the qr code
    		Result result = null;
    		try {
    			QRCodeReader reader = new QRCodeReader();  
    			result = reader.decode(bitmap);
    		}
    		catch (Exception e) {
    			return "Not detected";
//    			return "Decode failed!";
    		}
    		
    		
    		//return result.getText();
    		
    		

    		// Extracting the points detected on QR code
    		Point [] imgP = new Point[4];
    		int i =0;
    		ResultPoint[] points = result.getResultPoints();
    		for (ResultPoint p : points) {
				Log.d(TAG, "QR code point: " + p.getX() + " " + p.getY());

				imgP[i] = new Point();
				imgP[i].x = p.getX();
				imgP[i].y = p.getY();

				i++;
    		}
    		Log.d(TAG, "image type: " + image.type() ); 
    		
    		Mat grayImage = new Mat();
        	Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_RGB2GRAY);
        	
        	image = new Mat();
        	Imgproc.adaptiveThreshold(grayImage, image, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 10);
    		
    		
    		// TESTING white region detector
    		BitMatrix bitImg = new BitMatrix(image.width(), image.height());
    		for ( i=0;i<image.width();i++)
    		{
    			for (int j=0;j<image.height();j++)
    			{
    				//Log.d(TAG, "TEST: " + image.get(j, i)[0]); 
    				
//    				int value = bMap.getPixel(i, j);
//    				int R = (value >> 16) & 0xff ;    
//    				int G = (value >> 8) & 0xff ; 
//    				int B = value & 0xff ; 
    				
//					if (0.299 * R + 0.587 * G + 0.114 * B > 150)
    				//Log.d(TAG, "TEST: " + image.get(j, i)[0]); 
    				if(image.get(j, i)[0] < 128)
						bitImg.set(i, j);
//					else
//						bitImg.unset(i, j);
    				
    			}
    		}
    		  		
    		WhiteRectangleDetector wrd;
			try {
				wrd = new WhiteRectangleDetector(bitImg, (int)(imgP[0].y - imgP[1].y), (int)((imgP[0].x + imgP[2].x)/2), (int)((imgP[0].y + imgP[2].y)/2));
				ResultPoint [] rps = wrd.detect();
				 i =0;
	    		for (ResultPoint p : rps) {
	    			Log.d(TAG, "WRD point: " + p.getX() + " " + p.getY() ); 
	    			Point cvPoint = new Point(p.getX(), p.getY());
            		Core.circle(image, cvPoint, 20, new Scalar(0, 0, 255));
            		
            		imgP[i] = new Point();
        			imgP[i].x = p.getX();
        			imgP[i].y = p.getY();
        			i++;
	    		}
			} catch (Exception e) {
				Log.d(TAG, "WRD failed: " + e.getMessage() ); 
				return "WRD failed";
			}
    		
			
			// Saving the image with marked points
			File folder = new File(Environment.getExternalStorageDirectory()
					+ "/OpenAIL/rawData/Imgs");
    		Highgui.imwrite(folder.getAbsolutePath() + "/testDet.png", image);
    		
    		
    		
			// Creating image points
    		MatOfPoint2f imagePoints = new MatOfPoint2f(imgP[0], imgP[1], imgP[2], imgP[3]);
    		Log.d(TAG, "Created image points " + imagePoints.rows() + " " + imagePoints.cols() ); 
    		
    		
    		
    		// 3D points -> inside QR CODE
//    		Point3 bottomLeft = new Point3(0, 64, 0);
//    		Point3 topLeft = new Point3(0, 0, 0);
//    		Point3 topRight = new Point3(64, 0, 0);
//    		Point3 middle = new Point3(64/2.0, 64/2.0, 0);
//    		Point3 bottomRight = new Point3(53.0, 53.0, 0);
    		
    		// 3D points -> outside QR CODE
    		Point3 topLeft = new Point3(0, 0, 0);
    		Point3 bottomLeft = new Point3(0, 0.180, 0);
    		Point3 topRight = new Point3(0.180, 0, 0);
    		Point3 bottomRight = new Point3(0.180, 0.180, 0);
    		
    		
    		Log.d(TAG, "PreProjected points: " + topLeft.x + " " + topLeft.y); 
    		Log.d(TAG, "PreProjected points: " + bottomLeft.x + " " + bottomLeft.y); 
    		Log.d(TAG, "PreProjected points: " + topRight.x + " " + topRight.y); 
    		Log.d(TAG, "PreProjected points: " + bottomRight.x + " " + bottomRight.y); 
    		
    		MatOfPoint3f objectPoints = new MatOfPoint3f(topLeft, bottomLeft, topRight, bottomRight);
    		
    		Log.d(TAG, "Created object points " + objectPoints.rows() + " " + objectPoints.cols()); 
    		
	    	
    		
    		
    		// Place to save results
    		Mat rvec = new Mat(3, 1, CvType.CV_64F), tvec = new Mat(3, 1, CvType.CV_64F);
   
    		// Calling solvePNP
    		try {
    			Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, distCoeffs, rvec, tvec, false, Calib3d.CV_ITERATIVE);
    			
    			Log.d(TAG, "tvec: " + tvec.cols() + " " + tvec.rows()); 
    			Log.d(TAG, "tvec: " + tvec.get(0, 0)[0]); 
    			Log.d(TAG, "tvec: " + tvec.get(1, 0)[0]); 
    			Log.d(TAG, "tvec: " + tvec.get(2, 0)[0]); 
    			
    			MatOfPoint2f projectedPoints = new MatOfPoint2f();
    			Calib3d.projectPoints(objectPoints, rvec, tvec, cameraMatrix, distCoeffs, projectedPoints);
    			
    			for (int i1=0;i1<projectedPoints.rows();i1++) {
    				Log.d(TAG, "Projected points: " + projectedPoints.get(i1, 0)[0] + " " + projectedPoints.get(i1, 0)[1]); 
    			}
    		}
    		catch (Exception e) {
    			Log.d(TAG, "PNP failed: " + e.getMessage() ); 
    			return "PNP failed";
    		}
    		
    		Mat rotationMatrix = new Mat();
    		Calib3d.Rodrigues(rvec, rotationMatrix);
    		
    		double m00 = rotationMatrix.get(0, 0)[0];
    		double m21 = rotationMatrix.get(2, 1)[0];
    		double m12 = rotationMatrix.get(1, 2)[0];
    		double m02 = rotationMatrix.get(0, 2)[0];
    		double m20 = rotationMatrix.get(2, 0)[0];
    		double m01 = rotationMatrix.get(0, 1)[0];
    		double m10 = rotationMatrix.get(1, 0)[0];
    		double m11 = rotationMatrix.get(1, 1)[0];
    		double m22 = rotationMatrix.get(2, 2)[0];
    		
    		double tr = m00 + m11 + m22;
    		double qw = 0, qx = 0, qy = 0, qz = 0;
			if (tr > 0) {
				double S = Math.sqrt(tr + 1.0) * 2; // S=4*qw
				qw = 0.25 * S;
				qx = (m21 - m12) / S;
				qy = (m02 - m20) / S;
				qz = (m10 - m01) / S;
			} else if ((m00 > m11) & (m00 > m22)) {
				double S =  Math.sqrt(1.0 + m00 - m11 - m22) * 2; // S=4*qx
				qw = (m21 - m12) / S;
				qx = 0.25 * S;
				qy = (m01 + m10) / S;
				qz = (m02 + m20) / S;
			} else if (m11 > m22) {
				double S =  Math.sqrt(1.0 + m11 - m00 - m22) * 2; // S=4*qy
				qw = (m02 - m20) / S;
				qx = (m01 + m10) / S;
				qy = 0.25 * S;
				qz = (m12 + m21) / S;
			} else {
				double S =  Math.sqrt(1.0 + m22 - m00 - m11) * 2; // S=4*qz
				qw = (m10 - m01) / S;
				qx = (m02 + m20) / S;
				qy = (m12 + m21) / S;
				qz = 0.25 * S;
			}
    		
    		
    		Log.d(TAG, "SolvePNP success! XYZ: " + tvec.get(0, 0)[0] + " " + tvec.get(1, 0)[0] + " " + tvec.get(2, 0)[0] ); 
    		Log.d(TAG, "SolvePNP success! qx qy qz qw: " + qx + " " + qy + " " + qz + " " + qw ); 
    		
    		String posOrient = tvec.get(0, 0)[0] + " " + tvec.get(1, 0)[0] + " " + tvec.get(2, 0)[0] + " " + qx + " " + qy + " " + qz + " " + qw;
    		
    		
    		rotationMatrix = rotationMatrix.t();  // rotation of inverse
//    		tvec.put(1, 0, - tvec.get(1, 0)[0]);
//    		tvec.put(2, 0, - tvec.get(2, 0)[0]);
//    		tvec.put(3, 0, - tvec.get(3, 0)[0]);
    		//tvec = rotationMatrix * tvec; // translation of inverse
    		Mat tvec2 = new Mat();
    		Core.gemm(rotationMatrix, tvec, -1, new Mat(), 0, tvec2, 0);
    		Log.d(TAG, "Inverse! tvec2: " + tvec2.get(0, 0)[0] + " " + tvec2.get(1, 0)[0] + " " + tvec2.get(2, 0)[0] ); 
    		
    		double distanceToCamera = Math.sqrt(tvec2.get(0, 0)[0]*tvec2.get(0, 0)[0] + tvec2.get(2, 0)[0]*tvec2.get(2, 0)[0]);
    		Log.d(TAG, "Inverse! DIST: " + Math.sqrt(tvec2.get(0, 0)[0]*tvec2.get(0, 0)[0] + tvec2.get(2, 0)[0]*tvec2.get(2, 0)[0]) + " m" ); 
    		
    		// camera Z axis in XZ plane of QR
    		Mat Z = rotationMatrix.col(2);
    		Z.put(1, 0, 0);
    		// Z axis of QR
    		Mat Zqr = Mat.zeros(3, 1, CvType.CV_64F);
    		Zqr.put(2, 0, 1.0);
    		
    		
    		double dotProduct = Zqr.dot(Z);
    		Mat crossProduct = Zqr.cross(Z); 
    		double angleToCamera = Math.signum(crossProduct.get(1, 0)[0]) *Math.acos(dotProduct);
    		Log.d(TAG, "Inverse! ANGLE: " + Math.toDegrees(angleToCamera) + " deg"); 
    		
    		return distanceToCamera + " " + angleToCamera ;//+ "\r\n" + posOrient;
    		
    		//return result.toString();
    	}

      
    
	}
	
	

}
