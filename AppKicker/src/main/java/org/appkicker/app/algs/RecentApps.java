package org.appkicker.app.algs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.appkicker.app.utils.AppKickerApplication;
import org.appkicker.app.utils.Utils;
import org.appkicker.app.widget.WidgetProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Returns the recent apps to be filled into the kicker widget. Apps are listed
 * in order of last usage. Only apps that have an icon in the launcher are
 * returned.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 * 
 */
public class RecentApps extends BroadcastReceiver {

	private static String CACHE = RecentApps.class.getName();

	private static final int HISTORY_LENGTH = WidgetProvider.NUM_KICKER_APPS_TO_SHOW * 2;
	
	private static Queue<String> recentApps;
	
	private static boolean isRegistered = false;
	
	public RecentApps() {
		if (!isRegistered) {
			IntentFilter ifi = new IntentFilter();
			ifi.addAction(Intent.ACTION_SCREEN_OFF);
			ifi.addAction(Intent.ACTION_SHUTDOWN);
			AppKickerApplication.getAppContext().registerReceiver(this, ifi);
			isRegistered = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.appkicker.app.logging.AppChangeListener#onAppChange(java.lang.String, java.lang.String, android.content.Context)
	 */
	public void onAppChange(String previous, String current) {
		loadCacheFromFile();
		
		Utils.dToast("RecentApps: appChange " + previous + ">" + current);
		
		// we only include those apps, that can be shown in the kicker later
		if (current != null && Utils.canBeShownInKicker(current, AppKickerApplication.getAppContext())) {
			recentApps.remove(current);
			if (recentApps.size() >= HISTORY_LENGTH) {
				recentApps.poll();
			}
			recentApps.offer(current);

		}
	}

	
	public static short getId() {
		return AppKickerFiller.RECENT_APPS_ALG_ID;
	}

	public static String[] getAppsForKicker() {
		loadCacheFromFile();

		String[] is = recentApps.toArray(new String[] {});
		
		// bring into reverse order
		String[] re = new String[is.length];
		for (int i = 0; i < re.length; i++) {
			re[i] = is[re.length - i - 1];
		}
		
		return re;
	}
	
	private static final Object recentAppsMutex = new Object();

	/**
	 * loads the persisted file if transitions are currently null
	 */
	@SuppressWarnings("unchecked")
	private static void loadCacheFromFile() {
		synchronized (recentAppsMutex) {
			
			// try to load if null
			if (recentApps == null) {
				try {
					Utils.d(new RecentApps(), "trying to load file from cache");
					Context context = AppKickerApplication.getAppContext();
					FileInputStream fis = context.openFileInput(CACHE);
					ObjectInputStream is = new ObjectInputStream(fis);
					recentApps = (Queue<String>) is.readObject();
					is.close();
				} catch (IOException e) {
					Utils.e(new RecentApps(), e.getLocalizedMessage());
					recentApps = new ArrayBlockingQueue<String>(HISTORY_LENGTH);
				} catch (ClassNotFoundException e) {
					Utils.e(new RecentApps(), e.getLocalizedMessage());
					recentApps = new ArrayBlockingQueue<String>(HISTORY_LENGTH);
				}

				// if still null, maybe because persisted object was empty or exception occurred
				if (recentApps == null) {
					recentApps = new ArrayBlockingQueue<String>(HISTORY_LENGTH);
				}
			}
			
		}		
	}
	
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Utils.d(this, "received: " + intent.getAction());
		if (WidgetProvider.INTENT_APPCHANGE_ACTION.equals(intent.getAction())) {
			// process app change
			String prev = intent.getStringExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDPREVIOUS);
			String curr = intent.getStringExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDCURRENT);
			onAppChange(prev, curr);
		} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) || Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
			// save if device is going screen-off
			synchronized (recentAppsMutex) {
				try {
					Utils.d(this, "writing memory to cache file");
					FileOutputStream fos = context.openFileOutput(CACHE, Context.MODE_PRIVATE);
					ObjectOutputStream os = new ObjectOutputStream(fos);
					os.writeObject(recentApps);
					os.close();
				} catch (IOException e) {
					Utils.e(this, e.getLocalizedMessage());
				}
			}
		}
	}

}
