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
	private static final String TAG = "LocalizationView";
	
	
	
	
	
	private LocalizationViewDrawThread drawThread;
	
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
		drawThread.buildingPlan = buildingPlan_;
		drawThread.mapPixels2Metres=buildingPlan_.oldMapPixels2Metres;
		drawThread.backgroundResizedPx2OriginalPx = buildingPlan_.backgroundResizedPx2OriginalPx;
		drawThread.mBackgroundImageDraw = buildingPlan_.mBackgroundImage;
		drawThread.refreshDrawingSizes();
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
		if ( event.getRawY() < 920)
			drawThread.zoom += 0.1; 
		else
			drawThread.zoom -= 0.1; 
		drawThread.refreshDrawingSizes();
		Log.d(TAG, "onTouch! Zoom = " + drawThread.zoom);
		return false;
		
	}
}
