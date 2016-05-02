package org.appkicker.app.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.appkicker.app.R;
import org.appkicker.app.algs.AppKickerFiller;
import org.appkicker.app.logging.AppUsageLogger;
import org.appkicker.app.logging.BackgroundService;
import org.appkicker.app.utils.Utils;
import org.appkicker.app.widget.WidgetProvider;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * @author Matthias Boehmer, mail@matthiasboehmer.de
 */
public class WidgetConfigurationActivity extends Activity {

	private int widgetId;
	private Spinner spinnerAlgo;
	private Spinner spinnerBackground;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		BackgroundService.startByIntent(this);

		Utils.dToast("onCreate WidgetConfigurationActivity");
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
		    widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		
		setContentView(R.layout.configurewidget_activity);
		
		
		//checkBox = (CheckBox) findViewById(R.id.checkShowBackground);
		
	    spinnerAlgo = (Spinner) findViewById(R.id.spinnerAlgorithm);
	    spinnerBackground = (Spinner) findViewById(R.id.spinnerBackground);
	    
	    String[] arrayNames = getResources().getStringArray(R.array.algs_array);
	    
	    // randomize for counter-balancing
	    List<String> algs = new ArrayList<String>(5);
		algs.add(arrayNames[1]);
		algs.add(arrayNames[2]);
		algs.add(arrayNames[3]);
		algs.add(arrayNames[4]);
		algs.add(arrayNames[5]);
		Collections.shuffle(algs);
		arrayNames[1] = algs.get(0);
		arrayNames[2] = algs.get(1);
		arrayNames[3] = algs.get(2);
		arrayNames[4] = algs.get(3);
		arrayNames[5] = algs.get(4);
		
		ArrayAdapter<CharSequence> adapterAlgo = new ArrayAdapter<CharSequence>(
				this,android.R.layout.simple_spinner_item, arrayNames); 
	    
	    adapterAlgo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinnerAlgo.setAdapter(adapterAlgo);
		
	    ArrayAdapter<CharSequence> backgroundAlgo = ArrayAdapter.createFromResource(
	            this, R.array.background_array, android.R.layout.simple_spinner_item);
	    backgroundAlgo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    spinnerBackground.setAdapter(backgroundAlgo);
	    
		
	}
	
	@Override
	protected void onResume() {
		Utils.dToast("onResume WidgetConfigurationActivity");
		super.onResume();
	}
	


	public static final int BACKGROUND_COLOR_GREY = 0;
	public static final int BACKGROUND_COLOR_RED = 1;
	public static final int BACKGROUND_COLOR_GREEN = 2;
	public static final int BACKGROUND_COLOR_BLUE = 3;
	public static final int BACKGROUND_COLOR_TRANSPARENT = 4;
	
	public void onOkClick(View v) {
		
		Utils.dToast(this, "alg: "+spinnerAlgo.getSelectedItem().toString());
		Utils.dToast(this, "bac: "+spinnerBackground.getSelectedItem().toString());
		
		SharedPreferences.Editor prefs = this.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
		
		// retrieve settings
		//boolean showBackground = checkBox.isChecked();
				
		
		// retrieve chosen selection method
		String methodString = spinnerAlgo.getSelectedItem().toString();
		int method = AppKickerFiller.STUDY_APPS_ALG_ID;
		if (methodString.toLowerCase().contains("scientific")) method = AppKickerFiller.STUDY_APPS_ALG_ID;
		else if (methodString.toLowerCase().contains("ppm")) method = AppKickerFiller.PPM_APPS_ALG_ID;
		else if (methodString.toLowerCase().contains("recently")) method = AppKickerFiller.RECENT_APPS_ALG_ID;
		else if (methodString.toLowerCase().contains("most")) method = AppKickerFiller.MOST_APPS_ALG_ID;
		else if (methodString.toLowerCase().contains("sequentially")) method = AppKickerFiller.TRANSI_APPS_ALG_ID;
		else if (methodString.toLowerCase().contains("locally")) method = AppKickerFiller.LOCATION_APPS_ALG_ID;
		Utils.dToast(this, "setting method: " + method);
		prefs.putInt(Utils.SETTINGS_settings_widgetalgo + widgetId, method);
		
		// retrieve chosen background settings
		String colorString = spinnerBackground.getSelectedItem().toString();
		int color = BACKGROUND_COLOR_GREY;
		if (colorString.toLowerCase().contains("grey")) color = BACKGROUND_COLOR_GREY;
		else if (colorString.toLowerCase().contains("red")) color = BACKGROUND_COLOR_RED;
		else if (colorString.toLowerCase().contains("green")) color = BACKGROUND_COLOR_GREEN;
		else if (colorString.toLowerCase().contains("blue")) color = BACKGROUND_COLOR_BLUE;
		else if (colorString.toLowerCase().contains("transparent")) color = BACKGROUND_COLOR_TRANSPARENT;
		prefs.putInt(Utils.SETTINGS_settings_widgetbackground + widgetId, color);
		
		// write UI into widget's settings
		//prefs.putBoolean(Utils.SETTINGS_settings_widgetalgo + widgetId, showBackground);
        prefs.commit();
        
		WidgetProvider.configureWidget(this, AppWidgetManager.getInstance(this), widgetId);

		AppUsageLogger.getAppUsageLogger(this).logWidgetCreated(method, widgetId);
		
		// return widget
		returnAndCreateWidget(widgetId);
	}

	
	private void returnAndCreateWidget(int widgetId) {
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

}