package com.ankitguglani.impulse;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nclSDK.Ncl;
import nclSDK.NclBool;
import nclSDK.NclCallback;
import nclSDK.NclEvent;
import nclSDK.NclEventType;
import nclSDK.NclMode;
import nclSDK.NclProvision;

import com.google.android.glass.timeline.LiveCard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RemoteViews;

public class Authentication extends Activity {
	private static 	LiveCard liveCard;
	private static 	String ip = "192.168.1.225";
	public 	static 	int nymiHandle = 0;
	final String path = Environment.getExternalStorageDirectory() + "";
	public static ArrayList<NclProvision> provisions = new ArrayList<NclProvision>();
	String PREFS_NAME = "provision";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Ncl.InitiateLibrary(this, ip, 9089);

		final NclCallback cb = new NclCallback(this, "HandleCallBack", NclEventType.NCL_EVENT_ANY);
		new Thread(new Runnable() {

			@Override
			public void run() {
				Boolean b = Ncl.init(cb, null, "LOCK", NclMode.NCL_MODE_DEV, path + "/SomeOtherFile.txt");

			}
		}).start();

		showNotification();
	}
	
	private void showNotification() {
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.activity_authentication);
		if(liveCard != null){
			liveCard.unpublish();
		}
		liveCard = new LiveCard(getApplication(),"beacon");
		liveCard.setViews(views);
		Intent menuIntent = new Intent(this, MenuActivity.class);
		menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		liveCard.setAction(PendingIntent.getActivity(getApplicationContext(), 0, menuIntent, 0));
		liveCard.publish(LiveCard.PublishMode.REVEAL);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.authentication, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}
	
	public void HandleCallBack(NclEvent event, Object userData) {

		Log.d("HandleCallBack", "NclEvent: "
				+ NclEventType.values()[event.type]);

		switch (NclEventType.values()[event.type]) {
		case NCL_EVENT_INIT: {
			writeToAppLog("NCL_EVENT_INIT Returned "
					+ NclBool.values()[event.init.success] + "\n");
			break;
		}

		case NCL_EVENT_DISCOVERY: {
			writeToAppLog("NCL_EVENT_DISCOVERY	Rssi: " + event.discovery.rssi
					+ "    TxPowerLevel: " + event.discovery.txPowerLevel
					+ "\n");
			nymiHandle = event.discovery.nymiHandle;

			break;
		}

		case NCL_EVENT_FIND: {
			writeToAppLog("NCL_EVENT_FIND Rssi: " + event.find.rssi
					+ "    TxPowerLevel: " + event.find.txPowerLevel + "\n");
			nymiHandle = event.find.nymiHandle;
			break;
		}

		case NCL_EVENT_DETECTION: {
			break;
		}

		case NCL_EVENT_AGREEMENT: {
			writeToAppLog("NCL_EVENT_AGREEMENT\n");
			nymiHandle = event.agreement.nymiHandle;
			break;
		}

		case NCL_EVENT_PROVISION: {
			Authentication.provisions.add(event.provision.provision);
			SharedPreferences id = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = id.edit();
			editor.putString("ID", SomeWhatReadable(event.provision.provision.id));
			editor.commit();
			break;
		}

		case NCL_EVENT_VALIDATION: {
			writeToAppLog("NCL_EVENT_VALIDATION\n");
			nymiHandle = event.validation.nymiHandle;
			break;
		}

		case NCL_EVENT_CREATED_SK: {
			writeToAppLog("NCL_EVENT_CREATED_SK\n");
			nymiHandle = event.createdSk.nymiHandle;
			writeToAppLog("SK: " + SomeWhatReadable(event.createdSk.sk) + "\n");
			writeToAppLog("ID: " + SomeWhatReadable(event.createdSk.id) + "\n");
			break;
		}
		case NCL_EVENT_GOT_SK: {
			writeToAppLog("NCL_EVENT_GOT_SK\n");
			nymiHandle = event.gotSk.nymiHandle;
			writeToAppLog("SK: " + SomeWhatReadable(event.gotSk.sk) + "\n");
			break;
		}
		case NCL_EVENT_PRG: {
			writeToAppLog("NCL_EVENT_PRG\n");
			nymiHandle = event.prg.nymiHandle;
			writeToAppLog("Value: " + SomeWhatReadable(event.prg.value) + "\n");
			break;
		}

		case NCL_EVENT_RSSI: {
			writeToAppLog("NCL_EVENT_RSSI		Rssi:	" + event.rssi.rssi + "\n");
			nymiHandle = event.rssi.nymiHandle;
			break;
		}

		case NCL_EVENT_DISCONNECTION: {
			writeToAppLog("NCL_EVENT_DISCONNECTION\n");
			break;
		}

		}
	}
	
	private static void writeToAppLog(String msg){
		Log.d("Nymi", msg);
	}
	
	private static String SomeWhatReadable(char[] arr) {
		String str = "";
		for (char c : arr) {
			str += ((int) c) + " ";
		}
		return str;
	}
	
	public static void onDiscover()
	{
		Ncl.startDiscovery();
	}
	
	public static void onAgree()
	{
		if(!Ncl.agree(nymiHandle))
		{
			writeToAppLog("Agree Failed!");
		}
	}
	
	public static void onProvision()
	{
		Ncl.provision(nymiHandle);
	}
	
	public static void onFind()
	{
		Ncl.startFinding(provisions, provisions.size(), NclBool.NCL_FALSE);
	}
	
	public static void onValidate()
	{
		Ncl.validate(nymiHandle);
	}
}
