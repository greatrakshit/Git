package com.trinity.dial100Citizen;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SocketConn {
	private java.net.Socket requestSocket;
	private SharedPreferences prefs;
	private String ipaddress,portNo; 
	
	public Socket createSocket()
	{	
		prefs = PreferenceManager.getDefaultSharedPreferences(SOSApp.getInstance());
		ipaddress  = prefs.getString("IP", "Nothing");		
		portNo= prefs.getString("Port", "Nothing");
	
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Log.d("Socket", "Connecting to " + ipaddress + ":"+ Integer.parseInt(portNo));
					requestSocket = new java.net.Socket(ipaddress,Integer.parseInt(portNo));
					Log.d("Socket", "Connected to " + ipaddress + ":"+ Integer.parseInt(portNo));
				} 
				catch (UnknownHostException e) {
					e.printStackTrace();
				} 
				catch (ConnectException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}	
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return requestSocket;
	}	

	public void closeSocket() throws IOException  
	{
		requestSocket.close();
	}
}
