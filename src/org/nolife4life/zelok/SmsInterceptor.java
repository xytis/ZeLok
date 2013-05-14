/**
 * 
 */
package org.nolife4life.zelok;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * @author xytis
 *
 */
public class SmsInterceptor extends BroadcastReceiver {
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    
	@Override
	public void onReceive(Context context, Intent intent) {
		//Check saved sate:
		SharedPreferences settings = context.getSharedPreferences(Constants.PREFS_NAME, 0); 
		boolean setupComplete = settings.getInt("setupState", 0) >= Constants.SETUP_PHONE_SET;
		
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
                    String phoneNumber = settings.getString("phoneNumber", "");                
                    if (messages[0].getOriginatingAddress().equals(phoneNumber)) {
                    	Log.i(Constants.LOG, "This message triggered the Zelok");
                    	//This line will stop the sms from going to other receivers.
                    	abortBroadcast();
                    	//Either trigger existing activity, either display notification, which will
                    	//trigger required state.
                    	Intent i = new Intent(context, MainActivity.class);
                    	i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NEW_TASK);
                    	i.putExtra("Message", messages[0].getMessageBody());
                    	if (MainActivity.isActivityVisible()) {
                    		context.startActivity(i);
                    	} else {
							PendingIntent pIntent = PendingIntent.getActivity(
									context, 0, i, 0);

							// this
							
							Notification notification = new Notification(R.drawable.ic_launcher,  context.getString(R.string.notification_setup_finished_short), System.currentTimeMillis());
							notification.setLatestEventInfo(context, context.getString(R.string.notification_setup_finished_title), context.getString(R.string.notification_setup_finished_content), pIntent);

							// Hide the notification after its selected
							notification.flags |= Notification.FLAG_AUTO_CANCEL;
							notification.flags |= Notification.FLAG_NO_CLEAR;
							
							NotificationManager notificationManager = (NotificationManager) context
									.getSystemService(Context.NOTIFICATION_SERVICE);

							notificationManager.notify(0, notification);
                    	}
                    }
                }
            }
        }
	}

}
