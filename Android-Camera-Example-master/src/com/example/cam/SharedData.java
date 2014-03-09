package com.example.cam;

public class SharedData {
    public boolean write_flag;
    public static SharedData globalInstance = new SharedData();
    
    public SharedData()
    {
    	write_flag = false;
    }
}
	