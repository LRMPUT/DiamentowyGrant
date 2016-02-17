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

package org.dg.main;

import java.util.ArrayList;
import java.util.List;

import org.dg.openAIL.BuildingPlan;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Pair;

public class LocalizationView extends SurfaceView implements SurfaceHolder.Callback{
	private static final String moduleLogName = "LocalizationView";
	
	
	private LocalizationViewDrawThread drawThread;
	private double goalX = 0.0, goalY = 0.0;
	private boolean goalSet = false;
	
	public LocalizationView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        drawThread = new LocalizationViewDrawThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
            }
        });
	}
	
	public void setWiFiScanLocations( List<Pair<Double, Double>> _newWiFiScanLocations) {
		drawThread.setWiFiScanLocations(_newWiFiScanLocations);
	}
	
	public void setUserLocations( List<Pair<Double, Double>> _userLocations) {
		drawThread.setUserLocations(_userLocations);
	}

	public void setBuildingPlan(BuildingPlan buildingPlan_) {
		drawThread.setBuildingPlan(buildingPlan_);
	}
	
	@Override
    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        drawThread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
	@Override
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        drawThread.setRunning(true);
        drawThread.start();
    }

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		drawThread.setRunning(false);
		try {
			drawThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		goalX = event.getX();
		goalY = event.getY();
		goalSet = true;
		
//		if ( event.getRawY() < 920)
//			drawThread.increaseZoom();
//		else
//			drawThread.decreaseZoom();
//		
//		Log.d(TAG, "onTouch! Zoom = ");
		Log.d(moduleLogName, "Clicked!");
		return false;
		
	}
	
	public boolean isGoalSet() {
		return goalSet;
	}
	
	public void showBackgroundPlan (boolean showBackgroundPlan) {
		drawThread.showBackgroundPlan (showBackgroundPlan);
	}
	
	public Pair<Double, Double> getGoal() {
		goalSet = false;
		return drawThread.getMetresFromPixels(new Pair<Double, Double>(goalX, goalY));
	}

	public void setPathToGoal(List<Pair<Double, Double>> pathToGoal) {
		drawThread.setPathToGoal(pathToGoal);
	}
}
