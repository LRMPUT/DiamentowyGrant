package org.dg.camera;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class QRCode {
    private final String TAG = "QRCode";
    
    private Context mContext;
    
    public QRCode (Context context) {
    	mContext = context;
    }
	
	public void decode(Mat image) {
		Log.d(TAG, "creating async task");
		new DecodeAsyncTask(mContext).execute(image);
	}
	
	private class DecodeAsyncTask extends AsyncTask<Mat, Void, String> {
		
		Context context;

        /**
         * @param context
         */
        private DecodeAsyncTask(Context _context) {
        	context = _context;
        }
        
        @Override
        protected String doInBackground(Mat... image) {
            return decodeQRImage(image[0]);
        }
        

        @Override
        protected void onPostExecute(String result) {
        	Log.d(TAG, "onPostExecute : " + result);
        	Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        }
        
        private String decodeQRImage(Mat image) {
        	Log.d(TAG, "decodeQRImage in async");
    		Bitmap bMap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
    		Utils.matToBitmap(image, bMap);
    		int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];  
    		
    		//copy pixel data from the Bitmap into the 'intArray' array  
    		bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());  

    		LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

    		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    		QRCodeReader reader = new QRCodeReader();  
    		
    		//....doing the actually reading
    		Result result = null;
    		try {
    			result = reader.decode(bitmap);
    		}
    		catch (Exception e) {
    			return "Decode failed!";
    		}
    		
    		return result.toString();
    	}

      
    
	}
	
	

}
