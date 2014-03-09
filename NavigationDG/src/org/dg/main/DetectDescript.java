package org.dg.main;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.crypto.spec.DESKeySpec;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.utils.Converters;

import android.os.Environment;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class DetectDescript implements Runnable {

	int priority = 0;
	int detectorType = 0;
	Mat image;

	// values between 0 and 10
	public void setPriority(int value) {
		priority = value;
	}

	@Override
	public void run() {
		android.os.Process.setThreadPriority(0);

		File root = Environment.getExternalStorageDirectory();
		VisualOdometry vo = new VisualOdometry();

		FileOutputStream outStream;
		String nl = System.getProperty("line.separator");
		
		int setSize = 100;
		int startTime, difference;
		try {
			
			int[] detectors = { FeatureDetector.FAST, FeatureDetector.STAR,
					FeatureDetector.SIFT, FeatureDetector.SURF,
					FeatureDetector.ORB, /* FeatureDetector.MSER,*/
					FeatureDetector.GFTT, FeatureDetector.HARRIS /*,
					FeatureDetector.DENSE, FeatureDetector.SIMPLEBLOB*/ };

			int[] descriptors = { DescriptorExtractor.SIFT,
					DescriptorExtractor.SURF, DescriptorExtractor.ORB,
					DescriptorExtractor.BRIEF, DescriptorExtractor.BRISK,
					DescriptorExtractor.FREAK };
			
			// Creating detectors and descriptors
			FeatureDetector detector[] = new FeatureDetector[detectors.length];
			MatOfKeyPoint keypoints = new MatOfKeyPoint();
			for (int j = 0; j < detectors.length; j++) {
				detector[j] = FeatureDetector.create(detectors[j]);
			}
			
			DescriptorExtractor extractor[] = new DescriptorExtractor[descriptors.length];
			Mat desc = new Mat();
			for (int j = 0; j < descriptors.length; j++) {
				extractor[j] = DescriptorExtractor.create(descriptors[j]);
			}
			
			// For all descriptors
			for (int k = 5; k < 6; k++) {
				String fi = String.format("%04d_desc_java", k);
				File file = new File(root, "_exp/desk/res/" + fi + ".txt");
				outStream = new FileOutputStream(file.getAbsolutePath());
				DataOutputStream desc_java = new DataOutputStream(outStream);
				
				fi = String.format("%04d_desc_nat", k);
				file = new File(root, "_exp/desk/res/" + fi + ".txt");
				outStream = new FileOutputStream(file.getAbsolutePath());
				DataOutputStream desc_nat = new DataOutputStream(outStream);
				
				
				// For all detectors
				for (int j = 0; j < 1/*detectors.length*/; j++) {

					DataOutputStream det_java = null;
					DataOutputStream det_nat = null;
					if ( k==0 )
					{
						fi = String.format("%04d_det_java", j);
						file = new File(root, "_exp/desk/res/" + fi + ".log");
						outStream = new FileOutputStream(file.getAbsolutePath());
						det_java = new DataOutputStream(outStream);
						
						fi = String.format("%04d_det_nat", j);
						file = new File(root, "_exp/desk/res/" + fi + ".log");
						outStream = new FileOutputStream(file.getAbsolutePath());
						det_nat = new DataOutputStream(outStream);
					}

					// For all images
					double sumdesk = 0, imga = 0;
					double srdesk = 0;
					for (int i = 1; i < setSize; i++) {

						// Read image
						String name = String.format("%04d.png", i);
						file = new File(root, "_exp/desk/" + name);
						Mat img = Highgui.imread(file.getAbsolutePath());

						// JAVA part
						startTime = (int) System.currentTimeMillis();
							detector[j].detect(img, keypoints);
						difference = (int) (System.currentTimeMillis() - startTime);

						
						
						int siz = (keypoints.toList()).size();
						
						
						//Log.d("DetDes", "Detector " + j + " : " + siz
						//		+ " time:" + difference + " | ");
						//if ( k == 0)
						//{
						//	det_java.writeUTF(Integer.toString(difference) + "\t"
						//			+ Integer.toString(siz) + nl);
						//}

						startTime = (int) System.currentTimeMillis();
							extractor[k].compute(img, keypoints, desc);
						difference = (int) (System.currentTimeMillis() - startTime);
						
						//sumdesk += ( difference * 1.0 / siz );
						imga++;

						//Log.d("DetDes", "Extractor " + k + " : " + difference);
						//desc_java.writeUTF(Integer.toString(difference) + "\t"+ Integer.toString(siz));
						//desc_java.writeUTF(nl);

						//
						// NDK
						//
						//startTime = (int) System.currentTimeMillis();
						//	int a = vo.detect(img, detectors[j]);
						//difference = (int) (System.currentTimeMillis() - startTime);
						//
						//if (k==0)
						//{
						//	det_nat.writeUTF(Integer.toString(a));
						//	det_nat.writeUTF(nl);
						//}
						
						//int czas_det = a;
						//Log.d("DetDes", "Native Detector " + j + " : "
						//		+ difference + " | " + a);

						//startTime = (int) System.currentTimeMillis();
						//int ile = vo.descriptFeatures(img.getNativeObjAddr(),
						//		detectors[j], descriptors[k]);
						
						//int ile = vo.SiftDescription(img.getNativeObjAddr(), 0);
						//difference = (int) (System.currentTimeMillis()
						//		- startTime);

						//sumdesk += ( ile * 1.0 /* siz*/ );
						//srdesk += ( ile * 1.0 / siz );
						//Log.e("DetDes", "Native extractor worked: "
						//		+ siz + " == " + ile);
						
						//desc_nat.writeUTF(Integer.toString(czas_det) + "\t" +  ile + "\t" +  Integer.toString(difference) + 
						//		"\t" + siz);
						//desc_nat.writeUTF(nl);
						

					}
					
					if ( k== 0)
					{
						det_java.close();
						det_nat.close();
					}
					
					Log.e("DetDes", "Stats: "
							+ sumdesk/imga);
					
					Log.e("DetDes", "Sred: "
							+ srdesk/imga);
					
				}
				
				
				desc_java.close();
				desc_nat.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.e("TEST", "Ended computation");

	}

}
