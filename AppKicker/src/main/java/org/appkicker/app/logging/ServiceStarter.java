package org.appkicker.app.logging;

import org.appkicker.app.utils.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class ServiceStarter extends BroadcastReceiver {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent action) {
//		Utils.dToast(AppKickerApplication.getAppContext(), "onReceive");

		Utils.d(this, "received intent: " + action.toURI() + " -- starting BackgroundService");
		Intent starter = new Intent(context, BackgroundService.class);
		starter.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startService(starter);
		
		if (Intent.ACTION_SHUTDOWN.equals(action.getAction())) {
			AppUsageLogger aul = AppUsageLogger.getAppUsageLogger(context);
			aul.logDeviceTurnedOff();
		} else if (Intent.ACTION_BOOT_COMPLETED.equals(action.getAction())) {
			AppUsageLogger aul = AppUsageLogger.getAppUsageLogger(context);
			aul.logDeviceBooted();

			aul.logAlreadyInstalledPackages();
		}
		
	}

}
