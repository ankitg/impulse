package com.ankitguglani.impulse;

import java.util.List;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.google.android.glass.timeline.LiveCard;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

public class BeaconService extends Service implements SensorEventListener{
	private Handler handler;
	private SensorManager sensorManager;
	private Sensor stepSensor;
	private BeaconManager beaconManager;
	private Region houseRegion;
	private Beacon specialBeacon;

	private enum BeaconState {INSIDE, OUTSIDE};
	private BeaconState officeState;
	private LiveCard liveCard;
	
	private static final String TAG = "BeaconService";
	
	private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";

	private static final int specialMajor = 47233;
	private static final int specialMinor = 1;

	private static final double enterThreshold = 1.5;
	private static final double exitThreshold = 2.5;



	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void runOnUiThread(Runnable runnable) {
		handler.post(runnable);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		handler = new Handler();


		// TODO add sensor data to stop/start beacon scanning
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		sensorManager.registerListener(this, stepSensor,SensorManager.SENSOR_DELAY_NORMAL);

		officeState = BeaconState.OUTSIDE;

		houseRegion = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);
		beaconManager = new BeaconManager(getApplicationContext());

		// Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
		// In order for this demo to be more responsive and immediate we lower down those values.
		//beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(25));
		//beaconManager.setForegroundScanPeriod(TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(10));
		
		beaconManager.setRangingListener(new BeaconManager.RangingListener() {
			@Override
			public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						for (Beacon beacon : beacons) {
							//Log.d(TAG, "MAC = " + beacon.getMacAddress() + ", RSSI = " + -beacon.getRssi());
							if (beacon.getMajor() == specialMajor && beacon.getMinor() == specialMinor ){
								specialBeacon = beacon;
							}
						}
						if (specialBeacon != null){
							double officeDistance = Utils.computeAccuracy(specialBeacon);
							Log.d(TAG, "officeDistance: " + officeDistance);
							if (officeDistance < enterThreshold && officeState == BeaconState.OUTSIDE){
								officeState = BeaconState.INSIDE;
								showNotification("You are at AngelHack");
							}else if (officeDistance > exitThreshold && officeState == BeaconState.INSIDE){
								officeState = BeaconState.OUTSIDE;
								MainActivity.unPublish();
							}
						}
						else
						{
							Log.d(TAG,"no beacon");
						}
					}
				});
			}
		});
		showNotification("hi");
		//stopScanning();
		startScanning();
	}

	private void startScanning(){
		beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
			@Override
			public void onServiceReady() {
				try {
					//beaconManager.startMonitoring(houseRegion);
					beaconManager.startRanging(houseRegion);
				} catch (RemoteException e) {
					Log.d(TAG, "Error while starting Ranging");
				}
			}
		});
	}

	private void stopScanning(){
		try {
			//beaconManager.stopMonitoring(houseRegion);
			beaconManager.stopRanging(houseRegion);
		} catch (RemoteException e) {
			Log.e(TAG, "Cannot stop but it does not matter now", e);
		}
	}

	private void showNotification(String msg) {
		Log.d(TAG, msg);
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.livecard_beacon);
		views.setTextViewText(R.id.livecard_content,msg);
		if(liveCard != null){
			liveCard.unpublish();
		}
		liveCard = new LiveCard(getApplication(),"beacon");
		liveCard.setViews(views);
		Intent intent2  = MenuActivity.SetUpMenu(this);
		intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		liveCard.setAction(PendingIntent.getActivity(getApplicationContext(), 0, intent2, 0));
		liveCard.publish(LiveCard.PublishMode.REVEAL);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		beaconManager.disconnect();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

	}

}
