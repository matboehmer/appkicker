package org.appkicker.app.sync;

import android.text.format.DateUtils;

/**
 * Configuration of the app related to different version, e.g. for development
 * purpose or resease on the market. Always check this values before releasing a
 * new version to the market.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public interface Settings {

	/* values for development purpose */
	/*
	long SYNCINTERVALREQUEST = DateUtils.SECOND_IN_MILLIS * 10; // we request a sync every 30 seconds
	long SYNCINTERVAL = DateUtils.SECOND_IN_MILLIS * 30; // we do a sync every 60 seconds at maximum
	 */

	
	/* values for market release*/
	long SYNCINTERVALREQUEST = DateUtils.SECOND_IN_MILLIS * 120; // we request a sync every 30 seconds
	long SYNCINTERVAL = DateUtils.HOUR_IN_MILLIS * 6; // default for relase
	
}
