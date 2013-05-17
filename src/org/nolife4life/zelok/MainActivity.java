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

		setViewByState(getState());		
	}
	
	private void setViewByState(int state) {
		switch (state) {
		case Constants.FRESH_START: {
			//Launch introduction slides.
			enterIntroductionState();	    	
			break;
		}	    
		case Constants.INTRODUCTION_STATE: {
			enterIntroductionState();
			break;
		}
		case Constants.SETUP_START: {
			enterSetupState();
			break;
		}
		case Constants.SETUP_PHONE_ENTERED: {
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
		case Constants.SETUP_PHONE_CONFIRMED: {
			enterTestState();
			break;
		}
		case Constants.SETUP_TEST_STARTED: {
			Bundle bundle = getIntent().getExtras();
			if (bundle != null) {
				//Check for message:
				parseMessageAndEnterState(bundle.getString("Message"));
			} else {
				setContentView(R.layout.setup_wait_for_response);
			}
			break;
		}
		case Constants.SETUP_TEST_COMPLETED: {
			enterCongratulationsState();
		}
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

	private int getState() {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		return settings.getInt("state", 0);		
	}
	
	private void setState(int state) {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt("last_state", settings.getInt("state", 0));
		editor.putInt("state", Constants.INTRODUCTION_STATE);
		editor.commit();
	}
	
	private void setUserPhoneNumber(String number) {
		number = TrackerMessage.FormatPhoneNumber(number);

		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);    	
		SharedPreferences.Editor editor = settings.edit();

		editor.putString("user_phone_number", number);
		editor.commit();
	}
	
	private String getUserPhoneNumber() {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);   
		return settings.getString("user_phone_number", "");
	}
	
	private void setDevicePhoneNumber(String number) {
		number = TrackerMessage.FormatPhoneNumber(number);

		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);    	
		SharedPreferences.Editor editor = settings.edit();

		editor.putString("device_phone_number", number);
		editor.commit();
	}
	
	private String getDevicePhoneNumber() {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);   
		return settings.getString("device_phone_number", "");
	}
	
	private void enterIntroductionState() {
		setState(Constants.INTRODUCTION_STATE);
		setContentView(R.layout.introduction_hello);   
	}

	public void clickedIntroductionSlide(View view) {
		//TODO: this method should advance the slides until the intro is over.
		enterSetupState();
	}

	private void enterSetupState() {
		setState(Constants.SETUP_START);
		//Try to get any saved device number:
		String number = getDevicePhoneNumber();
		if (number.length() > 0) {
			//Place this number instead of hint.
			EditText phoneField = (EditText) findViewById(R.id.editSetupPhone);
			phoneField.setText(number);
		}
		
		setContentView(R.layout.setup_enter_phone_step);
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
	
	public void retryLastAction(View view) {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		int state = settings.getInt("last_state", 0);
		
		editor.putInt("state", state);
		editor.commit();
		
		//Launch state
		setViewByState(state);
	}

	public void clickedDevicePhoneEntered(View view) {
		EditText phoneField = (EditText) findViewById(R.id.editSetupPhone);
		//Check for valid phone number:
		if (!enteredValidPhoneNumber(phoneField)) {
			Toast.makeText(this, "Please enter a valid number",
					Toast.LENGTH_LONG).show();
			return;
		}
		//Setup listener to use given number
		String number = phoneField.getText().toString();
		setDevicePhoneNumber(number);
		//Change the view to "Please Wait"

		hideRetryButton();
		setContentView(R.layout.setup_wait_for_response);
		new Timer().schedule(new ShowRetryButtonTask(), 300000);

		queryUserPhoneNumber();
				
		sendMessage(createSetupString());	
		
		setState(Constants.SETUP_PHONE_ENTERED);	
	}

	private void queryUserPhoneNumber() {
		TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String number = tMgr.getLine1Number();
		
		number = TrackerMessage.FormatPhoneNumber(number);
		assert number.length() > 0 : "Device phone number query failed.";
		
		setUserPhoneNumber(number);
	}
	
	private String createSetupString() {
		String number = getUserPhoneNumber();
		
		assert number.length() > 0 : "Device phone number not saved in preferences!";		
		
		return TrackerMessage.AlarmDestinationNumber(number);
	}
	
	private void enterTestState() {		
		setContentView(R.layout.setup_test_device);
	}
	
	public void clickedTestCurrentSettings(View view) {
		hideRetryButton();
		setContentView(R.layout.setup_wait_for_response);
		new Timer().schedule(new ShowRetryButtonTask(), 300000);
		
		setState(Constants.SETUP_TEST_STARTED);
		sendMessage(TrackerMessage.WhereAmI());	
	}
	
	private void enterCongratulationsState() {
		setContentView(R.layout.setup_done);
	}
	
	public void clickedCongratulationsSlide(View view) {
		setState(Constants.NORMAL);
		setContentView(R.layout.activity_main);
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

	public void openSettings(View view) {
		setContentView(R.layout.activity_settings);
	}

	private void parseMessageAndEnterState(String message) {
		//Check for config header:
		if (message.matches("^CFG.*")) {
			//Forward to test view:
			setContentView(R.layout.setup_test_device);
			setState(Constants.SETUP_PHONE_CONFIRMED);
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
