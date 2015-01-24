package org.dg.inertialSensors;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.util.Log;

public class MagneticRecognition {

	List<List<Float>> placeDatabase = null;

	
	public MagneticRecognition() {
		placeDatabase = new ArrayList<List<Float>>();
	}
	
	public void addPlace(List<Float> magneticFingerprint)
	{
		List<Float> featuresForMagneticFingerprint = new ArrayList<Float>();
		
		computeFeatures(magneticFingerprint, featuresForMagneticFingerprint);
		
		placeDatabase.add(featuresForMagneticFingerprint);
	}

	/**
	 * @param magneticFingerprint
	 * @param featuresForMagneticFingerprint
	 */
	private void computeFeatures(List<Float> magneticFingerprint,
			List<Float> featuresForMagneticFingerprint) {
		// Mean
		float sum = 0.0f;
		for (Float x : magneticFingerprint)
			sum += x;
		float mean = sum / magneticFingerprint.size();
		featuresForMagneticFingerprint.add(mean);
		
		// Variance
		float var = 0.0f;
		for (Float x : magneticFingerprint)
			var += Math.pow(x - mean, 2);
		var = var / magneticFingerprint.size(); 
		featuresForMagneticFingerprint.add(var);
	}
		
	public int recognizePlace(List<Float> magneticFingerprint)
	{
		List<Float> featuresForMagneticFingerprint = new ArrayList<Float>();
		computeFeatures(magneticFingerprint, featuresForMagneticFingerprint);
		
		
		int bestIndex = 0, index = 0;
		Float bestValue = -1.0f;
		
		float val = computeDifference(placeDatabase.get(0), placeDatabase.get(1));
		
		Log.d("Main::Activity", "TEST value value: " + val);
		
		// Check all places in database
		for (List<Float> place : placeDatabase)
		{	
			// Computing similarity measure
			float value = computeDifference(place,  featuresForMagneticFingerprint);
			
			Log.d("Main::Activity", "Computed value: " + value);
			
			// Finding the most probable place
			if ( (value < bestValue || bestValue < 0.0f) && value >= 0.0f)
			{
				bestValue = value;
				bestIndex = index;
			}
			index = index + 1;
		}
		Log.d("Main::Activity", "Best index: " + bestIndex); 
		return bestIndex;
	}
	
	public int getSizeOfPlaceDatabase()
	{
		return placeDatabase.size();
	}
	
	private float computeDifference(List<Float> placeA, List<Float> placeB)
	{
		if (placeA.size() != placeB.size())
			return -1.0f;
		
		float difference = 0.0f;
		for(ListIterator<Float> iter = placeA.listIterator(), iter2 = placeB.listIterator(); iter.hasNext() && iter2.hasNext(); )
		{
			float element = iter.next(), element2 = iter2.next();
			difference += Math.pow((element - element2),2); 
		}
		return difference;
	}
	
}
