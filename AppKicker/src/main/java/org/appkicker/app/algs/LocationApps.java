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

import org.appkicker.app.logging.LocationObserver;
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
public class LocationApps extends BroadcastReceiver {

	
	private static Map<String, Map<String, Integer>> appUsePerPlace;	
	private static final Object cacheMutex = new Object();
	
	private static final String NULLSTRING = "nulltile";
	
	private static String lastLaunchAbleApp = NULLSTRING;
	private static String lastCurrentApp = NULLSTRING;

	private static boolean isRegistered = false;
	
	public LocationApps() {
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
			
			String curr = intent.getStringExtra(WidgetProvider.INTENT_KICKERUPDATE_FIELDCURRENT);
			String place = LocationObserver.getCurrentPlaceID();
			logAppUseAtPlace(place, curr);
			
		} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) || Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
			// save if device is going screen-off
			synchronized (cacheMutex) {
				try {
					Utils.d(this, "writing memory to cache file");
					FileOutputStream fos = context.openFileOutput(CACHE, Context.MODE_PRIVATE);
					ObjectOutputStream os = new ObjectOutputStream(fos);
					os.writeObject(appUsePerPlace);
					os.close();
				} catch (IOException e) {
					Utils.e(this, e.getLocalizedMessage());
				}
			}
		}
	}

	private void logAppUseAtPlace(String placeID, String app) {
		loadCacheFromFile();
		printTable();
		
		if (app == null) app = NULLSTRING;
		
		boolean currLaunchable = Utils.appIsShownInLaucher(app, AppKickerApplication.getAppContext());
		
		

		Utils.d(this, "logging use: " + placeID + " -> " + app +" (last launchable: "+lastLaunchAbleApp+")");
		if (currLaunchable) {
			countTransition(placeID, app);
		}
		
	}

	private void countTransition(String placeID, String app) {
		int is = getAppUseAtPlaceCount(placeID, app);
		setAppUseAtPlaceCount(placeID, app, is+1);
		
		Utils.d(this,"counted transition: " + placeID + " -> "+ app + " : "+ (is+1));
		
	}
	
	
	private static void printTable() {
		synchronized (cacheMutex) {
			Utils.d(new LocationApps(), "db: ---------------------");
			if (appUsePerPlace == null) return;
			for(String from : appUsePerPlace.keySet()) {
				Map<String, Integer> row = appUsePerPlace.get(from);
				for(String to: row.keySet()) {
					int i = row.get(to);
					Utils.d(new LocationApps(), "db: @"+ from + " : " + to + " = " + i);
				}
			}
			Utils.d(new LocationApps(), "db: ---------------------");
		}
	}
	
	private void setAppUseAtPlaceCount(String prev, String curr, int i) {
		synchronized (cacheMutex) {
			Map<String, Integer> row = appUsePerPlace.get(prev);
			
			if (row == null) {
				row = new HashMap<String, Integer>();
				appUsePerPlace.put(prev, row);
			}
			row.put(curr, i);
		}
	}

	
	
	/**
	 * the number of transitions that have been recorded for that sequence
	 * 
	 * @param placeID
	 * @param app
	 * @return
	 */
	private int getAppUseAtPlaceCount(String placeID, String app) {
		Integer field;
	
		synchronized (cacheMutex) {
			// get the row
			Map<String, Integer> row = appUsePerPlace.get(placeID);
			if (row == null) {
				appUsePerPlace.put(placeID, new HashMap<String, Integer>());
				return 0;
			}
			
			// get the field if row exists
			field = row.get(app);
			if (field == null) {
				row.put(app, 0);
				return 0;
			}
		}
		return field;
	}

	private static String CACHE = LocationApps.class.getName();
	
	/**
	 * loads the persisted file if transitions are currently null
	 */
	@SuppressWarnings("unchecked")
	private static void loadCacheFromFile() {
		synchronized (cacheMutex) {
			
			// try to load if null
			if (appUsePerPlace == null) {
				try {
					Utils.d(new LocationApps(), "trying to load file from cache");
					Context context = AppKickerApplication.getAppContext();
					FileInputStream fis = context.openFileInput(CACHE);
					ObjectInputStream is = new ObjectInputStream(fis);
					appUsePerPlace = (Map<String, Map<String, Integer>>) is.readObject();
					is.close();
				} catch (IOException e) {
					Utils.e(new LocationApps(), e.getLocalizedMessage());
					appUsePerPlace = new HashMap<String, Map<String, Integer>>(10);
				} catch (ClassNotFoundException e) {
					Utils.e(new LocationApps(), e.getLocalizedMessage());
					appUsePerPlace = new HashMap<String, Map<String, Integer>>(10);
				}

				// if still null, maybe because persisted object was empty or exception occurred
				if (appUsePerPlace == null) {
					appUsePerPlace = new HashMap<String, Map<String, Integer>>(10);
				}
			}
			
		}		
	}

	

	
	public static String[] getAppsForKicker() {
		loadCacheFromFile();
		
		String placeID = LocationObserver.getCurrentPlaceID();

		Utils.d(new LocationApps(), "------------ BOF getAppsForKicker ------");
		
		synchronized (cacheMutex) {
			final Map<String, Integer> resulting = appUsePerPlace.get(placeID);
			
			List<String> candidates = new Vector<String>();
			
			if (resulting != null) {
				candidates.addAll(resulting.keySet());
			} else {
				// this is our fallback when no apps have been used here
				return MostUsedApps.getAppsForKicker();
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
				Utils.d(new LocationApps(),"sorted @"+placeID+" // --------------------");
				for (String can : candidates)
					Utils.d(new LocationApps(),"sorted @"+placeID+" // " + resulting.get(can)+":"+can);
			}
			
			
			Utils.d(new LocationApps(), "------------ EOF getAppsForKicker ------");
			return candidates.toArray(new String[]{});
		}
	}

	public static short getId() {
		return AppKickerFiller.LOCATION_APPS_ALG_ID;
	}

}
