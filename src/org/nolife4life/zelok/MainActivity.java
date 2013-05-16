package org.nolife4life.zelok;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MainActivity.activityResumed();

		if (BuildConfig.DEBUG) {
			Log.d(Constants.LOG, "onCreated called");
		}

		setContentView(R.layout.activity_main);

		//Get current applcation state.

		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		int applicationState = settings.getInt("state", 0);

		switch (applicationState) {
		case Constants.FRESH_START: {
			//Launch introduction slides.
			enterIntroductionState();	    	
			break;
		}	    
		case Constants.SETUP_START: {
			enterSetupState();
			break;
		}
		case Constants.SETUP_PHONE_SET: {
			//Check if Intent returned a message. If not -- display error message.
			Bundle bundle = getIntent().getExtras();
			if (bundle != null) {
				//Check for message:
				parseMessageAndEnterState(bundle.getString("Message"));
			} else {
				setContentView(R.layout.setup_wait_for_response);
			}
			break;
		}
		//		case Constants.SETUP_CONFIRMATION_RECEIVED: {
		//			setContentView(R.layout.activity_test);
		//			break;
		//		}
		default: {
			//Check if Intent returned a message. If not -- ?
			Bundle bundle = getIntent().getExtras();
			if (bundle != null) {
				parseMessageAndEnterState(bundle.getString("Message"));			
			} else {
				setContentView(R.layout.activity_main);
			}
			break;
		}
		}		
	}

	private void enterIntroductionState() {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("state", Constants.INTRODUCTION_STATE);
		setContentView(R.layout.introduction_hello);
		editor.commit();	    
	}

	public void clickedIntroductionSlide(View view) {
		//TODO: this method should advance the slides until the intro is over.
		enterSetupState();
	}

	private void enterSetupState() {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("state", Constants.SETUP_START);
		//Try to get any saved device number:
		
		setContentView(R.layout.setup_enter_phone_step);
		editor.commit();
	}

	//Hides retry button.
	class ShowRetryButtonTask extends TimerTask {
		@Override
		public void run() {
			showRetryButton();
		}
	};

	private void showRetryButton() {
		findViewById(R.id.retry_button).setVisibility(View.VISIBLE);
	}

	private void hideRetryButton() {
		findViewById(R.id.retry_button).setVisibility(View.GONE);
	}

	public void setupPhoneNumberEntered(View view) {
		EditText setupPhoneNumber = (EditText) findViewById(R.id.editSetupPhone);
		//Check for valid phone number:
		if (!enteredValidPhoneNumber(setupPhoneNumber)) {
			Toast.makeText(this, "Please enter a valid number",
					Toast.LENGTH_LONG).show();
			return;
		}
		//Setup listener to use given number
		String number = setupPhoneNumber.getText().toString();
		savePhoneNumber(number);
		//Change the view to "Please Wait"

		hideRetryButton();
		setContentView(R.layout.setup_wait_for_response);
		new Timer().schedule(new ShowRetryButtonTask(), 300000);

		getUserPhoneNumber();
				
		sendMessage(createSetupString());	
		
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("state", Constants.SETUP_PHONE_ENTERED);
		setContentView(R.layout.introduction_hello);
		editor.commit();	
	}

	private void getUserPhoneNumber() {
		TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String number = tMgr.getLine1Number();
		
		number = TrackerMessage.FormatPhoneNumber(number);
		assert number.length() > 0 : "Device phone number query failed.";
		
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putString("user_phone_number", number);
		editor.commit();
	}
	
	private String createSetupString() {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		String number = settings.getString("user_phone_number", "");
		
		assert number.length() > 0 : "Device phone number not saved in preferences!";		
		
		return TrackerMessage.AlarmDestinationNumber(number);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		MainActivity.activityResumed();
	}

	@Override
	public void onRestart() {
		super.onRestart();
		MainActivity.activityResumed();
	}

	@Override
	public void onStop() {
		super.onStop();
		MainActivity.activityPaused();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		MainActivity.activityPaused();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onEnter(View view) {
		//Switch to other views if setup is completed. This method is triggered by a button.
		setContentView(R.layout.activity_setup);
	}



	public void onSetup(View view) {
		//Try to reinit the setup.
		setContentView(R.layout.activity_setup);
		EditText setupPhoneNumber = (EditText) findViewById(R.id.editSetupPhone);
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0); 
		String phoneNumber = settings.getString("phoneNumber", "");
		if (phoneNumber.length() > 0) {
			setupPhoneNumber.setText(phoneNumber);
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			setupPhoneNumber.setHint(getResources().getString(R.string.setup_textfield_default));
		}
	}

	public void openSettings(View view) {

		setContentView(R.layout.activity_settings);
	}

	private void parseMessageAndEnterState(String message) {
		//Check for config header:
		if (message.matches("^CFG.*")) {
			//Forward to test view:
			setContentView(R.layout.setup_test_device);
		} else if (message.matches("^INF.*")) {
			saveBateryState(message);
			setContentView(R.layout.activity_main);
		} else if (message.matches("^ALARM.*")) {
			saveBateryState(message);
			message = message.substring(message.indexOf("http://"));
			//Message should contain a link.
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(message));
			startActivity(i);
			setContentView(R.layout.activity_main);
		} else {
			//Responce to WIM?
			//Message should contain a link.
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(message));
			startActivity(i);
			setContentView(R.layout.activity_main);
		}
	}

	private void sendMessage(String message) {
		//Send setup message
		SmsManager sm = SmsManager.getDefault();

		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		String phoneNumber = settings.getString("phoneNumber", "");

		sm.sendTextMessage(phoneNumber, null, message, null, null);				
	}



	private String createLocationQueryString() {
		return "WIM?";
	}

	public void onTest(View view) {
		setContentView(R.layout.setup_wait_for_response);
		sendMessage(createLocationQueryString());		
	}

	private boolean enteredValidPhoneNumber(EditText text) {
		return text.getText().length() != 0;
	}

	private void saveBateryState(String message) {
		Pattern p = Pattern.compile(".*BAT_LVL:(\\d+)");
		Matcher m = p.matcher(message);
		if (m.find()) {
			int bateryLevel = Integer.parseInt(m.group(1));

			SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);    	
			SharedPreferences.Editor editor = settings.edit();

			editor.putInt("bateryLevel", bateryLevel);
			editor.putLong("bateryCheck", System.currentTimeMillis());

			editor.commit();
		} else {
			Log.e(Constants.LOG, "Failed to set battery!");
		}
	}

	private void savePhoneNumber(String number) {
		Log.i(Constants.LOG, "Saving number: " + number);
		number.trim().replaceAll("[ -]", "");
		Log.i(Constants.LOG, "After formatting: " + number);

		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);    	
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("setupState", Constants.SETUP_PHONE_SET);
		editor.putString("phoneNumber", number);

		editor.commit();
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
