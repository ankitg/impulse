package com.ankitguglani.impulse;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MenuActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		openOptionsMenu();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_items, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.stop:
			stopService(new Intent(this, BeaconService.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public static Intent SetUpMenu(final Service service, final float lat, final float lon, String address) {
		final Intent intent = new Intent(service, MenuActivity.class);
		return intent;
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing else to do, closing the Activity.
		finish();
	}
}
