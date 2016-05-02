package org.appkicker.app.algs;

import org.appkicker.app.widget.WidgetProvider;

/**
 * All algorithms that have the purpose of filling icons into the kicker should
 * implement this interface to be used in the {@link WidgetProvider}.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 * 
 */
public interface AppKickerFiller {

	public static short RECENT_APPS_UNKNOWN_ID = -1;
	
	public static short STUDY_APPS_ALG_ID = 1000;
	public static short RECENT_APPS_ALG_ID = 1;
	public static short TRANSI_APPS_ALG_ID = 2;
	public static short MOST_APPS_ALG_ID = 3;
	public static short LOCATION_APPS_ALG_ID = 4;
	public static short PPM_APPS_ALG_ID = 5;


	

	public abstract String[] getAppsForKicker();
	
	/**
	 * Returns an identifier for this algorithm.
	 * @return
	 */
	abstract short getId();
	
}
