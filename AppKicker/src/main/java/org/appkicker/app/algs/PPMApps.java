package org.appkicker.app.algs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.appkicker.app.utils.AppKickerApplication;
import org.appkicker.app.utils.Utils;
import org.appkicker.app.widget.WidgetProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import edu.umass.cs.falcon.model.PPM;

/**
 *
 *
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class PPMApps extends BroadcastReceiver {


		
	private static final Object cacheMutex = new Object();
	private static final Object historyMutex = new Object();

	private static final String NULLSTRING = "app.null";

	private static String lastCurrentApp = NULLSTRING;

	private static boolean isRegistered = false;

	private static PPM ppm = null;
	private static PPMPersistentData data = null;
	private static int NUM_APPS_TO_PREDICT = 5;
	private static FileOutputStream historyWriter = null;
	private static FileInputStream historyReader = null;

	public PPMApps() {
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
			logCurrentApp(curr);

		} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction()) || Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
			// save if device is going screen-off
			synchronized (cacheMutex) {
				try {
					Utils.d(this, "writing memory to cache file");
					FileOutputStream fos = context.openFileOutput(CACHE, Context.MODE_PRIVATE);
					ObjectOutputStream os = new ObjectOutputStream(fos);
					os.writeObject(data);
					os.close();
				} catch (IOException e) {
					Utils.e(this, e.getLocalizedMessage());
				}
				Utils.d("Writing persistent data");
			}
			synchronized(historyMutex) {
				if(historyWriter!=null) {
					try {
						historyWriter.close();
						historyWriter = null;
					} catch (IOException e) {
						Utils.e(this, e.getLocalizedMessage());
					}
					Utils.d(this,"Writing history");
				}
			}
		}
	}

	private void logCurrentApp(String curr) {
		loadCacheFromFile();
		if(curr == null) curr = NULLSTRING;

		boolean currLaunchable = Utils.appIsShownInLaucher(curr, AppKickerApplication.getAppContext());
		Utils.d(this, "logging current app: " + curr);
		if (currLaunchable) {
			updateModel(curr); // this line should be within the condition since the launcher itself is an application that will be triggered, i.e. we shoud ignore the launcher
			lastCurrentApp = curr;
		}
	}

	private void updateModel(String currentApp) {
		if(currentApp.contains("com.android.launcher"))
			return;
		Integer encoding = data.appToEncodingMap.get(currentApp);
		if(encoding==null) {
			encoding = data.lastAppEncoding+1;
			data.appToEncodingMap.put(currentApp, encoding);
			data.encodingToAppMap[encoding] = currentApp;
			data.lastAppEncoding = encoding;
			Utils.d(new PPMApps(),"Adding "+currentApp+" to "+encoding);
			if(data.lastAppEncoding == 255){
				//Recycle if 256 apps encountered
				data.lastAppEncoding = 0;
			}
		}
		Calendar gc = GregorianCalendar.getInstance();
		int HOUR_OF_DAY = gc.get(Calendar.HOUR_OF_DAY);
		ppm.updateModel(encoding, 0, HOUR_OF_DAY/4);
		synchronized(historyMutex) {
			if(historyWriter!=null) {
				try {
					byte b[] = new byte[]{(byte)(HOUR_OF_DAY/4),encoding.byteValue()};
					historyWriter.write(b);
				} catch (IOException e) {
					Utils.e(new PPMApps(), e.getLocalizedMessage());
				}
			}
		}
	}




	private static String CACHE = PPMApps.class.getName();
	private static String HISTORY = PPMApps.class.getName()+".history";

	/**
	 * loads the persisted file if app to encoding map is currently null
	 */
	private static void loadCacheFromFile() 
	{
		synchronized(historyMutex) 
		{
			boolean ppmWasNull = (ppm==null);
			if(ppmWasNull) ppm = new PPM(NUM_APPS_TO_PREDICT);
			if(historyReader == null || ppmWasNull) {
				Context context = AppKickerApplication.getAppContext();
				try {
					historyReader = context.openFileInput(HISTORY);
					initializeModel();
					historyReader.close();
				} catch (IOException e) {
					Utils.e(new PPMApps(), e.getLocalizedMessage());
				}
				Utils.d("Reading History");
			}
		}
		synchronized(historyMutex) 
		{
			if(historyWriter == null) {
				Context context = AppKickerApplication.getAppContext();
				try {
					historyWriter = context.openFileOutput(HISTORY,Context.MODE_APPEND);
				} catch (IOException e) {
					Utils.e(new PPMApps(), e.getLocalizedMessage());
				}
				Utils.d("Initializing history writer");
			}
		}
		synchronized (cacheMutex) 
		{
			// try to load if null
			if (data == null) {
				try {
					Utils.d(new PPMApps(), "trying to load file from cache");
					Context context = AppKickerApplication.getAppContext();
					FileInputStream fis = context.openFileInput(CACHE);
					ObjectInputStream is = new ObjectInputStream(fis);
					data = (PPMPersistentData) is.readObject();
					is.close();
					Utils.d("Reading persistent data");
				} catch (IOException e) {
					Utils.e(new PPMApps(), e.getLocalizedMessage());
					data = new PPMPersistentData();
				} catch (ClassNotFoundException e) {
					Utils.e(new PPMApps(), e.getLocalizedMessage());
					data = new PPMPersistentData();
				}

				// if still null, maybe because persisted object was empty or exception occurred
				if (data == null) {
					data = new PPMPersistentData();
				}
			}//null data
		}//cachemutex

	}

	private static void initializeModel() {
		int TIME_OF_DAY = 0;
		try{
			while((TIME_OF_DAY = historyReader.read())!=-1) {
				int app = historyReader.read();
				if(app!=-1)
					ppm.updateModel(app, 0, TIME_OF_DAY);
				else return;
			}
		} catch (IOException e) {
			Utils.e(new PPMApps(), e.getLocalizedMessage());
			return;
		}
	}



	/**
	 * @return array of predicted apps
	 */
	public static String[] getAppsForKicker() {
		loadCacheFromFile();

		Utils.d(new PPMApps(), "get apps for kicker following on " + lastCurrentApp);


		Utils.d(new PPMApps(), "------------ BOF getAppsForKicker ------");
		Calendar gc = GregorianCalendar.getInstance();
		int HOUR_OF_DAY = gc.get(Calendar.HOUR_OF_DAY);
		synchronized (cacheMutex) {
			int TIME_OF_DAY = HOUR_OF_DAY/4;
			int LOC_CLUSTER_ID = 0;
			//Get Top-k predictions for current time of day
			double predictions[][] = ppm.getTopPredictions(LOC_CLUSTER_ID, TIME_OF_DAY, NUM_APPS_TO_PREDICT);

			double symbols[] = predictions[0];
			//double probabilities[] = predictions[1];
			List<String> candidates = new Vector<String>();
			for(int i=0;i<symbols.length;i++) {
				int encoding = (int)symbols[i];
				Utils.d(new PPMApps(),"Received prediction" + symbols[i]);
				if(encoding>0 && encoding<=255) {
					String candidate = data.encodingToAppMap[encoding];
					Utils.d(new PPMApps(),"Received candidate" + candidate);
					if(candidate!=null) candidates.add(candidate);
				}
			}

			if (Utils.D) {
				Utils.d(new PPMApps(),"Candidate @ // --------------------");
				for (String can : candidates)
					Utils.d(new PPMApps(),"sorted @ // "+can);
			}

			Utils.d(new PPMApps(), "------------ EOF getAppsForKicker ------");
			return candidates.toArray(new String[]{});
		}

	}

	public static short getId() {
		return AppKickerFiller.PPM_APPS_ALG_ID;
	}

	static class PPMPersistentData implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 7721961483315529821L;
		HashMap<String,Integer> appToEncodingMap;
		String[] encodingToAppMap;
		int lastAppEncoding = 0;

		PPMPersistentData() {
			appToEncodingMap = new HashMap<String,Integer>();
			encodingToAppMap = new String[256];
			lastAppEncoding = 0;
		}
	}
}
