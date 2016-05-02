package org.appkicker.app.logging;

import java.util.ArrayList;
import java.util.List;

import org.appkicker.app.data.db.AppUsageEventDAO;
import org.appkicker.app.data.entities.AppUsageEvent;
import org.appkicker.app.utils.Utils;
import org.appkicker.app.widget.WidgetProvider;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * <p>
 * This runnable observes a users device interaction and makes it persistent to
 * the local database. It tracks the application usage and saves events in
 * memory (in a list). At the end of a session (i.e. when the device goes into
 * standby) the data is saved to a local database on the mobile device.
 * database.
 * </p>
 * 
 * <p>
 * This component is a thread that can be controlled through the start and pause
 * methods.
 * </p>
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class AppUsageLogger extends HandlerThread {
	
	/**
	 * informs all listeners about application change
	 * 
	 * @param previous
	 * @param current
	 */
	private void fireAppChange(String previous, String current) {
		
		// Update all interested receivers via intent
		Intent updateKicker = new Intent();
		updateKicker.setAction(WidgetProvider.INTENT_APPCHANGE_ACTION);
		updateKicker.putExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDPREVIOUS, previous);
		updateKicker.putExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDCURRENT, current);
		context.sendBroadcast(updateKicker);
		Utils.d(this, "broadcast intent: " + updateKicker.getAction());
		
		Utils.d(this, "fire app change: " + previous + " -> " + current);
		
	}
	
	/** number of recent tasks that will be tracked form the API */
	private static final int MAX_TASKS = 1;

	/** period of running app usage checks (in milliseconds) */
	public static long TIME_APPCHECKINTERVAL = 500;
	private static final long TIME_IMMEDIATELY = 5;

	/** states of the thread */
	private static final int DO_OBSERVE = 0;
	private static final int DO_PAUSE = 1;
	private static final int DO_START = 2;

	/** task id for the app that was open before going into standby */
	private static final int LAST_BEFORE_STANDBY = -1;

	/** package name of the phone app */
	private static final Object PHONEPACKAGENAME = "com.android.phone";

	/** number of items in table that we cache on the mobile before sending data to the server */
	public static final int MIN_INTERACTIONS_TO_SEND = 2;

	/** history of apps that have been used in current session */
	public static ArrayList<AppUsageEvent> appHistory;

	/** whether the logger should be active or not */
	public static boolean ACTIVE = true;

	/** general history of other app events */
	private ArrayList<AppUsageEvent> generalInteractionHistory;
	
	/** reference to the activity manager for retrieving info on current apps */
	private ActivityManager activityManager;

	/** the previous app */
	private AppUsageEvent lastApp;

	/** list of recently running apps */
	private List<RunningTaskInfo> apps = null;
	
	/** handler for thread messages */
	private Handler mHandler = null;

	/** number of apps that are already in our list */
	private int lengthHistory;

	/** context of the app */
	private Context context;

	/** to check if keyboard is locked */
	private KeyguardManager keyManager;

	/** special package name for logging special events not related to app usage */
	private static final String SPECIALLOGGINGPACKAGE = "-event-";

	/** semaphore for synchronized blocks, since some other threads are using this component */
	private static final Object semaphore = new Object();

	/** singleton instance of the logger */
	private static AppUsageLogger instance = null;

	/**
	 * get the singleton
	 * 
	 * @param c
	 * @return
	 */
	public static AppUsageLogger getAppUsageLogger(Context c) {
		Utils.d(AppUsageLogger.class, "getAppUsageLogger");
		synchronized (semaphore) {
			if (instance == null) {
				instance = new AppUsageLogger(c);
				instance.start();
				instance.waitUntilReady();
			}
		}
		
		return instance;
	}
	
	
	/**
	 * creates a new app logger that needs to be started afterwards
	 * @param context
	 */
	private AppUsageLogger(Context context) {
		super("AppSensorThread");
		
		// better synchronize
//		appChangeListeners = new HashSet<AppChangeListener>();
		
		Utils.d(AppUsageLogger.class, "created new instance");
		this.context = context;
		keyManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		appHistory = new ArrayList<AppUsageEvent>();
		generalInteractionHistory = new ArrayList<AppUsageEvent>();
		activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		
		
		
	}

	/**
	 * Analyzes the runtime of the currently used application and adds the
	 * information to the history (list of recent apps) in memory.
	 * 
	 * @param forceClosingHistory
	 */
	private void observeCurrentApp(boolean forceClosingHistory) {
		synchronized (appHistory) {
			
			//Utils.d(appHistory);

			apps = activityManager.getRunningTasks(MAX_TASKS);

			if (apps == null) {
				Utils.e(this, "ups, we cannot look which apps are running; API ERROR");
				return;
			}
			
			/** fill list with running apps */
			for (RunningTaskInfo app : apps) {
				// Utils.d2(this, "we are in the RunningTaskInfo loop");

				lengthHistory = appHistory.size();

				if (lengthHistory > 0) {

					lastApp = appHistory.get(appHistory.size() - 1);

					if (app.id == lastApp.taskID) {

						/** here the previous app is still running */
						lastApp.runtime = Utils.getCurrentTime() - lastApp.starttime;
						lastApp.longitude = LocationObserver.longitude;
						lastApp.latitude = LocationObserver.latitude;
						lastApp.accuracy = LocationObserver.accuracy;
						lastApp.speed = LocationObserver.age;
						lastApp.altitude = LocationObserver.altitude;
						lastApp.powerstate = HardwareObserver.powerstate;
						
						/** TODO update "running" attributes by build sliding average*/
						lastApp.wifistate = HardwareObserver.wifistate;
						lastApp.bluetoothstate = HardwareObserver.bluetoothstate;
						lastApp.headphones += HardwareObserver.headphones;
						
						// HardwareObserver.orientation is either +1 or -1
						HardwareObserver.updateOrientation(context);
						lastApp.orientation += HardwareObserver.orientation;
						
						if (forceClosingHistory) {
							lastApp.taskID = LAST_BEFORE_STANDBY;
						}
					} else {
						/** here the user has opened a different app */
						if (lastApp.taskID != LAST_BEFORE_STANDBY) {
							// add runtime to the old app
							lastApp.runtime = (Utils.getCurrentTime() - lastApp.starttime);
						}

						Utils.d(this, lastApp.packageName + "(taskID:" + lastApp.taskID + ") " + " was running " + lastApp.runtime);
						Utils.d(this, lastApp.packageName + " ran " + Math.round(lastApp.runtime/1000) + " s");
						
						// inform listeners about change
						String currentPackageName = app.baseActivity.getPackageName();
						fireAppChange(lastApp.packageName, currentPackageName);
						
						if (!forceClosingHistory) {
							
							// create a usage event and fill in all the context information
							AppUsageEvent aue = new AppUsageEvent();
							aue.taskID = app.id;
							aue.packageName = currentPackageName;
							aue.eventtype = AppUsageEvent.EVENT_INUSE;
							aue.starttime = Utils.getCurrentTime();
							aue.runtime = 0; // since we just started
							aue.longitude = LocationObserver.longitude;
							aue.latitude = LocationObserver.latitude;
							aue.accuracy = LocationObserver.accuracy;
							aue.speed = LocationObserver.age;
							aue.altitude = LocationObserver.altitude;
							aue.powerstate = HardwareObserver.powerstate;
							aue.wifistate = HardwareObserver.wifistate;
							aue.bluetoothstate = HardwareObserver.bluetoothstate;
							aue.headphones = HardwareObserver.headphones;
							aue.orientation = HardwareObserver.orientation;
							aue.timestampOfLastScreenOn = HardwareObserver.timestampOfLastScreenOn;
							aue.syncStatus = AppUsageEvent.STATUS_LOCAL_ONLY;
							
							appHistory.add(aue);
							Utils.d(this, "added the following app to history: " + aue.packageName);

						}
					}

				} else {

					/** here the user starts his first app */
					AppUsageEvent aue = new AppUsageEvent();
					aue.taskID = app.id;
					aue.runtime = 0; // since we just started
					aue.packageName = app.baseActivity.getPackageName();
					aue.eventtype = AppUsageEvent.EVENT_INUSE;
					aue.starttime = Utils.getCurrentTime();
					aue.longitude = LocationObserver.longitude;
					aue.latitude = LocationObserver.latitude;
					aue.accuracy = LocationObserver.accuracy;
					aue.powerlevel = HardwareObserver.powerlevel;
					aue.powerstate = HardwareObserver.powerstate;
					aue.speed = LocationObserver.age;
					aue.altitude = LocationObserver.altitude;
					aue.wifistate = HardwareObserver.wifistate;
					aue.bluetoothstate = HardwareObserver.bluetoothstate;
					aue.headphones = HardwareObserver.headphones;
					aue.orientation = HardwareObserver.orientation;
					aue.timestampOfLastScreenOn = HardwareObserver.timestampOfLastScreenOn;
					aue.syncStatus = AppUsageEvent.STATUS_LOCAL_ONLY;
					
					appHistory.add(aue);
					Utils.d(this,"added the first app to history: " + aue.packageName);
					
					// inform listeners about change
					String currentPackageName = app.baseActivity.getPackageName();
					fireAppChange(null, currentPackageName);

				}
				
				
				if (Utils.D)
				{
					AppUsageEvent li = getLastInteraction();
					Utils.d(this, "last interaction: " + li.packageName + "("+li.runtime+" ms)");
				}
			}
		}
	}


	@Deprecated
	public AppUsageEvent getPreviousInteraction() {
		int s = appHistory.size();
		if (s > 1) {
			return appHistory.get(s-2); 
		} else {
			return null;
		}
	}
	
	private AppUsageEvent getLastInteraction() {
		int s = appHistory.size();
		if (s > 0) {
			return appHistory.get(s-1); 
		} else {
			return null;
		}
	}

	/**
	 * Makes the history of app interaction, which is normally written to the
	 * list in memory, persistent to the local database. Usually, this is done
	 * when the device goes into standby mode.
	 */
	private void makeAppHistoryPersistent() {
		Utils.d(this, "making app history persistent from memory");
		AppUsageEventDAO d = new AppUsageEventDAO(context);
		d.openWrite();
		synchronized (appHistory) {
			// we do not need to persist those app usages with zero runtime,
			// since they result from errors (e.g. logging when users klicks in
			// standby mode
			d.insertWithoutZeroUsage(appHistory);
			appHistory.clear();
		}
		synchronized (generalInteractionHistory) {
			d.insert(generalInteractionHistory);
			generalInteractionHistory.clear();
		}
		d.close();
	}
	
	

	public void logCustom(AppUsageEvent aue) {
		synchronized (generalInteractionHistory) {
			generalInteractionHistory.add(aue);
		}
	}

	public void logAppInstalled(String packageName) {
		logCustom(new AppUsageEvent(packageName, Utils.getCurrentTime(), 0,
				AppUsageEvent.EVENT_INSTALLED, AppUsageEvent.STATUS_LOCAL_ONLY));
	}

	public void logAppRemoved(String packageName) {

		logCustom(new AppUsageEvent(packageName, Utils.getCurrentTime(), 0,
				AppUsageEvent.EVENT_UNINSTALLED,
				AppUsageEvent.STATUS_LOCAL_ONLY));

	}

	public void logAppUpdated(String packageName) {

		logCustom(new AppUsageEvent(packageName, Utils.getCurrentTime(), 0,
				AppUsageEvent.EVENT_UPDATE, AppUsageEvent.STATUS_LOCAL_ONLY));

	}

	public void logDeviceScreenOn() {
		Utils.d(this,"logging EVENT_SCREENON");
		logCustom(new AppUsageEvent(SPECIALLOGGINGPACKAGE, Utils.getCurrentTime(), 0, AppUsageEvent.EVENT_SCREENON, AppUsageEvent.STATUS_LOCAL_ONLY));
	}	
	
	public void logDeviceScreenOff() {
		Utils.d(this,"logging EVENT_SCREENOFF");
		logCustom(new AppUsageEvent(SPECIALLOGGINGPACKAGE, Utils.getCurrentTime(), 0, AppUsageEvent.EVENT_SCREENOFF, AppUsageEvent.STATUS_LOCAL_ONLY));
	}	
	
	public void logDeviceBooted() {
		Utils.d(this,"logging EVENT_BOOT");
		HardwareObserver.timestampOfLastScreenOn = Utils.getCurrentTime(); // obviously user has turned on screen
		synchronized (generalInteractionHistory) {
			generalInteractionHistory.add(new AppUsageEvent(SPECIALLOGGINGPACKAGE, Utils.getCurrentTime(), 0, AppUsageEvent.EVENT_BOOT, AppUsageEvent.STATUS_LOCAL_ONLY));
		}
	}
	
	public void logWidgetCreated(int widgetAlgorithm, int widgetId) {
		Utils.d(this,"logging WIDGET CREATED");
		synchronized (generalInteractionHistory) {
			AppUsageEvent aue = new AppUsageEvent(
					SPECIALLOGGINGPACKAGE, 
					Utils.getCurrentTime(), 
					widgetAlgorithm, 
					AppUsageEvent.EVENT_WIDGETCREATED, 
					AppUsageEvent.STATUS_LOCAL_ONLY);
			aue.orientation = (short)widgetId;
			generalInteractionHistory.add(aue);
		}
	}
	
	public void logWidgetRemoved(int widgetId) {
		Utils.d(this,"logging WIDGET CREATED");
		synchronized (generalInteractionHistory) {
			AppUsageEvent aue = new AppUsageEvent(
					SPECIALLOGGINGPACKAGE, 
					Utils.getCurrentTime(), 
					0, 
					AppUsageEvent.EVENT_WIDGETREMOVED, 
					AppUsageEvent.STATUS_LOCAL_ONLY);
			aue.orientation = (short)widgetId;
			generalInteractionHistory.add(aue);
		}	}

	
	
	/**
	 * logs all packages that are currently installed on the device 
	 */
	public void logAlreadyInstalledPackages() {

		List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);
		ArrayList<AppUsageEvent> installedApps = new ArrayList<AppUsageEvent>();
		Log.d(Utils.TAG, "persisting " + packs.size() + " already installed apps");

		for (PackageInfo pi : packs) {
			
			AppUsageEvent aue; 
			boolean launchable = Utils.appIsShownInLaucher(pi.packageName, context);
			if (launchable) {
				// create a usage event and fill in all the context information
				aue = new AppUsageEvent(
					pi.packageName,
					Utils.getCurrentTime(), 0,
					AppUsageEvent.EVENT_SNAPSHOT_LAUNCHABLE,
					AppUsageEvent.STATUS_LOCAL_ONLY);
			} else {
				aue = new AppUsageEvent(
						pi.packageName,
						Utils.getCurrentTime(), 0,
						AppUsageEvent.EVENT_SNAPSHOT,
						AppUsageEvent.STATUS_LOCAL_ONLY);
			}
			
			installedApps.add(aue);
		}
		
		synchronized (generalInteractionHistory) {
			generalInteractionHistory.addAll(installedApps);
		}
	   
	}

	

	public void logDeviceTurnedOff() {
		Utils.d(this,"logging EVENT_POWEROFF");
		synchronized (generalInteractionHistory) {
			generalInteractionHistory.add(new AppUsageEvent(SPECIALLOGGINGPACKAGE, Utils.getCurrentTime(), 0, AppUsageEvent.EVENT_POWEROFF, AppUsageEvent.STATUS_LOCAL_ONLY));
		}
		
		// better make everything persistent when turning off ;)
		makeAppHistoryPersistent();
	}
	

	
	private synchronized void waitUntilReady() {
		Utils.d(this,"waitUntilReady: waiting...");
		
		if (mHandler == null) {

			Callback messageCallback = new Callback() {

				public boolean handleMessage(Message msg) {
					
					keyManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
					boolean rm = keyManager.inKeyguardRestrictedInputMode();
					boolean ph = isUserOnPhone();

					switch (msg.what) {

					case DO_OBSERVE:

						// check whether keys are locked or user is on phone
						rm = keyManager.inKeyguardRestrictedInputMode();
						ph = isUserOnPhone();

						final AppUsageEvent lasi = getLastInteraction();
						String lasi_packageName = (lasi == null) ? null : lasi.packageName;
						
						if (
								( // device is in restricted mode and user currently not in a phone call
										rm && !ph
								)
								||
								( // screen is locked and users is on phone
										(HardwareObserver.screenState == HardwareObserver.SCREEN_OFF)
										&& !PHONEPACKAGENAME.equals(lasi_packageName))
								) {
							// screen still to be locked
							mHandler.sendEmptyMessageDelayed(DO_START, TIME_APPCHECKINTERVAL);
						} else {
							observeCurrentApp(false);
							mHandler.sendEmptyMessageDelayed(DO_OBSERVE, TIME_APPCHECKINTERVAL);
						}
						break;

					case DO_START: // Utils.d(AppUsageLogger.this,
									// "case DO_START");

						rm = keyManager.inKeyguardRestrictedInputMode();
						ph = isUserOnPhone();

						Utils.d(AppUsageLogger.this, "in restricted mode" + rm);
						Utils.d(AppUsageLogger.this, "in phone mode" + ph);
						if (rm && !ph) {
							// screen still seems to be locked and there is no
							// incoming call
							Utils.d(AppUsageLogger.this,
									"device is in restricted mode");
							mHandler.sendEmptyMessageDelayed(DO_START, TIME_APPCHECKINTERVAL);
						} else {
							// screen was unlocked by user
							Utils.d(AppUsageLogger.this, "device is not in restricted mode anymore");
							mHandler.sendEmptyMessageDelayed(DO_OBSERVE, TIME_IMMEDIATELY);
						}
						break;

					case DO_PAUSE: // Utils.d(AppUsageLogger.this,
									// "case DO_PAUSE");
						observeCurrentApp(true);
						mHandler.removeMessages(DO_START);
						mHandler.removeMessages(DO_OBSERVE);
						// tidy up everything and make history persistent
						makeAppHistoryPersistent();
						break;
					}

					return false;
				}
			};

			mHandler = new Handler(getLooper(), messageCallback);
		}

		Utils.d(this,"waitUntilReady: got it!");
	}

	
	
	
	public synchronized void pauseLogging() {
		waitUntilReady();
		// send message to the worker thread if initialized
		Utils.d(this, "pause logging");

		Utils.d(this, "send DO_PAUSE");
		mHandler.sendEmptyMessageDelayed(DO_PAUSE, TIME_IMMEDIATELY);
	}
	
	public synchronized void startLogging() {
		Utils.d(this, "start logging");
		waitUntilReady();
		
		// maybe timestamp is zero, e.g. if phone comes in after device was booted
		if (HardwareObserver.timestampOfLastScreenOn == 0) {
			HardwareObserver.timestampOfLastScreenOn = Utils.getCurrentTime();
		}

		Utils.d(this, "send DO_START");
		mHandler.sendEmptyMessageDelayed(DO_START, TIME_IMMEDIATELY);
	}

	
	
	private boolean isUserOnPhone() {
		List<RunningTaskInfo> current = activityManager.getRunningTasks(1);
		if (current.size() > 0) {
			RunningTaskInfo rti = current.get(0);
			boolean userIsOnThePhone = rti.baseActivity.getPackageName().equals(PHONEPACKAGENAME);
			return userIsOnThePhone;
		}
		return false;
		
	}
	/**
	 * Call this method if device screen turns off to check whether user is
	 * currently on the phone or not (some phones turn the screen off when the
	 * user is coming close to the display to prevent unintended button
	 * pressing). If phone app is running, we will continue to log, otherwise we
	 * will pause the logging.
	 */
	public void checkStandByOnSceenOff() {
		boolean userIsOnThePhone = false;
		
		List<RunningTaskInfo> current = activityManager.getRunningTasks(1);
		if (current.size() > 0) {
			RunningTaskInfo rti = current.get(0);
			userIsOnThePhone = rti.baseActivity.getPackageName().equals(PHONEPACKAGENAME);
		}
		
		// only pause if user is not on the phone, otherwise track phone usage
		if (!userIsOnThePhone) {
			pauseLogging();
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		Utils.d2(this, "destroy()");
	};

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		Utils.d2(this, "finalize()");
	}




	



}
