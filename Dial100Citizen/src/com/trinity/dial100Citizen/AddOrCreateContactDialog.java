package com.trinity.dial100Citizen;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class AddOrCreateContactDialog extends Activity implements OnClickListener{

	TextView textViewChoose, textViewCreate;
	final static int choose_from_contacts_request = 8323, createNewContactRequest = 9079;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_add_or_create_contact_dialog);
		
		textViewChoose = (TextView)findViewById(R.id.textViewChoose);
		textViewCreate = (TextView)findViewById(R.id.textViewCreate);
		
		textViewChoose.setOnClickListener(this);
		textViewCreate.setOnClickListener(this);
	}
	
	public void chooseFromContacts(View v){
		Intent openContacts = new Intent(Intent.ACTION_GET_CONTENT);
		openContacts.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		startActivityForResult(openContacts, choose_from_contacts_request);
	}
	
	public void createNewContact(View v){
		Intent createContact = new Intent(AddOrCreateContactDialog.this, CreateContactDialog.class);
		startActivityForResult(createContact , createNewContactRequest);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		try{

			// TODO Auto-generated method stub
			super.onActivityResult(requestCode, resultCode, data);
			switch(requestCode){
			case choose_from_contacts_request:
				if(data!=null){
					Uri uri = data.getData();
					if(uri != null){
						Cursor c = null;
						try {
							c = getContentResolver().query(uri, new String[]{
									ContactsContract.CommonDataKinds.Phone.NUMBER,
									ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null, null);
							if(c!=null && c.moveToFirst()){
								String number = c.getString(0).replace(" ", "").trim();
								if(number.length()>10)
									number = number.substring(number.length()-10);
								String name = c.getString(1).trim();
								
								Log.d(getLocalClassName(), name+" "+number);
								//list.add(name+":"+number);
								//list.add(1, number);
								Intent contact_chosen = new Intent();
								contact_chosen.putExtra("name", name);
								contact_chosen.putExtra("number", number);
								setResult(choose_from_contacts_request, contact_chosen);
								finish();
								
							}
						} catch (Exception e){
							e.printStackTrace();
						}
					}
				}
				break;
			case createNewContactRequest:
				
				String name = data.getStringExtra("name");
				String number = data.getStringExtra("number");
				
				Intent contact_chosen = new Intent();
				contact_chosen.putExtra("name", name);
				contact_chosen.putExtra("number", number);
				setResult(createNewContactRequest, contact_chosen);
				finish();
				break;
			}
			
			//CA.notifyDataSetChanged();
		
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.textViewChoose:
			chooseFromContacts(v);
			break;
		case R.id.textViewCreate:
			createNewContact(v);
			break;
		}
	}
}
