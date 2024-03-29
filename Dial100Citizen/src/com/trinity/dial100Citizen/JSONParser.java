package com.trinity.dial100Citizen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JSONParser {

	private InputStream is = null;
	private JSONObject jObj = null;
	private String jsonStr = "";


	public JSONObject getJSONFromUrl(String url) {

		url.replace(" ", "%20");
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			HttpResponse httpResponse = httpClient.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			is = httpEntity.getContent();	
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		} 
		catch(Exception e){
			e.printStackTrace();	
		}
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			jsonStr = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();	
		}
		try {
			jObj = new JSONObject(jsonStr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.d(getClass().getCanonicalName(), "JSON Parser returning object :"+jObj);
		return jObj;

	}


}
