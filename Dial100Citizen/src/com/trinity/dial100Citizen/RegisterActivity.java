package com.trinity.dial100Citizen;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.trinity.dial100Citizen.R;

public class RegisterActivity extends Activity implements OnCheckedChangeListener{

	TextView textViewAddContacts;
	EditText editTextName, editTextPostalAddress, editTextMobileNumber;
	CheckBox checkBoxTC1, checkBoxTC2, checkBoxTC3, checkBoxTC4 ;
	Button buttonOk;
	private DBAdapter mDbHelper;
	private Cursor mCursor;
	private String jsp_url;
	private String base_stn;
	private SharedPreferences pref;
	private Editor editor;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		buttonOk = (Button)findViewById(R.id.buttonOk);
		buttonOk.setEnabled(false);
		
		editTextName = (EditText)findViewById(R.id.editTextName);
		editTextPostalAddress = (EditText)findViewById(R.id.editTextPostalAddress);
		editTextMobileNumber = (EditText)findViewById(R.id.editTextMobileNumber);
		
		checkBoxTC1 = (CheckBox)findViewById(R.id.checkBoxTC1);
		checkBoxTC2 = (CheckBox)findViewById(R.id.checkBoxTC2);
		checkBoxTC3 = (CheckBox)findViewById(R.id.checkBoxTC3);
		checkBoxTC4 = (CheckBox)findViewById(R.id.checkBoxTC4);
		
		checkBoxTC1.setOnCheckedChangeListener(this);
		checkBoxTC2.setOnCheckedChangeListener(this);
		checkBoxTC3.setOnCheckedChangeListener(this);
		checkBoxTC4.setOnCheckedChangeListener(this);
		
		textViewAddContacts = (TextView)findViewById(R.id.textViewAddContacts);
		textViewAddContacts.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(new Intent(RegisterActivity.this, AddContactsList.class));
			}
		});
		
		mDbHelper = new DBAdapter(this);
		mDbHelper.open();
		mCursor=mDbHelper.fetchConfigAccounts();
		jsp_url=mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONFIG_JSP_URL));
		base_stn=mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONFIG_BASESTN));
		//mDbHelper.close();
		System.out.println("jsp_url=="+jsp_url);
		System.out.println("base_stn=="+base_stn);
		
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pref.edit();
	}
	
	public void finish(View v){
		finish();
	}
	
	public void okay(View v){
			if(enteredDetailsAreFine())
				new RegisterUser(this).execute();
	}

	public void startSosActivity(View v){
		startActivity(new Intent(RegisterActivity.this, SOSActivity.class));
	}
	
	public boolean enteredDetailsAreFine(){
		
		if(editTextName.getText().toString().trim().length()<2){
			Toast.makeText(getApplicationContext(), "Please enter a valid name", Toast.LENGTH_LONG).show();
			return false;
		}
		
		if(editTextMobileNumber.getText().toString().trim().length()<10){
			Toast.makeText(getApplicationContext(), "Please enter a valid mobile number", Toast.LENGTH_LONG).show();
			return false;
		}
		
		if(editTextPostalAddress.getText().toString().trim().length()<2){
			Toast.makeText(getApplicationContext(), "Please enter a valid address", Toast.LENGTH_LONG).show();
			return false;
		}
		
		mCursor=mDbHelper.fetchContacts();
		mCursor.moveToFirst();
		
		if(mCursor.getCount()<2){
			Toast.makeText(getApplicationContext(), "Please enter atleast 2 contacts", Toast.LENGTH_LONG).show();
			return false;
		}
		
		
		return true;
		
	}
	
	public class RegisterUser extends AsyncTask<Void, Void, String>{

		Context context;
		ProgressDialog pg;
		
		String name1, contact1, name2, contact2;
		private String userNumber;
		
		public RegisterUser(Context context){
			this.context = context;
		}
		

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			pg.dismiss();	
			try{
				if(result.contains("Success")){
					editor.putString("SimNo",userNumber);
					editor.commit();
					Toast.makeText(getApplicationContext(), "User registered successfully", Toast.LENGTH_SHORT).show();
					Intent i = new Intent(getApplicationContext(),SOSActivity.class);
					editor.putString(Global.app_status, Global.app_registered).commit();
					startActivity(i);
					finish();
				} else if(result.contains("Failure")){
					Toast.makeText(getApplicationContext(), "Failed to register application.", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(getApplicationContext(), "Unable to reach server. Please try after sometime.", Toast.LENGTH_LONG).show();
				}

			} catch(Exception e){
				e.printStackTrace();
			}
		}


		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			try{
				pg = new ProgressDialog(context);
				pg.setMessage("Registering Dial100 Application. Please Wait.");
				pg.show();
			} catch(Exception e){
				e.printStackTrace();
			}
			
			try{
				mCursor=mDbHelper.fetchContacts();
				mCursor.moveToFirst();
				for(int i=0;i<2;i++){
					if(i ==0){
						name1 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_NAME));
						contact1 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_MOB));
						mCursor.moveToNext();
					}
					if(i ==1){
						name2 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_NAME));
						contact2 = mCursor.getString(mCursor.getColumnIndex(DBAdapter.CONTACTS_MOB));
					}
				}
			} catch(Exception e){
				e.printStackTrace();
			}

		}

		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String value = null;
			userNumber = editTextMobileNumber.getText().toString().trim();
			String url=jsp_url+"sos_registration.jsp?Name="+editTextName.getText().toString().trim().replace(" ","%20")+"&City="
							+""+"&Address="+editTextPostalAddress.getText().toString().trim().replace(" ","%20")+"&Number="+userNumber+
							"&Email="+""+"&Name1="+name1.replace(" ","%20")+"&Name2="+name2.replace(" ","%20")+"&Contact1="+contact1+"&Contact2="+contact2+"&Gender=null";
			System.out.println("URL = "+url);
			JSONParser jParser=new JSONParser();
			JSONObject json=jParser.getJSONFromUrl(url);
			System.out.println("json="+json);
			try
			{	
				if(json == null){
					value = "Failed";
				}
				else if(json.has("Value")){
					value = json.getString("Value");
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			return value;
		}
		
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		switch(buttonView.getId()){
		case R.id.checkBoxTC1:
			Log.d(getClass().getCanonicalName(), "CheckBox 1");
			if(isChecked && checkBoxTC2.isChecked() && checkBoxTC3.isChecked() && checkBoxTC4.isChecked())
				buttonOk.setEnabled(true);
			else
				buttonOk.setEnabled(false);
			break;
		case R.id.checkBoxTC2:
			Log.d(getClass().getCanonicalName(), "CheckBox21");
			if(isChecked && checkBoxTC1.isChecked() && checkBoxTC3.isChecked() && checkBoxTC4.isChecked())
				buttonOk.setEnabled(true);
			else
				buttonOk.setEnabled(false);
			break;
		case R.id.checkBoxTC3:
			Log.d(getClass().getCanonicalName(), "CheckBox 3");
			if(isChecked && checkBoxTC1.isChecked() && checkBoxTC2.isChecked() && checkBoxTC4.isChecked())
				buttonOk.setEnabled(true);
			else
				buttonOk.setEnabled(false);
			break;
		case R.id.checkBoxTC4:
			Log.d(getClass().getCanonicalName(), "CheckBox 3");
			if(isChecked && checkBoxTC1.isChecked() && checkBoxTC2.isChecked() && checkBoxTC3.isChecked())
				buttonOk.setEnabled(true);
			else
				buttonOk.setEnabled(false);
			break;
		}
	}
}
