package com.trinity.dial100Citizen;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactsAdapter extends ArrayAdapter<String> implements OnCheckedChangeListener{
	
	Context context;
	List<String> values;
	
	public ContactsAdapter(Context context, int resource, List<String> values) {
		super(context, resource, values);
		// TODO Auto-generated constructor stub
		this.context = context;
		this.values = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View rowView = null;
		    Log.d(getClass().toString(), position+" - "+values.get(position));
			switch(position){
			case 0:				
				rowView = inflater.inflate(R.layout.add_contact_view, parent, false);
				rowView.setBackgroundColor(Color.parseColor("#000022"));
				break;
			default:
			    rowView = inflater.inflate(R.layout.contact_view, parent, false);			    
			    TextView textViewName = (TextView) rowView.findViewById(R.id.textViewName);
			    TextView textViewNumber = (TextView) rowView.findViewById(R.id.textViewNumber);
			    ImageView imageViewContactImage = (ImageView) rowView.findViewById(R.id.imageViewContactImage);
				imageViewContactImage.setBackgroundResource(R.drawable.contact_icon);
				String[] contact = values.get(position).split(":");
				textViewName.setText(contact[1]);
				textViewNumber.setText(contact[2]);
				((CheckBox)rowView.findViewById(R.id.checkBoxDelete)).setOnCheckedChangeListener(this);
				rowView.setTag(contact[0]);
				break;
			}
		return rowView;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		
		//int checkBox = ((ListView)buttonView.getParent().getParent()).getPositionForView((View) buttonView.getParent());
		int checkBoxTag =  Integer.valueOf(((View)buttonView.getParent()).getTag().toString().trim());
		
		if(isChecked) {
			AddContactsList.CHECKED_CONTACTS = AddContactsList.CHECKED_CONTACTS +"/" + checkBoxTag;
			Log.d("MyTag", "CheckBox "+checkBoxTag);
		} else {
			AddContactsList.CHECKED_CONTACTS = AddContactsList.CHECKED_CONTACTS.replace("/"+String.valueOf(checkBoxTag), "");
		}
		Log.d("MyTag", "Selected Checkboxes - "+AddContactsList.CHECKED_CONTACTS );
	}

}
