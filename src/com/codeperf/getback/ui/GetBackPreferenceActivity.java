package com.codeperf.getback.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.codeperf.getback.R;
import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;
import com.codeperf.getback.core.GetBackCoreService;

public class GetBackPreferenceActivity extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private Resources resources = null;

	private SharedPreferences sharedPref = null;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		resources = getResources();

		addPreferencesFromResource(R.xml.preferences);

		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		if (!Utils.isFrontCameraPresent(this)) {
			Preference capturePref = (Preference) findPreference(resources
					.getString(R.string.pref_key_capture));
			capturePref.setSummary(resources
					.getString(R.string.pref_summary_no_front_camera));
			capturePref.setEnabled(false);

			// PreferenceCategory mCategory = (PreferenceCategory)
			// findPreference(resources
			// .getString(R.string.pref_category_primary));
			// mCategory.removePreference(capturePref);
		}

		if (!Utils.getConnectivityStatus(this)) {
			PreferenceScreen mainPrefScreen = (PreferenceScreen) findPreference(resources
					.getString(R.string.pref_key_parent_preference));
			Preference adPref = (Preference) findPreference(resources
					.getString(R.string.pref_key_ad));
			mainPrefScreen.removePreference(adPref);
		}

		changePreferenceSummary(R.string.pref_key_email,
				R.string.pref_summary_register_email);
		changePreferenceSummary(R.string.pref_key_alternative_no,
				R.string.pref_summary_register_contact_no);
		changePreferenceSummary(R.string.pref_key_command_text,
				R.string.pref_summary_command_text);

		String commandText = sharedPref.getString(
				resources.getString(R.string.pref_key_command_text), "");

		String revokeNo = sharedPref.getString(
				resources.getString(R.string.pref_key_revoke_setup), "");
		if (revokeNo.isEmpty()) {
			revokeNo = resources.getString(R.string.default_revoke_number);
		}
		Preference revokePref = (Preference) findPreference(resources
				.getString(R.string.pref_key_revoke_setup));
		revokePref.setSummary(resources
				.getString(R.string.pref_summary_revoke_no) + " " + revokeNo);

		if (!commandText.isEmpty()) {
			String command1 = sharedPref
					.getString(resources
							.getString(R.string.pref_key_command_number_1), "");

			if (!command1.isEmpty()) {
				Preference pref = (Preference) findPreference(resources
						.getString(R.string.pref_key_command_number_1));
				pref.setSummary("SMS Format - " + commandText + "::" + command1);
			}

			String command2 = sharedPref
					.getString(resources
							.getString(R.string.pref_key_command_number_2), "");
			if (!command2.isEmpty()) {
				Preference pref = (Preference) findPreference(resources
						.getString(R.string.pref_key_command_number_2));
				pref.setSummary("SMS Format - " + commandText + "::" + command2);
			}
		} else {
			changeCommandNoPrefState(false);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		if (key.equals(resources.getString(R.string.pref_key_operation_mode))) {
			onChangedOperationMode(sharedPreferences, key);
		} else if (key.equals(resources
				.getString(R.string.pref_key_command_number_1))) {
			onChangedCommandNo1(sharedPreferences, key);
		} else if (key.equals(resources
				.getString(R.string.pref_key_command_number_2))) {
			onChangedCommandNo2(sharedPreferences, key);
		} else if (key.equals(resources
				.getString(R.string.pref_key_command_text))) {
			onChangedCommandText(sharedPreferences, key);
		} else if (key.equals(resources
				.getString(R.string.pref_key_revoke_setup))) {
			onChangedRevokeSetup(sharedPreferences, key);
		} else if (key.equals(resources.getString(R.string.pref_key_email))) {
			changePreferenceSummary(R.string.pref_key_email,
					R.string.pref_summary_register_email);
		} else if (key.equals(resources
				.getString(R.string.pref_key_alternative_no))) {
			changePreferenceSummary(R.string.pref_key_alternative_no,
					R.string.pref_summary_register_contact_no);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private void onChangedOperationMode(SharedPreferences sharedPreferences,
			String key) {
		boolean isActive = sharedPreferences.getBoolean(key, false);
		if (isActive) {
			Utils.hideApplication(getApplicationContext());
			Utils.configureCurrentSim(getApplicationContext());
			startService(new Intent(this, GetBackCoreService.class));
		} else {
			Utils.unhideApplication(getApplicationContext());
			Intent intent = new Intent(this, GetBackCoreService.class);
			intent.setAction(Constants.ACTION_CLEAR_GETBACK);
			startService(intent);
		}
	}

	@SuppressWarnings("deprecation")
	private void onChangedCommandNo1(SharedPreferences sharedPreferences,
			String key) {
		String value = sharedPreferences.getString(key, "");
		Preference commandPref = (Preference) findPreference(key);
		if (!value.isEmpty()) {

			String command2No = sharedPreferences
					.getString(resources
							.getString(R.string.pref_key_command_number_2), "");

			if (command2No.equals(value)) {
				Toast.makeText(this, "Command numbers must be unique",
						Toast.LENGTH_LONG).show();
				Editor editor = sharedPreferences.edit();
				editor.remove(key);
				editor.commit();
			} else {
				String commandText = sharedPreferences
						.getString(resources
								.getString(R.string.pref_key_command_text), "");
				commandPref.setSummary("SMS Format - " + commandText + "::"
						+ value);
			}
		} else {
			changePreferenceSummary(R.string.pref_key_command_number_1,
					R.string.pref_summary_command_no_1);
		}
	}

	@SuppressWarnings("deprecation")
	private void onChangedCommandNo2(SharedPreferences sharedPreferences,
			String key) {
		String value = sharedPreferences.getString(key, "");
		Preference commandPref = (Preference) findPreference(key);
		if (!value.isEmpty()) {

			String command1No = sharedPreferences
					.getString(resources
							.getString(R.string.pref_key_command_number_1), "");

			if (command1No.equals(value)) {
				Toast.makeText(this, "Command numbers must be unique",
						Toast.LENGTH_LONG).show();
				Editor editor = sharedPreferences.edit();
				editor.remove(key);
				editor.commit();
			} else {
				String commandText = sharedPreferences
						.getString(resources
								.getString(R.string.pref_key_command_text), "");
				commandPref.setSummary("SMS Format - " + commandText + "::"
						+ value);
			}
		} else {
			changePreferenceSummary(R.string.pref_key_command_number_2,
					R.string.pref_summary_command_no_2);
		}
	}

	@SuppressWarnings("deprecation")
	private void onChangedCommandText(SharedPreferences sharedPreferences,
			String key) {
		changePreferenceSummary(R.string.pref_key_command_text,
				R.string.pref_summary_command_text);
		String value = sharedPreferences.getString(key, "");
		String commandNo1 = sharedPreferences.getString(
				resources.getString(R.string.pref_key_command_number_1), "");
		String commandNo2 = sharedPreferences.getString(
				resources.getString(R.string.pref_key_command_number_2), "");
		Preference commandNo1Pref = (Preference) findPreference(resources
				.getString(R.string.pref_key_command_number_1));
		Preference commandNo2Pref = (Preference) findPreference(resources
				.getString(R.string.pref_key_command_number_2));
		if (value.isEmpty()) {
			// Disable command numbers
			commandNo1Pref.setSummary(commandNo1);
			commandNo2Pref.setSummary(commandNo2);
			changeCommandNoPrefState(false);
		} else {
			// Enable command numbers
			changeCommandNoPrefState(true);
			if (!commandNo1.isEmpty())
				commandNo1Pref.setSummary("SMS Format - " + value + "::"
						+ commandNo1);
			if (!commandNo2.isEmpty())
				commandNo2Pref.setSummary("SMS Format - " + value + "::"
						+ commandNo2);
		}
	}

	@SuppressWarnings("deprecation")
	private void onChangedRevokeSetup(SharedPreferences sharedPreferences,
			String key) {
		String value = sharedPreferences.getString(key, "");
		if (value.isEmpty()) {
			value = resources.getString(R.string.default_revoke_number);
		}
		Preference revokePref = (Preference) findPreference(key);
		revokePref.setSummary(resources
				.getString(R.string.pref_summary_revoke_no) + " " + value);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@SuppressWarnings("deprecation")
	private void changePreferenceSummary(int key, int summaryId) {
		String value = sharedPref.getString(resources.getString(key), "");
		Preference pref = findPreference(resources.getString(key));
		if (value.isEmpty()) {
			pref.setSummary(resources.getString(summaryId));
		} else {
			pref.setSummary(value);
		}
	}

	@SuppressWarnings("deprecation")
	private void changeCommandNoPrefState(boolean state) {
		Preference commandPref = (Preference) findPreference(resources
				.getString(R.string.pref_key_command_number_1));
		commandPref.setEnabled(state);

		commandPref = (EditTextPreference) findPreference(resources
				.getString(R.string.pref_key_command_number_2));
		commandPref.setEnabled(state);
	}
}
