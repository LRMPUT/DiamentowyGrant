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

package org.dg.camera;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class Preview extends ViewGroup implements SurfaceHolder.Callback, PreviewCallback  {
    private final String TAG = "Camera::Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera = null; 
    
    ReentrantLock curPreviewImageLock = new ReentrantLock();
	
	Mat curPreviewImage = null;


    public Preview(Context context, SurfaceView sv) {
        super(context);

        mSurfaceView = sv;
       
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    // Just test
    public Preview(SurfaceView sv) {
        super(sv.getContext());

        mSurfaceView = sv;
       
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	Log.d(TAG, "Tap detected");
    	return true;
    }

    public void setCamera(Camera camera) {
//    	Log.d(TAG, String.format("Preview::setCamera"));
    	mCamera = camera;
    	if (mCamera != null) {
//        	Log.d(TAG, String.format("Preview::setCamera, mCamera != null"));
    		mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
    		requestLayout();

    		// get Camera parameters
    		Camera.Parameters params = mCamera.getParameters();

    		//List<String> focusModes = params.getSupportedFocusModes();
    		/*if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
    			// set the focus mode
    			params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
    			// set Camera parameters
    			mCamera.setParameters(params);
    		}*/
//    		params.setPictureSize(640, 480);
//    		params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
    		
    		//mCamera.setDisplayOrientation(90);
    		mCamera.setParameters(params);
    	}
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//    	Log.d(TAG, "onMeasure");
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        	Log.d(TAG, String.format("mPreviewSize = (%d, %d)", mPreviewSize.width, mPreviewSize.height));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
//    	Log.d(TAG, "surfaceCreated");
        try {
            if (mCamera != null) {
//            	Log.d(TAG, "mCamera != null");
            	
//        		Camera.Parameters parameters = mCamera.getParameters();
//        		parameters.setPreviewSize(306, 163);
            	
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.3;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	Log.d(TAG, "surfaceChanged");
    	if(mCamera != null) {
//        	Log.d(TAG, "mCamera != null");
        	
        	//need to stop - otherwise setParameters will fail when changing previewSize
            mCamera.stopPreview();
            
    		Camera.Parameters parameters = mCamera.getParameters();
    		parameters.setPreviewSize(640, 480);
    		
    //		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//    		parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    		requestLayout();

    		mCamera.setParameters(parameters);
    		mCamera.setPreviewCallback(this);
    		mCamera.startPreview();
    		
    	}
    }
    
    @Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
    	Camera.Parameters parameters = mCamera.getParameters();
    	
		int imageFormat = parameters.getPreviewFormat(); 		   
		if (imageFormat == ImageFormat.NV21)
		{
//			Log.d(TAG, "imageFormat == ImageFormat.NV21");
//			Log.d(TAG, String.format("data.length = %d", data.length));
    	   
			Camera.Size prevSize = parameters.getPreviewSize();
			
//			Log.d(TAG, String.format("preview size = (%d, %d)", prevSize.width, prevSize.height));
			
			Mat imageBGRA = new Mat();
			Mat imageYUV = new Mat(prevSize.height + prevSize.height / 2, prevSize.width, CvType.CV_8UC1);
			imageYUV.put(0,  0, data);
			
			Imgproc.cvtColor(imageYUV, imageBGRA, Imgproc.COLOR_YUV420sp2BGR, 4);
    	   
			curPreviewImageLock.lock();
			
			try {

				curPreviewImage = imageBGRA;
				
			} finally {
			//						Log.d(TAG, "onPreviewFrame finally");
				curPreviewImageLock.unlock();
			}
    	   
       }

	}
    
    public Mat  getCurPreviewImage() {
		Mat ret = null;
		
		curPreviewImageLock.lock();
		
		try {
			if(curPreviewImage != null){
				ret = curPreviewImage.clone();
			}
		} finally {
//			Log.d(TAG, "getCurPreviewImage finally");
			curPreviewImageLock.unlock();
		}
		
		return ret;
    }
	

}
