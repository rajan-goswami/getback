package com.codeperf.getback.core;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;

public class SMSNotifier extends BroadcastReceiver {

	public static final String ACTION_SMS_SENT = "com.codeperf.getback.SMS_SENT_ACTION";
	private static ISMSNotifierCallback callback;

	@Override
	public void onReceive(Context paramContext, Intent paramIntent) {
		String message = null;
		switch (getResultCode()) {
		case Activity.RESULT_OK:
			message = "Message sent!";
			String receipient = "";
			if (paramIntent != null)
				receipient = paramIntent.getStringExtra("current_receipient");

			callback.onSmsSent(receipient);
			return;
		case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
			message = "Error.";
			break;
		case SmsManager.RESULT_ERROR_NO_SERVICE:
			message = "Error: No service.";
			break;
		case SmsManager.RESULT_ERROR_NULL_PDU:
			message = "Error: Null PDU.";
			break;
		case SmsManager.RESULT_ERROR_RADIO_OFF:
			message = "Error: Radio off.";
			break;
		}
		Utils.LogUtil.LogD(Constants.LOG_TAG, message);
		callback.onSMSError(-1);
	}

	public static void sendMessage(Context context, String receipient,
			String messageBody, ISMSNotifierCallback cb) {
		if (receipient == null || messageBody == null || receipient.isEmpty()
				|| messageBody.isEmpty()) {
			Utils.LogUtil.LogW(Constants.LOG_TAG,
					"Receipients/MessageBody is missing");
			return;
		}

		callback = cb;

		SmsManager smsManager = SmsManager.getDefault();

		ArrayList<String> messages = smsManager.divideMessage(messageBody);
		ArrayList<PendingIntent> sentIntentList = new ArrayList<PendingIntent>(
				messages.size());

		for (int i = 0; i < messages.size(); i++) {
			Intent sendInt = new Intent();
			sendInt.setAction(ACTION_SMS_SENT);
			sendInt.putExtra("current_receipient", receipient);
			PendingIntent sendResult = PendingIntent.getBroadcast(context, 0,
					sendInt, 0);
			sentIntentList.add(sendResult);
		}

		smsManager.sendMultipartTextMessage(receipient, null, messages,
				sentIntentList, null);
	}
}
