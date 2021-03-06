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

package org.dg.inertialSensors;


import org.dg.openAIL.ConfigurationReader.Parameters;

import android.util.Log;

public class Stepometer implements Runnable{
	// NDK Connection
	// fftFindDominantFrequency -> computes the dominant frequency in the accelerometer signal passed as a parameter.
	//		The accelerometerMeasurementFrequency is used together with window size to find the frequency in Hz:
	//		dominantFrequency = indexOfMaxValueInFrequencyDomain * accelerometerMeasurementFrequency / windowSize;
	public native float fftFindDominantFrequency(float [] accWindow, float accelerometerMeasurementFrequency);
	
	// Parameters
	Parameters.InertialSensors.Stepometer parameters;
	
	// Loading the NDK library
	public Stepometer(Parameters.InertialSensors.Stepometer _parameters) {
		System.loadLibrary("StepometerModule");
		parameters = _parameters;
	}

	// Last detected frequency (in Hz)
	float lastDetectedFrequency = 0.0f;
	
	// Total number of steps and total number of distance covered
	float detectedNumberOfSteps = 0;
	float coveredStepDistance = 0.0f;
	float lastReportedCoveredStepDistance = 0.0f;
	
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
		
		if (parameters.verbose > 0)
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
		
		if (parameters.verbose > 0)
			Log.d("Stepometer", "Found frequency: " + lastDetectedFrequency + " Hz");
		
		// If the detected frequency is inside walking frequencies
		if (parameters.minFrequency <= lastDetectedFrequency
				&& lastDetectedFrequency <= parameters.maxFrequency) {
			// new distance = step of a person * frequency 
			coveredStepDistance = (float) (coveredStepDistance + parameters.stepSize * lastDetectedFrequency);
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
	
	public float getStepDistance() {
		float distance =  coveredStepDistance - lastReportedCoveredStepDistance;
		lastReportedCoveredStepDistance = coveredStepDistance;
		return distance;
	}
	
	public float getLastFoundFreq()
	{
		return lastDetectedFrequency;
	}
}
