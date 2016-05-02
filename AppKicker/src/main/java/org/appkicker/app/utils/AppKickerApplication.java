package org.appkicker.app.utils;

import android.app.Application;
import android.content.Context;
import android.location.LocationManager;

/**
 * A wrapper around the applicaiton itself, to keep a static reference to the
 * application context.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class AppKickerApplication extends Application {

	private static Context context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		AppKickerApplication.context = getApplicationContext();
	}

	public static Context getAppContext() {
		return context;
	}

	public static LocationManager getLocationManager() {
		return (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	// public static WifiManager getWifiManager() {
	// return (WifiManager) context.getSystemService(WIFI_SERVICE);
	// }
}
