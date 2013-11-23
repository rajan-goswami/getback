package com.codeperf.getback.core;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.codeperf.getback.R;
import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;

public class GetBackCoreService extends Service implements
		ILocationFinderCallback, IFrontCaptureCallback, ISMSNotifierCallback,
		ISendEmailCallback {

	private String remoteSmsOrigin = null;
	private String triggerCommand = null;

	private static class ActionLocks {
		public AtomicBoolean lockCapture;
		public AtomicBoolean lockSmsSend;
		public AtomicBoolean lockEmailSend;
		public AtomicBoolean lockLocationFind;
		public AtomicBoolean lockDataDelete;

		public ActionLocks() {
			lockCapture = new AtomicBoolean(false);
			lockSmsSend = new AtomicBoolean(false);
			lockEmailSend = new AtomicBoolean(false);
			lockLocationFind = new AtomicBoolean(false);
			lockDataDelete = new AtomicBoolean(false);
		}

		public void reset() {
			lockCapture.set(false);
			lockSmsSend.set(false);
			lockEmailSend.set(false);
			lockLocationFind.set(false);
			lockDataDelete.set(false);
		}
	}

	private static ActionLocks actionLocks = null;

	private SharedPreferences preferences;

	private static GetBackStateFlags stateFlags = new GetBackStateFlags();
	private static GetBackFeatures features = new GetBackFeatures();

	private static boolean isModeActive = false;

	private String currentAddress = null;
	private String photoPath = null;

	private LocationManager locationManager = null;

	public GetBackCoreService() {
		super();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Service Created");

		// register screen on/off receiver
		IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOnOffReceiver, screenFilter);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, gpsStatusListener);

		actionLocks = new ActionLocks();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Service onStartCommand");

		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (intent != null) {
			String action = intent.getAction();

			Utils.LogUtil.LogD(Constants.LOG_TAG, "Action = " + action);

			initStates();
			initFeatures();

			// Init all switches and flags at every start
			if (action != null) {

				if (action.equals(Constants.ACTION_BOOT_COMPLETED)) {
					if (stateFlags.isTheftTriggered) {
						takeAction(null);
					} else {
						new Thread(new SIMCheckingThread()).start();
					}
				} else if (action.equals(Constants.ACTION_SMS_RECEIVED)) {
					synchronized (stateFlags) {
						stateFlags.isTriggerSmsReceived = true;
						triggerTheft();
						Bundle bundle = intent.getExtras();
						if (bundle != null) {
							remoteSmsOrigin = bundle
									.getString(Constants.KEY_SENDER);
							triggerCommand = Utils.parseCommandFromSms(bundle
									.getString(Constants.KEY_MESSAGE_BODY));
							Utils.LogUtil.LogD(Constants.LOG_TAG,
									"remoteSmsOrigin - " + remoteSmsOrigin);
							Utils.LogUtil.LogD(Constants.LOG_TAG,
									"triggerCommand - " + triggerCommand);
							addStringPreference(
									Constants.PREFERENCE_TRIGGER_ORIGIN,
									remoteSmsOrigin);
							addStringPreference(
									Constants.PREFERENCE_TRIGGER_COMMAND,
									triggerCommand);

							// We will re-send SMS notification for primary
							// actions command every-time we get remote sms. so
							// here we are purposely setting isSmsSent to false.
							String[] configCommands = Utils
									.getConfiguredCommandNo(this);
							if (triggerCommand != null
									&& triggerCommand.equals(configCommands[0])) {
								synchronized (stateFlags) {
									stateFlags.isSmsSent = false;
									addBooleanPreference(
											Constants.PREFERENCE_IS_SMS_SENT,
											stateFlags.isSmsSent);
								}

								actionLocks.lockSmsSend.set(false);
								actionLocks.lockLocationFind.set(false);
							}
						}
						takeAction(null);
					}
				} else if (action.equals(Constants.ACTION_NETWORKSTATE_CHANGED)) {
					synchronized (stateFlags) {
						stateFlags.isNetworkAvailable = Utils
								.getConnectivityStatus(this);
						takeAction(null);
					}
				} else if (action.equals(Constants.ACTION_CLEAR_GETBACK)) {
					reinitGetBackIntoVigilantMode();
				} else if (action.equals(Constants.ACTION_TEST)) {

					stateFlags.isTriggerSmsReceived = true;
					remoteSmsOrigin = "8793841987";
					triggerCommand = "1001";
					triggerTheft();
					addStringPreference(Constants.PREFERENCE_TRIGGER_ORIGIN,
							remoteSmsOrigin);
					addStringPreference(Constants.PREFERENCE_TRIGGER_COMMAND,
							triggerCommand);

					takeAction(null);
				}
			}
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {

		super.onDestroy();

		unregisterReceiver(screenOnOffReceiver);
		locationManager.removeUpdates(gpsStatusListener);
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Service Destroyed");

		actionLocks.reset();
	}

	private void initFeatures() {

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Initializing features");

		features.setPhotoCapture(preferences.getBoolean(getResources()
				.getString(R.string.pref_key_capture), false));
		features.setLocation(preferences.getBoolean(
				getResources().getString(R.string.pref_key_location), false));
		features.setClearContacts(preferences.getBoolean(getResources()
				.getString(R.string.pref_key_contacts), false));
		features.setClearSms(preferences.getBoolean(
				getResources().getString(R.string.pref_key_sms), false));
		features.setFormatSdCard(preferences.getBoolean(getResources()
				.getString(R.string.pref_key_sdcard_format), false));
		features.setClearEmailAccounts(preferences.getBoolean(getResources()
				.getString(R.string.pref_key_remove_account), false));
	}

	private void initStates() {
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Initializing states");

		synchronized (stateFlags) {
			stateFlags.isTheftTriggered = preferences.getBoolean(
					Constants.PREFERENCE_IS_THEFT_FLAG, false);
			stateFlags.isPhotoCaptured = preferences.getBoolean(
					Constants.PREFERENCE_IS_PHOTO_CAPTURED, false);

			if (stateFlags.isPhotoCaptured)
				photoPath = preferences.getString(
						Constants.PREFERENCE_PHOTO_PATH, "");

			stateFlags.isEmailSent = preferences.getBoolean(
					Constants.PREFERENCE_IS_EMAIL_SENT, false);
			stateFlags.isSmsSent = preferences.getBoolean(
					Constants.PREFERENCE_IS_SMS_SENT, false);
			stateFlags.isDataDeleted = preferences.getBoolean(
					Constants.PREFERENCE_IS_DATA_DELETED, false);
			stateFlags.isNetworkAvailable = Utils.getConnectivityStatus(this);
			isModeActive = preferences.getBoolean(
					getResources().getString(R.string.pref_key_operation_mode),
					false);

			remoteSmsOrigin = preferences.getString(
					Constants.PREFERENCE_TRIGGER_ORIGIN, null);
			triggerCommand = preferences.getString(
					Constants.PREFERENCE_TRIGGER_COMMAND, null);
		}
	}

	// We might be arriving here from multiple threads
	private synchronized void takeAction(Bundle bundle) {

		Utils.LogUtil.LogD(Constants.LOG_TAG, "Inside takeAction");
		Utils.LogUtil.LogD(Constants.LOG_TAG,
				"Enabled features : " + features.toString());
		Utils.LogUtil.LogD(Constants.LOG_TAG,
				"State Flags : " + stateFlags.toString());
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Active Mode : " + isModeActive);

		if (isModeActive) {

			if (features.isPhotoCapture() && !stateFlags.isPhotoCaptured) {
				if (stateFlags.isScreenOn)
					if (stateFlags.isTheftTriggered)
						capturePhoto();
			}

			if (features.isLocation() && !stateFlags.isLocationFound) {
				if (stateFlags.isTheftTriggered) {
					findCurrentLocation();
				}
			}

			Utils.LogUtil.LogD(Constants.LOG_TAG, "Before sendSms Block");

			// check if we got trigger command
			if (stateFlags.isTriggerSmsReceived) {

				if (triggerCommand != null) {
					String[] configCommands = Utils
							.getConfiguredCommandNo(this);
					if (triggerCommand.equals(configCommands[0])) {
						if (!stateFlags.isSmsSent) {
							if (features.isLocation()) {
								LocationFinder location = new LocationFinder(
										this);
								if (location.canFindLocation()) {
									if (stateFlags.isLocationFound) {
										sendSms(currentAddress, remoteSmsOrigin);
									} else {
										Utils.LogUtil
												.LogD(Constants.LOG_TAG,
														"Waiting for location to be found");
									}
								} else
									sendSms(null, remoteSmsOrigin);
							} else
								sendSms(null, remoteSmsOrigin);
						}
					} else if (triggerCommand.equals(configCommands[1])) {

						if (!stateFlags.isDataDeleted)
							deleteUserData();
					} else {
						// Even though command text was Ok, Command no is
						// invalid
						Utils.LogUtil.LogW(Constants.LOG_TAG,
								"Received invalid command No !!! ");
					}
				} else
					Utils.LogUtil
							.LogE(Constants.LOG_TAG,
									"Command no. might be missing in Trigger SMS",
									null);
			}

			if (!stateFlags.isEmailSent) {
				if (stateFlags.isNetworkAvailable) {
					if (stateFlags.isPhotoCaptured
							&& stateFlags.isTheftTriggered) {
						if (photoPath != null) {

							String configEmail = preferences.getString(
									getResources().getString(
											R.string.pref_key_email), "");
							if (!configEmail.isEmpty())
								sendEmail(configEmail, photoPath);
						}
					}
				}
			}

			// Sending sms on pre-configured number
			if (!stateFlags.isSmsSent) {
				String alternativeNo = getAlternatvieNo();
				if (alternativeNo != null && !alternativeNo.isEmpty()) {
					if (features.isLocation()) {
						LocationFinder location = new LocationFinder(this);
						if (location.canFindLocation()) {
							if (stateFlags.isLocationFound)
								sendSms(currentAddress, alternativeNo);
							else
								return;
						}
					}
					sendSms(null, alternativeNo);
				}
			}
		}
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Exiting takeAction()");
	}

	private class SIMCheckingThread implements Runnable {

		@Override
		public void run() {
			try {
				boolean isSIMReady = false;
				// check for SIM change
				final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
				if (tm.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
					while (!isSIMReady) {
						isSIMReady = tm.getSimState() == TelephonyManager.SIM_STATE_READY;
						if (isSIMReady) {
							if (isSimCanged()) {
								Utils.LogUtil.LogD(Constants.LOG_TAG,
										"SIM is changed ");
								triggerTheft();
								takeAction(null);
							} else {
								Utils.LogUtil.LogD(Constants.LOG_TAG,
										"In Vigilant Mode !!!");
							}
						} else {
							Thread.sleep(5000);
						}
					}
				}
			} catch (InterruptedException e) {
				Utils.LogUtil.LogE(Constants.LOG_TAG,
						"Can't wait for SIM Ready state.", e);
			}
		}
	}

	private void findCurrentLocation() {

		if (actionLocks.lockLocationFind.compareAndSet(false, true)) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					Utils.LogUtil.LogD(Constants.LOG_TAG,
							"Inside findLocation thread");

					Looper.prepare();
					LocationFinder locFinder = new LocationFinder(
							GetBackCoreService.this);
					if (!locFinder.findLocation(GetBackCoreService.this)) {
						Utils.LogUtil.LogD(Constants.LOG_TAG,
								"Location can not be found right now");
					}
					Looper.loop();
				}
			}).start();
		}
	}

	private void capturePhoto() {

		if (actionLocks.lockCapture.compareAndSet(false, true)) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					Utils.LogUtil.LogD(Constants.LOG_TAG,
							"Inside captureThread run");

					Looper.prepare();

					// Check if phone is being used.
					FrontCapture frontCapture = new FrontCapture(
							GetBackCoreService.this.getBaseContext());
					frontCapture.capturePhoto(GetBackCoreService.this);

					Looper.loop();

				}
			}).start();
		}
	}

	private void sendSms(final String address, final String receipient) {

		if (actionLocks.lockSmsSend.compareAndSet(false, true)) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					Utils.LogUtil.LogD(Constants.LOG_TAG,
							"Inside sendSmsThread run");
					TelephonyManager telephonyMan = (TelephonyManager) GetBackCoreService.this
							.getSystemService(Context.TELEPHONY_SERVICE);
					String messageBody = "GetBack Alert: [Serial] = "
							+ telephonyMan.getSimSerialNumber()
							+ ",[Operator] = "
							+ telephonyMan.getNetworkOperatorName();
					if (address != null)
						messageBody += ",[Location] = " + address;

					CounterAction.sendSMS(
							GetBackCoreService.this.getApplicationContext(),
							receipient, messageBody, GetBackCoreService.this);
				}
			}).start();
		}
	}

	private void sendEmail(final String configuredEmailId,
			final String photoPath) {

		if (actionLocks.lockEmailSend.compareAndSet(false, true)) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					Utils.LogUtil.LogD(Constants.LOG_TAG,
							"Inside sendEmailThread run");
					if (!configuredEmailId.isEmpty()) {
						String body = "Hi, "
								+ "\n"
								+ "		Anti-theft actions are triggered on your phone. Which means your phone might be stolen or lost."
								+ " Attached is the captured photo at the time of trigger."
								+ "\n" + "Thank you.";
						CounterAction.sendEmail(
								"Alert Notification from GetBack !", body,
								configuredEmailId, photoPath,
								GetBackCoreService.this);
					}
				}
			}).start();
		}
	}

	private void deleteUserData() {

		if (actionLocks.lockDataDelete.compareAndSet(false, true)) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					Utils.LogUtil.LogD(Constants.LOG_TAG,
							"Inside deleteDataThread run");
					if (features.isClearSms()) {
						// Clear SMS
						CounterAction.clearAllSMS(GetBackCoreService.this);
					}

					if (features.isClearContacts()) {

						// Take backup of contacts into .vcf file
						String vCardPath = CounterAction
								.backupContacts(GetBackCoreService.this);
						if (vCardPath == null) {
							Utils.LogUtil.LogW(Constants.LOG_TAG,
									"vCard file not prepared");
						} else {
							String configEmail = preferences.getString(
									getResources().getString(
											R.string.pref_key_email), "");
							if (!configEmail.isEmpty()) {
								CounterAction
										.sendEmail(
												"Alert Notification from GetBack !",
												"Hi, "
														+ "\n"
														+ "		GetBack has attached a vcard file which is a backup of all contacts."
														+ "\n" + "Thank you.",
												configEmail, vCardPath, null);
							}
						}
						// But we must wipe contacts even though we might failed
						// to take backup, or failed to send backup
						CounterAction.clearAllContacts(GetBackCoreService.this);
					}

					if (features.isFormatSdCard()) {
						// Format SD Card
						CounterAction
								.formatExternalStorage(GetBackCoreService.this);
					}

					if (features.isClearEmailAccounts()) {
						// Remove Accounts
						CounterAction.removeAccounts(GetBackCoreService.this);
					}

					synchronized (stateFlags) {
						stateFlags.isDataDeleted = true;
						addBooleanPreference(
								Constants.PREFERENCE_IS_DATA_DELETED,
								stateFlags.isDataDeleted);
					}

					actionLocks.lockDataDelete.set(false);
				}
			}).start();
		}
	}

	private void reinitGetBackIntoVigilantMode() {

		Utils.LogUtil.LogD(Constants.LOG_TAG,
				"Going to Reiniate states of GetBack");

		synchronized (stateFlags) {

			removeKey(Constants.PREFERENCE_TRIGGER_ORIGIN);
			removeKey(Constants.PREFERENCE_TRIGGER_COMMAND);
			remoteSmsOrigin = null;
			triggerCommand = null;

			removeKey(Constants.PREFERENCE_IS_THEFT_FLAG);
			removeKey(Constants.PREFERENCE_IS_PHOTO_CAPTURED);
			removeKey(Constants.PREFERENCE_PHOTO_PATH);
			removeKey(Constants.PREFERENCE_IS_EMAIL_SENT);
			removeKey(Constants.PREFERENCE_IS_SMS_SENT);
			removeKey(Constants.PREFERENCE_IS_DATA_DELETED);

			stateFlags.reset();
		}
		actionLocks.reset();
	}

	private boolean isSimCanged() {

		boolean bReturn = false;
		String simSerialPref = preferences.getString(
				Constants.PREFERENCE_SIM_SERIAL, "");
		TelephonyManager telephonyMgr = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String simSerial = telephonyMgr.getSimSerialNumber();

		if (simSerial != null) {
			if (simSerialPref == null || simSerialPref.isEmpty()) {
				// At the beginning store user's SIM serial
				// addStringPreference(Constants.PREFERENCE_SIM_SERIAL,
				// simSerial);
			} else {
				if (!simSerial.equals(simSerialPref))
					bReturn = true;
			}
		}
		return bReturn;
	}

	private void triggerTheft() {
		stateFlags.isTheftTriggered = true;
		addBooleanPreference(Constants.PREFERENCE_IS_THEFT_FLAG, true);
	}

	private String getAlternatvieNo() {
		return preferences.getString(
				getResources().getString(R.string.pref_key_alternative_no), "");
	}

	@Override
	public void onLocationFound(String address) {
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Current Address : " + address);

		synchronized (stateFlags) {
			stateFlags.isLocationFound = true;
			currentAddress = address;

			if (stateFlags.isSmsSent) {
				// It means we already sent sms specifying new SIM number
				// But we need to send location now. so resend sms
				stateFlags.isSmsSent = false;
				addBooleanPreference(Constants.PREFERENCE_IS_SMS_SENT,
						stateFlags.isSmsSent);
			}
		}

		actionLocks.lockLocationFind.set(false);
		takeAction(null);
	}

	@Override
	public void onLocationError(int errorCode) {

		synchronized (stateFlags) {
			stateFlags.isLocationFound = false;
		}

		actionLocks.lockLocationFind.set(false);
		takeAction(null);
	}

	@Override
	public void onPhotoCaptured(String filePath) {
		synchronized (stateFlags) {
			stateFlags.isPhotoCaptured = true;
			addBooleanPreference(Constants.PREFERENCE_IS_PHOTO_CAPTURED,
					stateFlags.isPhotoCaptured);

			Utils.LogUtil.LogD(Constants.LOG_TAG, "Image saved at - "
					+ filePath);
			photoPath = filePath;
			addStringPreference(Constants.PREFERENCE_PHOTO_PATH, photoPath);
		}

		actionLocks.lockCapture.set(false);
		takeAction(null);
	}

	@Override
	public void onCaptureError(int errorCode) {
		synchronized (stateFlags) {
			stateFlags.isPhotoCaptured = false;
			addBooleanPreference(Constants.PREFERENCE_IS_PHOTO_CAPTURED,
					stateFlags.isPhotoCaptured);
		}

		actionLocks.lockCapture.set(false);
		takeAction(null);
	}

	@Override
	public void onSmsSent(String receipient) {
		Utils.LogUtil.LogD(Constants.LOG_TAG,
				"SMS Notification sent Successfully");

		synchronized (stateFlags) {
			stateFlags.isSmsSent = true;
			addBooleanPreference(Constants.PREFERENCE_IS_SMS_SENT,
					stateFlags.isSmsSent);
		}
		// Delete from sent box
		CounterAction.deleteSmsByAddress(this, receipient);

		actionLocks.lockSmsSend.set(false);
		takeAction(null);
	}

	@Override
	public void onSMSError(int errorCode) {
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Error in Sending SMS : "
				+ errorCode);

		synchronized (stateFlags) {
			stateFlags.isSmsSent = false;
			addBooleanPreference(Constants.PREFERENCE_IS_SMS_SENT,
					stateFlags.isSmsSent);
		}

		actionLocks.lockSmsSend.set(false);
		takeAction(null);
	}

	@Override
	public void onEmailSent() {
		Utils.LogUtil.LogD(Constants.LOG_TAG,
				"Email Notification sent successfully");

		synchronized (stateFlags) {
			stateFlags.isEmailSent = true;
			addBooleanPreference(Constants.PREFERENCE_IS_EMAIL_SENT,
					stateFlags.isEmailSent);

			// Delete photo
			File file = new File(photoPath);
			file.delete();
			removeKey(Constants.PREFERENCE_PHOTO_PATH);

			// Allow new photo capture at next trigger
			stateFlags.isPhotoCaptured = false;
			addBooleanPreference(Constants.PREFERENCE_IS_PHOTO_CAPTURED,
					stateFlags.isPhotoCaptured);
		}

		actionLocks.lockEmailSend.set(false);
		takeAction(null);
	}

	@Override
	public void onEmailError(int errorCode) {
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Error in Sending Email : "
				+ errorCode);

		synchronized (stateFlags) {
			stateFlags.isEmailSent = false;
			addBooleanPreference(Constants.PREFERENCE_IS_EMAIL_SENT,
					stateFlags.isEmailSent);
		}

		// Delete photo
		File file = new File(photoPath);
		file.delete();
		removeKey(Constants.PREFERENCE_PHOTO_PATH);

		actionLocks.lockEmailSend.set(false);
		takeAction(null);
	}

	private BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() != null
					&& intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				Utils.LogUtil.LogD(Constants.LOG_TAG,
						"Screen Off Intent received");
				stateFlags.isScreenOn = false;
			} else if (intent.getAction() != null
					&& intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				Utils.LogUtil.LogD(Constants.LOG_TAG,
						"Screen On Intent received");
				stateFlags.isScreenOn = true;
			}
			takeAction(null);
		}
	};

	private void addStringPreference(String key, String value) {
		Editor edit = preferences.edit();
		edit.putString(key, value);
		edit.commit();
	}

	private void addBooleanPreference(String key, boolean value) {
		Editor edit = preferences.edit();
		edit.putBoolean(key, value);
		edit.commit();
	}

	private void removeKey(String key) {
		Editor edit = preferences.edit();
		edit.remove(key);
		edit.commit();
	}

	// Define a listener that responds to location updates
	LocationListener gpsStatusListener = new LocationListener() {

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
			if (LocationManager.GPS_PROVIDER.equals(provider)) {
				if (stateFlags.isTheftTriggered)
					takeAction(null);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onLocationChanged(Location location) {
		}
	};
}
