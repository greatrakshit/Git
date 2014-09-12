package com.trinity.dial100Citizen;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;

	/*			SETTINGS PAGE - ACCESSIBLE FROM OPTIONS MENU - FOR FUTURE USE
	 * */

public class SettingsActivity extends ListActivity implements OnItemSelectedListener{

	String[] settingsList;
	ArrayAdapter<String> settingsAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		settingsList = getResources().getStringArray(R.array.settings_list);
		
		settingsAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, settingsList);
		getListView().setAdapter(settingsAdapter);
		
		getListView().setOnItemSelectedListener(this);
	}
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View view, int position,
			long arg3) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
}
