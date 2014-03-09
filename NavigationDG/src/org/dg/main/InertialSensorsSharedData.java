package org.dg.main;

public class InertialSensorsSharedData {
    public boolean write_flag;
    public float [][]inertialData;
    public static InertialSensorsSharedData globalInstance = new InertialSensorsSharedData();
    
    public enum InSensor {    	
    	ACCELEROMETER, GYROSCOPE, MAGNETIC, LINEARACC, ROTATION
    }
    
    public InertialSensorsSharedData()
    {
    	inertialData = new float[5][3];
    	write_flag = false;
    }
}
	