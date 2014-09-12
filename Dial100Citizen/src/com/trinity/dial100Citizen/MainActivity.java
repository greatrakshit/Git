package com.trinity.dial100Citizen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import com.trinity.dial100Citizen.R;

public class MainActivity extends Activity {

	TextView textViewRegister;
	private SharedPreferences pref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
		setContentView(R.layout.activity_main);
		
		textViewRegister = (TextView)findViewById(R.id.textViewRegister);
		textViewRegister.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(MainActivity.this, RegisterActivity.class));
			}
		});
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		if(pref.getString(Global.app_status, Global.app_not_registered).equals(Global.app_registered)){
			finish();
		}
	}
	
	
}
