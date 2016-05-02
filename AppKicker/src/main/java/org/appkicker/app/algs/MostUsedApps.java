package org.appkicker.app.algs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.appkicker.app.utils.AppKickerApplication;
import org.appkicker.app.utils.Utils;
import org.appkicker.app.widget.WidgetProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * This {@link AppKickerFiller} implements an approach that schedules the most used apps
 * into the kicker.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class MostUsedApps extends BroadcastReceiver {

	
	private static Map<String, Integer> usageCounter;
	
	
	private static final Object cacheMutex = new Object();
	
	private static final String NULLSTRING = "app.null";
	

	private static boolean isRegistered = false;
	
	public MostUsedApps() {
		if (!isRegistered) {
			IntentFilter ifi = new IntentFilter();
			ifi.addAction(Intent.ACTION_SCREEN_OFF);
			ifi.addAction(Intent.ACTION_SHUTDOWN);
			AppKickerApplication.getAppContext().registerReceiver(this, ifi);
			isRegistered = true;
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Utils.d(this, "received: " + intent.getAction());
		
		if (WidgetProvider.INTENT_APPCHANGE_ACTION.equals(intent.getAction())) {
			String prev = intent.getStringExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDPREVIOUS);
			String curr = intent.getStringExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDCURRENT);
			logTransition(prev, curr);
			
		} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) || Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
			// save if device is going screen-off
			synchronized (cacheMutex) {
				try {
					Utils.d(this, "writing memory to cache file");
					FileOutputStream fos = context.openFileOutput(CACHE, Context.MODE_PRIVATE);
					ObjectOutputStream os = new ObjectOutputStream(fos);
					os.writeObject(usageCounter);
					os.close();
				} catch (IOException e) {
					Utils.e(this, e.getLocalizedMessage());
				}
			}
		}
	}

	private void logTransition(String prev, String curr) {
		loadCacheFromFile();
		
		if (prev == null) prev = NULLSTRING;
		if (curr == null) curr = NULLSTRING;
		
		
		boolean currLaunchable = Utils.appIsShownInLaucher(curr, AppKickerApplication.getAppContext());
		
		if (currLaunchable) {
			countUsage(curr);
		}
		
	}

	private void countUsage(String curr) {
		int is = getTransitionsCount(curr);
		setTransitionsCount(curr, is+1);
		
		Utils.d(this,"counted usage: "+ curr + " : "+ (is+1));
		
	}
	
	
	private static void printTable() {
		if (Utils.D)
		synchronized (cacheMutex) {
			Utils.d(new MostUsedApps(), "db: ---------------------");
			if (usageCounter == null) return;
			for(String from : usageCounter.keySet()) {
				int i = usageCounter.get(from);
				Utils.d(new MostUsedApps(), "db: "+ from + " : " + i);
			}
			Utils.d(new MostUsedApps(), "db: ---------------------");
		}
	}
	
	private void setTransitionsCount(String curr, int i) {
		synchronized (cacheMutex) {
			usageCounter.put(curr, i);
		}
	}

	
	
	/**
	 * the number of transitions that have been recorded for that sequence
	 * 
	 * @param prev
	 * @param curr
	 * @return
	 */
	private int getTransitionsCount(String curr) {
		Integer field;
	
		synchronized (cacheMutex) {
			// get the row
			field = usageCounter.get(curr);
			if (field == null) {
				usageCounter.put(curr, 0);
				field = 0;
			}

		}
		return field;
	}

	private static String CACHE = MostUsedApps.class.getName();
	
	/**
	 * loads the persisted file if transitions are currently null
	 */
	@SuppressWarnings("unchecked")
	private static void loadCacheFromFile() {
		synchronized (cacheMutex) {
			
			// try to load if null
			if (usageCounter == null) {
				try {
					Utils.d(new MostUsedApps(), "trying to load file from cache");
					Context context = AppKickerApplication.getAppContext();
					FileInputStream fis = context.openFileInput(CACHE);
					ObjectInputStream is = new ObjectInputStream(fis);
					usageCounter = (Map<String, Integer>) is.readObject();
					is.close();
				} catch (IOException e) {
					Utils.e(new MostUsedApps(), e.getLocalizedMessage());
					usageCounter = new HashMap<String, Integer>(10);
				} catch (ClassNotFoundException e) {
					Utils.e(new MostUsedApps(), e.getLocalizedMessage());
					usageCounter = new HashMap<String, Integer>(10);
				}

				// if still null, maybe because persisted object was empty or exception occurred
				if (usageCounter == null) {
					usageCounter = new HashMap<String, Integer>(10);
				}
			}
			
		}		
	}


	
	public static String[] getAppsForKicker() {
		loadCacheFromFile();
		
		
		printTable();

		
		Utils.d(new MostUsedApps(), "------------ BOF getAppsForKicker ------");
		
		synchronized (cacheMutex) {
			
			
			List<String> candidates = new Vector<String>();
			
			
			candidates.addAll(usageCounter.keySet());
			
			Collections.sort(candidates,new Comparator<String>() {
				public int compare(String lhs, String rhs) {
					int ilhs = usageCounter.get(lhs);
					int irhs = usageCounter.get(rhs);
					if (ilhs > irhs) return -1;
					if (ilhs < irhs) return +1;
					return 0;
				}
			});
			
			
			if (Utils.D) {
				Utils.d(new MostUsedApps(),"sorted // --------------------");
				for (String can : candidates)
					Utils.d(new MostUsedApps(),"sorted // " + usageCounter.get(can)+":"+can);
			}
			
			Utils.d(new MostUsedApps(), "------------ EOF getAppsForKicker ------");
			return candidates.toArray(new String[]{});
		}
		
		
	}

	public static short getId() {
		return AppKickerFiller.MOST_APPS_ALG_ID;
	}

}
