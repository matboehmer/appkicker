package org.appkicker.app.logging;

import android.content.Context;


/**
 * {@link Deprecated} use broadcast receiver instead
 * 
 * This listeners are called when a different application is launched by user
 */
@Deprecated
public interface AppChangeListener {
	
	/**
	 * @param previous the packagename of the previous app, null if no previous app
	 * @param current packagename of current app
	 */
	void onAppChange(String previous, String current, Context context);
	
	
}