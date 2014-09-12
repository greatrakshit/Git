package com.trinity.dial100Citizen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class SOSService extends Service implements FusedLocationListener.LocationListener{
	private static final String TRACKING_TASK = "org.trinity.sosapp.TRACKING_TASK";
	private static final String SEND_PKT_TASK = "org.trinity.sosapp.SEND_PKT_TASK";
	private static final String SOCKET_RECREATE_TASK = "org.trinity.sosapp.SOCKET_RECREATE_TASK";
	private static final String LISTENING_TASK = "org.trinity.sosapp.LISTENING_TASK";
	private IntentFilter trackFilter,sendPktFilter,recreateSocketFilter,listenFilter;
	private Intent trackIntent,sendPktIntent,recreateSocketIntent,listenIntent;
	private PendingIntent trackPI,sendPktPI,recreateSocketPI,listenPI;
	private AlarmManager trackAlarm,sendPktAlarm,recreateSocketAlarm,listenAlarm;
	private boolean flag;
	private SocketConn requestSocket;
	private Socket getSocket;
	private boolean result = false;
	private DBAdapter mDbHelper;
	private Cursor mCursor;
	private long gpsTimeInSec;
	private Date gpsDate ; 
	private String replyMsgId;
	private Calendar calendar;
	private SimpleDateFormat dateformatter,timeformatter;
	private String storedPacket,gpsTimeNow,gpsDateNow,dateNow,timeNow,batteryStatus;
	private BroadcastReceiver alarmReceiver;
	private Editor editor;
	private Location mLocation;
	private int cId, lac,batteryLevel,strengthAmplitude,myLatitude=0,myLongitude=0,locAreaCode,cellId;
	private String simNo;
	private TelephonyManager tm;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private SharedPreferences pref;
	double sentLat = 0;
	double sentLon = 0;
	private static NotificationManager mNotificationManager;
	public static SOSService sosServiceInstance;
	@Override
	public void onCreate() {
		super.onCreate();		
	
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
		
		SecondaryLocationManager ml = new SecondaryLocationManager(getApplicationContext());
		
		if(ml.canGetLocation){
			
			mLocation = ml.getLocation();
			try{
				SOSActivity.sosLocation = mLocation; 
			} catch(Exception e){
				e.printStackTrace();
			}
			
			
		}
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pref.edit();
		editor.putString("SOSStatus", "Sent");
		editor.commit();	
		simNo = pref.getString("SimNo", "Empty");
		mDbHelper = new DBAdapter(this);
		mDbHelper.open();
		requestSocket = new SocketConn();
		try{
			if(checkInternetConnection())  
			{	
				
				getSocket = requestSocket.createSocket();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		dateformatter = new SimpleDateFormat("ddMMyy",Locale.getDefault());
		timeformatter = new SimpleDateFormat("HH:mm:ss",Locale.getDefault());

		trackIntent = new Intent(TRACKING_TASK);
		trackPI = PendingIntent.getBroadcast(getApplicationContext(), 0, trackIntent, 0);
		trackAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 2);
		trackAlarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),10000, trackPI);		

		sendPktIntent = new Intent(SEND_PKT_TASK);
		sendPktPI = PendingIntent.getBroadcast(getApplicationContext(), 0, sendPktIntent, 0);
		sendPktAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND,5);
		sendPktAlarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),3000, sendPktPI);		

		recreateSocketIntent = new Intent(SOCKET_RECREATE_TASK);
		recreateSocketPI = PendingIntent.getBroadcast(getApplicationContext(), 0, recreateSocketIntent, 0);
		recreateSocketAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE,2);
		calendar.add(Calendar.SECOND, 1);
		recreateSocketAlarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 121000, recreateSocketPI);		

		listenIntent = new Intent(LISTENING_TASK);
		listenPI = PendingIntent.getBroadcast(getApplicationContext(), 0, listenIntent, 0);
		listenAlarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 1);
		listenAlarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),3000, listenPI);		

		alarmReceiver = new BroadcastReceiver() {		        

			@Override
			public void onReceive(Context context, Intent intent)
			{
				if(intent.getAction().equals(TRACKING_TASK)){
					callTracking();		         
				} else if(intent.getAction().equals(SEND_PKT_TASK)){
					callSendingPkt();
				} else if(intent.getAction().equals(SOCKET_RECREATE_TASK)){
					callRecreatingSocket();		
				} else if(intent.getAction().equals(LISTENING_TASK)){
					callListening();	
				}
			}
		};
		trackFilter = new IntentFilter(TRACKING_TASK);
		registerReceiver(alarmReceiver, trackFilter);
		sendPktFilter = new IntentFilter(SEND_PKT_TASK);
		registerReceiver(alarmReceiver, sendPktFilter);
		recreateSocketFilter = new IntentFilter(SOCKET_RECREATE_TASK);
		registerReceiver(alarmReceiver, recreateSocketFilter);
		listenFilter = new IntentFilter(LISTENING_TASK);
		registerReceiver(alarmReceiver, listenFilter);

		notifyUser();
		
		sosServiceInstance = this;
	}
	
	private void notifyUser() {
		// TODO Auto-generated method stub
//		NotificationCompat.Builder mBuilder =
//		        new NotificationCompat.Builder(this)
//		        .setSmallIcon(R.anim.notification_blinker)
//		        .setContentTitle("Dial100 Activated")
//		        .setContentText("Your location is being tracked. Click to stop.");
//		Intent result = new Intent(this, StopSOSActivity.class);
//		
//		TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
//		stackBuilder.addParentStack(StopSOSActivity.class);
//		stackBuilder.addNextIntent(result);
//		
//		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//		mBuilder.setContentIntent(resultPendingIntent);
//		mBuilder.setOngoing(true);
//		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//		mNotificationManager.notify(Global.notification_id, mBuilder.build());
		
	    Intent intent = new Intent(this, StopSOSActivity.class);
	    PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

	    Notification noti = new Notification.Builder(this)
	        .setContentTitle("Dial100 Activated")
	        .setContentText("Your location is being tracked. Click to stop.").setSmallIcon(R.anim.notification_blinker)
	        .setContentIntent(pIntent).build();
	    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    noti.flags |= Notification.FLAG_ONGOING_EVENT;

	    mNotificationManager.notify(Global.notification_id, noti);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public class TemplateLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
					mLocation = location;
					Log.d(getClass().getCanonicalName(), "Location received by TemplateLocationListener onLocationChanged");
					//setSOSActivityLocationIfRequired(location);
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

	public void callTracking(){

		pingUiToSendSosIfRequired();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try
				{						
					calendar = Calendar.getInstance();
					dateNow = dateformatter.format(calendar.getTime());
					timeNow = timeformatter.format(calendar.getTime());

					batteryStatus = "00";
					batteryLevel = 40;
					strengthAmplitude = 15;

					if (mLocation != null)		
					{						 
						gpsTimeInSec = mLocation.getTime();
						gpsDate = new Date(gpsTimeInSec);
						gpsTimeNow = timeformatter.format(gpsDate);		
						gpsDateNow = dateformatter.format(gpsDate);
						{				
	
 					storedPacket = "MTS,01,0099," + simNo + ",NM,GP,"+strengthAmplitude+","+  timeNow + ",A,"+ mLocation.getLatitude() + ",N,"
							+ mLocation.getLongitude() + ",E,"+ mLocation.getSpeed() * 3.6 + ",69.13,"
							+ dateNow + ",3072,00,4000,"+batteryStatus+","+batteryLevel+",90,0650";

							sentLat = mLocation.getLatitude();
							sentLon = mLocation.getLongitude();
							mDbHelper.createFwMsgAccount(storedPacket, dateNow+ " " + timeNow, "0");
							//mLocation = null;
							storedPacket=null;
						}
					}
					else
					{	
						if(checkInternetConnection()){
							GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();
							cId = location.getCid();
							lac = location.getLac();
							result = RqsLocation(cId, lac);	

							if(result){
								
								Log.d(getClass().getCanonicalName(), "mLocation is NULL. Sending cell ID Location FOR TRACKING PACKET");

  								storedPacket = "MTS,01,0099," + simNo + ",NM,GP,"+strengthAmplitude+","+  timeNow + ",A,"+ (Double.valueOf(myLatitude)/ 1000000) + ",N,"
								+ (Double.valueOf(myLongitude)/ 1000000) + ",E,0,69.13,"
								+ dateNow + ",3072,00,4000,"+batteryStatus+","+batteryLevel+",90,0650";

								mDbHelper.createFwMsgAccount(storedPacket, dateNow+ " " + timeNow, "0");	
								storedPacket=null;
							}
							else{

								  storedPacket = "MTS,01,0099," + simNo + ",NM,GP,"+strengthAmplitude+","+ timeNow + ",V,,,,,,,"+ dateNow + ",3072,"+batteryStatus+",,11,"+batteryLevel+",090,0650";

								mDbHelper.createFwMsgAccount(storedPacket, dateNow+ " " + timeNow, "0");	
								storedPacket=null;
							}
						}
						else
						{
							 storedPacket = "MTS,01,0099," + simNo + ",NM,GP,"+strengthAmplitude+","+ timeNow + ",V,,,,,,,"+ dateNow + ",3072,"+batteryStatus+",,11,"+batteryLevel+",090,0650";
							
							mDbHelper.createFwMsgAccount(storedPacket, dateNow+ " " + timeNow, "0");	
							storedPacket=null;
						}				
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}	
			}
		}).start();		
	}
	
	public void sendStopSosPacket(){
		calendar = Calendar.getInstance();
		dateNow = dateformatter.format(calendar.getTime());
		timeNow = timeformatter.format(calendar.getTime());	

		storedPacket = "VTS,05,0068," + simNo + ",000069" + ","
				+ "null" + "," + "MSG67" + "," + dateNow + "," + timeNow + ","
				+ "0.0" + "," + "0.0";

		try {
			fwSendToRemote(storedPacket, getSocket,null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		resetAndStopService();
	}
//	public void setSOSActivityLocationkIfRequired(Location location) {
//		// TODO Auto-generated method stub
//		Log.d(getClass().getCanonicalName(), "Checking whether SOSActivity.sosLocation is NULL.");
//		//Setting sosLocation for SOSActivity in case it is null
//		try {
//			if(SOSActivity.sosLocation==null){
//				Log.d(getClass().getCanonicalName(), "SOSActivity.sosLocation IS NULL. Setting SOSActivity.sosLocation from ApplicationService.");
//				SOSActivity.sosLocation = location;
//			} else {
//				Log.d(getClass().getCanonicalName(), "SOSActivity.sosLocation NOT NULL.");
//			}
//		} catch(Exception e){
//			e.printStackTrace();
//		}
//	}

	private void pingUiToSendSosIfRequired() {
		// TODO Auto-generated method stub
		Log.d(getClass().getCanonicalName(), "Pinging UI to send SOS if required");
		try{
			if(!SOSActivity.SOS_SENT){
				Message msg = new Message();
				msg.what = SOSActivity.send_sos_if_not_sent;
				if(myLatitude!=0 && myLongitude!=0)
					msg.obj = myLatitude+","+myLongitude;
				SOSActivity.uiHandler.sendMessage(msg);
			} else {
				Log.d(getClass().getName(), "Not pinging UI because SOS_SENT already sent. i.e SOS_SENT is true.");
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public void callListening(){
		new Thread(new Runnable() {

			@Override
			public void run() {
				try
				{						
					if (getSocket.isConnected() == true)
					{
						DataInputStream datainputstream;
						try 
						{					
							if(getSocket.getInputStream()!= null){
								datainputstream = new DataInputStream(getSocket.getInputStream());
								if (datainputstream.available() > 0)
								{
									byte buffer[] = new byte[datainputstream.available()];
									datainputstream.read(buffer);
									String message = new String(buffer);

									System.out.println("Message =  "+message);
									if (message.contains("VTS06"))
									{
										message.trim();
										message = message.substring(message.indexOf("VTS06") + 6,message.indexOf("$"));
										calendar = Calendar.getInstance();
										dateNow = dateformatter.format(calendar.getTime());
										timeNow = timeformatter.format(calendar.getTime());
										//mDbHelper.createFwMsgAccount("VTS,03,0024,RX," + simNo + ","+ replyMsgId + ",1234",dateNow + " " + timeNow, "0");
										if(checkInternetConnection() && getSocket.isConnected()) 	
										{
											if(getSocket.getOutputStream()!= null)
											{ 
												java.io.PrintWriter pw = new java.io.PrintWriter(getSocket.getOutputStream());
												pw.println("VTS,03,0024,RX," + simNo + ","+ replyMsgId + ",1234");
												pw.flush();
//												stopTracking();
//												
//												editor.putString("SOSStatus", "NotSent");
//												editor.commit();
//												trackAlarm.cancel(trackPI);
//												sendPktAlarm.cancel(sendPktPI);
//												listenAlarm.cancel(listenPI);
//												recreateSocketAlarm.cancel(recreateSocketPI);
//												unregisterReceiver(alarmReceiver);											
//												stopSelf();
//												System.exit(0);
												
												resetAndStopService();
												
											} 
										}
									} 
									else if(message.contains("VTS05"))
									{
										calendar = Calendar.getInstance();
										dateNow = dateformatter.format(calendar.getTime());
										timeNow = timeformatter.format(calendar.getTime());
										replyMsgId = message.substring(5,message.length()-6);	
										mDbHelper.createFwMsgAccount("VTS,03,0024,RT," + simNo + "," + replyMsgId + ",1234", dateNow + " " + timeNow, "0");				
									}
								}
							}
						}
						catch(SocketException e)
						{				 
							if(checkInternetConnection()) 	
							{
								getSocket = requestSocket.createSocket();									
							}
							e.printStackTrace();
						}
						catch (NullPointerException e)
						{
							e.printStackTrace();					
						}
						catch (ArrayIndexOutOfBoundsException e)
						{
							e.printStackTrace();
						}

						catch (Exception e)
						{
							e.printStackTrace();
						}
					} 

					else
					{
						getSocket = requestSocket.createSocket();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}	
			}
		}).start();	
	}
	
	public void stopTracking(){
		editor.putString("SOSStatus", "NotSent");
		editor.commit();
		trackAlarm.cancel(trackPI);
		sendPktAlarm.cancel(sendPktPI);
		listenAlarm.cancel(listenPI);
		recreateSocketAlarm.cancel(recreateSocketPI);
		unregisterReceiver(alarmReceiver);
		stopSelf();
		System.exit(0);
	}

	public void callSendingPkt() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try     
				{			
					mCursor = mDbHelper.fetchFwMessageAccounts();
					if (mCursor.moveToFirst())
					{
						do
						{	
							String fwRowId = mCursor.getString(mCursor.getColumnIndex(DBAdapter.ROW_ID));					
							String storedMsg = mCursor.getString(mCursor.getColumnIndex(DBAdapter.DESCRIPTION));
							if(checkInternetConnection()) 	
							{
								if(flag == true)
								{																
									getSocket = requestSocket.createSocket();
									fwSendToRemote(storedMsg, getSocket,fwRowId);
									flag = false;
								}
								else
								{
									fwSendToRemote(storedMsg, getSocket,fwRowId);
								}

							}
						}				
						while (mCursor.moveToNext());
					}				
				}
				catch(Exception e)
				{
					e.printStackTrace();
					flag = true;
				}	
			}
		}).start();	
	}
	
	public void fwSendToRemote(String text, Socket socket,String rowId) throws IOException
	{
		try
		{		
			if(checkInternetConnection() && socket.isConnected()) 	
			{
				if(socket.getOutputStream()!= null)
				{ 
					System.out.println("Packet -- "+text);
					java.io.PrintWriter pw = new java.io.PrintWriter(socket.getOutputStream());
					pw.println(text);
					pw.flush();
					mDbHelper.deleteFwMessageAccounts(rowId);
					//notifyUser();
				} 
			}			 
		} 			
		catch(SocketException e)
		{				 
			if(checkInternetConnection()) 	
			{
				getSocket = requestSocket.createSocket();									
			}
			e.printStackTrace();
		}
		catch(SocketTimeoutException e)
		{
			if(checkInternetConnection()) 	
			{
				getSocket = requestSocket.createSocket();									
			}
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{				
			if(checkInternetConnection()) 	
			{
				getSocket = requestSocket.createSocket();									
			}
			e.printStackTrace();
		}
		catch (Exception e)
		{				
			e.printStackTrace();
		}
	}

	public void callRecreatingSocket(){
		new Thread(new Runnable() {

			@Override
			public void run() {

				try 
				{
					if(checkInternetConnection()) 	
					{
						if(getSocket.isConnected())
						{
							requestSocket.closeSocket();
						}
						getSocket = requestSocket.createSocket();		
					}
				} 

				catch (NullPointerException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}
	private boolean checkInternetConnection() {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
			return  true;
		} else {
			return  false;
		}
	}

	protected boolean RqsLocation(int cid, int lac) {
		cellId = cid;
		locAreaCode = lac;

		String urlmmap = "http://www.google.com/glm/mmap";

		try {
			URL url = new URL(urlmmap);
			URLConnection conn = url.openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setRequestMethod("POST");
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			httpConn.connect();

			OutputStream outputStream = httpConn.getOutputStream();
			WriteData(outputStream, cellId, locAreaCode);

			InputStream inputStream = httpConn.getInputStream();
			DataInputStream dataInputStream = new DataInputStream(inputStream);

			dataInputStream.readShort();
			dataInputStream.readByte();
			int code = dataInputStream.readInt();
			if (code == 0) {
				myLatitude = dataInputStream.readInt();
				myLongitude = dataInputStream.readInt();			
				result = true;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private void WriteData(OutputStream out, int cid, int lac) throws IOException {
		DataOutputStream dataOutputStream = new DataOutputStream(out);
		dataOutputStream.writeShort(21);
		dataOutputStream.writeLong(0);
		dataOutputStream.writeUTF("en");
		dataOutputStream.writeUTF("Android");
		dataOutputStream.writeUTF("1.0");
		dataOutputStream.writeUTF("Web");
		dataOutputStream.writeByte(27);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(3);
		dataOutputStream.writeUTF("");

		dataOutputStream.writeInt(cid);
		dataOutputStream.writeInt(lac);

		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.writeInt(0);
		dataOutputStream.flush();
	}
	@Override
	public void onReceiveLocation(Location location) {
		// TODO Auto-generated method stub
		mLocation = location;
		Log.d(getClass().getCanonicalName(), "Location received by FusedLocationListener onReceiveLocation");
		//setSOSActivityLocationIfRequired(location);
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//resetAndStopService();
	}

	public void resetAndStopService() {
		
		// TODO Auto-generated method stub	
		Log.d(getClass().getCanonicalName(), "resetAndStopService");
		
		mNotificationManager.cancel(Global.notification_id);	
		try{
			SOSActivity.SOS_SENT = false;
		}catch(Exception e){
			e.printStackTrace();
		}
		try{
			SOSActivity.sosActivityInstance.finish();
		}catch(Exception e){
			e.printStackTrace();
		}		
		stopTracking();
	}
	
	
}