package com.trinity.dial100Citizen;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;


public class SecondaryLocationManager implements LocationListener {

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;
   
    boolean isNetworkEnabled = false;
    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0; // 10 sec

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public SecondaryLocationManager(Context context) {
        this.mContext = context;
        getLocation();
    }

    @SuppressLint("ServiceCast")
	public Location getLocation() {
        try {
        	Log.v("LOCATION CALLED", "MYLOCATION");
            locationManager = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            
            TelephonyManager telMgr = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
            int simState = telMgr.getSimState();
            
            Log.v("SIM STATE", ""+simState);
            
            if(simState > 1){            	
    			isNetworkEnabled =true;
    		}
            
            Log.v("IS NETWORK ENABLED", ""+isNetworkEnabled);
            // getting network status

           
            this.canGetLocation = true;

            // if DATA Enabled get lat/long using WIFI Services
                    
            // if GPS Enabled get lat/long using GPS Services
          if(isGPSEnabled || isNetworkEnabled){
        	  
                if (location == null) {
				
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (locationManager != null) {
					
                    	if(location !=null){
                    		
                               location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                               Log.v("GPSSSSSSS", ""+location);
							   
                    	} else{
						
								locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
								location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                Log.v("Netwokkkkkk", ""+location);

                               }

                    }
                }
            }else{             	
                    location =null;                   
                }
                   	
            	
            
            if (location != null) {
            	
            	Log.v("LOCATION",""+ location);
                latitude = location.getLatitude();
                longitude = location.getLongitude();
               
                Log.v("LATITUDE-------------", ""+latitude);
                Log.v("LONGITUDE------------", ""+longitude);
                
            }else{
            	
            
            	Log.v("LOCATION","NULL");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(SecondaryLocationManager.this);
        }       
    }

    /**
     * Function to get latitude
     * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
            Log.v("GetLatiude : ", ""+latitude);

        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
            Log.v("GetLongitude : ", ""+longitude);

        }

        // return longitude
        return longitude;
    }

    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

 

}

