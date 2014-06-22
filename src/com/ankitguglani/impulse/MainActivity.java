package com.ankitguglani.impulse;

import com.google.android.glass.timeline.LiveCard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class MainActivity extends Activity{
	private static final String TAG = "BeaconService";
	private static LiveCard liveCard;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showNotification();
		
		Intent serviceIntent = new Intent(MainActivity.this, BeaconService.class);
		serviceIntent.setAction("com.ankitguglani.impulse.SCAN");
		startService(serviceIntent);
	}
	
	private void showNotification() {
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.activity_glass);
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
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	public  static void  unPublish(){
		if(liveCard != null){
			liveCard.unpublish();
		}
	}
	
}
