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
 * This {@link AppKickerFiller} implements an approach that schedules those apps
 * into the kicker, that usually are used after the currently app is used. The
 * algorithm is based on transitions between apps and the model is saved as a
 * graph-based structure. Only those apps that are shown in the launcher are
 * considered.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class TransitionApps extends BroadcastReceiver {

	
	private static Map<String, Map<String, Integer>> transitions;	
	private static final Object cacheMutex = new Object();
	
	private static final String NULLSTRING = "app.null";
	
	private static String lastLaunchAbleApp = NULLSTRING;
	private static String lastCurrentApp = NULLSTRING;

	private static boolean isRegistered = false;
	
	public TransitionApps() {
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
					os.writeObject(transitions);
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
		
		
		boolean prevLaunchable = Utils.appIsShownInLaucher(prev, AppKickerApplication.getAppContext());
		boolean currLaunchable = Utils.appIsShownInLaucher(curr, AppKickerApplication.getAppContext());
		
		if (prevLaunchable || (prev == NULLSTRING))
			lastLaunchAbleApp = prev;
		

		Utils.d(this, "logging transition: " + prev + " -> " + curr +" (last launchable: "+lastLaunchAbleApp+")");
		if (currLaunchable) {
			lastCurrentApp = curr;
			countTransition(lastLaunchAbleApp, curr);
		}
		
	}

	private void countTransition(String prev, String curr) {
		int is = getTransitionsCount(prev, curr);
		setTransitionsCount(prev, curr, is+1);
		
		Utils.d(this,"counted transition: " + prev + " -> "+ curr + " : "+ (is+1));
		
	}
	
	
	private static void printTable() {
		synchronized (cacheMutex) {
			Utils.d(new TransitionApps(), "db: ---------------------");
			if (transitions == null) return;
			for(String from : transitions.keySet()) {
				Map<String, Integer> row = transitions.get(from);
				for(String to: row.keySet()) {
					int i = row.get(to);
					Utils.d(new TransitionApps(), "db: "+ from + " -> " + to + " : " + i);
				}
			}
			Utils.d(new TransitionApps(), "db: ---------------------");
		}
	}
	
	private void setTransitionsCount(String prev, String curr, int i) {
		synchronized (cacheMutex) {
			Map<String, Integer> row = transitions.get(prev);
			
			if (row == null) {
				row = new HashMap<String, Integer>();
				transitions.put(prev, row);
			}
			row.put(curr, i);
		}
	}

	
	
	/**
	 * the number of transitions that have been recorded for that sequence
	 * 
	 * @param prev
	 * @param curr
	 * @return
	 */
	private int getTransitionsCount(String prev, String curr) {
		Integer field;
	
		synchronized (cacheMutex) {
			// get the row
			Map<String, Integer> row = transitions.get(prev);
			if (row == null) {
				transitions.put(prev, new HashMap<String, Integer>());
				return 0;
			}
			
			// get the field if row exists
			field = row.get(curr);
			if (field == null) {
				row.put(curr, 0);
				return 0;
			}
		}
		return field;
	}

	private static String CACHE = TransitionApps.class.getName();
	
	/**
	 * loads the persisted file if transitions are currently null
	 */
	@SuppressWarnings("unchecked")
	private static void loadCacheFromFile() {
		synchronized (cacheMutex) {
			
			// try to load if null
			if (transitions == null) {
				try {
					Utils.d(new TransitionApps(), "trying to load file from cache");
					Context context = AppKickerApplication.getAppContext();
					FileInputStream fis = context.openFileInput(CACHE);
					ObjectInputStream is = new ObjectInputStream(fis);
					transitions = (Map<String, Map<String, Integer>>) is.readObject();
					is.close();
				} catch (IOException e) {
					Utils.e(new TransitionApps(), e.getLocalizedMessage());
					transitions = new HashMap<String, Map<String, Integer>>(10);
				} catch (ClassNotFoundException e) {
					Utils.e(new TransitionApps(), e.getLocalizedMessage());
					transitions = new HashMap<String, Map<String, Integer>>(10);
				}

				// if still null, maybe because persisted object was empty or exception occurred
				if (transitions == null) {
					transitions = new HashMap<String, Map<String, Integer>>(10);
				}
			}
			
		}		
	}


	
	public static String[] getAppsForKicker() {
		loadCacheFromFile();
		
		String base = (lastLaunchAbleApp == NULLSTRING) ? NULLSTRING : lastCurrentApp;

		Utils.d(new TransitionApps(), "get apps for kicker following on " + lastCurrentApp);
		printTable();

		
		Utils.d(new TransitionApps(), "------------ BOF getAppsForKicker ------");
		
		synchronized (cacheMutex) {
			final Map<String, Integer> resulting = transitions.get(base);
			
			List<String> candidates = new Vector<String>();
			
			if (resulting != null) {
				candidates.addAll(resulting.keySet());
			}
	
			Collections.sort(candidates,new Comparator<String>() {
				public int compare(String lhs, String rhs) {
					int ilhs = resulting.get(lhs);
					int irhs = resulting.get(rhs);
					if (ilhs > irhs) return -1;
					if (ilhs < irhs) return +1;
					return 0;
				}
			});
			
			
			if (Utils.D) {
				Utils.d(new TransitionApps(),"sorted @"+base+" // --------------------");
				for (String can : candidates)
					Utils.d(new TransitionApps(),"sorted @"+base+" // " + resulting.get(can)+":"+can);
			}
			
			
			Utils.d(new TransitionApps(), "------------ EOF getAppsForKicker ------");
			return candidates.toArray(new String[]{});
		}
		
		
		
		
	}

	public static short getId() {
		return AppKickerFiller.TRANSI_APPS_ALG_ID;
	}

}
