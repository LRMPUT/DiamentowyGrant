package com.example.cam;

public class SharedData {
    public static boolean write_flag;
    public static int id;
    public static long startTimestamp;
    public static SharedData globalInstance = new SharedData();
    
    public SharedData()
    {
    	id = -1;
    	startTimestamp = 0;
    	write_flag = false;
    }
}
	