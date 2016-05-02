package org.appkicker.app.widget;

import java.util.Random;

import org.appkicker.app.R;
import org.appkicker.app.algs.AppKickerFiller;
import org.appkicker.app.algs.LocationApps;
import org.appkicker.app.algs.MostUsedApps;
import org.appkicker.app.algs.PPMApps;
import org.appkicker.app.algs.RecentApps;
import org.appkicker.app.algs.TransitionApps;
import org.appkicker.app.data.entities.AppUsageEvent;
import org.appkicker.app.logging.AppUsageLogger;
import org.appkicker.app.logging.BackgroundService;
import org.appkicker.app.ui.DisclaimerActivity;
import org.appkicker.app.ui.WidgetConfigurationActivity;
import org.appkicker.app.utils.Utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RemoteViews;

/**
* @author Matthias Boehmer, mail@matthiasboehmer.de
*/
public class WidgetProvider extends AppWidgetProvider {
	
	/* number of apps to show in widget */
	public static final int NUM_KICKER_APPS_TO_SHOW = 5;
	
	/* IDs of icon buttons */
	static int[] KICKER_ICONS = new int[] { R.id.widgetbutton1, R.id.widgetbutton2, R.id.widgetbutton3, R.id.widgetbutton4, R.id.widgetbutton5 };
    
	/* IDs of text labels for icon buttons */
	static int[] KICKER_LABELS = new int[] { R.id.kickerText1, R.id.kickerText2, R.id.kickerText3, R.id.kickerText4, R.id.kickerText5 };

    
	/** intent action and fields for starting app from widget */
	public static final String INTENT_KICKAPP_ACTION = "org.appkicker.app.KICKAPP_ACTION";
	public static final String INTENT_FIELD_PACKAGENAME = "org.appkicker.app.IntentFieldPackagename";
	public static final String INTENT_FIELD_KICKERPOSITION = "org.appkicker.app.IntentFieldPosision";
	public static final String INTENT_FIELD_KICKERALGORITHM = "org.appkicker.app.IntentFieldAlgorithm";
	
	/** broadcast to update the widget */
	public static final String INTENT_APPCHANGE_ACTION = "org.appkicker.app.KICKERUPDATE_ACTION";

	public static final String INTENT_KICKERUPDATE_FIELDCURRENT = "org.appkicker.app.KICKERUPDATE_CURR";

	public static final String INTENT_KICKERUPDATE_FIELDPREVIOUS = "org.appkicker.app.KICKERUPDATE_PREV";

//	public static final String INTENT_APPCHANGE_ACTION = "org.appkicker.app.APPCHANGE_ACTION";
	
    /**
	 * triggers an update of the widget's UI
	 */
	public void triggerUpdate(Context context) {
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.kickerwidget);

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));

		SharedPreferences prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0);
		
		for (int widgetId : appWidgetIds) {
			
			ResolveInfo[] apps = new ResolveInfo[]{};
			
			int method = prefs.getInt(Utils.SETTINGS_settings_widgetalgo + widgetId, AppKickerFiller.STUDY_APPS_ALG_ID);
			
			Utils.d(this, "updating w:" + widgetId + " m:"+method);
			int alg = AppKickerFiller.RECENT_APPS_UNKNOWN_ID;
			
			switch (method) {
			case AppKickerFiller.RECENT_APPS_ALG_ID:
				apps = packageNamesToRI(RecentApps.getAppsForKicker(), context);
				alg = RecentApps.getId();
				break;
			
			case AppKickerFiller.PPM_APPS_ALG_ID:
				apps = packageNamesToRI(PPMApps.getAppsForKicker(), context);
				alg = PPMApps.getId();
				break;

			case AppKickerFiller.TRANSI_APPS_ALG_ID:
				apps = packageNamesToRI(TransitionApps.getAppsForKicker(), context);
				alg = TransitionApps.getId();
				break;
				
			case AppKickerFiller.MOST_APPS_ALG_ID:
				apps = packageNamesToRI(MostUsedApps.getAppsForKicker(), context);
				alg = MostUsedApps.getId();
				break;

			case AppKickerFiller.LOCATION_APPS_ALG_ID:
				apps = packageNamesToRI(LocationApps.getAppsForKicker(), context);
				alg = LocationApps.getId();
				break;
				
			default:
			case AppKickerFiller.STUDY_APPS_ALG_ID:
			
				
				Random r = new Random(System.currentTimeMillis());
				int rand = r.nextInt(5);
				
				switch (rand) {
				case 0:
					apps = packageNamesToRI(RecentApps.getAppsForKicker(), context);
					alg = RecentApps.getId() + AppKickerFiller.STUDY_APPS_ALG_ID;
					break;
				case 1:
					apps = packageNamesToRI(TransitionApps.getAppsForKicker(), context);
					alg = TransitionApps.getId() + AppKickerFiller.STUDY_APPS_ALG_ID;
					break;
				case 2:
					apps = packageNamesToRI(MostUsedApps.getAppsForKicker(), context);
					alg = MostUsedApps.getId() + AppKickerFiller.STUDY_APPS_ALG_ID;
					break;
				case 3:
					apps = packageNamesToRI(LocationApps.getAppsForKicker(), context);
					alg = LocationApps.getId() + AppKickerFiller.STUDY_APPS_ALG_ID;
					break;
				case 4:
					apps = packageNamesToRI(PPMApps.getAppsForKicker(), context);
					alg = PPMApps.getId() + AppKickerFiller.STUDY_APPS_ALG_ID;
					break;
				}
				
				
			}
			
			
			if (apps.length == 0) {
				// show info if we have no apps
				hideEmptyInfo(views, getContentStringForAlg(alg, context));
				startThings(context);
			} else {
				// hide info if we have apps
				showEmptyInfo(views, getContentStringForAlg(alg, context));
			}
			
			// show the new apps in the kicker
			updateKickerWidget(context, widgetId, views, apps, alg);
			
			appWidgetManager.updateAppWidget(widgetId, views);
			//appWidgetManager.updateAppWidget(appWidgetIds, views);
		}
		
	}


	private static ResolveInfo[] packageNamesToRI(String[] pnames, Context c) {
		PackageManager pm = c.getPackageManager();
		
		ResolveInfo[] ris = new ResolveInfo[pnames.length];
		
		for (int i=0;i<pnames.length;i++) {
			if (pnames[i] == null) {
				continue;
			};
			
			Intent launchintent = pm.getLaunchIntentForPackage(pnames[i]);
			if (launchintent != null) {
				ris[i] = pm.resolveActivity(launchintent, 0);
			}
			
		}
		return ris;
	}

	
	private static String getContentStringForAlg(int alg, Context context) {
		switch(alg) {
		case AppKickerFiller.MOST_APPS_ALG_ID:
		case AppKickerFiller.MOST_APPS_ALG_ID + AppKickerFiller.STUDY_APPS_ALG_ID:
			return context.getString(R.string.noappsforkickerMost);
		case AppKickerFiller.RECENT_APPS_ALG_ID:
		case AppKickerFiller.RECENT_APPS_ALG_ID + AppKickerFiller.STUDY_APPS_ALG_ID:
			return context.getString(R.string.noappsforkickerRece);
		case AppKickerFiller.TRANSI_APPS_ALG_ID:
		case AppKickerFiller.TRANSI_APPS_ALG_ID + AppKickerFiller.STUDY_APPS_ALG_ID:
			return context.getString(R.string.noappsforkickerSequ);
		default:
			return context.getString(R.string.noappsforkicker);
		}
	}
	
	private static void showEmptyInfo(RemoteViews views, String text) {
		views.setTextViewText(R.id.noContentText, text);
		views.setViewVisibility(R.id.noContent, View.GONE);
		views.setViewVisibility(R.id.content, View.VISIBLE);
	}
	
	private static void hideEmptyInfo(RemoteViews views, String text) {
		views.setTextViewText(R.id.noContentText, text);
		views.setViewVisibility(R.id.noContent, View.VISIBLE);
		views.setViewVisibility(R.id.content, View.GONE);
	}

	// ----------------------------------------------------------------------------------------------
	// component life cycle
	// ----------------------------------------------------------------------------------------------
	
	
	@Override
	public void onEnabled(Context c) {
		super.onEnabled(c);
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
		int[] widgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(c, WidgetProvider.class));
		for (int widgetId : widgetIds) {
			configureWidget(c, appWidgetManager, widgetId);
		}

		startThings(c);
    	triggerUpdate(c);
   	}

	private void startThings(Context c) {
		
		// start all background services
		BackgroundService.startByIntent(c);
		
		// start IntentServices to listen to app chances
		c.startService(new Intent(c, TransitionApps.class));
		c.startService(new Intent(c, RecentApps.class));
		
	}

	@Override
	public void onUpdate(Context c, AppWidgetManager appWidgetManager, int[] widgetIds) {
		BackgroundService.startByIntent(c);
		
        

		for (int widgetId : widgetIds) {
			configureWidget(c, appWidgetManager, widgetId);
		}
		
    	triggerUpdate(c);
		Utils.d("updating " + widgetIds.length + " widgets");
    }

	public static void configureWidget(Context c, AppWidgetManager appWidgetManager, int widgetId) {
		RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.kickerwidget);

		// background configuration
//		boolean showBackground = prefs.getBoolean(Utils.SETTINGS_settings_widgetalgo + widgetId, true);
//		Utils.d(this, "config :: showBackground = " + showBackground);
//		if (showBackground) {
//			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_dark_bg);
//		} else {
//			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_grey);
//		}

		SharedPreferences prefs = c.getSharedPreferences(Utils.PREFS_NAME, 0);
		int color = prefs.getInt(Utils.SETTINGS_settings_widgetbackground + widgetId, WidgetConfigurationActivity.BACKGROUND_COLOR_GREY);
		Utils.dToast(new WidgetProvider(), "set color: " + color);
		switch (color) {
		case WidgetConfigurationActivity.BACKGROUND_COLOR_RED:
			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_red);
			break;
		case WidgetConfigurationActivity.BACKGROUND_COLOR_GREEN:
			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_green);
			break;
		case WidgetConfigurationActivity.BACKGROUND_COLOR_BLUE:
			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_blue);
			break;
		case WidgetConfigurationActivity.BACKGROUND_COLOR_TRANSPARENT:
			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_transparent);
			break;
		default:
		case WidgetConfigurationActivity.BACKGROUND_COLOR_GREY:
			views.setInt(R.id.kickerwidget, "setBackgroundResource", R.drawable.appwidget_grey);
			break;
		}
		
//		int alg = prefs.getInt(Utils.SETTINGS_settings_widgetalgo + widgetId, AppKickerFiller.STUDY_APPS_ALG_ID);
//		CharSequence text = "woop";getContentStringForAlg(alg, c);
//		views.setTextViewText(R.id.noContent, text );
		
        // Tell the widget manager
        appWidgetManager.updateAppWidget(widgetId, views);	
    }


	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int id : appWidgetIds) {
			AppUsageLogger.getAppUsageLogger(context).logWidgetRemoved(id);
		}
		Utils.dToast("widget deleted " + Utils.toString(appWidgetIds));
	}
    
	@Override
	public void onDisabled(Context context) {
		// TODO Auto-generated method stub
		super.onDisabled(context);
	}
		
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (INTENT_APPCHANGE_ACTION.equals(intent.getAction())) {
			Utils.dToast("INTENT_KICKERUPDATE_ACTION");
			triggerUpdate(context);
		} else if (INTENT_KICKAPP_ACTION.equals(intent.getAction())) {
			
			/*
			 * use click to ask for disclaimer if not accepted
			 */
			boolean ack = Utils.isDisclaimerAcknowledged(context);
			String pname = intent.getStringExtra(INTENT_FIELD_PACKAGENAME);

			
			
			if (ack) {
				/*
				 * use click to restart service if stopped
				 */
				BackgroundService.startByIntent(context);
				startAppAndLog(intent, pname, context);
			} else {
				
				
				// ask if we did not ask for a week
				boolean askAgain = Utils.askAgainForDisclaimer(context);
				if (askAgain) {
					// ask again
					Utils.updateLastTimeAskedForDisclaimer(context);
					Intent disc = new Intent(context, DisclaimerActivity.class);
					disc.putExtra(INTENT_FIELD_PACKAGENAME, pname);
					disc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(disc);
				} else {
					// just start app
					startAppAndLog(intent, pname, context);
				}
				
				
				
			}
			
			
//		} else if (INTENT_KICKERUPDATE_ACTION.equals(intent.getAction())) {
//			AppWidgetManager awm = AppWidgetManager.getInstance(context);
//			int[] ids = awm.getAppWidgetIds(new ComponentName(context,WidgetProvider.class));
//			onUpdate(context, awm , ids );
		} else {
			Utils.d(this, "onReceive: action " + intent.getAction());
		}
	}    
    
	/**
	 * Starts the app that was clicked on in the widget
	 * 
	 * @param intent
	 * @param pname
	 * @param context
	 */
	private void startAppAndLog(Intent intent, String pname, Context context) {
		/*
		 *  log this app start
		 */
		int pos = intent.getIntExtra(INTENT_FIELD_KICKERPOSITION, -1);
		int alg = intent.getIntExtra(INTENT_FIELD_KICKERALGORITHM, -1);
		Utils.d(this, "onReceive: here we go with " + pname + "| p: "+pos+ "| a:" + alg);

		
		AppUsageEvent aue = new AppUsageEvent(
				pname, 
				Utils.getCurrentTime(), 
				alg, 
				AppUsageEvent.EVENT_KICKERCLICKBASE + pos, 
				AppUsageEvent.STATUS_LOCAL_ONLY);
		Utils.dToast(this, pname+" a:"+alg+" @"+pos);
		
		AppUsageLogger.getAppUsageLogger(context).logCustom(aue);
		
		/*
		 *  start the app
		 */
		Intent app = context.getPackageManager().getLaunchIntentForPackage(pname);
		if (app != null) {
			context.startActivity(app);
		}	}


	// ----------------------------------------------------------------------------------------------
	// static methods
	// ----------------------------------------------------------------------------------------------

    
	/**
	 * Method updates the apps shown in the kicker.
	 * 
	 * @param c
	 * @param views
	 * @param apps
	 * @param alg 
	 */
	private void updateKickerWidget(Context c, int widgetID, RemoteViews views, ResolveInfo[] apps, int alg) {
		
		Utils.dToast(this, "updating widget now");
		
    	PackageManager pm = c.getPackageManager();
        
    	int i_fill = 0;
		for (ResolveInfo app : apps) {
			
			// we cannt show more apps than we have space for
			if (i_fill >= NUM_KICKER_APPS_TO_SHOW) break;

			if (app != null) {
				Drawable icon = null;
				CharSequence label = null;
				
				icon = app.loadIcon(pm);
				label = app.loadLabel(pm);
			
				if (label == null) {
					c.getResources().getString(R.string.defaultKickName);
				}
				
				if (icon == null) {
					icon = c.getResources().getDrawable(R.drawable.ic_launcher);
				}
				
				Bitmap bm = ((BitmapDrawable) icon).getBitmap();
				views.setTextViewText(KICKER_LABELS[i_fill], label);
				views.setImageViewBitmap(KICKER_ICONS[i_fill], bm);
				
				// intent to start app and track click when user clicks on kicker icon
				Intent intent = new Intent(c, WidgetProvider.class);
				intent.setAction(INTENT_KICKAPP_ACTION);
				intent.putExtra(INTENT_FIELD_PACKAGENAME, app.activityInfo.packageName);
				intent.putExtra(INTENT_FIELD_KICKERPOSITION, i_fill);
				intent.putExtra(INTENT_FIELD_KICKERALGORITHM, alg);
				
				
				PendingIntent pintent = PendingIntent.getBroadcast(c, widgetID * 10 + i_fill, intent, PendingIntent.FLAG_UPDATE_CURRENT);
				views.setOnClickPendingIntent(KICKER_ICONS[i_fill], pintent);
				views.setViewVisibility(KICKER_LABELS[i_fill], View.VISIBLE);
				views.setViewVisibility(KICKER_ICONS[i_fill], View.VISIBLE);
				
				
				Utils.d(WidgetProvider.class, i_fill + " : " + app.resolvePackageName);
				
				// proceed with next position
				i_fill++;
			}

		}
    	
    	
		for (int i = i_fill; i < NUM_KICKER_APPS_TO_SHOW; i++) {
			
			views.setTextViewText(KICKER_LABELS[i], "kicker");
			views.setImageViewResource(KICKER_ICONS[i], R.drawable.ic_launcher);
			Intent intent = new Intent();
			views.setOnClickPendingIntent(KICKER_ICONS[i], PendingIntent.getBroadcast(c, i, intent , PendingIntent.FLAG_CANCEL_CURRENT));
			
			
			views.setViewVisibility(KICKER_LABELS[i], View.INVISIBLE);
			views.setViewVisibility(KICKER_ICONS[i], View.INVISIBLE);
		}
    }

	
}
