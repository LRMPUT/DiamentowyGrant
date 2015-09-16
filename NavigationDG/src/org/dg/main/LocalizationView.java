package org.dg.main;


import java.util.ArrayList;
import java.util.List;

import org.dg.openAIL.IdPair;

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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Pair;

public class LocalizationView extends SurfaceView implements SurfaceHolder.Callback{
	
	class DrawThread extends Thread{
		private static final String TAG = "drawThread";
		
		private static final float drawSize = 25.0f;
		
		private SurfaceHolder mSurfaceHolder;
		private Handler mHandler;
		private Context mContext;
		
		private boolean mRun = true;
		
        /** The drawable to use as the background of the animation canvas */
        private Bitmap mBackgroundImage;
		
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
		private RectF mScratchRect;
		private Paint mLinePaint;
		
		/**
		 * 
		 * @param surfaceHolder
		 * @param context
		 * @param handler
		 */
		private List<Pair<Double, Double>> wifiScanLocations = new ArrayList<Pair<Double, Double>>();
		private List<Pair<Double, Double>> userLocations = new ArrayList<Pair<Double, Double>>();
		
		float scale = 0.0f;
		float centerX = 0.0f, centerY = 0.0f;
		
		DrawThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler)
        {
			Log.d(TAG, "Created thread");
			
			// get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;
            
        	Resources res = context.getResources();
            mBackgroundImage = BitmapFactory.decodeResource(res,
                    R.drawable.cmbin_rotated2);
            
            
            mScratchRect = new RectF(0, 0, 0, 0);
            
            mLinePaint = new Paint();
            mLinePaint.setAntiAlias(true);
            mLinePaint.setARGB(255, 0, 0, 255);
                    
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
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
				
				try {
					sleep(400, 0);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		 /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
                
				recomputeDrawingScale();
				
	            double widthScale = (double) (width) / mBackgroundImage.getWidth();
	            double heightScale = (double) (height) / mBackgroundImage.getHeight();
	            
	            double minimumScale = Math.min(widthScale, heightScale);
	            
	            int wid = (int) (minimumScale * mBackgroundImage.getWidth());
	            int hei = (int) (minimumScale * mBackgroundImage.getHeight()); 
	            
	           
                mBackgroundImage = Bitmap.createScaledBitmap(
                        mBackgroundImage, wid, hei, true);
            }
        }

		/**
		 * 
		 */
		private void recomputeDrawingScale() {
			// Find the interval of changes for X and Y
			double minX = 0.0, maxX = 0.0, minY = 0.0, maxY = 0.0;
			scale = 1.0f;
			boolean start = true;
			synchronized (wifiScanLocations) {
				for (Pair<Double, Double> p : wifiScanLocations) {
					if (p.second < minX || start)
						minX = p.second;
					if (p.second > maxX || start)
						maxX = p.second;

					if (p.first < minY || start)
						minY = p.first;
					if (p.first > maxY || start)
						maxY = p.first;

					start = false;
				}
			}

//			synchronized (userLocations) {
//				for (Pair<Double, Double> p : userLocations) {
//					if (p.second < minX || start)
//						minX = p.second;
//					if (p.second > maxX || start)
//						maxX = p.second;
//
//					if (p.first < minY || start)
//						minY = p.first;
//					if (p.first > maxY || start)
//						maxY = p.first;
//
//					start = false;
//				}
//			}

			Log.d(TAG, "X: " + minX + " " + maxX);
			Log.d(TAG, "Y: " + minY + " " + maxY);

			// Computing the scale of drawing
//			float scaleX = mCanvasWidth / (float) (maxX - minX);
//			float scaleY = mCanvasHeight / (float) (maxY - minY);

			//scale = (float) (Math.min(scaleX, scaleY) / 1.4);
			
			float unknownX = (float) ((maxX - minX) /  (664.0-248.0) * 1049.0);
			float unknownY = (float) ((maxY - minY) / (2360.0-509.0) * 3049.0);
			
			float scaleX = Math.abs(mCanvasWidth / (float) (unknownX));
			float scaleY = Math.abs(mCanvasHeight / (float) (unknownY));
			scale = (float) (Math.min(scaleX, scaleY));
			
			centerX = (float) ((maxX + minX) / 2) + 67.0f/1049.0f * unknownX;
			centerY = (float) ((maxY + minY) / 2) + 90.0f/3049.0f * unknownY; //- 39.0f/1049.0f * unknownY;
				
		}
		
		public void setWiFiScanLocations(List<Pair<Double, Double>> _newWiFiScanLocations)
		{
			synchronized (wifiScanLocations) {
				wifiScanLocations.clear();
				for(Pair<Double, Double> item: _newWiFiScanLocations) 
				{
					Pair<Double, Double> tmp = new Pair<Double, Double>(item.first, item.second);
					wifiScanLocations.add( tmp );
				}
			}
			recomputeDrawingScale();
		}
		
		public void setUserLocations(List<Pair<Double, Double>> _userLocations)
		{
			synchronized (userLocations) {
				userLocations.clear();
				for(Pair<Double, Double> item: _userLocations) 
				{
					Log.d(TAG, "User locations : " + item.first + " "  + item.second);
					Pair<Double, Double> tmp = new Pair<Double, Double>(item.first, item.second);
					userLocations.add( tmp );
				}
			}
			recomputeDrawingScale();
		}
		
		
		private void doDraw(Canvas canvas) {
			Log.d(TAG, "Called draw - wifiLocations: " + wifiScanLocations.size() + " userLocations: " + userLocations.size());
			
			canvas.drawColor(Color.BLACK);
			
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			int left = (mCanvasWidth - mBackgroundImage.getWidth())/2;
			int top = (mCanvasHeight - mBackgroundImage.getHeight())/2;
			
			canvas.drawBitmap(mBackgroundImage, left, top, null);
			         
           
			
			
//            drawX(canvas, 700, 700, xSize, 255, 0, 0);
//            drawLine(canvas, 800, 600, 800, 800, 0, 0, 255);
//            drawCircle(canvas, 800, 800, xSize, 0, 0, 255);
            
			
				// Draw known WiFi scan locations as red X
				synchronized (wifiScanLocations) {
					
					for (Pair<Double, Double> p : wifiScanLocations) {
						float drawX = (float) ((p.second - centerX) * scale + mCanvasWidth / 2);
						float drawY = (float) ((p.first - centerY) * (-scale) + mCanvasHeight / 2);
						
						drawX(canvas, drawX, drawY, drawSize, 255, 0, 0);
					}
				}

				// Draw locations as blue circles connected by blue lines
				synchronized (userLocations) {
					boolean firstLocation = true;
					float prevDrawX = 0.0f, prevDrawY = 0.0f;
					for (Pair<Double, Double> p : userLocations) {
						float drawX = (float) ((p.second - centerX) * scale + mCanvasWidth / 2);
						float drawY = (float) ((p.first - centerY) * (-scale) + mCanvasHeight / 2);

						// Log.d(TAG, "Called drawCircle: " + p.first + " " +
						// p.second + " | " + drawX + " " + drawY);
						drawCircle(canvas, drawX, drawY, drawSize, 99, 209, 244);

						if (!firstLocation) {
							drawLine(canvas, prevDrawX, prevDrawY, drawX,
									drawY, 99, 209, 244);
						} else
							firstLocation = false;

						prevDrawX = drawX;
						prevDrawY = drawY;
					}
				}
			
		}
		
		private void drawLine(Canvas canvas, float x1, float y1, float x2, float y2, int r, int g, int b) {
			canvas.save();
			
            Paint mLinePaint = new Paint();
            mLinePaint.setAntiAlias(true);
            mLinePaint.setARGB(255, r, g, b);
            
            canvas.drawLine(x1, y1, x2, y2, mLinePaint);

            canvas.restore();
		}
	
		/**
		 * @param canvas
		 * @param xSize
		 */
		private void drawCircle(Canvas canvas, float x, float y, float xSize, int r, int g, int b) {
			canvas.save();
			
            Paint mCirclePaint = new Paint();
            mCirclePaint.setAntiAlias(true);
            mCirclePaint.setARGB(255, r, g, b);
            
            canvas.drawCircle(x, y, xSize/2, mCirclePaint);

            canvas.restore();
		}

		/**
		 * @param canvas
		 * @param xSize
		 */
		private void drawX(Canvas canvas, float x, float y, float xSize, int r, int g, int b) {
			canvas.save();
			canvas.translate(x, y);
			
            Paint mXPaint = new Paint();
            RectF mRect = new RectF();
            mXPaint.setAntiAlias(true);
            mXPaint.setARGB(255, r, g, b);
            
            canvas.rotate(45);
            mRect.set(-0.71f*xSize, -0.16f*xSize, 0.71f*xSize, 0.16f*xSize);
            canvas.drawRect(mRect, mXPaint);
            mRect.set(0.16f*xSize, 0.71f*xSize, -0.16f*xSize, -0.71f*xSize);
            canvas.drawRect(mRect, mXPaint);
            
            canvas.restore();
		}
		
		 /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

	}
	
	
	private DrawThread drawThread;
	
	public LocalizationView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        drawThread = new DrawThread(holder, context, new Handler() {
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

}
