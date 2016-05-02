package org.appkicker.app.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import org.appkicker.app.backup.WrapperBackupAgent;
import org.appkicker.app.utils.Utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * This class provides information on the device itself, e.g. operating system
 * and a unique identifier for the installation of our app. In contrast to
 * {@link HardwareObserver} this class provides more static information about
 * the device.
 * 
 * http://android-developers.blogspot.com/2011/03/identifying-app-installations.
 * html
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class DeviceObserver {

	private static String sID = null;
	public static final String INSTALLATION = "INSTALLATION";

	/**
	 * id of the installation, created as a UUID
	 * 
	 * @param context
	 * @return
	 */
	public synchronized static String id(Context context) {
		if (sID == null) {
			File installation = new File(context.getFilesDir(), INSTALLATION);
			try {
				if (!installation.exists()) {
					writeInstallationFile(installation);
					WrapperBackupAgent.dataChanged(context);
				}
				sID = readInstallationFile(installation);
				Utils.d(context, "Installation id loaded: "+sID);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return sID;
	}

	/**
	 * retrieves the device id that was generated previously
	 * 
	 * @param installation
	 * @return
	 * @throws IOException
	 */
	private static String readInstallationFile(File installation) throws IOException {
		RandomAccessFile f = new RandomAccessFile(installation, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}

	/**
	 * writes a file to make the generated id persistent
	 * 
	 * @param installation
	 * @throws IOException
	 */
	private static void writeInstallationFile(File installation)
			throws IOException {
		FileOutputStream out = new FileOutputStream(installation);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}

	public final static short APILEVEL_UNKNOWN = -1;
	
	/** API level of the Android operating system */
	public static int apilevel = APILEVEL_UNKNOWN;

	/** resolution of the device */
	private static String resolution = null;

	/**
	 * resolution of the device in pixels
	 * 
	 * @param c
	 * @return
	 */
	public static synchronized String resolution(Context c) {
		if (resolution != null) {
			return resolution;
		} else {
			WindowManager windowManager = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics metrics = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(metrics);
			resolution = metrics.widthPixels + "x" + metrics.heightPixels;
			return resolution;
		}
	}

	public static int apiLevel() {
		return android.os.Build.VERSION.SDK_INT;
	}
	
	public static String modelName() {
		return Build.MODEL;
	}
	
	public static String productName() {
		return Build.PRODUCT;
	}

	private static int appVersion = 0;
	public static synchronized int appVersion(Context context) {
		if (appVersion != 0) {
			return appVersion;
		} else {
			try {
				PackageManager manager = context.getPackageManager();
				PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
				appVersion = info.versionCode;
			} catch (NameNotFoundException e) {
				Utils.e(DeviceObserver.class, e.getMessage());
			}
			return appVersion;
		}
	}

	/**
	 * Return the ID of the application like we used it in former versions of
	 * appazaar, i.e. the olf style device id.
	 * 
	 * @param context
	 * @return
	 */
	/*
	public static String hashedIMEI(Context context) {
		String deviceID = Utils.DEFAULT_DEVICE_ID;
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		try {
			deviceID = telephonyManager.getDeviceId();
			if (deviceID == null) {
				Utils.e(null, "Could not get device id (null)! Using " + Utils.DEFAULT_DEVICE_ID);
				deviceID = Utils.DEFAULT_DEVICE_ID;
			} else {
				deviceID = Utils.md5(deviceID);
			}
		} catch (SecurityException e) {
			Utils.e(null, "Could not get device id (SecurityException)! Using " + Utils.DEFAULT_DEVICE_ID + " instead.");

			deviceID = Utils.DEFAULT_DEVICE_ID;
		}
		return deviceID;
	}
	*/
}
