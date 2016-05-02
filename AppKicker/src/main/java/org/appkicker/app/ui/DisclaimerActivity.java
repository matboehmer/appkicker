package org.appkicker.app.ui;

import org.appkicker.app.R;
import org.appkicker.app.logging.BackgroundService;
import org.appkicker.app.utils.Utils;
import org.appkicker.app.widget.WidgetProvider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class DisclaimerActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.disclaimer_activity);
		BackgroundService.startByIntent(this);
		
		WebView wv = (WebView) findViewById(R.id.disclaimerText);
		wv.loadUrl("file:///android_asset/disclaimer.html");
	}

//	protected void dispatchOrAsk() {
//		// Check whether the user has already accepted
//
//		boolean accepted = Utils.isDisclaimerAcknowledged(this);
//		Utils.d(this, "accepted:" + accepted);
//
//		if (accepted) {
//			finish();
//			
//			
//					
//		} else {
//			
//			
//		}
//	}
	
	public void onAgreeClick(View v) {
		Utils.setDisclaimerAcknowledged(this);
		
		startApplicationAfterwards();
		
	}

	private void startApplicationAfterwards() {
		// start other app if call was from widget
		Intent startIntent = getIntent();
		String pname = startIntent.getStringExtra(WidgetProvider.INTENT_FIELD_PACKAGENAME);

		if (pname != null) {
			// now we should start the app that the user wanted to start previously
			Intent app = this.getPackageManager().getLaunchIntentForPackage(pname);
			app.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (app != null) {
				this.startActivity(app);
			}
		} else {
			finish();
		}	
	}

	public void onDeclineClick(View v) {
		Utils.setDisclaimerNotAcknowledged(this);
		//Toast.makeText(this, R.string.accepttouse, 1500).show();
		startApplicationAfterwards();
//		moveTaskToBack(true);
	}
}