package org.dg.inertialSensors;

import java.io.File;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

import android.hardware.SensorManager;
import android.util.Log;

public class ProcessRecorded {
	// TAG
	private static final String TAG = "ProcessRecorded";
			
	static final double ns2s = 1e-9;
	static final double ms2s = 1e-3;
	static final float[] enuToNwu = new float[]{0, -1, 0,
												1, 0, 0,
												0, 0, 1};
	static final float[] enuToNED = new float[]{0, 1, 0,
												1, 0, 0,
												0, 0, -1};
	static final float[] nwuToNED = new float[]{1, 0, 0,
												0, -1, 0,
												0, 0, -1};
	
	static final float[] xSenseToTel = new float[]{0.0f, 1.0f, 0.0f,
													1.0f, 0.0f, 0.0f,
													0.0f, 0.0f, -1.0f};
	
	
	
//	public static float[] eulerXYZToQuat(float[] euler){
//		float[] quat = new float[4];
//		float c1 = Math.cos(euler[])
//		
//		return quat;
//	}
	
	private static float[] quatInv(float[] q){
		float[] ret = new float[4];
		ret[0] = q[0];
		ret[1] = -q[1];
		ret[2] = -q[2];
		ret[3] = -q[3];
		
		return ret;
	}
	
	//http://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation#Quaternions_briefly
	private static float[] quatMul(float[] q1, float[] q2){
		float[] ret = new float[4];
		
//		Log.d(TAG, String.format("q1 = {%f, %f, %f, %f}",
//									q1[0],
//									q1[1],
//									q1[2],
//									q1[3]));
//		
//		Log.d(TAG, String.format("q2 = {%f, %f, %f, %f}",
//									q2[0],
//									q2[1],
//									q2[2],
//									q2[3]));
		
		ret[0] = q1[0]*q2[0] - q1[1]*q2[1] - q1[2]*q2[2] - q1[3]*q2[3];
		ret[1] = q1[0]*q2[1] + q1[1]*q2[0] + q1[2]*q2[3] - q1[3]*q2[2];
		ret[2] = q1[0]*q2[2] + q1[2]*q2[0] + q1[3]*q2[1] - q1[1]*q2[3];
		ret[3] = q1[0]*q2[3] + q1[3]*q2[0] + q1[1]*q2[2] - q1[2]*q2[1];
		
//		Log.d(TAG, String.format("ret = {%f, %f, %f, %f}",
//									ret[0],
//									ret[1],
//									ret[2],
//									ret[3]));
		
		return ret;
	}
	
	private static float[] invOrthoNormMat(float[] A){
		float[] ret = new float[9];
//		Log.d(TAG, String.format("A = \n {%f, %f, %f,\n"
//				+ "%f, %f, %f,\n"
//				+ "%f, %f, %f}\n",
//				A[0], A[1], A[2],
//				A[3], A[4], A[5],
//				A[6], A[7], A[8]));
		
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < 3; j++){
				ret[j*3 + i] = A[i*3 + j];
			}
		}
		
//		Log.d(TAG, String.format("ret = \n {%f, %f, %f,\n"
//				+ "%f, %f, %f,\n"
//				+ "%f, %f, %f}\n",
//				ret[0], ret[1], ret[2],
//				ret[3], ret[4], ret[5],
//				ret[6], ret[7], ret[8]));
		
		return ret;
	}
	
	private static void saveToStream(PrintStream stream,
									Long timestamp,
									float[] values)
	{

		stream.print(Long.toString(timestamp));
		for (int i = 0; i < values.length; i++) {
			stream.print(" " + Float.toString(values[i]));
		}
		stream.print(System.getProperty("line.separator"));
	}
	
	public ProcessRecorded(){
		
	}
	
	public static double process(File dir, float coeff){
		Scanner gyroScanner = null;
		Scanner orientScanner = null;
		Scanner xSenseScanner = null;

		try{
			gyroScanner = new Scanner(new BufferedReader(new FileReader(dir.toString() + "/gyro.log")));
			gyroScanner.useLocale(Locale.US);
			orientScanner = new Scanner(new BufferedReader(new FileReader(dir.toString() + "/orient.log")));
			orientScanner.useLocale(Locale.US);
			xSenseScanner = new Scanner(new BufferedReader(new FileReader(dir.toString() + "/Xsense.data")));
			xSenseScanner.useLocale(Locale.US);
			
			PrintStream compFilterOutStream = new PrintStream(new FileOutputStream(dir.toString() + "/compFilterEuler.log"));
			PrintStream xSenseOutStream = new PrintStream(new FileOutputStream(dir.toString() + "/xSenseEuler.log"));
			PrintStream orientOutStream = new PrintStream(new FileOutputStream(dir.toString() + "/orientEuler.log"));
			PrintStream AEKFOutStream = new PrintStream(new FileOutputStream(dir.toString() + "/AEKFEuler.log"));

			
			ComplementaryFilter compFilter = new ComplementaryFilter(coeff);
			AHRSModule AEKF = new AHRSModule();
			
			long prevGyroTimestamp = gyroScanner.nextLong();
			long nextGyroTimestamp = prevGyroTimestamp;
			long firstGyroTimestamp = prevGyroTimestamp;
			long nextOrientTimestamp = orientScanner.nextLong();
			float nextXSenseTimestamp = -1;
			xSenseScanner.nextFloat();
			xSenseScanner.next();
			
			double J2 = 0.0;
		
			float[] orientQuat = null;
			float[] xSenseQuat = null;
			float [] lastAndroidQuat = new float [4];
			
//			Log.d(TAG, String.format("nextGyrotimestamp = %d", nextGyroTimestamp));
			
			int count = 0;
			while((gyroScanner.hasNextFloat() || orientScanner.hasNextFloat())/* && (count < 150)*/){
				
				while(nextXSenseTimestamp < nextGyroTimestamp && xSenseScanner.hasNextFloat()){
					if(xSenseQuat == null){
						xSenseQuat = new float[4];
					}
					//skip accelerometer data
					xSenseScanner.nextFloat();
					xSenseScanner.nextFloat();
					xSenseScanner.nextFloat();
					xSenseScanner.next();
					xSenseQuat[0] = xSenseScanner.nextFloat();
					xSenseQuat[1] = xSenseScanner.nextFloat();
					xSenseQuat[2] = xSenseScanner.nextFloat();
					xSenseQuat[3] = xSenseScanner.nextFloat();
					//skip gyro, mag
					xSenseScanner.next();
					xSenseScanner.nextFloat();
					xSenseScanner.nextFloat();
					xSenseScanner.nextFloat();
					xSenseScanner.next();
					xSenseScanner.nextFloat();
					xSenseScanner.nextFloat();
					xSenseScanner.nextFloat();
					if(xSenseScanner.hasNextFloat()){
						nextXSenseTimestamp = xSenseScanner.nextFloat();
						xSenseScanner.next();
					}
				}

				if(count % 100 == 0){
//					Log.d(TAG, ".");
//					Log.d(TAG, String.format("nextGyroTimestamp = %d", nextGyroTimestamp));
//					Log.d(TAG, String.format("nextOrientTimestamp = %d", nextOrientTimestamp));
//					Log.d(TAG, String.format("nextXSenseTimestamp = %f", nextXSenseTimestamp));
//					Log.d(TAG, String.format("count = %d", count));
				}
				
//				Log.d(TAG, String.format("xSenseQuat = {%f, %f, %f, %f}",
//													xSenseQuat[0],
//													xSenseQuat[1],
//													xSenseQuat[2],
//													xSenseQuat[3]));
				
				if((nextGyroTimestamp <= nextOrientTimestamp || !orientScanner.hasNextFloat())
						&& gyroScanner.hasNextFloat())
				{
					float[] gyroVals = new float[3];
					gyroVals[0] = gyroScanner.nextFloat();
					gyroVals[1] = gyroScanner.nextFloat();
					gyroVals[2] = gyroScanner.nextFloat();
					if(nextGyroTimestamp - prevGyroTimestamp > 0){
						float dt = (nextGyroTimestamp - prevGyroTimestamp) * (float)ms2s;
						compFilter.gyroscopeUpdate(gyroVals, dt);
						
						float x = (float) (gyroVals[0]);
						float y = (float) (gyroVals[1]);
						float z = (float) (gyroVals[2]);
						float newDt = (float) (dt);
						AEKF.predict(x, y, z, newDt);
					}
					
					if(orientQuat != null){
						
						// Complementary filter - QUAT TO EULER
						float[] compFilterQuat = compFilter.getEstimate();
						float[] compFilterMat = new float[9];
						SensorManager.getRotationMatrixFromVector(compFilterMat,
																ComplementaryFilter.quat2rotVec(compFilterQuat));
						//compFilterMat = ComplementaryFilter.matMul(enuToNED, compFilterMat);
						//compFilterMat = ComplementaryFilter.matMul(compFilterMat, xSenseToTel);
						
						float[] compFilterOrient = new float[3];
						//SensorManager.getOrientation(compFilterMat, compFilterOrient);
						compFilterOrient = eulerZYX(compFilterMat);
						saveToStream(compFilterOutStream, nextGyroTimestamp, compFilterOrient);
						
						// AEKF - QUAT TO EULER
						float[] AEKFQuat = AEKF.getEstimate();
						float[] AEKFMat = new float[9];
						SensorManager.getRotationMatrixFromVector(AEKFMat,
								ComplementaryFilter.quat2rotVec(AEKFQuat));
						//AEKFMat = ComplementaryFilter.matMul(enuToNED, AEKFMat);
						//AEKFMat = ComplementaryFilter.matMul(AEKFMat, xSenseToTel);
						
						float[] AEKFOrient = new float[3];
					//	SensorManager.getOrientation(AEKFMat, AEKFOrient);
						AEKFOrient = eulerZYX(AEKFMat);
						saveToStream(AEKFOutStream, nextGyroTimestamp, AEKFOrient);
						
						
						// ERROR
						float[] xSenseMat = new float[9];
						
			
						SensorManager.getRotationMatrixFromVector(xSenseMat, ComplementaryFilter.quat2rotVec(xSenseQuat));
						//xSenseMat = ComplementaryFilter.matMul(nwuToNED, xSenseMat);
						xSenseMat = ComplementaryFilter.matMul(xSenseMat, xSenseToTel);
						xSenseMat = ComplementaryFilter.matMul(enuToNwu, xSenseMat);
						
						float[] xSenseOrient = new float[3];
						//SensorManager.getOrientation(xSenseMat, xSenseOrient);
						xSenseOrient = eulerZYX(xSenseMat);
						
						saveToStream(xSenseOutStream, (long)nextXSenseTimestamp, xSenseOrient);
						
						
//						float[] diffQuat = quatMul(compFilterQuat, quatInv(xSenseQuatTelFrame));
//						
//						double norm = Math.sqrt((double)diffQuat[0] * diffQuat[0] +
//												(double)diffQuat[1] * diffQuat[1] +
//												(double)diffQuat[2] * diffQuat[2] +
//												(double)diffQuat[3] * diffQuat[3]);
						
//						double diffAng = 2.0f * Math.acos(Math.abs((double)diffQuat[0]/ norm));
						double diffAngZ = Math.min(Math.abs(Math.abs(compFilterOrient[0] - xSenseOrient[0]) - 2.0 * Math.PI),
													Math.abs(compFilterOrient[0] - xSenseOrient[0]));
						double diffAngX = Math.min(Math.abs(Math.abs(compFilterOrient[1] - xSenseOrient[1]) - 2.0 * Math.PI),
													Math.abs(compFilterOrient[1] - xSenseOrient[1]));
						double diffAngY = Math.min(Math.abs(Math.abs(compFilterOrient[2] - xSenseOrient[2]) - 2.0 * Math.PI),
													Math.abs(compFilterOrient[2] - xSenseOrient[2]));
						double diffAng = Math.sqrt(diffAngZ*diffAngZ +
													diffAngX*diffAngX + 
													diffAngY*diffAngY);
//						Log.d(TAG, String.format("diffQuat = {%f, %f, %f, %f}",
//						diffQuat[0],
//						diffQuat[1],
//						diffQuat[2],
//						diffQuat[3]));
//						Log.d(TAG, String.format("diffAng = %e", diffAng));
//						Log.d(TAG, String.format("diff int = %e", diffAng * diffAng * (nextGyroTimestamp - prevGyroTimestamp) * ns2s));
						
						J2 += diffAng * (nextGyroTimestamp - prevGyroTimestamp) * ms2s;
						
//						Log.d(TAG, String.format("orientQuat = {%f, %f, %f, %f}",
//													orientQuat[0],
//													orientQuat[1],
//													orientQuat[2],
//													orientQuat[3]));
//						
//						Log.d(TAG, String.format("gyroVals = {%f, %f, %f}",
//														gyroVals[0],
//														gyroVals[1],
//														gyroVals[2]));
						
//						float cfMul = compFilterQuat[0] > 0 ? 1.0f : -1.0f;
//						float[] compFilterRotVec = new float[]{cfMul*compFilterQuat[1],
//																cfMul*compFilterQuat[2],
//																cfMul*compFilterQuat[3]};

							
//						float[] orientMat = new float[9];
//						SensorManager.getRotationMatrixFromVector(orientMat,
//																	ComplementaryFilter.quat2rotVec(orientQuat));
						
//						Log.d(TAG, String.format("orientMat = \n {%f, %f, %f,\n"
//										+ "%f, %f, %f,\n"
//										+ "%f, %f, %f}\n",
//										orientMat[0], orientMat[1], orientMat[2],
//										orientMat[3], orientMat[4], orientMat[5],
//										orientMat[6], orientMat[7], orientMat[8]));
//						
//						Log.d(TAG, String.format("compFilterOrient = {%f, %f, %f}",
//													compFilterOrient[0],
//													compFilterOrient[1],
//													compFilterOrient[2]));
//						
//						Log.d(TAG, String.format("compFilterQuat = {%f, %f, %f, %f}",
//												compFilterQuat[0],
//												compFilterQuat[1],
//												compFilterQuat[2],
//												compFilterQuat[3]));
//						
//						Log.d(TAG, String.format("compFilterMat = \n {%f, %f, %f,\n"
//										+ "%f, %f, %f,\n"
//										+ "%f, %f, %f}\n",
//										compFilterMat[0], compFilterMat[1], compFilterMat[2],
//										compFilterMat[3], compFilterMat[4], compFilterMat[5],
//										compFilterMat[6], compFilterMat[7], compFilterMat[8]));
//										
//						Log.d(TAG, String.format("xSenseOrient = {%f, %f, %f}",
//														xSenseOrient[0],
//														xSenseOrient[1],
//														xSenseOrient[2]));
//						Log.d(TAG, String.format("xSenseQuatTelFrame = {%f, %f, %f, %f}",
//																xSenseQuatTelFrame[0],
//																xSenseQuatTelFrame[1],
//																xSenseQuatTelFrame[2],
//																xSenseQuatTelFrame[3]));
//						Log.d(TAG, String.format("xSenseMat = \n {%f, %f, %f,\n"
//										+ "%f, %f, %f,\n"
//										+ "%f, %f, %f}\n",
//										xSenseMat[0], xSenseMat[1], xSenseMat[2],
//										xSenseMat[3], xSenseMat[4], xSenseMat[5],
//										xSenseMat[6], xSenseMat[7], xSenseMat[8]));
//						
//						float[] xscfDiffMat = ComplementaryFilter.matMul(compFilterMat, invOrthoNormMat(xSenseMat));
//						
//						Log.d(TAG, String.format("xscfDiffMat = \n {%f, %f, %f,\n"
//												+ "%f, %f, %f,\n"
//												+ "%f, %f, %f}\n",
//												xscfDiffMat[0], xscfDiffMat[1], xscfDiffMat[2],
//												xscfDiffMat[3], xscfDiffMat[4], xscfDiffMat[5],
//												xscfDiffMat[6], xscfDiffMat[7], xscfDiffMat[8]));
						
						float[] orientMat = new float[9];
						SensorManager.getRotationMatrixFromVector(orientMat, ComplementaryFilter.quat2rotVec(orientQuat));
						//orientMat = ComplementaryFilter.matMul(enuToNED, orientMat);
						//orientMat = ComplementaryFilter.matMul(orientMat, xSenseToTel);
						
						float[] orientOrient = new float[3];
						//SensorManager.getOrientation(orientMat, orientOrient);
						orientOrient = eulerZYX(orientMat);
						saveToStream(orientOutStream, (long)nextOrientTimestamp, orientOrient);
						
						count++;
					}
					
					prevGyroTimestamp = nextGyroTimestamp;
					if(gyroScanner.hasNextLong()){
						nextGyroTimestamp = gyroScanner.nextLong();
					}
				}
				else if(orientScanner.hasNextFloat()){
					if(orientQuat == null){
						orientQuat = new float[4];
					}
					float[] orientRotVec = new float[3];
					orientRotVec[0] = orientScanner.nextFloat();
					orientRotVec[1] = orientScanner.nextFloat();
					orientRotVec[2] = orientScanner.nextFloat();
					SensorManager.getQuaternionFromVector(orientQuat, orientRotVec);
					
					compFilter.magAccUpdate(orientQuat);
					
					// Check if need to change sign
					float noChangeSign = RMSE(orientQuat, lastAndroidQuat);
					float changeSign = RMSE(orientQuat, minus(lastAndroidQuat));
//					Log.d("AEKF", "Change sign 1: " + orientQuat[0] + " "
//							+ orientQuat[1] + " " + orientQuat[2] + " "
//							+ orientQuat[3]);
//					Log.d("AEKF", "Change sign 2: " + lastAndroidQuat[0] + " "
//							+ lastAndroidQuat[1] + " " + lastAndroidQuat[2]
//							+ " " + lastAndroidQuat[3] + " ");
					
//					float [] zzz = minus(lastAndroidQuat);
//					Log.d("AEKF", "Change sign 3: " +zzz[0] + " "
//							+ zzz[1] + " " + zzz[2] + " "
//							+ zzz[3]);
//					
//					Log.d("AEKF", "Change sign 4: " +(orientQuat[0]-zzz[0]) + " "
//							+ (orientQuat[1]-zzz[1]) + " " + (orientQuat[2] - zzz[2]) + " "
//							+ (orientQuat[3] -zzz[3]));
//					
//					float wtf = 0.0f;
//					for (int i=0;i<4;i++) {
//						wtf += ((orientQuat[i] - zzz[i])*(orientQuat[i] - zzz[i]));
//					}
//					wtf = (float) Math.sqrt(wtf);
//					Log.d("AEKF",
//							"Change sign: " + noChangeSign
//									+ " " + changeSign );
					if (noChangeSign > changeSign) {
						orientQuat = minus(orientQuat);
					}
					lastAndroidQuat = orientQuat;
					
					AEKF.correct(orientQuat[0], orientQuat[1], orientQuat[2], orientQuat[3]);
					
					if(orientScanner.hasNextLong()){
						nextOrientTimestamp = orientScanner.nextLong();
					}
				}
				
			}
			
			J2 = J2 / ((nextGyroTimestamp - firstGyroTimestamp) * ms2s);
			
			AEKFOutStream.close();
			compFilterOutStream.close();
			xSenseOutStream.close();
			orientOutStream.close();
			
			Log.d(TAG, String.format("J2 = %e", J2));
			
			return J2;
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
		}
		
		return -1;
	}
	
	
	private static float RMSE(float[] x, float [] y) {
		if (x.length != y.length) {
			return 99999.0f;
		}
		float val = 0.0f;
		for (int i=0;i<4;i++) {
			val += ((x[i] - y[i])*(x[i] - y[i]));
		}
		return (float) Math.sqrt(val);
	}
	
	private static float [] minus(float [] x) {
		float [] rtn = new float [x.length];
		for (int i=0;i<x.length;i++)
			rtn[i] = -x[i];
		return rtn;
	}
	
	
	
	// ZYX convention
	// POWINNO BYÆ: http://web.mit.edu/2.05/www/Handout/HO2.PDF
	// Ale jest 3-2-1 wed³ug Jarka Goœliñskiego ;p
	static float [] eulerZYX(float [] Mat)
	{
		float []wynik = new float[3];
		// http://web.mit.edu/2.05/www/Handout/HO2.PDF
//		if ( Math.abs(Mat[0] - Mat[3]) < 0.00001) {
//			wynik[1] = (float) (Math.PI/2);
//			wynik[0] = 0;
//			wynik[2] = (float) Math.atan2(Mat[1], Mat[4]);
//		}
//		else
//		{
//			wynik[1] = (float) Math.atan2(-Mat[6], Math.sqrt(Mat[0]*Mat[0] + Mat[3]*Mat[3]));
//			wynik[0] = (float) Math.atan2(Mat[3], Mat[0]);
//			wynik[2] = (float) Math.atan2(Mat[7], Mat[8]);
//		}
		
		float quat [] = ComplementaryFilter.mat2quat(Mat);
		float q1 = quat[0];
		float q2 = quat[1];
		float q3 = quat[2];
		float q4 = quat[3];
		
		wynik[0] = (float) Math.atan2((2*q1*q2 + 2*q3*q4), 1-2*(q2*q2+q3*q3));
		wynik[1] = (float) Math.asin(2*q1*q3-2*q4*q2);
		wynik[2] = (float) Math.atan2(2*(q1*q4 + q2*q3), 1-2*(q3*q3+q4*q4));
		return wynik;
	}
}
