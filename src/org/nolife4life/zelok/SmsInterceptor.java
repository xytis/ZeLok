/**
 * 
 */
package org.nolife4life.zelok;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * @author xytis
 *
 */
public class SmsInterceptor extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static String phoneNumber;
    private static boolean setupComplete = false;
    
    public static void setPhoneNumber(String number) {
    	Log.i(Constants.LOG, "Number received: " + number);
    	number.trim().replaceAll("[ -]", "");
    	Log.i(Constants.LOG, "Number formated: " + number);
    	phoneNumber = number;
    	setupComplete = true;
    }
    
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(Constants.LOG, "Intent received: " + intent.getAction());
		if (setupComplete && intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[])bundle.get("pdus");
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                }
                if (messages.length > -1) {
                	Log.i(Constants.LOG, "Message received: " + messages[0].getMessageBody());
                    Log.i(Constants.LOG, "From: " + messages[0].getOriginatingAddress());
                    if (messages[0].getOriginatingAddress().equals(phoneNumber)) {
                    	Log.i(Constants.LOG, "This message triggered the Zelok");
                    	//This line will stop the sms from going to other receivers.
                    	abortBroadcast();
                    	//Either trigger existing activity, either display notification, which will
                    	//trigger required state.
                    	Intent i = new Intent(context, MainActivity.class);
                    	i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NEW_TASK);
                    	i.putExtra("Value1", "This value one for ActivityTwo ");
                    	if (MainActivity.isActivityVisible()) {
                    		context.startActivity(i);
                    	} else {
                    		
                    	}
                    }
                }
            }
        }
	}

}
