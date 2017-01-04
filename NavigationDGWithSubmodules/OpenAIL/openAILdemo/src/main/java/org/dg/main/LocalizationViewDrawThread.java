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
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;

public class LocalizationViewDrawThread extends Thread {
	private static final String TAG = "drawThread";
	
	// Stores information about building plan image and corridors
	public BuildingPlan buildingPlan = new BuildingPlan();

	// Some const of drawing
	private static final float drawSize = 25.0f;

	// Used to lock canvas
	private SurfaceHolder mSurfaceHolder;

	// If the drawing thread should be running
	private boolean mRun = true;

	/** The drawable to use as the background of the animation canvas */
	private Bitmap mBackgroundImageDraw = null, mLegend = null; 
	
	/**
	 * Current height of the surface/canvas.
	 * 
	 * @see #setSurfaceSize
	 */
	private int mCanvasHeight = 1;

	/**
	 * Current width of the surface/canvas.
	 * 
	 * @see #setSurfaceSize
	 */
	private int mCanvasWidth = 1;

	// Current user poses and known WiFi scans
	private List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
	private List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();
	private List<Pair<Double, Double>> pathToGoal = new ArrayList<Pair<Double, Double>>();

	// Zoom
	private double zoom = 1.0;

	// Scales used to convert from px to metres and px to px in case of resizing image
	private double mapPixels2Metres = 1;
	private double backgroundResizedPx2OriginalPx = 1;

	// Parameters - drawingFrequency in Hz
	private double drawingFrequency = 2;
	
	// Informs if estimates are w.r.t map
	private boolean showBackgroundPlan = false;
	
	LocalizationViewDrawThread(SurfaceHolder surfaceHolder, Context context,
			Handler handler) {
		Log.d(TAG, "Created thread");

		mSurfaceHolder = surfaceHolder;

		// Legend
		Resources res = context.getResources();
		mLegend = BitmapFactory.decodeResource(res, R.drawable.legend);

	}

	@Override
	public void run() {
		while (mRun) {
			Canvas c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					doDraw(c);
				}
			} finally {
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}

			// We sleep 500 ms
			try {
				sleep( (long) (1000.0 / drawingFrequency), 0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setBuildingPlan(BuildingPlan buildingPlan_) {
		buildingPlan = buildingPlan_;
		mapPixels2Metres=buildingPlan_.oldMapPixels2Metres;
		backgroundResizedPx2OriginalPx = buildingPlan_.backgroundResizedPx2OriginalPx;
		mBackgroundImageDraw = buildingPlan_.mBackgroundImage;
		refreshDrawingSizes();
	}

	public void increaseZoom() {
		if ( zoom < 10.0 )
		{
			zoom += 0.1; 
			refreshDrawingSizes();
		}
	}
	
	public void decreaseZoom() {
		if ( zoom > 0.1 )
		{
			zoom -= 0.1; 
			refreshDrawingSizes();
		}
	}
	
	
	public void refreshDrawingSizes() {
		if (buildingPlan.mBackgroundImage != null) {
			double widthScale = (double) (mCanvasWidth * zoom)
					/ buildingPlan.mBackgroundImage.getWidth();
			double heightScale = (double) (mCanvasHeight * zoom)
					/ buildingPlan.mBackgroundImage.getHeight();

			double minimumScale = Math.min(widthScale, heightScale);

			int wid = (int) (minimumScale * buildingPlan.mBackgroundImage
					.getWidth());
			int hei = (int) (minimumScale * buildingPlan.mBackgroundImage
					.getHeight());

			backgroundResizedPx2OriginalPx = minimumScale;

			Log.d(TAG, "setSurfaceSize: old="
					+ buildingPlan.mBackgroundImage.getWidth() + " new="
					+ wid + " scale=" + minimumScale);

			// Update
			mapPixels2Metres = buildingPlan.oldMapPixels2Metres
					* backgroundResizedPx2OriginalPx;

			mBackgroundImageDraw = Bitmap.createScaledBitmap(
					buildingPlan.mBackgroundImage, wid, hei, true);
		}

		mLegend = Bitmap.createScaledBitmap(mLegend, mCanvasWidth,
				mLegend.getHeight() * mCanvasWidth / mLegend.getWidth(), true);
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (mSurfaceHolder) {
			mCanvasWidth = width;
			mCanvasHeight = height;	
		}
		refreshDrawingSizes();
	}

	// New WiFi Scan Locations
	public void setWiFiScanLocations(
			List<Pair<Double, Double>> _newWiFiScanLocations) {
		synchronized (wifiScanLocations) {
			wifiScanLocations.clear();
			for (Pair<Double, Double> item : _newWiFiScanLocations) {
				Pair<Double, Double> tmp = new Pair<Double, Double>(item.first,
						item.second);
				wifiScanLocations.add(tmp);
			}
		}
	}

	// New user locations
	public void setUserLocations(List<Pair<Double, Double>> _userLocations) {
		synchronized (userLocations) {
			userLocations.clear();
			for (Pair<Double, Double> item : _userLocations) {
				Pair<Double, Double> tmp = new Pair<Double, Double>(item.first,
						item.second);
				userLocations.add(tmp);
			}
		}
	}
	
	public void setPathToGoal(List<Pair<Double, Double>> pathToGoalList) {
		synchronized (pathToGoal) {
			pathToGoal.clear();
			for (Pair<Double, Double> item : pathToGoalList) {
				Pair<Double, Double> tmp = new Pair<Double, Double>(item.first,
						item.second);
				pathToGoal.add(tmp);
			}
			
		}
		
	}

	public void setRunning(boolean b) {
		mRun = b;
	}
	
	public void showBackgroundPlan (boolean _showBackgroundPlan) {
		showBackgroundPlan = _showBackgroundPlan;
	}
	
	public Pair<Double, Double> getMetresFromPixels(Pair<Double, Double> pixels) {
		double x = (pixels.first - buildingPlan.originX * backgroundResizedPx2OriginalPx) / mapPixels2Metres;
		double y = (pixels.second - buildingPlan.originY * backgroundResizedPx2OriginalPx) / mapPixels2Metres;
		return new Pair<Double, Double>(x, y);
	}

	/*
	 * Method called in each iteration to redraw the scene
	 */
	private void doDraw(Canvas canvas) {
		Log.d(TAG, "Called draw - wifiLocations: " + wifiScanLocations.size()
				+ " userLocations: " + userLocations.size());

		Log.d(TAG, "backgroundResizedPx2OriginalPx: "
				+ backgroundResizedPx2OriginalPx);
		Log.d(TAG, "mapPixels2Metres: " + mapPixels2Metres);

		// Clearing the canvas
		canvas.drawColor(Color.BLACK);

		// Draw the background image.
		if (mBackgroundImageDraw != null && showBackgroundPlan)
			canvas.drawBitmap(mBackgroundImageDraw, 0, 0, null);

		// Draw the legend
		canvas.drawBitmap(mLegend, canvas.getWidth() - mLegend.getWidth(),
				canvas.getHeight() - mLegend.getHeight(), null);

		// Draw the building plan origin
		float centerX = (float) (buildingPlan.originX * backgroundResizedPx2OriginalPx);
		float centerY = (float) (buildingPlan.originY * backgroundResizedPx2OriginalPx);
		// drawX(canvas, (float) centerX, (float) centerY, drawSize, 255, 0, 0);

		// Draw the nodes in the buildingPlan
		for (Node n : buildingPlan.nodeLocations) {
			float drawX = (float) ((n.getPx() * backgroundResizedPx2OriginalPx));
			float drawY = (float) ((n.getPy() * backgroundResizedPx2OriginalPx));
			drawCircle(canvas, drawX, drawY, drawSize / 3, 255, 0, 0);
			Log.d(TAG, "nodeLocations: " + drawX + " & " + drawY);
		}

		// Draw the edges in the buildingPlan
		for (Edge e : buildingPlan.edgeLocations) {
			float firstX = (float) (e.from.getPx() * backgroundResizedPx2OriginalPx);
			float firstY = (float) (e.from.getPy() * backgroundResizedPx2OriginalPx);
			float secondX = (float) (e.to.getPx() * backgroundResizedPx2OriginalPx);
			float secondY = (float) (e.to.getPy() * backgroundResizedPx2OriginalPx);
			drawLine(canvas, firstX, firstY, secondX, secondY, 255, 0, 0);
		}
		
		// Draw known WiFi scan locations as red X
		synchronized (wifiScanLocations) {

			for (Pair<Double, Double> p : wifiScanLocations) {
				float drawX = (float) (p.first * mapPixels2Metres + centerX);
				float drawY = (float) (p.second * mapPixels2Metres + centerY);

				drawX(canvas, drawX, drawY, drawSize, 255, 0, 0);
			}
		}
		
		// Draw navigation plan
		synchronized (pathToGoal) {
			boolean firstLocation = true;
			float prevDrawX = 0.0f, prevDrawY = 0.0f;
			for (Pair<Double, Double> p : pathToGoal) {
				float drawX = (float) (p.first * mapPixels2Metres + centerX);
				float drawY = (float) (p.second * mapPixels2Metres + centerY);

				drawCircle(canvas, drawX, drawY, drawSize/2, 0, 255, 0);

				if (!firstLocation) {
					drawLine(canvas, prevDrawX, prevDrawY, drawX, drawY, 0,
							255, 0, 4);
				} else
					firstLocation = false;

				prevDrawX = drawX;
				prevDrawY = drawY;
			}

		}

		// Draw locations as blue circles connected by blue lines
		synchronized (userLocations) {
			boolean firstLocation = true;
			float prevDrawX = 0.0f, prevDrawY = 0.0f;
			for (Pair<Double, Double> p : userLocations) {
				float drawX = (float) (p.first * mapPixels2Metres + centerX);
				float drawY = (float) (p.second * mapPixels2Metres + centerY);

				drawCircle(canvas, drawX, drawY, drawSize, 99, 209, 244);

				if (!firstLocation) {
					drawLine(canvas, prevDrawX, prevDrawY, drawX, drawY, 99,
							209, 244);
				} else
					firstLocation = false;

				prevDrawX = drawX;
				prevDrawY = drawY;
			}
		}
		
		

	}

	/**
	 * Draws a line on canvas between points (x1,y1) and (x2,y2) with color
	 * (r,g,b)
	 */
	private void drawLine(Canvas canvas, float x1, float y1, float x2,
			float y2, int r, int g, int b) {
		drawLine(canvas, x1, y1, x2, y2, r, g, b, 0);
	}
	
	/**
	 * Draws a line on canvas between points (x1,y1) and (x2,y2) with color
	 * (r,g,b)
	 */
	private void drawLine(Canvas canvas, float x1, float y1, float x2,
			float y2, int r, int g, int b, int strokeWidth) {
		canvas.save();

		Paint mLinePaint = new Paint();
		mLinePaint.setAntiAlias(true);
		mLinePaint.setARGB(255, r, g, b);
		mLinePaint.setStrokeWidth(strokeWidth);

		canvas.drawLine(x1, y1, x2, y2, mLinePaint);

		canvas.restore();
	}

	/**
	 * Draws circle on canvas at (x,y) with radius xSize with color (r,g,b)
	 */
	private void drawCircle(Canvas canvas, float x, float y, float xSize,
			int r, int g, int b) {
		canvas.save();

		Paint mCirclePaint = new Paint();
		mCirclePaint.setAntiAlias(true);
		mCirclePaint.setARGB(255, r, g, b);

		canvas.drawCircle(x, y, xSize / 2, mCirclePaint);

		canvas.restore();
	}

	/**
	 * Draws a "X" on canvas at (x,y) with xSize with color (r,g,b)
	 */
	private void drawX(Canvas canvas, float x, float y, float xSize, int r,
			int g, int b) {
		canvas.save();
		canvas.translate(x, y);

		Paint mXPaint = new Paint();
		RectF mRect = new RectF();
		mXPaint.setAntiAlias(true);
		mXPaint.setARGB(255, r, g, b);

		canvas.rotate(45);
		mRect.set(-0.71f * xSize, -0.16f * xSize, 0.71f * xSize, 0.16f * xSize);
		canvas.drawRect(mRect, mXPaint);
		mRect.set(0.16f * xSize, 0.71f * xSize, -0.16f * xSize, -0.71f * xSize);
		canvas.drawRect(mRect, mXPaint);

		canvas.restore();
	}

	

}