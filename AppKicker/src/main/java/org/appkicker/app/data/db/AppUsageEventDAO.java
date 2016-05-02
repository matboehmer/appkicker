package org.appkicker.app.data.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.appkicker.app.data.entities.AppUsageEvent;
import org.appkicker.app.utils.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * This DAO persistently maintains all data that was collected on app
 * interaction.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class AppUsageEventDAO extends GeneralDAO {

	// --------------------------------------------
	// SCHEMA
	// --------------------------------------------
	
	public static String TABLE_NAME = "AppUsageEvents";
	
	// TODO create rows for all fields of the object
	public static final String CNAME_ID = "_id";
	public static final String CNAME_PACKAGENAME = "packageName";
	public static final String CNAME_EVENTTYPE = "eventtype";
	public static final String CNAME_RUNTIME = "runtime";
	public static final String CNAME_STARTTIME = "starttime";
	public static final String CNAME_BLUETOOTHSTATE = "bluetoothstate";
	public static final String CNAME_WIFISTATE = "wifistate";
	public static final String CNAME_POWERSTATE = "powerstate";
	public static final String CNAME_ACCURACY = "accuracy";
	public static final String CNAME_LATITUDE = "latitude";
	public static final String CNAME_LONGITUDE = "longitude";
	public static final String CNAME_HEADPHONES = "headphones";
	public static final String CNAME_SYNCSTATUS = "syncStatus";
	public static final String CNAME_LASTSCREENON = "lastScreenOn";
	public static final String CNAME_SPEED = "speed";
	public static final String CNAME_ALTITUDE = "altitude";
	public static final String CNAME_ORIENTATION = "orientation";
	public static final String CNAME_POWERLEVEL = "powerlevel";
	
	public static final String[] PROJECTION = {
	    CNAME_ID,
	    CNAME_PACKAGENAME,
	    CNAME_EVENTTYPE,
	    CNAME_RUNTIME,
	    CNAME_STARTTIME,
	    CNAME_BLUETOOTHSTATE,
	    CNAME_WIFISTATE,
	    CNAME_POWERSTATE,
	    CNAME_ACCURACY,
	    CNAME_LATITUDE,
	    CNAME_LONGITUDE,
	    CNAME_HEADPHONES,
	    CNAME_SYNCSTATUS,
	    CNAME_LASTSCREENON,
	    CNAME_SPEED,
	    CNAME_ALTITUDE,
	    CNAME_ORIENTATION,
	    CNAME_POWERLEVEL
    };
	
	public final static int CNUM_ID = 0;
	public final static int CNUM_PACKAGENAME = 1;
	public final static int CNUM_EVENTTYPE = 2;
	public final static int CNUM_RUNTIME = 3;
	public final static int CNUM_STARTTIME = 4;
	public final static int CNUM_BLUETOOTHSTATE = 5;
	public final static int CNUM_WIFISTATE = 6;
	public final static int CNUM_POWERSTATE = 7;
	public final static int CNUM_ACCURACY = 8;
	public final static int CNUM_LATITUDE = 9;
	public final static int CNUM_LONGITUDE = 10;
	public final static int CNUM_HEADPHONES = 11;
	public final static int CNUM_SYNCSTATUS = 12;
	public final static int CNUM_LASTSCREENON = 13;
	public final static int CNUM_SPEED = 14;
	public final static int CNUM_ALTITUDE = 15;
	public final static int CNUM_ORIENTATION = 16;
	public final static int CNUM_POWERLEVEL = 17;
	

	// --------------------------------------------
	// STATUS WHEN SYNCING WITH SERVER
	// --------------------------------------------
	
	public static final short STATUS_LOCAL_ONLY = 1;
	public static final short STATUS_SYNCING = 2;
	public static final short STATUS_SERVER_PERSISTED = 3;
	
	
	public static final String TABLE_CREATE =
			// TODO optimize types of attributes
			"CREATE TABLE " + TABLE_NAME + " (" +
				CNAME_ID + " INTEGER PRIMARY KEY, " +
				CNAME_PACKAGENAME + " STRING, " +
				CNAME_EVENTTYPE + " INTEGER, " +
				CNAME_RUNTIME + " LONG, " +
				CNAME_STARTTIME + " LONG, " +
			    CNAME_BLUETOOTHSTATE + " INTEGER, " +
			    CNAME_WIFISTATE + " INTEGER, " +
			    CNAME_POWERSTATE + " INTEGER, " +
			    CNAME_ACCURACY + " DOUBLE, " +
			    CNAME_LATITUDE + " DOUBLE, " +
			    CNAME_LONGITUDE +  " DOUBLE, " +
			    CNAME_HEADPHONES + " INTEGER, " +
			    CNAME_SYNCSTATUS + " INTEGER, " +
			    CNAME_LASTSCREENON + " LONG, " +
			    CNAME_SPEED + " DOUBLE, " +
			    CNAME_ALTITUDE + " DOUBLE, " +
			    CNAME_ORIENTATION + " INTEGER, " +
			    CNAME_POWERLEVEL + " INTEGER " +
		");";

	// --------------------------------------------
	// QUERIES
	// --------------------------------------------
	
	private final static String WHERE_SYNCSTATUS = CNAME_SYNCSTATUS + "=?";
	private final static String WHERE_LASTSCREENON_SMALLEREQUAL = CNAME_LASTSCREENON + "<=?";
	
	// --------------------------------------------
	// LIVECYCLE
	// --------------------------------------------
	
	public AppUsageEventDAO(Context context) {
		super(context);
	}
	
	// --------------------------------------------
	// CRUD
	// --------------------------------------------
	
	/**
	 * inserts all events where runtime is greater than zero and event type is usage
	 * @param list
	 */
	public void insertWithoutZeroUsage(List<AppUsageEvent> list) {
		for (AppUsageEvent aue : list) {
			if(aue.runtime == 0 && aue.eventtype == AppUsageEvent.EVENT_INUSE) {
				// do nothing
			} else {
				insert(aue);
			}
		}
	}
	
	
	public void insert(List<AppUsageEvent> list) {
		for (AppUsageEvent aue : list) {
			insert(aue);
		}
	}
	
	public void insert(AppUsageEvent aue) {
		Utils.d(this, "writing to db: " + aue);
		ContentValues cv = aue2ContentValues(aue);
		db.insert(TABLE_NAME, null, cv);
	}
	
	public void clear() {
		db.delete(TABLE_NAME, null, null);
	}

	// --------------------------------------------
	// TRANSFORMATION
	// --------------------------------------------

	private ContentValues aue2ContentValues(AppUsageEvent aue) {
		ContentValues cv = new ContentValues();
		cv.put(CNAME_PACKAGENAME, aue.packageName);
		cv.put(CNAME_EVENTTYPE, aue.eventtype);
		cv.put(CNAME_STARTTIME, aue.starttime);
		cv.put(CNAME_RUNTIME, aue.runtime);
		cv.put(CNAME_LONGITUDE, aue.longitude);
		cv.put(CNAME_LATITUDE, aue.latitude);
		cv.put(CNAME_ACCURACY, aue.accuracy);
		cv.put(CNAME_POWERSTATE, aue.powerstate);
		cv.put(CNAME_WIFISTATE, aue.wifistate);
		cv.put(CNAME_BLUETOOTHSTATE, aue.bluetoothstate);
		cv.put(CNAME_HEADPHONES, aue.headphones);
		cv.put(CNAME_SYNCSTATUS, aue.syncStatus);
		cv.put(CNAME_LASTSCREENON, aue.timestampOfLastScreenOn);
		cv.put(CNAME_SPEED, aue.speed);
		cv.put(CNAME_ALTITUDE, aue.altitude);
		cv.put(CNAME_ORIENTATION, aue.orientation);
		cv.put(CNAME_POWERLEVEL, aue.powerlevel);
		return cv;
	}

	public Cursor findAll() {
		Cursor c = db.query(
				TABLE_NAME, 
				PROJECTION, 
				null, 
				null, 
				null, 
				null, 
				null);
		return c;		
	}

	
	public List<AppUsageEvent> findAllAsEntities() {
		Cursor c = findAll();
		AppUsageEvent aue;
		List<AppUsageEvent> l = new ArrayList<AppUsageEvent>();
		if(c.moveToFirst()) {
			do {
				aue = cursor2entity(c);
				l.add(aue);
			} while (c.moveToNext());
		}
		return l;
	}
	
	/**
	 * This method returns all the records for application 
	 * usage events excluding the ones already successfully
	 * synced with the server. The returned cursor
	 * is ordered in a way that will increase the effect of compressing
	 * the values based on the eliminating redundancy of sequential
	 * records with similar fields. This is particularly designed
	 * for the needs of the functionality for syncing the records
	 * on the server.
	 * 
	 * @return
	 */
	public Cursor getAndUpdateNonSyncedUsageEvents(int maximumNumberOfRecords) {
        // reset all syncing records to local only
        ContentValues values = new ContentValues();
        values.put(CNAME_SYNCSTATUS, AppUsageEvent.STATUS_LOCAL_ONLY);
        db.update(TABLE_NAME, values, WHERE_SYNCSTATUS, new String[]{String.valueOf(AppUsageEvent.STATUS_SYNCING)});
       
        // update to syncing only the limited amount of records
        String updateString = "UPDATE " + TABLE_NAME + " SET syncStatus=" + AppUsageEvent.STATUS_SYNCING + " WHERE " + CNAME_ID + " IN(" +
        "SELECT " + CNAME_ID + " FROM " + TABLE_NAME + " WHERE syncStatus=" + AppUsageEvent.STATUS_LOCAL_ONLY + " LIMIT " + maximumNumberOfRecords + ")";
        db.execSQL(updateString);
       
        // return the syncing interests
        Cursor c = db.query(
                        TABLE_NAME,
                        PROJECTION,
                        WHERE_SYNCSTATUS,
                        new String[]{String.valueOf(AppUsageEvent.STATUS_SYNCING)},
                        null,
                        null,
                        null);
        return c;
	}
	
	/**
	 * 
	 * @return
	 * 		A list of applications usage events
	 */
	public List<AppUsageEvent> getAndUpdateNonSyncedInterestsAsEntities(int maximumNumberOfRecords) {
		Cursor c = getAndUpdateNonSyncedUsageEvents(maximumNumberOfRecords);
		AppUsageEvent aue;
		List<AppUsageEvent> syncingUsage = new Vector<AppUsageEvent>();
		if(c.moveToFirst()) {
			do {
				aue = cursor2entity(c);
				syncingUsage.add(aue);
			} while (c.moveToNext());
		}
		c.close();
		return syncingUsage;		
	}
	
	/**
	 *  Updates app usage events which have 
	 *  status syncing to status server persisted
	 * 	
	 * 	@return
	 * 		number of rows affected
	 */
	public int updateSyncingToServerPersisted() {
		// update syncing status to server persisted status
		ContentValues values = new ContentValues();
		values.put(CNAME_SYNCSTATUS, AppUsageEvent.STATUS_SERVER_PERSISTED);
		return db.update(TABLE_NAME, values, WHERE_SYNCSTATUS, 
				new String[]{String.valueOf(AppUsageEvent.STATUS_SYNCING)});
	}
	
	/**
	 * Deletes all synced records which are already too old
	 * to be relevant usage events
	 * 
	 * @param olderThanInclusive
	 * 			The boundary timestamp (inclusive) below which 
	 * 			every synced timestamps will be deleted
	 * @return
	 * 			number of rows affected
	 */
	public int deleteOldSyncedUsageEvents(long olderThanInclusive) {
		return db.delete(TABLE_NAME, 
				WHERE_SYNCSTATUS + " AND " + WHERE_LASTSCREENON_SMALLEREQUAL, 
				new String[]{String.valueOf(STATUS_SERVER_PERSISTED),
				String.valueOf(olderThanInclusive)
				});
	}

	private AppUsageEvent cursor2entity(Cursor c) {
		AppUsageEvent aue = new AppUsageEvent();
		
		aue.packageName = c.getString(CNUM_PACKAGENAME);
		aue.eventtype = c.getShort(CNUM_EVENTTYPE);
		aue.runtime = c.getLong(CNUM_RUNTIME);
		aue.starttime = c.getLong(CNUM_STARTTIME);
		aue.longitude = c.getDouble(CNUM_LONGITUDE);
		aue.latitude = c.getDouble(CNUM_LATITUDE);
		aue.accuracy = c.getDouble(CNUM_ACCURACY);
		aue.powerstate = c.getShort(CNUM_POWERSTATE);
		aue.wifistate = c.getShort(CNUM_WIFISTATE);
		aue.bluetoothstate = c.getShort(CNUM_BLUETOOTHSTATE);
		aue.headphones = c.getShort(CNUM_HEADPHONES);
		aue.syncStatus = c.getInt(CNUM_SYNCSTATUS);
		aue.timestampOfLastScreenOn = c.getLong(CNUM_LASTSCREENON);
		aue.speed = c.getDouble(CNUM_SPEED);
		aue.altitude = c.getDouble(CNUM_ALTITUDE);
		aue.orientation = c.getShort(CNUM_ORIENTATION);
		aue.powerlevel = c.getShort(CNUM_POWERLEVEL);
		return aue;
	}

	
	
}
