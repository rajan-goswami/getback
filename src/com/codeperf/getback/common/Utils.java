package com.codeperf.getback.common;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.codeperf.getback.R;

public class Utils {

	// private static Context context = null;
	private static final char[] SEKRIT = { 'Q', 'W', 'E', 'R', 'T', '$', '%',
			'^', '&', '2', '3', '5', '6' };

	public static String getApplicationName(Context context) {
		if (context != null) {
			int stringId = context.getApplicationInfo().labelRes;
			return context.getString(stringId);
		}
		return null;
	}

	public static boolean isFrontCameraPresent(Context context) {
		// Utils.context = context.getApplicationContext();

		boolean result = false;
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {

			int numOfCameras = Camera.getNumberOfCameras();
			for (int i = 0; i < numOfCameras; i++) {
				CameraInfo info = new CameraInfo();
				Camera.getCameraInfo(i, info);
				if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	public static boolean getConnectivityStatus(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (null != activeNetwork) {
			if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
				return true;

			if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
				return true;
		}
		return false;
	}

	public static void turnGPSOn(Context context) {
		String provider = Settings.Secure.getString(
				context.getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Providers - " + provider);
		if (!provider.contains("gps")) { // if gps is disabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			context.sendBroadcast(poke);
		}
	}

	public static void turnGPSOff(Context context) {
		String provider = Settings.Secure.getString(
				context.getContentResolver(),
				Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (provider.contains("gps")) { // if gps is enabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings",
					"com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			context.sendBroadcast(poke);
		}
	}

	public static boolean isActiveMode(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean isActive = sharedPref.getBoolean(context.getResources()
				.getString(R.string.pref_key_operation_mode), false);
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Active Mode - " + isActive);
		return isActive;

	}

	public static boolean isHelpEnableAtStartup(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean isHelp = sharedPref.getBoolean(context.getResources()
				.getString(R.string.pref_key_help_enable), true);
		return isHelp;
	}

	public static String getRevocationCode(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String revokeCode = sharedPref.getString(context.getResources()
				.getString(R.string.pref_key_revoke_setup), "");
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Revocation code - " + revokeCode);
		return revokeCode;
	}

	public static void configureCurrentSim(Context context) {
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (sharedPref.contains(Constants.PREFERENCE_SIM_SERIAL)) {
			Editor edit = sharedPref.edit();
			edit.remove(Constants.PREFERENCE_SIM_SERIAL);
			edit.commit();
		}

		TelephonyManager telephonyMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String simSerial = telephonyMgr.getSimSerialNumber();

		if (simSerial != null) {
			Editor edit = sharedPref.edit();
			edit.putString(Constants.PREFERENCE_SIM_SERIAL, simSerial);
			edit.commit();
		}
	}

	public static void hideApplication(Context context) {
		Log.d("GetBack", "Inside hideApplication");
		ComponentName componentToDisabled = new ComponentName(
				"com.codeperf.getback", "com.codeperf.getback.ui.MainActivity");
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(componentToDisabled,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);
	}

	public static void unhideApplication(Context context) {
		Log.d("GetBack", "Inside unhideApplication");
		ComponentName componentToEnabled = new ComponentName(
				"com.codeperf.getback", "com.codeperf.getback.ui.MainActivity");
		PackageManager pm = context.getApplicationContext().getPackageManager();
		pm.setComponentEnabledSetting(componentToEnabled,
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
				PackageManager.DONT_KILL_APP);
	}

	public static boolean isTriggerSMS(Context context, String messageBody) {
		boolean bReturn = false;
		if (messageBody != null && !messageBody.isEmpty()) {
			String[] splits = messageBody.split(
					Constants.SMS_COMMAND_DELIMETER, 2);
			if (splits != null && splits.length == 2) {
				SharedPreferences sharedPref = PreferenceManager
						.getDefaultSharedPreferences(context);
				String commandText = sharedPref.getString(
						context.getResources().getString(
								R.string.pref_key_command_text), "");
				if (!commandText.isEmpty()) {
					if (commandText.equals(splits[0]))
						bReturn = true;
				}
			}
		}

		return bReturn;
	}

	public static String[] getConfiguredCommandNo(Context context) {
		String[] commandNos = new String[2];
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		commandNos[0] = sharedPref.getString(
				context.getResources().getString(
						R.string.pref_key_command_number_1), "");
		commandNos[1] = sharedPref.getString(
				context.getResources().getString(
						R.string.pref_key_command_number_2), "");
		return commandNos;
	}

	public static String parseCommandFromSms(String messageBody) {
		if (messageBody != null && !messageBody.isEmpty()) {
			String[] splits = messageBody.split(
					Constants.SMS_COMMAND_DELIMETER, 2);
			if (splits != null && splits.length == 2) {
				return splits[1];
			}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	public String encrypt(Context context, String value) {

		try {
			final byte[] bytes = value != null ? value.getBytes("utf-8")
					: new byte[0];
			SecretKeyFactory keyFactory = SecretKeyFactory
					.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(
					Cipher.ENCRYPT_MODE,
					key,
					new PBEParameterSpec(Settings.Secure.getString(
							context.getContentResolver(),
							Settings.System.ANDROID_ID).getBytes("utf-8"), 20));
			return new String(Base64.encode(pbeCipher.doFinal(bytes),
					Base64.NO_WRAP), "utf-8");

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@SuppressWarnings("deprecation")
	public String decrypt(Context context, String value) {
		try {
			final byte[] bytes = value != null ? Base64.decode(value,
					Base64.DEFAULT) : new byte[0];
			SecretKeyFactory keyFactory = SecretKeyFactory
					.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(
					Cipher.DECRYPT_MODE,
					key,
					new PBEParameterSpec(Settings.Secure.getString(
							context.getContentResolver(),
							Settings.System.ANDROID_ID).getBytes("utf-8"), 20));
			return new String(pbeCipher.doFinal(bytes), "utf-8");

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static class LogUtil {
		public static void LogD(String tag, String msg) {
			if (Constants.DEBUG_FLAG)
				Log.d(tag, "===== " + msg + " =====");
			// if (context != null) {
			// Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			// }
		}

		public static void LogW(String tag, String msg) {
			if (Constants.DEBUG_FLAG)
				Log.w(tag, "~~~~~ " + msg + " ~~~~~");
		}

		public static void LogE(String tag, String msg, Throwable e) {
			if (Constants.DEBUG_FLAG)
				Log.e(tag, "^^^^^ " + msg + " ^^^^^", e);
		}
	}
}
