package com.codeperf.getback.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;
import com.codeperf.getback.core.GetBackCoreService;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Utils.LogUtil.LogD(Constants.LOG_TAG, "BootReceiver Called");

			// Check if GetBack was in active mode when device went off
			if (Utils.isActiveMode(context)) {

				// If user by chance did not setup revoke code,
				// Unhide application.
				if (Utils.getRevocationCode(context).isEmpty())
					Utils.unhideApplication(context);

				intent.setAction(Constants.ACTION_BOOT_COMPLETED);
				context.startService(new Intent(context,
						GetBackCoreService.class));
			}
		}
	}
}
