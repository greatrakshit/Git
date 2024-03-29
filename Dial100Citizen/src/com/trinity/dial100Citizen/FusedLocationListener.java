package com.trinity.dial100Citizen;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class FusedLocationListener implements 
						GooglePlayServicesClient.ConnectionCallbacks, 
						GooglePlayServicesClient.OnConnectionFailedListener, 
						LocationListener {
	
	   public interface LocationListener {
	        public void onReceiveLocation(Location location);
	    }

	    private LocationListener mListener;

	    public static final String TAG = "Fused";
	    private LocationClient locationClient;
	    private LocationRequest locationRequest;


	    protected int minDistanceToUpdate = 0;
	    protected int minTimeToUpdate = 0;

	    protected Context mContext;


	    @Override
	    public void onConnected(Bundle bundle) {
	    	try{
	        Log.d(TAG, "Connected");
	        locationRequest = new LocationRequest();
	        locationRequest.setSmallestDisplacement(0);
	        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	        locationRequest.setInterval(0);
	        locationRequest.setNumUpdates(1);
	        locationClient.requestLocationUpdates(locationRequest, this);
	    	} catch(Exception e){
	    		e.printStackTrace();
	    	}

	    }

	    @Override
	    public void onDisconnected() {
	        Log.d(TAG, "Disconnected");
	    }

	    @Override
	    public void onConnectionFailed(ConnectionResult connectionResult) {
	        Log.d(TAG, "Failed :"+connectionResult.getErrorCode());
	    }


	    private static FusedLocationListener instance;

	    public static synchronized FusedLocationListener getInstance(Context context, LocationListener listener){
	        if (null==instance) {
	            instance = new FusedLocationListener(context, listener);
	        }
	        return instance;
	    }


	    private FusedLocationListener(Context context, LocationListener listener){
	        mContext = context;
	        mListener = listener;
	    }


	    public void start(){

	        Log.d(TAG, "Listener started");
	        locationClient = new LocationClient(mContext,this,this);
	        locationClient.connect();

	    }


	    @Override
	    public void onLocationChanged(Location location) {
	        Log.d(TAG, "Location received: " + location.getLatitude() + ";" + location.getLongitude());
	        //notify listener with new location
	        mListener.onReceiveLocation(location);
	    }


	    public void stop() {
	        locationClient.removeLocationUpdates(this);
	    }
	
}