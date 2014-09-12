package com.trinity.dial100Citizen;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;



public class SOSActivity extends Activity implements FusedLocationListener.LocationListener{

	private SharedPreferences pref;
	private Editor editor;
	private String simNo;
	private com.trinity.dial100Citizen.DBAdapter mDbHelper;
	private Cursor mCursor;
	private String jsp_url;
	private String base_stn;
	public static Location sosLocation;
	//private boolean SEND_SOS_WHEN_LOCATION_IS_AVAILABLE = false;
	private SmsManager sms;
	private LocationManager locationManager;
	private TemplateLocationListener locationListener;
	private static boolean SEND_SOS_WHEN_LOCATION_IS_AVAILABLE = false;
	private static double cellIdLatitude = 0;
	private static double cellIdLongitude = 0;
	public static boolean SOS_SENT = false;
	public static Handler uiHandler;
	public static Activity sosActivityInstance;
	public static final int send_sos_if_not_sent = 48534;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sos);	
		
		//SOS_SENT = false;
		Log.d(getClass().getCanonicalName(), "SOS_SENT SET FALSE");
			
		uiHandler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				Log.d(getClass().getCanonicalName(), "MESSAGE received by UI from service.");
				super.handleMessage(msg);
				switch(msg.what){
				case send_sos_if_not_sent:
					Log.d(getClass().getCanonicalName(), "MESSAGE ID - send_sos_if_not_sent");
					if(msg.obj!=null) {
						Log.d(getClass().getCanonicalName(), "MESSAGE received by UI from service contains LOCATION. Updating cellIdLatitude & cellIdLongitude");
						String[] cellIdString = ((String) msg.obj).split(",");
						cellIdLatitude = (Double.valueOf(cellIdString[0])/ 1000000);
						cellIdLongitude =(Double.valueOf(cellIdString[1])/ 1000000);
						Log.d(getClass().getCanonicalName(), "Updated values :cellIdLatitude - "+cellIdLatitude+", cellIdLongitude - "+cellIdLongitude);
					} else {
						Log.d(getClass().getCanonicalName(), "MESSAGE received by UI from service DOES NOT contain location.");
					}
					if(SEND_SOS_WHEN_LOCATION_IS_AVAILABLE)
						sendSosIfNotSent();
					break;
				}
			}
			
		};
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pref.edit();
		
		Log.d(getClass().getCanonicalName(), "App_Status :"+pref.getString(Global.app_status, Global.app_not_registered));
		if(!pref.getString(Global.app_status, Global.app_not_registered).equals(Global.app_registered)){
			Log.d(getClass().getCanonicalName(), "App Not Registered. Redirecting to Launch Screen.");
			startActivity(new Intent(SOSActivity.this, MainActivity.class));
			finish();
		}
		
		//editor.putString("AppStatus", "LoggedIn");
		//editor.commit();
		simNo = pref.getString("SimNo", "Empty");				
		mDbHelper = new DBAdapter(this);
		mDbHelper.open();		
		mCursor = mDbHelper.fetchConfigAccounts();
		jsp_url = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONFIG_JSP_URL));
		base_stn = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONFIG_BASESTN));
		
		new GetBaseStation().execute();	
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		try {
			locationListener = new TemplateLocationListener();
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);	
			
			FusedLocationListener locationListener = FusedLocationListener.getInstance(getApplicationContext(), this);
			locationListener.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		FusedLocationListener locationListener = FusedLocationListener.getInstance(getApplicationContext(), this);
		locationListener.start();

		
		SecondaryLocationManager SLM = new SecondaryLocationManager(getApplicationContext());		
		if(SLM.canGetLocation){			
			sosLocation = SLM.getLocation();			
		}
		
		if(sosLocation==null)
			sosLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if(sosLocation==null)
			sosLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
		sms = SmsManager.getDefault();
		
		sosActivityInstance = this;

	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		confirmLocationServicesIsEnabled();
		
		if(pref.getString(Global.contacts_status, Global.contacts_unedited).equals(Global.contacts_edited)){
			new UpdateContactsOnServer().execute();
		}
	}
	
	
	public class TemplateLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
					sosLocation = location;
					if(SEND_SOS_WHEN_LOCATION_IS_AVAILABLE)
							sendSosIfNotSent();
		}	

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			switch (status) {
			case LocationProvider.AVAILABLE:
				break;
			case LocationProvider.OUT_OF_SERVICE:
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				break;
			default:
				break;
			}
		}
	}
	
	private void confirmLocationServicesIsEnabled() {
		// TODO Auto-generated method stub
		 LocationManager lm = null;
		 boolean gps_enabled = false,network_enabled = false;
		    if(lm==null)
		        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		    try{
		    	gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		    } catch(Exception ex){
		    	ex.printStackTrace();
		    }
		    try{
		    	network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		    } catch(Exception ex){
		    	ex.printStackTrace();
		    }

		   if(!gps_enabled && !network_enabled){
		        Builder dialog = new AlertDialog.Builder(this);
		        dialog.setMessage(getResources().getString(R.string.location_services_disabled));
		        dialog.setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
		                // TODO Auto-generated method stub
		                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		                startActivity(myIntent);
		                //get gps
		            }
		        });
		        dialog.setNegativeButton(getString(R.string.exit_application), new DialogInterface.OnClickListener() {

		            @Override
		            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
		                // TODO Auto-generated method stub
		            	finish();
		            }
		        });
		        dialog.show();

		    }
	}


	@Override
	public void onReceiveLocation(Location location) {
		// TODO Auto-generated method stub
		sosLocation = location;
		Log.d(getClass().getCanonicalName(), "Location Received by SOSActivity");
		if(SEND_SOS_WHEN_LOCATION_IS_AVAILABLE)
			sendSosIfNotSent();
	}

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(getClass().getSimpleName(), "SOSActivity DESTROYED");
//		try{
//			if(SOSService.sosServiceInstance!=null)
//				SOSService.sosServiceInstance.resetAndStopService();
//		} catch(Exception e){
//			e.printStackTrace();
//		}		
	}

	private void sendSosIfNotSent() {
		// TODO Auto-generated method stub
		if(!SOS_SENT){
			if(sosLocation!=null || (cellIdLatitude!=0 & cellIdLongitude!=0)){
				Log.d(getClass().getCanonicalName(), "SENDING ALERT because SOS SENT - FALSE");
				sendSOStoPolice();
				sendSOStoContacts();			
				SOS_SENT = true;
				SEND_SOS_WHEN_LOCATION_IS_AVAILABLE = false;
				Log.d(getClass().getCanonicalName(), "SOS_SENT set as TRUE. SEND_SOS_WHEN_LOCATION_IS_AVAILABLE set as FALSE");
			} else {
				SEND_SOS_WHEN_LOCATION_IS_AVAILABLE  = true;
				Log.d(getClass().getCanonicalName(), "sosLocation is NULL. SEND_SOS_WHEN_LOCATION_IS_AVAILABLE  = true. Waiting for location...");
			}
		} else {
			Log.d(getClass().getCanonicalName(), "NOT SENDING ALERT because SOS HAS ALREADY BEEN SENT. i.e SOS SENT - TRUE");
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case R.id.sos_contacts:
			startActivity(new Intent(SOSActivity.this, AddContactsList.class));
			break;
/*		case R.id.sos_settings:
			startActivity(new Intent(SOSActivity.this, SettingsActivity.class));
			break;*/
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void startSendingSOS(View v){
		if(!SOS_SENT)
			startSendingSOS();
		else
			Toast.makeText(getApplicationContext(), "Dial100 App has already been activated", Toast.LENGTH_LONG).show();
	}
	
	public void startSendingSOS() {
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(2000);
		sendSosIfNotSent();
		startTrackingService();
		moveTaskToBack(true);
		((ImageView)findViewById(R.id.imageButtonSos)).setBackgroundResource(R.drawable.sos_icon_green);	
	}

	public void sendSOStoPolice(){
		String num = pref.getString("basestation", "");
		String txtContent = null;
		if(sosLocation!=null)
			txtContent = Global.POLICE_CODE + sosLocation.getLatitude() + "," + sosLocation.getLongitude();
		else if(cellIdLatitude!=0 && cellIdLongitude!=0)
			txtContent = Global.POLICE_CODE + Double.valueOf(cellIdLatitude) + "," + Double.valueOf(cellIdLongitude);
			
		if(!num.contains("+91"))
			num = "+91" + num.trim();
		sms.sendTextMessage(num, null, txtContent, null, null);
		Log.d(getLocalClassName(), txtContent+" sent to POLICE - "+num);
		deleteSms();
	}
	
	public void sendSOStoContacts(){
		
		Cursor cursor = mDbHelper.fetchContacts();
		String txtContent = null;
		if(sosLocation!=null)
			txtContent = "I am in an Emergency. View my location on Google Maps (within "+ sosLocation.getAccuracy()+" metres)"+
				"http://maps.google.com/maps?q="+sosLocation.getLatitude()+","+sosLocation.getLongitude();		
		else if(cellIdLatitude!=0 && cellIdLongitude!=0)
			txtContent = "I am in an Emergency. View my location on Google Maps "+
					"http://maps.google.com/maps?q="+Double.valueOf(cellIdLatitude)+","+Double.valueOf(cellIdLongitude);		

		cursor.moveToFirst();
		if (cursor.getCount() > 0) {
			do {
				String num = cursor.getString(cursor.getColumnIndex(DBAdapter.CONTACTS_MOB));
				if(!num.contains("+91"))
					num = "+91" + num.trim();
				
				sms.sendTextMessage(num, null,txtContent, null, null);
				Log.d(getLocalClassName(), txtContent+" sent to CONTACT - "+num);
				new getAddressInBackground(num).execute();
			} while(cursor.moveToNext());
		}
		//mDbHelper.close();		
	}
	
	public void startTrackingService(){
		if(pref.getString("SOSStatus", "NotSent").equals("NotSent")){
			startService(new Intent(this,SOSService.class));
		}
		else{
			ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
				if (!SOSService.class.getName().equals(service.service.getClassName())) {
					startService(new Intent(this,SOSService.class));
				}
			}
		}
	}
	
	private class UpdateContactsOnServer extends AsyncTask<Void, Void, String>{		
		private String newName1 = "";
		private String newName2 = "";
		private String newContact1 = "";
		private String newContact2 = "";

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mCursor = mDbHelper.fetchContacts();
			mCursor.moveToFirst();
			int i = 0;
			if(mCursor.getCount()>0){
				do {
					if(i ==0){
						newName1 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_NAME));
						newContact1 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_MOB));
					}
					if(i ==1){
						newName2 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_NAME));
						newContact2 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_MOB));
					}
					i++;
				} while(mCursor.moveToNext());
			}
		}

		@Override
		protected String doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			String url = jsp_url+"update_contacts.jsp?Number="+simNo+"&Name1="+newName1.replace(" ", "%20")+"&Name2="+newName2.replace(" ", "%20")+"&Contact1="+newContact1+"&Contact2="+newContact2;
			JSONParser jParser=new JSONParser();
			JSONObject json=jParser.getJSONFromUrl(url);
			String value = "Failed";
			try
			{	
				if(json == null){
					value  = "Failed";
				}
				if(json.has("Result")){
					value = json.getString("Result");
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			Log.d(getClass().getCanonicalName(), "Update Contacts Result :"+value);
			return value;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result.equals("Success"))
				editor.putString(Global.contacts_status, Global.contacts_unedited).commit();
		}	
		
		
	}
	private class getAddressInBackground extends AsyncTask<Void, Void, String>{

		String messageContent;
		String num;
		
		public getAddressInBackground(String num) {
			// TODO Auto-generated constructor stub
			this.num = num;
		}
		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String result = "failure";
			Log.d(getClass().getCanonicalName(), "FETCHING ADDRESS FROM GOOGLE");
			try {
				String url = null;
				if(sosLocation!=null){
					Log.d(getClass().getCanonicalName(), "USING sosLocation to fetch address");
					url = "http://maps.googleapis.com/maps/api/geocode/json?latlng="+sosLocation.getLatitude()+","+sosLocation.getLongitude()+"&sensor=true";
				}
				else if(cellIdLatitude!=0 & cellIdLongitude!=0){
					Log.d(getClass().getCanonicalName(), "USING CellID to fetch address");
					url = "http://maps.googleapis.com/maps/api/geocode/json?latlng="+cellIdLatitude+","+cellIdLongitude+"&sensor=true";
				}
				
				JSONParser jParser=new JSONParser();
				JSONObject json=jParser.getJSONFromUrl(url);	
				JSONArray jArray = json.getJSONArray("results");
				JSONObject jObject = jArray.getJSONObject(0);
				String location = jObject.getString("formatted_address");
				System.out.println("LOC:"+location.toString());
				messageContent = "I am in an Emergency. My address is "+location;
				result = "success";
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result.equals("success")){
				if (num.length() > 9) {
					SmsManager sms = SmsManager.getDefault();
					sms.sendTextMessage(num, null,messageContent, null, null);
					Log.d("MyTag", messageContent+" sent to "+num);
					deleteSms();
					//Toast.makeText(getApplicationContext(), "Address sent to SOS contact :"+num, Toast.LENGTH_LONG).show();
				} 
			} else {
				//Toast.makeText(getApplicationContext(), "Failed to fetch address", Toast.LENGTH_LONG).show();
				Log.d("MyTag", "Failed to fetch address");
			}
		}	
	}

	private void deleteSms(){
		try {
			Uri uri = Uri.parse("content://sms/sent");
			Uri uriSms = uri;

			Cursor c = getContentResolver().query(uriSms, null, null, null,null);

			if (c.moveToNext()) {

				int id = c.getInt(0);
				int thread_id = c.getInt(1);
				getContentResolver().delete(
						Uri.parse("content://sms/conversations/"+ thread_id),
						"thread_id=? and _id=?",new String[] { String.valueOf(thread_id),String.valueOf(id)});
			}
		} catch (Exception e) {
			finish();
			e.printStackTrace();
			
		}
	}

	public class GetBaseStation extends AsyncTask<String, Void, String> {

		private String value;

		@Override
		protected String doInBackground(String... params) {
			//String url = "http://220.227.96.177:8081/SOS/base_station.jsp";
			String url = jsp_url+"base_station.jsp";

			JSONParser jParser = new JSONParser();
			JSONObject json = jParser.getJSONFromUrl(url);
			System.out.println("json=" + json);
			try {
				if (json == null) {
					value = "Failure";
				}
				else if (json.has("Value")) {
					value = json.getString("Value");

				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return value;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			try {
				if (!result.contains("Failure")) {
					String config[] = value.split(" ");
					Log.d(getClass().getCanonicalName(), "Basestation :"+config[0]+", IP :"+config[1]+", Port :"+config[2]);
					editor.putString("basestation", config[0]);					
					editor.putString("IP", config[1]);
					editor.putString("Port", config[2]);
					editor.commit();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
		@Override
		public void onBackPressed() {
			if(SOS_SENT)
				moveTaskToBack(true);
			else
				finish();
		}
	
		// Before 2.0
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
		    if (keyCode == KeyEvent.KEYCODE_BACK) {
		    	if(SOS_SENT)
		    		moveTaskToBack(true);
		    	else
		    		finish();
		        return true;
		    }
		    return super.onKeyDown(keyCode, event);
		}
	
}
