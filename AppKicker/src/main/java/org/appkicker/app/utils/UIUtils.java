package org.appkicker.app.utils;

import org.appkicker.app.ui.DisclaimerActivity;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class UIUtils {

//	public static void openHomePage(Context c) {
//		Intent intent = new Intent(c, HomeActivity.class);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		Log.d(Utils.TAG, "UIUtils: HomeActivity");
//		c.startActivity(intent);
//	}
//
	public static void openDisclaimerActivity(Context c) {
		Intent intent = new Intent(c, DisclaimerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		c.startActivity(intent);
	}
//	
//	public static void openURIView(Context c, Uri uri) {
//		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//		c.startActivity(intent);
//	}
//
//	public static void openURIView(Context c, Uri uri, String headline) {
//		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//		intent.putExtra("headline", headline); // TODO externalize
//		c.startActivity(intent);
//	}

	public static void shortToast(Context context, String text) {
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(AppKickerApplication.getAppContext(), text, duration);
		toast.show();
	}
	
	public static void shortToastNotYetImplemented(Context context) {
		int duration = Toast.LENGTH_SHORT;
		CharSequence text = "Not yet implemented ;-)";
		Toast toast = Toast.makeText(context, text , duration);
		toast.show();
	}

}
