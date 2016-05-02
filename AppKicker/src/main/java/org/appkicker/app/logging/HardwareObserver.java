package org.appkicker.app.logging;

import org.appkicker.app.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * <p>
 * This components observes the hardware and provides up-to-date information on
 * the states of hardware components. In contrast to {@link DeviceObserver} the
 * information provided by this class is more dynamic and changes more often,
 * e.g. screen orientation.
 * </p>
 * <p>
 * As an observer, this component just traces the information and makes it
 * available through static variables.
 * </p>
 * 
 * TODO this component needs to be implemented, maybe as a service with own
 * threads
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class HardwareObserver {

	// ---------------------------------------------
	// constants
	// ---------------------------------------------

	
	public final static short POWER_UNKNOWN = 0; 
	public final static short POWER_CONNECTED = 1; // powerplug is connected
	public final static short POWER_UNCONNECTED = 2; // device is on battery
	
	private static final int POWERLEVEL_UNKNOWN = -1;

	public final static short WIFI_UNKNOWN = 0;
	public final static short WIFI_OFF = 1;
	public final static short WIFI_ON = 2; // but not connected
	public final static short WIFI_CONNECTED = 3;
	
	public final static short BLUETOOTH_UNKNOWN = 0;
//	public final static short BLUETOOTH_OFF = 1;
//	public final static short BLUETOOTH_ON = 2;
//	public final static short BLUETOOTH_CONNECTED = 3;

	
	// -1 and +1 since in the end we want to know which more appeared more often during interaction
	public final static short ORIENTATION_LANDSCAPE = -1;
	public final static short ORIENTATION_UNKNOWN = 0;
	public final static short ORIENTATION_PORTRAIT = 1;

	// -1 and +1 since in the end we want to know which more appeared more often during interaction
	public final static short HEADPHONES_UNCONNECTED = -1;
	public final static short HEADPHONES_UNKNOWN = 0;
	public final static short HEADPHONES_CONNECTED = 1;
	
	public final static short SCREEN_UNKNOWN = 0;
	public final static short SCREEN_ON = 1;
	public final static short SCREEN_OFF = 2;
	

	// ---------------------------------------------
	// public properties
	// ---------------------------------------------

	// ---------------------------------------------
	// public properties
	// ---------------------------------------------

	/**
	 * power state
	 * 
	 * <li>POWER_CONNECTED: connected to power plug <li>POWER_LOW <li>POWER_HIGH
	 */
	public static short powerstate = POWER_UNKNOWN;

	/** power level of the device in percent */
	public static short powerlevel = POWERLEVEL_UNKNOWN;
	
	/**
	 * wifi state
	 * 
	 * <li>WIFI_OFF: turned off <li>WIFI_ON: turned on, but not connected <li>
	 * WIFI_CONNECTED: turned on, and connected
	 */
	public static short wifistate = WIFI_UNKNOWN;

	/** bluetooth state */
	public static short bluetoothstate = BLUETOOTH_UNKNOWN;

	/** orientation of the device */
	public static short orientation = ORIENTATION_UNKNOWN;

	/** state of the headphones */
	public static short headphones = HEADPHONES_UNKNOWN;
	
	/** state of the screen */
	public static short screenState = SCREEN_UNKNOWN;
	
	/** resolution of the device */
	public static long timestampOfLastScreenOn = 0;
	
	// ---------------------------------------------
	// methods
	// ---------------------------------------------

	public static void wifiChanged(Intent intent) {
		int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

		switch (state) {
		case WifiManager.WIFI_STATE_ENABLED:
		case WifiManager.WIFI_STATE_ENABLING:

//				Utils.dToast("appsensor: WIFI_ON");
				wifistate = WIFI_ON;
			break;
		case WifiManager.WIFI_STATE_DISABLING:
		case WifiManager.WIFI_STATE_DISABLED:
//			Utils.dToast("appsensor: WIFI_OFF");
			wifistate = WIFI_OFF;
			break;
		case WifiManager.WIFI_STATE_UNKNOWN:
		default:
//			Utils.dToast("appsensor: WIFI_UNKNOWN");
			wifistate = WIFI_UNKNOWN;
			break;
		}

	}

	/**
	 * Writes the state of the headphones from the intent to observer's public
	 * attributes.
	 * 
	 * @param intent
	 */
	public static void headphonesChanges(Intent intent) {
		int headSetState = intent.getIntExtra("state", -1);
		switch (headSetState) {
		case 1:
			headphones = HEADPHONES_CONNECTED;
			Utils.dToast("HEADPHONES_CONNECTED: " + headSetState);
			break;
		case 0:
			headphones = HEADPHONES_UNCONNECTED;
			Utils.dToast("HEADPHONES_UNCONNECTED: " + headSetState);
			break;
		default:
			headphones = HEADPHONES_UNKNOWN;
			Utils.dToast("HEADPHONES_UNKNOWN: " + headSetState);
			break;
		}
	}

	/*
	public static void bluetoothChanges(Intent intent) {
		Utils.dToast(intent.getAction());
		int s = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
		switch (s) {
		case BluetoothAdapter.STATE_OFF:
		case BluetoothAdapter.STATE_TURNING_OFF:
			bluetoothstate = BLUETOOTH_ON;
			break;
		case BluetoothAdapter.STATE_ON:
		case BluetoothAdapter.STATE_TURNING_ON:
			bluetoothstate = BLUETOOTH_OFF;
			break;
		default:
			bluetoothstate = BLUETOOTH_UNKNOWN;
			break;
		}
	}
	*/

	public static void networkChanged(Intent intent) {
		NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if (info.isConnected()) {
			wifistate = WIFI_CONNECTED;
		} /*else {
			wifistate = WIFI_ON;
		} */
	}
	
	public static void updateOrientation(Context context) {
		switch (context.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_LANDSCAPE:
				orientation = ORIENTATION_LANDSCAPE;
				break;
			case Configuration.ORIENTATION_PORTRAIT:
				orientation = ORIENTATION_PORTRAIT;
				break;
			default:
				orientation = ORIENTATION_UNKNOWN;
		}
	}
}
