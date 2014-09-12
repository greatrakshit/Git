package com.trinity.dial100Citizen;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.trinity.dial100Citizen.R;

public class AddContactsList extends Activity implements OnItemClickListener{

	ListView listViewRoot;
	ContactsAdapter CA;
	final static int add_contact = 898;
	private static final int MAX_CONTACTS_ALLOWED = 2;
	DBAdapter mDb;
	List<String> list = new ArrayList<String>();
	private SharedPreferences pref;
	private Editor editor;
	Button buttonBack;
	
	public static String CHECKED_CONTACTS = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_contacts);	
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		editor = pref.edit();
		
		mDb = new DBAdapter(getApplicationContext());
		mDb.open();		
		
		buttonBack = (Button)findViewById(R.id.buttonBack);
		
		CA = new ContactsAdapter(getApplicationContext(), R.layout.contact_view, list);		
		listViewRoot = (ListView) findViewById(R.id.listViewContacts);
		listViewRoot.setAdapter(CA);		
		listViewRoot.setOnItemClickListener(this);
		refreshContactsList();
		
	}
	
	private void refreshContactsList() {
		// TODO Auto-generated method stub
		CA.clear();
		list.clear();
		list.add("Add Contact");
		Cursor c = mDb.fetchContacts();
		if(c.getCount()>0){
			if(c.moveToFirst()){
				list.add(c.getString(0)+":"+c.getString(1)+":"+c.getString(2));				
				while(c.moveToNext())
					list.add(c.getString(0)+":"+c.getString(1)+":"+c.getString(2));
			}
		}
		CA.notifyDataSetChanged();
		invalidateOptionsMenu();
	}

	public void cancelDeleteContacts(View v){
		toggleDeleteContactsVisibility();
	}
	
	public void deleteSelectedContacts(View v){
		String[] contact = CHECKED_CONTACTS.split("/");
		for(int i=0; i<contact.length; i++){
			if(contact[i].trim().length()>0) {
				int d = Integer.valueOf(contact[i].trim());
				Log.d(getClass().getCanonicalName(), d + " deleted");
				mDb.deleteContact(String.valueOf(d));
			}
		}
		toggleDeleteContactsVisibility();
		refreshContactsList();
		setFlagToUpdateContactsOnServer();
	}

	private void setFlagToUpdateContactsOnServer() {
		// TODO Auto-generated method stub
		Log.d(getClass().getCanonicalName(), "Contacts Edited. Setting flag to update contacts on server as TRUE");
		editor.putString(Global.contacts_status, Global.contacts_edited).commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		try{			
			String name = data.getStringExtra("name");
			String number = data.getStringExtra("number");
			Log.d(getLocalClassName(), name+":"+number);
			//list.add(name+":"+number);
			number = number.replace("+91", "");
			if(number.length()>10) number = number.substring(number.length()-10);
			if(saveContact(name, number))
				Toast.makeText(getApplicationContext(), "Contact Successfully Added", Toast.LENGTH_LONG).show();
			else 
				Toast.makeText(getApplicationContext(), "Failed to store contact", Toast.LENGTH_LONG).show();
			
		} catch(Exception e){
			e.printStackTrace();
		}
		
		refreshContactsList();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		Cursor c = mDb.fetchContacts();
		if(c.getCount()>0){
			inflater.inflate(R.menu.delete_menu_glow, menu);
		} else {
			inflater.inflate(R.menu.delete_menu, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(getClass().getCanonicalName(), "OnOptionsItemSelected");
		// TODO Auto-generated method stub
		switch(item.getItemId()){
	    case android.R.id.home:
	        finish();
	        return true;
		case R.id.delete_contact:
			toggleDeleteContactsVisibility();
			break;
/*		case R.id.sos_settings:
			startActivity(new Intent(SOSActivity.this, SettingsActivity.class));
			break;*/
		}
		return super.onOptionsItemSelected(item);
	}


	private void toggleDeleteContactsVisibility() {
		// TODO Auto-generated method stub
		int i=0;
		while(i<list.size()){
			//if(i!=1) ((listViewRoot.getChildAt(i)).findViewById(R.id.checkBoxDelete)).setVisibility(View.INVISIBLE);
			if(((listViewRoot.getChildAt(i)).findViewById(R.id.checkBoxDelete)!=null)){
				View v = (listViewRoot.getChildAt(i)).findViewById(R.id.checkBoxDelete);
				if(v.getVisibility()==View.VISIBLE){
					v.setVisibility(View.INVISIBLE);
					((CheckBox)v).setChecked(false);
					((LinearLayout)findViewById(R.id.linearLayoutButtons)).setVisibility(View.INVISIBLE);
				} else {
					v.setVisibility(View.VISIBLE);
					((LinearLayout)findViewById(R.id.linearLayoutButtons)).setVisibility(View.VISIBLE);
				}
				
				//Log.d("MyTag", "Not null "+i);
			}
			i++;
		}
		
		if(((LinearLayout)findViewById(R.id.linearLayoutButtons)).getVisibility()==View.VISIBLE)
			buttonBack.setVisibility(View.INVISIBLE);
		else
			buttonBack.setVisibility(View.VISIBLE);
	}

	public void back(View v){
		finish();
	}
	public boolean saveContact(String name, String number){	
		setFlagToUpdateContactsOnServer();
		return mDb.insertContact(name, number);		
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
		// TODO Auto-generated method stub
		Log.d(getLocalClassName(), "Clicked");
		switch(position){
		case 0:
			showAddContactsOption();
			break;
		default:
			break;
	}
}

	private void showAddContactsOption() {
		// TODO Auto-generated method stub
		if(list.size() < (MAX_CONTACTS_ALLOWED+1)) // +1 FOR "ADD CONTACTS" ITEM IN LIST
			startActivityForResult(new Intent(AddContactsList.this, AddOrCreateContactDialog.class), add_contact);
		else
			Toast.makeText(getApplicationContext(), "Maximum "+MAX_CONTACTS_ALLOWED+" Contacts Allowed", Toast.LENGTH_LONG).show();
	}
}

