package com.trinity.dial100Citizen;

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class StopSOSActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

	    //Remove notification bar
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_stop_sos);
	}
	
	public void cancel(View v){
		finish();
	}
	
	public void stopDial100(View v){
		
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(500);
		
		try{
			SOSService.sosServiceInstance.sendStopSosPacket();
		} catch(Exception e){
			e.printStackTrace();
		}				
		
//		Intent stopService = new Intent(this, SOSService.class);
//		stopService(stopService);		
		finish();
	}
}
