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
