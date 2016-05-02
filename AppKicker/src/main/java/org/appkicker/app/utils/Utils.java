package org.appkicker.app.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.appkicker.app.data.entities.AppUsageEvent;
import org.appkicker.app.logging.LocationObserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.util.Log;


/**
 * This class provides some technical utilities, e.g. for logging.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class Utils {

	/** tag for logging output */
	public static final String TAG = "appkicker";

	/** names of the settings **/
	public static final String PREFS_NAME = "org.appkicker.app.prefs";
	public static final String DISCLAIMER_ACK = "disclaimer_acknowledge";
	public static final String LASTTIMEASKED = "lasttimeasked";
	public static final String SETTINGS_INSTALLATIONID_NAME = "settings_id";
	public static final String SETTINGS_DEVICEID_NAME = "settings_deviceid";
	public static final String SETTINGS_settings_sensorsactive = "settings_sensorsactive";
	public static final String SETTINGS_settings_sensorsamplingrate = "settings_sensorsamplingrate";
	public static final String SETTINGS_settings_serverip = "settings_serverip";
	public static final String SETTINGS_settings_serverport = "settings_serverport";
	public static final String SETTINGS_settings_syncwifionly = "settings_syncwifionly";
	public static final String SETTINGS_settings_syncrate = "settings_syncrate";
	public static final String SETTINGS_settings_widgetalgo = "widgetalgorithm";
	public static final String SETTINGS_settings_widgetbackground = "widgetback";
	public static final String SETTINGS_TIMESTAMP_LAST_SERVERSYNC = "lastserversync";
	public static final int SETTINGS_settings_syncrate_default = 360;
	public static final int SETTINGS_settings_sensorsamplingrate_default = 500;
	public static final boolean SETTINGS_settings_syncwifionly_default = false;
	public static final boolean SETTINGS_settings_sensorsactive_default = true;

	/** debugging output, e.g. to enable on-screen debug notifications */
	public static final boolean D = true;
	public static final boolean Dtoast = false;

	/** charset for communication with the server */
	public static final String CHARSET = "UTF-8";

	/** this is the default value of the old style ID if we cannot get it from the API */
	@Deprecated public static final String DEFAULT_DEVICE_ID = "unknownDeviceID";

	


	/**
	 * returns timestamp in UTC milliseconds
	 * @return
	 */
	public static long getCurrentTime() {
		return System.currentTimeMillis();
	}
	
	private static int offsetUTC = 0;
	private static boolean offsetUTCset = false;
	
	/**
	 * get offset to UTC in seconds
	 * @return
	 */
	public synchronized static int utcOFF() {
		if (offsetUTCset) {
			return offsetUTC;
		} else {
			offsetUTC = Calendar.getInstance().getTimeZone().getRawOffset() / (1000 * 3600);
			offsetUTCset = true;
			return offsetUTC;
		}
	}
	
	public static void d(String o, String msg) {
		if (Utils.D) {
			if (o != null) {
				Log.d(o, msg);
			} else {
				Log.d(TAG, "NULL : " + msg);
			}
		}
	}

	
	public static void d(Object o, String msg) {
		if (Utils.D) {
			if (o != null) {
				Log.d(o.getClass().getSimpleName(), msg);
			} else {
				Log.d(TAG, "NULL : " + msg);
			}
		}
	}

	public static void e(Object o, String msg) {
		if (o == null) o = "(anonym object)";
		if (Utils.D) Log.d(TAG, o.getClass().getSimpleName() + ": " + msg);
	}
	
	/**
	 * @param o
	 * @param msg
	 */
	public static void d2(Object o, String msg) {
		if (Utils.D) {
			Log.d(TAG, o.getClass().getSimpleName() + ": " + msg);
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	private static List<Class> log = new ArrayList<Class>();
	static {
//		log.add(TransitionApps.class);
//		log.add(WidgetConfigurationActivity.class);
//		log.add(WidgetProvider.class);
		log.add(LocationObserver.class);
//		log.add(BackgroundService.class);
//		log.add(SyncThread.class);
	}
	
	/**
	 * prints debug output to log as well as toasting it
	 * @param o
	 * @param msg
	 */
	public static void dToast(Class<?> o, String msg) {
		try {
			dToast(o.newInstance(), msg);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public static void dToast(Object o, String msg) {
		if (Utils.D && Utils.Dtoast && log.contains(o.getClass())) {
			UIUtils.shortToast(AppKickerApplication.getAppContext(), msg);
			Log.w(TAG, "___TOAST___" + o.getClass().getSimpleName() + ": " + msg );
		}
	}
	
	public static void dToast(String msg) {
		if (Utils.D) {
			dToast(AppKickerApplication.getAppContext(), msg);
		}
	}
	
	public static Object getLastOfList(List<?> l) {
		final int s = l.size(); 
		if (s == 0) return null;
		return l.get(s-1);
	}

	public static Object getNLastOfList(List<?> l, int n) {
		final int s = l.size(); 
		if (s == 0) return null;
		return l.get(s-n-1);
	}

	
	/**
	 * whether the user did acknowledge the disclaimer or not
	 * 
	 * @return
	 */
	public static boolean isDisclaimerAcknowledged(Context c) {
		SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return settings.getBoolean(DISCLAIMER_ACK, false);
	}
	
	/**
	 * @param c
	 */
	public static void setDisclaimerAcknowledged(Context c) {
		SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(DISCLAIMER_ACK, true);
		editor.commit();
	}
	
	public static long getLastTimeAskedForDisclaimer(Context c) {
		SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return settings.getLong(LASTTIMEASKED, 0);
	}
	
	public static void updateLastTimeAskedForDisclaimer(Context c) {
		SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong(LASTTIMEASKED, Utils.getCurrentTime());
		editor.commit();
	}
	
	/**
	 * set the disclaimer to be unacknowledged, e.g. if the user revokes permission to collect data on app usage.
	 * @param c
	 */
	public static void setDisclaimerNotAcknowledged(Context c) {
		SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(DISCLAIMER_ACK, false);
		editor.commit();
	}
	
	
	/**
	 * Generates a md5 Hash of the string that's given
	 * 
	 * @param s
	 *            string to hash
	 * @return md5 Hash or if error null
	 */
	public static String md5(String s) {
		if (s == null) {
			e(null, "s == null");
			return null;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
	        digest.reset();
	        digest.update(s.getBytes("UTF-8"));
	        byte[] a = digest.digest();
	        int len = a.length;
	        StringBuilder sb = new StringBuilder(len << 1);
	        for (int i = 0; i < len; i++) {
	            sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
	            sb.append(Character.forDigit(a[i] & 0x0f, 16));
	        }
	        return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e(null, "md5 NoSuchAlgorithmException: " + e.getMessage());
		} catch (NullPointerException e) {
			e(null, " md5 NullPointerException: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e(null, " md5 UnsupportedEncodingException: " + e.getMessage());
		}
		e(null, " md5 sthg wrong...");

		return null;
	}


	public static void copyStream(InputStream is, OutputStream os) {
		final int buffer_size = 1024;
		Utils.d(Utils.class, "copying stream from is to os");
		try {
			byte[] bytes = new byte[buffer_size];
			for (;;) {
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
		} catch (Exception ex) {
			Utils.e(Utils.class, ex.getLocalizedMessage());
		}
	}

	public static void d(String string) {
		d(null, string);
	}

	public static void d(ArrayList<AppUsageEvent> appHistory) {
			Utils.d("applog", "---------------");
			for (AppUsageEvent a : appHistory) {
				Utils.d("applog", a.toStringLong());
			}
	}

	/**
	 * returns true if there is a launcher intent for the app, i.e. this app is shown in the app drawer
	 * @param packageName
	 * @param context
	 * @return
	 */
	public static boolean appIsShownInLaucher(String packageName, Context context) {
		Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
		return (intent != null);
	}

	

	/**
	 * Tells us, whether we can show the app in the kickers
	 * @param pname
	 * @return
	 */
	public static boolean canBeShownInKicker(String pname, Context c) {
		PackageManager pm = c.getPackageManager();
		
		// does it have a launcher intent?
		final Intent launchintent = pm.getLaunchIntentForPackage(pname);
		if (launchintent == null) {
//			Utils.dToast(pname + " has no launchintent");
			return false;
		}

		// does it have app info?
		final ResolveInfo ris = pm.resolveActivity(launchintent, 0);
		if (ris == null) {
//			Utils.dToast(pname + " has no ris");
			return false;
		}

		// does it have a name?
		final CharSequence label = ris.loadLabel(pm);
		if (label == null) {
//			Utils.dToast(pname + " has no label");
			return false;
		}
		
		// does it have an icon?
		final Drawable icon = ris.loadIcon(pm);
		if (icon == null) {
//			Utils.dToast(pname + " has no icon");
			return false;
		}
		return true;
	}
	
	
	
	public static String toString(int[] appWidgetIds) {
		StringBuffer sb = new StringBuffer("|");
		for (int i : appWidgetIds) {
			sb.append(i);
			sb.append("|");
		}
		
		return sb.toString();
	}


    /**
     * Returns an array of package names for the given array of resolve infos.
     * 
     * @param a
     * @return
     */
    public static String[] toStrigA(ResolveInfo[] a) {
    	String[] s = new String[]{};
    	for (int i=0; i<a.length;i++) {
    		s[i]=a[i].activityInfo.packageName;
    	}
		return s;
    }

	/**
	 * determines whether we should ask again for the disclaimer after waiting some time
	 * @param context
	 * @return
	 */
	public static boolean askAgainForDisclaimer(Context context) {
		long lastTimeAsked;
		lastTimeAsked = Utils.getLastTimeAskedForDisclaimer(context); 
		return (Utils.getCurrentTime() - lastTimeAsked) > DateUtils.WEEK_IN_MILLIS;
		
		
	}

	
    
	
}
