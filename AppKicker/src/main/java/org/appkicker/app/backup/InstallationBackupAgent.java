package org.appkicker.app.backup;

import org.appkicker.app.logging.DeviceObserver;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;

public class InstallationBackupAgent extends BackupAgentHelper {

    // A key to uniquely identify the set of backup data
    static final String FILES_BACKUP_KEY = "installation_id_file";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        FileBackupHelper filesHelper = new FileBackupHelper(this, DeviceObserver.INSTALLATION);
        addHelper(FILES_BACKUP_KEY, filesHelper);
    }
}
