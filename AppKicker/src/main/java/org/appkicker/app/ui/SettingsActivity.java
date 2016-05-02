package org.appkicker.app.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import org.appkicker.app.R;
import org.appkicker.app.logging.AppUsageLogger;
import org.appkicker.app.logging.DeviceObserver;
import org.appkicker.app.sync.SyncThread;
import org.appkicker.app.utils.Utils;

/**
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.appsensor_settings);
		
		Preference pi = findPreference(Utils.SETTINGS_INSTALLATIONID_NAME);
		pi.setSummary(DeviceObserver.id(this));

//		Preference pd = findPreference(Utils.SETTINGS_DEVICEID_NAME);
//		pd.setSummary(DeviceObserver.hashedIMEI(this));
		
		// listen to changing preferences
		registerChangeListener();
		
	}

	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterChangeListener();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterChangeListener();
	}
	
	private void registerChangeListener() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	private void unregisterChangeListener() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		registerChangeListener();
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		
		if(key.equals(Utils.SETTINGS_settings_sensorsactive)) {
			boolean activate = prefs.getBoolean(Utils.SETTINGS_settings_sensorsactive, Utils.SETTINGS_settings_sensorsactive_default);
			if (activate) {
//				Utils.dToast("AppSensor activated in settings");
				AppUsageLogger.getAppUsageLogger(this).startLogging();
			} else {
//				Utils.dToast("AppSensor deactivated in settings");
				AppUsageLogger.getAppUsageLogger(this).pauseLogging();
			}			
		} else if(key.equals(Utils.SETTINGS_settings_sensorsamplingrate)) {
			String rate_string = prefs.getString(Utils.SETTINGS_settings_sensorsamplingrate, Utils.SETTINGS_settings_sensorsamplingrate_default+"");
			int rate = new Integer(rate_string);
			AppUsageLogger.TIME_APPCHECKINTERVAL = rate;
//		} else if(key.equals(Utils.SETTINGS_settings_syncwifionly)) {
//			boolean syncwifionly = prefs.getBoolean(Utils.SETTINGS_settings_syncwifionly, Utils.SETTINGS_settings_syncwifionly_default);
//			SyncThread.getSyncThread(this).setSyncWifiOnly(syncwifionly);
//			Utils.dToast("settings_syncwifionly: " + syncwifionly);
		} else if(key.equals(Utils.SETTINGS_settings_syncrate)) {
			String syncrateMinutesStrng = prefs.getString(Utils.SETTINGS_settings_syncrate, Utils.SETTINGS_settings_syncrate_default+"");
			int syncrateMinutes = new Integer(syncrateMinutesStrng);
			SyncThread.getSyncThread(this).setTimeSyncInterval(syncrateMinutes);
			Utils.dToast("settings_syncrate: " + syncrateMinutes);
		}
	}
}
