package com.trinity.dial100Citizen;

import android.app.Application;

public class SOSApp extends Application {
    private static SOSApp instance;

	    @Override
	    public void onCreate() {
	        super.onCreate();
	        instance = this;
	    }

	    public static SOSApp getInstance() {
	        return instance;
	    }
}
