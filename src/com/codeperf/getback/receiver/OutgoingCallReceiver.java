package com.codeperf.getback.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;
import com.codeperf.getback.ui.HelpActivity;

public class OutgoingCallReceiver extends BroadcastReceiver {

	private static final String ACTION = "android.intent.action.NEW_OUTGOING_CALL";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() != null && intent.getAction().equals(ACTION)) {

			String phoneNumber = getResultData();
			Utils.LogUtil.LogD(Constants.LOG_TAG, "getResultData : "
					+ phoneNumber);
			
			if (phoneNumber == null) {

				// No reformatted number, use the original
				phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				Utils.LogUtil.LogD(Constants.LOG_TAG, "original number : "
						+ phoneNumber);
			}
			
			// Fix : On some device, phone number contains some spaces inside,
			// So Removing them before comparison 
 			phoneNumber = phoneNumber.replace(" ", "");
			
			if (Utils.getRevocationCode(context).equals(phoneNumber)) {

				// Revoke GetBack only if it is in active mode
				if (Utils.isActiveMode(context)) {

					setResultData(null);
					Intent startIntent = new Intent(context, HelpActivity.class);
					startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(startIntent);
					return;
				}
			}
			// Use rewritten number as the result data.
			setResultData(phoneNumber);
		}
	}
}
