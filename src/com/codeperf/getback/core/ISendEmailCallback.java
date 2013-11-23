package com.codeperf.getback.core;

public interface ISendEmailCallback {

	public void onEmailSent();

	public void onEmailError(int errorCode);

	public static enum ErrorCode {

	}
}
