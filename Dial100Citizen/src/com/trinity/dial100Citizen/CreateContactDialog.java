package com.trinity.dial100Citizen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.trinity.dial100Citizen.R;

public class CreateContactDialog extends Activity {
	
	EditText editTextContactName, editTextContactNumber;
	Button buttonCancelCreateContact, buttonOkCreateContact;
	
	String contact_name, contact_number;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_create_contact_dialog);

		editTextContactName = (EditText)findViewById(R.id.editTextContactName);
		editTextContactNumber = (EditText)findViewById(R.id.editTextContactNumber);		
	}
	
	public void cancel(View v){
		finish();
	}
	
	public void okay(View v){		
		if(fieldsSet()){
			Intent contact_chosen = new Intent();
			contact_chosen.putExtra("name", contact_name);
			contact_chosen.putExtra("number", contact_number);
			setResult(AddOrCreateContactDialog.createNewContactRequest, contact_chosen);
			finish();
		}
	}

	private boolean fieldsSet() {
		// TODO Auto-generated method stub
		contact_name = editTextContactName.getText().toString().trim();
		contact_number = editTextContactNumber.getText().toString().trim();		
		if(contact_name.length()==0 || contact_number.length()==0){
			Toast.makeText(getApplicationContext(), "Please fill all textfields", Toast.LENGTH_LONG).show();
			return false;
		}		
		return true;
	}
}
