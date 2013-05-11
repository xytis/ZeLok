package org.nolife4life.zelok;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	private EditText setupPhoneNumber;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (BuildConfig.DEBUG) {
			Log.d(Constants.LOG, "onCreated called");
		}
		
		activityVisible = true;
		
		Bundle extras = getIntent().getExtras();
	    if (extras == null) {
	    	setContentView(R.layout.activity_main);
	    } else {
	    	setupCompleted();
	    }		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onSetup(View view) {
		setContentView(R.layout.activity_setup);
		setupPhoneNumber = (EditText) findViewById(R.id.editSetupPhone);
	}
	
	public void onSend(View view) {
		//Check for valid phone number:
		if (!enteredValidPhoneNumber(setupPhoneNumber)) {
			Toast.makeText(this, "Please enter a valid number",
					Toast.LENGTH_LONG).show();
			return;
		}
		//Setup listener to use given number
		String number = setupPhoneNumber.getText().toString();
		SmsInterceptor.setPhoneNumber(number);
		
		//Send setup message
		SmsManager sm = SmsManager.getDefault();
		String msg = "Hello You";
		sm.sendTextMessage(number, null, msg, null, null);
		
		//Change the view to "Please Wait"
		setContentView(R.layout.activity_wait);
	}
	
	public void onTest(View view) {
		
	}
	
	public void setupCompleted() {
		setContentView(R.layout.activity_test);
	}
	
	private boolean enteredValidPhoneNumber(EditText text) {
		return text.getText().length() != 0;
	}
	
	public static boolean isActivityVisible() {
		return activityVisible;
	}

	public static void activityResumed() {
		activityVisible = true;
	}

	public static void activityPaused() {
		activityVisible = false;
	}

	private static boolean activityVisible;

}
