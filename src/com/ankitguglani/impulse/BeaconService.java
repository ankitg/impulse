package com.ankitguglani.impulse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.google.android.glass.timeline.LiveCard;

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

//	private static final int specialMajor = 47233;
//	private static final int specialMinor = 1;

	private static final double enterThreshold = 1.5;
	private static final double exitThreshold = 2.5;
	private List<Integer> dismissIDs = new ArrayList<Integer>();
	//private	int   currentID	= 1;


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
//							if (beacon.getMajor() == specialMajor && beacon.getMinor() == specialMinor ){
//								specialBeacon = beacon;
//							}
							
							if(dismissIDs.indexOf(beacon.getMinor()) == -1 )
							{
								specialBeacon = beacon;
//								dismissIDs.add(specialBeacon.getMinor());
							}
						}
						
						if (specialBeacon != null){
							double officeDistance = Utils.computeAccuracy(specialBeacon);
							Log.d(TAG, "officeDistance: " + officeDistance);
							if (officeDistance < enterThreshold && officeState == BeaconState.OUTSIDE){
								officeState = BeaconState.INSIDE;
								if(dismissIDs.indexOf(specialBeacon.getMinor()) == -1)
								{
									String url = "https://api.mongolab.com/api/1/databases/impulse/collections/beacons?apiKey=4fe65986e4b0cb519caaa0a3&q=%7" +
											"Bmajor:"+specialBeacon.getMajor()+",minor:"+specialBeacon.getMinor()+"%7D";
								new RequestTask().execute(url);
								Log.d(TAG,"url: "+ url);
								dismissIDs.add(specialBeacon.getMinor());
								}
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
		//showNotification("hi");
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
	
	class RequestTask extends AsyncTask<String, String, String> {

	    @Override
	    protected String doInBackground(String... uri) {
	      HttpClient httpclient = new DefaultHttpClient();
	      HttpResponse response;
	      String responseString = null;
	      try {
	        response = httpclient.execute(new HttpGet(uri[0]));
	        StatusLine statusLine = response.getStatusLine();
	        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	          ByteArrayOutputStream out = new ByteArrayOutputStream();
	          response.getEntity().writeTo(out);
	          out.close();
	          responseString = out.toString();
	        } else{
	          //Closes the connection.
	          response.getEntity().getContent().close();
	          throw new IOException(statusLine.getReasonPhrase());
	        }
	      } catch (ClientProtocolException e) {
	        //TODO Handle problems..
	      } catch (IOException e) {
	        //TODO Handle problems..
	      }
	      return responseString;
	    }

	    @Override
	    protected void onPostExecute(String result) {
	      super.onPostExecute(result);


	      Log.d(TAG, "loaded");

	      try {
	        JSONArray jsonArray = new JSONArray(result);
	        JSONObject jObj = (JSONObject) jsonArray.get(0);
	        
	        jObj.getString("imageurl");
	        String mTitle = jObj.getString("title");
	        String mSubTitle = jObj.getString("subtitle");
	        
	        showNotification(mTitle);
	        Log.d(TAG, "showNotification");

	      } catch (JSONException e) {
	        e.printStackTrace();
	      }
	      //Do anything with response..
	    }
	  }

}
