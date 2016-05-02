package org.appkicker.app.backup;

import org.appkicker.app.utils.Utils;

import android.app.backup.BackupManager;
import android.content.Context;

public class WrapperBackupAgent {
	private static Boolean backupManagerAvailable = null;

	public static boolean isAvailable() 
	{
		if(backupManagerAvailable == null) {
			try
		    {
		        Class.forName("android.app.backup.BackupManager");
		        backupManagerAvailable = true;
		    }
		    catch (Exception e)
		    {
		    	backupManagerAvailable = false;
		    }
		}
		return backupManagerAvailable;
	}

	public static void dataChanged(Context context)
	{
		if(isAvailable()) {
			BackupManager wrappedInstance = new BackupManager(context);
			wrappedInstance.dataChanged();
			Utils.d(context, "Backup functionality is available. Backing up installation id.");
		}
		else Utils.d(context, "Backup functionality is NOT AVAILABLE.");
	}
}
