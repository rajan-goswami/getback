package com.codeperf.getback.core;

public class GetBackStateFlags {

	public boolean isLocationFound = false;
	public boolean isPhotoCaptured = false;
	public boolean isTheftTriggered = false;
	public boolean isEmailSent = false;
	public boolean isSmsSent = false;
	public boolean isDataDeleted = false;
	public boolean isScreenOn = true;
	public boolean isTriggerSmsReceived = false;
	public boolean isNetworkAvailable = false;

	public GetBackStateFlags() {
		reset();
	}

	@Override
	public String toString() {
		return "isLocationFound = " + isLocationFound + ", isPhotoCaptured = "
				+ isPhotoCaptured + ", isTheftTriggered = " + isTheftTriggered
				+ ", isEmailSent = " + isEmailSent + ", isSmsSent = "
				+ isSmsSent + ", isDataDeleted = " + isDataDeleted
				+ ", isScreenOn = " + isScreenOn + ", isTriggerSmsReceived = "
				+ isTriggerSmsReceived + ", isNetworkAvailable = "
				+ isNetworkAvailable;
	}

	public void reset() {
		isLocationFound = isPhotoCaptured = isTheftTriggered = isEmailSent = isSmsSent = isDataDeleted = isScreenOn = isTriggerSmsReceived = isNetworkAvailable = false;
	}
}
