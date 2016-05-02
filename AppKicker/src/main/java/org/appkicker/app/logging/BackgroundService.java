package org.appkicker.app.logging;

import org.appkicker.app.sync.SyncThread;
import org.appkicker.app.utils.Utils;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * <p>
 * This service is the main component for handling all background routines, e.g.
 * listening to intents and starting threads. This component registers itself to
 * all the broadcasts that are of interest for appsensor; for every received
 * intent it updates the required observers.
 * </p>
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class BackgroundService extends Service {

	/** Thread for logging application usage */
	private AppUsageLogger appLogger;

	private LocationObserver locationLogger;

	/** Thread for persisting data to server */
	private SyncThread syncThread;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/** receiver that reacts on screen on and off */
	private static BroadcastReceiver intentListener;

	/** filter for intents related to screen */
	private static IntentFilter screenIntentFilter = new IntentFilter();
	
	/** filter for intents related to package manager */
	private static IntentFilter packageIntentFilter = new IntentFilter();

	/** wifi broadcasts */
	static {
		
		// register for install/update/removal
		packageIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED); // added 
		packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED); // uninstall
		packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED); // Update
		packageIntentFilter.addDataScheme("package");

		// register the receiver for screen on/off events
		screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
		screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		screenIntentFilter.addAction(Intent.ACTION_SHUTDOWN);
		screenIntentFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
		screenIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		screenIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
		screenIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();

		Utils.d(this, "service was created -- next we start all background threads");

		// create and start the service for logging app usage
		appLogger = AppUsageLogger.getAppUsageLogger(getBaseContext());
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String rate_string = prefs.getString(Utils.SETTINGS_settings_sensorsamplingrate, Utils.SETTINGS_settings_sensorsamplingrate_default+"");
		int rate = new Integer(rate_string);
		AppUsageLogger.TIME_APPCHECKINTERVAL = rate;

		boolean activate = prefs.getBoolean(Utils.SETTINGS_settings_sensorsactive, true);
		if (activate) {
			Utils.dToast(this, "AppSensor activated");
			AppUsageLogger.getAppUsageLogger(this).startLogging();
		} else {
			Utils.dToast(this, "AppSensor deactivated");
			AppUsageLogger.getAppUsageLogger(this).pauseLogging();
		}
		
				
		// start the service for logging locations
		locationLogger = new LocationObserver(getBaseContext());
		locationLogger.startLogging();
		
		
		
		// create and start the sync service with settings
		syncThread = SyncThread.getSyncThread(getBaseContext());
		
//		boolean syncwifionly = prefs.getBoolean(Utils.SETTINGS_settings_syncwifionly, Utils.SETTINGS_settings_syncwifionly_default);
//		SyncThread.getSyncThread(this).setSyncWifiOnly(syncwifionly);

//		String syncrateMinutesStrng = prefs.getString(Utils.SETTINGS_settings_syncrate, Utils.SETTINGS_settings_syncrate_default+"");
//		int syncrateMinutes = new Integer(syncrateMinutesStrng);
//		SyncThread.getSyncThread(this).setTimeSyncInterval(syncrateMinutes);
//		SyncThread.getSyncThread(this).requestSync();

		
		
		// listen to broadcast intents
		intentListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				String packageName = "null";
				if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
					Utils.dToast(context, "package added");
					packageName = intent.getDataString().replaceAll("package:", "");
					appLogger.logAppInstalled(packageName);
				} else if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
					Utils.dToast(context, "package removed");
					packageName = intent.getDataString().replaceAll("package:", "");
					appLogger.logAppRemoved(packageName);
				} else if (action.equals(Intent.ACTION_PACKAGE_CHANGED) || action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
					Utils.dToast(context, "package replaces");
					packageName = intent.getDataString().replaceAll("package:", "");
					appLogger.logAppUpdated(packageName);
				} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
					Utils.d(BackgroundService.this, "screen turned on");
					HardwareObserver.screenState = HardwareObserver.SCREEN_ON;
					HardwareObserver.timestampOfLastScreenOn = Utils.getCurrentTime();
					LocationObserver.performLocationUpdate();
					appLogger.logDeviceScreenOn();
					appLogger.startLogging();
				} else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
					Utils.d(BackgroundService.this, "screen turned off");
					HardwareObserver.screenState = HardwareObserver.SCREEN_OFF;
					LocationObserver.performLocationUpdate();
					appLogger.logDeviceScreenOff();
					appLogger.checkStandByOnSceenOff();
				} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
					HardwareObserver.wifiChanged(intent);
				} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
					HardwareObserver.networkChanged(intent);
				} else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
					HardwareObserver.headphonesChanges(intent);
//				} else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
//					HardwareObserver.bluetoothChanges(intent);
				} else {
					Utils.dToast(BackgroundService.this, "unhandled: " + intent.getAction());
				}
			}
		};
		
		
		
		// read current bluetooth state
		/* FIXME
		if (BluetoothAdapter.getDefaultAdapter() != null) {
			if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
				HardwareObserver.bluetoothstate = HardwareObserver.BLUETOOTH_ON;
			} else {
				HardwareObserver.bluetoothstate = HardwareObserver.BLUETOOTH_OFF;
			}
		} else {
			HardwareObserver.bluetoothstate = HardwareObserver.BLUETOOTH_OFF;
		}
		*/
		
	   BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
	        int scale = -1;
	        int level = -1;
	        int voltage = -1;
	        int temp = -1;
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	            temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
	            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
	            
				if (0 == intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
					// device is on battery
					HardwareObserver.powerstate = HardwareObserver.POWER_UNCONNECTED;
				} else {
					HardwareObserver.powerstate = HardwareObserver.POWER_CONNECTED;
				}

	            HardwareObserver.powerlevel = (short) (100*level/scale);
	            Log.d(Utils.TAG, "BatteryManager: level is "+HardwareObserver.powerlevel  + "(" +level+"/"+scale+") , temp is "+temp+", voltage is "+voltage);
	        }
	    };
	    
	    
		
		// register with intent filter
		registerReceiver(intentListener, screenIntentFilter);
		registerReceiver(intentListener, packageIntentFilter);
		registerReceiver(intentListener, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		registerReceiver(intentListener, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		
		
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(intentListener);
		Utils.d2(this, "service was destroyed");
	}

	/**
	 * The service can be started by using this method. Calling this method can
	 * only be beneficial, maybe the user has stopped the service. However, the
	 * service will only start if the disclaimer was acknowledged.
	 * 
	 * @param c
	 */
	public static void startByIntent(Context c) {
		Utils.d(BackgroundService.class, "starting BackgroundService by Intent");
		Intent starter = new Intent(c, BackgroundService.class);
		starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startService(starter);
	}

}
