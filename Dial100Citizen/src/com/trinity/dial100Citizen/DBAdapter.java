package com.trinity.dial100Citizen;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter
{
	private static final String DATABASE_NAME="SOS";
	private static final int DATABASE_VERSION = 3;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private final Context mCtx;
	private Cursor mCursor;

	public static final String ROW_ID = "_id";
	
	private static final String FWD_MESSAGE_TABLE = "Fwd_Msg_Details";
	public static final String DATE_TIME = "date_time";
	public static final String FWD_MSG_STATUS = "status";

	private static final String MESSAGE_TABLE = "Msg_Details";
	public static final String DESCRIPTION = "description";

	public static final String MSG_STATUS = "status";
	public static final String MSG_READ_STATUS = "read_unread_status";

	public static final String CONTACTS_TABLE = "Contact_Info";
	public static final String CONTACTS_ID = "_id";
	public static final String CONTACTS_NAME = "name";
	public static final String CONTACTS_MOB = "mob_no";
	
	private static final String CONFIG_TABLE = "Config_Details";
	public static final String CONFIG_IP = "ip_address";
	public static final String CONFIG_PORT = "port_no";
	public static final String CONFIG_SECONDARY_IP = "secondary_ip_address";
	public static final String CONFIG_SIMNO = "sim_no";
	public static final String CONFIG_BASESTN = "base_stn";
	public static final String CONFIG_INTERVAL = "interval";
	public static final String CONFIG_URL = "app_path_url";
	public static final String CONFIG_JSP_URL = "jsp_url";
	public static final String CONFIG_MAP_URL = "map_url";
	
	
	private static final String FWD_MSG_TABLE_CREATE = "create table Fwd_Msg_Details (_id integer primary key autoincrement,description text not null,"
		+ "date_time text not null,status text not null);";

	private static final String MESSAGE_TABLE_CREATE = "create table Msg_Details (_id integer primary key autoincrement,description text not null,"
		+ "date_time text not null,status text not null,read_unread_status text not null);";
	
	private static final String CONTACTS_TABLE_CREATE = "create table IF NOT EXISTS " + CONTACTS_TABLE + 
								"(_id integer primary key autoincrement, name varchar(20), mob_no varchar(12))";
	
	private static final String CONFIG_TABLE_CREATE = "create table Config_Details (_id integer primary key autoincrement, "
		+ "ip_address text not null, port_no text not null,"
		+ "sim_no integer not null,base_stn text not null,interval text not null,app_path_url text not null,jsp_url text not null,map_url text ,secondary_ip_address text);";

	private static final String[] ConfigFields = new String[] { ROW_ID,
		CONFIG_IP, CONFIG_PORT, CONFIG_SIMNO, CONFIG_BASESTN,
		CONFIG_INTERVAL, CONFIG_URL,CONFIG_JSP_URL,CONFIG_MAP_URL,CONFIG_SECONDARY_IP };
	
	private static final String[] ContactFields = new String[] {CONTACTS_ID, CONTACTS_NAME, CONTACTS_MOB};

//	private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('182.73.236.114','5001','9945129701','9535444045','20','http://182.73.236.114:84/Uploadfile/','http://182.73.236.114:8080/SOS/','http://182.73.236.114:8080/geoserver/gwc/service/wms?VERSION=1.1.1&SRS=EPSG:4326','182.73.236.114');";
//	private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('115.249.189.183','5050','9611170088','9739805938','20','http://182.73.236.114:84/Uploadfile/','http://115.249.189.183:8080/SOS/','http://182.73.236.114:8080/geoserver/gwc/service/wms?VERSION=1.1.1&SRS=EPSG:4326','115.249.189.183');";
//	private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('115.249.189.182','5001','9611170088','9739805938','20','http://182.73.236.114:84/Uploadfile/','http://192.168.1.57:8080/SOS/','http://182.73.236.114:8080/geoserver/gwc/service/wms?VERSION=1.1.1&SRS=EPSG:4326','115.249.189.183');";
//	private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('115.249.189.182','5001','9611170088','9739805938','20','http://182.73.236.114:84/Uploadfile/','http://115.249.189.182:8080/SOS/','http://182.73.236.114:8080/geoserver/gwc/service/wms?VERSION=1.1.1&SRS=EPSG:4326','115.249.189.182');";
	//private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('192.168.1.57','7000','9611170088','9739805938','20','http://182.73.236.114:84/Uploadfile/','http://192.168.1.57:8080/SOS/','http://182.73.236.114:8080/geoserver/gwc/service/wms?VERSION=1.1.1&SRS=EPSG:4326','192.168.1.57');";
	
	
	private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('','','','','','','http://182.73.236.117:8081/Dial100Citizen/','','182.73.236.117');";
	//private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('','','','','','','http://192.168.1.57:8080/Dial100Citizen/','','192.168.1.57');";
	//private static final String InsertConfigdata = "INSERT INTO Config_Details (ip_address, port_no, sim_no,base_stn,interval,app_path_url,jsp_url,map_url,secondary_ip_address) VALUES ('','','','','','','http://61.16.137.213:8080/Dial100Citizen/','','61.16.137.213');";
	
	public DBAdapter(Context ctx) {
		this.mCtx = ctx;
	}
	public class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(FWD_MSG_TABLE_CREATE);
			db.execSQL(MESSAGE_TABLE_CREATE);
			db.execSQL(CONFIG_TABLE_CREATE);
			db.execSQL(CONTACTS_TABLE_CREATE);
			db.execSQL(InsertConfigdata);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			System.out.println("Upgrade");
			db.execSQL("DROP TABLE IF EXISTS " + FWD_MESSAGE_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + MESSAGE_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + CONFIG_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE);
			onCreate(db);
		}
	}
	public DBAdapter open() throws SQLException
	{
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	public void close() {
		mDbHelper.close();
	}

	public long createFwMsgAccount(String description, String datetime,String status) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(DESCRIPTION, description);
		initialValues.put(DATE_TIME, datetime);
		initialValues.put(FWD_MSG_STATUS, status);
		return mDb.insert(FWD_MESSAGE_TABLE, null, initialValues);
	}

	public void insertConfiguration(){
		mDb.execSQL(InsertConfigdata);
	}
	public Cursor fetchFwMessageAccounts() {

		mCursor = mDb.query(FWD_MESSAGE_TABLE, new String[]{ROW_ID,DESCRIPTION},FWD_MSG_STATUS + "=0",null,null,null, ROW_ID + " DESC","1");
		if (mCursor != null) 
		{	
			mCursor.moveToFirst();
		}
		return mCursor;
	}	
	public  void deleteFwMessageAccounts(String rowId)
	{		
		mDb.delete(FWD_MESSAGE_TABLE,ROW_ID + "=" + rowId,null);
	}
	
	public Cursor fetchConfigAccounts() {

		Cursor cursor = mDb.query(CONFIG_TABLE, ConfigFields, null, null, null,
				null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}
	
	public Cursor fetchContacts() {

		
		Cursor cursor = mDb.query(CONTACTS_TABLE, ContactFields, null, null, null,
				null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		Log.d(getClass().getCanonicalName(), cursor.getCount()+" contacts fetched");
		return cursor;
	}
	
	public boolean insertContact(String name, String number){
		ContentValues value = new ContentValues();
		value.put("name",name.trim());
		value.put("mob_no", number.trim());
		long i = mDb.insert(CONTACTS_TABLE, null, value);
		if(i!=-1)
			return true;
		else 
			return false;
	}
	
	public  int deleteContact(String rowId)
	{		
		int del = mDb.delete(CONTACTS_TABLE,ROW_ID + "=" + rowId,null);
		return del;
	}
	
	public boolean updateConfig(String colName, String colValue) {
		ContentValues newValues = new ContentValues();
		newValues.put(colName, colValue);
		return mDb.update(CONFIG_TABLE, newValues, null, null) > 0;
	}
}