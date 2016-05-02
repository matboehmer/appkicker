package org.appkicker.app.data.db;

import org.appkicker.app.data.entities.App;
import org.appkicker.app.utils.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

/**
 * Data access object for apps that allows to retrieve information from the local mobile database.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class AppDAO extends GeneralDAO {
	
	// --------------------------------------------
	// SCHEMA
	// --------------------------------------------

	public static String TABLE_NAME = "apps";
	
	public static final String CNAME_ID = "_id";
	public static final String CNAME_PACKAGENAME = "packagename";
	public static final String CNAME_NAME = "title";
	public static final String CNAME_CATEGORY = "category";
	public static final String CNAME_DESCRIPTION = "description";
	public static final String CNAME_RATING = "rating";
	public static final String CNAME_PRICE = "price";
	public static final String CNAME_TIMESTAMP = "timestamp"; // timestamp when
	public static final String CNAME_RECOMMENDABLE = "recommendable";

	public static final String[] PROJECTION = {
	    CNAME_ID,
	    CNAME_PACKAGENAME,
	    CNAME_NAME,
	    CNAME_CATEGORY,
	    CNAME_DESCRIPTION,
	    CNAME_RATING,
	    CNAME_PRICE,
	    CNAME_TIMESTAMP,
	    CNAME_RECOMMENDABLE
    };

	public final static int CNUM_ID = 0;
	public final static int CNUM_PACKAGENAME = 1;
	public final static int CNUM_NAME = 2;
	public final static int CNUM_CATEGORY = 3;
	public final static int CNUM_DESCRIPTION = 4;
	public final static int CNUM_RATING = 5;
	public final static int CNUM_PRICE = 6;
	public final static int CNUM_TIMESTAMP = 7;
	public final static int CNUM_RECOMMENDABLE = 8;
	
	
	public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" +
		CNAME_ID + " INTEGER PRIMARY KEY, " +
		CNAME_PACKAGENAME + " TEXT, " +
		CNAME_NAME + " TEXT, " +
		CNAME_CATEGORY + " TEXT, " +
		CNAME_DESCRIPTION + " TEXT, " +
		CNAME_RATING + " DOUBLE, " +
		CNAME_PRICE + " DOUBLE, " +
		CNAME_TIMESTAMP + " LONG, " +
		CNAME_RECOMMENDABLE + " INTEGER " +
	");";
	
	// --------------------------------------------
	// QUERIES
	// --------------------------------------------
	
	private final static String WHERE_PACKAGENAME = CNAME_PACKAGENAME + "=?";
	
	// --------------------------------------------
	// LIVECYCLE
	// --------------------------------------------
	
	public AppDAO(Context context) {
		super(context);
	}
	
	// --------------------------------------------
	// CRUD
	// --------------------------------------------
	
	public Cursor find(String packageName) {
		Cursor c = db.query(
				TABLE_NAME, 
				PROJECTION, 
				WHERE_PACKAGENAME, 
				new String[]{packageName}, 
				null, 
				null, 
				null);
		return c;
	}

	public void insert(App a) {
		ContentValues cv = app2ContentValues(a);
		db.insert(TABLE_NAME, null, cv);
	}

	public Cursor app2cursor(App a) {
		MatrixCursor mcp = new MatrixCursor(PROJECTION);
		mcp.addRow(app2objectarray(a));
		return mcp;
	}

	public void update(App a) {
		ContentValues values = app2ContentValues(a);
		db.update(TABLE_NAME, values , WHERE_PACKAGENAME, new String[]{a.packagename});
	}
	
	public void deleteAll() {
		Utils.d2(this, "delete all from " + TABLE_NAME);
		db.delete(TABLE_NAME, null, null);
	}
	
	// --------------------------------------------
	// TRANSFORMATION
	// --------------------------------------------

	public static Object[] app2objectarray(App a) {
		return new Object[] { 
				a.id, 
				a.packagename, 
				a.name, 
				a.category, 
				a.description,
				a.rating,
				a.price,
				a.timestamp,
				a.recommendable
		};
	}
	
	public static App cursor2app(Cursor c) {
		c.moveToFirst();
		App a = new App();
		a.id = c.getInt(CNUM_ID); 
		a.packagename= c.getString(CNUM_PACKAGENAME); 
		a.name = c.getString(CNUM_NAME); 
		a.category = c.getString(CNUM_CATEGORY); 
		a.description = c.getString(CNUM_DESCRIPTION);
		a.rating = c.getDouble(CNUM_RATING);
		a.price = c.getDouble(CNUM_PRICE);
		a.timestamp = c.getLong(CNUM_TIMESTAMP);
		a.recommendable = c.getInt(CNUM_RECOMMENDABLE);
		return a;
	}

	public static ContentValues app2ContentValues(App a) {
		ContentValues cv = new ContentValues();
		cv.put(CNAME_CATEGORY, a.category);
		cv.put(CNAME_DESCRIPTION, a.description);
		cv.put(CNAME_NAME, a.name);
		cv.put(CNAME_PACKAGENAME, a.packagename);
		cv.put(CNAME_ID, a.id);
		cv.put(CNAME_PRICE, a.price);
		cv.put(CNAME_RATING, a.rating);
		cv.put(CNAME_TIMESTAMP, a.timestamp);
		cv.put(CNAME_RECOMMENDABLE, a.recommendable);
		return cv;
	}



}
