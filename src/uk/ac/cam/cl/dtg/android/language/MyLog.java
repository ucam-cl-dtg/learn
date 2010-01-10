package uk.ac.cam.cl.dtg.android.language;

import android.util.Log;

public class MyLog{
	private static final boolean LOG_ON = false;
	
	public static void d(String tag, String msg)
	{
		if (LOG_ON)
			Log.d(tag, msg);
	}
	
	public static void e(String tag, String msg)
	{
		if (LOG_ON)
			Log.e(tag, msg);
	}
	
	public static void i(String tag, String msg)
	{
		if (LOG_ON)
			Log.i(tag, msg);
	}
	
	public static void w(String tag, String msg)
	{
		if (LOG_ON)
			Log.w(tag, msg);
	}
	
	
	
}
