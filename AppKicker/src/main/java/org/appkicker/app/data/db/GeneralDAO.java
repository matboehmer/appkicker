package org.appkicker.app.data.db;

import org.appkicker.app.utils.MyDBHelper;
import org.appkicker.app.utils.Utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * This component is a general data access object handling the live cycle of the
 * connection. Other DAOs of the app should subclass this component.
 * 
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public abstract class GeneralDAO {

	private Context context;
	private MyDBHelper dbHelper;
	protected SQLiteDatabase db;

	public GeneralDAO(Context context) {
		this.context = context;
		dbHelper = new MyDBHelper(this.context);
	}

	public GeneralDAO open() {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public GeneralDAO openWrite() {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public GeneralDAO openRead() {
		// db = dbHelper.getReadableDatabase();
		db = dbHelper.getWritableDatabase();
		Log.d(Utils.TAG, "db opened4read :-)");
		return this;
	}

	public void close() {
		db.close();
		Log.d(Utils.TAG, "db closed :-(");
	}
	
}
