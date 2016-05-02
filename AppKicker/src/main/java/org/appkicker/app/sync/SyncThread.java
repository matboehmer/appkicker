package org.appkicker.app.sync;

import java.util.List;

import org.appkicker.app.data.db.AppUsageEventDAO;
import org.appkicker.app.data.entities.AppUsageEvent;
import org.appkicker.app.logging.AppUsageLogger;
import org.appkicker.app.logging.DeviceObserver;
import org.appkicker.app.logging.LocationObserver;
import org.appkicker.app.utils.CSVCompressor;
import org.appkicker.app.utils.NetUtils;
import org.appkicker.app.utils.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.text.format.DateUtils;

/**
 * This thread is responsible for requesting the sync of data with the server
 * 
 * @author Lyubomir Ganev
 *
 */
public class SyncThread extends HandlerThread {
	
	private static final Object semaphoreSingleton = new Object();
	
	/** handler for thread messages */
	private Handler mHandler = null;

	/** context of the app */
	protected static Context context;

	private static final int DO_REQUEST_SYNC = 0;
	
	/** frequency of requesting sync with server */
	private static long TIME_SYNC_INTERVAL_REQUEST = Settings.SYNCINTERVALREQUEST;
	
	/** frequency of running sync with server */
	private static long TIME_SYNC_INTERVAL = Settings.SYNCINTERVAL;
	
	
	private static final long TIME_IMMEDIATELY = 100;

	/** maximum age of local data points after syncing to server */
	private static final long MAX_AGE_OFLOCALDATA = DateUtils.HOUR_IN_MILLIS * 12;

	/** maximum number of iterations for trying to send data to server */
	private static final int MAX_LOOPS = 100;
	
	/** maximum number of data points to be synced with server in one batch */
	private static int MAX_SYNCED_APP_USAGES = 25;
	
	/** singleton */
	private static SyncThread instance;

	/**
	 * singleton for this component
	 * @param context
	 * @return
	 */
	public static SyncThread getSyncThread(Context context) {
		synchronized (semaphoreSingleton) {
			if (instance == null) {
				instance = new SyncThread("SyncThread", context);
				
				// always start the thread directly, nobody requires it unstarted
				instance.start();
				
				// and always wait for looper and handler to be created
				instance.waitUntilReady();
				instance.requestSync();
				Utils.d(instance, "getSyncThread");
			}
		}
		return instance;
	}
	
	
	/**
	 * creates a new app logger that needs to be started afterwards
	 * @param context
	 */
	private SyncThread(String name, Context c) {
		super(name);
		Utils.dToast(this, "created new SyncThread");
		context = c;
	}
	
	/**
	 * manually request an immediate sync of the data
	 */
	public void requestSync() {
		waitUntilReady();
		Utils.dToast(this, "sync was requested");
		mHandler.sendEmptyMessageDelayed(DO_REQUEST_SYNC, TIME_IMMEDIATELY);
	}
	

	public void setTimeSyncInterval(int minutes) {
		
		waitUntilReady();
		Utils.dToast(this, "seeting new time: " + minutes + "minutes");
		
		TIME_SYNC_INTERVAL_REQUEST = minutes * DateUtils.MINUTE_IN_MILLIS;
		
		mHandler.sendEmptyMessageDelayed(DO_REQUEST_SYNC, TIME_SYNC_INTERVAL_REQUEST);
		
	}


	/**
	 * do schedule a sync
	 */
	private void doSync() {
		
		Utils.dToast(this, "try to sync now");
		
		if (!Utils.isDisclaimerAcknowledged(context)) {
			Utils.dToast(this, "no sync b/c discl");
			return;
		}
		
		SharedPreferences settings = context.getSharedPreferences(Utils.PREFS_NAME, Context.MODE_PRIVATE);
		long lastTime = settings.getLong(Utils.SETTINGS_TIMESTAMP_LAST_SERVERSYNC, 0);
		long currentTime = Utils.getCurrentTime();
		
		if (currentTime > (lastTime + TIME_SYNC_INTERVAL)) {
			Utils.dToast(this, "sendAppHistoryToServer");
			sendAppHistoryToServer();
		}
		
	}

	
	private synchronized void waitUntilReady() {
		Utils.d(this, "waitUntilReady: waiting...");

		
		if (mHandler == null) {
			Callback callback = new Callback() {
	
				public boolean handleMessage(Message msg) {
	
					switch (msg.what) {
					case DO_REQUEST_SYNC:
						Utils.d(SyncThread.instance, "event DO_REQUEST_SYNC");
						doSync();
	
						Utils.d(SyncThread.instance, "TIME_SYNC_INTERVAL: "	+ TIME_SYNC_INTERVAL_REQUEST);
						mHandler.sendEmptyMessageDelayed(DO_REQUEST_SYNC, TIME_SYNC_INTERVAL_REQUEST);
						break;
	
					default:
						break;
					}
	
					return false;
				}
	
			};
		
			mHandler = new Handler(getLooper(), callback);
		}
	
		Utils.d(this, "waitUntilReady: got it!");
	}
	
	
	
    private synchronized boolean sendAppHistoryToServer() {
    	
//    	Utils.dToast("sendAppHistoryToServer");
    	
    	
		final NetUtils netUtils = new NetUtils(context);

		// get everything from the database
		final AppUsageEventDAO d = new AppUsageEventDAO(context);
		d.openWrite();
		
		boolean result = false;

		/*
		 * This is loop is terminated when there are no more portions of records
		 * to be synced or the sync failed at some point. There is a just
		 * in case limit of MAX_LOOPS iterations, so that it does not loop infinitely
		 * in some exceptional case.
		 */
		int loopLimit = 1;
		while(loopLimit < MAX_LOOPS) {
			loopLimit++;
			final List<AppUsageEvent> notSynced = d.getAndUpdateNonSyncedInterestsAsEntities(MAX_SYNCED_APP_USAGES);
			Utils.d(this, "data points not synced: " + notSynced.size());
			
			// only send something if we have something in the database
			if (notSynced.size() > AppUsageLogger.MIN_INTERACTIONS_TO_SEND) {
				
				CSVCompressor compressor = new CSVCompressor();
				
				// add header
				compressor.add(new String[]{
						"iid", // installation id
						"res", // resolution
						"mod", // model name
						"api", // api level
						"apv", // appsensor version
						"eve", // event type
						"pac", // package name
						"sta", // timestamp of starttime in milliseconds UTC
						"off", // offset to UTC in hours
						"run", // runtime of app in milliseconds
						"lon", // longitude of interaction
						"lat", // latitude of interaction
						"alt", // altitude
						"spe", // speed
						"ccd", // country code of this location
						"acc", // accuracy of location information
						"pos", // power state
						"pol", // power level
						"blu", // bluetooth state
						"wif", // wifi state 
						"hea", // headphones state
						"ori", // orientation of the device
						"lso"  // timestamp of last screen on				
				});
				
				// add rows from interaction history
				for (AppUsageEvent aue : notSynced) {
					Utils.d(this, "added " + aue.toStringLong());
					compressor.add(new String[]{
						DeviceObserver.id(context),
						DeviceObserver.resolution(context),
						DeviceObserver.modelName(),
						"" + DeviceObserver.apiLevel(),
						"" + DeviceObserver.appVersion(context),
						"" + aue.eventtype,
						aue.packageName,
						"" + aue.starttime, // starttime in UTC milliseconds
						"" + Utils.utcOFF(), // offset to UTC in hours
						"" + aue.runtime,
						"" + aue.longitude,
						"" + aue.latitude,
						"" + aue.altitude,
						"" + aue.speed,
						"" + LocationObserver.countryCode,
						"" + aue.accuracy,
						"" + aue.powerstate,
						"" + aue.powerlevel,	
						"" + aue.bluetoothstate,
						"" + aue.wifistate,
						"" + aue.headphones,
						"" + aue.orientation,
						"" + aue.timestampOfLastScreenOn
					});
				}
				
				String sentApps = compressor.getCSV();
				// send to the server and remove from local db if worked
				result = netUtils.postToURL(NetUtils.getScriptURL(context), sentApps);
				
				Utils.d(this, sentApps);
				
				// update sync status to server persisted status 
				// only if the send was successful
				if (result) {
					int number = d.updateSyncingToServerPersisted();
					Utils.dToast(this, "Persisted # to server:" + number);
				}
				else break; // if the sync failed, stop the whole sync operation of all portions of records
			}
			else {
				Utils.d(this, "only " + notSynced.size() + " entries, but min is " +  AppUsageLogger.MIN_INTERACTIONS_TO_SEND);
				result = true;
				break; // no more portions of records to be sent, so stop the syncing of portions of records
			}
		}
		
		// clear too old server persisted records
		int rowsAffected = d.deleteOldSyncedUsageEvents(Utils.getCurrentTime() - MAX_AGE_OFLOCALDATA);
		Utils.d(this, "Deleted # of old persisted app usage:" + rowsAffected);
		d.close();
		return result;
	}
	
}
