package org.dg.globalSynchronization;

public class GlobalSharedData {

    public static boolean write_flag;
    public static int id;
    public static long startTimestamp;
    public static GlobalSharedData globalInstance = new GlobalSharedData();
    
    public GlobalSharedData()
    {
    	id = -1;
    	startTimestamp = 0;
    	write_flag = false;
    }
}
	