package org.dg.inertialSensors;


import android.util.Log;

public class Stepometer implements Runnable{
	// NDK Connection
	// fftFindDominantFrequency -> computes the dominant frequency in the accelerometer signal passed as a parameter.
	//		The accelerometerMeasurementFrequency is used together with window size to find the frequency in Hz:
	//		dominantFrequency = indexOfMaxValueInFrequencyDomain * accelerometerMeasurementFrequency / windowSize;
	public native float fftFindDominantFrequency(float [] accWindow, float accelerometerMeasurementFrequency);
	
	// Loading the NDK library
	public Stepometer() {
		System.loadLibrary("StepometerModule");
	}

	// Last detected frequency (in Hz)
	float lastDetectedFrequency = 0.0f;
	
	// Step size in meters - 0.7 m as default
	final float personalStepSize = 0.7f;
	
	// Total number of steps and total number of distance covered
	float detectedNumberOfSteps = 0;
	float coveredStepDistance = 0.0f;
	float lastReportedCoveredStepDistance = 0.0f;
	
	// The walking frequencies borders
	final float leftWalkingFrequencyBorder = 1.2f;
	final float rightWalkingFrequencyBorder = 2.8f;
	
	// Accelerometer measurement frequency (in Hz)
	final float accelerometerMeasurementFrequency = 200.0f;
	
	// accelerometer measurements to process
	float [] accWindow;
	int accWindowSize = 0;
	
	public void setAccWindow(float [] _accWindow, int _accWindowSize)
	{
		accWindow = _accWindow;
		accWindowSize = _accWindowSize;
	}
	
	@Override
	public void run() {
		
		Log.d("Stepometer", "Starting fft test, size of window : " + Integer.toString(accWindowSize));
		
		// We need to copy the data into the float array to pass into NDK
		float sum = 0.0f;
		for (int i=0;i<accWindowSize;i++)
		{
			sum = sum + accWindow[i];
		}
		sum = sum / accWindowSize;
	
		// Just remove the mean of the signal
		for (int i=0;i<accWindowSize;i++)
		{
			accWindow[i] = accWindow[i] - sum;
		}
		
		// Perform frequency detection
		lastDetectedFrequency = fftFindDominantFrequency(accWindow, accelerometerMeasurementFrequency);
		Log.d("Stepometer", "Found frequency: " + lastDetectedFrequency + " Hz");
		
		// If the detected frequency is inside walking frequencies
		if (leftWalkingFrequencyBorder <= lastDetectedFrequency
				&& lastDetectedFrequency <= rightWalkingFrequencyBorder) {
			// new distance = step of a person * frequency 
			coveredStepDistance = coveredStepDistance + personalStepSize * lastDetectedFrequency;
			detectedNumberOfSteps = detectedNumberOfSteps + lastDetectedFrequency;
		}
		
	}
	
	// Getting measured values
	public float getDetectedNumberOfSteps()
	{
		return detectedNumberOfSteps;
	}
	
	public float getCoveredStepDistance()
	{
		return coveredStepDistance;
	}
	
	public float getGraphStepDistance() {
		float distance =  coveredStepDistance - lastReportedCoveredStepDistance;
		lastReportedCoveredStepDistance = coveredStepDistance;
		return distance;
	}
	
	public float getLastFoundFreq()
	{
		return lastDetectedFrequency;
	}
}
