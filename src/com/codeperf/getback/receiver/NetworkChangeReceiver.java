package com.codeperf.getback.receiver;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;
import com.codeperf.getback.core.GetBackCoreService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Received NetworkChange event");

		// Check if GetBack was in active mode when device went off
		if (Utils.isActiveMode(context)) {
			Intent serviceIntent = new Intent(
					Constants.ACTION_NETWORKSTATE_CHANGED);
			serviceIntent.setClass(context, GetBackCoreService.class);
			context.startService(serviceIntent);
		}
	}
}
