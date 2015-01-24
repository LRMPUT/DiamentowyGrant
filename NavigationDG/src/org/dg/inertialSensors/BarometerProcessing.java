package org.dg.inertialSensors;

public class BarometerProcessing implements Runnable {

	final float floorLvlDifference = 3.40f;
	float startingHeight;
	float currentHeight;
	
	boolean started = false;

	public BarometerProcessing() {
	}
	
	public void start(float _startingHeight) {
		startingHeight = _startingHeight;
		currentHeight = _startingHeight;
		started = true;
	}
	
	public void stop ()
	{
		started = false;
	}
	
	public boolean isStarted() {
		return started;
	}

	public void setCurrentHeight(float _currentHeight) {
		currentHeight = _currentHeight;
	}

	public float getEstimateHeight() {
		return (currentHeight - startingHeight);
	}
	
	public int getCurrentFloor() {
		if (started)
		{
			int currentFloor = Math.round((currentHeight - startingHeight) / floorLvlDifference);
			return currentFloor;
		}
		return 0;
	}

	public boolean liftDetected() {
		// TODO : implement
		return false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}
}
