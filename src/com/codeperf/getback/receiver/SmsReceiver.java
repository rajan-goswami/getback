package com.codeperf.getback.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;
import com.codeperf.getback.core.GetBackCoreService;

public class SmsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Sms Received in receiver");

		// Check if GetBack core is in active mode.
		if (Utils.isActiveMode(context)) {

			Bundle extras = intent.getExtras();

			Object[] pdus = (Object[]) extras.get("pdus");
			for (Object pdu : pdus) {
				SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);

				String origin = msg.getOriginatingAddress();
				String body = msg.getMessageBody();

				// Check if it has a special body.
				if (Utils.isTriggerCommandText(context, body)) {

					Utils.LogUtil.LogD(Constants.LOG_TAG,
							"Valid command text is received");

					// Stop it being passed to the main Messaging inbox
					this.abortBroadcast();

					// Check if command number matches with one of configured
					// commands for recovery actions
					if (Utils.hasConfiguredCommandNo(context, body)) {

						Bundle bundle = new Bundle();
						bundle.putString(Constants.KEY_SENDER, origin);
						bundle.putString(Constants.KEY_MESSAGE_BODY, body);

						Intent serviceIntent = new Intent(
								Constants.ACTION_SMS_RECEIVED);
						serviceIntent.setClass(context,
								GetBackCoreService.class);
						serviceIntent.putExtras(bundle);
						context.startService(serviceIntent);
					}
				}
			}
		}
	};

}
